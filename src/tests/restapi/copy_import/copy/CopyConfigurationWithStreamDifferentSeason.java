package tests.restapi.copy_import.copy;

import java.io.IOException;





import org.apache.commons.lang3.RandomStringUtils;
import org.apache.wink.json4j.JSONArray;
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
import tests.restapi.SeasonsRestApi;
import tests.restapi.StreamsRestApi;

//when running in branches: we are not testing between 2 season of the same product but between 2 branches on the same season
public class CopyConfigurationWithStreamDifferentSeason {
	protected String seasonID;
	private String seasonID2;
	protected String productID;
	protected String featureID1;
	protected String featureID2;
	protected String configID;
	protected String filePath;
	protected String m_url;
	protected ProductsRestApi p;
	protected FeaturesRestApi f;
	protected SeasonsRestApi s;
	private String sessionToken = "";
	protected AirlockUtils baseUtils;
	private String configIDSeason2;
	private String srcBranchID;
	private String destBranchID;
	private boolean runOnMaster;
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
	public void copySeason() throws IOException, JSONException{
		String feature = FileUtils.fileToString(filePath + "feature1.txt", "UTF-8", false);
		String featureID1 = f.addFeatureToBranch(seasonID, srcBranchID, feature, "ROOT", sessionToken);
		Assert.assertFalse(featureID1.contains("error"), "Feature was not added to the season");
		
		String configRule = FileUtils.fileToString(filePath + "configuration_rule1.txt", "UTF-8", false);
		JSONObject json = new JSONObject(configRule);
		json.put("name", RandomStringUtils.randomAlphabetic(5));
		json.put("minAppVersion", "10.5");
		JSONObject jj = new JSONObject();
		jj.put("ruleString", " (context.streams != undefined && context.streams.video_played != undefined && context.streams.video_played.averageAdsTime == \"10\")");
		json.put("rule", jj);
		String configuration =  "{ \"text\" :  context.streams.video_played.averageAdsTime	}" ;		
		json.put("configuration", configuration);
		configID =  f.addFeatureToBranch(seasonID,srcBranchID, json.toString(), featureID1, sessionToken);
		Assert.assertFalse(configID.contains("error"), "Can't create feature: " + configID);
		
		

		//when running in branches: we are not testing between 2 season of the same product but between 2 branches on the same season
		try {
			if (runOnMaster) {
				JSONObject sJson = new JSONObject();
				sJson.put("minVersion", "2.0");
				
				seasonID2 = s.addSeason(productID, sJson.toString(), sessionToken);
				Assert.assertFalse(seasonID2.contains("error"), "The second season was not created: " + seasonID2);
				
				destBranchID = BranchesRestApi.MASTER;
			} else {
				seasonID2 = seasonID;
				baseUtils.setSeasonId(seasonID2);
				destBranchID = baseUtils.addBranchFromBranch("b2", srcBranchID,seasonID);
			}
		}catch (Exception e){
			destBranchID = null;
		}

		
	}
	
	@Test(dependsOnMethods="copySeason", description="Parse new season ids")
	public void getNewFeaturesIds() throws Exception{
		 JSONArray features = f.getFeaturesBySeasonFromBranch(seasonID2, destBranchID, sessionToken);
		 configIDSeason2 = 	features.getJSONObject(0).getJSONArray("configurationRules").getJSONObject(0).getString("uniqueId"); 	
	}
	
	
	@Test (dependsOnMethods="getNewFeaturesIds", description="Copy configuration under feature in the new season. First, copy without namesuffix, then copy with namesuffix")
	public void copyConfigurationUnderFeature() throws IOException, JSONException{
		String feature = FileUtils.fileToString(filePath + "feature2.txt", "UTF-8", false);
		String featureID = f.addFeatureToBranch(seasonID2, destBranchID, feature, "ROOT", sessionToken);
		Assert.assertFalse(featureID.contains("error"), "Feature was not added to season" + featureID);

		//should fail copy without suffix
		String response = f.copyItemBetweenBranches(configID, featureID, "ACT", null, null, sessionToken, srcBranchID, destBranchID);
		Assert.assertTrue(response.contains("illegalName"), "Configuration was copied with existing name ");

		response = f.copyItemBetweenBranches(configID, featureID, "ACT", null, "suffix1", sessionToken, srcBranchID, destBranchID);
		Assert.assertTrue(response.contains("newSubTreeId"), "Configuration was not copied: " + response);
		
		JSONObject result = new JSONObject(response);
		String newFeature = f.getFeatureFromBranch(result.getString("newSubTreeId"), destBranchID, sessionToken);
		String oldFeature = f.getFeatureFromBranch(configID, srcBranchID, sessionToken);
		Assert.assertTrue(f.jsonObjsAreEqual(new JSONObject(newFeature), new JSONObject(oldFeature)));

	}
	
	@Test (dependsOnMethods="copyConfigurationUnderFeature", description="Copy configuration under mix feature in the new season. First, copy without namesuffix, then copy with namesuffix")
	public void copyConfigurationUnderMixFeature() throws IOException{
		String featureMix = FileUtils.fileToString(filePath + "feature-mutual.txt", "UTF-8", false);
		String mixId = f.addFeatureToBranch(seasonID2, destBranchID, featureMix, "ROOT", sessionToken);
		Assert.assertFalse(mixId.contains("error"), "Feature was not added to the season" + mixId);
						
		String response = f.copyItemBetweenBranches(configID, mixId, "ACT", null, null, sessionToken, srcBranchID, destBranchID);
		Assert.assertTrue(response.contains("illegalName"), "Configuration was copied with existing name ");

		response = f.copyItemBetweenBranches(configID, mixId, "ACT", null, "suffix2", sessionToken, srcBranchID, destBranchID);
		Assert.assertTrue(response.contains("error"), "Configuraiton was copied under features mix ");
	}
	
	@Test (dependsOnMethods="copyConfigurationUnderMixFeature", description="Copy configuration under root in the new season. First, copy without namesuffix, then copy with namesuffix")
	public void copyConfigurationUnderRoot() throws IOException{
		String rootId = f.getRootId(seasonID2, sessionToken);
		
		String response = f.copyItemBetweenBranches(configID, rootId, "ACT", null, null, sessionToken, srcBranchID, destBranchID);
		Assert.assertTrue(response.contains("illegalName"), "Configuration was copied with existing name  ");
		
		response = f.copyItemBetweenBranches(configID, rootId, "ACT", null, "suffix3", sessionToken, srcBranchID, destBranchID);
		Assert.assertTrue(response.contains("error"), "Configuration was copied under root ");
	}
	
	
	@Test (dependsOnMethods="copyConfigurationUnderRoot", description="Copy configuration under itself in the new season. First, copy without namesuffix, then copy with namesuffix")
	public void copyConfigurationUnderItself() throws IOException, JSONException{
		//should fail copy without suffix

		String response = f.copyItemBetweenBranches(configID, configIDSeason2, "ACT", null, null, sessionToken, srcBranchID, destBranchID);
		Assert.assertTrue(response.contains("illegalName"), "Configuration was copied with existing name  ");
		
		response = f.copyItemBetweenBranches(configID, configIDSeason2, "ACT", null, "suffix4", sessionToken, srcBranchID, destBranchID);
		Assert.assertTrue(response.contains("newSubTreeId"), "Configuration was not copied: " + response);
		
		JSONObject result = new JSONObject(response);
		String newFeature = f.getFeatureFromBranch(result.getString("newSubTreeId"), destBranchID, sessionToken);
		JSONObject oldFeature = new JSONObject(f.getFeatureFromBranch(configIDSeason2, destBranchID, sessionToken));
		Assert.assertTrue(f.jsonObjsAreEqual(new JSONObject(newFeature), oldFeature.getJSONArray("configurationRules").getJSONObject(0)));

	}
	
	@Test (dependsOnMethods="copyConfigurationUnderItself", description="Copy configuration under configuration in the new season. First, copy without namesuffix, then copy with namesuffix")
	public void copyConfigurationUnderConfiguration() throws IOException, JSONException{
		String feature = FileUtils.fileToString(filePath + "feature3.txt", "UTF-8", false);
		featureID2 = f.addFeatureToBranch(seasonID2, destBranchID, feature, "ROOT", sessionToken);
		Assert.assertFalse(featureID2.contains("error"), "Feature was not added to season" + featureID2);
		
		String configuration = FileUtils.fileToString(filePath + "configuration_rule2.txt", "UTF-8", false);
		String configID2 = f.addFeatureToBranch(seasonID2, destBranchID, configuration, featureID2, sessionToken);
		Assert.assertFalse(configID.contains("error"), "Feature was not added to the season");

		String response = f.copyItemBetweenBranches(configID, configID2, "ACT", null, null, sessionToken, srcBranchID, destBranchID);
		Assert.assertTrue(response.contains("illegalName"), "Configuration was copied with existing name  ");

		response = f.copyItemBetweenBranches(configID, configID2, "ACT", null, "suffix5", sessionToken, srcBranchID, destBranchID);
		Assert.assertTrue(response.contains("newSubTreeId"), "Configuration was not copied: " + response);
		
		JSONObject result = new JSONObject(response);
		String newFeature = f.getFeatureFromBranch(result.getString("newSubTreeId"), destBranchID, sessionToken);
		String oldFeature = f.getFeatureFromBranch(configID, srcBranchID, sessionToken);
		Assert.assertTrue(f.jsonObjsAreEqual(new JSONObject(newFeature), new JSONObject(oldFeature)));

	}
	
	@Test (dependsOnMethods="copyConfigurationUnderConfiguration", description="Copy configuration under mix configuration in the new season. First, copy without namesuffix, then copy with namesuffix")
	public void copyConfigurationUnderMixConfiguration() throws IOException, JSONException{
		String configuration = FileUtils.fileToString(filePath + "configuration_feature-mutual.txt", "UTF-8", false);
		String mixConfigID = f.addFeatureToBranch(seasonID2, destBranchID, configuration, featureID2, sessionToken);
		Assert.assertFalse(mixConfigID.contains("error"), "Feature was not added to the season");
			
		String response = f.copyItemBetweenBranches(configID, mixConfigID, "ACT", null, null, sessionToken, srcBranchID, destBranchID);
		Assert.assertTrue(response.contains("illegalName"), "Configuration was copied with existing name  ");

		response = f.copyItemBetweenBranches(configID, mixConfigID, "ACT", null, "suffix6", sessionToken, srcBranchID, destBranchID);
		Assert.assertTrue(response.contains("newSubTreeId"), "Configuration was not copied: " + response);
		
		JSONObject result = new JSONObject(response);
		String newFeature = f.getFeatureFromBranch(result.getString("newSubTreeId"), destBranchID, sessionToken);
		String oldFeature = f.getFeatureFromBranch(configID, srcBranchID, sessionToken);
		Assert.assertTrue(f.jsonObjsAreEqual(new JSONObject(newFeature), new JSONObject(oldFeature)));
	}
	
	
	@AfterTest
	private void reset(){
		baseUtils.reset(productID, sessionToken);
	}
}