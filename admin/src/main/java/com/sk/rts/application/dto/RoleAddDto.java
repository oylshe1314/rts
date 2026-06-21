package com.sk.rts.application.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

@Getter
@Schema(description = "角色添加")
public class RoleAddDto {

    @NotBlank
    @Schema(description = "名称")
    private final String name;

    @Schema(description = "备注")
    private final String remark;

    public RoleAddDto(@JsonProperty("name") String name,
                      @JsonProperty("remark") String remark) {
        this.name = name;
        this.remark = remark;
    }
}
