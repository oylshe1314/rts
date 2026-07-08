package com.sk.rts.application.entity;

import com.sk.rts.application.entity.base.BaseEntity;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RoleAuthority extends BaseEntity {

    /**
     * 角色ID
     */
    private Long roleId;

    /**
     * 菜单ID
     */
    private Long menuId;

    /**
     * 菜单
     */
    private Menu menu;
}
