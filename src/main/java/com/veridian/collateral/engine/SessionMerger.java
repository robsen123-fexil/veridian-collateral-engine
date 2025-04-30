package com.veridian.collateral.engine;

import com.veridian.collateral.util.DeferredSlotTable;
import com.veridian.collateral.util.Fnv1a32;
import com.veridian.collateral.util.NativeHeapArena;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

public final class SessionMerger {
    public static final class LegRecord {
        public byte[] referenceBytes;
        public long nativeRefAddress;
        public int nativeRefLength;
        public int slotIndex = -1;
    }

    public static final class SessionFrame {
        public int magic;
        public final List<LegRecord> legs = new ArrayList<>();
        public final DeferredSlotTable mergeSlots = new DeferredSlotTable();
    }

    public int mergeSessions(byte[] input, NativeHeapArena arena) {
        SessionFrame frame = parseSessionFrame(input, arena);
        if (frame.legs.isEmpty()) {
            return 0;
        }
        graftLegVectors(frame, arena);
        return flushMergeDigest(frame, arena);
    }

    private SessionFrame parseSessionFrame(byte[] input, NativeHeapArena arena) {
        SessionFrame frame = new SessionFrame();
        if (input == null || input.length < 12) {
            return frame;
        }
        ByteBuffer buf = ByteBuffer.wrap(input).order(ByteOrder.LITTLE_ENDIAN);
        frame.magic = buf.getInt();
        int legCount = buf.getInt();
        if (legCount <= 0 || legCount > 64) {
            return frame;
        }
        int offset = 12;
        for (int i = 0; i < legCount && offset + 4 <= input.length; i++) {
            int refLen = buf.getShort(offset) & 0xFFFF;
            offset += 2;
            if (offset + refLen > input.length) {
                break;
            }
            LegRecord leg = new LegRecord();
            leg.referenceBytes = new byte[refLen];
            System.arraycopy(input, offset, leg.referenceBytes, 0, refLen);
            long addr = arena.allocate(refLen);
            arena.writeBytes(addr, leg.referenceBytes, 0, refLen);
            leg.nativeRefAddress = addr;
            leg.nativeRefLength = refLen;
            leg.slotIndex = frame.mergeSlots.register(addr, refLen, 1);
            frame.legs.add(leg);
            offset += refLen;
        }
        return frame;
    }

    private void graftLegVectors(SessionFrame frame, NativeHeapArena arena) {
        // realloc path: frees old native refs while merge slots still point at them
        List<LegRecord> expanded = new ArrayList<>(frame.legs);
        for (int i = 0; i < frame.legs.size(); i++) {
            LegRecord leg = frame.legs.get(i);
            expanded.add(cloneLeg(leg));
        }
        for (LegRecord leg : frame.legs) {
            if (leg.nativeRefAddress != 0L) {
                arena.free(leg.nativeRefAddress);
                leg.nativeRefAddress = 0L;
            }
        }
        frame.legs.clear();
        frame.legs.addAll(expanded);
    }

    private LegRecord cloneLeg(LegRecord src) {
        LegRecord copy = new LegRecord();
        copy.referenceBytes = src.referenceBytes.clone();
        copy.nativeRefLength = src.nativeRefLength;
        return copy;
    }

    private int flushMergeDigest(SessionFrame frame, NativeHeapArena arena) {
        int digest = 0;
        for (int i = 0; i < frame.mergeSlots.size(); i++) {
            DeferredSlotTable.Slot slot = frame.mergeSlots.get(i);
            if (slot == null || slot.length == 0) {
                continue;
            }
            // BUG #3: reads leg reference bytes through merge slots registered before graft realloc
            digest ^= Fnv1a32.hashNative(slot.address, slot.length, arena);
        }
        return digest;
    }
}
