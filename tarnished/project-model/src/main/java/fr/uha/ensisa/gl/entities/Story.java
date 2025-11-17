package fr.uha.ensisa.gl.entities;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.ArrayList;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
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
}