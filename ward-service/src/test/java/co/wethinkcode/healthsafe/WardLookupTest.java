package co.wethinkcode.healthsafe;

import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;

public class WardLookupTest {

    @Test
    void findsWardByIdCaseInsensitive() {
        WardRecord w1 = new WardRecord();
        w1.wardId = "W-01";

        WardRecord w2 = new WardRecord();
        w2.wardId = "W-02";

        List<WardRecord> wards = List.of(w1, w2);

        Optional<WardRecord> result = WardLookup.findById(wards, "w-01");

        assertTrue(result.isPresent(), "Should find W-01 even when searched with lowercase id");
        assertEquals("W-01", result.get().wardId);
    }


    @Test
    void returnsEmptyWhenWardIdNotFound() {
        WardRecord w1 = new WardRecord();
        w1.wardId = "W-01";

        List<WardRecord> wards = List.of(w1);

        Optional<WardRecord> result = WardLookup.findById(wards, "W-99");

        assertTrue(result.isEmpty(), "Should return empty Optional when id doesn't exist");
    }


}
