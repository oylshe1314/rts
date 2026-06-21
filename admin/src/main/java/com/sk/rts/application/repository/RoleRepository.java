package com.sk.rts.application.repository;

import com.sk.rts.application.entity.Role;
import com.sk.rts.application.jooq.Tables;
import io.vertx.core.Future;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.SqlConnection;
import io.vertx.sqlclient.Tuple;
import lombok.AllArgsConstructor;
import org.jooq.DSLContext;
import org.jooq.SelectForUpdateOfStep;
import org.springframework.stereotype.Repository;

@Repository
@AllArgsConstructor
public class RoleRepository {

    private final DSLContext dslContext;

    public Future<Role> getForUpdate(SqlConnection connection, Long id) {
        SelectForUpdateOfStep<?> query = dslContext.select(
                Tables.ROLE.ID,
                Tables.ROLE.NAME,
                Tables.ROLE.CODE,
                Tables.ROLE.STATUS,
                Tables.ROLE.REMARK,
                Tables.ROLE.CREATE_BY,
                Tables.ROLE.CREATE_TIME,
                Tables.ROLE.UPDATE_BY,
                Tables.ROLE.UPDATE_TIME
        ).from(Tables.ROLE).where(Tables.ROLE.ID.eq(id)).forUpdate();
        return connection.preparedQuery(query.getSQL()).execute(Tuple.tuple(query.getBindValues())).flatMap(rows -> {
            if (rows.size() == 0) {
                return Future.succeededFuture();
            }
            return Future.succeededFuture(Role.fromRow(rows.iterator().next()));
        });
    }

    public Future<Boolean> existsByName(SqlConnection connection, String name) {
        SelectForUpdateOfStep<?> query = dslContext.select(Tables.ROLE.ID).from(Tables.ROLE).where(Tables.ROLE.NAME.eq(name)).forUpdate();
        return connection.preparedQuery(query.getSQL()).execute(Tuple.tuple(query.getBindValues())).map(rows -> rows.size() > 0);
    }
}
