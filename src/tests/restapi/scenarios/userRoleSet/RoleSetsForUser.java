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

public class RoleSetsForUser {
	protected String seasonID;
	protected String filePath;
	protected String m_url;
	protected String userStr;
	protected String userIdentifier;
	
	private String userID1;
	private String userID2;
	private String userID3;
	
	protected OperationRestApi operApi;
	private AirlockUtils baseUtils;
	protected String productID1;
	protected String productID2;
	protected String productID3;
	private String sessionToken = "";
	private LinkedList<String> usersIds = new LinkedList<String>();
	
	@BeforeClass
	@Parameters({"url", "analyticsUrl", "translationsUrl", "operationsUrl", "configPath", "sessionToken", "userName", "userPassword", "appName", "productsToDeleteFile"})
	public void init(String url, String analyticsUrl, String translationsUrl, String c_operationsUrl, String configPath, String sToken, String userName, String userPassword, String appName, String productsToDeleteFile) throws Exception{
		filePath = configPath;
		baseUtils = new AirlockUtils(url, analyticsUrl, translationsUrl, configPath, sToken, userName, userPassword, appName, productsToDeleteFile);
		sessionToken = baseUtils.sessionToken;
		
		productID1 = baseUtils.createProductCopyGlobalAdmins();
		baseUtils.printProductToFile(productID1);		
		baseUtils.createSeason(productID1);

		m_url = url;
		operApi = new OperationRestApi();
		operApi.setURL(c_operationsUrl);
		userStr = FileUtils.fileToString(filePath + "airlockUser.txt", "UTF-8", false);
		userIdentifier = RandomStringUtils.randomAlphabetic(5) + "@il.ibm.com";
	}
	
	@Test (description = "Add global user")
	private void addGlobalUser() throws Exception{
		JSONObject json = new JSONObject(userStr);
		JSONArray roles = new JSONArray();
		roles.add("Administrator");
		userID1 = addGlobalUser(json, roles, userIdentifier);
		
		String response = operApi.getUserRoleSets(sessionToken, userIdentifier);
		Assert.assertFalse(response.contains("error"), "Fail retrieving user role sets: " + response);	
		
		JSONArray userRoleSets = new JSONObject(response).getJSONArray("userRoleSets");
		Assert.assertTrue(userRoleSets.size() == 1, "wrong number of user role sets");
		
		Assert.assertTrue(userRoleSets.getJSONObject(0).getString("uniqueId").equals(userID1), "wrong uniqueId in user role sets");
		//Assert.assertTrue(userRoleSets.getJSONObject(0).getString("productId") == null, "wrong productId in user role sets");
		//Assert.assertTrue(userRoleSets.getJSONObject(0).getJSONArray("roles").size() == 7, "wrong number of roles in user role sets");
		Assert.assertTrue(userRoleSets.getJSONObject(0).getJSONArray("roles").getString(0).equals("Administrator"), "wrong roles in user role sets");
	}
	
	@Test (dependsOnMethods = "addGlobalUser", description = "Add user to product1")
	private void addUserToProduct1() throws Exception{
		JSONObject json = new JSONObject(userStr);
		JSONArray roles = new JSONArray();
		roles.add("Viewer");
		roles.add("Editor");
		
		userID2 = addUserToProduct(json, roles, userIdentifier, productID1);
		
		String response = operApi.getUserRoleSets(sessionToken, userIdentifier);
		Assert.assertFalse(response.contains("error"), "Fail retrieving user role sets: " + response);	
		
		JSONArray userRoleSets = new JSONObject(response).getJSONArray("userRoleSets");
		Assert.assertTrue(userRoleSets.size() == 2, "wrong number of user role sets");
		
		int globalUserIndex = getIndexById(userRoleSets, userID1);
		Assert.assertTrue(userRoleSets.getJSONObject(globalUserIndex).getString("uniqueId").equals(userID1), "wrong uniqueId in user role sets");
		//Assert.assertTrue(userRoleSets.getJSONObject(globalUserIndex).getJSONArray("roles").size() == 1, "wrong number of roles in user role sets");
		Assert.assertTrue(userRoleSets.getJSONObject(globalUserIndex).getJSONArray("roles").getString(0).equals("Administrator"), "wrong roles in user role sets");
		
		int productUserIndex = getIndexById(userRoleSets, userID2);
		Assert.assertTrue(userRoleSets.getJSONObject(productUserIndex).getString("uniqueId").equals(userID2), "wrong uniqueId in user role sets");
		Assert.assertTrue(userRoleSets.getJSONObject(productUserIndex).getString("productId").equals(productID1), "wrong productId in user role sets");
		Assert.assertTrue(userRoleSets.getJSONObject(productUserIndex).getJSONArray("roles").size() == 2, "wrong number of roles in user role sets");
		Assert.assertTrue(userRoleSets.getJSONObject(productUserIndex).getJSONArray("roles").getString(0).equals("Viewer"), "wrong roles in user role sets");
		Assert.assertTrue(userRoleSets.getJSONObject(productUserIndex).getJSONArray("roles").getString(1).equals("Editor"), "wrong roles in user role sets");
	}
	
	private int getIndexById(JSONArray userRoleSets, String userID) throws JSONException {
		for (int i=0; i<userRoleSets.size(); i++) {
			if (userRoleSets.getJSONObject(i).getString("uniqueId").equals(userID))
				return i;
		}
		return -1;
	}

	@Test (dependsOnMethods = "addUserToProduct1", description = "craete product2 and look for user")
	private void createProduct2() throws Exception{
		productID2 = baseUtils.createProductCopyGlobalAdmins();
		baseUtils.printProductToFile(productID2);
		baseUtils.createSeason(productID2);
		
		String response = operApi.getUserRoleSets(sessionToken, userIdentifier);
		Assert.assertFalse(response.contains("error"), "Fail retrieving user role sets: " + response);	
		
		JSONArray userRoleSets = new JSONObject(response).getJSONArray("userRoleSets");
		Assert.assertTrue(userRoleSets.size() == 3, "wrong number of user role sets");
		
		int globalUserIndex = getIndexById(userRoleSets, userID1);		
		Assert.assertTrue(userRoleSets.getJSONObject(globalUserIndex).getString("uniqueId").equals(userID1), "wrong uniqueId in user role sets");
		//Assert.assertTrue(userRoleSets.getJSONObject(globalUserIndex).getJSONArray("roles").size() == 1, "wrong number of roles in user role sets");
		Assert.assertTrue(userRoleSets.getJSONObject(globalUserIndex).getJSONArray("roles").getString(0).equals("Administrator"), "wrong roles in user role sets");
		
		int product1UserIndex = getIndexById(userRoleSets, userID2);
		Assert.assertTrue(userRoleSets.getJSONObject(product1UserIndex).getString("uniqueId").equals(userID2), "wrong uniqueId in user role sets");
		Assert.assertTrue(userRoleSets.getJSONObject(product1UserIndex).getString("productId").equals(productID1), "wrong productId in user role sets");
		Assert.assertTrue(userRoleSets.getJSONObject(product1UserIndex).getJSONArray("roles").size() == 2, "wrong number of roles in user role sets");
		Assert.assertTrue(userRoleSets.getJSONObject(product1UserIndex).getJSONArray("roles").getString(0).equals("Viewer"), "wrong roles in user role sets");
		Assert.assertTrue(userRoleSets.getJSONObject(product1UserIndex).getJSONArray("roles").getString(1).equals("Editor"), "wrong roles in user role sets");
		
		int product2Index = -1;
		if (product1UserIndex!=0 && globalUserIndex!=0)
			product2Index = 0;
		else if (product1UserIndex!=1 && globalUserIndex!=1)
			product2Index = 1;
		else if (product1UserIndex!=2 && globalUserIndex!=2)
			product2Index = 2;
		
		userID3 = userRoleSets.getJSONObject(product2Index).getString("uniqueId");
		Assert.assertTrue(!userID3.equals(userID2) && !userID3.equals(userID1), "wrong uniqueId in user role sets");
		Assert.assertTrue(userRoleSets.getJSONObject(product2Index).getString("productId").equals(productID2), "wrong productId in user role sets");
		//Assert.assertTrue(userRoleSets.getJSONObject(product2Index).getJSONArray("roles").size() == 1, "wrong number of roles in user role sets");
		Assert.assertTrue(userRoleSets.getJSONObject(product2Index).getJSONArray("roles").getString(0).equals("Administrator"), "wrong roles in user role sets");
	}
	
	@Test (dependsOnMethods = "createProduct2", description = "delete product1 and look for user role sets")
	private void deleteProduct1() throws Exception{
		baseUtils.reset(productID1, sessionToken);
		
		String response = operApi.getUserRoleSets(sessionToken, userIdentifier);
		Assert.assertFalse(response.contains("error"), "Fail retrieving user role sets: " + response);	
		
		JSONArray userRoleSets = new JSONObject(response).getJSONArray("userRoleSets");
		Assert.assertTrue(userRoleSets.size() == 2, "wrong number of user role sets");
		
		int globalUserIndex = getIndexById(userRoleSets, userID1);		
		Assert.assertTrue(userRoleSets.getJSONObject(globalUserIndex).getString("uniqueId").equals(userID1), "wrong uniqueId in user role sets");
		//Assert.assertTrue(userRoleSets.getJSONObject(globalUserIndex).getJSONArray("roles").size() == 1, "wrong number of roles in user role sets");
		Assert.assertTrue(userRoleSets.getJSONObject(globalUserIndex).getJSONArray("roles").getString(0).equals("Administrator"), "wrong roles in user role sets");
		
		int product2Index = getIndexById(userRoleSets, userID3);
		Assert.assertTrue(userRoleSets.getJSONObject(product2Index).getString("uniqueId").equals(userID3), "wrong uniqueId in user role sets");
		Assert.assertTrue(userRoleSets.getJSONObject(product2Index).getString("productId").equals(productID2), "wrong productId in user role sets");
		//Assert.assertTrue(userRoleSets.getJSONObject(product2Index).getJSONArray("roles").size() == 1, "wrong number of roles in user role sets");
		Assert.assertTrue(userRoleSets.getJSONObject(product2Index).getJSONArray("roles").getString(0).equals("Administrator"), "wrong roles in user role sets");
	}
	
	
	@Test (dependsOnMethods = "deleteProduct1", description = "update global user role sets")
	private void updateGlobalUserRoles() throws Exception{
		JSONObject json = getUser(userID1);
		JSONArray roles = new JSONArray();
		roles.add("Viewer");
		json.put("roles", roles);
		String response = operApi.updateAirlockUser(userID1, json.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "fail to update user role set:" + response);
		
		
		response = operApi.getUserRoleSets(sessionToken, userIdentifier);
		Assert.assertFalse(response.contains("error"), "Fail retrieving user role sets: " + response);	
		
		JSONArray userRoleSets = new JSONObject(response).getJSONArray("userRoleSets");
		Assert.assertTrue(userRoleSets.size() == 2, "wrong number of user role sets");
		
		int globalUserIndex = getIndexById(userRoleSets, userID1);		
		Assert.assertTrue(userRoleSets.getJSONObject(globalUserIndex).getString("uniqueId").equals(userID1), "wrong uniqueId in user role sets");
		Assert.assertTrue(userRoleSets.getJSONObject(globalUserIndex).getJSONArray("roles").size() == 1, "wrong number of roles in user role sets");
		Assert.assertTrue(userRoleSets.getJSONObject(globalUserIndex).getJSONArray("roles").getString(0).equals("Viewer"), "wrong roles in user role sets");
		/*Assert.assertTrue(userRoleSets.getJSONObject(globalUserIndex).getJSONArray("roles").getString(1).equals("Editor"), "wrong roles in user role sets");
		Assert.assertTrue(userRoleSets.getJSONObject(globalUserIndex).getJSONArray("roles").getString(2).equals("Administrator"), "wrong roles in user role sets");
		Assert.assertTrue(userRoleSets.getJSONObject(globalUserIndex).getJSONArray("roles").getString(3).equals("ProductLead"), "wrong roles in user role sets");
		*/
		int product2Index = getIndexById(userRoleSets, userID3);
		Assert.assertTrue(userRoleSets.getJSONObject(product2Index).getString("uniqueId").equals(userID3), "wrong uniqueId in user role sets");
		Assert.assertTrue(userRoleSets.getJSONObject(product2Index).getString("productId").equals(productID2), "wrong productId in user role sets");
		//Assert.assertTrue(userRoleSets.getJSONObject(product2Index).getJSONArray("roles").size() == 1, "wrong number of roles in user role sets");
		Assert.assertTrue(userRoleSets.getJSONObject(product2Index).getJSONArray("roles").getString(0).equals("Administrator"), "wrong roles in user role sets");
	}
	
	
	
	/*
	@Test (dependsOnMethods = "deleteGlobalUserRoles", description = "create product3")
	private void craeteProduct3() throws Exception{
		productID3 = baseUtils.createProduct();
		baseUtils.printProductToFile(productID3);
		baseUtils.createSeason(productID3);
		
		String response = operApi.getUserRoleSets(sessionToken, userIdentifier);
		Assert.assertFalse(response.contains("error"), "Fail retrieving user role sets: " + response);	
		
		JSONArray userRoleSets = new JSONObject(response).getJSONArray("userRoleSets");
		Assert.assertTrue(userRoleSets.size() == 1, "wrong number of user role sets");
		
		Assert.assertTrue(userRoleSets.getJSONObject(0).getString("uniqueId").equals(userID3), "wrong uniqueId in user role sets");
		Assert.assertTrue(userRoleSets.getJSONObject(0).getString("productId").equals(productID2), "wrong productId in user role sets");
		Assert.assertTrue(userRoleSets.getJSONObject(0).getJSONArray("roles").size() == 1, "wrong number of roles in user role sets");
		Assert.assertTrue(userRoleSets.getJSONObject(0).getJSONArray("roles").getString(0).equals("Viewer"), "wrong roles in user role sets");
	}
	*/
	@Test (dependsOnMethods = "updateGlobalUserRoles", description = "delete user from product 2")
	private void deleteUserFromProduct2() throws Exception{
		int code = operApi.deleteAirlockUser(userID3, sessionToken);
		Assert.assertTrue(code == 200, "fail to delete user role set");
		
		
		String response = operApi.getUserRoleSets(sessionToken, userIdentifier);
		Assert.assertFalse(response.contains("error"), "Fail retrieving user role sets: " + response);	
		
		JSONArray userRoleSets = new JSONObject(response).getJSONArray("userRoleSets");
		Assert.assertTrue(userRoleSets.size() == 1, "wrong number of user role sets");
		
		int globalUserIndex = getIndexById(userRoleSets, userID1);		
		Assert.assertTrue(userRoleSets.getJSONObject(globalUserIndex).getString("uniqueId").equals(userID1), "wrong uniqueId in user role sets");
		Assert.assertTrue(userRoleSets.getJSONObject(globalUserIndex).getJSONArray("roles").size() == 1, "wrong number of roles in user role sets");
		Assert.assertTrue(userRoleSets.getJSONObject(globalUserIndex).getJSONArray("roles").getString(0).equals("Viewer"), "wrong roles in user role sets");
		/*Assert.assertTrue(userRoleSets.getJSONObject(globalUserIndex).getJSONArray("roles").getString(1).equals("Editor"), "wrong roles in user role sets");
		Assert.assertTrue(userRoleSets.getJSONObject(globalUserIndex).getJSONArray("roles").getString(2).equals("Administrator"), "wrong roles in user role sets");
		Assert.assertTrue(userRoleSets.getJSONObject(globalUserIndex).getJSONArray("roles").getString(3).equals("ProductLead"), "wrong roles in user role sets");
		*/
	}
	
	@Test (dependsOnMethods = "deleteUserFromProduct2", description = "delete global user role sets")
	private void deleteGlobalUserRoles() throws Exception{
		int code = operApi.deleteAirlockUser(userID1, sessionToken);
		Assert.assertTrue(code == 200, "fail to delete user role set");
		
		String response = operApi.getUserRoleSets(sessionToken, userIdentifier);
		Assert.assertFalse(response.contains("error"), "Fail retrieving user role sets: " + response);	
		
		JSONArray userRoleSets = new JSONObject(response).getJSONArray("userRoleSets");
		Assert.assertTrue(userRoleSets.size() == 0, "wrong number of user role sets");
	}
	
	private JSONObject getUser(String userID) throws Exception{
		String user = operApi.getAirlockUser(userID, sessionToken);
		JSONObject json = new JSONObject(user);
		return json;
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
		baseUtils.reset(productID2, sessionToken);
		baseUtils.reset(productID3, sessionToken);
	
		for (String userId:usersIds) {
			operApi.deleteAirlockUser(userId, sessionToken);
		}
	}

	

}
