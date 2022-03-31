package org.monarchinitiative.lirical.model;

public enum Sex {

    FEMALE("Female"),MALE("Male"), UNKNOWN("Unknown");

    private final String label;

    Sex(String label) {
        this.label=label;
    }
    @Override
    public String toString() {
        return label;
    }


}
