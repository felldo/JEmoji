package net.fellbaum.jemoji;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum Qualification {

    COMPONENT("component"),
    FULLY_QUALIFIED("fully-qualified"),
    MINIMALLY_QUALIFIED("minimally-qualified"),
    UNQUALIFIED("unqualified");

    private final String qualification;

    Qualification(String qualification) {
        this.qualification = qualification;
    }

    public String getQualification() {
        return qualification;
    }

    @JsonCreator
    public static Qualification fromString(String qualification) {
        for (Qualification q : Qualification.values()) {
            if (q.getQualification().equals(qualification)) {
                return q;
            }
        }
        throw new IllegalArgumentException("Unknown qualification encountered");
    }
}
