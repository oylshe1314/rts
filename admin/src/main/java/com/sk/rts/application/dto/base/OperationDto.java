package com.sk.rts.application.dto.base;

import com.sk.rts.application.validation.NullOrNotBlank;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

import java.time.OffsetDateTime;

@Getter
public abstract class OperationDto extends BaseDto {

    @NullOrNotBlank
    @Schema(title = "备注")
    private final String remark;

    @Schema(title = "操作员")
    private final String updateBy;

    @Schema(title = "操作时间")
    private final OffsetDateTime updateTime;

    protected OperationDto(Long id, String remark, String updateBy, OffsetDateTime updateTime) {
        super(id);
        this.remark = remark;
        this.updateBy = updateBy;
        this.updateTime = updateTime;
    }
}
