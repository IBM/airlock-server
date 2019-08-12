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

public class ImportFeatureDifferentSeason {
	protected String seasonID;
	protected String seasonID2;
	protected String productID;
	protected String featureID1;
	protected String filePath;
	protected String m_url;
	protected ProductsRestApi p;
	protected FeaturesRestApi f;
	private BranchesRestApi br ;
	protected SeasonsRestApi s;
	private String sessionToken = "";
	protected AirlockUtils baseUtils;
	
	//in new season
	private String rootId;
	private String featureID2Season2;
	private String mixFeatureID2Season2;
	private String configIDSeason2;
	private String mixConfigIDSeason2;

	private boolean runOnMaster;
	private String srcBranchID;
	private String destBranchID;


	@BeforeClass
	@Parameters({"url", "analyticsUrl", "translationsUrl", "configPath", "sessionToken", "userName", "userPassword", "appName", "productsToDeleteFile", "runOnMaster"})
	public void init(String url, String analyticsUrl, String translationsUrl, String configPath, String sToken, String userName, String userPassword, String appName, String productsToDeleteFile, Boolean onMaster) throws Exception{
		m_url = url;
		filePath = configPath;
		p = new ProductsRestApi();
		p.setURL(m_url);
		f = new FeaturesRestApi();
		f.setURL(m_url);
		s = new SeasonsRestApi();
		s.setURL(m_url);
		runOnMaster = onMaster;
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

	}
	
	/*
	  	Feature under feature - allowed
		Feature under mix of features - allowed
		Feature under root - allowed
		Feature under config - not allowed
		Feature under mix config - not allowed
		
	 */
	
	@Test (description="Create first season with 2 feature. Copy season")
	public void copySeason() throws Exception{
		String feature2 = FileUtils.fileToString(filePath + "feature2.txt", "UTF-8", false);
		String featureID2 = f.addFeatureToBranch(seasonID, srcBranchID, feature2, "ROOT", sessionToken);
		Assert.assertFalse(featureID2.contains("error"), "Feature was not added to the season");
		
		String featureMix = FileUtils.fileToString(filePath + "feature-mutual.txt", "UTF-8", false);
		String mixId = f.addFeatureToBranch(seasonID, srcBranchID, featureMix, "ROOT", sessionToken);
		Assert.assertFalse(mixId.contains("error"), "Feature was not added to the season" + mixId);

		String configuration = FileUtils.fileToString(filePath + "configuration_rule1.txt", "UTF-8", false);
		String configID = f.addFeatureToBranch(seasonID, srcBranchID, configuration, featureID2, sessionToken);
		Assert.assertFalse(configID.contains("error"), "Feature was not added to the season");
		
		String mixConfiguration = FileUtils.fileToString(filePath + "configuration_feature-mutual.txt", "UTF-8", false);
		String mixConfigID = f.addFeatureToBranch(seasonID, srcBranchID, mixConfiguration, featureID2, sessionToken);
		Assert.assertFalse(mixConfigID.contains("error"), "Feature was not added to the season");

		
		//this feature will be copied from the first season to the second
		String feature1 = FileUtils.fileToString(filePath + "feature1.txt", "UTF-8", false);
		featureID1 = f.addFeatureToBranch(seasonID, srcBranchID, feature1, "ROOT", sessionToken);
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
	
	@Test(dependsOnMethods="copySeason", description="Parse new season ids")
	public void getNewFeaturesIds() throws Exception{

		 	rootId = f.getBranchRootId(seasonID2, destBranchID, sessionToken);
		 	
		 	JSONArray features = f.getFeaturesBySeasonFromBranch(seasonID2, destBranchID, sessionToken);
		 	
		 	featureID2Season2 = features.getJSONObject(0).getString("uniqueId");
		 	mixFeatureID2Season2 =features.getJSONObject(1).getString("uniqueId");
		 			 	
		 	JSONArray configurations = features.getJSONObject(0).getJSONArray("configurationRules");
		 	for (Object el: configurations){
		 		JSONObject config = new JSONObject(el);
		 		if (config.getString("type").equals("CONFIGURATION_RULE"))
		 			configIDSeason2 = config.getString("uniqueId");
		 		else if (config.getString("type").equals("CONFIG_MUTUAL_EXCLUSION_GROUP"))
		 			mixConfigIDSeason2 = config.getString("uniqueId");
		 	}
		
	}
	
	@Test (dependsOnMethods="getNewFeaturesIds", description="Import single feature under another feature in the second season. ")
	public void importSingleFeatureUnderFeature() throws IOException, JSONException{
		String featureToImport = f.getFeatureFromBranch(featureID1, srcBranchID, sessionToken);
		
		//should fail copy without suffix
		String response = f.importFeatureToBranch(featureToImport, featureID2Season2, "ACT", null, null, true, sessionToken, destBranchID);
		Assert.assertTrue(response.contains("illegalName"), "Feature was imported with existing name ");

		//should fail copy without overrideids
		response = f.importFeatureToBranch(featureToImport, featureID2Season2, "ACT", null, "suffix1", false, sessionToken, destBranchID);
		Assert.assertTrue(response.contains("illegalName"), "Feature was imported with existing name ");
		
		response = f.importFeatureToBranch(featureToImport, featureID2Season2, "ACT", null, "suffix1", true, sessionToken, destBranchID);
		Assert.assertTrue(response.contains("newSubTreeId"), "Feature was not imported: " + response);
		
		JSONObject result = new JSONObject(response);
		String newFeature = f.getFeatureFromBranch(result.getString("newSubTreeId"), destBranchID, sessionToken);
		Assert.assertTrue(f.jsonObjsAreEqual(new JSONObject(newFeature), new JSONObject(featureToImport)));

	}
	
	
	@Test (dependsOnMethods="importSingleFeatureUnderFeature", description="Import single feature under mix feature in the second season.")
	public void importSingleFeatureUnderMixFeature() throws IOException, JSONException{
		String featureToImport = f.getFeatureFromBranch(featureID1, srcBranchID, sessionToken);
		
		//should fail copy without suffix
		String response = f.importFeatureToBranch(featureToImport, mixFeatureID2Season2, "ACT", null, null, true,  sessionToken, destBranchID);
		Assert.assertTrue(response.contains("illegalName"), "Feature was copied with existing name ");
		
		response = f.importFeatureToBranch(featureToImport, mixFeatureID2Season2, "ACT", null, "suffix2", true, sessionToken, destBranchID);
		Assert.assertTrue(response.contains("newSubTreeId"), "Feature was not copied: " + response);
		
		JSONObject result = new JSONObject(response);
		String newFeature = f.getFeatureFromBranch(result.getString("newSubTreeId"), destBranchID, sessionToken);
		Assert.assertTrue(f.jsonObjsAreEqual(new JSONObject(newFeature), new JSONObject(featureToImport)));

	}
	
	@Test (dependsOnMethods="importSingleFeatureUnderMixFeature", description="Import single feature under root in the second season.")
	public void importSingleFeatureUnderRoot() throws IOException, JSONException{
		String featureToImport = f.getFeatureFromBranch(featureID1, srcBranchID, sessionToken);
		
		//should fail copy without suffix
		String response = f.importFeatureToBranch(featureToImport, rootId, "ACT", null, null, true, sessionToken, destBranchID);
		Assert.assertTrue(response.contains("illegalName"), "Feature was imported with existing name ");

		
		response = f.importFeatureToBranch(featureToImport, rootId, "ACT", null, "suffix3", true, sessionToken, destBranchID);
		Assert.assertTrue(response.contains("newSubTreeId"), "Feature was not imported: " + response);
		
		JSONObject result = new JSONObject(response);
		String newFeature = f.getFeatureFromBranch(result.getString("newSubTreeId"), destBranchID, sessionToken);
		Assert.assertTrue(f.jsonObjsAreEqual(new JSONObject(newFeature), new JSONObject(featureToImport)));

	}
	
	
	@Test (dependsOnMethods="importSingleFeatureUnderRoot", description="Import single feature under configuration in the second season.")
	public void importSingleFeatureUnderConfiguration() throws IOException{
		String featureToImport = f.getFeatureFromBranch(featureID1, srcBranchID, sessionToken);
		
		//should fail copy without suffix
		String response = f.importFeatureToBranch(featureToImport, configIDSeason2, "ACT", null, null, true, sessionToken, destBranchID);
		Assert.assertTrue(response.contains("illegalName"), "Feature was imported with existing name ");
		
		response = f.importFeatureToBranch(featureToImport, configIDSeason2, "ACT", null, "suffix4", true, sessionToken, destBranchID);
		Assert.assertTrue(response.contains("error"), "Feature was imported under configuration " + response);
	}
	
	@Test (dependsOnMethods="importSingleFeatureUnderRoot", description="Import single feature under mix configuration in the second season.")
	public void importSingleFeatureUnderMixConfiguration() throws IOException{
		String featureToImport = f.getFeatureFromBranch(featureID1, srcBranchID, sessionToken);
		
		String response = f.importFeatureToBranch(featureToImport, mixConfigIDSeason2, "ACT", null, "suffix5", true, sessionToken, destBranchID);
		Assert.assertTrue(response.contains("error"), "Feature was imported under configuration " + response);
	}
	
	@AfterTest
	private void reset(){
		baseUtils.reset(productID, sessionToken);
	}

}