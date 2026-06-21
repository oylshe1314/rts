package com.sk.rts.application.entity;

import com.sk.rts.application.entity.base.BaseEntity;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;

@Getter
@Setter
public class OperationRecord extends BaseEntity {

    /**
     * 操作员ID
     */
    private Long operatorId;

    /**
     * 操作员(用户名)
     */
    private String operator;

    /**
     * 操作, login, logout, add, update, delete, changeState等
     */
    private String operation;

    /**
     * 操作参数，表名，登录账号等
     */
    private String operateArgs;

    /**
     * 备注
     */
    private String remark;

    /**
     * 操作员登录IP
     */
    private String loginIp;

    /**
     * 操作时间
     */
    private OffsetDateTime operateTime;
}
