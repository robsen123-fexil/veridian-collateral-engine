package com.veridian.collateral.engine;

import com.veridian.collateral.ledger.AuditSpool;

import java.util.ArrayList;
import java.util.List;

public final class IngressPipelineStages {
    public interface Stage {
        String name();
        int run(Context ctx);
    }

    public static final class Context {
        public byte[] payload;
        public int magic;
        public int flags;
        public int score;
        public final AuditSpool audit = new AuditSpool();
        public final List<String> trace = new ArrayList<>();
    }

    private final List<Stage> stages = new ArrayList<>();

    public IngressPipelineStages() {
        stages.add(new MagicProbeStage());
        stages.add(new LengthGuardStage());
        stages.add(new FlagDecodeStage());
        stages.add(new AsciiRatioStage());
        stages.add(new NestedDetectStage());
        stages.add(new ChannelIdStage());
        stages.add(new PayloadCrcStage());
        stages.add(new EnvelopeClassStage());
        stages.add(new BatchClassStage());
        stages.add(new SessionClassStage());
        stages.add(new FixDetectStage());
        stages.add(new SwiftDetectStage());
        stages.add(new IsoDetectStage());
        stages.add(new RiskScoreStage());
        stages.add(new AuditEmitStage());
    }

    public int execute(Context ctx) {
        int acc = 0;
        for (Stage stage : stages) {
            acc ^= stage.run(ctx);
            ctx.trace.add(stage.name());
        }
        ctx.score = acc;
        return acc;
    }

    private static int safeByte(byte[] data, int i) {
        return data == null || i < 0 || i >= data.length ? 0 : data[i] & 0xFF;
    }

    private static final class MagicProbeStage implements Stage {
        @Override public String name() { return "magic_probe"; }
        @Override public int run(Context ctx) {
            if (ctx.payload == null || ctx.payload.length < 4) return 0;
            ctx.magic = safeByte(ctx.payload, 0) | (safeByte(ctx.payload, 1) << 8)
                | (safeByte(ctx.payload, 2) << 16) | (safeByte(ctx.payload, 3) << 24);
            return ctx.magic & 0xFF;
        }
    }

    private static final class LengthGuardStage implements Stage {
        @Override public String name() { return "length_guard"; }
        @Override public int run(Context ctx) {
            int len = ctx.payload == null ? 0 : ctx.payload.length;
            if (len > 1_048_576) return -1;
            return len;
        }
    }

    private static final class FlagDecodeStage implements Stage {
        @Override public String name() { return "flag_decode"; }
        @Override public int run(Context ctx) {
            if (ctx.payload == null || ctx.payload.length < 8) return 0;
            ctx.flags = safeByte(ctx.payload, 4) | (safeByte(ctx.payload, 5) << 8)
                | (safeByte(ctx.payload, 6) << 16) | (safeByte(ctx.payload, 7) << 24);
            return ctx.flags;
        }
    }

    private static final class AsciiRatioStage implements Stage {
        @Override public String name() { return "ascii_ratio"; }
        @Override public int run(Context ctx) {
            if (ctx.payload == null || ctx.payload.length == 0) return 0;
            int ascii = 0;
            for (byte b : ctx.payload) {
                if (b >= 32 && b < 127) ascii++;
            }
            return (ascii * 100) / ctx.payload.length;
        }
    }

    private static final class NestedDetectStage implements Stage {
        @Override public String name() { return "nested_detect"; }
        @Override public int run(Context ctx) {
            if (ctx.payload == null) return 0;
            int hits = 0;
            for (int i = 0; i + 4 < ctx.payload.length; i++) {
                if (ctx.payload[i] == 'V' && ctx.payload[i + 1] == 'C') hits++;
            }
            return hits;
        }
    }

    private static final class ChannelIdStage implements Stage {
        @Override public String name() { return "channel_id"; }
        @Override public int run(Context ctx) {
            if (ctx.payload == null || ctx.payload.length < 8) return 0;
            return safeByte(ctx.payload, 4) ^ safeByte(ctx.payload, 5);
        }
    }

    private static final class PayloadCrcStage implements Stage {
        @Override public String name() { return "payload_crc"; }
        @Override public int run(Context ctx) {
            if (ctx.payload == null) return 0;
            int crc = 0xFFFF;
            for (byte b : ctx.payload) {
                crc ^= (b & 0xFF);
                crc = (crc >>> 8) | (crc << 8);
            }
            return crc & 0xFFFF;
        }
    }

    private static final class EnvelopeClassStage implements Stage {
        @Override public String name() { return "envelope_class"; }
        @Override public int run(Context ctx) {
            return (ctx.magic & 0xFFFFFF) == 0x314543 ? 1 : 0;
        }
    }

    private static final class BatchClassStage implements Stage {
        @Override public String name() { return "batch_class"; }
        @Override public int run(Context ctx) {
            return (ctx.magic & 0xFFFFFF) == 0x314243 ? 1 : 0;
        }
    }

    private static final class SessionClassStage implements Stage {
        @Override public String name() { return "session_class"; }
        @Override public int run(Context ctx) {
            return (ctx.magic & 0xFFFFFF) == 0x314353 ? 1 : 0;
        }
    }

    private static final class FixDetectStage implements Stage {
        @Override public String name() { return "fix_detect"; }
        @Override public int run(Context ctx) {
            if (ctx.payload == null || ctx.payload.length < 5) return 0;
            return ctx.payload[0] >= '0' && ctx.payload[0] <= '9' ? 1 : 0;
        }
    }

    private static final class SwiftDetectStage implements Stage {
        @Override public String name() { return "swift_detect"; }
        @Override public int run(Context ctx) {
            if (ctx.payload == null) return 0;
            for (byte b : ctx.payload) {
                if (b == ':') return 1;
            }
            return 0;
        }
    }

    private static final class IsoDetectStage implements Stage {
        @Override public String name() { return "iso_detect"; }
        @Override public int run(Context ctx) {
            if (ctx.payload == null || ctx.payload.length < 4) return 0;
            for (int i = 0; i < 4; i++) {
                byte b = ctx.payload[i];
                if (b < '0' || b > '9') return 0;
            }
            return 1;
        }
    }

    private static final class RiskScoreStage implements Stage {
        @Override public String name() { return "risk_score"; }
        @Override public int run(Context ctx) {
            int score = ctx.flags + (ctx.payload == null ? 0 : ctx.payload.length);
            if (score > 10000) score = 10000;
            return score;
        }
    }

    private static final class AuditEmitStage implements Stage {
        @Override public String name() { return "audit_emit"; }
        @Override public int run(Context ctx) {
            ctx.audit.append("ingress", "stage_complete", String.valueOf(ctx.score));
            return ctx.audit.size();
        }
    }
}
