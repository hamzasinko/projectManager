package fr.uha.ensisa.gl.tarnished.model;


public enum StoryStatus {
    TODO,
    IN_PROGRESS,
    REVIEW,
    DONE,
    BLOCKED;


    public static StoryStatus fromString(String value) {
        if (value == null) throw new IllegalArgumentException("StoryStatus value is null");
        try {
            return StoryStatus.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid StoryStatus: " + value, e);
        }
    }
}

