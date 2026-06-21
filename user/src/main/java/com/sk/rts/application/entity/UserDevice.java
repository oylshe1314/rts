package com.sk.rts.application.entity;

import com.sk.rts.application.entity.base.BaseEntity;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;

@Getter
@Setter
public class UserDevice extends BaseEntity {

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 设备编号
     */
    private String deviceNo;

    /**
     * 客户端
     */
    private String caller;

    /**
     * 版本号
     */
    private String version;

    /**
     * 渠道
     */
    private String channel;

    /**
     * 平台
     */
    private String platform;

    /**
     * 序列号
     */
    private String serialNo;

    /**
     * 创建时间
     */
    private OffsetDateTime createTime;
}
