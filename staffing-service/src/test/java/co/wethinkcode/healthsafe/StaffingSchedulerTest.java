package co.wethinkcode.healthsafe;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;


public class StaffingSchedulerTest {

    @Test
    void lowAlertLevelRequiresOneDoctor() {
        int doctors = StaffingScheduler.doctorsRequired(0);
        assertEquals(1, doctors);
    }
}
