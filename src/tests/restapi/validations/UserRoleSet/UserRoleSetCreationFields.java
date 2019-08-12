package tests.restapi.validations.UserRoleSet;

import java.io.IOException;
import java.util.LinkedList;
import java.util.UUID;

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

public class UserRoleSetCreationFields {
	protected String seasonID;
	protected String filePath;
	protected String m_url;
	protected String user;
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
		user = FileUtils.fileToString(filePath + "airlockUser.txt", "UTF-8", false);
		
	}
	
	@Test
	private void identifierField() throws Exception{
		JSONObject json = new JSONObject(user);
		json.remove("identifier");
		addUser(json, "identifier", true);
		
		json = new JSONObject(user);
		json.put("identifier", "");
		addUser(json, "identifier", true);
		
		json = new JSONObject(user);
		json.put("identifier", JSON.NULL);
		addUser(json, "identifier", true);
	}
	
	@Test
	private void rolesField() throws Exception{
		JSONObject json = new JSONObject(user);
		json.remove("roles");
		addUser(json, "roles", true);
		
		json = new JSONObject(user);
		json.put("roles", "");
		addUser(json, "roles", true);
		
		json = new JSONObject(user);
		json.put("roles", JSON.NULL);
		addUser(json, "roles", true);
		
		json = new JSONObject(user);
		json.put("roles", new JSONArray());
		addUser(json, "roles", true);
		
		json = new JSONObject(user);
		json.put("roles", new JSONArray());
		addUser(json, "roles", true);
		
		JSONArray rolesArr = new JSONArray();
		rolesArr.add("Viewer");
		rolesArr.add("Viewer");
		json = new JSONObject(user);
		json.put("roles", rolesArr);
		addUser(json, "roles", true);
		
		rolesArr.clear();
		rolesArr.add("Viewe");
		json = new JSONObject(user);
		json.put("roles", rolesArr);
		addUser(json, "roles", true);

	}
	@Test
	private void isGroupRepresentationField() throws Exception{
		JSONObject json = new JSONObject(user);
		json.remove("isGroupRepresentation");
		addUser(json, "isGroupRepresentation", true);
		
		json = new JSONObject(user);
		json.put("isGroupRepresentation", "");
		addUser(json, "isGroupRepresentation", true);
		
		json = new JSONObject(user);
		json.put("isGroupRepresentation", JSON.NULL);
		addUser(json, "isGroupRepresentation", true);
	}
	
	@Test
	private void uniqueIdField() throws Exception{
		JSONObject json = new JSONObject(user);
		json = new JSONObject(user);
		json.put("uniqueId", "");
		addUser(json, "uniqueId", true);
		
		json = new JSONObject(user);
		json.put("uniqueId", JSON.NULL);
		addUser(json, "uniqueId", false);
		
		json = new JSONObject(user);
		json.put("uniqueId", UUID.randomUUID());
		addUser(json, "uniqueId", true);
	}
	
	@Test
	private void productIdField() throws Exception{
		JSONObject json = new JSONObject(user);
		json = new JSONObject(user);
		json.put("productId", "");
		addUser(json, "productId", true);
		
		json = new JSONObject(user);
		json.put("productId", JSON.NULL);
		addUser(json, "productId", false);
		
		json = new JSONObject(user);
		json.put("productId", UUID.randomUUID());
		addUser(json, "productId", true);
	}
	
	@Test
	private void creationDateField() throws Exception{
		JSONObject json = new JSONObject(user);
		json = new JSONObject(user);
		json.put("creationDate", System.currentTimeMillis());
		addUser(json, "creationDate", true);

		json = new JSONObject(user);
		json.put("creationDate", "");
		addUser(json, "creationDate", true);
		
		json = new JSONObject(user);
		json.put("creationDate", JSON.NULL);
		addUser(json, "creationDate", false);
	}
	
	@Test
	private void creatorField() throws Exception{
		JSONObject json = new JSONObject(user);
		json.remove("creator");
		addUser(json, "creator", true);
		
		json = new JSONObject(user);
		json.put("creator", "");
		addUser(json, "creator", true);
		
		json = new JSONObject(user);
		json.put("creator", JSON.NULL);
		addUser(json, "creator", true);
	}
	
	
	@Test
	private void lastModifiedField() throws Exception{
		JSONObject json = new JSONObject(user);
		json = new JSONObject(user);
		json.put("lastModified", System.currentTimeMillis()-10000);
		addUser(json, "lastModified", true);

		json = new JSONObject(user);
		json.put("lastModified", "");
		addUser(json, "lastModified", true);
		
		json = new JSONObject(user);
		json.put("lastModified", JSON.NULL);
		addUser(json, "lastModified", false);
	}
	
		
	private void addUser(JSONObject userJson, String field, boolean expectFailure) throws Exception{
		if (!field.equals("identifier"))
			userJson.put("identifier", RandomStringUtils.randomAlphabetic(5));

		try {
			String response = operApi.addGlobalAirlockUser(userJson.toString(), sessionToken);
			Assert.assertEquals(response.contains("error"), expectFailure,  "Test failed for field: " + field);
			
			if (!expectFailure)
				usersIds.add(response);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}	
	}
	
	
	@AfterTest
	public void reset() throws Exception{
		baseUtils.reset(productID, sessionToken);
		for (String userId:usersIds) {
			operApi.deleteAirlockUser(userId, sessionToken);
		}
	}

	

}
