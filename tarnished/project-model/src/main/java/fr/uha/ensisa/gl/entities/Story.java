package fr.uha.ensisa.gl.entities;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Story {
    private int id;
    private String title;
    private String description;
    private StoryStatus status;
    private User userAssigned;
    private Date dateStart;
    private Date dateCreated;
    private Date dateEnd;
    private List<WorkLog> workLogs = new ArrayList<>();
    private long totalTimeSpent;
    private Long projectId;
    private Long columnId;

    public void addWorkLog(WorkLog workLog) {
        if (workLogs == null) {
            workLogs = new ArrayList<>();
        }
        workLogs.add(workLog);
        calculateTotalTime();
    }

    public void removeWorkLog(long workLogId) {
        if (workLogs != null) {
            workLogs.removeIf(wl -> wl.getId() == workLogId);
            calculateTotalTime();
        }
    }

    public void calculateTotalTime() {
        if (workLogs == null) {
            totalTimeSpent = 0;
            return;
        }
        totalTimeSpent = workLogs.stream()
                .mapToLong(WorkLog::getDuration)
                .sum();
    }

    public WorkLog getRunningWorkLog() {
        if (workLogs == null) {
            return null;
        }
        return workLogs.stream()
                .filter(WorkLog::isRunning)
                .findFirst()
                .orElse(null);
    }
}