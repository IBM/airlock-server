package tests.restapi.scenarios.userRoleSet;

import static org.testng.Assert.assertTrue;

import java.util.LinkedList;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.wink.json4j.JSONArray;
import org.apache.wink.json4j.JSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import tests.com.ibm.qautils.FileUtils;
import tests.restapi.AirlockUtils;
import tests.restapi.OperationRestApi;

public class UserRoleSetNegativeTests {
	protected String seasonID;
	protected String filePath;
	protected String m_url;
	protected String userStr;
	protected String userIdentifier;
	
	private String globalUserID;
	private String productUserID;
	
	protected OperationRestApi operApi;
	private AirlockUtils baseUtils;
	protected String productID1;
	protected String productID2;
	
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

		productID2 = baseUtils.createProduct();
		baseUtils.printProductToFile(productID2);		
		baseUtils.createSeason(productID2);

		m_url = url;
		operApi = new OperationRestApi();
		operApi.setURL(c_operationsUrl);
		userStr = FileUtils.fileToString(filePath + "airlockUser.txt", "UTF-8", false);
		userIdentifier = RandomStringUtils.randomAlphabetic(5) + "@il.ibm.com";
	}
	
	@Test (description = "Add global user without roles")
	private void addGlobalUser() throws Exception{
		JSONObject json = new JSONObject(userStr);
		JSONArray roles = new JSONArray();
		
		json.put("identifier", userIdentifier);
		json.put("roles", roles);
		globalUserID = operApi.addGlobalAirlockUser(json.toString(), sessionToken);
		Assert.assertTrue(globalUserID.contains("error"), "can add global user without roles");	
		
		roles.add("nonExistingRole");
		json.put("roles", roles);
		globalUserID = operApi.addGlobalAirlockUser(json.toString(), sessionToken);
		Assert.assertTrue(globalUserID.contains("error") && globalUserID.contains("nonExistingRole"), "Can set non existing role to user");	
		
		roles.clear();
		roles.add("ProductLead");
		roles.add("ProductLead");
		json.put("roles", roles);
		globalUserID = operApi.addGlobalAirlockUser(json.toString(), sessionToken);
		Assert.assertTrue(globalUserID.contains("error") && globalUserID.contains("ProductLead"), "Can set non existing role to user");	
		
		roles.clear();
		roles.add("Viewer");
		json.put("roles", roles);
		globalUserID = operApi.addGlobalAirlockUser(json.toString(), sessionToken);
		Assert.assertFalse(globalUserID.contains("error"), "fail adding global user");
		
		usersIds.add(globalUserID);	
		
	}
	
	@Test (dependsOnMethods = "addGlobalUser", description = "Add user to product1")
	private void addUserToProduct1() throws Exception{
		JSONObject json = new JSONObject(userStr);
		JSONArray roles = new JSONArray();
		
		json.put("identifier", userIdentifier);
		json.put("roles", roles);
		productUserID = operApi.addProductAirlockUser(json.toString(), productID1, sessionToken);
		Assert.assertTrue(productUserID.contains("error"), "can add product user without roles");	
		
		roles.add("nonExistingRole");
		json.put("roles", roles);
		productUserID = operApi.addGlobalAirlockUser(json.toString(), sessionToken);
		Assert.assertTrue(productUserID.contains("error") && productUserID.contains("nonExistingRole"), "Can set non existing role to user");	
		
		roles.clear();
		roles.add("Viewer");
		roles.add("Viewer");
		json.put("roles", roles);
		productUserID = operApi.addGlobalAirlockUser(json.toString(), sessionToken);
		Assert.assertTrue(productUserID.contains("error") && productUserID.contains("Viewer"), "Can set non existing role to user");	
		
		roles.clear();
		roles.add("Viewer");
		roles.add("Editor");
		json.put("roles", roles);
		productUserID = operApi.addProductAirlockUser(json.toString(), productID1, sessionToken);
		Assert.assertFalse(productUserID.contains("error"), "fail adding product user");
	}
	
	
	@Test (dependsOnMethods = "addUserToProduct1", description = "move global user to product")
	private void moveGlobalUserToProduct() throws Exception{
		JSONObject userJson = getUser(globalUserID);
		//assertTrue(userJson.getString("productId") == null, "product is set to global user");
		
		userJson.put("productId", productID1);
		String resp = operApi.updateAirlockUser(userJson.getString("uniqueId"), userJson.toString(), sessionToken);
		Assert.assertTrue(resp.contains("error"), "Can move global user to product user");
	}
	
	@Test (dependsOnMethods = "moveGlobalUserToProduct", description = "move product user to global")
	private void moveProductUserToGlobal() throws Exception{
		JSONObject userJson = getUser(productUserID);
		assertTrue(userJson.getString("productId").equals(productID1), "wrong product is set to global user");
		
		String pId = null;
		userJson.put("productId", pId);
		String resp = operApi.updateAirlockUser(userJson.getString("uniqueId"), userJson.toString(), sessionToken);
		Assert.assertTrue(resp.contains("error"), "Can move product user to global user");
	}
	
	@Test (dependsOnMethods = "moveGlobalUserToProduct", description = "move user from product to product")
	private void moveUserFromProductToProduct() throws Exception{
		JSONObject userJson = getUser(productUserID);
		assertTrue(userJson.getString("productId").equals(productID1), "wrong product is set to global user");
		
		userJson.put("productId", productID2);
		String resp = operApi.updateAirlockUser(userJson.getString("uniqueId"), userJson.toString(), sessionToken);
		Assert.assertTrue(resp.contains("error"), "Can move  user from product to product");
	}
	
	@Test (dependsOnMethods = "moveUserFromProductToProduct", description = "change identifier for global user")
	private void changeIdentifierForGlobalUser() throws Exception{
		JSONObject userJson = getUser(globalUserID);
		assertTrue(userJson.getString("identifier").equals(userIdentifier), "wrong identifier is set to global user");
		
		userJson.put("identifier", "XXX@gmail.com");
		String resp = operApi.updateAirlockUser(userJson.getString("uniqueId"), userJson.toString(), sessionToken);
		Assert.assertTrue(resp.contains("error"), "Can change user identifier");
	}
	
	@Test (dependsOnMethods = "changeIdentifierForGlobalUser", description = "change identifier for product user")
	private void changeIdentifierForProductUser() throws Exception{
		JSONObject userJson = getUser(productUserID);
		assertTrue(userJson.getString("identifier").equals(userIdentifier), "wrong identifier is set to product user");
		
		userJson.put("identifier", "XXX@gmail.com");
		String resp = operApi.updateAirlockUser(userJson.getString("uniqueId"), userJson.toString(), sessionToken);
		Assert.assertTrue(resp.contains("error"), "Can change user identifier");
	}
	
	@Test (dependsOnMethods = "changeIdentifierForProductUser", description = "Set non existing role for global user")
	private void nonExistingRoleForGlobalUser() throws Exception{
		JSONObject userJson = getUser(globalUserID);
		JSONArray rolesArr = userJson.getJSONArray("roles");
		rolesArr.add("nonExistingRole");
		userJson.put("roles", rolesArr);
		String resp = operApi.updateAirlockUser(globalUserID, userJson.toString(), sessionToken);
		Assert.assertTrue(resp.contains("error") && resp.contains("nonExistingRole"), "Can set non existing role to user");
	}
	
	@Test (dependsOnMethods = "nonExistingRoleForGlobalUser", description = "Set non existing role for product user")
	private void nonExistingRoleForProductUser() throws Exception{
		JSONObject userJson = getUser(productUserID);
		JSONArray rolesArr = userJson.getJSONArray("roles");
		rolesArr.add("nonExistingRole");
		userJson.put("roles", rolesArr);
		String resp = operApi.updateAirlockUser(productUserID, userJson.toString(), sessionToken);
		Assert.assertTrue(resp.contains("error") && resp.contains("nonExistingRole"), "Can set non existing role to user");
	}
	
	@Test (dependsOnMethods = "nonExistingRoleForProductUser", description = "Set same role twice for global user")
	private void duplicateRoleForGlobalUser() throws Exception{
		JSONObject userJson = getUser(globalUserID);
		JSONArray rolesArr = userJson.getJSONArray("roles");
		rolesArr.add("ProductLead");
		rolesArr.add("ProductLead");
		
		userJson.put("roles", rolesArr);
		String resp = operApi.updateAirlockUser(globalUserID, userJson.toString(), sessionToken);
		Assert.assertTrue(resp.contains("error") && resp.contains("ProductLead"), "Can set same role twice to user");
	}
	
	@Test (dependsOnMethods = "duplicateRoleForGlobalUser", description = "Set same role twice for product use")
	private void duplicateRoleForProductUser() throws Exception{
		JSONObject userJson = getUser(productUserID);
		JSONArray rolesArr = userJson.getJSONArray("roles");
		rolesArr.add("ProductLead");
		rolesArr.add("ProductLead");
		
		userJson.put("roles", rolesArr);
		String resp = operApi.updateAirlockUser(productUserID, userJson.toString(), sessionToken);
		Assert.assertTrue(resp.contains("error") && resp.contains("ProductLead"), "Can set same role twice to user");
	}
	
	@Test (dependsOnMethods = "duplicateRoleForProductUser", description = "try deleting user twice")
	private void deleteUserTwice() throws Exception{
		JSONObject json = new JSONObject(userStr);
		JSONArray roles = new JSONArray();
		roles.add("Editor");
		
		json.put("identifier", userIdentifier+"temp");
		json.put("roles", roles);
		String delUserId = operApi.addGlobalAirlockUser(json.toString(), sessionToken);
		Assert.assertFalse(delUserId.contains("error"), "can not add user");	
		
		int code = operApi.deleteAirlockUser(delUserId, sessionToken);
		Assert.assertTrue(code == 200, "can not delete user");	
		
		code = operApi.deleteAirlockUser(delUserId, sessionToken);
		Assert.assertFalse(code == 200, "can delete user twice");	
	}
		
	private JSONObject getUser(String userID) throws Exception{
		String user = operApi.getAirlockUser(userID, sessionToken);
		Assert.assertFalse(user.contains("error"), "cannot get user:" + user);
		JSONObject json = new JSONObject(user);
		return json;
	}
	
	
	@AfterTest
	public void reset() throws Exception{
		baseUtils.reset(productID1, sessionToken);
		baseUtils.reset(productID2, sessionToken);
		
		for (String userId:usersIds) {
			operApi.deleteAirlockUser(userId, sessionToken);
		}
	}

	

}
