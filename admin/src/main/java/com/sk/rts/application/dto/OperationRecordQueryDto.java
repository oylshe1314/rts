package com.sk.rts.application.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

@Getter
@Schema(description = "操作记录查询")
public class OperationRecordQueryDto {

    @Schema(description = "操作人ID")
    private final Long operatorId;

    @Schema(description = "操作人")
    private final String operator;

    @Schema(description = "操作")
    private final String operation;

    @JsonCreator
    public OperationRecordQueryDto(@JsonProperty("operatorId") Long operatorId,
                                   @JsonProperty("operator") String operator,
                                   @JsonProperty("operation") String operation) {
        this.operatorId = operatorId;
        this.operator = operator;
        this.operation = operation;
    }
}
