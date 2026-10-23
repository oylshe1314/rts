package com.sk.rts.application.service;

import com.sk.rts.application.dto.OperationRecordDto;
import com.sk.rts.application.dto.OperationRecordQueryDto;
import com.sk.rts.application.dto.PageQueryDto;
import com.sk.rts.application.dto.PageResultDto;
import com.sk.rts.application.entity.AdminOperationRecord;
import com.sk.rts.application.exception.ResponseStatus;
import com.sk.rts.application.exception.StandardStatusException;
import com.sk.rts.application.jooq.Tables;
import com.sk.rts.application.util.TimeUtil;
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

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@NullMarked
@AllArgsConstructor
public class OperationRecordService {

    private final Pool pool;
    private final DSLContext dslContext;

    /**
     * 操作记录分页查询
     *
     * @param pageRequestDtoMono 查询参数
     * @return 分页查询结果
     */
    public Mono<PageResultDto<OperationRecordDto>> query(Mono<PageQueryDto<@Nullable OperationRecordQueryDto>> pageRequestDtoMono) {
        return pageRequestDtoMono.flatMap(pageRequestDto -> {
            OperationRecordQueryDto queryDto = pageRequestDto.getQuery();

            Select<?> pageQuery = dslContext.select(
                            Tables.ADMIN_OPERATION_RECORD.ID,
                            Tables.ADMIN_OPERATION_RECORD.OPERATOR_ID,
                            Tables.ADMIN_OPERATION_RECORD.OPERATOR,
                            Tables.ADMIN_OPERATION_RECORD.OPERATION,
                            Tables.ADMIN_OPERATION_RECORD.ARGUMENTS,
                            Tables.ADMIN_OPERATION_RECORD.REMARK,
                            Tables.ADMIN_OPERATION_RECORD.IP_ADDRESS,
                            Tables.ADMIN_OPERATION_RECORD.CREATE_TIME)
                    .from(Tables.ADMIN_OPERATION_RECORD);
            Select<?> countQuery = dslContext.selectCount().from(Tables.ADMIN_OPERATION_RECORD);

            List<Condition> conditions = new ArrayList<>();
            if (queryDto != null) {
                if (queryDto.getOperatorId() != null) {
                    conditions.add(Tables.ADMIN_OPERATION_RECORD.OPERATOR_ID.eq(queryDto.getOperatorId()));
                }
                if (queryDto.getOperation() != null) {
                    conditions.add(Tables.ADMIN_OPERATION_RECORD.OPERATION.eq(queryDto.getOperation()));
                }
                if (queryDto.getBeginTime() != null) {
                    conditions.add(Tables.ADMIN_OPERATION_RECORD.CREATE_TIME.ge(TimeUtil.toOffsetDateTime(LocalDate.parse(queryDto.getBeginTime(), DateTimeFormatter.ISO_DATE))));
                }
                if (queryDto.getEndTime() != null) {
                    conditions.add(Tables.ADMIN_OPERATION_RECORD.CREATE_TIME.le(TimeUtil.toOffsetDateTime(LocalDate.parse(queryDto.getEndTime(), DateTimeFormatter.ISO_DATE))));
                }
            }

            if (!conditions.isEmpty()) {
                pageQuery = ((SelectWhereStep<?>) pageQuery).where(conditions);
                countQuery = ((SelectWhereStep<?>) countQuery).where(conditions);
            }

            if (pageRequestDto.getSort() == null) {
                pageQuery = ((SelectOrderByStep<?>) pageQuery).orderBy(Tables.ADMIN_OPERATION_RECORD.ID.desc());
            } else {
                OrderField<?> sortField = Tables.ADMIN_OPERATION_RECORD.field(pageRequestDto.getSort());
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
                    .map(rows -> rows.stream().map(row -> new OperationRecordDto(AdminOperationRecord.fromRow(row))).toList())
                    .flatMap(records -> {
                        if (records.size() < pageRequestDto.getPageSize()) {
                            return Future.succeededFuture(new PageResultDto<>(pageRequestDto.getPageNo(), pageRequestDto.getPageSize(), (pageRequestDto.getPageSize().longValue() * (pageRequestDto.getPageNo().longValue() - 1)) + records.size(), records));
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
