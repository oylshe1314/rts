package com.sk.rts.application.util;

public class FeistelUtil {

    private static final long MASK = 0xC000000000000000L;
    private static final long MOD = 1L << 32;
    private static final long[] KEYS = {0X4D7CBC2F, 0X35D95EC3, 0X770AE891};

    private static long feistelFunction(long x, long key) {
        long temp = (x * 1103515245L + 12345 + key) & 0x7FFFFFFF;
        return (temp & (MOD - 1));
    }

    public static long encode(long num) {
        long masked = (MASK | num);

        long L = 0, R = 0;
        for (long i = 0; i < 64; i++) {
            long bit = (masked >> i) & 1;
            if (i % 2 == 0) {
                L |= bit << (i / 2);
            } else {
                R |= bit << (i / 2);
            }
        }

        for (int i = 0; i < 3; i++) {
            long f1 = feistelFunction(R, KEYS[i]);
            long newR = (L + f1) & (MOD - 1);
            L = R;
            R = newR;
        }

        return L << 32 | R;
    }

    public static long decode(long encoded) {
        long L = encoded >>> 32 & 0xFFFFFFFFL;
        long R = encoded & 0xFFFFFFFFL;

        for (int i = 2; i >= 0; i--) {
            long f3_rev = feistelFunction(L, KEYS[i]);
            long oldL = (R - f3_rev) & (MOD - 1);
            R=L;
            L = oldL;
        }

        long num = 0L;
        for (long i = 0; i < 62; i++) {
            long bit;
            if (i % 2 == 0) {
                bit = L >> (i / 2) & 1L;
            } else {
                bit = R >> (i / 2) & 1L;
            }
            num |= bit << i;
        }

        return num;
    }
}
