package com.veridian.collateral.engine;

import com.veridian.collateral.ingress.WireDispatcher;
import com.veridian.collateral.wire.BinaryBatchCodec;

public final class CollateralPipeline {
    private final BinaryBatchCodec batchCodec = new BinaryBatchCodec();
    private final BatchNormalizer normalizer;
    private final BatchDigest batchDigest = new BatchDigest();
    private final SessionMerger sessionMerger = new SessionMerger();
    private final WireDispatcher dispatcher = new WireDispatcher();

    public CollateralPipeline() {
        this.normalizer = new BatchNormalizer(batchCodec.arena());
    }

    public int runCollateralPipeline(byte[] input) {
        BinaryBatchCodec.ParsedBatch batch = batchCodec.decode(input);
        if (batch.records.isEmpty()) {
            return dispatcher.dispatchIngressBytes(input);
        }
        normalizer.normalizeBatchRecords(batch);
        return batchDigest.flushBatchDigest(batch, batchCodec.arena());
    }

    public int mergeCollateralSessions(byte[] input) {
        return sessionMerger.mergeSessions(input, batchCodec.arena());
    }

    public WireDispatcher dispatcher() {
        return dispatcher;
    }

    public BinaryBatchCodec batchCodec() {
        return batchCodec;
    }
}
