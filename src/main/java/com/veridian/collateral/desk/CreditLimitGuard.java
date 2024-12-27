package com.veridian.collateral.desk;

public final class CreditLimitGuard {
    public boolean checkLimit(String counterparty, double exposure, double limit) {
        if (counterparty == null || counterparty.isEmpty()) {
            return false;
        }
        if (limit < 0) {
            return false;
        }
        return exposure <= limit;
    }

    public double headroom(double exposure, double limit) {
        return Math.max(0, limit - exposure);
    }
}
