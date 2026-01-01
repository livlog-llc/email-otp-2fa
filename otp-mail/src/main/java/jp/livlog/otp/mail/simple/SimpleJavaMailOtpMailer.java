package jp.livlog.otp.mail.simple;

import jp.livlog.otp.mail.OtpMailRequest;
import jp.livlog.otp.mail.OtpMailer;
import jp.livlog.otp.mail.SmtpConfig;
import org.simplejavamail.api.email.Email;
import org.simplejavamail.api.email.EmailPopulatingBuilder;
import org.simplejavamail.api.mailer.Mailer;
import org.simplejavamail.api.mailer.config.TransportStrategy;
import org.simplejavamail.email.EmailBuilder;
import org.simplejavamail.mailer.MailerBuilder;

import java.util.Objects;
import java.util.Properties;

public class SimpleJavaMailOtpMailer implements OtpMailer {

    private final Mailer mailer;
    private final String fromAddress;
    private final String fromName;

    /**
     * 完全にプログラム設定で作る（最もわかりやすく横展向き）
     */
    public SimpleJavaMailOtpMailer(SmtpConfig cfg) {
        Objects.requireNonNull(cfg, "cfg");
        this.fromAddress = Objects.requireNonNull(cfg.fromAddress(), "fromAddress");
        this.fromName = cfg.fromName();

        TransportStrategy strategy = switch (cfg.transport()) {
            case PLAIN -> TransportStrategy.SMTP;
            case STARTTLS -> TransportStrategy.SMTP_TLS;
            case SMTPS -> TransportStrategy.SMTPS;
        };

        var builder = MailerBuilder.withSMTPServer(cfg.host(), cfg.port(), cfg.username(), cfg.password())
                .withTransportStrategy(strategy);

        // タイムアウト（JavaMailのプロパティを渡す）
        Properties props = new Properties();
        if (cfg.connectionTimeoutMs() != null) props.put("mail.smtp.connectiontimeout", String.valueOf(cfg.connectionTimeoutMs()));
        if (cfg.readTimeoutMs() != null) props.put("mail.smtp.timeout", String.valueOf(cfg.readTimeoutMs()));
        if (cfg.writeTimeoutMs() != null) props.put("mail.smtp.writetimeout", String.valueOf(cfg.writeTimeoutMs()));
        if (!props.isEmpty()) builder.withProperties(props);

        // Mailerは「作って使い回す」設計が基本（スレッドセーフ想定）
        this.mailer = builder.buildMailer();
    }

    /**
     * properties/env で設定したい場合：
     * Simple Java Mail は classpath 上の simplejavamail.properties などから設定できます。:contentReference[oaicite:2]{index=2}
     * ここでは Mailer を外から注入できる形も用意します。
     */
    public SimpleJavaMailOtpMailer(Mailer mailer, String fromAddress, String fromName) {
        this.mailer = Objects.requireNonNull(mailer, "mailer");
        this.fromAddress = Objects.requireNonNull(fromAddress, "fromAddress");
        this.fromName = fromName;
    }

    @Override
    public void send(OtpMailRequest req) {
        EmailPopulatingBuilder eb = EmailBuilder.startingBlank()
                .from(fromName != null ? fromName : "", fromAddress)
                .to(req.to())
                .withSubject(req.subject());

        if (req.textBody() != null && !req.textBody().isBlank()) {
            eb = eb.withPlainText(req.textBody());
        }
        if (req.htmlBody() != null && !req.htmlBody().isBlank()) {
            eb = eb.withHTMLText(req.htmlBody());
        }

        Email email = eb.buildEmail();
        mailer.sendMail(email);
    }
}
