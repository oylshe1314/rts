package com.sk.rts.application.auth;

import com.sk.rts.application.component.ValidationUtil;
import com.sk.rts.application.dto.PasswordLoginDto;
import com.sk.rts.application.exception.ResponseStatus;
import com.sk.rts.application.exception.StandardStatusException;
import org.jspecify.annotations.NullMarked;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

@Component
@NullMarked
public class UserPasswordAuthConverter extends UserAuthConverter {

    public UserPasswordAuthConverter(JsonMapper jsonMapper, ValidationUtil validationUtil) {
        super(jsonMapper, validationUtil);
    }

    @Override
    protected UserAuthToken parse(DataBuffer buffer) {
        try {
            PasswordLoginDto loginDto = jsonMapper.readValue(buffer.asInputStream(true), PasswordLoginDto.class);
            if (validationUtil.validate(loginDto)) {
                return new UserAuthToken(loginDto.getAccount(), loginDto.getPassword());
            } else {
                throw new BadCredentialsException("", new StandardStatusException(ResponseStatus.parameter_error));
            }
        } catch (JacksonException exception) {
            throw new BadCredentialsException("", new StandardStatusException(ResponseStatus.bad_request));
        }
    }
}
