package com.ibm.airlock.admin.cohorts;

import com.ibm.airlock.Constants;
import com.ibm.airlock.Strings;
import com.ibm.airlock.admin.*;
import com.ibm.airlock.admin.airlytics.JobStatus;
import com.ibm.airlock.admin.airlytics.ValueType;
import com.ibm.airlock.admin.authentication.UserInfo;
import org.apache.wink.json4j.JSONArray;
import org.apache.wink.json4j.JSONException;
import org.apache.wink.json4j.JSONObject;

import javax.servlet.ServletContext;
import javax.ws.rs.core.Response;
import java.util.*;

public class CohortItem {

    private UUID productId = null;
    private UUID uniqueId = null; //nc + u
    private String name = null; //c+u
    private String description = null; //opt in c+u (if missing or null in update don't change)
    private Date creationDate = null; //nc + u (not changed)
    private String creator = null;	//c+u (creator not changed)
    private String queryCondition = null;
    private String queryAdditionalValue = null;
    private ValueType valueType = ValueType.STRING;// adding this
    private JobStatus calculationStatus;
    private String calculationStatusMessage;
    private CohortCalculationFrequency updateFrequency = CohortCalculationFrequency.MANUAL;
    private List<String> joinedTables = null;
    private Date lastModified = null; // required in update. forbidden in create

    private Map<String, CohortExportItem> exports = new HashMap<>();

    private Long usersNumber = null;
    private Integer retriesNumber = 0;

    public Map<String, CohortExportItem> getExports() {
        return exports;
    }

    public void setExports(Map<String, CohortExportItem> exports) {
        this.exports = exports;
    }

    public Long getUsersNumber() {
        return usersNumber;
    }

    public void setUsersNumber(Long usersNumber) {
        this.usersNumber = usersNumber;
    }

    public String updateFromAirCohortsResponse(AirCohortsResponse item, boolean notify) {
        StringBuilder updateDetails = new StringBuilder();
        boolean shouldNotifyExports = false;
        boolean isExportStatus = false;
        if (item instanceof AirCohortsExportStatusResponse) {
            AirCohortsExportStatusResponse exportItem = (AirCohortsExportStatusResponse) item;
            String exportKey = exportItem.getExportKey();
            if (exportKey!=null && (exportItem.getThirdPartyStatusDetails()!=null || exportItem.getAirlyticsStatusDetails()!=null)) {
                isExportStatus = true;
            }
        }
        if (!isExportStatus) {
            boolean didStatusChanges = false;
            //updating calculation and users number
            if (item.getStatus() != null && !Objects.equals(calculationStatus, item.getStatus())) {
                updateDetails.append("'status' changed from " + calculationStatus + " to " + item.getStatus() + ";	");
                calculationStatus = item.getStatus();
                if (calculationStatus == JobStatus.COMPLETED) {
                    shouldNotifyExports = true;
                }
                didStatusChanges = true;
            }
            if (item.getStatusMessage() != null && !Objects.equals(this.calculationStatusMessage, item.getStatusMessage())) {
                updateDetails.append("'calculationStatusMessage' changed from " + calculationStatusMessage + " to " + item.getStatusMessage() + ";	");
                calculationStatusMessage = item.getStatusMessage();
            }

            if (item.getUsersNumber() != null && !Objects.equals(this.usersNumber, item.getUsersNumber())) {
                updateDetails.append("'usersNumber' changed from " + usersNumber + " to " + item.getUsersNumber() + ";	");
                usersNumber = item.getUsersNumber();
            }

            if (item.getRetriesNumber() != null && !Objects.equals(this.retriesNumber, item.getRetriesNumber())) {
                updateDetails.append("'retriesNumber' changed from " + retriesNumber + " to " + item.getRetriesNumber() + ";	");
                retriesNumber = item.getRetriesNumber();
            }

            if (didStatusChanges && item.getStatus()==JobStatus.PENDING &&
                item.getStatusMessage()!=null && item.getStatusMessage().equals("Pending") &&
                item.getUsersNumber()==null) {
                if (exports!= null) {
                    for (Map.Entry<String, CohortExportItem> entry : exports.entrySet()) {
                        String key = entry.getKey();
                        CohortExportItem oldItem = entry.getValue();
                        CohortExportItem newItem = new CohortExportItem();
                        newItem.setExportName(oldItem.getExportName());
                        exports.put(key, newItem);
                    }
                    updateDetails.append("export items got reset");
                }
            }
        }

        if (isExportStatus) {
            AirCohortsExportStatusResponse exportItem = (AirCohortsExportStatusResponse) item;
            String exportKey = exportItem.getExportKey();
            if (exportKey != null) {
                CohortExportItem export = exports.get(exportKey);
                if (export != null) {
                    boolean isUPS = !exportKey.equalsIgnoreCase(Constants.COHORT_EXPORT_TYPE_LOCALYTICS);
                    if (shouldNotifyExports) {
                        export.setLastExportTime(new Date());
                    }
                    if (exportItem.getAirlyticsStatusDetails() != null && !Objects.equals(export.getExportStatusDetails(), exportItem.getAirlyticsStatusDetails())) {
                        updateDetails.append("'exportStatusDetails' of "+exportKey+" was changed from " + export.getExportStatusDetails() + " to " + exportItem.getAirlyticsStatusDetails() + ";	");
                        export.updateExportStatusDetails(exportItem.getAirlyticsStatusDetails(), isUPS);
                    }
                    if (exportItem.getThirdPartyStatusDetails() != null) {
                        String thirdPartyUpdateDetails = export.updateThirdPartyStatus(exportItem.getThirdPartyStatusDetails());
                        if (thirdPartyUpdateDetails!=null && !thirdPartyUpdateDetails.isEmpty()) {
                            updateDetails.append(thirdPartyUpdateDetails);
                        }
                    }
                    //update the completion status if needed
                    if (!isUPS) {
                        export.sumThirdPartyStatuses();
                    }
                }

            }

        }
        if (notify && shouldNotifyExports) {
//            for (CohortExportItem exportItem : exports.values()) {
//
//            }
        }
        return updateDetails.toString();
    }

    public JobStatus getCalculationStatus() {
        return calculationStatus;
    }

    public void setCalculationStatus(JobStatus calculationStatus) {
        this.calculationStatus = calculationStatus;
    }

    public String updateExportName(String exportKey, String newExportName, Date now) {
        StringBuilder updateDetails = new StringBuilder();
        boolean wasChanged = false;
        if (this.exports.containsKey(exportKey)) {
            CohortExportItem exportItem = this.exports.get(exportKey);
            if (!newExportName.equals(exportItem.getExportName())) {
                updateDetails.append("'exportName' of "+exportKey+" was changed from " + exportItem.getExportName() + " to " + newExportName);
                exportItem.setExportName(newExportName);
                wasChanged = true;
            }
        }

        if (wasChanged) {
            lastModified = now;
        }

        return updateDetails.toString();

    }

    public String deleteExport(String exportKey, ServletContext context, Date now) {
        StringBuilder updateDetails = new StringBuilder();
        boolean wasChanged = false;
        if (this.exports.containsKey(exportKey)) {
            this.exports.remove(exportKey);
            updateDetails.append("export with key '"+exportKey+"' was removed");
            wasChanged = true;
        }

        if (wasChanged) {
            lastModified = now;
        }
        return updateDetails.toString();
    }

    public boolean isDummyExport() {
        if (this.getExports()!=null) {
            for (String exportKey : this.getExports().keySet()) {
                if (exportKey.equalsIgnoreCase(Constants.COHORT_EXPORT_TYPE_DB_ONLY)) {
                    return true;
                }
            }
        }
        return false;
    }

    public int getRetriesNumber() {
        return retriesNumber;
    }

    public void setRetriesNumber(int retriesNumber) {
        this.retriesNumber = retriesNumber;
    }

    public ValueType getValueType() {
        return valueType;
    }

    public void setValueType(ValueType valueType) {
        this.valueType = valueType;
    }


    public enum CohortCalculationFrequency {
        MANUAL, HOURLY, DAILY, WEEKLY, MONTHLY;
        public static CohortCalculationFrequency fromString(String str) {
            if (str == null)
                return null;
            if (str.equalsIgnoreCase(MANUAL.toString())) {
                return MANUAL;
            } else if (str.equalsIgnoreCase(HOURLY.toString())) {
                return HOURLY;
            } else if (str.equalsIgnoreCase(DAILY.toString())) {
                return DAILY;
            } else if (str.equalsIgnoreCase(WEEKLY.toString())) {
                return WEEKLY;
            } else if (str.equalsIgnoreCase(MONTHLY.toString())) {
                return MONTHLY;
            }
            return null;
        }
    }


    public Date getLastModified() {
        return lastModified;
    }

    public void setLastModified(Date lastModified) {
        this.lastModified = lastModified;
    }



    public UUID getUniqueId() {
        return uniqueId;
    }

    public void setUniqueId(UUID uniqueId) {
        this.uniqueId = uniqueId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Date getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(Date creationDate) {
        this.creationDate = creationDate;
    }

    public String getCreator() {
        return creator;
    }

    public void setCreator(String creator) {
        this.creator = creator;
    }

    public String getQueryCondition() {
        return queryCondition;
    }

    public void setQueryCondition(String queryCondition) {
        this.queryCondition = queryCondition;
    }

    public CohortCalculationFrequency  getUpdateFrequency() {
        return updateFrequency;
    }

    public void setUpdateFrequency(CohortCalculationFrequency updateFrequency) {
        this.updateFrequency = updateFrequency;
    }

    public UUID getProductId() {
        return productId;
    }

    public void setProductId(UUID productId) {
        this.productId = productId;
    }

    public CohortItem(UUID productId) {
        this.productId=productId;
    }


    public List<String> getJoinedTables() {
        return joinedTables;
    }

    public void setJoinedTables(List<String> joinedTables) {
        this.joinedTables = joinedTables;
    }

    public ValidationResults validateCohortStatusJSON(JSONObject updatedCohortJSON, ServletContext context) throws JSONException {
        //status
        if (updatedCohortJSON.containsKey(Constants.JSON_FIELD_COHORT_STATUS) && updatedCohortJSON.getString(Constants.JSON_FIELD_COHORT_STATUS) != null) {
            if (JobStatus.fromString(updatedCohortJSON.getString(Constants.JSON_FIELD_COHORT_STATUS))==null) {
                return new ValidationResults(String.format(Strings.fieldStatus, updatedCohortJSON.getString(Constants.JSON_FIELD_COHORT_STATUS)), Response.Status.BAD_REQUEST);
            }
        }

        if (updatedCohortJSON.containsKey(Constants.JSON_FIELD_COHORT_USERS_NUMBER) && updatedCohortJSON.get(Constants.JSON_FIELD_COHORT_USERS_NUMBER) != null) {
            try {
                updatedCohortJSON.getLong(Constants.JSON_FIELD_COHORT_USERS_NUMBER);
            } catch (JSONException e) {
                return new ValidationResults(Strings.fieldUsersNumber, Response.Status.BAD_REQUEST);
            }
        }

        if (updatedCohortJSON.containsKey(Constants.JSON_FIELD_COHORT_RETRIES_NUMBER) && updatedCohortJSON.get(Constants.JSON_FIELD_COHORT_RETRIES_NUMBER) != null) {
            try {
                updatedCohortJSON.getLong(Constants.JSON_FIELD_COHORT_RETRIES_NUMBER);
            } catch (JSONException e) {
                return new ValidationResults(Strings.fieldUsersNumber, Response.Status.BAD_REQUEST);
            }
        }
        return null;
    }

    public String updateCohortStatus(JSONObject input, ServletContext context, Date now) throws JSONException {
        AirCohortsExportStatusResponse response = new AirCohortsExportStatusResponse(input);

        String updateDetails = this.updateFromAirCohortsResponse(response, false);
        boolean wasChanged = updateDetails != null && !updateDetails.isEmpty();
        if (wasChanged) {
            lastModified = now;

        }

        return updateDetails;
    }

    public String updateCohort(JSONObject input, ServletContext context, Date now) throws JSONException {
        boolean wasChanged = false;
        StringBuilder updateDetails = new StringBuilder();

        if (input.containsKey(Constants.JSON_FIELD_NAME) && input.get(Constants.JSON_FIELD_NAME)!=null) {
            String updateName = input.getString(Constants.JSON_FIELD_NAME);
            if (!updateName.equals(name)) {
                updateDetails.append("'name' changed from " + name + " to " + updateName + ";	");
                name = updateName;
                wasChanged = true;
            }
        }

        if (input.containsKey(Constants.JSON_FIELD_DESCRIPTION) && input.get(Constants.JSON_FIELD_DESCRIPTION)!=null) {
            String updateDescription = input.getString(Constants.JSON_FIELD_DESCRIPTION);
            if (!updateDescription.equals(description)) {
                updateDetails.append("'description' changed from " + description + " to " + updateDescription + ";	");
                description = updateDescription;
                wasChanged = true;
            }
        }

        if (input.containsKey(Constants.JSON_FIELD_JOINED_TABLES) && input.get(Constants.JSON_FIELD_JOINED_TABLES)!=null) {
            List<String> updateJoinedTablesDetails = this.jsonArrayToList(input.getJSONArray(Constants.JSON_FIELD_JOINED_TABLES));
            if (joinedTables == null || !updateJoinedTablesDetails.equals(joinedTables)) {
                updateDetails.append("'joinedTables' changed from " + joinedTables + " to " + updateJoinedTablesDetails + ";	");
                joinedTables = updateJoinedTablesDetails;
                wasChanged = true;
            }
        }

        if (input.containsKey(Constants.JSON_FIELD_COHORT_USERS_NUMBER) && input.get(Constants.JSON_FIELD_COHORT_USERS_NUMBER)!=null) {
            Long updateUsersNum = input.getLong(Constants.JSON_FIELD_COHORT_USERS_NUMBER);
            if (!updateUsersNum.equals(usersNumber)) {
                updateDetails.append("'usersNumber' changed from " + usersNumber + " to " + updateUsersNum + ";	");
                usersNumber = updateUsersNum;
                wasChanged = true;
            }
        }

        if (input.containsKey(Constants.JSON_FIELD_COHORT_QUERY) && input.get(Constants.JSON_FIELD_COHORT_QUERY)!=null) {
            String updateQueryCondition = input.getString(Constants.JSON_FIELD_COHORT_QUERY);
            if (!updateQueryCondition.equals(queryCondition)) {
                updateDetails.append("'queryCondition' changed from " + queryCondition + " to " + updateQueryCondition + ";	");
                queryCondition = updateQueryCondition;
                wasChanged = true;
            }
        }

        if (input.containsKey(Constants.JSON_FIELD_COHORT_QUERY_ADDITIONAL_VALUE) && input.get(Constants.JSON_FIELD_COHORT_QUERY_ADDITIONAL_VALUE)!=null) {
            String updateAdditionalValue = input.getString(Constants.JSON_FIELD_COHORT_QUERY_ADDITIONAL_VALUE);
            if (!updateAdditionalValue.equals(queryAdditionalValue)) {
                updateDetails.append("'queryAdditionalValue' changed from " + queryAdditionalValue + " to " + updateAdditionalValue + ";	");
                queryAdditionalValue = updateAdditionalValue;
                wasChanged = true;
            }
        }

        if (input.containsKey(Constants.JSON_FIELD_COHORT_FREQUENCY) && input.get(Constants.JSON_FIELD_COHORT_FREQUENCY)!=null) {
            CohortCalculationFrequency updateUpdateFrequency = CohortCalculationFrequency.fromString(input.getString(Constants.JSON_FIELD_COHORT_FREQUENCY));
            if (!updateUpdateFrequency.equals(updateFrequency)) {
                updateDetails.append("'updateFrequency' changed from " + updateFrequency + " to " + updateUpdateFrequency + ";	");
                updateFrequency = updateUpdateFrequency;
                wasChanged = true;
            }
        }

        if (input.containsKey(Constants.JSON_FIELD_COHORT_EXPORTS) && input.get(Constants.JSON_FIELD_COHORT_EXPORTS)!=null) {
            Map<String, CohortExportItem> updatedMap = getExportsMap(input.getJSONObject(Constants.JSON_FIELD_COHORT_EXPORTS));
            if (!exports.equals(updatedMap)) {
                updateDetails.append("'exports' changed from " + exports + " to " + updatedMap + ";	");
                exports = updatedMap;
                wasChanged = true;
            }
        }

        if (wasChanged) {
            lastModified = now;
        }

        return updateDetails.toString();
    }
    public void fromJSON(JSONObject input) throws JSONException {
        if (input.containsKey(Constants.JSON_FIELD_PRODUCT_ID) && input.get(Constants.JSON_FIELD_PRODUCT_ID) != null) {
            String sStr = input.getString(Constants.JSON_FIELD_PRODUCT_ID);
            productId = UUID.fromString(sStr);
        }

        if (input.containsKey(Constants.JSON_FIELD_UNIQUE_ID) && input.get(Constants.JSON_FIELD_UNIQUE_ID) != null) {
            String sStr = input.getString(Constants.JSON_FIELD_UNIQUE_ID);
            uniqueId = UUID.fromString(sStr);
        }

        if (input.containsKey(Constants.JSON_FIELD_NAME) && input.get(Constants.JSON_FIELD_NAME)!=null)
            name = input.getString(Constants.JSON_FIELD_NAME);

        if (input.containsKey(Constants.JSON_FIELD_DESCRIPTION) && input.get(Constants.JSON_FIELD_DESCRIPTION)!=null)
            description = input.getString(Constants.JSON_FIELD_DESCRIPTION).trim();

        if (input.containsKey(Constants.JSON_FIELD_COHORT_USERS_NUMBER) && input.get(Constants.JSON_FIELD_COHORT_USERS_NUMBER)!=null)
            usersNumber = input.getLong(Constants.JSON_FIELD_COHORT_USERS_NUMBER);

        if (input.containsKey(Constants.JSON_FIELD_COHORT_RETRIES_NUMBER) && input.get(Constants.JSON_FIELD_COHORT_RETRIES_NUMBER)!=null)
            retriesNumber = input.getInt(Constants.JSON_FIELD_COHORT_RETRIES_NUMBER);

        if (input.containsKey(Constants.JSON_FIELD_COHORT_QUERY_ADDITIONAL_VALUE) && input.get(Constants.JSON_FIELD_COHORT_QUERY_ADDITIONAL_VALUE)!=null)
            queryAdditionalValue = input.getString(Constants.JSON_FIELD_COHORT_QUERY_ADDITIONAL_VALUE).trim();

        if (input.containsKey(Constants.JSON_FEATURE_FIELD_CREATOR) && input.get(Constants.JSON_FEATURE_FIELD_CREATOR)!=null)
            creator = input.getString(Constants.JSON_FEATURE_FIELD_CREATOR).trim();

        if (input.containsKey(Constants.JSON_FIELD_COHORT_CALCULATION_STATUS) && input.get(Constants.JSON_FIELD_COHORT_CALCULATION_STATUS)!=null)
            calculationStatus = JobStatus.fromString(input.getString(Constants.JSON_FIELD_COHORT_CALCULATION_STATUS).trim());

        if (input.containsKey(Constants.JSON_FIELD_COHORT_VALUE_TYPE) && input.get(Constants.JSON_FIELD_COHORT_VALUE_TYPE)!=null)
            valueType = ValueType.fromString(input.getString(Constants.JSON_FIELD_COHORT_VALUE_TYPE).trim());

        if (input.containsKey(Constants.JSON_FIELD_COHORT_CALCULATION_STATUS_MESSAGE) && input.get(Constants.JSON_FIELD_COHORT_CALCULATION_STATUS_MESSAGE)!=null)
            calculationStatusMessage = input.getString(Constants.JSON_FIELD_COHORT_CALCULATION_STATUS_MESSAGE).trim();

        if (input.containsKey(Constants.JSON_FEATURE_FIELD_CREATION_DATE) && input.get(Constants.JSON_FEATURE_FIELD_CREATION_DATE)!=null) {
            long timeInMS = input.getLong(Constants.JSON_FEATURE_FIELD_CREATION_DATE);
            creationDate = new Date(timeInMS);
        } else {
            creationDate = new Date();
        }


        if (input.containsKey(Constants.JSON_FIELD_LAST_MODIFIED) && input.get(Constants.JSON_FIELD_LAST_MODIFIED)!=null) {
            long timeInMS = input.getLong(Constants.JSON_FIELD_LAST_MODIFIED);
            lastModified = new Date(timeInMS);
        }  else {
            lastModified = new Date();
        }

        if (input.containsKey(Constants.JSON_FIELD_COHORT_QUERY) && input.get(Constants.JSON_FIELD_COHORT_QUERY)!=null)
            queryCondition = input.getString(Constants.JSON_FIELD_COHORT_QUERY).trim();

        if (input.containsKey(Constants.JSON_FIELD_COHORT_FREQUENCY) && input.get(Constants.JSON_FIELD_COHORT_FREQUENCY)!=null)
            updateFrequency = CohortCalculationFrequency.fromString(input.getString(Constants.JSON_FIELD_COHORT_FREQUENCY).trim());

        if (input.containsKey(Constants.JSON_FIELD_COHORT_EXPORTS) && input.get(Constants.JSON_FIELD_COHORT_EXPORTS)!=null) {
            JSONObject exportsObj = input.getJSONObject(Constants.JSON_FIELD_COHORT_EXPORTS);
            exports = getExportsMap(exportsObj);
        }

        if (input.containsKey(Constants.JSON_FIELD_JOINED_TABLES) && input.get(Constants.JSON_FIELD_JOINED_TABLES)!=null)
            joinedTables = this.jsonArrayToList(input.getJSONArray(Constants.JSON_FIELD_JOINED_TABLES));
    }

    private Map<String, CohortExportItem> getExportsMap(JSONObject exportsObj) throws JSONException {
        Map<String, CohortExportItem> toRet = new HashMap<String, CohortExportItem>();
        for (Object key : exportsObj.keySet()) {
            String exportKey = (String) key;
            JSONObject exportJSON = exportsObj.getJSONObject(exportKey);
            CohortExportItem item = new CohortExportItem();
            item.fromJSON(exportJSON);
            toRet.put(exportKey, item);
        }
        return toRet;
    }

    public JSONObject toJson () throws JSONException {
        JSONObject res = new JSONObject();

        res.put(Constants.JSON_FIELD_UNIQUE_ID, uniqueId==null?null:uniqueId.toString());
        res.put(Constants.JSON_FIELD_PRODUCT_ID, productId==null?null:productId.toString());
        res.put(Constants.JSON_FIELD_NAME, name);
        res.put(Constants.JSON_FIELD_DESCRIPTION, description);
        res.put(Constants.JSON_FEATURE_FIELD_CREATION_DATE, creationDate.getTime());
        res.put(Constants.JSON_FEATURE_FIELD_CREATOR, creator);
        res.put(Constants.JSON_FIELD_LAST_MODIFIED, lastModified.getTime());
        res.put(Constants.JSON_FIELD_COHORT_QUERY, queryCondition);
        res.put(Constants.JSON_FIELD_COHORT_FREQUENCY, updateFrequency != null ? updateFrequency.toString() : null);
        res.put(Constants.JSON_FIELD_COHORT_USERS_NUMBER, usersNumber);
        res.put(Constants.JSON_FIELD_COHORT_RETRIES_NUMBER, retriesNumber);
        res.put(Constants.JSON_FIELD_COHORT_QUERY_ADDITIONAL_VALUE, queryAdditionalValue);
        res.put(Constants.JSON_FIELD_COHORT_EXPORTS, getExportsJson());
        res.put(Constants.JSON_FIELD_COHORT_CALCULATION_STATUS, calculationStatus != null ? calculationStatus.toString() : null);
        res.put(Constants.JSON_FIELD_COHORT_VALUE_TYPE, valueType != null ? valueType.getLabel() : null);
        res.put(Constants.JSON_FIELD_COHORT_CALCULATION_STATUS_MESSAGE, calculationStatusMessage);
        res.put(Constants.JSON_FIELD_JOINED_TABLES,joinedTables != null ? new JSONArray(joinedTables) : null);
        return res;
    }

    private JSONObject getExportsJson() throws JSONException {
        if (exports==null) return null;
        JSONObject exportsObj = new JSONObject();
        for (Map.Entry<String, CohortExportItem> entry : exports.entrySet()) {
            JSONObject exportObj = entry.getValue().toJson();
            exportsObj.put(entry.getKey(), exportObj);
        }
        return exportsObj;
    }

    public ValidationResults checkExport() {
        if (this.queryCondition==null || this.queryCondition.isEmpty()) {
            return new ValidationResults(Strings.fieldQueryConditionIsMissing, Response.Status.BAD_REQUEST);
        }
        if (this.exports==null || this.exports.isEmpty()) {
            return new ValidationResults("Empty export item", Response.Status.BAD_REQUEST);
        }
        for (Map.Entry<String, CohortExportItem> entry : exports.entrySet()) {
            boolean isActive = true;
            String exportKey = entry.getKey();
            if (exportKey.equalsIgnoreCase(Constants.COHORT_EXPORT_TYPE_DB_ONLY)) {
                isActive = false;
            }
            CohortExportItem exportItem = entry.getValue();
            if (isActive) {
                if (exportItem.getExportName()==null || exportItem.getExportName().isEmpty()) {
                    return new ValidationResults(String.format(Strings.fieldIsMissing, Constants.JSON_FIELD_COHORT_EXPORT_NAME), Response.Status.BAD_REQUEST);
                }
            }
        }
        return null;
    }
    public ValidationResults validateCohortJSON(JSONObject cohortJSON, ServletContext context, UserInfo userInfo) throws JSONException {
        try {
            Constants.Action action = Constants.Action.ADD;
            if (cohortJSON.containsKey(Constants.JSON_FIELD_UNIQUE_ID) && cohortJSON.get(Constants.JSON_FIELD_UNIQUE_ID)!=null) {
                //if JSON contains uniqueId - update an existing feature otherwise create a new feature
                action = Constants.Action.UPDATE;
            }

            //updateFrequency
            if (!cohortJSON.containsKey(Constants.JSON_FIELD_COHORT_FREQUENCY) || cohortJSON.getString(Constants.JSON_FIELD_COHORT_FREQUENCY) == null) {
                return new ValidationResults(String.format(Strings.fieldIsMissing, Constants.JSON_FIELD_COHORT_FREQUENCY), Response.Status.BAD_REQUEST);
            }
            CohortCalculationFrequency frequency = CohortCalculationFrequency.fromString(cohortJSON.getString(Constants.JSON_FIELD_COHORT_FREQUENCY));
            if (frequency==null) {
                return new ValidationResults(String.format(Strings.fieldFrequency, cohortJSON.getString(Constants.JSON_FIELD_COHORT_FREQUENCY)), Response.Status.BAD_REQUEST);
            }
            boolean active = frequency!=CohortCalculationFrequency.MANUAL;

            String objName = cohortJSON.getString(Constants.JSON_FIELD_NAME);
            String validateNameErr = Utilities.validateName(objName);
            if(validateNameErr!=null) {
                return new ValidationResults(validateNameErr, Response.Status.BAD_REQUEST);
            }

            if (active) {
                if (!cohortJSON.containsKey(Constants.JSON_FIELD_COHORT_QUERY) || cohortJSON.getString(Constants.JSON_FIELD_COHORT_QUERY) == null) {
                    return new ValidationResults(Strings.fieldQueryConditionIsMissing, Response.Status.BAD_REQUEST);
                }
                String queryCond = cohortJSON.getString(Constants.JSON_FIELD_COHORT_QUERY);
                if (queryCond.isEmpty()) {
                    return new ValidationResults(Strings.fieldQueryConditionIsMissing, Response.Status.BAD_REQUEST);
                }
            }
            //usersNumber - if exists, verify its a long
            if (cohortJSON.containsKey(Constants.JSON_FIELD_COHORT_USERS_NUMBER) && cohortJSON.get(Constants.JSON_FIELD_COHORT_USERS_NUMBER) != null) {
                cohortJSON.getLong(Constants.JSON_FIELD_COHORT_USERS_NUMBER);
            }



            //joinedTables
            if (!cohortJSON.containsKey(Constants.JSON_FIELD_JOINED_TABLES) || cohortJSON.get(Constants.JSON_FIELD_JOINED_TABLES) == null) {
                return new ValidationResults(String.format(Strings.fieldIsMissing, Constants.JSON_FIELD_JOINED_TABLES), Response.Status.BAD_REQUEST);
            }

            //creator
            if (!cohortJSON.containsKey(Constants.JSON_FEATURE_FIELD_CREATOR) || cohortJSON.getString(Constants.JSON_FEATURE_FIELD_CREATOR) == null || cohortJSON.getString(Constants.JSON_FEATURE_FIELD_CREATOR).isEmpty()) {
                return new ValidationResults(String.format(Strings.fieldIsMissing, Constants.JSON_FEATURE_FIELD_CREATOR), Response.Status.BAD_REQUEST);
            }

            if (action == Constants.Action.ADD){
                //creation date => should not appear in branch creation
                if (cohortJSON.containsKey(Constants.JSON_FEATURE_FIELD_CREATION_DATE) && cohortJSON.get(Constants.JSON_FEATURE_FIELD_CREATION_DATE)!=null) {
                    return new ValidationResults("The creationDate field cannot be specified during creation.", Response.Status.BAD_REQUEST);
                }
            } else {
                //creation date must appear
                if (!cohortJSON.containsKey(Constants.JSON_FEATURE_FIELD_CREATION_DATE) || cohortJSON.get(Constants.JSON_FEATURE_FIELD_CREATION_DATE)==null) {
                    return new ValidationResults("The creationDate field is missing. This field must be specified during update.", Response.Status.BAD_REQUEST);
                }

                //verify that legal long
                long creationdateLong = cohortJSON.getLong(Constants.JSON_FEATURE_FIELD_CREATION_DATE);

                //verify that was not changed
                if (!creationDate.equals(new Date(creationdateLong))) {
                    return new ValidationResults("creationDate cannot be changed during update", Response.Status.BAD_REQUEST);
                }

                //creator must exist and not be changed
                String creatorStr = cohortJSON.getString(Constants.JSON_FEATURE_FIELD_CREATOR);
                if (!creator.equals(creatorStr)) {
                    return new ValidationResults(String.format(Strings.fieldCannotBeChangedDuringUpdate, Constants.JSON_FEATURE_FIELD_CREATOR), Response.Status.BAD_REQUEST);
                }
            }

            //exports
            String exportType = null;
            if (cohortJSON.containsKey(Constants.JSON_FIELD_COHORT_EXPORTS) && cohortJSON.get(Constants.JSON_FIELD_COHORT_EXPORTS) != null) {
                JSONObject exports = cohortJSON.getJSONObject(Constants.JSON_FIELD_COHORT_EXPORTS);
                CohortExportItem exportItem = new CohortExportItem();
                for (Object key : exports.keySet()) {
                    boolean isActive = active;
                    String exportKey = (String) key;
                    if (exportType==null) {
                        exportType = exportKey;
                    }
                    if (exportKey.equalsIgnoreCase(Constants.COHORT_EXPORT_TYPE_DB_ONLY)) {
                        isActive = false;
                    }
                    JSONObject exportJSON = exports.getJSONObject(exportKey);
                    ValidationResults vr = exportItem.validateExportItemJSON(exportJSON, isActive);
                    if (vr != null) {
                        return vr;
                    }
                }
            }


            //calculationStatus
            if (cohortJSON.containsKey(Constants.JSON_FIELD_COHORT_CALCULATION_STATUS) && cohortJSON.get(Constants.JSON_FIELD_COHORT_CALCULATION_STATUS) != null && !cohortJSON.getString(Constants.JSON_FIELD_COHORT_CALCULATION_STATUS).isEmpty()) {
                if (JobStatus.fromString(cohortJSON.getString(Constants.JSON_FIELD_COHORT_CALCULATION_STATUS).trim())==null) {
                    return new ValidationResults(String.format(Strings.fieldCalculationStatus, cohortJSON.getString(Constants.JSON_FIELD_COHORT_CALCULATION_STATUS).trim()), Response.Status.BAD_REQUEST);
                }
            }

            //valueType
            if (cohortJSON.containsKey(Constants.JSON_FIELD_COHORT_VALUE_TYPE) && cohortJSON.get(Constants.JSON_FIELD_COHORT_VALUE_TYPE) != null && !cohortJSON.getString(Constants.JSON_FIELD_COHORT_VALUE_TYPE).isEmpty()) {
                ValueType valType = ValueType.fromString(cohortJSON.getString(Constants.JSON_FIELD_COHORT_VALUE_TYPE).trim());
                if (valType==null) {
                    return new ValidationResults(String.format(Strings.fieldValueType, cohortJSON.getString(Constants.JSON_FIELD_COHORT_VALUE_TYPE).trim()), Response.Status.BAD_REQUEST);
                }
                if (exportType.equalsIgnoreCase("Amplitude") ||exportType.equalsIgnoreCase("Braze")){
                    if(valType == ValueType.JSON) {
                        return new ValidationResults(String.format(Strings.fieldInvalidValueType, valType.getLabel(), exportType), Response.Status.BAD_REQUEST);
                    }
                }
            }


        } catch (JSONException jsne) {
            return new ValidationResults(jsne.getMessage(), Response.Status.BAD_REQUEST);
        }
        return null;


    }

    private List<String> jsonArrayToList(JSONArray jArray) throws JSONException {
        List<String> listdata = new ArrayList<String>();
        if (jArray != null) {
            for (int i=0;i<jArray.length();i++){
                listdata.add(jArray.getString(i));
            }
        }
        return listdata;
    }

}
