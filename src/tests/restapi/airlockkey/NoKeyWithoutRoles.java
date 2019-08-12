package tests.restapi.airlockkey;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.wink.json4j.JSONArray;
import org.apache.wink.json4j.JSONException;
import org.apache.wink.json4j.JSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import tests.com.ibm.qautils.FileUtils;
import tests.restapi.AirlockUtils;
import tests.restapi.FeaturesRestApi;
import tests.restapi.OperationRestApi;


public class NoKeyWithoutRoles {
	
	protected String sessionToken;
	protected String adminToken;
	protected String m_url;
	protected String operationsUrl;
	protected String translationUrl;
	protected String analyticsUrl;
	protected String adminUser;
	protected String adminPassword;
	protected String m_appName;
	protected String config;
	protected AirlockUtils baseUtils; 
	protected String apikey;
	protected OperationRestApi operApi;
	private FeaturesRestApi f;
	private String productLeadUser;
	private String productLeadPassword;
	
	@BeforeClass
	@Parameters({"url","translationsUrl","analyticsUrl","configPath", "operationsUrl","admin","adminPass","appName","editor","editorPass","productLead", "productLeadPass","productsToDeleteFile"})
	public void init(String url,String t_url,String a_url, String configPath, String c_operationsUrl,String admin,String adminPass, String appName, String editor, String editorPass, String productLead, String productLeadPass, String productsToDeleteFile) throws Exception{
		m_url = url;
		operationsUrl = c_operationsUrl;
		translationUrl = t_url;
		analyticsUrl = a_url;
		config = configPath;
		adminUser = admin;
		adminPassword = adminPass;
		productLeadUser = productLead;
		productLeadPassword = productLeadPass;

		operApi = new OperationRestApi();
		operApi.setURL(c_operationsUrl);
		f = new FeaturesRestApi();
		f.setURL(m_url);
		
		baseUtils = new AirlockUtils(m_url, a_url, t_url, configPath, "", adminUser, adminPassword, m_appName, productsToDeleteFile);
		if(appName != null) {
			m_appName = appName;
		}
		
		adminToken = baseUtils.setNewJWTToken(adminUser, adminPassword, m_appName);
		sessionToken = baseUtils.setNewJWTToken(productLeadUser, productLeadPassword, m_appName);

		if (adminToken == null){
			Assert.fail("Can't set adminToken");
		}
		
		apikey = FileUtils.fileToString(config + "airlockkey/key_template1.txt", "UTF-8", false);

		removeUserFromExistingProducts(productLeadUser);
		
		removeUserFromExistingProducts("*@weather.com");
		operApi.removeUserRoleSet("*@weather.com", adminToken, null);
 		
	}
	
	private void removeUserFromExistingProducts(String user) throws Exception {
		//get all user's role sets
		String response = operApi.getUserRoleSets(adminToken, user);
		Assert.assertFalse(response.contains("error"), "Fail retrieving user role sets: " + response);	
		
		JSONArray userRoleSets = new JSONObject(response).getJSONArray("userRoleSets");
		 
		for (int i=0; i<userRoleSets.size(); i++) {
			JSONObject roleSet = userRoleSets.getJSONObject(i);
			if (roleSet.get("productId")!=null) {
				String roleSetId = roleSet.getString("uniqueId");
				int code = operApi.deleteAirlockUser(roleSetId, adminToken);
				Assert.assertEquals(code, 200, "Cannot delete user roles");
			}
		}
	}

	/*
	 * ProductLead creates a key only for role Editor. Delete user from Editor role. His key must be deleted
	 */
	
	@Test (description="create and delete key for a subset of roles")
	public void editorRole() throws Exception{
		operApi.removeUserRoleSet("*@weather.com", adminToken, null);
 		
		JSONObject json = populateKeyName();
		JSONArray roles = new JSONArray();
		roles.add("Editor");
		json.put("roles", roles);
		
		String keyID = operApi.generateAirlockKey(json.toString(), sessionToken);
		Assert.assertFalse(keyID.contains("error"), "Can't create api key: " + keyID);
		
		operApi.removeUserRole("Editor", productLeadUser, adminToken); //remove editor but it stays since user is product lead
		
		String response = operApi.getKey(keyID, adminToken);
		Assert.assertFalse(response.contains("error"), "Api key was deleted: " + response);
		
		String rolesPerUser = operApi.getAllKeys(productLeadUser, adminToken);
		JSONArray keys = new JSONObject(rolesPerUser).getJSONArray("airlockAPIKeys");
		Assert.assertTrue(keys.size()==1, "User keysswas deleted");
		
		json = populateKeyName();
		roles = new JSONArray();
		roles.add("ProductLead");
		json.put("roles", roles);
		
		keyID = operApi.generateAirlockKey(json.toString(), sessionToken);
		Assert.assertFalse(keyID.contains("error"), "Can't create api key: " + keyID);
		
		operApi.removeUserRole("ProductLead", productLeadUser, adminToken); //remove editor but it stays since user is product lead
		response = operApi.getKey(keyID, adminToken);
		Assert.assertFalse(response.contains("error"), "Api key was deleted: " + response);
		JSONObject apiKeyObj = new JSONObject(response);
		Assert.assertTrue(apiKeyObj.getJSONArray("roles").size() == 2, "wrong number of roles"); //editor and viewer
		
		
		operApi.removeUserRole("Editor", productLeadUser, adminToken); //remove editor but it stays since user is product lead
		response = operApi.getKey(keyID, adminToken);
		Assert.assertFalse(response.contains("error"), "Api key was deleted: " + response);
		apiKeyObj = new JSONObject(response);
		Assert.assertTrue(apiKeyObj.getJSONArray("roles").size() == 1, "wrong number of roles"); //viewer
		
		operApi.removeUserRole("AnalyticsViewer", productLeadUser, adminToken); //remove editor but it stays since user is product lead
		response = operApi.getKey(keyID, adminToken);
		Assert.assertFalse(response.contains("error"), "Api key was deleted: " + response);
		apiKeyObj = new JSONObject(response);
		Assert.assertTrue(apiKeyObj.getJSONArray("roles").size() == 1, "wrong number of roles");  //viewer
		
		operApi.removeUserRole("TranslationSpecialist", productLeadUser, adminToken); //remove editor but it stays since user is product lead
		response = operApi.getKey(keyID, adminToken);
		Assert.assertFalse(response.contains("error"), "Api key was deleted: " + response);
		apiKeyObj = new JSONObject(response);
		Assert.assertTrue(apiKeyObj.getJSONArray("roles").size() == 1, "wrong number of roles");  //viewer
		
		operApi.removeUserRole("AnalyticsEditor", productLeadUser, adminToken); //remove editor but it stays since user is product lead
		response = operApi.getKey(keyID, adminToken);
		Assert.assertFalse(response.contains("error"), "Api key was deleted: " + response);
		apiKeyObj = new JSONObject(response);
		Assert.assertTrue(apiKeyObj.getJSONArray("roles").size() == 1, "wrong number of roles");  //viewer
		
		operApi.removeUserRole("Viewer", productLeadUser, adminToken); //remove editor but it stays since user is product lead
		
		response = operApi.getKey(keyID, adminToken);
		Assert.assertTrue(response.contains("error"), "Api key was not deleted: " + response);
		
		rolesPerUser = operApi.getAllKeys(productLeadUser, adminToken);
		keys = new JSONObject(rolesPerUser).getJSONArray("airlockAPIKeys");
		Assert.assertTrue(keys.size()==0, "all user keys were deleted");
		
	}
	
	@AfterTest (alwaysRun=true)
	private void reset() throws Exception{
		operApi.resetUsersFromList(config + "airlockkey/original_users.txt", adminToken);
		baseUtils.deleteKeys(null);
	}
	
	
	
	private JSONObject populateKeyName() throws JSONException{
		JSONObject json = new JSONObject(apikey);
		json.put("key", RandomStringUtils.randomAlphabetic(3));
		return json;

	}

}
