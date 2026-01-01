package jp.livlog.otp.service;

import jp.livlog.otp.hash.OtpHasher;
import jp.livlog.otp.model.OtpChallenge;
import jp.livlog.otp.model.VerifyResult;
import jp.livlog.otp.policy.Clock;
import jp.livlog.otp.policy.OtpPolicy;

import java.time.Instant;

public class OtpVerifier {

    private final OtpPolicy policy;
    private final Clock clock;
    private final OtpHasher hasher;

    public record VerifyUpdate(VerifyResult result, OtpChallenge updated) {}

    public OtpVerifier(OtpPolicy policy, Clock clock, OtpHasher hasher) {
        this.policy = policy;
        this.clock = clock;
        this.hasher = hasher;
    }

    public VerifyUpdate verify(OtpChallenge current, String otpInput) {

        // ① チャレンジが無い
        if (current == null) {
            return new VerifyUpdate(
                    VerifyResult.failure(VerifyResult.Reason.NOT_FOUND),
                    null
            );
        }

        // ② すでに成功済み
        if (current.status() == OtpChallenge.Status.VERIFIED) {
            return new VerifyUpdate(
                    VerifyResult.failure(VerifyResult.Reason.ALREADY_VERIFIED),
                    current
            );
        }

        // ③ ロック済み
        if (current.status() == OtpChallenge.Status.LOCKED) {
            return new VerifyUpdate(
                    VerifyResult.failure(VerifyResult.Reason.LOCKED),
                    current
            );
        }

        Instant now = clock.now();

        // ④ 有効期限切れ
        if (now.isAfter(current.expiresAt())) {
            OtpChallenge expired = new OtpChallenge(
                    current.challengeId(),
                    current.userId(),
                    current.otpHash(),
                    current.expiresAt(),
                    current.attempts(),
                    current.resends(),
                    current.lastSentAt(),
                    OtpChallenge.Status.EXPIRED
            );
            return new VerifyUpdate(
                    VerifyResult.failure(VerifyResult.Reason.EXPIRED),
                    expired
            );
        }

        // ⑤ 試行回数オーバー
        if (current.attempts() >= policy.maxAttempts()) {
            OtpChallenge locked = new OtpChallenge(
                    current.challengeId(),
                    current.userId(),
                    current.otpHash(),
                    current.expiresAt(),
                    current.attempts(),
                    current.resends(),
                    current.lastSentAt(),
                    OtpChallenge.Status.LOCKED
            );
            return new VerifyUpdate(
                    VerifyResult.failure(VerifyResult.Reason.TOO_MANY_ATTEMPTS),
                    locked
            );
        }

        // ⑥ OTP検証
        boolean ok = hasher.verify(otpInput, current.otpHash());
        int nextAttempts = current.attempts() + 1;

        if (ok) {
            OtpChallenge verified = new OtpChallenge(
                    current.challengeId(),
                    current.userId(),
                    current.otpHash(),
                    current.expiresAt(),
                    nextAttempts,
                    current.resends(),
                    current.lastSentAt(),
                    OtpChallenge.Status.VERIFIED
            );
            return new VerifyUpdate(
                    VerifyResult.success(),
                    verified
            );
        } else {
            OtpChallenge retry = new OtpChallenge(
                    current.challengeId(),
                    current.userId(),
                    current.otpHash(),
                    current.expiresAt(),
                    nextAttempts,
                    current.resends(),
                    current.lastSentAt(),
                    OtpChallenge.Status.PENDING
            );
            return new VerifyUpdate(
                    VerifyResult.failure(VerifyResult.Reason.INVALID_CODE),
                    retry
            );
        }
    }
}
