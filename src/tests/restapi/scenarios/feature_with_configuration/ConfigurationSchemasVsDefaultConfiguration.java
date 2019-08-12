package tests.restapi.scenarios.feature_with_configuration;

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

public class ConfigurationSchemasVsDefaultConfiguration {
	protected String seasonID;
	protected String featureID;
	protected String productID;
	protected String filePath;
	protected FeaturesRestApi f;
	protected ProductsRestApi p;
	private AirlockUtils baseUtils;
	private String sessionToken = "";
	
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
		String feature = FileUtils.fileToString(filePath + "feature1.txt", "UTF-8", false);
		featureID = f.addFeature(seasonID, feature, "ROOT", sessionToken );
	}

	
	@Test (description = "Add configurationSchema object to an existing feature without adding defaultSchema")
	public void addConfigurationSchema() throws IOException, JSONException{
		String feature = f.getFeature(featureID, sessionToken);
		JSONObject configSchema = new JSONObject();
		JSONObject schema = new JSONObject();
		schema.put("type", "string");
		configSchema.put("color", schema);
		JSONObject json = new JSONObject(feature);
		json.put("configurationSchema", configSchema);

		String response = f.updateFeature(seasonID, featureID, json.toString(), sessionToken);
		 Assert.assertTrue(response.contains("error"), "Test should fail, but instead passed: " + response );
	}
	
	@Test (dependsOnMethods = "addConfigurationSchema",  description = "Add configurationSchema object to an existing feature together with defaultSchema")
	public void createFeatureIncludingConfigurationSchema() throws IOException, JSONException{
		String feature = f.getFeature(featureID, sessionToken);
		JSONObject configSchema = new JSONObject();
		JSONObject schema = new JSONObject();
		schema.put("type", "string");
		configSchema.put("color", schema);
		
		JSONObject json = new JSONObject(feature);
		json.put("configurationSchema", configSchema);
		
		JSONObject defaultSchema = new JSONObject();
		defaultSchema.put("color", "red");
		json.put("defaultConfiguration", defaultSchema);
		String response = f.updateFeature(seasonID, featureID, json.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Test should pass, but instead failed: " + response );
	}

	@Test (dependsOnMethods = "createFeatureIncludingConfigurationSchema",  description = "Remove configurationSchema object and leave defaultSchema")
	public void removeConfigurationSchema() throws IOException, JSONException{
		String feature = f.getFeature(featureID, sessionToken);
		JSONObject json = new JSONObject(feature);
		json.put("configurationSchema", new JSONObject());
		String response = f.updateFeature(seasonID, featureID, json.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Test should pass, but instead failed: " + response );
	}
	
	@Test (dependsOnMethods = "removeConfigurationSchema",  description = "Remove defaultSchema as without configurationSchema it is optional")
	public void removeDefaultConfiguration() throws IOException, JSONException{
		String feature = f.getFeature(featureID, sessionToken);
		JSONObject json = new JSONObject(feature);
		json.put("defaultSchema", new JSONObject());
		String response = f.updateFeature(seasonID, featureID, json.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Test should pass, but instead failed: " + response );
	}
	
	@AfterTest
	private void reset(){
		baseUtils.reset(productID, sessionToken);
	}

}
