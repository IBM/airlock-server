package tests.restapi;

import org.apache.wink.json4j.JSONException;
import org.apache.wink.json4j.JSONObject;

import tests.com.ibm.qautils.RestClientUtils;

import java.io.IOException;

/**
 * Created by vmazel on 30/07/2017.
 */
public class ExperimentAnalyticsApi {
    protected static String m_url ;


    public void setURL (String url){
        m_url = url;
    }

    public String getAllExperiments (String sessionToken) throws Exception{
        RestClientUtils.RestCallResults res = RestClientUtils.sendGet(m_url+"/experiments", sessionToken);
        return res.message;
    }


    public String getExperiment (String experimentId, String sessionToken) throws Exception{
        RestClientUtils.RestCallResults res = RestClientUtils.sendGet(m_url+"/experiments/" + experimentId, sessionToken);
        return res.message;

    }
    
    public RestClientUtils.RestCallResults updateExperiment (String experimentId, String experiment, String sessionToken) throws IOException{
        RestClientUtils.RestCallResults res = RestClientUtils.sendPut(m_url+"/experiments/" + experimentId, experiment, sessionToken);
        return res;
    }

    public String addExperiment (String experimentId, String experiment, String sessionToken) throws IOException{
        RestClientUtils.RestCallResults res = RestClientUtils.sendPost(m_url+"/experiments/" + experimentId, experiment, sessionToken);
        return parseId(res.message);
    }

    public int deleteExperiment (String experimentId, String sessionToken) throws Exception {
        RestClientUtils.RestCallResults res = RestClientUtils.sendDelete(m_url+"/experiments/" + experimentId, sessionToken);
        return res.code;
    }


    public int healthcheck() throws Exception {
        RestClientUtils.RestCallResults res = RestClientUtils.sendGet(m_url+"/ops/healthcheck", "");
        return res.code;
    }

    public String getStatus (String sessionToken) throws Exception {
        RestClientUtils.RestCallResults res = RestClientUtils.sendGet(m_url+"/experiments/status", sessionToken);
        return res.message;
    }
    
    public String reindexExperiment(String experimentId, String sessionToken) throws IOException{
    	RestClientUtils.RestCallResults res = RestClientUtils.sendPut(m_url+"/experiments/" + experimentId + "/reindex", "", sessionToken);
    	return res.message;
    }
    
    public String resetExperimentDashboard(String experimentId, String sessionToken) throws IOException{
    	RestClientUtils.RestCallResults res = RestClientUtils.sendPut(m_url+"/experiments/" + experimentId + "/resetdashboard", "", sessionToken);
    	return res.message;
    }
    
    public String getConfiguration (String sessionToken) throws Exception {
        RestClientUtils.RestCallResults res = RestClientUtils.sendGet(m_url+"/ops/configuration", sessionToken);
        return res.message;
    }
    
    public String setConfiguration (String body, String sessionToken) throws Exception {
        RestClientUtils.RestCallResults res = RestClientUtils.sendPut(m_url+"/ops/configuration", body, sessionToken);
        return res.message;
    }
/*
    public static RuntimeRestApi.DateModificationResults getBucketFile(String m_url, String experimentID, String sessionToken) throws JSONException, IOException {
        String filePath = buildS3Path(m_url, experimentID);
        filePath = cleanFileName(filePath);
        RestCallResults res = RuntimeRestApi.getRuntimeFile(filePath, sessionToken);
		return new DateModificationResults (res.message, res.code);
    }

    private static String buildS3Path(String m_url, String experimentName) throws JSONException {
        String path = m_url + "/analytics/experiments/" +  experimentName + "/experimentBuckets.json";
        return cleanFileName(path);
    }

    private static String cleanFileName(String fileName){
        String filePath = fileName;
        filePath = filePath.replaceAll("//", "/");
        filePath = filePath.substring(7);
        filePath = "https://" + filePath;
        return filePath;
    }
*/    
    private String parseId(String input){
        String idString = "";
        JSONObject response = null;
        try {
            response = new JSONObject(input);

            if (response.containsKey("error")){
                idString = input;
            } else {
                idString = (String)response.get("experimentId");
            }
        } catch (JSONException e) {
            idString = "Invalid response: " + response;
        }

        return idString;
    }


}
