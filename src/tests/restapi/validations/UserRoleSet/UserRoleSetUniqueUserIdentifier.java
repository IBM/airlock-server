package tests.restapi.validations.UserRoleSet;


import java.util.LinkedList;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.wink.json4j.JSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import tests.com.ibm.qautils.FileUtils;
import tests.restapi.AirlockUtils;
import tests.restapi.OperationRestApi;

public class UserRoleSetUniqueUserIdentifier {
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
	}
	
	@Test (description="Create the same global user identifier twice")
	private void createGlobalUserTwice() throws Exception{
		String user = FileUtils.fileToString(filePath + "airlockUser.txt", "UTF-8", false);
		JSONObject userJSON = new JSONObject(user);
		userJSON.put("identifier", RandomStringUtils.randomAlphabetic(5)+"@il.ibm.com");
		userID = operApi.addGlobalAirlockUser(userJSON.toString(),sessionToken);
		usersIds.add(userID);
		Assert.assertFalse(userID.contains("error"), "User was not created: " + userID);

		String response = operApi.addGlobalAirlockUser(userJSON.toString(),sessionToken);
		Assert.assertTrue(response.contains("already exists"), "User was created" );
	}

	@Test (description="Create the same product user identifier twice")
	private void createProductUserTwice() throws Exception{
		String user = FileUtils.fileToString(filePath + "airlockUser.txt", "UTF-8", false);		
		JSONObject userJSON = new JSONObject(user);
		userJSON.put("identifier", RandomStringUtils.randomAlphabetic(5)+"@il.ibm.com");
		
		String userIDp = operApi.addProductAirlockUser(userJSON.toString(), productID, sessionToken);
		Assert.assertFalse(userIDp.contains("error"), "User was not created: " + userID);
		usersIds.add(userIDp);
		
		String response = operApi.addProductAirlockUser(userJSON.toString(), productID, sessionToken);
		Assert.assertTrue(response.contains("already exists"), "User was created" );
	}

	@AfterTest
	public void reset() throws Exception{
		baseUtils.reset(productID, sessionToken);
		for (String userId:usersIds) {
			operApi.deleteAirlockUser(userId, sessionToken);
		}
	}

	

}
