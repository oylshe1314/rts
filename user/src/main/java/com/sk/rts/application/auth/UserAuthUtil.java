package com.sk.rts.application.auth;

import com.sk.rts.application.util.FeistelUtil;

import java.util.UUID;

public final class UserAuthUtil {

    public static String buildSubject(long userId, long deviceId) {
        return new UUID(FeistelUtil.encode(userId), FeistelUtil.encode(deviceId)).toString();
    }

    public static Long[] parseSubject(String subject) {
        UUID uuid = UUID.fromString(subject);
        return new Long[]{FeistelUtil.decode(uuid.getMostSignificantBits()), FeistelUtil.decode(uuid.getLeastSignificantBits())};
    }

    public static Long parseUserId(String subject) {
        return parseSubject(subject)[0];
    }

    public static Long parseDeviceId(String subject) {
        return parseSubject(subject)[1];
    }
}
