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
import tests.restapi.AirlockUtils;
import tests.restapi.BranchesRestApi;
import tests.restapi.FeaturesRestApi;
import tests.restapi.InputSchemaRestApi;
import tests.restapi.ProductsRestApi;
import tests.restapi.StreamsRestApi;


//when running with branches this is a copy with in the same branch
public class CopyConfigurationWithStreamSameSeason {
	protected String seasonID;
	protected String productID;
	protected String featureID1;
	protected String featureID2;
	protected String configID;
	protected String filePath;
	protected String m_url;
	protected ProductsRestApi p;
	protected FeaturesRestApi f;
	private String sessionToken = "";
	protected AirlockUtils baseUtils;
	private String srcBranchID;
	protected StreamsRestApi streamApi;
	private InputSchemaRestApi schema;
	

	@BeforeClass
	@Parameters({"url", "analyticsUrl", "translationsUrl", "configPath", "sessionToken", "userName", "userPassword", "appName", "productsToDeleteFile", "runOnMaster"})
	public void init(String url, String analyticsUrl, String translationsUrl, String configPath, String sToken, String userName, String userPassword, String appName, String productsToDeleteFile, Boolean runOnMaster) throws Exception{
		m_url = url;
		filePath = configPath;
		p = new ProductsRestApi();
		p.setURL(m_url);
		f = new FeaturesRestApi();
		f.setURL(m_url);

	    
		baseUtils = new AirlockUtils(url, analyticsUrl, translationsUrl, configPath, sToken, userName, userPassword, appName, productsToDeleteFile);
		sessionToken = baseUtils.sessionToken;
		productID = baseUtils.createProduct();
		baseUtils.printProductToFile(productID);
		seasonID = baseUtils.createSeason(productID);
		streamApi = new StreamsRestApi();
		streamApi.setURL(url);
	    schema = new InputSchemaRestApi();
	    schema.setURL(url);	
		
		try {
			if (runOnMaster) {
				srcBranchID = BranchesRestApi.MASTER;
			} else {
				srcBranchID = baseUtils.createBranchInExperiment(analyticsUrl);
			}
		}catch (Exception e){
			srcBranchID = null;
		}

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
	
	@Test (dependsOnMethods="addInputSchema", description="Copy configuration under another feature in the same season. First, copy without namesuffix, then copy with namesuffix")
	public void copyConfigurationUnderFeature() throws IOException, JSONException{
		String feature1 = FileUtils.fileToString(filePath + "feature1.txt", "UTF-8", false);
		featureID1 = f.addFeatureToBranch(seasonID, srcBranchID, feature1, "ROOT", sessionToken);
		Assert.assertFalse(featureID1.contains("error"), "Feature was not added to the season");
		
		
		String feature = FileUtils.fileToString(filePath + "configuration_rule1.txt", "UTF-8", false);
		JSONObject json = new JSONObject(feature);
		json.put("name", RandomStringUtils.randomAlphabetic(5));
		json.put("minAppVersion", "10.5");
		JSONObject jj = new JSONObject();
		jj.put("ruleString", " (context.streams != undefined && context.streams.video_played != undefined && context.streams.video_played.averageAdsTime == \"10\")");
		json.put("rule", jj);
		String configuration =  "{ \"text\" :  context.streams.video_played.averageAdsTime	}" ;		
		json.put("configuration", configuration);
		configID =  f.addFeatureToBranch(seasonID, srcBranchID, json.toString(), featureID1, sessionToken);
		Assert.assertFalse(configID.contains("error"), "Can't create feature: " + configID);
				
		String feature2 = FileUtils.fileToString(filePath + "feature2.txt", "UTF-8", false);
		featureID2 = f.addFeatureToBranch(seasonID, srcBranchID, feature2, "ROOT", sessionToken);
		Assert.assertFalse(featureID2.contains("error"), "Feature was not added to the season");
		
		//should fail copy without suffix
		String response = f.copyItemBetweenBranches(configID, featureID2, "ACT", null, null, sessionToken, srcBranchID, srcBranchID);
		Assert.assertTrue(response.contains("illegalName"), "Configuration was copied with existing name ");

		
		response = f.copyItemBetweenBranches(configID, featureID2, "ACT", null, "suffix1", sessionToken, srcBranchID, srcBranchID);
		Assert.assertTrue(response.contains("newSubTreeId"), "Configuration was not copied: " + response);
		
		JSONObject result = new JSONObject(response);
		String newFeature = f.getFeatureFromBranch(result.getString("newSubTreeId"), srcBranchID, sessionToken);
		String oldFeature = f.getFeatureFromBranch(configID, srcBranchID, sessionToken);
		Assert.assertTrue(f.jsonObjsAreEqual(new JSONObject(newFeature), new JSONObject(oldFeature)));

	}
		
	
	@Test (dependsOnMethods="copyConfigurationUnderFeature", description="Copy configuration under itself in the same season. First, copy without namesuffix, then copy with namesuffix")
	public void copyConfigurationUnderItself() throws IOException, JSONException{
		//should fail copy without suffix

		String response = f.copyItemBetweenBranches(configID, configID, "ACT", null, null, sessionToken, srcBranchID, srcBranchID);
		Assert.assertTrue(response.contains("illegalName"), "Configuraiton was copied with existing name ");

		response = f.copyItemBetweenBranches(configID, configID, "ACT", null, "suffix4", sessionToken, srcBranchID, srcBranchID);
		Assert.assertTrue(response.contains("newSubTreeId"), "Configuraiton was not copied: " + response);
		
		JSONObject result = new JSONObject(response);
		String newFeature = f.getFeatureFromBranch(result.getString("newSubTreeId"), srcBranchID, sessionToken);
		JSONObject oldFeature = new JSONObject(f.getFeatureFromBranch(configID, srcBranchID, sessionToken));
		Assert.assertTrue(f.jsonObjsAreEqual(new JSONObject(newFeature), oldFeature.getJSONArray("configurationRules").getJSONObject(0)));

	}
	
	@Test (dependsOnMethods="copyConfigurationUnderItself", description="Copy configuration under configuration in the same feature in the same season. First, copy without namesuffix, then copy with namesuffix")
	public void copyConfigurationUnderConfiguration() throws IOException, JSONException{
		String configuration = FileUtils.fileToString(filePath + "configuration_rule2.txt", "UTF-8", false);
		String configID2 = f.addFeatureToBranch(seasonID, srcBranchID, configuration, featureID1, sessionToken);
		Assert.assertFalse(configID.contains("error"), "Configuraiton was not added to the season");
				
		//should fail copy without suffix
		String response = f.copyItemBetweenBranches(configID, configID2, "ACT", null, null, sessionToken, srcBranchID, srcBranchID);
		Assert.assertTrue(response.contains("illegalName"), "Configuraiton was copied with existing name ");
		
		response = f.copyItemBetweenBranches(configID, configID2, "ACT", null, "suffix5", sessionToken, srcBranchID, srcBranchID);
		Assert.assertTrue(response.contains("newSubTreeId"), "Configuraiton was not copied: " + response);
		
		JSONObject result = new JSONObject(response);
		String newFeature = f.getFeatureFromBranch(result.getString("newSubTreeId"), srcBranchID, sessionToken);
		String oldFeature = f.getFeatureFromBranch(configID, srcBranchID, sessionToken);
		Assert.assertTrue(f.jsonObjsAreEqual(new JSONObject(newFeature), new JSONObject(oldFeature)));

	}

	@Test (dependsOnMethods="copyConfigurationUnderConfiguration", description="Copy configuration under mix configuration in the same season. First, copy without namesuffix, then copy with namesuffix")
	public void copySingleFeatureUnderMixConfiguration() throws IOException, JSONException{
		String configuration = FileUtils.fileToString(filePath + "configuration_feature-mutual.txt", "UTF-8", false);
		String mixConfigID = f.addFeatureToBranch(seasonID, srcBranchID, configuration, featureID2, sessionToken);
		Assert.assertFalse(mixConfigID.contains("error"), "Feature was not added to the season");
			
		String response = f.copyItemBetweenBranches(configID, mixConfigID, "ACT", null, null, sessionToken, srcBranchID, srcBranchID);
		Assert.assertTrue(response.contains("illegalName"), "Configuraiton was copied with existing name ");
		
		response = f.copyItemBetweenBranches(configID, mixConfigID, "ACT", null, "suffix6", sessionToken, srcBranchID, srcBranchID);
		Assert.assertTrue(response.contains("newSubTreeId"), "Configuraiton was not copied: " + response);
		
		JSONObject result = new JSONObject(response);
		String newFeature = f.getFeatureFromBranch(result.getString("newSubTreeId"), srcBranchID, sessionToken);
		String oldFeature = f.getFeatureFromBranch(configID, srcBranchID, sessionToken);
		Assert.assertTrue(f.jsonObjsAreEqual(new JSONObject(newFeature), new JSONObject(oldFeature)));

	}
	
	
	@AfterTest
	private void reset(){
		baseUtils.reset(productID, sessionToken);
	}
}