package com.sk.rts.application.auth;

import lombok.Getter;
import org.springframework.http.server.reactive.ServerHttpRequest;

@Getter
public class AdminRemoteDetails {

    private final String remoteAddress;

    public AdminRemoteDetails(ServerHttpRequest request) {
        this.remoteAddress = request.getRemoteAddress() == null ? "" : request.getRemoteAddress().getHostString();
    }
}
