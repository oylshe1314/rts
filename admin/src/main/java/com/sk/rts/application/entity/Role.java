package com.sk.rts.application.entity;

import com.sk.rts.application.entity.base.StatusEntity;
import io.vertx.sqlclient.Row;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Role extends StatusEntity {

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
        role.setStatus(row.getInteger(2));
        role.setRemark(row.getString(3));
        role.setCreateBy(row.getString(4));
        role.setCreateTime(row.getOffsetDateTime(5));
        role.setUpdateBy(row.getString(6));
        role.setUpdateTime(row.getOffsetDateTime(7));
        return role;
    }
}
