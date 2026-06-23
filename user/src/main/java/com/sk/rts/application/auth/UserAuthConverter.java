package com.sk.rts.application.auth;

import com.sk.rts.application.component.ValidationUtil;
import com.sk.rts.application.exception.ResponseStatus;
import com.sk.rts.application.exception.StandardStatusException;
import lombok.AllArgsConstructor;
import org.jspecify.annotations.NullMarked;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.server.authentication.ServerAuthenticationConverter;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import tools.jackson.databind.json.JsonMapper;

@NullMarked
@AllArgsConstructor
public abstract class UserAuthConverter implements ServerAuthenticationConverter {

    protected final JsonMapper jsonMapper;
    protected final ValidationUtil validationUtil;

    @Override
    public Mono<Authentication> convert(ServerWebExchange exchange) {
        ServerHttpRequest request = exchange.getRequest();
        MediaType mediaType = request.getHeaders().getContentType();
        if (!MediaType.APPLICATION_JSON.equals(mediaType)) {
            return Mono.error(new BadCredentialsException("", new StandardStatusException(ResponseStatus.content_type_error)));
        }

        return DataBufferUtils.join(request.getBody()).map(buffer -> {
            UserAuthToken authRequest = parse(buffer);
            authRequest.setDetails(new UserRemoteDetails(request));
            return authRequest;
        });
    }

    protected abstract UserAuthToken parse(DataBuffer buffer);
}
