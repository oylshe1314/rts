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
    public Mono<PageResultDto<OperationRecordDto>> query(Mono<PageQueryDto<OperationRecordQueryDto>> pageRequestDtoMono) {
        return pageRequestDtoMono.flatMap(pageRequestDto -> {
            OperationRecordQueryDto queryDto = pageRequestDto.getQuery();

            SelectWhereStep<?> pageQuery = dslContext.select(Tables.OPERATION_RECORD.ID, Tables.OPERATION_RECORD.OPERATOR_ID, Tables.OPERATION_RECORD.OPERATOR, Tables.OPERATION_RECORD.OPERATION, Tables.OPERATION_RECORD.ARGUMENTS, Tables.OPERATION_RECORD.REMARK, Tables.OPERATION_RECORD.LOGIN_IP, Tables.OPERATION_RECORD.CREATE_TIME).from(Tables.OPERATION_RECORD);
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
                    .map(rows -> rows.stream().map(row -> new OperationRecordDto(OperationRecord.fromRow(row))).toList())
                    .flatMap(records -> {
                        if (records.size() < pageRequestDto.getPageSize()) {
                            return Future.succeededFuture(new PageResultDto<>(pageRequestDto.getPageNo(), pageRequestDto.getPageSize(), records.size(), records));
                        }

                        return connection.preparedQuery(countQuery.getSQL()).execute(Tuple.tuple(countQuery.getBindValues())).map(rows -> new PageResultDto<>(pageRequestDto.getPageNo(), pageRequestDto.getPageSize(), rows.iterator().next().getLong(0), records));
                    })
                    .onComplete(_ -> connection.close())
                    .onSuccess(sink::success)
                    .onFailure(sink::error)
            ));
        });
    }

    public Mono<Void> add(String operation, String arguments, String remark, AdminAuthDetails operator) {
        OperationRecord record = new OperationRecord();
        record.setOperatorId(operator.getAdminId());
        record.setOperator(operator.getUsername());
        record.setOperation(operation);
        record.setArguments(arguments);
        record.setRemark(remark);
        record.setLoginIp(operator.getAdminDetails().getLoginIp());
        record.setCreateTime(OffsetDateTime.now());
        return Mono.create(sink -> this.pool.getConnection().map(connection -> operationRecordRepository.insert(connection, record)
                .onComplete(_ -> connection.close())
                .onSuccess(_ -> sink.success())
                .onFailure(sink::error)
        ));
    }
}
