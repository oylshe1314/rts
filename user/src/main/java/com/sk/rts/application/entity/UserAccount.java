package com.sk.rts.application.entity;

import com.sk.rts.application.entity.base.BaseEntity;
import io.vertx.sqlclient.Row;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserAccount extends BaseEntity {

    /**
     * 用户名
     */
    private String username;

    /**
     * 手机
     */
    private String email;

    /**
     * 邮箱
     */
    private String phone;

    /**
     * 密码
     */
    private String password;

    public static UserAccount fromRow(Row row) {
        return fromRow(row, 0);
    }

    public static UserAccount fromRow(Row row, int indexOffset) {
        UserAccount account = new UserAccount();
        account.setId(row.getLong(indexOffset + 0));
        account.setUsername(row.getString(indexOffset + 1));
        account.setEmail(row.getString(indexOffset + 2));
        account.setPhone(row.getString(indexOffset + 3));
        account.setPassword(row.getString(indexOffset + 4));
        return account;
    }
}
