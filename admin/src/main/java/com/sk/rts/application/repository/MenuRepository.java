package com.sk.rts.application.repository;

import com.sk.rts.application.entity.Menu;
import com.sk.rts.application.jooq.Tables;
import io.vertx.core.Future;
import io.vertx.sqlclient.SqlConnection;
import io.vertx.sqlclient.Tuple;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jooq.DSLContext;
import org.jooq.InsertResultStep;
import org.jooq.SelectForUpdateOfStep;
import org.jooq.SelectLimitPercentStep;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Slf4j
@Repository
@AllArgsConstructor
public class MenuRepository {

    private final DSLContext dslContext;

    public Future<Menu> getForUpdate(SqlConnection connection, Long id) {
        SelectForUpdateOfStep<?> query = dslContext.select(
                        Tables.MENU.ID,
                        Tables.MENU.PARENT_ID,
                        Tables.MENU.TYPE,
                        Tables.MENU.NAME,
                        Tables.MENU.ICON,
                        Tables.MENU.PATH,
                        Tables.MENU.SORT_BY,
                        Tables.MENU.STATUS,
                        Tables.MENU.REMARK,
                        Tables.MENU.CREATE_BY,
                        Tables.MENU.CREATE_TIME,
                        Tables.MENU.UPDATE_BY,
                        Tables.MENU.UPDATE_TIME)
                .from(Tables.MENU)
                .where(Tables.MENU.ID.eq(id))
                .forUpdate();
        return connection.preparedQuery(query.getSQL()).execute(Tuple.tuple(query.getBindValues())).flatMap(rows -> {
            if (rows.size() == 0) {
                return Future.succeededFuture();
            }
            return Future.succeededFuture(Menu.fromRow(rows.iterator().next()));
        });
    }

    public Future<Boolean> existsByParentIdAndName(SqlConnection connection, Long parentId, String name) {
        SelectLimitPercentStep<?> query = dslContext.select(Tables.MENU.ID).from(Tables.MENU).where(Tables.MENU.PARENT_ID.eq(parentId)).and(Tables.MENU.NAME.eq(name)).limit(1);
        return connection.preparedQuery(query.getSQL()).execute(Tuple.tuple(query.getBindValues())).map(rows -> rows.size() > 0);
    }

    public Future<Boolean> existsByTypeAndPath(SqlConnection connection, Integer type, String path) {
        SelectLimitPercentStep<?> query = dslContext.select(Tables.MENU.ID).from(Tables.MENU).where(Tables.MENU.TYPE.eq(type)).and(Tables.MENU.PATH.eq(path)).limit(1);
        return connection.preparedQuery(query.getSQL()).execute(Tuple.tuple(query.getBindValues())).map(rows -> rows.size() > 0);
    }

    public Future<Boolean> existsByParentIdIn(SqlConnection connection, Collection<Long> parentIds) {
        SelectLimitPercentStep<?> query = dslContext.select(Tables.MENU.ID).from(Tables.MENU).where(Tables.MENU.PARENT_ID.in(parentIds)).limit(1);
        return connection.preparedQuery(query.getSQL()).execute(Tuple.tuple(query.getBindValues())).map(rows -> rows.size() > 0);
    }

    public Future<Long> insert(SqlConnection connection, Menu menu) {
        InsertResultStep<?> query = dslContext.insertInto(
                        Tables.MENU,
                        Tables.MENU.PARENT_ID,
                        Tables.MENU.TYPE,
                        Tables.MENU.NAME,
                        Tables.MENU.ICON,
                        Tables.MENU.PATH,
                        Tables.MENU.SORT_BY,
                        Tables.MENU.STATUS,
                        Tables.MENU.REMARK,
                        Tables.MENU.CREATE_BY,
                        Tables.MENU.CREATE_TIME,
                        Tables.MENU.UPDATE_BY,
                        Tables.MENU.UPDATE_TIME)
                .values(
                        menu.getParentId(),
                        menu.getType(),
                        menu.getName(),
                        menu.getIcon(),
                        menu.getPath(),
                        menu.getSortBy(),
                        menu.getStatus(),
                        menu.getRemark(),
                        menu.getCreateBy(),
                        menu.getCreateTime(),
                        menu.getUpdateBy(),
                        menu.getUpdateTime())
                .returning(Tables.MENU.ID);
        return connection.preparedQuery(query.getSQL()).execute(Tuple.tuple(query.getBindValues())).map(rows -> rows.iterator().next().getLong(0));
    }
}
