package com.sk.rts.application.dto.base;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

@Getter
@Schema(description = "ID")
public class BaseDto {

    @NotNull
    @Schema(title = "ID")
    private final Long id;

    protected BaseDto(Long id) {
        this.id = id;
    }
}
