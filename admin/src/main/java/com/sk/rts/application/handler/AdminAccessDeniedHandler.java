package com.sk.rts.application.handler;

import com.sk.rts.application.exception.ResponseStatus;
import com.sk.rts.application.exception.StandardStatusException;
import org.jspecify.annotations.NullMarked;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.server.ServerAuthenticationEntryPoint;
import org.springframework.security.web.server.authorization.ServerAccessDeniedHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import tools.jackson.databind.json.JsonMapper;

@Component
@NullMarked
public class AdminAccessDeniedHandler extends CustomRestHandler implements ServerAuthenticationEntryPoint, ServerAccessDeniedHandler {

    public AdminAccessDeniedHandler(JsonMapper jsonMapper) {
        super(jsonMapper);
    }

    @Override
    public Mono<Void> commence(ServerWebExchange exchange, AuthenticationException ex) {
        return respondException(exchange.getResponse(), new StandardStatusException(ResponseStatus.not_logged_in));
    }

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, AccessDeniedException denied) {
        return respondException(exchange.getResponse(), new StandardStatusException(ResponseStatus.access_denied));
    }
}
