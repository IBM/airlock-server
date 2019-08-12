package tests.restapi.integration;

import java.io.IOException;


import org.apache.wink.json4j.JSONException;
import org.apache.wink.json4j.JSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeClass;

import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import tests.com.ibm.qautils.FileUtils;
import tests.com.ibm.qautils.JSONUtils;
import tests.restapi.AirlockUtils;
import tests.restapi.FeaturesRestApi;
import tests.restapi.ProductsRestApi;
import tests.restapi.SeasonsRestApi;

public class SeasonVsFeatureMinAppVersionNumber {
	protected String seasonID;
	protected String season;
	protected String productID;
	protected String config;
	protected ProductsRestApi p;
	protected SeasonsRestApi s;
	protected FeaturesRestApi f;
	protected AirlockUtils baseUtils;
	private String sessionToken = "";
	
	@BeforeClass
	@Parameters({"url", "analyticsUrl", "translationsUrl", "configPath", "sessionToken", "userName", "userPassword", "appName", "productsToDeleteFile"})
	public void init(String url, String analyticsUrl, String translationsUrl, String configPath, String sToken, String userName, String userPassword, String appName, String productsToDeleteFile) throws Exception{
		config = configPath;
		p = new ProductsRestApi();
		s = new SeasonsRestApi();
		f = new FeaturesRestApi();
		p.setURL(url);
		s.setURL(url);
		f.setURL(url);
		baseUtils = new AirlockUtils(url, analyticsUrl, translationsUrl, configPath, sToken, userName, userPassword, appName, productsToDeleteFile);
		sessionToken = baseUtils.sessionToken;
		String product = FileUtils.fileToString(config + "product1.txt", "UTF-8", false);
		product = JSONUtils.generateUniqueString(product, 5, "name");
		product = JSONUtils.generateUniqueString(product, 5, "codeIdentifier");
		productID = p.addProduct(product, sessionToken);
		baseUtils.printProductToFile(productID);
		
		season = FileUtils.fileToString(config + "season1.txt", "UTF-8", false);
		seasonID = s.addSeason(productID, season, sessionToken);
	}
	
	@Test(description ="Add maxVersion to the last season")
	public void updateMaxVersion() throws Exception{
		String seasonJson = s.getSeason(productID, seasonID, sessionToken);
		JSONObject json = new JSONObject(seasonJson);
		json.put("maxVersion", "2.0");
		 String response = s.updateSeason(seasonID, json.toString(), sessionToken);
		 Assert.assertTrue(response.contains("error"), "Test should fail, but instead passed: " + response );
	}
	
	//second season must be created to assign maxVersion to the first season
	@Test (dependsOnMethods = "updateMaxVersion", description ="second season must be created to assign maxVersion to the first season") 
	public void createSecondSeason() throws JSONException, IOException{
		JSONObject json = new JSONObject(season);
		json.put("minVersion", "2.0");
		String response = s.addSeason(productID, json.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Test should pass, but instead failed: " + response );
	}
	
	//feature minAppVersion can't be greater than season's maxVersion
	//the first season's maxVersion is 2.0
	@Test (dependsOnMethods = "createSecondSeason", description = "feature minAppVersion can't be greater than season's maxVersion")
	public void addFeatureWithIllegalMinAppVer() throws Exception{
		String feature = FileUtils.fileToString(config + "feature1.txt", "UTF-8", false);
		JSONObject json = new JSONObject(feature);
		json.put("minAppVersion", "3.5");
		String response = f.addFeature(seasonID, json.toString(), "ROOT", sessionToken);
		Assert.assertTrue(response.contains("error"), "Test should fail, but instead passed: " + response );
	}
	
	//feature minAppVersion must be less than season's maxVersion
	@Test (dependsOnMethods = "addFeatureWithIllegalMinAppVer", description = "feature minAppVersion must be less than season's maxVersion")
	public void addFeatureWithLlegalMinAppVer() throws Exception{
		String feature = FileUtils.fileToString(config + "feature1.txt", "UTF-8", false);
		JSONObject json = new JSONObject(feature);
		json.put("minAppVersion", "1.9a");
		String response = f.addFeature(seasonID, json.toString(), "ROOT", sessionToken);
		Assert.assertFalse(response.contains("error"), "Test should pass, but instead failed: " + response );
	}
	
	
	@AfterTest ()
	public void reset(){
		baseUtils.reset(productID, sessionToken);
	}
	
	
}
