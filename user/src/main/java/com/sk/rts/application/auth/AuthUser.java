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
public class AuthUser {

    private Long id;

    private String username;

    private String email;

    private String phone;

    private String password;

    private String nickname;

    private String avatar;

    private OffsetDateTime registerTime;
}
