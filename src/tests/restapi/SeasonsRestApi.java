package tests.restapi;


import java.io.IOException;

import org.apache.wink.json4j.JSONArray;
import org.apache.wink.json4j.JSONException;
import org.apache.wink.json4j.JSONObject;
import org.testng.Assert;

import tests.com.ibm.qautils.RestClientUtils;

public class SeasonsRestApi {
	protected static String m_url ;


	public void setURL (String url){
		m_url = url;
	}

	public String addSeason(String productID, String seasonJson) throws IOException{
		String seasonID = "";
		RestClientUtils.RestCallResults res = RestClientUtils.sendPost(m_url+"/products/" + productID + "/seasons/", seasonJson);
		String season = res.message;
		seasonID = parseSeasonId(season);
		return seasonID;
	}
	



	public  String getAllSeasons(){
		String response="";
		try {		 	
			RestClientUtils.RestCallResults res = RestClientUtils.sendGet(m_url+"/products/seasons");
			response = res.message;
		} catch (Exception e) {
			Assert.fail("An exception was thrown when trying  to get a list of all seasons. Messaeg: "+e.getMessage()) ;
		}
		return response;
	}

	public  int deleteSeason(String seasonID){
		int response = -1;
		try {
			RestClientUtils.RestCallResults res = RestClientUtils.sendDelete(m_url+"/products/seasons/" + seasonID);
			response = res.code;
			//Assert.assertEquals(response, 200, "Failed to delete a season.");
		} catch (Exception e) {
			Assert.fail("An exception was thrown when trying  to delete a seasons. Message: "+e.getMessage()) ;
		}
		return response;
	}

	public JSONArray  getSeasonsPerProduct(String productID) throws Exception{
		JSONArray  seasons = new JSONArray ();
		RestClientUtils.RestCallResults res= RestClientUtils.sendGet(m_url+"/products/seasons");
		String response = res.message;
		JSONObject json = new JSONObject(response);
		JSONArray products = (JSONArray)json.get("products");
		String productUniqueId="";
		for(int i=0; i<products.size(); i++){
			JSONObject product = products.getJSONObject(i);
			productUniqueId = (String)product.get("uniqueId");
			if (productUniqueId.equals(productID)){
				seasons = product.getJSONArray("seasons");
			}
		}

		return seasons;

	}

	public String getSeason (String productID, String seasonID) throws Exception{
		JSONObject season = new JSONObject();
		JSONArray  seasons = getSeasonsPerProduct(productID);
		String seasonUniqueId = "";
		for(int i=0; i<seasons.size(); i++){
			JSONObject json = seasons.getJSONObject(i);
			seasonUniqueId = (String)json.get("uniqueId");
			if (seasonUniqueId.equals(seasonID)){
				season = json;
			}
		}		
		return season.toString();
	}

	public String getSeason (String productID, String seasonID, String sessionToken) throws Exception{
		JSONObject season = new JSONObject();
		JSONArray  seasons = getSeasonsPerProduct(productID, sessionToken);
		String seasonUniqueId = "";
		for(int i=0; i<seasons.size(); i++){
			JSONObject json = seasons.getJSONObject(i);
			seasonUniqueId = (String)json.get("uniqueId");
			if (seasonUniqueId.equals(seasonID)){
				season = json;
			}
		}		
		return season.toString();
	}

	public String updateSeason(String seasonID, String json){
		String seasonIDResp="";
		try {
			RestClientUtils.RestCallResults res = RestClientUtils.sendPut(m_url+"/products/seasons/" + seasonID, json);
			String response = res.message;
			seasonIDResp = parseSeasonId(response); 			
		} catch (Exception e) {
			Assert.fail("An exception was thrown when trying  to update a product. Message: "+e.getMessage()) ;
		}
		return seasonIDResp;
	}
	
	public String upgradeSeason(String seasonID, String fromVersion, String sessionToken) throws Exception{
		String seasonIDResp="";
		RestClientUtils.RestCallResults res = RestClientUtils.sendPut(m_url+"/products/seasons/upgrade/" + seasonID + "/" + fromVersion, "", sessionToken);
		String response = res.message;
		seasonIDResp = parseSeasonId(response);
		return seasonIDResp;
	}

	public void reset() {
		try {
			//RestClientUtils.sendPost(m_url+"/products/reset", null);

		} catch (Exception e) {
			Assert.fail("An exception was thrown when trying  to reset the system. Message: "+e.getMessage()) ;
		}
	}

	public boolean isServerRuntimeEncrypted(String sessionToken) throws Exception {
		RestClientUtils.RestCallResults res = RestClientUtils.sendGet(m_url.substring(0, m_url.length()-6)+"/ops/capabilities",sessionToken);
		String capabilitiesStr = res.message;
		JSONObject capabilitiesObj = new JSONObject(capabilitiesStr);
		JSONArray capabilitiesArr = capabilitiesObj.getJSONArray("capabilities");
		for (int i=0; i<capabilitiesArr.size(); i++) {
			if (capabilitiesArr.getString(i).equals("RUNTIME_ENCRYPTION")) {
				return true;
			}
		}
		return false;
	}
	
	public String addSeason(String productID, String seasonJson, String sessionToken) throws IOException{
		try {
			JSONObject seasonObj = new JSONObject(seasonJson);
			seasonObj.put("runtimeEncryption", isServerRuntimeEncrypted(sessionToken));
			String seasonID = "";
			RestClientUtils.RestCallResults res = RestClientUtils.sendPost(m_url+"/products/" + productID + "/seasons/", seasonObj.toString(), sessionToken);
			String season = res.message;
			seasonID = parseSeasonId(season);
			return seasonID;
		}
		catch (Exception e) {
			//throw IOException so wont need to change all calls 
			throw new IOException(e);
		}
	}
	
	public String addSeasonNoEncManipulations(String productID, String seasonJson, String sessionToken) throws IOException{
		try {
			String seasonID = "";
			RestClientUtils.RestCallResults res = RestClientUtils.sendPost(m_url+"/products/" + productID + "/seasons/", seasonJson, sessionToken);
			String season = res.message;
			seasonID = parseSeasonId(season);
			return seasonID;
		}
		catch (Exception e) {
			//throw IOException so wont need to change all calls 
			throw new IOException(e);
		}
	}
	
	public String addSeason(String productID, String seasonID, String seasonJson, String sessionToken) throws IOException{
		try {
			JSONObject seasonObj = new JSONObject(seasonJson);
			seasonObj.put("runtimeEncryption", isServerRuntimeEncrypted(sessionToken));
			RestClientUtils.RestCallResults res = RestClientUtils.sendPost(m_url+"/products/" + productID + "/seasons/" + seasonID, seasonObj.toString(), sessionToken);
			String season = res.message;
			seasonID = parseSeasonId(season);
			return seasonID;
		}
		catch (Exception e) {
			//throw IOException so wont need to change all calls 
			throw new IOException(e);
		}
	}

	public String addSeasonSpecifyEncryption(String productID, String seasonJson, boolean runetimeEncryption, String sessionToken) throws IOException{
		try {
			JSONObject seasonObj = new JSONObject(seasonJson);
			seasonObj.put("runtimeEncryption", runetimeEncryption);
			String seasonID = "";
			RestClientUtils.RestCallResults res = RestClientUtils.sendPost(m_url+"/products/" + productID + "/seasons/", seasonObj.toString(), sessionToken);
			String season = res.message;
			seasonID = parseSeasonId(season);
			return seasonID;
		}
		catch (Exception e) {
			//throw IOException so wont need to change all calls 
			throw new IOException(e);
		}
	}

	public  String getAllSeasons(String sessionToken){
		String response="";
		try {		 	
			RestClientUtils.RestCallResults res = RestClientUtils.sendGet(m_url+"/products/seasons", sessionToken);
			response = res.message;
		} catch (Exception e) {
			Assert.fail("An exception was thrown when trying  to get a list of all seasons. Messaeg: "+e.getMessage()) ;
		}
		return response;
	}

	public  int deleteSeason(String seasonID, String sessionToken){
		int response = -1;
		try {
			RestClientUtils.RestCallResults res = RestClientUtils.sendDelete(m_url+"/products/seasons/" + seasonID, sessionToken);
			response = res.code;
			//Assert.assertEquals(response, 200, "Failed to delete a season.");
		} catch (Exception e) {
			Assert.fail("An exception was thrown when trying  to delete a seasons. Message: "+e.getMessage()) ;
		}
		return response;
	}

	public JSONArray  getSeasonsPerProduct(String productID, String sessionToken) throws Exception{
		JSONArray  seasons = new JSONArray ();
		RestClientUtils.RestCallResults res = RestClientUtils.sendGet(m_url+"/products/seasons", sessionToken);
		String response = res.message;
		JSONObject json = new JSONObject(response);
		JSONArray products = (JSONArray)json.get("products");
		String productUniqueId="";
		for(int i=0; i<products.size(); i++){
			JSONObject product = products.getJSONObject(i);
			productUniqueId = (String)product.get("uniqueId");
			if (productUniqueId.equals(productID)){
				seasons = product.getJSONArray("seasons");
			}
		}

		return seasons;

	}


	public String updateSeason(String seasonID, String json, String sessionToken) throws Exception{
		String seasonIDResp="";
		RestClientUtils.RestCallResults res = RestClientUtils.sendPut(m_url+"/products/seasons/" + seasonID, json, sessionToken);
		String response = res.message;
		seasonIDResp = parseSeasonId(response);
		return seasonIDResp;
	}



	public String getDefaults(String seasonID, String sessionToken) {
		String defaults = "";
		try {
			RestClientUtils.RestCallResults res = RestClientUtils.sendGet(m_url + "/products/seasons/" + seasonID + "/defaults", sessionToken);
			defaults = res.message;
		} catch (Exception e) {
			Assert.fail("Could not get defaults" + e.getMessage());
		}
		return defaults;
	}
	
	public String getConstants(String seasonID, String platform, String sessionToken) {
		//GET /admin/products/seasons/{season-id}/constants
		String defaults = "";
		try {
			RestClientUtils.RestCallResults res = RestClientUtils.sendGet(m_url + "/products/seasons/" + seasonID + "/constants?platform=" + platform, sessionToken);
			defaults = res.message;
		} catch (Exception e) {
			Assert.fail("Could not get constants" + e.getMessage());
		}
		return defaults;
	}
	

	public String getServerVersion(String seasonID, String sessionToken){
		String version = "";
		try {
			RestClientUtils.RestCallResults res = RestClientUtils.sendGet(m_url + "/products/seasons/" + seasonID + "/version", sessionToken);
			version = res.message;
		} catch (Exception e) {
			Assert.fail("Could not get server version" + e.getMessage());
		}
		return version;
	}

	public String getEncryptionKey(String seasonID, String sessionToken){
		String key = "";
		try {
			RestClientUtils.RestCallResults res = RestClientUtils.sendGet(m_url + "/products/seasons/" + seasonID + "/encryptionkey", sessionToken);
			key = res.message;
		} catch (Exception e) {
			Assert.fail("Could not get encryption key" + e.getMessage());
		}
		return key;
	}
	
	public String getEncryptionKeyString(String seasonID, String sessionToken) throws JSONException{
		String res = getEncryptionKey(seasonID, sessionToken);
		JSONObject keyJson = new JSONObject(res);
		return keyJson.getString("encryptionKey");
	}
	
	public String resetEncryptionKey(String seasonID, String sessionToken){
		String key = "";
		try {
			RestClientUtils.RestCallResults res = RestClientUtils.sendPut(m_url + "/products/seasons/" + seasonID + "/encryptionkey", "", sessionToken);
			key = res.message;
		} catch (Exception e) {
			Assert.fail("Could not reset encryption key" + e.getMessage());
		}
		return key;
	}

	private String parseSeasonId(String season){
		String seasonID = "";
		JSONObject response = null;
		try {
			response = new JSONObject(season);
			if (response.containsKey("error")){
				seasonID = season;
			} else {
				seasonID = (String)response.get("uniqueId");
			}
		} catch (JSONException e) {
			seasonID = "Invalid response: " + response;
		}

		return seasonID;

	}
	
	public String getDocumentLinks(String seasonID, String sessionToken) throws Exception {
		RestClientUtils.RestCallResults res = RestClientUtils.sendGet(m_url + "/products/seasons/" + seasonID + "/documentlinks", sessionToken);
		return res.message;

	}

	public String getBranchesUsage(String seasonID, String sessionToken) throws Exception {
		RestClientUtils.RestCallResults res = RestClientUtils.sendGet(m_url + "/products/seasons/" + seasonID + "/branchesusage", sessionToken);
		return res.message;
	}
	
	
}
