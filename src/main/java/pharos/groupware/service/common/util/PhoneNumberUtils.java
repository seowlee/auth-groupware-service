package pharos.groupware.service.common.util;

public class PhoneNumberUtils {
    private PhoneNumberUtils() {
    }

    public static String normalize(String raw) {
        if (raw == null) return null;
        String s = raw.replaceAll("[\\s-]", "");
        if (s.startsWith("+82")) {
            s = "0" + s.substring(3);
        }
        return s;
    }
}
