package tests.restapi;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.apache.wink.json4j.JSONArray;
import org.apache.wink.json4j.JSONException;
import org.apache.wink.json4j.JSONObject;
import org.testng.Assert;



import tests.com.ibm.qautils.RestClientUtils;

public class FeaturesRestApi {
	protected static String m_url ;
	protected static double seasonVersion = 3.0;
	
	public void setURL (String url){
		m_url = url;
		
		if (System.getProperty("seasonVersion") != null)
			seasonVersion = Double.parseDouble(System.getProperty("seasonVersion"));
	}
	
	String getBranch(String branchID)
	{
		return (seasonVersion < 3.0) ? "" : "/branches/" + branchID;
	}
	
	public String getAllFeatures(String sessionToken){
		String response="";
		try {
		 	
			RestClientUtils.RestCallResults res = RestClientUtils.sendGet(m_url+"/features", sessionToken);
			if (res.code!=200) {
				Assert.fail("Error when trying  to get a list of all features. Messaeg: "+res.message) ;
			}

		 	//response contains feature json
		 			
		} catch (Exception e) {
			Assert.fail("An exception was thrown when trying  to get a list of all features. Messaeg: "+e.getMessage()) ;
		}
		return response;
	}
	
	public String featuresInSeason(String seasonId, String sessionToken){
		//returns a flat list of items in a season and all of its branches with type=feature
		RestClientUtils.RestCallResults res = null;
		try {
		 	
			res = RestClientUtils.sendGet(m_url+"/products/seasons/" + seasonId + "/allfeatures", sessionToken);
			if (res.code!=200) {
				Assert.fail("Error when trying  to get a list of all features. Messaeg: "+res.message) ;
			}
		} catch (Exception e) {
			Assert.fail("An exception was thrown when trying  to get a list of all features in season. Message: "+e.getMessage()) ;
		}
		return res.message;
	}

	public JSONArray findFeaturesParsed(String seasonId, String pattern, Collection<String> searchAreas, Collection<String> options, String sessionToken) {
		JSONArray features = new JSONArray();
		try {
			RestClientUtils.RestCallResults res = findFeatures(seasonId,"MASTER", pattern, searchAreas, options, sessionToken);
			JSONObject jsonResponse = new JSONObject(res.message);
			features = jsonResponse.getJSONArray("foundIds");
		} catch (Exception e) {
			Assert.fail("An exception was thrown when trying to find features. Message: " + e.getMessage());
		}
		return features;
	}

	// in case we need to verify response codes and message
	public RestClientUtils.RestCallResults findFeatures(String seasonId,String branchId, String pattern, Collection<String> searchAreas, Collection<String> options, String sessionToken) {
		RestClientUtils.RestCallResults res = null;
		try {
			String searchareasParams = multiParamsToString(searchAreas, "searchareas");
			String optionsParams = multiParamsToString(options, "options");
			res = RestClientUtils.sendGet(m_url + "/features/find/" + seasonId +"/"+branchId+ "?pattern=" + URLEncoder.encode(pattern, "UTF-8") + searchareasParams + optionsParams, sessionToken);
		} catch (Exception e) {
			Assert.fail("An exception was thrown when trying to find features. Message: " + e.getMessage());
		}
		return res;
	}

	private String multiParamsToString(Collection<String> params, String paramName) {
		if (params.isEmpty()) {
			return "";
		}
		StringBuilder idsString = new StringBuilder();
		for (String searchArea : params) {
			idsString.append("&").append(paramName).append("=").append(searchArea.toString());
		}
		return idsString.toString();
	}
	
	//ADD FEATURE
	
	public String addFeature(String seasonID, String featureJson, String parentID, String sessionToken) throws IOException{
		return  doAddFeature(seasonID, BranchesRestApi.MASTER, featureJson, parentID, sessionToken);
	}
	
	public String addFeatureToBranch(String seasonID, String branchID, String featureJson, String parentID, String sessionToken) throws IOException{
		return doAddFeature(seasonID, branchID, featureJson, parentID,  sessionToken);
	}
	
	private String doAddFeature(String seasonID, String branchID, String featureJson, String parentID, String sessionToken) throws IOException{
		String featureID = "";
		//RestClientUtils.RestCallResults res = RestClientUtils.sendPost(m_url+"/products/seasons/" + seasonID + "/features?parent=" + parentID, featureJson, sessionToken);
		RestClientUtils.RestCallResults res = RestClientUtils.sendPost(m_url+"/products/seasons/" + seasonID + getBranch(branchID) + "/features?parent=" + parentID, featureJson, sessionToken);
		String feature = res.message;
		featureID = parseFeatureId(feature);
 		return featureID;
	}

	
	
	//GET FEATURE
	public String getFeature(String featureID, String sessionToken){
		return doGetFeature(featureID, BranchesRestApi.MASTER, sessionToken);
	}
	
	public String getFeatureFromBranch(String featureID, String branchID, String sessionToken){
		return doGetFeature(featureID, branchID, sessionToken);
	}
	
	private String doGetFeature(String featureID, String branchID, String sessionToken){
		String response="";
 		try { 			
 			//RestClientUtils.RestCallResults res= RestClientUtils.sendGet(m_url+"/products/seasons/features/" + featureID, sessionToken);
 			RestClientUtils.RestCallResults res= RestClientUtils.sendGet(m_url+"/products/seasons" + getBranch(branchID) +  "/features/" + featureID, sessionToken);
 			response = res.message;
  			
		} catch (Exception e) {
			Assert.fail("An exception was thrown when trying  to get feature. Message: "+e.getMessage()) ;
		}
 		return response;
	}
	
	
	//GET AIRLOCK ITEM ATTRIBUTES
	public String getFeatureAttributes(String featureID, String sessionToken){
		return doGetItemAttributes(featureID, BranchesRestApi.MASTER, sessionToken);
	}
	
	public String getFeatureAttributesFromBranch(String featureID, String branchID, String sessionToken){
		return doGetItemAttributes(featureID, branchID, sessionToken);
	}
	
	private String doGetItemAttributes(String itemID, String branchID, String sessionToken){
		String response="";
 		try { 
 			
 			//RestClientUtils.RestCallResults res= RestClientUtils.sendGet(m_url+"/products/seasons/features/" + featureID + "/attributes", sessionToken);
 			RestClientUtils.RestCallResults res= RestClientUtils.sendGet(m_url+"/products/seasons" + getBranch(branchID) + "/items/" + itemID + "/attributes", sessionToken);
 			response = res.message;
 			//response contains feature json
 			
		} catch (Exception e) {
			Assert.fail("An exception was thrown when trying  to get feature attributes. Message: "+e.getMessage()) ;
		}
 		return response;
	}
	
	//GET FEATURES FOR SPECIFIED SEASON
	public JSONArray getFeaturesBySeason(String seasonID, String sessionToken){
		return doGetFeaturesBySeason(seasonID, BranchesRestApi.MASTER, sessionToken);
	}
	
	public JSONArray getFeaturesBySeasonFromBranch(String seasonID, String branchID, String sessionToken){
		return doGetFeaturesBySeason(seasonID, branchID, sessionToken);
	}
	
	private JSONArray doGetFeaturesBySeason(String seasonID, String branchID, String sessionToken) {
		JSONArray features=new JSONArray();
		try {
		 	//GET /admin/products/seasons/{season-id}/branches/{branch-id}/features
			//RestClientUtils.RestCallResults res= RestClientUtils.sendGet(m_url+"/products/seasons/" + seasonID + "/features", sessionToken);
			RestClientUtils.RestCallResults res= RestClientUtils.sendGet(m_url+"/products/seasons/" + seasonID + getBranch(branchID) + "/features", sessionToken);
			String response = res.message;
		 	JSONObject jsonResponse = new JSONObject(response);
		 	if (jsonResponse.containsKey("root")){
			 	JSONObject season = jsonResponse.getJSONObject("root");
				features = season.getJSONArray("features");
		 	}	
		 			
		} catch (Exception e) {
			Assert.fail("An exception was thrown when trying  to get a list of all features in season. Message: "+e.getMessage()) ;
		}
		return features;
	}
	
	public String getFeaturesBySeason(String seasonID, String branchID, String sessionToken) {
		String response="";
		try {

			RestClientUtils.RestCallResults res= RestClientUtils.sendGet(m_url+"/products/seasons/" + seasonID + getBranch(branchID) + "/features", sessionToken);
			response =  res.message;
	
		 			
		} catch (Exception e) {
			Assert.fail("An exception was thrown when trying  to get a list of all features in season. Message: "+e.getMessage()) ;
		}
		return response;
	}


	
	//DELETE FEATURE
	public int deleteFeature(String featureID, String sessionToken){
		return doDeleteFeature(featureID, BranchesRestApi.MASTER, sessionToken);
	}
	
	public int deleteFeatureFromBranch(String featureID, String branchID, String sessionToken){
		return doDeleteFeature(featureID, branchID, sessionToken);
	}
	
	private int doDeleteFeature(String featureID, String branchID, String sessionToken){
		int responseCode = -1;
 		try {
 			//DELETE /admin/products/seasons/branches/{branch-id}/features/{feature-id}
 			RestClientUtils.RestCallResults res = RestClientUtils.sendDelete(m_url+"/products/seasons" + getBranch(branchID) + "/features/" + featureID, sessionToken);
 			responseCode = res.code;
		} catch (Exception e) {
			Assert.fail("An exception was thrown when trying  to delete a feature. Message: "+e.getMessage()) ;
		}
 		return responseCode;
	}
	
	public String simulateDeleteFeature(String featureID, String sessionToken){
		return doSimulateDeleteFeature(featureID, BranchesRestApi.MASTER, sessionToken);
	}
	
	public String simulateDeleteFeatureFromBranch(String featureID, String branchID, String sessionToken){
		return doSimulateDeleteFeature(featureID, branchID, sessionToken);
	}
	
	private String doSimulateDeleteFeature(String featureID, String branchID, String sessionToken){
		String response="";
 		try {
 			//DELETE /admin/products/seasons/branches/{branch-id}/features/{feature-id}
 			RestClientUtils.RestCallResults res = RestClientUtils.sendDelete(m_url+"/products/seasons"+ getBranch(branchID) + "/features/" + featureID + "?mode=VALIDATE", sessionToken);
 			response = res.message;
		} catch (Exception e) {
			Assert.fail("An exception was thrown when trying  to simulate delete a feature. Message: "+e.getMessage()) ;
		}
 		return response;
	}
	
	
	//UPDATE FEATURE
	public String updateFeature(String seasonID, String featureID, String featureJson, String sessionToken) throws IOException{
		return doUpdateFeature(seasonID, BranchesRestApi.MASTER, featureID, featureJson, sessionToken);
	}
	
	public String updateFeatureInBranch(String seasonID, String branchID, String featureID, String featureJson, String sessionToken) throws IOException{
		return doUpdateFeature(seasonID, branchID, featureID, featureJson, sessionToken);
	}
	
	private String doUpdateFeature(String seasonID, String branchID, String featureID, String featureJson, String sessionToken)throws IOException{
		String newID = "";
		//RestClientUtils.RestCallResults res = RestClientUtils.sendPut(m_url+"/products/seasons/features/" + featureID, featureJson, sessionToken);
		//PUT /admin/products/seasons/branches/{branch-id}/features/{feature-id}
		RestClientUtils.RestCallResults res = RestClientUtils.sendPut(m_url+"/products/seasons"+ getBranch(branchID) + "/features/" + featureID, featureJson, sessionToken);
		String feature = res.message;
 		newID = parseFeatureId(feature);
 		return newID;
	}	
	
	
	public String simulateUpdateFeature(String seasonID, String featureID, String featureJson, String sessionToken) throws IOException{
		return doSimulateUpdateFeature(seasonID, BranchesRestApi.MASTER, featureID, featureJson, sessionToken);
	}
	
	public String simulateUpdateFeatureInBranch(String seasonID, String branchID, String featureID, String featureJson, String sessionToken) throws IOException{
		return doSimulateUpdateFeature(seasonID, branchID, featureID, featureJson, sessionToken);
	}
	
	private String doSimulateUpdateFeature(String seasonID, String branchID, String featureID, String featureJson, String sessionToken)throws IOException{

		//RestClientUtils.RestCallResults res = RestClientUtils.sendPut(m_url+"/products/seasons/features/" + featureID + "?mode=VALIDATE", featureJson, sessionToken);
		RestClientUtils.RestCallResults res = RestClientUtils.sendPut(m_url+"/products/seasons"  + getBranch(branchID) + "/features/" + featureID + "?mode=VALIDATE", featureJson, sessionToken);
		return res.message;
	}
	
	//GET ROOT FEATURE
	public String getRootId(String seasonID, String sessionToken){
		return doGetRootId(seasonID, BranchesRestApi.MASTER, sessionToken);
	}
	
	public String getBranchRootId(String seasonID, String branchID, String sessionToken){
		return doGetRootId(seasonID, branchID, sessionToken);
	}
	
	private String doGetRootId(String seasonID, String branchID, String sessionToken){
		String rootId = "";
		try {
		 	
			//RestClientUtils.RestCallResults res= RestClientUtils.sendGet(m_url+"/products/seasons/" + seasonID + "/features", sessionToken);
			RestClientUtils.RestCallResults res= RestClientUtils.sendGet(m_url+"/products/seasons/" + seasonID + getBranch(branchID) + "/features", sessionToken);
			String response = res.message;
		 	JSONObject jsonResponse = new JSONObject(response);
		 	JSONObject root = jsonResponse.getJSONObject("root");
			rootId = root.getString("uniqueId");
		 			
		} catch (Exception e) {
			Assert.fail("An exception was thrown when trying  to get a list of all features in season. Message: "+e.getMessage()) ;
		}				

		return rootId;
	}
	

	
	public String copyFeature(String sourceId, String targetId, String mode, String minAppVersion, String nameSuffix, String sessionToken) throws IOException{

		/*String params = "?mode=" + mode;
		
		if (nameSuffix != null)			
			//params = addPrm(params, "&namesuffix=" + nameSuffix);
			params += "&namesuffix=" + nameSuffix;
		
		if (minAppVersion != null)
			//params += addPrm(params, "minappversion=" + minAppVersion);
			params += "&minappversion=" + minAppVersion;

		//System.out.println(m_url+"/features/copy/" + sourceId + "/" + targetId + params);
		 RestClientUtils.RestCallResults res = RestClientUtils.sendPut(m_url+"/features/copy/branches/MASTER/" + sourceId + "/branches/MASTER/" + targetId + params, "", sessionToken);
		
		 return res.message;
*/
		return copyItemBetweenBranches (sourceId, targetId, mode, minAppVersion, nameSuffix, sessionToken,BranchesRestApi.MASTER, BranchesRestApi.MASTER);
	}

	
	public String copyItemBetweenBranches(String sourceId, String targetId, String mode, String minAppVersion, String nameSuffix, String sessionToken, String srcBranchId, String destBranchId) throws IOException{

		String params = "?mode=" + mode;
		
		if (nameSuffix != null)			
			//params = addPrm(params, "&namesuffix=" + nameSuffix);
			params += "&namesuffix=" + nameSuffix;
		
		if (minAppVersion != null)
			//params += addPrm(params, "minappversion=" + minAppVersion);
			params += "&minappversion=" + minAppVersion;

		//System.out.println(m_url+"/features/copy/" + sourceId + "/" + targetId + params);
		 RestClientUtils.RestCallResults res = RestClientUtils.sendPut(m_url+"/features/copy/branches/" + srcBranchId + "/" + sourceId + "/branches/" + destBranchId + "/" + targetId + params, "", sessionToken);
		
		 return res.message;

	}

	
	public String importFeature(String source, String targetId, String mode, String minAppVersion, String nameSuffix, boolean overrideids, String sessionToken) throws IOException{
		return importFeatureToBranch(source, targetId, mode, minAppVersion, nameSuffix, overrideids, sessionToken, BranchesRestApi.MASTER);
	}

	public String importFeatureToBranch(String source, String targetId, String mode, String minAppVersion, String nameSuffix, boolean overrideids, String sessionToken, String branchId) throws IOException{

		String params = "?mode=" + mode;
		
		if (nameSuffix != null)			
			params += "&namesuffix=" + nameSuffix;
		
		if (minAppVersion != null)
			params += "&minappversion=" + minAppVersion;

		
		params += "&overrideids=" + overrideids;
		
		
		 RestClientUtils.RestCallResults res = RestClientUtils.sendPut(m_url+"/features/import/branches/" + branchId + "/" + targetId + params, source, sessionToken);
		
		 return res.message;
	}

	String addPrm(String parms, String item)
	{
		parms = (parms.equals("")) ? "?" : "&";
		return parms + item;
	}
	
	
	private String parseFeatureId(String feature){
		String featureID = "";
		JSONObject response = null;
		try {
			response = new JSONObject(feature);
			
			if (response.containsKey("error")){
				featureID = feature;
			} else {
				featureID = (String)response.get("uniqueId");
			}
		} catch (JSONException e) {
			featureID = "Invalid response: " + response;
		}

		return featureID;
	}
	
	static final public String BRANCH_PARENT_NAME = "branchFeatureParentName";
	static final public String BRANCH_CONFIGURATION_RULE_ITEMS = "branchConfigurationRuleItems";
	static final public String BRANCH_ORDERING_RULE_ITEMS = "branchOrderingRuleItems";
	static final public String BRANCH_FEATURE_ITEMS = "branchFeaturesItems";
	public static final String BRANCH_PURCHASE_OPTIONS_ITEMS = "branchPurchaseOptionsItems";
	public static final String BRANCH_ENTITLEMENT_ITEMS = "branchEntitlementItems";
/*	public  boolean jsonObjsAreEqual (JSONObject js1, JSONObject js2) throws JSONException {
		if (js1.containsKey(BRANCH_PARENT_NAME)) {
			js1.remove(BRANCH_PARENT_NAME);
		}		
		
		if (js2.containsKey(BRANCH_PARENT_NAME)) {
			js2.remove(BRANCH_PARENT_NAME);
		}
		
		if (js1.containsKey(BRANCH_CONFIGURATION_RULE_ITEMS)) {
			js1.remove(BRANCH_PARENT_NAME);
		}		
		
		if (js2.containsKey(BRANCH_CONFIGURATION_RULE_ITEMS)) {
			js2.remove(BRANCH_PARENT_NAME);
		}
		
		if (js1.containsKey(BRANCH_FEATURE_ITEMS)) {
			js1.remove(BRANCH_PARENT_NAME);
		}		
		
		if (js2.containsKey(BRANCH_FEATURE_ITEMS)) {
			js2.remove(BRANCH_PARENT_NAME);
		}
		
		return jsonObjsAreEqual(js1, js2);
	}
	*/
	//this function compare 2 features trees ignoring the branch fields
	public  boolean jsonObjsAreEqual (JSONObject js1, JSONObject js2) throws JSONException {
	    if (js1 == null || js2 == null) {
	        return (js1 == js2);
	    }
	    
	    //clean branches fields - they are not needed for this comparison
	    if (js1.containsKey(BRANCH_PARENT_NAME)) {
			js1.remove(BRANCH_PARENT_NAME);
		}		
		
		if (js2.containsKey(BRANCH_PARENT_NAME)) {
			js2.remove(BRANCH_PARENT_NAME);
		}
		
		if (js1.containsKey(BRANCH_CONFIGURATION_RULE_ITEMS)) {
			js1.remove(BRANCH_CONFIGURATION_RULE_ITEMS);
		}		
		
		if (js2.containsKey(BRANCH_CONFIGURATION_RULE_ITEMS)) {
			js2.remove(BRANCH_CONFIGURATION_RULE_ITEMS);
		}
		
		if (js1.containsKey(BRANCH_ORDERING_RULE_ITEMS)) {
			js1.remove(BRANCH_ORDERING_RULE_ITEMS);
		}
		
		if (js2.containsKey(BRANCH_ORDERING_RULE_ITEMS)) {
			js2.remove(BRANCH_ORDERING_RULE_ITEMS);
		}		
		
		if (js1.containsKey(BRANCH_FEATURE_ITEMS)) {
			js1.remove(BRANCH_FEATURE_ITEMS);
		}
		
		if (js2.containsKey(BRANCH_FEATURE_ITEMS)) {
			js2.remove(BRANCH_FEATURE_ITEMS);
		}
		
		if (js1.containsKey(BRANCH_PURCHASE_OPTIONS_ITEMS)) {
			js1.remove(BRANCH_PURCHASE_OPTIONS_ITEMS);
		}
		
		if (js2.containsKey(BRANCH_PURCHASE_OPTIONS_ITEMS)) {
			js2.remove(BRANCH_PURCHASE_OPTIONS_ITEMS);
		}
		
		if (js1.containsKey(BRANCH_ENTITLEMENT_ITEMS)) {
			js1.remove(BRANCH_ENTITLEMENT_ITEMS);
		}
		
		if (js2.containsKey(BRANCH_ENTITLEMENT_ITEMS)) {
			js2.remove(BRANCH_ENTITLEMENT_ITEMS);
		}

	    List<String> l1 =  Arrays.asList(JSONObject.getNames(js1));
	    Collections.sort(l1);
	    List<String> l2 =  Arrays.asList(JSONObject.getNames(js2));
	    Collections.sort(l2);
	    if (!l1.equals(l2)) {
	    		System.out.println("l1=" + l1);
	    		System.out.println("l2=" + l2);
	        return false;
	    }
	    for (String key : l1) {
	    	if (key.equals("uniqueId") || key.equals("lastModified") || key.equals("creationDate") || key.equals("seasonId") 
	    			|| key.equals("name") || key.equals("rolloutPercentageBitmap") || key.equals("branchFeaturesItems") || key.equals("branchConfigurationRuleItems") 
	    			|| key.equals("branchEntitlementItems") || key.equals("branchPurchaseOptionsItems") || key.equals("branchFeatureParentName") 
	    			|| key.equals("branchStatus"))
	    		continue;

	    	if (key.equals("features") || key.equals("configurationRules") || key.equals("orderingRules") || key.equals("purchaseOptions") || key.equals("entitlements")){
	    		JSONArray array1 = js1.getJSONArray(key);
	    		JSONArray array2 = js2.getJSONArray(key);
	    		if (array1.size() != array2.size()){
	    			//System.out.println("array1"+array1);
	    			//System.out.println("array2"+array2);
	    			return false;
	    		} else {
	    			for (int i=0; i<array1.size(); i++){
	    				if (!jsonObjsAreEqual(array1.getJSONObject(i), array2.getJSONObject(i))){
	    					//System.out.println("recursive1 "+ array1.getJSONObject(i));
	    					//System.out.println("recursive2 "+ array2.getJSONObject(i));
	    					return false;
	    				}
	    			}
	    		}
	    		
	    	} else {
	    	
		        Object val1 = js1.get(key);
		        Object val2 = js2.get(key);
		        
		        if (val1 instanceof JSONObject && ((JSONObject) val1).length()!= 0 && ((JSONObject) val2).length()!= 0) {
		            if (!(val2 instanceof JSONObject)) {
		            	//System.out.println("key="+key);
		                return false;
		            }
		            if (!jsonObjsAreEqual((JSONObject)val1, (JSONObject)val2)) {
		            	//System.out.println("recursive2="+key);
		                return false;
		            }
		        }
	
		        if (val1 == null) {
		            if (val2 != null) {
		            	//System.out.println("val21="+val2);
		                return false;
		            }
		        }  else if (!val1.equals(val2)) {
		        	//System.out.println("key="+key);
		        	//System.out.println("val1="+val1);
		        	//System.out.println("val2="+val2);
		            return false;
		        }
		    }
	    }
	    
	    return true;
	}
	
	public void convertAllFeaturesToDevStage(String seasonId, String sessionToken) throws JSONException, IOException{
		JSONArray features = getFeaturesBySeason(seasonId, sessionToken);
		findFeatureInProduction(seasonId, features, sessionToken);
	}
	
	private void findFeatureInProduction(String seasonId, JSONArray features, String sessionToken) throws JSONException, IOException{
		for (int j=0; j<features.size(); j++){
			JSONObject feature = features.getJSONObject(j);
			String temp = getFeature(feature.getString("uniqueId"), sessionToken);
				feature = new JSONObject(temp);

					
				JSONArray configurationRules = new JSONArray();
				if (feature.containsKey("configurationRules"))
					configurationRules = feature.getJSONArray("configurationRules");
				
				if (configurationRules.size() != 0) {
					findFeatureInProduction(seasonId, configurationRules, sessionToken);				
				}
				
				
				JSONArray subFeatures = new JSONArray();
				if (feature.containsKey("features"))
					subFeatures = feature.getJSONArray("features");				
				if (subFeatures.size() != 0) {
					findFeatureInProduction(seasonId, subFeatures, sessionToken);
				
				}
				if (feature.has("stage") && feature.getString("stage").equals("PRODUCTION")){
					//change stage to development
					String updatedFeature = getFeature(feature.getString("uniqueId"), sessionToken);
					feature = new JSONObject(updatedFeature);
					String featureId = feature.getString("uniqueId");
					feature.put("stage", "DEVELOPMENT");
					updateFeature(seasonId, featureId, feature.toString(), sessionToken);
				}
		}	
	}


	public String getFeatureFollowers(String featureId, String sessionToken){
		String response="";
		try {
			RestClientUtils.RestCallResults res = RestClientUtils.sendGet(m_url + "/products/seasons/features/" + featureId+"/follow",sessionToken);
			response = res.message;
			//response list of followers

		} catch (Exception e) {
			Assert.fail("An exception was thrown when trying  to get followers. Message: "+e.getMessage()) ;
		}
		return response;
	}

	public String followFeature(String featureId, String sessionToken){
		String response="";
		try {
			RestClientUtils.RestCallResults res = RestClientUtils.sendPost(m_url + "/products/seasons/features/" + featureId+"/follow","",sessionToken);
			response = res.message;
		} catch (Exception e) {
			Assert.fail("An exception was thrown when trying  to follow feature. Message: "+e.getMessage()) ;
		}
		return response;
	}

	public int unfollowFeature(String featureId, String sessionToken){

		try {
			RestClientUtils.RestCallResults res = RestClientUtils.sendDelete(m_url + "/products/seasons/features/" + featureId+"/follow",sessionToken);
			return res.code;
		} catch (Exception e) {
			Assert.fail("An exception was thrown when trying  to unfollow feature. Message: "+e.getMessage()) ;
		}
		return -1;
	}
	
	public String setDateFormat() throws InterruptedException{
		Thread.sleep(2000);
		String dateFormat = RuntimeDateUtilities.getCurrentTimeStamp();
		Thread.sleep(2000);
		return dateFormat;
	}
	
	public void setSleep() throws InterruptedException{
		Thread.sleep(2000);
	}

}
