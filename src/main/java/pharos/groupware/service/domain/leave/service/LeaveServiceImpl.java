package pharos.groupware.service.domain.leave.service;

import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientResponseException;
import pharos.groupware.service.common.enums.LeaveStatusEnum;
import pharos.groupware.service.common.enums.LeaveTypeEnum;
import pharos.groupware.service.common.util.AuthUtils;
import pharos.groupware.service.common.util.DateUtils;
import pharos.groupware.service.domain.calendar.entity.PublicHoliday;
import pharos.groupware.service.domain.calendar.entity.PublicHolidayRepository;
import pharos.groupware.service.domain.leave.dto.*;
import pharos.groupware.service.domain.leave.entity.Leave;
import pharos.groupware.service.domain.leave.entity.LeaveRepository;
import pharos.groupware.service.domain.team.entity.User;
import pharos.groupware.service.domain.team.entity.UserRepository;
import pharos.groupware.service.domain.team.service.UserService;
import pharos.groupware.service.infrastructure.graph.GraphUserService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
@Service
public class LeaveServiceImpl implements LeaveService {
    private final GraphUserService graphUserService;
    private final UserService userService;
    private final LeaveBalanceService leaveBalanceService;
    private final LeaveRepository leaveRepository;
    private final PublicHolidayRepository publicHolidayRepository;
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
    @Transactional
    public Long applyLeave(CreateLeaveReqDto dto) {
        User user = userRepository.findByUserUuid(UUID.fromString(dto.getUserUuid()))
                .orElseThrow(() -> new EntityNotFoundException("해당 사용자를 찾을 수 없습니다."));

        Set<LocalDate> holidays = new HashSet<>(publicHolidayRepository.findAllByYear(2025)
                .stream().map(PublicHoliday::getHolidayDate).toList());

        String currentUsername = AuthUtils.getCurrentUsername();
        dto.setUserName(user.getUsername());
        dto.setUserEmail(user.getEmail());
        LeaveTypeEnum leaveType = LeaveTypeEnum.valueOf(dto.getLeaveType());
        BigDecimal usedDays = DateUtils.countLeaveDays(dto.getStartDt(), dto.getEndDt(), holidays);
        dto.setUsedDays(usedDays);

        String subject = leaveType.getDescription() + " 신청 (" + dto.getUserName() + ")";
        String body = "연차 유형: " + leaveType.getDescription() + "\n"
                + "사용 일: " + dto.getUsedDays() + "\n"
                + "신청자: " + dto.getUserName() + " (" + dto.getUserEmail() + ")\n"
                + "사유: " + (dto.getReason() != null ? dto.getReason() : "없음");

        String eventId = graphUserService.createEvent(
                subject,
                body,
                dto.getStartDt(),
                dto.getEndDt()
        );

        Leave leave = Leave.create(dto, user, eventId, currentUsername);
        Leave savedLeave = leaveRepository.save(leave);

        ApplyLeaveUsageReqDto useReq = new ApplyLeaveUsageReqDto();
        useReq.setUserId(user.getId());
        useReq.setLeaveType(leaveType);
        useReq.setYearNumber(user.getYearNumber());
        useReq.setUsedDays(usedDays);

        leaveBalanceService.applyUsage(useReq);
        return savedLeave.getId();
    }

    @Override
    @Transactional
    public Long updateLeave(Long id, UpdateLeaveReqDto reqDto) {
        Leave leave = leaveRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("연차를 찾을 수 없습니다."));
        User applicantUser = leave.getUser();
        User currentUser = userService.getCurrentUser();

        // 권한 체크
        if (!currentUser.getRole().isSuperAdmin()) {
            if (!applicantUser.getUserUuid().equals(currentUser.getUserUuid())) {
                throw new AccessDeniedException("수정 권한이 없습니다.");
            }
        }
        // 비즈니스 룰 검증 (기간/슬롯/영업일 등)
        leave.validateUpdatable(reqDto.getStartDt(), reqDto.getEndDt(), reqDto.getLeaveType());
        // 3) (선택) 영업일/공휴일 검증 등은 BusinessCalendarService로 사전에 계산/검증
        // businessCalendarService.assertBusinessRange(dto.getStartDt(), dto.getEndDt());
        try {

            String subject = LeaveTypeEnum.valueOf(reqDto.getLeaveType()).getDescription() + " 신청 (" + applicantUser.getUsername() + ")";
            String body = "연차 유형: " + LeaveTypeEnum.valueOf(reqDto.getLeaveType()).getDescription() + "\n"
                    + "신청자: " + applicantUser.getUsername() + " (" + applicantUser.getEmail() + ")\n"
                    + "사유: " + (reqDto.getReason() != null ? reqDto.getReason() : "없음");

            // 캘린더 이벤트도 update
            graphUserService.updateEvent(
                    leave.getCalendarEventId(),
                    subject,
                    body,
                    reqDto.getStartDt(),
                    reqDto.getEndDt()
            );
            String currentUsername = AuthUtils.getCurrentUsername();//TODO: currentUser.getUsername()?

            // 도메인 업데이트
            leave.updateFrom(reqDto, currentUsername);
            log.info("Updating leave with id {}", id);
            return leave.getId();
        } catch (RestClientResponseException e) {
            log.error("Failed to update Leave id={}, Graph event id={}, status={}, body={}",
                    leave.getId(), leave.getCalendarEventId(), e.getStatusCode(), e.getResponseBodyAsString(), e);
            throw e;
        }
    }

    @Override
    public void cancelLeave(Long id) {
        Leave leave = leaveRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("연차를 찾을 수 없습니다."));
        User applicantUser = leave.getUser();
        User currentUser = userService.getCurrentUser();
        // 권한 체크
        if (!currentUser.getRole().isSuperAdmin()) {
            if (!applicantUser.getUserUuid().equals(currentUser.getUserUuid())) {
                throw new AccessDeniedException("수정 권한이 없습니다.");
            }
        }
        //TODO:offsetDatetime localdatetime
        // 비즈니스 룰 검증 (기간/슬롯/영업일 등)
//        leave.validateUpdatable(leave.getStartDt()., leave.getEndDt(), leave.getLeaveType());
        graphUserService.deleteEvent(leave.getCalendarEventId());

    }
}
