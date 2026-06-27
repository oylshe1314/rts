package com.sk.rts.application.service;

import com.sk.rts.application.auth.*;
import com.sk.rts.application.component.TokenUtil;
import com.sk.rts.application.config.TokenProperties;
import com.sk.rts.application.entity.UserAccount;
import com.sk.rts.application.entity.UserDetails;
import com.sk.rts.application.entity.UserDevice;
import com.sk.rts.application.entity.enums.Platform;
import com.sk.rts.application.exception.StandardStatusException;
import com.sk.rts.application.jooq.Tables;
import com.sk.rts.application.jooq.tables.TableUserAccount;
import com.sk.rts.application.jooq.tables.TableUserDetails;
import com.sk.rts.application.jooq.tables.TableUserDevice;
import com.sk.rts.application.repository.UserDeviceRepository;
import com.sk.rts.application.util.RandomUtil;
import io.vertx.core.Future;
import io.vertx.sqlclient.Pool;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.Tuple;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jooq.DSLContext;
import org.jooq.Select;
import org.jooq.SelectJoinStep;
import org.jooq.SelectWhereStep;
import org.jspecify.annotations.NullMarked;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Mono;

import java.time.OffsetDateTime;
import java.util.List;

@Slf4j
@Service
@NullMarked
@AllArgsConstructor
public class AuthService {

    private final Pool pool;
    private final DSLContext dslContext;

    private final UserDeviceRepository userDeviceRepository;

    private final CacheService cacheService;

    private final PasswordEncoder passwordEncoder;
    private final TokenUtil tokenUtil;
    private final TokenProperties tokenProperties;

    public Mono<UserAuthDetails> passwordLogin(String account, String password, UserRemoteDetails remoteDetails) {
        Platform platform = Platform.parse(remoteDetails.getPlatform());
        if (platform == null) {
            log.warn("设备平台错误, platform={}", remoteDetails.getPlatform());
            return Mono.error(new BadCredentialsException("", new StandardStatusException("device.platform.error", "设备平台不正确")));
        }

        if (platform != Platform.web && !StringUtils.hasText(remoteDetails.getDevice())) {
            log.warn("设备序列号错误, device={}", remoteDetails.getPlatform());
            return Mono.error(new BadCredentialsException("", new StandardStatusException("device.serial.error", "设备序列号不正确")));
        }

        TableUserDetails u = Tables.USER_DETAILS.as("u");
        TableUserAccount a = Tables.USER_ACCOUNT.as("a");
        TableUserDevice d = Tables.USER_DEVICE.as("d");

        Select<?> query = dslContext.select(
                        u.ID, // 0
                        u.NICKNAME, // 1
                        u.AVATAR, // 2
                        u.GENDER, // 3
                        u.BIRTHDAY, // 4
                        u.CREATE_TIME, // 5
                        a.USERNAME, // 6
                        a.PHONE, // 7
                        a.EMAIL, // 8
                        a.PASSWORD, // 9
                        d.ID, // 10
                        d.USER_ID, // 11
                        d.PLATFORM, // 12
                        d.SERIAL_NO, // 13
                        d.DEVICE_NO, // 14
                        d.CREATE_TIME) // 15
                .from(u)
                .innerJoin(a).on(a.ID.eq(u.ID));

        if (platform != Platform.web) {
            query = ((SelectJoinStep<?>) query).leftJoin(d).on(d.USER_ID.eq(u.ID)).and(d.SERIAL_NO.eq(remoteDetails.getDevice()));
        }

        query = ((SelectWhereStep<?>) query).where(a.USERNAME.eq(account)).or(a.PHONE.eq(account)).or(a.EMAIL.eq(account));

        String sql = query.getSQL();
        List<Object> args = query.getBindValues();
        return Mono.create(sink -> pool.getConnection().flatMap(connection -> connection.preparedQuery(sql).execute(Tuple.tuple(args))
                .map(rows -> {
                    if (rows.size() == 0) {
                        throw new BadCredentialsException("", new StandardStatusException("user.username.incorrect", "账号或密码不正确"));
                    }

                    Row row = rows.iterator().next();
                    UserDetails details = UserDetails.fromRow(row, 0);
                    details.setAccount(UserAccount.fromRow(row, 6));
                    if (row.getLong(10) != null) {
                        details.setDevice(UserDevice.fromRow(row, 10));
                    }

                    return details;
                })
                .flatMap(details -> {
                    if (!passwordEncoder.matches(password, details.getAccount().getPassword())) {
                        return Future.failedFuture(new BadCredentialsException("", new StandardStatusException("user.password.incorrect", "账号或密码不正确")));
                    }

                    if (details.getDevice() == null) {
                        UserDevice device = new UserDevice();
                        device.setUserId(details.getId());
                        device.setPlatform(platform.name());
                        device.setSerialNo(remoteDetails.getDevice());
                        device.setDeviceNo(RandomUtil.randomNumber(8));
                        device.setCreateTime(OffsetDateTime.now());

                        return userDeviceRepository.insert(connection, device).map(id -> {
                            device.setId(id);
                            details.setDevice(device);
                            return details;
                        });
                    } else {
                        return Future.succeededFuture(details);
                    }
                })
                .map(details -> new UserAuthDetails(details, remoteDetails))
                .onSuccess(sink::success)
                .onFailure(sink::error)
        ));
    }

    public Mono<UserAuthDetails> captchaLogin(String account, String captcha, UserRemoteDetails remoteDetails) {
        return Mono.error(new UnsupportedOperationException("Not implements."));
    }

    public Mono<UserTokenDetails> generateToken(UserAuthDetails authDetails) {
        UserAccessToken userAccessToken = tokenUtil.generate(UserAuthUtil.buildSubject(authDetails.getUserId(), authDetails.getDeviceId()));

        String hash = RandomUtil.randomRefreshToken(authDetails.getUserId(), authDetails.getDeviceId());

        UserRefreshToken userRefreshToken = new UserRefreshToken();
        userRefreshToken.setSubject(userAccessToken.getSubject());
        userRefreshToken.setHash(passwordEncoder.encode(hash));
        userRefreshToken.setIssueTime(userAccessToken.getIssueTime());
        userRefreshToken.setExpireTime(userRefreshToken.getIssueTime() + tokenProperties.getRefreshToken().getExpiration().toSeconds());
        userRefreshToken.setRefreshTime(userRefreshToken.getExpireTime() - tokenProperties.getRefreshToken().getRefreshAdvance().toSeconds());

        return cacheService.saveUserRefreshToken(authDetails, userRefreshToken, tokenProperties.getRefreshToken().getExpiration()).doOnSuccess(_ -> userRefreshToken.setHash(hash)).thenReturn(new UserTokenDetails(userAccessToken, userRefreshToken));
    }

    public Mono<UserTokenDetails> refreshToken(String token, UserRemoteDetails remoteDetails) {
        return Mono.error(new UnsupportedOperationException("Not implements."));
    }

    public Mono<Void> logout(UserAuthDetails authDetails, UserAccessToken accessToken) {
        UserRefreshToken refreshToken = new UserRefreshToken();
        refreshToken.setSubject(accessToken.getSubject());
        return cacheService.removeUserRefreshToken(refreshToken).then(cacheService.removeUserAuthDetails(authDetails, accessToken));
    }
}
