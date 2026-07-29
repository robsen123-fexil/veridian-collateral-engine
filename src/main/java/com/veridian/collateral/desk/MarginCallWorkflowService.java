package com.veridian.collateral.desk;

import java.util.ArrayList;
import java.util.List;

public final class MarginCallWorkflowService {
    private final MarginCallScheduler scheduler = new MarginCallScheduler();
    private final MarginCallEscalationPolicy escalation = new MarginCallEscalationPolicy();
    private final CollateralCoverageAnalyzer coverage = new CollateralCoverageAnalyzer();

    public static final class WorkflowState {
        public String callId;
        public String accountId;
        public double deficit;
        public boolean closed;
        public final List<String> audit = new ArrayList<>();
    }

    public WorkflowState openCall(String accountId, double deficit, int deadlineHours) {
        WorkflowState state = new WorkflowState();
        state.accountId = accountId;
        scheduler.scheduleCall(accountId, deficit, deadlineHours);
        state.callId = "MC-WF-" + scheduler.openDeficit();
        state.deficit = deficit;
        state.audit.add("opened");
        return state;
    }

    public MarginCallEscalationPolicy.EscalationAction escalate(WorkflowState state, int hoursOpen) {
        MarginCallEscalationPolicy.OpenCall call = new MarginCallEscalationPolicy.OpenCall();
        call.callId = state.callId;
        call.accountId = state.accountId;
        call.deficit = state.deficit;
        call.hoursOpen = hoursOpen;
        MarginCallEscalationPolicy.EscalationAction action = escalation.evaluate(call);
        state.audit.add("escalate:" + action.actionCode);
        return action;
    }

    public boolean closeIfCovered(WorkflowState state, double pledged, double required) {
        CollateralCoverageAnalyzer.CoverageInput input = new CollateralCoverageAnalyzer.CoverageInput();
        input.requiredCollateral = required;
        input.pledgedCollateral = pledged;
        CollateralCoverageAnalyzer.CoverageResult result = coverage.analyze(input);
        if (result.sufficient) {
            state.closed = true;
            state.audit.add("closed:covered");
            return true;
        }
        return false;
    }
}
