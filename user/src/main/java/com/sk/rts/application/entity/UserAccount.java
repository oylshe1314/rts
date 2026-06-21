package com.sk.rts.application.entity;

import com.sk.rts.application.entity.base.BaseEntity;
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
}
