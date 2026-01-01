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

import java.util.Objects;

public class ServletEmailOtp2faService {

    private final String appName;
    private final OtpPolicy policy;
    private final OtpChallengeStore store;
    private final OtpMailer mailer;
    private final OtpMailTemplate template;

    private final OtpIssuer issuer;
    private final OtpVerifier verifier;

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
        this.store = Objects.requireNonNull(store, "store");
        this.mailer = Objects.requireNonNull(mailer, "mailer");
        this.template = Objects.requireNonNull(template, "template");

        // coreロジック
        this.issuer = new OtpIssuer(policy, clock, new OtpGenerator(), new OtpHasher());
        this.verifier = new OtpVerifier(policy, clock, new OtpHasher());
    }

    /** OTP発行→保存→メール送信。challengeId を返す */
    public String start(String userId, String email) {
        var issued = issuer.issue(userId);

        // 保存
        store.save(issued.challenge());

        // メール送信（subject/bodyはテンプレで）
        var rendered = template.render(appName, issued.otp(), policy.ttlSeconds());
        mailer.send(new OtpMailRequest(
                email,
                rendered.subject(),
                rendered.textBody(),
                rendered.htmlBody()
        ));

        return issued.challengeId();
    }

    /** OTP検証→更新保存。VerifyResult を返す */
    public VerifyResult verify(String challengeId, String otpInput) {
        var currentOpt = store.find(challengeId);
        if (currentOpt.isEmpty()) {
            return VerifyResult.failure(VerifyResult.Reason.NOT_FOUND);
        }

        OtpChallenge current = currentOpt.get();
        var update = verifier.verify(current, otpInput);

        // 状態更新を保存（EXPIRED/LOCKED/VERIFIED へ遷移する）
        if (update.updated() != null) {
            store.save(update.updated());
        }

        return update.result();
    }
}
