package com.sk.rts.application.entity;

import com.sk.rts.application.entity.base.BaseEntity;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.OffsetDateTime;

@Getter
@Setter
public class UserDetails extends BaseEntity {

    /**
     * 昵称
     */
    private String nickname;

    /**
     * 头像
     */
    private String avatar;

    /**
     * 性别
     */
    private Long gender;

    /**
     * 出生日期
     */
    private LocalDate birthday;

    /**
     * 创建时间
     */
    private OffsetDateTime createTime;
}
