package com.ibm.airlock.admin.airlytics;

public enum JobStatus {
    PENDING, RUNNING, FAILED, COMPLETED, EXPORTED;
    public static JobStatus fromString(String str) {
        if (str == null)
            return null;
        if (str.equalsIgnoreCase(PENDING.toString())) {
            return PENDING;
        } else if (str.equalsIgnoreCase(RUNNING.toString())) {
            return RUNNING;
        } else if (str.equalsIgnoreCase(FAILED.toString())) {
            return FAILED;
        } else if (str.equalsIgnoreCase(COMPLETED.toString())) {
            return COMPLETED;
        }  else if (str.equalsIgnoreCase(EXPORTED.toString())) {
            return EXPORTED;
        }
        return null;
    }
}
