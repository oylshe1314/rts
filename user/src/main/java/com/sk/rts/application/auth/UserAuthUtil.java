package com.sk.rts.application.auth;

import com.sk.rts.application.util.FeistelUtil;

import java.util.UUID;

public final class UserAuthUtil {

    public static String buildSubject(long userId, long deviceId) {
        return new UUID(FeistelUtil.encode(userId), FeistelUtil.encode(deviceId)).toString();
    }

    public static void parseSubject(String subject, UserAuthDetails authDetails) {
        UUID uuid = UUID.fromString(subject);
        authDetails.setUserId(uuid.getMostSignificantBits());
        authDetails.setDeviceId(uuid.getLeastSignificantBits());
    }

    public static long[] parseSubject(String subject) {
        UUID uuid = UUID.fromString(subject);
        return new long[]{uuid.getMostSignificantBits(), uuid.getLeastSignificantBits()};
    }

    public static long parseUserId(String subject) {
        return parseSubject(subject)[0];
    }

    public static long parseDeviceId(String subject) {
        return parseSubject(subject)[1];
    }

    public static String buildTokenKey(String subject) {
        return "message:user:token:" + subject;
    }

    public static String buildDetailsKey(long userId) {
        return "message:user:details:" + userId;
    }

    public static String buildDeviceKey(long deviceId) {
        return "message:user:device:" + deviceId;
    }
}
