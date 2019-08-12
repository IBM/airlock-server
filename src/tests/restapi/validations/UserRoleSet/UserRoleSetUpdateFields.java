package tests.restapi.validations.UserRoleSet;

import java.io.IOException;
import java.util.LinkedList;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.wink.json4j.JSON;
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

public class UserRoleSetUpdateFields {
	protected String seasonID;
	protected String filePath;
	protected String m_url;
	private String userID;
	protected OperationRestApi operApi;
	private AirlockUtils baseUtils;
	protected String productID;
	private String sessionToken = "";
	private LinkedList<String> usersIds = new LinkedList<String>();
	
	
	@BeforeClass
	@Parameters({"url", "analyticsUrl", "translationsUrl", "operationsUrl", "configPath", "sessionToken", "userName", "userPassword", "appName", "productsToDeleteFile"})
	public void init(String url, String analyticsUrl, String translationsUrl, String c_operationsUrl, String configPath, String sToken, String userName, String userPassword, String appName, String productsToDeleteFile) throws Exception{
		filePath = configPath;
		baseUtils = new AirlockUtils(url, analyticsUrl, translationsUrl, configPath, sToken, userName, userPassword, appName, productsToDeleteFile);
		sessionToken = baseUtils.sessionToken;
		productID = baseUtils.createProduct();
		baseUtils.printProductToFile(productID);
		seasonID = baseUtils.createSeason(productID);

		m_url = url;
		operApi = new OperationRestApi();
		operApi.setURL(c_operationsUrl);
		
		String user = FileUtils.fileToString(filePath + "airlockUser.txt", "UTF-8", false);
		JSONObject userJSON = new JSONObject(user);
		userJSON.put("identifier", RandomStringUtils.randomAlphabetic(5)+"@il.ibm.com");
		userID = operApi.addGlobalAirlockUser(userJSON.toString(),sessionToken);
		Assert.assertFalse(userID.contains("error"), "User was not created: " + userID);
		
		usersIds.add(userID);
		
	}
	
	@Test
	private void identifierField() throws Exception{
		JSONObject json = getUser();
		json.remove("identifier");
		updateUser(json, "identifier", true);
		
		json = getUser();
		json.put("identifier", "");
		updateUser(json, "identifier", true);
		
		json = getUser();
		json.put("identifier", JSON.NULL);
		updateUser(json, "identifier", true);
	}
	
	
	@Test
	private void isGroupRepresentationField() throws Exception{
		JSONObject json = getUser();
		json.remove("isGroupRepresentation");
		updateUser(json, "isGroupRepresentation", true);
		
		json = getUser();
		json.put("isGroupRepresentation", "");
		updateUser(json, "isGroupRepresentation", true);
		
		json = getUser();
		json.put("isGroupRepresentation", JSON.NULL);
		updateUser(json, "isGroupRepresentation", true);
	}
	
	@Test
	private void uniqueIdField() throws Exception{
		JSONObject json = getUser();
		json.put("uniqueId", "");
		updateUser(json, "uniqueId", true);
		
		json = getUser();
		json.put("uniqueId", JSON.NULL);
		updateUser(json, "uniqueId", false);
		
		json = getUser();
		json.remove("uniqueId");
		updateUser(json, "uniqueId", false);
	}
	
	@Test
	private void PRODUCTIdField() throws Exception{
		JSONObject json = getUser();
		json.put("productId", "");
		updateUser(json, "productId", true);
		
		json = getUser();
		json.put("productId", JSON.NULL);
		updateUser(json, "productId", false);
		
		json = getUser();
		json.remove("productId");
		updateUser(json, "productId", true);
	}
	
	@Test
	private void creationDateField() throws Exception{
		JSONObject json = getUser();
		json.put("creationDate", System.currentTimeMillis());
		updateUser(json, "creationDate", true);

		json = getUser();
		json.put("creationDate", "");
		updateUser(json, "creationDate", true);
		
		json = getUser();
		json.put("creationDate", JSON.NULL);
		updateUser(json, "creationDate", true);
	}
	
	@Test
	private void creatorField() throws Exception{
		JSONObject json = getUser();
		json.remove("creator");
		updateUser(json, "creator", true);
		
		json = getUser();
		json.put("creator", "");
		updateUser(json, "creator", true);
		
		json = getUser();
		json.put("creator", JSON.NULL);
		updateUser(json, "creator", true);
	}
	
	
	@Test
	private void rolesField() throws Exception{
		JSONObject json = getUser();
		json.remove("roles");
		updateUser(json, "roles", true);
		
		json = getUser();
		json.put("roles", "");
		updateUser(json, "roles", true);
		
		json = getUser();
		json.put("roles", JSON.NULL);
		updateUser(json, "roles", true);
		
		json = getUser();
		json.put("roles", new JSONArray());
		updateUser(json, "roles", true);

	}
	
	@Test
	private void lastModifiedField() throws Exception{
		JSONObject json = getUser();
		json.remove("lastModified");
		updateUser(json, "lastModified", true);

		json = getUser();
		json.put("lastModified", "");
		updateUser(json, "lastModified", true);
		
		json = getUser();
		json.put("lastModified", JSON.NULL);
		updateUser(json, "lastModified", true);
		
		JSONArray rolesArr = new JSONArray();
		rolesArr.add("Viewer");
		rolesArr.add("Viewer");
		json = getUser();
		json.put("roles", rolesArr);
		updateUser(json, "lastModified", true);
		
		rolesArr.clear();
		rolesArr.add("Viewe");
		json = getUser();
		json.put("roles", rolesArr);
		updateUser(json, "lastModified", true);
	}
		
	private void updateUser(JSONObject userJson, String field, boolean failureExpected) throws Exception{

		try {
			String response = operApi.updateAirlockUser(userID, userJson.toString(), sessionToken);
			Assert.assertEquals(response.contains("error"), failureExpected,  "Test failed for field: " + field);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}	
	}
	
	private JSONObject getUser() throws Exception{
		String user = operApi.getAirlockUser(userID, sessionToken);
		JSONObject json = new JSONObject(user);
		return json;
	}
	


	
	@AfterTest
	public void reset() throws Exception{
		baseUtils.reset(productID, sessionToken);
		for (String userId:usersIds) {
			operApi.deleteAirlockUser(userId, sessionToken);
		}
	}

	

}
