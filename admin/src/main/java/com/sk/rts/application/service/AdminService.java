package com.sk.rts.application.service;

import com.sk.rts.application.auth.AdminAuthDetails;
import com.sk.rts.application.auth.AdminAuthUtil;
import com.sk.rts.application.dto.*;
import com.sk.rts.application.entity.Admin;
import com.sk.rts.application.entity.Role;
import com.sk.rts.application.entity.enums.State;
import com.sk.rts.application.exception.ExceptionUtil;
import com.sk.rts.application.exception.ResponseStatus;
import com.sk.rts.application.exception.StandardStatusException;
import com.sk.rts.application.jooq.Tables;
import com.sk.rts.application.jooq.tables.TableAdmin;
import com.sk.rts.application.jooq.tables.TableRole;
import com.sk.rts.application.repository.AdminRepository;
import com.sk.rts.application.repository.OperationRecordRepository;
import com.sk.rts.application.repository.RoleRepository;
import io.vertx.core.Future;
import io.vertx.redis.client.Command;
import io.vertx.redis.client.Redis;
import io.vertx.redis.client.Request;
import io.vertx.sqlclient.Pool;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.Tuple;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.jooq.*;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@NullMarked
@AllArgsConstructor
public class AdminService {

    // 默认管理员的ID，默认管理员禁止删除或者禁用。
    private static final Long DEFAULT_ADMIN_ID = 1L;

    private final Pool pool;
    private final DSLContext dslContext;

    private final Redis redis;

    private final AdminRepository adminRepository;
    private final RoleRepository roleRepository;
    private final OperationRecordRepository operationRecordRepository;

    private final PasswordEncoder passwordEncoder;

    /**
     * 获取管理员选择列表
     *
     * @param roleId 角色ID，null时获取全部
     * @return 管理员选择列表
     */
    public Mono<List<AdminOptionDto>> adminSelectList(@Nullable Long roleId) {
        Select<?> query = dslContext.select(
                        Tables.ADMIN.ID,
                        Tables.ADMIN.ROLE_ID,
                        Tables.ADMIN.USERNAME,
                        Tables.ADMIN.NICKNAME,
                        Tables.ADMIN.AVATAR)
                .from(Tables.ADMIN);
        if (roleId != null) {
            query = ((SelectWhereStep<?>) query).where(Tables.ADMIN.ROLE_ID.eq(roleId));
        }

        String sql = query.getSQL();
        List<Object> args = query.getBindValues();
        return Flux.<AdminOptionDto>create(sink -> pool.preparedQuery(sql).execute(Tuple.tuple(args))
                .onFailure(sink::error)
                .onSuccess(rows -> {
                    for (Row row : rows) {
                        sink.next(new AdminOptionDto(row.getLong(0), row.getLong(1), row.getString(2), row.getString(3), row.getString(4)));
                    }
                    sink.complete();
                })
        ).collectList();
    }

    /**
     * 管理员分页查询, ID升序
     *
     * @param pageRequestDtoMono 分页查询参数
     * @return 分页查询结果
     */
    public Mono<PageResultDto<AdminDto>> query(Mono<PageQueryDto<@Nullable AdminQueryDto>> pageRequestDtoMono) {
        return pageRequestDtoMono.flatMap(pageRequestDto -> {
            AdminQueryDto queryDto = pageRequestDto.getQuery();

            TableAdmin a = Tables.ADMIN.as("a");
            TableRole r = Tables.ROLE.as("r");

            Select<?> pageQuery = dslContext.select(
                            a.ID,
                            a.ROLE_ID,
                            a.USERNAME,
                            a.PASSWORD,
                            a.PHONE,
                            a.EMAIL,
                            a.NICKNAME,
                            a.AVATAR,
                            a.STATE,
                            a.REMARK,
                            a.CREATE_BY,
                            a.CREATE_TIME,
                            a.UPDATE_BY,
                            a.UPDATE_TIME,
                            r.ID,
                            r.NAME)
                    .from(a).
                    innerJoin(r).on(r.ID.eq(a.ROLE_ID));
            Select<?> countQuery = dslContext.selectCount().from(a).innerJoin(r).on(r.ID.eq(a.ROLE_ID));

            List<Condition> conditions = new ArrayList<>();
            if (queryDto != null) {
                if (queryDto.getRoleId() != null) {
                    conditions.add(a.ROLE_ID.eq(queryDto.getRoleId()));
                }
                if (queryDto.getUsername() != null) {
                    conditions.add(a.USERNAME.like("%" + queryDto.getUsername() + "%"));
                }
                if (queryDto.getPhone() != null) {
                    conditions.add(a.PHONE.like("%" + queryDto.getPhone() + "%"));
                }
                if (queryDto.getEmail() != null) {
                    conditions.add(a.EMAIL.like("%" + queryDto.getEmail() + "%"));
                }
            }

            if (!conditions.isEmpty()) {
                pageQuery = ((SelectWhereStep<?>) pageQuery).where(conditions);
                countQuery = ((SelectWhereStep<?>) countQuery).where(conditions);
            }

            if (pageRequestDto.getSort() == null) {
                pageQuery = ((SelectOrderByStep<?>) pageQuery).orderBy(a.ID.asc());
            } else {
                OrderField<?> sortField = a.field(pageRequestDto.getSort());
                if (sortField == null) {
                    return Mono.error(new StandardStatusException(ResponseStatus.parameter_error));
                }

                if (Boolean.TRUE.equals(pageRequestDto.getDesc())) {
                    sortField = ((Field<?>) sortField).desc();
                }

                pageQuery = ((SelectOrderByStep<?>) pageQuery).orderBy(sortField);
            }

            pageQuery = ((SelectLimitStep<?>) pageQuery).offset(pageRequestDto.getOffset()).limit(pageRequestDto.getPageSize());

            String pageSql = pageQuery.getSQL();
            String countSql = countQuery.getSQL();
            List<Object> pageArgs = pageQuery.getBindValues();
            List<Object> countArgs = countQuery.getBindValues();
            return Mono.create(sink -> pool.getConnection().flatMap(connection -> connection.preparedQuery(pageSql).execute(Tuple.tuple(pageArgs))
                    .map(rows -> rows.stream().map(row -> {
                                Admin admin = Admin.fromRow(row);
                                admin.setRole(new Role());
                                admin.getRole().setId(row.getLong(14));
                                admin.getRole().setName(row.getString(15));
                                return new AdminDto(admin);
                            }).toList()
                    )
                    .flatMap(admins -> {
                        if (admins.size() < pageRequestDto.getPageSize()) {
                            return Future.succeededFuture(new PageResultDto<>(pageRequestDto.getPageNo(), pageRequestDto.getPageSize(), admins.size(), admins));
                        }

                        return connection.preparedQuery(countSql).execute(Tuple.tuple(countArgs)).map(rows -> new PageResultDto<>(pageRequestDto.getPageNo(), pageRequestDto.getPageSize(), rows.iterator().next().getLong(0), admins));
                    })
                    .onComplete(_ -> connection.close())
                    .onSuccess(sink::success)
                    .onFailure(sink::error)
            ));
        });
    }

    private <T> Future<T> recoverUniqueIndexException(Throwable throwable) {
        SQLException sqlException = ExceptionUtil.extractException(throwable, SQLException.class);
        if (sqlException != null && "23505".equals(sqlException.getSQLState())) {
            if (sqlException.getMessage().contains("idx_unique_admin_username")) {
                return Future.failedFuture(new StandardStatusException("user.username.exists", "用户名已存在"));
            }
            if (sqlException.getMessage().contains("idx_unique_admin_phone")) {
                return Future.failedFuture(new StandardStatusException("user.phone.exists", "手机号已存在"));
            }
            if (sqlException.getMessage().contains("idx_unique_admin_email")) {
                return Future.failedFuture(new StandardStatusException("user.email.exists", "邮箱已存在"));
            }
        }
        return Future.failedFuture(throwable);
    }

    /**
     * 管理员添加
     *
     * @param addDtoMono 添加参数
     * @param operator   操作员信息
     * @return 新添加的管理员信息
     * @throws StandardStatusException 影响状态错误异常
     */
    public Mono<AdminDto> add(Mono<AdminAddDto> addDtoMono, AdminAuthDetails operator) {
        return addDtoMono.flatMap(addDto -> Mono.create(sink -> pool.getConnection().flatMap(connection -> connection.begin()
                .compose(_ -> roleRepository.getForUpdate(connection, addDto.getRoleId()).flatMap(role -> role == null ? Future.failedFuture(new StandardStatusException("角色不存在")) : Future.succeededFuture(role)))
                .compose(role -> {
                    Admin admin = new Admin();
                    admin.setRoleId(role.getId());
                    admin.setUsername(addDto.getUsername());
                    admin.setPassword(passwordEncoder.encode(addDto.getPassword()));
                    admin.setPhone(ObjectUtils.getIfNull(addDto.getPhone(), ""));
                    admin.setEmail(ObjectUtils.getIfNull(addDto.getEmail(), ""));
                    admin.setNickname(addDto.getNickname());
                    admin.setAvatar(ObjectUtils.getIfNull(addDto.getAvatar(), ""));
                    admin.setState(State.enable.value());
                    admin.initOperation(addDto.getRemark(), operator.getUsername());

                    return adminRepository.insert(connection, admin)
                            .recover(this::recoverUniqueIndexException)
                            .compose(id -> {
                                admin.setId(id);
                                admin.setRole(role);
                                return operationRecordRepository.add(connection, "add", "admin", admin.getId().toString(), operator);
                            })
                            .compose(_ -> connection.transaction().commit())
                            .onComplete(_ -> connection.close())
                            .map(_ -> new AdminDto(admin));
                })
                .onSuccess(sink::success)
                .onFailure(sink::error)
        )));
    }

    /**
     * 管理员修改
     *
     * @param updateDtoMono 管理中修改信息
     * @param operator      操作员
     * @return 修改后的管理信息
     */
    public Mono<AdminDto> update(Mono<AdminUpdateDto> updateDtoMono, AdminAuthDetails operator) {
        return updateDtoMono.flatMap(updateDto -> {
            if (DEFAULT_ADMIN_ID.equals(updateDto.getId())) {
                return Mono.error(new StandardStatusException("默认管理员禁止修改"));
            }

            Map<Field<?>, Object> values = new LinkedHashMap<>();
            return Mono.create(sink -> pool.getConnection().flatMap(connection -> connection.begin()
                    .compose(_ -> adminRepository.getForUpdate(connection, updateDto.getId()).flatMap(admin -> admin == null ? Future.failedFuture(new StandardStatusException("管理员不存在")) : Future.succeededFuture(admin)))
                    .compose(admin -> {
                        if (updateDto.getRoleId() == null) {
                            return Future.succeededFuture(admin);
                        } else {
                            return roleRepository.getForUpdate(connection, updateDto.getRoleId()).flatMap(role -> {
                                if (role == null) {
                                    return Future.failedFuture(new StandardStatusException("角色不存在"));
                                }

                                if (State.disable(role.getState())) {
                                    return Future.failedFuture(new StandardStatusException("角色已禁用"));
                                }

                                admin.setRoleId(role.getId());
                                admin.setRole(role);
                                values.put(Tables.ADMIN.ROLE_ID, admin.getRoleId());
                                return Future.succeededFuture(admin);
                            });
                        }
                    })
                    .compose(admin -> {
                        if (updateDto.getPassword() != null) {
                            admin.setPassword(passwordEncoder.encode(updateDto.getPassword()));
                            values.put(Tables.ADMIN.PASSWORD, admin.getPassword());
                        }

                        if (updateDto.getPhone() != null) {
                            admin.setPhone(updateDto.getPhone());
                            values.put(Tables.ADMIN.PHONE, admin.getPhone());
                        }

                        if (updateDto.getEmail() != null) {
                            admin.setEmail(updateDto.getEmail());
                            values.put(Tables.ADMIN.EMAIL, admin.getEmail());
                        }

                        if (updateDto.getNickname() != null) {
                            admin.setNickname(updateDto.getNickname());
                            values.put(Tables.ADMIN.NICKNAME, admin.getNickname());
                        }

                        if (updateDto.getAvatar() != null) {
                            admin.setAvatar(updateDto.getAvatar());
                            values.put(Tables.ADMIN.AVATAR, admin.getAvatar());
                        }

                        admin.updateOperation(updateDto.getRemark(), operator.getUsername());
                        values.put(Tables.ADMIN.REMARK, admin.getRemark());
                        values.put(Tables.ADMIN.UPDATE_BY, admin.getUpdateBy());
                        values.put(Tables.ADMIN.UPDATE_TIME, admin.getUpdateTime());

                        Update<?> query = dslContext.update(Tables.ADMIN).set(values).where(Tables.ADMIN.ID.eq(admin.getId()));
                        return connection.preparedQuery(query.getSQL()).execute(Tuple.tuple(query.getBindValues()))
                                .recover(this::recoverUniqueIndexException)
                                .compose(_ -> operationRecordRepository.add(connection, "update", "admin", admin.getId().toString(), operator))
                                .compose(_ -> connection.transaction().commit())
                                .onComplete(_ -> connection.close())
                                .map(_ -> new AdminDto(admin));
                    })
                    .onSuccess(sink::success)
                    .onFailure(sink::error)
            ));
        });
    }

    /**
     * 管理员删除，默认管理员禁止删除
     *
     * @param deleteDtoMono 删除参数
     * @throws StandardStatusException 影响状态错误异常
     */
    public Mono<Void> delete(Mono<MultipleIdDto> deleteDtoMono, AdminAuthDetails operator) {
        return deleteDtoMono.flatMap(deleteDto -> {
            Set<Long> ids = new HashSet<>(deleteDto.getIds());
            if (ids.contains(DEFAULT_ADMIN_ID)) {
                return Mono.error(new StandardStatusException("默认管理员禁止删除"));
            }

            return Mono.create(sink -> {
                Set<String> keys = ids.stream().map(AdminAuthUtil::buildDetailsKey).collect(Collectors.toSet());

                Request request = Request.cmd(Command.EXISTS);
                keys.forEach(request::arg);

                redis.send(request)
                        .map(response -> response.toLong() > 0)
                        .flatMap(exists -> {
                            if (exists) {
                                return Future.failedFuture(new StandardStatusException("无法删除已登录的管理员"));
                            }

                            Delete<?> query = dslContext.deleteFrom(Tables.ADMIN).where(Tables.ADMIN.ID.in(ids));
                            return pool.getConnection().flatMap(connection -> connection.begin()
                                    .compose(_ -> connection.preparedQuery(query.getSQL()).execute(Tuple.tuple(query.getBindValues())))
                                    .compose(_ -> operationRecordRepository.add(connection, "delete", "admin", ids.stream().map(Object::toString).collect(Collectors.joining(",")), operator))
                                    .compose(_ -> connection.transaction().commit())
                                    .onComplete(_ -> connection.close())
                            );
                        })
                        .onSuccess(sink::success)
                        .onFailure(sink::error);
            });
        });
    }

    /**
     * 管理员状态修改(启用/禁用)，默认管理员禁止禁用
     *
     * @param changeDtoMono 修改参数
     * @param operator      操作员
     */
    public Mono<Void> changeState(Mono<ChangeStateDto> changeDtoMono, AdminAuthDetails operator) {
        return changeDtoMono.flatMap(changeDto -> {
            Set<Long> ids = new HashSet<>(changeDto.getIds());
            if (changeDto.getState() == State.disable.value() && ids.contains(DEFAULT_ADMIN_ID)) {
                return Mono.error(new StandardStatusException("默认管理员禁止禁用"));
            }

            State state = State.valueOf(changeDto.getState());

            Update<?> query = dslContext.update(Tables.ADMIN).set(Tables.ADMIN.STATE, state.value()).set(Tables.ADMIN.UPDATE_BY, operator.getUsername()).set(Tables.ADMIN.UPDATE_TIME, OffsetDateTime.now()).where(Tables.ADMIN.ID.in(ids));
            return Mono.create(sink -> pool.getConnection().flatMap(connection -> connection.begin()
                    .compose(_ -> connection.preparedQuery(query.getSQL()).execute(Tuple.tuple(query.getBindValues())))
                    .compose(_ -> operationRecordRepository.add(connection, "changeState", "admin", ids.stream().map(Object::toString).collect(Collectors.joining(",")), operator))
                    .compose(_ -> connection.transaction().commit())
                    .onComplete(_ -> connection.close())
                    .onSuccess(sink::success)
                    .onFailure(sink::error)
            ));
        });
    }
}
