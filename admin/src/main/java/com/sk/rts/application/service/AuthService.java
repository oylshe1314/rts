package com.sk.rts.application.service;

import com.sk.rts.application.auth.AdminAuthDetails;
import com.sk.rts.application.auth.AdminRemoteDetails;
import com.sk.rts.application.auth.ApiPathAuthority;
import com.sk.rts.application.entity.Admin;
import com.sk.rts.application.entity.Role;
import com.sk.rts.application.entity.enums.MenuType;
import com.sk.rts.application.entity.enums.Status;
import com.sk.rts.application.exception.StandardStatusException;
import com.sk.rts.application.jooq.Tables;
import com.sk.rts.application.repository.OperationRecordRepository;
import io.vertx.core.Future;
import io.vertx.sqlclient.Pool;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.Tuple;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jooq.DSLContext;
import org.jooq.SelectConditionStep;
import org.jspecify.annotations.NullMarked;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.Collection;

@Slf4j
@Service
@NullMarked
@AllArgsConstructor
public class AuthService {

    private final Pool pool;
    private final DSLContext dslContext;

    private final OperationRecordRepository operationRecordRepository;

    private final PasswordEncoder passwordEncoder;

    /**
     * 管理员登录认证
     *
     * @param username      账号
     * @param password      密码
     * @param remoteDetails 客户端信息
     * @return 管理员详情对象，包含权限信息
     */
    public Mono<AdminAuthDetails> login(String username, String password, AdminRemoteDetails remoteDetails) {
        SelectConditionStep<?> adminQuery = dslContext.select(Tables.ADMIN.ID, Tables.ADMIN.ROLE_ID, Tables.ADMIN.USERNAME, Tables.ADMIN.PASSWORD, Tables.ADMIN.PHONE, Tables.ADMIN.EMAIL, Tables.ADMIN.NICKNAME, Tables.ADMIN.AVATAR, Tables.ADMIN.STATUS, Tables.ROLE.ID, Tables.ROLE.NAME, Tables.ROLE.STATUS).from(Tables.ADMIN).innerJoin(Tables.ROLE).on(Tables.ROLE.ID.eq(Tables.ADMIN.ROLE_ID)).where(Tables.ADMIN.USERNAME.eq(username));

        return Mono.create(sink -> pool.getConnection().flatMap(connection -> connection.preparedQuery(adminQuery.getSQL())
                .execute(Tuple.tuple(adminQuery.getBindValues()))
                .map(rows -> {
                    if (rows.size() == 0) {
                        return null;
                    }

                    Row row = rows.iterator().next();

                    Admin admin = new Admin();
                    admin.setId(row.getLong(0));
                    admin.setRoleId(row.getLong(1));
                    admin.setUsername(row.getString(2));
                    admin.setPassword(row.getString(3));
                    admin.setPhone(row.getString(4));
                    admin.setEmail(row.getString(5));
                    admin.setNickname(row.getString(6));
                    admin.setAvatar(row.getString(7));
                    admin.setStatus(row.getInteger(8));

                    admin.setRole(new Role());
                    admin.getRole().setId(row.getLong(9));
                    admin.getRole().setName(row.getString(10));
                    admin.getRole().setStatus(row.getInteger(11));

                    return admin;
                })
                .flatMap(admin -> {
                    if (admin == null) {
                        return Future.failedFuture(new BadCredentialsException("", new StandardStatusException("账号或密码错误")));
                    }

                    if (admin.getRole().getStatus() != Status.enable.value()) {
                        return Future.failedFuture(new BadCredentialsException("", new StandardStatusException("角色已禁用")));
                    }

                    if (admin.getStatus() != Status.enable.value()) {
                        return Future.failedFuture(new BadCredentialsException("", new StandardStatusException("管理员已禁用")));
                    }

                    if (!passwordEncoder.matches(password, admin.getPassword())) {
                        return Future.failedFuture(new BadCredentialsException("", new StandardStatusException("账号或密码错误")));
                    }

                    AdminAuthDetails authDetails = new AdminAuthDetails(admin, remoteDetails);

                    SelectConditionStep<?> authoritiesQuery = dslContext.select(
                                    Tables.MENU.PATH)
                            .from(Tables.ROLE_MENU_AUTHORITY)
                            .innerJoin(Tables.MENU).on(Tables.MENU.ID.eq(Tables.ROLE_MENU_AUTHORITY.MENU_ID))
                            .where(Tables.ROLE_MENU_AUTHORITY.ROLE_ID.eq(admin.getRoleId())).and(Tables.MENU.TYPE.eq(MenuType.api.value()));
                    return connection.preparedQuery(authoritiesQuery.getSQL()).execute(Tuple.tuple(authoritiesQuery.getBindValues()))
                            .map(rows -> rows.stream().map(row -> new ApiPathAuthority(row.getString(0))).collect(authDetails::getAuthorities, Collection::add, Collection::addAll))
                            .flatMap(_ -> operationRecordRepository.add(connection, "login", authDetails.getUsername(), "", authDetails))
                            .map(authDetails);
                })
                .onComplete(_ -> connection.close())
                .onSuccess(sink::success)
                .onFailure(sink::error)
        ));
    }

    /**
     * 管理员登出
     *
     * @param adminDetails 管理员认证信息
     */
    public Mono<Void> logout(AdminAuthDetails adminDetails) {
        return Mono.create(sink -> pool.getConnection().flatMap(connection -> operationRecordRepository.add(connection, "logout", adminDetails.getUsername(), "", adminDetails)).onSuccess(sink::success).onFailure(sink::error));
    }
}
