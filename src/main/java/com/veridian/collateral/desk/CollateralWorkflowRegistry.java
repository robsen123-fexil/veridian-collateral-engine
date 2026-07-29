package com.veridian.collateral.desk;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public final class CollateralWorkflowRegistry {
    public enum WorkflowType {
        PLEDGE, RELEASE, SUBSTITUTION, MARGIN_CALL, SETTLEMENT_FAIL, RECALL
    }

    public interface WorkflowHandler {
        WorkflowType type();
        boolean canStart(CollateralWorkflowContext ctx);
        List<String> steps(CollateralWorkflowContext ctx);
        boolean validateTransition(CollateralWorkflowContext ctx, String fromStep, String toStep);
    }

    public static final class CollateralWorkflowContext {
        public String accountId = "";
        public WorkflowType type;
        public double amount;
        public String reference = "";
        public String currentStep = "INIT";
        public final List<String> audit = new ArrayList<>();
    }

    private final Map<WorkflowType, WorkflowHandler> handlers = new EnumMap<>(WorkflowType.class);

    public CollateralWorkflowRegistry() {
        handlers.put(WorkflowType.PLEDGE, new PledgeWorkflow());
        handlers.put(WorkflowType.RELEASE, new ReleaseWorkflow());
        handlers.put(WorkflowType.SUBSTITUTION, new SubstitutionWorkflow());
        handlers.put(WorkflowType.MARGIN_CALL, new MarginCallWorkflow());
        handlers.put(WorkflowType.SETTLEMENT_FAIL, new SettlementFailWorkflow());
        handlers.put(WorkflowType.RECALL, new RecallWorkflow());
    }

    public List<String> start(CollateralWorkflowContext ctx) {
        WorkflowHandler handler = handlers.get(ctx.type);
        if (handler == null || !handler.canStart(ctx)) {
            ctx.audit.add("reject:" + ctx.type);
            return List.of();
        }
        List<String> steps = handler.steps(ctx);
        ctx.audit.add("start:" + ctx.type);
        if (!steps.isEmpty()) {
            ctx.currentStep = steps.get(0);
        }
        return steps;
    }

    public boolean advance(CollateralWorkflowContext ctx) {
        WorkflowHandler handler = handlers.get(ctx.type);
        if (handler == null) {
            return false;
        }
        List<String> steps = handler.steps(ctx);
        int idx = steps.indexOf(ctx.currentStep);
        if (idx < 0 || idx + 1 >= steps.size()) {
            ctx.audit.add("complete");
            return false;
        }
        String next = steps.get(idx + 1);
        if (!handler.validateTransition(ctx, ctx.currentStep, next)) {
            ctx.audit.add("invalid_transition:" + ctx.currentStep + "->" + next);
            return false;
        }
        ctx.currentStep = next;
        ctx.audit.add("->" + next);
        return true;
    }

    private static final class PledgeWorkflow implements WorkflowHandler {
        @Override public WorkflowType type() { return WorkflowType.PLEDGE; }
        @Override public boolean canStart(CollateralWorkflowContext ctx) {
            return ctx.amount > 0 && ctx.accountId != null && !ctx.accountId.isEmpty();
        }
        @Override public List<String> steps(CollateralWorkflowContext ctx) {
            return List.of("INIT", "VALIDATE_ASSETS", "APPLY_HAIRCUT", "RECORD_PLEDGE", "CONFIRM");
        }
        @Override public boolean validateTransition(CollateralWorkflowContext ctx, String from, String to) {
            return stepOrder(from) <= stepOrder(to);
        }
        private int stepOrder(String step) {
            return switch (step) {
                case "INIT" -> 0;
                case "VALIDATE_ASSETS" -> 1;
                case "APPLY_HAIRCUT" -> 2;
                case "RECORD_PLEDGE" -> 3;
                case "CONFIRM" -> 4;
                default -> -1;
            };
        }
    }

    private static final class ReleaseWorkflow implements WorkflowHandler {
        @Override public WorkflowType type() { return WorkflowType.RELEASE; }
        @Override public boolean canStart(CollateralWorkflowContext ctx) {
            return ctx.reference != null && !ctx.reference.isEmpty();
        }
        @Override public List<String> steps(CollateralWorkflowContext ctx) {
            return List.of("INIT", "MARGIN_CHECK", "APPROVE_RELEASE", "SETTLE", "CONFIRM");
        }
        @Override public boolean validateTransition(CollateralWorkflowContext ctx, String from, String to) {
            if ("APPROVE_RELEASE".equals(to) && ctx.amount <= 0) {
                return false;
            }
            return true;
        }
    }

    private static final class SubstitutionWorkflow implements WorkflowHandler {
        @Override public WorkflowType type() { return WorkflowType.SUBSTITUTION; }
        @Override public boolean canStart(CollateralWorkflowContext ctx) {
            return ctx.amount > 0;
        }
        @Override public List<String> steps(CollateralWorkflowContext ctx) {
            return List.of("INIT", "IDENTIFY_OUT", "IDENTIFY_IN", "NET_HAIRCUT", "CONFIRM");
        }
        @Override public boolean validateTransition(CollateralWorkflowContext ctx, String from, String to) {
            return !from.equals(to);
        }
    }

    private static final class MarginCallWorkflow implements WorkflowHandler {
        @Override public WorkflowType type() { return WorkflowType.MARGIN_CALL; }
        @Override public boolean canStart(CollateralWorkflowContext ctx) {
            return ctx.amount > 0;
        }
        @Override public List<String> steps(CollateralWorkflowContext ctx) {
            return List.of("INIT", "CALCULATE_DEFICIT", "ISSUE_CALL", "MONITOR", "CLOSE");
        }
        @Override public boolean validateTransition(CollateralWorkflowContext ctx, String from, String to) {
            return true;
        }
    }

    private static final class SettlementFailWorkflow implements WorkflowHandler {
        @Override public WorkflowType type() { return WorkflowType.SETTLEMENT_FAIL; }
        @Override public boolean canStart(CollateralWorkflowContext ctx) {
            return ctx.reference != null && !ctx.reference.isEmpty();
        }
        @Override public List<String> steps(CollateralWorkflowContext ctx) {
            return List.of("INIT", "DIAGNOSE", "NOTIFY", "REMEDiate", "CLOSE");
        }
        @Override public boolean validateTransition(CollateralWorkflowContext ctx, String from, String to) {
            return true;
        }
    }

    private static final class RecallWorkflow implements WorkflowHandler {
        @Override public WorkflowType type() { return WorkflowType.RECALL; }
        @Override public boolean canStart(CollateralWorkflowContext ctx) {
            return ctx.amount > 0 && ctx.accountId != null;
        }
        @Override public List<String> steps(CollateralWorkflowContext ctx) {
            return List.of("INIT", "LOCATE", "RECALL", "CONFIRM");
        }
        @Override public boolean validateTransition(CollateralWorkflowContext ctx, String from, String to) {
            return true;
        }
    }
}
