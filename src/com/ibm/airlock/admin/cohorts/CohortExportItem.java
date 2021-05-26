package com.ibm.airlock.admin.cohorts;

import com.ibm.airlock.Constants;
import com.ibm.airlock.Strings;
import com.ibm.airlock.admin.Utilities;
import com.ibm.airlock.admin.ValidationResults;
import com.ibm.airlock.admin.airlytics.JobStatus;
import org.apache.wink.json4j.JSONException;
import org.apache.wink.json4j.JSONObject;

import javax.ws.rs.core.Response;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class CohortExportItem {

    private String exportName;
    private JobStatus exportStatus;
    private String exportStatusMessage;
    private StatusDetails exportStatusDetails;
    private Map<String, StatusDetails> statuses = new HashMap<>();
    private Date lastExportTime;

    public String getExportName() {
        return exportName;
    }

    public void setExportName(String exportName) {
        this.exportName = exportName;
    }

    public JobStatus getExportStatus() {
        return exportStatus;
    }

    public void setExportStatus(JobStatus exportStatus) {
        this.exportStatus = exportStatus;
    }

    public String getExportStatusMessage() {
        return exportStatusMessage;
    }

    public void setExportStatusMessage(String exportStatusMessage) {
        this.exportStatusMessage = exportStatusMessage;
    }

    public StatusDetails getExportStatusDetails() {
        return exportStatusDetails;
    }

    public void setExportStatusDetails(StatusDetails exportStatusDetails) {
        this.exportStatusDetails = exportStatusDetails;
    }

    public Date getLastExportTime() {
        return lastExportTime;
    }

    public void setLastExportTime(Date lastExportTime) {
        this.lastExportTime = lastExportTime;
    }


    public void updateExportStatusDetails(StatusDetails exportStatusDetails, boolean isUPS) {
        if (this.exportStatusDetails == null) {
            this.exportStatusDetails = exportStatusDetails;
            return;
        }
        if (exportStatusDetails.getActivityId() != null) {
            this.exportStatusDetails.setActivityId(exportStatusDetails.getActivityId());
        }

        if (exportStatusDetails.getDetailedMessage() != null) {
            this.exportStatusDetails.setDetailedMessage(exportStatusDetails.getDetailedMessage());
        }

        if (exportStatusDetails.getFailedImports() != null) {
            this.exportStatusDetails.setFailedImports(exportStatusDetails.getFailedImports());
        }

        if (exportStatusDetails.getParsedImports() != null) {
            this.exportStatusDetails.setParsedImports(exportStatusDetails.getParsedImports());
        }

        if (exportStatusDetails.getStatus() != null) {
            this.exportStatusDetails.setStatus(exportStatusDetails.getStatus());
        }

        if (exportStatusDetails.getSuccessfulImports() != null) {
            if (isUPS && this.exportStatusDetails.getSuccessfulImports() != null) {
                this.exportStatusDetails.setSuccessfulImports(this.exportStatusDetails.getSuccessfulImports()+exportStatusDetails.getSuccessfulImports());
                if (this.exportStatusDetails.getTotalImports()!= null && this.exportStatusDetails.getSuccessfulImports() >= this.exportStatusDetails.getTotalImports()) {
                    this.exportStatusDetails.setStatus(JobStatus.COMPLETED);
                }
            } else {
                this.exportStatusDetails.setSuccessfulImports(exportStatusDetails.getSuccessfulImports());
            }
        }

        if (exportStatusDetails.getTotalImports() != null) {
            this.exportStatusDetails.setTotalImports(exportStatusDetails.getTotalImports());
        }
    }
    public ValidationResults validateExportItemJSON(JSONObject cohortJSON, boolean exportActive) {
        try {

            boolean hasExportName = false;
            //exportName
            if (!cohortJSON.containsKey(Constants.JSON_FIELD_COHORT_EXPORT_NAME) || cohortJSON.get(Constants.JSON_FIELD_COHORT_EXPORT_NAME) == null) {
                if (exportActive) {
                    return new ValidationResults(String.format(Strings.fieldIsMissing, Constants.JSON_FIELD_COHORT_EXPORT_NAME), Response.Status.BAD_REQUEST);
                }
            } else {
                hasExportName = true;
            }

            if (hasExportName) {
                String expName = cohortJSON.getString(Constants.JSON_FIELD_COHORT_EXPORT_NAME);
                String validateExportNameErr = Utilities.validateExportName(expName);
                if(validateExportNameErr!=null) {
                    return new ValidationResults(validateExportNameErr, Response.Status.BAD_REQUEST);
                }
            }

            //exportStatus
            if (cohortJSON.containsKey(Constants.JSON_FIELD_COHORT_EXPORT_STATUS) && cohortJSON.get(Constants.JSON_FIELD_COHORT_EXPORT_STATUS) != null) {
                if (JobStatus.fromString(cohortJSON.getString(Constants.JSON_FIELD_COHORT_EXPORT_STATUS))==null) {
                    return new ValidationResults(String.format(Strings.fieldStatus, cohortJSON.getString(Constants.JSON_FIELD_COHORT_EXPORT_STATUS)), Response.Status.BAD_REQUEST);
                }
            }

        } catch (JSONException jsne) {
            return new ValidationResults(jsne.getMessage(), Response.Status.BAD_REQUEST);
        }
        return null;
    }

    public void fromJSON(JSONObject input) throws JSONException {

        //exportName
        if (input.containsKey(Constants.JSON_FIELD_COHORT_EXPORT_NAME) && input.get(Constants.JSON_FIELD_COHORT_EXPORT_NAME)!=null)
            exportName = input.getString(Constants.JSON_FIELD_COHORT_EXPORT_NAME).trim();

        //exportStatus
        if (input.containsKey(Constants.JSON_FIELD_COHORT_EXPORT_STATUS) && input.get(Constants.JSON_FIELD_COHORT_EXPORT_STATUS)!=null)
            exportStatus = JobStatus.fromString(input.getString(Constants.JSON_FIELD_COHORT_EXPORT_STATUS).trim());

        //exportStatusMessage
        if (input.containsKey(Constants.JSON_FIELD_COHORT_EXPORT_STATUS_MESSAGE) && input.get(Constants.JSON_FIELD_COHORT_EXPORT_STATUS_MESSAGE)!=null)
            exportStatusMessage = input.getString(Constants.JSON_FIELD_COHORT_EXPORT_STATUS_MESSAGE).trim();

        //exportStatusDetails
        if (input.containsKey(Constants.JSON_FIELD_COHORT_EXPORT_STATUS_DETAILS) && input.get(Constants.JSON_FIELD_COHORT_EXPORT_STATUS_DETAILS)!=null)
            exportStatusDetails = new StatusDetails(input.getJSONObject(Constants.JSON_FIELD_COHORT_EXPORT_STATUS_DETAILS));

        if (input.containsKey(Constants.JSON_FIELD_COHORT_LAST_EXPORT_TIME) && input.get(Constants.JSON_FIELD_COHORT_LAST_EXPORT_TIME)!=null) {
            long timeInMS = input.getLong(Constants.JSON_FIELD_COHORT_LAST_EXPORT_TIME);
            lastExportTime = new Date(timeInMS);
        }

        if (input.containsKey(Constants.JSON_FIELD_COHORT_EXPORT_STATUSES) && input.get(Constants.JSON_FIELD_COHORT_EXPORT_STATUSES)!=null) {
            JSONObject statusesObj = input.getJSONObject(Constants.JSON_FIELD_COHORT_EXPORT_STATUSES);
            statuses = getStatusesMap(statusesObj);
        }
    }

    private Map<String, StatusDetails> getStatusesMap(JSONObject statusesObj) throws JSONException {
        Map<String, StatusDetails> toRet = new HashMap<String, StatusDetails>();
        for (Object key : statusesObj.keySet()) {
            String activityId = (String) key;
            JSONObject statusJSON = statusesObj.getJSONObject(activityId);
            StatusDetails item = new StatusDetails(statusJSON);
            toRet.put(activityId, item);
        }
        return toRet;
    }

    private JSONObject getStatusesJson() throws JSONException {
        if (statuses==null) return null;
        JSONObject statusesObj = new JSONObject();
        for (Map.Entry<String, StatusDetails> entry : statuses.entrySet()) {
            JSONObject statusObj = entry.getValue().toJson();
            statusesObj.put(entry.getKey(), statusObj);
        }
        return statusesObj;
    }

    public JSONObject toJson () throws JSONException {
        JSONObject res = new JSONObject();

        res.put(Constants.JSON_FIELD_COHORT_EXPORT_NAME, exportName);
        res.put(Constants.JSON_FIELD_COHORT_EXPORT_STATUS, exportStatus != null ? exportStatus.toString(): null);
        res.put(Constants.JSON_FIELD_COHORT_EXPORT_STATUS_MESSAGE, exportStatusMessage);
        res.put(Constants.JSON_FIELD_COHORT_EXPORT_STATUS_DETAILS, exportStatusDetails != null ? exportStatusDetails.toJson() : null);
        res.put(Constants.JSON_FIELD_COHORT_LAST_EXPORT_TIME, lastExportTime != null ? lastExportTime.getTime() : null);
        res.put(Constants.JSON_FIELD_COHORT_EXPORT_STATUSES, getStatusesJson());
        return res;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        CohortExportItem that = (CohortExportItem) o;

        if (exportName != null ? !exportName.equals(that.exportName) : that.exportName != null) return false;
        if (exportStatus != null ? !exportStatus.equals(that.exportStatus) : that.exportStatus != null) return false;
        if (exportStatusMessage != null ? !exportStatusMessage.equals(that.exportStatusMessage) : that.exportStatusMessage != null)
            return false;
        if (exportStatusDetails != null ? !exportStatusDetails.equals(that.exportStatusDetails) : that.exportStatusDetails != null)
            return false;
        if (statuses != null ? !statuses.equals(that.statuses) : that.statuses != null)
            return false;
        return lastExportTime != null ? lastExportTime.equals(that.lastExportTime) : that.lastExportTime == null;
    }

    public Map<String, StatusDetails> getStatuses() {
        return statuses;
    }

    public void setStatuses(Map<String, StatusDetails> statuses) {
        this.statuses = statuses;
    }

    public String updateThirdPartyStatus(StatusDetails thirdPartyStatusDetails) {
        if (thirdPartyStatusDetails.getActivityId() != null) {
            if (this.statuses == null) {
                this.statuses = new HashMap<>();
            }
            this.statuses.put(thirdPartyStatusDetails.getActivityId(), thirdPartyStatusDetails);
        }
        return null;
    }

    public void sumThirdPartyStatuses() {
        if (this.exportStatusDetails!=null && this.statuses!=null) {
            long totalNum = this.exportStatusDetails.getTotalImports();
            boolean hasRunning = false;
            long sumSuccessful = 0;
            long sumFailed = 0;
            for (StatusDetails thirdParty : this.statuses.values()) {
                long currSuccessful = thirdParty.getSuccessfulImports()!=null? thirdParty.getSuccessfulImports() : 0;
                long currFailed = thirdParty.getFailedImports()!=null? thirdParty.getFailedImports() : 0;
                sumSuccessful = sumSuccessful + currSuccessful;
                sumFailed = sumFailed + currFailed;
                if (thirdParty.getStatus()==JobStatus.RUNNING) {
                    hasRunning = true;
                }
            }
            if (totalNum > 0 && (sumSuccessful+sumFailed >= totalNum)) {
                if (hasRunning) {
                    this.exportStatusDetails.setStatus(JobStatus.EXPORTED);
                } else {
                    this.exportStatusDetails.setStatus(JobStatus.COMPLETED);
                }
            }
        }
    }
}
