package com.sk.rts.application.service;

import com.sk.rts.application.auth.AdminAuthDetails;
import com.sk.rts.application.dto.OperationRecordDto;
import com.sk.rts.application.dto.OperationRecordQueryDto;
import com.sk.rts.application.dto.PageQueryDto;
import com.sk.rts.application.dto.PageResultDto;
import com.sk.rts.application.entity.OperationRecord;
import com.sk.rts.application.exception.ResponseStatus;
import com.sk.rts.application.exception.StandardStatusException;
import com.sk.rts.application.jooq.Tables;
import com.sk.rts.application.repository.OperationRecordRepository;
import io.vertx.core.Future;
import io.vertx.sqlclient.Pool;
import io.vertx.sqlclient.Tuple;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jooq.*;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@NullMarked
@AllArgsConstructor
public class OperationRecordService {

    private final Pool pool;
    private final DSLContext dslContext;

    private final OperationRecordRepository operationRecordRepository;

    /**
     * 操作记录分页查询
     *
     * @param pageRequestDtoMono 查询参数
     * @return 分页查询结果
     */
    public Mono<PageResultDto<OperationRecordDto>> query(Mono<PageQueryDto<@Nullable OperationRecordQueryDto>> pageRequestDtoMono) {
        return pageRequestDtoMono.flatMap(pageRequestDto -> {
            OperationRecordQueryDto queryDto = pageRequestDto.getQuery();

            Select<?> pageQuery = dslContext.select(Tables.OPERATION_RECORD.ID, Tables.OPERATION_RECORD.OPERATOR_ID, Tables.OPERATION_RECORD.OPERATOR, Tables.OPERATION_RECORD.OPERATION, Tables.OPERATION_RECORD.ARGUMENTS, Tables.OPERATION_RECORD.REMARK, Tables.OPERATION_RECORD.LOGIN_IP, Tables.OPERATION_RECORD.CREATE_TIME).from(Tables.OPERATION_RECORD);
            Select<?> countQuery = dslContext.selectCount().from(Tables.OPERATION_RECORD);

            List<Condition> conditions = new ArrayList<>();
            if (queryDto != null) {
                if (queryDto.getOperatorId() != null) {
                    conditions.add(Tables.OPERATION_RECORD.OPERATOR_ID.eq(queryDto.getOperatorId()));
                }
                if (queryDto.getOperator() != null) {
                    conditions.add(Tables.OPERATION_RECORD.OPERATOR.like("%" + queryDto.getOperator() + "%"));
                }
                if (queryDto.getOperation() != null) {
                    conditions.add(Tables.OPERATION_RECORD.OPERATION.eq(queryDto.getOperation()));
                }
            }

            if (!conditions.isEmpty()) {
                pageQuery = ((SelectWhereStep<?>) pageQuery).where(conditions);
                countQuery = ((SelectWhereStep<?>) countQuery).where(conditions);
            }

            if (pageRequestDto.getSort() == null) {
                pageQuery = ((SelectOrderByStep<?>) pageQuery).orderBy(Tables.OPERATION_RECORD.ID.asc());
            } else {
                OrderField<?> sortField = Tables.OPERATION_RECORD.field(pageRequestDto.getSort());
                if (sortField == null) {
                    return Mono.error(new StandardStatusException(ResponseStatus.parameter_error));
                }

                if (Boolean.TRUE.equals(pageRequestDto.getDesc())) {
                    sortField = ((Field<?>) sortField).desc();
                }

                pageQuery = ((SelectOrderByStep<?>) pageQuery).orderBy(sortField);
            }

            pageQuery = ((SelectLimitStep<?>) pageQuery).offset(pageRequestDto.getOffset()).limit(pageRequestDto.getPageSize());

            String pageSql = pageQuery.getSQL();
            String countSql = countQuery.getSQL();
            List<Object> pageArgs = pageQuery.getBindValues();
            List<Object> countArgs = countQuery.getBindValues();
            return Mono.create(sink -> pool.getConnection().flatMap(connection -> connection.preparedQuery(pageSql).execute(Tuple.tuple(pageArgs))
                    .map(rows -> rows.stream().map(row -> new OperationRecordDto(OperationRecord.fromRow(row))).toList())
                    .flatMap(records -> {
                        if (records.size() < pageRequestDto.getPageSize()) {
                            return Future.succeededFuture(new PageResultDto<>(pageRequestDto.getPageNo(), pageRequestDto.getPageSize(), records.size(), records));
                        }

                        return connection.preparedQuery(countSql).execute(Tuple.tuple(countArgs)).map(rows -> new PageResultDto<>(pageRequestDto.getPageNo(), pageRequestDto.getPageSize(), rows.iterator().next().getLong(0), records));
                    })
                    .onComplete(_ -> connection.close())
                    .onSuccess(sink::success)
                    .onFailure(sink::error)
            ));
        });
    }
}
