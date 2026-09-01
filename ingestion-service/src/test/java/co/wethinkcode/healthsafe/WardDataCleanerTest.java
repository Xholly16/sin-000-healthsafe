package co.wethinkcode.healthsafe;

import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

public class WardDataCleanerTest {
    @Test
    void mergesRowsWithSameWardIdIgnoringCase() throws Exception {
        List<WardRecord> wards = WardDataCleaner.loadAndClean("test-wards.csv");

        long countW01 = wards.stream()
                .filter(w -> w.wardId.equals("W-01"))
                .count();

        assertEquals(1, countW01, "W-01 and w-01 should be merged into a single record");
    }


    @Test
    void missingWingBecomesNull() throws Exception {
        List<WardRecord> wards = WardDataCleaner.loadAndClean("test-wards.csv");

        WardRecord w02 = wards.stream()
                .filter(w -> w.wardId.equals("W-02"))
                .findFirst()
                .orElseThrow();

        assertNull(w02.wing, "Missing wing should be normalised to null");
    }

}
