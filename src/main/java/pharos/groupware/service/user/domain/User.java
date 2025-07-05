package pharos.groupware.service.user.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import pharos.groupware.service.auth.dto.CreateUserReqDTO;
import pharos.groupware.service.common.enums.KeycloakRoleEnum;
import pharos.groupware.service.common.enums.UserStatusEnum;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "users", schema = "groupware")
public class User implements UserDetails {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @NotNull
    @ColumnDefault("gen_random_uuid()")
    @Column(name = "user_uuid", nullable = false)
    private UUID userUUID;

    @Size(max = 100)
    @NotNull
    @Column(name = "username", nullable = false, length = 100)
    private String username;

    @Column(name = "password")
    private String password;

    @Size(max = 255)
    @NotNull
    @Column(name = "email", nullable = false)
    private String email;

    @Size(max = 50)
    @Column(name = "first_name", length = 50)
    private String firstName;

    @Size(max = 50)
    @Column(name = "last_name", length = 50)
    private String lastName;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    private KeycloakRoleEnum role;

    @ColumnDefault("'active")
    @Enumerated(EnumType.STRING)
    private UserStatusEnum status;

    @ColumnDefault("now()")
    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    @Size(max = 50)
    @ColumnDefault("'system'")
    @Column(name = "created_by", length = 50)
    private String createdBy;

    @ColumnDefault("now()")
    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    @Size(max = 50)
    @ColumnDefault("'system'")
    @Column(name = "updated_by", length = 50)
    private String updatedBy;

    public static User from(CreateUserReqDTO dto, PasswordEncoder encoder) {
        User user = new User();
        user.setUsername(dto.getUsername());
        user.setPassword(encoder.encode(dto.getRawPassword()));
        user.setEmail(dto.getEmail());
        user.setFirstName(dto.getFirstName());
        user.setLastName(dto.getLastName());
        user.setRole(dto.getRole());
        user.setStatus(UserStatusEnum.ACTIVE);
        user.setUserUUID(dto.getUserUUID() != null ? UUID.fromString(dto.getUserUUID()) : UUID.randomUUID());
        user.setCreatedAt(OffsetDateTime.now());
        user.setCreatedBy(dto.getUsername());
        user.setUpdatedAt(OffsetDateTime.now());
        user.setUpdatedBy(dto.getUsername());
        return user;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of();
    }

    @Override
    public boolean isAccountNonExpired() {
        return getStatus().equals(UserStatusEnum.ACTIVE);
    }

    @Override
    public boolean isAccountNonLocked() {
        return getStatus().equals(UserStatusEnum.ACTIVE);
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return getStatus().equals(UserStatusEnum.ACTIVE);
    }

    @Override
    public boolean isEnabled() {
        return getStatus().equals(UserStatusEnum.ACTIVE);
    }

}