package com.veridian.collateral.desk;

import java.util.ArrayList;
import java.util.List;

public final class CollateralTransferMatcher {
    public static final class TransferLeg {
        public String transferId;
        public String fromAccount;
        public String toAccount;
        public String cusip;
        public long quantity;
        public double value;
    }

    public static final class MatchResult {
        public String inboundId;
        public String outboundId;
        public double valueDelta;
        public boolean matched;
    }

    public List<MatchResult> matchInternalTransfers(List<TransferLeg> legs, double tolerance) {
        List<MatchResult> results = new ArrayList<>();
        boolean[] used = new boolean[legs.size()];
        for (int i = 0; i < legs.size(); i++) {
            if (used[i]) {
                continue;
            }
            TransferLeg a = legs.get(i);
            for (int j = i + 1; j < legs.size(); j++) {
                if (used[j]) {
                    continue;
                }
                TransferLeg b = legs.get(j);
                if (!a.cusip.equals(b.cusip)) {
                    continue;
                }
                if (a.quantity != b.quantity) {
                    continue;
                }
                if (a.fromAccount.equals(b.toAccount) && a.toAccount.equals(b.fromAccount)) {
                    MatchResult match = new MatchResult();
                    match.inboundId = a.transferId;
                    match.outboundId = b.transferId;
                    match.valueDelta = Math.abs(a.value - b.value);
                    match.matched = match.valueDelta <= tolerance;
                    results.add(match);
                    used[i] = used[j] = true;
                    break;
                }
            }
        }
        return results;
    }

    public double unmatchedValue(List<TransferLeg> legs, List<MatchResult> matches) {
        double gross = 0;
        for (TransferLeg leg : legs) {
            gross += leg.value;
        }
        double matched = 0;
        for (MatchResult m : matches) {
            if (m.matched) {
                matched += m.valueDelta;
            }
        }
        return gross - matched;
    }
}
