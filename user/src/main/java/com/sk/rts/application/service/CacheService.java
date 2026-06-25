package com.sk.rts.application.service;

import com.google.protobuf.InvalidProtocolBufferException;
import com.sk.rts.application.auth.UserAccessToken;
import com.sk.rts.application.auth.UserAuthDetails;
import com.sk.rts.application.auth.UserAuthUtil;
import com.sk.rts.application.exception.ResponseStatus;
import com.sk.rts.application.exception.StandardStatusException;
import com.sk.rts.application.proto.caching.MsgAccessToken;
import com.sk.rts.application.proto.caching.MsgUserDetails;
import com.sk.rts.application.proto.caching.MsgUserDevice;
import io.vertx.core.Future;
import io.vertx.redis.client.Command;
import io.vertx.redis.client.Redis;
import io.vertx.redis.client.Request;
import lombok.AllArgsConstructor;
import org.jspecify.annotations.NullMarked;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@NullMarked
@AllArgsConstructor
public class CacheService {

    private final Redis redis;

    public Future<Void> saveUserAuthDetails(UserAccessToken accessToken, UserAuthDetails authDetails, Duration expiration) {
        MsgAccessToken.Builder msgAccessTokenBuilder = MsgAccessToken.newBuilder();
        msgAccessTokenBuilder.setSubject(accessToken.getSubject());
        msgAccessTokenBuilder.setToken(accessToken.getToken());
        msgAccessTokenBuilder.setIssueTime(accessToken.getIssueTime());
        msgAccessTokenBuilder.setExpiration(accessToken.getExpiration());

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
        msgUserDeviceBuilder.setDeviceNo(authDetails.getDeviceNo());
        msgUserDeviceBuilder.setPlatform(authDetails.getPlatform());
        msgUserDeviceBuilder.setSerialNo(authDetails.getDeviceNo());
        msgUserDeviceBuilder.setCreateTime(authDetails.getDeviceTime());

        MsgUserDevice msgUserDevice = msgUserDeviceBuilder.build();

        Request request = Request.cmd(Command.MSETEX);
        request.arg(3);
        request.arg(UserAuthUtil.buildTokenKey(accessToken.getSubject()));
        request.arg(msgAccessToken.toByteArray());
        request.arg(UserAuthUtil.buildDetailsKey(authDetails.getUserId()));
        request.arg(msgUserDetails.toByteArray());
        request.arg(UserAuthUtil.buildDeviceKey(authDetails.getDeviceId()));
        request.arg(msgUserDevice.toByteArray());
        request.arg("EX");
        request.arg(expiration.toSeconds());

        return redis.send(request).mapEmpty();
    }

    public Future<Void> queryUserAuthDetails(UserAccessToken accessToken, UserAuthDetails authDetails) {
        UserAuthUtil.parseSubject(accessToken.getSubject(), authDetails);

        Request request = Request.cmd(Command.MGET);
        request.arg(UserAuthUtil.buildTokenKey(accessToken.getSubject()));
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
                            accessToken.setExpiration(msgAccessToken.getExpiration());

                            authDetails.setUsername(msgUserDetails.getUsername());
                            authDetails.setEmail(msgUserDetails.getEmail());
                            authDetails.setPhone(msgUserDetails.getPhone());
                            authDetails.setPassword(msgUserDetails.getPassword());
                            authDetails.setNickname(msgUserDetails.getNickname());
                            authDetails.setAvatar(msgUserDetails.getAvatar());
                            authDetails.setRegisterTime(msgUserDetails.getCreateTime());
                            authDetails.setDeviceNo(msgUserDevice.getDeviceNo());
                            authDetails.setPlatform(msgUserDevice.getPlatform());
                            authDetails.setSerialNo(msgUserDevice.getSerialNo());
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
}
