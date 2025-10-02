package pharos.groupware.service.domain.team.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import pharos.groupware.service.domain.team.entity.User;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserApplicantResDto {
    private Long id;
    private String userUuid;
    private String username;
    private String email;

    public static UserApplicantResDto toApplicantDto(User u) {
        return UserApplicantResDto.builder()
                .id(u.getId())
                .userUuid(u.getUserUuid().toString())
                .username(u.getUsername())
                .email(u.getEmail())
                .build();
    }
}
