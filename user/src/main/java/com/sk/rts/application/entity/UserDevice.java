package com.sk.rts.application.entity;

import com.sk.rts.application.entity.base.BaseEntity;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserDevice extends BaseEntity {

    private String deviceNo;

    private String caller;

    private String version;

    private String channel;

    private String platform;

    private String serialNo;
}
