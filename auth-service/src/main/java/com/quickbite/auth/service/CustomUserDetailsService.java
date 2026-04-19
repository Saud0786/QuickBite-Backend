package com.quickbite.auth.service;

import com.quickbite.auth.entity.User;
import com.quickbite.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {
    
    private final UserRepository userRepository;
    
    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));

        String passwordHash = user.getPasswordHash();
        if (passwordHash == null || passwordHash.isBlank()) {
            // OAuth accounts may not have a local password, but Spring Security requires a non-null value.
            passwordHash = "{noop}oauth-user";
        }
        
        return org.springframework.security.core.userdetails.User.builder()
            .username(user.getEmail())
            .password(passwordHash)
            .authorities(Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + user.getRole().name())))
            .accountLocked(!user.isActive())
            .disabled(!user.isActive())
            .build();
    }
}

// JwtService.java
