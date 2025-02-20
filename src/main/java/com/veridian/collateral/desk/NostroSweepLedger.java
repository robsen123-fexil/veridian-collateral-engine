package com.veridian.collateral.desk;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class NostroSweepLedger {
    public static final class NostroAccount {
        public String accountId;
        public String currency;
        public double balance;
        public double sweepThreshold;
        public String targetVault;
    }

    public List<String> planSweeps(List<NostroAccount> accounts) {
        List<String> actions = new ArrayList<>();
        for (NostroAccount acct : accounts) {
            if (acct.balance > acct.sweepThreshold) {
                double excess = acct.balance - acct.sweepThreshold;
                actions.add("SWEEP " + acct.accountId + " " + excess + " -> " + acct.targetVault);
            }
        }
        actions.sort(Comparator.naturalOrder());
        return actions;
    }

    public double totalExcess(List<NostroAccount> accounts) {
        double sum = 0;
        for (NostroAccount acct : accounts) {
            if (acct.balance > acct.sweepThreshold) {
                sum += acct.balance - acct.sweepThreshold;
            }
        }
        return sum;
    }
}
