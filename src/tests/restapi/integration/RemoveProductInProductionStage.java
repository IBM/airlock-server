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

public class RemoveProductInProductionStage {
	protected String seasonID1;
	protected String featureID1;
	protected String featureID2;
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
	
	
	
	@Test (description = "Add product, season and features")
	public void addComponents() throws Exception{
		String product = FileUtils.fileToString(config + "product1.txt", "UTF-8", false);
		product = JSONUtils.generateUniqueString(product, 5, "name");
		product = JSONUtils.generateUniqueString(product, 5, "codeIdentifier");
		productID = p.addProduct(product, sessionToken);
		baseUtils.printProductToFile(productID);
		String season1 = FileUtils.fileToString(config + "season1.txt", "UTF-8", false);
		seasonID1 = s.addSeason(productID, season1, sessionToken);		
		String feature = FileUtils.fileToString(config + "feature_production.txt", "UTF-8", false);
		featureID1 = f.addFeature(seasonID1, feature, "ROOT", sessionToken);
		String feature1 = FileUtils.fileToString(config + "feature1.txt", "UTF-8", false);
		featureID2 = f.addFeature(seasonID1, feature1, "ROOT", sessionToken);


	}
	
	//Validate that if a feature is in PRODUCTION stage, its season and product can't be deleted
	
	@Test (dependsOnMethods="addComponents", description="If a feature is in PRODUCTION stage, its season and product can't be deleted")
	public void deleteProductInProduction() throws Exception{
		int responeCodeP = p.deleteProduct(productID, sessionToken);
		Assert.assertEquals(responeCodeP, 400, "Should not remove a product whose component is in PRODUCTION stage");
		
		int responseCodeS = s.deleteSeason(seasonID1, sessionToken);
		Assert.assertEquals(responseCodeS, 400, "Should not remove a season whose component is in PRODUCTION stage");		
		
		String feature1 = f.getFeature(featureID1, sessionToken);
		//TODO; should not look at the error string but at the response code that should be 404
		Assert.assertNotEquals(feature1, "{\"error\":\"Feature not found.\"}"/*"404"*/, "Removed feature in PRODUCTION stage");
		String feature2 = f.getFeature(featureID2, sessionToken);
		//TODO; should not look at the error string but at the response code that should be 404
		Assert.assertNotEquals(feature2, "{\"error\":\"Feature not found.\"}"/*"404"*/, "Removed feature in PRODUCTION stage");
	}	
	
	//Change the stage to DEVELOPMENT and validate that the product can be deleted
	@Test (dependsOnMethods="deleteProductInProduction", description="Change the stage to DEVELOPMENT and validate that the product can be deleted")
	public void moveFeatureToDevelopmentAndDelete() throws Exception{
		String feature1 = f.getFeature(featureID1, sessionToken);
		JSONObject json = new JSONObject(feature1);
		json.put("stage", "DEVELOPMENT");
		f.updateFeature(seasonID1, featureID1, json.toString(), sessionToken);
		int responeCodeP = p.deleteProduct(productID, sessionToken);
		Assert.assertEquals(responeCodeP, 200, "Failed to remove a product, although its components are not in PRODUCTION stage");
		
		int responseCode = s.deleteSeason(seasonID1, sessionToken);
		Assert.assertEquals(responseCode, 404, "Failed to remove a season");	
	
		feature1 = f.getFeature(featureID1, sessionToken);
		//TODO; should not look at the error string but at the response code that should be 404
		Assert.assertEquals(feature1, "{\"error\":\"Airlock item not found.\"}"/*"404"*/, "Failed to remove a feature");
		String feature2 = f.getFeature(featureID2, sessionToken);
		//TODO; should not look at the error string but at the response code that should be 404
		Assert.assertEquals(feature2, "{\"error\":\"Airlock item not found.\"}"/*"404"*/, "Failed to remove a feature");

	}
	
	@AfterTest ()
	private void reset(){
		baseUtils.reset(productID, sessionToken);
	}
}
