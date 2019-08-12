package tests.restapi.validations.UserRoleSet;

import java.io.IOException;
import java.util.LinkedList;
import java.util.UUID;

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

public class UserRoleSetUpdateValidation {
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
		json.put("identifier", "11@22.com");
		//updateStringField(json, "identifier", "11@22.com");
		String response = operApi.updateAirlockUser(userID, json.toString(), sessionToken);
		Assert.assertTrue(response.contains("error"), "User identifier was changed");	
	}
	
	
	@Test
	private void isGroupRepresentationField() throws Exception{
		JSONObject json = getUser();
		json.put("isGroupRepresentation", false);
		updateBooleanField(json, "isGroupRepresentation", false);
			
		json = getUser();
		json.put("isGroupRepresentation", true);
		updateBooleanField(json, "isGroupRepresentation", true);
	}
	
	@Test
	private void uniqueIdField() throws Exception{
		JSONObject json = getUser();
		json.put("uniqueId", UUID.randomUUID());
		String response = operApi.updateAirlockUser(userID, json.toString(), sessionToken);
		Assert.assertTrue(response.contains("error"), " User was updated with incorrect uniqueId: " + response);

	}
	
	@Test
	private void productIdField() throws Exception{
		JSONObject json = getUser();
		json.put("productId", UUID.randomUUID());
		String response = operApi.updateAirlockUser(userID, json.toString(), sessionToken);
		Assert.assertTrue(response.contains("error"), " User was updated with incorrect productId: " + response);

	}
	
	@Test
	private void creatorField() throws Exception{
		JSONObject json = getUser();
		json.put("creator", "new creator");
		String response = operApi.updateAirlockUser(userID, json.toString(), sessionToken);
		Assert.assertTrue(response.contains("error"), " User shouldn't be updated ");

	}
	


	@Test
	private void rolesField() throws Exception{
		JSONObject json = getUser();
		JSONArray roles = new JSONArray();
		roles.add("Viewer");
		json.put("roles", roles);
		
		try {
			String response = operApi.updateAirlockUser(userID, json.toString(), sessionToken);
			Assert.assertFalse(response.contains("error"), " Can't update user: " + response);
			
			String user = operApi.getAirlockUser(userID, sessionToken);
			json = new JSONObject(user);
			Assert.assertTrue (json.getJSONArray("roles").size()==1, "roles field was not updated");
			Assert.assertTrue (json.getJSONArray("roles").get(0).equals("Viewer"), "roles field incorrect");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	@Test
	private void lastModifiedField() throws Exception{
		JSONObject json = getUser();
		long newTime = System.currentTimeMillis()-1000;
		json.put("lastModified", newTime);
		updateDateField(json, "lastModified", newTime);

	}
	
	@Test
	private void creationDateField() throws Exception{
		JSONObject json = getUser();
		long newTime = System.currentTimeMillis()-1000;
		json.put("creationDate", newTime);
		
		String response = operApi.updateAirlockUser(userID, json.toString(), sessionToken);
		
		Assert.assertTrue (response.contains("error"), "creationDate  was updated");
	}
		
	@Test
	private void rolesFieldMultiUsers() throws Exception{
		JSONObject json = getUser();
		JSONArray roles = new JSONArray();
		roles.add("Editor");
		roles.add("Viewer");
		json.put("roles", roles);
		
		try {
			String response = operApi.updateAirlockUser(userID, json.toString(), sessionToken);
			Assert.assertFalse(response.contains("error"), " Can't update user: " + response);
			
			String user = operApi.getAirlockUser(userID, sessionToken);
			JSONObject newJson = new JSONObject(user);
			Assert.assertTrue (newJson.getJSONArray("roles").size()==2, "roles field was not updated");
			Assert.assertTrue (newJson.getJSONArray("roles").get(0).equals("Editor"), "roles field incorrect");
			Assert.assertTrue (newJson.getJSONArray("roles").get(1).equals("Viewer"), "roles field incorrect");
			
			roles.clear();
			roles.add("Editor");
			json.put("roles", roles);
			
			response = operApi.updateAirlockUser(userID, json.toString(), sessionToken);
			Assert.assertTrue(response.contains("error"), " Can update user when already changed by another user: " + response);
			
			user = operApi.getAirlockUser(userID, sessionToken);
			newJson = new JSONObject(user);
			Assert.assertTrue (newJson.getJSONArray("roles").size()==2, "roles field was not updated");
			Assert.assertTrue (newJson.getJSONArray("roles").get(0).equals("Editor"), "roles field incorrect");
			Assert.assertTrue (newJson.getJSONArray("roles").get(1).equals("Viewer"), "roles field incorrect");
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	/*
	private void updateStringField(JSONObject userJson, String field, String expectedValue) throws Exception{

		try {
			String response = operApi.updateAirlockUser(userID, userJson.toString(), sessionToken);
			Assert.assertEquals(response.contains("error"), false,  "Test failed for field: " + field);
			
			String user = operApi.getAirlockUser(userID, sessionToken);
			JSONObject json = new JSONObject(user);

			Assert.assertTrue (json.getString(field).equals(expectedValue),  field + " was not updated");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}	
	}*/
	private void updateBooleanField(JSONObject userJson, String field, Boolean expectedValue) throws Exception{

		try {
			String response = operApi.updateAirlockUser(userID, userJson.toString(), sessionToken);
			Assert.assertEquals(response.contains("error"), false,  "Test failed for field: " + field);
			
			String user = operApi.getAirlockUser(userID, sessionToken);
			JSONObject json = new JSONObject(user);

			Assert.assertTrue (json.getBoolean(field) == expectedValue,  field + " was not updated");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}	
	}
		
	private void updateDateField(JSONObject userJson, String field, long expectedValue) throws Exception{

		try {
			String response = operApi.updateAirlockUser(userID, userJson.toString(), sessionToken);
			
			String user = operApi.getAirlockUser(userID, sessionToken);
			JSONObject json = new JSONObject(user);
			Assert.assertFalse (json.getLong(field)==expectedValue,  field + " was updated");
			
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
