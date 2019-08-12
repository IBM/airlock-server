package tests.restapi.copy_import.copy;

import java.io.IOException;






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
import tests.restapi.FeaturesRestApi;
import tests.restapi.ProductsRestApi;
import tests.restapi.SeasonsRestApi;

public class CopyFeatureScenarios {
	protected String seasonID1;
	protected String seasonID2;
	protected String seasonID3;
	protected String productID;
	protected String featureID1;
	protected String filePath;
	protected String m_url;
	protected ProductsRestApi p;
	protected FeaturesRestApi f;
	protected SeasonsRestApi s;
	private String sessionToken = "";
	protected AirlockUtils baseUtils;
	private String mixId = "";
	private String srcBranchID;
	private String destBranchID;
	private boolean runOnMaster;

	@BeforeClass
	@Parameters({"url", "analyticsUrl", "translationsUrl", "configPath", "sessionToken", "userName", "userPassword", "appName", "productsToDeleteFile", "runOnMaster"})
	public void init(String url, String analyticsUrl, String translationsUrl, String configPath, String sToken, String userName, String userPassword, String appName, String productsToDeleteFile, Boolean runOnMaster) throws Exception{
		m_url = url;
		filePath = configPath;
		p = new ProductsRestApi();
		p.setURL(m_url);
		s = new SeasonsRestApi();
		s.setURL(m_url);
		f = new FeaturesRestApi();
		f.setURL(m_url);
	    
		baseUtils = new AirlockUtils(url, analyticsUrl, translationsUrl, configPath, sToken, userName, userPassword, appName, productsToDeleteFile);
		sessionToken = baseUtils.sessionToken;
		productID = baseUtils.createProduct();
		baseUtils.printProductToFile(productID);
		seasonID1 = baseUtils.createSeason(productID);
		
	}
	
	/*
		create 3 seasons S1, S2, S3
		add  features to each season
		copy feature1 from S2 to S1
		for all 3 seasons check: get features, add feature, update feature, delete feature
		
	 */
	
	@Test (description="Create 3 seasons with features: mix->f1, f2; cr1 under f2")
	public void createSeasons() throws IOException, JSONException{
		
		String featureMix = FileUtils.fileToString(filePath + "feature-mutual.txt", "UTF-8", false);
		mixId = f.addFeature(seasonID1, featureMix, "ROOT", sessionToken);
		Assert.assertFalse(mixId.contains("error"), "Feature was not added to the season" + mixId);

		String feature1 = FileUtils.fileToString(filePath + "feature1.txt", "UTF-8", false);
		String featureID1 = f.addFeature(seasonID1, feature1, mixId, sessionToken);
		Assert.assertFalse(featureID1.contains("error"), "Feature was not added to the season " + featureID1);

		String feature2 = FileUtils.fileToString(filePath + "feature2.txt", "UTF-8", false);
		String featureID2 = f.addFeature(seasonID1, feature2, mixId, sessionToken);
		Assert.assertFalse(featureID2.contains("error"), "Feature was not added to the season " + featureID2);

		String configuration = FileUtils.fileToString(filePath + "configuration_rule1.txt", "UTF-8", false);
		String configID = f.addFeature(seasonID1, configuration, featureID2, sessionToken);
		Assert.assertFalse(configID.contains("error"), "Feature was not added to the season");
		
		//create season2
		JSONObject sJson = new JSONObject();
		sJson.put("minVersion", "2.0");		
		seasonID2 = s.addSeason(productID, sJson.toString(), sessionToken);
		Assert.assertFalse(seasonID2.contains("error"), "The second season was not created: " + seasonID2);
		
		
		//create season2
		sJson = new JSONObject();
		sJson.put("minVersion", "5.0");		
		seasonID3 = s.addSeason(productID, sJson.toString(), sessionToken);
		Assert.assertFalse(seasonID3.contains("error"), "The second season was not created: " + seasonID3);
	}
	
	@Test (dependsOnMethods="createSeasons", description="Copy mix from season2 to season1 under root")
	public void copyFeature() throws IOException, JSONException{
		
		String rootId = f.getRootId(seasonID1, sessionToken);
		
		String response = f.copyFeature(mixId, rootId, "ACT", null, "suffix1", sessionToken);
		Assert.assertTrue(response.contains("newSubTreeId"), "Feature was not copied: " + response);
		
	}
	
	@Test(dependsOnMethods="copyFeature", description="Validate all seasons")
	public void checkFeaturesInSeasons() throws Exception{
			
			//get features in all seasons
		 	JSONArray features = f.getFeaturesBySeason(seasonID1, sessionToken);
		 	Assert.assertTrue(features.size()==2, "Incorrect number of features in season1");

		 	features = f.getFeaturesBySeason(seasonID2, sessionToken);
		 	Assert.assertTrue(features.size()==1, "Incorrect number of features in season2");

		 	features = f.getFeaturesBySeason(seasonID3, sessionToken);
		 	Assert.assertTrue(features.size()==1, "Incorrect number of features in season3");
		 	
		 	//Add feature to each season
		 	String feature3 = FileUtils.fileToString(filePath + "feature3.txt", "UTF-8", false);
		 	String f3S1Id = f.addFeature(seasonID1, feature3, "ROOT", sessionToken);
		 	Assert.assertFalse(f3S1Id.contains("error"), "Feature was not added to season1 " + f3S1Id);
		 	String f3S2Id = f.addFeature(seasonID2, feature3, "ROOT", sessionToken);
		 	Assert.assertFalse(f3S2Id.contains("error"), "Feature was not added to season1 " + f3S2Id);
		 	String f3S3Id = f.addFeature(seasonID3, feature3, "ROOT", sessionToken);
		 	Assert.assertFalse(f3S3Id.contains("error"), "Feature was not added to season1 " + f3S3Id);
		 	
		 	//Update feature in each season
		 	String f3S1 = f.getFeature(f3S1Id, sessionToken);
		 	JSONObject f3S1Json = new JSONObject(f3S1);
		 	f3S1Json.put("name", "feature3 in season1");
		 	String response = f.updateFeature(seasonID1, f3S1Id, f3S1Json.toString(), sessionToken);
		 	Assert.assertFalse(response.contains("error"), "Feature was not updated in season1 " + response);
		 	
		 	String f3S2 = f.getFeature(f3S2Id, sessionToken);
		 	JSONObject f3S2Json = new JSONObject(f3S2);
		 	f3S2Json.put("name", "feature3 in season2");
		 	response = f.updateFeature(seasonID2, f3S2Id, f3S2Json.toString(), sessionToken);
		 	Assert.assertFalse(response.contains("error"), "Feature was not updated in season2 " + response);
		 	
		 	String f3S3 = f.getFeature(f3S3Id, sessionToken);
		 	JSONObject f3S3Json = new JSONObject(f3S3);
		 	f3S3Json.put("name", "feature3 in season3");
		 	response = f.updateFeature(seasonID3, f3S3Id, f3S3Json.toString(), sessionToken);
		 	Assert.assertFalse(response.contains("error"), "Feature was not updated in season3 " + response);
		 	
		 	//delete feature in all seasons
		 	int respCode = f.deleteFeature(f3S1Id, sessionToken);
		 	Assert.assertTrue(respCode==200, "Feature3 was not deleted from season1");
		 	respCode = f.deleteFeature(f3S2Id, sessionToken);
		 	Assert.assertTrue(respCode==200, "Feature3 was not deleted from season2");
		 	respCode = f.deleteFeature(f3S3Id, sessionToken);
		 	Assert.assertTrue(respCode==200, "Feature3 was not deleted from season3");
		 	
			//get features in all seasons
		 	features = f.getFeaturesBySeason(seasonID1, sessionToken);
		 	Assert.assertTrue(features.size()==2, "Incorrect number of features in season1");

		 	features = f.getFeaturesBySeason(seasonID2, sessionToken);
		 	Assert.assertTrue(features.size()==1, "Incorrect number of features in season2");

		 	features = f.getFeaturesBySeason(seasonID3, sessionToken);
		 	Assert.assertTrue(features.size()==1, "Incorrect number of features in season3");

	}
	
	@AfterTest
	private void reset(){
		baseUtils.reset(productID, sessionToken);
	}

}