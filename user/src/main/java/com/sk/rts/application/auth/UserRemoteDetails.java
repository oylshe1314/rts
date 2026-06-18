package com.sk.rts.application.auth;

import lombok.Getter;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.http.server.reactive.ServerHttpRequest;

@Getter
public class UserRemoteDetails {

    private final String caller;

    private final String version;

    private final String channel;

    private final String platform;

    private final String device;

    private final String address;

    public UserRemoteDetails(ServerHttpRequest request) {
        caller = ObjectUtils.getIfNull(request.getHeaders().getFirst("X-Caller-Name"), "");
        version = ObjectUtils.getIfNull(request.getHeaders().getFirst("X-Caller-Version"), "");
        channel = ObjectUtils.getIfNull(request.getHeaders().getFirst("X-Caller-Channel"), "");
        platform = ObjectUtils.getIfNull(request.getHeaders().getFirst("X-Caller-Platform"), "");
        device = ObjectUtils.getIfNull(request.getHeaders().getFirst("X-Caller-Device"), "");
        this.address = request.getRemoteAddress() == null ? "" : request.getRemoteAddress().getHostString();
    }
}
