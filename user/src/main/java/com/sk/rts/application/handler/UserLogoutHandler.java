package com.sk.rts.application.handler;

import com.sk.rts.application.auth.UserAuthDetails;
import com.sk.rts.application.auth.UserAuthToken;
import com.sk.rts.application.dto.ResponseDto;
import com.sk.rts.application.service.AuthService;
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
            return authService.logout((UserAuthDetails) authToken.getPrincipal()).doOnSuccess(_ -> {
                SecurityContextHolder.getContext().setAuthentication(null);
                SecurityContextHolder.clearContext();
            });
        } else {
            return Mono.empty();
        }
    }

    @Override
    public Mono<Void> onLogoutSuccess(WebFilterExchange exchange, Authentication authentication) {
        return respond(exchange.getExchange().getResponse(), ResponseDto.success());
    }
}
