package com.sk.rts.application.service;

import com.sk.rts.application.auth.AdminAuthDetails;
import com.sk.rts.application.dto.*;
import com.sk.rts.application.entity.Menu;
import com.sk.rts.application.entity.Role;
import com.sk.rts.application.entity.RoleAuthority;
import com.sk.rts.application.entity.enums.Operation;
import com.sk.rts.application.entity.enums.State;
import com.sk.rts.application.exception.ExceptionUtil;
import com.sk.rts.application.exception.ResponseStatus;
import com.sk.rts.application.exception.StandardStatusException;
import com.sk.rts.application.jooq.Tables;
import com.sk.rts.application.jooq.tables.TableMenu;
import com.sk.rts.application.jooq.tables.TableRoleAuthority;
import com.sk.rts.application.repository.AdminRepository;
import com.sk.rts.application.repository.OperationRecordRepository;
import com.sk.rts.application.repository.RoleRepository;
import io.vertx.core.Future;
import io.vertx.sqlclient.Pool;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import io.vertx.sqlclient.Tuple;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jooq.*;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.Comparator;
import java.util.stream.Collectors;

/**
 * 角色服务类
 * <p>提供角色相关的业务逻辑处理，包括角色的增删改查、状态管理和权限分配等功能</p>
 */
@Slf4j
@Service
@NullMarked
@AllArgsConstructor
public class RoleService {

    // 默认角色的ID，默认角色禁止删除或者禁用
    protected static final Long DEFAULT_ROLE_ID = 1L;

    private final Pool pool;
    private final DSLContext dslContext;

    private final RoleRepository roleRepository;
    private final AdminRepository adminRepository;
    private final OperationRecordRepository operationRecordRepository;

    /**
     * 角色选择列表，适用于前端下拉选择列表
     *
     * @return 角色选择列表
     */
    public Mono<List<RoleOptionDto>> roleSelectList() {
        Select<?> query = dslContext.select(Tables.ROLE.ID, Tables.ROLE.NAME).from(Tables.ROLE);
        return Flux.<RoleOptionDto>create(sink -> pool.query(query.getSQL()).execute()
                .onFailure(sink::error)
                .onSuccess(rows -> {
                    for (Row row : rows) {
                        sink.next(new RoleOptionDto(row.getLong(0), row.getString(1)));
                    }
                    sink.complete();
                })
        ).collectList();
    }

    /**
     * 角色分页查询, ID升序
     *
     * @param pageRequestDtoMono 分页查询参数
     * @return 分页查询结果
     */
    public Mono<PageResultDto<RoleDto>> query(Mono<PageQueryDto<@Nullable RoleQueryDto>> pageRequestDtoMono) {
        return pageRequestDtoMono.flatMap(pageRequestDto -> {
            RoleQueryDto queryDto = pageRequestDto.getQuery();

            Select<?> pageQuery = dslContext.select(
                            Tables.ROLE.ID,
                            Tables.ROLE.NAME,
                            Tables.ROLE.CODE,
                            Tables.ROLE.STATE,
                            Tables.ROLE.REMARK,
                            Tables.ROLE.CREATE_BY,
                            Tables.ROLE.CREATE_TIME,
                            Tables.ROLE.UPDATE_BY,
                            Tables.ROLE.UPDATE_TIME)
                    .from(Tables.ROLE);
            Select<?> countQuery = dslContext.selectCount().from(Tables.ROLE);

            List<Condition> conditions = new ArrayList<>();

            if (queryDto != null) {
                if (queryDto.getName() != null) {
                    conditions.add(Tables.ROLE.NAME.like("%" + queryDto.getName() + "%"));
                }
                if (queryDto.getCode() != null) {
                    conditions.add(Tables.ROLE.CODE.like("%" + queryDto.getCode() + "%"));
                }
            }

            if (!conditions.isEmpty()) {
                pageQuery = ((SelectWhereStep<?>) pageQuery).where(conditions);
                countQuery = ((SelectWhereStep<?>) countQuery).where(conditions);
            }

            if (pageRequestDto.getSort() == null) {
                pageQuery = ((SelectOrderByStep<?>) pageQuery).orderBy(Tables.ROLE.ID.desc());
            } else {
                OrderField<?> sortField = Tables.ROLE.field(pageRequestDto.getSort());
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
                    .map(rows -> rows.stream().map(row -> new RoleDto(Role.fromRow(row))).toList())
                    .flatMap(roles -> {
                        if (roles.size() < pageRequestDto.getPageSize()) {
                            return Future.succeededFuture(new PageResultDto<>(pageRequestDto.getPageNo(), pageRequestDto.getPageSize(), (pageRequestDto.getPageSize().longValue() * (pageRequestDto.getPageNo().longValue() - 1)) + roles.size(), roles));
                        }

                        return connection.preparedQuery(countSql).execute(Tuple.tuple(countArgs)).map(rows -> new PageResultDto<>(pageRequestDto.getPageNo(), pageRequestDto.getPageSize(), rows.iterator().next().getLong(0), roles));
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
            if (sqlException.getMessage().contains("idx_unique_role_name")) {
                return Future.failedFuture(new StandardStatusException("role.name.exists", "角色名称已存在"));
            }
            if (sqlException.getMessage().contains("idx_unique_role_code")) {
                return Future.failedFuture(new StandardStatusException("role.code.exists", "角色代码已存在"));
            }
        }
        return Future.failedFuture(throwable);
    }

    /**
     * 角色添加
     *
     * @param addDtoMono 添加参数
     * @param operator   操作员
     * @return 新添加的角色信息
     * @throws StandardStatusException 影响状态错误异常
     */
    public Mono<RoleDto> add(Mono<RoleAddDto> addDtoMono, AdminAuthDetails operator) {
        return addDtoMono.flatMap(addDto -> Mono.create(sink -> pool.getConnection().flatMap(connection -> connection.begin()
                .compose(_ -> {
                    Role role = new Role();
                    role.setName(addDto.getName());
                    role.setCode(addDto.getCode());
                    role.setState(State.enable.value());
                    role.initOperation(addDto.getRemark(), operator.getUsername());

                    return roleRepository.insert(connection, role)
                            .recover(this::recoverUniqueIndexException)
                            .compose(id -> {
                                role.setId(id);
                                return operationRecordRepository.add(connection, Operation.add, role.getId().toString(), "role", operator);
                            })
                            .compose(_ -> connection.transaction().commit())
                            .onComplete(_ -> connection.close())
                            .map(_ -> new RoleDto(role));
                })
                .onSuccess(sink::success)
                .onFailure(sink::error)
        )));
    }

    /**
     * 角色修改
     *
     * @param updateDtoMono 修改参数
     * @param operator      操作员
     * @return 修改后的角色信息
     */
    public Mono<RoleDto> update(Mono<RoleUpdateDto> updateDtoMono, AdminAuthDetails operator) {
        return updateDtoMono.flatMap(updateDto -> {
            if (DEFAULT_ROLE_ID.equals(updateDto.getId())) {
                return Mono.error(new StandardStatusException("默认角色禁止修改"));
            }

            Map<Field<?>, Object> values = new HashMap<>();
            return Mono.create(sink -> pool.getConnection().flatMap(connection -> connection.begin()
                    .compose(_ -> roleRepository.getForUpdate(connection, updateDto.getId()).flatMap(role -> role == null ? Future.failedFuture(new StandardStatusException("角色不存在")) : Future.succeededFuture(role)))
                    .compose(role -> {
                        if (updateDto.getName() != null && !updateDto.getName().equals(role.getName())) {
                            role.setName(updateDto.getName());
                            values.put(Tables.ROLE.NAME, role.getName());
                        }

                        if (updateDto.getCode() != null && !updateDto.getCode().equals(role.getCode())) {
                            role.setCode(updateDto.getCode());
                            values.put(Tables.ROLE.CODE, role.getCode());
                        }

                        role.updateOperation(updateDto.getRemark(), operator.getUsername());
                        values.put(Tables.ROLE.REMARK, updateDto.getRemark());
                        values.put(Tables.ROLE.UPDATE_BY, role.getUpdateBy());
                        values.put(Tables.ROLE.UPDATE_TIME, role.getUpdateTime());

                        Update<?> query = dslContext.update(Tables.ROLE).set(values).where(Tables.ROLE.ID.eq(role.getId()));

                        return connection.preparedQuery(query.getSQL()).execute(Tuple.tuple(query.getBindValues()))
                                .recover(this::recoverUniqueIndexException)
                                .compose(_ -> operationRecordRepository.add(connection, Operation.update, role.getId().toString(), "role", operator))
                                .compose(_ -> connection.transaction().commit())
                                .onComplete(_ -> connection.close())
                                .map(_ -> new RoleDto(role));
                    })
                    .onSuccess(sink::success)
                    .onFailure(sink::error)
            ));
        });
    }

    /**
     * 角色删除，默认角色禁止删除
     *
     * @param deleteDtoMono 删除参数
     */
    public Mono<Void> delete(Mono<MultipleIdDto> deleteDtoMono, AdminAuthDetails operator) {
        return deleteDtoMono.flatMap(deleteDto -> {
            Set<Long> ids = new HashSet<>(deleteDto.getIds());
            if (ids.contains(DEFAULT_ROLE_ID)) {
                return Mono.error(new StandardStatusException("默认角色禁止删除"));
            }

            Delete<?> query = dslContext.deleteFrom(Tables.ROLE).where(Tables.ROLE.ID.in(ids));

            return Mono.create(sink -> pool.getConnection().flatMap(connection -> connection.begin()
                    .compose(_ -> adminRepository.existsByRoleIdIn(connection, ids))
                    .compose(exists -> {
                        if (exists) {
                            return Future.failedFuture(new StandardStatusException("请先删除角色下的管理员"));
                        }

                        return connection.preparedQuery(query.getSQL()).execute(Tuple.tuple(query.getBindValues()));
                    })
                    .compose(_ -> operationRecordRepository.add(connection, Operation.delete, ids.stream().map(Object::toString).collect(Collectors.joining(",")), "role", operator))
                    .compose(_ -> connection.transaction().commit())
                    .onComplete(_ -> connection.close())
                    .onSuccess(sink::success)
                    .onFailure(sink::error)
            ));
        });
    }

    /**
     * 角色状态修改(启用/禁用)，默认角色禁止禁用
     *
     * @param changeDtoMono 修改参数
     * @param operator      操作员
     */
    public Mono<Void> changeState(Mono<ChangeStateDto> changeDtoMono, AdminAuthDetails operator) {
        return changeDtoMono.flatMap(changeDto -> {
            Set<Long> ids = new HashSet<>(changeDto.getIds());
            if (State.disable(changeDto.getState()) && ids.contains(DEFAULT_ROLE_ID)) {
                return Mono.error(new StandardStatusException("默认角色禁止禁用"));
            }

            State state = State.valueOf(changeDto.getState());

            Update<?> query = dslContext.update(Tables.ROLE).set(Tables.ROLE.STATE, state.value()).set(Tables.ROLE.UPDATE_BY, operator.getUsername()).set(Tables.ROLE.UPDATE_TIME, OffsetDateTime.now()).where(Tables.ROLE.ID.in(ids));

            return Mono.create(sink -> pool.getConnection().flatMap(connection -> connection.begin()
                    .compose(_ -> connection.preparedQuery(query.getSQL()).execute(Tuple.tuple(query.getBindValues())))
                    .compose(_ -> operationRecordRepository.add(connection, Operation.changeState, changeDto.getIds().stream().map(Object::toString).collect(Collectors.joining(",")), "role," + state.name(), operator))
                    .compose(_ -> connection.transaction().commit())
                    .onComplete(_ -> connection.close())
                    .onSuccess(sink::success)
                    .onFailure(sink::error))
            );
        });
    }

    /**
     * 查询角色菜单权限
     *
     * @param id 角色ID
     * @return 角色菜单权限树
     */
    public Mono<List<RoleAuthorityDto>> getAuthorities(Long id) {
        TableMenu m = Tables.MENU.as("m");
        TableRoleAuthority a = Tables.ROLE_AUTHORITY.as("a");

        Select<?> query = dslContext.select(
                        m.ID,
                        m.PARENT_ID,
                        m.NAME,
                        m.SORT_BY,
                        a.ROLE_ID)
                .from(m)
                .leftJoin(a).on(a.MENU_ID.eq(m.ID)).and(a.ROLE_ID.eq(id))
                .where(m.STATE.eq(State.enable.value()))
                .orderBy(m.ID.asc());
        return Mono.<List<RoleAuthorityDto>>create(sink -> pool.preparedQuery(query.getSQL()).execute(Tuple.tuple(query.getBindValues()))
                .map(rows -> {
                    Map<Long, RoleAuthorityDto> authorityMap = new HashMap<>();
                    for (Row row : rows) {
                        Menu menu = new Menu();
                        menu.setId(row.getLong(0));
                        menu.setParentId(row.getLong(1));
                        menu.setName(row.getString(2));
                        menu.setSortBy(row.getInteger(3));

                        authorityMap.put(menu.getId(), new RoleAuthorityDto(menu, row.getLong(4) != null));
                    }

                    List<RoleAuthorityDto> authorityList = new ArrayList<>();
                    authorityMap.forEach((_, authority) -> {
                        if (authority.getParentId() == 0L) {
                            authorityList.add(authority);
                        } else {
                            RoleAuthorityDto parent = authorityMap.get(authority.getParentId());
                            if (parent != null) {
                                parent.getSubMenus().add(authority);
                            }
                        }
                    });

                    MenuSortableDto.sort(authorityList);
                    return authorityList;
                })
                .onSuccess(sink::success)
                .onFailure(sink::error)
        );
    }

    /**
     * 设置角色权限，默认角色的默认菜单不能被取消
     *
     * @param setDtoMono 角色菜单权限参数
     */
    public Mono<Void> setAuthorities(Mono<RoleAuthoritySetDto> setDtoMono, AdminAuthDetails operator) {
        return setDtoMono.flatMap(setDto -> Mono.create(sink -> pool.getConnection().flatMap(connection -> connection.begin()
                .compose(_ -> roleRepository.getForUpdate(connection, setDto.getRoleId()))
                .compose(role -> {
                    if (role == null) {
                        return Future.failedFuture(new StandardStatusException("角色不存在"));
                    }

                    Set<Long> menuIds = new HashSet<>(setDto.getMenuIds());

                    if (DEFAULT_ROLE_ID.equals(role.getId())) {
                        if (!menuIds.containsAll(MenuService.SYSTEM_MENU_IDS)) {
                            return Future.failedFuture(new StandardStatusException("默认角色不能取消系统菜单"));
                        }
                    }

                    Delete<?> deleteQuery = dslContext.deleteFrom(Tables.ROLE_AUTHORITY).where(Tables.ROLE_AUTHORITY.ROLE_ID.eq(setDto.getRoleId()));
                    Insert<?> insertQuery = dslContext.insertInto(Tables.ROLE_AUTHORITY, Tables.ROLE_AUTHORITY.ROLE_ID, Tables.ROLE_AUTHORITY.MENU_ID);

                    for (Long menuId : menuIds) {
                        insertQuery = ((InsertValuesStep2<?, Long, Long>) insertQuery).values(role.getId(), menuId);
                    }

                    String deleteSql = deleteQuery.getSQL();
                    String insertSql = insertQuery.getSQL();
                    List<Object> deleteArgs = deleteQuery.getBindValues();
                    List<Object> insertArgs = insertQuery.getBindValues();
                    return connection.preparedQuery(deleteSql).execute(Tuple.tuple(deleteArgs))
                            .compose(_ -> connection.preparedQuery(insertSql).execute(Tuple.tuple(insertArgs)))
                            .compose(_ -> operationRecordRepository.add(connection, Operation.setAuthorities, setDto.getRoleId().toString(), setDto.getMenuIds().stream().map(Object::toString).collect(Collectors.joining(",")), operator))
                            .compose(_ -> connection.transaction().commit());
                })
                .onComplete(_ -> connection.close())
                .onSuccess(sink::success)
                .onFailure(sink::error)
        )));
    }

    /**
     * 角色菜单权限比较
     *
     * @param idsDtoMono 角色ID列表
     * @return 菜单权限比较列表
     */
    public Mono<RoleAuthorityComparisonListDto> compareAuthorities(Mono<MultipleIdDto> idsDtoMono) {
        return idsDtoMono.flatMap(idsDto -> Mono.create(sink -> {
            Set<Long> roleIds = new HashSet<>(idsDto.getIds());

            Select<?> roleQuery = dslContext.select(Tables.ROLE.ID, Tables.ROLE.NAME).from(Tables.ROLE).where(Tables.ROLE.ID.in(roleIds));
            Select<?> authorityQuery = dslContext.select(Tables.ROLE_AUTHORITY.ID, Tables.ROLE_AUTHORITY.ROLE_ID, Tables.ROLE_AUTHORITY.MENU_ID).from(Tables.ROLE_AUTHORITY).where(Tables.ROLE_AUTHORITY.ROLE_ID.in(roleIds));
            Select<?> menuQuery = dslContext.select(Tables.MENU.ID, Tables.MENU.PARENT_ID, Tables.MENU.TYPE, Tables.MENU.NAME, Tables.MENU.ICON, Tables.MENU.SORT_BY).from(Tables.MENU).where(Tables.MENU.STATE.eq(State.enable.value())).orderBy(Tables.MENU.ID.asc());

            Future<RowSet<Row>> roleQueryFuture = pool.preparedQuery(roleQuery.getSQL()).execute(Tuple.tuple(roleQuery.getBindValues()));
            Future<RowSet<Row>> authorityQueryFuture = pool.preparedQuery(authorityQuery.getSQL()).execute(Tuple.tuple(authorityQuery.getBindValues()));
            Future<RowSet<Row>> menuQueryFuture = pool.preparedQuery(menuQuery.getSQL()).execute(Tuple.tuple(menuQuery.getBindValues()));

            Future.all(roleQueryFuture, authorityQueryFuture, menuQueryFuture).onFailure(sink::error).onSuccess(ar -> {
                RowSet<Row> roleRows = ar.resultAt(0);
                List<RoleOptionDto> roleList = new ArrayList<>();
                for (Row row : roleRows) {
                    Role role = new Role();
                    role.setId(row.getLong(0));
                    role.setName(row.getString(1));

                    roleList.add(new RoleOptionDto(role));
                }

                RowSet<Row> authorityRows = ar.resultAt(1);
                Map<Long, Set<Long>> menuRolesMap = new HashMap<>();
                for (Row row : authorityRows) {
                    RoleAuthority authority = new RoleAuthority();
                    authority.setId(row.getLong(0));
                    authority.setRoleId(row.getLong(1));
                    authority.setMenuId(row.getLong(2));

                    menuRolesMap.computeIfAbsent(authority.getMenuId(), _ -> new HashSet<>()).add(authority.getRoleId());
                }

                Map<Long, RoleAuthorityComparisonDto> comparisonMap = new HashMap<>();
                RowSet<Row> menuRows = ar.resultAt(2);
                for (Row row : menuRows) {
                    Menu menu = new Menu();
                    menu.setId(row.getLong(0));
                    menu.setParentId(row.getLong(1));
                    menu.setType(row.getInteger(2));
                    menu.setName(row.getString(3));
                    menu.setIcon(row.getString(4));
                    menu.setSortBy(row.getInteger(5));

                    Set<Long> menuRoles = menuRolesMap.get(menu.getId());

                    comparisonMap.put(menu.getId(), new RoleAuthorityComparisonDto(menu, menuRoles != null && menuRoles.size() != roleIds.size(), menuRoles));
                }

                List<RoleAuthorityComparisonDto> comparisonList = new ArrayList<>();
                comparisonMap.forEach((_, comparison) -> {
                    if (comparison.getParentId() == 0L) {
                        comparisonList.add(comparison);
                    } else {
                        RoleAuthorityComparisonDto parent = comparisonMap.get(comparison.getParentId());
                        if (parent != null) {
                            parent.getSubMenus().add(comparison);
                        }
                    }
                });

                roleList.sort(Comparator.comparingLong(RoleOptionDto::getId).reversed());
                MenuSortableDto.sort(comparisonList);

                sink.success(new RoleAuthorityComparisonListDto(roleList, comparisonList));
            });
        }));
    }

    /**
     * 角色菜单列表
     *
     * @param authDetails 管理员认证信息
     * @return 角色菜单列表
     */
    public Mono<List<RoleMenuDto>> roleMenus(AdminAuthDetails authDetails) {
        TableRoleAuthority a = Tables.ROLE_AUTHORITY.as("a");
        TableMenu m = Tables.MENU.as("m");

        Select<?> query = dslContext.select(a.ID, a.ROLE_ID, a.MENU_ID, m.ID, m.PARENT_ID, m.TYPE, m.NAME, m.ICON, m.PATH, m.SORT_BY).from(a).innerJoin(m).on(m.ID.eq(a.MENU_ID)).where(a.ROLE_ID.eq(authDetails.getRoleId())).and(m.STATE.eq(State.enable.value()));
        return Mono.create(sink -> pool.getConnection().flatMap(connection -> connection.preparedQuery(query.getSQL())
                .execute(Tuple.tuple(query.getBindValues()))
                .map(rows -> {
                    Map<Long, RoleMenuDto> menuMap = new HashMap<>(rows.size());
                    for (Row row : rows) {
                        RoleAuthority authority = new RoleAuthority();
                        authority.setId(row.getLong(0));
                        authority.setRoleId(row.getLong(1));
                        authority.setMenuId(row.getLong(2));

                        authority.setMenu(new Menu());
                        authority.getMenu().setId(row.getLong(3));
                        authority.getMenu().setParentId(row.getLong(4));
                        authority.getMenu().setType(row.getInteger(5));
                        authority.getMenu().setName(row.getString(6));
                        authority.getMenu().setIcon(row.getString(7));
                        authority.getMenu().setPath(row.getString(8));
                        authority.getMenu().setSortBy(row.getInteger(9));

                        menuMap.put(authority.getMenu().getId(), new RoleMenuDto(authority.getMenu()));
                    }

                    return menuMap;
                })
                .onComplete(_ -> connection.close())
                .map(menuMap -> {
                    List<RoleMenuDto> menus = new ArrayList<>();
                    menuMap.forEach((_, menu) -> {
                        if (menu.getParentId() == 0L) {
                            menus.add(menu);
                        } else {
                            RoleMenuDto parent = menuMap.get(menu.getParentId());
                            if (parent != null) {
                                parent.getSubMenus().add(menu);
                            }
                        }
                    });

                    MenuSortableDto.sort(menus);
                    return menus;
                })
                .onSuccess(sink::success)
                .onFailure(sink::error)
        ));
    }
}
