package com.sk.rts.application.repository;

import com.sk.rts.application.entity.AdminUser;
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
public class AdminUserRepository {

    private final DSLContext dslContext;

    public Future<AdminUser> getForUpdate(SqlConnection connection, Long id) {
        Select<?> query = dslContext.select(
                        Tables.ADMIN_USER.ID,
                        Tables.ADMIN_USER.ROLE_ID,
                        Tables.ADMIN_USER.USERNAME,
                        Tables.ADMIN_USER.PASSWORD,
                        Tables.ADMIN_USER.PHONE,
                        Tables.ADMIN_USER.EMAIL,
                        Tables.ADMIN_USER.NICKNAME,
                        Tables.ADMIN_USER.AVATAR,
                        Tables.ADMIN_USER.STATE,
                        Tables.ADMIN_USER.REMARK,
                        Tables.ADMIN_USER.CREATE_BY,
                        Tables.ADMIN_USER.CREATE_TIME,
                        Tables.ADMIN_USER.UPDATE_BY,
                        Tables.ADMIN_USER.UPDATE_TIME)
                .from(Tables.ADMIN_USER)
                .where(Tables.ADMIN_USER.ID.eq(id))
                .forUpdate();
        return connection.preparedQuery(query.getSQL()).execute(Tuple.tuple(query.getBindValues())).flatMap(rows -> {
            if (rows.size() == 0) {
                return Future.succeededFuture();
            }
            return Future.succeededFuture(AdminUser.fromRow(rows.iterator().next()));
        });
    }

    public Future<Long> insert(SqlConnection connection, AdminUser admin) {
        ResultQuery<?> query = dslContext.insertInto(
                        Tables.ADMIN_USER,
                        Tables.ADMIN_USER.ROLE_ID,
                        Tables.ADMIN_USER.USERNAME,
                        Tables.ADMIN_USER.PASSWORD,
                        Tables.ADMIN_USER.PHONE,
                        Tables.ADMIN_USER.EMAIL,
                        Tables.ADMIN_USER.NICKNAME,
                        Tables.ADMIN_USER.AVATAR,
                        Tables.ADMIN_USER.STATE,
                        Tables.ADMIN_USER.REMARK,
                        Tables.ADMIN_USER.CREATE_BY,
                        Tables.ADMIN_USER.CREATE_TIME,
                        Tables.ADMIN_USER.UPDATE_BY,
                        Tables.ADMIN_USER.UPDATE_TIME)
                .values(
                        admin.getRoleId(),
                        admin.getUsername(),
                        admin.getPassword(),
                        admin.getPhone(),
                        admin.getEmail(),
                        admin.getNickname(),
                        admin.getAvatar(),
                        admin.getState(),
                        admin.getRemark(),
                        admin.getCreateBy(),
                        admin.getCreateTime(),
                        admin.getUpdateBy(),
                        admin.getUpdateTime())
                .returning(Tables.ADMIN_USER.ID);
        return connection.preparedQuery(query.getSQL()).execute(Tuple.tuple(query.getBindValues())).map(rows -> rows.iterator().next().getLong(0));
    }

    public Future<Boolean> existsByRoleIdIn(SqlConnection connection, Collection<Long> roleIds) {
        Select<?> query = dslContext.select(Tables.ADMIN_USER.ID).from(Tables.ADMIN_USER).where(Tables.ADMIN_USER.ROLE_ID.in(roleIds));
        return connection.preparedQuery(query.getSQL()).execute(Tuple.tuple(query.getBindValues())).map(rows -> rows.size() > 0);
    }
}
