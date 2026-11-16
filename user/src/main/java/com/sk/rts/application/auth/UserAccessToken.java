package com.sk.rts.application.auth;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserAccessToken implements AuthSubject {

    private String token;

    private String subject;

    private Long issueTime;

    private Long expireTime;

    private Long userId;

    private Long deviceId;
}
