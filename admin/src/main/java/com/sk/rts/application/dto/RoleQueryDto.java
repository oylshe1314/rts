package com.sk.rts.application.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sk.rts.application.validation.NullOrNotBlank;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

@Getter
@Schema(description = "角色查询")
public class RoleQueryDto {

    @NullOrNotBlank
    @Schema(description = "名称")
    private final String name;

    @JsonCreator
    public RoleQueryDto(@JsonProperty("name") String name) {
        this.name = name;
    }
}
