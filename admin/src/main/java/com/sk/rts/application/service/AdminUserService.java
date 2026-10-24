package com.sk.rts.application.service;

import com.sk.rts.application.auth.AdminAuthDetails;
import com.sk.rts.application.auth.AdminAuthUtil;
import com.sk.rts.application.dto.*;
import com.sk.rts.application.entity.AdminRole;
import com.sk.rts.application.entity.AdminUser;
import com.sk.rts.application.entity.enums.Operation;
import com.sk.rts.application.entity.enums.State;
import com.sk.rts.application.exception.ExceptionUtil;
import com.sk.rts.application.exception.ResponseStatus;
import com.sk.rts.application.exception.StandardStatusException;
import com.sk.rts.application.jooq.Tables;
import com.sk.rts.application.jooq.tables.TableAdminRole;
import com.sk.rts.application.jooq.tables.TableAdminUser;
import com.sk.rts.application.repository.AdminOperationRecordRepository;
import com.sk.rts.application.repository.AdminRoleRepository;
import com.sk.rts.application.repository.AdminUserRepository;
import com.sk.rts.application.tools.UpdateRemarkBuilder;
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
import org.apache.commons.lang3.ObjectUtils;
import org.jooq.*;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Mono;

import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@NullMarked
@AllArgsConstructor
public class AdminUserService {

    // 默认管理员的ID，默认管理员禁止删除或者禁用。
    private static final Long DEFAULT_ADMIN_ID = 1L;

    private final Pool pool;
    private final DSLContext dslContext;

    private final Redis redis;

    private final AdminUserRepository adminUserRepository;
    private final AdminRoleRepository adminRoleRepository;
    private final AdminOperationRecordRepository adminOperationRecordRepository;

    private final PasswordEncoder passwordEncoder;

    /**
     * 获取管理员选择列表
     *
     * @param username 用户名
     * @return 管理员选择列表
     */
    public Mono<List<AdminOptionDto>> adminSelectList(@Nullable String username) {
        Select<?> query = dslContext.select(
                        Tables.ADMIN_USER.ID,
                        Tables.ADMIN_USER.ROLE_ID,
                        Tables.ADMIN_USER.USERNAME,
                        Tables.ADMIN_USER.NICKNAME,
                        Tables.ADMIN_USER.AVATAR,
                        Tables.ADMIN_ROLE.ID,
                        Tables.ADMIN_ROLE.NAME)
                .from(Tables.ADMIN_USER)
                .innerJoin(Tables.ADMIN_ROLE).on(Tables.ADMIN_ROLE.ID.eq(Tables.ADMIN_USER.ROLE_ID));
        if (StringUtils.hasText(username)) {
            query = ((SelectWhereStep<?>) query).where(Tables.ADMIN_USER.USERNAME.like("%" + username + "%"));
        }

        query = ((SelectLimitStep<?>) query).limit(20);

        String sql = query.getSQL();
        List<Object> args = query.getBindValues();
        Future<List<AdminOptionDto>> future = pool.withConnection(connection -> connection.preparedQuery(sql).execute(Tuple.tuple(args)).map(rows -> rows.stream().map(row -> {
                    AdminUser admin = new AdminUser();
                    admin.setId(row.getLong(0));
                    admin.setRoleId(row.getLong(1));
                    admin.setUsername(row.getString(2));
                    admin.setNickname(row.getString(3));
                    admin.setAvatar(row.getString(4));

                    admin.setRole(new AdminRole());
                    admin.getRole().setId(row.getLong(5));
                    admin.getRole().setName(row.getString(6));

                    return new AdminOptionDto(admin);
                }).toList()
        ));
        return ReactiveUtil.toMono(future);
    }

    /**
     * 管理员分页查询
     *
     * @param pageQueryDto 分页查询参数
     * @return 分页查询结果
     */
    public Mono<PageResultDto<AdminUserDto>> query(PageQueryDto<@Nullable AdminQueryDto> pageQueryDto) {
        AdminQueryDto queryDto = pageQueryDto.getQuery();

        TableAdminUser u = Tables.ADMIN_USER.as("u");
        TableAdminRole r = Tables.ADMIN_ROLE.as("r");

        Select<?> pageQuery = dslContext.select(
                        u.ID,
                        u.ROLE_ID,
                        u.USERNAME,
                        u.PASSWORD,
                        u.PHONE,
                        u.EMAIL,
                        u.NICKNAME,
                        u.AVATAR,
                        u.STATE,
                        u.REMARK,
                        u.CREATE_BY,
                        u.CREATE_TIME,
                        u.UPDATE_BY,
                        u.UPDATE_TIME,
                        r.ID,
                        r.NAME)
                .from(u).
                innerJoin(r).on(r.ID.eq(u.ROLE_ID));
        Select<?> countQuery = dslContext.selectCount().from(u).innerJoin(r).on(r.ID.eq(u.ROLE_ID));

        List<Condition> conditions = new ArrayList<>();
        if (queryDto != null) {
            if (queryDto.getRoleId() != null) {
                conditions.add(u.ROLE_ID.eq(queryDto.getRoleId()));
            }
            if (queryDto.getUsername() != null) {
                conditions.add(u.USERNAME.like("%" + queryDto.getUsername() + "%"));
            }
            if (queryDto.getPhone() != null) {
                conditions.add(u.PHONE.like("%" + queryDto.getPhone() + "%"));
            }
            if (queryDto.getEmail() != null) {
                conditions.add(u.EMAIL.like("%" + queryDto.getEmail() + "%"));
            }
        }

        if (conditions.size() > 0) {
            pageQuery = ((SelectWhereStep<?>) pageQuery).where(conditions);
            countQuery = ((SelectWhereStep<?>) countQuery).where(conditions);
        }

        if (pageQueryDto.getSort() == null) {
            pageQuery = ((SelectOrderByStep<?>) pageQuery).orderBy(u.ID.desc());
        } else {
            OrderField<?> sortField = u.field(pageQueryDto.getSort());
            if (sortField == null) {
                return Mono.error(new StandardStatusException(ResponseStatus.parameter_error));
            }

            if (Boolean.TRUE.equals(pageQueryDto.getDesc())) {
                sortField = ((Field<?>) sortField).desc();
            }

            pageQuery = ((SelectOrderByStep<?>) pageQuery).orderBy(sortField);
        }

        pageQuery = ((SelectLimitStep<?>) pageQuery).offset(pageQueryDto.getOffset()).limit(pageQueryDto.getPageSize());

        String pageSql = pageQuery.getSQL();
        String countSql = countQuery.getSQL();
        List<Object> pageArgs = pageQuery.getBindValues();
        List<Object> countArgs = countQuery.getBindValues();
        Future<PageResultDto<AdminUserDto>> future = pool.withConnection(connection -> connection.preparedQuery(pageSql).execute(Tuple.tuple(pageArgs))
                .flatMap(rows -> {
                    Set<Long> ids = new HashSet<>();
                    List<AdminUserDto> dtoList = new ArrayList<>();
                    for (Row row : rows) {
                        AdminUser admin = AdminUser.fromRow(row);
                        admin.setRole(new AdminRole());
                        admin.getRole().setId(row.getLong(14));
                        admin.getRole().setName(row.getString(15));
                        ids.add(admin.getId());
                        dtoList.add(new AdminUserDto(admin));
                    }

                    Request request = Request.cmd(Command.SMISMEMBER);
                    request.arg(AdminAuthUtil.buildLoggedIdsKey());
                    ids.forEach(request::arg);

                    return redis.send(request).map(response -> {
                        for (int i = 0; i < dtoList.size(); i++) {
                            dtoList.get(i).setLoggedIn(Boolean.TRUE.equals(response.get(i).toBoolean()));
                        }
                        return dtoList;
                    });
                })
                .flatMap(admins -> {
                    if (admins.size() < pageQueryDto.getPageSize()) {
                        return Future.succeededFuture(new PageResultDto<>(pageQueryDto.getPageNo(), pageQueryDto.getPageSize(), (pageQueryDto.getPageSize().longValue() * (pageQueryDto.getPageNo().longValue() - 1)) + admins.size(), admins));
                    }
                    return connection.preparedQuery(countSql).execute(Tuple.tuple(countArgs)).map(rows -> new PageResultDto<>(pageQueryDto.getPageNo(), pageQueryDto.getPageSize(), rows.iterator().next().getLong(0), admins));
                })
        );
        return ReactiveUtil.toMono(future);
    }

    private <T> Future<T> recoverUniqueIndexException(Throwable throwable) {
        SQLException sqlException = ExceptionUtil.extractException(throwable, SQLException.class);
        if (sqlException != null) {
            if (sqlException.getSQLState().startsWith("23")) {
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
        }
        return Future.failedFuture(throwable);
    }

    /**
     * 管理员添加
     *
     * @param addDto   添加参数
     * @param operator 操作员信息
     * @return 新添加的管理员信息
     * @throws StandardStatusException 影响状态错误异常
     */
    public Mono<AdminUserDto> add(AdminAddDto addDto, AdminAuthDetails operator) {
        Future<AdminUserDto> future = pool.withTransaction(connection -> adminRoleRepository.getForUpdate(connection, addDto.getRoleId())
                .flatMap(role -> {
                    if (role == null) {
                        Future.failedFuture(new StandardStatusException("角色不存在"));
                    }
                    return Future.succeededFuture(role);
                })
                .compose(role -> {
                    AdminUser admin = new AdminUser();
                    admin.setRoleId(role.getId());
                    admin.setUsername(addDto.getUsername());
                    admin.setPassword(passwordEncoder.encode(addDto.getPassword()));
                    admin.setPhone(ObjectUtils.getIfNull(addDto.getPhone(), ""));
                    admin.setEmail(ObjectUtils.getIfNull(addDto.getEmail(), ""));
                    admin.setNickname(addDto.getNickname());
                    admin.setAvatar(ObjectUtils.getIfNull(addDto.getAvatar(), ""));
                    admin.setState(State.enable.value());
                    admin.initOperation(addDto.getRemark(), operator.getUsername());

                    admin.setRole(role);

                    return adminUserRepository.insert(connection, admin)
                            .compose(id -> {
                                admin.setId(id);
                                return adminOperationRecordRepository.add(connection, Operation.add, admin.getId().toString(), "admin_user", "", operator);
                            })
                            .map(_ -> new AdminUserDto(admin))
                            .recover(this::recoverUniqueIndexException);
                })
        );
        return ReactiveUtil.toMono(future);
    }

    /**
     * 管理员修改
     *
     * @param updateDto 管理中修改信息
     * @param operator  操作员
     * @return 修改后的管理信息
     */
    public Mono<AdminUserDto> update(AdminUpdateDto updateDto, AdminAuthDetails operator) {
        if (DEFAULT_ADMIN_ID.equals(updateDto.getId())) {
            return Mono.error(new StandardStatusException("默认管理员禁止修改"));
        }

        Map<Field<?>, Object> values = new LinkedHashMap<>();
        UpdateRemarkBuilder remarkBuilder = new UpdateRemarkBuilder();
        Future<AdminUserDto> future = pool.withTransaction(connection -> adminUserRepository.getForUpdate(connection, updateDto.getId())
                .flatMap(user -> {
                    if (user == null) {
                        return Future.failedFuture(new StandardStatusException("管理员不存在"));
                    }
                    return Future.succeededFuture(user);
                })
                .compose(user -> {
                    if (updateDto.getRoleId() == null) {
                        return Future.succeededFuture(user);
                    } else {
                        return adminRoleRepository.getForUpdate(connection, updateDto.getRoleId()).flatMap(role -> {
                            if (role == null) {
                                return Future.failedFuture(new StandardStatusException("角色不存在"));
                            }

                            if (State.disable(role.getState())) {
                                return Future.failedFuture(new StandardStatusException("角色已禁用"));
                            }

                            remarkBuilder.add("roleId", user.getRoleId(), updateDto.getRoleId());
                            user.setRoleId(role.getId());
                            user.setRole(role);
                            values.put(Tables.ADMIN_USER.ROLE_ID, user.getRoleId());
                            return Future.succeededFuture(user);
                        });
                    }
                })
                .compose(admin -> {
                    if (updateDto.getPassword() != null) {
                        String password = passwordEncoder.encode(updateDto.getPassword());
                        remarkBuilder.add("password", admin.getPassword(), password);
                        admin.setPassword(password);
                        values.put(Tables.ADMIN_USER.PASSWORD, admin.getPassword());
                    }

                    if (updateDto.getPhone() != null) {
                        remarkBuilder.add("phone", admin.getPhone(), updateDto.getPhone());
                        admin.setPhone(updateDto.getPhone());
                        values.put(Tables.ADMIN_USER.PHONE, admin.getPhone());
                    }

                    if (updateDto.getEmail() != null) {
                        remarkBuilder.add("email", admin.getEmail(), updateDto.getEmail());
                        admin.setEmail(updateDto.getEmail());
                        values.put(Tables.ADMIN_USER.EMAIL, admin.getEmail());
                    }

                    if (updateDto.getNickname() != null) {
                        remarkBuilder.add("nickname", admin.getNickname(), updateDto.getNickname());
                        admin.setNickname(updateDto.getNickname());
                        values.put(Tables.ADMIN_USER.NICKNAME, admin.getNickname());
                    }

                    if (updateDto.getAvatar() != null) {
                        remarkBuilder.add("avatar", admin.getAvatar(), updateDto.getAvatar());
                        admin.setAvatar(updateDto.getAvatar());
                        values.put(Tables.ADMIN_USER.AVATAR, admin.getAvatar());
                    }

                    admin.updateOperation(updateDto.getRemark(), operator.getUsername());
                    values.put(Tables.ADMIN_USER.REMARK, admin.getRemark());
                    values.put(Tables.ADMIN_USER.UPDATE_BY, admin.getUpdateBy());
                    values.put(Tables.ADMIN_USER.UPDATE_TIME, admin.getUpdateTime());

                    Update<?> query = dslContext.update(Tables.ADMIN_USER).set(values).where(Tables.ADMIN_USER.ID.eq(admin.getId()));
                    return connection.preparedQuery(query.getSQL()).execute(Tuple.tuple(query.getBindValues()))
                            .compose(_ -> adminOperationRecordRepository.add(connection, Operation.update, admin.getId().toString(), "admin_user", remarkBuilder.build(), operator))
                            .map(_ -> new AdminUserDto(admin))
                            .recover(this::recoverUniqueIndexException);
                })
        );
        return ReactiveUtil.toMono(future);
    }

    /**
     * 管理员删除，默认管理员禁止删除
     *
     * @param deleteDto 删除参数
     * @throws StandardStatusException 影响状态错误异常
     */
    public Mono<Void> delete(MultipleIdDto deleteDto, AdminAuthDetails operator) {
        Set<Long> ids = new HashSet<>(deleteDto.getIds());
        if (ids.contains(DEFAULT_ADMIN_ID)) {
            return Mono.error(new StandardStatusException("默认管理员禁止删除"));
        }

        Request request = Request.cmd(Command.SMISMEMBER);
        request.arg(AdminAuthUtil.buildLoggedIdsKey());
        deleteDto.getIds().forEach(request::arg);

        Future<Void> future = redis.send(request).flatMap(response -> {
            boolean exists = response.stream().anyMatch(item -> Boolean.TRUE.equals(item.toBoolean()));
            if (exists) {
                return Future.failedFuture(new StandardStatusException("无法删除已登录的管理员"));
            }

            Delete<?> query = dslContext.deleteFrom(Tables.ADMIN_USER).where(Tables.ADMIN_USER.ID.in(ids));
            return pool.withTransaction(connection -> connection.begin()
                    .compose(_ -> connection.preparedQuery(query.getSQL()).execute(Tuple.tuple(query.getBindValues())))
                    .compose(_ -> adminOperationRecordRepository.add(connection, Operation.delete, ids.stream().map(Object::toString).collect(Collectors.joining(",")), "admin_user", "", operator))
            );
        });
        return ReactiveUtil.toMono(future);
    }

    /**
     * 管理员状态修改(启用/禁用)，默认管理员禁止禁用
     *
     * @param changeDto 修改参数
     * @param operator  操作员
     */
    public Mono<Void> changeState(StateChangeDto changeDto, AdminAuthDetails operator) {
        Set<Long> ids = new HashSet<>(changeDto.getIds());
        if (changeDto.getState() == State.disable.value() && ids.contains(DEFAULT_ADMIN_ID)) {
            return Mono.error(new StandardStatusException("默认管理员禁止禁用"));
        }

        State state = State.valueOf(changeDto.getState());

        Update<?> query = dslContext.update(Tables.ADMIN_USER).set(Tables.ADMIN_USER.STATE, state.value()).set(Tables.ADMIN_USER.UPDATE_BY, operator.getUsername()).set(Tables.ADMIN_USER.UPDATE_TIME, OffsetDateTime.now()).where(Tables.ADMIN_USER.ID.in(ids));
        Future<Void> future = pool.withTransaction(connection -> connection.preparedQuery(query.getSQL()).execute(Tuple.tuple(query.getBindValues()))
                .compose(_ -> adminOperationRecordRepository.add(connection, Operation.changeState, ids.stream().map(Object::toString).collect(Collectors.joining(",")), "admin_user", state.name(), operator))
        );
        return ReactiveUtil.toMono(future);
    }
}
