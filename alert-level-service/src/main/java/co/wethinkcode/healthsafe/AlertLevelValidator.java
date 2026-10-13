package co.wethinkcode.healthsafe;

import java.util.Map;

public class AlertLevelValidator {

    public static final int MIN_LEVEL = 0;
    public static final int MAX_LEVEL = 8;

    /**
     * Reads the "level" field from a request body and checks it is a whole number from 0 to 8.
     * Throws IllegalArgumentException (with a message the caller can show) if it is not.
     */
    public static int parse(Map<?, ?> body) {
        if (body == null || !body.containsKey("level")) {
            throw new IllegalArgumentException("Field 'level' is required");
        }
        Object raw = body.get("level");
        if (!(raw instanceof Number number)) {
            throw new IllegalArgumentException("level must be a number");
        }
        double value = number.doubleValue();
        if (Double.isInfinite(value) || value != Math.floor(value)) {
            throw new IllegalArgumentException("level must be a whole number");
        }
        if (value < MIN_LEVEL || value > MAX_LEVEL) {
            throw new IllegalArgumentException("level must be between 0 and 8");
        }
        return (int) value;
    }
}
