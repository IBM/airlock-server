package tests.restapi;

import java.io.IOException;

import org.apache.wink.json4j.JSONArray;
import org.apache.wink.json4j.JSONException;
import org.apache.wink.json4j.JSONObject;
import org.testng.Assert;

import tests.com.ibm.qautils.RestClientUtils;

public class InAppPurchasesRestApi {
	protected static String m_url ;
	
	public void setURL (String url){
		m_url = url;
	}

	public String addPurchaseItem(String seasonID, String purchaseJson, String parentID, String sessionToken) throws IOException{
		return  doAddPurchase(seasonID, BranchesRestApi.MASTER, purchaseJson, parentID, sessionToken);
	}
	
	public String addPurchaseItemToBranch(String seasonID, String branchID, String purchaseJson, String parentID, String sessionToken) throws IOException{
		return doAddPurchase(seasonID, branchID, purchaseJson, parentID,  sessionToken);
	}
	
	private String doAddPurchase(String seasonID, String branchID, String purchaseJson, String parentID, String sessionToken) throws IOException{
		String purchaseID = "";
		RestClientUtils.RestCallResults res = RestClientUtils.sendPost(m_url+"/products/seasons/" + seasonID + "/branches/" + branchID + "/entitlements?parent=" + parentID, purchaseJson, sessionToken);
		String feature = res.message;
		purchaseID = parsePurchaseId(feature);
 		return purchaseID;
	}
	
	public String getPurchaseItem(String purchaseID, String sessionToken){
		return doGetPurchaseItem(purchaseID, BranchesRestApi.MASTER, sessionToken);
	}
	
	public String getPurchaseItemFromBranch(String purchaseID, String branchID, String sessionToken){
		return doGetPurchaseItem(purchaseID, branchID, sessionToken);
	}
	
	private String doGetPurchaseItem(String purchaseID, String branchID, String sessionToken){
		String response="";
 		try { 			
 			RestClientUtils.RestCallResults res= RestClientUtils.sendGet(m_url+"/products/seasons/branches/" + branchID +  "/entitlements/" + purchaseID, sessionToken);
 			response = res.message;			
		} catch (Exception e) {
			Assert.fail("An exception was thrown when trying  to get purchase item. Message: "+e.getMessage()) ;
		}
 		return response;
	}
	

	public JSONArray getPurchasesBySeason(String seasonID, String sessionToken){
		return doGetPurchasesBySeason(seasonID, BranchesRestApi.MASTER, sessionToken);
	}
	
	public JSONArray getPurchasesBySeasonFromBranch(String seasonID, String branchID, String sessionToken){
		return doGetPurchasesBySeason(seasonID, branchID, sessionToken);
	}
	
	private JSONArray doGetPurchasesBySeason(String seasonID, String branchID, String sessionToken) {
		JSONArray features=new JSONArray();
		try {
		 	RestClientUtils.RestCallResults res= RestClientUtils.sendGet(m_url+"/products/seasons/" + seasonID + "/branches/" + branchID + "/entitlements", sessionToken);
			String response = res.message;
		 	JSONObject jsonResponse = new JSONObject(response);
		 	if (jsonResponse.containsKey("entitlementsRoot")){
			 	JSONObject season = jsonResponse.getJSONObject("entitlementsRoot");
				features = season.getJSONArray("entitlements");
		 	}	
		 			
		} catch (Exception e) {
			Assert.fail("An exception was thrown when trying  to get a list of all purchase items in season. Message: "+e.getMessage()) ;
		}
		return features;
	}
	
	private String parsePurchaseId(String feature){
		String purchaseID = "";
		JSONObject response = null;
		try {
			response = new JSONObject(feature);
			
			if (response.containsKey("error")){
				purchaseID = feature;
			} else {
				purchaseID = (String)response.get("uniqueId");
			}
		} catch (JSONException e) {
			purchaseID = "Invalid response: " + response;
		}

		return purchaseID;
	}
	
	//DELETE FEATURE
	public int deletePurchaseItem(String purchaseID, String sessionToken){
		return doDeletePurchaseItem(purchaseID, BranchesRestApi.MASTER, sessionToken);
	}
	
	public int deletePurchaseItemFromBranch(String purchaseID, String branchID, String sessionToken){
		return doDeletePurchaseItem(purchaseID, branchID, sessionToken);
	}
	
	private int doDeletePurchaseItem(String purchaseID, String branchID, String sessionToken){
		int responseCode = -1;
 		try {
 			RestClientUtils.RestCallResults res = RestClientUtils.sendDelete(m_url+"/products/seasons/branches/" + branchID + "/entitlements/" + purchaseID, sessionToken);
 			responseCode = res.code;
		} catch (Exception e) {
			Assert.fail("An exception was thrown when trying  to delete a purchase item. Message: "+e.getMessage()) ;
		}
 		return responseCode;
	}
	
	public String updatePurchaseItem(String seasonID, String purchaseID, String purchaseItemJson, String sessionToken) throws IOException{
		return doUpdatePurchaseItem(seasonID, BranchesRestApi.MASTER, purchaseID, purchaseItemJson, sessionToken);
	}
	
	public String updatePurchaseItemInBranch(String seasonID, String branchID, String purchaseID, String purchaseItemJson, String sessionToken) throws IOException{
		return doUpdatePurchaseItem(seasonID, branchID, purchaseID, purchaseItemJson, sessionToken);
	}
	
	private String doUpdatePurchaseItem(String seasonID, String branchID, String purchaseID, String purchaseItemJson, String sessionToken)throws IOException{
		String newID = "";
		RestClientUtils.RestCallResults res = RestClientUtils.sendPut(m_url+"/products/seasons/branches/" + branchID + "/entitlements/" + purchaseID, purchaseItemJson, sessionToken);
		String purchase = res.message;
 		newID = parsePurchaseId(purchase);
 		return newID;
	}	
	
	public void convertAllPurchasesToDevStage(String seasonId, String sessionToken) throws JSONException, IOException{
		JSONArray purchaseItems = getPurchasesBySeason(seasonId, sessionToken);
		findPurchaseItemsInProduction(seasonId, purchaseItems, sessionToken);
	}
	
	private void findPurchaseItemsInProduction(String seasonId, JSONArray purchaseItems, String sessionToken) throws JSONException, IOException{
		for (int j=0; j<purchaseItems.size(); j++){
			JSONObject pi = purchaseItems.getJSONObject(j);
			String temp = getPurchaseItem(pi.getString("uniqueId"), sessionToken);
				pi = new JSONObject(temp);

					
				JSONArray configurationRules = new JSONArray();
				if (pi.containsKey("configurationRules"))
					configurationRules = pi.getJSONArray("configurationRules");
				
				if (configurationRules.size() != 0) {
					findPurchaseItemsInProduction(seasonId, configurationRules, sessionToken);				
				}
				
				
				JSONArray subInAppPurchases= new JSONArray();
				if (pi.containsKey("entitlements"))
					subInAppPurchases = pi.getJSONArray("entitlements");				
				if (subInAppPurchases.size() != 0) {
					findPurchaseItemsInProduction(seasonId, subInAppPurchases, sessionToken);
				
				}
				
				JSONArray subPurchaseOptions= new JSONArray();
				if (pi.containsKey("purchaseOptions"))
					subPurchaseOptions = pi.getJSONArray("purchaseOptions");				
				if (subPurchaseOptions.size() != 0) {
					findPurchaseItemsInProduction(seasonId, subPurchaseOptions, sessionToken);
				
				}
				
				if (pi.has("stage") && pi.getString("stage").equals("PRODUCTION")){
					//change stage to development
					String updatedFeature = getPurchaseItem(pi.getString("uniqueId"), sessionToken);
					pi = new JSONObject(updatedFeature);
					String featureId = pi.getString("uniqueId");
					pi.put("stage", "DEVELOPMENT");
					updatePurchaseItem(seasonId, featureId, pi.toString(), sessionToken);
				}
		}	
	}

	public String getBranchRootId(String seasonID, String branchID, String sessionToken){
		return doGetRootId(seasonID, branchID, sessionToken);
	}
	
	private String doGetRootId(String seasonID, String branchID, String sessionToken){
		String rootId = "";
		try {
		 	
			//RestClientUtils.RestCallResults res= RestClientUtils.sendGet(m_url+"/products/seasons/" + seasonID + "/features", sessionToken);
			RestClientUtils.RestCallResults res= RestClientUtils.sendGet(m_url+"/products/seasons/" + seasonID + "/branches/" + branchID + "/entitlements", sessionToken);
			String response = res.message;
		 	JSONObject jsonResponse = new JSONObject(response);
		 	JSONObject root = jsonResponse.getJSONObject("entitlementsRoot");
			rootId = root.getString("uniqueId");
		 			
		} catch (Exception e) {
			Assert.fail("An exception was thrown when trying  to get a list of all purchases in season. Message: "+e.getMessage()) ;
		}				

		return rootId;
	}

	public String getAllPurchaseItems(String saesonID, String sessionToken){
		return doGetAllPurchaseItems(saesonID, BranchesRestApi.MASTER, sessionToken);
	}
	
	public String getAllPurchaseItemsFromBranch(String saesonID, String branchID, String sessionToken){
		return doGetAllPurchaseItems(saesonID, branchID, sessionToken);
	}
	
	private String doGetAllPurchaseItems(String seasonID, String branchID, String sessionToken){
		String response="";
 		try { 			
 			RestClientUtils.RestCallResults res= RestClientUtils.sendGet(m_url+"/products/seasons/" + seasonID + "/branches/" + branchID + "/entitlements", sessionToken);
 			response = res.message;			
		} catch (Exception e) {
			Assert.fail("An exception was thrown when trying  to get all purchase items. Message: "+e.getMessage()) ;
		}
 		return response;
	}
	
	public void setSleep() throws InterruptedException{
		Thread.sleep(2000);
	}
	
	public String setDateFormat() throws InterruptedException{
		Thread.sleep(2000);
		String dateFormat = RuntimeDateUtilities.getCurrentTimeStamp();
		Thread.sleep(2000);
		return dateFormat;
	}
}
