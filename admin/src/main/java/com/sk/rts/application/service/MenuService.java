package com.sk.rts.application.service;

import com.sk.rts.application.auth.AdminAuthDetails;
import com.sk.rts.application.dto.*;
import com.sk.rts.application.entity.Menu;
import com.sk.rts.application.entity.enums.MenuType;
import com.sk.rts.application.entity.enums.Operation;
import com.sk.rts.application.entity.enums.State;
import com.sk.rts.application.exception.ExceptionUtil;
import com.sk.rts.application.exception.ResponseStatus;
import com.sk.rts.application.exception.StandardStatusException;
import com.sk.rts.application.jooq.Tables;
import com.sk.rts.application.jooq.tables.TableMenu;
import com.sk.rts.application.repository.MenuRepository;
import com.sk.rts.application.repository.OperationRecordRepository;
import io.vertx.core.Future;
import io.vertx.sqlclient.Pool;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.Tuple;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jooq.*;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
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
public class MenuService {

    // 默认菜单，默认角色的默认菜单权限禁止移除
    protected static final Set<Long> SYSTEM_MENU_IDS = new HashSet<>(Arrays.asList(
            1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L,
            10L, 11L, 12L, 13L, 14L, 15L, 16L, 17L, 18L,
            19L, 20L, 21L, 22L, 23L, 24L, 25L, 26L, 27L
    ));

    private final Pool pool;
    private final DSLContext dslContext;

    private final MenuRepository menuRepository;
    private final OperationRecordRepository operationRecordRepository;

    /**
     * 获取菜单选择列表
     *
     * @param type 菜单类型，1：目录，2：菜单，3：接口
     * @return 菜单选择列表
     */
    public Mono<List<MenuOptionDto>> menuSelectList(Integer type) {
        Select<?> query = dslContext.select(Tables.MENU.ID, Tables.MENU.NAME).from(Tables.MENU).where(Tables.MENU.TYPE.eq(type));
        return Flux.<MenuOptionDto>create(sink -> pool.preparedQuery(query.getSQL()).execute(Tuple.tuple(query.getBindValues()))
                .onFailure(sink::error)
                .onSuccess(rows -> {
                    for (Row row : rows) {
                        sink.next(new MenuOptionDto(row.getLong(0), row.getString(1)));
                    }
                    sink.complete();
                })
        ).collectList();
    }

    /**
     * 菜单分页查询
     *
     * @param pageRequestDtoMono 分页查询参数
     * @return 分页查询结果
     */
    public Mono<PageResultDto<MenuDto>> query(Mono<PageQueryDto<@Nullable MenuQueryDto>> pageRequestDtoMono) {
        return pageRequestDtoMono.flatMap(pageRequestDto -> {
            MenuQueryDto queryDto = pageRequestDto.getQuery();

            TableMenu m = Tables.MENU.as("m");
            TableMenu p = Tables.MENU.as("p");

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
            if (queryDto != null) {
                if (queryDto.getType() != null) {
                    conditions.add(m.TYPE.eq(queryDto.getType()));
                }
                if (queryDto.getName() != null) {
                    conditions.add(m.NAME.like("%" + queryDto.getName() + "%"));
                }
                if (queryDto.getPath() != null) {
                    conditions.add(p.NAME.like("%" + queryDto.getPath() + "%"));
                }
            }

            if (!conditions.isEmpty()) {
                pageQuery = ((SelectWhereStep<?>) pageQuery).where(conditions);
                countQuery = ((SelectWhereStep<?>) countQuery).where(conditions);
            }

            if (pageRequestDto.getSort() == null) {
                pageQuery = ((SelectOrderByStep<?>) pageQuery).orderBy(m.ID.asc());
            } else {
                OrderField<?> sortField = m.field(pageRequestDto.getSort());
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
                                Menu menu = Menu.fromRow(row);
                                if (menu.getParentId() != 0L) {
                                    menu.setParent(new Menu());
                                    menu.getParent().setId(row.getLong(13));
                                    menu.getParent().setName(row.getString(14));
                                }

                                return new MenuDto(menu);
                            }).toList()
                    )
                    .flatMap(menus -> {
                        if (menus.size() < pageRequestDto.getPageSize()) {
                            return Future.succeededFuture(new PageResultDto<>(pageRequestDto.getPageNo(), pageRequestDto.getPageSize(), (pageRequestDto.getPageSize().longValue() * (pageRequestDto.getPageNo().longValue() - 1)) + menus.size(), menus));
                        }

                        return connection.preparedQuery(countSql).execute(Tuple.tuple(countArgs)).map(rows -> new PageResultDto<>(pageRequestDto.getPageNo(), pageRequestDto.getPageSize(), rows.iterator().next().getLong(0), menus));
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
            if (sqlException.getMessage().contains("idx_unique_menu_parent_id_and_name")) {
                return Future.failedFuture(new StandardStatusException("menu.name.exists", "菜单名称已存在"));
            }
            if (sqlException.getMessage().contains("idx_unique_menu_parent_id_and_path")) {
                return Future.failedFuture(new StandardStatusException("menu.path.exists", "菜单路径已存在"));
            }
        }
        return Future.failedFuture(throwable);
    }

    /**
     * 菜单添加
     *
     * @param addDtoMono 菜单添加参数
     * @param operator   操作员
     * @return 新添加的菜单信息
     */
    public Mono<MenuDto> add(Mono<MenuAddDto> addDtoMono, AdminAuthDetails operator) {
        return addDtoMono.flatMap(addDto -> {
            MenuType menuType = MenuType.valueOf(addDto.getType());
            if (menuType == null) {
                return Mono.error(new StandardStatusException("菜单类型错误"));
            }

            if (!StringUtils.hasText(addDto.getPath())) {
                if (menuType == MenuType.menu || menuType == MenuType.api) {
                    return Mono.error(new StandardStatusException("菜单和接口路径不能为空"));
                }
            }

            return Mono.create(sink -> pool.getConnection().flatMap(connection -> connection.begin()
                    .compose(_ -> {
                        if (addDto.getParentId() == null || addDto.getParentId() == 0L) {
                            if (menuType == MenuType.api) {
                                return Future.failedFuture(new StandardStatusException("上级菜单ID错误"));
                            } else {
                                return Future.succeededFuture();
                            }
                        } else {
                            return menuRepository.getForUpdate(connection, addDto.getParentId());
                        }
                    })
                    .compose(parent -> {
                        Menu menu = new Menu();
                        menu.setParentId(parent == null ? 0L : parent.getId());
                        menu.setType(menuType.value());
                        menu.setIcon(menuType == MenuType.api ? "" : addDto.getIcon());
                        menu.setName(addDto.getName());
                        menu.setPath(addDto.getPath() == null ? "" : addDto.getPath());
                        menu.setSortBy(addDto.getSortBy());
                        menu.setState(State.disable.value());
                        menu.initOperation(addDto.getRemark(), operator.getUsername());

                        menu.setParent(parent);

                        return menuRepository.insert(connection, menu)
                                .recover(this::recoverUniqueIndexException)
                                .compose(id -> {
                                    menu.setId(id);
                                    return operationRecordRepository.add(connection, Operation.add, "menu", menu.getId().toString(), operator);
                                })
                                .compose(_ -> connection.transaction().commit())
                                .onComplete(_ -> connection.close())
                                .map(_ -> new MenuDto(menu));
                    })
                    .onSuccess(sink::success)
                    .onFailure(sink::error)
            ));
        });
    }

    /**
     * 修改菜单
     *
     * @param updateDtoMono 菜单修改参数
     * @param operator      操作员
     * @return 修改后的菜单信息
     */
    public Mono<MenuDto> update(Mono<MenuUpdateDto> updateDtoMono, AdminAuthDetails operator) {
        return updateDtoMono.flatMap(updateDto -> {

            Map<Field<?>, Object> values = new HashMap<>();
            return Mono.create(sink -> pool.getConnection().flatMap(connection -> connection.begin()
                    .compose(_ -> menuRepository.getForUpdate(connection, updateDto.getId()).flatMap(menu -> menu == null ? Future.failedFuture(new StandardStatusException("菜单不存在")) : Future.succeededFuture(menu)))
                    .compose(menu -> {
                        if (updateDto.getType() != null && !updateDto.getType().equals(menu.getType())) {
                            MenuType menuType = MenuType.valueOf(updateDto.getType());
                            if (menuType == null) {
                                return Future.failedFuture(new StandardStatusException("菜单类型错误"));
                            }
                            menu.setType(menuType.value());
                            values.put(Tables.MENU.TYPE, menu.getType());
                        }
                        return Future.succeededFuture(menu);
                    })
                    .compose(menu -> {
                        if (updateDto.getParentId() != null && !updateDto.getParentId().equals(menu.getParentId())) {
                            menu.setParentId(updateDto.getParentId());
                            values.put(Tables.MENU.PARENT_ID, menu.getParentId());

                            if (menu.getParentId() != 0L) {
                                return menuRepository.getForUpdate(connection, menu.getParentId()).flatMap(parent -> {
                                    if (parent == null || MenuType.isApi(parent.getType())) {
                                        return Future.failedFuture(new StandardStatusException("上级菜单不存在"));
                                    }

                                    menu.setParent(parent);
                                    return Future.succeededFuture(menu);
                                });
                            }
                        }
                        return Future.succeededFuture(menu);
                    })
                    .compose(menu -> {
                        if (updateDto.getIcon() != null && !updateDto.getIcon().equals(menu.getIcon())) {
                            menu.setIcon(MenuType.isApi(menu.getType()) ? "" : updateDto.getIcon());
                            values.put(Tables.MENU.ICON, menu.getIcon());
                        }

                        if (updateDto.getName() != null && !updateDto.getName().equals(menu.getName())) {
                            menu.setName(updateDto.getName());
                            values.put(Tables.MENU.NAME, menu.getName());
                        }

                        if (updateDto.getPath() != null && !updateDto.getPath().equals(menu.getPath())) {
                            if (updateDto.getPath().isBlank()) {
                                if (MenuType.isMenu(menu.getType()) || MenuType.isApi(menu.getType())) {
                                    return Future.failedFuture(new StandardStatusException("菜单和接口路径不能为空"));
                                }
                            }
                            menu.setPath(updateDto.getPath());
                            values.put(Tables.MENU.PATH, menu.getPath());
                        }

                        if (updateDto.getSort() != null) {
                            menu.setSortBy(updateDto.getSort());
                            values.put(Tables.MENU.SORT_BY, menu.getSortBy());
                        }

                        menu.updateOperation(updateDto.getRemark(), operator.getUsername());
                        values.put(Tables.MENU.REMARK, menu.getRemark());
                        values.put(Tables.MENU.UPDATE_BY, menu.getUpdateBy());
                        values.put(Tables.MENU.UPDATE_TIME, menu.getUpdateTime());

                        Update<?> query = dslContext.update(Tables.MENU).set(values).where(Tables.MENU.ID.eq(menu.getId()));
                        return connection.preparedQuery(query.getSQL()).execute(Tuple.tuple(query.getBindValues()))
                                .recover(this::recoverUniqueIndexException)
                                .compose(_ -> operationRecordRepository.add(connection, Operation.update, "menu", menu.getId().toString(), operator))
                                .compose(_ -> connection.transaction().commit())
                                .onComplete(_ -> connection.close())
                                .map(_ -> new MenuDto(menu));
                    })
                    .onSuccess(sink::success)
                    .onFailure(sink::error)
            ));
        });
    }

    /**
     * 删除菜单，默认菜单禁止删除
     *
     * @param deleteDtoMono 删除参数
     */
    public Mono<Void> delete(Mono<MultipleIdDto> deleteDtoMono, AdminAuthDetails operator) {
        return deleteDtoMono.flatMap(deleteDto -> {
            Set<Long> ids = new HashSet<>(deleteDto.getIds());
            if (ids.containsAll(SYSTEM_MENU_IDS)) {
                return Mono.error(new StandardStatusException("系统菜单禁止删除"));
            }

            return Mono.create(sink -> pool.getConnection().flatMap(connection -> connection.begin()
                    .compose(_ -> menuRepository.existsByParentIdIn(connection, ids))
                    .compose(exists -> {
                        if (exists) {
                            return Future.failedFuture(new StandardStatusException("请先删除子菜单"));
                        }

                        Delete<?> query = dslContext.deleteFrom(Tables.MENU).where(Tables.MENU.ID.in(ids));
                        return connection.preparedQuery(query.getSQL()).execute(Tuple.tuple(query.getBindValues()));
                    })
                    .compose(_ -> operationRecordRepository.add(connection, Operation.delete, "menu", ids.stream().map(Object::toString).collect(Collectors.joining(",")), operator))
                    .compose(_ -> connection.transaction().commit())
                    .onComplete(_ -> connection.close())
                    .onSuccess(sink::success)
                    .onFailure(sink::error))
            );
        });
    }

    /**
     * 菜单状态修改(启用/禁用)，默认菜单禁止禁用
     *
     * @param changeDtoMono 状态修改参数
     * @param operator      操作员
     */
    public Mono<Void> changeState(Mono<ChangeStateDto> changeDtoMono, AdminAuthDetails operator) {
        return changeDtoMono.flatMap(changeDto -> {
            Set<Long> ids = new HashSet<>(changeDto.getIds());
            if (changeDto.getState() == State.disable.value() && ids.containsAll(SYSTEM_MENU_IDS)) {
                return Mono.error(new StandardStatusException("系统菜单禁止禁用"));
            }

            State state = State.valueOf(changeDto.getState());

            Update<?> query = dslContext.update(Tables.MENU).set(Tables.MENU.STATE, state.value()).set(Tables.MENU.UPDATE_BY, operator.getUsername()).set(Tables.MENU.UPDATE_TIME, OffsetDateTime.now()).where(Tables.MENU.STATE.in(ids));
            return Mono.create(sink -> pool.getConnection().flatMap(connection -> connection.begin()
                    .compose(_ -> connection.preparedQuery(query.getSQL()).execute(Tuple.tuple(query.getBindValues())))
                    .compose(_ -> operationRecordRepository.add(connection, Operation.changeState, "menu", ids.stream().map(Object::toString).collect(Collectors.joining(",")), operator))
                    .compose(_ -> connection.transaction().commit())
                    .onComplete(_ -> connection.close())
                    .onSuccess(sink::success)
                    .onFailure(sink::error)
            ));
        });
    }
}
