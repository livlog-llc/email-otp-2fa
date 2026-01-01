package jp.livlog.otp.web.servlet;

import jakarta.servlet.http.HttpSession;

public final class OtpWebSupport {
    private OtpWebSupport() {}

    public static void markPasswordOk(HttpSession session, String userId, String email) {
        session.setAttribute(OtpSessionKeys.USER_ID, userId);
        session.setAttribute(OtpSessionKeys.USER_EMAIL, email);
        session.setAttribute(OtpSessionKeys.PASSWORD_OK, Boolean.TRUE);
        session.setAttribute(OtpSessionKeys.MFA_OK, Boolean.FALSE);
        session.removeAttribute(OtpSessionKeys.CHALLENGE_ID);
    }

    public static boolean isPasswordOk(HttpSession session) {
        return session != null && Boolean.TRUE.equals(session.getAttribute(OtpSessionKeys.PASSWORD_OK));
    }

    public static boolean isMfaOk(HttpSession session) {
        return session != null && Boolean.TRUE.equals(session.getAttribute(OtpSessionKeys.MFA_OK));
    }

    public static String userId(HttpSession session) {
        Object v = session.getAttribute(OtpSessionKeys.USER_ID);
        return v != null ? v.toString() : null;
    }

    public static String userEmail(HttpSession session) {
        Object v = session.getAttribute(OtpSessionKeys.USER_EMAIL);
        return v != null ? v.toString() : null;
    }
}
