package jp.livlog.otp.web.servlet.filter;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jp.livlog.otp.web.servlet.OtpWebConfig;
import jp.livlog.otp.web.servlet.OtpWebSupport;

import java.io.IOException;
import java.util.Set;

public class MfaEnforcerFilter implements Filter {

    private final OtpWebConfig config;

    public MfaEnforcerFilter(OtpWebConfig config) {
        this.config = config;
    }

    @Override
    public void init(FilterConfig filterConfig) { }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse resp = (HttpServletResponse) response;

        String ctx = req.getContextPath();
        String uri = req.getRequestURI();
        String path = uri.startsWith(ctx) ? uri.substring(ctx.length()) : uri;

        // MFA関連URLは素通し
        if (path.equals(config.mfaPagePath())
                || path.equals(config.startPath())
                || path.equals(config.verifyPath())) {
            chain.doFilter(request, response);
            return;
        }

        // 保護対象でなければ素通し
        if (!isProtected(path, config.protectedPathPrefixes())) {
            chain.doFilter(request, response);
            return;
        }

        HttpSession session = req.getSession(false);

        // 未ログイン（パスワード認証前）なら素通し or アプリ側へ
        if (!OtpWebSupport.isPasswordOk(session)) {
            chain.doFilter(request, response);
            return;
        }

        // MFA未完了ならMFA画面へ
        if (!OtpWebSupport.isMfaOk(session)) {
            resp.sendRedirect(ctx + config.mfaPagePath());
            return;
        }

        chain.doFilter(request, response);
    }

    private boolean isProtected(String path, Set<String> prefixes) {
        for (String p : prefixes) {
            if (path.startsWith(p)) return true;
        }
        return false;
    }

    @Override
    public void destroy() { }
}
