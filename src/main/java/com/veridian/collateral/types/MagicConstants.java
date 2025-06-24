package com.veridian.collateral.types;

public final class MagicConstants {
    private MagicConstants() {}

    public static final int kBatchMagic = 0x56434231; // VCB1
    public static final int kEnvelopeMagic = 0x56434531; // VCE1
    public static final int kSessionMagic = 0x56435331; // VCS1
    public static final int kPledgeMagic = 0x56435031; // VCP1
    public static final int kMarginMagic = 0x56434D31; // VCM1
    public static final int kHaircutMagic = 0x56434831; // VCH1
    public static final int kSweepMagic = 0x56435357; // VCSW

    public static boolean isKnownBatch(int magic) {
        return magic == kBatchMagic || magic == kPledgeMagic;
    }

    public static boolean isKnownEnvelope(int magic) {
        return magic == kEnvelopeMagic || magic == kSweepMagic;
    }

    public static boolean isKnownSession(int magic) {
        return magic == kSessionMagic || magic == kMarginMagic;
    }
}
