package com.veridian.collateral.engine;

import com.veridian.collateral.wire.BinaryBatchCodec;

public final class BatchPipelineCoordinator {
    private final BinaryBatchCodec codec = new BinaryBatchCodec();
    private final BatchNormalizer normalizer;
    private final BatchDigest digest = new BatchDigest();

    public BatchPipelineCoordinator() {
        this.normalizer = new BatchNormalizer(codec.arena());
    }

    public int coordinate(byte[] input) {
        BinaryBatchCodec.ParsedBatch batch = codec.decode(input);
        if (batch.records.isEmpty()) {
            return 0;
        }
        normalizer.normalizeBatchRecords(batch);
        return digest.flushBatchDigest(batch, codec.arena());
    }
}
