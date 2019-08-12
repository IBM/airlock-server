package tests.restapi.scenarios.feature_with_configuration;

import java.io.IOException;

import java.util.Arrays;

import org.apache.wink.json4j.JSONException;
import org.apache.wink.json4j.JSONObject;
import org.apache.wink.json4j.JSONArray;
import org.testng.Assert;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import tests.com.ibm.qautils.FileUtils;
import tests.restapi.AirlockUtils;
import tests.restapi.FeaturesRestApi;
import tests.restapi.ProductsRestApi;

public class ConfigurationSchemasEnumVsDefaultSchema {
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

	
	@Test (description = "Add configurationSchema object with enum")
	public void addConfigurationSchema() throws IOException, JSONException{
		String feature = f.getFeature(featureID, sessionToken);

		JSONObject size = new JSONObject();
		size.put("type", "integer");
		JSONArray values = new JSONArray(Arrays.asList(1, 2, 3));
		size.put("enum", values);
		JSONObject sizeObj = new JSONObject();
		sizeObj.put("size", size);
		JSONObject configSchema = new JSONObject();
		configSchema.put("properties", sizeObj);
		configSchema.put("type", "object");

		JSONObject json = new JSONObject(feature);
		json.put("configurationSchema", configSchema);
		
		JSONObject defaultSchema = new JSONObject();
		defaultSchema.put("size", 1);		
		json.put("defaultConfiguration", defaultSchema.toString());
		String response = f.updateFeature(seasonID, featureID, json.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Test should pass, but instead failed: " + response );
	}

	@Test (dependsOnMethods = "addConfigurationSchema",  description = "Assign value that is not in enum in defaultConfiguration ")
	public void valueOutOfEnumRange() throws IOException, JSONException{
		String feature = f.getFeature(featureID, sessionToken);
		JSONObject json = new JSONObject(feature);
		
		JSONObject defaultSchema = new JSONObject();
		defaultSchema.put("size", 8);		
		json.put("defaultConfiguration", defaultSchema.toString());


		String response = f.updateFeature(seasonID, featureID, json.toString(), sessionToken);
		Assert.assertTrue(response.contains("error"), "Test should fail, but instead passed: " + response );
		
	}
	
	
	@Test (dependsOnMethods = "valueOutOfEnumRange", description = "Add minimum value  to configurationSchema object")
	public void addMinimumToConfigurationSchema() throws IOException, JSONException{
		String feature = f.getFeature(featureID, sessionToken);
		/*		  "type": "object",
		  "properties": {
		    "size": {
		        "minimum": 1,
		        "type": "integer"
		    }
		  }
*/
		JSONObject size = new JSONObject();
		size.put("type", "integer");
		size.put("minimum", 1);
		JSONObject sizeObj = new JSONObject();
		sizeObj.put("size", size);
		JSONObject configSchema = new JSONObject();
		configSchema.put("properties", sizeObj);
		configSchema.put("type", "object");

		JSONObject json = new JSONObject(feature);
		json.put("configurationSchema", configSchema);

		JSONObject defaultSchema = new JSONObject();
		defaultSchema.put("size", 2);		
		json.put("defaultConfiguration", defaultSchema.toString());
		
		String response = f.updateFeature(seasonID, featureID, json.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Test should pass, but instead failed: " + response );
	}
	
	@Test (dependsOnMethods = "addMinimumToConfigurationSchema",  description = "Assign value that is less than minimum in defaultConfiguration ")
	public void valueOutOfMinimumRange() throws IOException, JSONException{
		String feature = f.getFeature(featureID, sessionToken);
		JSONObject json = new JSONObject(feature);
		
		JSONObject defaultSchema = new JSONObject();
		defaultSchema.put("size", -1);		
		json.put("defaultConfiguration", defaultSchema.toString());
		String response = f.updateFeature(seasonID, featureID, json.toString(), sessionToken);
		 Assert.assertTrue(response.contains("error"), "Test should fail, but instead passed: " + response );
	}
	
	@Test (dependsOnMethods = "valueOutOfMinimumRange",  description = "Assign value that is greater than minimum in defaultConfiguration ")
	public void validMinimumRange() throws IOException, JSONException{
		String feature = f.getFeature(featureID, sessionToken);
		JSONObject json = new JSONObject(feature);
		
		JSONObject defaultSchema = new JSONObject();
		defaultSchema.put("size", 3);		
		json.put("defaultConfiguration", defaultSchema.toString());


		String response = f.updateFeature(seasonID, featureID, json.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Test should pass, but instead failed: " + response );
	}

	
	
	@AfterTest
	private void reset(){
		baseUtils.reset(productID, sessionToken);
	}

}
