package com.sk.rts.application.auth;

import com.sk.rts.application.component.TokenUtil;
import com.sk.rts.application.exception.ResponseStatus;
import com.sk.rts.application.exception.StandardStatusException;
import com.sk.rts.application.proto.caching.MsgUserDetails;
import com.sk.rts.application.proto.caching.MsgUserDevice;
import com.sk.rts.application.proto.caching.MsgUserToken;
import io.vertx.redis.client.Command;
import io.vertx.redis.client.Redis;
import io.vertx.redis.client.Request;
import lombok.AllArgsConstructor;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.web.server.context.ServerSecurityContextRepository;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
@NullMarked
@AllArgsConstructor
public class UserSecurityContextRepository implements ServerSecurityContextRepository {

    private final Redis redis;
    private final TokenUtil tokenUtil;

    @Override
    public Mono<Void> save(ServerWebExchange exchange, @Nullable SecurityContext context) {
        if (context == null) {
            return Mono.empty();
        }

        if (!(context.getAuthentication() instanceof UserAuthToken authToken)) {
            return Mono.error(new StandardStatusException(ResponseStatus.access_denied));
        } else {
            UserAuthDetails authDetails = (UserAuthDetails) authToken.getPrincipal();
            UserAccessToken accessToken = (UserAccessToken) authToken.getCredentials();

            MsgUserToken.Builder tokenBuilder = MsgUserToken.newBuilder();
            tokenBuilder.setSubject(accessToken.getSubject());
            tokenBuilder.setToken(accessToken.getToken());
            tokenBuilder.setIssueTime(accessToken.getIssueTime());
            tokenBuilder.setExpiration(accessToken.getExpiration());
            tokenBuilder.setUserId(authDetails.getUserId());
            tokenBuilder.setDeviceId(authDetails.getDeviceId());

            MsgUserToken msgUserToken = tokenBuilder.build();

            Request request = Request.cmd(Command.MSET);
            request.arg("message:user:token:" + msgUserToken.getSubject());
            request.arg(msgUserToken.toByteArray());
            request.arg("message:user:details:" + authDetails.getUserId());
            request.arg(authDetails.getDetails().toByteArray());
            request.arg("message:user:device:" + authDetails.getDeviceId());
            request.arg(authDetails.getDevice().toByteArray());

            return Mono.create(sink -> redis.send(request).onFailure(sink::error).onSuccess(_ -> sink.success()));
        }
    }

    @Override
    public Mono<SecurityContext> load(ServerWebExchange exchange) {
        String token = TokenUtil.getTokenFromRequest(exchange.getRequest());
        if (token == null) {
            return Mono.empty();
        }

        String subject = tokenUtil.extractSubject(token);

        Request tokenRequest = Request.cmd(Command.GET);
        tokenRequest.arg(subject);

        return Mono.create(sink -> redis.send(tokenRequest).onFailure(sink::error).onSuccess(tokenResponse -> {
            try {
                MsgUserToken msgUserToken = MsgUserToken.parseFrom(tokenResponse.toBytes());

                Request request = Request.cmd(Command.MGET);
                request.arg("message:user:details:" + msgUserToken.getUserId());
                request.arg("message:user:device:" + msgUserToken.getDeviceId());

                UserAccessToken accessToken = new UserAccessToken(
                        msgUserToken.getSubject(),
                        msgUserToken.getToken(),
                        msgUserToken.getIssueTime(),
                        msgUserToken.getExpiration()
                );

                redis.send(request).onFailure(sink::error).onSuccess(response -> {
                    try {
                        MsgUserDetails msgUserDetails = MsgUserDetails.parseFrom(response.get(0).toBytes());
                        MsgUserDevice msgUserDevice = MsgUserDevice.parseFrom(response.get(0).toBytes());

                        UserAuthDetails authDetails = new UserAuthDetails(msgUserDetails, msgUserDevice);
                        UserAuthToken authResult = new UserAuthToken(authDetails, accessToken);

                        sink.success(new SecurityContextImpl(authResult));
                    } catch (Exception e) {
                        sink.error(e);
                    }
                });
            } catch (Exception e) {
                sink.error(e);
            }
        }));
    }
}
