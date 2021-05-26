package com.ibm.airlock.admin.cohorts;

import com.ibm.airlock.Constants;
import org.apache.wink.json4j.JSONException;
import org.apache.wink.json4j.JSONObject;

public class AirCohortsExportStatusResponse extends AirCohortsResponse{

    private StatusDetails airlyticsStatusDetails;
    private StatusDetails thirdPartyStatusDetails;
    private String exportKey; // Localytics, Amplitude...

    public AirCohortsExportStatusResponse(JSONObject input) throws JSONException {
        super(input);
        if (input.containsKey(Constants.JSON_FIELD_COHORT_AIRLYTICS_STATUS_DETAILS) && input.get(Constants.JSON_FIELD_COHORT_AIRLYTICS_STATUS_DETAILS)!=null)
            airlyticsStatusDetails = new StatusDetails(input.getJSONObject(Constants.JSON_FIELD_COHORT_AIRLYTICS_STATUS_DETAILS));

        if (input.containsKey(Constants.JSON_FIELD_COHORT_THIRD_PARTY_STATUS_DETAILS) && input.get(Constants.JSON_FIELD_COHORT_THIRD_PARTY_STATUS_DETAILS)!=null)
            thirdPartyStatusDetails = new StatusDetails(input.getJSONObject(Constants.JSON_FIELD_COHORT_THIRD_PARTY_STATUS_DETAILS));

        if (input.containsKey(Constants.JSON_FIELD_COHORT_EXPORT_KEY) && input.get(Constants.JSON_FIELD_COHORT_EXPORT_KEY)!=null)
            exportKey = input.getString(Constants.JSON_FIELD_COHORT_EXPORT_KEY).trim();
    }
    @Override
    public JSONObject toJson () throws JSONException {
        JSONObject res = super.toJson();

        res.put(Constants.JSON_FIELD_COHORT_AIRLYTICS_STATUS_DETAILS, airlyticsStatusDetails==null?null:airlyticsStatusDetails.toJson());
        res.put(Constants.JSON_FIELD_COHORT_THIRD_PARTY_STATUS_DETAILS, thirdPartyStatusDetails==null?null:thirdPartyStatusDetails.toJson());
        res.put(Constants.JSON_FIELD_COHORT_EXPORT_KEY, exportKey);

        return res;
    }

    public String getExportKey() {
        return exportKey;
    }

    public void setExportKey(String exportKey) {
        this.exportKey = exportKey;
    }

    public StatusDetails getAirlyticsStatusDetails() {
        return airlyticsStatusDetails;
    }

    public void setAirlyticsStatusDetails(StatusDetails airlyticsStatusDetails) {
        this.airlyticsStatusDetails = airlyticsStatusDetails;
    }

    public StatusDetails getThirdPartyStatusDetails() {
        return thirdPartyStatusDetails;
    }

    public void setThirdPartyStatusDetails(StatusDetails thirdPartyStatusDetails) {
        this.thirdPartyStatusDetails = thirdPartyStatusDetails;
    }

}
