package tests.restapi.scenarios.feature_with_configuration;

import java.io.IOException;

import java.util.UUID;

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

public class UniqueConfigurationRuleName {
	protected String seasonID;
	protected String seasonID2;
	protected String featureID;
	protected String filePath;
	protected FeaturesRestApi f;
	protected String feature;
	protected ProductsRestApi p;
	protected SeasonsRestApi s;
	private AirlockUtils baseUtils;
	protected String productID;
	private String sessionToken = "";
	protected String configRuleID;
	protected String configuration;
	
	@BeforeClass
 	@Parameters({"url", "analyticsUrl", "translationsUrl", "configPath", "sessionToken", "userName", "userPassword", "appName", "productsToDeleteFile"})
	public void init(String url, String analyticsUrl, String translationsUrl, String configPath, String sToken, String userName, String userPassword, String appName, String productsToDeleteFile) throws Exception{
		filePath = configPath;
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
		featureID = f.addFeature(seasonID, feature, "ROOT", sessionToken);
		configuration = FileUtils.fileToString(filePath + "configuration_rule1.txt", "UTF-8", false); 

	}



	@Test (description = "Test creating the same configuration rule twice in the same season with the same namespace")
	public void testAddSameConfigurationRule() throws IOException {
		configRuleID = f.addFeature(seasonID, configuration, featureID, sessionToken); 
		String response = f.addFeature(seasonID, configuration, featureID, sessionToken);
		Assert.assertTrue(response.contains("error"), "Test should fail, but instead passed: " + response );
	}
	

	
	@Test (description = "Test creating the same configuration rule twice in the same season with different namespaces")
	public void testAddFeatureWithDifferentNamespace() throws JSONException, IOException{
			JSONObject obj = new JSONObject(configuration);
			obj.put("namespace", "namespace2");
			String configRuleID2 = f.addFeature(seasonID, obj.toString(), featureID, sessionToken);
			try {
				UUID.fromString(configRuleID2);
			} catch (Exception e){
				Assert.fail("Failed to add the same configuration rule name with different namespace");
			}

	}
	
	@Test (description = "Test creating  configuration rule with the same name/namespace as in feature")
	public void testAddConfigRuleWithFeatureName() throws JSONException, IOException{
		JSONObject jsonConfig = new JSONObject(configuration);
		JSONObject jsonFeature = new JSONObject(feature);
		jsonConfig.put("name", jsonFeature.getString("name"));
		jsonConfig.put("namespace", jsonFeature.getString("namespace"));
		String response = f.addFeature(seasonID, jsonConfig.toString(), featureID, sessionToken);
		Assert.assertTrue(response.contains("error"), "Test should fail, but instead passed: " + response );
	}
	

	@Test (description = "Test creating the same configuration rule twice in different seasons")
	public void testAddFeatureToDifferentSeason() throws JSONException, IOException{
		
			JSONObject obj = new JSONObject(feature);
			String featureID2 = f.addFeature(seasonID2, obj.toString(), "ROOT", sessionToken);
			//configuration minAppVersion should be greater or equal to season2 minAppVersion
			JSONObject configJson = new JSONObject(configuration);
			configJson.put("minAppVersion", "1.5");
			String configRuleID2 = f.addFeature(seasonID2, configJson.toString(), featureID2, sessionToken);
			try {
				UUID.fromString(configRuleID2);
			} catch (Exception e){
				Assert.fail("Failed to add the same configuration rule to different seasons");
			}
 			
	}
	
	/**
	 * Test updating the feature name to an existing name
	 * @throws IOException 
	 */
	@Test (description = "Test updating configuration rule name to an existing feature name")
	public void testUpdateConfigurationRuleNameToExisting() throws JSONException, IOException{
			configuration = f.getFeature(configRuleID, sessionToken);			
			JSONObject jsonConfig = new JSONObject(configuration);
			JSONObject jsonFeature = new JSONObject(feature);
			jsonConfig.put("name", jsonFeature.getString("name"));
			jsonConfig.put("namespace", jsonFeature.getString("namespace"));
			
			String response = f.updateFeature(seasonID, configRuleID, jsonConfig.toString(), sessionToken);
			Assert.assertTrue(response.contains("error"), "Test should fail, but instead passed: " + response );
	}
	
	
	@Test (description = "Test creating the same configuration rule twice in the same season with the same namespace")
	public void testAddSameConfigurationRuleToDifferentFeatures() throws IOException {
		//add first feature
		String feature2 = FileUtils.fileToString(filePath + "feature2.txt", "UTF-8", false);
		String featureID2 = f.addFeature(seasonID, feature2, "ROOT", sessionToken);
		
		//add second feature
		String feature3 = FileUtils.fileToString(filePath + "feature3.txt", "UTF-8", false);
		String featureID3 = f.addFeature(seasonID, feature3, "ROOT", sessionToken);
		
		//add same config rule to both features
		String configuration2 = FileUtils.fileToString(filePath + "configuration_rule2.txt", "UTF-8", false);
		String configRuleID2 = f.addFeature(seasonID, configuration2, featureID2, sessionToken); 
		configRuleID2 = f.addFeature(seasonID, configuration2, featureID3, sessionToken); 
		Assert.assertTrue(configRuleID2.contains("error"), "Test should fail, but instead passed: " + configRuleID2 );
	}

	@AfterTest
	private void reset(){
		baseUtils.reset(productID, sessionToken);
	}

}
