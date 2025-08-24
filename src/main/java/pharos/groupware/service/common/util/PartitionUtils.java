package pharos.groupware.service.common.util;

import java.util.ArrayList;
import java.util.List;

public final class PartitionUtils {
    private PartitionUtils() {
    }

    public static <T> List<List<T>> batchesOf(List<T> src, int size) {
        List<List<T>> out = new ArrayList<>();
        for (int i = 0; i < src.size(); i += size) {
            out.add(src.subList(i, Math.min(i + size, src.size())));
        }
        return out;
    }
}
