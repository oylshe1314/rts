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
import org.springframework.stereotype.Repository;

@Slf4j
@Repository
@AllArgsConstructor
public class UserDeviceRepository {

    private final DSLContext dslContext;

    public Future<Long> insert(SqlConnection connection, UserDevice device) {
        InsertResultStep<?> query = dslContext.insertInto(
                        Tables.USER_DEVICE,
                        Tables.USER_DEVICE.USER_ID,
                        Tables.USER_DEVICE.DEVICE_NO,
                        Tables.USER_DEVICE.PLATFORM,
                        Tables.USER_DEVICE.SERIAL_NO,
                        Tables.USER_DEVICE.CHANNEL,
                        Tables.USER_DEVICE.CALLER,
                        Tables.USER_DEVICE.VERSION,
                        Tables.USER_DEVICE.CREATE_TIME
                )
                .values(
                        device.getUserId(),
                        device.getDeviceNo(),
                        device.getPlatform(),
                        device.getSerialNo(),
                        device.getChannel(),
                        device.getCaller(),
                        device.getVersion(),
                        device.getCreateTime()
                )
                .returning(Tables.USER_DEVICE.ID);
        String sql = query.getSQL();
        log.debug("SQL: {}", sql);
        return connection.preparedQuery(sql).execute(Tuple.tuple(query.getBindValues())).map(rows -> rows.iterator().next().getLong(0));
    }
}
