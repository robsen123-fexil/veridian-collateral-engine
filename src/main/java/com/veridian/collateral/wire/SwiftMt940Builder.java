package com.veridian.collateral.wire;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public final class SwiftMt940Builder {
    public static final class StatementBuilder {
        private String account = "";
        private String currency = "USD";
        private double opening;
        private double closing;
        private final List<String> field61 = new ArrayList<>();

        public StatementBuilder account(String value) {
            this.account = value;
            return this;
        }

        public StatementBuilder currency(String value) {
            this.currency = value;
            return this;
        }

        public StatementBuilder openingBalance(double amount) {
            this.opening = amount;
            return this;
        }

        public StatementBuilder closingBalance(double amount) {
            this.closing = amount;
            return this;
        }

        public StatementBuilder addTransaction(String valueDate, char dc, double amount, String ref) {
            field61.add(valueDate + dc + formatAmount(amount) + "NTRF" + ref + "//" + ref);
            return this;
        }

        public byte[] build() {
            StringBuilder sb = new StringBuilder();
            sb.append(":20:STMT").append(System.lineSeparator());
            sb.append(":25:").append(account).append(System.lineSeparator());
            sb.append(":28C:00001/001").append(System.lineSeparator());
            sb.append(":60F:C").append(today()).append(currency).append(formatAmount(opening))
                .append(System.lineSeparator());
            for (String f61 : field61) {
                sb.append(":61:").append(f61).append(System.lineSeparator());
            }
            sb.append(":62F:C").append(today()).append(currency).append(formatAmount(closing))
                .append(System.lineSeparator());
            return sb.toString().getBytes(StandardCharsets.US_ASCII);
        }

        private String today() {
            return "260729";
        }

        private String formatAmount(double amount) {
            return String.format("%.2f", amount).replace('.', ',');
        }
    }

    public StatementBuilder newStatement() {
        return new StatementBuilder();
    }
}
