package com.veridian.collateral.util;

import java.util.Arrays;

public final class DeferredSlotTable {
    public static final class Slot {
        public long address;
        public int length;
        public int generation;
    }

    private Slot[] slots = new Slot[16];
    private int size;

    public void clear() {
        Arrays.fill(slots, 0, size, null);
        size = 0;
    }

    public int register(long address, int length, int generation) {
        if (size >= slots.length) {
            slots = Arrays.copyOf(slots, slots.length * 2);
        }
        Slot slot = new Slot();
        slot.address = address;
        slot.length = length;
        slot.generation = generation;
        slots[size] = slot;
        return size++;
    }

    public Slot get(int index) {
        if (index < 0 || index >= size) {
            return null;
        }
        return slots[index];
    }

    public int size() {
        return size;
    }

    public void invalidateGeneration(int generation) {
        for (int i = 0; i < size; i++) {
            if (slots[i] != null && slots[i].generation == generation) {
                slots[i].address = 0L;
                slots[i].length = 0;
            }
        }
    }
}
