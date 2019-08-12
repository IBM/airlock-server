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

public class ConfigurationSchemasValidationVsConfigurationRule {
	protected String seasonID;
	protected String featureID;
	protected String configID;
	protected String productID;
	protected String filePath;
	protected FeaturesRestApi f;
	protected ProductsRestApi p;
	private AirlockUtils baseUtils;
	private String sessionToken = "";
	protected String configuration;
	
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
		configuration = FileUtils.fileToString(filePath + "configuration_rule1.txt", "UTF-8", false);
	}


	
	@Test (description = "Add configurationSchema object to an existing feature together with defaultSchema")
	public void addConfigurationSchema() throws IOException, JSONException{
		String feature = f.getFeature(featureID, sessionToken);
		String configSchema = FileUtils.fileToString(filePath + "configurationSchema2.txt", "UTF-8", false);
		JSONObject configJson = new JSONObject(configSchema);
		JSONObject json = new JSONObject(feature);
		json.put("configurationSchema", configJson);
		
		String defaultSchema = FileUtils.fileToString(filePath + "defaultConfiguration2.txt", "UTF-8", false);
		json.put("defaultConfiguration", defaultSchema);
		String response = f.updateFeature(seasonID, featureID, json.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Test should pass, but instead failed: " + response );
	}

	//tests that create configuration rule

	@Test (dependsOnMethods = "addConfigurationSchema", description = "Add configuration rule with invalid configuration - additional field that doesn't exist in schema")
	public void addConfigurationRuleWithAdditionalField() throws IOException, JSONException{
		
		JSONObject configJson = new JSONObject(configuration);
		JSONObject outputConfig = new JSONObject();
		outputConfig.put("firstName", "Jon");
		configJson.put("configuration", outputConfig.toString());
		String response = f.addFeature(seasonID, configJson.toString(), featureID, sessionToken );
		Assert.assertTrue(response.contains("error"), "Test should fail, but instead passed: " + response );
		
	}
	
	@Test (dependsOnMethods = "addConfigurationRuleWithAdditionalField", description = "Add configuration rule with invalid configuration - field of incorrect type")
	public void addConfigurationRuleInvalidFieldType() throws IOException, JSONException{
		
		JSONObject configJson = new JSONObject(configuration);
		JSONObject outputConfig = new JSONObject();
		outputConfig.put("size", "5");
		configJson.put("configuration", outputConfig.toString());
		String response = f.addFeature(seasonID, configJson.toString(), featureID, sessionToken );
		Assert.assertTrue(response.contains("error"), "Test should fail, but instead passed: " + response );
	}
	
	@Test (dependsOnMethods = "addConfigurationRuleInvalidFieldType", description = "Add configuration rule with invalid configuration - field less than minimum")
	public void addConfigurationRuleFieldLessThanMinimum() throws IOException, JSONException{
		
		JSONObject configJson = new JSONObject(configuration);
		JSONObject outputConfig = new JSONObject();
		outputConfig.put("size", -1);
		configJson.put("configuration", outputConfig.toString());
		String response = f.addFeature(seasonID, configJson.toString(), featureID, sessionToken );
		Assert.assertTrue(response.contains("error"), "Test should fail, but instead passed: " + response );
	}
	
	@Test (dependsOnMethods = "addConfigurationRuleFieldLessThanMinimum", description = "Add configuration rule with valid configuration")
	public void addValidConfigurationRule() throws IOException, JSONException{
		
		String defaultSchema = FileUtils.fileToString(filePath + "defaultConfiguration2.txt", "UTF-8", false);
		String configuration = FileUtils.fileToString(filePath + "configuration_rule1.txt", "UTF-8", false);
		JSONObject configJson = new JSONObject(configuration);
		configJson.put("configuration", defaultSchema);
		configID = f.addFeature(seasonID, configJson.toString(), featureID, sessionToken );
		Assert.assertFalse(configID.contains("error"), "Test should pass, but instead failed: " + configID );
	}
	
	//tests that update a single configuration rule
	
	@Test (dependsOnMethods = "addValidConfigurationRule", description = "Update configuration rule with invalid configuration - additional field that doesn't exist in schema")
	public void updateConfigurationRuleWithAdditionalField() throws IOException, JSONException{
		configuration = f.getFeature(configID, sessionToken);
		JSONObject configJson = new JSONObject(configuration);
		JSONObject outputConfig = new JSONObject();
		outputConfig.put("firstName", "Jon");
		configJson.put("configuration", outputConfig.toString());
		String response = f.updateFeature(seasonID, configID, configJson.toString(), sessionToken );
		Assert.assertTrue(response.contains("error"), "Test should fail, but instead passed: " + response );
	}

	
	@Test (dependsOnMethods = "updateConfigurationRuleWithAdditionalField", description = "Update configuration rule with invalid configuration - field less than minimum")
	public void updateConfigurationRuleFieldLessThanMinimum() throws IOException, JSONException{
		configuration = f.getFeature(configID, sessionToken);
		JSONObject configJson = new JSONObject(configuration);
		JSONObject outputConfig = new JSONObject();
		outputConfig.put("size", -1);
		configJson.put("configuration", outputConfig.toString());
		String response = f.updateFeature(seasonID, configID, configJson.toString(), sessionToken );
		Assert.assertTrue(response.contains("error"), "Test should fail, but instead passed: " + response );
	}
	
	@Test (dependsOnMethods = "updateConfigurationRuleFieldLessThanMinimum", description = "Update configuration rule with invalid configuration - string instead of boolean")
	public void updateConfigurationRuleInvalidBooleanField() throws IOException, JSONException{
		configuration = f.getFeature(configID, sessionToken);
		JSONObject configJson = new JSONObject(configuration);
		JSONObject outputConfig = new JSONObject();
		outputConfig.put("alert", "test");
		configJson.put("configuration", outputConfig.toString());
		String response = f.updateFeature(seasonID, configID, configJson.toString(), sessionToken );
		Assert.assertTrue(response.contains("error"), "Test should fail, but instead passed: " + response );
	}
	
	
	@Test (dependsOnMethods = "updateConfigurationRuleInvalidBooleanField", description = "Update configuration rule with invalid configuration - field of incorrect type")
	public void updateConfigurationRuleInvalidFieldType() throws IOException, JSONException{
		configuration = f.getFeature(configID, sessionToken);
		JSONObject configJson = new JSONObject(configuration);
		JSONObject outputConfig = new JSONObject();
		outputConfig.put("size", "5");
		configJson.put("configuration", outputConfig.toString());
		String response = f.updateFeature(seasonID, configID, configJson.toString(), sessionToken );
		Assert.assertTrue(response.contains("error"), "Test should fail, but instead passed: " + response );
	}
	
	
	@AfterTest
	private void reset(){
		baseUtils.reset(productID, sessionToken);
	}

}
