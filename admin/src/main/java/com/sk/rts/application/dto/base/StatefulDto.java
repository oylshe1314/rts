package com.sk.rts.application.dto.base;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

import java.time.OffsetDateTime;

@Getter
public abstract class StatefulDto extends OperationDto {

    @Schema(title = "状态", description = "0.禁用, 1.启用")
    private final Integer state;

    protected StatefulDto(Long id, String remark, String updateBy, OffsetDateTime updateTime, Integer state) {
        super(id, remark, updateBy, updateTime);
        this.state = state;
    }
}
