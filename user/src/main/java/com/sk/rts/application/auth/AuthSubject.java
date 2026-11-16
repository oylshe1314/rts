package com.sk.rts.application.auth;

public interface AuthSubject {

    void setSubject(String subject);

    void setUserId(Long userId);

    void setDeviceId(Long deviceId);

    String getSubject();

    Long getUserId();

    Long getDeviceId();
}
