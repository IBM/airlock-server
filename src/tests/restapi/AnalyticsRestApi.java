package tests.restapi;

import java.util.HashMap;

import org.apache.wink.json4j.JSONArray;
import org.apache.wink.json4j.JSONException;
import org.apache.wink.json4j.JSONObject;
import org.testng.Assert;

import tests.com.ibm.qautils.RestClientUtils;



public class AnalyticsRestApi {
	protected static String m_url ;
	
	public void setURL (String url){
		m_url = url;
	}
	

	//get globalDataCollection
	public String getGlobalDataCollection(String seasonId,String branchID, String mode, String sessionToken){
			String response="";
		try {
			RestClientUtils.RestCallResults res = RestClientUtils.sendGet(m_url+"/globalDataCollection/" + seasonId + "/branches/" + branchID + "?mode=" + mode, sessionToken);
			response = res.message;
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return response;
		
	}
	
	//update globalDataCollection
	public String updateGlobalDataCollection(String seasonId,String branchID, String body, String sessionToken){
		String response="";
		try {
			RestClientUtils.RestCallResults res = RestClientUtils.sendPut(m_url+"/globalDataCollection/" + seasonId  + "/branches/" + branchID, body, sessionToken);
			response = res.message;
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
 		return response;

		
	}
	
	
	//add feature to globalDataCollection
	
	public String addFeatureToAnalytics(String featureId, String branchID,  String sessionToken){
		String response="";
		try {
			RestClientUtils.RestCallResults res = RestClientUtils.sendPost(m_url+"/globalDataCollection/" + "branches/" + branchID + "/feature/" + featureId ,"", sessionToken);
			response = res.message;
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return response;
		
	}
	
	//delete feature from analytics
	public String deleteFeatureFromAnalytics(String featureId,String branchID, String sessionToken){
		String response="";
		try {
			RestClientUtils.RestCallResults res = RestClientUtils.sendDelete(m_url+"/globalDataCollection/" + "branches/" + branchID + "/feature/" + featureId , sessionToken);
			response = res.message;
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return response;
		
	}
	
	//update input field to analytics
	
	public String updateInputFieldToAnalytics(String seasonId,String branchID, String inputFields, String sessionToken){
		String response="";
		try {
			RestClientUtils.RestCallResults res = RestClientUtils.sendPut(m_url+"/globalDataCollection/" + seasonId + "/branches/" + branchID + "/inputfields", inputFields, sessionToken);
			response = res.message;
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return response;
		
	}
	
	//add attributes to analytics
	public String addAttributesToAnalytics(String featureId,String branchID, String attributes, String sessionToken){
		String response="";
		try {
			RestClientUtils.RestCallResults res = RestClientUtils.sendPut(m_url+"/globalDataCollection/branches/" + branchID + "/feature/" + featureId + "/attributes", attributes, sessionToken);
			response = res.message;
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return response;
		
	}
	
	public String getQuota(String seasonId, String sessionToken){
		String response="";
		try {
			RestClientUtils.RestCallResults res = RestClientUtils.sendGet(m_url+"/" + seasonId + "/quota", sessionToken);
			response = res.message;
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return response;
		
	}

	
	public String getExperimentQuota(String experimentId, String sessionToken){
		String response="";
		//GET /analytics/products/experiments/{experiment-id}/quota
		try {
			RestClientUtils.RestCallResults res = RestClientUtils.sendGet(m_url+"/products/experiments/" + experimentId + "/quota", sessionToken);
			response = res.message;
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return response;
	}

	public String getExperimentGlobalDataCollection(String experimentId, String sessionToken){
		String response="";
		//GET /analytics/globalDataCollection/experiments/{experiment-id}
		try {
			RestClientUtils.RestCallResults res = RestClientUtils.sendGet(m_url+"/globalDataCollection/experiments/" + experimentId, sessionToken);
			response = res.message;
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return response;
	}

	public String updateQuota(String seasonId, int quota, String sessionToken){
		String response="";
		try {
			RestClientUtils.RestCallResults res = RestClientUtils.sendPut(m_url+"/" + seasonId + "/quota/" + quota, "", sessionToken);
			response = res.message;
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return response;
		
	}
	
	
	//functions to build analytics object
	public String addInputFieldsToAnalytics(String globalDataCollection, String inputField) throws JSONException{
		
		JSONObject json = new JSONObject(globalDataCollection);
		JSONObject analyticsDataCollection = json.getJSONObject("analyticsDataCollection");
		JSONArray inputFieldsToAnalytics = analyticsDataCollection.getJSONArray("inputFieldsForAnalytics");
		inputFieldsToAnalytics.add(inputField);
		return json.toString();
	}
	
	public String addFeatureOnOff(String globalDataCollection, String featureId) throws JSONException{
		
		JSONObject json = new JSONObject(globalDataCollection);
		JSONObject analyticsDataCollection = json.getJSONObject("analyticsDataCollection");
		JSONArray featuresOnOff = analyticsDataCollection.getJSONArray("featuresAndConfigurationsForAnalytics");
		featuresOnOff.add(featureId);
		return json.toString();
	}
	
	public String addFeaturesAttributesToAnalytics(String globalDataCollection, String featureId, JSONArray attributes) throws JSONException{

		
		JSONObject json = new JSONObject(globalDataCollection);
		JSONObject analyticsDataCollection = json.getJSONObject("analyticsDataCollection");
		JSONArray featuresAttributesToAnalytics = analyticsDataCollection.getJSONArray("featuresAttributesForAnalytics");
		JSONObject attJson = new JSONObject();
		attJson.put("id", featureId);
		attJson.put("attributes", attributes);
		featuresAttributesToAnalytics.add(attJson);
		return json.toString();
	}
	
	public JSONArray getAnalyticsDataCollectionByFeatureNames(String globalDataCollection) throws JSONException{
		JSONObject json = new JSONObject(globalDataCollection);
		JSONObject analyticsDataCollection = json.getJSONObject("analyticsDataCollection");
		if (analyticsDataCollection.containsKey("analyticsDataCollectionByFeatureNames"))
				return analyticsDataCollection.getJSONArray("analyticsDataCollectionByFeatureNames");
		
		return new JSONArray();
	}
	
	
	@SuppressWarnings("unchecked")
	public String validateDataCollectionByFeatureNames(String display, String basic) throws JSONException{
		JSONObject jsonBasic = new JSONObject(basic);
		JSONObject jsonDisplay = new JSONObject(display);
		
		HashMap featuresWithAttributesInDisplay = new HashMap();

		//check that the number of features in featuresAndConfigurationsForAnalytics is the same as the number of all features in display that have "sendToAnalytics"=true
		JSONArray featuresOnOff = jsonBasic.getJSONObject("analyticsDataCollection").getJSONArray("featuresAndConfigurationsForAnalytics");			
		JSONArray collectionByFeatureNames = jsonDisplay.getJSONObject("analyticsDataCollection").getJSONArray("analyticsDataCollectionByFeatureNames");
		int sendToAnalytics = 0;
		
		for (int i=0; i < collectionByFeatureNames.size(); i++){
			JSONObject item = collectionByFeatureNames.getJSONObject(i);
			if (item.getBoolean("sendToAnalytics")) {
				sendToAnalytics++;					
			}
			//create a map of feature id and attributes for the next test
			if (item.containsKey("attributes"))
				featuresWithAttributesInDisplay.put(item.getString("id"), item.getJSONArray("attributes"));
		}
		if (featuresOnOff.size() != sendToAnalytics)
			return "Incorrect number of featuresAndConfigurationsForAnalytics in analyticsDataCollectionByFeatureNames";
		
		
		//for each feature with attributes in analytics compare its attributes list with list in display
		JSONArray featuresAttributesToAnalytics = jsonBasic.getJSONObject("analyticsDataCollection").getJSONArray("featuresAttributesForAnalytics");
		for (int i=0; i < featuresAttributesToAnalytics.size(); i++){
			if (featuresAttributesToAnalytics.getJSONObject(i).containsKey("attributes")) {
				String id = featuresAttributesToAnalytics.getJSONObject(i).getString("id");
				if (featuresWithAttributesInDisplay.containsKey(id)){
					JSONArray displayAttributes = (JSONArray)featuresWithAttributesInDisplay.get(id);
					Assert.assertEquals(displayAttributes.size(), featuresAttributesToAnalytics.getJSONObject(i).getJSONArray("attributes").size(), "Incorrect number of attributes");										
				} else {
						return "One of the features with attributes was not found in analyticsDataCollectionByFeatureNames";
				}
		}	
		}
		
		return "true";
	}
	
	public String setDateFormat() throws InterruptedException{
		Thread.sleep(2000);
		String dateFormat = RuntimeDateUtilities.getCurrentTimeStamp();
		Thread.sleep(2000);
		return dateFormat;
	}
	
	public void setSleep() throws InterruptedException{
		Thread.sleep(3000);
	}


}
