package co.wethinkcode.healthsafe;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class AlertLevelValidatorTest {

    @Test
    void acceptsLevelsZeroToEight() {
        assertEquals(0, AlertLevelValidator.parse(Map.of("level", 0)));
        assertEquals(5, AlertLevelValidator.parse(Map.of("level", 5)));
        assertEquals(8, AlertLevelValidator.parse(Map.of("level", 8)));
    }

    @Test
    void rejectsLevelBelowZero() {
        assertThrows(IllegalArgumentException.class,
                () -> AlertLevelValidator.parse(Map.of("level", -1)));
    }

    @Test
    void rejectsLevelAboveEight() {
        assertThrows(IllegalArgumentException.class,
                () -> AlertLevelValidator.parse(Map.of("level", 9)));
    }

    @Test
    void rejectsMissingLevel() {
        assertThrows(IllegalArgumentException.class,
                () -> AlertLevelValidator.parse(Map.of()));
    }

    @Test
    void rejectsNullBody() {
        assertThrows(IllegalArgumentException.class,
                () -> AlertLevelValidator.parse(null));
    }

    @Test
    void rejectsNonNumericLevel() {
        assertThrows(IllegalArgumentException.class,
                () -> AlertLevelValidator.parse(Map.of("level", "high")));
    }

    @Test
    void rejectsFractionalLevel() {
        assertThrows(IllegalArgumentException.class,
                () -> AlertLevelValidator.parse(Map.of("level", 3.5)));
    }
}
