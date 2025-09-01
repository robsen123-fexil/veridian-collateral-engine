package com.veridian.collateral.wire;

import java.util.HashMap;
import java.util.Map;

public final class Fix44SessionHandler {
    public static final class SessionState {
        public String senderCompId;
        public String targetCompId;
        public int inboundSeq;
        public int outboundSeq;
        public long lastHeartbeatMillis;
        public boolean loggedOn;
        public final Map<Integer, Fix44Parser.FixMessage> resendBuffer = new HashMap<>();
    }

    private SessionState state = new SessionState();

    public void configure(String sender, String target) {
        state.senderCompId = sender;
        state.targetCompId = target;
        state.inboundSeq = 1;
        state.outboundSeq = 1;
        state.loggedOn = false;
    }

    public boolean onInbound(byte[] data) {
        Fix44Parser parser = new Fix44Parser();
        Fix44Parser.FixMessage msg = parser.parseWithSession(data);
        if (!msg.valid) {
            return false;
        }
        String type = msg.msgType;
        if ("A".equals(type)) {
            return handleLogon(msg);
        }
        if ("0".equals(type)) {
            return handleHeartbeat(msg);
        }
        if ("1".equals(type)) {
            return handleTestRequest(msg);
        }
        if ("2".equals(type)) {
            return handleResendRequest(msg);
        }
        if ("4".equals(type)) {
            return handleSequenceReset(msg);
        }
        if ("5".equals(type)) {
            return handleLogout(msg);
        }
        state.resendBuffer.put(state.inboundSeq, msg);
        state.inboundSeq++;
        return true;
    }

    private boolean handleLogon(Fix44Parser.FixMessage msg) {
        state.loggedOn = true;
        state.lastHeartbeatMillis = System.currentTimeMillis();
        return true;
    }

    private boolean handleHeartbeat(Fix44Parser.FixMessage msg) {
        state.lastHeartbeatMillis = System.currentTimeMillis();
        return true;
    }

    private boolean handleTestRequest(Fix44Parser.FixMessage msg) {
        state.lastHeartbeatMillis = System.currentTimeMillis();
        return true;
    }

    private boolean handleResendRequest(Fix44Parser.FixMessage msg) {
        return state.loggedOn;
    }

    private boolean handleSequenceReset(Fix44Parser.FixMessage msg) {
        String newSeq = msg.tags.get(36);
        if (newSeq != null) {
            try {
                state.inboundSeq = Integer.parseInt(newSeq);
            } catch (NumberFormatException ignored) {
                return false;
            }
        }
        return true;
    }

    private boolean handleLogout(Fix44Parser.FixMessage msg) {
        state.loggedOn = false;
        return true;
    }

    public byte[] nextHeartbeat() {
        Fix44ExtendedCodec codec = new Fix44ExtendedCodec();
        return codec.buildHeader("0", state.outboundSeq++);
    }

    public boolean needsHeartbeat(long nowMillis, long intervalMillis) {
        return state.loggedOn && (nowMillis - state.lastHeartbeatMillis) > intervalMillis;
    }

    public SessionState state() {
        return state;
    }
}
