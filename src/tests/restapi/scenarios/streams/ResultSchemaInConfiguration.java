package tests.restapi.scenarios.streams;


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
import tests.restapi.StreamsRestApi;

public class ResultSchemaInConfiguration {
	protected String seasonID;
	protected String filePath;
	protected String m_url;
	private String streamID;
	protected StreamsRestApi streamApi;
	private AirlockUtils baseUtils;
	protected String productID;
	private String sessionToken = "";
	private InputSchemaRestApi schema;
	private FeaturesRestApi f;
	private String featureID;
	private String configID;
	
	@BeforeClass
	@Parameters({"url", "analyticsUrl", "translationsUrl", "configPath", "sessionToken", "userName", "userPassword", "appName", "productsToDeleteFile"})
	public void init(String url, String analyticsUrl, String translationsUrl, String configPath, String sToken, String userName, String userPassword, String appName, String productsToDeleteFile) throws Exception{
		filePath = configPath;
		baseUtils = new AirlockUtils(url, analyticsUrl, translationsUrl, configPath, sToken, userName, userPassword, appName, productsToDeleteFile);
		sessionToken = baseUtils.sessionToken;
		productID = baseUtils.createProduct();
		baseUtils.printProductToFile(productID);
		seasonID = baseUtils.createSeason(productID);

		m_url = url;
		streamApi = new StreamsRestApi();
		streamApi.setURL(url);
		
		f = new FeaturesRestApi();
		f.setURL(url);
	    schema = new InputSchemaRestApi();
	    schema.setURL(url);		
	}
	
	//create feature/configuration using schema fields when result schema doesn't exist - should fail
	//add stream
	//create feature/configuration using schema fields when result schema exists - success
	
	@Test (description="add regular schema")
	public void addInputSchema() {
	    try {
	        String sch = schema.getInputSchema(seasonID, sessionToken);
	        JSONObject jsonSchema = new JSONObject(sch);
	        String schemaBody = FileUtils.fileToString(filePath + "inputSchemas/Android_schema_v3.0.json", "UTF-8", false);
	        jsonSchema.put("inputSchema", new JSONObject(schemaBody));
	        String results =  schema.updateInputSchema(seasonID, jsonSchema.toString(), sessionToken);
	        Assert.assertFalse(results.contains("error"), "Input schema was not added");
	    }catch (Exception e){
	        Assert.fail(e.getMessage());
	    }
	}
	
	@Test (dependsOnMethods="addInputSchema", description="Add feature")
	public void createFeature() throws JSONException, IOException{
		String feature = FileUtils.fileToString(filePath + "feature1.txt", "UTF-8", false);
		featureID =  f.addFeature(seasonID, feature, "ROOT", sessionToken);
		Assert.assertFalse(featureID.contains("error"), "Can't create feature: " + featureID);

	}
	
	@Test (dependsOnMethods="createFeature", description="Add configuration with result schema")
	public void createConfigurationNoStream() throws JSONException, IOException{
		String feature = FileUtils.fileToString(filePath + "configuration_rule1.txt", "UTF-8", false);
		JSONObject json = new JSONObject(feature);
		json.put("name", RandomStringUtils.randomAlphabetic(5));
		json.put("minAppVersion", "10.5");
		JSONObject jj = new JSONObject();
		jj.put("ruleString", " (context.streams != undefined && context.streams.video_played != undefined && context.streams.video_played.averageAdsTime == \"10\")");
		json.put("rule", jj);
		String configuration =  "{ \"text\" :  context.streams.video_played.averageAdsTime	}" ;		
		json.put("configuration", configuration);
		String response =  f.addFeature(seasonID, json.toString(), featureID, sessionToken);
		Assert.assertTrue(response.contains("error"), "Can't create configuration rule: " + response);

	}
	
	
	@Test (dependsOnMethods="createConfigurationNoStream", description="Create dev stream ")
	private void createStream() throws Exception{
		
		String stream = FileUtils.fileToString(filePath + "streams/stream1.txt", "UTF-8", false);
		JSONObject streamJson = new JSONObject(stream);
		streamJson.put("name", "video played");
		streamID = streamApi.createStream(seasonID, streamJson.toString(), sessionToken);
		Assert.assertFalse(streamID.contains("error"), "Stream was not created: " + streamID);
	}
	
	
	@Test (dependsOnMethods="createStream", description="Add feature with result schema")
	public void createConfigurationWithStream() throws JSONException, IOException{
		String feature = FileUtils.fileToString(filePath + "configuration_rule1.txt", "UTF-8", false);
		JSONObject json = new JSONObject(feature);
		json.put("name", RandomStringUtils.randomAlphabetic(5));
		json.put("minAppVersion", "10.5");
		JSONObject jj = new JSONObject();
		jj.put("ruleString", " (context.streams != undefined && context.streams.video_played != undefined && context.streams.video_played.averageAdsTime == \"10\")");
		json.put("rule", jj);
		String configuration =  "{ \"text\" :  context.streams.video_played.averageAdsTime	}" ;		
		json.put("configuration", configuration);
		configID =  f.addFeature(seasonID, json.toString(), featureID, sessionToken);
		Assert.assertFalse(configID.contains("error"), "Can't create feature: " + configID);

	}
	
	
	@Test (dependsOnMethods="createConfigurationWithStream", description="Move CR to production, stream is in dev stage")
	public void moveCRToProductionStreamInDev() throws JSONException, IOException{
		String feature = f.getFeature(featureID, sessionToken);
		JSONObject json = new JSONObject(feature);
		json.put("stage", "PRODUCTION");
		String response =  f.updateFeature(seasonID, featureID, json.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Can't update feature to production");
		
		String config = f.getFeature(featureID, sessionToken);
		JSONObject jsonCR = new JSONObject(config);
		jsonCR.put("stage", "PRODUCTION");
		String responseCR =  f.updateFeature(seasonID, configID, jsonCR.toString(), sessionToken);		
		Assert.assertTrue(responseCR.contains("error"), "Updated CR to production, when its rule uses field in development");

	}
	
	@Test (dependsOnMethods="moveCRToProductionStreamInDev", description="Move CR to lower version than stream version")
	public void moveCRToLowVersionStreamInHighVer() throws JSONException, IOException{
		String feature = f.getFeature(configID, sessionToken);
		JSONObject json = new JSONObject(feature);
		json.put("minAppVersion", "1.0");
		String response =  f.updateFeature(seasonID, configID, json.toString(), sessionToken);
		Assert.assertTrue(response.contains("error"), "Updated CR to low version, when its rule uses field in higher version");

	}
	
	@Test (dependsOnMethods="moveCRToLowVersionStreamInHighVer", description="Move stream to production")
	public void moveStreamToProduction() throws Exception{
		String stream = streamApi.getStream(streamID, sessionToken);
		JSONObject json = new JSONObject(stream);
		json.put("stage", "PRODUCTION");
		String response =  streamApi.updateStream(streamID, json.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Can't update stream stage to production: " + response);

	}
	
	//create a new feature in production without using stream, then update it to use stream
	@Test (dependsOnMethods="moveStreamToProduction", description="Create configuration in production, use stream in update")
	public void updateCRToUseStream() throws JSONException, IOException{
		String feature = FileUtils.fileToString(filePath + "configuration_rule1.txt", "UTF-8", false);
		JSONObject json = new JSONObject(feature);
		json.put("name", RandomStringUtils.randomAlphabetic(5));
		json.put("stage", "PRODUCTION");
		json.put("minAppVersion", "10.5");
		String feature4TestId =  f.addFeature(seasonID, json.toString(), featureID, sessionToken);
		Assert.assertFalse(feature4TestId.contains("error"), "Can't create production feature: " + feature4TestId);
		
		feature = f.getFeature(feature4TestId, sessionToken);
		json = new JSONObject(feature);
				
		JSONObject jj = new JSONObject();
		jj.put("ruleString", " (context.streams != undefined && context.streams.video_played != undefined && context.streams.video_played.averageAdsTime == \"10\")");
		json.put("rule", jj);
		String configuration =  "{ \"text\" :  context.streams.video_played.averageAdsTime	}" ;		
		json.put("configuration", configuration);
		
		String response = f.updateFeature(seasonID, feature4TestId, json.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Can't update production configuration rule with stream fields: " + response);
	}

	@Test (dependsOnMethods="updateCRToUseStream", description="Move CR to production stream is in prod stage")
	public void moveCRToProductionStreamInProd() throws JSONException, IOException{
		String feature = f.getFeature(configID, sessionToken);
		JSONObject json = new JSONObject(feature);
		json.put("stage", "PRODUCTION");
		String response =  f.updateFeature(seasonID, configID, json.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Can't Updated CR to production:" + response);

	}
	
	@Test (dependsOnMethods="moveCRToProductionStreamInProd", description="Move stream to development")
	public void moveStreamToDevelopmentFeatureInProd() throws Exception{
		String stream = streamApi.getStream(streamID, sessionToken);
		JSONObject json = new JSONObject(stream);
		json.put("stage", "DEVELOPMENT");
		String response =  streamApi.updateStream(streamID, json.toString(), sessionToken);
		Assert.assertTrue(response.contains("error"), "Updated stream stage to development when feature using it is in production");

	}
	
	@Test (dependsOnMethods="moveStreamToDevelopmentFeatureInProd", description="Move stream to high version")
	public void moveStreamToHighVerFeatureInLowVer() throws Exception{
		String stream = streamApi.getStream(streamID, sessionToken);
		JSONObject json = new JSONObject(stream);
		json.put("minAppVersion", "12.0");
		String response =  streamApi.updateStream(streamID, json.toString(), sessionToken);
		Assert.assertTrue(response.contains("error"), "Updated stream minVersion to higher version when feature using it is in lower version");

	}
	
	@AfterTest
	public void reset(){
		baseUtils.reset(productID, sessionToken);
	}

	

}
