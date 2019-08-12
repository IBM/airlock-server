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

public class SeasonVsConfigurationMinAppVersionNumber {
	protected String seasonID;
	protected String season;
	protected String productID;
	protected String featureID;
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
		String feature = FileUtils.fileToString(config + "feature1.txt", "UTF-8", false);
		featureID = f.addFeature(seasonID, feature, "ROOT", sessionToken);
	}
	

	
	//second season must be created to assign maxVersion to the first season
	@Test (description ="second season must be created to assign maxVersion to the first season") 
	public void createSecondSeason() throws JSONException, IOException{
		JSONObject json = new JSONObject(season);
		json.put("minVersion", "2.0");
		s.addSeason(productID, json.toString(), sessionToken);
	}
	
	//configuration rule minAppVersion can't be greater than season's maxVersion
	//the first season's maxVersion is 2.0
	@Test (dependsOnMethods = "createSecondSeason", description = "configuration rule minAppVersion can't be greater than season's maxVersion")
	public void addFeatureWithIllegalMinAppVer() throws Exception{
		String cofiguration = FileUtils.fileToString(config + "configuration_rule1.txt", "UTF-8", false);
		JSONObject json = new JSONObject(cofiguration);
		json.put("minAppVersion", "3.5");
		String response = f.addFeature(seasonID, json.toString(), featureID, sessionToken);
		Assert.assertTrue(response.contains("error"), "Test should fail, but instead passed: " + response );
	}
	
	//configuration rule minAppVersion must be less than season's maxVersion
	@Test (dependsOnMethods = "addFeatureWithIllegalMinAppVer", description = "configuration rule minAppVersion must be less than season's maxVersion")
	public void addFeatureWithLegalMinAppVer() throws Exception{
		String cofiguration = FileUtils.fileToString(config + "configuration_rule1.txt", "UTF-8", false);
		JSONObject json = new JSONObject(cofiguration);
		json.put("minAppVersion", "1.9a");
		String response = f.addFeature(seasonID, json.toString(), featureID, sessionToken);
		Assert.assertFalse(response.contains("error"), "Test should pass, but instead failed: " + response );
	}
	
	
	@AfterTest 
	public void reset(){
		baseUtils.reset(productID, sessionToken);
	}
	
	
}
