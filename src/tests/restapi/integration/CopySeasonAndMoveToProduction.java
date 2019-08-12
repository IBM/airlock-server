package tests.restapi.integration;

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


public class CopySeasonAndMoveToProduction {
	//bug #42 from https://docs.google.com/spreadsheets/d/1QROVN7adFg9psyDjZ-FOGhHQlLIjgfulpyYmGfqqrmA/edit?ts=57bee13c#gid=0
	
	/*
	 * Create a season with a feature and sub-feature
	 * Create another season (it will have these features)
	 * In the second season move the parent feature to production
	 */
	
	protected String seasonID1;
	protected String seasonID2;
	protected String productID;
	protected String filePath;
	protected FeaturesRestApi f;
	protected ProductsRestApi p;
	protected SeasonsRestApi s;
	private String sessionToken = "";
	private AirlockUtils baseUtils;
	
	
	@BeforeClass
	@Parameters({"url", "analyticsUrl", "translationsUrl", "configPath", "sessionToken", "userName", "userPassword", "appName", "productsToDeleteFile"})
	public void init(String url, String analyticsUrl, String translationsUrl, String configPath, String sToken, String userName, String userPassword, String appName, String productsToDeleteFile) throws Exception{
		filePath = configPath;
		baseUtils = new AirlockUtils(url, analyticsUrl, translationsUrl, configPath, sToken, userName, userPassword, appName, productsToDeleteFile);
		sessionToken = baseUtils.sessionToken;

		productID = baseUtils.createProduct();
		baseUtils.printProductToFile(productID);
		
		s = new SeasonsRestApi();
		f = new FeaturesRestApi();
		p = new ProductsRestApi();
		s.setURL(url);
		f.setURL(url);

	}
	
	
	@Test (description = "Add season and 2 features")
	public void addComponents() throws Exception{	
		String season = FileUtils.fileToString(filePath + "season1.txt", "UTF-8", false);
		seasonID1 = s.addSeason(productID, season, sessionToken);
		String feature1 = FileUtils.fileToString(filePath + "feature1.txt", "UTF-8", false);
		String featureID = f.addFeature(seasonID1, feature1, "ROOT", sessionToken);
		String feature2 = FileUtils.fileToString(filePath + "feature2.txt", "UTF-8", false);
		String response = f.addFeature(seasonID1, feature2, featureID, sessionToken);
		Assert.assertFalse(response.contains("error"), "Test should pass, but instead failed: " + response );
	}
	
	@Test (dependsOnMethods="addComponents", description = "Add second season")
	public void createNewSeason() throws IOException{
		String season = FileUtils.fileToString(filePath + "season2.txt", "UTF-8", false);
		seasonID2 = s.addSeason(productID, season, sessionToken);
		Assert.assertFalse(seasonID2.contains("error"), "Test should pass, but instead failed: " + seasonID2 );
	}
	
	@Test (dependsOnMethods="createNewSeason",  description = "Move a feature from the second season to production, validate and move back to development")
	public void moveFeatureToProduction() throws JSONException, IOException{
		JSONArray features = f.getFeaturesBySeason(seasonID2, sessionToken);
		JSONObject parent = new JSONObject(features.get(0));
		String uniqueId = parent.getString("uniqueId");
		parent.put("stage", "PRODUCTION");
		String response = f.updateFeature(seasonID2, uniqueId, parent.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Test should pass, but instead failed: " + response );
		
		String updatedFeature = f.getFeature(uniqueId, sessionToken);
		JSONObject updatedJson = new JSONObject(updatedFeature);
		Assert.assertTrue(updatedJson.getString("stage").equals("PRODUCTION"), "Failed to move the feature to production");
		
		updatedJson.put("stage", "DEVELOPMENT");
		response = f.updateFeature(seasonID2, uniqueId, updatedJson.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Test should pass, but instead failed: " + response );
	}
	
	
	@AfterTest 
	private void reset(){
		baseUtils.reset(productID, sessionToken);
	}
}
