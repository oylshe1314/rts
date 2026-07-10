package com.sk.rts.application.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sk.rts.application.validation.Date;
import com.sk.rts.application.validation.NullOrNotBlank;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import org.springframework.format.annotation.DateTimeFormat;

@Getter
@Schema(description = "操作记录查询")
public class OperationRecordQueryDto {

    @Positive
    @Schema(description = "操作人ID")
    private final Long operatorId;

    @NullOrNotBlank
    @Schema(description = "操作")
    private final String operation;

    @Date
    @Schema(description = "开始时间")
    private final String beginTime;

    @Date
    @Schema(description = "结束时间")
    private final String endTime;

    @JsonCreator
    public OperationRecordQueryDto(@JsonProperty("operatorId") Long operatorId,
                                   @JsonProperty("operation") String operation,
                                   @JsonProperty("beginTime") String beginTime,
                                   @JsonProperty("endTime") String endTime) {
        this.operatorId = operatorId;
        this.operation = operation;
        this.beginTime = beginTime;
        this.endTime = endTime;
    }
}
