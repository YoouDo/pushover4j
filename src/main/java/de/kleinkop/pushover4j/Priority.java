package de.kleinkop.pushover4j;

public enum Priority {
    LOWEST(-2),
    LOW(-1),
    NORMAL(0),
    HIGH(1),
    EMERGENCY(2);

    private final int value;

    Priority(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}
