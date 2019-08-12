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

public class GroupRepresentationTest {
	String[] AdminRoles = new String[] {"Administrator", "ProductLead", "Editor", "Viewer"};
	String[] ProductLeadRoles = new String[] {"ProductLead", "Editor", "Viewer"};
	String[] EditorRoles = new String[] {"Editor", "Viewer"};
	String[] AnalyticsEditorRoles = new String[] {"AnalyticsEditor", "Viewer"};
	String[] AnalyticsViewerRoles = new String[] {"AnalyticsViewer", "Viewer"};
	String[] TranslationSpecialistRoles = new String[] {"TranslationSpecialist", "Viewer"};
	String[] ViewerRoles = new String[] {"Viewer"};
	
	protected String seasonID;
	protected String filePath;
	protected String m_url;
	protected String userStr;
	
	protected OperationRestApi operApi;
	private AirlockUtils baseUtils;
	protected String productID1;
	
	private String sessionToken = "";
	private LinkedList<String> usersIds = new LinkedList<String>();
	
	private String mailExt;
	
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
		
		mailExt = "@" +RandomStringUtils.randomAlphabetic(5) + ".www.com";
		
	}
	
	@Test (description = "Add global users")
	private void addGlobalUser() throws Exception{
		//String mailExt = "@xxx.www.com";
		JSONArray roles = new JSONArray();
		roles.add("Viewer");
		JSONObject json = new JSONObject(userStr);
		String userIdentifier1 = RandomStringUtils.randomAlphabetic(5) + mailExt;
		json.put("identifier", userIdentifier1);
		json.put("roles", roles);
		
		String userID1 = operApi.addGlobalAirlockUser(json.toString(), sessionToken);
		Assert.assertFalse(userID1.contains("error"), "can't add user role set");	
		usersIds.add(userID1);
		
		String response = operApi.getRolesPerUser(userIdentifier1, sessionToken);
		Assert.assertFalse(response.contains("error"), "can't get user roles");
		JSONArray userRoles = new JSONObject(response).getJSONArray("roles");
		Assert.assertTrue(areRolesListEqual(userRoles, ViewerRoles), "wrong roles list");
		
		String userIdentifier2 = RandomStringUtils.randomAlphabetic(5) + "@il.ibm.com";
		json.put("identifier", userIdentifier2);
		json.put("roles", roles);
		
		String userID2 = operApi.addGlobalAirlockUser(json.toString(), sessionToken);
		Assert.assertFalse(userID2.contains("error"), "can't add user role set");	
		usersIds.add(userID2);
		
		response = operApi.getRolesPerUser(userIdentifier2, sessionToken);
		Assert.assertFalse(response.contains("error"), "can't get user roles");
		userRoles = new JSONObject(response).getJSONArray("roles");
		Assert.assertTrue(areRolesListEqual(userRoles, ViewerRoles), "wrong roles list");
		
		String userIdentifier3 = RandomStringUtils.randomAlphabetic(5) + mailExt;
		json.put("identifier", userIdentifier3);
		json.put("roles", roles);
		
		String userID3 = operApi.addGlobalAirlockUser(json.toString(), sessionToken);
		Assert.assertFalse(userID3.contains("error"), "can't add user role set");
		usersIds.add(userID3);
		
		response = operApi.getRolesPerUser(userIdentifier3, sessionToken);
		Assert.assertFalse(response.contains("error"), "can't get user roles");
		userRoles = new JSONObject(response).getJSONArray("roles");
		Assert.assertTrue(areRolesListEqual(userRoles, ViewerRoles), "wrong roles list");
		
		
		JSONArray groupIdentifierRoles = new JSONArray();
		groupIdentifierRoles.add("Editor");
		json = new JSONObject(userStr);
		String userIdentifierGR = "*" + mailExt;
		json.put("identifier", userIdentifierGR);
		json.put("roles", groupIdentifierRoles);
		json.put("isGroupRepresentation", true);
		
		String groupIdentifierUserID = operApi.addGlobalAirlockUser(json.toString(), sessionToken);
		Assert.assertFalse(groupIdentifierUserID.contains("error"), "can't add group identifier user role set");	
		usersIds.add(groupIdentifierUserID);
		
		response = operApi.getRolesPerUser(userIdentifier1, sessionToken);
		Assert.assertFalse(response.contains("error"), "can't get user roles");
		userRoles = new JSONObject(response).getJSONArray("roles");
		Assert.assertTrue(areRolesListEqual(userRoles, EditorRoles), "wrong roles list");
		
		
		response = operApi.getRolesPerUser(userIdentifier3, sessionToken);
		Assert.assertFalse(response.contains("error"), "can't get user roles");
		userRoles = new JSONObject(response).getJSONArray("roles");
		Assert.assertTrue(userRoles.size() == 2, "wrong number of roles");
		Assert.assertTrue(areRolesListEqual(userRoles, EditorRoles), "wrong roles list");
		
		response = operApi.getRolesPerUser(userIdentifier2, sessionToken);
		Assert.assertFalse(response.contains("error"), "can't get user roles");
		userRoles = new JSONObject(response).getJSONArray("roles");
		Assert.assertTrue(areRolesListEqual(userRoles, ViewerRoles), "wrong roles list");
		
		
		JSONObject userJson = getUser(groupIdentifierUserID);
		roles.add("ProductLead");
		userJson.put("roles", roles);
		String resp = operApi.updateAirlockUser(groupIdentifierUserID, userJson.toString(), sessionToken);
		Assert.assertFalse(resp.contains("error"), "Can not update user roles");
		
		response = operApi.getRolesPerUser(userIdentifier1, sessionToken);
		Assert.assertFalse(response.contains("error"), "can't get user roles");
		userRoles = new JSONObject(response).getJSONArray("roles");
		Assert.assertTrue(areRolesListEqual(userRoles, ProductLeadRoles), "wrong roles list");
		
		
		response = operApi.getRolesPerUser(userIdentifier3, sessionToken);
		Assert.assertFalse(response.contains("error"), "can't get user roles");
		userRoles = new JSONObject(response).getJSONArray("roles");
		Assert.assertTrue(areRolesListEqual(userRoles, ProductLeadRoles), "wrong roles list");
		
		response = operApi.getRolesPerUser(userIdentifier2, sessionToken);
		Assert.assertFalse(response.contains("error"), "can't get user roles");
		userRoles = new JSONObject(response).getJSONArray("roles");
		Assert.assertTrue(areRolesListEqual(userRoles, ViewerRoles), "wrong roles list");
		
		
		String userIdentifier4 = RandomStringUtils.randomAlphabetic(5) + mailExt;
		json.put("identifier", userIdentifier4);
		JSONArray newUserRoles = new JSONArray();
		newUserRoles.add("Editor");
		
		json.put("roles", newUserRoles);
		
		String userID4 = operApi.addGlobalAirlockUser(json.toString(), sessionToken);
		Assert.assertFalse(userID4.contains("error"), "can't add user role set");
		usersIds.add(userID4);
		
		response = operApi.getRolesPerUser(userIdentifier4, sessionToken);
		Assert.assertFalse(response.contains("error"), "can't get user roles");
		userRoles = new JSONObject(response).getJSONArray("roles");
		Assert.assertTrue(areRolesListEqual(userRoles, ProductLeadRoles), "wrong roles list");
		
		int code = operApi.deleteAirlockUser(groupIdentifierUserID,  sessionToken);
		Assert.assertTrue(code == 200, "can't delete user roles");
		

		response = operApi.getRolesPerUser(userIdentifier1, sessionToken);
		Assert.assertFalse(response.contains("error"), "can't get user roles");
		userRoles = new JSONObject(response).getJSONArray("roles");
		Assert.assertTrue(areRolesListEqual(userRoles, ViewerRoles), "wrong roles list");
		
		
		response = operApi.getRolesPerUser(userIdentifier3, sessionToken);
		Assert.assertFalse(response.contains("error"), "can't get user roles");
		userRoles = new JSONObject(response).getJSONArray("roles");
		Assert.assertTrue(areRolesListEqual(userRoles, ViewerRoles), "wrong roles list");
		
		response = operApi.getRolesPerUser(userIdentifier2, sessionToken);
		Assert.assertFalse(response.contains("error"), "can't get user roles");
		userRoles = new JSONObject(response).getJSONArray("roles");
		Assert.assertTrue(areRolesListEqual(userRoles, ViewerRoles), "wrong roles list");
		
		response = operApi.getRolesPerUser(userIdentifier4, sessionToken);
		Assert.assertFalse(response.contains("error"), "can't get user roles");
		userRoles = new JSONObject(response).getJSONArray("roles");
		Assert.assertTrue(areRolesListEqual(userRoles, EditorRoles), "wrong roles list");
		
		
	}

	@Test (description = "Add product users")
	private void addProductUser() throws Exception{
		JSONArray roles = new JSONArray();
		roles.add("Viewer");
		JSONObject json = new JSONObject(userStr);
		String userIdentifier1 = RandomStringUtils.randomAlphabetic(5) + mailExt;
		json.put("identifier", userIdentifier1);
		json.put("roles", roles);
		
		//first add user to global list because user cannot be in product if he is not global
		String userID = operApi.addGlobalAirlockUser(json.toString(), sessionToken);
		Assert.assertFalse(userID.contains("error"), "can't add user role set");	
		usersIds.add(userID);
		
		String userID1 = operApi.addProductAirlockUser(json.toString(), productID1, sessionToken);
		Assert.assertFalse(userID1.contains("error"), "can't add user role set");	
		usersIds.add(userID1);
		
		String response = operApi.getUserRolesPerProduct(sessionToken, productID1, userIdentifier1);
		Assert.assertFalse(response.contains("error"), "can't get user roles");
		JSONArray userRoles = new JSONObject(response).getJSONArray("roles");
		Assert.assertTrue(areRolesListEqual(userRoles, ViewerRoles), "wrong roles list");
		
		String userIdentifier2 = RandomStringUtils.randomAlphabetic(5) + "@il.ibm.com";
		json.put("identifier", userIdentifier2);
		json.put("roles", roles);
		
		String userID2 = operApi.addProductAirlockUser(json.toString(), productID1, sessionToken);
		Assert.assertFalse(userID2.contains("error"), "can't add user role set");	
		usersIds.add(userID2);
		
		response = operApi.getUserRolesPerProduct(sessionToken, productID1, userIdentifier2);
		Assert.assertFalse(response.contains("error"), "can't get user roles");
		userRoles = new JSONObject(response).getJSONArray("roles");
		Assert.assertTrue(areRolesListEqual(userRoles, ViewerRoles), "wrong roles list");
		
		String userIdentifier3 = RandomStringUtils.randomAlphabetic(5) + mailExt;
		json.put("identifier", userIdentifier3);
		json.put("roles", roles);
		
		//first add user to global list because user cannot be in product if he is not global
		String gUserID = operApi.addGlobalAirlockUser(json.toString(), sessionToken);
		Assert.assertFalse(gUserID.contains("error"), "can't add user role set");	
		usersIds.add(gUserID);
		
		String userID3 = operApi.addProductAirlockUser(json.toString(), productID1, sessionToken);
		Assert.assertFalse(userID3.contains("error"), "can't add user role set");
		usersIds.add(userID3);
		
		response = operApi.getUserRolesPerProduct(sessionToken, productID1, userIdentifier3);
		Assert.assertFalse(response.contains("error"), "can't get user roles");
		userRoles = new JSONObject(response).getJSONArray("roles");
		Assert.assertTrue(areRolesListEqual(userRoles, ViewerRoles), "wrong roles list");
		
		
		JSONArray groupIdentifierRoles = new JSONArray();
		groupIdentifierRoles.add("Editor");
		json = new JSONObject(userStr);
		String userIdentifierGR = "*" + mailExt;
		json.put("identifier", userIdentifierGR);
		json.put("roles", groupIdentifierRoles);
		json.put("isGroupRepresentation", true);
		
		//first add user to global list because user cannot be in product if he is not global
		String gGRUserID = operApi.addGlobalAirlockUser(json.toString(), sessionToken);
		Assert.assertFalse(gGRUserID.contains("error"), "can't add user role set");	
		usersIds.add(gGRUserID);
		
		String groupIdentifierUserID = operApi.addProductAirlockUser(json.toString(), productID1, sessionToken);
		Assert.assertFalse(groupIdentifierUserID.contains("error"), "can't add group identifier user role set");	
		usersIds.add(groupIdentifierUserID);
		
		response = operApi.getUserRolesPerProduct(sessionToken, productID1, userIdentifier1);
		Assert.assertFalse(response.contains("error"), "can't get user roles");
		userRoles = new JSONObject(response).getJSONArray("roles");
		Assert.assertTrue(areRolesListEqual(userRoles, EditorRoles), "wrong roles list");
		
		
		response = operApi.getUserRolesPerProduct(sessionToken, productID1, userIdentifier3);
		Assert.assertFalse(response.contains("error"), "can't get user roles");
		userRoles = new JSONObject(response).getJSONArray("roles");
		Assert.assertTrue(userRoles.size() == 2, "wrong number of roles");
		Assert.assertTrue(areRolesListEqual(userRoles, EditorRoles), "wrong roles list");
		
		response = operApi.getUserRolesPerProduct(sessionToken, productID1, userIdentifier2);
		Assert.assertFalse(response.contains("error"), "can't get user roles");
		userRoles = new JSONObject(response).getJSONArray("roles");
		Assert.assertTrue(areRolesListEqual(userRoles, ViewerRoles), "wrong roles list");
		
		
		JSONObject userJson = getUser(groupIdentifierUserID);
		roles.add("ProductLead");
		userJson.put("roles", roles);
		String resp = operApi.updateAirlockUser(groupIdentifierUserID, userJson.toString(), sessionToken);
		Assert.assertFalse(resp.contains("error"), "Can not update user roles");
		
		response = operApi.getUserRolesPerProduct(sessionToken, productID1, userIdentifier1);
		Assert.assertFalse(response.contains("error"), "can't get user roles");
		userRoles = new JSONObject(response).getJSONArray("roles");
		Assert.assertTrue(areRolesListEqual(userRoles, ProductLeadRoles), "wrong roles list");
		
		
		response = operApi.getUserRolesPerProduct(sessionToken, productID1, userIdentifier3);
		Assert.assertFalse(response.contains("error"), "can't get user roles");
		userRoles = new JSONObject(response).getJSONArray("roles");
		Assert.assertTrue(areRolesListEqual(userRoles, ProductLeadRoles), "wrong roles list");
		
		response = operApi.getUserRolesPerProduct(sessionToken, productID1, userIdentifier2);
		Assert.assertFalse(response.contains("error"), "can't get user roles");
		userRoles = new JSONObject(response).getJSONArray("roles");
		Assert.assertTrue(areRolesListEqual(userRoles, ViewerRoles), "wrong roles list");
		
		
		String userIdentifier4 = RandomStringUtils.randomAlphabetic(5) + mailExt;
		json.put("identifier", userIdentifier4);
		JSONArray newUserRoles = new JSONArray();
		newUserRoles.add("Editor");
		
		json.put("roles", newUserRoles);
		
		String userID4 = operApi.addProductAirlockUser(json.toString(), productID1, sessionToken);
		Assert.assertFalse(userID4.contains("error"), "can't add user role set");
		usersIds.add(userID4);
		
		response = operApi.getUserRolesPerProduct(sessionToken, productID1, userIdentifier4);
		Assert.assertFalse(response.contains("error"), "can't get user roles");
		userRoles = new JSONObject(response).getJSONArray("roles");
		Assert.assertTrue(areRolesListEqual(userRoles, ProductLeadRoles), "wrong roles list");
		
		int code = operApi.deleteAirlockUser(groupIdentifierUserID,  sessionToken);
		Assert.assertTrue(code == 200, "can't delete user roles");
		

		response = operApi.getUserRolesPerProduct(sessionToken, productID1, userIdentifier1);
		Assert.assertFalse(response.contains("error"), "can't get user roles");
		userRoles = new JSONObject(response).getJSONArray("roles");
		Assert.assertTrue(areRolesListEqual(userRoles, ViewerRoles), "wrong roles list");
		
		
		response = operApi.getUserRolesPerProduct(sessionToken, productID1, userIdentifier3);
		Assert.assertFalse(response.contains("error"), "can't get user roles");
		userRoles = new JSONObject(response).getJSONArray("roles");
		Assert.assertTrue(areRolesListEqual(userRoles, ViewerRoles), "wrong roles list");
		
		response = operApi.getUserRolesPerProduct(sessionToken, productID1, userIdentifier2);
		Assert.assertFalse(response.contains("error"), "can't get user roles");
		userRoles = new JSONObject(response).getJSONArray("roles");
		Assert.assertTrue(areRolesListEqual(userRoles, ViewerRoles), "wrong roles list");
		
		response = operApi.getUserRolesPerProduct(sessionToken, productID1, userIdentifier4);
		Assert.assertFalse(response.contains("error"), "can't get user roles");
		userRoles = new JSONObject(response).getJSONArray("roles");
		Assert.assertTrue(areRolesListEqual(userRoles, EditorRoles), "wrong roles list");
		
		
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
		
		for (String userId:usersIds) {
			operApi.deleteAirlockUser(userId, sessionToken);
		}
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

}
