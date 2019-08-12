package tests.restapi.analytics;

import java.io.IOException;


import org.apache.wink.json4j.JSONException;
import org.apache.wink.json4j.JSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import tests.restapi.AirlockUtils;
import tests.restapi.AnalyticsRestApi;
import tests.restapi.BranchesRestApi;
import tests.restapi.ProductsRestApi;

public class CreateAndValidateAnalyticsFields {
	protected String seasonID;
	protected String branchID;
	protected String productID;
	protected String filePath;
	protected ProductsRestApi p;
	protected AnalyticsRestApi an;
	private String sessionToken = "";
	protected AirlockUtils baseUtils;

	@BeforeClass
	@Parameters({"url", "analyticsUrl", "translationsUrl", "configPath", "sessionToken", "userName", "userPassword", "appName", "productsToDeleteFile", "branchType"})
	public void init(String url, String analyticsUrl, String translationsUrl, String configPath, String sToken, String userName, String userPassword, String appName, String productsToDeleteFile, String branchType) throws IOException{
		filePath = configPath;
		p = new ProductsRestApi();
		p.setURL(url);
		an = new AnalyticsRestApi();
		an.setURL(analyticsUrl);
		
		baseUtils = new AirlockUtils(url, analyticsUrl, translationsUrl, configPath, sToken, userName, userPassword, appName, productsToDeleteFile);
		sessionToken = baseUtils.sessionToken;
		productID = baseUtils.createProduct();
		baseUtils.printProductToFile(productID);
		seasonID = baseUtils.createSeason(productID);
		try {
			if(branchType.equals("Master")) {
				branchID = BranchesRestApi.MASTER;
			}
			else if(branchType.equals("StandAlone")) {
				branchID = baseUtils.addBranchFromBranch("branch1",BranchesRestApi.MASTER,seasonID);
			}
			else if(branchType.equals("DevExp")) {
				branchID = baseUtils.createBranchInExperiment(analyticsUrl);
			}
			else if(branchType.equals("ProdExp")) {
				branchID = baseUtils.createBranchInProdExperiment(analyticsUrl).getString("brId");
			}
			else{
				branchID = null;
			}
		}catch (Exception e){
			branchID = null;
		}
	}

	@Test (description="Check get globalDataCollection response")
	public void getGlobalDataCollection(){
		String response = an.getGlobalDataCollection(seasonID, branchID, "BASIC", sessionToken);
		Assert.assertFalse(response.contains("error"), "Incorrect globalDataCollection response");
	}

	@Test (dependsOnMethods="getGlobalDataCollection", description="Set rule string")
	public void changeRuleString() throws JSONException{
		String response = an.getGlobalDataCollection(seasonID, branchID, "BASIC", sessionToken);
		JSONObject json = new JSONObject(response);
		JSONObject newRuleString = json.getJSONObject("rule");
		newRuleString.put("ruleString", "false");
		json.put("rule", newRuleString);
		response = an.updateGlobalDataCollection(seasonID, branchID, json.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Rule string was updated");
	}

	@Test (dependsOnMethods="changeRuleString", description="Set rolloutPercentage")
	public void changeRolloutPercentage() throws JSONException{
		String response = an.getGlobalDataCollection(seasonID, branchID, "BASIC", sessionToken);
		JSONObject json = new JSONObject(response);
		json.put("rolloutPercentage", 50);
		response = an.updateGlobalDataCollection(seasonID, branchID, json.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "GlobalDataCollection was not updated");
		response = an.getGlobalDataCollection(seasonID, branchID, "BASIC", sessionToken);
		JSONObject newJson = new JSONObject(response);
		if(branchID.equals("MASTER")) {
			Assert.assertTrue(newJson.getInt("rolloutPercentage") == 50, "rolloutPercentage was not updated");
		}
		//Assert.assertNotEquals(newJson.getString("rolloutPercentageBitmap"), json.getString("rolloutPercentageBitmap"), "rolloutPercentageBitmap was not recalculated");
	}
	
	/*
	@Test (dependsOnMethods="changeRolloutPercentage", description="Set rolloutPercentageBitmap")
	public void changeRolloutPercentageBitmap() throws JSONException{
		String response = an.getGlobalDataCollection(seasonID, branchID, "BASIC", sessionToken);
		JSONObject json = new JSONObject(response);
		json.put("rolloutPercentageBitmap", "ACAABgiIAQAAQIAEAA==");
		response = an.updateGlobalDataCollection(seasonID, branchID, json.toString(), sessionToken);
		response = an.getGlobalDataCollection(seasonID, branchID, "BASIC", sessionToken);
		JSONObject newJson = new JSONObject(response);
		Assert.assertFalse(newJson.getString("rolloutPercentageBitmap").equals("ACAABgiIAQAAQIAEAA=="), "rolloutPercentageBitmap was updated");
	}
	*/

	@Test (dependsOnMethods="changeRolloutPercentage", description="Remove inputFieldsToAnalytics")
	public void removeInputFieldsToAnalytics() throws JSONException{
		String response = an.getGlobalDataCollection(seasonID, branchID, "BASIC", sessionToken);
		JSONObject analytics = new JSONObject(response);
		analytics.getJSONObject("analyticsDataCollection").remove("inputFieldsForAnalytics");

		response = an.updateGlobalDataCollection(seasonID, branchID, analytics.toString(), sessionToken);
		Assert.assertTrue(response.contains("error"), "Analytics created without inputFieldsToAnalytics field");
	}

	@Test (dependsOnMethods="removeInputFieldsToAnalytics", description="Remove featuresAndConfigurationsForAnalytics")
	public void removeFeaturesOnOff() throws JSONException{
		String response = an.getGlobalDataCollection(seasonID, branchID, "BASIC", sessionToken);
		JSONObject analytics = new JSONObject(response);
		analytics.getJSONObject("analyticsDataCollection").remove("featuresAndConfigurationsForAnalytics");

		response = an.updateGlobalDataCollection(seasonID, branchID, analytics.toString(), sessionToken);
		Assert.assertTrue(response.contains("error"), "Analytics created without featuresAndConfigurationsForAnalytics field");
	}

	@Test (dependsOnMethods="removeFeaturesOnOff", description="Remove featuresAttributesToAnalytics")
	public void removeFeaturesAttributesToAnalytics() throws JSONException{
		String response = an.getGlobalDataCollection(seasonID, branchID, "BASIC", sessionToken);
		JSONObject analytics = new JSONObject(response);
		analytics.getJSONObject("analyticsDataCollection").remove("featuresAttributesForAnalytics");

		response = an.updateGlobalDataCollection(seasonID, branchID, analytics.toString(), sessionToken);
		Assert.assertTrue(response.contains("error"), "Analytics created without featuresAttributesToAnalytics field");
	}

	@AfterTest
	private void reset(){
		baseUtils.reset(productID, sessionToken);
	}
}
