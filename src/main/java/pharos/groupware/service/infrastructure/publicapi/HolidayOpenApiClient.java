package pharos.groupware.service.infrastructure.publicapi;

import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.DefaultUriBuilderFactory;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

@Slf4j
@Component
public class HolidayOpenApiClient {
    private final WebClient webClient;
    private final XmlMapper xmlMapper = new XmlMapper();


    @Value("${external.holiday.service-key}")
    private String serviceKey; // URL-encoded (data.go.kr에서 발급)

    public HolidayOpenApiClient(WebClient.Builder webClientBuilder,
                                @Value("${external.holiday.base-url}") String baseUrl,
                                @Value("${external.holiday.service-key}") String serviceKey) {

        // 서비스 키의 특수 문자가 WebClient에 의해 이중 인코딩되는 것을 방지하기 위해
        // DefaultUriBuilderFactory의 인코딩 모드를 NONE으로 설정합니다.
        DefaultUriBuilderFactory factory = new DefaultUriBuilderFactory(baseUrl);
        factory.setEncodingMode(DefaultUriBuilderFactory.EncodingMode.NONE);

        this.webClient = webClientBuilder
                .uriBuilderFactory(factory)
                .build();
        this.serviceKey = serviceKey;
    }

    /**
     * "20251003" → LocalDate
     */
    public static LocalDate toDate(String yyyymmdd) {
        return LocalDate.parse(Objects.requireNonNull(yyyymmdd), DateTimeFormatter.ofPattern("yyyyMMdd"));
    }

    /**
     * 특정 연도의 공휴일 목록 조회(XML → DTO)
     */
    public List<HolidayApiItem> fetchYear(int year) {
//        String url = UriComponentsBuilder.fromUriString(baseUrl + "/getRestDeInfo")
//                .queryParam("serviceKey", serviceKey)  // ← 여기서 WebClient가 인코딩
//                .queryParam("numOfRows", 200)
//                .queryParam("pageNo", 1)
//                .queryParam("solYear", year)
//                .build(false)
//                .toUriString();

//        System.out.println("url: " + url);
        String xml = webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/getRestDeInfo")
                        .queryParam("serviceKey", serviceKey)
                        .queryParam("numOfRows", 200)
                        .queryParam("pageNo", 1)
                        .queryParam("solYear", year)
                        .build())
                .accept(MediaType.APPLICATION_XML)
                .retrieve()
                .bodyToMono(String.class)
                .block();

        try {
            HolidayApiResponse res = xmlMapper.readValue(xml, HolidayApiResponse.class);
            if (res == null || res.getBody() == null || res.getBody().getItems() == null) {
                log.warn("Parsed but items missing. xmlHead={}", xml.substring(0, Math.min(400, xml.length())));
                return Collections.emptyList();
            }
            return res.getBody().getItems();
        } catch (Exception e) {
            log.error("Holiday API XML parse error. xmlHead={}", xml.substring(0, Math.min(400, xml.length())));
            throw new RuntimeException("Holiday API XML parse error", e);
        }
    }
}
