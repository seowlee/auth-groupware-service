package pharos.groupware.service.domain.team.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "teams", schema = "groupware")
public class Team {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @NotNull
    @Column(name = "kr_name", nullable = false, length = 100)
    private String krName;

    @Column(name = "en_name", nullable = false, length = 100)
    private String enName;

    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    @Column(name = "created_by", length = 50)
    private String createdBy;

    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    @Column(name = "updated_by", length = 50)
    private String updatedBy;

    public Team(Long teamId) {
        this.id = teamId;
    }
}