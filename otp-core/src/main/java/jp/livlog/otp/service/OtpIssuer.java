package jp.livlog.otp.service;

import jp.livlog.otp.gen.OtpGenerator;
import jp.livlog.otp.hash.OtpHasher;
import jp.livlog.otp.model.OtpChallenge;
import jp.livlog.otp.policy.Clock;
import jp.livlog.otp.policy.OtpPolicy;

import java.time.Instant;
import java.util.UUID;

public class OtpIssuer {
    private final OtpPolicy policy;
    private final Clock clock;
    private final OtpGenerator generator;
    private final OtpHasher hasher;

    public record IssueResult(String challengeId, String otp, OtpChallenge challenge) {}

    public OtpIssuer(OtpPolicy policy, Clock clock, OtpGenerator generator, OtpHasher hasher) {
        this.policy = policy;
        this.clock = clock;
        this.generator = generator;
        this.hasher = hasher;
    }

    public IssueResult issue(String userId) {
        String otp = generator.generate(policy.digits());
        String otpHash = hasher.hash(otp);

        Instant now = clock.now();
        Instant exp = now.plusSeconds(policy.ttlSeconds());

        String challengeId = UUID.randomUUID().toString();
        OtpChallenge c = new OtpChallenge(
                challengeId,
                userId,
                otpHash,
                exp,
                0,
                0,
                now,
                OtpChallenge.Status.PENDING
        );
        return new IssueResult(challengeId, otp, c);
    }

    public IssueResult reissue(OtpChallenge current) {
        String otp = generator.generate(policy.digits());
        String otpHash = hasher.hash(otp);

        Instant now = clock.now();

        OtpChallenge updated = new OtpChallenge(
                current.challengeId(),
                current.userId(),
                otpHash,
                current.expiresAt(),
                current.attempts(),
                current.resends() + 1,
                now,
                OtpChallenge.Status.PENDING
        );

        return new IssueResult(current.challengeId(), otp, updated);
    }
}
