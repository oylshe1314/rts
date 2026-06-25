package com.sk.rts.application.repository;

import com.sk.rts.application.entity.UserDevice;
import com.sk.rts.application.jooq.Tables;
import io.vertx.core.Future;
import io.vertx.sqlclient.SqlConnection;
import io.vertx.sqlclient.Tuple;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jooq.DSLContext;
import org.jooq.InsertResultStep;
import org.jooq.ResultQuery;
import org.springframework.stereotype.Repository;

@Slf4j
@Repository
@AllArgsConstructor
public class UserDeviceRepository {

    private final DSLContext dslContext;

    public Future<Long> insert(SqlConnection connection, UserDevice device) {
        ResultQuery<?> query = dslContext.insertInto(
                        Tables.USER_DEVICE,
                        Tables.USER_DEVICE.USER_ID,
                        Tables.USER_DEVICE.DEVICE_NO,
                        Tables.USER_DEVICE.PLATFORM,
                        Tables.USER_DEVICE.SERIAL_NO,
                        Tables.USER_DEVICE.CREATE_TIME
                )
                .values(
                        device.getUserId(),
                        device.getDeviceNo(),
                        device.getPlatform(),
                        device.getSerialNo(),
                        device.getCreateTime()
                )
                .returning(Tables.USER_DEVICE.ID);
        return connection.preparedQuery(query.getSQL()).execute(Tuple.tuple(query.getBindValues())).map(rows -> rows.iterator().next().getLong(0));
    }
}
