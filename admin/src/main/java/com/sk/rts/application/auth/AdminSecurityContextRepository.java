package com.sk.rts.application.auth;

import com.sk.rts.application.component.TokenUtil;
import com.sk.rts.application.config.TokenProperties;
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

import java.util.List;

@Slf4j
@Component
@NullMarked
@AllArgsConstructor
public class AdminSecurityContextRepository implements ServerSecurityContextRepository {

    private final Redis redis;

    private final JsonMapper jsonMapper;

    private final TokenUtil tokenUtil;
    private final TokenProperties tokenProperties;

    @Override
    public Mono<Void> save(ServerWebExchange exchange, @Nullable SecurityContext context) {
        if (context == null) {
            return Mono.error(new StandardStatusException(ResponseStatus.not_logged_in));
        }

        if (context.getAuthentication() instanceof AdminAuthToken authToken && authToken.isAuthenticated()) {
            AdminAuthDetails authDetails = (AdminAuthDetails) authToken.getPrincipal();
            AdminAccessToken accessToken = (AdminAccessToken) authToken.getCredentials();

            Request request1 = Request.cmd(Command.MSETEX);
            request1.arg(2);
            request1.arg(AdminAuthUtil.buildTokenKey(accessToken.getSubject()));
            request1.arg(AdminAuthUtil.buildDetailsKey(authDetails.getAdminId()));
            request1.arg(jsonMapper.writeValueAsString(accessToken));
            request1.arg(jsonMapper.writeValueAsString(authDetails));
            request1.arg("EX");
            request1.arg(tokenProperties.getExpiration().toSeconds());

            Request request2 = Request.cmd(Command.SADD);
            request2.arg(AdminAuthUtil.buildLoggedIdsKey());
            request2.arg(authDetails.getAdminId());

            return ReactiveUtil.toMono(redis.batch(List.of(request1, request2)).mapEmpty());
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
        redisRequest.arg(AdminAuthUtil.buildTokenKey(subject));
        redisRequest.arg(AdminAuthUtil.buildDetailsKey(adminId));

        Future<SecurityContext> future = redis.send(redisRequest).flatMap(response -> {
            if (response.get(0) == null || response.get(1) == null) {
                return Future.failedFuture(new StandardStatusException(ResponseStatus.token_expired));
            }

            AdminAccessToken accessToken = jsonMapper.readValue(response.get(0).toString(), AdminAccessToken.class);
            AdminAuthDetails authDetails = jsonMapper.readValue(response.get(1).toString(), AdminAuthDetails.class);

            AdminAuthToken authResult = new AdminAuthToken(authDetails, accessToken);
            authResult.setDetails(new AdminRemoteDetails(request));

            return Future.succeededFuture(new SecurityContextImpl(authResult));
        });

        return ReactiveUtil.toMono(future).cache();
    }
}
