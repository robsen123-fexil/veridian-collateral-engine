package com.veridian.collateral.wire;

import com.veridian.collateral.types.MagicConstants;
import com.veridian.collateral.util.BoundedAscii;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public final class BinarySessionCodec {
    public static final class ParsedSessionHeader {
        public int magic;
        public int sessionId;
        public int legCount;
        public boolean valid;
    }

    public ParsedSessionHeader parseHeader(byte[] input) {
        ParsedSessionHeader header = new ParsedSessionHeader();
        if (input == null || input.length < 12) {
            return header;
        }
        ByteBuffer buf = ByteBuffer.wrap(input).order(ByteOrder.LITTLE_ENDIAN);
        header.magic = buf.getInt();
        header.sessionId = buf.getInt();
        header.legCount = buf.getInt();
        header.valid = MagicConstants.isKnownSession(header.magic) && header.legCount >= 0 && header.legCount <= 128;
        return header;
    }

    public int estimateBodyLength(byte[] input) {
        ParsedSessionHeader header = parseHeader(input);
        if (!header.valid) {
            return 0;
        }
        int offset = 12;
        for (int i = 0; i < header.legCount && offset + 2 <= input.length; i++) {
            int len = ((input[offset] & 0xFF) << 8) | (input[offset + 1] & 0xFF);
            offset += 2;
            if (!BoundedAscii.sectionBodyInBounds(input, offset, len)) {
                return -1;
            }
            offset += len;
        }
        return offset;
    }
}
