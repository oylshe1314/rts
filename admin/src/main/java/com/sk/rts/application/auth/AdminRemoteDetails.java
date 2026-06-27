package com.sk.rts.application.auth;

import lombok.Getter;
import org.springframework.http.server.reactive.ServerHttpRequest;

@Getter
public class AdminRemoteDetails {

    private final String ipAddress;

    public AdminRemoteDetails(ServerHttpRequest request) {
        this.ipAddress = request.getRemoteAddress() == null ? "" : request.getRemoteAddress().getHostString();
    }
}
