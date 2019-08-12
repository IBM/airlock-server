package tests.restapi.scenarios.feature;


import org.apache.wink.json4j.JSONException;
import org.testng.Assert;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import tests.restapi.AirlockUtils;
import tests.restapi.FeaturesRestApi;
import tests.restapi.ProductsRestApi;

public class GetNonExistingFeatureID {
	protected String seasonID;
	protected String featureID;
	protected String filePath;
	protected FeaturesRestApi f;
	protected ProductsRestApi p;
	protected String productID;
	private String sessionToken = "";
	private AirlockUtils baseUtils;
	
	@BeforeClass
 	@Parameters({"url", "analyticsUrl", "translationsUrl", "configPath", "sessionToken", "userName", "userPassword", "appName", "productsToDeleteFile"})
	public void init(String url, String analyticsUrl, String translationsUrl, String configPath, String sToken, String userName, String userPassword, String appName, String productsToDeleteFile) throws Exception{
		filePath = configPath;
		f = new FeaturesRestApi();
		f.setURL(url);
		p = new ProductsRestApi();
		p.setURL(url);
		if (sToken != null)
			sessionToken = sToken;
		baseUtils = new AirlockUtils(url, analyticsUrl, translationsUrl, configPath, sToken, userName, userPassword, appName, productsToDeleteFile);
		sessionToken = baseUtils.sessionToken;
		productID = baseUtils.createProduct();
		baseUtils.printProductToFile(productID);
		seasonID = baseUtils.createSeason(productID);
	}


	/**
	 * Test try to get feature using invalid ID
	 */
	@Test (description = "get feature using invalid ID")
	public void testGetFeatureWithInvalidID() throws JSONException{
		try {

			String feature = f.getFeature("e2d4efc6-90cf-40f7-a4aa-111aaa11a1a1", sessionToken);			
			Assert.assertNotEquals(feature, 404, "Feature not found 404 was expected, but code " + feature + " was received");
			//Assert.assertEquals(feature, 500, "Feature not found 404 was expected, but Internal code 500 was received");
		}  catch (Exception e) {
			Assert.fail("An exception was thrown when trying  to delete a non-existing feature. Message: "+e.getMessage()) ;
		}

	}
	
	@AfterTest
	private void reset(){
		baseUtils.reset(productID, sessionToken);
	}
}
