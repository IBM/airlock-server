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
import tests.restapi.BranchesRestApi;
import tests.restapi.FeaturesRestApi;
import tests.restapi.InputSchemaRestApi;
import tests.restapi.StreamsRestApi;

public class StreamInBranch {
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
	private BranchesRestApi br;
	private String branchID;
	
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
		br = new BranchesRestApi();
		br.setURL(m_url);
		
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
	
	
	@Test (dependsOnMethods="createStream", description="Add feature and configuration")
	public void createFeatures() throws JSONException, IOException{
		String feature = FileUtils.fileToString(filePath + "feature1.txt", "UTF-8", false);
		featureID =  f.addFeature(seasonID, feature, "ROOT", sessionToken);
		Assert.assertFalse(featureID.contains("error"), "Can't create feature: " + featureID);
		
		String config = FileUtils.fileToString(filePath + "configuration_rule1.txt", "UTF-8", false);
		JSONObject json = new JSONObject(config);
		json.put("name", RandomStringUtils.randomAlphabetic(5));
		json.put("minAppVersion", "10.5");
		JSONObject jj = new JSONObject();
		jj.put("ruleString", " (context.streams != undefined && context.streams.video_played != undefined && context.streams.video_played.averageAdsTime == \"10\")");
		json.put("rule", jj);
		String configuration =  "{ \"text\" :  context.streams.video_played.averageAdsTime	}" ;		
		json.put("configuration", configuration);
		configID =  f.addFeature(seasonID, json.toString(), featureID, sessionToken);
		Assert.assertFalse(configID.contains("error"), "Can't create  configuration rule: " + configID);

	}
	
	
	@Test (dependsOnMethods="createFeatures", description="Create branch and checkout feature")
	public void createBranch() throws JSONException, IOException{
		String branch = FileUtils.fileToString(filePath + "experiments/branch1.txt", "UTF-8", false);
		branchID= br.createBranch(seasonID, branch, BranchesRestApi.MASTER, sessionToken);

		String response = br.checkoutFeature(branchID, featureID, sessionToken);
		Assert.assertFalse(response.contains("error"), "Feature type was not checked out: " + response);

	}
	
	@Test (dependsOnMethods="createBranch", description="Update feature with stream in branch")
	public void updateFeatureInBranch() throws JSONException, IOException{
		String config = f.getFeatureFromBranch(configID, branchID, sessionToken);
		JSONObject json = new JSONObject(config) ;
		JSONObject jj = new JSONObject();
		jj.put("ruleString", " (context.streams != undefined && context.streams.video_played != undefined && context.streams.video_played.adsNumberOfSessionPerDay == \"10\")");
		json.put("rule", jj);
		String configuration =  "{ \"text\" :  context.streams.video_played.adsNumberOfSessionPerDay	}" ;		
		json.put("configuration", configuration);
		
		String response = f.updateFeatureInBranch(seasonID, branchID, configID, json.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Configuration rule was not updated in branch: " + response);
		
	}
	
	
	@Test (dependsOnMethods="updateFeatureInBranch", description="Add new feature with stream to branch")
	public void createFeatureInBranch() throws JSONException, IOException{
		String feature = FileUtils.fileToString(filePath + "feature2.txt", "UTF-8", false);
		String featureID2 =  f.addFeatureToBranch(seasonID, branchID, feature, "ROOT", sessionToken);
		Assert.assertFalse(featureID2.contains("error"), "Can't create feature in branch: " + featureID2);
		
		String config = FileUtils.fileToString(filePath + "configuration_rule2.txt", "UTF-8", false);
		JSONObject json = new JSONObject(config);
		json.put("name", RandomStringUtils.randomAlphabetic(5));
		json.put("minAppVersion", "10.5");
		JSONObject jj = new JSONObject();
		jj.put("ruleString", " (context.streams != undefined && context.streams.video_played != undefined && context.streams.video_played.averageAdsTime == \"10\")");
		json.put("rule", jj);
		String configuration =  "{ \"text\" :  context.streams.video_played.averageAdsTime	}" ;		
		json.put("configuration", configuration);
		String configID2 =  f.addFeatureToBranch(seasonID, branchID, json.toString(), featureID2, sessionToken);
		Assert.assertFalse(configID2.contains("error"), "Can't create configuration rule in branch: " + configID2);
		
	}
	
	
	@Test (dependsOnMethods="createFeatureInBranch",  description="update regular schema")
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
	
	@Test (dependsOnMethods="updateInputSchema", description="Rename stream inuse by feature in branch")
	public void renameStreamInUseByFeatureInBranch() throws Exception{
		String stream = FileUtils.fileToString(filePath + "streams/stream1.txt", "UTF-8", false);
		JSONObject streamJson = new JSONObject(stream);
		streamJson.put("name", "myStream");
		String streamID = streamApi.createStream(seasonID, streamJson.toString(), sessionToken);
		Assert.assertFalse(streamID.contains("error"), "Stream was not created: " + streamID);
		
		String feature = FileUtils.fileToString(filePath + "feature2.txt", "UTF-8", false);
		JSONObject json = new JSONObject(feature);
		json.put("name", RandomStringUtils.randomAlphabetic(5));
		json.put("minAppVersion", "10.5");
		JSONObject jj = new JSONObject();
		jj.put("ruleString", " (context.streams != undefined && context.streams.myStream != undefined && context.streams.myStream.averageAdsTime == \"10\")");
		json.put("rule", jj);
		
		String featureID =  f.addFeatureToBranch(seasonID, branchID, json.toString(), "ROOT", sessionToken);
		Assert.assertFalse(featureID.contains("error"), "Can't create feature in branch: " + featureID);
		
		//rename stream
		stream = streamApi.getStream(streamID, sessionToken);
		Assert.assertFalse(featureID.contains("error"), "gwt stream " + stream);
		
		json = new JSONObject(stream);
		json.put("name", "changedName");
		String updatedStream = streamApi.updateStream(streamID, json.toString(), sessionToken);
		Assert.assertTrue(updatedStream.contains("error"), "can change stream name inuse by feature in branch");
		
	}
	
	@Test (dependsOnMethods="renameStreamInUseByFeatureInBranch", description="Use stream in feature in branch deep under mtx")
	public void useStreamDeepInMTXInBranch() throws Exception{
		String mtx = FileUtils.fileToString(filePath + "feature-mutual.txt", "UTF-8", false);
		String mtxId = f.addFeatureToBranch(seasonID, branchID, mtx, "ROOT", sessionToken);
		Assert.assertFalse(mtxId.contains("error"), "Can't create mtx in branch: " + featureID);
		
		String feature = FileUtils.fileToString(filePath + "feature2.txt", "UTF-8", false);
		JSONObject json = new JSONObject(feature);
		json.put("name", RandomStringUtils.randomAlphabetic(5));
		json.put("minAppVersion", "10.5");
		JSONObject jj = new JSONObject();
		jj.put("ruleString", " (context.streams != undefined && context.streams.myStream != undefined && context.streams.myStream.averageAdsTime == \"10\")");
		json.put("rule", jj);
		
		String featureID1 =  f.addFeatureToBranch(seasonID, branchID, json.toString(), mtxId, sessionToken);
		Assert.assertFalse(featureID1.contains("error"), "Can't create feature in branch under mtx: " + featureID);
		
		json.put("name", RandomStringUtils.randomAlphabetic(5));
		String featureID2 =  f.addFeatureToBranch(seasonID, branchID, json.toString(), mtxId, sessionToken);
		Assert.assertFalse(featureID2.contains("error"), "Can't create feature in branch under mtx under feature: " + featureID);	
	}
	
	
	
	
	@AfterTest
	public void reset(){
		baseUtils.reset(productID, sessionToken);
	}

	

}
