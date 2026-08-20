package com.duynhat.ecommerce_backend.modules.auth.email;

public interface EmailService {

    void sendVerificationEmail(String email, String rawToken);
}
