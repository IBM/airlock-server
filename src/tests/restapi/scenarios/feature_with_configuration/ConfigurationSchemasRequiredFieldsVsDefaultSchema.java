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

public class ConfigurationSchemasRequiredFieldsVsDefaultSchema {
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


	
	@Test (description = "Add configurationSchema object to an existing feature together with defaultSchema")
	public void addConfigurationSchema() throws IOException, JSONException{
		String feature = f.getFeature(featureID, sessionToken);
		String configSchema = FileUtils.fileToString(filePath + "configurationSchema2.txt", "UTF-8", false);
		JSONObject configJson = new JSONObject(configSchema);
		JSONObject json = new JSONObject(feature);
		json.put("configurationSchema", configJson);
		
		String defaultSchema = FileUtils.fileToString(filePath + "defaultConfiguration2.txt", "UTF-8", false);
		JSONObject defaultJson = new JSONObject(defaultSchema);
		json.put("defaultConfiguration", defaultJson.toString());
		String response = f.updateFeature(seasonID, featureID, json.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Test should pass, but instead failed: " + response );
	}

	@Test (dependsOnMethods = "addConfigurationSchema",  description = "Remove required fields from default configuration")
	public void removeRequiredFieldsFromDefaultConfiguration() throws IOException, JSONException{
		String feature = f.getFeature(featureID, sessionToken);
		JSONObject json = new JSONObject(feature);	
		json.put("defaultConfiguration", "{\"color\":\"red\"}");
		//json.getJSONObject("defaultConfiguration").remove("color");
		//json.getJSONObject("defaultConfiguration").remove("text");

		String response = f.updateFeature(seasonID, featureID, json.toString(), sessionToken);
		 Assert.assertTrue(response.contains("error"), "Test should fail, but instead passed: " + response );
	}
	

	
	@AfterTest
	private void reset(){
		baseUtils.reset( productID, sessionToken);
	}

}
