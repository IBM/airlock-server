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
import tests.restapi.BranchesRestApi;
import tests.restapi.FeaturesRestApi;
import tests.restapi.ProductsRestApi;
import tests.restapi.SeasonsRestApi;

public class CopyMIXFeatureDifferentSeason {
	protected String seasonID;
	protected String seasonID2;
	protected String productID;
	protected String featureID1;
	protected String featureID2;
	protected String mixID1;
	protected String filePath;
	protected String m_url;
	protected ProductsRestApi p;
	protected FeaturesRestApi f;
	protected SeasonsRestApi s;
	private BranchesRestApi br ;	
	private String sessionToken = "";
	protected AirlockUtils baseUtils;
	
	//in new season
	private String rootId;
	private String featureID2Season2;
	private String mixFeatureID2Season2;
	private String configIDSeason2;
	private String mixConfigIDSeason2;
	
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
		f = new FeaturesRestApi();
		f.setURL(m_url);
		s = new SeasonsRestApi();
		s.setURL(m_url);
		br = new BranchesRestApi();
		br.setURL(url);
	    
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
	Mix Feature under feature - allowed
	Mix Feature under mix of features - allowed
	Mix Feature under config - not allowed
	Mix Feature under mix config - not allowed
	Mix Feature under root - allowed
		
	 */
	
	@Test (description="Create first season with 2 feature. Copy season")
	public void copySeason() throws Exception{
		String featureMix = FileUtils.fileToString(filePath + "feature-mutual.txt", "UTF-8", false);
		mixID1 = f.addFeatureToBranch(seasonID, srcBranchID, featureMix, "ROOT", sessionToken);
		Assert.assertFalse(mixID1.contains("error"), "Feature was not added to the season: " + mixID1);
		
		String feature1 = FileUtils.fileToString(filePath + "feature1.txt", "UTF-8", false);
		featureID1 = f.addFeatureToBranch(seasonID, srcBranchID, feature1, mixID1, sessionToken);
		Assert.assertFalse(featureID1.contains("error"), "Feature was not added to the season");

		
		JSONObject sJson = new JSONObject();
		sJson.put("minVersion", "2.0");
		
		seasonID2 = s.addSeason(productID, sJson.toString(), sessionToken);
		Assert.assertFalse(seasonID2.contains("error"), "The second season was not created: " + seasonID2);
		
		if (runOnMaster) {
			destBranchID = BranchesRestApi.MASTER;
		}
		else {

			String allBranches = br.getAllBranches(seasonID2,sessionToken);
			JSONObject jsonBranch = new JSONObject(allBranches);
			destBranchID = jsonBranch.getJSONArray("branches").getJSONObject(1).getString("uniqueId");
		}
	
	}
	
	@Test(dependsOnMethods="copySeason", description="Create features in new season. MIX will be copied under them")
	public void createFeaturesInNewSeason() throws Exception{
		//get root in the new season:
	 	rootId = f.getBranchRootId(seasonID2, destBranchID, sessionToken);
	 	
	 	JSONArray features = f.getFeaturesBySeasonFromBranch(seasonID2, destBranchID, sessionToken);
	 	
	 	mixFeatureID2Season2 =features.getJSONObject(0).getString("uniqueId");	 		
	 	
	 	//create new features in the new season
		
		String feature3 = FileUtils.fileToString(filePath + "feature3.txt", "UTF-8", false);
		featureID2Season2 = f.addFeatureToBranch(seasonID2, destBranchID, feature3, "ROOT", sessionToken);
		Assert.assertFalse(featureID2Season2.contains("error"), "Feature was not added to the season" + featureID2Season2);
		
		String mixConfiguration = FileUtils.fileToString(filePath + "configuration_feature-mutual.txt", "UTF-8", false);
		mixConfigIDSeason2 = f.addFeatureToBranch(seasonID2, destBranchID, mixConfiguration, featureID2Season2, sessionToken);
		Assert.assertFalse(mixFeatureID2Season2.contains("error"), "Feature was not added to the season" + mixFeatureID2Season2);
		
		String configuration = FileUtils.fileToString(filePath + "configuration_rule1.txt", "UTF-8", false);
		configIDSeason2 = f.addFeatureToBranch(seasonID2, destBranchID, configuration, featureID2Season2, sessionToken);
		Assert.assertFalse(configIDSeason2.contains("error"), "Feature was not added to the season" + configIDSeason2);

	}
	
	@Test (dependsOnMethods="createFeaturesInNewSeason", description="Copy mix feature under another feature in the second season. First, copy without namesuffix, then copy with namesuffix")
	public void copySingleFeatureUnderFeature() throws IOException, JSONException{
		
		//should fail copy without suffix
		String response = f.copyItemBetweenBranches(mixID1, featureID2Season2, "ACT", null, null, sessionToken, srcBranchID, destBranchID);
		Assert.assertTrue(response.contains("illegalName"), "Feature was copied with existing name ");

		
		response = f.copyItemBetweenBranches(mixID1, featureID2Season2, "ACT", null, "suffix1", sessionToken, srcBranchID, destBranchID);
		Assert.assertTrue(response.contains("newSubTreeId"), "Feature was not copied: " + response);
		
		JSONObject result = new JSONObject(response);
		String newFeature = f.getFeatureFromBranch(result.getString("newSubTreeId"), destBranchID, sessionToken);
		String oldFeature = f.getFeatureFromBranch(mixID1, srcBranchID, sessionToken);
		Assert.assertTrue(f.jsonObjsAreEqual(new JSONObject(newFeature), new JSONObject(oldFeature)));

	}
	
	
	@Test (dependsOnMethods="copySingleFeatureUnderFeature", description="Copy mix feature under mix feature in the second season. First, copy without namesuffix, then copy with namesuffix")
	public void copySingleFeatureUnderMixFeature() throws IOException, JSONException{
				
		//should fail copy without suffix
		String response = f.copyItemBetweenBranches(mixID1, mixFeatureID2Season2, "ACT", null, null, sessionToken, srcBranchID, destBranchID);
		Assert.assertTrue(response.contains("illegalName"), "Feature was copied with existing name ");

		
		response = f.copyItemBetweenBranches(mixID1, mixFeatureID2Season2, "ACT", null, "suffix2", sessionToken, srcBranchID, destBranchID);
		Assert.assertTrue(response.contains("newSubTreeId"), "Feature was not copied: " + response);
		
		JSONObject result = new JSONObject(response);
		String newFeature = f.getFeatureFromBranch(result.getString("newSubTreeId"), destBranchID, sessionToken);
		String oldFeature = f.getFeatureFromBranch(mixID1, srcBranchID, sessionToken);
		Assert.assertTrue(f.jsonObjsAreEqual(new JSONObject(newFeature), new JSONObject(oldFeature)));

	}
	
	@Test (dependsOnMethods="copySingleFeatureUnderMixFeature", description="Copy mix feature under root in the second season. First, copy without namesuffix, then copy with namesuffix")
	public void copySingleFeatureUnderRoot() throws IOException, JSONException{
				
		//should fail copy without suffix
		String response = f.copyItemBetweenBranches(mixID1, rootId, "ACT", null, null, sessionToken, srcBranchID, destBranchID);
		Assert.assertTrue(response.contains("illegalName"), "Feature was copied with existing name ");

		
		response = f.copyItemBetweenBranches(mixID1, rootId, "ACT", null, "suffix3", sessionToken, srcBranchID, destBranchID);
		Assert.assertTrue(response.contains("newSubTreeId"), "Feature was not copied: " + response);
		
		JSONObject result = new JSONObject(response);
		String newFeature = f.getFeatureFromBranch(result.getString("newSubTreeId"), destBranchID, sessionToken);
		String oldFeature = f.getFeatureFromBranch(mixID1, srcBranchID, sessionToken);
		Assert.assertTrue(f.jsonObjsAreEqual(new JSONObject(newFeature), new JSONObject(oldFeature)));

	}
	
	
	@Test (dependsOnMethods="copySingleFeatureUnderRoot", description="Copy mix feature under configuration in the second season. First, copy without namesuffix, then copy with namesuffix")
	public void copySingleFeatureUnderConfiguration() throws IOException{
				
		//should fail copy without suffix
		String response = f.copyItemBetweenBranches(mixID1, configIDSeason2, "ACT", null, null, sessionToken, srcBranchID, destBranchID);
		Assert.assertTrue(response.contains("illegalName"), "Feature was copied with existing name ");

		
		response = f.copyItemBetweenBranches(mixID1, configIDSeason2, "ACT", null, "suffix4", sessionToken, srcBranchID, destBranchID);
		Assert.assertTrue(response.contains("error"), "Feature was copied under configuration " + response);
	}
	
	@Test (dependsOnMethods="copySingleFeatureUnderConfiguration", description="Copy mix feature under mix configuration in the second season. First, copy without namesuffix, then copy with namesuffix")
	public void copySingleFeatureUnderMixConfiguration() throws IOException{
				
		//should fail copy without suffix
		String response = f.copyItemBetweenBranches(mixID1, mixConfigIDSeason2, "ACT", null, null, sessionToken, srcBranchID, destBranchID);
		Assert.assertTrue(response.contains("illegalName"), "Feature was copied with existing name ");

		
		response = f.copyItemBetweenBranches(mixID1, mixConfigIDSeason2, "ACT", null, "suffix5", sessionToken, srcBranchID, destBranchID);
		Assert.assertTrue(response.contains("error"), "Feature was copied under configuration " + response);
	}
	
	@AfterTest
	private void reset(){
		baseUtils.reset(productID, sessionToken);
	}

}