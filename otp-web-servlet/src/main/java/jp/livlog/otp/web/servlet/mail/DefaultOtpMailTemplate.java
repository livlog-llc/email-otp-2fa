package jp.livlog.otp.web.servlet.mail;

public class DefaultOtpMailTemplate implements OtpMailTemplate {

    @Override
    public Rendered render(String appName, String otp, int ttlSeconds) {
        int minutes = Math.max(1, ttlSeconds / 60);

        String subject = "[" + appName + "] 確認コード";

        String text = ""
                + "確認コードは " + otp + " です。\n"
                + "有効期限は " + minutes + " 分です。\n"
                + "このメールに心当たりがない場合は破棄してください。\n";

        String html = ""
                + "<p>確認コードは <b style=\"font-size:20px;letter-spacing:2px;\">" + otp + "</b> です。</p>"
                + "<p>有効期限は <b>" + minutes + " 分</b>です。</p>"
                + "<p style=\"color:#666;\">このメールに心当たりがない場合は破棄してください。</p>";

        return new Rendered(subject, text, html);
    }
}
