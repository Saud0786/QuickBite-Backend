package com.quickbite.order.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.quickbite.order.exception.UnauthorizedActionException;

import java.security.Key;

@Service
public class JwtAuthService {

    @Value("${jwt.secret}")
    private String secret;

    public String extractUserIdFromAuthorizationHeader(String authorizationHeader) {
        Claims claims = parseClaims(authorizationHeader);
        String userId = claims.get("userId", String.class);
        if (userId == null || userId.isBlank()) {
            throw new UnauthorizedActionException("Token does not contain userId claim.");
        }
        return userId;
    }

    public String extractRoleFromAuthorizationHeader(String authorizationHeader) {
        Claims claims = parseClaims(authorizationHeader);
        String role = claims.get("role", String.class);
        if (role == null || role.isBlank()) {
            throw new UnauthorizedActionException("Token does not contain role claim.");
        }
        return role.toUpperCase();
    }

    public void assertOwnerRole(String authorizationHeader) {
        String role = extractRoleFromAuthorizationHeader(authorizationHeader);
        if (!"OWNER".equals(role) && !"ADMIN".equals(role)) {
            throw new UnauthorizedActionException("Only OWNER or ADMIN can perform this action.");
        }
    }

    private Claims parseClaims(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            throw new UnauthorizedActionException("Missing or invalid Authorization header.");
        }

        String token = authorizationHeader.substring(7);
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (Exception ex) {
            throw new UnauthorizedActionException("Invalid or expired token.");
        }
    }

    private Key getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes());
    }
}