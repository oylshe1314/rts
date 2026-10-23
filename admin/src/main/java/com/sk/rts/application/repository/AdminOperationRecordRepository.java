package com.sk.rts.application.repository;

import com.sk.rts.application.auth.AdminAuthDetails;
import com.sk.rts.application.entity.AdminOperationRecord;
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
public class AdminOperationRecordRepository {

    private final Pool pool;
    private final DSLContext dslContext;

    public Future<Void> add(Operation operation, String arguments, String remark, AdminAuthDetails operator) {
        AdminOperationRecord record = new AdminOperationRecord();
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
        AdminOperationRecord record = new AdminOperationRecord();
        record.setOperatorId(operator.getAdminId());
        record.setOperator(operator.getUsername());
        record.setOperation(operation.name());
        record.setArguments(arguments);
        record.setRemark(remark);
        record.setIpAddress(operator.getIpAddress());
        record.setCreateTime(OffsetDateTime.now());
        return insert(connection, record).mapEmpty();
    }

    public Future<Long> insert(SqlConnection connection, AdminOperationRecord record) {
        ResultQuery<?> query = dslContext.insertInto(
                        Tables.ADMIN_OPERATION_RECORD,
                        Tables.ADMIN_OPERATION_RECORD.OPERATOR_ID,
                        Tables.ADMIN_OPERATION_RECORD.OPERATOR,
                        Tables.ADMIN_OPERATION_RECORD.OPERATION,
                        Tables.ADMIN_OPERATION_RECORD.ARGUMENTS,
                        Tables.ADMIN_OPERATION_RECORD.REMARK,
                        Tables.ADMIN_OPERATION_RECORD.IP_ADDRESS,
                        Tables.ADMIN_OPERATION_RECORD.CREATE_TIME
                ).values(
                        record.getOperatorId(),
                        record.getOperator(),
                        record.getOperation(),
                        record.getArguments(),
                        record.getRemark(),
                        record.getIpAddress(),
                        record.getCreateTime())
                .returning(Tables.ADMIN_OPERATION_RECORD.ID);
        return connection.preparedQuery(query.getSQL()).execute(Tuple.tuple(query.getBindValues())).map(rows -> rows.iterator().next().getLong(0));
    }
}
