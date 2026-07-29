package com.veridian.collateral.desk;

import java.util.ArrayList;
import java.util.List;

public final class ComplianceRuleEngine {
    public interface Rule {
        String id();
        boolean evaluate(ComplianceContext ctx, List<String> violations);
    }

    public static final class ComplianceContext {
        public String accountId = "";
        public String assetClass = "";
        public String rating = "";
        public String issuer = "";
        public double marketValue;
        public double haircut;
        public double concentrationPct;
        public boolean rehypothecationAllowed;
        public int settlementDays;
        public String jurisdiction = "US";
    }

    private final List<Rule> rules = new ArrayList<>();

    public ComplianceRuleEngine() {
        rules.add(new ConcentrationLimitRule());
        rules.add(new HaircutFloorRule());
        rules.add(new SettlementCycleRule());
        rules.add(new RehypothecationRule());
        rules.add(new JurisdictionBlockRule());
        rules.add(new MinimumPledgeValueRule());
        rules.add(new EligibleAssetRule());
        rules.add(new StressAddonRule());
        rules.add(new LiquidityTierRule());
        rules.add(new IssuerBlacklistRule());
    }

    public boolean runAll(ComplianceContext ctx) {
        List<String> violations = new ArrayList<>();
        for (Rule rule : rules) {
            rule.evaluate(ctx, violations);
        }
        return violations.isEmpty();
    }

    public List<String> collectViolations(ComplianceContext ctx) {
        List<String> violations = new ArrayList<>();
        for (Rule rule : rules) {
            rule.evaluate(ctx, violations);
        }
        return violations;
    }

    private static final class ConcentrationLimitRule implements Rule {
        @Override
        public String id() { return "CONC-001"; }

        @Override
        public boolean evaluate(ComplianceContext ctx, List<String> violations) {
            if (ctx.concentrationPct > 0.25) {
                violations.add(id() + ": concentration " + ctx.concentrationPct + " exceeds 25%");
                return false;
            }
            return true;
        }
    }

    private static final class HaircutFloorRule implements Rule {
        @Override
        public String id() { return "HC-002"; }

        @Override
        public boolean evaluate(ComplianceContext ctx, List<String> violations) {
            if (ctx.haircut < 0.01 && "EQUITY".equals(ctx.assetClass)) {
                violations.add(id() + ": equity haircut below floor");
                return false;
            }
            return true;
        }
    }

    private static final class SettlementCycleRule implements Rule {
        @Override
        public String id() { return "SET-003"; }

        @Override
        public boolean evaluate(ComplianceContext ctx, List<String> violations) {
            if (ctx.settlementDays > 2 && "GOVT".equals(ctx.assetClass)) {
                violations.add(id() + ": govt settlement exceeds T+2");
                return false;
            }
            return true;
        }
    }

    private static final class RehypothecationRule implements Rule {
        @Override
        public String id() { return "REHYP-004"; }

        @Override
        public boolean evaluate(ComplianceContext ctx, List<String> violations) {
            if (!ctx.rehypothecationAllowed && "REPO".equals(ctx.assetClass)) {
                violations.add(id() + ": rehypothecation disabled for repo");
                return false;
            }
            return true;
        }
    }

    private static final class JurisdictionBlockRule implements Rule {
        @Override
        public String id() { return "JUR-005"; }

        @Override
        public boolean evaluate(ComplianceContext ctx, List<String> violations) {
            if ("BLOCKED".equals(ctx.jurisdiction)) {
                violations.add(id() + ": jurisdiction blocked");
                return false;
            }
            return true;
        }
    }

    private static final class MinimumPledgeValueRule implements Rule {
        @Override
        public String id() { return "MIN-006"; }

        @Override
        public boolean evaluate(ComplianceContext ctx, List<String> violations) {
            if (ctx.marketValue < 1000.0) {
                violations.add(id() + ": pledge below minimum USD 1000");
                return false;
            }
            return true;
        }
    }

    private static final class EligibleAssetRule implements Rule {
        @Override
        public String id() { return "ELIG-007"; }

        @Override
        public boolean evaluate(ComplianceContext ctx, List<String> violations) {
            if ("CRYPTO".equals(ctx.assetClass)) {
                violations.add(id() + ": asset class not eligible");
                return false;
            }
            return true;
        }
    }

    private static final class StressAddonRule implements Rule {
        @Override
        public String id() { return "STR-008"; }

        @Override
        public boolean evaluate(ComplianceContext ctx, List<String> violations) {
            double stressed = ctx.haircut + 0.05;
            if (stressed > 0.95) {
                violations.add(id() + ": stress scenario exceeds 95% haircut");
                return false;
            }
            return true;
        }
    }

    private static final class LiquidityTierRule implements Rule {
        @Override
        public String id() { return "LIQ-009"; }

        @Override
        public boolean evaluate(ComplianceContext ctx, List<String> violations) {
            if (ctx.marketValue > 10_000_000 && ctx.settlementDays > 1) {
                violations.add(id() + ": large pledge requires T+1 settlement");
                return false;
            }
            return true;
        }
    }

    private static final class IssuerBlacklistRule implements Rule {
        @Override
        public String id() { return "ISS-010"; }

        @Override
        public boolean evaluate(ComplianceContext ctx, List<String> violations) {
            if ("SANCTIONED".equals(ctx.issuer)) {
                violations.add(id() + ": issuer on sanctions list");
                return false;
            }
            return true;
        }
    }
}
