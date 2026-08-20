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
                Welcome to Ecommerce!

                Please verify your email by clicking the link below:

                %s

                This link will expire in 30 minutes.

                If you did not create this account, you can ignore this email.
                """.formatted(
                verificationUrl
        ));

        mailSender.send(message);
    }
}
