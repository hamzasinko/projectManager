package fr.uha.ensisa.gl.tarnished.entities;

import java.util.Date;

public class WorkLog {
    private long id;
    private float duration;
    private Date hourStart;
    private Date hourEnd;
    private User user;

    // Constructeur par défaut
    public WorkLog() {
    }

    // Constructeur avec paramètres
    public WorkLog(long id, float duration, Date hourStart, Date hourEnd, User user) {
        this.id = id;
        this.duration = duration;
        this.hourStart = hourStart;
        this.hourEnd = hourEnd;
        this.user = user;
    }

    // Getters
    public long getId() {
        return id;
    }

    public float getDuration() {
        return duration;
    }

    public Date getHourStart() {
        return hourStart;
    }

    public Date getHourEnd() {
        return hourEnd;
    }

    public User getUser() {
        return user;
    }

    // Setters
    public void setId(long id) {
        this.id = id;
    }

    public void setDuration(float duration) {
        this.duration = duration;
    }

    public void setHourStart(Date hourStart) {
        this.hourStart = hourStart;
    }

    public void setHourEnd(Date hourEnd) {
        this.hourEnd = hourEnd;
    }

    public void setUser(User user) {
        this.user = user;
    }

    @Override
    public String toString() {
        return "WorkLog{" +
                "id=" + id +
                ", duration=" + duration +
                ", hourStart=" + hourStart +
                ", hourEnd=" + hourEnd +
                ", user=" + (user != null ? user.getName() : "null") +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        WorkLog workLog = (WorkLog) o;
        return id == workLog.id;
    }

    @Override
    public int hashCode() {
        return Long.hashCode(id);
    }
}
