package tests.restapi;



import tests.com.ibm.qautils.FileUtils;
import tests.com.ibm.qautils.RestClientUtils;

import java.io.IOException;

import org.apache.wink.json4j.JSONArray;
import org.apache.wink.json4j.JSONException;
import org.apache.wink.json4j.JSONObject;
import org.testng.Assert;



public class OperationRestApi {

	protected static String m_url ;

	public void setURL (String url){
		m_url = url;
	}
	

	public String getRoles(String sessionToken) throws Exception {
		RestClientUtils.RestCallResults res = RestClientUtils.sendGet(m_url+"/roles",sessionToken);
		return res.message;
	}

	public String setRoles(String body ,String sessionToken) throws IOException {
		RestClientUtils.RestCallResults res = RestClientUtils.sendPut(m_url+"/roles", body,sessionToken);
		return res.message;
	}

	public String getAirlockUsers(String sessionToken) throws Exception {
		RestClientUtils.RestCallResults res = RestClientUtils.sendGet(m_url+"/userrolesets",sessionToken);
		return res.message;
	}
	
	public String getProductAirlockUsers(String sessionToken, String productId) throws Exception {
		RestClientUtils.RestCallResults res = RestClientUtils.sendGet(m_url+"/products/" + productId +"/userrolesets",sessionToken);
		return res.message;
	}
	public String getUserRolesPerProduct(String sessionToken, String productId, String user) throws Exception {
		JSONObject payload = new JSONObject();
		payload.put("user", user);
		RestClientUtils.RestCallResults res = RestClientUtils.sendPost(m_url+"/products/" + productId +"/airlockuser/roles", payload.toString(), sessionToken);
		return res.message;
	}
	
	public String getUserRoleSets(String sessionToken, String user) throws Exception {
		JSONObject payload = new JSONObject();
		payload.put("identifier", user);
		RestClientUtils.RestCallResults res = RestClientUtils.sendPost(m_url +"/userrolesets/user", payload.toString(), sessionToken);
		return res.message;
	}
	
	public String addGlobalAirlockUser(String body, String sessionToken) throws Exception{
		RestClientUtils.RestCallResults res = RestClientUtils.sendPost(m_url+"/userrolesets", body, sessionToken);
		String user = res.message;
		String userID = parseUserId(user);
 		return userID;
	}
	
	private String parseUserId(String user){
		String featureID = "";
		JSONObject response = null;
		try {
			response = new JSONObject(user);
			
			if (response.containsKey("error")){
				featureID = user;
			} else {
				featureID = (String)response.get("uniqueId");
			}
		} catch (JSONException e) {
			featureID = "Invalid response: " + response;
		}

		return featureID;
	}

	public String addProductAirlockUser(String body, String productId, String sessionToken) throws Exception{
		RestClientUtils.RestCallResults res = RestClientUtils.sendPost(m_url+"/products/" + productId + "/userrolesets", body, sessionToken);
		//return res.message;
		String user = res.message;
		String userID = parseUserId(user);
 		return userID;
	}
	
	public int deleteAirlockUser(String userId, String sessionToken) throws Exception{
		RestClientUtils.RestCallResults res = RestClientUtils.sendDelete(m_url+ "/userrolesets/" + userId, sessionToken);
		return res.code;
	}
	
	public String getAirlockUser(String userId, String sessionToken) throws Exception{
		RestClientUtils.RestCallResults res = RestClientUtils.sendGet(m_url+ "/userrolesets/" + userId, sessionToken);
		return res.message;
	}
	
	public String updateAirlockUser(String userId, String body, String sessionToken) throws Exception{
		RestClientUtils.RestCallResults res = RestClientUtils.sendPut(m_url+ "/userrolesets/" + userId, body, sessionToken);
		return res.message;
	}

	//GET /ops/healthcheck
	public int healthcheck(String sessionToken) throws Exception {
		RestClientUtils.RestCallResults res = RestClientUtils.sendGet(m_url+"/healthcheck", sessionToken);
		return res.code;
	}
	
	/*
	 * Airlock Key Rest Api
	 */
	
	//POST /ops/airlockkeys
	public String generateAirlockKey(String content, String sessionToken) throws IOException{
		RestClientUtils.RestCallResults res = RestClientUtils.sendPost(m_url+"/airlockkeys", content, sessionToken);
		return parseUniqueId(res.message);
	}
	
	public String generateAirlockKeyCompleteResponse(String content, String sessionToken) throws IOException{
		RestClientUtils.RestCallResults res = RestClientUtils.sendPost(m_url+"/airlockkeys", content, sessionToken);
		return res.message;
	}
	
	//GET /ops/airlockkeys
	public String getAllKeys(String owner, String sessionToken) throws Exception{
		String perOwner = "";
		if (owner != null)
			perOwner = "?owner=" + owner;
		RestClientUtils.RestCallResults res = RestClientUtils.sendGet(m_url+"/airlockkeys" + perOwner, sessionToken);
		return res.message;
	}
	
	//PUT /ops/airlockkeys/{key-id}
	public String updateKey(String keyId, String keyContent, String sessionToken) throws Exception{
		RestClientUtils.RestCallResults res = RestClientUtils.sendPut(m_url+"/airlockkeys/" + keyId, keyContent, sessionToken);
		return parseUniqueId(res.message);
	}
	
	//DELETE /ops/airlockkeys/{key-id}
	public int deleteKey(String keyId, String sessionToken) throws Exception{
		RestClientUtils.RestCallResults res = RestClientUtils.sendDelete(m_url+"/airlockkeys/" + keyId, sessionToken);
		return res.code;
	}
	
	//GET /ops/airlockkeys/{key-id}
	public String getKey(String keyId, String sessionToken) throws Exception{
		RestClientUtils.RestCallResults res = RestClientUtils.sendGet(m_url+"/airlockkeys/" + keyId,sessionToken);
		return res.message;
	}
	
	//POST /ops/airlockuser/roles
	public String getRolesPerUser(String user, String sessionToken) throws Exception{
		String content = "{\"user\":\"" + user + "\"}";
		RestClientUtils.RestCallResults res = RestClientUtils.sendPost(m_url+"/airlockuser/roles", content,sessionToken);
		return res.message;
	}
	
	//GET /airlock/api/admin/authentication/startSessionFromKey/{apikey}/{password}
	public String startSessionFromKey(String apiKey, String keyPassword) throws Exception{
		String adminUrl = m_url.replace("airlock/api/ops", "airlock/api/admin");
		String content = buildSessionFromKeyPayload(apiKey, keyPassword);
		RestClientUtils.RestCallResults res = RestClientUtils.sendPost(adminUrl+"/authentication/startSessionFromKey/", content);
		return res.message;
	}
	
	/************* CAPABILITIES ************/
	
	//GET /ops/capabilities
	public String getCapabilities(String sessionToken) throws Exception{
		RestClientUtils.RestCallResults res = RestClientUtils.sendGet(m_url+"/capabilities",sessionToken);
		return res.message;
	}
	
	public String setCapabilities(String input, String sessionToken) throws Exception{
		RestClientUtils.RestCallResults res = RestClientUtils.sendPut(m_url+"/capabilities", input, sessionToken);
		return res.message;
	}
	
	//get season capabilties
	public String getSeasonCapabilities(String seasonId, String sessionToken) throws Exception{
		RestClientUtils.RestCallResults res = RestClientUtils.sendGet(m_url + "/seasons/"+ seasonId + "/capabilities",sessionToken);
		return res.message;
	}
	
	
	public String parseUniqueId(String content){
		String keyID = "";
		JSONObject response = null;
		try {
			response = new JSONObject(content);
			
			if (response.containsKey("error")){
				keyID = content;
			} else {
				keyID = (String)response.get("uniqueId");
			}
		} catch (JSONException e) {
			keyID = "Invalid response: " + response;
		}

		return keyID;
	}
	
	private String buildSessionFromKeyPayload(String apiKey, String password){
		return "{\"key\": \"" + apiKey + "\", \"keyPassword\": \"" + password + "\"}";
	}
	
	public String resetUsersFromList(String filName, String token) throws Exception{
			
		//retrieve users list
		String originalUsers = FileUtils.fileToString(filName, "UTF-8", false);
		JSONArray newUsersArray = new JSONArray(originalUsers);
		
		//existing users
		String response = getAirlockUsers(token);
		JSONArray existingUsers = new JSONObject(response).getJSONArray("users");
		for (int i=0; i<newUsersArray.size(); i++) {
			boolean found = false;
			JSONObject newUser = newUsersArray.getJSONObject(i);
			String newUserIdentifier = newUser.getString("identifier");
			//find the user id in airlock and delete it before adding it
			for (int j=0; j<existingUsers.size(); j++) {
				if (existingUsers.getJSONObject(j).getString("identifier").equals(newUserIdentifier)) {
					existingUsers.getJSONObject(j).put("roles", newUser.getJSONArray("roles"));
					//deleteAirlockUser(existingUsers.getJSONObject(j).getString("uniqueId"), token);
					updateAirlockUser(existingUsers.getJSONObject(j).getString("uniqueId"), existingUsers.getJSONObject(j).toString(), token);
					found = true;
					break;
				}
			}
			
			if (!found) {
				//add the new user
				addGlobalAirlockUser(newUser.toString(), token);
			}
		}
		return "";
	}
	
	public String resetUsersFromListForProduct(String productId, String filName, String token) throws Exception{
		
		//retrieve users list
		String originalUsers = FileUtils.fileToString(filName, "UTF-8", false);
		JSONArray newUsersArray = new JSONArray(originalUsers);
		
		//existing users
		String response = getProductAirlockUsers(token, productId);
		JSONArray existingUsers = new JSONObject(response).getJSONArray("users");
		for (int i=0; i<newUsersArray.size(); i++) {
			boolean found = false;
			JSONObject newUser = newUsersArray.getJSONObject(i);
			String newUserIdentifier = newUser.getString("identifier");
			//find the user id in airlock and delete it before adding it
			for (int j=0; j<existingUsers.size(); j++) {
				if (existingUsers.getJSONObject(j).getString("identifier").equals(newUserIdentifier)) {
					existingUsers.getJSONObject(j).put("roles", newUser.getJSONArray("roles"));
					updateAirlockUser(existingUsers.getJSONObject(j).getString("uniqueId"), existingUsers.getJSONObject(j).toString(), token);
					found = true;
					break;
				}
			}
			
			if (!found) {
				//add the new user
				addProductAirlockUser(newUser.toString(), productId, token);
			}
		}
		return "";
		/*
		//retrieve users list
		String originalUsers = FileUtils.fileToString(filName, "UTF-8", false);
		JSONArray newUsersArray = new JSONArray(originalUsers);
		
		//existing users
		String response = getProductAirlockUsers(token, productId);
		JSONArray existingUsers = new JSONObject(response).getJSONArray("users");
		for (int i=0; i<newUsersArray.size(); i++) {
			JSONObject newUser = newUsersArray.getJSONObject(i);
			String newUserId = newUser.getString("identifier");
			//find the user id in airlock and delete it before adding it
			for (int j=0; j<existingUsers.size(); j++) {
				if (existingUsers.getJSONObject(j).getString("identifier").equals(newUserId)) {
					deleteAirlockUser(newUserId, token);
					break;
				}
			}
			
			//add the new user
			addProductAirlockUser(newUser.toString(), productId, token);
		}
		return "";*/
	}

	public void removeUserRole (String role, String userIdentifier, String token) throws Exception{
		String response = getAirlockUsers(token);
		JSONObject allUsers = new JSONObject(response);
		JSONArray users = allUsers.getJSONArray("users");
		
		for (int i=0; i< users.size(); i++){
			JSONObject user = users.getJSONObject(i);
			if (user.getString("identifier").equals(userIdentifier)){
				JSONArray userExistingRoles = user.getJSONArray("roles");
				JSONArray userNewRoles = new JSONArray();
				for (int j=0; j<userExistingRoles.size(); j++) {
					if (!userExistingRoles.getString(j).equals(role)) {
						userNewRoles.add(userExistingRoles.getString(j));
					}
				}
				if (userNewRoles.size() == 0) {
					int code = deleteAirlockUser(user.getString("uniqueId"), token);
					Assert.assertEquals(code, 200, "Can't update user roles. Cannot delete user with no roles");
				}
				else {
					user.put("roles", userNewRoles);
					String resp = updateAirlockUser(user.getString("uniqueId"), user.toString(), token);
					Assert.assertFalse(resp.contains("error"), "Can't update user roles: " + resp);
				}
			}
		} 

	}
	
	//if product in null - remove global role set
	public void removeUserRoleSet (String userIdentifier, String token, String productID) throws Exception{
		String response = productID==null?getAirlockUsers(token):getProductAirlockUsers(token, productID);
		JSONObject allUsers = new JSONObject(response);
		JSONArray users = allUsers.getJSONArray("users");
		
		for (int i=0; i< users.size(); i++){
			JSONObject user = users.getJSONObject(i);
			if (user.getString("identifier").equals(userIdentifier)){
				String roleSetId = user.getString("uniqueId");
				deleteAirlockUser(roleSetId, token);
			}
		} 
	}

	
	public void removeUserRoleFromProduct (String role, String userIdentifier, String token, String productID) throws Exception{
		String response = getProductAirlockUsers(token, productID);
		JSONObject allUsers = new JSONObject(response);
		JSONArray users = allUsers.getJSONArray("users");
		
		for (int i=0; i< users.size(); i++){
			JSONObject user = users.getJSONObject(i);
			if (user.getString("identifier").equals(userIdentifier)){
				JSONArray userExistingRoles = user.getJSONArray("roles");
				JSONArray userNewRoles = new JSONArray();
				for (int j=0; j<userExistingRoles.size(); j++) {
					if (!userExistingRoles.getString(j).equals(role)) {
						userNewRoles.add(userExistingRoles.getString(j));
					}
				}
				if (userNewRoles.size() == 0) {
					int code = deleteAirlockUser(user.getString("uniqueId"), token);
					Assert.assertEquals(code, 200, "Can't update user roles. Cannot delete user with no roles");
				}
				else {
					user.put("roles", userNewRoles);
					String resp = updateAirlockUser(user.getString("uniqueId"), user.toString(), token);
					Assert.assertFalse(resp.contains("error"), "Can't update user roles: " + resp);
				}
			}
		} 

	}

	public void addUserRole (String role, String userIdentifier, String token) throws Exception{
		String response = getAirlockUsers(token);
		JSONObject allUsers = new JSONObject(response);
		JSONArray users = allUsers.getJSONArray("users");
		
		for (int i=0; i< users.size(); i++){
			JSONObject user = users.getJSONObject(i);
			if (user.getString("identifier").equals(userIdentifier)){
				JSONArray userExistingRoles = user.getJSONArray("roles");
				userExistingRoles.add(role);
				user.put("roles", userExistingRoles);
				String resp = updateAirlockUser(user.getString("uniqueId"), user.toString(), token);
				Assert.assertFalse(resp.contains("error"), "Can't update user roles: " + resp);
			}
		} 

	}
	
	/* Webhooks */
	
	public String getWebhooks(String sessionToken) throws Exception {
		RestClientUtils.RestCallResults res = RestClientUtils.sendGet(m_url+"/webhooks",sessionToken);
		return res.message;
	}
	
	public String addWebhook(String body ,String sessionToken) throws IOException {
		RestClientUtils.RestCallResults res = RestClientUtils.sendPost(m_url+"/webhooks", body,sessionToken);
		return res.message;
	}
	
	public int deleteWebhook(String webhookId, String sessionToken) throws Exception{
		RestClientUtils.RestCallResults res = RestClientUtils.sendDelete(m_url+ "/webhooks/" + webhookId, sessionToken);
		return res.code;
	}
	
	public String updateWebhook(String webhookId, String body ,String sessionToken) throws IOException {
		RestClientUtils.RestCallResults res = RestClientUtils.sendPut(m_url+"/webhooks/" + webhookId, body,sessionToken);
		return res.message;
	}
	
	
	public void addUserProductRole (String role, String userIdentifier, String token, String productID) throws Exception{
		String response = getProductAirlockUsers(token, productID);
		JSONObject allUsers = new JSONObject(response);
		JSONArray users = allUsers.getJSONArray("users");
		
		for (int i=0; i< users.size(); i++){
			JSONObject user = users.getJSONObject(i);
			if (user.getString("identifier").equals(userIdentifier)){
				JSONArray userExistingRoles = user.getJSONArray("roles");
				userExistingRoles.add(role);
				user.put("roles", userExistingRoles);
				String resp = updateAirlockUser(user.getString("uniqueId"), user.toString(), token);
				Assert.assertFalse(resp.contains("error"), "Can't update user roles: " + resp);
			}
		} 

	}
	
	
}
