package com.veridian.collateral.desk;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class CollateralReconciler {
    public static final class Leg {
        public String id;
        public String party;
        public double quantity;
        public double value;
        public String cusip;
    }

    public static final class MatchResult {
        public final List<String> matchedPairs = new ArrayList<>();
        public final List<String> unmatchedLeft = new ArrayList<>();
        public final List<String> unmatchedRight = new ArrayList<>();
        public double residual;
    }

    public MatchResult reconcileTriParty(List<Leg> custodian, List<Leg> client, double tolerance) {
        MatchResult result = new MatchResult();
        Map<String, Leg> clientByCusip = indexByCusip(client);
        boolean[] used = new boolean[client.size()];
        for (Leg left : custodian) {
            Leg right = clientByCusip.get(left.cusip);
            if (right == null) {
                result.unmatchedLeft.add(left.id);
                continue;
            }
            int idx = client.indexOf(right);
            if (idx >= 0 && !used[idx] && Math.abs(left.value - right.value) <= tolerance) {
                used[idx] = true;
                result.matchedPairs.add(left.id + "<->" + right.id);
            } else {
                result.unmatchedLeft.add(left.id);
            }
        }
        for (int i = 0; i < client.size(); i++) {
            if (!used[i]) {
                result.unmatchedRight.add(client.get(i).id);
            }
        }
        result.residual = computeResidual(custodian, client, result);
        return result;
    }

    private Map<String, Leg> indexByCusip(List<Leg> legs) {
        Map<String, Leg> map = new HashMap<>();
        for (Leg leg : legs) {
            if (leg.cusip != null) {
                map.put(leg.cusip, leg);
            }
        }
        return map;
    }

    private double computeResidual(List<Leg> left, List<Leg> right, MatchResult result) {
        double lv = left.stream().mapToDouble(l -> l.value).sum();
        double rv = right.stream().mapToDouble(l -> l.value).sum();
        return lv - rv;
    }
}
