package pharos.groupware.service.domain.leave.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientResponseException;
import pharos.groupware.service.common.enums.AuditActionEnum;
import pharos.groupware.service.common.enums.AuditStatusEnum;
import pharos.groupware.service.common.enums.LeaveStatusEnum;
import pharos.groupware.service.common.enums.LeaveTypeEnum;
import pharos.groupware.service.common.security.AppUser;
import pharos.groupware.service.domain.audit.service.AuditLogService;
import pharos.groupware.service.domain.holiday.entity.PublicHolidayRepository;
import pharos.groupware.service.domain.holiday.service.WorkDayService;
import pharos.groupware.service.domain.leave.dto.*;
import pharos.groupware.service.domain.leave.entity.Leave;
import pharos.groupware.service.domain.leave.entity.LeaveRepository;
import pharos.groupware.service.domain.team.entity.User;
import pharos.groupware.service.domain.team.entity.UserRepository;
import pharos.groupware.service.domain.team.service.UserService;
import pharos.groupware.service.infrastructure.graph.GraphGroupService;
import pharos.groupware.service.infrastructure.graph.GraphUserService;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static pharos.groupware.service.common.util.AuditLogUtils.details;
import static pharos.groupware.service.common.util.LeaveUtils.computeTenureWindow;

@Slf4j
@RequiredArgsConstructor
@Service
public class LeaveServiceImpl implements LeaveService {
    private static final ZoneOffset KST = ZoneOffset.ofHours(9);
    private final GraphUserService graphUserService;
    private final GraphGroupService graphGroupService;
    private final UserService userService;
    private final LeaveBalanceService leaveBalanceService;
    private final WorkDayService workDayService;
    private final AuditLogService auditLogService;
    private final LeaveRepository leaveRepository;
    private final PublicHolidayRepository publicHolidayRepository;
    private final UserRepository userRepository;

    @Override
    public Page<LeaveDetailResDto> getAllLeaves(LeaveSearchReqDto searchDto, Pageable pageable, AppUser actor) {
        // 정렬 조건 없으면 신청일 내림차순
        if (pageable.getSort().isUnsorted()) {
            Sort defaultSort = Sort.by(Sort.Order.desc("appliedAt"), Sort.Order.asc("id"));
            pageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), defaultSort);
        }
        LeaveTypeEnum typeEnum = StringUtils.hasText(searchDto.getType())
                ? LeaveTypeEnum.valueOf(searchDto.getType().toUpperCase()) : null;
        LeaveStatusEnum statusEnum = StringUtils.hasText(searchDto.getStatus())
                ? LeaveStatusEnum.valueOf(searchDto.getStatus().toUpperCase()) : null;

        User me = userRepository.findByUserUuid(actor.userUuid())
                .orElseThrow(() -> new EntityNotFoundException("현재 사용자를 찾을 수 없습니다."));

        Long userIdFilter = null;
        boolean superAdmin = actor.role() != null && actor.role().isSuperAdmin();
        boolean teamLeader = actor.role() != null && actor.role().isTeamLeader();

        if (Boolean.TRUE.equals(searchDto.getMyOnly())) {
            userIdFilter = me.getId();
            Integer k = searchDto.getTenureYear();
            if (k != null && k > 0 && me.getJoinedDate() != null) {
                var win = computeTenureWindow(me.getJoinedDate(), k);
                searchDto.setFrom(win.from());
                searchDto.setTo(win.to());
            }
        } else {
            if (superAdmin) {
                // 전체 허용. 선택된 신청자가 있으면 그 사용자만.
                if (searchDto.getUserId() != null) userIdFilter = searchDto.getUserId();
            } else if (teamLeader) {
                // 팀장: 팀 강제 고정
                Long myTeamId = me.getTeam() != null ? me.getTeam().getId() : null;
                searchDto.setTeamId(myTeamId);
                // 신청자 선택이 있으면 팀 내 사용자만 허용(아닐 경우 무시)
                if (searchDto.getUserId() != null) {
                    // 팀 검증 (간단 검증)
                    userRepository.findById(searchDto.getUserId()).ifPresent(u -> {
                        if (u.getTeam() != null && u.getTeam().getId().equals(myTeamId)) {
                            // OK
                        } else {
                            // 팀 외 사용자는 무시
                            searchDto.setUserId(null);
                        }
                    });
                }
                if (searchDto.getUserId() != null) userIdFilter = searchDto.getUserId();
            } else {
                // 일반 멤버가 우회로 myOnly=false를 보내도 내 것만으로 강제
                userIdFilter = me.getId();
                searchDto.setMyOnly(true);
            }
        }
        boolean hasKeyword = StringUtils.hasText(searchDto.getKeyword());
        boolean hasFrom = searchDto.getFrom() != null;
        boolean hasTo = searchDto.getTo() != null;
        Page<Leave> page = leaveRepository.findAllBySearchFilter(
                searchDto.getKeyword(),
                searchDto.getTeamId(),
                typeEnum,
                statusEnum,
                userIdFilter,
                hasFrom, searchDto.getFrom(),
                hasTo, searchDto.getTo(),
                pageable);

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
    public Long applyLeave(CreateLeaveReqDto dto, AppUser actor) {
        User user = userRepository.findByUserUuid(UUID.fromString(dto.getUserUuid()))
                .orElseThrow(() -> new EntityNotFoundException("해당 사용자를 찾을 수 없습니다."));
        // 권한 체크
        boolean isSuperAdmin = actor.role().isSuperAdmin();
        if (!isSuperAdmin && !user.getUserUuid().equals(actor.userUuid())) {
            throw new AccessDeniedException("신청 권한이 없습니다.");
        }

        dto.setUserName(user.getUsername());
        dto.setUserEmail(user.getEmail());
        LeaveTypeEnum leaveType = LeaveTypeEnum.valueOf(dto.getLeaveType());
        BigDecimal usedDays = workDayService.countLeaveDays(dto.getStartDt(), dto.getEndDt());
        dto.setUsedDays(usedDays);

        String subject = leaveType.getKrName() + " 신청 (" + dto.getUserName() + ")";
        String body = "연차 유형: " + leaveType.getKrName() + "\n"
                + "사용일수: " + usedDays.stripTrailingZeros().toPlainString() + "일\n"
                + "신청자: " + dto.getUserName() + " (" + dto.getUserEmail() + ")\n"
                + "사유: " + (dto.getReason() != null ? dto.getReason() : "없음");
        try {
            String eventId = graphGroupService.createGroupEvent(
                    subject,
                    body,
                    dto.getStartDt(),
                    dto.getEndDt()
            );

            Leave leave = Leave.create(dto, user, eventId, actor.username());
            Leave savedLeave = leaveRepository.save(leave);


            leaveBalanceService.applyUsage(user, leaveType, user.getYearNumber(), usedDays);
            auditLogService.saveLog(actor.id(), actor.username(),
                    AuditActionEnum.LEAVE_APPLY_GRAPH_CREATE, AuditStatusEnum.SUCCESS,
                    details("applyDto", dto, "subject", subject, "body", body,
                            "eventId", eventId, "leaveId", savedLeave.getId()));
            return savedLeave.getId();
        } catch (IllegalStateException ex) { // ← M365 미연동 등
            throw ex; // 메시지 보존
        } catch (RestClientResponseException e) {
            // Graph API 오류
            log.error("Graph API 오류 발생: status={}, body={}", e.getStatusCode(), e.getResponseBodyAsString(), e);
            auditLogService.saveLog(
                    actor.id(), actor.username(),
                    AuditActionEnum.LEAVE_APPLY_GRAPH_CREATE, AuditStatusEnum.FAILED,
                    details("applyDto", dto, "subject", subject, "body", body,
                            "httpStatus", e.getStatusCode().value(),
                            "responseBody", e.getResponseBodyAsString())
            );
            throw new IllegalArgumentException("연차 신청에 실패했습니다. (외부 캘린더 오류)");

        } catch (Exception e) {
            // 내부 로직 오류
            log.error("연차 신청 처리 중 내부 오류", e);
            auditLogService.saveLog(
                    actor.id(), actor.username(),
                    AuditActionEnum.LEAVE_APPLY_GRAPH_CREATE, AuditStatusEnum.FAILED,
                    details("applyDto", dto, "subject", subject, "body", body,
                            "error", e.getMessage())
            );
            throw new IllegalArgumentException("연차 신청에 실패했습니다.");
        }
    }

    @Override
    @Transactional
    public Long updateLeave(Long id, UpdateLeaveReqDto reqDto, AppUser actor) {
        Leave leave = leaveRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("연차를 찾을 수 없습니다."));
        User applicantUser = leave.getUser();
        // 권한 체크
        boolean isSuperAdmin = actor.role().isSuperAdmin();
        if (!isSuperAdmin && !applicantUser.getUserUuid().equals(actor.userUuid())) {
            throw new AccessDeniedException("수정 권한이 없습니다.");
        }
        // 비즈니스 룰 검증 (기간/슬롯/영업일 등)
        leave.validateUpdatable(reqDto.getStartDt(), reqDto.getEndDt(), reqDto.getLeaveType());

        // 공휴일만 선택 한 케이스 검증
        workDayService.assertBusinessRange(reqDto.getStartDt(), reqDto.getEndDt());

        BigDecimal usedDays = workDayService.countLeaveDays(reqDto.getStartDt(), reqDto.getEndDt());
        reqDto.setUsedDays(usedDays);
        String calendarEventId = leave.getCalendarEventId();

        String subject = LeaveTypeEnum.valueOf(reqDto.getLeaveType()).getKrName() + " 신청 (" + applicantUser.getUsername() + ")";
        String body = "연차 유형: " + LeaveTypeEnum.valueOf(reqDto.getLeaveType()).getKrName() + "\n"
                + "사용일수: " + usedDays.stripTrailingZeros().toPlainString() + "일\n"
                + "신청자: " + applicantUser.getUsername() + " (" + applicantUser.getEmail() + ")\n"
                + "사유: " + (reqDto.getReason() != null ? reqDto.getReason() : "없음");
        try {
            // 캘린더 이벤트도 update
            graphGroupService.updateGroupEvent(
                    calendarEventId,
                    subject,
                    body,
                    reqDto.getStartDt(),
                    reqDto.getEndDt()
            );

            // 도메인 업데이트
            leave.updateFrom(reqDto, actor.username());
            log.info("Updating leave with id {}", id);
            auditLogService.saveLog(actor.id(), actor.username(),
                    AuditActionEnum.LEAVE_UPDATE_GRAPH_UPDATE, AuditStatusEnum.SUCCESS,
                    details("reqDto", reqDto, "subject", subject, "body", body,
                            "eventId", calendarEventId, "leaveId", id));

            return leave.getId();
        } catch (RestClientResponseException e) {
            // Graph API 오류
            log.error("Graph API 오류(연차 수정): leaveId={}, eventId={}, status={}, body={}",
                    leave.getId(), calendarEventId, e.getStatusCode(), e.getResponseBodyAsString(), e);
            auditLogService.saveLog(
                    actor.id(), actor.username(),
                    AuditActionEnum.LEAVE_UPDATE_GRAPH_UPDATE, AuditStatusEnum.FAILED,
                    details("reqDto", reqDto, "subject", subject, "body", body,
                            "eventId", calendarEventId,
                            "httpStatus", e.getStatusCode().value(),
                            "responseBody", e.getResponseBodyAsString())
            );
            throw new IllegalArgumentException("연차 수정에 실패했습니다. (외부 캘린더 오류)");
        } catch (Exception e) {
            // 내부 오류
            log.error("연차 수정 내부 오류", e);
            auditLogService.saveLog(
                    actor.id(), actor.username(),
                    AuditActionEnum.LEAVE_UPDATE_GRAPH_UPDATE, AuditStatusEnum.FAILED,
                    details("reqDto", reqDto, "subject", subject, "body", body,
                            "eventId", calendarEventId, "error", e.getMessage())
            );
            throw new IllegalArgumentException("연차 수정에 실패했습니다.");
        }
    }

    @Override
    @Transactional
    public void cancelLeave(Long id, AppUser actor) {
        Leave leave = leaveRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("연차를 찾을 수 없습니다."));
        User applicantUser = leave.getUser();

        // 권한 체크
        boolean isSuperAdmin = actor.role().isSuperAdmin();
        if (!isSuperAdmin && !applicantUser.getUserUuid().equals(actor.userUuid())) {
            throw new AccessDeniedException("취소 권한이 없습니다.");
        }
        // 상태/시점 검증
        leave.validateCancelable();

        String calendarEventId = leave.getCalendarEventId();

        // LeaveBalance used 복구
        BigDecimal usedDays = leave.getUsedDays() == null ? BigDecimal.ZERO : leave.getUsedDays();
        try {
            if (usedDays.signum() > 0) {
                leaveBalanceService.revertUsage(
                        applicantUser,
                        leave.getLeaveType(),
                        applicantUser.getYearNumber(),
                        usedDays
                );
            }

            // Leave 상태 전이
            leave.cancel(actor.username());
            // 그래프 이벤트 삭제(실패해도 취소는 계속)

            graphGroupService.deleteGroupEvent(leave.getCalendarEventId());
        } catch (RestClientResponseException e) {
            log.warn("Graph API 오류(연차 취소) - 이벤트 삭제 실패: leaveId={}, eventId={}, status={}, body={}",
                    id, calendarEventId, e.getStatusCode(), e.getResponseBodyAsString(), e);

            auditLogService.saveLog(
                    actor.id(), actor.username(),
                    AuditActionEnum.LEAVE_CANCEL_GRAPH_DELETE, AuditStatusEnum.FAILED,
                    details("leaveId", id, "eventId", calendarEventId,
                            "httpStatus", e.getStatusCode().value(),
                            "responseBody", e.getResponseBodyAsString())
            );
            throw new IllegalArgumentException("연차 취소에 실패했습니다. (외부 캘린더 오류)");
        } catch (Exception e) {
            log.warn("연차 취소 중 Graph 이벤트 삭제 일반 오류: leaveId={}, eventId={}, err={}",
                    id, calendarEventId, e.getMessage(), e);

            auditLogService.saveLog(
                    actor.id(), actor.username(),
                    AuditActionEnum.LEAVE_CANCEL_GRAPH_DELETE, AuditStatusEnum.FAILED,
                    details("leaveId", id, "eventId", calendarEventId, "error", e.getMessage())
            );
            throw new IllegalArgumentException("연차 취소에 실패했습니다.");
        }
        log.info("Canceled leave id={} (userId={}, usedDays={})", leave.getId(), applicantUser.getId(), usedDays);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CalendarEventResDto> getCalendarEvents(
            Long teamId,
            LeaveTypeEnum type,
            LeaveStatusEnum status,
            OffsetDateTime start,
            OffsetDateTime end
    ) {
        // 1) 현재 사용자가 SUPER_ADMIN 인지 판단
//        boolean isSuperAdmin = userService.isCurrentUserSuperAdmin();

        // 2) 노출 허용 상태 집합
        Set<LeaveStatusEnum> allowed = EnumSet.of(LeaveStatusEnum.APPROVED, LeaveStatusEnum.PENDING);

        // 3) 클라이언트가 상태 필터를 넣어온 경우에도 "허용 집합"과 교집합만 허용
        if (status != null && allowed.contains(status)) {
            allowed = EnumSet.of(status);
        } else if (status != null && !allowed.contains(status)) {
            // 비허용 상태를 요청하면 결과를 비워 버림
            return List.of();
        }
//        final Set<LeaveStatusEnum> allowedFinal = allowed; // 람다용 final 복사
        // 4) 조회 후 상태 필터링 (레포지토리 변경 없이 동작)
        var rows = leaveRepository.findAllForCalendar(teamId, type, allowed, start, end);
        return rows.stream().map(l -> {
            var user = l.getUser();
            String title = user.getUsername() + " · " + l.getLeaveType().getKrName();
            return CalendarEventResDto.builder()
                    .id(String.valueOf(l.getId()))
                    .title(title)
                    .start(l.getStartDt().toString())
                    .end(l.getEndDt().toString())
                    .status(l.getStatus().name())
                    .type(l.getLeaveType().name())
                    .userName(user.getUsername())
                    .build();
        }).toList();
    }
}