package tests.restapi.scenarios.capabilities;

import java.io.IOException;
import java.util.ArrayList;

import org.apache.wink.json4j.JSONArray;
import org.apache.wink.json4j.JSONException;
import org.apache.wink.json4j.JSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import tests.restapi.AirlockUtils;
import tests.restapi.OperationRestApi;
import tests.restapi.SeasonsRestApi;

public class TestRuntimeEncryptionCapabilities {
	
	protected String m_url;
	protected JSONArray groups;
	protected String userGroups;
	private String sessionToken = "";
	private OperationRestApi operApi;
	private AirlockUtils baseUtils;
	private TestAllApi allApis;
	private String productID;
	private String seasonID;
	private ArrayList<String> results = new ArrayList<String>();
	private SeasonsRestApi s;
	
	@BeforeClass
	@Parameters({"url", "operationsUrl", "analyticsUrl", "translationsUrl", "configPath", "sessionToken", "userName", "userPassword", "appName", "productsToDeleteFile", "isAuthenticated"})
	public void init(String url,  String operationsUrl, String analyticsUrl, String translationsUrl, String configPath, String sToken, String userName, String userPassword, String appName, String productsToDeleteFile, String isAuthenticated) throws Exception{

		
		operApi = new OperationRestApi();
		operApi.setURL(operationsUrl);
		baseUtils = new AirlockUtils(url, analyticsUrl, translationsUrl, configPath, sToken, userName, userPassword, appName, productsToDeleteFile);
		sessionToken = baseUtils.sessionToken;
		allApis = new  TestAllApi(url,operationsUrl,translationsUrl,analyticsUrl, configPath);
		allApis.resetServerCapabilities(sessionToken);
		s = new SeasonsRestApi();
		s.setURL(url);
		if (isAuthenticated.equals("true"))
			allApis.isAuthenticated = true;
		
		baseUtils = new AirlockUtils(url, analyticsUrl, translationsUrl, configPath, sToken, userName, userPassword, appName, productsToDeleteFile);
		sessionToken = baseUtils.sessionToken;
		productID = baseUtils.createProduct();
		baseUtils.printProductToFile(productID);
		seasonID = baseUtils.createSeason(productID);
	}
	

	
	@Test (description = "Set  capabilities in product without Runtime Encryption")
	public void setCapabilities() throws Exception{
		//remove encryption from season
		JSONObject seasonJson = new JSONObject(s.getSeason(productID, seasonID));
		seasonJson.put("runtimeEncryption", false);
		String response = s.updateSeason(seasonID, seasonJson.toString());
		Assert.assertFalse(response.contains("error"), "Can't update season");
		
		JSONArray capabilities = allApis.getCapabilitiesInProduct(productID, sessionToken);
		capabilities.remove("RUNTIME_ENCRYPTION"); 
		response = allApis.setCapabilitiesInProduct(productID, capabilities, sessionToken);
		Assert.assertFalse(response.contains("error"), "Can't set capabilities");
	}
	
	@Test (dependsOnMethods="setCapabilities", description = "Test getEncryptionKey without Encryption Runtime capability")
	public void testGetEncryptionKey() throws JSONException, IOException{
		String response = allApis.getEncryptionKey(seasonID, sessionToken);
		validateNegativeTestResult(response, "get Encryption Key");
	}
	
	@Test (dependsOnMethods="testGetEncryptionKey", description = "Test resetEncryptionKey without Encryption Runtime capability")
	public void testResetEncryptionKey() throws JSONException, IOException{
		String response = allApis.resetEncryptionKey(seasonID, sessionToken);
		validateNegativeTestResult(response, "reset Encryption Key");
	}
	
	@Test (dependsOnMethods="testResetEncryptionKey", description = "Set Encryption Runtime capability and get/reset encryption key, remove Encryption Runtime capability")
	public void testGetResetAfterAddRemoveCapability() throws Exception{
		JSONArray capabilities = allApis.getCapabilitiesInProduct(productID, sessionToken);
		capabilities.add("RUNTIME_ENCRYPTION"); 
		String response = allApis.setCapabilitiesInProduct(productID, capabilities, sessionToken);
		Assert.assertFalse(response.contains("error"), "Can't set capabilities");
		
		//add encryption to season
		JSONObject seasonJson = new JSONObject(s.getSeason(productID, seasonID));
		seasonJson.put("runtimeEncryption", true);
		response = s.updateSeason(seasonID, seasonJson.toString());
		Assert.assertFalse(response.contains("error"), "Can't update season");
			
				
		String key = allApis.getEncryptionKey(seasonID, sessionToken);
		Assert.assertFalse(key.contains("error"), "Can't get EncryptionKey");
		
		key = allApis.resetEncryptionKey(seasonID, sessionToken);
		Assert.assertFalse(key.contains("error"), "Can't reset EncryptionKey");
		
		//add encryption to season
		seasonJson = new JSONObject(s.getSeason(productID, seasonID));
		seasonJson.put("runtimeEncryption", false);
		response = s.updateSeason(seasonID, seasonJson.toString());
		Assert.assertFalse(response.contains("error"), "Can't update season");
		
		capabilities = allApis.getCapabilitiesInProduct(productID, sessionToken);
		capabilities.remove("RUNTIME_ENCRYPTION"); 
		response = allApis.setCapabilitiesInProduct(productID, capabilities, sessionToken);
		Assert.assertFalse(response.contains("error"), "Can't set capabilities");

		//TODO: after add encryption per season - these 2 should fail!
		key = allApis.getEncryptionKey(seasonID, sessionToken);
		Assert.assertTrue(key.contains("error"), "Can't get EncryptionKey");
		
		key = allApis.resetEncryptionKey(seasonID, sessionToken);
		Assert.assertTrue(key.contains("error"), "Can't reset EncryptionKey");
	}
	/*	
	@Test (dependsOnMethods="testNegativeCapabilities", description = "Set notification capability")
	public void addNotificationCapabilities() throws Exception{		
		JSONArray capabilities = allApis.getCapabilitiesInProduct(productID, sessionToken);
		capabilities.add("NOTIFICATIONS"); 
		String response = allApis.setCapabilitiesInProduct(productID, capabilities, sessionToken);
		Assert.assertFalse(response.contains("error"), "Can't set capabilities");
		
	}
	
	@SuppressWarnings("unchecked")
	@Test (dependsOnMethods="addNotificationCapabilities", description = "test notification api with capability")
	public void testPositiveCapabilities() throws Exception{
		
		results = allApis.runAllNotifications(seasonID, sessionToken,false);
		
		if (results.size() > 0)
			Assert.fail("positive notification capability test failed: " + results.toString());

	}
	*/
	
	@AfterTest
	private void reset(){
		baseUtils.reset(productID, sessionToken);
	}
	
	
	private void validateNegativeTestResult(String response, String error){
		if (!response.contains("error"))
			results.add(error);
	}


}
