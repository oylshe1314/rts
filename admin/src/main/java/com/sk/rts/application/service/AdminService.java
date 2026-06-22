package com.sk.rts.application.service;

import com.sk.rts.application.auth.AdminAuthDetails;
import com.sk.rts.application.dto.*;
import com.sk.rts.application.entity.Admin;
import com.sk.rts.application.entity.Role;
import com.sk.rts.application.entity.enums.Status;
import com.sk.rts.application.exception.ResponseStatus;
import com.sk.rts.application.exception.StandardStatusException;
import com.sk.rts.application.jooq.Tables;
import com.sk.rts.application.jooq.tables.TableAdmin;
import com.sk.rts.application.jooq.tables.TableRole;
import com.sk.rts.application.repository.AdminRepository;
import com.sk.rts.application.repository.OperationRecordRepository;
import com.sk.rts.application.repository.RoleRepository;
import io.vertx.core.Future;
import io.vertx.sqlclient.Pool;
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
    public Mono<List<AdminSelectDto>> adminSelectList(@Nullable Long roleId) {
        SelectWhereStep<?> query = dslContext.select(Tables.ADMIN.ID, Tables.ADMIN.ROLE_ID, Tables.ADMIN.USERNAME, Tables.ADMIN.NICKNAME, Tables.ADMIN.AVATAR).from(Tables.ADMIN);
        if (roleId != null) {
            query.where(Tables.ADMIN.ROLE_ID.eq(roleId));
        }

        return Flux.<AdminSelectDto>create(sink -> pool.getConnection().flatMap(connection -> connection.preparedQuery(query.getSQL()).execute(Tuple.tuple(query.getBindValues())).onComplete(_ -> connection.close()))
                .onFailure(sink::error)
                .onSuccess(rows -> {
                    rows.forEach(row -> sink.next(new AdminSelectDto(row.getLong(0), row.getLong(1), row.getString(2), row.getString(3), row.getString(4))));
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
    public Mono<PageResultDto<AdminDto>> query(Mono<PageQueryDto<AdminQueryDto>> pageRequestDtoMono) {
        return pageRequestDtoMono.flatMap(pageRequestDto -> {
            AdminQueryDto queryDto = pageRequestDto.getQuery();

            TableAdmin a = Tables.ADMIN.as("a");
            TableRole r = Tables.ROLE.as("r");

            SelectWhereStep<?> pageQuery = dslContext.select(a.ID, a.ROLE_ID, a.USERNAME, a.PASSWORD, a.PHONE, a.EMAIL, a.NICKNAME, a.AVATAR, a.STATUS, a.REMARK, a.CREATE_BY, a.CREATE_TIME, a.UPDATE_BY, a.UPDATE_TIME, r.ID, r.NAME).from(a).innerJoin(r).on(r.ID.eq(a.ROLE_ID));
            SelectWhereStep<?> countQuery = dslContext.selectCount().from(a).innerJoin(r).on(r.ID.eq(a.ROLE_ID));

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

            if (conditions.size() > 0) {
                pageQuery.where(conditions);
                countQuery.where(conditions);
            }

            if (pageRequestDto.getSort() == null) {
                pageQuery.orderBy(a.ID.asc());
            } else {
                if (Boolean.TRUE.equals(pageRequestDto.getDesc())) {
                    pageQuery.orderBy(a.field(pageRequestDto.getSort()).desc());
                } else {
                    pageQuery.orderBy(a.field(pageRequestDto.getSort()));
                }
            }

            pageQuery.offset(pageRequestDto.getOffset()).limit(pageRequestDto.getPageSize());

            String sql = pageQuery.getSQL();
            log.debug("SQL: {}", sql);
            return Mono.create(sink -> pool.getConnection().flatMap(connection -> connection.preparedQuery(sql).execute(Tuple.of(pageQuery.getBindValues()))
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

                        return connection.preparedQuery(countQuery.getSQL()).execute(Tuple.tuple(countQuery.getBindValues()))
                                .map(rows -> new PageResultDto<>(pageRequestDto.getPageNo(), pageRequestDto.getPageSize(), rows.iterator().next().getLong(0), admins));
                    })
                    .onComplete(_ -> connection.close())
                    .onSuccess(sink::success)
                    .onFailure(sink::error)
            ));
        });
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
                .compose(_ -> adminRepository.existsByUsername(connection, addDto.getUsername()).flatMap(exists -> exists ? Future.failedFuture(new StandardStatusException("用户名已存在")) : Future.succeededFuture()))
                .compose(_ -> adminRepository.existsByNickname(connection, addDto.getNickname()).flatMap(exists -> exists ? Future.failedFuture(new StandardStatusException("昵称已存在")) : Future.succeededFuture()))
                .compose(_ -> roleRepository.getForUpdate(connection, addDto.getRoleId()).flatMap(role -> role == null ? Future.failedFuture(new StandardStatusException("角色不存在")) : Future.succeededFuture(role)).flatMap(role -> !Status.enable(role.getStatus()) ? Future.failedFuture(new StandardStatusException("角色已禁用")) : Future.succeededFuture(role)))
                .compose(role -> {
                    Admin admin = new Admin();
                    admin.setRoleId(role.getId());
                    admin.setUsername(addDto.getUsername());
                    admin.setPassword(passwordEncoder.encode(addDto.getPassword()));
                    admin.setPhone(ObjectUtils.getIfNull(addDto.getPhone(), ""));
                    admin.setEmail(ObjectUtils.getIfNull(addDto.getEmail(), ""));
                    admin.setNickname(addDto.getNickname());
                    admin.setAvatar(ObjectUtils.getIfNull(addDto.getAvatar(), ""));
                    admin.setStatus(Status.enable.value());
                    admin.initOperation(addDto.getRemark(), operator.getUsername());

                    admin.setRole(role);

                    return adminRepository.insert(connection, admin)
                            .compose(id -> {
                                admin.setId(id);
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

                                if (!Status.enable(role.getStatus())) {
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
                        if (updateDto.getNickname() == null || updateDto.getNickname().equals(admin.getNickname())) {
                            return Future.succeededFuture(admin);
                        }

                        return adminRepository.existsByNickname(connection, updateDto.getNickname()).flatMap(exists -> {
                            if (exists) {
                                return Future.failedFuture(new StandardStatusException("昵称已存在"));
                            }

                            admin.setNickname(updateDto.getNickname());
                            values.put(Tables.ADMIN.NICKNAME, admin.getNickname());
                            return Future.succeededFuture(admin);
                        });
                    })
                    .compose(admin -> {
                        if (updateDto.getPassword() != null) {
                            String password = passwordEncoder.encode(updateDto.getPassword());
                            if (password == null) {
                                return Future.failedFuture(new StandardStatusException(ResponseStatus.internal_error));
                            }

                            admin.setPassword(password);
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

                        if (updateDto.getAvatar() != null) {
                            admin.setAvatar(updateDto.getAvatar());
                            values.put(Tables.ADMIN.AVATAR, admin.getAvatar());
                        }

                        admin.updateOperation(updateDto.getRemark(), operator.getUsername());
                        values.put(Tables.ADMIN.REMARK, admin.getRemark());
                        values.put(Tables.ADMIN.UPDATE_BY, admin.getUpdateBy());
                        values.put(Tables.ADMIN.UPDATE_TIME, admin.getUpdateTime());

                        UpdateConditionStep<?> query = dslContext.update(Tables.ADMIN).set(values).where(Tables.ADMIN.ID.eq(admin.getId()));

                        String sql = query.getSQL();
                        log.debug("SQL: {}", sql);
                        return connection.preparedQuery(sql).execute(Tuple.tuple(query.getBindValues()))
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

            DeleteConditionStep<?> query = dslContext.deleteFrom(Tables.ADMIN).where(Tables.ADMIN.ID.in(ids));

            return Mono.create(sink -> pool.getConnection().flatMap(connection -> connection.begin()
                    .compose(_ -> connection.preparedQuery(query.getSQL()).execute(Tuple.tuple(query.getBindValues())))
                    .compose(_ -> operationRecordRepository.add(connection, "delete", "admin", ids.stream().map(Object::toString).collect(Collectors.joining(",")), operator))
                    .compose(_ -> connection.transaction().commit())
                    .onComplete(_ -> connection.close())
                    .onSuccess(sink::success)
                    .onFailure(sink::error)
            ));
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
            if (changeDto.getStatus() == Status.disable.value() && ids.contains(DEFAULT_ADMIN_ID)) {
                return Mono.error(new StandardStatusException("默认管理员禁止禁用"));
            }

            Status state = Status.valueOf(changeDto.getStatus());

            UpdateConditionStep<?> query = dslContext.update(Tables.ADMIN).set(Tables.ADMIN.STATUS, state.value()).set(Tables.ADMIN.UPDATE_BY, operator.getUsername()).set(Tables.ADMIN.UPDATE_TIME, OffsetDateTime.now()).where(Tables.ADMIN.ID.in(ids));

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
