package com.sk.rts.application.entity;

import com.sk.rts.application.entity.base.BaseEntity;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserDetails extends BaseEntity {

    private String nickname;

    private String avatar;

    private Long createTime;
}
