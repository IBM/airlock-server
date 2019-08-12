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

public class TestEntitlementsCapabilities {
	
	protected String m_url;
	protected JSONArray groups;
	protected String userGroups;
	private String sessionToken = "";
	private OperationRestApi operApi;
	private AirlockUtils baseUtils;
	private TestAllApi allApis;
	private String productID;
	private String seasonID;
	private String inAppPurchaseID;
	private String purchaseOptionsID;
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
	

	
	@Test (description = "Set  capabilities in product without ENTITLEMENTS")
	public void setCapabilities() throws Exception{
		JSONArray capabilities = allApis.getCapabilitiesInProduct(productID, sessionToken);
		capabilities.remove("ENTITLEMENTS"); 
		String response = allApis.setCapabilitiesInProduct(productID, capabilities, sessionToken);
		Assert.assertFalse(response.contains("error"), "Can't set capabilities");
	}
	
	@Test (dependsOnMethods="setCapabilities", description = "Test create inAppPurchase without ENTITLEMENTS capability")
	public void testCreateInAppPurchase() throws JSONException, IOException{
		String response = allApis.addInAppPurchase(seasonID, sessionToken);
		validateNegativeTestResult(response, "create inAppPurchase");
	}
	
	@Test (dependsOnMethods="testCreateInAppPurchase", description = "Set ENTITLEMENTS capability and add inAppPurchase, remove ENTITLEMENTS capability")
	public void addInAppPurchase() throws Exception{
		JSONArray capabilities = allApis.getCapabilitiesInProduct(productID, sessionToken);
		capabilities.add("ENTITLEMENTS"); 
		String response = allApis.setCapabilitiesInProduct(productID, capabilities, sessionToken);
		Assert.assertFalse(response.contains("error"), "Can't set capabilities");
		
		inAppPurchaseID = allApis.addInAppPurchase(seasonID, sessionToken);
		Assert.assertFalse(inAppPurchaseID.contains("error"), "Can't create inAppPurchase: " + inAppPurchaseID);
		
		purchaseOptionsID = allApis.addPurchaseOptions(seasonID, inAppPurchaseID, sessionToken);
		Assert.assertFalse(purchaseOptionsID.contains("error"), "Can't create purchaseOptions: " + purchaseOptionsID);
		
		capabilities = allApis.getCapabilitiesInProduct(productID, sessionToken);
		capabilities.remove("ENTITLEMENTS"); 
		response = allApis.setCapabilitiesInProduct(productID, capabilities, sessionToken);
		Assert.assertFalse(response.contains("error"), "Can't set capabilities");

	}
	
	@Test (dependsOnMethods="addInAppPurchase", description = "test ENTITLEMENTS api without capability")
	public void testNegativeCapabilities() throws Exception{
		
		String response = allApis.updatePurchase(seasonID, inAppPurchaseID, sessionToken);
		validateNegativeTestResult(response, "update inAppPurchase");
		
		response = allApis.getPurcahse(inAppPurchaseID, sessionToken);
		validateNegativeTestResult(response, "get inAppPurchase");
		
		response = allApis.getAllPurchases(seasonID, sessionToken);
		validateNegativeTestResult(response, "get all purchases");
		
		int responseCode = allApis.deletePurchase(inAppPurchaseID, sessionToken);
		if (responseCode == 200)
			results.add("delete inAppPurchase");

		if (results.size() > 0)
			Assert.fail("negative ENTITLEMENTS capability test failed: " + results.toString());
		
	}
	
	
	@Test (dependsOnMethods="testNegativeCapabilities", description = "Set ENTITLEMENTS capability")
	public void addInAppPurcahsesCapabilities() throws Exception{		
		JSONArray capabilities = allApis.getCapabilitiesInProduct(productID, sessionToken);
		capabilities.add("ENTITLEMENTS"); 
		String response = allApis.setCapabilitiesInProduct(productID, capabilities, sessionToken);
		Assert.assertFalse(response.contains("error"), "Can't set capabilities");
		
	}
	
	@SuppressWarnings("unchecked")
	@Test (dependsOnMethods="addInAppPurcahsesCapabilities", description = "test inAppPurchase api with capability")
	public void testPositiveCapabilities() throws Exception{
		
		results = allApis.runAllPurchases(seasonID, sessionToken,false);
		
		if (results.size() > 0)
			Assert.fail("positive ENTITLEMENTS capability test failed: " + results.toString());

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
