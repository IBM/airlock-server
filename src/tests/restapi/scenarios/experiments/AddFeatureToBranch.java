package tests.restapi.scenarios.experiments;

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


public class AddFeatureToBranch {
	protected String productID;
	protected String seasonID;
	private String branchID;
	private String branchID2;
	private String featureIDMaster;
	private String featureIDBranch;
	protected String filePath;
	protected String m_url;
	private String sessionToken = "";
	private AirlockUtils baseUtils;
	private BranchesRestApi br ;
	private FeaturesRestApi f;

	
	@BeforeClass
	@Parameters({"url", "analyticsUrl", "translationsUrl", "configPath", "sessionToken", "userName", "userPassword", "appName", "productsToDeleteFile"})
	public void init(String url, String analyticsUrl, String translationsUrl, String configPath, String sToken, String userName, String userPassword, String appName, String productsToDeleteFile) throws Exception{
		m_url = url;
		filePath = configPath ;
		f = new FeaturesRestApi();
		f.setURL(m_url);
		br = new BranchesRestApi();
		br.setURL(m_url);

		baseUtils = new AirlockUtils(m_url, analyticsUrl, translationsUrl, configPath, sToken, userName, userPassword, appName, productsToDeleteFile);
		sessionToken = baseUtils.sessionToken;

		productID = baseUtils.createProduct();
		baseUtils.printProductToFile(productID);
		seasonID = baseUtils.createSeason(productID);
		
	}
	
	/*
	Add new feature to branch
	Add same feature to branch twice
	Add feature to master and checkout in branch
	Add feature to branch with name like in master
	Add all feature types
	- checkout from master and add new feature under it to branch
	-add new feature to branch with master feature as parent
	 */

	@Test (description ="Add new branch") 
	public void addBranch () throws IOException, JSONException, InterruptedException {
		String branch = FileUtils.fileToString(filePath + "experiments/branch1.txt", "UTF-8", false);
		branchID= br.createBranch(seasonID, branch, BranchesRestApi.MASTER, sessionToken);
		
		branch = FileUtils.fileToString(filePath + "experiments/branch2.txt", "UTF-8", false);
		branchID2= br.createBranch(seasonID, branch, BranchesRestApi.MASTER, sessionToken);

			
	}
	
	@Test (dependsOnMethods="addBranch", description ="Add feature to master") 
	public void addFeatureToMaster () throws IOException, JSONException, InterruptedException {
		String feature = FileUtils.fileToString(filePath + "feature1.txt", "UTF-8", false);
		featureIDMaster = f.addFeature(seasonID, feature, "ROOT", sessionToken);
		Assert.assertFalse(featureIDMaster.contains("error"), "Feature was not added to master: " + featureIDMaster);
			
	}
	
	@Test(dependsOnMethods="addFeatureToMaster", description ="Add new feature to branch")
	public void addFeatureToBranch() throws Exception{
		String feature2 = FileUtils.fileToString(filePath + "feature2.txt", "UTF-8", false);
		featureIDBranch = f.addFeatureToBranch(seasonID, branchID, feature2, "ROOT", sessionToken);
		Assert.assertFalse(featureIDBranch.contains("error"), "Feature was not added to the branch: " + featureIDBranch);
		
		String response = br.getBranchWithFeatures(branchID, sessionToken);
		JSONObject brJson = new JSONObject(response);
		JSONArray features = brJson.getJSONArray("features");
		Assert.assertTrue(features.size()==1, "Incorrect number of features in the first branch: " + features.size());
		Assert.assertTrue(features.getJSONObject(0).getString("branchStatus").equals("NEW"), "Feature status is not NEW" );
		
		//get feature from branch
		String featureFromBranch = f.getFeatureFromBranch(featureIDBranch, branchID, sessionToken);
		JSONObject json = new JSONObject(featureFromBranch);
		Assert.assertTrue(json.getString("branchStatus").equals("NEW"), "Feature status is not NEW");
		
		//check that feature was not added to master
		JSONArray featuresInMaster = f.getFeaturesBySeason(seasonID, sessionToken);
		Assert.assertTrue(featuresInMaster.size()==1, "Incorrect number of feature in master: " + featuresInMaster.size());
		
		//check that feature was not added to second branch
		response = br.getBranchWithFeatures(branchID2, sessionToken);
		brJson = new JSONObject(response);
		features = brJson.getJSONArray("features");
		Assert.assertTrue(features.size()==0, "Incorrect number of features in the second branch: " + features.size());
	
	}
	
	@Test(dependsOnMethods="addFeatureToBranch", description ="Add feature to branch twice")
	public void addFeatureToBranchTwice() throws Exception{
		String feature2 = FileUtils.fileToString(filePath + "feature2.txt", "UTF-8", false);
		String response = f.addFeatureToBranch(seasonID, branchID, feature2, "ROOT", sessionToken);
		Assert.assertTrue(response.contains("error"), "Feature was not added to the branch: " + response);			
	}
	
	@Test(dependsOnMethods="addFeatureToBranchTwice", description ="Checkout feature from master")
	public void checkoutFeatureFromMaster() throws Exception{
		br.checkoutFeature(branchID, featureIDMaster, sessionToken);
		String response = br.getBranchWithFeatures(branchID, sessionToken);
		JSONObject brJson = new JSONObject(response);
		JSONArray features = brJson.getJSONArray("features");
		Assert.assertTrue(features.size()==2, "Incorrect number of features in the second branch: " + features.size());
		
		response = br.cancelCheckoutFeature(branchID, featureIDMaster, sessionToken);
		Assert.assertFalse(response.contains("error"), "cannot checkout");
	
	}
	
	@Test(dependsOnMethods="checkoutFeatureFromMaster", description ="Checkout feature from branch")
	public void checkoutFeatureFromBranch() throws Exception{
		String response = br.checkoutFeature(branchID, featureIDBranch, sessionToken);
		Assert.assertTrue(response.contains("not found"), "New feature in branch was checked out");
	
	}
	
	@Test(dependsOnMethods="checkoutFeatureFromBranch", description ="Add configuration mix")
	public void addConfigurationMixToBranch() throws Exception{
		String configurationMix = FileUtils.fileToString(filePath + "configuration_feature-mutual.txt", "UTF-8", false);
		String mixConfigID = f.addFeatureToBranch(seasonID, branchID, configurationMix, featureIDBranch, sessionToken);
		Assert.assertFalse(mixConfigID.contains("error"), "Configuration mix was not added to the branch");
	
	}
	
	@Test(dependsOnMethods="addConfigurationMixToBranch", description ="Add configuration rule")
	public void addConfigurationToBranch() throws Exception{
		String configurationMix = FileUtils.fileToString(filePath + "configuration_rule1.txt", "UTF-8", false);
		String configID = f.addFeatureToBranch(seasonID, branchID, configurationMix, featureIDBranch, sessionToken);
		Assert.assertFalse(configID.contains("error"), "Configuration rule was not added to the branchL: " + configID);
	
		int codeResponse = f.deleteFeatureFromBranch(featureIDBranch, branchID, sessionToken);
		Assert.assertTrue(codeResponse==200, "Feature was not deleted");
	}
	

	@Test(dependsOnMethods="addConfigurationToBranch", description ="Add and delete configuration rule")
	public void deleteConfigurationFromBranch() throws Exception{
		String feature2 = FileUtils.fileToString(filePath + "feature2.txt", "UTF-8", false);
		featureIDBranch = f.addFeatureToBranch(seasonID, branchID, feature2, "ROOT", sessionToken);
		Assert.assertFalse(featureIDBranch.contains("error"), "Feature was not added to the branch: " + featureIDBranch);
		
		String configuration = FileUtils.fileToString(filePath + "configuration_rule1.txt", "UTF-8", false);
		String configID = f.addFeatureToBranch(seasonID, branchID, configuration, featureIDBranch, sessionToken);
		Assert.assertFalse(configID.contains("error"), "Configuration rule was not added to the branchL: " + configID);
	
		int codeResponse = f.deleteFeatureFromBranch(configID, branchID, sessionToken);
		Assert.assertTrue(codeResponse==200, "Configuration rule was not deleted");
		
		String featureFromBranch = f.getFeatureFromBranch(featureIDBranch, branchID, sessionToken);
		Assert.assertFalse(featureFromBranch.contains("error"), "Feature not found?");

		codeResponse = f.deleteFeatureFromBranch(featureIDBranch, branchID, sessionToken);
		Assert.assertTrue(codeResponse==200, "Feature was not deleted");
		
	}
	
	@Test(dependsOnMethods="deleteConfigurationFromBranch", description ="Add mix of features")
	public void addFeatureMixToBranch() throws Exception{
		String featureMix = FileUtils.fileToString(filePath + "feature-mutual.txt", "UTF-8", false);
		String mixID = f.addFeatureToBranch(seasonID, branchID, featureMix, "ROOT", sessionToken);
		Assert.assertFalse(mixID.contains("error"), "Mix of features was not added to the branchL: " + mixID);	
		
		int codeResponse = f.deleteFeatureFromBranch(mixID, branchID, sessionToken);
		Assert.assertTrue(codeResponse==200, "Feature was not deleted");
	}
	
	@Test (dependsOnMethods="addFeatureMixToBranch", description ="Add feature to branch with name like in master") 
	public void addFeatureToBranchNameAsInMaster () throws IOException, JSONException, InterruptedException {
		String feature = FileUtils.fileToString(filePath + "feature1.txt", "UTF-8", false);
		String response = f.addFeatureToBranch(seasonID, branchID, feature, "ROOT", sessionToken);
		Assert.assertTrue(response.contains("error"), "Feature was added to branch with name like in master: " + response);
			
	}
	
	@Test (dependsOnMethods="addFeatureToBranchNameAsInMaster", description ="add new feature to branch with master feature as parent") 
	public void addFeatureToBranchParentInMaster () throws Exception {
		String feature = FileUtils.fileToString(filePath + "feature3.txt", "UTF-8", false);
		String id = f.addFeatureToBranch(seasonID, branchID, feature, featureIDMaster, sessionToken);
		Assert.assertTrue(id.contains("error"), "Feature was not added to branch with parent in master: " + id);
		
	}
	
	@Test (dependsOnMethods="addFeatureToBranchParentInMaster", description ="checkout from master and add new feature under it to branch") 
	public void addFeatureToBranchCheckoutParent () throws Exception {
		br.checkoutFeature(branchID, featureIDMaster, sessionToken);
		
		JSONObject parentFeature = new JSONObject(f.getFeature(featureIDMaster, sessionToken));
		
		String feature = FileUtils.fileToString(filePath + "feature1.txt", "UTF-8", false);
		JSONObject json = new JSONObject(feature);
		json.put("name", "feature4");
		String id = f.addFeatureToBranch(seasonID, branchID, json.toString(), featureIDMaster, sessionToken);
		Assert.assertFalse(id.contains("error"), "Feature was not added to branch with parent in master: " + id);
		
		String response = br.getBranchWithFeatures(branchID, sessionToken);
		JSONObject brJson = new JSONObject(response);
		JSONArray features = brJson.getJSONArray("features");
		//in features array - parent feature checked out from master with 2 branch new features
		Assert.assertTrue(features.size()==1, "Incorrect number of features in the second branch: " + features.size());
		Assert.assertTrue(features.getJSONObject(0).getJSONArray("features").getJSONObject(0).getString("branchStatus").equals("NEW"), "Feature status is not NEW in branch ");
		//Assert.assertTrue(features.getJSONObject(0).getJSONArray("features").getJSONObject(0).getString("branchFeatureParentName").contains(parentFeature.getString("name")), "Incorrect branchFeatureParentName in branch ");

		String featureFromBranch = f.getFeatureFromBranch(id, branchID, sessionToken);
		json = new JSONObject(featureFromBranch);
		Assert.assertTrue(json.getString("branchStatus").equals("NEW"), "Feature status is not NEW");
		//Assert.assertTrue(json.getString("branchFeatureParentName").contains(parentFeature.getString("name")), "Incorrect branchFeatureParentName in branch ");
	}
	
	//add configuration to master, add feature to branch with this configuration as a parent
	@Test (dependsOnMethods="addFeatureToBranchCheckoutParent", description ="add configuration to master, add feature to branch with this configuration as a parent ") 
	public void addFeatureToConfiguation () throws Exception {
		
		String configurationMix = FileUtils.fileToString(filePath + "configuration_feature-mutual.txt", "UTF-8", false);
		String mixConfigID = f.addFeature(seasonID, configurationMix, featureIDMaster, sessionToken);
		Assert.assertFalse(mixConfigID.contains("error"), "Configuration mix was not added to the season");

		String configuration1 = FileUtils.fileToString(filePath + "configuration_rule1.txt", "UTF-8", false);
		String configID = f.addFeature(seasonID, configuration1, mixConfigID, sessionToken);
		Assert.assertFalse(configID.contains("error"), "Configuration rule1 was not added to the season");
		
		String feature3 = FileUtils.fileToString(filePath + "feature3.txt", "UTF-8", false);
		String response = f.addFeatureToBranch(seasonID, branchID, feature3, mixConfigID, sessionToken);
		Assert.assertTrue(response.contains("error"), "Feature was added to the branch under mix of configuration rules: " + response);

		response = f.addFeatureToBranch(seasonID, branchID, feature3, configID, sessionToken);
		Assert.assertTrue(response.contains("error"), "Feature was added to the branch under configuration rule: " + response);

	}
	
	@AfterTest
	private void reset(){
		baseUtils.reset(productID, sessionToken);
	}
}
