package pharos.groupware.service.infrastructure.graph;

import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Service
public class AzureUserClient {

    private final AzureTokenProvider azureTokenProvider;
    private final RestTemplate restTemplate;

    public AzureUserClient(AzureTokenProvider azureTokenProvider) {
        this.azureTokenProvider = azureTokenProvider;
        this.restTemplate = new RestTemplate();
    }

    public String getUsers() {
        String accessToken = azureTokenProvider.getAccessToken();

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));

        HttpEntity<String> request = new HttpEntity<>(headers);

        String url = "https://graph.microsoft.com/v1.0/users";

        ResponseEntity<String> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                request,
                String.class
        );

        return response.getBody();  // 사용자 목록 JSON
    }
}
