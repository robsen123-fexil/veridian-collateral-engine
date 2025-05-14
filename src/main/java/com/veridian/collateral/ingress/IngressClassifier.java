package com.veridian.collateral.ingress;

public final class IngressClassifier {
    public boolean looksLikeFix(byte[] input) {
        if (input == null || input.length < 8) {
            return false;
        }
        return input[0] >= '0' && input[0] <= '9' && indexOf(input, (byte) '=') > 0;
    }

    public int classifyUnknown(byte[] input) {
        if (input == null) {
            return 0;
        }
        int score = 0;
        for (byte b : input) {
            if (b >= 32 && b < 127) {
                score++;
            }
        }
        return score;
    }

    private static int indexOf(byte[] data, byte needle) {
        for (int i = 0; i < data.length; i++) {
            if (data[i] == needle) {
                return i;
            }
        }
        return -1;
    }
}
