package jp.livlog.otp.web.servlet.service;

import jp.livlog.otp.gen.OtpGenerator;
import jp.livlog.otp.hash.OtpHasher;
import jp.livlog.otp.model.OtpChallenge;
import jp.livlog.otp.model.VerifyResult;
import jp.livlog.otp.policy.Clock;
import jp.livlog.otp.policy.OtpPolicy;
import jp.livlog.otp.service.OtpIssuer;
import jp.livlog.otp.service.OtpVerifier;
import jp.livlog.otp.storage.OtpChallengeStore;
import jp.livlog.otp.mail.OtpMailer;
import jp.livlog.otp.mail.OtpMailRequest;
import jp.livlog.otp.web.servlet.mail.OtpMailTemplate;

import java.time.Instant;
import java.util.Objects;

public class ServletEmailOtp2faService {

    private final String appName;
    private final OtpPolicy policy;
    private final Clock clock;
    private final OtpChallengeStore store;
    private final OtpMailer mailer;
    private final OtpMailTemplate template;

    private final OtpIssuer issuer;
    private final OtpVerifier verifier;

    public record StartResult(String challengeId, StartStatus status) {
        public enum StartStatus {
            SENT,
            TOO_FREQUENT,
            RESEND_LIMIT_EXCEEDED
        }

        public static StartResult sent(String challengeId) { return new StartResult(challengeId, StartStatus.SENT); }
        public static StartResult tooFrequent(String challengeId) {
            return new StartResult(challengeId, StartStatus.TOO_FREQUENT);
        }
        public static StartResult resendLimit(String challengeId) {
            return new StartResult(challengeId, StartStatus.RESEND_LIMIT_EXCEEDED);
        }
    }

    public ServletEmailOtp2faService(
            String appName,
            OtpPolicy policy,
            Clock clock,
            OtpChallengeStore store,
            OtpMailer mailer,
            OtpMailTemplate template
    ) {
        this.appName = Objects.requireNonNull(appName, "appName");
        this.policy = Objects.requireNonNull(policy, "policy");
        this.clock = Objects.requireNonNull(clock, "clock");
        this.store = Objects.requireNonNull(store, "store");
        this.mailer = Objects.requireNonNull(mailer, "mailer");
        this.template = Objects.requireNonNull(template, "template");

        // coreロジック
        this.issuer = new OtpIssuer(policy, clock, new OtpGenerator(), new OtpHasher());
        this.verifier = new OtpVerifier(policy, clock, new OtpHasher());
    }

    /** OTP発行/再送→保存→メール送信。結果を返す */
    public StartResult start(String userId, String email) {
        Instant now = clock.now();
        var existing = store.findLatestPendingByUser(userId);

        var resendDecision = existing
                .filter(ch -> now.isBefore(ch.expiresAt()))
                .map(ch -> decideResend(ch, now));

        if (resendDecision.isPresent()) {
            var decision = resendDecision.get();
            if (decision.failure() != null) {
                return decision.failure();
            }
            return sendAndPersist(decision.issued(), email);
        }

        var issued = issuer.issue(userId);
        return sendAndPersist(issued, email);
    }

    /** OTP検証→更新保存。VerifyResult を返す */
    public VerifyResult verify(String challengeId, String userId, String otpInput) {
        var currentOpt = store.find(challengeId);
        if (currentOpt.isEmpty()) {
            return VerifyResult.failure(VerifyResult.Reason.NOT_FOUND);
        }

        OtpChallenge current = currentOpt.get();
        if (!Objects.equals(userId, current.userId())) {
            return VerifyResult.failure(VerifyResult.Reason.NOT_FOUND);
        }
        var update = verifier.verify(current, otpInput);

        // 状態更新を保存（EXPIRED/LOCKED/VERIFIED へ遷移する）
        if (update.updated() != null) {
            store.save(update.updated());
        }

        return update.result();
    }

    private ResendDecision decideResend(OtpChallenge challenge, Instant now) {
        if (challenge.resends() >= policy.maxResends()) {
            return new ResendDecision(null, StartResult.resendLimit(challenge.challengeId()));
        }

        Instant lastSentAt = challenge.lastSentAt();
        if (lastSentAt != null) {
            Instant nextAllowed = lastSentAt.plusSeconds(policy.minResendIntervalSeconds());
            if (now.isBefore(nextAllowed)) {
                return new ResendDecision(null, StartResult.tooFrequent(challenge.challengeId()));
            }
        }

        return new ResendDecision(issuer.reissue(challenge), null);
    }

    private StartResult sendAndPersist(OtpIssuer.IssueResult issued, String email) {
        store.save(issued.challenge());

        // メール送信（subject/bodyはテンプレで）
        var rendered = template.render(appName, issued.otp(), policy.ttlSeconds());
        mailer.send(new OtpMailRequest(
                email,
                rendered.subject(),
                rendered.textBody(),
                rendered.htmlBody()
        ));
        return StartResult.sent(issued.challengeId());
    }

    private record ResendDecision(OtpIssuer.IssueResult issued, StartResult failure) {}
}
