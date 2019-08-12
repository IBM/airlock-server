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

public class DeleteStreamFieldInUseByFeature {
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
	
	
	@Test (dependsOnMethods="addInputSchema", description="Create dev stream ")
	private void createStream() throws Exception{
		
		String stream = FileUtils.fileToString(filePath + "streams/stream1.txt", "UTF-8", false);
		JSONObject streamJson = new JSONObject(stream);
		streamJson.put("name", "video played");
		streamID = streamApi.createStream(seasonID, streamJson.toString(), sessionToken);
		Assert.assertFalse(streamID.contains("error"), "Stream was not created: " + streamID);
	}
	
	
	@Test (dependsOnMethods="createStream", description="Add feature with result schema")
	public void createFeatureWithStream() throws JSONException, IOException{
		String feature = FileUtils.fileToString(filePath + "feature1.txt", "UTF-8", false);
		JSONObject json = new JSONObject(feature);
		json.put("name", RandomStringUtils.randomAlphabetic(5));
		json.put("minAppVersion", "10.5");
		JSONObject jj = new JSONObject();
		jj.put("ruleString", " (context.streams != undefined && context.streams.video_played != undefined && context.streams.video_played.averageAdsTime == \"10\")");
		json.put("rule", jj);
		featureID =  f.addFeature(seasonID, json.toString(), "ROOT", sessionToken);
		Assert.assertFalse(featureID.contains("error"), "Can't create feature: " + featureID);
		
		String cr = FileUtils.fileToString(filePath + "configuration_rule1.txt", "UTF-8", false);
		JSONObject jsonCR = new JSONObject(cr);
		jsonCR.put("name", RandomStringUtils.randomAlphabetic(5));
		jsonCR.put("minAppVersion", "10.5");
		JSONObject jjCR = new JSONObject();
		jjCR.put("ruleString", " (context.streams != undefined && context.streams.video_played != undefined && context.streams.video_played.averageAdsTime == \"10\")");
		jsonCR.put("rule", jjCR);
		String configuration =  "{ \"text\" :  context.streams.video_played.averageAdsTime	}" ;		
		jsonCR.put("configuration", configuration);
		configID =  f.addFeature(seasonID, jsonCR.toString(), featureID, sessionToken);
		Assert.assertFalse(configID.contains("error"), "Can't create configuration rule: " + configID);
		
	}
	
	@Test (dependsOnMethods="createFeatureWithStream", description="Update stream - remove field in use by feature and CR ")
	private void updateStreamRemoveField() throws Exception{
		String stream = streamApi.getStream(streamID, sessionToken);
		JSONObject streamJson = new JSONObject(stream);
		JSONObject newSchema = new JSONObject(FileUtils.fileToString(filePath + "streams/results_schema2.json", "UTF-8", false));
		streamJson.put("resultsSchema", newSchema);
		String result = streamApi.updateStream(streamID, streamJson.toString(), sessionToken);
		Assert.assertTrue(result.contains("error"), "Stream field in use by feature was deleted: " + result);


	}
	
	@Test (dependsOnMethods="updateStreamRemoveField", description="Update stream - add field and use it in feature and CR ")
	private void updateStreamAddField() throws Exception{
		String stream = streamApi.getStream(streamID, sessionToken);
		JSONObject streamJson = new JSONObject(stream);
		JSONObject newSchema = new JSONObject(FileUtils.fileToString(filePath + "streams/results_schema1.json", "UTF-8", false));
		streamJson.put("resultsSchema", newSchema);
		String result = streamApi.updateStream(streamID, streamJson.toString(), sessionToken);
		Assert.assertFalse(result.contains("error"), "Can't update stream: " + result);
		
		
		String feature = FileUtils.fileToString(filePath + "feature2.txt", "UTF-8", false);
		JSONObject json = new JSONObject(feature);
		json.put("name", RandomStringUtils.randomAlphabetic(5));
		json.put("minAppVersion", "10.5");
		JSONObject jj = new JSONObject();
		jj.put("ruleString", " (context.streams != undefined && context.streams.video_played != undefined && context.streams.video_played.adsTotalNumberOfSession == \"10\")");
		json.put("rule", jj);
		String featureID2 =  f.addFeature(seasonID, json.toString(), "ROOT", sessionToken);
		Assert.assertFalse(featureID2.contains("error"), "Can't create feature: " + featureID2);
		
		String cr = FileUtils.fileToString(filePath + "configuration_rule2.txt", "UTF-8", false);
		JSONObject jsonCR = new JSONObject(cr);
		jsonCR.put("name", RandomStringUtils.randomAlphabetic(5));
		jsonCR.put("minAppVersion", "10.5");
		JSONObject jjCR = new JSONObject();
		jjCR.put("ruleString", " (context.streams != undefined && context.streams.video_played != undefined && context.streams.video_played.adsTotalNumberOfSession == \"10\")");
		jsonCR.put("rule", jjCR);
		String configuration =  "{ \"text\" :  context.streams.video_played.adsTotalNumberOfSession	}" ;		
		jsonCR.put("configuration", configuration);
		String response =  f.addFeature(seasonID, jsonCR.toString(), featureID2, sessionToken);
		Assert.assertFalse(response.contains("error"), "Can't create configuration rule: " + response);


	}
	
	@Test (dependsOnMethods="updateStreamAddField", description="Update  feature and CR to use the new field")
	private void updateFeatureWithNewField() throws Exception{
	
		String feature = f.getFeature(featureID, sessionToken);
		JSONObject json = new JSONObject(feature);
		json.put("name", RandomStringUtils.randomAlphabetic(5));
		json.put("minAppVersion", "10.5");
		JSONObject jj = new JSONObject();
		jj.put("ruleString", " (context.streams != undefined && context.streams.video_played != undefined && context.streams.video_played.adsTotalNumberOfSession == \"10\")");
		json.put("rule", jj);
		featureID =  f.updateFeature(seasonID, featureID, json.toString(), sessionToken);
		Assert.assertFalse(featureID.contains("error"), "Can't update feature: " + featureID);
		
		String cr = f.getFeature(configID, sessionToken);
		JSONObject jsonCR = new JSONObject(cr);
		jsonCR.put("name", RandomStringUtils.randomAlphabetic(5));
		jsonCR.put("minAppVersion", "10.5");
		JSONObject jjCR = new JSONObject();
		jjCR.put("ruleString", " (context.streams != undefined && context.streams.video_played != undefined && context.streams.video_played.adsTotalNumberOfSession == \"10\")");
		jsonCR.put("rule", jjCR);
		String configuration =  "{ \"text\" :  context.streams.video_played.adsTotalNumberOfSession	}" ;		
		jsonCR.put("configuration", configuration);
		String response =  f.updateFeature(seasonID, configID, jsonCR.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Can't update configuration rule: " + response);


	}
	
	
	@Test (dependsOnMethods="updateFeatureWithNewField",  description="update regular schema")
	public void updateInputSchema() {
	    try {
	        String sch = schema.getInputSchema(seasonID, sessionToken);
	        JSONObject jsonSchema = new JSONObject(sch);
	        String schemaBody = FileUtils.fileToString(filePath + "inputSchemas/Android_schema_v40.json", "UTF-8", false);
	        

	        String results =  schema.validateSchema(seasonID, schemaBody, sessionToken);
	        Assert.assertFalse(results.contains("error"), "Input schema was not validated");
	        
	        JSONObject jsonValidateRes = new JSONObject(results);
	        Assert.assertTrue((jsonValidateRes.getJSONArray("brokenConfigurations").size()==0 &&
	        		jsonValidateRes.getJSONArray("brokenExperiments").size()==0 &&
	        		jsonValidateRes.getJSONArray("brokenRules").size()==0 &&
	        		jsonValidateRes.getJSONArray("brokenVariants").size()==0
	        		), "Input Schema validation failed");

	        jsonSchema.put("inputSchema", new JSONObject(schemaBody));
	        results =  schema.updateInputSchema(seasonID, jsonSchema.toString(), sessionToken);
	        Assert.assertFalse(results.contains("error"), "Input schema was not updated");
	    }catch (Exception e){
	        Assert.fail(e.getMessage());
	    }
	}
	
	
	
	@AfterTest
	public void reset(){
		baseUtils.reset(productID, sessionToken);
	}

	

}
