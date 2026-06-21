package com.sk.rts.application.controller;

import com.sk.rts.application.dto.*;
import com.sk.rts.application.service.OperationRecordService;
import com.sk.rts.application.validation.NullOrNotBlank;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.validator.constraints.Range;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@Slf4j
@Validated
@NullMarked
@RestController
@AllArgsConstructor
@RequestMapping("/operation/record")
@Tag(name = "operationRecord", description = "操作记录相关接口")
public class OperationRecordController {

    private final OperationRecordService operationRecordService;

    @Operation(summary = "操作记录查询", parameters = {
            @Parameter(name = "pageNo", required = true, description = "页码(从1开始)"),
            @Parameter(name = "pageSize", required = true, description = "单页数目(10-100)"),
            @Parameter(name = "sort", description = "排序字段"),
            @Parameter(name = "desc", description = "是否降序"),
            @Parameter(name = "operatorId", description = "查询参数 - 操作员ID", schema = @Schema(implementation = Integer.class)),
            @Parameter(name = "operatorUsername", description = "查询参数 - 操作员用户名", schema = @Schema(implementation = String.class)),
            @Parameter(name = "operation", description = "查询参数 - 操作", schema = @Schema(implementation = String.class)),
    })
    @RequestMapping(value = "/query", method = RequestMethod.GET)
    public Mono<ResponseDto<PageResultDto<OperationRecordDto>>> query(@NotNull @Positive Integer pageNo,
                                                                      @NotNull @Range(min = 10, max = 100) Integer pageSize,
                                                                      @Nullable @NullOrNotBlank String sort,
                                                                      @Nullable Boolean desc,
                                                                      @Parameter(hidden = true) @Nullable @Valid OperationRecordQueryDto queryDto) {
        return operationRecordService.query(Mono.just(new PageQueryDto<>(pageNo, pageSize, sort, desc, queryDto))).map(ResponseDto::success);
    }

    @Operation(summary = "操作记录查询")
    @RequestMapping(value = "/query", method = RequestMethod.POST)
    public Mono<ResponseDto<PageResultDto<OperationRecordDto>>> query(@RequestBody @Valid Mono<PageQueryDto<OperationRecordQueryDto>> pageRequestDtoMono) {
        return operationRecordService.query(pageRequestDtoMono).map(ResponseDto::success);
    }
}
