package tests.restapi.integration;


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

public class DeleteSeason {
	protected String seasonID;
	protected String featureID1;
	protected String featureID2;
	protected String productID;
	protected String config;
	protected FeaturesRestApi f;
	protected ProductsRestApi p;
	protected SeasonsRestApi s;
	protected AirlockUtils baseUtils;
	protected String m_url;
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
		String season = FileUtils.fileToString(config + "season1.txt", "UTF-8", false);
		seasonID = s.addSeason(productID, season, sessionToken);
		String feature1 = FileUtils.fileToString(config + "feature1.txt", "UTF-8", false);
		featureID1 = f.addFeature(seasonID, feature1, "ROOT", sessionToken);
		String feature2 = FileUtils.fileToString(config + "feature2.txt", "UTF-8", false);
		featureID2 = f.addFeature(seasonID, feature2, "ROOT", sessionToken);

	}
	
	//Delete a season and validate that its features were also deleted
	@Test (dependsOnMethods="addComponents", description="Delete a season and validate that its features were also deleted")
	public void deleteSeason() throws Exception{
		s.deleteSeason(seasonID, sessionToken);
		String feature1 = f.getFeature(featureID1, sessionToken);
		//TODO; should not look at the error string but at the response code that should be 404
		Assert.assertEquals(feature1, "{\"error\":\"Airlock item not found.\"}"/*"404"*/, "Did not delete a feature from deleted product");
		String feature2 = f.getFeature(featureID1, sessionToken);
		//TODO; should not look at the error string but at the response code that should be 404
		Assert.assertEquals(feature2, "{\"error\":\"Airlock item not found.\"}"/*"404"*/, "Did not delete a feature from deleted product");
				
	}
	
	@Test (dependsOnMethods="deleteSeason", description = "Validate that default and constants files were deleted")
	public void validateDeletedComponents() throws Exception{
		String constants = baseUtils.getConstants(seasonID, "Android");
		//TODO; should not look at the error string but at the response code that should be 404
		Assert.assertEquals(constants, "{\"error\":\"Season not found.\"}"/*"404"*/, "Did not delete android constants");

		constants = baseUtils.getConstants(seasonID, "iOS");
		//TODO; should not look at the error string but at the response code that should be 404
		Assert.assertEquals(constants, "{\"error\":\"Season not found.\"}"/*"404"*/, "Did not delete iOS constants");

		/*Integer constResponseCode = Integer.valueOf(constants);
		Assert.assertTrue((400 <= constResponseCode && constResponseCode <= 450), "Did not delete constants");*/
		String defaults = baseUtils.getDefaults(seasonID);
		//TODO; should not look at the error string but at the response code that should be 404
		Assert.assertEquals(defaults, "{\"error\":\"Season not found.\"}"/*"404"*/, "Did not delete defaults");

		//Integer defaultsResponseCode = Integer.valueOf(defaults);
		//Assert.assertTrue((400 <= defaultsResponseCode && defaultsResponseCode <= 450), "Did not delete defaults");

	}
	
	@AfterTest ()
	private void reset(){
		baseUtils.reset(productID, sessionToken);
	}
}
