package com.ibm.airlock.admin.cohorts;

import com.ibm.airlock.Constants;
import com.ibm.airlock.Strings;
import com.ibm.airlock.admin.Utilities;
import com.ibm.airlock.admin.ValidationResults;
import org.apache.wink.json4j.JSONArray;
import org.apache.wink.json4j.JSONException;
import org.apache.wink.json4j.JSONObject;

import javax.servlet.ServletContext;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;
import java.util.logging.Logger;

public class CohortsUtilities {

    public static final Logger logger = Logger.getLogger(CohortsUtilities.class.getName());

    public static final int CONNECTION_TIMEOUT_MS = 900000; //5 minutes

    public static AirCohortsCallResult executeCohort(ServletContext context, CohortItem cohort, String productId, String sessionToken) throws Exception {
        String airCohortsApiUrl = getCohortsUrl(context, productId);
        if (airCohortsApiUrl==null || airCohortsApiUrl.isEmpty())
            return null;

        URL url = new URL(airCohortsApiUrl+"/cohorts/"+cohort.getUniqueId().toString()+"/execute");
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setReadTimeout(CONNECTION_TIMEOUT_MS);
        con.setConnectTimeout(CONNECTION_TIMEOUT_MS);
        con.setRequestProperty(Constants.AUTHENTICATION_HEADER, sessionToken);

        con.setRequestMethod("POST");

        // Send post request
        con.setDoOutput(true);

        int code = con.getResponseCode();

        JSONObject responseObject = null;
        AirCohortsResponse cohortsResponse;

        if (code >= 200 && code <= 299) {
            logger.info("execute cohort succeed : " + url);
            return new AirCohortsCallResult(null, null);
        }

        String erroMsgJsonStr = "unknown error";
        if (con.getErrorStream() != null) {
            erroMsgJsonStr = Utilities.streamToString(con.getErrorStream());
        } else {
            erroMsgJsonStr = con.getResponseMessage();
        }

        return new AirCohortsCallResult(new ValidationResults(Strings.failCohortExecute + erroMsgJsonStr, Response.Status.INTERNAL_SERVER_ERROR),null);
    }

    private static String getCohortsUrl(ServletContext context, String productId) {
        Map<String, String> cohortsUrls = (Map<String, String>)context.getAttribute(Constants.COHORTS_SERVER_URL_PARAM_NAME);
        if (cohortsUrls!=null) {
            return cohortsUrls.get(productId);
        }
        return null;
    }
    public static AirCohortsCallResult deleteCohort(ServletContext context, CohortItem cohort, String exportKey, String productId, String sessionToken) throws Exception {
        String airCohortsApiUrl = getCohortsUrl(context, productId);
        if (airCohortsApiUrl==null || airCohortsApiUrl.isEmpty())
            return null;

        URL url = new URL(airCohortsApiUrl+"/cohorts/"+cohort.getUniqueId().toString()+"/delete");
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setReadTimeout(CONNECTION_TIMEOUT_MS);
        con.setConnectTimeout(CONNECTION_TIMEOUT_MS);
        con.setRequestProperty("Content-Type", "application/json; charset=utf-8");
        con.setRequestProperty(Constants.AUTHENTICATION_HEADER, sessionToken);

        con.setRequestMethod("POST");

        // Send post request
        con.setDoOutput(true);
        JSONObject obj = new JSONObject();
        obj.put("cohortId", cohort.getUniqueId().toString());
        obj.put("productId", cohort.getProductId().toString());
        if (exportKey != null && !exportKey.isEmpty()) {
            CohortExportItem item = cohort.getExports().get(exportKey);
            if (item != null) {
                JSONArray deletedExports = new JSONArray();
                JSONObject exportObj = new JSONObject();
                exportObj.put("exportType", exportKey);
                exportObj.put("exportFieldName", item.getExportName());
                deletedExports.add(exportObj);
                obj.put("deletedExports", deletedExports);
            }
        }
        String data = obj.toString();
        con.getOutputStream().write(data.getBytes("UTF-8"));

        int code = con.getResponseCode();

        JSONObject responseObject = null;
        AirCohortsResponse cohortsResponse;

        if (code >= 200 && code <= 299) {
            logger.info("delete cohort succeed : " + url);
            return new AirCohortsCallResult(null, null);
        }

        String erroMsgJsonStr = "unknown error";
        if (con.getErrorStream() != null) {
            erroMsgJsonStr = Utilities.streamToString(con.getErrorStream());
        } else {
            erroMsgJsonStr = con.getResponseMessage();
        }

        return new AirCohortsCallResult(new ValidationResults(Strings.failCohortExecute + erroMsgJsonStr, Response.Status.INTERNAL_SERVER_ERROR),null);
    }

    public static AirCohortsCallResult renameCohortExport(ServletContext context, CohortItem cohort, String exportKey, String oldExportName, String newExportName, String productId, String sessionToken) throws Exception {
        String airCohortsApiUrl = getCohortsUrl(context, productId);
        if (airCohortsApiUrl==null || airCohortsApiUrl.isEmpty())
            return null;

        URL url = new URL(airCohortsApiUrl+"/cohorts/"+cohort.getUniqueId().toString()+"/rename");
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setReadTimeout(CONNECTION_TIMEOUT_MS);
        con.setConnectTimeout(CONNECTION_TIMEOUT_MS);
        con.setRequestProperty("Content-Type", "application/json; charset=utf-8");
        con.setRequestProperty(Constants.AUTHENTICATION_HEADER, sessionToken);

        con.setRequestMethod("POST");

        // Send post request
        con.setDoOutput(true);
        JSONObject obj = new JSONObject();
        obj.put("cohortId", cohort.getUniqueId().toString());
        obj.put("productId", cohort.getProductId().toString());
        obj.put("exportKey", exportKey);
        obj.put("newExportName", newExportName);
        obj.put("oldExportName", oldExportName);
        String data = obj.toString();
        con.getOutputStream().write(data.getBytes("UTF-8"));

        int code = con.getResponseCode();

        JSONObject responseObject = null;
        AirCohortsResponse cohortsResponse;

        if (code >= 200 && code <= 299) {
            logger.info("delete cohort succeed : " + url);
            return new AirCohortsCallResult(null, null);
        }

        String erroMsgJsonStr = "unknown error";
        if (con.getErrorStream() != null) {
            erroMsgJsonStr = Utilities.streamToString(con.getErrorStream());
        } else {
            erroMsgJsonStr = con.getResponseMessage();
        }

        return new AirCohortsCallResult(new ValidationResults(Strings.failCohortExecute + erroMsgJsonStr, Response.Status.INTERNAL_SERVER_ERROR),null);
    }

    public static AirCohortsCallResult validateQuery(ServletContext context, QueryValidationItem item, boolean limit, String sessionToken) throws Exception{
        String airCohortsApiUrl = getCohortsUrl(context,item.getProductId().toString());
        if (airCohortsApiUrl==null || airCohortsApiUrl.isEmpty())
            return null;

        URL url = new URL(airCohortsApiUrl+"/cohorts/validate?limit="+limit);
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setReadTimeout(CONNECTION_TIMEOUT_MS);
        con.setConnectTimeout(CONNECTION_TIMEOUT_MS);
        con.setRequestProperty("Content-Type", "application/json; charset=utf-8");
        con.setRequestProperty(Constants.AUTHENTICATION_HEADER, sessionToken);

        con.setRequestMethod("POST");

        // Send post request
        con.setDoOutput(true);
        String data = item.toJson().toString();
        con.getOutputStream().write(data.getBytes("UTF-8"));
        int code = con.getResponseCode();

        JSONObject responseObject = null;
        AirCohortsResponse cohortsResponse;
        try {
            String resp = Utilities.streamToString(con.getInputStream());
            responseObject = new JSONObject(resp);
            cohortsResponse = new AirCohortsResponse(responseObject);
        } catch (JSONException e) {
            String erroMsgJsonStr = Utilities.streamToString(con.getErrorStream());
            JSONObject erroMsgJson = new JSONObject(erroMsgJsonStr);
            return new AirCohortsCallResult(new ValidationResults(Strings.failQueryValidateExecute + erroMsgJson.getString("error"), Response.Status.INTERNAL_SERVER_ERROR),null);
        }
        if (code >= 200 && code <= 299) {
            logger.info("execute cohort succeed : " + url);
            return new AirCohortsCallResult(null, cohortsResponse);
        }

        String erroMsgJsonStr = Utilities.streamToString(con.getErrorStream());
        JSONObject erroMsgJson = new JSONObject(erroMsgJsonStr);
        return new AirCohortsCallResult(new ValidationResults(Strings.failQueryValidateExecute + erroMsgJson.getString("error"), Response.Status.INTERNAL_SERVER_ERROR),null);
    }

    public static Response getColumnsNames(ServletContext context, String productId, String sessionToken) throws IOException {
        String airCohortsApiUrl = getCohortsUrl(context, productId);
        if (airCohortsApiUrl == null || airCohortsApiUrl.isEmpty())
            return Response.status(Response.Status.BAD_REQUEST).build();

        URL url = new URL(airCohortsApiUrl + "/cohorts/meta/all/columns");

        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setReadTimeout(CONNECTION_TIMEOUT_MS);
        con.setConnectTimeout(CONNECTION_TIMEOUT_MS);
        con.setRequestProperty(Constants.AUTHENTICATION_HEADER, sessionToken);

        con.setRequestMethod("GET");
        con.setDoOutput(true);

        int code = con.getResponseCode();
        if (code >= 200 && code <= 299) {
            String resp = Utilities.streamToString(con.getInputStream());
            logger.info("getColumnsNames succeed : " + url);
            return Response.ok(resp).build();
        } else {
            return Response.status(code).entity(Utilities.streamToString(con.getErrorStream())).build();
        }
    }

}
