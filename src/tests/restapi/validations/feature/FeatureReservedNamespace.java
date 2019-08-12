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
import tests.com.ibm.qautils.JSONUtils;
import tests.restapi.AirlockUtils;
import tests.restapi.FeaturesRestApi;
import tests.restapi.ProductsRestApi;

public class FeatureReservedNamespace {
	protected String seasonID;
	protected String filePath;
	protected String m_url;
	protected JSONObject json;
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
		f = new FeaturesRestApi();
		f.setURL(url);
		String feature = FileUtils.fileToString(filePath + "feature1.txt", "UTF-8", false);
		feature = JSONUtils.generateUniqueString(feature, 5, "name");
		json = new JSONObject(feature);
 		
	}
	
	@Test (description = "Add feature with reserved namespace")
	public void createFeature() throws JSONException, IOException{
		json.put("namespace", "airlockExp");
		String response = f.addFeature(seasonID, json.toString(), "ROOT", sessionToken);
		Assert.assertTrue(response.contains("error"), "Test should fail, but instead passed: " + response );
	}
	
	@Test (dependsOnMethods="createFeature", description = "Add feature with reserved namespace in uppercase")
	public void createFeatureUpperCase() throws JSONException, IOException{
		json.put("namespace", "airlockExp".toUpperCase());
		String response = f.addFeature(seasonID, json.toString(), "ROOT", sessionToken);
		Assert.assertTrue(response.contains("error"), "Test should fail, but instead passed: " + response );
	}
	
	@Test (dependsOnMethods="createFeatureUpperCase", description = "Add feature with reserved namespace lowcase")
	public void createFeatureLowerCase() throws JSONException, IOException{
		json.put("namespace", "airlockExp".toLowerCase());
		String response = f.addFeature(seasonID, json.toString(), "ROOT", sessionToken);
		Assert.assertTrue(response.contains("error"), "Test should fail, but instead passed: " + response );
	}
	
	@Test (dependsOnMethods="createFeatureLowerCase", description = "Update feature to reserved namespace")
	public void updateFeature() throws JSONException, IOException{
		json.put("namespace", "nmsp1");
		String featureID = f.addFeature(seasonID, json.toString(), "ROOT", sessionToken);
		Assert.assertFalse(featureID.contains("error"), "Test should pass, but instead failed: " + featureID );
		
		String feature = f.getFeature(featureID, sessionToken);
		json = new JSONObject(feature);
		json.put("namespace", "airlockExp");
		String response = f.updateFeature(seasonID, featureID, json.toString(), sessionToken);
		Assert.assertTrue(response.contains("error"), "Test should fail, but instead passed: " + response );
		
	}
	
	
	
	@AfterTest
	public void reset(){
		baseUtils.reset(productID, sessionToken);
	}
}
