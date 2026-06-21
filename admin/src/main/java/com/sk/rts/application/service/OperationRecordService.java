package com.sk.rts.application.service;

import com.sk.rts.application.auth.AdminAuthDetails;
import com.sk.rts.application.dto.OperationRecordDto;
import com.sk.rts.application.dto.OperationRecordQueryDto;
import com.sk.rts.application.dto.PageQueryDto;
import com.sk.rts.application.dto.PageResultDto;
import com.sk.rts.application.entity.OperationRecord;
import com.sk.rts.application.jooq.Tables;
import com.sk.rts.application.repository.OperationRecordRepository;
import io.vertx.core.Future;
import io.vertx.sqlclient.Pool;
import io.vertx.sqlclient.Tuple;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.SelectWhereStep;
import org.jspecify.annotations.NullMarked;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

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
    public Mono<PageResultDto<OperationRecordDto>> query(Mono<PageQueryDto<OperationRecordQueryDto>> pageRequestDtoMono) {
        return pageRequestDtoMono.flatMap(pageRequestDto -> {
            OperationRecordQueryDto queryDto = pageRequestDto.getQuery();

            SelectWhereStep<?> pageQuery = dslContext.select(Tables.OPERATION_RECORD.ID, Tables.OPERATION_RECORD.OPERATOR_ID, Tables.OPERATION_RECORD.OPERATOR, Tables.OPERATION_RECORD.OPERATION, Tables.OPERATION_RECORD.ARGUMENTS, Tables.OPERATION_RECORD.REMARK, Tables.OPERATION_RECORD.LOGIN_IP, Tables.OPERATION_RECORD.OPERATE_TIME).from(Tables.OPERATION_RECORD);
            SelectWhereStep<?> countQuery = dslContext.selectCount().from(Tables.OPERATION_RECORD);

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

            if (conditions.size() > 0) {
                pageQuery.where(conditions);
                countQuery.where(conditions);
            }

            if (pageRequestDto.getSort() == null) {
                pageQuery.orderBy(Tables.OPERATION_RECORD.ID.desc());
            } else {
                if (Boolean.TRUE.equals(pageRequestDto.getDesc())) {
                    pageQuery.orderBy(Tables.OPERATION_RECORD.field(pageRequestDto.getSort()).desc());
                } else {
                    pageQuery.orderBy(Tables.OPERATION_RECORD.field(pageRequestDto.getSort()));
                }
            }

            pageQuery.offset(pageRequestDto.getOffset()).limit(pageRequestDto.getPageSize());

            String sql = pageQuery.getSQL();
            log.debug("SQL: {}", sql);
            return Mono.create(sink -> pool.getConnection().flatMap(connection -> connection.preparedQuery(sql).execute(Tuple.tuple(pageQuery.getBindValues()))
                    .map(rows -> rows.stream().map(row -> {
                                OperationRecord record = new OperationRecord();
                                record.setId(row.getLong(0));
                                record.setOperatorId(row.getLong(1));
                                record.setOperator(row.getString(2));
                                record.setOperation(row.getString(3));
                                record.setOperateArgs(row.getString(4));
                                record.setRemark(row.getString(5));
                                record.setLoginIp(row.getString(6));
                                record.setOperateTime(row.getOffsetDateTime(7));

                                return new OperationRecordDto(record);
                            }).toList()
                    )
                    .flatMap(records -> {
                                if (records.size() < pageRequestDto.getPageSize()) {
                                    return Future.succeededFuture(new PageResultDto<>(pageRequestDto.getPageNo(), pageRequestDto.getPageSize(), records.size(), records));
                                }

                                return connection.preparedQuery(countQuery.getSQL()).execute(Tuple.tuple(countQuery.getBindValues()))
                                        .map(rows -> new PageResultDto<>(pageRequestDto.getPageNo(), pageRequestDto.getPageSize(), rows.iterator().next().getLong(0), records));
                            }
                    )
                    .onComplete(ar -> connection.close())
                    .onSuccess(sink::success)
                    .onFailure(sink::error)
            ));
        });
    }

    public Mono<Void> add(String operation, String operateArgs, String remark, AdminAuthDetails operator) {
        return Mono.create(sink -> this.pool.getConnection().onFailure(sink::error).onSuccess(connection -> operationRecordRepository.add(connection, operation, operateArgs, remark, operator).onSuccess(v -> sink.success()).onFailure(sink::error)));
    }
}
