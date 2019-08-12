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

public class DeleteFeatureFromMaster {
	protected String productID;
	protected String seasonID;
	private String branchID;
	private String featureID1;
	private String configID;
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
		br = new BranchesRestApi();
		br.setURL(m_url);

		baseUtils = new AirlockUtils(m_url, analyticsUrl, translationsUrl, configPath, sToken, userName, userPassword, appName, productsToDeleteFile);
		sessionToken = baseUtils.sessionToken;
		f = new FeaturesRestApi();
		f.setURL(m_url);

		productID = baseUtils.createProduct();
		baseUtils.printProductToFile(productID);
		seasonID = baseUtils.createSeason(productID);

	}
	
	/*

	Delete feature from branch
		- no delete feature in branch  if feature is from master
		 if sub-feature is checked-out, no delete: add new feature to branch. checkout feature from master and update its parent
		 to this new branch feature. delete this new branch feature - not allowed
		- add feature+config to master. check out in branch and delete from master. both feature and its configurations should
		 get new ids
		- checkout-out feature and delete from master. It should get new uniqueId in branch and status NEW
		- checkout-out feature with configuration rule  and delete from master. Configuration rule should get new uniqueId in branch and status NEW
		
		- add feature with 2 sub-features to master. Check out all of them. Uncheck parent feature and delete from master. In branch it should get status NEW
		and sub-features should get it as a parent
 */

	@Test (description ="Add features to master and add new branch") 
	public void addComponents () throws IOException, JSONException, InterruptedException {
		
		String feature = FileUtils.fileToString(filePath + "feature1.txt", "UTF-8", false);
		featureID1 = f.addFeature(seasonID, feature, "ROOT", sessionToken);
		Assert.assertFalse(featureID1.contains("error"), "Feature was not added to the season: " + featureID1);
				
		String configuration = FileUtils.fileToString(filePath + "configuration_rule1.txt", "UTF-8", false);
		configID = f.addFeature(seasonID, configuration, featureID1, sessionToken);
		Assert.assertFalse(configID.contains("error"), "Configuration1 was not added to the season: " + configID);

		String branch = FileUtils.fileToString(filePath + "experiments/branch1.txt", "UTF-8", false);
		branchID= br.createBranch(seasonID, branch, BranchesRestApi.MASTER, sessionToken);
		
		String response = br.checkoutFeature(branchID, featureID1, sessionToken);
		Assert.assertFalse(response.contains("error"), "feature was not checked out to branch");

}

	
	@Test(dependsOnMethods="addComponents", description ="Delete feature from branch when it was created in master - not allowed")
	public void deleteMasterFeatureFromBranch() throws Exception{
		
		int codeResponse = f.deleteFeatureFromBranch(featureID1, branchID, sessionToken);
		Assert.assertFalse(codeResponse == 200, "Master feature was deleted from branch");
		
		codeResponse = f.deleteFeatureFromBranch(configID, branchID, sessionToken);
		Assert.assertFalse(codeResponse == 200, "Master configuration rule was deleted from branch");
	}
	

	@Test(dependsOnMethods="deleteMasterFeatureFromBranch", description ="Delete configuration rule of checked out feature from  master should leave it in branch with new status")
	public void deleteMasterConfigurationRule() throws Exception{
		int codeResponse = f.deleteFeature(configID, sessionToken);
		Assert.assertTrue(codeResponse == 200, "Configuration rule was not deleted from master");
		
		String response = br.getBranchWithFeatures(branchID, sessionToken);
		JSONObject brJson = new JSONObject(response);
		JSONArray features = brJson.getJSONArray("features");
		Assert.assertTrue(features.size()==1, "Incorrect number of checked out features");
		//status changes to NEW
		Assert.assertTrue(features.getJSONObject(0).getJSONArray("configurationRules").getJSONObject(0).getString("branchStatus").equals("NEW"), "Deleted from master configuration rule status in branch is not NEW" );
		//new uniqueId is assigned
		Assert.assertFalse(features.getJSONObject(0).getJSONArray("configurationRules").getJSONObject(0).getString("uniqueId").equals(configID), "Deleted from master configuration rule uniqueId was not changed" );

	}
	
	@Test(dependsOnMethods="deleteMasterConfigurationRule", description ="Delete feature from  master should leave it in branch with new status")
	public void deleteMasterFeature() throws Exception{
		int codeResponse = f.deleteFeature(featureID1, sessionToken);
		Assert.assertTrue(codeResponse == 200, "Feature was not deleted from master");
		
		String response = br.getBranchWithFeatures(branchID, sessionToken);
		JSONObject brJson = new JSONObject(response);
		JSONArray features = brJson.getJSONArray("features");
		Assert.assertTrue(features.size()==1, "Incorrect number of checked out features");
		//status changes to NEW
		Assert.assertTrue(features.getJSONObject(0).getString("branchStatus").equals("NEW"), "Deleted from master feature status in branch is not NEW" );		
		//new uniqueId is assigned
		Assert.assertFalse(features.getJSONObject(0).getString("uniqueId").equals(featureID1), "Deleted from master feature uniqueId was not changed" );
		
		codeResponse = f.deleteFeatureFromBranch(features.getJSONObject(0).getString("uniqueId"), branchID, sessionToken);
		Assert.assertTrue(codeResponse == 200, "Feature was not deleted from branch");

	}

	@Test (dependsOnMethods="deleteMasterFeature", description ="Add and delete feature with configuration") 
	public void addDeleteFeatureWithConfiguration () throws Exception {
		//add feature and configuration to master
		String feature = FileUtils.fileToString(filePath + "feature1.txt", "UTF-8", false);
		featureID1 = f.addFeature(seasonID, feature, "ROOT", sessionToken);
		Assert.assertFalse(featureID1.contains("error"), "Feature was not added to the season: " + featureID1);
				
		String configuration = FileUtils.fileToString(filePath + "configuration_rule1.txt", "UTF-8", false);
		configID = f.addFeature(seasonID, configuration, featureID1, sessionToken);
		Assert.assertFalse(configID.contains("error"), "Configuration1 was not added to the season: " + configID);

		//checkout feature to branch
		String response = br.checkoutFeature(branchID, featureID1, sessionToken);
		Assert.assertFalse(response.contains("error"), "feature was not checked out to branch");

		//delete feature from master
		int codeResponse = f.deleteFeature(featureID1, sessionToken);
		Assert.assertTrue(codeResponse == 200, "Feature was not deleted from master");
		
		//validate statuses and ids of feature+configuration in branch
		response = br.getBranchWithFeatures(branchID, sessionToken);
		JSONObject brJson = new JSONObject(response);
		JSONArray features = brJson.getJSONArray("features");
		Assert.assertTrue(features.size()==1, "Incorrect number of checked out features");
		//feature status changes to NEW, new uniqueId is assigned
		Assert.assertTrue(features.getJSONObject(0).getString("branchStatus").equals("NEW"), "Deleted from master feature status in branch is not NEW" );		
		Assert.assertFalse(features.getJSONObject(0).getString("uniqueId").equals(featureID1), "Deleted from master feature uniqueId was not changed" );
		
		//configuration status changes to NEW, new uniqueId is assigned
		Assert.assertTrue(features.getJSONObject(0).getJSONArray("configurationRules").getJSONObject(0).getString("branchStatus").equals("NEW"), "Deleted from master configuration status in branch is not NEW" );		
		Assert.assertFalse(features.getJSONObject(0).getJSONArray("configurationRules").getJSONObject(0).getString("uniqueId").equals(configID), "Deleted from master configuration uniqueId in branch is not changed" );
	}
	
	@Test (dependsOnMethods="addDeleteFeatureWithConfiguration", description="Delete feature with sub-features from master")
	public void deleteFeatureWithSubFeatures() throws Exception{
		String feature = FileUtils.fileToString(filePath + "feature1.txt", "UTF-8", false);
		JSONObject json = new JSONObject(feature);
		json.put("name", "masterF");
		String parentFeature = f.addFeature(seasonID, json.toString(), "ROOT", sessionToken);
		Assert.assertFalse(parentFeature.contains("error"), "Parent feature was not added to the season: " + parentFeature);
		
		json.put("name", "child1");
		String child1 = f.addFeature(seasonID, json.toString(), parentFeature, sessionToken);
		Assert.assertFalse(child1.contains("error"), "Feature1 was not added to the season: " + child1);
		
		json.put("name", "child2");
		String child2 = f.addFeature(seasonID, json.toString(), parentFeature, sessionToken);
		Assert.assertFalse(child2.contains("error"), "Feature2 was not added to the season: " + child2);
		
		String response = br.checkoutFeature(branchID, child1, sessionToken); //checkout sub-feature also checks out its parent
		Assert.assertFalse(response.contains("error"), "cannot checkout");
		br.checkoutFeature(branchID, child2, sessionToken);
		Assert.assertFalse(response.contains("error"), "cannot checkout");
		br.cancelCheckoutFeature(branchID, parentFeature, sessionToken);
		Assert.assertFalse(response.contains("error"), "cannot cancel checkout");
		
		int code = f.deleteFeature(parentFeature, sessionToken);
		Assert.assertTrue(code==200, "Can't delete parent feature");
		
		response = br.getBranchWithFeatures(branchID, sessionToken);
		JSONObject brJson = new JSONObject(response);
		JSONArray features = brJson.getJSONArray("features");
		
		boolean found = false;
		for (int i=0; i<features.size(); i++){
			if (features.getJSONObject(i).getString("name").equals("masterF")){
				found = true;
				Assert.assertTrue(features.getJSONObject(i).getString("branchStatus").equals("NEW"));
				Assert.assertFalse(features.getJSONObject(i).getString("uniqueId").equals(parentFeature));
				JSONArray subfeatures = features.getJSONObject(i).getJSONArray("features");
				Assert.assertTrue(subfeatures.getJSONObject(0).getString("branchStatus").equals("NEW"), "Child1 status was not changed to NEW");
				Assert.assertTrue(subfeatures.getJSONObject(1).getString("branchStatus").equals("NEW"), "Child2 status was not changed to NEW");
			} 
		}
		
		Assert.assertTrue(found, "Parent feature was not found in branch");
	

	}
	
	
	@Test (dependsOnMethods="deleteFeatureWithSubFeatures", description ="Delete configuration rule") 
	public void deleteConfigurationRule () throws Exception {
		//add feature and 2 configurations to master
		String feature = FileUtils.fileToString(filePath + "feature2.txt", "UTF-8", false);
		featureID1 = f.addFeature(seasonID, feature, "ROOT", sessionToken);
		Assert.assertFalse(featureID1.contains("error"), "Feature was not added to the season: " + featureID1);
				
		JSONObject configuration = new JSONObject(FileUtils.fileToString(filePath + "configuration_rule1.txt", "UTF-8", false));
		configuration.put("name", "configToDel1");
		configID = f.addFeature(seasonID, configuration.toString(), featureID1, sessionToken);
		Assert.assertFalse(configID.contains("error"), "Configuration1 was not added to the season: " + configID);
		configuration.put("name", "configToDel2");
		String configID2 = f.addFeature(seasonID, configuration.toString(), featureID1, sessionToken);
		Assert.assertFalse(configID2.contains("error"), "Configuration2 was not added to the season: " + configID2);

		//checkout feature to branch
		String response = br.checkoutFeature(branchID, featureID1, sessionToken);
		Assert.assertFalse(response.contains("error"), "feature was not checked out to branch");
		
		//delete configuration from master
		int codeResponse = f.deleteFeature(configID2, sessionToken);
		Assert.assertTrue(codeResponse == 200, "Configuration was not deleted from master");
		
		//validate statuses and ids of feature+configuration in branch
		JSONObject resp = new JSONObject(f.getFeatureFromBranch(featureID1, branchID, sessionToken));
		
		//configuration status changes to NEW, new uniqueId is assigned
		Assert.assertTrue(resp.getJSONArray("configurationRules").size()==2, "Configuration deleted from master was also deleted from branch" );
		Assert.assertTrue(resp.getJSONArray("configurationRules").getJSONObject(1).getString("branchStatus").equals("NEW"), "Deleted from master configuration status in branch is not NEW" );		
		
	}

	@AfterTest
	private void reset(){
		baseUtils.reset(productID, sessionToken);
	}
}
