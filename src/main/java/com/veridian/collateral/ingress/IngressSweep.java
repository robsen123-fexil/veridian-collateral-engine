package com.veridian.collateral.ingress;

import com.veridian.collateral.engine.ChannelTape;
import com.veridian.collateral.wire.EnvelopeWireCodec;

public final class IngressSweep {
    private final EnvelopeWireCodec codec = new EnvelopeWireCodec();
    private final ChannelTape tape = new ChannelTape();

    public int processIngressStream(byte[] input) {
        EnvelopeWireCodec.ParsedEnvelope env = codec.parseEnvelope(input);
        if (!env.valid) {
            return 0;
        }
        tape.queueEnvelopeChannel(env, codec.arena());
        codec.destroyNestedStorage(env);
        return tape.sealDeferredChannels(codec.arena());
    }
}
