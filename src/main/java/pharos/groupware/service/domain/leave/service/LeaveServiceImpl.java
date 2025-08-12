package pharos.groupware.service.domain.leave.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import pharos.groupware.service.common.enums.LeaveStatusEnum;
import pharos.groupware.service.common.enums.LeaveTypeEnum;
import pharos.groupware.service.common.util.AuthUtils;
import pharos.groupware.service.domain.leave.dto.CreateLeaveReqDto;
import pharos.groupware.service.domain.leave.dto.LeaveDetailResDto;
import pharos.groupware.service.domain.leave.dto.LeaveSearchReqDto;
import pharos.groupware.service.domain.leave.entity.Leave;
import pharos.groupware.service.domain.leave.entity.LeaveRepository;
import pharos.groupware.service.domain.team.entity.User;
import pharos.groupware.service.domain.team.entity.UserRepository;
import pharos.groupware.service.infrastructure.graph.GraphUserService;

import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
@Service
public class LeaveServiceImpl implements LeaveService {
    private final GraphUserService graphUserService;
    private final LeaveRepository leaveRepository;
    private final UserRepository userRepository;


    @Override
    public Page<LeaveDetailResDto> getAllLeaves(LeaveSearchReqDto searchDto, Pageable pageable) {
        // 정렬 조건 없으면 신청일 내림차순
        if (pageable.getSort().isUnsorted()) {
            Sort defaultSort = Sort.by(Sort.Order.desc("appliedAt"), Sort.Order.asc("id"));
            pageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), defaultSort);
        }
        LeaveTypeEnum typeEnum = null;
        String typeStr = searchDto.getType();
        if (StringUtils.hasText(typeStr)) {
            typeEnum = LeaveTypeEnum.valueOf(typeStr.toUpperCase());
        }

        LeaveStatusEnum statusEnum = null;
        String statusStr = searchDto.getStatus();
        if (StringUtils.hasText(statusStr)) {
            statusEnum = LeaveStatusEnum.valueOf(statusStr.toUpperCase());
        }


        Page<Leave> page = leaveRepository.findAllBySearchFilter(searchDto.getUserUuid(), searchDto.getKeyword(), searchDto.getTeamId(), typeEnum, statusEnum, pageable);

        return page.map(LeaveDetailResDto::fromEntity);
    }

    @Override
    public LeaveDetailResDto getLeaveDetail(Long id) {
        Leave leave = leaveRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("해당 연차 정보가 존재하지 않습니다."));
        User user = userRepository.findById(leave.getUserId())
                .orElseThrow(() -> new EntityNotFoundException("해당 사용자 정보가 존재하지 않습니다."));
        return LeaveDetailResDto.fromEntity(leave, user);

    }


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
                dto.getStartDt(),
                dto.getEndDt()
        );

        Leave leave = Leave.create(dto, user, eventId, currentUsername);
        leaveRepository.save(leave);
    }

    @Override
    public void updateLeave(Long id, CreateLeaveReqDto reqDto) {

    }

    @Override
    public void cancelLeave(Long id) {

    }
}
