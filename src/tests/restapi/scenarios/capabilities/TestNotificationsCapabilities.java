package tests.restapi.scenarios.capabilities;

import java.io.IOException;
import java.util.ArrayList;

import org.apache.wink.json4j.JSONArray;
import org.apache.wink.json4j.JSONException;
import org.testng.Assert;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import tests.restapi.AirlockUtils;
import tests.restapi.OperationRestApi;

public class TestNotificationsCapabilities {
	
	protected String m_url;
	protected JSONArray groups;
	protected String userGroups;
	private String sessionToken = "";
	private OperationRestApi operApi;
	private AirlockUtils baseUtils;
	private TestAllApi allApis;
	private String productID;
	private String seasonID;
	private String notificationID;
	private ArrayList<String> results = new ArrayList<String>();
	
	@BeforeClass
	@Parameters({"url", "operationsUrl", "analyticsUrl", "translationsUrl", "configPath", "sessionToken", "userName", "userPassword", "appName", "productsToDeleteFile", "isAuthenticated"})
	public void init(String url,  String operationsUrl, String analyticsUrl, String translationsUrl, String configPath, String sToken, String userName, String userPassword, String appName, String productsToDeleteFile, String isAuthenticated) throws Exception{

		
		operApi = new OperationRestApi();
		operApi.setURL(operationsUrl);
		baseUtils = new AirlockUtils(url, analyticsUrl, translationsUrl, configPath, sToken, userName, userPassword, appName, productsToDeleteFile);
		sessionToken = baseUtils.sessionToken;
		allApis = new  TestAllApi(url,operationsUrl,translationsUrl,analyticsUrl, configPath);
		allApis.resetServerCapabilities(sessionToken);
		
		if (isAuthenticated.equals("true"))
			allApis.isAuthenticated = true;
		
		baseUtils = new AirlockUtils(url, analyticsUrl, translationsUrl, configPath, sToken, userName, userPassword, appName, productsToDeleteFile);
		sessionToken = baseUtils.sessionToken;
		productID = baseUtils.createProduct();
		baseUtils.printProductToFile(productID);
		seasonID = baseUtils.createSeason(productID);


	}
	

	
	@Test (description = "Set  capabilities in product without notification")
	public void setCapabilities() throws Exception{
		JSONArray capabilities = allApis.getCapabilitiesInProduct(productID, sessionToken);
		capabilities.remove("NOTIFICATIONS"); 
		String response = allApis.setCapabilitiesInProduct(productID, capabilities, sessionToken);
		Assert.assertFalse(response.contains("error"), "Can't set capabilities");
	}
	
	@Test (dependsOnMethods="setCapabilities", description = "Test create notification without notification capability")
	public void testCreateNotification() throws JSONException, IOException{
		String response = allApis.addNotification(seasonID, sessionToken);
		validateNegativeTestResult(response, "create notification");

	}
	
	@Test (dependsOnMethods="testCreateNotification", description = "Set notification capability and add notification, remove notification capability")
	public void addNotification() throws Exception{
		JSONArray capabilities = allApis.getCapabilitiesInProduct(productID, sessionToken);
		capabilities.add("NOTIFICATIONS"); 
		String response = allApis.setCapabilitiesInProduct(productID, capabilities, sessionToken);
		Assert.assertFalse(response.contains("error"), "Can't set capabilities");
		
		notificationID = allApis.addNotification(seasonID, sessionToken);
		Assert.assertFalse(notificationID.contains("error"), "Can't create stream: " + notificationID);
		
		capabilities = allApis.getCapabilitiesInProduct(productID, sessionToken);
		capabilities.remove("NOTIFICATIONS"); 
		response = allApis.setCapabilitiesInProduct(productID, capabilities, sessionToken);
		Assert.assertFalse(response.contains("error"), "Can't set capabilities");

	}
	
	@Test (dependsOnMethods="addNotification", description = "test notification api without capability")
	public void testNegativeCapabilities() throws Exception{
		
		String response = allApis.updateNotification(notificationID, sessionToken);
		validateNegativeTestResult(response, "update notification");
		
		response = allApis.getNotification(notificationID, sessionToken);
		validateNegativeTestResult(response, "get notification");
		
		
		response = allApis.getAllNotifications(seasonID, sessionToken);
		validateNegativeTestResult(response, "get all notification");
		
		response = allApis.updateAllNotifications(seasonID, sessionToken);
		validateNegativeTestResult(response, "get all notification");
	
		int responseCode = allApis.deleteNotification(notificationID, sessionToken);
		if (responseCode == 200)
			results.add("delete notification");

		
		if (results.size() > 0)
			Assert.fail("negative notification capability test failed: " + results.toString());
		
	}
	
	
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
	
	@AfterTest
	private void reset(){
		baseUtils.reset(productID, sessionToken);
	}
	
	
	private void validateNegativeTestResult(String response, String error){
		if (!response.contains("error"))
			results.add(error);
	}


}
