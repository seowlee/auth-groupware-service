package pharos.groupware.service.common.util;

public final class CommonUtils {
    private CommonUtils() {
    }

    public static String phoneNumberNormalize(String raw) {
        if (raw == null) return null;
        String s = raw.replaceAll("[\\s-]", "");
        if (s.startsWith("+82")) {
            s = "0" + s.substring(3);
        }
        return s;
    }

    public static short parseSeq(String s) {
        if (s == null || s.isBlank()) return 1;
        try {
            return Short.parseShort(s.trim());
        } catch (NumberFormatException e) {
            return 1;
        }
    }

}
