package com.sk.rts.application.service;

import com.sk.rts.application.auth.AdminAuthDetails;
import com.sk.rts.application.auth.AdminAuthUtil;
import com.sk.rts.application.auth.ApiPatternAuthority;
import com.sk.rts.application.dto.ChangeDetailsDto;
import com.sk.rts.application.dto.ChangePasswordDto;
import com.sk.rts.application.exception.ResponseStatus;
import com.sk.rts.application.exception.StandardStatusException;
import com.sk.rts.application.jooq.Tables;
import com.sk.rts.application.proto.caching.MsgAdminDetails;
import com.sk.rts.application.repository.OperationRecordRepository;
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

    private final Redis redis;

    private final OperationRecordRepository operationRecordRepository;

    private final PasswordEncoder passwordEncoder;

    private Future<Void> updateCache(AdminAuthDetails authDetails) {
        MsgAdminDetails.Builder msgAdminDetailsBuilder = MsgAdminDetails.newBuilder();
        msgAdminDetailsBuilder.setId(authDetails.getAdminId());
        msgAdminDetailsBuilder.setRoleId(authDetails.getRoleId());
        msgAdminDetailsBuilder.setRoleName(authDetails.getRoleName());
        msgAdminDetailsBuilder.setUsername(authDetails.getUsername());
        msgAdminDetailsBuilder.setPassword(authDetails.getPassword());
        msgAdminDetailsBuilder.setPhone(authDetails.getPhone());
        msgAdminDetailsBuilder.setEmail(authDetails.getEmail());
        msgAdminDetailsBuilder.setNickname(authDetails.getNickname());
        msgAdminDetailsBuilder.setAvatar(authDetails.getAvatar());
        for (ApiPatternAuthority authority : authDetails.getAuthorities()) {
            msgAdminDetailsBuilder.addAuthority(authority.getAuthority());
        }

        Request request = Request.cmd(Command.SET);
        request.arg(AdminAuthUtil.buildDetailsKey(authDetails.getAdminId()));
        request.arg(msgAdminDetailsBuilder.build().toByteArray());

        return redis.send(request).mapEmpty();
    }

    /**
     * 管理员信息修改
     *
     * @param changeDtoMono 信息修改参数
     * @param authDetails   管理员信息
     */
    public Mono<Void> changeDetails(Mono<ChangeDetailsDto> changeDtoMono, AdminAuthDetails authDetails) {
        return changeDtoMono.flatMap(changeDto -> {
            Map<Field<?>, Object> values = new HashMap<>();
            List<Runnable> runs = new ArrayList<>();
            if (changeDto.getPhone() != null) {
                values.put(Tables.ADMIN.PHONE, changeDto.getPhone());
                runs.add(() -> authDetails.setPhone(changeDto.getPhone()));
            }

            if (changeDto.getEmail() != null) {
                values.put(Tables.ADMIN.EMAIL, changeDto.getEmail());
                runs.add(() -> authDetails.setEmail(changeDto.getEmail()));
            }

            if (changeDto.getNickname() != null) {
                values.put(Tables.ADMIN.NICKNAME, changeDto.getNickname());
                runs.add(() -> authDetails.setNickname(changeDto.getNickname()));
            }

            if (changeDto.getAvatar() != null) {
                values.put(Tables.ADMIN.AVATAR, changeDto.getAvatar());
                runs.add(() -> authDetails.setAvatar(changeDto.getAvatar()));
            }

            if (values.isEmpty()) {
                return Mono.error(new StandardStatusException(ResponseStatus.parameter_error));
            }

            UpdateConditionStep<?> query = dslContext.update(Tables.ADMIN).set(values).where(Tables.ADMIN.ID.eq(authDetails.getAdminId()));
            return Mono.create(sink -> pool.getConnection().flatMap(connection -> connection.begin()
                    .compose(_ -> connection.preparedQuery(query.getSQL()).execute(Tuple.tuple(query.getBindValues())))
                    .compose(_ -> operationRecordRepository.add(connection, "changeDetail", String.valueOf(authDetails.getAdminId()), "", authDetails))
                    .compose(_ -> {
                        runs.forEach(Runnable::run);
                        return updateCache(authDetails);
                    })
                    .compose(_ -> connection.transaction().commit())
                    .onComplete(_ -> connection.close())
                    .onSuccess(sink::success)
                    .onFailure(sink::error)
            ));
        });
    }

    /**
     * 管理员修改密码
     *
     * @param changeDtoMono 密码修改参数
     * @param authDetails   管理员信息
     * @throws StandardStatusException 影响状态错误异常
     */
    public Mono<Void> changePassword(Mono<ChangePasswordDto> changeDtoMono, AdminAuthDetails authDetails) {
        return changeDtoMono.flatMap(changeDto -> {
                    if (!passwordEncoder.matches(changeDto.getOldPassword(), authDetails.getPassword())) {
                        return Mono.error(new StandardStatusException("旧密码错误"));
                    }

                    String newPassword = passwordEncoder.encode(changeDto.getNewPassword());
                    if (newPassword == null) {
                        return Mono.error(new StandardStatusException(ResponseStatus.internal_error));
                    }

                    return Mono.just(newPassword);
                })
                .flatMap(newPassword -> {
                    UpdateConditionStep<?> query = dslContext.update(Tables.ADMIN).set(Tables.ADMIN.PASSWORD, newPassword).where(Tables.ADMIN.ID.eq(authDetails.getAdminId()));
                    return Mono.create(sink -> pool.getConnection().flatMap(connection -> connection.begin()
                            .compose(_ -> connection.preparedQuery(query.getSQL()).execute(Tuple.tuple(query.getBindValues())))
                            .compose(_ -> operationRecordRepository.add(connection, "changePassword", String.valueOf(authDetails.getAdminId()), "", authDetails))
                            .compose(_ -> {
                                authDetails.setPassword(newPassword);
                                return updateCache(authDetails);
                            })
                            .compose(_ -> connection.transaction().commit())
                            .onComplete(_ -> connection.close())
                            .onSuccess(sink::success)
                            .onFailure(sink::error)
                    ));
                });
    }
}
