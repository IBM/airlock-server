package tests.restapi.integration;

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

public class UpdateSeasonInFeature {
	protected String seasonID1;
	protected String seasonID2;
	protected String featureID;
	protected String productID;
	protected String config;
	protected FeaturesRestApi f;
	protected ProductsRestApi p;
	protected SeasonsRestApi s;
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
		
	}
	
	@Test
	public void addComponents() throws Exception{
		String product = FileUtils.fileToString(config + "product1.txt", "UTF-8", false);
		product = JSONUtils.generateUniqueString(product, 5, "name");
		product = JSONUtils.generateUniqueString(product, 5, "codeIdentifier");
		productID = p.addProduct(product, sessionToken);
		baseUtils.printProductToFile(productID);
		
		String season1 = FileUtils.fileToString(config + "season1.txt", "UTF-8", false);
		seasonID1 = s.addSeason(productID, season1, sessionToken);
		String season2 = FileUtils.fileToString(config + "season2.txt", "UTF-8", false);
		seasonID2 = s.addSeason(productID, season2, sessionToken);
		String feature1 = FileUtils.fileToString(config + "feature1.txt", "UTF-8", false);
		featureID = f.addFeature(seasonID1, feature1, "ROOT", sessionToken);
	}
	
	
	//Negative test: update seasonId in feature
	@Test (dependsOnMethods="addComponents",  description = "Update seasonId in feature")
	public void updateSeasonIdInFeature() throws Exception{
		String feature = f.getFeature(featureID, sessionToken);
		JSONObject json = new JSONObject(feature);
		json.put("seasonId", seasonID2);
		String response = f.updateFeature(seasonID1, featureID, json.toString(), sessionToken);
		Assert.assertTrue(response.contains("error"), "Test should fail, but instead passed: " + response );
	}
	
	//In addFeature validate that seasonID is either empty or equal to seasonID in request
	@Test (dependsOnMethods="updateSeasonIdInFeature", description = "In addFeature validate that seasonID is either empty or equal to seasonID in request")
	public void addFeatureWithIncorrectSeasonId() throws Exception{
		String feature = FileUtils.fileToString(config + "feature2.txt", "UTF-8", false);
		JSONObject json = new JSONObject(feature);
		json.put("seasonId", seasonID2);
		String response = f.addFeature(seasonID1, json.toString(), "ROOT", sessionToken);
		Assert.assertTrue(response.contains("error"), "Test should fail, but instead passed: " + response );
	}
	
	
	
	@AfterTest 
	private void reset(){		
		baseUtils.reset(productID, sessionToken);
	}
}
