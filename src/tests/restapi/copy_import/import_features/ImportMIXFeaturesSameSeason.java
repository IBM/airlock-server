package tests.restapi.copy_import.import_features;

import java.io.IOException;


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


public class ImportMIXFeaturesSameSeason {
	protected String seasonID;
	protected String productID;
	protected String featureID1;
	protected String featureID2;
	protected String featureID3;
	private String mixID1;
	protected String configID;
	protected String filePath;
	protected String m_url;
	protected ProductsRestApi p;
	protected FeaturesRestApi f;
	private String sessionToken = "";
	protected AirlockUtils baseUtils;
	private String srcBranchID;

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
	Mix Feature under feature - allowed
	Mix Feature under mix of features - allowed
	Mix Feature under config - not allowed
	Mix Feature under mix config - not allowed
	Mix Feature under root - allowed
		
	 */
	
	@Test (description="Imort mix of features under another feature in the same season.")
	public void importMixUnderFeature() throws IOException, JSONException{
		String featureMix = FileUtils.fileToString(filePath + "feature-mutual.txt", "UTF-8", false);
		mixID1 = f.addFeatureToBranch(seasonID, srcBranchID, featureMix, "ROOT", sessionToken);
		Assert.assertFalse(mixID1.contains("error"), "Feature was not added to the season: " + mixID1);
		
		String feature1 = FileUtils.fileToString(filePath + "feature1.txt", "UTF-8", false);
		featureID1 = f.addFeatureToBranch(seasonID, srcBranchID, feature1, mixID1, sessionToken);
		Assert.assertFalse(featureID1.contains("error"), "Feature was not added to the season");

		
		String feature3 = FileUtils.fileToString(filePath + "feature3.txt", "UTF-8", false);
		featureID3 = f.addFeatureToBranch(seasonID, srcBranchID, feature3, "ROOT", sessionToken);
		Assert.assertFalse(featureID3.contains("error"), "Feature was not added to the season");

		//should fail import without suffix
		String featureToImport = f.getFeatureFromBranch(mixID1, srcBranchID, sessionToken);
		String response = f.importFeatureToBranch(featureToImport, featureID3, "ACT", null, null, true, sessionToken, srcBranchID);
		Assert.assertTrue(response.contains("illegalName"), "Feature was imported with existing name ");

		//should fail import without overrideids
		response = f.importFeatureToBranch(featureToImport, featureID3, "ACT", null, null, false, sessionToken, srcBranchID);
		Assert.assertTrue(response.contains("illegalId"), "Feature was imported with existing name ");
		
		response = f.importFeatureToBranch(featureToImport, featureID3, "ACT", null, "suffix1", true,  sessionToken, srcBranchID);
		Assert.assertTrue(response.contains("newSubTreeId"), "Feature was not copied: " + response);
		
		JSONObject result = new JSONObject(response);
		String newFeature = f.getFeatureFromBranch(result.getString("newSubTreeId"), srcBranchID, sessionToken);
		Assert.assertTrue(f.jsonObjsAreEqual(new JSONObject(newFeature), new JSONObject(featureToImport)));

	}
	
	@Test (dependsOnMethods="importMixUnderFeature", description="Import mix features under mix feature in the same season.")
	public void importMixUnderMixFeature() throws IOException, JSONException{
		String featureMix = FileUtils.fileToString(filePath + "feature-mutual.txt", "UTF-8", false);
		String mixId = f.addFeatureToBranch(seasonID, srcBranchID, featureMix, featureID3, sessionToken);
		Assert.assertFalse(mixId.contains("error"), "Feature was not added to the season" + mixId);
		
		String featureToImport = f.getFeatureFromBranch(mixID1, srcBranchID, sessionToken);
		
		//should fail copy without suffix
		String response = f.importFeatureToBranch(featureToImport, mixId, "ACT", null, null, true, sessionToken, srcBranchID);
		Assert.assertTrue(response.contains("illegalName"), "Feature was imported with existing name ");

		response = f.importFeatureToBranch(featureToImport, mixId, "ACT", null, null, false, sessionToken, srcBranchID);
		Assert.assertTrue(response.contains("illegalId"), "Feature was imported with existing name ");
		
		response = f.importFeatureToBranch(featureToImport, mixId, "ACT", null, "suffix2", true, sessionToken, srcBranchID);
		Assert.assertTrue(response.contains("newSubTreeId"), "Feature was not imported: " + response);
		
		JSONObject result = new JSONObject(response);
		String newFeature = f.getFeatureFromBranch(result.getString("newSubTreeId"), srcBranchID, sessionToken);
		Assert.assertTrue(f.jsonObjsAreEqual(new JSONObject(newFeature), new JSONObject(featureToImport)));

	}
	
	@Test (dependsOnMethods="importMixUnderMixFeature", description="Import mix feature under root in the same season. ")
	public void importMixUnderRoot() throws IOException, JSONException{
		String rootId = f.getBranchRootId(seasonID, srcBranchID, sessionToken);
		String featureToImport = f.getFeatureFromBranch(mixID1, srcBranchID, sessionToken);
		
		//should fail copy without suffix
		String response = f.importFeatureToBranch(featureToImport, rootId, "ACT", null, null, true, sessionToken, srcBranchID);
		Assert.assertTrue(response.contains("illegalName"), "Feature was imported with existing name ");

		
		response = f.importFeatureToBranch(featureToImport, rootId, "ACT", null, "suffix3", true, sessionToken, srcBranchID);
		Assert.assertTrue(response.contains("newSubTreeId"), "Feature was not imported: " + response);
		
		JSONObject result = new JSONObject(response);
		String newFeature = f.getFeatureFromBranch(result.getString("newSubTreeId"), srcBranchID, sessionToken);
		Assert.assertTrue(f.jsonObjsAreEqual(new JSONObject(newFeature), new JSONObject(featureToImport)));

	}
	
	
	@Test (dependsOnMethods="importMixUnderRoot", description="Import mix feature under configuration in the same season.")
	public void importMixUnderConfiguration() throws IOException{
		String configuration = FileUtils.fileToString(filePath + "configuration_rule1.txt", "UTF-8", false);
		configID = f.addFeatureToBranch(seasonID, srcBranchID, configuration, featureID3, sessionToken);
		Assert.assertFalse(configID.contains("error"), "Feature was not added to the season");
		
		String featureToImport = f.getFeatureFromBranch(mixID1, srcBranchID, sessionToken);
		
		//should fail copy without suffix
		String response = f.importFeatureToBranch(featureToImport, configID, "ACT", null, "suffix4", true, sessionToken, srcBranchID);
		Assert.assertTrue(response.contains("error"), "Feature was imported under configuration " + response);
	}
	
	@Test (dependsOnMethods="importMixUnderConfiguration", description="Import mix feature under mix configuration in the same season.")
	public void importMixUnderMixConfiguration() throws IOException{
		String configuration = FileUtils.fileToString(filePath + "configuration_feature-mutual.txt", "UTF-8", false);
		String mixConfigID = f.addFeatureToBranch(seasonID, srcBranchID, configuration, featureID3, sessionToken);
		Assert.assertFalse(mixConfigID.contains("error"), "Feature was not added to the season");
			
		String featureToImport = f.getFeatureFromBranch(mixID1, srcBranchID, sessionToken);
		
		//should fail copy without suffix
		String response = f.importFeatureToBranch(featureToImport, mixConfigID, "ACT", null, "suffix5", true, sessionToken, srcBranchID);
		Assert.assertTrue(response.contains("error"), "Feature was imported under configuration " + response);
	}
	
	@Test (dependsOnMethods="importMixUnderMixConfiguration", description="Import mix feature under itself in the same season.")
	public void importMixUnderItself() throws IOException, JSONException{
		String featureToImport = f.getFeatureFromBranch(mixID1, srcBranchID, sessionToken);
		
		//should fail copy without suffix
		String response = f.importFeatureToBranch(featureToImport, mixID1, "ACT", null, null, true, sessionToken, srcBranchID);
		Assert.assertTrue(response.contains("illegalName"), "Feature was copied with existing name ");

		
		response = f.importFeatureToBranch(featureToImport, mixID1, "ACT", null, "suffix6", true, sessionToken, srcBranchID);
		Assert.assertTrue(response.contains("newSubTreeId"), "Feature was not copied: " + response);
		
		JSONObject result = new JSONObject(response);
		String newFeature =  f.getFeatureFromBranch(result.getString("newSubTreeId"), srcBranchID, sessionToken);
		Assert.assertTrue(f.jsonObjsAreEqual(new JSONObject(newFeature), new JSONObject(featureToImport)));


	}
	
	@AfterTest
	private void reset(){
		baseUtils.reset(productID, sessionToken);
	}
}