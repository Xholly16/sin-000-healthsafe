package co.wethinkcode.healthsafe;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class WardDataCleanerNotesTest {

    private static List<WardRecord> wards;

    @BeforeAll
    static void load() throws Exception {
        wards = WardDataCleaner.loadAndClean("test-notes.csv");
    }

    private static WardRecord ward(String id) {
        return wards.stream()
                .filter(w -> w.wardId.equals(id))
                .findFirst()
                .orElseThrow();
    }

    private static void assertNoteContains(WardRecord w, String text) {
        assertNotNull(w.notes, "Expected a note on " + w.wardId);
        assertTrue(w.notes.contains(text),
                "Note on " + w.wardId + " should contain '" + text + "' but was: " + w.notes);
    }

    @Test
    void cleanRowHasNoNote() {
        assertNull(ward("W-01").notes);
    }

    @Test
    void nonNumericBedsAreFlagged() {
        WardRecord w = ward("W-02");
        assertNull(w.bedsAvailable);
        assertNoteContains(w, "non-numeric");
        assertNoteContains(w, "'five'");
    }

    @Test
    void placeholderBedsAreFlagged() {
        WardRecord w = ward("W-03");
        assertNull(w.bedsAvailable);
        assertNoteContains(w, "missing");
        assertNoteContains(w, "'N/A'");
    }

    @Test
    void missingWingIsFlagged() {
        WardRecord w = ward("W-03");
        assertNull(w.wing);
        assertNoteContains(w, "wing was missing");
    }

    @Test
    void negativeBedsAreFlagged() {
        WardRecord w = ward("W-04");
        assertNull(w.bedsAvailable);
        assertNoteContains(w, "negative");
    }

    @Test
    void unrealisticBedsAreFlagged() {
        WardRecord w = ward("W-05");
        assertNull(w.bedsAvailable);
        assertNoteContains(w, "unrealistic");
    }

    @Test
    void mergedDuplicateKeepsBothTheMergeNoteAndTheRowNote() {
        WardRecord w = ward("W-06");
        assertEquals(4, w.bedsAvailable);
        assertNoteContains(w, "Merged duplicate");
        assertNoteContains(w, "non-numeric");
    }
}
