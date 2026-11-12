package com.sk.rts.application.auth;

import com.sk.rts.application.component.validation.ValidationUtil;
import com.sk.rts.application.dto.CaptchaLoginDto;
import com.sk.rts.application.exception.ResponseStatus;
import com.sk.rts.application.exception.StandardStatusException;
import lombok.AllArgsConstructor;
import org.jspecify.annotations.NullMarked;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

@Component
@NullMarked
@AllArgsConstructor
public class UserCaptchaAuthConverter extends UserAuthConverter {

    private final JsonMapper jsonMapper;
    private final ValidationUtil validationUtil;

    @Override
    protected UserAuthToken parse(DataBuffer buffer) {
        try {
            CaptchaLoginDto loginDto = jsonMapper.readValue(buffer.asInputStream(true), CaptchaLoginDto.class);
            if (validationUtil.validate(loginDto)) {
                return new UserAuthToken(loginDto.getAccount(), loginDto.getCaptcha());
            } else {
                throw new BadCredentialsException("", new StandardStatusException(ResponseStatus.parameter_error));
            }
        } catch (JacksonException exception) {
            throw new BadCredentialsException("", new StandardStatusException(ResponseStatus.bad_request));
        }
    }
}
