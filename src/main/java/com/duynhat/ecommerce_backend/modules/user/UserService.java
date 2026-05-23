package com.duynhat.ecommerce_backend.modules.user;

import com.duynhat.ecommerce_backend.modules.user.entity.User;

public interface UserService {

    User findByEmail(String email);
    boolean existsByEmail(String email);

}
