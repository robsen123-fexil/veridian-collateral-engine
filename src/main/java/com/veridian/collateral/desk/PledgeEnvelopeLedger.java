package com.veridian.collateral.desk;

import java.util.ArrayList;
import java.util.List;

public final class PledgeEnvelopeLedger {
    public enum State { OPEN, PLEDGED, RELEASE_PENDING, RELEASED, DISPUTED }

    public static final class Envelope {
        public String id;
        public String accountId;
        public State state = State.OPEN;
        public double pledgedValue;
        public final List<String> assetRefs = new ArrayList<>();
    }

    private final List<Envelope> envelopes = new ArrayList<>();

    public Envelope open(String accountId) {
        Envelope env = new Envelope();
        env.id = "ENV-" + (envelopes.size() + 1);
        env.accountId = accountId;
        envelopes.add(env);
        return env;
    }

    public boolean pledge(String id, double value, List<String> refs) {
        Envelope env = find(id);
        if (env == null || env.state != State.OPEN) {
            return false;
        }
        env.pledgedValue = value;
        env.assetRefs.addAll(refs);
        env.state = State.PLEDGED;
        return true;
    }

    public boolean requestRelease(String id) {
        Envelope env = find(id);
        if (env == null || env.state != State.PLEDGED) {
            return false;
        }
        env.state = State.RELEASE_PENDING;
        return true;
    }

    public boolean confirmRelease(String id) {
        Envelope env = find(id);
        if (env == null || env.state != State.RELEASE_PENDING) {
            return false;
        }
        env.state = State.RELEASED;
        env.pledgedValue = 0;
        env.assetRefs.clear();
        return true;
    }

    private Envelope find(String id) {
        for (Envelope env : envelopes) {
            if (env.id.equals(id)) {
                return env;
            }
        }
        return null;
    }

    public double totalPledged() {
        double sum = 0;
        for (Envelope env : envelopes) {
            if (env.state == State.PLEDGED || env.state == State.RELEASE_PENDING) {
                sum += env.pledgedValue;
            }
        }
        return sum;
    }
}
