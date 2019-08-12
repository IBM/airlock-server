package tests.restapi.scenarios.userRoleSet;

import java.util.LinkedList;

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
import tests.restapi.OperationRestApi;

public class TestUserIdentifierCaseSensitivity {
	protected String seasonID;
	protected String filePath;
	protected String m_url;
	protected String userStr;
	protected String userIdentifierLowerCase;
	protected String userIdentifierUpperCase;
	
	private String userID1;
	private String userID2;
	
	protected OperationRestApi operApi;
	private AirlockUtils baseUtils;
	protected String productID1;
	private String sessionToken = "";
	private LinkedList<String> usersIds = new LinkedList<String>();
	
	@BeforeClass
	@Parameters({"url", "analyticsUrl", "translationsUrl", "operationsUrl", "configPath", "sessionToken", "userName", "userPassword", "appName", "productsToDeleteFile"})
	public void init(String url, String analyticsUrl, String translationsUrl, String c_operationsUrl, String configPath, String sToken, String userName, String userPassword, String appName, String productsToDeleteFile) throws Exception{
		filePath = configPath;
		baseUtils = new AirlockUtils(url, analyticsUrl, translationsUrl, configPath, sToken, userName, userPassword, appName, productsToDeleteFile);
		sessionToken = baseUtils.sessionToken;
		
		productID1 = baseUtils.createProduct();
		baseUtils.printProductToFile(productID1);		
		baseUtils.createSeason(productID1);

		m_url = url;
		operApi = new OperationRestApi();
		operApi.setURL(c_operationsUrl);
		userStr = FileUtils.fileToString(filePath + "airlockUser.txt", "UTF-8", false);
		userIdentifierLowerCase = RandomStringUtils.randomAlphabetic(5) + "@il.ibm.com";
		userIdentifierLowerCase = userIdentifierLowerCase.toLowerCase();
		
		userIdentifierUpperCase = userIdentifierLowerCase;
		userIdentifierUpperCase = userIdentifierLowerCase.toUpperCase();
	}
	
	@Test (description = "Add global user")
	private void addGlobalUser() throws Exception{
		JSONObject json = new JSONObject(userStr);
		JSONArray roles = new JSONArray();
		roles.add("Viewer");
		userID1 = addGlobalUser(json, roles, userIdentifierLowerCase);
		
		json.put("identifier", userIdentifierUpperCase);
		json.put("roles", roles);
		String response = operApi.addGlobalAirlockUser(json.toString(), sessionToken);
		
		Assert.assertTrue(response.contains("error") && response.contains("already exists"), "manage to craete global user twice with upper case");
		
		response = operApi.getUserRoleSets(sessionToken, userIdentifierLowerCase);
		Assert.assertFalse(response.contains("error"), "Fail retrieving user role sets: " + response);	
		
		JSONArray userRoleSets = new JSONObject(response).getJSONArray("userRoleSets");
		Assert.assertTrue(userRoleSets.size() == 1, "wrong number of user role sets");	
		Assert.assertTrue(userRoleSets.getJSONObject(0).getString("uniqueId").equals(userID1), "wrong uniqueId in user role sets");
		Assert.assertTrue(userRoleSets.getJSONObject(0).getJSONArray("roles").size() == 1, "wrong number of roles in user role sets");
		Assert.assertTrue(userRoleSets.getJSONObject(0).getJSONArray("roles").getString(0).equals("Viewer"), "wrong roles in user role sets");
		
		response = operApi.getRolesPerUser(userIdentifierLowerCase, sessionToken);
		Assert.assertFalse(response.contains("error"), "Fail retrieving user roles: " + response);	
		
		JSONArray userRoles = new JSONObject(response).getJSONArray("roles");
		Assert.assertTrue(userRoles.size() == 1, "wrong number of user roles");
		Assert.assertTrue(userRoles.getString(0).equals("Viewer"), "wrong roles in user roles");
		
		//asking for roles and roleSets is case insensitive
		response = operApi.getUserRoleSets(sessionToken, userIdentifierUpperCase);
		Assert.assertFalse(response.contains("error"), "fail retrieving user role sets: " + response);
		
		userRoleSets = new JSONObject(response).getJSONArray("userRoleSets");
		Assert.assertTrue(userRoleSets.size() == 1, "wrong number of user role sets");
		Assert.assertTrue(userRoleSets.getJSONObject(0).getString("uniqueId").equals(userID1), "wrong uniqueId in user role sets");
		Assert.assertTrue(userRoleSets.getJSONObject(0).getJSONArray("roles").size() == 1, "wrong number of roles in user role sets");
		Assert.assertTrue(userRoleSets.getJSONObject(0).getJSONArray("roles").getString(0).equals("Viewer"), "wrong roles in user role sets");
		
		response = operApi.getRolesPerUser(userIdentifierUpperCase, sessionToken);
		Assert.assertFalse(response.contains("error"), "Fail retrieving user roles: " + response);	
		
		userRoles = new JSONObject(response).getJSONArray("roles");
		Assert.assertTrue(userRoles.size() == 1, "wrong number of user roles");
		Assert.assertTrue(userRoles.getString(0).equals("Viewer"), "wrong roles in user roles");
		
		
	}
	
	@Test (dependsOnMethods = "addGlobalUser", description = "Add user to product1")
	private void addUserToProduct1() throws Exception{
		JSONObject json = new JSONObject(userStr);
		JSONArray roles = new JSONArray();
		roles.add("Viewer");
		roles.add("Editor");
		
		userID2 = addUserToProduct(json, roles, userIdentifierLowerCase, productID1);
		
		
		json.put("identifier", userIdentifierUpperCase);
		json.put("roles", roles);
		String response = operApi.addProductAirlockUser(json.toString(), productID1, sessionToken);
				
		response = operApi.getUserRoleSets(sessionToken, userIdentifierLowerCase);
		Assert.assertFalse(response.contains("error"), "Fail retrieving user role sets: " + response);	
		
		JSONArray userRoleSets = new JSONObject(response).getJSONArray("userRoleSets");
		Assert.assertTrue(userRoleSets.size() == 2, "wrong number of user role sets");
		
		int globalUserIndex = getIndexById(userRoleSets, userID1);
		Assert.assertTrue(userRoleSets.getJSONObject(globalUserIndex).getString("uniqueId").equals(userID1), "wrong uniqueId in user role sets");
		Assert.assertTrue(userRoleSets.getJSONObject(globalUserIndex).getJSONArray("roles").size() == 1, "wrong number of roles in user role sets");
		Assert.assertTrue(userRoleSets.getJSONObject(globalUserIndex).getJSONArray("roles").getString(0).equals("Viewer"), "wrong roles in user role sets");
		
		int productUserIndex = getIndexById(userRoleSets, userID2);
		Assert.assertTrue(userRoleSets.getJSONObject(productUserIndex).getString("uniqueId").equals(userID2), "wrong uniqueId in user role sets");
		Assert.assertTrue(userRoleSets.getJSONObject(productUserIndex).getString("productId").equals(productID1), "wrong productId in user role sets");
		Assert.assertTrue(userRoleSets.getJSONObject(productUserIndex).getJSONArray("roles").size() == 2, "wrong number of roles in user role sets");
		Assert.assertTrue(userRoleSets.getJSONObject(productUserIndex).getJSONArray("roles").getString(0).equals("Viewer"), "wrong roles in user role sets");
		Assert.assertTrue(userRoleSets.getJSONObject(productUserIndex).getJSONArray("roles").getString(1).equals("Editor"), "wrong roles in user role sets");
		
		response = operApi.getUserRolesPerProduct(sessionToken, productID1, userIdentifierLowerCase);
		Assert.assertFalse(response.contains("error"), "Fail retrieving user roles: " + response);	
		
		JSONArray userRoles = new JSONObject(response).getJSONArray("roles");
		Assert.assertTrue(userRoles.size() == 2, "wrong number of user roles");
		Assert.assertTrue(userRoles.getString(0).equals("Viewer") || userRoles.getString(0).equals("Editor"), "wrong roles in user roles");
		Assert.assertTrue(userRoles.getString(1).equals("Editor") || userRoles.getString(1).equals("Viewer"), "wrong roles in user roles");
		
		//asking for roles and roleSets is case insensitive
		response = operApi.getUserRoleSets(sessionToken, userIdentifierUpperCase);
		Assert.assertFalse(response.contains("error"), "Fail retrieving user role sets: " + response);	
		
		userRoleSets = new JSONObject(response).getJSONArray("userRoleSets");
		Assert.assertTrue(userRoleSets.size() == 2, "wrong number of user role sets");
		
		globalUserIndex = getIndexById(userRoleSets, userID1);
		Assert.assertTrue(userRoleSets.getJSONObject(globalUserIndex).getString("uniqueId").equals(userID1), "wrong uniqueId in user role sets");
		Assert.assertTrue(userRoleSets.getJSONObject(globalUserIndex).getJSONArray("roles").size() == 1, "wrong number of roles in user role sets");
		Assert.assertTrue(userRoleSets.getJSONObject(globalUserIndex).getJSONArray("roles").getString(0).equals("Viewer"), "wrong roles in user role sets");
		
		productUserIndex = getIndexById(userRoleSets, userID2);
		Assert.assertTrue(userRoleSets.getJSONObject(productUserIndex).getString("uniqueId").equals(userID2), "wrong uniqueId in user role sets");
		Assert.assertTrue(userRoleSets.getJSONObject(productUserIndex).getString("productId").equals(productID1), "wrong productId in user role sets");
		Assert.assertTrue(userRoleSets.getJSONObject(productUserIndex).getJSONArray("roles").size() == 2, "wrong number of roles in user role sets");
		Assert.assertTrue(userRoleSets.getJSONObject(productUserIndex).getJSONArray("roles").getString(0).equals("Viewer"), "wrong roles in user role sets");
		Assert.assertTrue(userRoleSets.getJSONObject(productUserIndex).getJSONArray("roles").getString(1).equals("Editor"), "wrong roles in user role sets");
		
		response = operApi.getUserRolesPerProduct(sessionToken, productID1, userIdentifierUpperCase);
		Assert.assertFalse(response.contains("error"), "Fail retrieving user roles: " + response);	
		
		userRoles = new JSONObject(response).getJSONArray("roles");
		Assert.assertTrue(userRoles.size() == 2, "wrong number of user roles");
		Assert.assertTrue(userRoles.getString(0).equals("Viewer") || userRoles.getString(0).equals("Editor"), "wrong roles in user roles");
		Assert.assertTrue(userRoles.getString(1).equals("Editor") || userRoles.getString(1).equals("Viewer"), "wrong roles in user roles");
		
	}

	private int getIndexById(JSONArray userRoleSets, String userID) throws JSONException {
		for (int i=0; i<userRoleSets.size(); i++) {
			if (userRoleSets.getJSONObject(i).getString("uniqueId").equals(userID))
				return i;
		}
		return -1;
	}
	
	private String addUserToProduct(JSONObject userJson, JSONArray roles, String userIdentifier, String productId) throws Exception{
		userJson.put("identifier", userIdentifier);
		userJson.put("roles", roles);
		String response = operApi.addProductAirlockUser(userJson.toString(), productId, sessionToken);
		Assert.assertFalse(response.contains("error"), "user creation failed: " + response);
		
		usersIds.add(response);	
		return response;
	}
	
	private String addGlobalUser(JSONObject userJson, JSONArray roles, String userIdentifier) throws Exception{
		userJson.put("identifier", userIdentifier);
		userJson.put("roles", roles);
		String response = operApi.addGlobalAirlockUser(userJson.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "user creation failed: " + response);
		
		usersIds.add(response);	
		return response;
	}
	
	
	@AfterTest
	public void reset() throws Exception{
		baseUtils.reset(productID1, sessionToken);
		
		for (String userId:usersIds) {
			operApi.deleteAirlockUser(userId, sessionToken);
		}
	}

	

}
