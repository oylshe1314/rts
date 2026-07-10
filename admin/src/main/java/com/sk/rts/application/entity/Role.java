package com.sk.rts.application.entity;

import com.sk.rts.application.entity.base.StateEntity;
import io.vertx.sqlclient.Row;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Role extends StateEntity {

    /**
     * 角色名称
     */
    private String name;

    /**
     * 角色名称
     */
    private String code;

    public static Role fromRow(Row row) {
        Role role = new Role();
        role.setId(row.getLong(0));
        role.setName(row.getString(1));
        role.setCode(row.getString(2));
        role.setState(row.getInteger(3));
        role.setRemark(row.getString(4));
        role.setCreateBy(row.getString(5));
        role.setCreateTime(row.getOffsetDateTime(6));
        role.setUpdateBy(row.getString(7));
        role.setUpdateTime(row.getOffsetDateTime(8));
        return role;
    }
}
