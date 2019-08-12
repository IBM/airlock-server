package tests.restapi.scenarios.feature;

import java.io.IOException;




import org.apache.wink.json4j.JSONException;
import org.testng.Assert;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import tests.com.ibm.qautils.FileUtils;
import tests.restapi.AirlockUtils;
import tests.restapi.FeaturesRestApi;
import tests.restapi.ProductsRestApi;


public class GetDeletedFeature {
	protected String seasonID;
	protected String productID;
	protected String featureID;
	protected String filePath;
	protected FeaturesRestApi f;
	protected ProductsRestApi p;
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
	 * Test try to get deleted feature
	 */
	@Test (description = "get deleted feature")
	public void testGetDeletedFeature() throws JSONException{
		try {
			String feature1 = FileUtils.fileToString(filePath, "UTF-8", false);
			featureID = f.addFeature(seasonID, feature1, "ROOT", sessionToken);
			f.deleteFeature(featureID, sessionToken);
			String response = f.getFeature(featureID, sessionToken);
			//TODO; should not look at the error string but at the response code that should be 404
			Assert.assertTrue(response.equals("{\"error\":\"Airlock item not found.\"}"/*"404"*/), "Did not delete feature");			
		} catch (IOException e) {
			e.printStackTrace();
		} catch (Exception e) {
			Assert.fail("An exception was thrown when trying  to delete a non-existing feature. Message: "+e.getMessage()) ;
		}
			
	}
	
	@AfterTest
	private void reset(){
		baseUtils.reset(productID, sessionToken);
	}
}
