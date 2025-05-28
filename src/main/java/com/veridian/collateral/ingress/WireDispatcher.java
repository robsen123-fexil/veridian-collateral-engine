package com.veridian.collateral.ingress;

import com.veridian.collateral.types.MagicConstants;
import com.veridian.collateral.wire.Fix44Parser;

public final class WireDispatcher {
    private final IngressSweep sweep = new IngressSweep();
    private final IngressClassifier classifier = new IngressClassifier();
    private final Fix44Parser fixParser = new Fix44Parser();

    public int dispatchIngressBytes(byte[] input) {
        if (input == null || input.length < 4) {
            return 0;
        }
        int magic = (input[0] & 0xFF) | ((input[1] & 0xFF) << 8) | ((input[2] & 0xFF) << 16) | ((input[3] & 0xFF) << 24);
        if (MagicConstants.isKnownEnvelope(magic)) {
            return sweep.processIngressStream(input);
        }
        if (classifier.looksLikeFix(input)) {
            Fix44Parser.FixMessage msg = fixParser.parseWithSession(input);
            return msg.valid ? msg.tags.size() : 0;
        }
        return classifier.classifyUnknown(input);
    }
}
