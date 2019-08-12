package tests.restapi.upgradeV2V2_5;

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
import tests.com.ibm.qautils.JSONUtils;
import tests.restapi.AirlockUtils;
import tests.restapi.FeaturesRestApi;
import tests.restapi.ProductsRestApi;
import tests.restapi.SeasonsRestApi;

public class ImportFeatureCheckRolloutPercentageBitmap {
	protected String seasonID;
	protected String seasonID2;
	protected String productID;
	protected String productID2;
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


	@BeforeClass
	@Parameters({"url", "analyticsUrl", "translationsUrl", "configPath", "sessionToken", "userName", "userPassword", "appName", "productsToDeleteFile", "productId", "seasonId"})
	public void init(String url, String analyticsUrl, String translationsUrl, String configPath, String sToken, String userName, String userPassword, String appName, String productsToDeleteFile, String productId, String seasonId) throws Exception{
		m_url = url;
		filePath = configPath;
		p = new ProductsRestApi();
		p.setURL(m_url);
		f = new FeaturesRestApi();
		f.setURL(m_url);
		s = new SeasonsRestApi();
		s.setURL(m_url);
	    
		baseUtils = new AirlockUtils(url, analyticsUrl, translationsUrl, configPath, sToken, userName, userPassword, appName, productsToDeleteFile);
		sessionToken = baseUtils.sessionToken;

		productID = productId;
		seasonID = seasonId;
		
	}
	
	/*
	- create a feature in old season
	- copy inside old season
	- copy from old to new season
	- copy from new to old season
		
	 */
	
	@Test (description="Import a feature inside the same old season")
	public void importFeatureInsideOldSeason() throws IOException, JSONException{
		String feature1 = FileUtils.fileToString(filePath + "feature1.txt", "UTF-8", false);
		JSONObject json = new JSONObject(feature1);
		json.put("name", "a"+RandomStringUtils.randomAlphanumeric(5));
		featureID1 = f.addFeature(seasonID, json.toString(), "ROOT", sessionToken);
		Assert.assertFalse(featureID1.contains("error"), "Feature was not added to the season");
		
		String rootId = f.getRootId(seasonID, sessionToken);
		String featureToImport = f.getFeature(featureID1, sessionToken);
		String response = f.importFeature(featureToImport, rootId, "ACT", null, "suffix1", true, sessionToken);
		Assert.assertTrue(response.contains("newSubTreeId"), "Feature was not copied: " + response);
		
		JSONObject result = new JSONObject(response);
		String newFeature = f.getFeature(result.getString("newSubTreeId"), sessionToken);
		JSONObject newJson = new JSONObject(newFeature);
		Assert.assertTrue(newJson.containsKey("rolloutPercentageBitmap"), "rolloutPercentageBitmap was not found after copying a feauture inside old season");
	}
	

	@Test (dependsOnMethods ="importFeatureInsideOldSeason",  description="Copy a feature from old season to new season")
	public void importFeatureFromOldToNewSeason() throws IOException, JSONException{
		String product = FileUtils.fileToString(filePath + "product1.txt", "UTF-8", false);
		product = JSONUtils.generateUniqueString(product, 5, "codeIdentifier");
		product = JSONUtils.generateUniqueString(product, 8, "name");
		productID2 = p.addProduct(product, sessionToken);
		baseUtils.printProductToFile(productID2);		
		
		JSONObject sJson = new JSONObject();
		sJson.put("minVersion", "2.0");
		
		seasonID2 = s.addSeason(productID2, sJson.toString(), sessionToken);
		Assert.assertFalse(seasonID2.contains("error"), "The second season was not created: " + seasonID2);

		//copy to new season root
		String rootId = f.getRootId(seasonID2, sessionToken);
		String featureToImport = f.getFeature(featureID1, sessionToken);
		String response = f.importFeature(featureToImport, rootId, "ACT", null, "suffix2", true, sessionToken);
		Assert.assertTrue(response.contains("newSubTreeId"), "Feature was not copied: " + response);
		
		JSONObject result = new JSONObject(response);
		String newFeature = f.getFeature(result.getString("newSubTreeId"), sessionToken);
		JSONObject newJson = new JSONObject(newFeature);
		
		//bitmap should will not be preserved in the new season copied from old season, but if percentage is updated, bitmap shouldn't cange
		Assert.assertFalse(newJson.containsKey("rolloutPercentageBitmap"), "rolloutPercentageBitmap was not found after copying a feauture from old to new season");
		Assert.assertTrue(newJson.getDouble("rolloutPercentage")> 0, "rolloutPercentage is not of type double after copying a feauture from old to new season");
	}
	
	@Test (dependsOnMethods ="importFeatureFromOldToNewSeason",  description="Copy a feature from new season to old season with incorrect percentage")
	public void importFeatureFromNewToOldSeasonInvalidPercentage() throws IOException, JSONException{
		JSONArray features = f.getFeaturesBySeason(seasonID2, sessionToken);
		String featureToCopyID = features.getJSONObject(0).getString("uniqueId");
		String featureToCopy = f.getFeature(featureToCopyID, sessionToken);
		JSONObject jsonF = new JSONObject(featureToCopy);
		jsonF.put("rolloutPercentage", 54.4);
		String updateResp = f.updateFeature(seasonID2, featureToCopyID, jsonF.toString(), sessionToken);
		Assert.assertFalse(updateResp.contains("error"), "Feature was updated to the new rolloutPercentage: " + updateResp);
		
		//copy to old season root with incorrect 
		String rootId = f.getRootId(seasonID, sessionToken);
		String featureToImport = f.getFeature(featureToCopyID, sessionToken);
		String response = f.importFeature(featureToImport, rootId, "ACT", null, "suffix3", true, sessionToken);
		Assert.assertTrue(response.contains("error"), "Feature was copied with incorrect rolloutPercentage: " + response);
		
	}
	
	@Test (dependsOnMethods ="importFeatureFromNewToOldSeasonInvalidPercentage",  description="Copy a feature from new season to old season")
	public void importFeatureFromNewToOldSeason() throws IOException, JSONException{
		JSONArray features = f.getFeaturesBySeason(seasonID2, sessionToken);
		String featureToCopyID = features.getJSONObject(0).getString("uniqueId");
		String featureToCopy = f.getFeature(featureToCopyID, sessionToken);
		JSONObject jsonF = new JSONObject(featureToCopy);
		jsonF.put("rolloutPercentage", 55);
		String updateResp = f.updateFeature(seasonID2, featureToCopyID, jsonF.toString(), sessionToken);
		Assert.assertFalse(updateResp.contains("error"), "Feature was updated to the new rolloutPercentage: " + updateResp);
		
		//copy to old season root 
		String rootId = f.getRootId(seasonID, sessionToken);
		String featureToImport = f.getFeature(featureToCopyID, sessionToken);
		String response = f.importFeature(featureToImport, rootId, "ACT", null, "suffix3", true, sessionToken);
		Assert.assertTrue(response.contains("newSubTreeId"), "Feature was not copied: " + response);
		
		JSONObject result = new JSONObject(response);
		String newFeature = f.getFeature(result.getString("newSubTreeId"), sessionToken);
		JSONObject newJson = new JSONObject(newFeature);
		Assert.assertTrue(newJson.containsKey("rolloutPercentageBitmap"), "rolloutPercentageBitmap was not found after copying a feauture from new to old season");
		Assert.assertTrue(newJson.getInt("rolloutPercentage")>0, "rolloutPercentage is not of type int after copying a feauture from new to old season");
	}
	
	@AfterTest
	private void reset(){
		baseUtils.reset(productID, sessionToken);
	}
}