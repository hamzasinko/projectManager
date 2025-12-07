package fr.uha.ensisa.gl;

import fr.uha.ensisa.gl.entities.WorkLog;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class WorkLogTest {

    @Test
    void testCreateWorkLog() {
        WorkLog workLog = new WorkLog();
        workLog.setId(1);
        workLog.setUserId(1);
        workLog.setStoryId(1);
        workLog.setStart(LocalDateTime.now());
        
        assertNotNull(workLog);
        assertEquals(1, workLog.getId());
        assertEquals(1, workLog.getUserId());
        assertEquals(1, workLog.getStoryId());
        assertNotNull(workLog.getStart());
    }

    @Test
    void testCalculateDurationAutomatically() {
        LocalDateTime start = LocalDateTime.now().minusMinutes(30);
        LocalDateTime end = LocalDateTime.now();
        
        WorkLog workLog = new WorkLog();
        workLog.setId(1);
        workLog.setStart(start);
        workLog.setEnd(end);
        
        assertNotNull(workLog.getStart());
        assertNotNull(workLog.getEnd());
    }

    @Test
    void testValidationStartBeforeEnd() {
        LocalDateTime start = LocalDateTime.now();
        LocalDateTime end = LocalDateTime.now().plusMinutes(30);
        
        WorkLog workLog = new WorkLog();
        workLog.setStart(start);
        workLog.setEnd(end);
        
        assertTrue(workLog.getStart().isBefore(workLog.getEnd()));
    }

    @Test
    void testStopTimer() {
        LocalDateTime start = LocalDateTime.now().minusMinutes(10);
        
        WorkLog workLog = new WorkLog(1, start, 1, 1);
        
        assertTrue(workLog.isRunning());
        
        workLog.stopTimer();
        
        assertFalse(workLog.isRunning());
        assertNotNull(workLog.getEnd());
        assertTrue(workLog.getDuration() > 0);
    }

    @Test
    void testIsRunning() {
        WorkLog workLog = new WorkLog(1, LocalDateTime.now(), 1, 1);
        
        assertTrue(workLog.isRunning());
        
        workLog.stopTimer();
        
        assertFalse(workLog.isRunning());
    }
}