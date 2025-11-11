package fr.uha.ensisa.gl.entities;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Project {
    int id;
    String name;
    String description;
    Date dateStarted;
    Date dateEnded;
    User owner;
    List<User> members;
    List<Column> workFlow;
}
