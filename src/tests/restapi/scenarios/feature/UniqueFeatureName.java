package tests.restapi.scenarios.feature;

import java.io.IOException;

import java.util.UUID;

import org.apache.commons.lang3.RandomStringUtils;
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
import tests.restapi.SeasonsRestApi;

public class UniqueFeatureName {
	protected String seasonID;
	protected String seasonID2;
	protected String featureID;
	protected String filePath;
	protected FeaturesRestApi f;
	protected String featureID1;
	protected String feature;
	protected ProductsRestApi p;
	protected SeasonsRestApi s;
	protected String productID;
	private String sessionToken = "";
	private AirlockUtils baseUtils;
	
	@BeforeClass
 	@Parameters({"url", "analyticsUrl", "translationsUrl", "configPath", "sessionToken", "userName", "userPassword", "appName", "productsToDeleteFile"})
	public void init(String url, String analyticsUrl, String translationsUrl, String configPath, String sToken, String userName, String userPassword, String appName, String productsToDeleteFile) throws Exception{
		p = new ProductsRestApi();
		p.setURL(url);
		s = new SeasonsRestApi();
		s.setURL(url);
		baseUtils = new AirlockUtils(url, analyticsUrl, translationsUrl, configPath, sToken, userName, userPassword, appName, productsToDeleteFile);
		sessionToken = baseUtils.sessionToken;
		productID = baseUtils.createProduct();
		baseUtils.printProductToFile(productID);
		String season1 = FileUtils.fileToString(configPath + "season1.txt", "UTF-8", false);
		seasonID = s.addSeason(productID, season1, sessionToken);
		String season2 = FileUtils.fileToString(configPath + "season2.txt", "UTF-8", false);
		seasonID2 = s.addSeason(productID, season2, sessionToken);
		f = new FeaturesRestApi();
		f.setURL(url);
		feature = FileUtils.fileToString(configPath + "feature1.txt", "UTF-8", false);
		featureID1 = f.addFeature(seasonID, feature, "ROOT", sessionToken);

	}


	/**
	 * Test creating the same feature twice in the same season with the same namespace - action not allowed
	 * @throws IOException 
	 */
	@Test (description = "Test creating the same feature twice in the same season with the same namespace")
	public void testAddSameFeature() throws IOException {
			String response = f.addFeature(seasonID, feature, "ROOT", sessionToken); 	
			Assert.assertTrue(response.contains("error"), "Test should fail, but instead passed: " + response );
	}
	
	/**
	 * Test creating the same feature twice in the same season with different namespaces - allowed action
	 * @throws IOException 
	 */
	@Test (description = "Test creating the same feature twice in the same season with different namespaces")
	public void testAddFeatureWithDifferentNamespace() throws JSONException, IOException{
			JSONObject obj = new JSONObject(feature);
			obj.put("namespace", "namespace2");
			String featureID2 = f.addFeature(seasonID, obj.toString(), "ROOT", sessionToken);
			try {
				UUID.fromString(featureID2);
			} catch (Exception e){
				Assert.fail("Failed to add the same feature name with different namespace");
			}

	}
	
	/**
	 * Test creating the same feature twice in different seasons - allowed action
	 * @throws IOException 
	 */
	@Test (description = "Test creating the same feature twice in different seasons")
	public void testAddFeatureToDifferentSeason() throws JSONException, IOException{
		
			JSONObject obj = new JSONObject(feature);
			String featureID2 = f.addFeature(seasonID2, obj.toString(), "ROOT", sessionToken);
			try {
				UUID.fromString(featureID2);
			} catch (Exception e){
				Assert.fail("Failed to add the same feature to different seasons");
			}
 			
	}
	
	/**
	 * Test updating the feature name to an existing name
	 * @throws IOException 
	 */
	@Test (description = "Test updating the feature name to an existing name")
	public void testUpdateFeatureNameToExisting() throws JSONException, IOException{
			
			JSONObject obj = new JSONObject(feature);
			String newName = RandomStringUtils.randomAlphabetic(5).toUpperCase();
			obj.put("name", newName);
			f.addFeature(seasonID, obj.toString(), "ROOT", sessionToken);
			
			String feature1 = f.getFeature(featureID1, sessionToken);
			obj = new JSONObject(feature1);
			obj.put("name", newName);
			String response = f.updateFeature(seasonID, featureID1, obj.toString(), sessionToken);
			Assert.assertTrue(response.contains("error"), "Test should fail, but instead passed: " + response );
	}
	
	/**
	 * Create the same name in different namespaces and then update the namespace for one of them
	 * @throws IOException 
	 */
	@Test (description = "Create the same name in different namespaces and then update the namespace for one of them")
	public void testUpdateFeatureToSameNamespace() throws JSONException, IOException{
		JSONObject obj = new JSONObject(feature);
		String newName = RandomStringUtils.randomAlphabetic(5).toUpperCase();
		obj.put("namespace", newName);
		f.addFeature(seasonID, obj.toString(), "ROOT", sessionToken);
		
		String feature1 = f.getFeature(featureID1, sessionToken);
		obj = new JSONObject(feature1);
		obj.put("namespace", newName);
		String response = f.updateFeature(seasonID, featureID1, obj.toString(), sessionToken);
		Assert.assertTrue(response.contains("error"), "Test should fail, but instead passed: " + response );	
	}

	@AfterTest
	private void reset(){
		baseUtils.reset(productID, sessionToken);
	}

}
