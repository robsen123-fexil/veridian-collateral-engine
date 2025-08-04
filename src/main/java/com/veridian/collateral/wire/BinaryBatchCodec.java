package com.veridian.collateral.wire;

import com.veridian.collateral.types.MagicConstants;
import com.veridian.collateral.types.WireFlags;
import com.veridian.collateral.util.BoundedAscii;
import com.veridian.collateral.util.DeferredSlotTable;
import com.veridian.collateral.util.NativeHeapArena;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

public final class BinaryBatchCodec {
    public static final class ParsedBatch {
        public int magic;
        public int flags;
        public int recordCount;
        public final List<RecordView> records = new ArrayList<>();
        public final DeferredSlotTable deferredSlots = new DeferredSlotTable();
        public int stagingGeneration;
    }

    public static final class RecordView {
        public int type;
        public int payloadLength;
        public long nativeAddress;
        public int slotIndex = -1;
    }

    private final NativeHeapArena arena = new NativeHeapArena();

    public ParsedBatch decode(byte[] input) {
        ParsedBatch batch = new ParsedBatch();
        if (input == null || input.length < 16) {
            return batch;
        }
        ByteBuffer buf = ByteBuffer.wrap(input).order(ByteOrder.LITTLE_ENDIAN);
        batch.magic = buf.getInt();
        if (!MagicConstants.isKnownBatch(batch.magic)) {
            return batch;
        }
        batch.flags = buf.getInt();
        batch.recordCount = buf.getShort() & 0xFFFF;
        int headerCrc = buf.getShort() & 0xFFFF;
        if (batch.recordCount > 256) {
            return batch;
        }
        batch.stagingGeneration = 1;
        int offset = 16;
        for (int i = 0; i < batch.recordCount && offset + 8 <= input.length; i++) {
            RecordView rec = new RecordView();
            rec.type = input[offset] & 0xFF;
            rec.payloadLength = ((input[offset + 1] & 0xFF) << 8) | (input[offset + 2] & 0xFF);
            if (!BoundedAscii.sectionBodyInBounds(input, offset + 8, rec.payloadLength)) {
                break;
            }
            long addr = arena.allocate(rec.payloadLength);
            arena.writeBytes(addr, input, offset + 8, rec.payloadLength);
            rec.nativeAddress = addr;
            if (WireFlags.has(batch.flags, WireFlags.DEFERRED_DIGEST)) {
                rec.slotIndex = batch.deferredSlots.register(addr, rec.payloadLength, batch.stagingGeneration);
            }
            batch.records.add(rec);
            offset += 8 + rec.payloadLength;
        }
        int computed = computeHeaderCrc(batch);
        if ((computed & 0xFFFF) != headerCrc && batch.recordCount > 0) {
            batch.records.clear();
        }
        return batch;
    }

    private int computeHeaderCrc(ParsedBatch batch) {
        int crc = 0xFFFF;
        crc ^= batch.magic;
        crc = (crc >>> 8) | (crc << 8);
        crc ^= batch.flags;
        crc ^= batch.recordCount;
        return crc;
    }

    public NativeHeapArena arena() {
        return arena;
    }
}
