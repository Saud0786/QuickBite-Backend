package com.quickbite.auth.controller;

import com.quickbite.auth.dto.*;
import com.quickbite.auth.service.AuthService;
import com.quickbite.auth.service.JwtService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Authentication", description = "Authentication and User Management APIs")
public class AuthController {
    
    private final AuthService authService;
    private final JwtService jwtService;
    
    @PostMapping("/register")
    @Operation(summary = "Register a new user")
    public ResponseEntity<ApiResponse<UserDto>> register(@Valid @RequestBody RegisterRequest request) {
        UserDto user = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success(user, "User registered successfully"));
    }
    
    @PostMapping("/login")
    @Operation(summary = "Login user")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success(response, "Login successful"));
    }
    
    @PostMapping("/logout")
    @Operation(summary = "Logout user")
    public ResponseEntity<ApiResponse<Void>> logout(HttpServletRequest request, 
                                                     @AuthenticationPrincipal UserDetails userDetails) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            String userId = authService.getUserByEmail(userDetails.getUsername()).getUserId();
            authService.logout(token, userId);
        }
        return ResponseEntity.ok(ApiResponse.success(null, "Logout successful"));
    }
    
    @PostMapping("/refresh")
    @Operation(summary = "Refresh access token")
    public ResponseEntity<ApiResponse<LoginResponse>> refreshToken(@RequestHeader("Authorization") String refreshToken) {
        String token = refreshToken.substring(7);
        LoginResponse response = authService.refreshToken(token);
        return ResponseEntity.ok(ApiResponse.success(response, "Token refreshed successfully"));
    }
    
    @GetMapping("/me")
    @Operation(summary = "Get current user profile")
    public ResponseEntity<ApiResponse<UserDto>> getCurrentUser(@AuthenticationPrincipal UserDetails userDetails) {
        UserDto user = authService.getUserByEmail(userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success(user, "User profile retrieved"));
    }
    
    @GetMapping("/user/{userId}")
    @Operation(summary = "Get user by ID")
    public ResponseEntity<ApiResponse<UserDto>> getUserById(@PathVariable String userId) {
        UserDto user = authService.getUserById(userId);
        return ResponseEntity.ok(ApiResponse.success(user, "User found"));
    }
    
    @GetMapping("/user/email/{email}")
    @Operation(summary = "Get user by email")
    public ResponseEntity<ApiResponse<UserDto>> getUserByEmail(@PathVariable String email) {
        UserDto user = authService.getUserByEmail(email);
        return ResponseEntity.ok(ApiResponse.success(user, "User found"));
    }
    
    @PutMapping("/profile")
    @Operation(summary = "Update user profile")
    public ResponseEntity<ApiResponse<UserDto>> updateProfile(@AuthenticationPrincipal UserDetails userDetails,
                                                               @Valid @RequestBody ProfileUpdateRequest request) {
        String userId = authService.getUserByEmail(userDetails.getUsername()).getUserId();
        UserDto user = authService.updateProfile(userId, request);
        return ResponseEntity.ok(ApiResponse.success(user, "Profile updated successfully"));
    }
    
    @PostMapping("/change-password")
    @Operation(summary = "Change password")
    public ResponseEntity<ApiResponse<Void>> changePassword(@AuthenticationPrincipal UserDetails userDetails,
                                                             @Valid @RequestBody ChangePasswordRequest request) {
        String userId = authService.getUserByEmail(userDetails.getUsername()).getUserId();
        authService.changePassword(userId, request);
        return ResponseEntity.ok(ApiResponse.success(null, "Password changed successfully"));
    }
    
    @DeleteMapping("/deactivate")
    @Operation(summary = "Deactivate account")
    public ResponseEntity<ApiResponse<Void>> deactivateAccount(@AuthenticationPrincipal UserDetails userDetails) {
        String userId = authService.getUserByEmail(userDetails.getUsername()).getUserId();
        authService.deactivateAccount(userId);
        return ResponseEntity.ok(ApiResponse.success(null, "Account deactivated successfully"));
    }
    
    @PostMapping("/oauth/{provider}")
    @Operation(summary = "OAuth login handler")
    public ResponseEntity<ApiResponse<LoginResponse>> oauthLogin(@PathVariable String provider,
                                                                  @RequestParam String email,
                                                                  @RequestParam String name,
                                                                  @RequestParam String providerId) {
        UserDto user = authService.processOAuthLogin(email, name, provider, providerId);
        
        // Generate tokens for OAuth user
        UserDetails userDetails = org.springframework.security.core.userdetails.User.builder()
            .username(user.getEmail())
            .password("")
            .authorities("ROLE_" + user.getRole())
            .build();
        
        java.util.Map<String, Object> extraClaims = new java.util.HashMap<>();
        extraClaims.put("userId", user.getUserId());
        extraClaims.put("role", user.getRole());
        
        String accessToken = jwtService.generateToken(extraClaims, userDetails, jwtService.getExpirationTime());
        String refreshToken = jwtService.generateRefreshToken(userDetails);
        
        LoginResponse response = LoginResponse.builder()
            .accessToken(accessToken)
            .refreshToken(refreshToken)
            .tokenType("Bearer")
            .expiresIn(jwtService.getExpirationTime())
            .user(user)
            .build();
        
        return ResponseEntity.ok(ApiResponse.success(response, "OAuth login successful"));
    }
    
    @GetMapping("/validate")
    @Operation(summary = "Validate token")
    public ResponseEntity<ApiResponse<Boolean>> validateToken(@RequestParam String token) {
        String username = jwtService.extractUsername(token);
        if (username != null) {
            UserDetails userDetails = org.springframework.security.core.userdetails.User.builder()
                .username(username)
                .password("")
                .authorities("ROLE_USER")
                .build();
            boolean isValid = jwtService.isTokenValid(token, userDetails);
            return ResponseEntity.ok(ApiResponse.success(isValid, isValid ? "Token is valid" : "Token is invalid"));
        }
        return ResponseEntity.ok(ApiResponse.success(false, "Token is invalid"));
    }
}