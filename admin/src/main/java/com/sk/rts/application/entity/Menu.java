package com.sk.rts.application.entity;

import com.sk.rts.application.entity.base.StateEntity;
import io.vertx.sqlclient.Row;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Menu extends StateEntity {

    /**
     * 父级菜单ID
     */
    private Long parentId;

    /**
     * 菜单类型，1：目录，2：菜单，3：接口(所有增删改查都是接口, 用来控制读写权限)
     */
    private Integer type;

    /**
     * 菜单图标
     */
    private String name;

    /**
     * 菜单名称
     */
    private String icon;

    /**
     * 路径
     */
    private String component;

    /**
     * 路径
     */
    private String path;

    /**
     * 排序字段 —— 升序
     */
    private Integer sortBy;

    /**
     * 上级菜单
     */
    private Menu parent;

    public static Menu fromRow(Row row) {
        Menu menu = new Menu();
        menu.setId(row.getLong(0));
        menu.setParentId(row.getLong(1));
        menu.setType(row.getInteger(2));
        menu.setName(row.getString(3));
        menu.setIcon(row.getString(4));
        menu.setPath(row.getString(5));
        menu.setSortBy(row.getInteger(6));
        menu.setState(row.getInteger(7));
        menu.setRemark(row.getString(8));
        menu.setCreateBy(row.getString(9));
        menu.setCreateTime(row.getOffsetDateTime(10));
        menu.setUpdateBy(row.getString(11));
        menu.setUpdateTime(row.getOffsetDateTime(12));
        return menu;
    }
}
