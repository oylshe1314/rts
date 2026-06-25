package com.sk.rts.application.auth;

import lombok.Getter;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.http.server.reactive.ServerHttpRequest;

@Getter
public class UserRemoteDetails {

    private final String platform;

    private final String device;

    private final String address;

    public UserRemoteDetails(ServerHttpRequest request) {
        platform = ObjectUtils.getIfNull(request.getHeaders().getFirst("X-Caller-Platform"), "");
        device = ObjectUtils.getIfNull(request.getHeaders().getFirst("X-Caller-Device"), "");
        this.address = request.getRemoteAddress() == null ? "" : request.getRemoteAddress().getHostString();
    }
}
