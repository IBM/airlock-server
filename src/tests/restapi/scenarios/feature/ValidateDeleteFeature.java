package tests.restapi.scenarios.feature;

import java.io.IOException;




import org.apache.wink.json4j.JSONArray;
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

public class ValidateDeleteFeature {
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
		baseUtils = new AirlockUtils(url, analyticsUrl, translationsUrl, configPath, sToken, userName, userPassword, appName, productsToDeleteFile);
		sessionToken = baseUtils.sessionToken;
		productID = baseUtils.createProduct();
		baseUtils.printProductToFile(productID);
		seasonID = baseUtils.createSeason(productID);
	}


	/**
	 * Delete the feature and validate that it has been deleted
	 */
	@Test (description = "Delete the feature and validate that it has been deleted")
	public void testFeatureDeleteAndValidate() throws JSONException{
		try {			
			String feature1 = FileUtils.fileToString(filePath + "feature1.txt", "UTF-8", false);
			String feature2 = FileUtils.fileToString(filePath + "feature2.txt", "UTF-8", false);	
			f.addFeature(seasonID, feature1, "ROOT", sessionToken);
			String featureID2 = f.addFeature(seasonID, feature2, "ROOT", sessionToken);
			
			Assert.assertEquals(getFeatures(seasonID), 2);
			f.deleteFeature(featureID2, sessionToken);
			Assert.assertEquals(getFeatures(seasonID), 1);

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}

 			
	}
	
	private int getFeatures(String seasonID){
		JSONArray features = f.getFeaturesBySeason(seasonID, sessionToken);
		return features.size();

	}
	@AfterTest
	private void reset(){
		baseUtils.reset(productID, sessionToken);
	}

}
