package com.veridian.collateral.desk;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public final class TriPartyWorkflowEngine {
    public enum Step {
        INITIATE,
        ALLOCATE,
        MARGIN_CHECK,
        PLEDGE,
        CONFIRM,
        SETTLE,
        RELEASE
    }

    public static final class WorkflowInstance {
        public String id;
        public Step current;
        public String accountId;
        public double pledgedValue;
        public final List<String> audit = new ArrayList<>();
        public boolean completed;
    }

    private final Map<Step, Step> transitions = new EnumMap<>(Step.class);
    private final List<WorkflowInstance> instances = new ArrayList<>();
    private long sequence;

    public TriPartyWorkflowEngine() {
        transitions.put(Step.INITIATE, Step.ALLOCATE);
        transitions.put(Step.ALLOCATE, Step.MARGIN_CHECK);
        transitions.put(Step.MARGIN_CHECK, Step.PLEDGE);
        transitions.put(Step.PLEDGE, Step.CONFIRM);
        transitions.put(Step.CONFIRM, Step.SETTLE);
        transitions.put(Step.SETTLE, Step.RELEASE);
    }

    public WorkflowInstance start(String accountId) {
        WorkflowInstance wf = new WorkflowInstance();
        wf.id = "TP-" + (++sequence);
        wf.accountId = accountId;
        wf.current = Step.INITIATE;
        wf.audit.add("start");
        instances.add(wf);
        return wf;
    }

    public boolean advance(WorkflowInstance wf) {
        if (wf == null || wf.completed) {
            return false;
        }
        Step next = transitions.get(wf.current);
        if (next == null) {
            wf.completed = true;
            wf.audit.add("done");
            return true;
        }
        wf.current = next;
        wf.audit.add("->" + next.name());
        return true;
    }

    public boolean runToCompletion(WorkflowInstance wf, double pledgeValue) {
        wf.pledgedValue = pledgeValue;
        while (!wf.completed) {
            if (!advance(wf)) {
                return false;
            }
        }
        return true;
    }

    public List<WorkflowInstance> openInstances() {
        List<WorkflowInstance> out = new ArrayList<>();
        for (WorkflowInstance wf : instances) {
            if (!wf.completed) {
                out.add(wf);
            }
        }
        return out;
    }
}
