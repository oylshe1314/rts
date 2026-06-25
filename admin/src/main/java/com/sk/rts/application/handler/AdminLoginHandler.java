package com.sk.rts.application.handler;

import com.sk.rts.application.auth.AdminAccessToken;
import com.sk.rts.application.auth.AdminAuthToken;
import com.sk.rts.application.dto.AccessTokenDto;
import com.sk.rts.application.dto.ResponseDto;
import com.sk.rts.application.exception.ResponseStatus;
import com.sk.rts.application.exception.StandardStatusException;
import com.sk.rts.application.strategy.LoginConflictStrategy;
import org.jspecify.annotations.NullMarked;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.server.WebFilterExchange;
import org.springframework.security.web.server.authentication.ServerAuthenticationFailureHandler;
import org.springframework.security.web.server.authentication.ServerAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import tools.jackson.databind.json.JsonMapper;

@Component
@NullMarked
public class AdminLoginHandler extends CustomRestHandler implements ServerAuthenticationSuccessHandler, ServerAuthenticationFailureHandler {

    public AdminLoginHandler(JsonMapper jsonMapper) {
        super(jsonMapper);
    }

    @Override
    public Mono<Void> onAuthenticationSuccess(WebFilterExchange exchange, Authentication authentication) {
        if (authentication instanceof AdminAuthToken authToken) {
            return respond(exchange.getExchange().getResponse(), ResponseDto.success(new AccessTokenDto((AdminAccessToken) authToken.getCredentials())));
        } else {
            return respondException(exchange.getExchange().getResponse(), new StandardStatusException(ResponseStatus.internal_error));
        }
    }

    @Override
    public Mono<Void> onAuthenticationFailure(WebFilterExchange exchange, AuthenticationException exception) {
        return respondException(exchange.getExchange().getResponse(), exception);
    }
}
