package fr.uha.ensisa.gl;

import fr.uha.ensisa.gl.entities.Report;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class ReportTest {

    @Test
    void noArgsConstructorAndSetters_work() {
        Report r = new Report(); // @NoArgsConstructor

        r.setAvgTimePerTask(3.5f);
        r.setSuccessRatio(0.8f);
        r.setPerformPerUser(2.1f);

        assertEquals(Float.valueOf(3.5f), r.getAvgTimePerTask());
        assertEquals(Float.valueOf(0.8f),  r.getSuccessRatio());
        assertEquals(Float.valueOf(2.1f),  r.getPerformPerUser());
    }

    @Test
    void allArgsConstructor_setsAllFields() {
        Report r = new Report(4.0f, 0.75f, 1.2f); // @AllArgsConstructor

        assertEquals(Float.valueOf(4.0f), r.getAvgTimePerTask());
        assertEquals(Float.valueOf(0.75f), r.getSuccessRatio());
        assertEquals(Float.valueOf(1.2f), r.getPerformPerUser());
    }

    @Test
    void gettersReturnNull_whenFieldsAreNull() {
        Report r = new Report();
        r.setAvgTimePerTask(null);
        r.setSuccessRatio(null);
        r.setPerformPerUser(null);

        assertNull(r.getAvgTimePerTask());
        assertNull(r.getSuccessRatio());
        assertNull(r.getPerformPerUser());
    }

    @Test
    void equalsAndHashCode_sameValues_areEqual() {
        Report r1 = new Report(3.0f, 0.6f, 1.0f);
        Report r2 = new Report(3.0f, 0.6f, 1.0f);

        assertEquals(r1, r2);
        assertEquals(r1.hashCode(), r2.hashCode());
    }

    @Test
    void equalsAndHashCode_differentValue_notEqual() {
        Report r1 = new Report(3.0f, 0.6f, 1.0f);
        Report r2 = new Report(3.0f, 0.6f, 1.1f); // différent

        assertNotEquals(r1, r2);
        assertNotEquals(r1.hashCode(), r2.hashCode());
    }

    @Test
    void toString_containsFieldNames() {
        Report r = new Report(2.5f, 0.9f, 0.7f);
        String s = r.toString(); // généré par @Data

        assertTrue(s.contains("avgTimePerTask=2.5"));
        assertTrue(s.contains("successRatio=0.9"));
        assertTrue(s.contains("performPerUser=0.7"));
    }

}
