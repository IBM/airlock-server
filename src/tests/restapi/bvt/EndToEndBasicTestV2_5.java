package tests.restapi.bvt;

import org.apache.wink.json4j.JSONArray;

import org.testng.Assert;
import org.testng.annotations.BeforeClass;

import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import tests.com.ibm.qautils.FileUtils;
import tests.com.ibm.qautils.JSONUtils;
import tests.restapi.AirlockUtils;
import tests.restapi.FeaturesRestApi;
import tests.restapi.ProductsRestApi;
import tests.restapi.SeasonsRestApi;

public class EndToEndBasicTestV2_5 {
	protected String seasonID;
	protected String featureID1;
	protected String featureID2;
	protected String productID;
	protected String config;
	protected FeaturesRestApi f;
	protected ProductsRestApi p;
	protected SeasonsRestApi s;
	protected AirlockUtils baseUtils;
	protected String sessionToken = "";

	@BeforeClass
	@Parameters({"url", "analyticsUrl", "translationsUrl", "configPath", "sessionToken", "userName", "userPassword", "appName", "productsToDeleteFile"})
	public void init(String url, String analyticsUrl, String translationsUrl, String configPath, String sToken, String userName, String userPassword, String appName, String productsToDeleteFile) throws Exception{
		baseUtils = new AirlockUtils(url, analyticsUrl, translationsUrl, configPath, sToken, userName, userPassword, appName, productsToDeleteFile);
		sessionToken = baseUtils.sessionToken;

		config = configPath;
		p = new ProductsRestApi();
		s = new SeasonsRestApi();
		f = new FeaturesRestApi();
		p.setURL(url);
		s.setURL(url);
		f.setURL(url); 
	}
	
	@Test (description = "Add product, season and 2 features. Validate that all components were added")
	public void addComponents() throws Exception{
		String product = FileUtils.fileToString(config + "product1.txt", "UTF-8", false);
		product = JSONUtils.generateUniqueString(product, 5, "codeIdentifier");
		product = JSONUtils.generateUniqueString(product, 8, "name");
		productID = p.addProduct(product, sessionToken);
		baseUtils.printProductToFile(productID);
		String season = FileUtils.fileToString(config + "season1.txt", "UTF-8", false);
		seasonID = s.addSeason(productID, season, sessionToken);
		String feature1 = FileUtils.fileToString(config + "feature1.txt", "UTF-8", false);
		String feature2 = FileUtils.fileToString(config + "feature2.txt", "UTF-8", false);
		featureID1 = f.addFeature(seasonID, feature1, "ROOT", sessionToken);
		featureID2 = f.addFeature(seasonID, feature2, "ROOT", sessionToken);
		JSONArray seasonsInProduct = s.getSeasonsPerProduct(productID, sessionToken);
		Assert.assertEquals(seasonsInProduct.size(), 1, "The number of seasons is incorrect. " + seasonsInProduct.size());
		JSONArray features = f.getFeaturesBySeason(seasonID, sessionToken);
		Assert.assertEquals(features.size(), 2, "The number of features in addComponents is incorrect. " + features.size());
	}
	
	@Test (dependsOnMethods="addComponents", description = "Remove product, season and 2 features. Validate that all components were removed")
	public void deleteComponents() throws Exception{
		int responseCode = f.deleteFeature(featureID1, sessionToken);
		Assert.assertEquals(responseCode, 200, "Failed to delete a feature.");
		
		JSONArray features = f.getFeaturesBySeason(seasonID, sessionToken);
		Assert.assertEquals(features.size(), 1, "The number of features in deleteComponents is incorrect. " + features.size());
		responseCode = s.deleteSeason(seasonID, sessionToken);
		Assert.assertEquals(responseCode, 200, "Failed to delete a season.");
		JSONArray seasonsInProduct = s.getSeasonsPerProduct(productID, sessionToken);
		Assert.assertEquals(seasonsInProduct.size(), 0, "The number of seasons in deleteComponents is incorrect. " + seasonsInProduct.size());
		responseCode = p.deleteProduct(productID, sessionToken);
		Assert.assertEquals(responseCode, 200, "Failed to delete a product.");		
		String response = p.getProduct(productID, sessionToken);
		//TODO; should not look at the error string but at the response code that should be 404
		Assert.assertEquals(response, "{\"error\":\"Product not found.\"}"/*"404"*/, "Failed to delete a product.");
		
	}


}
