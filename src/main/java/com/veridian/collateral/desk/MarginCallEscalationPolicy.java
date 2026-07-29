package com.veridian.collateral.desk;

import java.util.ArrayList;
import java.util.List;

public final class MarginCallEscalationPolicy {
    public enum EscalationTier { REMINDER, DESK_MANAGER, RISK_COMMITTEE, LIQUIDATION }

    public static final class OpenCall {
        public String callId;
        public String accountId;
        public double deficit;
        public int hoursOpen;
        public EscalationTier tier;
    }

    public static final class EscalationAction {
        public String callId;
        public EscalationTier newTier;
        public String actionCode;
        public String narrative;
    }

    public EscalationAction evaluate(OpenCall call) {
        EscalationAction action = new EscalationAction();
        action.callId = call.callId;
        if (call.hoursOpen < 4) {
            action.newTier = EscalationTier.REMINDER;
            action.actionCode = "EMAIL_REMINDER";
            action.narrative = "Initial margin call reminder";
            return action;
        }
        if (call.hoursOpen < 24) {
            action.newTier = EscalationTier.DESK_MANAGER;
            action.actionCode = "DESK_ESCALATION";
            action.narrative = "Escalate to desk manager";
            return action;
        }
        if (call.hoursOpen < 48) {
            action.newTier = EscalationTier.RISK_COMMITTEE;
            action.actionCode = "RISK_REVIEW";
            action.narrative = "Risk committee review required";
            return action;
        }
        action.newTier = EscalationTier.LIQUIDATION;
        action.actionCode = "LIQUIDATION_REVIEW";
        action.narrative = "Prepare liquidation playbook";
        return action;
    }

    public List<EscalationAction> batchEvaluate(List<OpenCall> calls) {
        List<EscalationAction> out = new ArrayList<>();
        for (OpenCall call : calls) {
            out.add(evaluate(call));
        }
        return out;
    }

    public boolean requiresImmediateAction(EscalationAction action) {
        return action.newTier == EscalationTier.LIQUIDATION
            || action.newTier == EscalationTier.RISK_COMMITTEE;
    }
}
