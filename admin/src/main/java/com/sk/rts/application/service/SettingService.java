package com.sk.rts.application.service;

import com.sk.rts.application.auth.AdminAuthDetails;
import com.sk.rts.application.auth.AdminAuthUtil;
import com.sk.rts.application.dto.PasswordChangeDto;
import com.sk.rts.application.dto.DetailsChangeDto;
import com.sk.rts.application.entity.enums.Operation;
import com.sk.rts.application.exception.ResponseStatus;
import com.sk.rts.application.exception.StandardStatusException;
import com.sk.rts.application.jooq.Tables;
import com.sk.rts.application.repository.AdminOperationRecordRepository;
import com.sk.rts.application.tools.UpdateRemarkBuilder;
import com.sk.rts.application.util.ReactiveUtil;
import io.vertx.core.Future;
import io.vertx.redis.client.Command;
import io.vertx.redis.client.Redis;
import io.vertx.redis.client.Request;
import io.vertx.sqlclient.Pool;
import io.vertx.sqlclient.Tuple;
import lombok.AllArgsConstructor;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.UpdateConditionStep;
import org.jspecify.annotations.NullMarked;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import tools.jackson.databind.json.JsonMapper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@NullMarked
@AllArgsConstructor
public class SettingService {

    private final Pool pool;
    private final DSLContext dslContext;

    private final AdminOperationRecordRepository operationRecordRepository;

    private final Redis redis;

    private final JsonMapper jsonMapper;

    private final PasswordEncoder passwordEncoder;

    private Future<Void> updateCache(AdminAuthDetails authDetails) {
        Request request = Request.cmd(Command.SET);
        request.arg(AdminAuthUtil.buildDetailsKey(authDetails.getAdminId()));
        request.arg(jsonMapper.writeValueAsString(authDetails));

        return redis.send(request).mapEmpty();
    }

    /**
     * 管理员信息修改
     *
     * @param changeDto   信息修改参数
     * @param authDetails 管理员信息
     */
    public Mono<Void> changeDetails(DetailsChangeDto changeDto, AdminAuthDetails authDetails) {
        List<Runnable> runs = new ArrayList<>();
        Map<Field<?>, Object> values = new HashMap<>();
        UpdateRemarkBuilder remarkBuilder = new UpdateRemarkBuilder();
        if (changeDto.getPhone() != null) {
            remarkBuilder.add("phone", authDetails.getPhone(), changeDto.getPhone());
            values.put(Tables.ADMIN_USER.PHONE, changeDto.getPhone());
            runs.add(() -> authDetails.setPhone(changeDto.getPhone()));
        }

        if (changeDto.getEmail() != null) {
            remarkBuilder.add("email", authDetails.getEmail(), changeDto.getEmail());
            values.put(Tables.ADMIN_USER.EMAIL, changeDto.getEmail());
            runs.add(() -> authDetails.setEmail(changeDto.getEmail()));
        }

        if (changeDto.getNickname() != null) {
            remarkBuilder.add("nickname", authDetails.getNickname(), changeDto.getNickname());
            values.put(Tables.ADMIN_USER.NICKNAME, changeDto.getNickname());
            runs.add(() -> authDetails.setNickname(changeDto.getNickname()));
        }

        if (changeDto.getAvatar() != null) {
            remarkBuilder.add("avatar", authDetails.getAvatar(), changeDto.getAvatar());
            values.put(Tables.ADMIN_USER.AVATAR, changeDto.getAvatar());
            runs.add(() -> authDetails.setAvatar(changeDto.getAvatar()));
        }

        if (values.isEmpty()) {
            return Mono.error(new StandardStatusException(ResponseStatus.parameter_error));
        }

        UpdateConditionStep<?> query = dslContext.update(Tables.ADMIN_USER).set(values).where(Tables.ADMIN_USER.ID.eq(authDetails.getAdminId()));
        Future<Void> future = pool.withTransaction(connection -> connection.preparedQuery(query.getSQL()).execute(Tuple.tuple(query.getBindValues()))
                .compose(_ -> operationRecordRepository.add(connection, Operation.changeDetails, String.valueOf(authDetails.getAdminId()), "admin_user", remarkBuilder.build(), authDetails))
                .compose(_ -> {
                    runs.forEach(Runnable::run);
                    return updateCache(authDetails);
                })
        );
        return ReactiveUtil.toMono(future);
    }

    /**
     * 管理员修改密码
     *
     * @param changeDto   密码修改参数
     * @param authDetails 管理员信息
     * @throws StandardStatusException 影响状态错误异常
     */
    public Mono<Void> changePassword(PasswordChangeDto changeDto, AdminAuthDetails authDetails) {
        if (!passwordEncoder.matches(changeDto.getOldPassword(), authDetails.getPassword())) {
            return Mono.error(new StandardStatusException("旧密码错误"));
        }

        String newPassword = passwordEncoder.encode(changeDto.getNewPassword());
        if (newPassword == null) {
            return Mono.error(new StandardStatusException(ResponseStatus.internal_error));
        }

        UpdateRemarkBuilder remarkBuilder = new UpdateRemarkBuilder();
        remarkBuilder.add("password", authDetails.getPassword(), newPassword);

        UpdateConditionStep<?> query = dslContext.update(Tables.ADMIN_USER).set(Tables.ADMIN_USER.PASSWORD, newPassword).where(Tables.ADMIN_USER.ID.eq(authDetails.getAdminId()));
        Future<Void> future = pool.withTransaction(connection -> connection.preparedQuery(query.getSQL()).execute(Tuple.tuple(query.getBindValues()))
                .compose(_ -> operationRecordRepository.add(connection, Operation.changePassword, String.valueOf(authDetails.getAdminId()), "admin_user", remarkBuilder.build(), authDetails))
                .compose(_ -> {
                    authDetails.setPassword(newPassword);
                    return updateCache(authDetails);
                })
        );
        return ReactiveUtil.toMono(future);
    }
}
