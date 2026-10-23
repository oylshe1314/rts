package com.sk.rts.application.repository;

import com.sk.rts.application.entity.AdminMenu;
import com.sk.rts.application.jooq.Tables;
import io.vertx.core.Future;
import io.vertx.sqlclient.SqlConnection;
import io.vertx.sqlclient.Tuple;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jooq.*;
import org.springframework.stereotype.Repository;

import java.util.Collection;

@Slf4j
@Repository
@AllArgsConstructor
public class AdminMenuRepository {

    private final DSLContext dslContext;

    public Future<AdminMenu> getForUpdate(SqlConnection connection, Long id) {
        Select<?> query = dslContext.select(
                        Tables.ADMIN_MENU.ID,
                        Tables.ADMIN_MENU.PARENT_ID,
                        Tables.ADMIN_MENU.TYPE,
                        Tables.ADMIN_MENU.NAME,
                        Tables.ADMIN_MENU.ICON,
                        Tables.ADMIN_MENU.PATH,
                        Tables.ADMIN_MENU.SORT_BY,
                        Tables.ADMIN_MENU.STATE,
                        Tables.ADMIN_MENU.REMARK,
                        Tables.ADMIN_MENU.CREATE_BY,
                        Tables.ADMIN_MENU.CREATE_TIME,
                        Tables.ADMIN_MENU.UPDATE_BY,
                        Tables.ADMIN_MENU.UPDATE_TIME)
                .from(Tables.ADMIN_MENU)
                .where(Tables.ADMIN_MENU.ID.eq(id))
                .forUpdate();
        return connection.preparedQuery(query.getSQL()).execute(Tuple.tuple(query.getBindValues())).flatMap(rows -> {
            if (rows.size() == 0) {
                return Future.succeededFuture();
            }
            return Future.succeededFuture(AdminMenu.fromRow(rows.iterator().next()));
        });
    }

    public Future<Long> insert(SqlConnection connection, AdminMenu menu) {
        ResultQuery<?> query = dslContext.insertInto(
                        Tables.ADMIN_MENU,
                        Tables.ADMIN_MENU.PARENT_ID,
                        Tables.ADMIN_MENU.TYPE,
                        Tables.ADMIN_MENU.NAME,
                        Tables.ADMIN_MENU.ICON,
                        Tables.ADMIN_MENU.PATH,
                        Tables.ADMIN_MENU.SORT_BY,
                        Tables.ADMIN_MENU.STATE,
                        Tables.ADMIN_MENU.REMARK,
                        Tables.ADMIN_MENU.CREATE_BY,
                        Tables.ADMIN_MENU.CREATE_TIME,
                        Tables.ADMIN_MENU.UPDATE_BY,
                        Tables.ADMIN_MENU.UPDATE_TIME)
                .values(
                        menu.getParentId(),
                        menu.getType(),
                        menu.getName(),
                        menu.getIcon(),
                        menu.getPath(),
                        menu.getSortBy(),
                        menu.getState(),
                        menu.getRemark(),
                        menu.getCreateBy(),
                        menu.getCreateTime(),
                        menu.getUpdateBy(),
                        menu.getUpdateTime())
                .returning(Tables.ADMIN_MENU.ID);
        return connection.preparedQuery(query.getSQL()).execute(Tuple.tuple(query.getBindValues())).map(rows -> rows.iterator().next().getLong(0));
    }

    public Future<Boolean> existsByParentIdIn(SqlConnection connection, Collection<Long> parentIds) {
        Select<?> query = dslContext.select(Tables.ADMIN_MENU.ID).from(Tables.ADMIN_MENU).where(Tables.ADMIN_MENU.PARENT_ID.in(parentIds)).limit(1);
        return connection.preparedQuery(query.getSQL()).execute(Tuple.tuple(query.getBindValues())).map(rows -> rows.size() > 0);
    }
}
