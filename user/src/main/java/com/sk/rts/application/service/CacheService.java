package com.sk.rts.application.service;

import com.google.protobuf.InvalidProtocolBufferException;
import com.sk.rts.application.auth.UserAccessToken;
import com.sk.rts.application.auth.UserAuthDetails;
import com.sk.rts.application.auth.UserAuthUtil;
import com.sk.rts.application.auth.UserRefreshToken;
import com.sk.rts.application.exception.ResponseStatus;
import com.sk.rts.application.exception.StandardStatusException;
import com.sk.rts.application.proto.caching.MsgAccessToken;
import com.sk.rts.application.proto.caching.MsgRefreshToken;
import com.sk.rts.application.proto.caching.MsgUserDetails;
import com.sk.rts.application.proto.caching.MsgUserDevice;
import io.vertx.core.Future;
import io.vertx.redis.client.Command;
import io.vertx.redis.client.Redis;
import io.vertx.redis.client.Request;
import lombok.AllArgsConstructor;
import org.jspecify.annotations.NullMarked;
import org.springframework.stereotype.Service;

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
    public Future<Void> saveUserAuthDetails(UserAuthDetails authDetails, UserAccessToken accessToken) {
        MsgAccessToken.Builder msgAccessTokenBuilder = MsgAccessToken.newBuilder();
        msgAccessTokenBuilder.setSubject(accessToken.getSubject());
        msgAccessTokenBuilder.setToken(accessToken.getToken());
        msgAccessTokenBuilder.setIssueTime(accessToken.getIssueTime());
        msgAccessTokenBuilder.setExpireTime(accessToken.getExpireTime());

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
        request.arg(accessToken.getExpireTime() - accessToken.getIssueTime());

        return redis.send(request).mapEmpty();
    }

    /**
     * 查询用户认证信息
     *
     * @param accessToken 访问TOKEn
     * @param authDetails 认证详情
     */
    public Future<Void> queryUserAuthDetails(UserAuthDetails authDetails, UserAccessToken accessToken) {
        Request request = Request.cmd(Command.MGET);
        request.arg(UserAuthUtil.buildAccessTokenKey(accessToken.getSubject()));
        request.arg(UserAuthUtil.buildDetailsKey(authDetails.getUserId()));
        request.arg(UserAuthUtil.buildDeviceKey(authDetails.getDeviceId()));

        return redis.send(request).flatMap(response -> {
            try {
                if (response.get(0) != null) {
                    MsgAccessToken msgAccessToken = MsgAccessToken.parseFrom(response.get(0).toBytes());
                    if (accessToken.getToken().equals(msgAccessToken.getToken())) {
                        if (response.get(1) != null && response.get(2) != null) {
                            MsgUserDetails msgUserDetails = MsgUserDetails.parseFrom(response.get(1).toBytes());
                            MsgUserDevice msgUserDevice = MsgUserDevice.parseFrom(response.get(2).toBytes());

                            accessToken.setIssueTime(msgAccessToken.getIssueTime());
                            accessToken.setExpireTime(msgAccessToken.getExpireTime());

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

                            return Future.succeededFuture();
                        }
                    }
                }
                return Future.failedFuture(new StandardStatusException(ResponseStatus.token_expired));
            } catch (InvalidProtocolBufferException e) {
                return Future.failedFuture(new StandardStatusException(ResponseStatus.internal_error));
            }
        });
    }

    /**
     * 删除用户认证信息
     *
     * @param authDetails 用户认证信息
     * @param accessToken 用户访问TOKEN
     */
    public Future<Void> removeUserAuthDetails(UserAuthDetails authDetails, UserAccessToken accessToken) {
        Request request = Request.cmd(Command.DEL);
        request.arg(UserAuthUtil.buildAccessTokenKey(accessToken.getSubject()));
        request.arg(UserAuthUtil.buildDeviceKey(authDetails.getDeviceId()));

        return redis.send(request).mapEmpty();
    }

    public Future<Void> saveUserRefreshToken(UserRefreshToken refreshToken) {
        MsgRefreshToken.Builder msgRefreshTokenBuilder = MsgRefreshToken.newBuilder();
        msgRefreshTokenBuilder.setUserId(refreshToken.getUserId());
        msgRefreshTokenBuilder.setDeviceId(refreshToken.getDeviceId());
        msgRefreshTokenBuilder.setHash(refreshToken.getHash());
        msgRefreshTokenBuilder.setIssueTime(refreshToken.getIssueTime());
        msgRefreshTokenBuilder.setExpireTime(refreshToken.getExpireTime());
        msgRefreshTokenBuilder.setRefreshTime(refreshToken.getRefreshTime());

        MsgRefreshToken msgRefreshToken = msgRefreshTokenBuilder.build();

        Request request = Request.cmd(Command.SET);
        request.arg(UserAuthUtil.buildRefreshTokenKey(refreshToken.getSubject()));
        request.arg(msgRefreshToken.toByteArray());
        request.arg("EX");
        request.arg(refreshToken.getExpireTime() - refreshToken.getIssueTime());

        return redis.send(request).mapEmpty();
    }

    public Future<Void> queryUserRefreshToken(UserRefreshToken refreshToken) {
        Request request = Request.cmd(Command.GET);
        request.arg(UserAuthUtil.buildRefreshTokenKey(refreshToken.getSubject()));

        return redis.send(request).flatMap(response -> {
            try {
                if (response.get(0) == null) {
                    return Future.failedFuture(new StandardStatusException(ResponseStatus.token_expired));
                }

                MsgRefreshToken msgRefreshToken = MsgRefreshToken.parseFrom(response.toBytes());

                refreshToken.setHash(msgRefreshToken.getHash());
                refreshToken.setIssueTime(msgRefreshToken.getIssueTime());
                refreshToken.setExpireTime(msgRefreshToken.getExpireTime());
                refreshToken.setRefreshTime(msgRefreshToken.getRefreshTime());

                return Future.succeededFuture();
            } catch (InvalidProtocolBufferException e) {
                return Future.failedFuture(new StandardStatusException(ResponseStatus.internal_error));
            }
        });
    }

    public Future<Void> removeUserRefreshToken(UserRefreshToken refreshToken) {
        Request request = Request.cmd(Command.DEL);
        request.arg(UserAuthUtil.buildAccessTokenKey(refreshToken.getSubject()));

        return redis.send(request).mapEmpty();
    }
}
