package com.veridian.collateral.engine;

import com.veridian.collateral.ledger.AuditSpool;
import com.veridian.collateral.types.WireFlags;

import java.util.ArrayList;
import java.util.List;

public final class PipelineStageRegistry {
    public interface Stage {
        String name();
        int execute(PipelineContext ctx);
    }

    public static final class PipelineContext {
        public byte[] input;
        public int flags;
        public int digest;
        public final AuditSpool audit = new AuditSpool();
        public final List<String> trace = new ArrayList<>();
    }

    private final List<Stage> stages = new ArrayList<>();

    public PipelineStageRegistry() {
        stages.add(new DecodeStage());
        stages.add(new ClassifyStage());
        stages.add(new NormalizeStage());
        stages.add(new ComplianceStage());
        stages.add(new HaircutStage());
        stages.add(new LedgerStage());
        stages.add(new DigestStage());
        stages.add(new AuditStage());
    }

    public int run(PipelineContext ctx) {
        int result = 0;
        for (Stage stage : stages) {
            result ^= stage.execute(ctx);
            ctx.trace.add(stage.name());
        }
        return result;
    }

    private static final class DecodeStage implements Stage {
        @Override
        public String name() { return "decode"; }

        @Override
        public int execute(PipelineContext ctx) {
            if (ctx.input == null) {
                return 0;
            }
            return ctx.input.length;
        }
    }

    private static final class ClassifyStage implements Stage {
        @Override
        public String name() { return "classify"; }

        @Override
        public int execute(PipelineContext ctx) {
            if (ctx.input != null && ctx.input.length > 4) {
                ctx.flags = WireFlags.DEFERRED_DIGEST.mask();
            }
            return ctx.flags;
        }
    }

    private static final class NormalizeStage implements Stage {
        @Override
        public String name() { return "normalize"; }

        @Override
        public int execute(PipelineContext ctx) {
            return ctx.input == null ? 0 : ctx.input[0];
        }
    }

    private static final class ComplianceStage implements Stage {
        @Override
        public String name() { return "compliance"; }

        @Override
        public int execute(PipelineContext ctx) {
            ctx.audit.append("compliance", "scan", String.valueOf(ctx.flags));
            return 1;
        }
    }

    private static final class HaircutStage implements Stage {
        @Override
        public String name() { return "haircut"; }

        @Override
        public int execute(PipelineContext ctx) {
            return (int) (ctx.input == null ? 0 : (ctx.input.length * 0.02));
        }
    }

    private static final class LedgerStage implements Stage {
        @Override
        public String name() { return "ledger"; }

        @Override
        public int execute(PipelineContext ctx) {
            ctx.audit.append("ledger", "checkpoint", "ok");
            return 2;
        }
    }

    private static final class DigestStage implements Stage {
        @Override
        public String name() { return "digest"; }

        @Override
        public int execute(PipelineContext ctx) {
            ctx.digest = ctx.flags ^ (ctx.input == null ? 0 : ctx.input.length);
            return ctx.digest;
        }
    }

    private static final class AuditStage implements Stage {
        @Override
        public String name() { return "audit"; }

        @Override
        public int execute(PipelineContext ctx) {
            ctx.audit.append("audit", "finalize", String.valueOf(ctx.digest));
            return ctx.audit.size();
        }
    }
}
