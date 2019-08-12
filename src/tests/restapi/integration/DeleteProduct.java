package tests.restapi.integration;


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

public class DeleteProduct {
	protected String seasonID;
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
	
	@Test (description = "Add product, season and feature")
	public void addComponents() throws Exception{
		String product = FileUtils.fileToString(config + "product1.txt", "UTF-8", false);
		product = JSONUtils.generateUniqueString(product, 5, "name");
		product = JSONUtils.generateUniqueString(product, 5, "codeIdentifier");
		productID = p.addProduct(product, sessionToken);
		baseUtils.printProductToFile(productID);
		String season = FileUtils.fileToString(config + "season1.txt", "UTF-8", false);
		seasonID = s.addSeason(productID, season, sessionToken);
		String feature1 = FileUtils.fileToString(config + "feature1.txt", "UTF-8", false);
		featureID = f.addFeature(seasonID, feature1, "ROOT", sessionToken);
	}
	
	
	//Delete a product and validate that its season and features were deleted (if they are not in PRODUCTION stage - another test)
	@Test (dependsOnMethods="addComponents", description = "Delete a product and validate that its season and features were deleted")
	public void deleteProduct() throws Exception{
		int responseCode = p.deleteProduct(productID, sessionToken);
		Assert.assertEquals(responseCode, 200, "Failed to delete a product.");	
		String response = p.getProduct(productID, sessionToken);
		//TODO; should not look at the error string but at the response code that should be 404
		Assert.assertEquals(response, "{\"error\":\"Product not found.\"}"/*"404"*/, "Failed to delete a product.");		
		JSONArray seasonsInProduct = s.getSeasonsPerProduct(productID, sessionToken);
		Assert.assertEquals(seasonsInProduct.size(), 0, "Did not delete a season from deleted product");
		String feature = f.getFeature(featureID, sessionToken);
		//TODO; should not look at the error string but at the response code that should be 404
		Assert.assertEquals(feature, "{\"error\":\"Airlock item not found.\"}"/*"404"*/, "Did not delete a feature from deleted product");
	}	

}
