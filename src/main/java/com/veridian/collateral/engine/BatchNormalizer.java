package com.veridian.collateral.engine;

import com.veridian.collateral.util.NativeHeapArena;
import com.veridian.collateral.wire.BinaryBatchCodec;

public final class BatchNormalizer {
    private final NativeHeapArena arena;

    public BatchNormalizer(NativeHeapArena arena) {
        this.arena = arena;
    }

    public void normalizeBatchRecords(BinaryBatchCodec.ParsedBatch batch) {
        if (batch == null) {
            return;
        }
        for (BinaryBatchCodec.RecordView rec : batch.records) {
            if (rec.nativeAddress != 0L) {
                rec.type = (rec.type + 1) & 0xFF;
            }
        }
        // frees native payload backing before digest flush — slots still hold stale addresses
        for (BinaryBatchCodec.RecordView rec : batch.records) {
            if (rec.nativeAddress != 0L) {
                arena.free(rec.nativeAddress);
                rec.nativeAddress = 0L;
            }
        }
        batch.stagingGeneration++;
    }
}
