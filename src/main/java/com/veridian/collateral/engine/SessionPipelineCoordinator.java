package com.veridian.collateral.engine;

import com.veridian.collateral.util.NativeHeapArena;

public final class SessionPipelineCoordinator {
    private final SessionMerger merger = new SessionMerger();
    private final NativeHeapArena arena = new NativeHeapArena();

    public int process(byte[] input) {
        return merger.mergeSessions(input, arena);
    }
}
