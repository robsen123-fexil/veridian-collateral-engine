package com.veridian.collateral.desk;

import java.util.ArrayList;
import java.util.List;

public final class VaultConcentrationMonitor {
    public static final class Holding {
        public String vaultId;
        public String issuer;
        public double marketValue;
    }

    public static final class ConcentrationAlert {
        public String vaultId;
        public String issuer;
        public double concentrationPct;
        public double limitPct;
        public boolean breached;
    }

    public List<ConcentrationAlert> evaluate(List<Holding> holdings, double limitPct) {
        double totalByVault = 0;
        java.util.Map<String, Double> vaultTotals = new java.util.HashMap<>();
        java.util.Map<String, Double> vaultIssuer = new java.util.HashMap<>();
        for (Holding h : holdings) {
            vaultTotals.merge(h.vaultId, h.marketValue, Double::sum);
            vaultIssuer.merge(h.vaultId + "|" + h.issuer, h.marketValue, Double::sum);
        }
        List<ConcentrationAlert> alerts = new ArrayList<>();
        for (java.util.Map.Entry<String, Double> e : vaultIssuer.entrySet()) {
            String[] parts = e.getKey().split("\\|", 2);
            String vault = parts[0];
            String issuer = parts.length > 1 ? parts[1] : "";
            double vaultTotal = vaultTotals.getOrDefault(vault, 0.0);
            ConcentrationAlert alert = new ConcentrationAlert();
            alert.vaultId = vault;
            alert.issuer = issuer;
            alert.limitPct = limitPct;
            alert.concentrationPct = vaultTotal <= 0 ? 0 : e.getValue() / vaultTotal;
            alert.breached = alert.concentrationPct > limitPct;
            alerts.add(alert);
        }
        return alerts;
    }

    public int breachCount(List<ConcentrationAlert> alerts) {
        int n = 0;
        for (ConcentrationAlert a : alerts) {
            if (a.breached) {
                n++;
            }
        }
        return n;
    }
}
