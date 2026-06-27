package com.sk.rts.application.auth;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserRefreshToken {

    private String subject;

    private String hash;

    private Long issueTime;

    private Long expireTime;

    private Long refreshTime;
}
