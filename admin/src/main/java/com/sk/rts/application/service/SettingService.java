package com.sk.rts.application.service;

import com.sk.rts.application.auth.AdminAuthDetails;
import com.sk.rts.application.dto.ChangeDetailDto;
import com.sk.rts.application.dto.ChangePasswordDto;
import com.sk.rts.application.exception.ResponseStatus;
import com.sk.rts.application.exception.StandardStatusException;
import com.sk.rts.application.jooq.Tables;
import com.sk.rts.application.proto.caching.MsgAdminDetails;
import com.sk.rts.application.repository.OperationRecordRepository;
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
import java.util.function.Consumer;

@Service
@NullMarked
@AllArgsConstructor
public class SettingService {

    // 数据库访问仓库
    private final Pool pool;
    // SQL构建
    private final DSLContext dslContext;
    // 密码编码器
    private final PasswordEncoder passwordEncoder;
    // 操作记录仓库
    private final OperationRecordRepository operationRecordRepository;

    /**
     * 管理员信息修改
     *
     * @param changeDtoMono 信息修改参数
     * @param authDetails   管理员信息
     */
    public Mono<Void> changeDetail(Mono<ChangeDetailDto> changeDtoMono, AdminAuthDetails authDetails) {
        return changeDtoMono.flatMap(changeDto -> {
            Map<Field<?>, Object> values = new HashMap<>();
            List<Consumer<MsgAdminDetails.Builder>> runs = new ArrayList<>();
            if (changeDto.getPhone() != null) {
                values.put(Tables.ADMIN.PHONE, changeDto.getPhone());
                runs.add((builder) -> builder.setPhone(changeDto.getPhone()));
            }

            if (changeDto.getEmail() != null) {
                values.put(Tables.ADMIN.EMAIL, changeDto.getEmail());
                runs.add((builder) -> builder.setEmail(changeDto.getEmail()));
            }

            if (changeDto.getNickname() != null) {
                values.put(Tables.ADMIN.NICKNAME, changeDto.getNickname());
                runs.add((builder) -> builder.setNickname(changeDto.getNickname()));
            }

            if (changeDto.getAvatar() != null) {
                values.put(Tables.ADMIN.AVATAR, changeDto.getAvatar());
                runs.add((builder) -> builder.setAvatar(changeDto.getAvatar()));
            }

            if (values.isEmpty()) {
                return Mono.error(new StandardStatusException(ResponseStatus.parameter_error));
            }

            UpdateConditionStep<?> query = dslContext.update(Tables.ADMIN).set(values).where(Tables.ADMIN.ID.eq(authDetails.getAdminId()));
            return Mono.create(sink -> pool.getConnection().flatMap(connection -> connection.begin()
                    .compose(_ -> connection.preparedQuery(query.getSQL()).execute(Tuple.tuple(query.getBindValues())))
                    .compose(_ -> operationRecordRepository.add(connection, "changeDetail", authDetails.getUsername(), "", authDetails))
                    .compose(_ -> connection.transaction().commit())
                    .onComplete(_ -> connection.close())
                    .onSuccess(_ -> {
                        MsgAdminDetails.Builder msgAdminDetailsBuilder = authDetails.getAdminDetails().toBuilder();
                        runs.forEach(consumer -> consumer.accept(msgAdminDetailsBuilder));
                        authDetails.setAdminDetails(msgAdminDetailsBuilder.build());
                        sink.success();
                    })
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
                            .compose(_ -> operationRecordRepository.add(connection, "changePassword", authDetails.getUsername(), "", authDetails))
                            .compose(_ -> connection.transaction().commit())
                            .onComplete(_ -> connection.close())
                            .onSuccess(_ -> {
                                MsgAdminDetails.Builder msgAdminDetailsBuilder = authDetails.getAdminDetails().toBuilder();
                                msgAdminDetailsBuilder.setPassword(newPassword);
                                authDetails.setAdminDetails(msgAdminDetailsBuilder.build());
                                sink.success();
                            })
                            .onFailure(sink::error)
                    ));
                });
    }
}
