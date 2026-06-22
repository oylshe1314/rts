package com.sk.rts.application.auth;

import com.sk.rts.application.component.ValidationUtil;
import com.sk.rts.application.dto.PasswordLoginDto;
import com.sk.rts.application.exception.ResponseStatus;
import com.sk.rts.application.exception.StandardStatusException;
import lombok.AllArgsConstructor;
import org.jspecify.annotations.NullMarked;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.server.authentication.ServerAuthenticationConverter;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

@Component
@NullMarked
@AllArgsConstructor
public class UserAuthConverter implements ServerAuthenticationConverter {

    private final JsonMapper jsonMapper;
    private final ValidationUtil validationUtil;

    @Override
    public Mono<Authentication> convert(ServerWebExchange exchange) {
        ServerHttpRequest request = exchange.getRequest();
        MediaType mediaType = request.getHeaders().getContentType();
        if (!MediaType.APPLICATION_JSON.equals(mediaType)) {
            return Mono.error(new BadCredentialsException("", new StandardStatusException(ResponseStatus.content_type_error)));
        }

        return DataBufferUtils.join(request.getBody()).handle(((buffer, sink) -> {
            try {
                PasswordLoginDto loginDto = jsonMapper.readValue(buffer.asInputStream(true), PasswordLoginDto.class);
                if (validationUtil.validate(loginDto)) {
                    UserAuthToken authRequest = new UserAuthToken(loginDto.getAccount(), loginDto.getPassword());
                    authRequest.setDetails(new UserRemoteDetails(request));
                    sink.next(authRequest);
                } else {
                    sink.error(new BadCredentialsException("", new StandardStatusException(ResponseStatus.parameter_error)));
                }
            } catch (JacksonException exception) {
                sink.error(new BadCredentialsException("", new StandardStatusException(ResponseStatus.bad_request)));
            }
        }));
    }
}
