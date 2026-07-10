package com.sk.rts.application.repository;

import com.sk.rts.application.auth.AdminAuthDetails;
import com.sk.rts.application.entity.OperationRecord;
import com.sk.rts.application.entity.enums.Operation;
import com.sk.rts.application.jooq.Tables;
import io.vertx.core.Future;
import io.vertx.sqlclient.Pool;
import io.vertx.sqlclient.SqlConnection;
import io.vertx.sqlclient.Tuple;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jooq.DSLContext;
import org.jooq.ResultQuery;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;

@Slf4j
@Repository
@AllArgsConstructor
public class OperationRecordRepository {

    private final Pool pool;
    private final DSLContext dslContext;

    public Future<Void> add(Operation operation, String arguments, String remark, AdminAuthDetails operator) {
        OperationRecord record = new OperationRecord();
        record.setOperatorId(operator.getAdminId());
        record.setOperator(operator.getUsername());
        record.setOperation(operation.name());
        record.setArguments(arguments);
        record.setRemark(remark);
        record.setIpAddress(operator.getIpAddress());
        record.setCreateTime(OffsetDateTime.now());
        return pool.getConnection().flatMap(connection -> insert(connection, record)).mapEmpty();
    }

    public Future<Void> add(SqlConnection connection, Operation operation, String arguments, String remark, AdminAuthDetails operator) {
        OperationRecord record = new OperationRecord();
        record.setOperatorId(operator.getAdminId());
        record.setOperator(operator.getUsername());
        record.setOperation(operation.name());
        record.setArguments(arguments);
        record.setRemark(remark);
        record.setIpAddress(operator.getIpAddress());
        record.setCreateTime(OffsetDateTime.now());
        return insert(connection, record).mapEmpty();
    }

    public Future<Long> insert(SqlConnection connection, OperationRecord record) {
        ResultQuery<?> query = dslContext.insertInto(
                        Tables.OPERATION_RECORD,
                        Tables.OPERATION_RECORD.OPERATOR_ID,
                        Tables.OPERATION_RECORD.OPERATOR,
                        Tables.OPERATION_RECORD.OPERATION,
                        Tables.OPERATION_RECORD.ARGUMENTS,
                        Tables.OPERATION_RECORD.REMARK,
                        Tables.OPERATION_RECORD.IP_ADDRESS,
                        Tables.OPERATION_RECORD.CREATE_TIME
                ).values(
                        record.getOperatorId(),
                        record.getOperator(),
                        record.getOperation(),
                        record.getArguments(),
                        record.getRemark(),
                        record.getIpAddress(),
                        record.getCreateTime())
                .returning(Tables.OPERATION_RECORD.ID);
        return connection.preparedQuery(query.getSQL()).execute(Tuple.tuple(query.getBindValues())).map(rows -> rows.iterator().next().getLong(0));
    }
}
