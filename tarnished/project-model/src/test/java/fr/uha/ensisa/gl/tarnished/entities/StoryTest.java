package fr.uha.ensisa.gl.tarnished.entities;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Date;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class StoryTest {

    private Story sut;

    @BeforeEach
    void setUp() {
        sut = new Story();
    }

    @Test
    void testSetGetId() {
        sut.setId(1L);
        assertEquals(1L, sut.getId());
    }

    @Test
    void testSetGetTitle() {
        sut.setTitle("Test");
        assertEquals("Test", sut.getTitle());
    }

    @Test
    void testSetGetDescription() {
        sut.setDescription("Description");
        assertEquals("Description", sut.getDescription());
    }

    @Test
    void testSetGetStatus() {
        sut.setStatus(StoryStatus.DONE);
        assertEquals(StoryStatus.DONE, sut.getStatus());
    }

    @Test
    void testSetGetUserAssigned() {
        User user = new User();
        sut.setUserAssigned(user);
        assertEquals(user, sut.getUserAssigned());
    }

    @Test
    void testSetGetDateStart() {
        Date date = new Date();
        sut.setDateStart(date);
        assertEquals(date, sut.getDateStart());
    }

    @Test
    void testSetGetDateEnd() {
        Date date = new Date();
        sut.setDateEnd(date);
        assertEquals(date, sut.getDateEnd());
    }

    @Test
    void testAddWorkLog() {
        WorkLog log = new WorkLog();
        sut.addWorkLog(log);
        assertEquals(1, sut.getWorkLogs().size());
    }

    @Test
    void testRemoveWorkLog() {
        WorkLog log = new WorkLog();
        sut.addWorkLog(log);
        sut.removeWorkLog(log);
        assertEquals(0, sut.getWorkLogs().size());
    }

    @Test
    void testEquals() {
        Story story1 = new Story();
        story1.setId(1L);

        Story story2 = new Story();
        story2.setId(1L);

        assertEquals(story1, story2);
    }
}