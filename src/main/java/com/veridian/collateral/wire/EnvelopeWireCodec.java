package com.veridian.collateral.wire;

import com.veridian.collateral.types.MagicConstants;
import com.veridian.collateral.util.BoundedAscii;
import com.veridian.collateral.util.NativeHeapArena;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public final class EnvelopeWireCodec {
    public static final class ParsedEnvelope {
        public int magic;
        public int channelId;
        public int generation = 1;
        public long nestedPayloadAddress;
        public int nestedPayloadLength;
        public boolean valid;
    }

    private final NativeHeapArena arena = new NativeHeapArena();

    public ParsedEnvelope parseEnvelope(byte[] input) {
        ParsedEnvelope env = new ParsedEnvelope();
        if (input == null || input.length < 20) {
            return env;
        }
        ByteBuffer buf = ByteBuffer.wrap(input).order(ByteOrder.LITTLE_ENDIAN);
        env.magic = buf.getInt();
        if (!MagicConstants.isKnownEnvelope(env.magic)) {
            return env;
        }
        env.channelId = buf.getInt();
        env.nestedPayloadLength = buf.getShort() & 0xFFFF;
        int nestedOffset = 20;
        if (!BoundedAscii.sectionBodyInBounds(input, nestedOffset, env.nestedPayloadLength)) {
            return env;
        }
        long addr = arena.allocate(env.nestedPayloadLength);
        arena.writeBytes(addr, input, nestedOffset, env.nestedPayloadLength);
        env.nestedPayloadAddress = addr;
        env.valid = true;
        return env;
    }

    public void destroyNestedStorage(ParsedEnvelope env) {
        if (env != null && env.nestedPayloadAddress != 0L) {
            arena.free(env.nestedPayloadAddress);
            env.nestedPayloadAddress = 0L;
            env.generation++;
        }
    }

    public NativeHeapArena arena() {
        return arena;
    }
}
