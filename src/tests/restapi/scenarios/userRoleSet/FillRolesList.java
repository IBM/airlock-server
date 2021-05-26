package tests.restapi.scenarios.userRoleSet;

import java.util.Arrays;
import java.util.HashSet;
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

import com.ibm.airlock.admin.Utilities;

import tests.com.ibm.qautils.FileUtils;
import tests.restapi.AirlockUtils;
import tests.restapi.OperationRestApi;

public class FillRolesList {
	protected String seasonID;
	protected String filePath;
	protected String m_url;
	protected String userStr;
	
	protected OperationRestApi operApi;
	private AirlockUtils baseUtils;
	protected String productID1;
	
	private String sessionToken = "";
	private LinkedList<String> usersIds = new LinkedList<String>();
	
	String[] AdminRoles = new String[] {"Administrator","ProductLead","Editor","Viewer","AnalyticsViewer","AnalyticsEditor","AnalyticsPowerUser"};
	String[] ProductLeadRoles = new String[] {"ProductLead", "Editor", "Viewer"};
	String[] EditorRoles = new String[] {"Editor", "Viewer"};
	String[] AnalyticsEditorRoles = new String[] {"AnalyticsEditor", "Viewer"};
	String[] AnalyticsViewerRoles = new String[] {"AnalyticsViewer", "Viewer"};
	String[] TranslationSpecialistRoles = new String[] {"TranslationSpecialist", "Viewer"};
	String[] ViewerRoles = new String[] {"Viewer"};
	
	
	
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
	
	private void validateRolesListInAddition (JSONArray givenRoles, String[] expectedRoles, String productId) throws Exception {
		JSONObject json = new JSONObject(userStr);
		String userIdentifier = RandomStringUtils.randomAlphabetic(5) + "@il.ibm.com";
		json.put("identifier", userIdentifier);
		json.put("roles", givenRoles);
		
		String userID = null;
		if (productId == null) {
			userID = operApi.addGlobalAirlockUser(json.toString(), sessionToken);
		}
		else {
			userID = operApi.addProductAirlockUser(json.toString(), productId, sessionToken);
		}
		Assert.assertFalse(userID.contains("error"), "can't add user role set");	
		
		validateExistingRoles(userID, expectedRoles);
		usersIds.add(userID);
	}
	
	private void validateRolesListInUpdate (JSONArray initialRoles, JSONArray updatedRoles, String[] expectedRoles, String productID) throws Exception {
		//create role set
		JSONObject json = new JSONObject(userStr);
		String userIdentifier = RandomStringUtils.randomAlphabetic(5) + "@il.ibm.com";
		json.put("identifier", userIdentifier);
		json.put("roles", initialRoles);
		
		String userID = null;
		if (productID == null) {
			userID = operApi.addGlobalAirlockUser(json.toString(), sessionToken);
		}
		else {
			userID = operApi.addProductAirlockUser(json.toString(), productID, sessionToken);
		}
		Assert.assertFalse(userID.contains("error"), "can't add user role set");	
		usersIds.add(userID);
		
		//update role set
		JSONObject userJson = getUser(userID);
		userJson.put("roles", updatedRoles);
		
		String resp = operApi.updateAirlockUser(userID, userJson.toString(), sessionToken);
		Assert.assertFalse(resp.contains("error"), "Can update user role set");
		
		//validate updated roles list
		validateExistingRoles(userID, expectedRoles);
	}
	
	@Test (description = "Add global user with all types of roles")
	private void addGlobalUser() throws Exception{
		JSONArray roles = new JSONArray();
		roles.add("Administrator");
		validateRolesListInAddition(roles, AdminRoles, null);
		
		roles.clear();
		roles.add("ProductLead");
		validateRolesListInAddition(roles, ProductLeadRoles, null);
		
		roles.clear();
		roles.add("Editor");
		validateRolesListInAddition(roles, EditorRoles, null);
		
		roles.clear();
		roles.add("Viewer");
		validateRolesListInAddition(roles, ViewerRoles, null);
		
		roles.clear();
		roles.add("TranslationSpecialist");
		validateRolesListInAddition(roles, TranslationSpecialistRoles, null);
		
		roles.clear();
		roles.add("AnalyticsEditor");
		validateRolesListInAddition(roles, AnalyticsEditorRoles, null);
		
		roles.clear();
		roles.add("AnalyticsViewer");
		validateRolesListInAddition(roles, AnalyticsViewerRoles, null);
		
		roles.clear();
		roles.add("Administrator");
		roles.add("Editor");
		validateRolesListInAddition(roles, AdminRoles, null);
		
		roles.clear();
		roles.add("ProductLead");
		roles.add("Viewer");
		validateRolesListInAddition(roles, ProductLeadRoles, null);
		
		roles.clear();
		roles.add("TranslationSpecialist");
		roles.add("Viewer");
		validateRolesListInAddition(roles, TranslationSpecialistRoles, null);
		
		roles.clear();
		roles.add("AnalyticsViewer");
		roles.add("Administrator");
		roles.add("Editor");
		validateRolesListInAddition(roles, new String[] {"Administrator", "ProductLead", "Editor", "Viewer", "AnalyticsViewer"}, null);
	}
	
	@Test (dependsOnMethods = "addGlobalUser", description = "Add product user with all types of roles")
	private void addProductUser() throws Exception{
		JSONArray roles = new JSONArray();
		roles.add("Administrator");
		validateRolesListInAddition(roles, AdminRoles, productID1);
		
		roles.clear();
		roles.add("ProductLead");
		validateRolesListInAddition(roles, ProductLeadRoles, productID1);
		
		roles.clear();
		roles.add("Editor");
		validateRolesListInAddition(roles, EditorRoles, productID1);
		
		roles.clear();
		roles.add("Viewer");
		validateRolesListInAddition(roles, ViewerRoles, productID1);
		
		roles.clear();
		roles.add("TranslationSpecialist");
		validateRolesListInAddition(roles, TranslationSpecialistRoles, productID1);
		
		roles.clear();
		roles.add("AnalyticsEditor");
		validateRolesListInAddition(roles, AnalyticsEditorRoles, productID1);
		
		roles.clear();
		roles.add("AnalyticsViewer");
		validateRolesListInAddition(roles, AnalyticsViewerRoles, productID1);
		
		roles.clear();
		roles.add("Viewer");
		roles.add("ProductLead");
		validateRolesListInAddition(roles, ProductLeadRoles, productID1);
		
		roles.clear();
		roles.add("TranslationSpecialist");
		roles.add("Viewer");
		validateRolesListInAddition(roles, TranslationSpecialistRoles, productID1);
		
		roles.clear();
		roles.add("Viewer");
		roles.add("Administrator");
		roles.add("Editor");
		validateRolesListInAddition(roles, AdminRoles, productID1);
		
		roles.clear();
		roles.add("TranslationSpecialist");
		roles.add("AnalyticsViewer");
		validateRolesListInAddition(roles, new String[] {"TranslationSpecialist","AnalyticsViewer","Viewer"}, productID1);
	}
	
	@Test (dependsOnMethods = "addProductUser", description = "update user role set")
	private void updateUserRoleSet() throws Exception{
		JSONArray initinalRoles = new JSONArray();
		initinalRoles.add("Administrator");		
		JSONArray updatedRoles = new JSONArray();
		updatedRoles.add("Editor");		
		validateRolesListInUpdate(initinalRoles, updatedRoles, EditorRoles, null);
		
		
		initinalRoles.clear();
		initinalRoles.add("Administrator");
		initinalRoles.add("AnalyticsViewer");	
		updatedRoles.clear();
		updatedRoles.add("TranslationSpecialist");
		updatedRoles.add("Editor");
		validateRolesListInUpdate(initinalRoles, updatedRoles, new String[] {"Editor", "Viewer", "TranslationSpecialist"},  null);
		
		initinalRoles.clear();
		initinalRoles.add("ProductLead");		
		updatedRoles.clear();
		updatedRoles.add("Administrator");		
		validateRolesListInUpdate(initinalRoles, updatedRoles, AdminRoles, productID1);
		
		
		initinalRoles.clear();
		initinalRoles.add("Editor");		
		updatedRoles.clear();
		updatedRoles.add("ProductLead");		
		validateRolesListInUpdate(initinalRoles, updatedRoles, ProductLeadRoles, productID1);
		
	}
	
	
	private void validateExistingRoles(String userID, String[] expectedRoles) throws Exception {
		JSONObject userJson = getUser(userID);
		
		JSONArray existingRoles = userJson.getJSONArray("roles");
		Assert.assertTrue(areRolesListEqual(existingRoles, expectedRoles), "wrong roles list");
	}
	
	private boolean areRolesListEqual(JSONArray existingRoles, String[] expectedRoles) throws JSONException {
		if ((existingRoles == null || existingRoles.isEmpty()) && ((expectedRoles == null) || expectedRoles.length == 0))
			return true; //both null or empty

		if (existingRoles == null || expectedRoles == null)
			return false; //only one is null and the other is not empty

		HashSet<String> h1 = new HashSet<String>( Arrays.asList( Utilities.jsonArrToStringArr(existingRoles) ));
		HashSet<String> h2 = new HashSet<String>(Arrays.asList(expectedRoles));
		return h1.equals(h2);
	}
/*
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
		*/
	private JSONObject getUser(String userID) throws Exception{
		String user = operApi.getAirlockUser(userID, sessionToken);
		Assert.assertFalse(user.contains("error"), "cannot get user:" + user);
		JSONObject json = new JSONObject(user);
		return json;
	}
	
	
	@AfterTest
	public void reset() throws Exception{
		baseUtils.reset(productID1, sessionToken);
		
		for (String userId:usersIds) {
			operApi.deleteAirlockUser(userId, sessionToken);
		}
	}

	

}
