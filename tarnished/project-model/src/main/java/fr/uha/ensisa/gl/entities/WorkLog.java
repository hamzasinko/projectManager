package fr.uha.ensisa.gl.entities;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WorkLog {
    private int id;
    private float duration;
    private Date hourStart;
    private Date hourEnd;
    private User user;
}
