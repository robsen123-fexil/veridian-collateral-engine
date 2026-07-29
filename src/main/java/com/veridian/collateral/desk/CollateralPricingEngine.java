package com.veridian.collateral.desk;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class CollateralPricingEngine {
    public static final class BondQuote {
        public String cusip;
        public double faceValue;
        public double couponRate;
        public int yearsToMaturity;
        public double yieldToMaturity;
        public double cleanPrice;
        public double dirtyPrice;
        public double duration;
        public double convexity;
    }

    public BondQuote priceFixedRateBond(String cusip, double face, double couponPct, int years, double ytm) {
        BondQuote q = new BondQuote();
        q.cusip = cusip;
        q.faceValue = face;
        q.couponRate = couponPct;
        q.yearsToMaturity = years;
        q.yieldToMaturity = ytm;
        double c = face * couponPct / 100.0;
        double pvCoupons = 0;
        for (int t = 1; t <= years; t++) {
            pvCoupons += c / Math.pow(1 + ytm, t);
        }
        double pvPrincipal = face / Math.pow(1 + ytm, years);
        q.cleanPrice = pvCoupons + pvPrincipal;
        q.dirtyPrice = q.cleanPrice + accruedInterest(face, couponPct, 180);
        q.duration = modifiedDuration(face, couponPct, years, ytm);
        q.convexity = convexity(face, couponPct, years, ytm);
        return q;
    }

    public double accruedInterest(double face, double couponPct, int daysSinceLastCoupon) {
        double annual = face * couponPct / 100.0;
        return annual * (daysSinceLastCoupon / 360.0);
    }

    public double modifiedDuration(double face, double couponPct, int years, double ytm) {
        double c = face * couponPct / 100.0;
        double price = 0;
        double weighted = 0;
        for (int t = 1; t <= years; t++) {
            double df = Math.pow(1 + ytm, t);
            double cf = c;
            if (t == years) {
                cf += face;
            }
            price += cf / df;
            weighted += (t * cf / df);
        }
        if (price <= 0) {
            return 0;
        }
        return weighted / price / (1 + ytm);
    }

    public double convexity(double face, double couponPct, int years, double ytm) {
        double c = face * couponPct / 100.0;
        double price = 0;
        double conv = 0;
        for (int t = 1; t <= years; t++) {
            double df = Math.pow(1 + ytm, t);
            double cf = c + (t == years ? face : 0);
            price += cf / df;
            conv += (t * (t + 1) * cf) / (df * Math.pow(1 + ytm, 2));
        }
        return price <= 0 ? 0 : conv / price;
    }

    public List<BondQuote> ladder(String prefix, double face, double coupon, int maxYears, double ytm) {
        List<BondQuote> out = new ArrayList<>();
        for (int y = 1; y <= maxYears; y++) {
            out.add(priceFixedRateBond(prefix + y, face, coupon, y, ytm + (y * 0.001)));
        }
        out.sort(Comparator.comparingDouble(q -> q.cleanPrice));
        return out;
    }

    public double portfolioValue(List<BondQuote> quotes) {
        double sum = 0;
        for (BondQuote q : quotes) {
            sum += q.cleanPrice;
        }
        return sum;
    }

    public double haircutAdjustedValue(BondQuote quote, HaircutGrid grid, String assetClass, String rating) {
        double h = grid.effectiveHaircut(assetClass, rating, false);
        return quote.cleanPrice * (1.0 - h);
    }
}
