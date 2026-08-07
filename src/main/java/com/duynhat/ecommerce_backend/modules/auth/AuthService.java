package com.duynhat.ecommerce_backend.modules.auth;

import com.duynhat.ecommerce_backend.modules.auth.dto.internal.LoginResult;
import com.duynhat.ecommerce_backend.modules.auth.dto.internal.RefreshResult;
import com.duynhat.ecommerce_backend.modules.auth.dto.request.LoginRequest;
import com.duynhat.ecommerce_backend.modules.auth.dto.request.RegisterRequest;
import com.duynhat.ecommerce_backend.modules.auth.dto.response.RegisterResponse;

public interface AuthService {

    RegisterResponse register(RegisterRequest request);
    LoginResult login(LoginRequest request);
    RefreshResult refresh(String rawRefreshToken);
    void logout(String rawRefreshToken);
}
