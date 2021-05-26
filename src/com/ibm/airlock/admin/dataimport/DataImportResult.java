package com.ibm.airlock.admin.dataimport;

import com.ibm.airlock.Constants;
import com.ibm.airlock.admin.airlytics.JobStatus;
import org.apache.wink.json4j.JSONException;
import org.apache.wink.json4j.JSONObject;

public class DataImportResult {
    private JobStatus status;
    private String statusMessage;
    private String detailedMessage;
    private Long successfulImports;

    public DataImportResult(JSONObject input) throws JSONException {
        if (input.containsKey(Constants.JSON_FIELD_DATA_IMPORT_STATUS) && input.get(Constants.JSON_FIELD_DATA_IMPORT_STATUS)!=null)
            status = JobStatus.fromString(input.getString(Constants.JSON_FIELD_DATA_IMPORT_STATUS).trim());

        if (input.containsKey(Constants.JSON_FIELD_DATA_IMPORT_STATUS_MESSAGE) && input.get(Constants.JSON_FIELD_DATA_IMPORT_STATUS_MESSAGE)!=null)
            statusMessage = input.getString(Constants.JSON_FIELD_DATA_IMPORT_STATUS_MESSAGE).trim();

        if (input.containsKey(Constants.JSON_FIELD_DATA_IMPORT_STATUS_DETAILS) && input.get(Constants.JSON_FIELD_DATA_IMPORT_STATUS_DETAILS)!=null)
            detailedMessage = input.getString(Constants.JSON_FIELD_DATA_IMPORT_STATUS_DETAILS).trim();

        if (input.containsKey(Constants.JSON_FIELD_DATA_IMPORT_STATUS_SUCCESFUL_IMPORTS) && input.get(Constants.JSON_FIELD_DATA_IMPORT_STATUS_SUCCESFUL_IMPORTS)!=null)
            successfulImports = input.getLong(Constants.JSON_FIELD_DATA_IMPORT_STATUS_SUCCESFUL_IMPORTS);
    }

    public JSONObject toJson () throws JSONException {
        JSONObject res = new JSONObject();

        res.put(Constants.JSON_FIELD_DATA_IMPORT_STATUS, status==null?null:status.toString());
        res.put(Constants.JSON_FIELD_DATA_IMPORT_STATUS_MESSAGE, statusMessage==null?null:statusMessage);
        res.put(Constants.JSON_FIELD_DATA_IMPORT_STATUS_DETAILS, detailedMessage==null?null:detailedMessage);
        res.put(Constants.JSON_FIELD_DATA_IMPORT_STATUS_SUCCESFUL_IMPORTS, successfulImports==null?null:successfulImports);

        return res;
    }

    public JobStatus getStatus() {
        return status;
    }

    public String getStatusMessage() {
        return statusMessage;
    }

    public String getDetailedMessage() {
        return detailedMessage;
    }

    public Long getSuccessfulImports() {
        return successfulImports;
    }


}
