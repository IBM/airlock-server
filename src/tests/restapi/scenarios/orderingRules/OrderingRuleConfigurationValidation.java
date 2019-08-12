package tests.restapi.scenarios.orderingRules;

import java.io.IOException;

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
import tests.restapi.InputSchemaRestApi;
import tests.restapi.scenarios.orderingRules.Config;

public class OrderingRuleConfigurationValidation {
	protected String seasonID;
	protected String filePath;
	protected FeaturesRestApi f;
	private AirlockUtils baseUtils;
	protected String productID;
	protected String m_url;
	private String sessionToken = "";
	protected String orderingRule;
	protected String orderingRuleID;
	protected String featureID;
	private String childID1;
	protected InputSchemaRestApi schema;
	protected Config config;
	
	@BeforeClass
	@Parameters({"url", "analyticsUrl", "translationsUrl", "configPath", "sessionToken", "userName", "userPassword", "appName", "productsToDeleteFile"})
	public void init(String url, String analyticsUrl, String translationsUrl, String configPath, String sToken, String userName, String userPassword, String appName, String productsToDeleteFile) throws Exception{
		m_url = url;
		filePath = configPath;
		f = new FeaturesRestApi();
		f.setURL(m_url);
        schema = new InputSchemaRestApi();
        schema.setURL(m_url);
 
		baseUtils = new AirlockUtils(url, analyticsUrl, translationsUrl, configPath, sToken, userName, userPassword, appName, productsToDeleteFile);
		sessionToken = baseUtils.sessionToken;

		productID = baseUtils.createProduct();
		baseUtils.printProductToFile(productID);

		config = new Config(url, analyticsUrl, translationsUrl, configPath, sToken, userName, userPassword, appName, productsToDeleteFile);
		config.stage = "DEVELOPMENT";
		seasonID = config.addSeason("9.0");

		String feature = FileUtils.fileToString(filePath + "feature1.txt", "UTF-8", false);
		featureID = f.addFeature(seasonID, feature, "ROOT", sessionToken);

		orderingRule = FileUtils.fileToString(filePath + "orderingRule/orderingRule1.txt", "UTF-8", false);
		JSONObject jsonOR = new JSONObject(orderingRule);
		jsonOR.put("name", RandomStringUtils.randomAlphabetic(5));
		orderingRuleID = f.addFeature(seasonID, jsonOR.toString(), featureID, sessionToken);
	}
	

	
	@Test (description = "Pass weight values that are not double")
	public void invalidWeightValues() throws JSONException, IOException{
		String feature = FileUtils.fileToString(filePath + "feature2.txt", "UTF-8", false);
		JSONObject json = new JSONObject(feature);
		json.put("name", RandomStringUtils.randomAlphabetic(5));
		childID1 = f.addFeature(seasonID, json.toString(), featureID, sessionToken);		

		JSONObject jsonOR = new JSONObject(f.getFeature(orderingRuleID, sessionToken));
		
		JSONObject configJson = new JSONObject();
		configJson.put(childID1, "aaa");
		
		jsonOR.put("configuration", configJson.toString());
		String response = f.updateFeature(seasonID, orderingRuleID, jsonOR.toString(), sessionToken);
		Assert.assertTrue(response.contains("error"), "Updated orderingRule with incorrect weight  " );
		
		configJson = new JSONObject();
		configJson.put(childID1, "1.a");
		
		jsonOR.put("configuration", configJson.toString());
		response = f.updateFeature(seasonID, orderingRuleID, jsonOR.toString(), sessionToken);
		Assert.assertTrue(response.contains("error"), "Updated orderingRule with incorrect weight  " );
		
		configJson = new JSONObject();
		configJson.put(childID1, "true");
		
		jsonOR.put("configuration", configJson.toString());
		response = f.updateFeature(seasonID, orderingRuleID, jsonOR.toString(), sessionToken);
		Assert.assertTrue(response.contains("error"), "Updated orderingRule with incorrect weight  " );

		configJson = new JSONObject();
		configJson.put(childID1, "\"5\"");
		
		jsonOR.put("configuration", configJson.toString());
		response = f.updateFeature(seasonID, orderingRuleID, jsonOR.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Update orderingRule with correct weight failed" );
	}
	
	@Test (dependsOnMethods="invalidWeightValues", description = "Weight value from input schema")
	public void weightValueFromSchema() throws Exception{
		//add input schema
		String sch = schema.getInputSchema(seasonID, sessionToken);
        JSONObject jsonSchema = new JSONObject(sch);
        String schemaBody = FileUtils.fileToString(filePath + "inputSchemas/inputSchema_optionalField_unitsOfMeasure.txt", "UTF-8", false);
        jsonSchema.put("inputSchema", new JSONObject(schemaBody));
        String schemaResponse = schema.updateInputSchema(seasonID, jsonSchema.toString(), sessionToken);
        Assert.assertFalse(schemaResponse.contains("error"), "Schema was not added to the season" + schemaResponse);

		JSONObject jsonOR = new JSONObject(f.getFeature(orderingRuleID, sessionToken));
		
		//string
		JSONObject configJson = new JSONObject();
		configJson.put(childID1, "context.device.connectionType");
		
		jsonOR.put("configuration", configJson.toString());
		String response = f.updateFeature(seasonID, orderingRuleID, jsonOR.toString(), sessionToken);
		Assert.assertTrue(response.contains("error"), "Update orderingRule with incorrect weight  " );

		//integer
		configJson = new JSONObject();
		configJson.put(childID1, "context.device.screenHeight");
		jsonOR.put("configuration", configJson.toString());
		response = f.updateFeature(seasonID, orderingRuleID, jsonOR.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Update orderingRule with correct weight failed " + response );

		//boolean
		configJson = new JSONObject();
		configJson.put(childID1, "context.userPreferences.is24HourFormat");
		jsonOR.put("configuration", configJson.toString());
		response = f.updateFeature(seasonID, orderingRuleID, jsonOR.toString(), sessionToken);
		Assert.assertTrue(response.contains("error"), "Update orderingRule with correct weight failed " );

	}
	
	
	@Test (dependsOnMethods="weightValueFromSchema", description = "Weight value from utility")
	public void weightValueFromUtility() throws Exception{
		//add utility
		String utilityID1 = config.addUtility("function isString() {return \"aaa\";}");
		Assert.assertFalse(utilityID1.contains("error"), "utility was not added to the season: " + utilityID1);

		String utilityID2 = config.addUtility("function isBoolean() {return true;}");
		Assert.assertFalse(utilityID2.contains("error"), "utility was not added to the season: " + utilityID2);

		String utilityID3 = config.addUtility("function isInteger() {return 10;}");
		Assert.assertFalse(utilityID3.contains("error"), "utility was not added to the season: " + utilityID3);

		JSONObject jsonOR = new JSONObject(f.getFeature(orderingRuleID, sessionToken));
		
		//string
		JSONObject configJson = new JSONObject();
		configJson.put(childID1, "isString()");
		
		jsonOR.put("configuration", configJson.toString());
		String response = f.updateFeature(seasonID, orderingRuleID, jsonOR.toString(), sessionToken);
		Assert.assertTrue(response.contains("error"), "Update orderingRule with incorrect weight  " );

		//boolean
		configJson = new JSONObject();
		configJson.put(childID1, "isBoolean()");
		jsonOR.put("configuration", configJson.toString());
		response = f.updateFeature(seasonID, orderingRuleID, jsonOR.toString(), sessionToken);
		Assert.assertTrue(response.contains("error"), "Update orderingRule with correct weight failed " );

		//integer
	    configJson = new JSONObject();
		configJson.put(childID1, "isInteger()");
		
		jsonOR.put("configuration", configJson.toString());
		response = f.updateFeature(seasonID, orderingRuleID, jsonOR.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Update orderingRule with correct weight failed " );

	}
	
	@Test (dependsOnMethods="weightValueFromUtility", description = "Weight value from stream")
	public void weightValueFromStream() throws Exception{
		String streamID = config.createStream("1.1.1");
		Assert.assertFalse(streamID.contains("error"), "Stream was not created: " + streamID);
		
		JSONObject jsonOR = new JSONObject(f.getFeature(orderingRuleID, sessionToken));
		String ruleString = "if (context.streams != undefined && context.streams.video_played != undefined && context.streams.video_played.averageAdsTime) {true;} else {false;}";
		JSONObject rule = new JSONObject();
		rule.put("ruleString", ruleString);
		jsonOR.put("rule", rule);
		
		//string
		JSONObject configJson = new JSONObject();
		configJson.put(childID1, "context.streams.video_played.adsNumberOfSessionPerDay");
		
		jsonOR.put("configuration", configJson.toString());
		String response = f.updateFeature(seasonID, orderingRuleID, jsonOR.toString(), sessionToken);
		Assert.assertTrue(response.contains("error"), "Update orderingRule with incorrect weight  " );

		//integer
		configJson = new JSONObject();
		configJson.put(childID1, "context.streams.video_played.adsNumber");
		
		jsonOR.put("configuration", configJson.toString());
		response = f.updateFeature(seasonID, orderingRuleID, jsonOR.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Update orderingRule with correct weight failed " + response );


	}
	
	
	@AfterTest
	private void reset(){
		baseUtils.reset(productID, sessionToken);
	}
}
