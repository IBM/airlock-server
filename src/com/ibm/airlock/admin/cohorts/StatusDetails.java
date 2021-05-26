package com.ibm.airlock.admin.cohorts;

import com.ibm.airlock.Constants;
import com.ibm.airlock.admin.airlytics.JobStatus;
import org.apache.wink.json4j.JSONException;
import org.apache.wink.json4j.JSONObject;

import java.util.Objects;

public class StatusDetails {
    JobStatus status;
    private String activityId;
    private String detailedMessage;
    private Long failedImports;
    private Long parsedImports;
    private Long successfulImports;
    private Long totalImports;

    public StatusDetails(JSONObject input) throws JSONException {
        if (input.containsKey(Constants.JSON_FIELD_COHORT_STATUS_ACTIVITY_ID) && input.get(Constants.JSON_FIELD_COHORT_STATUS_ACTIVITY_ID) != null) {
            activityId = input.getString(Constants.JSON_FIELD_COHORT_STATUS_ACTIVITY_ID);
        }
        if (input.containsKey(Constants.JSON_FIELD_COHORT_STATUS_DETAILED_MESSAGE) && input.get(Constants.JSON_FIELD_COHORT_STATUS_DETAILED_MESSAGE) != null) {
            detailedMessage = input.getString(Constants.JSON_FIELD_COHORT_STATUS_DETAILED_MESSAGE);
        }
        if (input.containsKey(Constants.JSON_FIELD_COHORT_STATUS_FAILED_IMPORTS) && input.get(Constants.JSON_FIELD_COHORT_STATUS_FAILED_IMPORTS) != null) {
            failedImports = input.getLong(Constants.JSON_FIELD_COHORT_STATUS_FAILED_IMPORTS);
        }
        if (input.containsKey(Constants.JSON_FIELD_COHORT_STATUS_PARSED_IMPORTS) && input.get(Constants.JSON_FIELD_COHORT_STATUS_PARSED_IMPORTS) != null) {
            parsedImports = input.getLong(Constants.JSON_FIELD_COHORT_STATUS_PARSED_IMPORTS);
        }
        if (input.containsKey(Constants.JSON_FIELD_COHORT_STATUS_SUCCESFUL_IMPORTS) && input.get(Constants.JSON_FIELD_COHORT_STATUS_SUCCESFUL_IMPORTS) != null) {
            successfulImports = input.getLong(Constants.JSON_FIELD_COHORT_STATUS_SUCCESFUL_IMPORTS);
        }
        if (input.containsKey(Constants.JSON_FIELD_COHORT_STATUS_TOTAL_IMPORTS) && input.get(Constants.JSON_FIELD_COHORT_STATUS_TOTAL_IMPORTS) != null) {
            totalImports = input.getLong(Constants.JSON_FIELD_COHORT_STATUS_TOTAL_IMPORTS);
        }
        if (input.containsKey(Constants.JSON_FIELD_COHORT_STATUS) && input.get(Constants.JSON_FIELD_COHORT_STATUS) != null) {
            status = JobStatus.fromString(input.getString(Constants.JSON_FIELD_COHORT_STATUS));
        }
    }

    public JSONObject toJson () throws JSONException {
        JSONObject res = new JSONObject();

        res.put(Constants.JSON_FIELD_COHORT_STATUS_ACTIVITY_ID, activityId==null?null:activityId);
        res.put(Constants.JSON_FIELD_COHORT_STATUS_DETAILED_MESSAGE, detailedMessage==null?null:detailedMessage);
        res.put(Constants.JSON_FIELD_COHORT_STATUS_FAILED_IMPORTS, failedImports==null?null:failedImports);
        res.put(Constants.JSON_FIELD_COHORT_STATUS_PARSED_IMPORTS, parsedImports==null?null:parsedImports);
        res.put(Constants.JSON_FIELD_COHORT_STATUS_SUCCESFUL_IMPORTS, successfulImports==null?null:successfulImports);
        res.put(Constants.JSON_FIELD_COHORT_STATUS_TOTAL_IMPORTS, totalImports==null?null:totalImports);
        res.put(Constants.JSON_FIELD_COHORT_STATUS, status==null?null:status.toString());

        return res;
    }

    public String getActivityId() {
        return activityId;
    }

    public void setActivityId(String activityId) {
        this.activityId = activityId;
    }

    public String getDetailedMessage() {
        return detailedMessage;
    }

    public void setDetailedMessage(String detailedMessage) {
        this.detailedMessage = detailedMessage;
    }

    public Long getFailedImports() {
        return failedImports;
    }

    public void setFailedImports(Long failedImports) {
        this.failedImports = failedImports;
    }

    public Long getParsedImports() {
        return parsedImports;
    }

    public void setParsedImports(Long parsedImports) {
        this.parsedImports = parsedImports;
    }

    public Long getSuccessfulImports() {
        return successfulImports;
    }

    public void setSuccessfulImports(Long successfulImports) {
        this.successfulImports = successfulImports;
    }

    public Long getTotalImports() {
        return totalImports;
    }

    public void setTotalImports(Long totalImports) {
        this.totalImports = totalImports;
    }

    public JobStatus getStatus() {
        return status;
    }

    public void setStatus(JobStatus status) {
        this.status = status;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof StatusDetails)) return false;
        StatusDetails other = (StatusDetails) obj;
        return Objects.equals(this.activityId, other.activityId) &&
                Objects.equals(this.detailedMessage, other.detailedMessage) &&
                Objects.equals(this.failedImports, other.failedImports) &&
                Objects.equals(this.parsedImports, other.parsedImports) &&
                Objects.equals(this.successfulImports, other.successfulImports) &&
                Objects.equals(this.totalImports, other.totalImports) &&
                Objects.equals(this.status, other.status);
    }

}
