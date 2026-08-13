package com.sk.rts.application.service;

import com.sk.rts.application.auth.*;
import com.sk.rts.application.component.TokenUtil;
import com.sk.rts.application.config.TokenProperties;
import com.sk.rts.application.entity.UserAccount;
import com.sk.rts.application.entity.UserDetails;
import com.sk.rts.application.entity.UserDevice;
import com.sk.rts.application.entity.enums.Platform;
import com.sk.rts.application.exception.ResponseStatus;
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
                        a.ID, // 6
                        a.USERNAME, // 7
                        a.PHONE, // 8
                        a.EMAIL, // 9
                        a.PASSWORD, // 10
                        d.ID, // 11
                        d.USER_ID, // 12
                        d.PLATFORM, // 13
                        d.SERIAL_NO, // 14
                        d.DEVICE_NO, // 15
                        d.CREATE_TIME) // 16
                .from(u)
                .innerJoin(a).on(a.ID.eq(u.ID));

        if (platform != Platform.web) {
            query = ((SelectJoinStep<?>) query).leftJoin(d).on(d.USER_ID.eq(u.ID)).and(d.SERIAL_NO.eq(remoteDetails.getDevice()));
        }

        query = ((SelectWhereStep<?>) query).where(a.USERNAME.eq(account)).or(a.PHONE.eq(account)).or(a.EMAIL.eq(account));

        String sql = query.getSQL();
        List<Object> args = query.getBindValues();
        return Mono.create(sink -> pool.getConnection().flatMap(connection -> connection.preparedQuery(sql).execute(Tuple.tuple(args))
                .flatMap(rows -> {
                    if (rows.size() == 0) {
                        return Future.failedFuture(new BadCredentialsException("", new StandardStatusException("user.username.incorrect", "账号或密码不正确")));
                    }

                    Row row = rows.iterator().next();
                    UserDetails details = UserDetails.fromRow(row, 0);
                    details.setAccount(UserAccount.fromRow(row, 6));
                    if (row.getLong(11) != null) {
                        details.setDevice(UserDevice.fromRow(row, 11));
                    }

                    return Future.succeededFuture(details);
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
                .onComplete(_ -> connection.close())
                .onSuccess(sink::success)
                .onFailure(sink::error)
        ));
    }

    public Mono<UserAuthDetails> captchaLogin(String account, String captcha, UserRemoteDetails remoteDetails) {
        return Mono.error(new UnsupportedOperationException("Not implements."));
    }

    private UserAccessToken generateAccessToken(UserAuthDetails authDetails, OffsetDateTime issueTime) {
        OffsetDateTime expireTime = issueTime.plus(tokenProperties.getAccessToken().getExpiration());

        UserAccessToken accessToken = new UserAccessToken();
        accessToken.setSubject(UserAuthUtil.buildSubject(authDetails.getUserId(), authDetails.getDeviceId()));
        accessToken.setUserId(authDetails.getUserId());
        accessToken.setDeviceId(authDetails.getDeviceId());
        accessToken.setToken(tokenUtil.generate(accessToken.getSubject(), issueTime, expireTime));
        accessToken.setIssueTime(issueTime.toEpochSecond());
        accessToken.setExpireTime(expireTime.toEpochSecond());

        return accessToken;
    }

    private UserRefreshToken generateRefreshToken(UserAuthDetails authDetails, OffsetDateTime issueTime) {
        OffsetDateTime expireTime = issueTime.plus(tokenProperties.getRefreshToken().getExpiration());
        OffsetDateTime refreshTime = expireTime.minus(tokenProperties.getRefreshToken().getRefreshAdvance());

        UserRefreshToken refreshToken = new UserRefreshToken();
        refreshToken.setSubject(UserAuthUtil.buildSubject(authDetails.getUserId(), authDetails.getDeviceId()));
        refreshToken.setUserId(authDetails.getUserId());
        refreshToken.setDeviceId(authDetails.getDeviceId());
        refreshToken.setHash(RandomUtil.randomRefreshToken(authDetails.getUserId(), authDetails.getDeviceId()));
        refreshToken.setIssueTime(issueTime.toEpochSecond());
        refreshToken.setExpireTime(expireTime.toEpochSecond());
        refreshToken.setRefreshTime(refreshTime.toEpochSecond());

        return refreshToken;
    }

    private Future<UserTokenDetails> generateUserToken(UserAuthDetails authDetails) {
        OffsetDateTime now = OffsetDateTime.now();
        UserAccessToken userAccessToken = generateAccessToken(authDetails, now);
        UserRefreshToken userRefreshToken = generateRefreshToken(authDetails, now);

        String hash = userRefreshToken.getHash();
        userRefreshToken.setHash(passwordEncoder.encode(hash));

        return cacheService.saveUserRefreshToken(userRefreshToken).map(_ -> {
            userRefreshToken.setHash(hash);
            return new UserTokenDetails(userAccessToken, userRefreshToken);
        });
    }

    public Mono<UserTokenDetails> generateToken(UserAuthDetails authDetails) {
        return Mono.create(sink -> generateUserToken(authDetails).onSuccess(sink::success).onFailure(sink::error));
    }

    public Mono<UserTokenDetails> refreshToken(String subject, String hash, UserRemoteDetails remoteDetails) {
        UserRefreshToken userRefreshToken = new UserRefreshToken();
        userRefreshToken.setSubject(subject);

        OffsetDateTime now = OffsetDateTime.now();
        long nowTimestamp = now.toEpochSecond();
        return Mono.create(sink -> cacheService.queryUserRefreshToken(userRefreshToken)
                .flatMap(_ -> {
                    if (nowTimestamp >= userRefreshToken.getExpireTime()) {
                        return Future.failedFuture(new StandardStatusException(ResponseStatus.token_expired));
                    }

                    if (!passwordEncoder.matches(hash, userRefreshToken.getHash())) {
                        return Future.failedFuture(new StandardStatusException(ResponseStatus.token_invalid));
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
                                    a.ID, // 6
                                    a.USERNAME, // 7
                                    a.PHONE, // 8
                                    a.EMAIL, // 9
                                    a.PASSWORD, // 10
                                    d.ID, // 11
                                    d.USER_ID, // 12
                                    d.PLATFORM, // 13
                                    d.SERIAL_NO, // 14
                                    d.DEVICE_NO, // 15
                                    d.CREATE_TIME) // 16
                            .from(u)
                            .innerJoin(a).on(a.ID.eq(u.ID))
                            .leftJoin(d).on(d.USER_ID.eq(u.ID)).and(d.ID.eq(userRefreshToken.getDeviceId()))
                            .where(u.ID.eq(userRefreshToken.getUserId()));

                    String sql = query.getSQL();
                    List<Object> args = query.getBindValues();
                    return pool.preparedQuery(sql).execute(Tuple.tuple(args))
                            .flatMap(rows -> {
                                if (rows.size() == 0) {
                                    return Future.failedFuture(new StandardStatusException(ResponseStatus.token_invalid));
                                }

                                Row row = rows.iterator().next();
                                UserDetails userDetails = UserDetails.fromRow(row, 0);
                                userDetails.setAccount(UserAccount.fromRow(row, 6));
                                if (row.getLong(11) != null) {
                                    userDetails.setDevice(UserDevice.fromRow(row, 11));
                                }

                                if (!userDetails.getDevice().getSerialNo().equals(remoteDetails.getDevice())) {
                                    return Future.failedFuture(new StandardStatusException(ResponseStatus.token_invalid));
                                }

                                UserAuthDetails authDetails = new UserAuthDetails(userDetails, remoteDetails);

                                UserAccessToken userAccessToken = generateAccessToken(authDetails, now);
                                if (nowTimestamp >= userRefreshToken.getRefreshTime()) {
                                    return generateUserToken(authDetails);
                                } else {
                                    return Future.succeededFuture(new UserTokenDetails(userAccessToken, userRefreshToken));
                                }
                            });
                })
                .onSuccess(sink::success)
                .onFailure(sink::error)
        );
    }

    public Mono<Void> logout(UserAuthDetails authDetails, UserAccessToken accessToken) {
        UserRefreshToken refreshToken = new UserRefreshToken();
        refreshToken.setSubject(accessToken.getSubject());
        return Mono.create(sink -> Future.all(cacheService.removeUserAuthDetails(authDetails, accessToken), cacheService.removeUserRefreshToken(refreshToken)).onSuccess(_ -> sink.success()).onFailure(sink::error));
    }
}
