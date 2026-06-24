package com.sk.rts.application.auth;

import com.sk.rts.application.component.TokenUtil;
import com.sk.rts.application.exception.ResponseStatus;
import com.sk.rts.application.exception.StandardStatusException;
import com.sk.rts.application.proto.caching.MsgAccessToken;
import com.sk.rts.application.proto.caching.MsgUserDetails;
import com.sk.rts.application.proto.caching.MsgUserDevice;
import io.vertx.redis.client.Command;
import io.vertx.redis.client.Redis;
import io.vertx.redis.client.Request;
import lombok.AllArgsConstructor;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.http.server.reactive.ServerHttpRequest;
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

        if (context.getAuthentication() instanceof UserAuthToken authToken) {
            UserAuthDetails authDetails = (UserAuthDetails) authToken.getPrincipal();
            UserAccessToken accessToken = (UserAccessToken) authToken.getCredentials();

            Request request = Request.cmd(Command.MSET);
            request.arg(UserAuthDetails.buildTokenKey(accessToken.getSubject()));
            request.arg(accessToken.toProto().toByteArray());
            request.arg(UserAuthDetails.buildDetailsKey(authDetails.getUsername()));
            request.arg(authDetails.getDetails().toByteArray());
            request.arg(UserAuthDetails.buildDeviceKey(authDetails.getDeviceNo()));
            request.arg(authDetails.getDevice().toByteArray());

            return Mono.create(sink -> redis.send(request).onFailure(sink::error).onSuccess(_ -> sink.success()));
        } else {
            return Mono.error(new StandardStatusException(ResponseStatus.access_denied));
        }
    }

    @Override
    public Mono<SecurityContext> load(ServerWebExchange exchange) {
        ServerHttpRequest request = exchange.getRequest();
        String token = TokenUtil.getTokenFromRequest(request);
        if (token == null) {
            return Mono.empty();
        }

        String subject = tokenUtil.extractSubject(token);
        String[] keys = subject.split(":");

        Request redisRequest = Request.cmd(Command.GET);
        redisRequest.arg(UserAuthDetails.buildTokenKey(subject));
        redisRequest.arg(UserAuthDetails.buildDetailsKey(keys[0]));
        redisRequest.arg(UserAuthDetails.buildDeviceKey(keys[1]));

        return Mono.create(sink -> redis.send(redisRequest).onFailure(sink::error).onSuccess(response -> {
            try {
                if (response.get(0) != null) {
                    MsgAccessToken msgUserToken = MsgAccessToken.parseFrom(response.get(0).toBytes());
                    if (msgUserToken.getToken().equals(token)) {
                        if (response.get(1) != null && response.get(2) != null) {
                            MsgUserDetails msgUserDetails = MsgUserDetails.parseFrom(response.get(1).toBytes());
                            MsgUserDevice msgUserDevice = MsgUserDevice.parseFrom(response.get(2).toBytes());

                            UserAuthToken authToken = new UserAuthToken(new UserAuthDetails(msgUserDetails, msgUserDevice), new UserAccessToken(msgUserToken));
                            authToken.setDetails(new UserRemoteDetails(request));

                            sink.success(new SecurityContextImpl(authToken));
                            return;
                        }
                    }
                }
                sink.error(new StandardStatusException(ResponseStatus.token_expired));
            } catch (Exception e) {
                sink.error(new StandardStatusException(ResponseStatus.internal_error));
            }
        }));
    }
}
