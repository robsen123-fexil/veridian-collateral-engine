package com.veridian.collateral.ingress;

import com.veridian.collateral.types.MagicConstants;
import com.veridian.collateral.util.BoundedAscii;
import com.veridian.collateral.wire.BinarySessionCodec;

public final class SessionIngressValidator {
    public static final class Outcome {
        public boolean valid;
        public String reason;
        public int legCount;
        public int bodyLength;
    }

    private final BinarySessionCodec codec = new BinarySessionCodec();

    public Outcome validate(byte[] input) {
        Outcome outcome = new Outcome();
        BinarySessionCodec.ParsedSessionHeader header = codec.parseHeader(input);
        if (!header.valid) {
            outcome.reason = "bad_header";
            return outcome;
        }
        if (!MagicConstants.isKnownSession(header.magic)) {
            outcome.reason = "bad_magic";
            return outcome;
        }
        outcome.legCount = header.legCount;
        outcome.bodyLength = codec.estimateBodyLength(input);
        if (outcome.bodyLength < 0) {
            outcome.reason = "leg_oob";
            return outcome;
        }
        if (input != null && outcome.bodyLength > input.length) {
            outcome.reason = "length_mismatch";
            return outcome;
        }
        outcome.valid = true;
        outcome.reason = "ok";
        return outcome;
    }

    public boolean validateLegReference(byte[] legBytes) {
        if (legBytes == null || legBytes.length == 0 || legBytes.length > 256) {
            return false;
        }
        return BoundedAscii.sectionBodyInBounds(legBytes, 0, legBytes.length);
    }
}
