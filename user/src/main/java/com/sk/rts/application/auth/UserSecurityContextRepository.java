package com.sk.rts.application.auth;

import com.sk.rts.application.component.token.TokenUtil;
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

    private final TokenUtil tokenUtil;

    @Override
    public Mono<Void> save(ServerWebExchange exchange, @Nullable SecurityContext context) {
        return Mono.empty();
    }

    @Override
    public Mono<SecurityContext> load(ServerWebExchange exchange) {
        UserRemoteDetails remoteDetails = new UserRemoteDetails(exchange);

        String token = TokenUtil.getTokenFromRequest(exchange.getRequest());

        UserAuthToken authResult;
        if (token != null) {
            UserAccessToken accessToken = new UserAccessToken();
            accessToken.setToken(token);

            AuthDetails authDetails = tokenUtil.verify(accessToken);
            authDetails.setLocale(remoteDetails.getLocale());
            authDetails.setIpAddress(remoteDetails.getIpAddress());

            authResult = new UserAuthToken(authDetails, new TokenDetails(accessToken, null));
        } else {
            authResult = new UserAuthToken("anonymousUser", "");
            authResult.setDetails(remoteDetails);
        }

        return Mono.<SecurityContext>just(new SecurityContextImpl(authResult)).cache();
    }
}
