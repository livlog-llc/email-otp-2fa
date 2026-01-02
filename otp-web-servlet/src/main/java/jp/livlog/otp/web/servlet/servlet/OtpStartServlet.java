package jp.livlog.otp.web.servlet.servlet;

import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jp.livlog.otp.web.servlet.OtpSessionKeys;
import jp.livlog.otp.web.servlet.OtpWebSupport;
import jp.livlog.otp.web.servlet.service.ServletEmailOtp2faService;

import java.io.IOException;

public class OtpStartServlet extends HttpServlet {

    private final ServletEmailOtp2faService service;

    public OtpStartServlet(ServletEmailOtp2faService service) {
        this.service = service;
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        HttpSession session = req.getSession(false);
        if (session == null || !OtpWebSupport.isPasswordOk(session)) {
            resp.sendError(401);
            return;
        }

        String userId = OtpWebSupport.userId(session);
        String email = OtpWebSupport.userEmail(session);
        if (userId == null || email == null) {
            resp.sendError(400, "Missing session attributes: userId/email");
            return;
        }

        var result = service.start(userId, email);
        if (result.status() != ServletEmailOtp2faService.StartResult.StartStatus.SENT) {
            resp.sendError(429);
            return;
        }

        session.setAttribute(OtpSessionKeys.CHALLENGE_ID, result.challengeId());
        session.setAttribute(OtpSessionKeys.MFA_OK, Boolean.FALSE);

        resp.setStatus(204); // No Content
    }
}
