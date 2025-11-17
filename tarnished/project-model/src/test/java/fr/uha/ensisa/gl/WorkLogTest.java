package fr.uha.ensisa.gl;

import java.util.Date;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class WorkLogTest {

    private WorkLog sut;

    @BeforeEach
    void setUp() {
        sut = new WorkLog();
    }

    @Test
    void testSetGetId() {
        sut.setId(1L);
        assertEquals(1L, sut.getId());
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
        log1.setId(1L);

        WorkLog log2 = new WorkLog();
        log2.setId(1L);

        assertEquals(log1, log2);
    }
}