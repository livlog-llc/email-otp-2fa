package jp.livlog.otp.model;

public record VerifyResult(
        boolean ok,
        Reason reason
) {
    public enum Reason {
        OK,
        NOT_FOUND,
        EXPIRED,
        LOCKED,
        TOO_MANY_ATTEMPTS,
        INVALID_CODE,
        ALREADY_VERIFIED
    }

    public static VerifyResult success() { return new VerifyResult(true, Reason.OK); }
    public static VerifyResult failure(Reason r) { return new VerifyResult(false, r); }
}
