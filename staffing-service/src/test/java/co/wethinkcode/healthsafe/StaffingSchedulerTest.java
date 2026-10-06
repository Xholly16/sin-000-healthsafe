package co.wethinkcode.healthsafe;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;


public class StaffingSchedulerTest {

    @Test
    void lowAlertLevelRequiresOneDoctor() {
        int doctors = StaffingScheduler.doctorsRequired(0);
        assertEquals(1, doctors);
    }

    @Test
    void mediumAlertLevelsNeedThreeDoctors() {
        assertEquals(3, scheduler.doctorsRequired(3));
        assertEquals(3, scheduler.doctorsRequired(5));
    }

    @Test
    void highAlertLevelsNeedSixDoctors() {
        assertEquals(6, scheduler.doctorsRequired(6));
        assertEquals(6, scheduler.doctorsRequired(8));
    }

    @Test
    void levelOutsideZeroToEightIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> scheduler.doctorsRequired(-1));
        assertThrows(IllegalArgumentException.class, () -> scheduler.doctorsRequired(9));
    }
}
