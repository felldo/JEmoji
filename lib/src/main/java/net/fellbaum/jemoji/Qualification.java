package net.fellbaum.jemoji;

import java.util.Arrays;
import java.util.List;

public enum Qualification {

    COMPONENT("component"),
    FULLY_QUALIFIED("fully-qualified"),
    MINIMALLY_QUALIFIED("minimally-qualified"),
    UNQUALIFIED("unqualified");

    private static final List<Qualification> QUALIFICATION_LIST = Arrays.asList(values());
    private final String qualification;

    Qualification(final String qualification) {
        this.qualification = qualification;
    }

    public String getQualification() {
        return qualification;
    }

    public static Qualification fromString(final String qualification) {
        for (Qualification q : QUALIFICATION_LIST) {
            if (q.getQualification().equals(qualification)) {
                return q;
            }
        }
        throw new IllegalArgumentException("Unknown qualification encountered");
    }
}
