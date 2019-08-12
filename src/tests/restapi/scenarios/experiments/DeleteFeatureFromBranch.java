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


public class DeleteFeatureFromBranch {
	protected String productID;
	protected String seasonID;
	private String branchID;
	private String featureID1;
	private String featureID2;
	private String mixConfigID;
	private String configID1;
	private String configID2;
	protected String filePath;
	protected String m_url;
	private String sessionToken = "";
	private AirlockUtils baseUtils;
	private BranchesRestApi br ;
	private FeaturesRestApi f;
	private JSONObject feature;

	
	@BeforeClass
	@Parameters({"url", "analyticsUrl", "translationsUrl", "configPath", "sessionToken", "userName", "userPassword", "appName", "productsToDeleteFile"})
	public void init(String url, String analyticsUrl, String translationsUrl, String configPath, String sToken, String userName, String userPassword, String appName, String productsToDeleteFile) throws Exception{
		m_url = url;
		filePath = configPath ;
		br = new BranchesRestApi();
		br.setURL(m_url);
        f = new FeaturesRestApi();
        f.setURL(m_url);
		baseUtils = new AirlockUtils(m_url, analyticsUrl, translationsUrl, configPath, sToken, userName, userPassword, appName, productsToDeleteFile);
		sessionToken = baseUtils.sessionToken;

		productID = baseUtils.createProduct();
		baseUtils.printProductToFile(productID);
		seasonID = baseUtils.createSeason(productID);
		feature = new JSONObject(FileUtils.fileToString(filePath + "feature1.txt", "UTF-8", false));
		

	}
	
	/*
	 * add feature + configs to branch and delete - allowed
 */

	@Test (description ="Add feature with configurations to branch") 
	public void addComponents () throws IOException, JSONException, InterruptedException {
		String branch = FileUtils.fileToString(filePath + "experiments/branch1.txt", "UTF-8", false);
		branchID= br.createBranch(seasonID, branch, BranchesRestApi.MASTER, sessionToken);

		feature.put("name", "F1");
		featureID1 = f.addFeatureToBranch(seasonID, branchID, feature.toString(), "ROOT", sessionToken);
		Assert.assertFalse(featureID1.contains("error"), "Feature was not added to the season: " + featureID1);
				
		String configurationMix = FileUtils.fileToString(filePath + "configuration_feature-mutual.txt", "UTF-8", false);
		mixConfigID = f.addFeatureToBranch(seasonID, branchID, configurationMix, featureID1, sessionToken);
		Assert.assertFalse(mixConfigID.contains("error"), "Configuration mix was not added to the season");

		String configuration1 = FileUtils.fileToString(filePath + "configuration_rule1.txt", "UTF-8", false);
		configID1 = f.addFeatureToBranch(seasonID, branchID, configuration1, mixConfigID, sessionToken);
		Assert.assertFalse(configID1.contains("error"), "Configuration rule1 was not added to the season");
				
		String configuration2 = FileUtils.fileToString(filePath + "configuration_rule2.txt", "UTF-8", false);
		configID2 = f.addFeatureToBranch(seasonID, branchID, configuration2, mixConfigID, sessionToken);
		Assert.assertFalse(configID2.contains("error"), "Configuration rule1 was not added to the season");

	}

	
	@Test(dependsOnMethods="addComponents", description ="Delete configuration rule")
	public void deleteConfiguration() throws Exception{
		
		int codeResponse = f.deleteFeatureFromBranch(configID2, branchID, sessionToken);
		Assert.assertTrue(codeResponse == 200, "Configuraton was not deleted from branch");
		
		String response = br.getBranchWithFeatures(branchID, sessionToken);
		JSONObject brJson = new JSONObject(response);
		JSONArray features = brJson.getJSONArray("features");

		Assert.assertTrue(features.getJSONObject(0).getJSONArray("configurationRules").getJSONObject(0).getJSONArray("configurationRules").size() == 1, "Configuration rule was not deleted");
	}
	
	@Test(dependsOnMethods="deleteConfiguration", description ="Delete mix of configuration rules")
	public void deleteConfigurationMix() throws Exception{
		
		int codeResponse = f.deleteFeatureFromBranch(mixConfigID, branchID, sessionToken);
		Assert.assertTrue(codeResponse == 200, "Configuraton mix was not deleted from branch");
		
		String response = br.getBranchWithFeatures(branchID, sessionToken);
		JSONObject brJson = new JSONObject(response);
		JSONArray features = brJson.getJSONArray("features");

		Assert.assertTrue(features.getJSONObject(0).getJSONArray("configurationRules").size() == 0, "Configuration rule was not deleted");
		
		response = f.getFeature(configID2, sessionToken);
		Assert.assertTrue(response.contains("error"), "Second configuration was not deleted  from branch");
	}
	
	@Test(dependsOnMethods="deleteConfigurationMix", description ="Delete feature with configurations")
	public void deleteFeature() throws Exception{
		//configurations were deleted by previous tests, add them again
		String configurationMix = FileUtils.fileToString(filePath + "configuration_feature-mutual.txt", "UTF-8", false);
		mixConfigID = f.addFeatureToBranch(seasonID, branchID, configurationMix, featureID1, sessionToken);
		Assert.assertFalse(mixConfigID.contains("error"), "Configuration mix was not added to the season: " + mixConfigID);

		String configuration1 = FileUtils.fileToString(filePath + "configuration_rule1.txt", "UTF-8", false);
		configID1 = f.addFeatureToBranch(seasonID, branchID, configuration1, mixConfigID, sessionToken);
		Assert.assertFalse(configID1.contains("error"), "Configuration rule1 was not added to the season: " + configID1);
				
		String configuration2 = FileUtils.fileToString(filePath + "configuration_rule2.txt", "UTF-8", false);
		configID2 = f.addFeatureToBranch(seasonID, branchID, configuration2, mixConfigID, sessionToken);
		Assert.assertFalse(configID2.contains("error"), "Configuration rule1 was not added to the season: " + configID2);

		//delete feature
		int codeResponse = f.deleteFeatureFromBranch(featureID1, branchID, sessionToken);
		Assert.assertTrue(codeResponse == 200, "Configuraton mix was not deleted from branch");
		
		String response = br.getBranchWithFeatures(branchID, sessionToken);
		JSONObject brJson = new JSONObject(response);
		JSONArray features = brJson.getJSONArray("features");

		Assert.assertTrue(features.size() == 0, "Feature was not deleted");
		
		response = f.getFeature(configID1, sessionToken);
		Assert.assertTrue(response.contains("error"), "Configuration1 was not deleted  from branch");
		response = f.getFeature(configID2, sessionToken);
		Assert.assertTrue(response.contains("error"), "Configuration2 was not deleted  from branch");
		response = f.getFeature(mixConfigID, sessionToken);
		Assert.assertTrue(response.contains("error"), "Configuration mix was not deleted  from branch");

	}
	
	@Test (dependsOnMethods="deleteFeature", description="Checked out feature can't be deleted from branch")
	public void deleteCheckedOutFeatureFromBranch() throws IOException, JSONException{
		feature.put("name", "F2");
		featureID2 = f.addFeature(seasonID, feature.toString(), "ROOT", sessionToken);
		Assert.assertFalse(featureID2.contains("error"), "Feature was not added to the season: " + featureID2);
		
		br.checkoutFeature(branchID, featureID2, sessionToken);
		
		int response = f.deleteFeatureFromBranch(featureID2, branchID, sessionToken);
		Assert.assertTrue(response != 200, "Deleted checked out feature from branch");

	}
	
	@Test (dependsOnMethods="deleteCheckedOutFeatureFromBranch", description="Feature with status NONE can't be deleted from branch")
	public void deleteNoneFeatureFromBranch() throws IOException{
		
		String res = br.cancelCheckoutFeature(branchID, featureID2, sessionToken);
		Assert.assertFalse(res.contains("error"), "cannot checkout");
		
		int resp = f.deleteFeatureFromBranch(featureID2, branchID, sessionToken);
		Assert.assertTrue(resp != 200, "Deleted NONE feature from branch");

	}
	
	//Validate that new feature in barnch cannot be deleted if it has chceked_out children.
	@Test (dependsOnMethods="deleteNoneFeatureFromBranch", description="Checked out subfeature can't be deleted from branch")
	public void deleteSubfeatureFromBranch() throws IOException, JSONException{
		feature.put("name", "F3");
		String featureID3 = f.addFeatureToBranch(seasonID, branchID, feature.toString(), "ROOT", sessionToken);
		Assert.assertFalse(featureID3.contains("error"), "Feature was not added to the season: " + featureID3);
		
		br.checkoutFeature(branchID, featureID2, sessionToken);
		
		JSONObject feature3 = new JSONObject(f.getFeatureFromBranch(featureID3, branchID, sessionToken));
		JSONObject feature2 = new JSONObject(f.getFeatureFromBranch(featureID2, branchID, sessionToken));
		JSONArray children = new JSONArray();
		children.add(feature2);
		feature3.put("features", children);
		String response = f.updateFeatureInBranch(seasonID, branchID, featureID3, feature3.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Can't move subfeature to new feature in branch: " + response);
		int resp = f.deleteFeatureFromBranch(featureID3, branchID, sessionToken);
		Assert.assertTrue(resp != 200, "New feature in branch with subfeature in status NONE was deleted from branch");

	}
	


	@AfterTest
	private void reset(){
		baseUtils.reset(productID, sessionToken);
	}
}
