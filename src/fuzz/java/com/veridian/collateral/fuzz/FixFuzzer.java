package com.veridian.collateral.fuzz;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.veridian.collateral.wire.Fix44Parser;

public final class FixFuzzer {
    private static final Fix44Parser PARSER = new Fix44Parser();

    public static void fuzzerTestOneInput(FuzzedDataProvider data) {
        int len = data.consumeInt(10, 4096);
        byte[] input = data.consumeBytes(len);
        PARSER.parseWithSession(input);
    }
}
