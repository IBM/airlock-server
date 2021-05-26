package com.ibm.airlock.admin.cohorts;

import com.ibm.airlock.Constants;
import org.apache.wink.json4j.JSONArray;
import org.apache.wink.json4j.JSONException;
import org.apache.wink.json4j.JSONObject;

import javax.servlet.ServletContext;
import java.util.*;

public class AirlockCohorts {

    private UUID productId;
    private List<CohortItem> cohorts = new LinkedList<>();
    private Date lastModified = new Date();

    public AirlockCohorts(UUID productId) {
        this.productId = productId;
    }

    public List<CohortItem> getCohorts() {
        return cohorts;
    }

    public void setCohorts(List<CohortItem> cohorts) {
        this.cohorts = cohorts;
    }

    public JSONObject toJson() throws JSONException {
        JSONObject res = new JSONObject();
        JSONArray cohortsArr = new JSONArray();
        for (CohortItem cohortItem : cohorts) {
            JSONObject auObj = cohortItem.toJson();
            cohortsArr.add(auObj);
        }
        res.put(Constants.JSON_FIELD_PRODUCT_ID, productId.toString());
        res.put(Constants.JSON_FIELD_COHORTS, cohortsArr);
        res.put(Constants.JSON_FIELD_LAST_MODIFIED, lastModified.getTime());

        return res;
    }

    public void fromJSON(JSONObject input, ServletContext context) throws JSONException {

        if (input.containsKey(Constants.JSON_FIELD_PRODUCT_ID) && input.get(Constants.JSON_FIELD_PRODUCT_ID) != null) {
            String sStr = input.getString(Constants.JSON_FIELD_PRODUCT_ID);
            productId = UUID.fromString(sStr);
        }

        if (input.containsKey(Constants.JSON_FIELD_LAST_MODIFIED) && input.get(Constants.JSON_FIELD_LAST_MODIFIED)!=null) {
            long timeInMS = (Long)input.get(Constants.JSON_FIELD_LAST_MODIFIED);
            lastModified = new Date(timeInMS);
        }  else {
            lastModified = new Date();
        }

        if (input.containsKey(Constants.JSON_FIELD_COHORTS) && input.get(Constants.JSON_FIELD_COHORTS) != null) {
            cohorts.clear();

            @SuppressWarnings("unchecked")
            Map<String, CohortItem> cohortsDB = (Map<String, CohortItem>)context.getAttribute(Constants.COHORTS_DB_PARAM_NAME);

            JSONArray cohortsArr = input.getJSONArray(Constants.JSON_FIELD_COHORTS); //after validation - i know it is json array
            for (int i=0; i<cohortsArr.size(); i++) {
                JSONObject cohortJsonObj = cohortsArr.getJSONObject(i); //after validation - i know it is json object
                CohortItem cohort = new CohortItem(productId);
                cohort.fromJSON(cohortJsonObj);
                cohorts.add(cohort);
                cohortsDB.put(cohort.getUniqueId().toString(), cohort); //this function is only called when server initialized - hence added to experiments db
            }

        }
    }

    public UUID getProductId() {
        return productId;
    }

    public void setProductId(UUID productId) {
        this.productId = productId;
    }

    public void addCohort(CohortItem cohort) {
        cohorts.add(cohort);
        lastModified = new Date();
    }

    public Date getLastModified() {
        return lastModified;
    }

    public void setLastModified(Date lastModified) {
        this.lastModified = lastModified;
    }

    public String removeCohort(UUID cohortId) {
        for (int i=0; i< cohorts.size(); i++) {
            if (cohorts.get(i).getUniqueId().equals(cohortId)) {
                cohorts.remove(i);
                lastModified = new Date();
                return null;
            }
        }

        return "Unable to remove cohort " + cohortId.toString() + ": The specified cohort does not exist in this product.";
    }

    public String updateFromJSON(JSONObject input) throws JSONException {
        String updateDetails = "";
        if (input.containsKey(Constants.JSON_FIELD_COHORTS) && input.get(Constants.JSON_FIELD_COHORTS) != null) {
            List<CohortItem> updatedCohorts = new ArrayList<>();
            JSONArray cohortsArr = input.getJSONArray(Constants.JSON_FIELD_COHORTS); //after validation - i know it is json array
            for (int i=0; i<cohortsArr.size(); i++) {
                JSONObject cohortJsonObj = cohortsArr.getJSONObject(i); //after validation - i know it is json object
                CohortItem cohort = new CohortItem(productId);
                cohort.fromJSON(cohortJsonObj);
                updatedCohorts.add(cohort);
            }
            JSONArray updatedObj = cohortsToJSON(updatedCohorts);
            JSONArray currentObj = cohortsToJSON(cohorts);
            if (!updatedObj.toString().equals(currentObj.toString())) {
                updateDetails = "'cohorts' was changed from "+currentObj.toString()+" to "+ updatedObj.toString();
                cohorts = updatedCohorts;
            }
        }
        return updateDetails;
    }

    private JSONArray cohortsToJSON(List<CohortItem> cohorts) throws JSONException {
        JSONArray toRet = new JSONArray();
        if (cohorts == null) return toRet;
        for (CohortItem item : cohorts) {
            toRet.add(item.toJson());
        }
        return toRet;
    }
}
