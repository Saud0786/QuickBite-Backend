// AuthService.java
package com.quickbite.auth.service;

import com.quickbite.auth.dto.*;

public interface AuthService {
    UserDto register(RegisterRequest request);
    LoginResponse login(LoginRequest request);
    void logout(String token, String userId);
    LoginResponse refreshToken(String refreshToken);
    UserDto getUserByEmail(String email);
    UserDto getUserById(String userId);
    UserDto updateProfile(String userId, ProfileUpdateRequest request);
    void changePassword(String userId, ChangePasswordRequest request);
    void deactivateAccount(String userId);
    void activateAccount(String userId);
    UserDto processOAuthLogin(String email, String name, String provider, String providerId);
}

// AuthServiceImpl.java
