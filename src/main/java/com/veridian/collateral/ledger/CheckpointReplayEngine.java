package com.veridian.collateral.ledger;

import java.util.ArrayList;
import java.util.List;

public final class CheckpointReplayEngine {
    public static final class ReplayStep {
        public long checkpointId;
        public String action;
        public boolean success;
    }

    public List<ReplayStep> replay(VaultCheckpointStore store, List<String> refs) {
        List<ReplayStep> steps = new ArrayList<>();
        for (String ref : refs) {
            ReplayStep step = new ReplayStep();
            step.checkpointId = store.count();
            step.action = "apply:" + ref;
            step.success = ref != null && !ref.isEmpty();
            steps.add(step);
            if (step.success) {
                store.create("replay", List.of(ref));
            }
        }
        return steps;
    }
}
