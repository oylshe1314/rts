package com.sk.rts.application.service;

import com.sk.rts.application.auth.AdminAuthDetails;
import com.sk.rts.application.dto.*;
import com.sk.rts.application.entity.Menu;
import com.sk.rts.application.entity.Role;
import com.sk.rts.application.entity.RoleMenuAuthority;
import com.sk.rts.application.entity.enums.Status;
import com.sk.rts.application.exception.StandardStatusException;
import com.sk.rts.application.jooq.Tables;
import com.sk.rts.application.jooq.tables.TableMenu;
import com.sk.rts.application.jooq.tables.TableRoleMenuAuthority;
import com.sk.rts.application.repository.AdminRepository;
import com.sk.rts.application.repository.OperationRecordRepository;
import com.sk.rts.application.repository.RoleRepository;
import io.vertx.core.Future;
import io.vertx.sqlclient.Pool;
import io.vertx.sqlclient.Tuple;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jooq.*;
import org.jspecify.annotations.NullMarked;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.OffsetDateTime;
import java.util.*;
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

    // 数据库访问仓库
    private final Pool pool;
    // SQL构建
    private final DSLContext dslContext;
    // 角色仓库
    private final RoleRepository roleRepository;
    // 管理员仓库
    private final AdminRepository adminRepository;
    // 操作记录仓库
    private final OperationRecordRepository operationRecordRepository;

    /**
     * 角色选择列表，适用于前端下拉选择列表
     *
     * @return 角色选择列表
     */
    public Mono<List<RoleSelectDto>> roleSelectList() {
        SelectWhereStep<?> query = dslContext.select(Tables.ROLE.ID, Tables.ROLE.NAME).from(Tables.ROLE);
        return Flux.<RoleSelectDto>create(sink -> pool.getConnection().flatMap(connection -> connection.query(query.getSQL()).execute().onComplete(_ -> connection.close()))
                .onFailure(sink::error)
                .onSuccess(rows -> {
                    rows.forEach(row -> sink.next(new RoleSelectDto(row.getLong(0), row.getString(1))));
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
    public Mono<PageResultDto<RoleDto>> query(Mono<PageQueryDto<RoleQueryDto>> pageRequestDtoMono) {
        return pageRequestDtoMono.flatMap(pageRequestDto -> {
            RoleQueryDto queryDto = pageRequestDto.getQuery();

            SelectWhereStep<?> pageQuery = dslContext.select(Tables.ROLE.ID, Tables.ROLE.NAME, Tables.ROLE.STATUS, Tables.ROLE.REMARK, Tables.ROLE.CREATE_BY, Tables.ROLE.CREATE_TIME, Tables.ROLE.UPDATE_BY, Tables.ROLE.UPDATE_TIME).from(Tables.ROLE);
            SelectWhereStep<?> countQuery = dslContext.selectCount().from(Tables.ROLE);

            List<Condition> conditions = new ArrayList<>();

            if (queryDto != null) {
                if (queryDto.getName() != null) {
                    conditions.add(Tables.ROLE.NAME.like("%" + queryDto.getName() + "%"));
                }
            }

            if (conditions.size() > 0) {
                pageQuery.where(conditions);
                countQuery.where(conditions);
            }


            if (pageRequestDto.getSort() == null) {
                pageQuery.orderBy(Tables.ROLE.ID.asc());
            } else {
                if (Boolean.TRUE.equals(pageRequestDto.getDesc())) {
                    pageQuery.orderBy(Tables.ROLE.field(pageRequestDto.getSort()).desc());
                } else {
                    pageQuery.orderBy(Tables.ROLE.field(pageRequestDto.getSort()));
                }
            }

            pageQuery.offset(pageRequestDto.getOffset()).limit(pageRequestDto.getPageSize());

            String sql = pageQuery.getSQL();
            log.debug("SQL: {}", sql);
            return Mono.create(sink -> pool.getConnection().flatMap(connection -> connection.preparedQuery(sql)
                    .execute(Tuple.tuple(pageQuery.getBindValues()))
                    .map(rows -> rows.stream().map(row -> new RoleDto(Role.fromRow(row))).toList())
                    .flatMap(roles -> {
                        if (roles.size() < pageRequestDto.getPageSize()) {
                            return Future.succeededFuture(new PageResultDto<>(pageRequestDto.getPageNo(), pageRequestDto.getPageSize(), roles.size(), roles));
                        }

                        return connection.preparedQuery(countQuery.getSQL())
                                .execute(Tuple.tuple(countQuery.getBindValues()))
                                .map(rows -> new PageResultDto<>(pageRequestDto.getPageNo(), pageRequestDto.getPageSize(), rows.iterator().next().getLong(0), roles));
                    })
                    .onComplete(_ -> connection.close())
                    .onSuccess(sink::success)
                    .onFailure(sink::error)
            ));
        });
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
                .compose(_ -> roleRepository.existsByName(connection, addDto.getName()).flatMap(exists -> exists ? Future.failedFuture(new StandardStatusException("角色已存在")) : Future.succeededFuture()))
                .compose(_ -> {
                    Role role = new Role();
                    role.setName(addDto.getName());
                    role.setStatus(Status.enable.value());
                    role.initOperation(addDto.getRemark(), operator.getUsername());

                    InsertResultStep<?> query = dslContext.insertInto(Tables.ROLE, Tables.ROLE.NAME, Tables.ROLE.STATUS, Tables.ROLE.REMARK, Tables.ROLE.CREATE_BY, Tables.ROLE.CREATE_TIME, Tables.ROLE.UPDATE_BY, Tables.ROLE.UPDATE_TIME).values(role.getName(), role.getStatus(), role.getRemark(), role.getCreateBy(), role.getCreateTime(), role.getUpdateBy(), role.getUpdateTime()).returning(Tables.ROLE.ID);

                    String sql = query.getSQL();
                    log.debug("SQL: {}", sql);
                    return connection.preparedQuery(sql).execute(Tuple.tuple(query.getBindValues()))
                            .compose(rows -> {
                                role.setId(rows.iterator().next().getLong(0));
                                return operationRecordRepository.add(connection, "add", "role", role.getId().toString(), operator);
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
                        if (updateDto.getName() == null || updateDto.getName().equals(role.getName())) {
                            return Future.succeededFuture(role);
                        }

                        return roleRepository.existsByName(connection, updateDto.getName()).flatMap(exists -> {
                            if (exists) {
                                return Future.failedFuture(new StandardStatusException("角色已存在"));
                            }

                            role.setName(updateDto.getName());
                            values.put(Tables.ROLE.NAME, role.getName());

                            return Future.succeededFuture(role);
                        });
                    })
                    .compose(role -> {
                        role.updateOperation(updateDto.getRemark(), operator.getUsername());
                        values.put(Tables.ROLE.REMARK, updateDto.getRemark());
                        values.put(Tables.ROLE.UPDATE_BY, role.getUpdateBy());
                        values.put(Tables.ROLE.UPDATE_TIME, role.getUpdateTime());

                        UpdateConditionStep<?> query = dslContext.update(Tables.ROLE).set(values).where(Tables.ROLE.ID.eq(role.getId()));

                        String sql = query.getSQL();
                        log.debug("SQL: {}", sql);
                        return connection.preparedQuery(sql).execute(Tuple.tuple(query.getBindValues()))
                                .compose(_ -> operationRecordRepository.add(connection, "update", "role", role.getId().toString(), operator))
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

            DeleteConditionStep<?> query = dslContext.deleteFrom(Tables.ROLE).where(Tables.ROLE.ID.in(ids));

            return Mono.create(sink -> pool.getConnection().flatMap(connection -> connection.begin()
                    .compose(_ -> adminRepository.existsByRoleIdIn(connection, ids))
                    .compose(exists -> {
                        if (exists) {
                            return Future.failedFuture(new StandardStatusException("请先删除角色下的管理员"));
                        }

                        return connection.preparedQuery(query.getSQL()).execute(Tuple.tuple(query.getBindValues()));
                    })
                    .compose(_ -> operationRecordRepository.add(connection, "delete", "role", ids.stream().map(Object::toString).collect(Collectors.joining(",")), operator))
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
            if (changeDto.getStatus() == Status.disable.value() && ids.contains(DEFAULT_ROLE_ID)) {
                return Mono.error(new StandardStatusException("默认角色禁止禁用"));
            }

            Status state = Status.valueOf(changeDto.getStatus());

            UpdateConditionStep<?> query = dslContext.update(Tables.ROLE).set(Tables.ROLE.STATUS, state.value()).set(Tables.ROLE.UPDATE_BY, operator.getUsername()).set(Tables.ROLE.UPDATE_TIME, OffsetDateTime.now()).where(Tables.ROLE.ID.in(ids));

            return Mono.create(sink -> pool.getConnection().flatMap(connection -> connection.begin()
                    .compose(_ -> connection.preparedQuery(query.getSQL()).execute(Tuple.tuple(query.getBindValues())))
                    .compose(_ -> operationRecordRepository.add(connection, "changeState", "role", changeDto.getIds().stream().map(Object::toString).collect(Collectors.joining(",")), operator))
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
    public Mono<List<RoleMenuAuthorityDto>> getAuthorities(Long id) {
        TableMenu m = Tables.MENU.as("m");
        TableRoleMenuAuthority a = Tables.ROLE_MENU_AUTHORITY.as("a");

        SelectConditionStep<?> query = dslContext.select(m.ID, m.PARENT_ID, m.NAME, m.SORT_BY, a.ROLE_ID).from(m).leftJoin(a).on(a.MENU_ID.eq(m.ID)).and(a.ROLE_ID.eq(id)).where(m.STATUS.eq(Status.enable.value()));

        String sql = query.getSQL();
        log.debug("SQL: {}", sql);
        return Mono.<List<RoleMenuAuthorityDto>>create(sink -> pool.getConnection().flatMap(connection -> connection.preparedQuery(sql)
                .execute(Tuple.tuple(query.getBindValues()))
                .map(rows -> rows.stream().map(row -> {
                            Menu menu = new Menu();
                            menu.setId(row.getLong(0));
                            menu.setParentId(row.getLong(1));
                            menu.setName(row.getString(2));
                            menu.setSortBy(row.getInteger(3));
                            boolean checked = row.getLong(4) != null;

                            return new RoleMenuAuthorityDto(menu, checked);
                        }).toList()
                )
                .onComplete(_ -> connection.close())
                .onSuccess(sink::success)
                .onFailure(sink::error)
        )).map(authorities -> {
            MenuSortableDto.sort(authorities);
            return authorities;
        });
    }

    /**
     * 设置角色权限，默认角色的默认菜单不能被取消
     *
     * @param setDtoMono 角色菜单权限参数
     */
    public Mono<Void> setAuthorities(Mono<RoleMenuAuthoritySetDto> setDtoMono, AdminAuthDetails operator) {
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

                    DeleteConditionStep<?> deleteQuery = dslContext.deleteFrom(Tables.ROLE_MENU_AUTHORITY).where(Tables.ROLE_MENU_AUTHORITY.ROLE_ID.eq(setDto.getRoleId()));
                    InsertValuesStep2<?, Long, Long> insertQuery = dslContext.insertInto(Tables.ROLE_MENU_AUTHORITY, Tables.ROLE_MENU_AUTHORITY.ROLE_ID, Tables.ROLE_MENU_AUTHORITY.MENU_ID);

                    menuIds.forEach(menuId -> insertQuery.values(role.getId(), menuId));

                    return connection.preparedQuery(deleteQuery.getSQL()).execute(Tuple.tuple(deleteQuery.getBindValues()))
                            .compose(_ -> connection.preparedQuery(insertQuery.getSQL()).execute(Tuple.tuple(insertQuery.getBindValues())))
                            .compose(_ -> operationRecordRepository.add(connection, "setAuthorities", setDto.getRoleId().toString(), setDto.getMenuIds().stream().map(Object::toString).collect(Collectors.joining(",")), operator))
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
    public Mono<List<RoleMenuAuthorityCompareDto>> compareAuthorities(Mono<MultipleIdDto> idsDtoMono) {
        return idsDtoMono.flatMap(idsDto -> {
            SelectConditionStep<?> authoritiesQuery = dslContext.select(Tables.ROLE_MENU_AUTHORITY.ROLE_ID, Tables.ROLE_MENU_AUTHORITY.MENU_ID).where(Tables.ROLE_MENU_AUTHORITY.ROLE_ID.in(idsDto.getIds()));
            SelectConditionStep<?> menuQuery = dslContext.select(Tables.MENU.ID, Tables.MENU.PARENT_ID, Tables.MENU.NAME, Tables.MENU.SORT_BY).where(Tables.MENU.STATUS.eq(Status.enable.value()));

            return Mono.<List<RoleMenuAuthorityCompareDto>>create(sink -> pool.getConnection().flatMap(connection -> connection.preparedQuery(authoritiesQuery.getSQL())
                    .execute(Tuple.tuple(menuQuery.getBindValues()))
                    .map(rows -> {
                        Map<Long, Set<Long>> menusRoleIds = new HashMap<>();
                        rows.forEach(row -> menusRoleIds.computeIfAbsent(row.getLong(1), _ -> new HashSet<>()).add(row.getLong(0)));
                        return menusRoleIds;
                    })
                    .flatMap(menusRoleIds -> connection.preparedQuery(menuQuery.getSQL())
                            .execute(Tuple.tuple(menuQuery.getBindValues()))
                            .map(rows -> rows.stream().map(row -> {
                                        Menu menu = new Menu();
                                        menu.setId(row.getLong(0));
                                        menu.setParentId(row.getLong(1));
                                        menu.setName(row.getString(2));
                                        menu.setSortBy(row.getInteger(3));

                                        Set<Long> roleIds = menusRoleIds.get(menu.getId());

                                        return new RoleMenuAuthorityCompareDto(menu, roleIds != null && roleIds.size() != idsDto.getIds().size(), roleIds);
                                    }).toList()
                            ))
                    .onComplete(_ -> connection.close())
                    .onSuccess(sink::success)
                    .onFailure(sink::error)
            )).map(authorities -> {
                MenuSortableDto.sort(authorities);
                return authorities;
            });
        });
    }

    /**
     * 角色菜单列表
     *
     * @param authDetails 管理员认证信息
     * @return 角色菜单列表
     */
    public Mono<List<RoleMenuDto>> roleMenus(AdminAuthDetails authDetails) {
        TableRoleMenuAuthority a = Tables.ROLE_MENU_AUTHORITY.as("a");
        TableMenu m = Tables.MENU.as("m");

        SelectConditionStep<?> query = dslContext.select(a.ID, a.ROLE_ID, a.MENU_ID, m.ID, m.PARENT_ID, m.TYPE, m.NAME, m.ICON, m.PATH, m.SORT_BY).from(a).innerJoin(m).on(m.ID.eq(a.MENU_ID)).where(a.ROLE_ID.eq(authDetails.getRoleId())).and(m.STATUS.eq(Status.enable.value()));
        return Mono.create(sink -> pool.getConnection().flatMap(connection -> connection.preparedQuery(query.getSQL())
                .execute(Tuple.tuple(query.getBindValues()))
                .map(rows -> {
                    Map<Long, RoleMenuDto> menuMap = new HashMap<>(rows.size());
                    rows.forEach(row -> {
                        RoleMenuAuthority authority = new RoleMenuAuthority();
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
                    });

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
                .onComplete(_ -> connection.close())
                .onSuccess(sink::success)
                .onFailure(sink::error)
        ));
    }
}
