package com.ntd.exchange_crypto.user.model;

import com.ntd.exchange_crypto.user.enums.Role;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.Set;

@Entity
@Table(name = "user")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Builder
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, length = 36)
    String id;

    @Column(nullable = false, unique = true)
    String email;

    @Column(nullable = false)
    String password;

    @Column(name = "full_name")
    String fullName;

    @Column(unique = true, length = 15)
    String phone;

    String avatar;

    @Builder.Default
    boolean active = true;

    @Column(name = "is_2FA_enabled")
    boolean tfaEnabled = false;

    @Column(nullable = false)
    Set<String> roles;

    String secret;

}
