package tests.restapi.copy_import.copy;

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
import tests.com.ibm.qautils.JSONUtils;
import tests.restapi.AirlockUtils;
import tests.restapi.BranchesRestApi;
import tests.restapi.FeaturesRestApi;
import tests.restapi.InputSchemaRestApi;
import tests.restapi.ProductsRestApi;
import tests.restapi.SeasonsRestApi;
import tests.restapi.StreamsRestApi;

public class CopyConfigurationWithStreamDifferentProduct {
	protected String seasonID;
	private String seasonID2;
	protected String productID;
	private String productID2;
	protected String featureID1;
	protected String featureID2;
	private String mixId;
	private String mixConfigID;
	protected String configID;
	private String configID2;
	protected String filePath;
	protected String m_url;
	protected ProductsRestApi p;
	protected FeaturesRestApi f;
	protected SeasonsRestApi s;
	private String sessionToken = "";
	protected AirlockUtils baseUtils;
	private String srcBranchID;
	private String destBranchID;
	private boolean runOnMaster;
	protected StreamsRestApi streamApi;
	private InputSchemaRestApi schema;
 	private String configToCopyName;

	@BeforeClass
	@Parameters({"url", "analyticsUrl", "translationsUrl", "configPath", "sessionToken", "userName", "userPassword", "appName", "productsToDeleteFile", "runOnMaster"})
	public void init(String url, String analyticsUrl, String translationsUrl, String configPath, String sToken, String userName, String userPassword, String appName, String productsToDeleteFile, Boolean runOnMaster) throws Exception{
		m_url = url;
		filePath = configPath;
		p = new ProductsRestApi();
		p.setURL(m_url);
		f = new FeaturesRestApi();
		f.setURL(m_url);
		s = new SeasonsRestApi();
		s.setURL(m_url);
		streamApi = new StreamsRestApi();
		streamApi.setURL(url);
	    schema = new InputSchemaRestApi();
	    schema.setURL(url);	
	    
		baseUtils = new AirlockUtils(url, analyticsUrl, translationsUrl, configPath, sToken, userName, userPassword, appName, productsToDeleteFile);
		sessionToken = baseUtils.sessionToken;
		productID = baseUtils.createProduct();
		baseUtils.printProductToFile(productID);
		seasonID = baseUtils.createSeason(productID);
		try {
			if (runOnMaster) {
				srcBranchID = BranchesRestApi.MASTER;
			} else {
				srcBranchID = baseUtils.createBranchInExperiment(analyticsUrl);
			}
		}catch (Exception e){
			srcBranchID = null;
		}
		this.runOnMaster = runOnMaster;
	}
	
	/*
	Config under feature - allowed
	Config under config - allowed
	Config under mix of configs - allowed
	Config under root - not allowed
	Config under mix of features - not allowed
		
	 */

	@Test (description="add regular schema and stream")
	public void addInputSchema() throws Exception {
	        String sch = schema.getInputSchema(seasonID, sessionToken);
	        JSONObject jsonSchema = new JSONObject(sch);
	        String schemaBody = FileUtils.fileToString(filePath + "inputSchemas/Android_schema_v3.0.json", "UTF-8", false);
	        jsonSchema.put("inputSchema", new JSONObject(schemaBody));
	        String results =  schema.updateInputSchema(seasonID, jsonSchema.toString(), sessionToken);
	        Assert.assertFalse(results.contains("error"), "Input schema was not added");
	    
		String stream = FileUtils.fileToString(filePath + "streams/stream1.txt", "UTF-8", false);
		JSONObject streamJson = new JSONObject(stream);
		streamJson.put("name", "video played");
		String streamID = streamApi.createStream(seasonID, streamJson.toString(), sessionToken);
		Assert.assertFalse(streamID.contains("error"), "Stream was not created: " + streamID);

	}
	
	@Test (dependsOnMethods="addInputSchema", description="Create first season with feature and configuration. Copy season")
	public void addConfiguration() throws IOException, JSONException{
		String feature = FileUtils.fileToString(filePath + "feature1.txt", "UTF-8", false);
		String featureID1 = f.addFeatureToBranch(seasonID, srcBranchID, feature, "ROOT", sessionToken);
		Assert.assertFalse(featureID1.contains("error"), "Feature was not added to the season");
		
		String configRule = FileUtils.fileToString(filePath + "configuration_rule1.txt", "UTF-8", false);
		JSONObject json = new JSONObject(configRule);
		configToCopyName = RandomStringUtils.randomAlphabetic(5);
		json.put("name",configToCopyName );
		json.put("minAppVersion", "10.5");
		JSONObject jj = new JSONObject();
		jj.put("ruleString", " (context.streams != undefined && context.streams.video_played != undefined && context.streams.video_played.averageAdsTime == \"10\")");
		json.put("rule", jj);
		String configuration =  "{ \"text\" :  context.streams.video_played.averageAdsTime	}" ;		
		json.put("configuration", configuration);
		configID =  f.addFeatureToBranch(seasonID, srcBranchID, json.toString(), featureID1, sessionToken);
		Assert.assertFalse(configID.contains("error"), "Can't create feature: " + configID);

		
	}
	
	@Test (dependsOnMethods="addConfiguration", description="Create new product with all components")
	public void createNewProduct() throws IOException, JSONException{
		//create second product
		String product = FileUtils.fileToString(filePath + "product1.txt", "UTF-8", false);
		product = JSONUtils.generateUniqueString(product, 5, "codeIdentifier");
		product = JSONUtils.generateUniqueString(product, 8, "name");
		productID2 = p.addProduct(product, sessionToken);
		baseUtils.printProductToFile(productID2);
		
		//add season to second product
		JSONObject sJson = new JSONObject();
		sJson.put("minVersion", "2.0");		
		seasonID2 = s.addSeason(productID2, sJson.toString(), sessionToken);
		Assert.assertFalse(seasonID2.contains("error"), "The season was not created in the new product: " + seasonID2);

		try {
			if (runOnMaster) {
				destBranchID = BranchesRestApi.MASTER;
			} else {
				baseUtils.setSeasonId(seasonID2);
				destBranchID = baseUtils.addBranch("b1");
			}
		}catch (Exception e){
			destBranchID = null;
		}
		
		
		String feature1 = FileUtils.fileToString(filePath + "feature1.txt", "UTF-8", false);
		featureID2 = f.addFeatureToBranch(seasonID2, destBranchID,  feature1, "ROOT", sessionToken);
		Assert.assertFalse(featureID2.contains("error"), "Feature was not added to the season");
		
		String featureMix = FileUtils.fileToString(filePath + "feature-mutual.txt", "UTF-8", false);
		mixId = f.addFeatureToBranch(seasonID2, destBranchID, featureMix, "ROOT", sessionToken);
		Assert.assertFalse(mixId.contains("error"), "Feature was not added to the season" + mixId);

		String configuration = FileUtils.fileToString(filePath + "configuration_rule1.txt", "UTF-8", false);
		configID2 = f.addFeatureToBranch(seasonID2, destBranchID, configuration, featureID2, sessionToken);
		Assert.assertFalse(configID2.contains("error"), "Feature was not added to the season");
		JSONObject json = new JSONObject(configuration);
		json.put("name",configToCopyName );
		String response = f.addFeatureToBranch(seasonID2, destBranchID, json.toString(), featureID2, sessionToken);
		Assert.assertFalse(response.contains("error"), "Config was not copied to new product " + configID);

		String mixConfiguration = FileUtils.fileToString(filePath + "configuration_feature-mutual.txt", "UTF-8", false);
		mixConfigID = f.addFeatureToBranch(seasonID2, destBranchID, mixConfiguration, featureID2, sessionToken);
		Assert.assertFalse(mixConfigID.contains("error"), "Feature was not added to the season");

	}
	
	@Test (dependsOnMethods="createNewProduct", description="Copy configuration under feature in the new product without adding stream")
	public void copyConfigurationUnderFeatureNoStreams() throws IOException, JSONException{

		//should fail copy without suffix
		String response = f.copyItemBetweenBranches(configID, featureID2, "VALIDATE", null, null, sessionToken, srcBranchID, destBranchID);
		Assert.assertTrue(response.contains("missingFields"), "Configuration was copied with missing stream in mode VALIDATE");

		//it is allowed to copy without stream in mode ACT
		response = f.copyItemBetweenBranches(configID, featureID2, "ACT", null, "suffix1", sessionToken, srcBranchID, destBranchID);
		Assert.assertFalse(response.contains("missingFields"), "Configuration was copied with missing stream in mode ACT");

	}
	
	@Test (dependsOnMethods="copyConfigurationUnderFeatureNoStreams", description="add regular schema and stream")
	public void addInputSchemaSecondProduct() throws Exception {
	        String sch = schema.getInputSchema(seasonID2, sessionToken);
	        JSONObject jsonSchema = new JSONObject(sch);
	        String schemaBody = FileUtils.fileToString(filePath + "inputSchemas/Android_schema_v3.0.json", "UTF-8", false);
	        jsonSchema.put("inputSchema", new JSONObject(schemaBody));
	        String results =  schema.updateInputSchema(seasonID2, jsonSchema.toString(), sessionToken);
	        Assert.assertFalse(results.contains("error"), "Input schema was not added");
	    
		String stream = FileUtils.fileToString(filePath + "streams/stream1.txt", "UTF-8", false);
		JSONObject streamJson = new JSONObject(stream);
		streamJson.put("name", "video played");
		String streamID = streamApi.createStream(seasonID2, streamJson.toString(), sessionToken);
		Assert.assertFalse(streamID.contains("error"), "Stream was not created: " + streamID);

	}
	
	@Test (dependsOnMethods="addInputSchemaSecondProduct", description="Copy configuration under feature in the new product without adding stream")
	public void copyConfigurationUnderFeatureWithStreams() throws IOException, JSONException{

		//should fail copy without suffix
		String response = f.copyItemBetweenBranches(configID, featureID2, "VALIDATE", null, null, sessionToken, srcBranchID, destBranchID);
		Assert.assertFalse(response.contains("missingFields"), "Configuration was not copied");

		response = f.copyItemBetweenBranches(configID, featureID2, "ACT", null, "suffix1", sessionToken, srcBranchID, destBranchID);
		Assert.assertFalse(response.contains("missingFields"), "Configuration was not copied ");

	}
	
	
	@AfterTest
	private void reset(){
		baseUtils.reset(productID, sessionToken);
		baseUtils.reset(productID2, sessionToken);
	}
}
