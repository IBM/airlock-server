package tests.restapi.scenarios.feature_with_configuration;

import java.io.IOException;








import org.apache.wink.json4j.JSONException;
import org.apache.wink.json4j.JSONObject;
import org.apache.wink.json4j.JSONArray;
import org.testng.Assert;

import tests.com.ibm.qautils.FileUtils;
import tests.restapi.AirlockUtils;
import tests.restapi.FeaturesRestApi;
import tests.restapi.ProductsRestApi;

public class UpdateFeatureWithConfigurationInProductionStage {
	protected String seasonID;
	protected String featureID;
	protected String productID;
	protected String configurationID;
	protected String filePath;
	protected FeaturesRestApi f;
	protected ProductsRestApi p;
	private AirlockUtils baseUtils;
	private String sessionToken = "";
	
	
	/**
	 * DESIGN CHANGE:
		production is allowed under development
	 */
	
	
	//@BeforeClass
 	//@Parameters({"url", "analyticsUrl", "translationsUrl", "configPath", "sessionToken", "userName", "userPassword", "appName", "productsToDeleteFile"})
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



	//@Test (description = "Create feature and configuration in development stage")
	public void addComponents() throws JSONException, IOException{
		
			String feature = FileUtils.fileToString(filePath + "feature1.txt", "UTF-8", false);	
			featureID = f.addFeature(seasonID, feature, "ROOT", sessionToken);
			Assert.assertFalse(featureID.contains("error"), "Test should pass, but instead failed: " + featureID );
			
			String configuration = FileUtils.fileToString(filePath + "configuration_rule1.txt", "UTF-8", false);	
			configurationID = f.addFeature(seasonID, configuration, featureID, sessionToken);
			Assert.assertFalse(configurationID.contains("error"), "Test should pass, but instead failed: " + configurationID );
	}
	
	//@Test (dependsOnMethods= "addComponents", description = "Update configuration to production stage when feature is in development stage")
	public void updateConfigurationToProduction() throws JSONException, IOException{

			String configuration = f.getFeature(configurationID, sessionToken);	
			JSONObject json = new JSONObject(configuration);
			json.put("stage", "PRODUCTON");	
			String response = f.updateFeature(seasonID, configurationID, json.toString(), sessionToken);
			Assert.assertTrue(response.contains("error"), "Test should fail, but instead passed: " + response );
	}
	
	//@Test (dependsOnMethods= "updateConfigurationToProduction",  description = "Update feature to production stage when configuration is in development stage")
	public void updateFeatureToProduction() throws JSONException, IOException{

			String feature = f.getFeature(featureID, sessionToken);	
			JSONObject json = new JSONObject(feature);
			json.put("stage", "PRODUCTION");	
			String response = f.updateFeature(seasonID, featureID, json.toString(), sessionToken);
			Assert.assertFalse(response.contains("error"), "Test should pass, but instead failed: " + response );
	}
	
	//@Test (dependsOnMethods= "updateFeatureToProduction",  description = "Update feature and configuration to production stage ")
	public void updateFeatureAndConfigurationToProduction() throws JSONException, IOException{

			String configuration = f.getFeature(configurationID, sessionToken);	
			JSONObject json = new JSONObject(configuration);
			json.put("stage", "PRODUCTION");	
			String response = f.updateFeature(seasonID, configurationID, json.toString(), sessionToken);
			Assert.assertFalse(response.contains("error"), "Test should pass, but instead failed: " + response );
	}
	
	//@Test (dependsOnMethods= "updateFeatureAndConfigurationToProduction",  description = "Update feature to development when configuration is in production stage ")
	public void updateFeatureToDevConfigInProduction() throws JSONException, IOException{

		String feature = f.getFeature(featureID, sessionToken);	
		JSONObject json = new JSONObject(feature);
		json.put("stage", "DEVELOPMENT");	
		String response = f.updateFeature(seasonID, featureID, json.toString(), sessionToken);
		Assert.assertTrue(response.contains("error"), "Test should fail, but instead passed: " + response );
	}
	
	//@Test (dependsOnMethods= "updateFeatureToDevConfigInProduction",  description = "Move  configuration to development when feature in production")
	public void updateConfigToDevelopment() throws JSONException, IOException{

		String configuration = f.getFeature(configurationID, sessionToken);	
		JSONObject json = new JSONObject(configuration);
		json.put("stage", "DEVELOPMENT");	
		String response = f.updateFeature(seasonID, configurationID, json.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Test should pass, but instead failed: " + response );
	}
	
	//@Test (dependsOnMethods= "updateConfigToDevelopment",  description = "Move  both feature and configuration to production")
	public void moveFeatureAndConfigToProduction() throws JSONException, IOException{

		String feature = f.getFeature(featureID, sessionToken);	
		JSONObject json = new JSONObject(feature);
		json.put("stage", "PRODUCTION");
		
		String configuration = f.getFeature(configurationID, sessionToken);	
		JSONObject jsonConfig = new JSONObject(configuration);
		jsonConfig.put("stage", "PRODUCTION");
		
		JSONArray configs = new JSONArray();
		configs.put(jsonConfig);
		json.put("configurationRules", configs);
		
		String response = f.updateFeature(seasonID, featureID, json.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Test should pass, but instead failed: " + response );
	}
	
	//@Test (dependsOnMethods= "moveFeatureAndConfigToProduction",  description = "Move  both feature and configuration to development")
	public void moveFeatureAndConfigToDevelopment() throws JSONException, IOException{

		String feature = f.getFeature(featureID, sessionToken);	
		JSONObject json = new JSONObject(feature);
		json.put("stage", "DEVELOPMENT");
		
		String configuration = f.getFeature(configurationID, sessionToken);	
		JSONObject jsonConfig = new JSONObject(configuration);
		jsonConfig.put("stage", "DEVELOPMENT");
		
		JSONArray configs = new JSONArray();
		configs.put(jsonConfig);
		json.put("configurationRules", configs);
		
		String response = f.updateFeature(seasonID, featureID, json.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Test should pass, but instead failed: " + response );
	}
	

	//@AfterTest
	private void reset(){
		baseUtils.reset(productID, sessionToken);
	}

}
