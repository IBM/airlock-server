package tests.restapi.scenarios.capabilities;

import java.util.ArrayList;

import org.apache.wink.json4j.JSONArray;
import org.apache.wink.json4j.JSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import tests.com.ibm.qautils.FileUtils;
import tests.restapi.AirlockUtils;
import tests.restapi.BranchesRestApi;
import tests.restapi.FeaturesRestApi;
import tests.restapi.OperationRestApi;

public class TestAnalyticsCapabilities {
	
	protected String m_url;
	protected JSONArray groups;
	protected String userGroups;
	private String sessionToken = "";
	private OperationRestApi operApi;
	private AirlockUtils baseUtils;
	private FeaturesRestApi f;
	private TestAllApi allApis;
	private String productID;
	private String seasonID;
	private String branchID = BranchesRestApi.MASTER;
	private String featureID;
	private String filePath;
	private ArrayList<String> results = new ArrayList<String>();
	
	@BeforeClass
	@Parameters({"url", "operationsUrl", "analyticsUrl", "translationsUrl", "configPath", "sessionToken", "userName", "userPassword", "appName", "productsToDeleteFile", "isAuthenticated"})
	public void init(String url,  String operationsUrl, String analyticsUrl, String translationsUrl, String configPath, String sToken, String userName, String userPassword, String appName, String productsToDeleteFile, String isAuthenticated) throws Exception{
		filePath = configPath;
		
		operApi = new OperationRestApi();
		operApi.setURL(operationsUrl);
		f = new FeaturesRestApi();
		f.setURL(url);
		
		baseUtils = new AirlockUtils(url, analyticsUrl, translationsUrl, configPath, sToken, userName, userPassword, appName, productsToDeleteFile);
		sessionToken = baseUtils.sessionToken;
		allApis = new  TestAllApi(url,operationsUrl,translationsUrl,analyticsUrl, configPath);
		allApis.resetServerCapabilities(sessionToken);

		
		baseUtils = new AirlockUtils(url, analyticsUrl, translationsUrl, configPath, sToken, userName, userPassword, appName, productsToDeleteFile);
		sessionToken = baseUtils.sessionToken;
		productID = baseUtils.createProduct();
		baseUtils.printProductToFile(productID);
		seasonID = baseUtils.createSeason(productID);

	}
	
	@Test(description="add components")
	public void addComponents() throws Exception{
		featureID = allApis.createFeature(seasonID, branchID, sessionToken);
		Assert.assertFalse(featureID.contains("error"), "feature was not created: " + featureID);
		
		String config = FileUtils.fileToString(filePath + "configuration_rule1.txt", "UTF-8", false);
		JSONObject jsonConfig = new JSONObject(config);
		JSONObject newConfiguration = new JSONObject();
		newConfiguration.put("color", "red");
		jsonConfig.put("configuration", newConfiguration);
		String configID = f.addFeatureToBranch(seasonID, branchID, jsonConfig.toString(), featureID, sessionToken);
		Assert.assertFalse(configID.contains("error"), "Configuration was not added to the season" + configID);
		
		String response = allApis.updateInputSchema(seasonID, sessionToken);
		Assert.assertFalse(response.contains("error"), "Can't update input schema: " + response);

	}
	
	@Test (dependsOnMethods="addComponents", description = "Set  capabilities in product without analytics")
	public void setCapabilities() throws Exception{
		JSONArray capabilities = allApis.getCapabilitiesInProduct(productID, sessionToken);
		capabilities.remove("ANALYTICS"); 
		String response = allApis.setCapabilitiesInProduct(productID, capabilities, sessionToken);
		Assert.assertFalse(response.contains("error"), "Can't set capabilities");
	}
	
	@Test (dependsOnMethods="setCapabilities", description = "test analytics api without capability")
	public void testNegativeCapabilities() throws Exception{
		
		String response = allApis.addFeatureToAnalytics(featureID, branchID, sessionToken);
		validateNegativeTestResult(response, "add feature to analytics");
		
		JSONArray attributes = new JSONArray();
		JSONObject attr1 = new JSONObject();
		attr1.put("name", "color");
		attr1.put("type", "REGULAR");
		attributes.add(attr1);

		response = allApis.updateFeatureAttributesForAnalytics(featureID, branchID, attributes.toString(), sessionToken);
		validateNegativeTestResult(response, "update attributes to analytics");
		
		response = allApis.updateInputFieldsForAnalytics(seasonID, branchID, "context.weatherSummary.closestLightning.cardinalDirection", sessionToken);
		validateNegativeTestResult(response, "update input fields to analytics");
		
		response = allApis.getGlobalDataCollection(seasonID, branchID, sessionToken);
		validateNegativeTestResult(response, "get analytics global data");
		
		String input = "{\"featuresAttributesForAnalytics\": [\"1306a855-40d4-40c5-85a7-f85259caaaaa\"]}";	//input doesn't matter
		response = allApis.updateGlobalDataCollection(seasonID, branchID, input, sessionToken);
		validateNegativeTestResult(response, "update  analytics global data");
		
		response = allApis.removeFeatureFromAnalytics(featureID, branchID, sessionToken);
		validateNegativeTestResult(response, "remove feature from analytics");
		
		response = allApis.getAnalyticsQuota(seasonID, sessionToken);
		validateNegativeTestResult(response, "get analytics quota");
		
		response = allApis.setAnalyticsQuota(seasonID, sessionToken);
		validateNegativeTestResult(response, "set analytics quota");
		
	}
	
	
	@Test (dependsOnMethods="testNegativeCapabilities", description = "Set ANALYTICS capability")
	public void addAnalyticsCapabilities() throws Exception{		
		JSONArray capabilities = allApis.getCapabilitiesInProduct(productID, sessionToken);
		capabilities.add("ANALYTICS"); 
		String response = allApis.setCapabilitiesInProduct(productID, capabilities, sessionToken);
		Assert.assertFalse(response.contains("error"), "Can't set capabilities " + response);
		
	}
	
	@SuppressWarnings("unchecked")
	@Test (dependsOnMethods="addAnalyticsCapabilities", description = "test analytics api with capability")
	public void testPositiveCapabilities() throws Exception{
		
		results = allApis.runAllAnalytics(seasonID, branchID, featureID, sessionToken, false);
		
		if (results.size() > 0)
			Assert.fail("positive analytics capability test failed: " + results.toString());

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
