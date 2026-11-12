package com.sk.rts.application.auth;

import com.sk.rts.application.util.CodecUtil;
import com.sk.rts.application.util.FeistelUtil;

public final class AdminAuthUtil {

    public static String buildSubject(long adminId) {
        return CodecUtil.encode64(FeistelUtil.encode(adminId));
    }

    public static long parseSubject(String subject) {
        return FeistelUtil.decode(CodecUtil.decode64(subject));
    }
}
