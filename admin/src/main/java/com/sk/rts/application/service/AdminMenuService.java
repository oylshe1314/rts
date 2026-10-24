package com.sk.rts.application.service;

import com.sk.rts.application.auth.AdminAuthDetails;
import com.sk.rts.application.dto.*;
import com.sk.rts.application.entity.AdminMenu;
import com.sk.rts.application.entity.enums.MenuType;
import com.sk.rts.application.entity.enums.Operation;
import com.sk.rts.application.entity.enums.State;
import com.sk.rts.application.exception.ExceptionUtil;
import com.sk.rts.application.exception.ResponseStatus;
import com.sk.rts.application.exception.StandardStatusException;
import com.sk.rts.application.jooq.Tables;
import com.sk.rts.application.jooq.tables.TableAdminMenu;
import com.sk.rts.application.repository.AdminMenuRepository;
import com.sk.rts.application.repository.AdminOperationRecordRepository;
import com.sk.rts.application.tools.UpdateRemarkBuilder;
import com.sk.rts.application.util.ReactiveUtil;
import io.vertx.core.Future;
import io.vertx.sqlclient.Pool;
import io.vertx.sqlclient.Tuple;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jooq.*;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
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
public class AdminMenuService {

    // 默认菜单，默认角色的默认菜单权限禁止移除
    protected static final Set<Long> SYSTEM_MENU_IDS = Set.of(
            1L, 2L, 3L, 4L, 5L, 6L, 7L, //菜单
            8L, 9L, 10L, 11L, 12L, 13L, 14L, 15L, 16L, 17L, 18L, 19L, 20L, 21L, 22L, 23L, //管理员
            24L, 25L, 26L, 27L, 28L //设置
    );

    private final Pool pool;
    private final DSLContext dslContext;

    private final AdminMenuRepository adminMenuRepository;
    private final AdminOperationRecordRepository adminOperationRecordRepository;

    /**
     * 获取菜单选择列表
     *
     * @param type 菜单类型，1：目录，2：菜单，3：接口
     * @return 菜单选择列表
     */
    public Mono<List<AdminMenuOptionDto>> menuSelectList(Integer type) {
        Select<?> query = dslContext.select(Tables.ADMIN_MENU.ID, Tables.ADMIN_MENU.NAME).from(Tables.ADMIN_MENU).where(Tables.ADMIN_MENU.TYPE.eq(type));
        Future<List<AdminMenuOptionDto>> future = pool.preparedQuery(query.getSQL()).execute(Tuple.tuple(query.getBindValues())).map(rows -> rows.stream().map(row -> new AdminMenuOptionDto(row.getLong(0), row.getString(1))).toList());
        return ReactiveUtil.toMono(future);
    }

    /**
     * 菜单分页查询
     *
     * @param pageQueryDto 分页查询参数
     * @return 分页查询结果
     */
    public Mono<PageResultDto<AdminMenuDto>> query(PageQueryDto<@Nullable AdminMenuQueryDto> pageQueryDto) {
        AdminMenuQueryDto adminMenuQueryDto = pageQueryDto.getQuery();

        TableAdminMenu m = Tables.ADMIN_MENU.as("m");
        TableAdminMenu p = Tables.ADMIN_MENU.as("p");

        Select<?> pageQuery = dslContext.select(
                        m.ID,
                        m.PARENT_ID,
                        m.TYPE,
                        m.NAME,
                        m.ICON,
                        m.PATH,
                        m.SORT_BY,
                        m.STATE,
                        m.REMARK,
                        m.CREATE_BY,
                        m.CREATE_TIME,
                        m.UPDATE_BY,
                        m.UPDATE_TIME,
                        p.ID,
                        p.NAME)
                .from(m)
                .leftJoin(p).on(p.ID.eq(m.PARENT_ID));
        Select<?> countQuery = dslContext.selectCount().from(m).leftJoin(p).on(p.ID.eq(m.PARENT_ID));

        List<Condition> conditions = new ArrayList<>();
        if (adminMenuQueryDto != null) {
            if (adminMenuQueryDto.getType() != null) {
                conditions.add(m.TYPE.eq(adminMenuQueryDto.getType()));
            }
            if (adminMenuQueryDto.getName() != null) {
                conditions.add(m.NAME.like("%" + adminMenuQueryDto.getName() + "%"));
            }
            if (adminMenuQueryDto.getPath() != null) {
                conditions.add(p.NAME.like("%" + adminMenuQueryDto.getPath() + "%"));
            }
        }

        if (!conditions.isEmpty()) {
            pageQuery = ((SelectWhereStep<?>) pageQuery).where(conditions);
            countQuery = ((SelectWhereStep<?>) countQuery).where(conditions);
        }

        if (pageQueryDto.getSort() == null) {
            pageQuery = ((SelectOrderByStep<?>) pageQuery).orderBy(m.ID.desc());
        } else {
            OrderField<?> sortField = m.field(pageQueryDto.getSort());
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
        Future<PageResultDto<AdminMenuDto>> future = pool.withConnection(connection -> connection.preparedQuery(pageSql).execute(Tuple.tuple(pageArgs))
                .map(rows -> rows.stream().map(row -> {
                            AdminMenu menu = AdminMenu.fromRow(row);
                            if (menu.getParentId() != 0L) {
                                menu.setParent(new AdminMenu());
                                menu.getParent().setId(row.getLong(13));
                                menu.getParent().setName(row.getString(14));
                            }

                            return new AdminMenuDto(menu);
                        }).toList()
                )
                .flatMap(menus -> {
                    if (menus.size() < pageQueryDto.getPageSize()) {
                        return Future.succeededFuture(new PageResultDto<>(pageQueryDto.getPageNo(), pageQueryDto.getPageSize(), (pageQueryDto.getPageSize().longValue() * (pageQueryDto.getPageNo().longValue() - 1)) + menus.size(), menus));
                    }
                    return connection.preparedQuery(countSql).execute(Tuple.tuple(countArgs)).map(rows -> new PageResultDto<>(pageQueryDto.getPageNo(), pageQueryDto.getPageSize(), rows.iterator().next().getLong(0), menus));
                })
        );
        return ReactiveUtil.toMono(future);
    }

    private <T> Future<T> recoverUniqueIndexException(Throwable throwable) {
        SQLException sqlException = ExceptionUtil.extractException(throwable, SQLException.class);
        if (sqlException != null) {
            if (sqlException.getSQLState().startsWith("23")) {
                if (sqlException.getMessage().contains("idx_unique_menu_parent_id_and_name")) {
                    return Future.failedFuture(new StandardStatusException("menu.name.exists", "菜单名称已存在"));
                }
                if (sqlException.getMessage().contains("idx_unique_menu_parent_id_and_path")) {
                    return Future.failedFuture(new StandardStatusException("menu.path.exists", "菜单路径已存在"));
                }
            }
        }
        return Future.failedFuture(throwable);
    }

    /**
     * 菜单添加
     *
     * @param addDto   菜单添加参数
     * @param operator 操作员
     * @return 新添加的菜单信息
     */
    public Mono<AdminMenuDto> add(AdminMenuAddDto addDto, AdminAuthDetails operator) {
        MenuType menuType = MenuType.valueOf(addDto.getType());
        if (menuType == null) {
            return Mono.error(new StandardStatusException("菜单类型错误"));
        }

        if (!StringUtils.hasText(addDto.getPath())) {
            if (menuType == MenuType.menu || menuType == MenuType.api) {
                return Mono.error(new StandardStatusException("菜单和接口路径不能为空"));
            }
        }

        Future<AdminMenuDto> future = pool.withTransaction(connection -> Future.succeededFuture()
                .compose(_ -> {
                    if (addDto.getParentId() == null || addDto.getParentId() == 0L) {
                        if (menuType != MenuType.dir) {
                            return Future.failedFuture(new StandardStatusException("上级菜单ID错误"));
                        } else {
                            return Future.succeededFuture();
                        }
                    } else {
                        if (menuType == MenuType.dir) {
                            return Future.succeededFuture();
                        }

                        return adminMenuRepository.getForUpdate(connection, addDto.getParentId()).flatMap(parent -> {
                            if (parent == null) {
                                return Future.failedFuture(new StandardStatusException("找不到上级菜单"));
                            }
                            if (menuType == MenuType.menu) {
                                if (!MenuType.isDir(parent.getType())) {
                                    return Future.failedFuture(new StandardStatusException("菜单的上级必须是目录"));
                                }
                            } else {
                                if (!MenuType.isMenu(parent.getType())) {
                                    return Future.failedFuture(new StandardStatusException("接口的上级必须是菜单"));
                                }
                            }
                            return Future.succeededFuture(parent);
                        });
                    }
                })
                .compose(parent -> {
                    AdminMenu menu = new AdminMenu();
                    menu.setParentId(parent == null ? 0L : parent.getId());
                    menu.setType(menuType.value());
                    menu.setName(addDto.getName());
                    menu.setIcon(menuType == MenuType.api ? "" : addDto.getIcon());
                    menu.setPath(addDto.getPath() == null ? "" : addDto.getPath());
                    menu.setSortBy(addDto.getSortBy());
                    menu.setState(State.disable.value());
                    menu.initOperation(addDto.getRemark(), operator.getUsername());

                    menu.setParent(parent);

                    return adminMenuRepository.insert(connection, menu)
                            .compose(id -> {
                                menu.setId(id);
                                return adminOperationRecordRepository.add(connection, Operation.add, menu.getId().toString(), "admin_menu", "", operator);
                            })
                            .map(_ -> new AdminMenuDto(menu))
                            .recover(this::recoverUniqueIndexException);
                })
        );
        return ReactiveUtil.toMono(future);
    }

    /**
     * 修改菜单
     *
     * @param updateDto 菜单修改参数
     * @param operator  操作员
     * @return 修改后的菜单信息
     */
    public Mono<AdminMenuDto> update(AdminMenuUpdateDto updateDto, AdminAuthDetails operator) {
        Map<Field<?>, Object> values = new HashMap<>();
        UpdateRemarkBuilder remarkBuilder = new UpdateRemarkBuilder();
        Future<AdminMenuDto> future = pool.withTransaction(connection -> adminMenuRepository.getForUpdate(connection, updateDto.getId())
                .flatMap(menu -> {
                    if (menu == null) {
                        return Future.failedFuture(new StandardStatusException("菜单不存在"));
                    }
                    return Future.succeededFuture(menu);
                })
                .compose(menu -> {
                    if (updateDto.getType() != null && !updateDto.getType().equals(menu.getType())) {
                        MenuType menuType = MenuType.valueOf(updateDto.getType());
                        if (menuType == null) {
                            return Future.failedFuture(new StandardStatusException("菜单类型错误"));
                        }
                        remarkBuilder.add("type", menu.getType(), updateDto.getType());
                        menu.setType(menuType.value());
                        values.put(Tables.ADMIN_MENU.TYPE, menu.getType());
                    }
                    return Future.succeededFuture(menu);
                })
                .compose(menu -> {
                    if (MenuType.isDir(menu.getType())) {
                        remarkBuilder.add("parentId", menu.getParentId(), 0L);
                        menu.setParentId(0L);
                        values.put(Tables.ADMIN_MENU.PARENT_ID, 0L);
                    } else {
                        if (updateDto.getParentId() != null && !updateDto.getParentId().equals(menu.getParentId())) {
                            remarkBuilder.add("parentId", menu.getParentId(), updateDto.getParentId());
                            menu.setParentId(updateDto.getParentId());
                            values.put(Tables.ADMIN_MENU.PARENT_ID, menu.getParentId());
                        }

                        if (menu.getParentId() == 0L) {
                            return Future.failedFuture(new StandardStatusException("上级菜单不能为空"));
                        }

                        return adminMenuRepository.getForUpdate(connection, menu.getParentId()).flatMap(parent -> {
                            if (parent == null || MenuType.isApi(parent.getType())) {
                                return Future.failedFuture(new StandardStatusException("上级菜单不存在"));
                            }
                            if (MenuType.isMenu(menu.getType())) {
                                if (!MenuType.isDir(parent.getType())) {
                                    return Future.failedFuture(new StandardStatusException("菜单的上级必须是目录"));
                                }
                            } else {
                                if (!MenuType.isMenu(parent.getType())) {
                                    return Future.failedFuture(new StandardStatusException("接口的上级必须是菜单"));
                                }
                            }

                            menu.setParent(parent);
                            return Future.succeededFuture(menu);
                        });
                    }
                    return Future.succeededFuture(menu);
                })
                .compose(menu -> {
                    if (updateDto.getName() != null && !updateDto.getName().equals(menu.getName())) {
                        remarkBuilder.add("name", menu.getName(), updateDto.getName());
                        menu.setName(updateDto.getName());
                        values.put(Tables.ADMIN_MENU.NAME, menu.getName());
                    }

                    if (updateDto.getIcon() != null && !updateDto.getIcon().equals(menu.getIcon())) {
                        if (MenuType.isApi(menu.getType()) && StringUtils.hasText(updateDto.getIcon())) {
                            return Future.failedFuture(new StandardStatusException("接口无法设置图标"));
                        }
                        remarkBuilder.add("icon", menu.getIcon(), updateDto.getIcon());
                        menu.setIcon(updateDto.getIcon());
                        values.put(Tables.ADMIN_MENU.ICON, menu.getIcon());
                    }

                    if (updateDto.getPath() != null && !updateDto.getPath().equals(menu.getPath())) {
                        if ((MenuType.isMenu(menu.getType()) || MenuType.isApi(menu.getType())) && !StringUtils.hasText(updateDto.getPath())) {
                            return Future.failedFuture(new StandardStatusException("菜单和接口路径不能为空"));
                        }
                        remarkBuilder.add("path", menu.getPath(), updateDto.getPath());
                        menu.setPath(updateDto.getPath());
                        values.put(Tables.ADMIN_MENU.PATH, menu.getPath());
                    }

                    if (updateDto.getSortBy() != null) {
                        remarkBuilder.add("sortBy", menu.getSortBy(), updateDto.getSortBy());
                        menu.setSortBy(updateDto.getSortBy());
                        values.put(Tables.ADMIN_MENU.SORT_BY, menu.getSortBy());
                    }

                    menu.updateOperation(updateDto.getRemark(), operator.getUsername());
                    values.put(Tables.ADMIN_MENU.REMARK, menu.getRemark());
                    values.put(Tables.ADMIN_MENU.UPDATE_BY, menu.getUpdateBy());
                    values.put(Tables.ADMIN_MENU.UPDATE_TIME, menu.getUpdateTime());

                    Update<?> query = dslContext.update(Tables.ADMIN_MENU).set(values).where(Tables.ADMIN_MENU.ID.eq(menu.getId()));
                    return connection.preparedQuery(query.getSQL()).execute(Tuple.tuple(query.getBindValues()))
                            .compose(_ -> adminOperationRecordRepository.add(connection, Operation.update, menu.getId().toString(), "admin_menu", remarkBuilder.build(), operator))
                            .map(_ -> new AdminMenuDto(menu))
                            .recover(this::recoverUniqueIndexException);
                })
        );
        return ReactiveUtil.toMono(future);
    }

    /**
     * 删除菜单，默认菜单禁止删除
     *
     * @param deleteDto 删除参数
     */
    public Mono<Void> delete(MultipleIdDto deleteDto, AdminAuthDetails operator) {
        Set<Long> ids = new HashSet<>(deleteDto.getIds());
        if (ids.containsAll(SYSTEM_MENU_IDS)) {
            return Mono.error(new StandardStatusException("系统菜单禁止删除"));
        }

        Future<Void> future = pool.withTransaction(connection -> adminMenuRepository.existsByParentIdIn(connection, ids)
                .compose(exists -> {
                    if (exists) {
                        return Future.failedFuture(new StandardStatusException("请先删除子菜单"));
                    }
                    Delete<?> query = dslContext.deleteFrom(Tables.ADMIN_MENU).where(Tables.ADMIN_MENU.ID.in(ids));
                    return connection.preparedQuery(query.getSQL()).execute(Tuple.tuple(query.getBindValues()));
                })
                .compose(_ -> adminOperationRecordRepository.add(connection, Operation.delete, ids.stream().map(Object::toString).collect(Collectors.joining(",")), "admin_menu", "", operator))
        );
        return ReactiveUtil.toMono(future);
    }

    /**
     * 菜单状态修改(启用/禁用)，默认菜单禁止禁用
     *
     * @param changeDto 状态修改参数
     * @param operator  操作员
     */
    public Mono<Void> changeState(StateChangeDto changeDto, AdminAuthDetails operator) {
        Set<Long> ids = new HashSet<>(changeDto.getIds());
        if (changeDto.getState() == State.disable.value() && ids.containsAll(SYSTEM_MENU_IDS)) {
            return Mono.error(new StandardStatusException("系统菜单禁止禁用"));
        }

        State state = State.valueOf(changeDto.getState());

        Update<?> query = dslContext.update(Tables.ADMIN_MENU).set(Tables.ADMIN_MENU.STATE, state.value()).set(Tables.ADMIN_MENU.UPDATE_BY, operator.getUsername()).set(Tables.ADMIN_MENU.UPDATE_TIME, OffsetDateTime.now()).where(Tables.ADMIN_MENU.STATE.in(ids));
        Future<Void> future = pool.withTransaction(connection -> connection.preparedQuery(query.getSQL()).execute(Tuple.tuple(query.getBindValues()))
                .compose(_ -> adminOperationRecordRepository.add(connection, Operation.changeState, ids.stream().map(Object::toString).collect(Collectors.joining(",")), "admin_menu", state.name(), operator))
        );
        return ReactiveUtil.toMono(future);
    }
}
