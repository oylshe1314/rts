package com.sk.rts.application.entity;

import com.sk.rts.application.entity.base.StateEntity;
import io.vertx.sqlclient.Row;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class Admin extends StateEntity {

    /**
     * 角色ID
     */
    private Long roleId;

    /**
     * 用户名
     */
    private String username;

    /**
     * 密码
     */
    private String password;

    /**
     * 手机号码
     */
    private String phone;

    /**
     * 邮箱
     */
    private String email;

    /**
     * 昵称
     */
    private String nickname;

    /**
     * 头像
     */
    private String avatar;

    /**
     * 角色
     */
    private Role role;

    public static Admin fromRow(Row row){
        Admin admin = new Admin();
        admin.setId(row.getLong(0));
        admin.setRoleId(row.getLong(1));
        admin.setUsername(row.getString(2));
        admin.setPassword(row.getString(3));
        admin.setPhone(row.getString(4));
        admin.setEmail(row.getString(5));
        admin.setNickname(row.getString(6));
        admin.setAvatar(row.getString(7));
        admin.setState(row.getInteger(8));
        admin.setRemark(row.getString(9));
        admin.setCreateBy(row.getString(10));
        admin.setCreateTime(row.getOffsetDateTime(11));
        admin.setUpdateBy(row.getString(12));
        admin.setUpdateTime(row.getOffsetDateTime(13));
        return admin;
    }
}
