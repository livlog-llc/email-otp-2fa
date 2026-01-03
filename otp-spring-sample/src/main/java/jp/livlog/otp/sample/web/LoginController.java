package jp.livlog.otp.sample.web;

import jakarta.servlet.http.HttpSession;
import jp.livlog.otp.web.spring.OtpWebSupport;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
public class LoginController {

    @GetMapping("/login")
    public String loginForm(@RequestParam(value = "error", required = false) String error, Model model) {
        model.addAttribute("error", error != null);
        return "login";
    }

    @PostMapping("/login")
    public String login(
            @RequestParam("userId") String userId,
            @RequestParam("password") String password,
            HttpSession session
    ) {
        // ★ サンプル用：固定認証
        if ("user".equals(userId) && "password".equals(password)) {
            OtpWebSupport.markPasswordOk(session, userId, "user@example.com");
            return "redirect:/mfa";
        }
        return "redirect:/login?error";
    }
}
