package com.sk.rts.application.dto;

import com.sk.rts.application.dto.base.BaseDto;
import com.sk.rts.application.entity.OperationRecord;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

import java.time.OffsetDateTime;

@Getter
@Schema(description = "操作记录")
public class OperationRecordDto extends BaseDto {

    @Schema(description = "操作员ID")
    private final Long operatorId;

    @Schema(description = "操作员用户名")
    private final String operator;

    @Schema(description = "操作, login, logout, add, update, delete, changeState等")
    private final String operation;

    @Schema(description = "操作参数，表名，登录账号等")
    private final String operateArgs;

    @Schema(description = "备注")
    private final String remark;

    @Schema(description = "操作员登录IP")
    private final String ipAddress;

    @Schema(description = "操作时间")
    private final OffsetDateTime operateTime;

    public OperationRecordDto(Long id, Long operatorId, String operator, String operation, String operateArgs, String remark, String ipAddress, OffsetDateTime operateTime) {
        super(id);
        this.operatorId = operatorId;
        this.operator = operator;
        this.operation = operation;
        this.operateArgs = operateArgs;
        this.remark = remark;
        this.ipAddress = ipAddress;
        this.operateTime = operateTime;
    }

    public OperationRecordDto(OperationRecord record) {
        super(record.getId());
        this.operatorId = record.getOperatorId();
        this.operator = record.getOperator();
        this.operation = record.getOperation();
        this.operateArgs = record.getArguments();
        this.remark = record.getRemark();
        this.ipAddress = record.getIpAddress();
        this.operateTime = record.getCreateTime();
    }
}
