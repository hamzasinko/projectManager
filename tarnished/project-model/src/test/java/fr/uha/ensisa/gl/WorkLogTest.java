package fr.uha.ensisa.gl;

import java.util.Date;

import fr.uha.ensisa.gl.entities.User;
import fr.uha.ensisa.gl.entities.WorkLog;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class WorkLogTest {

    private WorkLog sut;

    @BeforeEach
    void setUp() {
        sut = new WorkLog();
    }

    @Test
    void testSetGetId() {
        sut.setId(1);
        assertEquals(1, sut.getId());
    }

    @Test
    void testSetGetDuration() {
        sut.setDuration(2.5f);
        assertEquals(2.5f, sut.getDuration());
    }

    @Test
    void testSetGetHourStart() {
        Date date = new Date();
        sut.setHourStart(date);
        assertEquals(date, sut.getHourStart());
    }

    @Test
    void testSetGetHourEnd() {
        Date date = new Date();
        sut.setHourEnd(date);
        assertEquals(date, sut.getHourEnd());
    }

    @Test
    void testSetGetUser() {
        User user = new User();
        sut.setUser(user);
        assertEquals(user, sut.getUser());
    }

    @Test
    void testEquals() {
        WorkLog log1 = new WorkLog();
        log1.setId(1);

        WorkLog log2 = new WorkLog();
        log2.setId(1);

        assertEquals(log1, log2);
    }
}