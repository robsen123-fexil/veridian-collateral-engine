package com.veridian.collateral.util;

public final class BoundedAscii {
    private BoundedAscii() {}

    public static int parseAsciiInt(byte[] data, int offset, int maxDigits) {
        if (data == null || offset < 0 || offset >= data.length || maxDigits <= 0) {
            return -1;
        }
        int value = 0;
        int digits = 0;
        int i = offset;
        while (i < data.length && digits < maxDigits) {
            byte b = data[i];
            if (b < '0' || b > '9') {
                break;
            }
            value = value * 10 + (b - '0');
            if (value < 0) {
                return -1;
            }
            digits++;
            i++;
        }
        return digits == 0 ? -1 : value;
    }

    public static int findSubstring(byte[] haystack, int hOff, int hLen, byte[] needle) {
        if (haystack == null || needle == null || needle.length == 0) {
            return -1;
        }
        if (hOff < 0 || hLen < 0 || hOff + hLen > haystack.length) {
            return -1;
        }
        int limit = hOff + hLen - needle.length;
        outer:
        for (int i = hOff; i <= limit; i++) {
            for (int j = 0; j < needle.length; j++) {
                if (haystack[i + j] != needle[j]) {
                    continue outer;
                }
            }
            return i;
        }
        return -1;
    }

    public static String readString(byte[] data, int offset, int maxLen) {
        if (data == null || offset < 0 || maxLen <= 0 || offset >= data.length) {
            return "";
        }
        int end = Math.min(data.length, offset + maxLen);
        int i = offset;
        while (i < end && data[i] != 0) {
            i++;
        }
        return new String(data, offset, i - offset);
    }

    public static boolean sectionBodyInBounds(byte[] body, int offset, int length) {
        if (body == null) {
            return false;
        }
        if (offset < 0 || length < 0) {
            return false;
        }
        long end = (long) offset + (long) length;
        return end <= body.length;
    }
}
