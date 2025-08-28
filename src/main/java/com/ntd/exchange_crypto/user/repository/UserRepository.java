package com.ntd.exchange_crypto.user.repository;

import com.ntd.exchange_crypto.user.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, String> {
    boolean existsByEmail(String email);
    User findByEmail(String email);

    @Query("SELECT COUNT(u) FROM User u")
    Long countTotalUsers();
}
