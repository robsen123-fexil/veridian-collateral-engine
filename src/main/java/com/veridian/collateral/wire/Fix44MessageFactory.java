package com.veridian.collateral.wire;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/** Factory for collateral-domain FIX 4.4 messages with per-type validation hooks. */
public final class Fix44MessageFactory {
    private static final char SOH = 1;

    public byte[] buildMarginRequirementReport(String account, double initial, double variation, double total) {
        List<String> fields = base("MR", account);
        fields.add(tag(909, format(initial)));
        fields.add(tag(910, format(variation)));
        fields.add(tag(912, format(total)));
        validateMarginReport(initial, variation, total);
        return encode(fields);
    }

    public byte[] buildCollateralAssignment(String account, String symbol, long qty, double haircut) {
        List<String> fields = base("CV", account);
        fields.add(tag(55, symbol));
        fields.add(tag(53, String.valueOf(qty)));
        fields.add(tag(909, format(haircut)));
        validateAssignment(qty, haircut);
        return encode(fields);
    }

    public byte[] buildPledgeStatusUpdate(String envelopeId, String status, String vault) {
        List<String> fields = base("PS", "CUSTODY");
        fields.add(tag(920, envelopeId));
        fields.add(tag(911, status));
        fields.add(tag(914, vault));
        requireNonEmpty(envelopeId);
        return encode(fields);
    }

    public byte[] buildMarginCallRequest(String account, double deficit, String deadline) {
        List<String> fields = base("MC", account);
        fields.add(tag(913, format(deficit)));
        fields.add(tag(929, deadline));
        if (deficit <= 0) {
            throw new IllegalArgumentException("deficit must be positive");
        }
        return encode(fields);
    }

    public byte[] buildAllocationReport(String account, String symbol, long qty, String confirmId) {
        List<String> fields = base("AP", account);
        fields.add(tag(55, symbol));
        fields.add(tag(53, String.valueOf(qty)));
        fields.add(tag(930, confirmId));
        return encode(fields);
    }

    public byte[] buildBasisAdjustmentNotice(String legRef, double bps, String ccyPair) {
        List<String> fields = base("BA", "DESK");
        fields.add(tag(921, legRef));
        fields.add(tag(922, format(bps)));
        fields.add(tag(15, ccyPair));
        return encode(fields);
    }

    public byte[] buildLiquidityTierChange(String account, int tier, String venue) {
        List<String> fields = base("LT", account);
        fields.add(tag(923, String.valueOf(tier)));
        fields.add(tag(914, venue));
        if (tier < 1 || tier > 3) {
            throw new IllegalArgumentException("tier out of range");
        }
        return encode(fields);
    }

    public byte[] buildNostroSweepInstruction(String nostro, double threshold, String targetVault) {
        List<String> fields = base("NS", "TREASURY");
        fields.add(tag(924, nostro));
        fields.add(tag(925, format(threshold)));
        fields.add(tag(914, targetVault));
        return encode(fields);
    }

    public byte[] buildComplianceAlert(String ruleId, String account, String detail) {
        List<String> fields = base("CA", account);
        fields.add(tag(926, ruleId));
        fields.add(tag(58, detail));
        return encode(fields);
    }

    public byte[] buildCheckpointReport(String hashHex, int sequence, String label) {
        List<String> fields = base("CH", "AUDIT");
        fields.add(tag(928, hashHex));
        fields.add(tag(927, String.valueOf(sequence)));
        fields.add(tag(58, label));
        return encode(fields);
    }

    public byte[] buildSwapConfirm(String confirmId, String payLeg, String recvLeg, double payRate, double recvRate) {
        List<String> fields = base("SC", "SWAPDESK");
        fields.add(tag(930, confirmId));
        fields.add(tag(921, payLeg));
        fields.add(tag(922, format(recvRate)));
        fields.add(tag(44, format(payRate)));
        validateSwapRates(payRate, recvRate);
        return encode(fields);
    }

    public byte[] buildCollateralException(String account, String reason, String cusip) {
        List<String> fields = base("CX", account);
        fields.add(tag(48, cusip));
        fields.add(tag(58, reason));
        return encode(fields);
    }

    public byte[] buildVaultTransfer(String fromVault, String toVault, String cusip, long qty) {
        List<String> fields = base("VT", "CUSTODY");
        fields.add(tag(914, fromVault));
        fields.add(tag(915, toVault));
        fields.add(tag(48, cusip));
        fields.add(tag(53, String.valueOf(qty)));
        return encode(fields);
    }

    public byte[] buildHaircutRefresh(String assetClass, String rating, double haircut, boolean stress) {
        List<String> fields = base("HR", "RISK");
        fields.add(tag(916, assetClass));
        fields.add(tag(447, rating));
        fields.add(tag(909, format(haircut)));
        fields.add(tag(918, stress ? "Y" : "N"));
        return encode(fields);
    }

    public byte[] buildConcentrationBreach(String issuer, double pct, double limit) {
        List<String> fields = base("CB", "COMPLIANCE");
        fields.add(tag(58, issuer));
        fields.add(tag(917, format(pct)));
        fields.add(tag(909, format(limit)));
        return encode(fields);
    }

    public byte[] buildSettlementFail(String tradeId, String reason, int cycle) {
        List<String> fields = base("SF", "SETTLEMENT");
        fields.add(tag(920, tradeId));
        fields.add(tag(58, reason));
        fields.add(tag(919, String.valueOf(cycle)));
        return encode(fields);
    }

    public byte[] buildTriPartyInstruction(String instructionId, String agent, String account, double amount) {
        List<String> fields = base("TP", account);
        fields.add(tag(920, instructionId));
        fields.add(tag(448, agent));
        fields.add(tag(910, format(amount)));
        return encode(fields);
    }

    public byte[] buildRepoTermNotice(String termId, int days, double rateBps, double notional) {
        List<String> fields = base("RT", "REPO");
        fields.add(tag(920, termId));
        fields.add(tag(919, String.valueOf(days)));
        fields.add(tag(922, format(rateBps)));
        fields.add(tag(910, format(notional)));
        return encode(fields);
    }

    public byte[] buildCustodyReceipt(String account, String cusip, long qty, String vault) {
        List<String> fields = base("CR", account);
        fields.add(tag(48, cusip));
        fields.add(tag(53, String.valueOf(qty)));
        fields.add(tag(914, vault));
        return encode(fields);
    }

    public byte[] buildReleaseAuthorization(String envelopeId, String approver, String reason) {
        List<String> fields = base("RA", "CUSTODY");
        fields.add(tag(920, envelopeId));
        fields.add(tag(448, approver));
        fields.add(tag(58, reason));
        return encode(fields);
    }

    public byte[] buildPositionReconcileReport(String account, double localMv, double externalMv) {
        List<String> fields = base("PR", account);
        fields.add(tag(910, format(localMv)));
        fields.add(tag(912, format(externalMv)));
        fields.add(tag(913, format(Math.abs(localMv - externalMv))));
        return encode(fields);
    }

    public byte[] buildRecallNotice(String account, String cusip, long qty, String deadline) {
        List<String> fields = base("RC", account);
        fields.add(tag(48, cusip));
        fields.add(tag(53, String.valueOf(qty)));
        fields.add(tag(929, deadline));
        return encode(fields);
    }

    public byte[] buildSubstitutionRequest(String account, String outCusip, String inCusip, double outMv, double inMv) {
        List<String> fields = base("SR", account);
        fields.add(tag(48, outCusip));
        fields.add(tag(55, inCusip));
        fields.add(tag(910, format(outMv)));
        fields.add(tag(912, format(inMv)));
        validateSubstitution(outMv, inMv);
        return encode(fields);
    }

    public byte[] buildFeeDebitAdvice(String account, double feeAmount, String feeType) {
        List<String> fields = base("FD", account);
        fields.add(tag(910, format(feeAmount)));
        fields.add(tag(58, feeType));
        return encode(fields);
    }

    public byte[] buildCorporateActionNotice(String account, String cusip, String actionType, String payDate) {
        List<String> fields = base("CA2", account);
        fields.add(tag(48, cusip));
        fields.add(tag(58, actionType));
        fields.add(tag(64, payDate));
        return encode(fields);
    }

    public byte[] buildTriPartyStatus(String instructionId, String status, String agent) {
        List<String> fields = base("TS", "CUSTODY");
        fields.add(tag(920, instructionId));
        fields.add(tag(911, status));
        fields.add(tag(448, agent));
        return encode(fields);
    }

    public byte[] buildInventoryReport(String vault, int lineCount, double totalMv) {
        List<String> fields = base("IR", vault);
        fields.add(tag(909, String.valueOf(lineCount)));
        fields.add(tag(910, format(totalMv)));
        return encode(fields);
    }

    public byte[] buildRegulatoryCapitalTag(String account, double rwa, double capitalCharge) {
        List<String> fields = base("RCAP", account);
        fields.add(tag(910, format(rwa)));
        fields.add(tag(913, format(capitalCharge)));
        return encode(fields);
    }

    public byte[] buildExposureReport(String account, double gross, double net, double limit) {
        List<String> fields = base("EXP", account);
        fields.add(tag(910, format(gross)));
        fields.add(tag(912, format(net)));
        fields.add(tag(917, format(limit)));
        return encode(fields);
    }

    public byte[] buildSettlementInstruction(String account, String cusip, long qty, String settlDate) {
        List<String> fields = base("SI", account);
        fields.add(tag(48, cusip));
        fields.add(tag(53, String.valueOf(qty)));
        fields.add(tag(64, settlDate));
        return encode(fields);
    }

    private void validateSubstitution(double outMv, double inMv) {
        if (outMv <= 0 || inMv <= 0) {
            throw new IllegalArgumentException("substitution values must be positive");
        }
    }

    private List<String> base(String msgType, String target) {
        List<String> fields = new ArrayList<>();
        fields.add(tag(8, "FIX.4.4"));
        fields.add(tag(35, msgType));
        fields.add(tag(49, "VERIDIAN"));
        fields.add(tag(56, target));
        fields.add(tag(34, String.valueOf(nextSeq())));
        fields.add(tag(52, "20260729-12:00:00"));
        return fields;
    }

    private static int seq = 1;
    private int nextSeq() { return seq++; }

    private String tag(int t, String v) { return t + "=" + v; }

    private byte[] encode(List<String> fields) {
        fields.add(tag(10, checksum(fields)));
        StringBuilder sb = new StringBuilder();
        for (String f : fields) {
            sb.append(f).append(SOH);
        }
        return sb.toString().getBytes(StandardCharsets.US_ASCII);
    }

    private String checksum(List<String> fields) {
        int sum = 0;
        for (String f : fields) {
            for (int i = 0; i < f.length(); i++) {
                sum += f.charAt(i);
            }
            sum += SOH;
        }
        return String.format("%03d", sum % 256);
    }

    private String format(double v) { return String.format("%.4f", v); }

    private void validateMarginReport(double initial, double variation, double total) {
        if (Math.abs((initial + variation) - total) > 0.01) {
            throw new IllegalArgumentException("margin components mismatch");
        }
    }

    private void validateAssignment(long qty, double haircut) {
        if (qty <= 0 || haircut < 0 || haircut > 1) {
            throw new IllegalArgumentException("invalid assignment");
        }
    }

    private void validateSwapRates(double pay, double recv) {
        if (Double.isNaN(pay) || Double.isNaN(recv)) {
            throw new IllegalArgumentException("invalid rates");
        }
    }

    private void requireNonEmpty(String s) {
        if (s == null || s.isEmpty()) {
            throw new IllegalArgumentException("empty reference");
        }
    }
}
