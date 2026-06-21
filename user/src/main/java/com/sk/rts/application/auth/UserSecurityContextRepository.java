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

        if (context.getAuthentication() instanceof UserAuthToken authToken) {
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
            request.arg(UserAuthDetails.buildTokenKey(msgUserToken.getSubject()));
            request.arg(msgUserToken.toByteArray());
            request.arg(UserAuthDetails.buildDetailsKey(authDetails.getUsername()));
            request.arg(authDetails.getDetails().toByteArray());
            request.arg(UserAuthDetails.buildDeviceKey(authDetails.getDevice().getDeviceNo()));
            request.arg(authDetails.getDevice().toByteArray());

            return Mono.create(sink -> redis.send(request).onFailure(sink::error).onSuccess(_ -> sink.success()));
        } else {
            return Mono.error(new StandardStatusException(ResponseStatus.access_denied));
        }
    }

    @Override
    public Mono<SecurityContext> load(ServerWebExchange exchange) {
        String token = TokenUtil.getTokenFromRequest(exchange.getRequest());
        if (token == null) {
            return Mono.empty();
        }

        String subject = tokenUtil.extractSubject(token);
        String[] keys = subject.split(":");

        Request tokenRequest = Request.cmd(Command.GET);
        tokenRequest.arg(UserAuthDetails.buildTokenKey(subject));
        tokenRequest.arg(UserAuthDetails.buildDetailsKey(keys[0]));
        tokenRequest.arg(UserAuthDetails.buildDeviceKey(keys[1]));

        return Mono.create(sink -> redis.send(tokenRequest).onFailure(sink::error).onSuccess(response -> {
            try {
                MsgUserToken msgUserToken = MsgUserToken.parseFrom(response.get(0).toBytes());
                if (msgUserToken.getToken().equals(token)) {
                    MsgUserDetails msgUserDetails = MsgUserDetails.parseFrom(response.get(1).toBytes());
                    MsgUserDevice msgUserDevice = MsgUserDevice.parseFrom(response.get(2).toBytes());

                    sink.success(new SecurityContextImpl(new UserAuthToken(new UserAuthDetails(msgUserDetails, msgUserDevice), new UserAccessToken(msgUserToken))));
                } else {
                    sink.error(new StandardStatusException(ResponseStatus.token_expired));
                }
            } catch (Exception e) {
                sink.error(e);
            }
        }));
    }
}
