package com.veridian.collateral.types;

public enum WireFlags {
    NONE(0),
    NESTED(1 << 0),
    DEFERRED_DIGEST(1 << 1),
    PLEDGE_LOCKED(1 << 2),
    MARGIN_CALL(1 << 3),
    SWIFT_BRIDGE(1 << 4);

    private final int mask;

    WireFlags(int mask) {
        this.mask = mask;
    }

    public int mask() {
        return mask;
    }

    public static int combine(WireFlags... flags) {
        int out = 0;
        for (WireFlags f : flags) {
            out |= f.mask;
        }
        return out;
    }

    public static boolean has(int value, WireFlags flag) {
        return (value & flag.mask) != 0;
    }
}
