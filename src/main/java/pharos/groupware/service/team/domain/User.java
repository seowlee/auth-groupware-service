package pharos.groupware.service.team.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import org.hibernate.annotations.ColumnDefault;
import org.springframework.security.crypto.password.PasswordEncoder;
import pharos.groupware.service.admin.dto.CreateUserReqDto;
import pharos.groupware.service.common.enums.UserRoleEnum;
import pharos.groupware.service.common.enums.UserStatusEnum;
import pharos.groupware.service.team.dto.CreateIdpUserReqDto;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Entity
@Table(name = "users", schema = "groupware")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @NotNull
    @ColumnDefault("gen_random_uuid()")
    @Column(name = "user_uuid", nullable = false)
    private UUID userUuid;

//    @Size(max = 100)
//    @Column(name = "graph_user_id", length = 100)
//    private String graphUserId;

    @NotNull
    @Column(name = "username", nullable = false, length = 100)
    private String username;

    @NotNull
    @Column(name = "email", nullable = false)
    private String email;

    @Column(name = "password", length = Integer.MAX_VALUE)
    private String password;

    @Column(name = "first_name", length = 50)
    private String firstName;

    @Column(name = "last_name", length = 50)
    private String lastName;

    @NotNull
    @ColumnDefault("CURRENT_DATE")
    @Column(name = "joined_date", nullable = false)
    private LocalDate joinedDate;

    @NotNull
    @ColumnDefault("1")
    @Column(name = "year_number", nullable = false)
    private Integer yearNumber;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    private UserRoleEnum role;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private UserStatusEnum status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id")
    private Team team;

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

    @ColumnDefault("'system'")
    @Column(name = "updated_by", length = 50)
    private String updatedBy;

    public static User create(CreateUserReqDto reqDTO, Team team, PasswordEncoder passwordEncoder) {
        User user = new User();
        user.userUuid = reqDTO.getUserUUID() != null ? UUID.fromString(reqDTO.getUserUUID()) : UUID.randomUUID();
        user.username = reqDTO.getUsername();
        user.email = reqDTO.getEmail();
        user.password = passwordEncoder.encode(reqDTO.getRawPassword());
        user.firstName = reqDTO.getFirstName();
        user.lastName = reqDTO.getLastName();
        user.joinedDate = reqDTO.getJoinedDate();
        user.yearNumber = 1;
        user.role = reqDTO.getRole();
        user.status = UserStatusEnum.ACTIVE;
        user.team = team;
        user.createdAt = OffsetDateTime.now();
        user.createdBy = reqDTO.getUsername();
        user.updatedAt = OffsetDateTime.now();
        user.updatedBy = reqDTO.getUsername();
        return user;
    }

    public static User create(CreateIdpUserReqDto reqDTO, PasswordEncoder passwordEncoder) {
        User user = new User();
        user.userUuid = UUID.randomUUID();  // IDP 사용자는 UUID 고정
        user.username = reqDTO.getUsername();
        user.email = reqDTO.getEmail();
        user.password = passwordEncoder.encode(reqDTO.getRawPassword());
        user.firstName = reqDTO.getFirstName();
        user.lastName = reqDTO.getLastName();
        user.joinedDate = reqDTO.getJoinedDate();
        user.yearNumber = 1;
        user.role = reqDTO.getRole();
        user.status = reqDTO.getStatus(); // PENDING
        user.createdAt = OffsetDateTime.now();
        user.createdBy = reqDTO.getUsername(); // 보통 nickname 사용
        user.updatedAt = OffsetDateTime.now();
        user.updatedBy = reqDTO.getUsername();
        return user;
    }

    public void deactivate() {
        this.status = UserStatusEnum.INACTIVE;
        this.updatedAt = OffsetDateTime.now();
    }

}