package com.sk.rts.application.handler;

import com.sk.rts.application.auth.AuthDetails;
import com.sk.rts.application.auth.UserAccessToken;
import com.sk.rts.application.auth.UserAuthToken;
import com.sk.rts.application.dto.ResponseDto;
import com.sk.rts.application.service.AuthService;
import com.sk.rts.application.util.ReactiveUtil;
import io.vertx.core.Future;
import org.jspecify.annotations.NullMarked;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.server.WebFilterExchange;
import org.springframework.security.web.server.authentication.logout.ServerLogoutHandler;
import org.springframework.security.web.server.authentication.logout.ServerLogoutSuccessHandler;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import tools.jackson.databind.json.JsonMapper;


@Component
@NullMarked
public class UserLogoutHandler extends CustomRestHandler implements ServerLogoutHandler, ServerLogoutSuccessHandler {

    private final AuthService authService;

    public UserLogoutHandler(JsonMapper jsonMapper, AuthService authService) {
        super(jsonMapper);
        this.authService = authService;
    }

    @Override
    public Mono<Void> logout(WebFilterExchange exchange, Authentication authentication) {
        if (authentication instanceof UserAuthToken authToken) {
            Future<Void> future = authService.logout((AuthDetails) authToken.getPrincipal(), (UserAccessToken) authToken.getCredentials());
            future.onSuccess(_ -> {
                SecurityContextHolder.getContext().setAuthentication(null);
                SecurityContextHolder.clearContext();
            });
            return ReactiveUtil.toMono(future);
        } else {
            return Mono.empty();
        }
    }

    @Override
    public Mono<Void> onLogoutSuccess(WebFilterExchange exchange, Authentication authentication) {
        return respond(exchange.getExchange().getResponse(), ResponseDto.success());
    }
}
