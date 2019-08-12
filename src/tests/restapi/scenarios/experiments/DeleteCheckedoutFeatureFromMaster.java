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

public class DeleteCheckedoutFeatureFromMaster {
	protected String productID;
	protected String seasonID;
	private String branchID;
	private String featureID1;
	private String featureID2;
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
		create feature in master, check it out in branch and then delete is from master. validate that is now new in branch
	 */

	//F1->CR1
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

	
	@Test(dependsOnMethods="addComponents", description ="Delete feature from master")
	public void deleteFeatureFromMaster() throws Exception{
		
		int codeResponse = f.deleteFeature(featureID1, sessionToken);
		Assert.assertTrue(codeResponse == 200, "Master feature was deleted");
		
		//verify that feature is now NEW in  branch
		String response = br.getBranchWithFeatures(branchID, sessionToken);
		JSONObject brJson = new JSONObject(response);
		JSONArray features = brJson.getJSONArray("features");
		Assert.assertTrue(features.size()==1, "Incorrect number of checked out features");
		Assert.assertTrue(features.getJSONObject(0).getJSONArray("configurationRules").size()==1, "Incorrect number of checked out configuration rules");
		
		
		//status changes to NEW
		Assert.assertTrue(features.getJSONObject(0).getString("branchStatus").equals("NEW"), "Deleted from master feature status in branch is not NEW" );
		Assert.assertTrue(features.getJSONObject(0).getJSONArray("configurationRules").getJSONObject(0).getString("branchStatus").equals("NEW"), "Deleted from master configuration rule status in branch is not NEW" );
		//new uniqueId is assigned
		Assert.assertFalse(features.getJSONObject(0).getString("uniqueId").equals(featureID1), "Deleted from master feature uniqueId was not changed" );
		Assert.assertFalse(features.getJSONObject(0).getJSONArray("configurationRules").getJSONObject(0).getString("uniqueId").equals(configID), "Deleted from master configuration rule uniqueId was not changed" );

		String newFeatureIdInBranch = features.getJSONObject(0).getString("uniqueId");
		
		codeResponse = f.deleteFeatureFromBranch(newFeatureIdInBranch, branchID, sessionToken);
		Assert.assertTrue(codeResponse == 200, "Master feature was deleted from branch");
		
		response = br.getBranchWithFeatures(branchID, sessionToken);
		brJson = new JSONObject(response);
		features = brJson.getJSONArray("features");
		Assert.assertTrue(features.size()==0, "Incorrect number of checked out features");	
	}
	
	//F1->F2
	@Test (dependsOnMethods="deleteFeatureFromMaster", description ="Add 2 features to master, checkout F2 and uncheckout F1") 
	public void addComponents2 () throws IOException, JSONException, InterruptedException {
		
		String feature = FileUtils.fileToString(filePath + "feature1.txt", "UTF-8", false);
		featureID1 = f.addFeature(seasonID, feature, "ROOT", sessionToken);
		Assert.assertFalse(featureID1.contains("error"), "Feature1 was not added to the season: " + featureID1);
				
		JSONObject featureObj = new JSONObject(feature);
		featureObj.put("name", "F2");
		featureID2 = f.addFeature(seasonID, featureObj.toString(), featureID1, sessionToken);
		Assert.assertFalse(configID.contains("error"), "Feature2 was not added to the season: " + configID);

		String response = br.checkoutFeature(branchID, featureID2, sessionToken);
		Assert.assertFalse(response.contains("error"), "feature2 was not checked out to branch");
		
		response = br.cancelCheckoutFeature(branchID, featureID1, sessionToken);
		Assert.assertFalse(response.contains("error"), "feature1 was not un-checked out to branch");
	}
	
	@Test(dependsOnMethods="addComponents2", description ="Delete feature from master")
	public void deleteFeature2FromMaster() throws Exception{
		
		int codeResponse = f.deleteFeature(featureID2, sessionToken);
		Assert.assertTrue(codeResponse == 200, "Master feature2 was not deleted");
		
		//verify that feature is now NEW in  branch
		String response = br.getBranchWithFeatures(branchID, sessionToken);
		JSONObject brJson = new JSONObject(response);
		JSONArray features = brJson.getJSONArray("features");
		Assert.assertTrue(features.size()==1, "Incorrect number of checked out features");
		Assert.assertTrue(features.getJSONObject(0).getString("branchStatus").equals("NEW"), "Deleted from master feature status in branch is not NEW" );
		Assert.assertFalse(features.getJSONObject(0).getString("uniqueId").equals(featureID2), "Deleted from master feature uniqueId was not changed" );
		Assert.assertTrue(features.getJSONObject(0).getString("name").equals("F2"), "Deleted from master feature uniqueId was not changed" );		
	}

	@AfterTest
	private void reset(){
		baseUtils.reset(productID, sessionToken);
	}
}
