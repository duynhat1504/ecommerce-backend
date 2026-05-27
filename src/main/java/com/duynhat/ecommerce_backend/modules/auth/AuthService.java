package com.duynhat.ecommerce_backend.modules.auth;

import com.duynhat.ecommerce_backend.modules.auth.dto.request.RegisterRequest;
import com.duynhat.ecommerce_backend.modules.auth.dto.response.RegisterResponse;

public interface AuthService {

    RegisterResponse register(RegisterRequest request);

}
