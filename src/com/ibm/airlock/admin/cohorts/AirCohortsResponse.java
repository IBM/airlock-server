package com.ibm.airlock.admin.cohorts;

import com.ibm.airlock.Constants;
import com.ibm.airlock.admin.airlytics.JobStatus;
import org.apache.wink.json4j.JSONException;
import org.apache.wink.json4j.JSONObject;

public class AirCohortsResponse {
    private JobStatus status;
    private String statusMessage;
    private Long usersNumber;
    private Integer retriesNumber = 0;

    public AirCohortsResponse(JSONObject input) throws JSONException {
        if (input.containsKey(Constants.JSON_FIELD_COHORT_STATUS) && input.get(Constants.JSON_FIELD_COHORT_STATUS)!=null)
            status = JobStatus.fromString(input.getString(Constants.JSON_FIELD_COHORT_STATUS).trim());

        if (input.containsKey(Constants.JSON_FIELD_COHORT_STATUS_MESSAGE) && input.get(Constants.JSON_FIELD_COHORT_STATUS_MESSAGE)!=null)
            statusMessage = input.getString(Constants.JSON_FIELD_COHORT_STATUS_MESSAGE).trim();

        if (input.containsKey(Constants.JSON_FIELD_COHORT_USERS_NUMBER) && input.get(Constants.JSON_FIELD_COHORT_USERS_NUMBER)!=null)
            usersNumber = input.getLong(Constants.JSON_FIELD_COHORT_USERS_NUMBER);

        if (input.containsKey(Constants.JSON_FIELD_COHORT_RETRIES_NUMBER) && input.get(Constants.JSON_FIELD_COHORT_RETRIES_NUMBER)!=null)
            retriesNumber = input.getInt(Constants.JSON_FIELD_COHORT_RETRIES_NUMBER);

    }

    public JSONObject toJson () throws JSONException {
        JSONObject res = new JSONObject();

        res.put(Constants.JSON_FIELD_COHORT_STATUS, status==null?null:status.toString());
        res.put(Constants.JSON_FIELD_COHORT_STATUS_MESSAGE, statusMessage==null?null:statusMessage);
        res.put(Constants.JSON_FIELD_COHORT_USERS_NUMBER, usersNumber==null?null:usersNumber);
        res.put(Constants.JSON_FIELD_COHORT_RETRIES_NUMBER, retriesNumber==null?null:retriesNumber);

        return res;
    }

    public JobStatus getStatus() {
        return status;
    }

    public void setStatus(JobStatus status) {
        this.status = status;
    }

    public String getStatusMessage() {
        return statusMessage;
    }

    public void setStatusMessage(String statusMessage) {
        this.statusMessage = statusMessage;
    }

    public Long getUsersNumber() {
        return usersNumber;
    }

    public void setUsersNumber(Long usersNumber) {
        this.usersNumber = usersNumber;
    }


    public Integer getRetriesNumber() {
        return retriesNumber;
    }

    public void setRetriesNumber(int retriesNumber) {
        this.retriesNumber = retriesNumber;
    }
}
