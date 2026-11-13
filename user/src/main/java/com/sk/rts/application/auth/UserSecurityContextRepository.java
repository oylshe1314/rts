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

    private final Redis redis;

    private final TokenUtil tokenUtil;

    private final JsonMapper jsonMapper;

    private final RedisKeyBuilder redisKeyBuilder;
    private final PasswordEncoder passwordEncoder;

    @Override
    public Mono<Void> save(ServerWebExchange exchange, @Nullable SecurityContext context) {
        if (context != null && context.getAuthentication() instanceof UserAuthToken authToken && authToken.isAuthenticated()) {
            TokenDetails tokenDetails = (TokenDetails) authToken.getCredentials();
            UserRefreshToken refreshToken = tokenDetails.getRefreshToken();
            if (refreshToken != null) {
                String hash = refreshToken.getHash();
                refreshToken.setHash(passwordEncoder.encode(hash));

                Request request = Request.cmd(Command.MSETEX);
                request.arg(1);

                request.arg(redisKeyBuilder.build("refreshToken", refreshToken.getSubject()));
                request.arg(jsonMapper.writeValueAsString(refreshToken));

                request.arg("EXAT");
                request.arg(refreshToken.getExpireTime());

                refreshToken.setHash(hash);

                return ReactiveUtil.toMono(redis.send(request).mapEmpty());
            }
        }
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
