package jp.livlog.otp.web.servlet.servlet;

import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jp.livlog.otp.model.VerifyResult;
import jp.livlog.otp.web.servlet.OtpSessionKeys;
import jp.livlog.otp.web.servlet.OtpWebConfig;
import jp.livlog.otp.web.servlet.OtpWebSupport;
import jp.livlog.otp.web.servlet.service.ServletEmailOtp2faService;

import java.io.IOException;

public class OtpVerifyServlet extends HttpServlet {

    private final ServletEmailOtp2faService service;
    private final OtpWebConfig config;

    public OtpVerifyServlet(ServletEmailOtp2faService service, OtpWebConfig config) {
        this.service = service;
        this.config = config;
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        HttpSession session = req.getSession(false);
        if (session == null || !OtpWebSupport.isPasswordOk(session)) {
            resp.sendError(401);
            return;
        }

        String challengeId = (String) session.getAttribute(OtpSessionKeys.CHALLENGE_ID);
        String userId = OtpWebSupport.userId(session);
        if (challengeId == null || challengeId.isBlank() || userId == null || userId.isBlank()) {
            resp.sendRedirect(req.getContextPath() + config.failureRedirect());
            return;
        }

        String otp = req.getParameter("otp");
        if (otp == null || otp.isBlank()) {
            resp.sendRedirect(req.getContextPath() + config.failureRedirect());
            return;
        }

        VerifyResult result = service.verify(challengeId, userId, otp);

        if (result.ok()) {
            session.setAttribute(OtpSessionKeys.MFA_OK, Boolean.TRUE);
            resp.sendRedirect(req.getContextPath() + config.successRedirect());
        } else {
            // 失敗時はMFA継続（challengeは残す）
            resp.sendRedirect(req.getContextPath() + config.failureRedirect());
        }
    }
}
