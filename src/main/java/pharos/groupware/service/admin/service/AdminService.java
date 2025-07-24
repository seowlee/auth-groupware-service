package pharos.groupware.service.admin.service;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import pharos.groupware.service.admin.dto.LoginReqDto;
import pharos.groupware.service.admin.dto.LoginResDto;

public interface AdminService {
    LoginResDto login(LoginReqDto reqDto);

    LoginResDto login(LoginReqDto reqDto, HttpServletRequest request, HttpServletResponse response);
}
