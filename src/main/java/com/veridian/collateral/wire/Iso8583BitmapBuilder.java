package com.veridian.collateral.wire;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.Map;

public final class Iso8583BitmapBuilder {
    public byte[] buildAuthorizationRequest(String pan, String processingCode, long amount, int stan) {
        List<Field> fields = new ArrayList<>();
        fields.add(new Field(2, pan));
        fields.add(new Field(3, processingCode));
        fields.add(new Field(4, String.format("%012d", amount)));
        fields.add(new Field(7, "0729120000"));
        fields.add(new Field(11, String.format("%06d", stan)));
        fields.add(new Field(49, "840"));
        return encode("0100", fields);
    }

    public byte[] buildFinancialRequest(String pan, String processingCode, long amount, int stan, String terminalId) {
        List<Field> fields = new ArrayList<>();
        fields.add(new Field(2, pan));
        fields.add(new Field(3, processingCode));
        fields.add(new Field(4, String.format("%012d", amount)));
        fields.add(new Field(7, "0729120000"));
        fields.add(new Field(11, String.format("%06d", stan)));
        fields.add(new Field(41, pad(terminalId, 8)));
        fields.add(new Field(49, "840"));
        return encode("0200", fields);
    }

    public byte[] buildReversal(String pan, String processingCode, long amount, int stan, String originalData) {
        List<Field> fields = new ArrayList<>();
        fields.add(new Field(2, pan));
        fields.add(new Field(3, processingCode));
        fields.add(new Field(4, String.format("%012d", amount)));
        fields.add(new Field(11, String.format("%06d", stan)));
        fields.add(new Field(90, originalData));
        return encode("0400", fields);
    }

    private byte[] encode(String mti, List<Field> fields) {
        BitSet bits = new BitSet(128);
        for (Field f : fields) {
            bits.set(f.number - 1);
        }
        byte[] bitmap = bitSetToBitmap(bits, 64);
        StringBuilder sb = new StringBuilder();
        sb.append(mti);
        for (byte b : bitmap) {
            sb.append(String.format("%02X", b));
        }
        for (Field f : fields) {
            if (f.number == 2 || f.number == 32) {
                sb.append(String.format("%02d", f.value.length()));
            }
            sb.append(f.value);
        }
        return sb.toString().getBytes(StandardCharsets.US_ASCII);
    }

    private byte[] bitSetToBitmap(BitSet bits, int size) {
        byte[] out = new byte[size / 8];
        for (int i = 0; i < size; i++) {
            if (bits.get(i)) {
                out[i / 8] |= (byte) (1 << (7 - (i % 8)));
            }
        }
        return out;
    }

    private String pad(String s, int len) {
        if (s.length() >= len) {
            return s.substring(0, len);
        }
        return s + " ".repeat(len - s.length());
    }

    private static final class Field {
        final int number;
        final String value;
        Field(int number, String value) {
            this.number = number;
            this.value = value;
        }
    }

    public Map<Integer, String> decodeFields(byte[] data, Iso8583MessageParser parser) {
        return parser.parse(data).fields;
    }
}
