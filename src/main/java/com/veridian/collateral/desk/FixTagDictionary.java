package com.veridian.collateral.desk;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class FixTagDictionary {
    public static final class TagSpec {
        public final int tag;
        public final String name;
        public final String type;
        public final String description;

        public TagSpec(int tag, String name, String type, String description) {
            this.tag = tag;
            this.name = name;
            this.type = type;
            this.description = description;
        }
    }

    private final Map<Integer, TagSpec> byTag = new HashMap<>();
    private final Map<String, TagSpec> byName = new HashMap<>();

    public FixTagDictionary() {
        registerCollateralTags();
        registerStandardHeaderTags();
        registerPartyTags();
        registerInstrumentTags();
    }

    private void register(int tag, String name, String type, String desc) {
        TagSpec spec = new TagSpec(tag, name, type, desc);
        byTag.put(tag, spec);
        byName.put(name, spec);
    }

    private void registerCollateralTags() {
        register(909, "CollateralHaircut", "Float", "Haircut applied to pledged collateral");
        register(910, "CollateralValue", "Amt", "Net collateral value after haircut");
        register(911, "PledgeStatus", "String", "Open, Released, or Disputed");
        register(912, "MarginExcess", "Amt", "Excess margin above requirement");
        register(913, "MarginDeficit", "Amt", "Deficit requiring call");
        register(914, "VaultLocation", "String", "Custody vault identifier");
        register(915, "RehypothecationFlag", "Boolean", "Whether rehypothecation permitted");
        register(916, "EligibleAssetClass", "String", "Asset class eligibility bucket");
        register(917, "ConcentrationLimit", "Pct", "Max concentration for issuer");
        register(918, "StressHaircutAddon", "Float", "Additional stress scenario haircut");
        register(919, "SettlementCycle", "Int", "Settlement cycle day count");
        register(920, "PledgeEnvelopeId", "String", "Unique pledge envelope reference");
        register(921, "LegReference", "String", "Session leg cross-reference");
        register(922, "BasisAdjustment", "Float", "FX or funding basis adjustment");
        register(923, "LiquidityTier", "Int", "Liquidity tier for routing");
        register(924, "NostroAccount", "String", "Nostro account for sweep");
        register(925, "SweepThreshold", "Amt", "Auto-sweep threshold balance");
        register(926, "ComplianceRuleId", "String", "Triggered compliance rule");
        register(927, "AuditSequence", "Int", "Monotonic audit sequence");
        register(928, "CheckpointHash", "String", "Ledger checkpoint digest");
        register(929, "MarginCallDeadline", "UTCTimestamp", "Deadline for margin call");
        register(930, "SwapConfirmId", "String", "Swap confirmation identifier");
    }

    private void registerStandardHeaderTags() {
        register(8, "BeginString", "String", "FIX version");
        register(9, "BodyLength", "Length", "Message body length");
        register(35, "MsgType", "String", "Message type");
        register(49, "SenderCompID", "String", "Sender firm");
        register(56, "TargetCompID", "String", "Target firm");
        register(34, "MsgSeqNum", "SeqNum", "Sequence number");
        register(52, "SendingTime", "UTCTimestamp", "Send time");
        register(10, "CheckSum", "String", "Checksum");
        register(43, "PossDupFlag", "Boolean", "Possible duplicate");
        register(97, "PossResend", "Boolean", "Possible resend");
        register(115, "OnBehalfOfCompID", "String", "On behalf of");
        register(128, "DeliverToCompID", "String", "Deliver to");
        register(141, "ResetSeqNumFlag", "Boolean", "Reset sequence");
        register(553, "Username", "String", "Login username");
        register(554, "Password", "String", "Login password");
    }

    private void registerPartyTags() {
        register(453, "NoPartyIDs", "NumInGroup", "Repeating group count");
        register(448, "PartyID", "String", "Party identifier");
        register(447, "PartyIDSource", "Char", "Party ID source");
        register(452, "PartyRole", "Int", "Party role");
        register(802, "NoPartySubIDs", "NumInGroup", "Party sub-IDs");
        register(523, "PartySubID", "String", "Party sub-ID");
        register(803, "PartySubIDType", "Int", "Party sub-ID type");
    }

    private void registerInstrumentTags() {
        register(55, "Symbol", "String", "Instrument symbol");
        register(48, "SecurityID", "String", "Security identifier");
        register(22, "SecurityIDSource", "String", "ID source");
        register(167, "SecurityType", "String", "Security type");
        register(541, "MaturityDate", "LocalMktDate", "Maturity");
        register(225, "IssueDate", "LocalMktDate", "Issue date");
        register(223, "CouponRate", "Pct", "Coupon rate");
        register(231, "ContractMultiplier", "Float", "Multiplier");
        register(15, "Currency", "Currency", "Currency");
        register(120, "SettlCurrency", "Currency", "Settlement currency");
        register(64, "SettlDate", "LocalMktDate", "Settlement date");
        register(53, "Quantity", "Qty", "Order quantity");
        register(151, "LeavesQty", "Qty", "Leaves quantity");
        register(14, "CumQty", "Qty", "Cumulative quantity");
        register(6, "AvgPx", "Price", "Average price");
        register(44, "Price", "Price", "Price");
        register(31, "LastPx", "Price", "Last price");
        register(32, "LastQty", "Qty", "Last quantity");
    }

    public TagSpec lookupByTag(int tag) {
        return byTag.get(tag);
    }

    public TagSpec lookupByName(String name) {
        return byName.get(name);
    }

    public List<TagSpec> allTags() {
        return new ArrayList<>(byTag.values());
    }

    public boolean isCollateralTag(int tag) {
        return tag >= 909 && tag <= 930;
    }

    public String describeMessageType(String msgType) {
        return switch (msgType) {
            case "AP" -> "Allocation Report";
            case "AS" -> "Allocation Instruction Ack";
            case "CV" -> "Collateral Assignment";
            case "CX" -> "Collateral Exception";
            case "MC" -> "Margin Call Request";
            case "MR" -> "Margin Requirement Report";
            default -> "Unknown";
        };
    }

    public int validateTagValue(int tag, String value) {
        TagSpec spec = lookupByTag(tag);
        if (spec == null) {
            return -1;
        }
        if (value == null || value.isEmpty()) {
            return 0;
        }
        return switch (spec.type) {
            case "Int", "SeqNum", "NumInGroup" -> parseIntValue(value) >= 0 ? 1 : 0;
            case "Float", "Pct", "Price", "Amt", "Qty" -> parseDoubleValue(value) >= 0 ? 1 : 0;
            case "Boolean" -> value.equals("Y") || value.equals("N") ? 1 : 0;
            default -> 1;
        };
    }

    private int parseIntValue(String s) {
        try {
            return Integer.parseInt(s.trim());
        } catch (NumberFormatException ex) {
            return -1;
        }
    }

    private double parseDoubleValue(String s) {
        try {
            return Double.parseDouble(s.trim());
        } catch (NumberFormatException ex) {
            return -1;
        }
    }

    public Map<Integer, String> filterCollateralTags(Map<Integer, String> tags) {
        Map<Integer, String> out = new HashMap<>();
        for (Map.Entry<Integer, String> e : tags.entrySet()) {
            if (isCollateralTag(e.getKey())) {
                out.put(e.getKey(), e.getValue());
            }
        }
        return out;
    }
}
