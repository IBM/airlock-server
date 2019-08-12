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

public class ProducRoleSetsUponProductCreation {
	protected String seasonID;
	protected String filePath;
	protected String m_url;
	protected String userStr;
	
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
	
	private String userIdentifier1;
	private String userIdentifier2;
	private String userIdentifier3;
	
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
		
	}
	
	@Test (description = "Add global user")
	private void addGlobalUser() throws Exception{
		userIdentifier1 = RandomStringUtils.randomAlphabetic(5) + "@il.ibm.com";
		JSONObject json = new JSONObject(userStr);
		JSONArray roles = new JSONArray();
		roles.add("Viewer");
		userID1 = addGlobalUser(json, roles, userIdentifier1);
		usersIds.add(userID1);
		String response = operApi.getProductAirlockUsers(sessionToken, productID1);
		Assert.assertFalse(response.contains("error"), "Fail retrieving product user role sets " + response);	
		
		JSONObject prodUserRoleSets = new JSONObject(response);
		verifyRoleSetExistance(prodUserRoleSets, userIdentifier1, false);
		
		response = operApi.getAirlockUsers(sessionToken);
		Assert.assertFalse(response.contains("error"), "Fail retrieving global user role sets " + response);	
		
		JSONObject globalUserRoleSets = new JSONObject(response);
		verifyRoleSetExistance(globalUserRoleSets, userIdentifier1, true);
		
	}
	
	@Test (dependsOnMethods = "addGlobalUser", description = "Add product user")
	private void addProductUser() throws Exception{
		userIdentifier2 = RandomStringUtils.randomAlphabetic(5) + "@il.ibm.com";
		JSONObject json = new JSONObject(userStr);
		JSONArray roles = new JSONArray();
		roles.add("Viewer");
		userID2 = addUserToProduct(json, roles, userIdentifier2, productID1);
		usersIds.add(userID2);
		
		
		String response = operApi.getProductAirlockUsers(sessionToken, productID1);
		Assert.assertFalse(response.contains("error"), "Fail retrieving product user role sets " + response);	
		
		JSONObject prodUserRoleSets = new JSONObject(response);
		verifyRoleSetExistance(prodUserRoleSets, userIdentifier2, true);
		
		response = operApi.getAirlockUsers(sessionToken);
		Assert.assertFalse(response.contains("error"), "Fail retrieving global user role sets " + response);	
		
		JSONObject globalUserRoleSets = new JSONObject(response);
		verifyRoleSetExistance(globalUserRoleSets, userIdentifier2, false);
		
	}
	
	@Test (dependsOnMethods = "addProductUser", description = "create product 2")
	private void createProduct2() throws Exception{
		productID2 = baseUtils.createProduct();
		baseUtils.printProductToFile(productID2);
		baseUtils.createSeason(productID2);
		
		String response = operApi.getProductAirlockUsers(sessionToken, productID2);
		Assert.assertFalse(response.contains("error"), "Fail retrieving product2 user role sets " + response);	
		
		JSONObject prodUserRoleSets = new JSONObject(response);
		verifyRoleSetExistance(prodUserRoleSets, userIdentifier1, true);
		verifyRoleSetExistance(prodUserRoleSets, userIdentifier2, false);
		
		JSONObject userRoleSet = findRoleSetForUser(prodUserRoleSets, userIdentifier1);
		Assert.assertFalse(userRoleSet.getString("uniqueId").equals(userID1), "uniqueId in duplication wasnt changed");
		Assert.assertTrue(userRoleSet.getString("productId").equals(productID2), "wrong product id");
	}
	
	@Test (dependsOnMethods = "createProduct2", description = "delete global user")
	private void deleteGlobalUser() throws Exception{
		int code = operApi.deleteAirlockUser(userID1, sessionToken);
		Assert.assertFalse(code == 200, "can dselete global roleset even though user exists in products");
		
		String response = operApi.getProductAirlockUsers(sessionToken, productID2);
		Assert.assertFalse(response.contains("error"), "Fail retrieving product2 user role sets " + response);	
		
		JSONObject prodUserRoleSets = new JSONObject(response);
		verifyRoleSetExistance(prodUserRoleSets, userIdentifier1, true);
	
		response = operApi.getAirlockUsers(sessionToken);
		Assert.assertFalse(response.contains("error"), "Fail retrieving global user role sets " + response);	
		
		JSONObject globalUserRoleSets = new JSONObject(response);
		verifyRoleSetExistance(globalUserRoleSets, userIdentifier1, true);
		
		JSONObject res = getUser(userID1);
		Assert.assertFalse(res.containsKey("error"), "user role was not deleted");
		
		JSONObject userRoleSet = findRoleSetForUser(prodUserRoleSets, userIdentifier1);
		res = getUser(userRoleSet.getString("uniqueId"));
		Assert.assertFalse(res.containsKey("error"), "user role was deleted");
		
	}
	
	private JSONObject findRoleSetForUser(JSONObject userRoleSets, String userIdentifier) throws JSONException {
		JSONArray usersArr = userRoleSets.getJSONArray("users");
		for (int i=0; i<usersArr.size(); i++) {
			JSONObject userRoleSet = usersArr.getJSONObject(i);
			if (userRoleSet.getString("identifier").equals(userIdentifier)) {
				return userRoleSet;
			}
		}
		
		return null;
	}

	private void verifyRoleSetExistance(JSONObject userRoleSets, String userIdentifier, boolean shouldExist) throws JSONException {
		JSONArray usersArr = userRoleSets.getJSONArray("users");
		boolean found = false;
		for (int i=0; i<usersArr.size(); i++) {
			JSONObject userRoleSet = usersArr.getJSONObject(i);
			if (userRoleSet.getString("identifier").equals(userIdentifier)) {
				found = true;
				break;
			}
		}
		
		Assert.assertTrue(shouldExist == found, "User role set for user " + userIdentifier + ": shouldExist = " + shouldExist + ", found = " + found );
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
