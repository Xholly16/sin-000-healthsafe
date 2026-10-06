package co.wethinkcode.healthsafe;

public class StaffingScheduler {

    public static int doctorsRequired(int alertLevel) {
        if (alertLevel < 0 || alertLevel > 8) {
            throw new IllegalArgumentException("Alert level must be between 0 and 8: " + alertLevel);
        }
        if (alertLevel <= 2) {
            return 1;
        }
        if (alertLevel <= 5) {
            return 3;
        }
        return 6;
    }
}
