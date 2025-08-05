package pharos.groupware.service.leave.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import pharos.groupware.service.common.enums.LeaveTypeEnum;
import pharos.groupware.service.common.util.AuthUtils;
import pharos.groupware.service.infrastructure.graph.GraphUserService;
import pharos.groupware.service.leave.domain.Leave;
import pharos.groupware.service.leave.domain.LeaveRepository;
import pharos.groupware.service.leave.dto.CreateLeaveReqDto;
import pharos.groupware.service.leave.dto.LeaveDetailResDto;
import pharos.groupware.service.team.domain.User;
import pharos.groupware.service.team.domain.UserRepository;

import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
@Service
public class LeaveServiceImpl implements LeaveService {
    private final GraphUserService graphUserService;
    private final LeaveRepository leaveRepository;
    private final UserRepository userRepository;

    @Override
    public void applyLeave(CreateLeaveReqDto dto) {
        User user = userRepository.findByUserUuid(UUID.fromString(dto.getUserUuid()))
                .orElseThrow(() -> new EntityNotFoundException("해당 사용자를 찾을 수 없습니다."));
        String currentUsername = AuthUtils.getCurrentUsername();
        dto.setUserName(user.getUsername());
        dto.setUserEmail(user.getEmail());
        LeaveTypeEnum leaveType = LeaveTypeEnum.valueOf(dto.getLeaveType());

        String subject = leaveType.getDescription() + " 신청 (" + dto.getUserName() + ")";
        String body = "연차 유형: " + leaveType.getDescription() + "\n"
                + "신청자: " + dto.getUserName() + " (" + dto.getUserEmail() + ")\n"
                + "사유: " + (dto.getReason() != null ? dto.getReason() : "없음");

        String eventId = graphUserService.createEvent(
                subject,
                body,
                dto.getStartTime(),
                dto.getEndTime()
        );

        Leave leave = Leave.create(dto, user, eventId, currentUsername);
        leaveRepository.save(leave);
    }

    @Override
    public LeaveDetailResDto getLeaveDetail(Long id) {
        Leave leave = leaveRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("해당 연차 정보가 존재하지 않습니다."));
        User user = userRepository.findById(leave.getUserId())
                .orElseThrow(() -> new EntityNotFoundException("해당 사용자 정보가 존재하지 않습니다."));
        return LeaveDetailResDto.fromEntity(leave, user);

    }
}
