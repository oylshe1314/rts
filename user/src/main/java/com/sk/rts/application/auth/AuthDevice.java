package com.sk.rts.application.auth;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AuthDevice {

    private Long id;

    private String platform;

    private String deviceId;

    private String deviceNo;

    private OffsetDateTime addTime;
}
