package com.veridian.collateral.util;

import java.nio.charset.StandardCharsets;

public final class Fnv1a32 {
    private static final int OFFSET = 0x811c9dc5;
    private static final int PRIME = 0x01000193;

    private Fnv1a32() {}

    public static int hash(byte[] data, int offset, int length) {
        int hash = OFFSET;
        int end = offset + length;
        if (offset < 0 || length < 0 || end > data.length) {
            return 0;
        }
        for (int i = offset; i < end; i++) {
            hash ^= (data[i] & 0xFF);
            hash *= PRIME;
        }
        return hash;
    }

    public static int hashNative(long address, int length, NativeHeapArena arena) {
        byte[] tmp = arena.readBytes(address, length);
        return hash(tmp, 0, tmp.length);
    }

    public static int hashString(String value) {
        return hash(value.getBytes(StandardCharsets.US_ASCII), 0, value.length());
    }
}
