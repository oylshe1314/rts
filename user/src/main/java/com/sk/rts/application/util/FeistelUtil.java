package com.sk.rts.application.util;

public class FeistelUtil {
    private static final int TOTAL_BITS = 34;
    private static final int HALF_BITS = 17;
    private static final int MOD = 1 << HALF_BITS;
    private static final long TOP_MASK = 0x3L << (TOTAL_BITS - 2); // 0x600000000L
    private static final int[] ROUND_KEYS = {0x1234, 0x5678, 0x9ABC};
    private static final long BASE = 1_000_000_000L;
    private static final long RANGE = 9_000_000_000L;

    private static int feistelFunction(int x, int key) {
        // 使用线性同余 + 异或制造非线性
        long temp = (x * 1103515245L + 12345 + key) & 0x7FFFFFFF;
        // 取低17位
        return (int) (temp & (MOD - 1));
    }

    private static long interleave(int L, int R) {
        long result = 0;
        for (int i = 0; i < TOTAL_BITS; i++) {
            int bit;
            if (i % 2 == 0) {
                bit = (L >> (i / 2)) & 1;
            } else {
                bit = (R >> (i / 2)) & 1;
            }
            result |= ((long) bit << i);
        }
        return result;
    }

    public static long encode(int originalId) {
        long maskedId = (originalId & 0xFFFFFFFFL) | TOP_MASK;

        int L = 0, R = 0;
        for (int i = 0; i < TOTAL_BITS; i++) {
            int bit = (int) ((maskedId >> i) & 1);
            if (i % 2 == 0) {
                L |= (bit << (i / 2));
            } else {
                R |= (bit << (i / 2));
            }
        }

        for (int round = 0; round < 3; round++) {
            int f = feistelFunction(R, ROUND_KEYS[round]);
            int newR = (L + f) & (MOD - 1);
            L = R;
            R = newR;
        }

        long result = interleave(L, R);
        return (result % RANGE) + BASE;
    }

    public static int decode(long obfuscated) {
        long maskedId = (obfuscated - BASE) % RANGE;

        int L = 0, R = 0;
        for (int i = 0; i < TOTAL_BITS; i++) {
            int bit = (int) ((maskedId >> i) & 1);
            if (i % 2 == 0) {
                L |= (bit << (i / 2));
            } else {
                R |= (bit << (i / 2));
            }
        }

        for (int round = 2; round >= 0; round--) {
            int f = feistelFunction(R, ROUND_KEYS[round]);
            int oldL = (L - f) & (MOD - 1);
            L = R;
            R = oldL;
        }

        long decodedMasked = interleave(L, R);
        return (int) (decodedMasked & ~TOP_MASK);
    }
}
