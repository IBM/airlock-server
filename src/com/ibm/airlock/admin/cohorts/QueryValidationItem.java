package com.ibm.airlock.admin.cohorts;

import com.ibm.airlock.Constants;
import com.ibm.airlock.Strings;
import com.ibm.airlock.admin.Utilities;
import com.ibm.airlock.admin.ValidationResults;
import com.ibm.airlock.admin.authentication.UserInfo;
import org.apache.wink.json4j.JSONArray;
import org.apache.wink.json4j.JSONException;
import org.apache.wink.json4j.JSONObject;

import javax.servlet.ServletContext;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

public class QueryValidationItem {

    private UUID productId = null;
    private String queryCondition = null;
    private String queryAdditionalValue = null;
    private List<String> joinedTables = null;
    private String exportType;



    public String getQueryCondition() {
        return queryCondition;
    }

    public void setQueryCondition(String queryCondition) {
        this.queryCondition = queryCondition;
    }


    public UUID getProductId() {
        return productId;
    }

    public void setProductId(UUID productId) {
        this.productId = productId;
    }

    public QueryValidationItem(UUID productId) {
        this.productId=productId;
    }

    public void fromJSON(JSONObject input) throws JSONException {
        if (input.containsKey(Constants.JSON_FIELD_PRODUCT_ID) && input.get(Constants.JSON_FIELD_PRODUCT_ID) != null) {
            String sStr = input.getString(Constants.JSON_FIELD_PRODUCT_ID);
            productId = UUID.fromString(sStr);
        }

        if (input.containsKey(Constants.JSON_FIELD_COHORT_QUERY_ADDITIONAL_VALUE) && input.get(Constants.JSON_FIELD_COHORT_QUERY_ADDITIONAL_VALUE)!=null)
            queryAdditionalValue = input.getString(Constants.JSON_FIELD_COHORT_QUERY_ADDITIONAL_VALUE).trim();

        if (input.containsKey(Constants.JSON_FIELD_COHORT_QUERY) && input.get(Constants.JSON_FIELD_COHORT_QUERY)!=null)
            queryCondition = input.getString(Constants.JSON_FIELD_COHORT_QUERY).trim();

        if (input.containsKey(Constants.JSON_FIELD_JOINED_TABLES) && input.get(Constants.JSON_FIELD_JOINED_TABLES)!=null)
            joinedTables = this.jsonArrayToList(input.getJSONArray(Constants.JSON_FIELD_JOINED_TABLES));

        if (input.containsKey(Constants.JSON_FIELD_EXPORT_TYPE) && input.get(Constants.JSON_FIELD_EXPORT_TYPE)!=null)
            exportType = input.getString(Constants.JSON_FIELD_EXPORT_TYPE);
    }

    public JSONObject toJson () throws JSONException {
        JSONObject res = new JSONObject();

        res.put(Constants.JSON_FIELD_PRODUCT_ID, productId==null?null:productId.toString());

        res.put(Constants.JSON_FIELD_COHORT_QUERY, queryCondition);
        res.put(Constants.JSON_FIELD_JOINED_TABLES,joinedTables != null ? new JSONArray(joinedTables) : null);
        res.put(Constants.JSON_FIELD_COHORT_QUERY_ADDITIONAL_VALUE, queryAdditionalValue);
        res.put(Constants.JSON_FIELD_EXPORT_TYPE, exportType);

        return res;
    }

    public ValidationResults validateItemJSON(JSONObject itemJSON) throws JSONException {
        try {
            if (!itemJSON.containsKey(Constants.JSON_FIELD_COHORT_QUERY) || itemJSON.getString(Constants.JSON_FIELD_COHORT_QUERY) == null) {
                return new ValidationResults(String.format(Strings.fieldIsMissing, Constants.JSON_FIELD_COHORT_QUERY), Response.Status.BAD_REQUEST);
            }

            if (!itemJSON.containsKey(Constants.JSON_FIELD_JOINED_TABLES) || itemJSON.get(Constants.JSON_FIELD_JOINED_TABLES) == null) {
                return new ValidationResults(String.format(Strings.fieldIsMissing, Constants.JSON_FIELD_JOINED_TABLES), Response.Status.BAD_REQUEST);
            }

            if (!itemJSON.containsKey(Constants.JSON_FIELD_PRODUCT_ID) || itemJSON.getString(Constants.JSON_FIELD_PRODUCT_ID) == null) {
                return new ValidationResults(String.format(Strings.fieldIsMissing, Constants.JSON_FIELD_PRODUCT_ID), Response.Status.BAD_REQUEST);
            }

            if (!itemJSON.containsKey(Constants.JSON_FIELD_EXPORT_TYPE) || itemJSON.getString(Constants.JSON_FIELD_EXPORT_TYPE) == null) {
                return new ValidationResults(String.format(Strings.fieldIsMissing, Constants.JSON_FIELD_EXPORT_TYPE), Response.Status.BAD_REQUEST);
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

    public String getExportType() {
        return exportType;
    }

    public void setExportType(String exportType) {
        this.exportType = exportType;
    }
}
