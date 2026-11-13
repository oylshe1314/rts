package com.sk.rts.application.util;

import org.apache.commons.codec.digest.DigestUtils;

import java.nio.ByteBuffer;
import java.util.concurrent.ThreadLocalRandom;

public final class RandomUtil {
    private static final byte[] NUMBERS= "0123456789".getBytes();
    private static final byte[] UPPERS= "ABCDEFGHIJKLMNOPQRSTUVWXYZ".getBytes();
    private static final byte[] LOWERS= "abcdefghijklmnopqrstuvwxyz".getBytes();
    private static final byte[] LETTERS= "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz".getBytes();
    private static final byte[] CHARACTERS= "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz".getBytes();

    private static String randomString(byte[] characters, int length) {
        byte[] bytes = new byte[length];
        for (int i = 0; i < length; i++) {
            bytes[i] = characters[ThreadLocalRandom.current().nextInt(characters.length)];
        }
        return new String(bytes);
    }

    public static String randomNumbers(int length) {
        return randomString(NUMBERS, length);
    }

    public static String randomUppers(int length) {
        return randomString(UPPERS, length);
    }

    public static String randomLowers(int length) {
        return randomString(LOWERS, length);
    }

    public static String randomLetters(int length) {
        return randomString(LETTERS, length);
    }

    public static String randomCharacters(int length) {
        return randomString(CHARACTERS, length);
    }

    public static String randomHash(Long number1, Long number2) {
        ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES * 4);
        buffer.putLong(System.currentTimeMillis());
        buffer.putLong(number1);
        buffer.putLong(number2);
        buffer.putLong(ThreadLocalRandom.current().nextLong());
        return DigestUtils.sha256Hex(buffer.array());
    }
}
