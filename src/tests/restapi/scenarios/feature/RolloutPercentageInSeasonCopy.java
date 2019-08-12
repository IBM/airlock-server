package tests.restapi.scenarios.feature;

import java.io.IOException;




import org.apache.wink.json4j.JSONArray;
import org.apache.wink.json4j.JSONException;
import org.apache.wink.json4j.JSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeClass;

import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import tests.com.ibm.qautils.FileUtils;
import tests.restapi.AirlockUtils;
import tests.restapi.FeaturesRestApi;
import tests.restapi.ProductsRestApi;
import tests.restapi.SeasonsRestApi;


public class RolloutPercentageInSeasonCopy {
	protected String seasonID;
	protected String featureID;
	protected String featureID2;
	protected String configID;
	protected String productID;
	private String seasonID2;
	protected String filePath;
	protected JSONObject json;
	protected FeaturesRestApi f;
	protected ProductsRestApi p;
	protected SeasonsRestApi s;
	private String sessionToken = "";
	protected AirlockUtils baseUtils;

	
	@BeforeClass
 	@Parameters({"url", "analyticsUrl", "translationsUrl", "configPath", "sessionToken", "userName", "userPassword", "appName", "productsToDeleteFile"})
	public void init(String url, String analyticsUrl, String translationsUrl, String configPath, String sToken, String userName, String userPassword, String appName, String productsToDeleteFile) throws Exception{
		filePath = configPath;
		f = new FeaturesRestApi();
		f.setURL(url);
		p = new ProductsRestApi();
		p.setURL(url);
		s = new SeasonsRestApi();
		s.setURL(url);
		if (sToken != null)
			sessionToken = sToken;
		baseUtils = new AirlockUtils(url, analyticsUrl, translationsUrl, configPath, sToken, userName, userPassword, appName, productsToDeleteFile);
		sessionToken = baseUtils.sessionToken;
		productID = baseUtils.createProduct();
		baseUtils.printProductToFile(productID);
		seasonID = baseUtils.createSeason(productID);
	}
	
	
	@Test (description="Create feature, subfeature and configuration rule")
	public void createFeatures() throws JSONException, IOException, InterruptedException{

		String feature = FileUtils.fileToString(filePath + "feature1.txt", "UTF-8", false);
		JSONObject json = new JSONObject(feature);
		json.put("rolloutPercentage", 0.01);
		featureID = f.addFeature(seasonID, json.toString(), "ROOT", sessionToken);
		Assert.assertFalse(featureID.contains("error"), "Feature was not created " + featureID);
		
		String feature2 = FileUtils.fileToString(filePath + "feature2.txt", "UTF-8", false);
		JSONObject json2 = new JSONObject(feature2);
		json2.put("rolloutPercentage", 10);
		featureID2 = f.addFeature(seasonID, json2.toString(), featureID, sessionToken);
		Assert.assertFalse(featureID2.contains("error") || featureID2.contains("Invalid"), "Feature was not created " + featureID2);

		String configuration = FileUtils.fileToString(filePath + "configuration_rule1.txt", "UTF-8", false);
		JSONObject jsonCR = new JSONObject(configuration);
		jsonCR.put("rolloutPercentage", 10);
		configID = f.addFeature(seasonID, jsonCR.toString(), featureID, sessionToken);
		Assert.assertFalse(configID.contains("error") || configID.contains("Invalid"), "Configuration was not created " + featureID2);

	}
	
	@Test (dependsOnMethods="createFeatures", description="Copy season and copy feature1 to the root")
	public void duplicateSeason() throws JSONException, IOException, InterruptedException{
		
		String season = FileUtils.fileToString(filePath + "season2.txt", "UTF-8", false);
		seasonID2 = s.addSeason(productID, season, sessionToken);
		Assert.assertFalse(seasonID2.contains("error"), "Test should pass, but instead failed: " + seasonID2 );

	}
	
	@Test (dependsOnMethods="duplicateSeason", description="rolloutPercentage in new season")
	public void validateRolloutPercentage() throws JSONException, IOException, InterruptedException{
		
		JSONArray features = f.getFeaturesBySeason(seasonID2, sessionToken);
		JSONObject feature1NewSeason = features.getJSONObject(0);
		JSONObject feature2NewSeason = feature1NewSeason.getJSONArray("features").getJSONObject(0);
		JSONObject configurationNewSeason = feature1NewSeason.getJSONArray("configurationRules").getJSONObject(0);
		
		String feature1 = f.getFeature(featureID, sessionToken);
		JSONObject feature1OldSeason = new JSONObject(feature1);
		String feature2 = f.getFeature(featureID2, sessionToken);
		JSONObject feature2OldSeason = new JSONObject(feature2);
		String configuration = f.getFeature(configID, sessionToken);
		JSONObject configurationOldSeason = new JSONObject(configuration);
				
		Assert.assertTrue(feature1NewSeason.getDouble("rolloutPercentage")==feature1OldSeason.getDouble("rolloutPercentage"), "rolloutPercentage for the first feature was changed");
		Assert.assertTrue(feature2NewSeason.getDouble("rolloutPercentage")==feature2OldSeason.getDouble("rolloutPercentage"), "rolloutPercentage for the second feature was changed");
		Assert.assertTrue(configurationNewSeason.getDouble("rolloutPercentage")==configurationOldSeason.getDouble("rolloutPercentage"), "rolloutPercentage for configuration rule was changed");
		
		/*
		Assert.assertTrue(feature1NewSeason.getString("rolloutPercentageBitmap").equals(feature1OldSeason.getString("rolloutPercentageBitmap")), "rolloutPercentageBitmap for the first feature was changed");
		Assert.assertTrue(feature2NewSeason.getString("rolloutPercentageBitmap").equals(feature2OldSeason.getString("rolloutPercentageBitmap")), "rolloutPercentageBitmap for the second feature was changed");
		Assert.assertTrue(configurationNewSeason.getString("rolloutPercentageBitmap").equals(configurationOldSeason.getString("rolloutPercentageBitmap")), "rolloutPercentageBitmap for configuration rule was changed");
		*/
	}
	



	@AfterTest
	private void reset(){
		baseUtils.reset(productID, sessionToken);
	}
}
