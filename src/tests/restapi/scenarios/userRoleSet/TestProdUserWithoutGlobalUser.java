package tests.restapi.scenarios.userRoleSet;

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

public class TestProdUserWithoutGlobalUser {
	protected String seasonID;
	protected String filePath;
	protected String m_url;
	protected String userStr;
	protected String userIdentifier;
	
	private String userIDGlobal;
	private String userID1;
	private String userID2;
	
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
		userIdentifier = RandomStringUtils.randomAlphabetic(5) + "@abc.def.com";
	}
	
	@Test (description = "Add product user")
	private void addProductUser() throws Exception{
		JSONObject json = new JSONObject(userStr);
		JSONArray roles = new JSONArray();
		roles.add("Viewer");
		roles.add("Editor");
		json.put("identifier", userIdentifier);
		json.put("roles", roles);
		String response = operApi.addProductAirlockUser(json.toString(), productID1, sessionToken);
		Assert.assertTrue(response.contains("error"), "manage to craete product user with no global roles");
		
		
		JSONArray globalRoles = new JSONArray();
		globalRoles.add("Viewer");
		
		userIDGlobal = addGlobalUser(json, roles, userIdentifier);
		
		json.put("roles", roles);
		
		userID1 = addUserToProduct(json, roles, userIdentifier, productID1);
		userID2 = addUserToProduct(json, roles, userIdentifier, productID2);
	}
	
	@Test (dependsOnMethods = "addProductUser", description = "delete global user")
	private void deleteGlobalUser() throws Exception{
		
		int code = operApi.deleteAirlockUser(userIDGlobal, sessionToken);
		Assert.assertFalse(code == 200, "can delete global user that has products permisssions");	
			
		code = operApi.deleteAirlockUser(userID1, sessionToken);
		Assert.assertTrue(code == 200, "cannot delete product user");	
		
		code = operApi.deleteAirlockUser(userIDGlobal, sessionToken);
		Assert.assertFalse(code == 200, "can delete global user that has products permisssions");	
		
		code = operApi.deleteAirlockUser(userID2, sessionToken);
		Assert.assertTrue(code == 200, "cannot delete product user");	
		
		code = operApi.deleteAirlockUser(userIDGlobal, sessionToken);
		Assert.assertTrue(code == 200, "cannot delete global user that has no products permisssions");	
		
	}
	
	@Test (dependsOnMethods = "deleteGlobalUser", description = "Add product user again")
	private void addProductUser2() throws Exception{
		JSONObject json = new JSONObject(userStr);
		JSONArray roles = new JSONArray();
		roles.add("Viewer");
		roles.add("Editor");
		json.put("identifier", userIdentifier);
		json.put("roles", roles);
		String response = operApi.addProductAirlockUser(json.toString(), productID1, sessionToken);
		Assert.assertTrue(response.contains("error"), "manage to craete product user with no global roles");
		
		
		JSONArray globalRoles = new JSONArray();
		globalRoles.add("Viewer");
		
		userIDGlobal = addGlobalUser(json, roles, userIdentifier);
		
		json.put("roles", roles);
		
		userID1 = addUserToProduct(json, roles, userIdentifier, productID1);
		userID2 = addUserToProduct(json, roles, userIdentifier, productID2);
	}
	
	@Test (dependsOnMethods = "addProductUser2", description = "delete global user after deleting products")
	private void deleteGlobalUser2() throws Exception{
		
		int code = operApi.deleteAirlockUser(userIDGlobal, sessionToken);
		Assert.assertFalse(code == 200, "can delete global user that has products permisssions");	
			
		baseUtils.reset(productID1, sessionToken);
		
		code = operApi.deleteAirlockUser(userIDGlobal, sessionToken);
		Assert.assertFalse(code == 200, "can delete global user that has products permisssions");	
		
		baseUtils.reset(productID2, sessionToken);
		
		code = operApi.deleteAirlockUser(userIDGlobal, sessionToken);
		Assert.assertTrue(code == 200, "cannot delete global user that has no products permisssions");	
		
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
		//baseUtils.reset(productID2, sessionToken);
		//baseUtils.reset(productID1, sessionToken);
	
		for (String userId:usersIds) {
			operApi.deleteAirlockUser(userId, sessionToken);
		}
	}

	

}
