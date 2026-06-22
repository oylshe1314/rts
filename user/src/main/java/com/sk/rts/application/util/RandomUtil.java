package com.sk.rts.application.util;

import org.apache.commons.codec.digest.DigestUtils;

import java.nio.ByteBuffer;
import java.util.Random;

public final class RandomUtil {

    private final static Random random = new Random();

    private static byte randomByte(int min, int range) {
        return (byte) (min + random.nextInt(range));
    }

    public static String randomUpper(int length) {
        byte[] bytes = new byte[length];
        for (int i = 0; i < length; i++) {
            bytes[i] = randomByte(65, 26);
        }
        return new String(bytes);
    }

    public static String randomLower(int length) {
        byte[] bytes = new byte[length];
        for (int i = 0; i < length; i++) {
            bytes[i] = randomByte(97, 26);
        }
        return new String(bytes);
    }

    public static String randomString(int length) {
        byte[] bytes = new byte[length];
        for (int i = 0; i < length; i++) {
            bytes[i] = random.nextInt() % 2 == 0 ? randomByte(65, 26) : randomByte(97, 26);
        }
        return new String(bytes);
    }

    public static String randomNumber(int length) {
        byte[] bytes = new byte[length];
        for (int i = 0; i < length; i++) {
            bytes[i] = randomByte(48, 10);
        }
        return new String(bytes);
    }

    public static Long randomPositive(int length) {
        if (length <= 0) {
            return 0L;
        }

        Random random = new Random(System.currentTimeMillis());
        long b = (long) Math.pow(10.0, length - 1);
        return random.nextLong(b * 9) + b;
    }

    public static String randomRefreshToken(Long userId, Long deviceId) {
        ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES * 4);
        buffer.putLong(System.currentTimeMillis());
        buffer.putLong(userId);
        buffer.putLong(deviceId);
        buffer.putLong(random.nextLong());
        return DigestUtils.sha256Hex(buffer.array());
    }
}
