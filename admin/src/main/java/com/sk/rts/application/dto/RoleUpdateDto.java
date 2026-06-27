package com.sk.rts.application.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.sk.rts.application.dto.base.BaseDto;
import com.sk.rts.application.validation.NullOrNotBlank;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

@Getter
@Schema(description = "角色修改")
public class RoleUpdateDto extends BaseDto {

    @NullOrNotBlank
    @Schema(description = "名称")
    private final String name;

    @NullOrNotBlank
    @Schema(description = "代码")
    private final String code;

    @Schema(description = "备注")
    private final String remark;

    public RoleUpdateDto(@JsonProperty("id") Long id,
                         @JsonProperty("name") String name,
                         @JsonProperty("code") String code,
                         @JsonProperty("remark") String remark) {
        super(id);
        this.name = name;
        this.code = code;
        this.remark = remark;
    }
}
