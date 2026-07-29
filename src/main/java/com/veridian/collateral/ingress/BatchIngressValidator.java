package com.veridian.collateral.ingress;

import com.veridian.collateral.types.MagicConstants;
import com.veridian.collateral.types.WireFlags;
import com.veridian.collateral.util.BoundedAscii;

public final class BatchIngressValidator {
    public static final class Outcome {
        public boolean valid;
        public String reason;
        public int recordCount;
        public boolean deferredDigest;
    }

    public Outcome validate(byte[] input) {
        Outcome outcome = new Outcome();
        if (input == null || input.length < 16) {
            outcome.reason = "short_header";
            return outcome;
        }
        int magic = readLe32(input, 0);
        if (!MagicConstants.isKnownBatch(magic)) {
            outcome.reason = "bad_magic";
            return outcome;
        }
        int flags = readLe32(input, 4);
        outcome.deferredDigest = WireFlags.has(flags, WireFlags.DEFERRED_DIGEST);
        outcome.recordCount = ((input[8] & 0xFF) << 8) | (input[9] & 0xFF);
        if (outcome.recordCount > 256) {
            outcome.reason = "too_many_records";
            return outcome;
        }
        int offset = 16;
        for (int i = 0; i < outcome.recordCount && offset + 8 <= input.length; i++) {
            int payloadLen = ((input[offset + 1] & 0xFF) << 8) | (input[offset + 2] & 0xFF);
            if (!BoundedAscii.sectionBodyInBounds(input, offset + 8, payloadLen)) {
                outcome.reason = "record_" + i + "_oob";
                return outcome;
            }
            offset += 8 + payloadLen;
        }
        outcome.valid = true;
        outcome.reason = "ok";
        return outcome;
    }

    private int readLe32(byte[] data, int offset) {
        return (data[offset] & 0xFF) | ((data[offset + 1] & 0xFF) << 8)
            | ((data[offset + 2] & 0xFF) << 16) | ((data[offset + 3] & 0xFF) << 24);
    }
}
