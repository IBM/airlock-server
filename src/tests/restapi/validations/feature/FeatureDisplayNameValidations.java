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

public class FeatureDisplayNameValidations {
	protected String seasonID;
	protected String productID;
	protected String featureID;
	protected String filePath;
	protected String m_url;
	protected JSONObject json;
	protected FeaturesRestApi f;
	protected ProductsRestApi p;
	protected AirlockUtils baseUtils;
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
		f = new FeaturesRestApi();
		f.setURL(url);
	}
	
	
	@Test (description = "Create a feature without displayName")
	public void createFeatureWithoutDisplayName() throws JSONException, IOException{
		String feature = FileUtils.fileToString(filePath + "feature1.txt", "UTF-8", false);
		String response  = f.addFeature(seasonID, feature, "ROOT", sessionToken); 
		Assert.assertFalse(response.contains("error"), "Test should pass, but instead failed: " + response );
		
		feature = f.getFeature(response, sessionToken);
		JSONObject json = new JSONObject(feature);
		Assert.assertTrue(json.containsKey("displayName"), "Test should pass, but instead failed: " + response );
	}
	

	
	@Test (dependsOnMethods="createFeatureWithoutDisplayName", description = "Create a feature with displayName")
	public void createFeatureWithDisplayName() throws JSONException, IOException{
		String feature = FileUtils.fileToString(filePath + "feature2.txt", "UTF-8", false);
		JSONObject json = new JSONObject(feature);
		json.put("displayName", "new display name");
		featureID = f.addFeature(seasonID, json.toString(), "ROOT", sessionToken); 
		Assert.assertFalse(featureID.contains("error"), "Test should pass, but instead failed: " + featureID );
		
		feature = f.getFeature(featureID, sessionToken);
		json = new JSONObject(feature);
		Assert.assertTrue(json.getString("displayName").equals("new display name"), "Test should pass, but instead failed: " + featureID );
	}
	
	
	@Test (dependsOnMethods="createFeatureWithDisplayName", description = "Update feature without displayName")
	public void updateFeatureWithoutDisplayName() throws JSONException, IOException{
		String feature = f.getFeature(featureID, sessionToken);
		JSONObject json = new JSONObject(feature);
		json.remove("displayName");
		f.updateFeature(seasonID, featureID, json.toString(), sessionToken);
		feature = f.getFeature(featureID, sessionToken);
		json = new JSONObject(feature);
		Assert.assertTrue(json.getString("displayName").equals("new display name"), "Test should pass, but instead failed: " + featureID );
	}
	
	@Test (dependsOnMethods="updateFeatureWithoutDisplayName", description = "Update feature without displayName")
	public void updateFeatureWithDisplayName() throws JSONException, IOException{
		String feature = f.getFeature(featureID, sessionToken);
		JSONObject json = new JSONObject(feature);
		json.put("displayName", "display name 2");
		f.updateFeature(seasonID, featureID, json.toString(), sessionToken);
		feature = f.getFeature(featureID, sessionToken);
		json = new JSONObject(feature);
		Assert.assertTrue(json.getString("displayName").equals("display name 2"), "Test should pass, but instead failed: " + featureID );
	}
	
	
	@AfterTest
	private void reset(){
		baseUtils.reset(productID, sessionToken);
	}
}
