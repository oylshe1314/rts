package com.sk.rts.application.service;

import com.sk.rts.application.auth.AdminAuthDetails;
import com.sk.rts.application.dto.*;
import com.sk.rts.application.entity.AdminMenu;
import com.sk.rts.application.entity.AdminRole;
import com.sk.rts.application.entity.AdminRoleAuthority;
import com.sk.rts.application.entity.enums.Operation;
import com.sk.rts.application.entity.enums.State;
import com.sk.rts.application.exception.ExceptionUtil;
import com.sk.rts.application.exception.ResponseStatus;
import com.sk.rts.application.exception.StandardStatusException;
import com.sk.rts.application.jooq.Tables;
import com.sk.rts.application.jooq.tables.TableAdminMenu;
import com.sk.rts.application.jooq.tables.TableAdminRoleAuthority;
import com.sk.rts.application.repository.AdminOperationRecordRepository;
import com.sk.rts.application.repository.AdminUserRepository;
import com.sk.rts.application.repository.RoleRepository;
import com.sk.rts.application.util.ReactiveUtil;
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
public class AdminRoleService {

    // 默认角色的ID，默认角色禁止删除或者禁用
    protected static final Long DEFAULT_ROLE_ID = 1L;

    private final Pool pool;
    private final DSLContext dslContext;

    private final RoleRepository roleRepository;
    private final AdminUserRepository adminRepository;
    private final AdminOperationRecordRepository operationRecordRepository;

    /**
     * 角色选择列表，适用于前端下拉选择列表
     *
     * @return 角色选择列表
     */
    public Mono<List<RoleOptionDto>> roleSelectList() {
        Select<?> query = dslContext.select(Tables.ADMIN_ROLE.ID, Tables.ADMIN_ROLE.NAME).from(Tables.ADMIN_ROLE);
        Future<List<RoleOptionDto>> future = pool.withConnection(connection -> connection.query(query.getSQL()).execute().map(rows -> rows.stream().map(row -> new RoleOptionDto(row.getLong(0), row.getString(1))).toList()));
        return ReactiveUtil.toMono(future);
    }

    /**
     * 角色分页查询
     *
     * @param pageQueryDto 分页查询参数
     * @return 分页查询结果
     */
    public Mono<PageResultDto<AdminRoleDto>> query(PageQueryDto<@Nullable RoleQueryDto> pageQueryDto) {
        RoleQueryDto queryDto = pageQueryDto.getQuery();

        Select<?> pageQuery = dslContext.select(
                        Tables.ADMIN_ROLE.ID,
                        Tables.ADMIN_ROLE.NAME,
                        Tables.ADMIN_ROLE.CODE,
                        Tables.ADMIN_ROLE.STATE,
                        Tables.ADMIN_ROLE.REMARK,
                        Tables.ADMIN_ROLE.CREATE_BY,
                        Tables.ADMIN_ROLE.CREATE_TIME,
                        Tables.ADMIN_ROLE.UPDATE_BY,
                        Tables.ADMIN_ROLE.UPDATE_TIME)
                .from(Tables.ADMIN_ROLE);
        Select<?> countQuery = dslContext.selectCount().from(Tables.ADMIN_ROLE);

        List<Condition> conditions = new ArrayList<>();

        if (queryDto != null) {
            if (queryDto.getName() != null) {
                conditions.add(Tables.ADMIN_ROLE.NAME.like("%" + queryDto.getName() + "%"));
            }
            if (queryDto.getCode() != null) {
                conditions.add(Tables.ADMIN_ROLE.CODE.like("%" + queryDto.getCode() + "%"));
            }
        }

        if (!conditions.isEmpty()) {
            pageQuery = ((SelectWhereStep<?>) pageQuery).where(conditions);
            countQuery = ((SelectWhereStep<?>) countQuery).where(conditions);
        }

        if (pageQueryDto.getSort() == null) {
            pageQuery = ((SelectOrderByStep<?>) pageQuery).orderBy(Tables.ADMIN_ROLE.ID.desc());
        } else {
            OrderField<?> sortField = Tables.ADMIN_ROLE.field(pageQueryDto.getSort());
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
        Future<PageResultDto<AdminRoleDto>> future = pool.withConnection(connection -> connection.preparedQuery(pageSql).execute(Tuple.tuple(pageArgs))
                .map(rows -> rows.stream().map(row -> new AdminRoleDto(AdminRole.fromRow(row))).toList())
                .flatMap(roles -> {
                    if (roles.size() < pageQueryDto.getPageSize()) {
                        return Future.succeededFuture(new PageResultDto<>(pageQueryDto.getPageNo(), pageQueryDto.getPageSize(), (pageQueryDto.getPageSize().longValue() * (pageQueryDto.getPageNo().longValue() - 1)) + roles.size(), roles));
                    }

                    return connection.preparedQuery(countSql).execute(Tuple.tuple(countArgs)).map(rows -> new PageResultDto<>(pageQueryDto.getPageNo(), pageQueryDto.getPageSize(), rows.iterator().next().getLong(0), roles));
                })
        );
        return ReactiveUtil.toMono(future);
    }

    private <T> Future<T> recoverUniqueIndexException(Throwable throwable) {
        SQLException sqlException = ExceptionUtil.extractException(throwable, SQLException.class);
        if (sqlException != null) {
            if (sqlException.getSQLState().startsWith("23")) {
                if (sqlException.getMessage().contains("idx_unique_role_name")) {
                    return Future.failedFuture(new StandardStatusException("role.name.exists", "角色名称已存在"));
                }
                if (sqlException.getMessage().contains("idx_unique_role_code")) {
                    return Future.failedFuture(new StandardStatusException("role.code.exists", "角色代码已存在"));
                }
            }
        }
        return Future.failedFuture(throwable);
    }

    /**
     * 角色添加
     *
     * @param addDto   添加参数
     * @param operator 操作员
     * @return 新添加的角色信息
     * @throws StandardStatusException 影响状态错误异常
     */
    public Mono<AdminRoleDto> add(AdminRoleAddDto addDto, AdminAuthDetails operator) {
        AdminRole role = new AdminRole();
        role.setName(addDto.getName());
        role.setCode(addDto.getCode());
        role.setState(State.enable.value());
        role.initOperation(addDto.getRemark(), operator.getUsername());

        Future<AdminRoleDto> future = pool.withTransaction(connection -> roleRepository.insert(connection, role)
                .compose(id -> {
                    role.setId(id);
                    return operationRecordRepository.add(connection, Operation.add, role.getId().toString(), "role", operator);
                })
                .map(_ -> new AdminRoleDto(role))
                .recover(this::recoverUniqueIndexException)
        );
        return ReactiveUtil.toMono(future);
    }

    /**
     * 角色修改
     *
     * @param updateDto 修改参数
     * @param operator  操作员
     * @return 修改后的角色信息
     */
    public Mono<AdminRoleDto> update(RoleUpdateDto updateDto, AdminAuthDetails operator) {
        if (DEFAULT_ROLE_ID.equals(updateDto.getId())) {
            return Mono.error(new StandardStatusException("默认角色禁止修改"));
        }

        Map<Field<?>, Object> values = new HashMap<>();
        Future<AdminRoleDto> future = pool.withTransaction(connection -> roleRepository.getForUpdate(connection, updateDto.getId())
                .flatMap(menu -> {
                    if (menu == null) {
                        return Future.failedFuture(new StandardStatusException("角色不存在"));
                    }
                    return Future.succeededFuture(menu);
                })
                .compose(role -> {
                    if (updateDto.getName() != null && !updateDto.getName().equals(role.getName())) {
                        role.setName(updateDto.getName());
                        values.put(Tables.ADMIN_ROLE.NAME, role.getName());
                    }

                    if (updateDto.getCode() != null && !updateDto.getCode().equals(role.getCode())) {
                        role.setCode(updateDto.getCode());
                        values.put(Tables.ADMIN_ROLE.CODE, role.getCode());
                    }

                    role.updateOperation(updateDto.getRemark(), operator.getUsername());
                    values.put(Tables.ADMIN_ROLE.REMARK, updateDto.getRemark());
                    values.put(Tables.ADMIN_ROLE.UPDATE_BY, role.getUpdateBy());
                    values.put(Tables.ADMIN_ROLE.UPDATE_TIME, role.getUpdateTime());

                    Update<?> query = dslContext.update(Tables.ADMIN_ROLE).set(values).where(Tables.ADMIN_ROLE.ID.eq(role.getId()));
                    return connection.preparedQuery(query.getSQL()).execute(Tuple.tuple(query.getBindValues()))
                            .compose(_ -> operationRecordRepository.add(connection, Operation.update, role.getId().toString(), "role", operator))
                            .map(_ -> new AdminRoleDto(role))
                            .recover(this::recoverUniqueIndexException);
                })
        );
        return ReactiveUtil.toMono(future);
    }

    /**
     * 角色删除，默认角色禁止删除
     *
     * @param deleteDto 删除参数
     */
    public Mono<Void> delete(MultipleIdDto deleteDto, AdminAuthDetails operator) {
        Set<Long> ids = new HashSet<>(deleteDto.getIds());
        if (ids.contains(DEFAULT_ROLE_ID)) {
            return Mono.error(new StandardStatusException("默认角色禁止删除"));
        }

        Delete<?> query = dslContext.deleteFrom(Tables.ADMIN_ROLE).where(Tables.ADMIN_ROLE.ID.in(ids));
        Future<Void> future = pool.withTransaction(connection -> adminRepository.existsByRoleIdIn(connection, ids)
                .compose(exists -> {
                    if (exists) {
                        return Future.failedFuture(new StandardStatusException("请先删除角色下的管理员"));
                    }

                    return connection.preparedQuery(query.getSQL()).execute(Tuple.tuple(query.getBindValues()));
                })
                .compose(_ -> operationRecordRepository.add(connection, Operation.delete, ids.stream().map(Object::toString).collect(Collectors.joining(",")), "role", operator))
        );
        return ReactiveUtil.toMono(future);
    }

    /**
     * 角色状态修改(启用/禁用)，默认角色禁止禁用
     *
     * @param changeDto 修改参数
     * @param operator  操作员
     */
    public Mono<Void> changeState(StateChangeDto changeDto, AdminAuthDetails operator) {
        Set<Long> ids = new HashSet<>(changeDto.getIds());
        if (State.disable(changeDto.getState()) && ids.contains(DEFAULT_ROLE_ID)) {
            return Mono.error(new StandardStatusException("默认角色禁止禁用"));
        }

        State state = State.valueOf(changeDto.getState());

        Update<?> query = dslContext.update(Tables.ADMIN_ROLE).set(Tables.ADMIN_ROLE.STATE, state.value()).set(Tables.ADMIN_ROLE.UPDATE_BY, operator.getUsername()).set(Tables.ADMIN_ROLE.UPDATE_TIME, OffsetDateTime.now()).where(Tables.ADMIN_ROLE.ID.in(ids));
        Future<Void> future = pool.withConnection(connection -> connection.preparedQuery(query.getSQL()).execute(Tuple.tuple(query.getBindValues()))
                .compose(_ -> operationRecordRepository.add(connection, Operation.changeState, changeDto.getIds().stream().map(Object::toString).collect(Collectors.joining(",")), "role," + state.name(), operator))
        );
        return ReactiveUtil.toMono(future);
    }

    /**
     * 查询角色菜单权限
     *
     * @param id 角色ID
     * @return 角色菜单权限树
     */
    public Mono<List<RoleAuthorityDto>> getAuthorities(Long id) {
        TableAdminMenu m = Tables.ADMIN_MENU.as("m");
        TableAdminRoleAuthority a = Tables.ADMIN_ROLE_AUTHORITY.as("a");

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
        Future<List<RoleAuthorityDto>> future = pool.withConnection(connection -> connection.preparedQuery(query.getSQL()).execute(Tuple.tuple(query.getBindValues()))
                .map(rows -> {
                    Map<Long, RoleAuthorityDto> authorityMap = new HashMap<>();
                    for (Row row : rows) {
                        AdminMenu menu = new AdminMenu();
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
        );
        return ReactiveUtil.toMono(future);
    }

    /**
     * 设置角色权限，默认角色的默认菜单不能被取消
     *
     * @param setDto 角色菜单权限参数
     */
    public Mono<Void> setAuthorities(RoleAuthoritySetDto setDto, AdminAuthDetails operator) {
        Future<Void> future = pool.withTransaction(connection -> roleRepository.getForUpdate(connection, setDto.getRoleId())
                .compose(role -> {
                    if (role == null) {
                        return Future.failedFuture(new StandardStatusException("角色不存在"));
                    }

                    Set<Long> menuIds = new HashSet<>(setDto.getMenuIds());

                    if (DEFAULT_ROLE_ID.equals(role.getId())) {
                        if (!menuIds.containsAll(AdminMenuService.SYSTEM_MENU_IDS)) {
                            return Future.failedFuture(new StandardStatusException("默认角色不能取消系统菜单"));
                        }
                    }

                    Delete<?> deleteQuery = dslContext.deleteFrom(Tables.ADMIN_ROLE_AUTHORITY).where(Tables.ADMIN_ROLE_AUTHORITY.ROLE_ID.eq(setDto.getRoleId()));
                    Insert<?> insertQuery = dslContext.insertInto(Tables.ADMIN_ROLE_AUTHORITY, Tables.ADMIN_ROLE_AUTHORITY.ROLE_ID, Tables.ADMIN_ROLE_AUTHORITY.MENU_ID);

                    for (Long menuId : menuIds) {
                        insertQuery = ((InsertValuesStep2<?, Long, Long>) insertQuery).values(role.getId(), menuId);
                    }

                    String deleteSql = deleteQuery.getSQL();
                    String insertSql = insertQuery.getSQL();
                    List<Object> deleteArgs = deleteQuery.getBindValues();
                    List<Object> insertArgs = insertQuery.getBindValues();
                    return connection.preparedQuery(deleteSql).execute(Tuple.tuple(deleteArgs))
                            .compose(_ -> connection.preparedQuery(insertSql).execute(Tuple.tuple(insertArgs)))
                            .compose(_ -> operationRecordRepository.add(connection, Operation.setAuthorities, role.getId().toString(), setDto.getMenuIds().stream().map(Object::toString).collect(Collectors.joining(",")), operator));
                })
        );
        return ReactiveUtil.toMono(future);
    }

    /**
     * 角色菜单权限比较
     *
     * @param idsDto 角色ID列表
     * @return 菜单权限比较列表
     */
    public Mono<RoleAuthorityComparisonListDto> compareAuthorities(MultipleIdDto idsDto) {
        Set<Long> roleIds = new LinkedHashSet<>(idsDto.getIds());

        Select<?> roleQuery = dslContext.select(Tables.ADMIN_ROLE.ID, Tables.ADMIN_ROLE.NAME).from(Tables.ADMIN_ROLE).where(Tables.ADMIN_ROLE.ID.in(roleIds));
        Select<?> authorityQuery = dslContext.select(Tables.ADMIN_ROLE_AUTHORITY.ID, Tables.ADMIN_ROLE_AUTHORITY.ROLE_ID, Tables.ADMIN_ROLE_AUTHORITY.MENU_ID).from(Tables.ADMIN_ROLE_AUTHORITY).where(Tables.ADMIN_ROLE_AUTHORITY.ROLE_ID.in(roleIds));
        Select<?> menuQuery = dslContext.select(Tables.ADMIN_MENU.ID, Tables.ADMIN_MENU.PARENT_ID, Tables.ADMIN_MENU.TYPE, Tables.ADMIN_MENU.NAME, Tables.ADMIN_MENU.ICON, Tables.ADMIN_MENU.SORT_BY).from(Tables.ADMIN_MENU).where(Tables.ADMIN_MENU.STATE.eq(State.enable.value())).orderBy(Tables.ADMIN_MENU.ID.asc());
        Future<RoleAuthorityComparisonListDto> future = pool.withConnection(connection -> {
            Future<RowSet<Row>> roleQueryFuture = connection.preparedQuery(roleQuery.getSQL()).execute(Tuple.tuple(roleQuery.getBindValues()));
            Future<RowSet<Row>> authorityQueryFuture = connection.preparedQuery(authorityQuery.getSQL()).execute(Tuple.tuple(authorityQuery.getBindValues()));
            Future<RowSet<Row>> menuQueryFuture = connection.preparedQuery(menuQuery.getSQL()).execute(Tuple.tuple(menuQuery.getBindValues()));

            return Future.all(roleQueryFuture, authorityQueryFuture, menuQueryFuture).map(compositeFuture -> {
                RowSet<Row> roleRows = compositeFuture.resultAt(0);
                List<RoleOptionDto> roleList = new ArrayList<>();
                for (Row row : roleRows) {
                    AdminRole role = new AdminRole();
                    role.setId(row.getLong(0));
                    role.setName(row.getString(1));

                    roleList.add(new RoleOptionDto(role));
                }

                RowSet<Row> authorityRows = compositeFuture.resultAt(1);
                Map<Long, Set<Long>> menuRolesMap = new HashMap<>();
                for (Row row : authorityRows) {
                    AdminRoleAuthority authority = new AdminRoleAuthority();
                    authority.setId(row.getLong(0));
                    authority.setRoleId(row.getLong(1));
                    authority.setMenuId(row.getLong(2));

                    menuRolesMap.computeIfAbsent(authority.getMenuId(), _ -> new HashSet<>()).add(authority.getRoleId());
                }

                Map<Long, RoleAuthorityComparisonDto> comparisonMap = new HashMap<>();
                RowSet<Row> menuRows = compositeFuture.resultAt(2);
                for (Row row : menuRows) {
                    AdminMenu menu = new AdminMenu();
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
                return new RoleAuthorityComparisonListDto(roleList, comparisonList);
            });
        });
        return ReactiveUtil.toMono(future);
    }

    /**
     * 角色菜单列表
     *
     * @param authDetails 管理员认证信息
     * @return 角色菜单列表
     */
    public Mono<List<AdminRoleMenuDto>> roleMenus(AdminAuthDetails authDetails) {
        TableAdminRoleAuthority a = Tables.ADMIN_ROLE_AUTHORITY.as("a");
        TableAdminMenu m = Tables.ADMIN_MENU.as("m");

        Select<?> query = dslContext.select(a.ID, a.ROLE_ID, a.MENU_ID, m.ID, m.PARENT_ID, m.TYPE, m.NAME, m.ICON, m.PATH, m.SORT_BY).from(a).innerJoin(m).on(m.ID.eq(a.MENU_ID)).where(a.ROLE_ID.eq(authDetails.getRoleId())).and(m.STATE.eq(State.enable.value()));
        Future<List<AdminRoleMenuDto>> future = pool.withConnection(connection -> connection.preparedQuery(query.getSQL()).execute(Tuple.tuple(query.getBindValues()))
                .map(rows -> {
                    Map<Long, AdminRoleMenuDto> menuMap = new HashMap<>(rows.size());
                    for (Row row : rows) {
                        AdminRoleAuthority authority = new AdminRoleAuthority();
                        authority.setId(row.getLong(0));
                        authority.setRoleId(row.getLong(1));
                        authority.setMenuId(row.getLong(2));

                        authority.setMenu(new AdminMenu());
                        authority.getMenu().setId(row.getLong(3));
                        authority.getMenu().setParentId(row.getLong(4));
                        authority.getMenu().setType(row.getInteger(5));
                        authority.getMenu().setName(row.getString(6));
                        authority.getMenu().setIcon(row.getString(7));
                        authority.getMenu().setPath(row.getString(8));
                        authority.getMenu().setSortBy(row.getInteger(9));

                        menuMap.put(authority.getMenu().getId(), new AdminRoleMenuDto(authority.getMenu()));
                    }

                    List<AdminRoleMenuDto> menus = new ArrayList<>();
                    menuMap.forEach((_, menu) -> {
                        if (menu.getParentId() == 0L) {
                            menus.add(menu);
                        } else {
                            AdminRoleMenuDto parent = menuMap.get(menu.getParentId());
                            if (parent != null) {
                                parent.getSubMenus().add(menu);
                            }
                        }
                    });

                    MenuSortableDto.sort(menus);
                    return menus;
                })
        );
        return ReactiveUtil.toMono(future);
    }
}
