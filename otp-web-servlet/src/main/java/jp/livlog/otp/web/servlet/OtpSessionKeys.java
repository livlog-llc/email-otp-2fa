package jp.livlog.otp.web.servlet;

public final class OtpSessionKeys {
    private OtpSessionKeys() {}

    // アプリ側がセットする（パスワード認証OK後）
    public static final String USER_ID = "otp.userId";
    public static final String USER_EMAIL = "otp.userEmail";
    public static final String PASSWORD_OK = "otp.passwordOk";

    // otp-web-servlet が管理
    public static final String MFA_OK = "otp.mfaOk";
    public static final String CHALLENGE_ID = "otp.challengeId";
}
