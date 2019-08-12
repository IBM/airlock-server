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

public class MIXGroupFieldsValidation {
	protected String seasonID;
	protected String featureID;
	protected String filePath;
	protected String m_url;
	protected String feature;
	protected FeaturesRestApi f;
	protected ProductsRestApi p;
	private AirlockUtils baseUtils;
	protected String productID;
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
		feature = FileUtils.fileToString(filePath + "feature-mutual.txt", "UTF-8", false);
	}
	@Test 
		public void createFeature() throws JSONException, IOException{
		
		String response = f.addFeature(seasonID, feature, "ROOT", sessionToken);
		Assert.assertFalse(response.contains("error"), "Feature was not created: " + response );
	}
	
	@Test 
	public void missingMaxFeaturesOn() throws JSONException, IOException{
		JSONObject json = new JSONObject(feature);
		json.remove("maxFeaturesOn");
		String featureID = f.addFeature(seasonID, json.toString(), "ROOT", sessionToken);
		//by default this field is added with value 1
		String createdFeature = f.getFeature(featureID, sessionToken);
		json = new JSONObject(createdFeature); 
		Assert.assertEquals(json.getString("maxFeaturesOn"), "1", "Incorrect default value for maxFeaturesOn");
		
	}
	
	@Test 
	public void negativeMaxFeaturesOn() throws JSONException, IOException{
		JSONObject json = new JSONObject(feature);
		json.remove("maxFeaturesOn");
		json.put("maxFeaturesOn", -1);
		String response = f.addFeature(seasonID, json.toString(), "ROOT", sessionToken);
		Assert.assertTrue(response.contains("error"), "Test should fail, but instead passed: " + response );
	}
	
	@Test 
	public void zeroMaxFeaturesOn() throws JSONException, IOException{
		JSONObject json = new JSONObject(feature);
		json.remove("maxFeaturesOn");
		json.put("maxFeaturesOn", 0);
		String response = f.addFeature(seasonID, json.toString(), "ROOT", sessionToken);
		Assert.assertTrue(response.contains("error"), "Test should fail, but instead passed: " + response );
	}
	
	@Test 
	public void positiveMaxFeaturesOn() throws JSONException, IOException{
		JSONObject json = new JSONObject(feature);
		json.remove("maxFeaturesOn");
		json.put("maxFeaturesOn", 10);
		String response = f.addFeature(seasonID, json.toString(), "ROOT", sessionToken);
		Assert.assertFalse(response.contains("error"), "Test should pass, but instead failed: " + response );
	}
	
	@Test
	public void emptyMaxFeaturesOn() throws JSONException, IOException{
		JSONObject json = new JSONObject(feature);
		json.remove("maxFeaturesOn");
		json.put("maxFeaturesOn", "");
		String response = f.addFeature(seasonID, json.toString(), "ROOT", sessionToken);	
		Assert.assertTrue(response.contains("error"), "Test should fail, but instead passed: " + response );
	}
	
	@Test 
	public void nullMaxFeaturesOn() throws JSONException, IOException{
		JSONObject json = new JSONObject(feature);
		json.remove("maxFeaturesOn");
		json.put("maxFeaturesOn", JSONObject.NULL);
		String response = f.addFeature(seasonID, json.toString(), "ROOT", sessionToken);
		Assert.assertFalse(response.contains("error"), "Test should pass, but instead failed: " + response );
	}
	
	@AfterTest
	private void reset(){
		baseUtils.reset(productID, sessionToken);
	}

	

}
