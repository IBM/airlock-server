package com.ibm.airlock.admin.airlytics;

public enum ValueType {
    STRING("String"),
    BOOL("Boolean"),
    INT("Integer Number"),
    FLOAT("Floating Point Number"),
    DATE("Date"),
    ARRAY("Array"),
    JSON("JSON");
    private String label;
    ValueType(String label) {
        this.label = label;
    }
    public String getLabel() {
        return label;
    }

    public static ValueType fromString(String str) {
        if (str == null)
            return null;
        if (str.equalsIgnoreCase(STRING.getLabel())) {
            return STRING;
        } else if (str.equalsIgnoreCase(BOOL.getLabel())) {
            return BOOL;
        } else if (str.equalsIgnoreCase(INT.getLabel())) {
            return INT;
        } else if (str.equalsIgnoreCase(FLOAT.getLabel())) {
            return FLOAT;
        } else if (str.equalsIgnoreCase(DATE.getLabel())) {
            return DATE;
        } else if (str.equalsIgnoreCase(ARRAY.getLabel())) {
            return ARRAY;
        } else if (str.equalsIgnoreCase(JSON.getLabel())) {
            return JSON;
        }
        return null;
    }
}
