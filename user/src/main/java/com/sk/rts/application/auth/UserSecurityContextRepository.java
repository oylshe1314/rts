package com.sk.rts.application.auth;

import com.sk.rts.application.component.TokenUtil;
import com.sk.rts.application.config.TokenProperties;
import com.sk.rts.application.exception.ResponseStatus;
import com.sk.rts.application.exception.StandardStatusException;
import com.sk.rts.application.proto.caching.MsgAccessToken;
import com.sk.rts.application.proto.caching.MsgUserDetails;
import com.sk.rts.application.proto.caching.MsgUserDevice;
import com.sk.rts.application.service.CacheService;
import io.vertx.redis.client.Command;
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

    private final TokenUtil tokenUtil;
    private final TokenProperties tokenProperties;

    private final CacheService cacheService;

    @Override
    public Mono<Void> save(ServerWebExchange exchange, @Nullable SecurityContext context) {
        if (context == null) {
            return Mono.empty();
        }

        if (context.getAuthentication() instanceof UserAuthToken authToken && authToken.isAuthenticated()) {
            UserAuthDetails authDetails = (UserAuthDetails) authToken.getPrincipal();
            UserAccessToken accessToken = (UserAccessToken) authToken.getCredentials();

            return Mono.create(sink -> cacheService.saveUserAuthDetails(accessToken, authDetails, tokenProperties.getAccessToken().getExpiration()).onSuccess(sink::success).onFailure(sink::error));
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

        try {
            String subject = tokenUtil.extractSubject(token);

            UserAuthDetails authDetails = new UserAuthDetails();
            UserAccessToken accessToken = new UserAccessToken();
            accessToken.setToken(subject);
            accessToken.setToken(token);

            return Mono.create(sink -> cacheService.queryUserAuthDetails(accessToken, authDetails)
                    .onFailure(sink::error)
                    .onSuccess(_ -> {
                        authDetails.setLoginIp(new UserRemoteDetails(request).getAddress());
                        sink.success(new SecurityContextImpl( new UserAuthToken(authDetails, accessToken)));
                    })
            );
        } catch (StandardStatusException e) {
            return Mono.error(e);
        }
    }
}
