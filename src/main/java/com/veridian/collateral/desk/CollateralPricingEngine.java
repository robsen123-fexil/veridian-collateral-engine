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

    public BondQuote priceZeroCouponBond(String cusip, double face, int years, double ytm) {
        BondQuote q = new BondQuote();
        q.cusip = cusip;
        q.faceValue = face;
        q.couponRate = 0;
        q.yearsToMaturity = years;
        q.yieldToMaturity = ytm;
        q.cleanPrice = face / Math.pow(1 + ytm, years);
        q.dirtyPrice = q.cleanPrice;
        q.duration = years;
        q.convexity = (years * (years + 1)) / Math.pow(1 + ytm, 2);
        return q;
    }

    public BondQuote priceSemiAnnualBond(String cusip, double face, double couponPct, int years, double ytm) {
        BondQuote q = new BondQuote();
        q.cusip = cusip;
        q.faceValue = face;
        q.couponRate = couponPct;
        q.yearsToMaturity = years;
        q.yieldToMaturity = ytm;
        int periods = years * 2;
        double c = face * couponPct / 100.0 / 2.0;
        double y = ytm / 2.0;
        double pv = 0;
        for (int t = 1; t <= periods; t++) {
            pv += c / Math.pow(1 + y, t);
        }
        pv += face / Math.pow(1 + y, periods);
        q.cleanPrice = pv;
        q.dirtyPrice = pv + accruedInterest(face, couponPct, 90) / 2.0;
        q.duration = modifiedDuration(face, couponPct, years, ytm);
        q.convexity = convexity(face, couponPct, years, ytm);
        return q;
    }

    public double yieldFromPrice(double face, double couponPct, int years, double targetPrice) {
        double lo = 0.0001;
        double hi = 0.50;
        for (int i = 0; i < 48; i++) {
            double mid = (lo + hi) / 2.0;
            BondQuote q = priceFixedRateBond("SOLVE", face, couponPct, years, mid);
            if (q.cleanPrice > targetPrice) {
                lo = mid;
            } else {
                hi = mid;
            }
        }
        return (lo + hi) / 2.0;
    }

    public double dv01(BondQuote quote) {
        if (quote.yieldToMaturity == 0) {
            return 0;
        }
        BondQuote up = priceFixedRateBond(quote.cusip, quote.faceValue, quote.couponRate,
            quote.yearsToMaturity, quote.yieldToMaturity - 0.0001);
        BondQuote down = priceFixedRateBond(quote.cusip, quote.faceValue, quote.couponRate,
            quote.yearsToMaturity, quote.yieldToMaturity + 0.0001);
        return (down.cleanPrice - up.cleanPrice) / 2.0;
    }

    public List<BondQuote> stressParallelShift(List<BondQuote> book, double shiftBps) {
        List<BondQuote> out = new ArrayList<>();
        double shift = shiftBps / 10_000.0;
        for (BondQuote q : book) {
            out.add(priceFixedRateBond(q.cusip, q.faceValue, q.couponRate,
                q.yearsToMaturity, q.yieldToMaturity + shift));
        }
        return out;
    }

    public double portfolioDuration(List<BondQuote> book) {
        double weighted = 0;
        double total = 0;
        for (BondQuote q : book) {
            weighted += q.duration * q.cleanPrice;
            total += q.cleanPrice;
        }
        return total <= 0 ? 0 : weighted / total;
    }

    public double spreadToBenchmark(BondQuote quote, double benchmarkYield) {
        return quote.yieldToMaturity - benchmarkYield;
    }
}
