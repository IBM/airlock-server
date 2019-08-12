package tests.restapi.scenarios.capabilities;


import java.util.ArrayList;

import org.apache.wink.json4j.JSONArray;
import org.testng.Assert;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import tests.restapi.AirlockUtils;
import tests.restapi.BranchesRestApi;
import tests.restapi.OperationRestApi;
import tests.restapi.SeasonsRestApi;

public class TestStringCapabilities {
	
	protected String m_url;
	protected JSONArray groups;
	protected String userGroups;
	private String sessionToken = "";
	private OperationRestApi operApi;
	private AirlockUtils baseUtils;
	private SeasonsRestApi seasonApi;
	private TestAllApi allApis;
	private String productID;
	private String seasonID;
	private String stringID;
	private ArrayList<String> results = new ArrayList<String>();
	
	@BeforeClass
	@Parameters({"url", "operationsUrl", "analyticsUrl", "translationsUrl", "configPath", "sessionToken", "userName", "userPassword", "appName", "productsToDeleteFile", "isAuthenticated"})
	public void init(String url,  String operationsUrl, String analyticsUrl, String translationsUrl, String configPath, String sToken, String userName, String userPassword, String appName, String productsToDeleteFile, String isAuthenticated) throws Exception{
		
		operApi = new OperationRestApi();
		operApi.setURL(operationsUrl);
		seasonApi = new SeasonsRestApi();
		seasonApi.setURL(url);;
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
	

	
	@Test (description = "Set  capabilities in product without string")
	public void setCapabilities() throws Exception{
		JSONArray capabilities = allApis.getCapabilitiesInProduct(productID, sessionToken);
		capabilities.remove("TRANSLATIONS"); 
		String response = allApis.setCapabilitiesInProduct(productID, capabilities, sessionToken);
		Assert.assertFalse(response.contains("error"), "Can't set capabilities");
	}
	
	@Test (dependsOnMethods="setCapabilities", description = "Test create string without translation capability")
	public void testCreateString() throws Exception{
		String response = allApis.addString(seasonID, "/strings/string1.txt", sessionToken);
		Assert.assertTrue(response.contains("error"), "Added string without Translation capabilities");

	}
	
	@Test (dependsOnMethods="testCreateString", description = "Set string capability and add string, remove string capability")
	public void addString() throws Exception{
		JSONArray capabilities = allApis.getCapabilitiesInProduct(productID, sessionToken);
		capabilities.add("TRANSLATIONS"); 
		String response = allApis.setCapabilitiesInProduct(productID, capabilities, sessionToken);
		Assert.assertFalse(response.contains("error"), "Can't set capabilities");
		
		stringID = allApis.addString(seasonID, "/strings/string2.txt", sessionToken);
		Assert.assertFalse(stringID.contains("error"), "Can't create string: " + stringID);
		
		capabilities = allApis.getCapabilitiesInProduct(productID, sessionToken);
		capabilities.remove("TRANSLATIONS"); 
		response = allApis.setCapabilitiesInProduct(productID, capabilities, sessionToken);
		Assert.assertFalse(response.contains("error"), "Can't set capabilities");

	}
	
	@Test (dependsOnMethods="addString", description = "test string api without capability")
	public void testNegativeCapabilities() throws Exception{
		
		String response = allApis.addString(seasonID, "/strings/string1.txt", sessionToken);
		validateNegativeTestResult(response, "create string");
		response = allApis.getAllStrings(seasonID, sessionToken);
		validateNegativeTestResult(response, "get all strings");
		response = allApis.getString(stringID, sessionToken);
		validateNegativeTestResult(response, "get string");
		response = allApis.updateString(stringID, sessionToken);
		validateNegativeTestResult(response, "update string");
		response = allApis.getStringForTranslation(seasonID, sessionToken);
		validateNegativeTestResult(response, "getStringForTranslation");
		response = allApis.getNewStringsForTranslation(seasonID, sessionToken);
		validateNegativeTestResult(response, "getNewStringsForTranslation");
		
		String featureID = allApis.createFeature(seasonID, BranchesRestApi.MASTER, sessionToken);
		response = allApis.getStringsUsedInFeature(featureID, sessionToken);
		validateNegativeTestResult(response, "getNewStringsForTranslation");
		response = allApis.getStringStatuses(seasonID, sessionToken);
		validateNegativeTestResult(response, "getStringStatuses");
		response = allApis.getStringsByStatuses(seasonID, sessionToken);
		validateNegativeTestResult(response, "getStringsByStatuses");
		response = allApis.getSupportedLocales(seasonID, sessionToken);
		validateNegativeTestResult(response, "getSupportedLocales");
		response = allApis.addSupportedLocales(seasonID, sessionToken);
		validateNegativeTestResult(response, "addSupportedLocales");
		
		response = allApis.addTranslation(seasonID, "fr", sessionToken);
		validateNegativeTestResult(response, "addTranslation");
		response = allApis.getTranslation(seasonID, sessionToken);
		validateNegativeTestResult(response, "addTranslation");
		response = allApis.updateTranslation(seasonID, sessionToken);
		validateNegativeTestResult(response, "updateTranslation");
		
		response = allApis.markForTranslation(seasonID, stringID, sessionToken);
		validateNegativeTestResult(response, "markForTranslation");
		response = allApis.reviewTranslation(seasonID, stringID, sessionToken);
		validateNegativeTestResult(response, "reviewTranslation");
		response = allApis.sendToTranslation(seasonID, stringID, sessionToken);
		validateNegativeTestResult(response, "sendToTranslation");
		response = allApis.overrideTranslate(stringID, sessionToken);
		validateNegativeTestResult(response, "overrideTranslate");
		response = allApis.cancelOverrideTranslate(stringID, sessionToken);
		validateNegativeTestResult(response, "cancelOverrideTranslate");
		response = allApis.getTranslationSummary(seasonID, stringID, sessionToken);
		validateNegativeTestResult(response, "getTranslationSummary");
		response = allApis.removeSupportedLocales(seasonID, sessionToken);
		validateNegativeTestResult(response, "removeSupportedLocales");
		
		String season = "{\"minVersion\":\"3.0\"}";
		String seasonID2 = seasonApi.addSeason(productID, season, sessionToken);
		response = allApis.copyStrings(seasonID2, stringID, sessionToken);
		validateNegativeTestResult(response, "copyStrings");
		response = allApis.importStrings(seasonID, seasonID2, sessionToken);
		validateNegativeTestResult(response, "importStrings");
		
		int responseCode = allApis.deleteString(stringID, sessionToken);
		if (responseCode == 200)
			results.add("delete string");	
		
		if (results.size() > 0)
			Assert.fail("negative string capability test failed: " + results.toString());
		
	}
	
	
	@Test (dependsOnMethods="testNegativeCapabilities", description = "Set string capability")
	public void addStringCapabilities() throws Exception{		
		JSONArray capabilities = allApis.getCapabilitiesInProduct(productID, sessionToken);
		capabilities.add("TRANSLATIONS"); 
		String response = allApis.setCapabilitiesInProduct(productID, capabilities, sessionToken);
		Assert.assertFalse(response.contains("error"), "Can't set capabilities");
		
	}
	
	@SuppressWarnings("unchecked")
	@Test (dependsOnMethods="addStringCapabilities", description = "test string api with capability")
	public void testPositiveCapabilities() throws Exception{
		

		results = allApis.runAllStrings(seasonID, productID, sessionToken, false);
		
		if (results.size() > 0)
			Assert.fail("positive string capability test failed: " + results.toString());

	}
	
	@AfterTest(alwaysRun = true)
	private void reset(){
		baseUtils.reset(productID, sessionToken);
	}
	
	
	private void validateNegativeTestResult(String response, String error){
		if (!response.contains("error"))
			results.add(error);
	}


}
