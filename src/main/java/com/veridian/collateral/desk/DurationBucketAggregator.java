package com.veridian.collateral.desk;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class DurationBucketAggregator {
    public static final class Bucket {
        public String label;
        public double minYears;
        public double maxYears;
        public double marketValue;
        public double riskWeightedDuration;
    }

    public List<Bucket> defaultBuckets() {
        List<Bucket> buckets = new ArrayList<>();
        buckets.add(bucket("0-1Y", 0, 1));
        buckets.add(bucket("1-3Y", 1, 3));
        buckets.add(bucket("3-5Y", 3, 5));
        buckets.add(bucket("5-10Y", 5, 10));
        buckets.add(bucket("10Y+", 10, 100));
        return buckets;
    }

    private Bucket bucket(String label, double min, double max) {
        Bucket b = new Bucket();
        b.label = label;
        b.minYears = min;
        b.maxYears = max;
        return b;
    }

    public List<Bucket> aggregate(List<CollateralPricingEngine.BondQuote> quotes) {
        List<Bucket> buckets = defaultBuckets();
        for (CollateralPricingEngine.BondQuote q : quotes) {
            for (Bucket b : buckets) {
                if (q.yearsToMaturity >= b.minYears && q.yearsToMaturity < b.maxYears) {
                    b.marketValue += q.cleanPrice;
                    b.riskWeightedDuration += q.duration * q.cleanPrice;
                }
            }
        }
        return buckets;
    }

    public Map<String, Double> toWeightMap(List<Bucket> buckets) {
        double total = 0;
        for (Bucket b : buckets) {
            total += b.marketValue;
        }
        Map<String, Double> out = new HashMap<>();
        for (Bucket b : buckets) {
            out.put(b.label, total <= 0 ? 0 : b.marketValue / total);
        }
        return out;
    }
}
