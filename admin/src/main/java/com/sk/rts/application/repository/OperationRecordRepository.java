package com.sk.rts.application.repository;

import com.sk.rts.application.auth.AdminAuthDetails;
import com.sk.rts.application.jooq.Tables;
import com.sk.rts.application.proto.caching.MsgAdminDetails;
import io.vertx.core.Future;
import io.vertx.sqlclient.SqlConnection;
import io.vertx.sqlclient.Tuple;
import lombok.AllArgsConstructor;
import org.jooq.DSLContext;
import org.jooq.InsertReturningStep;
import org.jspecify.annotations.NullMarked;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;

@Repository
@NullMarked
@AllArgsConstructor
public class OperationRecordRepository {

    private final DSLContext dslContext;

    public Future<Void> add(SqlConnection connection, String operation, String operateArgs, String remark, AdminAuthDetails operator) {
        InsertReturningStep<?> query = dslContext.insertInto(
                Tables.OPERATION_RECORD,
                Tables.OPERATION_RECORD.OPERATOR_ID,
                Tables.OPERATION_RECORD.OPERATOR,
                Tables.OPERATION_RECORD.OPERATION,
                Tables.OPERATION_RECORD.ARGUMENTS,
                Tables.OPERATION_RECORD.REMARK,
                Tables.OPERATION_RECORD.LOGIN_IP,
                Tables.OPERATION_RECORD.OPERATE_TIME
        ).values(operator.getAdminId(), operator.getUsername(), operation, operateArgs, remark, operator.getAdminDetails().getLoginIp(), OffsetDateTime.now());
        return connection.preparedQuery(query.getSQL()).execute(Tuple.tuple(query.getBindValues())).flatMap(rows -> Future.succeededFuture());
    }
}
