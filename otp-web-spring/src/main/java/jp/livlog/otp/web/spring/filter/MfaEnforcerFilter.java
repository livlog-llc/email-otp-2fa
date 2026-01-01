package jp.livlog.otp.web.spring.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jp.livlog.otp.web.spring.OtpWebProperties;
import jp.livlog.otp.web.spring.OtpWebSupport;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

public class MfaEnforcerFilter extends OncePerRequestFilter {

    private final OtpWebProperties props;

    public MfaEnforcerFilter(OtpWebProperties props) {
        this.props = props;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse resp, FilterChain chain)
            throws ServletException, IOException {

        String ctx = req.getContextPath() == null ? "" : req.getContextPath();
        String uri = req.getRequestURI();
        String path = uri.startsWith(ctx) ? uri.substring(ctx.length()) : uri;

        // MFA関連URLは通す
        if (path.equals(props.getMfaPagePath())
                || path.equals(props.getStartPath())
                || path.equals(props.getVerifyPath())) {
            chain.doFilter(req, resp);
            return;
        }

        // 保護対象でなければ通す
        if (!isProtected(path, props.getProtectedPathPrefixes())) {
            chain.doFilter(req, resp);
            return;
        }

        HttpSession session = req.getSession(false);

        // 未ログインなら拒否（アプリ側でのログインを要求）
        if (!OtpWebSupport.isPasswordOk(session)) {
            resp.sendError(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        // MFA未完了ならMFAページへ
        if (!OtpWebSupport.isMfaOk(session)) {
            resp.sendRedirect(ctx + props.getMfaPagePath());
            return;
        }

        chain.doFilter(req, resp);
    }

    private boolean isProtected(String path, List<String> prefixes) {
        for (String p : prefixes) {
            if (path.startsWith(p)) return true;
        }
        return false;
    }
}
