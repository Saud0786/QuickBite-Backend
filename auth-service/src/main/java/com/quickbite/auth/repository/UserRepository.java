package com.quickbite.auth.repository;

import com.quickbite.auth.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, String> {
    Optional<User> findByEmail(String email);
    Optional<User> findByUserId(String userId);
    boolean existsByEmail(String email);
    boolean existsByPhone(String phone);
    List<User> findAllByRole(User.Role role);
    Optional<User> findByPhone(String phone);
    List<User> findByFullNameContaining(String name);
    void deleteByUserId(String userId);
    Optional<User> findByProviderAndProviderId(User.AuthProvider provider, String providerId);
    
    @Modifying
    @Query("UPDATE User u SET u.lastLoginAt = CURRENT_TIMESTAMP WHERE u.userId = :userId")
    void updateLastLogin(@Param("userId") String userId);
}