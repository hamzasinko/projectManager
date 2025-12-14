package fr.uha.ensisa.gl.entities;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Project {
    private int id;
    private String name;
    private String description;
    private Date dateStarted;
    private Date dateEnded;
    private User owner;
    private List<User> members;
    private List<Column> workFlow;
    private List<Swimlane> swimlanes = new ArrayList<>();
}
