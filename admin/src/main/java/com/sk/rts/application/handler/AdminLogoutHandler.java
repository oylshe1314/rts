package com.sk.rts.application.handler;

import com.sk.rts.application.auth.AdminAuthToken;
import com.sk.rts.application.auth.AdminAuthDetails;
import com.sk.rts.application.dto.ResponseDto;
import com.sk.rts.application.service.AuthService;
import org.jspecify.annotations.NullMarked;
import org.springframework.http.ResponseCookie;
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
public class AdminLogoutHandler extends CustomRestHandler implements ServerLogoutHandler, ServerLogoutSuccessHandler {

    private final AuthService authService;

    public AdminLogoutHandler(JsonMapper jsonMapper, AuthService authService) {
        super(jsonMapper);
        this.authService = authService;
    }

    @Override
    public Mono<Void> logout(WebFilterExchange exchange, Authentication authentication) {
        if (!(authentication instanceof AdminAuthToken authToken)) {
            return Mono.empty();
        }

        SecurityContextHolder.getContext().setAuthentication(null);
        SecurityContextHolder.clearContext();

        AdminAuthDetails adminAuthDetails = (AdminAuthDetails) authToken.getPrincipal();

        return authService.logout(adminAuthDetails).then(exchange.getExchange().getSession()).flatMap(session -> {
            if (session.isExpired()) {
                return Mono.empty();
            }

            ResponseCookie.ResponseCookieBuilder cookieBuilder = ResponseCookie.from("SESSION", null);
            cookieBuilder.path("/");
            cookieBuilder.httpOnly(true);
            cookieBuilder.maxAge(0);
            cookieBuilder.secure(exchange.getExchange().getRequest().getSslInfo() != null);
            exchange.getExchange().getResponse().addCookie(cookieBuilder.build());

            return session.invalidate();
        });
    }

    @Override
    public Mono<Void> onLogoutSuccess(WebFilterExchange exchange, Authentication authentication) {
        return respond(exchange.getExchange().getResponse(), ResponseDto.success());
    }
}
