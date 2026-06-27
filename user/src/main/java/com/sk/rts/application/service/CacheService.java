package com.sk.rts.application.service;

import com.google.protobuf.InvalidProtocolBufferException;
import com.sk.rts.application.auth.UserAccessToken;
import com.sk.rts.application.auth.UserAuthDetails;
import com.sk.rts.application.auth.UserAuthUtil;
import com.sk.rts.application.auth.UserRefreshToken;
import com.sk.rts.application.config.TokenProperties;
import com.sk.rts.application.exception.ResponseStatus;
import com.sk.rts.application.exception.StandardStatusException;
import com.sk.rts.application.proto.caching.MsgAccessToken;
import com.sk.rts.application.proto.caching.MsgRefreshToken;
import com.sk.rts.application.proto.caching.MsgUserDetails;
import com.sk.rts.application.proto.caching.MsgUserDevice;
import io.vertx.redis.client.Command;
import io.vertx.redis.client.Redis;
import io.vertx.redis.client.Request;
import lombok.AllArgsConstructor;
import org.jspecify.annotations.NullMarked;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Service
@NullMarked
@AllArgsConstructor
public class CacheService {

    private final Redis redis;

    /**
     * 保存用户认证信息
     *
     * @param accessToken 访问TOKEN
     * @param authDetails 用户详情
     */
    public Mono<Void> saveUserAuthDetails(UserAuthDetails authDetails, UserAccessToken accessToken, Duration duration) {
        return Mono.create(sink -> {
            MsgAccessToken.Builder msgAccessTokenBuilder = MsgAccessToken.newBuilder();
            msgAccessTokenBuilder.setSubject(accessToken.getSubject());
            msgAccessTokenBuilder.setToken(accessToken.getToken());
            msgAccessTokenBuilder.setIssueTime(accessToken.getIssueTime());
            msgAccessTokenBuilder.setExpiration(accessToken.getExpireTime());

            MsgAccessToken msgAccessToken = msgAccessTokenBuilder.build();

            MsgUserDetails.Builder msgUserDetailsBuilder = MsgUserDetails.newBuilder();
            msgUserDetailsBuilder.setId(authDetails.getUserId());
            msgUserDetailsBuilder.setUsername(authDetails.getUsername());
            msgUserDetailsBuilder.setEmail(authDetails.getEmail());
            msgUserDetailsBuilder.setPhone(authDetails.getPhone());
            msgUserDetailsBuilder.setPassword(authDetails.getPassword());
            msgUserDetailsBuilder.setNickname(authDetails.getNickname());
            msgUserDetailsBuilder.setAvatar(authDetails.getAvatar());
            msgUserDetailsBuilder.setCreateTime(authDetails.getRegisterTime());

            MsgUserDetails msgUserDetails = msgUserDetailsBuilder.build();

            MsgUserDevice.Builder msgUserDeviceBuilder = MsgUserDevice.newBuilder();
            msgUserDeviceBuilder.setId(authDetails.getDeviceId());
            msgUserDeviceBuilder.setPlatform(authDetails.getPlatform());
            msgUserDeviceBuilder.setSerialNo(authDetails.getDeviceNo());
            msgUserDeviceBuilder.setDeviceNo(authDetails.getDeviceNo());
            msgUserDeviceBuilder.setCreateTime(authDetails.getDeviceTime());

            MsgUserDevice msgUserDevice = msgUserDeviceBuilder.build();

            Request request = Request.cmd(Command.MSETEX);
            request.arg(3);
            request.arg(UserAuthUtil.buildAccessTokenKey(accessToken.getSubject()));
            request.arg(msgAccessToken.toByteArray());
            request.arg(UserAuthUtil.buildDetailsKey(authDetails.getUserId()));
            request.arg(msgUserDetails.toByteArray());
            request.arg(UserAuthUtil.buildDeviceKey(authDetails.getDeviceId()));
            request.arg(msgUserDevice.toByteArray());
            request.arg("EX");
            request.arg(duration.toSeconds());

            redis.send(request).onSuccess(_ -> sink.success()).onFailure(sink::error);
        });
    }

    /**
     * 查询用户认证信息
     *
     * @param accessToken 访问TOKEn
     * @param authDetails 认证详情
     */
    public Mono<Void> queryUserAuthDetails(UserAuthDetails authDetails, UserAccessToken accessToken) {
        return Mono.create(sink -> {
            UserAuthUtil.parseSubject(accessToken.getSubject(), authDetails);

            Request request = Request.cmd(Command.MGET);
            request.arg(UserAuthUtil.buildAccessTokenKey(accessToken.getSubject()));
            request.arg(UserAuthUtil.buildDetailsKey(authDetails.getUserId()));
            request.arg(UserAuthUtil.buildDeviceKey(authDetails.getDeviceId()));

            redis.send(request).onFailure(sink::error).onSuccess(response -> {
                try {
                    if (response.get(0) != null) {
                        MsgAccessToken msgAccessToken = MsgAccessToken.parseFrom(response.get(0).toBytes());
                        if (accessToken.getToken().equals(msgAccessToken.getToken())) {
                            if (response.get(1) != null && response.get(2) != null) {
                                MsgUserDetails msgUserDetails = MsgUserDetails.parseFrom(response.get(1).toBytes());
                                MsgUserDevice msgUserDevice = MsgUserDevice.parseFrom(response.get(2).toBytes());

                                accessToken.setIssueTime(msgAccessToken.getIssueTime());
                                accessToken.setExpireTime(msgAccessToken.getExpiration());

                                authDetails.setUsername(msgUserDetails.getUsername());
                                authDetails.setEmail(msgUserDetails.getEmail());
                                authDetails.setPhone(msgUserDetails.getPhone());
                                authDetails.setPassword(msgUserDetails.getPassword());
                                authDetails.setNickname(msgUserDetails.getNickname());
                                authDetails.setAvatar(msgUserDetails.getAvatar());
                                authDetails.setRegisterTime(msgUserDetails.getCreateTime());
                                authDetails.setPlatform(msgUserDevice.getPlatform());
                                authDetails.setSerialNo(msgUserDevice.getSerialNo());
                                authDetails.setDeviceNo(msgUserDevice.getDeviceNo());
                                authDetails.setDeviceTime(msgUserDevice.getCreateTime());

                                sink.success();
                                return;
                            }
                        }
                    }
                    sink.error(new StandardStatusException(ResponseStatus.token_expired));
                } catch (InvalidProtocolBufferException e) {
                    sink.error(new StandardStatusException(ResponseStatus.internal_error));
                }
            });
        });
    }

    /**
     * 删除用户认证信息
     *
     * @param authDetails 用户认证信息
     * @param accessToken 用户访问TOKEN
     */
    public Mono<Void> removeUserAuthDetails(UserAuthDetails authDetails, UserAccessToken accessToken) {
        return Mono.create(sink -> {
            Request request = Request.cmd(Command.DEL);
            request.arg(UserAuthUtil.buildAccessTokenKey(accessToken.getSubject()));
            request.arg(UserAuthUtil.buildDeviceKey(authDetails.getDeviceId()));

            redis.send(request).onSuccess(_ -> sink.success()).onFailure(sink::error);
        });
    }

    public Mono<Void> saveUserRefreshToken(UserAuthDetails authDetails, UserRefreshToken refreshToken, Duration duration) {
        return Mono.create(sink -> {
            MsgRefreshToken.Builder msgRefreshTokenBuilder = MsgRefreshToken.newBuilder();
            msgRefreshTokenBuilder.setUserId(authDetails.getUserId());
            msgRefreshTokenBuilder.setDeviceId(authDetails.getDeviceId());
            msgRefreshTokenBuilder.setHash(refreshToken.getHash());
            msgRefreshTokenBuilder.setIssueTime(refreshToken.getIssueTime());
            msgRefreshTokenBuilder.setExpireTime(refreshToken.getExpireTime());
            msgRefreshTokenBuilder.setRefreshTime(refreshToken.getRefreshTime());

            MsgRefreshToken msgRefreshToken = msgRefreshTokenBuilder.build();

            Request request = Request.cmd(Command.SET);
            request.arg(UserAuthUtil.buildRefreshTokenKey(refreshToken.getSubject()));
            request.arg(msgRefreshToken.toByteArray());
            request.arg("EX");
            request.arg(duration.toSeconds());

            redis.send(request).onSuccess(_ -> sink.success()).onFailure(sink::error);
        });
    }

    public Mono<Void> queryUserRefreshToken(UserRefreshToken refreshToken) {
        return Mono.create(sink -> {
            Request request = Request.cmd(Command.GET);
            request.arg(UserAuthUtil.buildRefreshTokenKey(refreshToken.getSubject()));

            redis.send(request).onFailure(sink::error).onSuccess(response -> {

                try {
                    if (response.get(0) != null) {
                        MsgRefreshToken msgRefreshToken = MsgRefreshToken.parseFrom(response.toBytes());

                        refreshToken.setHash(msgRefreshToken.getHash());
                        refreshToken.setIssueTime(msgRefreshToken.getIssueTime());
                        refreshToken.setExpireTime(msgRefreshToken.getExpireTime());
                        refreshToken.setRefreshTime(msgRefreshToken.getRefreshTime());

                        sink.success();
                    }
                    sink.error(new StandardStatusException(ResponseStatus.token_expired));
                } catch (InvalidProtocolBufferException e) {
                    sink.error(new StandardStatusException(ResponseStatus.internal_error));
                }
            });
        });
    }

    public Mono<Void> removeUserRefreshToken(UserRefreshToken refreshToken) {
        return Mono.create(sink -> {
            Request request = Request.cmd(Command.DEL);
            request.arg(UserAuthUtil.buildAccessTokenKey(refreshToken.getSubject()));

            redis.send(request).onSuccess(_ -> sink.success()).onFailure(sink::error);
        });
    }
}
