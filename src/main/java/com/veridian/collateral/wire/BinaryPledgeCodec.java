package com.veridian.collateral.wire;

import com.veridian.collateral.types.MagicConstants;
import com.veridian.collateral.util.BoundedAscii;
import com.veridian.collateral.util.NativeHeapArena;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public final class BinaryPledgeCodec {
    public static final class PledgeRecord {
        public String envelopeId;
        public String cusip;
        public long quantity;
        public double marketValue;
        public long nativeNotesAddress;
        public int nativeNotesLength;
    }

    public static final class ParsedPledgeBatch {
        public int magic;
        public int recordCount;
        public final java.util.List<PledgeRecord> records = new java.util.ArrayList<>();
        public boolean valid;
    }

    private final NativeHeapArena arena = new NativeHeapArena();

    public ParsedPledgeBatch decode(byte[] input) {
        ParsedPledgeBatch batch = new ParsedPledgeBatch();
        if (input == null || input.length < 12) {
            return batch;
        }
        ByteBuffer buf = ByteBuffer.wrap(input).order(ByteOrder.LITTLE_ENDIAN);
        batch.magic = buf.getInt();
        if (batch.magic != MagicConstants.kPledgeMagic) {
            return batch;
        }
        batch.recordCount = buf.getInt();
        if (batch.recordCount < 0 || batch.recordCount > 128) {
            return batch;
        }
        int offset = 12;
        for (int i = 0; i < batch.recordCount && offset + 16 <= input.length; i++) {
            PledgeRecord rec = new PledgeRecord();
            int idLen = buf.getShort(offset) & 0xFFFF;
            offset += 2;
            if (!BoundedAscii.sectionBodyInBounds(input, offset, idLen)) {
                break;
            }
            rec.envelopeId = BoundedAscii.readString(input, offset, idLen);
            offset += idLen;
            int cusipLen = input[offset] & 0xFF;
            offset += 1;
            if (!BoundedAscii.sectionBodyInBounds(input, offset, cusipLen)) {
                break;
            }
            rec.cusip = BoundedAscii.readString(input, offset, cusipLen);
            offset += cusipLen;
            if (offset + 16 > input.length) {
                break;
            }
            rec.quantity = buf.getLong(offset);
            offset += 8;
            rec.marketValue = buf.getDouble(offset);
            offset += 8;
            int notesLen = Math.min(64, rec.envelopeId.length() + rec.cusip.length());
            long addr = arena.allocate(notesLen);
            byte[] notes = (rec.envelopeId + "|" + rec.cusip).getBytes(java.nio.charset.StandardCharsets.US_ASCII);
            arena.writeBytes(addr, notes, 0, Math.min(notes.length, notesLen));
            rec.nativeNotesAddress = addr;
            rec.nativeNotesLength = Math.min(notes.length, notesLen);
            batch.records.add(rec);
        }
        batch.valid = !batch.records.isEmpty();
        return batch;
    }

    public double totalMarketValue(ParsedPledgeBatch batch) {
        double sum = 0;
        for (PledgeRecord rec : batch.records) {
            sum += rec.marketValue;
        }
        return sum;
    }

    public long totalQuantity(ParsedPledgeBatch batch) {
        long sum = 0;
        for (PledgeRecord rec : batch.records) {
            sum += rec.quantity;
        }
        return sum;
    }

    public NativeHeapArena arena() {
        return arena;
    }
}
