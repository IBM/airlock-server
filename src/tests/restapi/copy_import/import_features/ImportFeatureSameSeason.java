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


public class ImportFeatureSameSeason {
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
	  	Feature under feature - allowed
		Feature under mix of features - allowed
		Feature under root - allowed
		Feature under config - not allowed
		Feature under mix config - not allowed
		Feature under itself - allowed
		
	 */
	
	@Test (description="Import single feature under another feature in the same season. ")
	public void importSingleFeatureUnderFeature() throws IOException, JSONException{
		String feature1 = FileUtils.fileToString(filePath + "feature1.txt", "UTF-8", false);
		featureID1 = f.addFeatureToBranch(seasonID, srcBranchID, feature1, "ROOT", sessionToken);
		Assert.assertFalse(featureID1.contains("error"), "Feature was not added to the season");
		
		String feature2 = FileUtils.fileToString(filePath + "feature2.txt", "UTF-8", false);
		featureID2 = f.addFeatureToBranch(seasonID, srcBranchID, feature2, "ROOT", sessionToken);
		Assert.assertFalse(featureID2.contains("error"), "Feature was not added to the season");
		
		//should fail copy without suffix
		String featureToImport = f.getFeatureFromBranch(featureID1, srcBranchID, sessionToken);
		String response = f.importFeatureToBranch(featureToImport, featureID2, "ACT", null, null,true, sessionToken, srcBranchID);		
		Assert.assertTrue(response.contains("illegalName"), "Feature was imported with existing name ");
		
		//should fail copy without overrideids
		response = f.importFeatureToBranch(featureToImport, featureID2, "ACT", null, "suffix1", false, sessionToken, srcBranchID);		
		Assert.assertTrue(response.contains("illegalId"), "Feature was imported with existing name ");
		
		response = f.importFeatureToBranch(featureToImport, featureID2, "ACT", null, "suffix1", true, sessionToken, srcBranchID);
		Assert.assertTrue(response.contains("newSubTreeId"), "Feature was not imported: " + response);
		
		JSONObject result = new JSONObject(response);
		String newFeature = f.getFeatureFromBranch(result.getString("newSubTreeId"), srcBranchID, sessionToken);
		Assert.assertTrue(f.jsonObjsAreEqual(new JSONObject(newFeature), new JSONObject(featureToImport)));
	}
	
	@Test (dependsOnMethods="importSingleFeatureUnderFeature", description="import single feature under mix feature in the same season. ")
	public void importSingleFeatureUnderMixFeature() throws IOException, JSONException{
		String featureMix = FileUtils.fileToString(filePath + "feature-mutual.txt", "UTF-8", false);
		String mixId = f.addFeatureToBranch(seasonID, srcBranchID, featureMix, featureID2, sessionToken);
		Assert.assertFalse(mixId.contains("error"), "Feature was not added to the season" + mixId);
				
		//should fail copy without suffix
		String featureToImport = f.getFeatureFromBranch(featureID1, srcBranchID, sessionToken);
		String response = f.importFeatureToBranch(featureToImport, mixId, "ACT", null, null, true, sessionToken, srcBranchID);
		Assert.assertTrue(response.contains("illegalName"), "Feature was imported with existing id ");

		
		response = f.importFeatureToBranch(featureToImport, mixId, "ACT", null, "suffix2", true, sessionToken, srcBranchID);
		Assert.assertTrue(response.contains("newSubTreeId"), "Feature was not imported: " + response);
		
		JSONObject result = new JSONObject(response);
		String newFeature = f.getFeatureFromBranch(result.getString("newSubTreeId"), srcBranchID, sessionToken);
		Assert.assertTrue(f.jsonObjsAreEqual(new JSONObject(newFeature), new JSONObject(featureToImport)));

	}
	
	@Test (dependsOnMethods="importSingleFeatureUnderMixFeature", description="Import single feature under root in the same season.")
	public void importSingleFeatureUnderRoot() throws IOException, JSONException{
		String rootId = f.getRootId(seasonID, sessionToken);
		String featureToImport = f.getFeatureFromBranch(featureID1, srcBranchID, sessionToken);
		//should fail copy without suffix
		String response = f.importFeatureToBranch(featureToImport, rootId, "ACT", null, null, true, sessionToken, srcBranchID);
		Assert.assertTrue(response.contains("illegalName"), "Feature was imported with existing name ");

		//should fail copy without overrideids
		response = f.importFeatureToBranch(featureToImport, rootId, "ACT", null, "suffix1", false, sessionToken, srcBranchID);		
		Assert.assertTrue(response.contains("illegalId"), "Feature was imported with existing id ");

		
		response = f.importFeatureToBranch(featureToImport, rootId, "ACT", null, "suffix3", true, sessionToken, srcBranchID);
		Assert.assertTrue(response.contains("newSubTreeId"), "Feature was not imported: " + response);
		
		JSONObject result = new JSONObject(response);
		String newFeature = f.getFeatureFromBranch(result.getString("newSubTreeId"), srcBranchID, sessionToken);
		Assert.assertTrue(f.jsonObjsAreEqual(new JSONObject(newFeature), new JSONObject(featureToImport)));

	}
	
	
	@Test (dependsOnMethods="importSingleFeatureUnderRoot", description="Import single feature under configuration in the same season.")
	public void importSingleFeatureUnderConfiguration() throws IOException{
		String configuration = FileUtils.fileToString(filePath + "configuration_rule1.txt", "UTF-8", false);
		configID = f.addFeatureToBranch(seasonID, srcBranchID, configuration, featureID2, sessionToken);
		Assert.assertFalse(configID.contains("error"), "Feature was not added to the season");

		String featureToImport = f.getFeatureFromBranch(featureID1, srcBranchID, sessionToken);
		String response = f.importFeatureToBranch(featureToImport, configID, "ACT", null, "suffix4", true, sessionToken, srcBranchID);
		Assert.assertTrue(response.contains("error"), "Feature was imported under configuration " + response);
	}
	
	@Test (dependsOnMethods="importSingleFeatureUnderConfiguration", description="Import single feature under mix configuration in the same season. ")
	public void importSingleFeatureUnderMixConfiguration() throws IOException{
		String configuration = FileUtils.fileToString(filePath + "configuration_feature-mutual.txt", "UTF-8", false);
		String mixConfigID = f.addFeatureToBranch(seasonID, srcBranchID, configuration, featureID2, sessionToken);
		Assert.assertFalse(mixConfigID.contains("error"), "Feature was not added to the season");
				
		String featureToImport = f.getFeatureFromBranch(featureID1, srcBranchID, sessionToken);		
		String response = f.importFeatureToBranch(featureToImport, mixConfigID, "ACT", null, "suffix5", true, sessionToken, srcBranchID);
		Assert.assertTrue(response.contains("error"), "Feature was imported under configuration " + response);
	}
	
	@Test (dependsOnMethods="importSingleFeatureUnderMixConfiguration", description="Import single feature under itself in the same season. ")
	public void importSingleFeatureUnderItself() throws IOException, JSONException{
		String featureToImport = f.getFeatureFromBranch(featureID1, srcBranchID, sessionToken);		
		
		//should fail copy without suffix
		String response = f.importFeatureToBranch(featureToImport, featureID1, "ACT", null, null, true, sessionToken, srcBranchID);
		Assert.assertTrue(response.contains("illegalName"), "Feature was imported with existing name ");

		
		response = f.importFeatureToBranch(featureToImport, featureID1, "ACT", null, "suffix6", true, sessionToken, srcBranchID);
		Assert.assertTrue(response.contains("newSubTreeId"), "Feature was not imported: " + response);
		
		JSONObject result = new JSONObject(response);
		String newFeature = f.getFeatureFromBranch(result.getString("newSubTreeId"), srcBranchID, sessionToken);
		JSONObject oldFeature = new JSONObject(f.getFeatureFromBranch(featureID1, srcBranchID, sessionToken));
		Assert.assertTrue(f.jsonObjsAreEqual(new JSONObject(newFeature), oldFeature.getJSONArray("features").getJSONObject(0)));

	}
	
	
	@AfterTest
	private void reset(){
		baseUtils.reset(productID, sessionToken);
	}
}