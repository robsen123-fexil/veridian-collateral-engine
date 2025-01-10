package com.veridian.collateral.desk;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class EligibleAssetRegistry {
    public static final class AssetRecord {
        public String cusip;
        public String assetClass;
        public String rating;
        public String issuer;
        public boolean eligible;
        public double liquidityScore;
    }

    private final Map<String, AssetRecord> byCusip = new HashMap<>();

    public void register(String cusip, String assetClass, String rating, String issuer, double liquidityScore) {
        AssetRecord rec = new AssetRecord();
        rec.cusip = cusip;
        rec.assetClass = assetClass;
        rec.rating = rating;
        rec.issuer = issuer;
        rec.liquidityScore = liquidityScore;
        rec.eligible = !"SANCTIONED".equals(issuer) && !"CRYPTO".equals(assetClass);
        byCusip.put(cusip, rec);
    }

    public AssetRecord lookup(String cusip) {
        return byCusip.get(cusip);
    }

    public List<AssetRecord> eligibleByClass(String assetClass) {
        List<AssetRecord> out = new ArrayList<>();
        for (AssetRecord rec : byCusip.values()) {
            if (rec.eligible && rec.assetClass.equals(assetClass)) {
                out.add(rec);
            }
        }
        return out;
    }

    public int countEligible() {
        int n = 0;
        for (AssetRecord rec : byCusip.values()) {
            if (rec.eligible) {
                n++;
            }
        }
        return n;
    }
}
