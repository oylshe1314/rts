package com.sk.rts.application.auth;

import lombok.Getter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.server.ServerWebExchange;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.Locale;

@Getter
public class UserRemoteDetails {

    private final Locale locale;

    private final String platform;

    private final String deviceId;

    private final String ipAddress;

    public UserRemoteDetails(ServerWebExchange exchange) {
        Locale locale = exchange.getLocaleContext().getLocale();
        if (locale == null) {
            locale = Locale.getDefault();
        }

        this.locale = locale;

        ServerHttpRequest request = exchange.getRequest();

        HttpHeaders headers = request.getHeaders();
        String platform = headers.getFirst("X-Caller-Platform");
        String deviceId = headers.getFirst("X-Caller-Device-Id");

        this.platform = platform != null ? platform.toLowerCase() : "";
        this.deviceId = deviceId != null ? deviceId.toUpperCase() : "";

        InetSocketAddress remoteAddress = request.getRemoteAddress();
        if (remoteAddress == null) {
            this.ipAddress = "";
        } else {
            this.ipAddress = remoteAddress.getHostString();
        }
    }
}
