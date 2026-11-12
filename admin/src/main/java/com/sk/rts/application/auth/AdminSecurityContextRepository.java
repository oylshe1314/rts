package com.sk.rts.application.auth;

import com.sk.rts.application.component.TokenUtil;
import com.sk.rts.application.component.builder.RedisKeyBuilder;
import com.sk.rts.application.exception.ResponseStatus;
import com.sk.rts.application.exception.StandardStatusException;
import com.sk.rts.application.util.ReactiveUtil;
import io.vertx.core.Future;
import io.vertx.redis.client.Command;
import io.vertx.redis.client.Redis;
import io.vertx.redis.client.Request;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.web.server.context.ServerSecurityContextRepository;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import tools.jackson.databind.json.JsonMapper;

@Slf4j
@Component
@NullMarked
@AllArgsConstructor
public class AdminSecurityContextRepository implements ServerSecurityContextRepository {

    private final Redis redis;

    private final TokenUtil tokenUtil;

    private final JsonMapper jsonMapper;

    private final RedisKeyBuilder redisKeyBuilder;

    @Override
    public Mono<Void> save(ServerWebExchange exchange, @Nullable SecurityContext context) {
        if (context == null) {
            return Mono.error(new StandardStatusException(ResponseStatus.not_logged_in));
        }

        if (context.getAuthentication() instanceof AdminAuthToken authToken && authToken.isAuthenticated()) {
            AdminAuthDetails authDetails = (AdminAuthDetails) authToken.getPrincipal();
            AdminAccessToken accessToken = (AdminAccessToken) authToken.getCredentials();

            Request request = Request.cmd(Command.MSETEX);
            request.arg(2);

            request.arg(redisKeyBuilder.build("authDetails", authDetails.getAdminId()));
            request.arg(jsonMapper.writeValueAsString(authDetails));

            request.arg(redisKeyBuilder.build("accessToken", accessToken.getSubject()));
            request.arg(jsonMapper.writeValueAsString(accessToken));

            request.arg("EXAT");
            request.arg(accessToken.getExpireTime());

            return ReactiveUtil.toMono(redis.send(request).mapEmpty());
        } else {
            return Mono.empty();
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
        long adminId = AdminAuthUtil.parseSubject(subject);

        Request redisRequest = Request.cmd(Command.MGET);
        redisRequest.arg(redisKeyBuilder.build("authDetails", adminId));
        redisRequest.arg(redisKeyBuilder.build("accessToken", subject));

        Future<SecurityContext> future = redis.send(redisRequest).flatMap(response -> {
            if (response.get(0) == null || response.get(1) == null) {
                return Future.failedFuture(new StandardStatusException(ResponseStatus.token_expired));
            }

            AdminAuthDetails authDetails = jsonMapper.readValue(response.get(1).toString(), AdminAuthDetails.class);
            AdminAccessToken accessToken = jsonMapper.readValue(response.get(2).toString(), AdminAccessToken.class);

            if (!accessToken.getToken().equals(token)) {
                return Future.failedFuture(new StandardStatusException(ResponseStatus.token_expired));
            }

            AdminAuthToken authResult = new AdminAuthToken(authDetails, accessToken);
            authResult.setDetails(new AdminRemoteDetails(request));

            return Future.succeededFuture(new SecurityContextImpl(authResult));
        });

        return ReactiveUtil.toMono(future).cache();
    }
}
