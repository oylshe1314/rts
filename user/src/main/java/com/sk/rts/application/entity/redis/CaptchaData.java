package com.sk.rts.application.entity.redis;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CaptchaData {

    private Integer type;

    private String key;

    private String value;

    private Long expireTime;
}
