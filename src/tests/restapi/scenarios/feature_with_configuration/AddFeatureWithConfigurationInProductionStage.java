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

public class AddFeatureWithConfigurationInProductionStage {
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



	//@Test (description = "Create feature in development stage")
	public void addComponents() throws JSONException, IOException{
		
			String feature = FileUtils.fileToString(filePath + "feature1.txt", "UTF-8", false);	
			featureID = f.addFeature(seasonID, feature, "ROOT", sessionToken);
			Assert.assertFalse(featureID.contains("error"), "Test should pass, but instead failed: " + featureID );
			
	}
	
	//@Test (dependsOnMethods = "addComponents", description = "Add configuration in production stage to feature in development")
	public void addConfiguratonInProduction1() throws JSONException, IOException{
		
			String configuration = FileUtils.fileToString(filePath + "configuration_rule1.txt", "UTF-8", false);
			JSONObject json = new JSONObject(configuration);
			json.put("stage", "PRODUCTION");
			configurationID = f.addFeature(seasonID, json.toString(), featureID, sessionToken);
			Assert.assertTrue(configurationID.contains("error"), "Test should fail, but instead passed: " + configurationID );
			
	}
	
	//@Test (dependsOnMethods = "addConfiguratonInProduction1", description = "Move feature to production stage")
	public void moveFeatureToProduction() throws JSONException, IOException{
		
		String feature = f.getFeature(featureID, sessionToken);	
		JSONObject json = new JSONObject(feature);
		json.put("stage", "PRODUCTION");	
		f.updateFeature(seasonID, featureID, json.toString(), sessionToken);
			
	}
	
	//@Test (dependsOnMethods = "moveFeatureToProduction", description = "Add configuration in production stage to feature in production")
	public void addConfiguratonInProduction2() throws JSONException, IOException{
		
			String configuration = FileUtils.fileToString(filePath + "configuration_rule1.txt", "UTF-8", false);
			JSONObject json = new JSONObject(configuration);
			json.put("stage", "PRODUCTION");
			configurationID = f.addFeature(seasonID, json.toString(), featureID, sessionToken);
			Assert.assertFalse(configurationID.contains("error"), "Test should pass, but instead failed: " + configurationID );
	}
	
	//@Test (dependsOnMethods= "addConfiguratonInProduction2",  description = "Move  both feature and configuration to development")
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
