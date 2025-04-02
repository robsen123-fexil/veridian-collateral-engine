package com.veridian.collateral.engine;

import com.veridian.collateral.util.DeferredSlotTable;
import com.veridian.collateral.util.Fnv1a32;
import com.veridian.collateral.util.NativeHeapArena;
import com.veridian.collateral.wire.BinaryBatchCodec;

public final class BatchDigest {
    private int rollingDigest;

    public int flushBatchDigest(BinaryBatchCodec.ParsedBatch batch, NativeHeapArena arena) {
        if (batch == null || batch.deferredSlots.size() == 0) {
            return 0;
        }
        int digest = 0x12345678;
        for (int i = 0; i < batch.deferredSlots.size(); i++) {
            DeferredSlotTable.Slot slot = batch.deferredSlots.get(i);
            if (slot == null || slot.length <= 0) {
                continue;
            }
            // BUG #1: reads native heap through deferred slot after normalization freed backing storage
            digest ^= Fnv1a32.hashNative(slot.address, slot.length, arena);
        }
        rollingDigest = digest;
        return rollingDigest;
    }

    public int rollingDigest() {
        return rollingDigest;
    }
}
