package com.sk.rts.application.service;

import com.sk.rts.application.auth.*;
import com.sk.rts.application.component.TokenUtil;
import com.sk.rts.application.config.TokenProperties;
import com.sk.rts.application.entity.UserToken;
import com.sk.rts.application.entity.UserAccount;
import com.sk.rts.application.entity.UserDetails;
import com.sk.rts.application.entity.UserDevice;
import com.sk.rts.application.entity.enums.Platform;
import com.sk.rts.application.entity.enums.Status;
import com.sk.rts.application.exception.StandardStatusException;
import com.sk.rts.application.jooq.Tables;
import com.sk.rts.application.jooq.tables.TableUserAccount;
import com.sk.rts.application.jooq.tables.TableUserDetail;
import com.sk.rts.application.jooq.tables.TableUserDevice;
import com.sk.rts.application.proto.caching.MsgRefreshToken;
import com.sk.rts.application.repository.UserDeviceRepository;
import com.sk.rts.application.repository.UserTokenRepository;
import com.sk.rts.application.util.RandomUtil;
import io.vertx.core.Future;
import io.vertx.redis.client.Command;
import io.vertx.redis.client.Redis;
import io.vertx.redis.client.Request;
import io.vertx.sqlclient.Pool;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.Tuple;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jooq.DSLContext;
import org.jooq.SelectConditionStep;
import org.jspecify.annotations.NullMarked;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Mono;

import java.time.OffsetDateTime;

@Slf4j
@Service
@NullMarked
@AllArgsConstructor
public class AuthService {

    private final Pool pool;
    private final DSLContext dslContext;
    private final Redis redis;

    private UserDeviceRepository userDeviceRepository;
    private final UserTokenRepository userRefreshTokenRepository;

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

        TableUserDetail u = Tables.USER_DETAIL.as("u");
        TableUserAccount a = Tables.USER_ACCOUNT.as("a");
        TableUserDevice d = Tables.USER_DEVICE.as("d");

        SelectConditionStep<?> query = dslContext.select(
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
                        d.DEVICE_NO, // 12
                        d.CALLER, // 13
                        d.VERSION, // 14
                        d.CHANNEL, // 15
                        d.PLATFORM, // 16
                        d.SERIAL_NO, // 17
                        d.CREATE_TIME) // 18
                .from(u)
                .innerJoin(a).on(a.ID.eq(u.ID))
                .leftJoin(d).on(d.USER_ID.eq(u.ID))
                .where(a.USERNAME.eq(account)).or(a.PHONE.eq(account)).or(a.EMAIL.eq(account));

        return Mono.create(sink -> pool.getConnection().flatMap(connection -> connection.preparedQuery(query.getSQL()).execute(Tuple.tuple(query.getBindValues()))
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
                        device.setDeviceNo(RandomUtil.randomNumber(8));
                        device.setPlatform(platform.name());
                        device.setSerialNo(remoteDetails.getDevice());
                        device.setChannel(remoteDetails.getChannel());
                        device.setCaller(remoteDetails.getCaller());
                        device.setVersion(remoteDetails.getVersion());
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
                .map(details -> new UserAuthDetails(details.toProto(), details.getDevice().toProto()))
                .onSuccess(sink::success)
                .onFailure(sink::error)
        ));
    }

    public Mono<UserAuthDetails> captchaLogin(String account, String captcha, UserRemoteDetails remoteDetails) {
        return Mono.error(new UnsupportedOperationException("Not implements."));
    }

    public Mono<UserTokenDetails> generateToken(UserAuthDetails authDetails) {
        UserAccessToken userAccessToken = tokenUtil.generate(UserAuthDetails.buildSubject(authDetails.getUsername(), authDetails.getDeviceNo()));

        String token = RandomUtil.randomRefreshToken(authDetails.getUserId(), authDetails.getDeviceId());

        UserToken userToken = new UserToken();
        userToken.setUserId(authDetails.getUserId());
        userToken.setDeviceId(authDetails.getDeviceId());
        userToken.setHash(passwordEncoder.encode(token));
        userToken.setStatus(Status.enable.value());
        userToken.setIssueTime(OffsetDateTime.now());
        userToken.setExpireTime(userToken.getIssueTime().plus(tokenProperties.getRefreshToken().getExpiration()));
        userToken.setRefreshTime(userToken.getExpireTime().minus(tokenProperties.getRefreshToken().getRefreshAdvance()));

        return Mono.create(sink -> pool.getConnection().map(connection -> connection.begin()
                .compose(_ -> userRefreshTokenRepository.deleteByUserIdAndDeviceId(connection, authDetails.getUserId(), authDetails.getDeviceId()))
                .compose(_ -> userRefreshTokenRepository.insert(connection, userToken).onSuccess(userToken::setId))
                .compose(_ -> connection.transaction().commit())
                .flatMap(_ -> {
                    MsgRefreshToken msgRefreshToken = userToken.toProto();

                    Request request = Request.cmd(Command.SET);
                    request.arg("message:refresh:token" + msgRefreshToken.getKey());
                    request.arg(msgRefreshToken.toByteArray());

                    return redis.send(request).map(_ -> new UserRefreshToken(token, msgRefreshToken.getIssueTime(), msgRefreshToken.getExpireTime()));
                })
                .map(userRefreshToken -> new UserTokenDetails(userAccessToken, userRefreshToken))
                .onSuccess(sink::success)
                .onFailure(sink::error)
        ));
    }

    public Mono<UserTokenDetails> refreshToken(String token, UserRemoteDetails remoteDetails) {
        return Mono.error(new UnsupportedOperationException("Not implements."));
    }

    public Mono<Void> logout(UserAuthDetails authDetails) {
        return Mono.error(new UnsupportedOperationException("Not implements."));
    }
}
