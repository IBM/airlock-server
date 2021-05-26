package com.ibm.airlock.admin.dataimport;

import com.ibm.airlock.Constants;
import com.ibm.airlock.admin.cohorts.CohortItem;
import org.apache.wink.json4j.JSONArray;
import org.apache.wink.json4j.JSONException;
import org.apache.wink.json4j.JSONObject;

import javax.servlet.ServletContext;
import java.util.*;

public class AirlyticsDataImport {

    private Long pruneThreshold = null;
    private UUID productId;
    private List<DataImportItem> jobs = new LinkedList<>();
    private Date lastModified = new Date();

    public AirlyticsDataImport(UUID uniqueId) {
        productId = uniqueId;
    }

    public Long getPruneThreshold() {
        return pruneThreshold;
    }

    public void setPruneThreshold(Long pruneThreshold) {
        this.pruneThreshold = pruneThreshold;
    }

    public UUID getProductId() {
        return productId;
    }

    public void setProductId(UUID productId) {
        this.productId = productId;
    }

    public List<DataImportItem> getJobs() {
        return jobs;
    }

    public void setJobs(List<DataImportItem> jobs) {
        this.jobs = jobs;
    }

    public Date getLastModified() {
        return lastModified;
    }

    public void setLastModified(Date lastModified) {
        this.lastModified = lastModified;
    }

    public JSONObject toJson() throws JSONException {
        JSONObject res = new JSONObject();
        JSONArray jobsArr = new JSONArray();
        for (DataImportItem dataImportItem : jobs) {
            JSONObject auObj = dataImportItem.toJson();
            jobsArr.add(auObj);
        }
        res.put(Constants.JSON_FIELD_PRODUCT_ID, productId.toString());
        res.put(Constants.JSON_FIELD_JOBS, jobsArr);
        res.put(Constants.JSON_FIELD_DATA_IMPORT_PRUNE_THRESHOLD, pruneThreshold);
        res.put(Constants.JSON_FIELD_LAST_MODIFIED, lastModified.getTime());

        return res;
    }

    public void fromJSON(JSONObject input, ServletContext context) throws JSONException {

        if (input.containsKey(Constants.JSON_FIELD_DATA_IMPORT_PRUNE_THRESHOLD) && input.get(Constants.JSON_FIELD_DATA_IMPORT_PRUNE_THRESHOLD)!=null) {
            pruneThreshold = input.getLong(Constants.JSON_FIELD_DATA_IMPORT_PRUNE_THRESHOLD);
        }

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

        if (input.containsKey(Constants.JSON_FIELD_JOBS) && input.get(Constants.JSON_FIELD_JOBS) != null) {
            jobs.clear();

            @SuppressWarnings("unchecked")
            Map<String, DataImportItem> jobsDB = (Map<String, DataImportItem>)context.getAttribute(Constants.DATA_IMPORT_DB_PARAM_NAME);

            JSONArray jobsArr = input.getJSONArray(Constants.JSON_FIELD_JOBS); //after validation - i know it is json array
            for (int i=0; i<jobsArr.size(); i++) {
                JSONObject jobJsonObj = jobsArr.getJSONObject(i); //after validation - i know it is json object
                DataImportItem job = new DataImportItem(productId);
                job.fromJSON(jobJsonObj);
                jobs.add(job);
                jobsDB.put(job.getUniqueId().toString(), job); //this function is only called when server initialized - hence added to experiments db
            }

        }
    }

    public void addJob(DataImportItem job) {
        jobs.add(job);
        lastModified = new Date();
    }

    public String removeJob(UUID jobId) {
        for (int i=0; i< jobs.size(); i++) {
            if (jobs.get(i).getUniqueId().equals(jobId)) {
                jobs.remove(i);
                lastModified = new Date();
                return null;
            }
        }

        return "Unable to remove job " + jobId.toString() + ": The specified job does not exist in this product.";
    }

    public String updateFromJSON(JSONObject input) throws JSONException {
        String updateDetails = "";
        if (input.containsKey(Constants.JSON_FIELD_DATA_IMPORT_PRUNE_THRESHOLD) && input.get(Constants.JSON_FIELD_DATA_IMPORT_PRUNE_THRESHOLD) != null) {
            if (pruneThreshold==null || input.getLong(Constants.JSON_FIELD_DATA_IMPORT_PRUNE_THRESHOLD) != pruneThreshold.longValue()) {
                updateDetails = "'pruneThreshold' was changed from "+pruneThreshold+" to "+ input.getLong(Constants.JSON_FIELD_DATA_IMPORT_PRUNE_THRESHOLD);
                pruneThreshold = input.getLong(Constants.JSON_FIELD_DATA_IMPORT_PRUNE_THRESHOLD);
            }
        }
        if (input.containsKey(Constants.JSON_FIELD_JOBS) && input.get(Constants.JSON_FIELD_JOBS) != null) {
            List<DataImportItem> updatedJobs = new ArrayList<>();
            JSONArray jobsArr = input.getJSONArray(Constants.JSON_FIELD_JOBS); //after validation - i know it is json array
            for (int i=0; i<jobsArr.size(); i++) {
                JSONObject jobJsonObj = jobsArr.getJSONObject(i); //after validation - i know it is json object
                DataImportItem item = new DataImportItem(productId);
                item.fromJSON(jobJsonObj);
                updatedJobs.add(item);
            }
            JSONArray updatedObj = jobsToJSON(updatedJobs);
            JSONArray currentObj = jobsToJSON(jobs);
            if (!updatedObj.toString().equals(currentObj.toString())) {
                updateDetails = "'jobs' was changed from "+currentObj.toString()+" to "+ updatedObj.toString();
                jobs = updatedJobs;
            }
        }
        return updateDetails;
    }

    private JSONArray jobsToJSON(List<DataImportItem> jobs) throws JSONException {
        JSONArray toRet = new JSONArray();
        if (jobs == null) return toRet;
        for (DataImportItem item : jobs) {
            toRet.add(item.toJson());
        }
        return toRet;
    }
}
