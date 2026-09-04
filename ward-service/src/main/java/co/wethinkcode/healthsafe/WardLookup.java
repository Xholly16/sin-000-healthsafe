package co.wethinkcode.healthsafe;

import java.util.List;
import java.util.Optional;

public class WardLookup {

    public static Optional<WardRecord> findById(List<WardRecord> wards, String id) {
        return wards.stream()
                .filter(w -> w.wardId.equalsIgnoreCase(id))
                .findFirst();
    }
}
