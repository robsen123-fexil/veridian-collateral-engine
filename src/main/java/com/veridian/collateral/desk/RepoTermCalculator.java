package com.veridian.collateral.desk;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public final class RepoTermCalculator {
    public static final class RepoTerm {
        public LocalDate startDate;
        public LocalDate endDate;
        public int dayCount;
        public double rateBps;
        public double notional;
        public double accrual;
        public String dayCountConvention;
    }

    public RepoTerm calculate(LocalDate start, LocalDate end, double notional, double rateBps, String convention) {
        RepoTerm term = new RepoTerm();
        term.startDate = start;
        term.endDate = end;
        term.notional = notional;
        term.rateBps = rateBps;
        term.dayCountConvention = convention;
        term.dayCount = (int) ChronoUnit.DAYS.between(start, end);
        if (term.dayCount < 0) {
            term.dayCount = 0;
        }
        double yearFraction = yearFraction(term.dayCount, convention);
        term.accrual = notional * (rateBps / 10_000.0) * yearFraction;
        return term;
    }

    private double yearFraction(int days, String convention) {
        if ("ACT/360".equals(convention)) {
            return days / 360.0;
        }
        if ("ACT/365".equals(convention)) {
            return days / 365.0;
        }
        return days / 360.0;
    }

    public double rollAccrual(RepoTerm term, LocalDate asOf) {
        if (asOf.isBefore(term.startDate)) {
            return 0;
        }
        if (asOf.isAfter(term.endDate)) {
            return term.accrual;
        }
        int elapsed = (int) ChronoUnit.DAYS.between(term.startDate, asOf);
        double full = yearFraction(term.dayCount, term.dayCountConvention);
        double partial = yearFraction(elapsed, term.dayCountConvention);
        if (full <= 0) {
            return 0;
        }
        return term.notional * (term.rateBps / 10_000.0) * partial;
    }
}
