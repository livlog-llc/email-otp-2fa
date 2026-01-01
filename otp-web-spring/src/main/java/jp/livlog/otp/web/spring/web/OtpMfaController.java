package jp.livlog.otp.web.spring.web;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jp.livlog.otp.model.VerifyResult;
import jp.livlog.otp.web.spring.OtpSessionKeys;
import jp.livlog.otp.web.spring.OtpWebProperties;
import jp.livlog.otp.web.spring.OtpWebSupport;
import jp.livlog.otp.web.spring.service.SpringEmailOtp2faService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
public class OtpMfaController {

    private final SpringEmailOtp2faService service;
    private final OtpWebProperties props;

    public OtpMfaController(SpringEmailOtp2faService service, OtpWebProperties props) {
        this.service = service;
        this.props = props;
    }

    @PostMapping("${otp.web.start-path:/mfa/start}")
    public ResponseEntity<Void> start(HttpServletRequest req) {
        HttpSession session = req.getSession(false);
        if (session == null || !OtpWebSupport.isPasswordOk(session)) {
            return ResponseEntity.status(401).build();
        }

        String userId = OtpWebSupport.userId(session);
        String email = OtpWebSupport.userEmail(session);
        if (userId == null || email == null) {
            return ResponseEntity.badRequest().build();
        }

        var result = service.start(userId, email);
        if (result.status() != SpringEmailOtp2faService.StartResult.StartStatus.SENT) {
            return ResponseEntity.status(429).build();
        }

        session.setAttribute(OtpSessionKeys.CHALLENGE_ID, result.challengeId());
        session.setAttribute(OtpSessionKeys.MFA_OK, Boolean.FALSE);

        return ResponseEntity.noContent().build();
    }

    @PostMapping("${otp.web.verify-path:/mfa/verify}")
    public ResponseEntity<Void> verify(@RequestParam("otp") String otp, HttpServletRequest req) {
        HttpSession session = req.getSession(false);
        if (session == null || !OtpWebSupport.isPasswordOk(session)) {
            return ResponseEntity.status(401).build();
        }

        String challengeId = (String) session.getAttribute(OtpSessionKeys.CHALLENGE_ID);
        String userId = OtpWebSupport.userId(session);
        if (challengeId == null || challengeId.isBlank() || otp == null || otp.isBlank() || userId == null) {
            return redirect(req, props.getFailureRedirect());
        }

        VerifyResult result = service.verify(challengeId, userId, otp);
        if (result.ok()) {
            session.setAttribute(OtpSessionKeys.MFA_OK, Boolean.TRUE);
            return redirect(req, props.getSuccessRedirect());
        }
        return redirect(req, props.getFailureRedirect());
    }

    private ResponseEntity<Void> redirect(HttpServletRequest req, String path) {
        String ctx = req.getContextPath() == null ? "" : req.getContextPath();
        return ResponseEntity.status(302).location(URI.create(ctx + path)).build();
    }
}
