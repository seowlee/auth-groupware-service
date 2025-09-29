package pharos.groupware.service.domain.m365.service;

public interface M365IntegrationReadService {
    /**
     * 테넌트(조직) 차원에서 M365 위임 갱신 토큰이 보관돼 있으면 true
     */
    boolean isLinked();
}
