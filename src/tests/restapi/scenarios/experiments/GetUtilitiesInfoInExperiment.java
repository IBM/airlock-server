package tests.restapi.scenarios.experiments;

import java.io.IOException;

import java.util.Properties;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.wink.json4j.JSONException;
import org.apache.wink.json4j.JSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import tests.com.ibm.qautils.FileUtils;
import tests.restapi.AirlockUtils;
import tests.restapi.ExperimentsRestApi;
import tests.restapi.FeaturesRestApi;
import tests.restapi.ProductsRestApi;
import tests.restapi.SeasonsRestApi;
import tests.restapi.UtilitiesRestApi;


public class GetUtilitiesInfoInExperiment {
	protected String seasonID1;
	private String seasonID2;
	private String seasonID3;
	protected String featureID;
	protected String productID;
	protected String filePath;
	private String experimentID1;
	private String experimentID2;
	private String experimentID3;
	protected FeaturesRestApi f;
	protected ProductsRestApi p;
	protected UtilitiesRestApi u;
	private ExperimentsRestApi exp ;
	private SeasonsRestApi s;
	private String sessionToken = "";	
	protected AirlockUtils baseUtils;
	
	protected UtilitiesRestApi utilitiesApi;
	
	
	@BeforeClass
	@Parameters({"url", "analyticsUrl", "translationsUrl", "configPath", "sessionToken", "userName", "userPassword", "appName", "productsToDeleteFile"})
	public void init(String url, String analyticsUrl, String translationsUrl, String configPath, String sToken, String userName, String userPassword, String appName, String productsToDeleteFile) throws Exception{
		filePath = configPath;
		if (sToken != null)
			sessionToken = sToken;

		p = new ProductsRestApi();
		p.setURL(url);
		u = new UtilitiesRestApi();
		u.setURL(url);
		s = new SeasonsRestApi();
		s.setURL(url);
		exp = new ExperimentsRestApi();
		exp.setURL(analyticsUrl); 

		baseUtils = new AirlockUtils(url, analyticsUrl, translationsUrl, configPath, sToken, userName, userPassword, appName, productsToDeleteFile);
		sessionToken = baseUtils.sessionToken;
		productID = baseUtils.createProduct();
		baseUtils.printProductToFile(productID);

		utilitiesApi = new UtilitiesRestApi();
		utilitiesApi.setURL(url);
		
	}
	
	/*
	 * season1: 1.0-2.0
	 * season2: 2.0-3.0
	 * season3: 3.0-
	 * exp1: 0.5-2.5 (season1, season2)
	 * exp2: 1.5-4.5 (season1, season2, season3)
	 * exp3: 3.5-5.0 (season3)
	 */
	
	@Test(description = "Add seasons, utilities and experiments")
	public void addSeasons() throws JSONException, IOException{
		JSONObject season = new JSONObject();
		season.put("minVersion", "1.0");
		seasonID1 = s.addSeason(productID, season.toString(), sessionToken);
				
		season.put("minVersion", "2.0");
		seasonID2 = s.addSeason(productID, season.toString(), sessionToken);

		season.put("minVersion", "3.0");
		seasonID3 = s.addSeason(productID, season.toString(), sessionToken);

		String experiment = FileUtils.fileToString(filePath + "experiments/experiment1.txt", "UTF-8", false);
		JSONObject expJson = new JSONObject(experiment);
		expJson.put("name", "experiment."+RandomStringUtils.randomAlphabetic(5));
		expJson.put("minVersion", "0.5");
		expJson.put("maxVersion", "2.5");
		expJson.put("enabled", false);
		experimentID1 = exp.createExperiment(productID, expJson.toString(), sessionToken);

		expJson.put("name", "experiment."+RandomStringUtils.randomAlphabetic(5));
		expJson.put("minVersion", "1.5");
		expJson.put("maxVersion", "4.5");
		expJson.put("enabled", false);
		experimentID2 = exp.createExperiment(productID, expJson.toString(), sessionToken);
		
		expJson.put("name", "experiment."+RandomStringUtils.randomAlphabetic(5));
		expJson.put("minVersion", "3.5");
		expJson.put("maxVersion", "5.0");
		expJson.put("enabled", false);
		experimentID3 = exp.createExperiment(productID, expJson.toString(), sessionToken);
		
		//utility to season1:
		Properties utilProps1 = new Properties();
		utilProps1.setProperty("stage", "DEVELOPMENT");
		utilProps1.setProperty("name", "utilA");
		utilProps1.setProperty("utility", "function A(){return true;}");
		String response = u.addUtility(seasonID1, utilProps1, sessionToken);
		Assert.assertFalse(response.contains("error"), "Can't add utility A: " + response);
		
		//utility to season2:
		Properties utilProps2 = new Properties();
		utilProps2.setProperty("stage", "PRODUCTION");
		utilProps2.setProperty("name", "utilB");
		utilProps2.setProperty("utility", "function B(){return true;}");
		response = u.addUtility(seasonID1, utilProps2, sessionToken);
		Assert.assertFalse(response.contains("error"), "Can't add utility B: " + response);
		
		//utility to season3:
		Properties utilProps3 = new Properties();
		utilProps3.setProperty("stage", "DEVELOPMENT");
		utilProps3.setProperty("name", "utilC");
		utilProps3.setProperty("utility", "function C(){return true;}");
		response = u.addUtility(seasonID3, utilProps3, sessionToken);
		Assert.assertFalse(response.contains("error"), "Can't add utility C"  + response);

	}
	
	@Test(dependsOnMethods = "addSeasons",  description = "Get utilities info from experiments")
	public void getUtilitiesInfo() throws Exception{
		JSONObject utilitiesInfoJson = new JSONObject(exp.getUtilitiesInfo(experimentID1, "DEVELOPMENT", sessionToken));
		Assert.assertTrue(containsUtilityInfo(utilitiesInfoJson, "A"), "Function A was not found in experiment1");	
		Assert.assertTrue(containsUtilityInfo(utilitiesInfoJson, "B"), "Function B was not found in experiment1");
		utilitiesInfoJson = new JSONObject(exp.getUtilitiesInfo(experimentID1, "PRODUCTION", sessionToken));
		Assert.assertFalse(containsUtilityInfo(utilitiesInfoJson, "A"), "Function A was found in experiment1");
		Assert.assertTrue(containsUtilityInfo(utilitiesInfoJson, "B"), "Function B was not found in experiment1");
		
		utilitiesInfoJson = new JSONObject(exp.getUtilitiesInfo(experimentID2, "DEVELOPMENT", sessionToken));
		Assert.assertTrue(containsUtilityInfo(utilitiesInfoJson, "A"), "Function A was not found in experiment2");	
		Assert.assertTrue(containsUtilityInfo(utilitiesInfoJson, "B"), "Function B was not found in experiment2");
		utilitiesInfoJson = new JSONObject(exp.getUtilitiesInfo(experimentID2, "PRODUCTION", sessionToken));
		Assert.assertFalse(containsUtilityInfo(utilitiesInfoJson, "A"), "Function A was found in experiment2");
		Assert.assertTrue(containsUtilityInfo(utilitiesInfoJson, "B"), "Function B was not found in experiment2");

		utilitiesInfoJson = new JSONObject(exp.getUtilitiesInfo(experimentID3, "DEVELOPMENT", sessionToken));
		Assert.assertFalse(containsUtilityInfo(utilitiesInfoJson, "A"), "Function A was found in experiment3");	
		Assert.assertFalse(containsUtilityInfo(utilitiesInfoJson, "B"), "Function B was found in experiment3");	
		utilitiesInfoJson = new JSONObject(exp.getUtilitiesInfo(experimentID3, "DEVELOPMENT", sessionToken));
		Assert.assertTrue(containsUtilityInfo(utilitiesInfoJson, "C"), "Function C was not found in experiment3");


	}
	


	private boolean containsUtilityInfo(JSONObject utilitiesInfoJson, String functionName) throws JSONException {
		if (!utilitiesInfoJson.containsKey(functionName))
			return false;

		return true;
	}

	
	@AfterTest
	private void reset(){
		baseUtils.reset(productID, sessionToken);
	}
}
