package jp.livlog.otp.servletsample;

import java.io.IOException;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.annotation.WebServlet;

import jp.livlog.otp.web.servlet.OtpWebSupport;

@WebServlet(name = "loginServlet", urlPatterns = "/login")
public class LoginServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("text/html;charset=UTF-8");
        resp.getWriter().write(PageTemplates.login(req.getContextPath(), req.getParameter("error") != null));
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String userId = req.getParameter("userId");
        String password = req.getParameter("password");
        HttpSession session = req.getSession();

        // サンプル用の固定ID/パスワード
        if ("user".equals(userId) && "password".equals(password)) {
            OtpWebSupport.markPasswordOk(session, userId, "user@example.com");
            resp.sendRedirect(req.getContextPath() + "/mfa");
            return;
        }
        resp.sendRedirect(req.getContextPath() + "/login?error=1");
    }
}
