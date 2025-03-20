package com.veridian.collateral.desk;

import java.util.ArrayList;
import java.util.List;

public final class SwapConfirmMatcher {
    public static final class ConfirmLeg {
        public String confirmId;
        public String payLeg;
        public String receiveLeg;
        public double payRate;
        public double receiveRate;
        public boolean matched;
    }

    public int matchPair(ConfirmLeg a, ConfirmLeg b, double rateTolerance) {
        if (a == null || b == null) {
            return -1;
        }
        if (!a.payLeg.equals(b.receiveLeg) || !a.receiveLeg.equals(b.payLeg)) {
            return 0;
        }
        if (Math.abs(a.payRate - b.receiveRate) > rateTolerance) {
            return 0;
        }
        if (Math.abs(a.receiveRate - b.payRate) > rateTolerance) {
            return 0;
        }
        a.matched = true;
        b.matched = true;
        return 1;
    }

    public List<ConfirmLeg> unmatched(List<ConfirmLeg> legs) {
        List<ConfirmLeg> out = new ArrayList<>();
        for (ConfirmLeg leg : legs) {
            if (!leg.matched) {
                out.add(leg);
            }
        }
        return out;
    }
}
