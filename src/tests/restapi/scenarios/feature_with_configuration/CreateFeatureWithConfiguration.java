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

public class CreateFeatureWithConfiguration {
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
	
	}
	
	
	@Test (description = "Create a regular feature when confguration rule is including features field")
	public void createFeatureIncludingConfiguration() throws IOException{
		String feature = FileUtils.fileToString(filePath + "feature_with_configuration.txt", "UTF-8", false);
		String response = f.addFeature(seasonID, feature, "ROOT", sessionToken );
		 Assert.assertTrue(response.contains("error"), "Test should fail, but instead passed: " + response );
	}
	
	
	@Test (description = "Create a regular feature with configurationSchema object without defaultConfiguration")
	public void createFeatureIncludingConfigurationSchema() throws IOException, JSONException{
		String feature = FileUtils.fileToString(filePath + "feature1.txt", "UTF-8", false);
		JSONObject configSchema = new JSONObject();
		JSONObject schema = new JSONObject();
		schema.put("type", "string");
		configSchema.put("color", schema);
		JSONObject json = new JSONObject(feature);
		json.put("configurationSchema", configSchema);

		String response = f.addFeature(seasonID, json.toString(), "ROOT", sessionToken );
		 Assert.assertTrue(response.contains("error"), "Test should fail, but instead passed: " + response );
	}
	
	@Test (description = "Create a regular feature with configurationSchema and defaultConfiguration")
	public void createFeatureIncludingDefaultSchema() throws IOException, JSONException{
		String feature = FileUtils.fileToString(filePath + "feature2.txt", "UTF-8", false);
		JSONObject defaultConfig = new JSONObject();
		defaultConfig.put("color", "red");
		defaultConfig.put("text", "some text");
		
		
		JSONObject configSchema = new JSONObject();
		JSONObject schema = new JSONObject();
		schema.put("type", "string");
		configSchema.put("color", schema);
		
		JSONObject json = new JSONObject(feature);
		json.put("defaultConfiguration", defaultConfig);
		json.put("configurationSchema", configSchema);

		String response = f.addFeature(seasonID, json.toString(), "ROOT", sessionToken );
		Assert.assertFalse(response.contains("error"), "Test should pass, but instead failed: " + response );
	}
	

	@AfterTest
	private void reset(){
		baseUtils.reset(productID, sessionToken);
	}

}
