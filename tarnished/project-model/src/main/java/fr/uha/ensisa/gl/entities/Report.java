package fr.uha.ensisa.gl.entities;

import lombok.*;

@Data @NoArgsConstructor @AllArgsConstructor
public class Report {
    private Float avgTimePerTask; // heures
    private Float successRatio;   // 0..1
    private Float performPerUser;
}
