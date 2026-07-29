package com.veridian.collateral.wire;

import java.util.HashMap;
import java.util.Map;

public final class SwiftFieldRegistry {
    public static final class FieldDef {
        public final String tag;
        public final String name;
        public final String status;
        public final int maxLen;

        public FieldDef(String tag, String name, String status, int maxLen) {
            this.tag = tag;
            this.name = name;
            this.status = status;
            this.maxLen = maxLen;
        }
    }

    private final Map<String, FieldDef> byTag = new HashMap<>();

    public SwiftFieldRegistry() {
        registerMt940Fields();
        registerMt535Fields();
        registerMt544Fields();
        registerCommonFields();
    }

    private void reg(String tag, String name, String status, int maxLen) {
        byTag.put(tag, new FieldDef(tag, name, status, maxLen));
    }

    private void registerCommonFields() {
        reg(":20:", "Transaction Reference", "M", 16);
        reg(":21:", "Related Reference", "O", 16);
        reg(":23B:", "Bank Operation Code", "M", 4);
        reg(":26T:", "Transaction Type", "O", 3);
        reg(":32A:", "Value Date/Currency/Amount", "M", 24);
        reg(":33B:", "Currency/Instructed Amount", "O", 18);
        reg(":50K:", "Ordering Customer", "O", 140);
        reg(":52A:", "Ordering Institution", "O", 140);
        reg(":53A:", "Sender Correspondent", "O", 140);
        reg(":54A:", "Receiver Correspondent", "O", 140);
        reg(":56A:", "Intermediary", "O", 140);
        reg(":57A:", "Account With Institution", "O", 140);
        reg(":59:", "Beneficiary Customer", "M", 140);
        reg(":70:", "Remittance Information", "O", 140);
        reg(":71A:", "Details of Charges", "M", 3);
        reg(":72:", "Sender to Receiver Info", "O", 210);
    }

    private void registerMt940Fields() {
        reg(":25:", "Account Identification", "M", 35);
        reg(":28C:", "Statement Number/Sequence", "M", 11);
        reg(":60F:", "Opening Balance", "M", 15);
        reg(":60M:", "Opening Balance Intermediate", "O", 15);
        reg(":61:", "Statement Line", "O", 65);
        reg(":62F:", "Closing Balance", "M", 15);
        reg(":62M:", "Closing Balance Intermediate", "O", 15);
        reg(":64:", "Closing Available Balance", "O", 15);
        reg(":65:", "Forward Available Balance", "O", 15);
        reg(":86:", "Information to Account Owner", "O", 390);
        reg(":90D:", "Availability", "O", 140);
        reg(":90C:", "Availability", "O", 140);
    }

    private void registerMt535Fields() {
        reg(":97A:", "Safekeeping Account", "M", 35);
        reg(":17B:", "Activity Flag", "M", 1);
        reg(":35B:", "Identification of Security", "O", 140);
        reg(":93B:", "Balance", "O", 140);
        reg(":94B:", "Place of Safekeeping", "O", 140);
        reg(":19A:", "Amount", "O", 140);
        reg(":90A:", "Price", "O", 140);
        reg(":98A:", "Date", "O", 140);
    }

    private void registerMt544Fields() {
        reg(":16R:", "Start of Block", "M", 6);
        reg(":16S:", "End of Block", "M", 6);
        reg(":35B:", "ISIN", "M", 140);
        reg(":36B:", "Quantity", "M", 140);
        reg(":97A:", "Safekeeping Account", "M", 35);
        reg(":95P:", "Party", "O", 140);
        reg(":98A:", "Settlement Date", "M", 140);
    }

    public FieldDef lookup(String tag) {
        return byTag.get(tag);
    }

    public boolean isMandatory(String tag) {
        FieldDef def = lookup(tag);
        return def != null && "M".equals(def.status);
    }

    public int count() {
        return byTag.size();
    }
}
