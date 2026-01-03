package jp.livlog.otp.sample.web;

import jakarta.servlet.http.HttpSession;
import jp.livlog.otp.web.spring.OtpWebSupport;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class MfaController {

    @GetMapping("/mfa")
    public String mfa(
            HttpSession session,
            @RequestParam(value = "error", required = false) String error,
            Model model
    ) {
        if (!OtpWebSupport.isPasswordOk(session)) {
            return "redirect:/login";
        }
        model.addAttribute("error", error != null);
        return "mfa";
    }
}
