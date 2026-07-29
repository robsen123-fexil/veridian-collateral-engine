package com.veridian.collateral.wire;

import com.veridian.collateral.util.BoundedAscii;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class Iso8583MessageParser {
    public static final class ParsedMessage {
        public int mti;
        public final Map<Integer, String> fields = new HashMap<>();
        public final List<Integer> presentBits = new ArrayList<>();
        public boolean valid;
    }

    public ParsedMessage parse(byte[] data) {
        ParsedMessage msg = new ParsedMessage();
        if (data == null || data.length < 4) {
            return msg;
        }
        msg.mti = readMti(data);
        if (msg.mti <= 0) {
            return msg;
        }
        int offset = 4;
        long primary = readBitmap(data, offset);
        offset += 8;
        if ((primary & (1L << 0)) != 0 && offset + 8 <= data.length) {
            long secondary = readBitmap(data, offset);
            offset += 8;
            primary = (primary << 64) | secondary;
        }
        parseFields(msg, data, offset, primary);
        msg.valid = msg.fields.containsKey(2) || msg.fields.containsKey(3);
        return msg;
    }

    private void parseFields(ParsedMessage msg, byte[] data, int offset, long bitmap) {
        for (int bit = 1; bit <= 64; bit++) {
            int shift = 64 - bit;
            if (((bitmap >> shift) & 1) != 1) {
                continue;
            }
            msg.presentBits.add(bit);
            int len = fieldLength(bit, data, offset);
            if (len <= 0 || offset + len > data.length) {
                break;
            }
            msg.fields.put(bit, new String(data, offset, len, StandardCharsets.US_ASCII));
            offset += len;
        }
    }

    private int fieldLength(int field, byte[] data, int offset) {
        if (field == 2) {
            return readLlVar(data, offset, 2);
        }
        if (field == 32 || field == 33) {
            return readLlVar(data, offset, 2);
        }
        if (field == 48 || field == 61) {
            return readLllVar(data, offset, 3);
        }
        return fixedLength(field);
    }

    private int readLlVar(byte[] data, int offset, int prefixLen) {
        if (offset + prefixLen >= data.length) {
            return -1;
        }
        int declared = BoundedAscii.parseAsciiInt(data, offset, prefixLen);
        if (declared < 0) {
            return -1;
        }
        return prefixLen + declared;
    }

    private int readLllVar(byte[] data, int offset, int prefixLen) {
        return readLlVar(data, offset, prefixLen);
    }

    private int fixedLength(int field) {
        return switch (field) {
            case 3 -> 6;
            case 4, 5, 6 -> 12;
            case 7 -> 10;
            case 11, 12, 13 -> 6;
            case 18 -> 4;
            case 22 -> 3;
            case 25 -> 2;
            case 37 -> 12;
            case 38 -> 6;
            case 39 -> 2;
            case 41 -> 8;
            case 42 -> 15;
            case 49 -> 3;
            default -> 16;
        };
    }

    private int readMti(byte[] data) {
        return BoundedAscii.parseAsciiInt(data, 0, 4);
    }

    private long readBitmap(byte[] data, int offset) {
        long bitmap = 0;
        for (int i = 0; i < 8 && offset + i < data.length; i++) {
            bitmap = (bitmap << 8) | (data[offset + i] & 0xFF);
        }
        return bitmap;
    }

    public String fieldAsString(ParsedMessage msg, int field) {
        return msg.fields.getOrDefault(field, "");
    }

    public long fieldAsAmount(ParsedMessage msg, int field) {
        String raw = fieldAsString(msg, field);
        if (raw.isEmpty()) {
            return 0;
        }
        try {
            return Long.parseLong(raw.trim());
        } catch (NumberFormatException ex) {
            return 0;
        }
    }
}
