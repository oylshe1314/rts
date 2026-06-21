package com.sk.rts.application.service;

import com.sk.rts.application.auth.AdminAuthDetails;
import com.sk.rts.application.dto.*;
import com.sk.rts.application.entity.Menu;
import com.sk.rts.application.entity.enums.MenuType;
import com.sk.rts.application.entity.enums.Status;
import com.sk.rts.application.exception.StandardStatusException;
import com.sk.rts.application.jooq.Tables;
import com.sk.rts.application.jooq.tables.TableMenu;
import com.sk.rts.application.repository.MenuRepository;
import com.sk.rts.application.repository.OperationRecordRepository;
import io.vertx.core.Future;
import io.vertx.sqlclient.Pool;
import io.vertx.sqlclient.Tuple;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jooq.*;
import org.jspecify.annotations.NullMarked;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

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

    // 数据库访问
    private final Pool pool;
    // SQL构建
    private final DSLContext dslContext;
    // 菜单仓库
    private final MenuRepository menuRepository;
    // 操作记录服务
    private final OperationRecordRepository operationRecordRepository;

    /**
     * 获取菜单选择列表
     *
     * @param type 菜单类型，1：目录，2：菜单，3：接口
     * @return 菜单选择列表
     */
    public Mono<List<MenuSelectDto>> menuSelectList(Integer type) {
        SelectConditionStep<?> query = dslContext.select(Tables.MENU.ID, Tables.MENU.NAME).from(Tables.MENU).where(Tables.MENU.TYPE.eq(type));
        return Flux.<MenuSelectDto>create(sink -> pool.getConnection().flatMap(connection -> connection.preparedQuery(query.getSQL()).execute(Tuple.tuple(query.getBindValues())).onComplete(_ -> connection.close()))
                .onFailure(sink::error)
                .onSuccess(rows -> {
                    rows.forEach(row -> sink.next(new MenuSelectDto(row.getLong(0), row.getString(1))));
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
    public Mono<PageResultDto<MenuDto>> query(Mono<PageQueryDto<MenuQueryDto>> pageRequestDtoMono) {
        return pageRequestDtoMono.flatMap(pageRequestDto -> {
            MenuQueryDto queryDto = pageRequestDto.getQuery();

            TableMenu m = Tables.MENU.as("m");
            TableMenu p = Tables.MENU.as("p");

            SelectWhereStep<?> pageQuery = dslContext.select(m.ID, m.PARENT_ID, m.TYPE, m.NAME, m.ICON, m.PATH, m.SORT_BY, m.STATUS, m.REMARK, m.CREATE_BY, m.CREATE_TIME, m.UPDATE_BY, m.UPDATE_TIME, p.ID, p.NAME).from(m).leftJoin(p).on(p.ID.eq(m.PARENT_ID));
            SelectWhereStep<?> countQuery = dslContext.selectCount().from(m).leftJoin(p).on(p.ID.eq(m.PARENT_ID));

            List<Condition> conditions = new ArrayList<>();
            if (queryDto != null) {
                if (queryDto.getParentName() != null) {
                    conditions.add(p.NAME.like("%" + queryDto.getParentName() + "%"));
                }
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

            if (conditions.size() > 0) {
                pageQuery.where(conditions);
                countQuery.where(conditions);
            }

            if (pageRequestDto.getSort() == null) {
                pageQuery.orderBy(m.ID.asc());
            } else {
                if (Boolean.TRUE.equals(pageRequestDto.getDesc())) {
                    pageQuery.orderBy(m.field(pageRequestDto.getSort()).desc());
                } else {
                    pageQuery.orderBy(m.field(pageRequestDto.getSort()));
                }
            }

            pageQuery.offset(pageRequestDto.getOffset()).limit(pageRequestDto.getPageSize());

            String sql = pageQuery.getSQL();
            log.debug("SQL: {}", sql);
            return Mono.create(sink -> pool.getConnection().flatMap(connection -> connection.preparedQuery(sql).execute(Tuple.tuple(pageQuery.getBindValues()))
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
                            return Future.succeededFuture(new PageResultDto<>(pageRequestDto.getPageNo(), pageRequestDto.getPageSize(), menus.size(), menus));
                        }

                        return connection.preparedQuery(countQuery.getSQL()).execute(Tuple.tuple(countQuery.getBindValues()))
                                .map(rows -> new PageResultDto<>(pageRequestDto.getPageNo(), pageRequestDto.getPageSize(), rows.iterator().next().getLong(0), menus));
                    })
                    .onComplete(_ -> connection.close())
                    .onSuccess(sink::success)
                    .onFailure(sink::error)
            ));
        });
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
                    .compose(parent -> menuRepository.existsByParentIdAndName(connection, parent == null ? 0L : parent.getId(), addDto.getName()).flatMap(exists -> exists ? Future.failedFuture(new StandardStatusException("菜单名称已存在")) : Future.succeededFuture(parent)))
                    .compose(parent -> menuRepository.existsByTypeAndPath(connection, menuType.value(), addDto.getPath()).flatMap(exists -> exists ? Future.failedFuture(new StandardStatusException("菜单路径已存在")) : Future.succeededFuture(parent)))
                    .compose(parent -> {
                        Menu menu = new Menu();
                        menu.setParentId(parent == null ? 0L : parent.getId());
                        menu.setType(menuType.value());
                        menu.setIcon(menuType == MenuType.api ? "" : addDto.getIcon());
                        menu.setName(addDto.getName());
                        menu.setPath(addDto.getPath() == null ? "" : addDto.getPath());
                        menu.setSortBy(addDto.getSortBy());
                        menu.setStatus(Status.disable.value());
                        menu.initOperation(addDto.getRemark(), operator.getUsername());

                        menu.setParent(parent);

                        InsertResultStep<?> query = dslContext.insertInto(Tables.MENU, Tables.MENU.PARENT_ID, Tables.MENU.TYPE, Tables.MENU.NAME, Tables.MENU.ICON, Tables.MENU.PATH, Tables.MENU.SORT_BY, Tables.MENU.STATUS, Tables.MENU.REMARK, Tables.MENU.CREATE_BY, Tables.MENU.CREATE_TIME, Tables.MENU.UPDATE_BY, Tables.MENU.UPDATE_TIME).values(menu.getParentId(), menu.getType(), menu.getName(), menu.getIcon(), menu.getPath(), menu.getSortBy(), menu.getStatus(), menu.getRemark(), menu.getCreateBy(), menu.getCreateTime(), menu.getUpdateBy(), menu.getUpdateTime()).returning(Tables.MENU.ID);

                        String sql = query.getSQL();
                        log.debug("SQL: {}", sql);
                        return connection.preparedQuery(sql).execute(Tuple.tuple(query.getBindValues()))
                                .compose(rows -> {
                                    menu.setId(rows.iterator().next().getLong(0));
                                    return operationRecordRepository.add(connection, "add", "menu", menu.getId().toString(), operator);
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
                        if (updateDto.getType() != null) {
                            MenuType menuType = MenuType.valueOf(updateDto.getType());
                            if (menuType == null) {
                                return Future.failedFuture(new StandardStatusException("菜单类型错误"));
                            }
                            menu.setType(menuType.value());
                            values.put(Tables.MENU.TYPE, menu.getType());
                        }

                        if (updateDto.getParentId() == null || updateDto.getParentId() == 0L) {
                            if (menu.getType() == MenuType.api.value()) {
                                return Future.failedFuture(new StandardStatusException("上级菜单ID错误"));
                            } else {
                                menu.setParentId(0L);
                                menu.setParent(null);
                                values.put(Tables.MENU.PARENT_ID, menu.getParentId());
                                return Future.succeededFuture(menu);
                            }
                        } else {
                            return menuRepository.existsByParentIdAndName(connection, updateDto.getParentId(), updateDto.getName()).flatMap(exists -> {
                                if (exists) {
                                    return Future.failedFuture(new StandardStatusException("菜单名称已存在"));
                                }

                                return menuRepository.getForUpdate(connection, updateDto.getParentId()).flatMap(parent -> {
                                    if (parent == null) {
                                        return Future.failedFuture(new StandardStatusException("上级菜单不存在"));
                                    }

                                    menu.setParentId(parent.getId());
                                    menu.setParent(parent);
                                    values.put(Tables.MENU.PARENT_ID, menu.getParentId());

                                    return Future.succeededFuture(menu);
                                });
                            });
                        }
                    })
                    .compose(menu -> {
                        if (updateDto.getName() != null) {
                            return Future.succeededFuture(menu);
                        }

                        return menuRepository.existsByParentIdAndName(connection, menu.getParentId(), menu.getName()).flatMap(exists -> {
                            if (exists) {
                                return Future.failedFuture(new StandardStatusException("菜单名称已存在"));
                            }

                            menu.setName(updateDto.getName());
                            values.put(Tables.MENU.NAME, menu.getName());
                            return Future.succeededFuture(menu);
                        });
                    })
                    .compose(menu -> {
                        if (updateDto.getPath() == null) {
                            return Future.succeededFuture(menu);
                        }

                        if (updateDto.getPath().isBlank()) {
                            if (menu.getType() == MenuType.menu.value() || menu.getType() == MenuType.api.value()) {
                                return Future.failedFuture(new StandardStatusException("菜单和接口路径不能为空"));
                            }
                        }

                        return menuRepository.existsByTypeAndPath(connection, menu.getType(), updateDto.getPath()).flatMap(exists -> {
                            if (exists) {
                                return Future.failedFuture(new StandardStatusException("路径菜单已存在"));
                            }

                            menu.setPath(updateDto.getPath());
                            values.put(Tables.MENU.PATH, menu.getPath());
                            return Future.succeededFuture(menu);
                        });
                    })
                    .compose(menu -> {
                        if (updateDto.getIcon() != null) {
                            menu.setIcon(menu.getType() == MenuType.api.value() ? "" : updateDto.getIcon());
                            values.put(Tables.MENU.ICON, menu.getIcon());
                        }

                        if (updateDto.getSort() != null) {
                            menu.setSortBy(updateDto.getSort());
                            values.put(Tables.MENU.SORT_BY, menu.getSortBy());
                        }

                        menu.updateOperation(updateDto.getRemark(), operator.getUsername());
                        values.put(Tables.MENU.REMARK, menu.getRemark());
                        values.put(Tables.MENU.UPDATE_BY, menu.getUpdateBy());
                        values.put(Tables.MENU.UPDATE_TIME, menu.getUpdateTime());

                        UpdateConditionStep<?> query = dslContext.update(Tables.MENU).set(values).where(Tables.MENU.ID.eq(menu.getId()));

                        String sql = query.getSQL();
                        log.debug("SQL: {}", sql);
                        return connection.preparedQuery(sql).execute(Tuple.tuple(query.getBindValues()))
                                .compose(_ -> operationRecordRepository.add(connection, "add", "menu", menu.getId().toString(), operator))
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

                        DeleteConditionStep<?> query = dslContext.deleteFrom(Tables.MENU).where(Tables.MENU.ID.in(ids));

                        return connection.preparedQuery(query.getSQL()).execute(Tuple.tuple(query.getBindValues()));
                    })
                    .compose(_ -> operationRecordRepository.add(connection, "delete", "menu", ids.stream().map(Object::toString).collect(Collectors.joining(",")), operator))
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
            if (changeDto.getStatus() == Status.disable.value() && ids.containsAll(SYSTEM_MENU_IDS)) {
                return Mono.error(new StandardStatusException("系统菜单禁止禁用"));
            }

            Status state = Status.valueOf(changeDto.getStatus());

            UpdateConditionStep<?> query = dslContext.update(Tables.MENU).set(Tables.MENU.STATUS, state.value()).set(Tables.MENU.UPDATE_BY, operator.getUsername()).set(Tables.MENU.UPDATE_TIME, OffsetDateTime.now()).where(Tables.MENU.STATUS.in(ids));

            return Mono.create(sink -> pool.getConnection().flatMap(connection -> connection.begin()
                    .compose(_ -> connection.preparedQuery(query.getSQL()).execute(Tuple.tuple(query.getBindValues())))
                    .compose(_ -> operationRecordRepository.add(connection, "changeState", "menu", ids.stream().map(Object::toString).collect(Collectors.joining(",")), operator))
                    .compose(_ -> connection.transaction().commit())
                    .onComplete(_ -> connection.close())
                    .onSuccess(sink::success)
                    .onFailure(sink::error)
            ));
        });
    }
}
