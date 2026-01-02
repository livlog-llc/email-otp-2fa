package jp.livlog.otp.sample.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class MfaController {

    @GetMapping("/mfa")
    public String mfa() {
        return "mfa";
    }
}
