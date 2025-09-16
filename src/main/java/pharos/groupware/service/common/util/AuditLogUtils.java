package pharos.groupware.service.common.util;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.LinkedHashMap;
import java.util.Map;

public final class AuditLogUtils {

    private static final ObjectMapper OM = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .findAndRegisterModules();

    private AuditLogUtils() {
    }

    /**
     * 클라이언트 IP 추출
     */
    public static String currentIp() {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs == null) return null;

        HttpServletRequest req = attrs.getRequest();
        if (req == null) return null;

        String ip = req.getHeader("X-Forwarded-For");
        if (ip != null && !ip.isBlank()) {
            return ip.split(",")[0].trim();
        }
        return req.getRemoteAddr();
    }

    /**
     * 객체 → JSON 문자열 (pretty print 가능)
     */
    public static String toJson(Object obj) {
        try {
            return OM.writerWithDefaultPrettyPrinter().writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            return "{\"_jsonError\":\"" + e.getMessage() + "\"}";
        }
    }

    /**
     * key/value 가 null 이면 무시하고 LinkedHashMap 으로 반환
     */
    public static Map<String, Object> details(Object... kvs) {
        Map<String, Object> m = new LinkedHashMap<>();
        for (int i = 0; i + 1 < kvs.length; i += 2) {
            Object key = kvs[i];
            Object val = kvs[i + 1];
            if (key != null && val != null) {
                m.put(String.valueOf(key), val);
            }
        }
        return m;
    }

    /**
     * 조건부 put 유틸이 필요할 때
     */
    public static void putIfPresent(Map<String, Object> m, String key, Object value) {
        if (key != null && value != null) m.put(key, value);
    }
}
