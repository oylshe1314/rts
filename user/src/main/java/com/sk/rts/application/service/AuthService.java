package com.sk.rts.application.service;

import com.sk.rts.application.auth.*;
import com.sk.rts.application.component.builder.RedisKeyBuilder;
import com.sk.rts.application.component.token.TokenProperties;
import com.sk.rts.application.component.token.TokenUtil;
import com.sk.rts.application.entity.UserAccount;
import com.sk.rts.application.entity.UserDetails;
import com.sk.rts.application.entity.UserDevice;
import com.sk.rts.application.entity.enums.CaptchaType;
import com.sk.rts.application.exception.ResponseStatus;
import com.sk.rts.application.exception.StandardStatusException;
import com.sk.rts.application.jooq.Tables;
import com.sk.rts.application.jooq.tables.TableUserAccount;
import com.sk.rts.application.jooq.tables.TableUserDetails;
import com.sk.rts.application.jooq.tables.TableUserDevice;
import com.sk.rts.application.repository.UserDeviceRepository;
import com.sk.rts.application.util.CheckUtil;
import com.sk.rts.application.util.RandomUtil;
import com.sk.rts.application.util.TimeUtil;
import io.vertx.core.Future;
import io.vertx.redis.client.Command;
import io.vertx.redis.client.Redis;
import io.vertx.redis.client.Request;
import io.vertx.sqlclient.Pool;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.SqlConnection;
import io.vertx.sqlclient.Tuple;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jooq.DSLContext;
import org.jooq.Select;
import org.jspecify.annotations.NullMarked;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import tools.jackson.databind.json.JsonMapper;

import java.time.OffsetDateTime;

@Slf4j
@Service
@NullMarked
@AllArgsConstructor
public class AuthService {

    private final Pool pool;
    private final DSLContext dslContext;

    private final Redis redis;

    private final UserDeviceRepository userDeviceRepository;

    private final CaptchaService captchaService;

    private final PasswordEncoder passwordEncoder;

    private final TokenUtil tokenUtil;
    private final TokenProperties tokenProperties;

    private final JsonMapper jsonMapper;
    private final RedisKeyBuilder redisKeyBuilder;

    private Select<?> buildQuery(String account, UserRemoteDetails remoteDetails) {
        TableUserDetails u = Tables.USER_DETAILS.as("u");
        TableUserAccount a = Tables.USER_ACCOUNT.as("a");
        TableUserDevice d = Tables.USER_DEVICE.as("d");

        return dslContext.select(
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
                .leftJoin(d).on(d.USER_ID.eq(u.ID)).and(d.SERIAL_NO.eq(remoteDetails.getDeviceId()))
                .where(a.USERNAME.eq(account)).or(a.PHONE.eq(account)).or(a.EMAIL.eq(account));
    }

    public Future<UserDetails> queryUserDetails(SqlConnection connection, String account, UserRemoteDetails remoteDetails) {
        Select<?> query = buildQuery(account, remoteDetails);
        return connection.preparedQuery(query.getSQL()).execute(Tuple.tuple(query.getBindValues())).flatMap(rows -> {
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
        });
    }

    private UserAccessToken generateAccessToken(AuthDetails authDetails, OffsetDateTime issueTime) {
        OffsetDateTime expireTime = issueTime.plus(tokenProperties.getAccessToken().getExpiration());

        UserAccessToken accessToken = new UserAccessToken();
        accessToken.setSubject(UserAuthUtil.buildSubject(authDetails.getUserId(), authDetails.getDeviceId()));
        accessToken.setUserId(authDetails.getUserId());
        accessToken.setDeviceId(authDetails.getDeviceId());
        accessToken.setToken(tokenUtil.generate(authDetails, issueTime, expireTime));
        accessToken.setIssueTime(issueTime.toEpochSecond());
        accessToken.setExpireTime(expireTime.toEpochSecond());

        return accessToken;
    }

    private UserRefreshToken generateRefreshToken(AuthDetails authDetails, OffsetDateTime issueTime) {
        OffsetDateTime expireTime = issueTime.plus(tokenProperties.getRefreshToken().getExpiration());
        OffsetDateTime refreshTime = expireTime.minus(tokenProperties.getRefreshToken().getRefreshAdvance());

        UserRefreshToken refreshToken = new UserRefreshToken();
        refreshToken.setSubject(UserAuthUtil.buildSubject(authDetails.getUserId(), authDetails.getDeviceId()));
        refreshToken.setUserId(authDetails.getUserId());
        refreshToken.setDeviceId(authDetails.getDeviceId());
        refreshToken.setHash(RandomUtil.randomHash(authDetails.getUserId(), authDetails.getDeviceId()));
        refreshToken.setIssueTime(issueTime.toEpochSecond());
        refreshToken.setExpireTime(expireTime.toEpochSecond());
        refreshToken.setRefreshTime(refreshTime.toEpochSecond());

        return refreshToken;
    }

    private TokenDetails buildTokenDetails(AuthDetails authDetails) {
        OffsetDateTime now = OffsetDateTime.now();
        UserAccessToken accessToken = generateAccessToken(authDetails, now);
        UserRefreshToken refreshToken = generateRefreshToken(authDetails, now);

        return new TokenDetails(accessToken, refreshToken);
    }

    private AuthDetails buildAuthDetails(AuthUser authUser, AuthDevice authDevice, UserRemoteDetails remoteDetails) {
        AuthDetails authDetails = new AuthDetails();
        authDetails.setUserId(authUser.getId());
        authDetails.setDeviceId(authDevice.getId());
        authDetails.setSubject(UserAuthUtil.buildSubject(authDetails.getUserId(), authDetails.getDeviceId()));
        authDetails.setUsername(authUser.getUsername());
        authDetails.setPassword("");
        authDetails.setLocale(remoteDetails.getLocale());
        authDetails.setIpAddress(remoteDetails.getIpAddress());
        return authDetails;
    }

    private Future<Void> saveRefreshToken(UserRefreshToken refreshToken) {
        String hash = refreshToken.getHash();
        refreshToken.setHash(passwordEncoder.encode(hash));

        Request request = Request.cmd(Command.MSETEX);
        request.arg(1);

        request.arg(redisKeyBuilder.build("refreshToken", refreshToken.getSubject()));
        request.arg(jsonMapper.writeValueAsString(refreshToken));

        request.arg("EXAT");
        request.arg(refreshToken.getExpireTime());

        refreshToken.setHash(hash);

        return redis.send(request).mapEmpty();
    }

    private Future<Authentication> saveUserDetails(UserDetails userDetails, UserRemoteDetails remoteDetails) {
        AuthUser authUser = new AuthUser();
        authUser.setId(userDetails.getId());
        authUser.setUsername(userDetails.getAccount().getUsername());
        authUser.setEmail(userDetails.getAccount().getEmail());
        authUser.setPhone(userDetails.getAccount().getPhone());
        authUser.setPassword(userDetails.getAccount().getPassword());
        authUser.setNickname(userDetails.getNickname());
        authUser.setAvatar(userDetails.getAvatar());
        authUser.setRegisterTime(userDetails.getCreateTime());

        AuthDevice authDevice = new AuthDevice();
        authDevice.setId(userDetails.getDevice().getId());
        authDevice.setPlatform(userDetails.getDevice().getPlatform());
        authDevice.setDeviceId(userDetails.getDevice().getDeviceId());
        authDevice.setDeviceNo(userDetails.getDevice().getDeviceNo());
        authDevice.setAddTime(userDetails.getDevice().getCreateTime());

        Request request = Request.cmd(Command.MSETEX);
        request.arg(2);
        request.arg(redisKeyBuilder.build("authUser", authUser.getId()));
        request.arg(jsonMapper.writeValueAsString(authUser));
        request.arg(redisKeyBuilder.build("authDevice", authDevice.getId()));
        request.arg(jsonMapper.writeValueAsString(authDevice));

        return redis.send(request).flatMap(_ -> {
            AuthDetails authDetails = buildAuthDetails(authUser, authDevice, remoteDetails);
            TokenDetails tokenDetails = buildTokenDetails(authDetails);
            return saveRefreshToken(tokenDetails.getRefreshToken()).map(new UserAuthToken(authDetails, tokenDetails));
        });
    }

    public UserDevice createUserDevice(UserDetails userDetails, UserRemoteDetails remoteDetails) {
        UserDevice device = new UserDevice();
        device.setUserId(userDetails.getId());
        device.setPlatform(remoteDetails.getPlatform());
        device.setDeviceId(remoteDetails.getDeviceId());
        device.setDeviceNo(RandomUtil.randomNumbers(8));
        device.setCreateTime(OffsetDateTime.now());
        return device;
    }

    private Future<Authentication> login(SqlConnection connection, UserDetails userDetails, UserRemoteDetails remoteDetails) {
        if (userDetails.getDevice() == null) {
            UserDevice device = createUserDevice(userDetails, remoteDetails);
            return userDeviceRepository.insert(connection, device).flatMap(id -> {
                device.setId(id);
                userDetails.setDevice(device);
                return saveUserDetails(userDetails, remoteDetails);
            });
        } else {
            return saveUserDetails(userDetails, remoteDetails);
        }
    }

    public Future<Authentication> passwordLogin(String account, String password, UserRemoteDetails remoteDetails) {
        try {
            CheckUtil.checkDevice(remoteDetails.getPlatform(), remoteDetails.getDeviceId());
            CheckUtil.checkAccount(account);
            CheckUtil.safetyPassword(password);
        } catch (Exception exception) {
            return Future.failedFuture(new BadCredentialsException("", exception));
        }

        return pool.withConnection(connection -> queryUserDetails(connection, account, remoteDetails).flatMap(userDetails -> {
                    if (!passwordEncoder.matches(password, userDetails.getAccount().getPassword())) {
                        return Future.failedFuture(new BadCredentialsException("", new StandardStatusException("user.password.incorrect", "账号或密码不正确")));
                    }
                    return login(connection, userDetails, remoteDetails);
                })
        );
    }


    public Future<Authentication> captchaLogin(String account, String captcha, UserRemoteDetails remoteDetails) {
        try {
            CheckUtil.checkDevice(remoteDetails.getPlatform(), remoteDetails.getDeviceId());
            CheckUtil.checkAccount(account);
        } catch (Exception exception) {
            return Future.failedFuture(exception);
        }

        return captchaService.verify(CaptchaType.login, account, captcha).transform(ar -> {
            if (ar.failed()) {
                return Future.failedFuture(new BadCredentialsException("", ar.cause()));
            }
            return pool.withConnection(connection -> queryUserDetails(connection, account, remoteDetails).flatMap(userDetails -> login(connection, userDetails, remoteDetails)));
        });
    }

    private Select<?> buildQuery(Long userId, Long deviceId) {
        TableUserDetails u = Tables.USER_DETAILS.as("u");
        TableUserAccount a = Tables.USER_ACCOUNT.as("a");
        TableUserDevice d = Tables.USER_DEVICE.as("d");

        return dslContext.select(
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
                .innerJoin(d).on(d.ID.eq(deviceId)).and(d.USER_ID.eq(u.ID))
                .where(u.ID.eq(userId));
    }

    public Future<UserDetails> queryUserDetails(SqlConnection connection, Long userId, Long deviceId) {
        Select<?> query = buildQuery(userId, deviceId);
        return connection.preparedQuery(query.getSQL()).execute(Tuple.tuple(query.getBindValues())).flatMap(rows -> {
            if (rows.size() == 0) {
                return Future.failedFuture(new BadCredentialsException("", new StandardStatusException(ResponseStatus.internal_error)));
            }

            Row row = rows.iterator().next();
            UserDetails details = UserDetails.fromRow(row, 0);
            details.setAccount(UserAccount.fromRow(row, 6));
            if (row.getLong(11) != null) {
                details.setDevice(UserDevice.fromRow(row, 11));
            }

            return Future.succeededFuture(details);
        });
    }

    public Future<Authentication> tokenRefresh(String subject, String hash, UserRemoteDetails remoteDetails) {
        try {
            CheckUtil.checkDevice(remoteDetails.getPlatform(), remoteDetails.getDeviceId());
        } catch (Exception exception) {
            return Future.failedFuture(new BadCredentialsException("", exception));
        }

        String redisKey = redisKeyBuilder.build("refreshToken", subject);

        long now = TimeUtil.nowSeconds();
        return redis.send(Request.cmd(Command.GET).arg(redisKey)).flatMap(response -> {
            if (response == null) {
                return Future.failedFuture(new BadCredentialsException("", new StandardStatusException(ResponseStatus.token_expired)));
            }

            UserRefreshToken refreshToken = jsonMapper.readValue(response.toBytes(), UserRefreshToken.class);
            if (now >= refreshToken.getExpireTime()) {
                return Future.failedFuture(new BadCredentialsException("", new StandardStatusException(ResponseStatus.token_expired)));
            }

            if (!passwordEncoder.matches(hash, refreshToken.getHash())) {
                return Future.failedFuture(new BadCredentialsException("", new StandardStatusException(ResponseStatus.token_invalid)));
            }

            return pool.withConnection(connection -> queryUserDetails(connection, refreshToken.getUserId(), refreshToken.getDeviceId()).flatMap(userDetails -> {
                if (!remoteDetails.getPlatform().equals(userDetails.getDevice().getPlatform()) || !remoteDetails.getDeviceId().equals(userDetails.getDevice().getDeviceId())) {
                    return Future.failedFuture(new BadCredentialsException("", new StandardStatusException(ResponseStatus.token_invalid)));
                }

                return saveUserDetails(userDetails, remoteDetails);
            }));

        });
    }

    public Future<Void> logout(AuthDetails authDetails, UserAccessToken accessToken) {
        UserRefreshToken refreshToken = new UserRefreshToken();
        refreshToken.setSubject(accessToken.getSubject());
        Request request = Request.cmd(Command.DEL);
        request.arg(redisKeyBuilder.build("authUser", authDetails.getUserId()));
        request.arg(redisKeyBuilder.build("authDevice", authDetails.getDeviceId()));
        request.arg(redisKeyBuilder.build("refreshToken", accessToken.getSubject()));
        return redis.send(request).mapEmpty();
    }
}
