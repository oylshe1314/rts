package com.sk.rts.application.entity.base;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public abstract class StateEntity extends OperationEntity {

    /**
     * 状态，0:禁用，1：启用，其他未定义
     */
    private Integer state;
}
