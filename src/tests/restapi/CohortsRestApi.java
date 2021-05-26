package tests.restapi;

import org.apache.wink.json4j.JSONException;
import org.apache.wink.json4j.JSONObject;
import tests.com.ibm.qautils.RestClientUtils;

import java.io.IOException;


public class CohortsRestApi
{
	protected String m_url;

	public void setUrl(String url) throws IOException
	{
		m_url = url;
	}
	
	//GET /admin/airlytics/products/{product-id}/cohorts
	public String getAllCohorts(String productID, String sessionToken){
		String response="";
 		try { 			
 			RestClientUtils.RestCallResults res= RestClientUtils.sendGet(m_url+"/airlytics/products/" + productID + "/cohorts", sessionToken);
 			response = res.message;
  			
		} catch (Exception e) {
			System.out.println("An exception was thrown when trying  to get cohorts. Message: "+e.getMessage()) ;
		}
 		return response;
	}

	//PUT /admin/airlytics/products/{product-id}/cohorts
	public String updateCohortsData(String productID, String content, String sessionToken){
		String response="";
 		try { 			
 			RestClientUtils.RestCallResults res= RestClientUtils.sendPut(m_url+"/airlytics/products/" + productID + "/cohorts", content, sessionToken);
 			response = res.message;
  			
		} catch (Exception e) {
			System.out.println("An exception was thrown when trying  to update all notifications. Message: "+e.getMessage()) ;
		}
 		return response;
	}
	
	//POST /admin/airlytics/products/{product-id}/cohorts
	public String createCohort(String productID, String cohortBody, String sessionToken) throws IOException{
		String cohortID = "";
		RestClientUtils.RestCallResults res = RestClientUtils.sendPost(m_url+"/airlytics/products/" + productID + "/cohorts", cohortBody, sessionToken);
		cohortID = parseCohortId(res.message);
		return cohortID;
		
	}

	public String exportCohort(String cohortID, String sessionToken) {
		//PUT /admin/airlytics/cohorts/{cohort-id}/execute
		String response="";
		try {
			RestClientUtils.RestCallResults res = RestClientUtils.sendPut(m_url+"/airlytics/cohorts/" + cohortID + "/execute", "{}", sessionToken);
			return res.message;
		} catch (IOException e) {
			System.out.println("An exception was thrown when trying  to export airlock cohort. Message: "+e.getMessage()) ;
		}
		return response;
	}
	
	// /admin/airlytics/cohorts/{cohort-id} - delete , update, get
	
	public String getCohort(String cohortID, String sessionToken){
		String response="";
		try {
			RestClientUtils.RestCallResults res = RestClientUtils.sendGet(m_url+"/airlytics/cohorts/" + cohortID, sessionToken);
			response = res.message;
		} catch (Exception e) {
			System.out.println("An exception was thrown when trying  to get airlock cohort. Message: "+e.getMessage()) ;
		}
			return response;
	}
	
	public int deleteCohort(String cohortID, String sessionToken){
		try {
			RestClientUtils.RestCallResults res = RestClientUtils.sendDelete(m_url+"/airlytics/cohorts/" + cohortID, sessionToken);
			return res.code;
		} catch (Exception e) {
			System.out.println("An exception was thrown when trying  to delete airlock cohort. Message: "+e.getMessage()) ;
		}
			return -1;
	}
	
	public String updateCohort(String cohortID, String cohort, String sessionToken){
		String responseID="";
		try {
			RestClientUtils.RestCallResults res = RestClientUtils.sendPut(m_url+"/airlytics/cohorts/" + cohortID, cohort, sessionToken);
			responseID = parseCohortId(res.message);
		} catch (Exception e) {
			System.out.println("An exception was thrown when trying  to update airlock cohort. Message: "+e.getMessage()) ;
		}
			return responseID;
	}
	
	private String parseCohortId(String notification){
		String cohortID = "";
		JSONObject response = null;
		try {
			response = new JSONObject(notification);
			if (response.containsKey("error")){
				cohortID = notification;
			} else {
				cohortID = (String)response.get("uniqueId");
			}
		} catch (JSONException e) {
			cohortID = "Invalid response in AirlockNotification: " + response;
		}

		return cohortID;

	}

}
