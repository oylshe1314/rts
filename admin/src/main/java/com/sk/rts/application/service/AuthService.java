package com.sk.rts.application.service;

import com.sk.rts.application.auth.*;
import com.sk.rts.application.dto.SingleIdDto;
import com.sk.rts.application.entity.AdminRole;
import com.sk.rts.application.entity.AdminUser;
import com.sk.rts.application.entity.enums.MenuType;
import com.sk.rts.application.entity.enums.Operation;
import com.sk.rts.application.entity.enums.State;
import com.sk.rts.application.exception.StandardStatusException;
import com.sk.rts.application.jooq.Tables;
import com.sk.rts.application.repository.AdminOperationRecordRepository;
import com.sk.rts.application.util.CodecUtil;
import com.sk.rts.application.util.FeistelUtil;
import com.sk.rts.application.util.ReactiveUtil;
import io.vertx.core.Future;
import io.vertx.redis.client.Command;
import io.vertx.redis.client.Redis;
import io.vertx.redis.client.Request;
import io.vertx.sqlclient.Pool;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.Tuple;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jooq.DSLContext;
import org.jooq.Select;
import org.jspecify.annotations.NullMarked;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.Collection;
import java.util.List;

@Slf4j
@Service
@NullMarked
@AllArgsConstructor
public class AuthService {

    private final Pool pool;
    private final DSLContext dslContext;

    private final Redis redis;

    private final AdminOperationRecordRepository operationRecordRepository;

    private final PasswordEncoder passwordEncoder;

    /**
     * 管理员登录认证
     *
     * @param account       账号: 用户名/手机号/邮箱
     * @param password      密码
     * @param remoteDetails 客户端信息
     * @return 管理员详情对象，包含权限信息
     */
    public Mono<AdminAuthDetails> login(String account, String password, AdminRemoteDetails remoteDetails) {
        Select<?> adminQuery = dslContext.select(
                        Tables.ADMIN_USER.ID,
                        Tables.ADMIN_USER.ROLE_ID,
                        Tables.ADMIN_USER.USERNAME,
                        Tables.ADMIN_USER.PASSWORD,
                        Tables.ADMIN_USER.PHONE,
                        Tables.ADMIN_USER.EMAIL,
                        Tables.ADMIN_USER.NICKNAME,
                        Tables.ADMIN_USER.AVATAR,
                        Tables.ADMIN_USER.STATE,
                        Tables.ADMIN_ROLE.ID,
                        Tables.ADMIN_ROLE.NAME,
                        Tables.ADMIN_ROLE.STATE)
                .from(Tables.ADMIN_USER)
                .innerJoin(Tables.ADMIN_ROLE).on(Tables.ADMIN_ROLE.ID.eq(Tables.ADMIN_USER.ROLE_ID))
                .where(Tables.ADMIN_USER.USERNAME.eq(account)).or(Tables.ADMIN_USER.PHONE.eq(account)).or(Tables.ADMIN_USER.EMAIL.eq(account));

        Future<AdminAuthDetails> future = pool.withConnection(connection -> connection.preparedQuery(adminQuery.getSQL()).execute(Tuple.tuple(adminQuery.getBindValues()))
                .map(rows -> {
                    if (rows.size() == 0) {
                        return null;
                    }

                    Row row = rows.iterator().next();

                    AdminUser user = new AdminUser();
                    user.setId(row.getLong(0));
                    user.setRoleId(row.getLong(1));
                    user.setUsername(row.getString(2));
                    user.setPassword(row.getString(3));
                    user.setPhone(row.getString(4));
                    user.setEmail(row.getString(5));
                    user.setNickname(row.getString(6));
                    user.setAvatar(row.getString(7));
                    user.setState(row.getInteger(8));

                    user.setRole(new AdminRole());
                    user.getRole().setId(row.getLong(9));
                    user.getRole().setName(row.getString(10));
                    user.getRole().setState(row.getInteger(11));

                    return user;
                })
                .flatMap(admin -> {
                    if (admin == null) {
                        return Future.failedFuture(new BadCredentialsException("", new StandardStatusException("账号或密码错误")));
                    }

                    if (admin.getRole().getState() != State.enable.value()) {
                        return Future.failedFuture(new BadCredentialsException("", new StandardStatusException("角色已禁用")));
                    }

                    if (admin.getState() != State.enable.value()) {
                        return Future.failedFuture(new BadCredentialsException("", new StandardStatusException("管理员已禁用")));
                    }

                    if (!passwordEncoder.matches(password, admin.getPassword())) {
                        return Future.failedFuture(new BadCredentialsException("", new StandardStatusException("账号或密码错误")));
                    }

                    AdminAuthDetails authDetails = new AdminAuthDetails(admin, remoteDetails);

                    Select<?> authoritiesQuery = dslContext.select(
                                    Tables.ADMIN_MENU.PATH)
                            .from(Tables.ADMIN_ROLE_AUTHORITY)
                            .innerJoin(Tables.ADMIN_MENU).on(Tables.ADMIN_MENU.ID.eq(Tables.ADMIN_ROLE_AUTHORITY.MENU_ID))
                            .where(Tables.ADMIN_ROLE_AUTHORITY.ROLE_ID.eq(admin.getRoleId())).and(Tables.ADMIN_MENU.TYPE.eq(MenuType.api.value()))
                            .orderBy(Tables.ADMIN_MENU.ID.asc());
                    return connection.preparedQuery(authoritiesQuery.getSQL()).execute(Tuple.tuple(authoritiesQuery.getBindValues()))
                            .map(rows -> rows.stream().map(row -> new ApiPatternAuthority(row.getString(0))).collect(authDetails::getAuthorities, Collection::add, Collection::addAll))
                            .flatMap(_ -> operationRecordRepository.add(connection, Operation.login, authDetails.getAdminId().toString(), "admin_user", "", authDetails))
                            .map(authDetails);
                })
        );
        return ReactiveUtil.toMono(future);
    }

    /**
     * 管理员登出
     *
     * @param accessToken 管理员TOKEN信息
     * @param authDetails 管理员认证信息
     */
    public Mono<Void> logout(AdminAuthDetails authDetails, AdminAccessToken accessToken) {
        long adminId = authDetails.getAdminId();
        String subject = accessToken.getSubject();

        Request request1 = Request.cmd(Command.DEL);
        request1.arg(AdminAuthUtil.buildDetailsKey(adminId));
        request1.arg(AdminAuthUtil.buildTokenKey(subject));

        Request request2 = Request.cmd(Command.SREM);
        request2.arg(AdminAuthUtil.buildLoggedIdsKey());
        request2.arg(authDetails.getAdminId());

        Future<Void> future = redis.batch(List.of(request1, request2)).flatMap(_ -> operationRecordRepository.add(Operation.logout, String.valueOf(adminId), "admin_user", "", authDetails));
        return ReactiveUtil.toMono(future);
    }

    /**
     * 管理员跳出
     *
     * @param kickoutDtoMono 踢出参数
     * @param operator       操作员
     */
    public Mono<Void> kickout(Mono<SingleIdDto> kickoutDtoMono, AdminAuthDetails operator) {
        return kickoutDtoMono.flatMap(kickoutDto -> {
            String subject = CodecUtil.encode64(FeistelUtil.encode(kickoutDto.getId()));

            Request request1 = Request.cmd(Command.DEL);
            request1.arg(AdminAuthUtil.buildDetailsKey(kickoutDto.getId()));
            request1.arg(AdminAuthUtil.buildTokenKey(subject));

            Request request2 = Request.cmd(Command.SREM);
            request2.arg(AdminAuthUtil.buildLoggedIdsKey());
            request2.arg(kickoutDto.getId());

            Future<Void> future = redis.batch(List.of(request1, request2)).flatMap(_ -> operationRecordRepository.add(Operation.kickout, kickoutDto.getId().toString(), "admin_user", "", operator));
            return ReactiveUtil.toMono(future);
        });
    }
}
