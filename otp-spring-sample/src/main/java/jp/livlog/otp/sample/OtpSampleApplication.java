package jp.livlog.otp.sample;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "jp.livlog.otp")
public class OtpSampleApplication {
    public static void main(String[] args) {
        SpringApplication.run(OtpSampleApplication.class, args);
    }
}
