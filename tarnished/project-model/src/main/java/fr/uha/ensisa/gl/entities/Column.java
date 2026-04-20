package fr.uha.ensisa.gl.entities;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.List;


@Data
@NoArgsConstructor
@AllArgsConstructor

public class Column {

    private int id;

    private String name;

    private int position;

    private int maxCapacity;

    private boolean hasSubColumns;

    private Project project;

    private List<Story> stories;

}
