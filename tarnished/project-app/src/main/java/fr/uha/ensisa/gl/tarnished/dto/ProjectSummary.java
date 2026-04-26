package fr.uha.ensisa.gl.tarnished.dto;

/**
 * DTO minimal pour la page d'accueil (RGESN 4.1).
 * On évite de transmettre des graphes d'objets inutiles (owner, members, workflow...).
 */
public final class ProjectSummary {
    private final int id;
    private final String name;
    private final String description;

    public ProjectSummary(int id, String name, String description) {
        this.id = id;
        this.name = name;
        this.description = description;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }
}

