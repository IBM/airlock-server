package com.ibm.airlock.admin.dataimport;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;
import java.util.logging.Logger;

import javax.servlet.ServletContext;
import javax.ws.rs.core.Response;

import org.apache.wink.json4j.JSONException;
import org.apache.wink.json4j.JSONObject;

import com.ibm.airlock.Constants;
import com.ibm.airlock.Strings;
import com.ibm.airlock.admin.Utilities;
import com.ibm.airlock.admin.ValidationResults;

public class DataImportUtilities {
    public static final Logger logger = Logger.getLogger(DataImportUtilities.class.getName());

    public static final int CONNECTION_TIMEOUT_MS = 120000; //2 minutes

    private static String getDataImportUrl(ServletContext context, String productId) {
        Map<String, String> dataImportUrls = (Map<String, String>)context.getAttribute(Constants.DATA_IMPORT_SERVER_URL_PARAM_NAME);
        if (dataImportUrls!=null) {
            return dataImportUrls.get(productId);
        }
        return null;
    }
    public static DataImportCallResult importData(ServletContext context, DataImportItem dataImportItem, String sessionToken) throws IOException, JSONException {
        String dataImportApiUrl = getDataImportUrl(context, dataImportItem.getProductId().toString());
        if (dataImportApiUrl==null || dataImportApiUrl.isEmpty())
            return null;

        URL url = new URL(dataImportApiUrl+"/dataimport/s3file");
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setReadTimeout(CONNECTION_TIMEOUT_MS);
        con.setConnectTimeout(CONNECTION_TIMEOUT_MS);
        con.setRequestProperty("Content-Type", "application/json; charset=utf-8");
        con.setRequestProperty(Constants.AUTHENTICATION_HEADER, sessionToken);

        con.setRequestMethod("POST");

        // Send post request
        con.setDoOutput(true);
        JSONObject obj = new JSONObject();
        obj.put("uniqueId", dataImportItem.getUniqueId().toString());
        obj.put("productId", dataImportItem.getProductId().toString());
        obj.put("s3File", dataImportItem.getS3File());
        obj.put("overwrite", dataImportItem.getOverwrite());
        obj.put("targetTable", dataImportItem.getTargetTable());
        String data = obj.toString();
        con.getOutputStream().write(data.getBytes("UTF-8"));

        int code = con.getResponseCode();

        JSONObject responseObject = null;
//        DataImportResult dataImportResponse;
//        try {
//            String resp = Utilities.streamToString(con.getInputStream());
//            responseObject = new JSONObject(resp);
//            dataImportResponse = new DataImportResult(responseObject);
//        } catch (JSONException e) {
//            String erroMsgJsonStr = Utilities.streamToString(con.getErrorStream());
//            JSONObject erroMsgJson = new JSONObject(erroMsgJsonStr);
//            logger.severe("Error on importData from import-data-server call:"+erroMsgJsonStr);
//            return new DataImportCallResult(new ValidationResults(Strings.failDataImportExecute + erroMsgJson.getString("error"), Response.Status.INTERNAL_SERVER_ERROR),null);
//        }

        if (code >= 200 && code <= 299) {
            logger.info("data import succeed : " + url);
            return new DataImportCallResult(null, null);
        }

        String erroMsgJsonStr = getErrorMessage(dataImportApiUrl, code, con);
        JSONObject erroMsgJson = new JSONObject(erroMsgJsonStr);
        return new DataImportCallResult(new ValidationResults(Strings.failDataImportExecute + erroMsgJson.getString("error"), Response.Status.INTERNAL_SERVER_ERROR),null);
    }
    
    public static Response getDataImportTables(ServletContext context,String prodctId ,String sessionToken) throws IOException {
        String dataImportApiUrl = getDataImportUrl(context, prodctId);
        
        if (dataImportApiUrl==null || dataImportApiUrl.isEmpty()) {
            return null;
        }

        URL url = new URL(dataImportApiUrl+"/dataimport/meta/tables");
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setReadTimeout(CONNECTION_TIMEOUT_MS);
        con.setConnectTimeout(CONNECTION_TIMEOUT_MS);
        con.setRequestProperty(Constants.AUTHENTICATION_HEADER, sessionToken);

        con.setRequestMethod("GET");
        con.setDoOutput(true);

        int code = con.getResponseCode();
        
        if (code >= 200 && code <= 299) {
            String resp = Utilities.streamToString(con.getInputStream());
            logger.info("getTables succeed : " + url);
            return Response.ok(resp).build();
        } else {
            return Response.status(code).entity(getErrorMessage(dataImportApiUrl, code, con)).build();
        }
    }

	private static String getErrorMessage(String dataImportApiUrl, int code, HttpURLConnection con) {
		String msg = null; 
		
		try {
			msg = Utilities.streamToString(con.getErrorStream());
		} catch (Exception e) {
			msg = "Error " + code + " from AI Data Import service at " + dataImportApiUrl;
		}
		return msg;
	}
}
