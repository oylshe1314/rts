package com.sk.rts.application.repository;

import com.sk.rts.application.entity.UserToken;
import com.sk.rts.application.jooq.Tables;
import io.vertx.core.Future;
import io.vertx.sqlclient.SqlConnection;
import io.vertx.sqlclient.Tuple;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jooq.DSLContext;
import org.jooq.DeleteConditionStep;
import org.jooq.InsertResultStep;
import org.springframework.stereotype.Repository;

@Slf4j
@Repository
@AllArgsConstructor
public class UserTokenRepository {

    private final DSLContext dslContext;

    public Future<Long> insert(SqlConnection connection, UserToken token) {
        InsertResultStep<?> query = dslContext.insertInto(
                Tables.USER_TOKEN,
                Tables.USER_TOKEN.USER_ID,
                Tables.USER_TOKEN.DEVICE_ID,
                Tables.USER_TOKEN.HASH,
                Tables.USER_TOKEN.STATUS,
                Tables.USER_TOKEN.ISSUE_TIME,
                Tables.USER_TOKEN.EXPIRE_TIME,
                Tables.USER_TOKEN.REFRESH_TIME
        ).values(
                token.getUserId(),
                token.getDeviceId(),
                token.getHash(),
                token.getStatus(),
                token.getIssueTime(),
                token.getExpireTime(),
                token.getRefreshTime()
        ).returning(Tables.USER_TOKEN.ID);
        return connection.preparedQuery(query.getSQL()).execute(Tuple.tuple(query.getBindValues())).map(rows -> rows.iterator().next().getLong(0));
    }

    public Future<Void> deleteById(SqlConnection connection, Long id) {
        DeleteConditionStep<?> query = dslContext.deleteFrom(Tables.USER_TOKEN).where(Tables.USER_TOKEN.ID.eq(id));
        return connection.preparedQuery(query.getSQL()).execute(Tuple.tuple(query.getBindValues())).mapEmpty();
    }

    public Future<Void> deleteByUserIdAndDeviceId(SqlConnection connection, Long userId, Long deviceId) {
        DeleteConditionStep<?> query = dslContext.deleteFrom(Tables.USER_TOKEN).where(Tables.USER_TOKEN.USER_ID.eq(userId)).and(Tables.USER_TOKEN.DEVICE_ID.eq(deviceId));
        return connection.preparedQuery(query.getSQL()).execute(Tuple.tuple(query.getBindValues())).mapEmpty();
    }
}
