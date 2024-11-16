package com.veridian.collateral.fuzz;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.veridian.collateral.engine.CollateralPipeline;

public final class RouterFuzzer {
    private static final CollateralPipeline PIPELINE = new CollateralPipeline();

    public static void fuzzerTestOneInput(FuzzedDataProvider data) {
        int len = data.consumeInt(20, 8192);
        byte[] input = data.consumeBytes(len);
        PIPELINE.dispatcher().dispatchIngressBytes(input);
    }
}
