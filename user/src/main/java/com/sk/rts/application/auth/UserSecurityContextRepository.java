package com.sk.rts.application.auth;

import com.sk.rts.application.component.builder.RedisKeyBuilder;
import com.sk.rts.application.component.token.TokenUtil;
import com.sk.rts.application.util.ReactiveUtil;
import io.vertx.redis.client.Command;
import io.vertx.redis.client.Redis;
import io.vertx.redis.client.Request;
import lombok.AllArgsConstructor;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.server.context.ServerSecurityContextRepository;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import tools.jackson.databind.json.JsonMapper;

@Component
@NullMarked
@AllArgsConstructor
public class UserSecurityContextRepository implements ServerSecurityContextRepository {

    private final TokenUtil tokenUtil;

    @Override
    public Mono<Void> save(ServerWebExchange exchange, @Nullable SecurityContext context) {
        return Mono.empty();
    }

    @Override
    public Mono<SecurityContext> load(ServerWebExchange exchange) {
        String token = TokenUtil.getTokenFromRequest(exchange.getRequest());
        if (token == null) {
            return Mono.empty();
        }

        UserRemoteDetails remoteDetails = new UserRemoteDetails(exchange);

        UserAccessToken accessToken = new UserAccessToken();
        accessToken.setToken(token);

        AuthDetails authDetails = tokenUtil.verify(accessToken);

        authDetails.setLocale(remoteDetails.getLocale());
        authDetails.setIpAddress(remoteDetails.getIpAddress());

        return Mono.just(new SecurityContextImpl(new UserAuthToken(authDetails, new TokenDetails(accessToken, null))));
    }
}
