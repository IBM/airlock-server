package tests.restapi;

import org.apache.wink.json4j.JSONException;
import org.apache.wink.json4j.JSONObject;
import tests.com.ibm.qautils.RestClientUtils;

import java.io.IOException;


public class DataimportRestApi
{
	protected String m_url;

	public void setUrl(String url) throws IOException
	{
		m_url = url;
	}
	
	//GET /admin/airlytics/products/{product-id}/dataimport
	public String getAllJobs(String productID, String sessionToken){
		String response="";
 		try { 			
 			RestClientUtils.RestCallResults res= RestClientUtils.sendGet(m_url+"/airlytics/products/" + productID + "/dataimport", sessionToken);
 			response = res.message;
  			
		} catch (Exception e) {
			System.out.println("An exception was thrown when trying  to get data imports. Message: "+e.getMessage()) ;
		}
 		return response;
	}

	//PUT /admin/airlytics/products/{product-id}/dataimport
	public String updateDataImportsData(String productID, String content, String sessionToken){
		String response="";
 		try { 			
 			RestClientUtils.RestCallResults res= RestClientUtils.sendPut(m_url+"/airlytics/products/" + productID + "/dataimport", content, sessionToken);
 			response = res.message;
  			
		} catch (Exception e) {
			System.out.println("An exception was thrown when trying  to update all data imports. Message: "+e.getMessage()) ;
		}
 		return response;
	}
	
	//POST /admin/airlytics/products/{product-id}/dataimport
	public String createJob(String productID, String jobBody, String sessionToken) throws IOException{
		String jobID = "";
		RestClientUtils.RestCallResults res = RestClientUtils.sendPost(m_url+"/airlytics/products/" + productID + "/dataimport", jobBody, sessionToken);
		jobID = parseJobId(res.message);
		return jobID;
		
	}
	
	// /admin/airlytics/dataimport/{job-id} - delete , update, get
	
	public String getJob(String jobID, String sessionToken){
		String response="";
		try {
			RestClientUtils.RestCallResults res = RestClientUtils.sendGet(m_url+"/airlytics/dataimport/" + jobID, sessionToken);
			response = res.message;
		} catch (Exception e) {
			System.out.println("An exception was thrown when trying  to get airlock data import job. Message: "+e.getMessage()) ;
		}
			return response;
	}
	
	public int deleteJob(String jobID, String sessionToken){
		try {
			RestClientUtils.RestCallResults res = RestClientUtils.sendDelete(m_url+"/airlytics/dataimport/" + jobID, sessionToken);
			return res.code;
		} catch (Exception e) {
			System.out.println("An exception was thrown when trying  to delete dataimport job. Message: "+e.getMessage()) ;
		}
			return -1;
	}
	
	public String updateJob(String jobID, String job, String sessionToken){
		String responseID="";
		try {
			RestClientUtils.RestCallResults res = RestClientUtils.sendPut(m_url+"/airlytics/dataimport/" + jobID, job, sessionToken);
			responseID = parseJobId(res.message);
		} catch (Exception e) {
			System.out.println("An exception was thrown when trying  to update dataimport job. Message: "+e.getMessage()) ;
		}
			return responseID;
	}
	
	private String parseJobId(String notification){
		String jobID = "";
		JSONObject response = null;
		try {
			response = new JSONObject(notification);
			if (response.containsKey("error")){
				jobID = notification;
			} else {
				jobID = (String)response.get("uniqueId");
			}
		} catch (JSONException e) {
			jobID = "Invalid response in AirlockNotification: " + response;
		}

		return jobID;

	}

}
