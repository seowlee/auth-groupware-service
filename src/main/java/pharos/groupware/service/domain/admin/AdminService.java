package pharos.groupware.service.domain.account.service;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import pharos.groupware.service.domain.admin.dto.LoginReqDto;
import pharos.groupware.service.domain.admin.dto.LoginResDto;

public interface AdminService {
    LoginResDto login(LoginReqDto reqDto);

    LoginResDto login(LoginReqDto reqDto, HttpServletRequest request, HttpServletResponse response);
}
