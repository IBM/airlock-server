package tests.restapi.scenarios.utilities;

import java.io.File;

import java.io.IOException;
import java.io.StringReader;
import java.util.Properties;

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
import tests.restapi.InputSchemaRestApi;
import tests.restapi.ProductsRestApi;
import tests.restapi.UtilitiesRestApi;

public class UseContextFieldsInUtility {
	protected String seasonID;
	protected String featureID;
	protected String productID;
	protected String stringID;
	protected String configID;
	protected String  utilityID;
	protected String deepFreezeID;
	protected String filePath;
	protected FeaturesRestApi f;
	protected ProductsRestApi p;
	protected UtilitiesRestApi u;
	//protected StringsRestApi t;
	protected InputSchemaRestApi schemaApi;
	private String sessionToken = "";	
	protected AirlockUtils baseUtils;
	
	
	protected UtilitiesRestApi utilitiesApi;
	
	
	@BeforeClass
	@Parameters({"url", "analyticsUrl", "translationsUrl", "configPath", "sessionToken", "userName", "userPassword", "appName", "productsToDeleteFile"})
	public void init(String url, String analyticsUrl, String translationsUrl, String configPath, String sToken, String userName, String userPassword, String appName, String productsToDeleteFile) throws Exception{
		filePath = configPath;
		baseUtils = new AirlockUtils(url, analyticsUrl, translationsUrl, configPath, sToken, userName, userPassword, appName, productsToDeleteFile);
		sessionToken = baseUtils.sessionToken;

		p = new ProductsRestApi();
		p.setURL(url);
		u = new UtilitiesRestApi();
		u.setURL(url);
		
		f = new FeaturesRestApi();
		f.setURL(url);
				
		schemaApi = new InputSchemaRestApi();
		schemaApi.setURL(url);
		productID = baseUtils.createProduct();
		baseUtils.printProductToFile(productID);
		seasonID = baseUtils.createSeason(productID);
		
		utilitiesApi = new UtilitiesRestApi();
		utilitiesApi.setURL(url);
		
		String feature = FileUtils.fileToString(filePath + "feature1.txt", "UTF-8", false);
		JSONObject featureJson = new JSONObject(feature);
		//featureJson.put("stage", "PRODUCTION");
		featureID = f.addFeature(seasonID, featureJson.toString(), "ROOT", sessionToken);				
	}
	
	@Test(description = "update inputSchema")
	public void updateInputSchema() throws Exception{
		String schemaStr = FileUtils.fileToString(filePath + "inputSchema.txt", "UTF-8", false);
		String schemaJsonStr = schemaApi.getInputSchema(seasonID, sessionToken);
		JSONObject schemaJson = new JSONObject(schemaJsonStr);
		schemaJson.put("inputSchema", new JSONObject(schemaStr));
		String response = schemaApi.updateInputSchema(seasonID, schemaJson.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Test should pass, but instead failed: " + response );
	}
	
	
	@Test( dependsOnMethods = "updateInputSchema",  description = "Add utility that uses a nonexistent string")
	public void addUtility() throws IOException, JSONException{		
		String utility = FileUtils.fileToString(filePath + "utilities" + File.separator + "utility1.txt", "UTF-8", false);
		Properties utilProps = new Properties();
		utilProps.load(new StringReader(utility));
		utilProps.setProperty("utility", "function A(){return context.device.connectionTypeWrong == \"aaa\";}");
		//utilProps.setProperty("stage", "PRODUCTION");
		
		utilityID = u.addUtility(seasonID, utilProps, sessionToken);
		Assert.assertFalse(utilityID.contains("error"), "Test should pass, but instead failed: " + utilityID );
	}
	
	@Test(dependsOnMethods = "addUtility",  description = "Add configuration that uses the utility tha uses a nonexisent string.")
	public void addConfigUsingUtilWithMissingString() throws IOException, JSONException{
		
		String configuration = FileUtils.fileToString(filePath + "configuration_rule1.txt", "UTF-8", false);
		
		JSONObject json = new JSONObject(configuration);
		json.put("configuration", "{\"text\": A()}");
		//json.put("stage", "PRODUCTION");
		
		configID = f.addFeature(seasonID, json.toString(), featureID, sessionToken );
		Assert.assertTrue(configID.contains("error"), "Test should fail, but instead passed: " + configID );
	}
	
	@Test(dependsOnMethods = "addConfigUsingUtilWithMissingString",  description = "Add configuration that uses the utility tha uses a nonexisent string.")
	public void addFeatureUsingUtilWithMissingString() throws IOException, JSONException{
		
		String feature = FileUtils.fileToString(filePath + "feature2.txt", "UTF-8", false);
		JSONObject featureJson = new JSONObject(feature);
		//featureJson.put("stage", "PRODUCTION");
		
		JSONObject rule = new JSONObject();
		rule.put("ruleString", "A() == true");
		featureJson.put("rule", rule);
		
		featureID = f.addFeature(seasonID, featureJson.toString(), "ROOT", sessionToken);
		Assert.assertTrue(featureID.contains("error"), "Test should fail, but instead passed: " + featureID );
	}
	

	
	@AfterTest
	private void reset(){
		baseUtils.reset(productID, sessionToken);
	}
}
