package pharos.groupware.service.domain.team.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.ColumnDefault;
import org.springframework.security.crypto.password.PasswordEncoder;
import pharos.groupware.service.common.enums.UserRoleEnum;
import pharos.groupware.service.common.enums.UserStatusEnum;
import pharos.groupware.service.domain.admin.dto.CreateUserReqDto;
import pharos.groupware.service.domain.admin.dto.PendingUserDto;
import pharos.groupware.service.domain.admin.dto.UpdateUserByAdminReqDto;
import pharos.groupware.service.domain.team.dto.CreateIdpUserReqDto;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
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

    @Column(name = "kakaosub", length = 100)
    private String kakaoSub;

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

    public static User create(CreateUserReqDto reqDTO, Team team, String currentUsername, PasswordEncoder passwordEncoder) {
        User user = new User();
        user.userUuid = reqDTO.getUserUUID() != null ? UUID.fromString(reqDTO.getUserUUID()) : UUID.randomUUID();
        user.username = reqDTO.getUsername();
        user.email = reqDTO.getEmail();
        user.password = passwordEncoder.encode(reqDTO.getRawPassword());
        user.firstName = reqDTO.getFirstName();
        user.lastName = reqDTO.getLastName();
        user.joinedDate = reqDTO.getJoinedDate();
        user.yearNumber = reqDTO.getYearNumber();
        user.role = reqDTO.getRole();
        user.status = UserStatusEnum.ACTIVE;
        user.team = team;
        user.createdAt = OffsetDateTime.now();
        user.createdBy = currentUsername;
        user.updatedAt = OffsetDateTime.now();
        user.updatedBy = currentUsername;
        return user;
    }

    public static User fromPendingDto(
            PendingUserDto dto,
            Team defaultTeam,
            PasswordEncoder passwordEncoder
    ) {
        User u = new User();
        u.userUuid = UUID.randomUUID();
        u.username = dto.getEmail().substring(0, dto.getEmail().indexOf('@'));
        u.kakaoSub = dto.getProviderUserId();
        u.email = dto.getEmail();
        u.password = passwordEncoder.encode("1234");
        u.firstName = dto.getFirstName();
        u.lastName = dto.getLastName();
        u.joinedDate = LocalDate.now();
        u.yearNumber = 1;
        u.role = UserRoleEnum.TEAM_MEMBER;
        u.status = UserStatusEnum.PENDING;
        u.team = defaultTeam;
        u.createdAt = OffsetDateTime.now();
        u.createdBy = Optional.ofNullable(dto.getProvider()).orElse("system");
        u.updatedAt = u.createdAt;
        u.updatedBy = u.createdBy;
        return u;
    }

    public static User create(CreateIdpUserReqDto reqDTO, PasswordEncoder passwordEncoder) {
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

    public void updateByAdmin(UpdateUserByAdminReqDto reqDto, String currentUsername) {
        if (reqDto.getEmail() != null) this.email = reqDto.getEmail();
        if (reqDto.getFirstName() != null) this.firstName = reqDto.getFirstName();
        if (reqDto.getLastName() != null) this.lastName = reqDto.getLastName();
        if (reqDto.getRole() != null) this.role = UserRoleEnum.valueOf(reqDto.getRole());
        if (reqDto.getStatus() != null) this.status = UserStatusEnum.valueOf(reqDto.getStatus());
        if (reqDto.getTeamId() != null) this.team = new Team(reqDto.getTeamId());
        this.updatedAt = OffsetDateTime.now();
        this.updatedBy = currentUsername;
    }

    /**
     * PENDING → ACTIVE 승인 처리
     *
     * @param encodedPassword 새로 인코딩된 비밀번호
     * @param keycloakUserId  Keycloak 에 생성된 사용자 ID
     * @param approver        승인자(관리자) 이름
     */
    public void approve(String encodedPassword,
                        String keycloakUserId) {
        this.password = encodedPassword;
        this.userUuid = UUID.fromString(keycloakUserId);
        this.status = UserStatusEnum.ACTIVE;
        this.updatedAt = OffsetDateTime.now();
        this.updatedBy = "system";
    }
}