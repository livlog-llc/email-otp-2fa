package jp.livlog.otp.web.servlet;

import java.util.Set;

public record OtpWebConfig(
        String mfaPagePath,      // 例: "/mfa"（MFA画面URL）
        String startPath,        // 例: "/mfa/start"
        String verifyPath,       // 例: "/mfa/verify"
        String successRedirect,  // 例: "/app"
        String failureRedirect,  // 例: "/mfa?error=1"
        Set<String> protectedPathPrefixes // 例: Set.of("/app")
) {
    public static OtpWebConfig defaultConfig() {
        return new OtpWebConfig(
                "/mfa",
                "/mfa/start",
                "/mfa/verify",
                "/app",
                "/mfa?error=1",
                Set.of("/app")
        );
    }
}
