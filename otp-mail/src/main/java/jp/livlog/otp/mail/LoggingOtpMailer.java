package jp.livlog.otp.mail;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Development-friendly mailer that simply logs OTP contents instead of sending an email.
 */
public class LoggingOtpMailer implements OtpMailer {

    private static final Logger log = LoggerFactory.getLogger(LoggingOtpMailer.class);

    @Override
    public void send(OtpMailRequest request) {
        log.info("[LoggingOtpMailer] to={} subject={} textBody={} htmlBody={}",
                request.to(), request.subject(), request.textBody(), request.htmlBody());
    }
}
