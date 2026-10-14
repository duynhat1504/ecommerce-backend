package com.duynhat.ecommerce_backend.modules.auth.email;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

@Service
public class GmailEmailService implements EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.backend-url}")
    private String backendUrl;

    @Value("${spring.mail.username}")
    private String fromEmail;

    public GmailEmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @Override
    public void sendVerificationEmail(String email, String rawToken) {

        String verificationUrl = UriComponentsBuilder
                .fromUriString(backendUrl)
                .path("/api/auth/verify-email")
                .queryParam("token", rawToken)
                .build()
                .toUriString();

        SimpleMailMessage message = new SimpleMailMessage();

        message.setFrom(fromEmail);
        message.setTo(email);
        message.setSubject("Verify your email");

        message.setText("""
                Chào bạn,
                
                Cảm ơn bạn đã đăng ký tài khoản tại CHẬM.
                
                Vui lòng xác thực email bằng cách nhấn vào liên kết dưới đây:
                
                        %s
                
                Liên kết sẽ hết hạn sau 10 phút.
                
                Nếu bạn không tạo tài khoản này, bạn có thể bỏ qua email.
                """.formatted(
                verificationUrl
        ));

        mailSender.send(message);
    }
}
