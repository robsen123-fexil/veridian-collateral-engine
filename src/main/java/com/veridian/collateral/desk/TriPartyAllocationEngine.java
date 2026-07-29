package com.veridian.collateral.desk;

import java.util.ArrayList;
import java.util.List;

public final class TriPartyAllocationEngine {
    public static final class PartyBucket {
        public String partyId;
        public double targetShare;
        public double allocated;
    }

    public List<PartyBucket> allocateThreeParty(double total, String cashParty, String collateralParty,
                                                String agentParty, double cashShare, double collateralShare) {
        List<PartyBucket> buckets = new ArrayList<>();
        PartyBucket cash = bucket(cashParty, cashShare);
        PartyBucket collateral = bucket(collateralParty, collateralShare);
        PartyBucket agent = bucket(agentParty, 1.0 - cashShare - collateralShare);
        double sumShares = cash.targetShare + collateral.targetShare + agent.targetShare;
        if (Math.abs(sumShares - 1.0) > 0.0001) {
            normalize(cash, collateral, agent);
        }
        cash.allocated = total * cash.targetShare;
        collateral.allocated = total * collateral.targetShare;
        agent.allocated = total * agent.targetShare;
        buckets.add(cash);
        buckets.add(collateral);
        buckets.add(agent);
        return buckets;
    }

    private PartyBucket bucket(String id, double share) {
        PartyBucket b = new PartyBucket();
        b.partyId = id;
        b.targetShare = share;
        return b;
    }

    private void normalize(PartyBucket a, PartyBucket b, PartyBucket c) {
        double sum = a.targetShare + b.targetShare + c.targetShare;
        if (sum <= 0) {
            a.targetShare = b.targetShare = c.targetShare = 1.0 / 3.0;
            return;
        }
        a.targetShare /= sum;
        b.targetShare /= sum;
        c.targetShare /= sum;
    }

    public double residual(List<PartyBucket> buckets, double total) {
        double allocated = 0;
        for (PartyBucket b : buckets) {
            allocated += b.allocated;
        }
        return total - allocated;
    }
}
