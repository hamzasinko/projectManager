package fr.uha.ensisa.gl.tarnished.entities;

import java.util.Date;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor

public class Story {
    private long id;
    private String title;
    private String description;
    private StoryStatus status;
    private User userAssigned;
    private Date dateStart;
    private Date dateCreated;
    private Date dateEnd;
    private List<WorkLog> workLogs;

    // Constructeur
    public Story() {
        this.workLogs = new ArrayList<>();
        this.dateCreated = new Date();
        this.status = StoryStatus.TODO;
    }


    public Story(String title) {
        this();
        this.title = title;
    }

    // constructeur
    public Story(long id, String title, String description) {
        this(title);
        this.id = id;
        this.description = description;
    }

    // Getters
    public long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public StoryStatus getStatus() {
        return status;
    }

    public User getUserAssigned() {
        return userAssigned;
    }

    public Date getDateStart() {
        return dateStart;
    }

    public Date getDateCreated() {
        return dateCreated;
    }

    public Date getDateEnd() {
        return dateEnd;
    }

    public List<WorkLog> getWorkLogs() {
        return new ArrayList<>(workLogs);

    // Setters
    public void setId(long id) {
        this.id = id;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setStatus(StoryStatus status) {
        this.status = status;
    }

    public void setUserAssigned(User userAssigned) {
        this.userAssigned = userAssigned;
    }

    public void setDateStart(Date dateStart) {
        this.dateStart = dateStart;
    }

    public void setDateCreated(Date dateCreated) {
        this.dateCreated = dateCreated;
    }

    public void setDateEnd(Date dateEnd) {
        this.dateEnd = dateEnd;
    }

    // Méthode pour ajouter un WorkLog
    public void addWorkLog(WorkLog workLog) {
        if (workLog != null) {
            this.workLogs.add(workLog);
        }
    }


    public void removeWorkLog(WorkLog workLog) {
        this.workLogs.remove(workLog);
    }

    @Override
    public String toString() {
        return "Story{" +
                "id=" + id +
                ", title='" + title + '\'' +
                ", status=" + status +
                ", userAssigned=" + (userAssigned != null ? userAssigned.getName() : "none") +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Story story = (Story) o;
        return id == story.id;
    }

    @Override
    public int hashCode() {
        return Long.hashCode(id);
    }
}