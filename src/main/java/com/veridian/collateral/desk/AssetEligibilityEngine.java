package com.veridian.collateral.desk;

import java.util.ArrayList;
import java.util.List;

public final class AssetEligibilityEngine {
    public static final class EligibilityCheck {
        public String cusip;
        public String assetClass;
        public String rating;
        public String jurisdiction;
        public boolean listed;
        public boolean sanctioned;
        public boolean eligible;
        public final List<String> reasons = new ArrayList<>();
    }

    private final EligibleAssetRegistry registry = new EligibleAssetRegistry();
    private final ComplianceRuleEngine compliance = new ComplianceRuleEngine();

    public AssetEligibilityEngine() {
        registry.register("US912828XW86", "GOVT", "AAA", "UST", 0.99);
        registry.register("CORP-IG-001", "CORP", "IG", "ACME", 0.85);
        registry.register("EQ-L-001", "EQUITY", "LARGE", "BIGCO", 0.75);
    }

    public EligibilityCheck evaluate(String cusip, String assetClass, String rating, String jurisdiction,
                                     double marketValue, boolean listed) {
        EligibilityCheck check = new EligibilityCheck();
        check.cusip = cusip;
        check.assetClass = assetClass;
        check.rating = rating;
        check.jurisdiction = jurisdiction;
        check.listed = listed;
        EligibleAssetRegistry.AssetRecord rec = registry.lookup(cusip);
        if (rec != null) {
            check.assetClass = rec.assetClass;
            check.rating = rec.rating;
            check.sanctioned = "SANCTIONED".equals(rec.issuer);
        }
        if (check.sanctioned) {
            check.reasons.add("issuer_sanctioned");
        }
        if (!listed && "EQUITY".equals(check.assetClass)) {
            check.reasons.add("unlisted_equity");
        }
        if ("CRYPTO".equals(check.assetClass)) {
            check.reasons.add("unsupported_asset_class");
        }
        ComplianceRuleEngine.ComplianceContext ctx = new ComplianceRuleEngine.ComplianceContext();
        ctx.assetClass = check.assetClass;
        ctx.issuer = rec != null ? rec.issuer : "";
        ctx.marketValue = marketValue;
        ctx.jurisdiction = jurisdiction;
        ctx.haircut = 0.05;
        if (!compliance.runAll(ctx)) {
            check.reasons.addAll(compliance.collectViolations(ctx));
        }
        check.eligible = check.reasons.isEmpty();
        return check;
    }

    public List<EligibilityCheck> evaluateBatch(List<String> cusips, List<Double> marketValues) {
        List<EligibilityCheck> out = new ArrayList<>();
        for (int i = 0; i < cusips.size(); i++) {
            double mv = i < marketValues.size() ? marketValues.get(i) : 0;
            out.add(evaluate(cusips.get(i), "CORP", "IG", "US", mv, true));
        }
        return out;
    }

    public int countEligible(List<EligibilityCheck> checks) {
        int n = 0;
        for (EligibilityCheck c : checks) {
            if (c.eligible) {
                n++;
            }
        }
        return n;
    }
}
