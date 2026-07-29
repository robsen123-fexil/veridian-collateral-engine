package com.veridian.collateral.ingress;

import com.veridian.collateral.types.MagicConstants;
import com.veridian.collateral.util.BoundedAscii;

public final class EnvelopeIngressValidator {
    public static final class ValidationOutcome {
        public boolean accepted;
        public String reason;
        public int channelId;
        public int nestedLength;
    }

    public ValidationOutcome validate(byte[] input) {
        ValidationOutcome outcome = new ValidationOutcome();
        if (input == null || input.length < 20) {
            outcome.reason = "too_short";
            return outcome;
        }
        int magic = readLe32(input, 0);
        if (!MagicConstants.isKnownEnvelope(magic)) {
            outcome.reason = "bad_magic";
            return outcome;
        }
        outcome.channelId = readLe32(input, 4);
        outcome.nestedLength = ((input[8] & 0xFF) << 8) | (input[9] & 0xFF);
        if (outcome.nestedLength > 65535) {
            outcome.reason = "nested_too_large";
            return outcome;
        }
        if (!BoundedAscii.sectionBodyInBounds(input, 20, outcome.nestedLength)) {
            outcome.reason = "nested_oob";
            return outcome;
        }
        if (outcome.channelId <= 0) {
            outcome.reason = "invalid_channel";
            return outcome;
        }
        outcome.accepted = true;
        outcome.reason = "ok";
        return outcome;
    }

    private int readLe32(byte[] data, int offset) {
        return (data[offset] & 0xFF) | ((data[offset + 1] & 0xFF) << 8)
            | ((data[offset + 2] & 0xFF) << 16) | ((data[offset + 3] & 0xFF) << 24);
    }
}
