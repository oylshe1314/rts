package com.sk.rts.application.handler;

import com.sk.rts.application.auth.UserAuthToken;
import com.sk.rts.application.auth.UserTokenDetails;
import com.sk.rts.application.dto.ResponseDto;
import com.sk.rts.application.dto.UserTokenDto;
import com.sk.rts.application.exception.ResponseStatus;
import com.sk.rts.application.exception.StandardStatusException;
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
public class UserLoginHandler extends CustomRestHandler implements ServerAuthenticationSuccessHandler, ServerAuthenticationFailureHandler {

    public UserLoginHandler(JsonMapper jsonMapper) {
        super(jsonMapper);
    }

    @Override
    public Mono<Void> onAuthenticationSuccess(WebFilterExchange exchange, Authentication authentication) {
        if (authentication instanceof UserAuthToken authToken) {
            UserTokenDetails tokenDetails = (UserTokenDetails) authToken.getDetails();
            if (tokenDetails != null) {
                return respond(exchange.getExchange().getResponse(), ResponseDto.success(new UserTokenDto(tokenDetails)));
            } else {
                return respondException(exchange.getExchange().getResponse(), new StandardStatusException(ResponseStatus.internal_error));
            }
        } else {
            return respondException(exchange.getExchange().getResponse(), new StandardStatusException(ResponseStatus.internal_error));
        }
    }

    @Override
    public Mono<Void> onAuthenticationFailure(WebFilterExchange exchange, AuthenticationException exception) {
        return respondException(exchange.getExchange().getResponse(), exception);
    }
}
