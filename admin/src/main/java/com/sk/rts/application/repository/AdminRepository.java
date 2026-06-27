package com.sk.rts.application.repository;

import com.sk.rts.application.entity.Admin;
import com.sk.rts.application.jooq.Tables;
import io.vertx.core.Future;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.SqlConnection;
import io.vertx.sqlclient.Tuple;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jooq.*;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Slf4j
@Repository
@AllArgsConstructor
public class AdminRepository {

    private final DSLContext dslContext;

    public Future<Admin> getForUpdate(SqlConnection connection, Long id) {
        Select<?> query = dslContext.select(
                        Tables.ADMIN.ID,
                        Tables.ADMIN.ROLE_ID,
                        Tables.ADMIN.USERNAME,
                        Tables.ADMIN.PASSWORD,
                        Tables.ADMIN.PHONE,
                        Tables.ADMIN.EMAIL,
                        Tables.ADMIN.NICKNAME,
                        Tables.ADMIN.AVATAR,
                        Tables.ADMIN.STATUS,
                        Tables.ADMIN.REMARK,
                        Tables.ADMIN.CREATE_BY,
                        Tables.ADMIN.CREATE_TIME,
                        Tables.ADMIN.UPDATE_BY,
                        Tables.ADMIN.UPDATE_TIME)
                .from(Tables.ADMIN)
                .where(Tables.ADMIN.ID.eq(id))
                .forUpdate();
        return connection.preparedQuery(query.getSQL()).execute(Tuple.tuple(query.getBindValues())).flatMap(rows -> {
            if (rows.size() == 0) {
                return Future.succeededFuture();
            }
            return Future.succeededFuture(Admin.fromRow(rows.iterator().next()));
        });
    }

    public Future<Boolean> existsByRoleIdIn(SqlConnection connection, Collection<Long> roleIds) {
        Select<?> query = dslContext.select(Tables.ADMIN.ID).from(Tables.ADMIN).where(Tables.ADMIN.ROLE_ID.in(roleIds));
        return connection.preparedQuery(query.getSQL()).execute(Tuple.tuple(query.getBindValues())).map(rows -> rows.size() > 0);
    }

    public Future<Long> insert(SqlConnection connection, Admin admin) {
        ResultQuery<?> query = dslContext.insertInto(
                        Tables.ADMIN,
                        Tables.ADMIN.ROLE_ID,
                        Tables.ADMIN.USERNAME,
                        Tables.ADMIN.PASSWORD,
                        Tables.ADMIN.PHONE,
                        Tables.ADMIN.EMAIL,
                        Tables.ADMIN.NICKNAME,
                        Tables.ADMIN.AVATAR,
                        Tables.ADMIN.STATUS,
                        Tables.ADMIN.REMARK,
                        Tables.ADMIN.CREATE_BY,
                        Tables.ADMIN.CREATE_TIME,
                        Tables.ADMIN.UPDATE_BY,
                        Tables.ADMIN.UPDATE_TIME)
                .values(
                        admin.getRoleId(),
                        admin.getUsername(),
                        admin.getPassword(),
                        admin.getPhone(),
                        admin.getEmail(),
                        admin.getNickname(),
                        admin.getAvatar(),
                        admin.getStatus(),
                        admin.getRemark(),
                        admin.getCreateBy(),
                        admin.getCreateTime(),
                        admin.getUpdateBy(),
                        admin.getUpdateTime())
                .returning(Tables.ADMIN.ID);
        return connection.preparedQuery(query.getSQL()).execute(Tuple.tuple(query.getBindValues())).map(rows -> rows.iterator().next().getLong(0));
    }
}
