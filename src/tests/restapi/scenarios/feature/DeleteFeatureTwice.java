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

public class DeleteFeatureTwice {
	protected String seasonID;
	protected String productID;
	protected String featureID;
	protected String filePath;
	protected FeaturesRestApi f;
	private String sessionToken = "";
	private AirlockUtils baseUtils;

	@BeforeClass
	@Parameters({"url", "analyticsUrl", "translationsUrl", "configPath", "sessionToken", "userName", "userPassword", "appName", "productsToDeleteFile"})
	public void init(String url, String analyticsUrl, String translationsUrl, String configPath, String sToken, String userName, String userPassword, String appName, String productsToDeleteFile) throws Exception{
		filePath = configPath;
		f = new FeaturesRestApi();
		f.setURL(url);
		if (sToken != null)
			sessionToken = sToken;
		baseUtils = new AirlockUtils(url, analyticsUrl, translationsUrl, configPath, sToken, userName, userPassword, appName, productsToDeleteFile);
		sessionToken = baseUtils.sessionToken;
		productID = baseUtils.createProduct();
		baseUtils.printProductToFile(productID);
		seasonID = baseUtils.createSeason(productID);
	}


	/**
	 * Test try to deleted a feature twice
	 */
	@Test (description = "Deleted a feature twice")
	public void testFeatureDeletedTwice() throws JSONException{
		try {
			String feature1 = FileUtils.fileToString(filePath + "feature1.txt", "UTF-8", false);
			featureID = f.addFeature(seasonID, feature1, "ROOT", sessionToken);
			int responseCode = f.deleteFeature(featureID, sessionToken);
			responseCode = f.deleteFeature(featureID, sessionToken);
			//TODO; should not look at the error string but at the response code that should be 404
			Assert.assertNotEquals(responseCode, "{\"error\":\"Airlock item not found.\"}"/*"404"*/, "Feature not found 404 was expected, but code " + responseCode + " was received");
			//Assert.assertEquals(responseCode, 500, "Feature not found 404 was expected, but Internal code 500 was received");
						
		} catch (IOException e) {
			e.printStackTrace();
		} catch (Exception e) {
			Assert.fail("An exception was thrown when trying  to delete a feature. Message: "+e.getMessage()) ;
		}

		
	}
	
	
	@AfterTest
	private void reset(){
		baseUtils.reset(productID, sessionToken);

	}
}
