package tests.restapi.scenarios.experiments;

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

public class DeleteParentFeatureFromMaster {
	protected String productID;
	protected String seasonID;
	private String branchID;
	private String featureID1;
	private String featureID2;
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


	@Test (description ="Add  F1 feature to master, check it out and add F2 under F1 in branch and add new branch") 
	public void addComponents () throws IOException, JSONException, InterruptedException {
		
		String feature = FileUtils.fileToString(filePath + "feature1.txt", "UTF-8", false);
		featureID1 = f.addFeature(seasonID, feature, "ROOT", sessionToken);
		Assert.assertFalse(featureID1.contains("error"), "Feature was not added to the season: " + featureID1);
				
		String branch = FileUtils.fileToString(filePath + "experiments/branch1.txt", "UTF-8", false);
		branchID= br.createBranch(seasonID, branch, BranchesRestApi.MASTER, sessionToken);
		
		String response = br.checkoutFeature(branchID, featureID1, sessionToken);
		Assert.assertFalse(response.contains("error"), "feature was not checked out to branch");

		JSONObject featureObj = new JSONObject(feature);
		featureObj.put("name", "F2");
		featureID2 = f.addFeatureToBranch(seasonID, branchID, featureObj.toString(), featureID1, sessionToken);
		Assert.assertFalse(featureID2.contains("error"), "Feature2 was not added to the season: " + featureID2);
	}

	
	@Test(dependsOnMethods="addComponents", description ="Uncheckout F1 feature from branch")
	public void unChcekoutFeatureWithNewSubFeature() throws Exception{
		
		String response = br.cancelCheckoutFeature(branchID, featureID1, sessionToken);
		Assert.assertTrue(response.contains("error") && response.contains("Unable to cancel checkout. The item has NEW sub-items in the branch."), "feature was un-checked from to branch even though it gas new sub feature");

		int codeResponse = f.deleteFeatureFromBranch(featureID2, branchID, sessionToken);
		Assert.assertTrue(codeResponse == 200, "feature2 was not deleted from branch");
		
/*		//verify that F2 branch parent is now ns1.F1
		response = br.getBranchWithFeatures(branchID, sessionToken);
		JSONObject brJson = new JSONObject(response);
		JSONArray features = brJson.getJSONArray("features");
		Assert.assertTrue(features.size()==1, "Incorrect number of features in branch");
		Assert.assertTrue(features.getJSONObject(0).getString("branchFeatureParentName").equals("ns1.Feature1"), "Incorrect branch parent name");
		Assert.assertTrue(features.getJSONObject(0).getString("branchStatus").equals("NEW"), "Incorrect branch status");*/
	}
/*	
	@Test (dependsOnMethods="unChcekoutFeature", description ="delete F1 from master") 
	public void deleteF1FromMaster () throws Exception {
		int codeResponse = f.deleteFeature(featureID1, sessionToken);
		Assert.assertTrue(codeResponse == 200, "Master feature2 was not deleted");
		
		
		//verify that F2 branch parent is now ns1.F1
		//TODO: what should be the branch structure? 
		String response = br.getBranchWithFeatures(branchID, sessionToken);
		JSONObject brJson = new JSONObject(response);
		JSONArray features = brJson.getJSONArray("features");
		Assert.assertTrue(features.size()==1, "Incorrect number of checked out features");
		Assert.assertTrue(features.getJSONObject(0).getString("branchFeatureParentName").equals("ns1.Feature1"), "Incorrect branch parent name");
		Assert.assertTrue(features.getJSONObject(0).getString("branchStatus").equals("NEW"), "Incorrect branch status");
	}
	*/
	
	@Test(dependsOnMethods="unChcekoutFeatureWithNewSubFeature", description ="Uncheckout F1 feature from branch")
	public void unChcekoutFeatureWithNewSubConfigurationRule() throws Exception{
		
		String configuration = FileUtils.fileToString(filePath + "configuration_rule1.txt", "UTF-8", false);
		String configID = f.addFeatureToBranch(seasonID, branchID, configuration, featureID1, sessionToken);
		Assert.assertFalse(configID.contains("error"), "Configuration1 was not added to the season: " + configID);

		String response = br.cancelCheckoutFeature(branchID, featureID1, sessionToken);
		Assert.assertTrue(response.contains("error") && response.contains("Unable to cancel checkout. The item has NEW sub-items in the branch."), "feature was un-checked from to branch even though it gas new sub feature");
		
		int codeResponse = f.deleteFeatureFromBranch(configID, branchID, sessionToken);
		Assert.assertTrue(codeResponse == 200, "feature2 was not deleted from branch");
		
	}

	@Test(dependsOnMethods="unChcekoutFeatureWithNewSubConfigurationRule", description ="Uncheckout F1 feature from branch")
	public void unChcekoutFeatureWithNewSubOrderingRule() throws Exception{
		String feature = FileUtils.fileToString(filePath + "feature1.txt", "UTF-8", false);
		
		JSONObject featureObj = new JSONObject(feature);
		featureObj.put("name", "F2");
		featureID2 = f.addFeature(seasonID, featureObj.toString(), featureID1, sessionToken);
		Assert.assertFalse(featureID2.contains("error"), "Feature2 was not added to the season: " + featureID2);
		
		String orderingRule = FileUtils.fileToString(filePath + "orderingRule/orderingRule1.txt", "UTF-8", false);
		String orderingRuleId = f.addFeatureToBranch(seasonID, branchID, orderingRule, featureID1, sessionToken);
		Assert.assertFalse(orderingRuleId.contains("error"), "orderingRule was not added to the season: " + orderingRuleId);

		String response = br.cancelCheckoutFeature(branchID, featureID1, sessionToken);
		Assert.assertTrue(response.contains("error") && response.contains("Unable to cancel checkout. The item has NEW sub-items in the branch."), "feature was un-checked from to branch even though it gas new sub feature");
	}

	@AfterTest
	private void reset(){
		//baseUtils.reset(productID, sessionToken);
	}
}
