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

public class FeatureWithInvalidID {
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
	 * Test try to get feature with non-existing ID
	 */
	@Test (description = "get feature with non-existing ID")
	public void testGetFeatureWithInvalidID() throws JSONException{
		try {

			String response = f.getFeature("12345", sessionToken);			
			//Assert.assertEquals(response, "400", "Code 400 for bad request, but " + response + " was received");
			//TODO: look for the error message and not at the error code
			Assert.assertEquals(response, "{\"error\":\"Illegal feature-id GUID: Invalid UUID string: 12345\"}", "Error message {\"error\":\"Illegal feature-id GUID:Invalid UUID string: 12345\"} expected but response '" + response + "' was received");
		}  catch (Exception e) {
			Assert.fail("An exception was thrown when trying  to get a non-existing feature. Message: "+e.getMessage()) ;
		}

	}
	
	@AfterTest
	private void reset(){
		baseUtils.reset(productID, sessionToken);
	}
}
