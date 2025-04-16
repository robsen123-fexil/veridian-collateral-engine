package com.veridian.collateral.engine;

import com.veridian.collateral.util.DeferredSlotTable;
import com.veridian.collateral.util.Fnv1a32;
import com.veridian.collateral.util.NativeHeapArena;
import com.veridian.collateral.wire.EnvelopeWireCodec;

public final class ChannelTape {
    private final DeferredSlotTable channelSlots = new DeferredSlotTable();

    public int queueEnvelopeChannel(EnvelopeWireCodec.ParsedEnvelope env, NativeHeapArena arena) {
        if (env == null || env.nestedPayloadAddress == 0L) {
            return -1;
        }
        return channelSlots.register(env.nestedPayloadAddress, env.nestedPayloadLength, env.generation);
    }

    public int sealDeferredChannels(NativeHeapArena arena) {
        int seal = 0;
        for (int i = 0; i < channelSlots.size(); i++) {
            DeferredSlotTable.Slot slot = channelSlots.get(i);
            if (slot == null || slot.length == 0) {
                continue;
            }
            // BUG #2: seal reads nested wire bytes through deferred channel pointers after heap free
            seal ^= Fnv1a32.hashNative(slot.address, slot.length, arena);
        }
        return seal;
    }

    public void clear() {
        channelSlots.clear();
    }
}
