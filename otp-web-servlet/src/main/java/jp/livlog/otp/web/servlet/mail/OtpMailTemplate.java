package jp.livlog.otp.web.servlet.mail;

public interface OtpMailTemplate {
    record Rendered(String subject, String textBody, String htmlBody) {}

    Rendered render(String appName, String otp, int ttlSeconds);
}
