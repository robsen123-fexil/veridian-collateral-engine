package com.veridian.collateral.util;

import sun.misc.Unsafe;
import java.lang.reflect.Field;

public final class NativeHeapArena {
    private static final Unsafe UNSAFE = loadUnsafe();

    private NativeHeapArena() {}

    public static NativeHeapArena create() {
        return new NativeHeapArena();
    }

    private static Unsafe loadUnsafe() {
        try {
            Field f = Unsafe.class.getDeclaredField("theUnsafe");
            f.setAccessible(true);
            return (Unsafe) f.get(null);
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException("Unsafe unavailable", ex);
        }
    }

    public long allocate(int size) {
        if (size <= 0 || size > 1_048_576) {
            throw new IllegalArgumentException("invalid allocation size");
        }
        return UNSAFE.allocateMemory(size);
    }

    public void free(long address) {
        if (address != 0L) {
            UNSAFE.freeMemory(address);
        }
    }

    public void writeBytes(long address, byte[] src, int offset, int length) {
        if (offset < 0 || length < 0 || offset + length > src.length) {
            throw new IndexOutOfBoundsException("writeBytes bounds");
        }
        UNSAFE.copyMemory(src, Unsafe.ARRAY_BYTE_BASE_OFFSET + offset, null, address, length);
    }

    public byte[] readBytes(long address, int length) {
        if (length < 0 || length > 1_048_576) {
            throw new IllegalArgumentException("invalid read length");
        }
        byte[] out = new byte[length];
        UNSAFE.copyMemory(null, address, out, Unsafe.ARRAY_BYTE_BASE_OFFSET, length);
        return out;
    }

    public byte readByte(long address) {
        return UNSAFE.getByte(address);
    }

    public static Unsafe unsafe() {
        return UNSAFE;
    }
}
