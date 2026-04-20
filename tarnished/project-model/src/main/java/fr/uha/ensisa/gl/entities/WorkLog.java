package fr.uha.ensisa.gl.entities;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WorkLog {
    private long id;
    private LocalDateTime start;
    private LocalDateTime end;
    private long duration;
    private long userId;
    private String comment;
    private long storyId;

    public WorkLog(long id, LocalDateTime start, long userId, long storyId) {
        this.id = id;
        this.start = start;
        this.userId = userId;
        this.storyId = storyId;
        this.duration = 0;
    }

    public void stopTimer() {
        if (this.end == null) {
            this.end = LocalDateTime.now();
            if (this.start != null) {
                this.duration = ChronoUnit.MINUTES.between(this.start, this.end);
            }
        }
    }

    public boolean isRunning() {
        return this.end == null;
    }
}
