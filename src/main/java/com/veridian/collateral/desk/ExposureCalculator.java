package com.veridian.collateral.desk;

import java.util.ArrayList;
import java.util.List;

public final class ExposureCalculator {
    public static final class ExposureLine {
        public String id;
        public String assetClass;
        public double notional;
        public double delta;
        public double beta;
        public String currency;
    }

    public double grossExposure(List<ExposureLine> lines) {
        double sum = 0;
        for (ExposureLine line : lines) {
            sum += Math.abs(line.notional * line.delta);
        }
        return sum;
    }

    public double netExposure(List<ExposureLine> lines) {
        double sum = 0;
        for (ExposureLine line : lines) {
            sum += line.notional * line.delta;
        }
        return sum;
    }

    public double betaAdjustedExposure(List<ExposureLine> lines, double marketMove) {
        double sum = 0;
        for (ExposureLine line : lines) {
            sum += line.notional * line.delta * (1.0 + line.beta * marketMove);
        }
        return sum;
    }

    public double currencySplitExposure(List<ExposureLine> lines, String currency) {
        double sum = 0;
        for (ExposureLine line : lines) {
            if (currency.equals(line.currency)) {
                sum += Math.abs(line.notional);
            }
        }
        return sum;
    }

    public List<ExposureLine> filterByAssetClass(List<ExposureLine> lines, String assetClass) {
        List<ExposureLine> out = new ArrayList<>();
        for (ExposureLine line : lines) {
            if (assetClass.equals(line.assetClass)) {
                out.add(line);
            }
        }
        return out;
    }

    public double concentrationByIssuer(List<ExposureLine> lines, String issuerPrefix) {
        double total = grossExposure(lines);
        if (total <= 0) {
            return 0;
        }
        double issuer = 0;
        for (ExposureLine line : lines) {
            if (line.id != null && line.id.startsWith(issuerPrefix)) {
                issuer += Math.abs(line.notional);
            }
        }
        return issuer / total;
    }

    public double stressEquityShock(List<ExposureLine> lines, double shock) {
        double pnl = 0;
        for (ExposureLine line : lines) {
            if ("EQUITY".equals(line.assetClass)) {
                pnl += line.notional * shock * line.delta;
            }
        }
        return pnl;
    }

    public double stressRatesUp(List<ExposureLine> lines, double bp) {
        double shift = bp / 10_000.0;
        double pnl = 0;
        for (ExposureLine line : lines) {
            if ("GOVT".equals(line.assetClass) || "CORP".equals(line.assetClass)) {
                pnl -= line.notional * shift * 5.0;
            }
        }
        return pnl;
    }

    public double stressFxMove(List<ExposureLine> lines, String ccy, double pct) {
        double pnl = 0;
        for (ExposureLine line : lines) {
            if (ccy.equals(line.currency)) {
                pnl += line.notional * pct;
            }
        }
        return pnl;
    }

    public double combinedStress(List<ExposureLine> lines, double eqShock, double rateBp, String fxCcy, double fxPct) {
        return stressEquityShock(lines, eqShock) + stressRatesUp(lines, rateBp) + stressFxMove(lines, fxCcy, fxPct);
    }

    public double hedgeRatio(List<ExposureLine> assets, List<ExposureLine> hedges) {
        double asset = grossExposure(assets);
        double hedge = grossExposure(hedges);
        if (asset <= 0) {
            return 0;
        }
        return Math.min(1.0, hedge / asset);
    }

    public double residualAfterHedge(List<ExposureLine> assets, List<ExposureLine> hedges) {
        return grossExposure(assets) - grossExposure(hedges);
    }
}
