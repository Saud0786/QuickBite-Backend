package com.quickbite.auth.service.impl;

import com.quickbite.auth.dto.*;
import com.quickbite.auth.entity.User;
import com.quickbite.auth.exception.CustomExceptions;
import com.quickbite.auth.repository.UserRepository;
import com.quickbite.auth.service.AuthService;
import com.quickbite.auth.service.ImageUploadService;
import com.quickbite.auth.service.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class AuthServiceImpl implements AuthService {
    
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final RedisTemplate<String, String> redisTemplate;
    private final ImageUploadService imageUploadService;
    
    private static final String TOKEN_BLACKLIST_PREFIX = "blacklist:token:";
    
    @Override
    public UserDto register(RegisterRequest request) {
        log.info("Registering new user with email: {}", request.getEmail());
        
        // Check if user exists
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new CustomExceptions.UserAlreadyExistsException("User with email " + request.getEmail() + " already exists");
        }
        
        if (userRepository.existsByPhone(request.getPhone())) {
            throw new CustomExceptions.UserAlreadyExistsException("User with phone " + request.getPhone() + " already exists");
        }
        
        // Set default role if not provided
        User.Role role = request.getRole() != null ? request.getRole() : User.Role.CUSTOMER;
        
        // Create new user
        User user = User.builder()
            .fullName(request.getFullName())
            .email(request.getEmail())
            .passwordHash(passwordEncoder.encode(request.getPassword()))
            .phone(request.getPhone())
            .role(role)
            .provider(User.AuthProvider.LOCAL)
            .isActive(true)
            .isEmailVerified(false)
            .isPhoneVerified(true)
            .createdAt(LocalDateTime.now())
            .build();
        
        User savedUser = userRepository.save(user);
        log.info("User registered successfully with ID: {}", savedUser.getUserId());
        
        return UserDto.fromEntity(savedUser);
    }
    
    @Override
    public LoginResponse login(LoginRequest request) {
        log.info("Login attempt for user: {}", request.getEmail());
        
        // Authenticate
        Authentication authentication = authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );
        
        SecurityContextHolder.getContext().setAuthentication(authentication);
        
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        User user = userRepository.findByEmail(userDetails.getUsername())
            .orElseThrow(() -> new CustomExceptions.UserNotFoundException("User not found"));
        
        // Check if account is active
        if (!user.isActive()) {
            throw new CustomExceptions.AccountDeactivatedException("Account is deactivated. Please contact support.");
        }
        
        // Update last login
        userRepository.updateLastLogin(user.getUserId());
        
        // Generate tokens
        java.util.Map<String, Object> extraClaims = new java.util.HashMap<>();
        extraClaims.put("userId", user.getUserId());
        extraClaims.put("role", user.getRole().name());
        
        String accessToken = jwtService.generateToken(extraClaims, userDetails, jwtService.getExpirationTime());
        String refreshToken = jwtService.generateRefreshToken(userDetails);
        
        log.info("User logged in successfully: {}", user.getEmail());
        
        return LoginResponse.builder()
            .accessToken(accessToken)
            .refreshToken(refreshToken)
            .tokenType("Bearer")
            .expiresIn(jwtService.getExpirationTime())
            .user(UserDto.fromEntity(user))
            .build();
    }
    
    @Override
    public void logout(String token, String userId) {
        log.info("Logout for user ID: {}", userId);
        
        // Add token to blacklist
        long expiration = jwtService.getExpirationTime();
        redisTemplate.opsForValue().set(
            TOKEN_BLACKLIST_PREFIX + token,
            "logout",
            Duration.ofMillis(expiration)
        );
        
        log.info("User logged out successfully: {}", userId);
    }
    
    @Override
    public LoginResponse refreshToken(String refreshToken) {
        log.info("Refreshing token");
        
        String userEmail = jwtService.extractUsername(refreshToken);
        
        if (userEmail == null) {
            throw new CustomExceptions.InvalidTokenException("Invalid refresh token");
        }
        
        User user = userRepository.findByEmail(userEmail)
            .orElseThrow(() -> new CustomExceptions.UserNotFoundException("User not found"));
        
        if (!user.isActive()) {
            throw new CustomExceptions.AccountDeactivatedException("Account is deactivated");
        }
        
        UserDetails userDetails = org.springframework.security.core.userdetails.User.builder()
            .username(user.getEmail())
            .password(user.getPasswordHash())
            .authorities("ROLE_" + user.getRole().name())
            .build();
        
        if (!jwtService.isTokenValid(refreshToken, userDetails)) {
            throw new CustomExceptions.InvalidTokenException("Invalid or expired refresh token");
        }
        
        java.util.Map<String, Object> extraClaims = new java.util.HashMap<>();
        extraClaims.put("userId", user.getUserId());
        extraClaims.put("role", user.getRole().name());
        
        String newAccessToken = jwtService.generateToken(extraClaims, userDetails, jwtService.getExpirationTime());
        String newRefreshToken = jwtService.generateRefreshToken(userDetails);
        
        return LoginResponse.builder()
            .accessToken(newAccessToken)
            .refreshToken(newRefreshToken)
            .tokenType("Bearer")
            .expiresIn(jwtService.getExpirationTime())
            .user(UserDto.fromEntity(user))
            .build();
    }
    
    @Override
    public UserDto getUserByEmail(String email) {
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new CustomExceptions.UserNotFoundException("User not found with email: " + email));
        return UserDto.fromEntity(user);
    }
    
    @Override
    public UserDto getUserById(String userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new CustomExceptions.UserNotFoundException("User not found with ID: " + userId));
        return UserDto.fromEntity(user);
    }
    
    @Override
    @Transactional
    public UserDto updateProfile(String userId, ProfileUpdateRequest request) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new CustomExceptions.UserNotFoundException("User not found"));
        
        if (request.getFullName() != null && !request.getFullName().isEmpty()) {
            user.setFullName(request.getFullName());
        }
        
        if (request.getEmail() != null && !request.getEmail().equals(user.getEmail())) {
            if (userRepository.existsByEmail(request.getEmail())) {
                throw new CustomExceptions.UserAlreadyExistsException("Email already in use");
            }
            user.setEmail(request.getEmail());
            user.setEmailVerified(false);
        }
        
        if (request.getPhone() != null && !request.getPhone().equals(user.getPhone())) {
            if (userRepository.existsByPhone(request.getPhone())) {
                throw new CustomExceptions.UserAlreadyExistsException("Phone number already in use");
            }
            user.setPhone(request.getPhone());
        }
        
        if (request.getProfilePicUrl() != null) {
            user.setProfilePicUrl(request.getProfilePicUrl());
        }

        if (request.getProfileImage() != null && !request.getProfileImage().isEmpty()) {
            String uploadedUrl = imageUploadService.uploadImage(request.getProfileImage());
            if (uploadedUrl != null) {
                user.setProfilePicUrl(uploadedUrl);
            }
        }
        
        User updatedUser = userRepository.save(user);
        log.info("Profile updated for user: {}", userId);
        
        return UserDto.fromEntity(updatedUser);
    }
    
    @Override
    @Transactional
    public void changePassword(String userId, ChangePasswordRequest request) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new CustomExceptions.UserNotFoundException("User not found"));
        
        // Verify current password
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
            throw new CustomExceptions.InvalidCredentialsException("Current password is incorrect");
        }
        
        // Update password
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
        
        log.info("Password changed for user: {}", userId);
    }
    
    @Override
    @Transactional
    public void deactivateAccount(String userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new CustomExceptions.UserNotFoundException("User not found"));
        
        userRepository.delete(user);
        
        log.info("Account permanently deleted for user: {}", userId);
    }
    
    @Override
    @Transactional
    public void activateAccount(String userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new CustomExceptions.UserNotFoundException("User not found"));
        
        user.setActive(true);
        userRepository.save(user);
        
        log.info("Account activated for user: {}", userId);
    }
    
    @Override
    @Transactional
    public UserDto processOAuthLogin(String email, String name, String provider, String providerId) {
        log.info("Processing OAuth login for email: {} with provider: {}", email, provider);
        
        User.AuthProvider authProvider = User.AuthProvider.valueOf(provider.toUpperCase());
        
        // Try to find user by provider and providerId
        User user = userRepository.findByProviderAndProviderId(authProvider, providerId)
            .orElse(null);
        
        // If not found, try by email
        if (user == null) {
            user = userRepository.findByEmail(email).orElse(null);
        }
        
        // If still not found, create new user
        if (user == null) {
            user = User.builder()
                .fullName(name)
                .email(email)
                .phone("oauth_" + providerId) // Fix unique phone constraint violation
                .role(User.Role.CUSTOMER)
                .provider(authProvider)
                .providerId(providerId)
                .isActive(true)
                .isEmailVerified(true)
                .isPhoneVerified(false)
                .build();
            
            user = userRepository.save(user);
            log.info("New OAuth user created: {}", email);
        } else {
            // Update provider info if needed
            if (user.getProvider() == User.AuthProvider.LOCAL) {
                user.setProvider(authProvider);
                user.setProviderId(providerId);
                user = userRepository.save(user);
            }
            
            userRepository.updateLastLogin(user.getUserId());
            log.info("Existing OAuth user logged in: {}", email);
        }
        
        return UserDto.fromEntity(user);
    }
}

// CustomUserDetailsService.java
