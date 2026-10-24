package com.sk.rts.application.repository;

import com.sk.rts.application.entity.AdminRole;
import com.sk.rts.application.jooq.Tables;
import io.vertx.core.Future;
import io.vertx.sqlclient.SqlConnection;
import io.vertx.sqlclient.Tuple;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jooq.DSLContext;
import org.jooq.ResultQuery;
import org.jooq.Select;
import org.springframework.stereotype.Repository;

@Slf4j
@Repository
@AllArgsConstructor
public class AdminRoleRepository {

    private final DSLContext dslContext;

    public Future<AdminRole> getForUpdate(SqlConnection connection, Long id) {
        Select<?> query = dslContext.select(
                Tables.ADMIN_ROLE.ID,
                Tables.ADMIN_ROLE.NAME,
                Tables.ADMIN_ROLE.CODE,
                Tables.ADMIN_ROLE.STATE,
                Tables.ADMIN_ROLE.REMARK,
                Tables.ADMIN_ROLE.CREATE_BY,
                Tables.ADMIN_ROLE.CREATE_TIME,
                Tables.ADMIN_ROLE.UPDATE_BY,
                Tables.ADMIN_ROLE.UPDATE_TIME
        ).from(Tables.ADMIN_ROLE).where(Tables.ADMIN_ROLE.ID.eq(id)).forUpdate();
        return connection.preparedQuery(query.getSQL()).execute(Tuple.tuple(query.getBindValues())).flatMap(rows -> {
            if (rows.size() == 0) {
                return Future.succeededFuture();
            }
            return Future.succeededFuture(AdminRole.fromRow(rows.iterator().next()));
        });
    }

    public Future<Long> insert(SqlConnection connection, AdminRole role) {
        ResultQuery<?> query = dslContext.insertInto(
                        Tables.ADMIN_ROLE,
                        Tables.ADMIN_ROLE.NAME,
                        Tables.ADMIN_ROLE.CODE,
                        Tables.ADMIN_ROLE.STATE,
                        Tables.ADMIN_ROLE.REMARK,
                        Tables.ADMIN_ROLE.CREATE_BY,
                        Tables.ADMIN_ROLE.CREATE_TIME,
                        Tables.ADMIN_ROLE.UPDATE_BY,
                        Tables.ADMIN_ROLE.UPDATE_TIME)
                .values(
                        role.getName(),
                        role.getCode(),
                        role.getState(),
                        role.getRemark(),
                        role.getCreateBy(),
                        role.getCreateTime(),
                        role.getUpdateBy(),
                        role.getUpdateTime())
                .returning(Tables.ADMIN_ROLE.ID);
        return connection.preparedQuery(query.getSQL()).execute(Tuple.tuple(query.getBindValues())).map(rows -> rows.iterator().next().getLong(0));
    }
}
