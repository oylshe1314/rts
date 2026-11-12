package com.sk.rts.application.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

@Getter
@Schema(description = "TOKEN刷新")
public class TokenRefreshDto {

    @NotBlank
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "主题")
    private final String subject;

    @NotBlank
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "TOKEN刷新HASH")
    private final String hash;

    public TokenRefreshDto(@JsonProperty("subject") String subject, @JsonProperty("hash") String hash) {
        this.subject = subject;
        this.hash = hash;
    }
}
