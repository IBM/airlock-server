package tests.restapi.validations.feature;

import java.io.IOException;










import org.apache.wink.json4j.JSONException;
import org.apache.wink.json4j.JSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeClass;

import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import tests.com.ibm.qautils.FileUtils;

import tests.restapi.AirlockUtils;
import tests.restapi.FeaturesRestApi;
import tests.restapi.ProductsRestApi;

public class FeatureValidateInteger {
	protected String seasonID;
	protected String featureID;
	protected String filePath;
	protected String m_url;
	protected JSONObject json;
	protected ProductsRestApi p;
	protected String productID;
	protected String feature;
	protected FeaturesRestApi f;
	private AirlockUtils baseUtils;
	private String sessionToken = "";
	
	@BeforeClass
	@Parameters({"url", "analyticsUrl", "translationsUrl", "configPath", "sessionToken", "userName", "userPassword", "appName", "productsToDeleteFile"})
	public void init(String url, String analyticsUrl, String translationsUrl, String configPath, String sToken, String userName, String userPassword, String appName, String productsToDeleteFile) throws Exception{
		filePath = configPath;
		p = new ProductsRestApi();
		p.setURL(url);
		baseUtils = new AirlockUtils(url, analyticsUrl, translationsUrl, configPath, sToken, userName, userPassword, appName, productsToDeleteFile);
		sessionToken = baseUtils.sessionToken;
		productID = baseUtils.createProduct();
		baseUtils.printProductToFile(productID);
		seasonID = baseUtils.createSeason(productID);
		
		m_url = url;
		feature = FileUtils.fileToString(filePath + "feature1.txt", "UTF-8", false);
		f = new FeaturesRestApi();
		f.setURL(url);
 		
	}
	
	@Test(description = "rolloutPercentage is more than 100%")
	public void ValidateRolloutPercentage1() throws JSONException, IOException{
		json = new JSONObject(feature);
		json.put("rolloutPercentage", 300);
		addFeature(json.toString());
	}
	
	@Test(description = "rolloutPercentage set to a negative number")
	public void ValidateRolloutPercentage2() throws JSONException, IOException{
		json = new JSONObject(feature);
		json.put("rolloutPercentage", -1);
		addFeature(json.toString());
	}

	
	@Test(description = "rolloutPercentage set to a characters string")
	public void ValidateRolloutPercentage3() throws JSONException, IOException{
		json = new JSONObject(feature);
		json.put("rolloutPercentage", "test");
		addFeature(json.toString());
	}
	
	
	@Test (description = "rolloutPercentage set to a float number")
	public void ValidateRolloutPercentage4() throws JSONException, IOException{
		json = new JSONObject(feature);
		json.put("rolloutPercentage", 20.5);
		String response = f.addFeature(seasonID,json.toString(), "ROOT", sessionToken);
		Assert.assertFalse(response.contains("error"), "Test should pass, but instead failed: " + response );
	}
	
	
	private void addFeature(String featureJson){

		try {
			String response = f.addFeature(seasonID, featureJson, "ROOT", sessionToken);
			Assert.assertTrue(response.contains("error"), "Test should fail, but instead passed: " + response );

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	
	}
	


	@AfterTest
	public void reset(){
		baseUtils.reset(productID, sessionToken);
	}

}
