package tests.restapi.copy_import.import_features;

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

public class ImportConfigurationDifferentSeason {
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
	private BranchesRestApi br ;
	private String sessionToken = "";
	protected AirlockUtils baseUtils;
	private String configIDSeason2;
	private boolean runOnMaster;
	private String srcBranchID;
	private String destBranchID;


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
	Config under feature - allowed
	Config under config - allowed
	Config under mix of configs - allowed
	Config under root - not allowed
	Config under mix of features - not allowed
		
	 */

	@Test (description="Create first season with feature and configuration. Copy season")
	public void copySeason() throws Exception{
		String feature = FileUtils.fileToString(filePath + "feature1.txt", "UTF-8", false);
		String featureID1 = f.addFeatureToBranch(seasonID, srcBranchID, feature, "ROOT", sessionToken);
		Assert.assertFalse(featureID1.contains("error"), "Feature was not added to the season");
		
		String configuration = FileUtils.fileToString(filePath + "configuration_rule1.txt", "UTF-8", false);
		configID = f.addFeatureToBranch(seasonID, srcBranchID, configuration, featureID1, sessionToken);
		Assert.assertFalse(configID.contains("error"), "Feature was not added to the season");
		
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
	
	@Test(dependsOnMethods="copySeason", description="Parse new season ids")
	public void getNewFeaturesIds() throws Exception{
		 JSONArray features = f.getFeaturesBySeasonFromBranch(seasonID2, destBranchID, sessionToken);
		 configIDSeason2 = 	features.getJSONObject(0).getJSONArray("configurationRules").getJSONObject(0).getString("uniqueId"); 	
	}
	
	
	@Test (dependsOnMethods="getNewFeaturesIds", description="Import configuration under feature in the new season.")
	public void importConfigurationUnderFeature() throws IOException, JSONException{
		String feature = FileUtils.fileToString(filePath + "feature2.txt", "UTF-8", false);
		String featureID = f.addFeatureToBranch(seasonID2, destBranchID, feature, "ROOT", sessionToken);
		Assert.assertFalse(featureID.contains("error"), "Feature was not added to season" + featureID);

		String configToImport = f.getFeatureFromBranch(configID, srcBranchID, sessionToken);
		
		//should fail copy without suffix
		String response = f.importFeatureToBranch(configToImport, featureID, "ACT", null, null, true, sessionToken, destBranchID);
		Assert.assertTrue(response.contains("illegalName"), "Configuration was imported with existing name ");
		
		response = f.importFeatureToBranch(configToImport, featureID, "ACT", null, "suffix1", false, sessionToken, destBranchID);
		Assert.assertTrue(response.contains("illegalName"), "Configuration was imported with existing name ");

		response = f.importFeatureToBranch(configToImport, featureID, "ACT", null, "suffix1", true, sessionToken, destBranchID);
		Assert.assertTrue(response.contains("newSubTreeId"), "Configuration was not imported: " + response);
		
		JSONObject result = new JSONObject(response);
		String newFeature = f.getFeatureFromBranch(result.getString("newSubTreeId"), destBranchID, sessionToken);
		String oldFeature = f.getFeatureFromBranch(configID, srcBranchID, sessionToken);
		Assert.assertTrue(f.jsonObjsAreEqual(new JSONObject(newFeature), new JSONObject(oldFeature)));

	}
	
	@Test (dependsOnMethods="importConfigurationUnderFeature", description="Import configuration under mix feature in the new season.")
	public void importConfigurationUnderMixFeature() throws IOException{
		String featureMix = FileUtils.fileToString(filePath + "feature-mutual.txt", "UTF-8", false);
		String mixId = f.addFeatureToBranch(seasonID2, destBranchID, featureMix, "ROOT", sessionToken);
		Assert.assertFalse(mixId.contains("error"), "Feature was not added to the season" + mixId);
		
		String configToImport = f.getFeatureFromBranch(configID, srcBranchID, sessionToken);

		String response = f.importFeatureToBranch(configToImport, mixId, "ACT", null, "suffix2", true, sessionToken, destBranchID);
		Assert.assertTrue(response.contains("error"), "Configuraiton was imported under features mix ");
	}
	
	@Test (dependsOnMethods="importConfigurationUnderMixFeature", description="Import configuration under root in the new season.")
	public void importConfigurationUnderRoot() throws IOException{
		String rootId = f.getBranchRootId(seasonID2, destBranchID, sessionToken);
		String configToImport = f.getFeatureFromBranch(configID, srcBranchID, sessionToken);
		
		String response = f.importFeatureToBranch(configToImport, rootId, "ACT", null, "suffix3", true, sessionToken, destBranchID);
		Assert.assertTrue(response.contains("error"), "Configuration was imported under root ");
	}
	
	
	@Test (dependsOnMethods="importConfigurationUnderRoot", description="Import configuration under itself in the new season.")
	public void importConfigurationUnderItself() throws IOException, JSONException{
		//should fail copy without suffix
		String configToImport = f.getFeatureFromBranch(configID, srcBranchID, sessionToken);
		
		String response = f.importFeatureToBranch(configToImport, configIDSeason2, "ACT", null, null, true, sessionToken, destBranchID);
		Assert.assertTrue(response.contains("illegalName"), "Configuration was imported with existing name  ");
		
		response = f.importFeatureToBranch(configToImport, configIDSeason2, "ACT", null, "suffix4", true, sessionToken, destBranchID);
		Assert.assertTrue(response.contains("newSubTreeId"), "Configuration was not imported: " + response);
		
		JSONObject result = new JSONObject(response);
		String newFeature = f.getFeatureFromBranch(result.getString("newSubTreeId"), destBranchID, sessionToken);
		JSONObject oldFeature = new JSONObject(f.getFeatureFromBranch(configIDSeason2, destBranchID, sessionToken));
		Assert.assertTrue(f.jsonObjsAreEqual(new JSONObject(newFeature), oldFeature.getJSONArray("configurationRules").getJSONObject(0)));

	}
	
	@Test (dependsOnMethods="importConfigurationUnderItself", description="Import configuration under configuration in the new season.")
	public void importConfigurationUnderConfiguration() throws IOException, JSONException{
		String feature = FileUtils.fileToString(filePath + "feature3.txt", "UTF-8", false);
		featureID2 = f.addFeatureToBranch(seasonID2, destBranchID, feature, "ROOT", sessionToken);
		Assert.assertFalse(featureID2.contains("error"), "Feature was not added to season" + featureID2);
		
		String configuration = FileUtils.fileToString(filePath + "configuration_rule2.txt", "UTF-8", false);
		String configID2 = f.addFeatureToBranch(seasonID2, destBranchID, configuration, featureID2, sessionToken);
		Assert.assertFalse(configID.contains("error"), "Feature was not added to the season");

		String configToImport = f.getFeatureFromBranch(configID, srcBranchID, sessionToken);
		
		String response = f.importFeatureToBranch(configToImport, configID2, "ACT", null, null, true, sessionToken, destBranchID);
		Assert.assertTrue(response.contains("illegalName"), "Configuration was imported with existing name  ");

		response = f.importFeatureToBranch(configToImport, configID2, "ACT", null, "suffix5", true, sessionToken, destBranchID);
		Assert.assertTrue(response.contains("newSubTreeId"), "Configuration was not imported: " + response);
		
		JSONObject result = new JSONObject(response);
		String newFeature = f.getFeatureFromBranch(result.getString("newSubTreeId"), destBranchID, sessionToken);
		Assert.assertTrue(f.jsonObjsAreEqual(new JSONObject(newFeature), new JSONObject(configToImport)));

	}
	
	@Test (dependsOnMethods="importConfigurationUnderConfiguration", description="Import configuration under mix configuration in the new season.")
	public void importConfigurationUnderMixConfiguration() throws IOException, JSONException{
		String configuration = FileUtils.fileToString(filePath + "configuration_feature-mutual.txt", "UTF-8", false);
		String mixConfigID = f.addFeatureToBranch(seasonID2, destBranchID, configuration, featureID2, sessionToken);
		Assert.assertFalse(mixConfigID.contains("error"), "Feature was not added to the season");
		
		String configToImport = f.getFeatureFromBranch(configID, srcBranchID, sessionToken);
		
		String response = f.importFeatureToBranch(configToImport, mixConfigID, "ACT", null, null, true, sessionToken, destBranchID);
		Assert.assertTrue(response.contains("illegalName"), "Configuration was imported with existing name  ");

		response = f.importFeatureToBranch(configToImport, mixConfigID, "ACT", null, "suffix6", true, sessionToken, destBranchID);
		Assert.assertTrue(response.contains("newSubTreeId"), "Configuration was not imported: " + response);
		
		JSONObject result = new JSONObject(response);
		String newFeature = f.getFeatureFromBranch(result.getString("newSubTreeId"), destBranchID, sessionToken);
		Assert.assertTrue(f.jsonObjsAreEqual(new JSONObject(newFeature), new JSONObject(configToImport)));

	}
	
	
	@AfterTest
	private void reset(){
		baseUtils.reset(productID, sessionToken);
	}
}