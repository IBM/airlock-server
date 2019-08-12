package tests.restapi.scenarios.experiments;

import java.io.IOException;


import org.apache.commons.lang3.RandomStringUtils;
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

public class CheckoutFeatureScenario10 {
	protected String productID;
	protected String seasonID;
	private String branchID;
	private String mixID;
	private JSONObject fJson;
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
		
		String feature = FileUtils.fileToString(filePath + "feature1.txt", "UTF-8", false);
		fJson = new JSONObject(feature);

		
	}


	
	/*
	 * In master add mtx and under it 3 features.
    	Check the mtx out
    	add new feature under root to branch
    	move the new feature to the checked out mtx in barnch
	 */
	@Test (description ="MTX -> F1, F2, F3 checkout MTX") 
	public void scenario10 () throws Exception {
		branchID = addBranch("branch1");
		Assert.assertFalse(branchID.contains("error"), "Branch1 was not created: " + branchID);

		String featureMix = FileUtils.fileToString(filePath + "feature-mutual.txt", "UTF-8", false);
		mixID = f.addFeature(seasonID, featureMix, "ROOT", sessionToken);
		Assert.assertFalse(mixID.contains("error"), "MTX was not added to the season: " + mixID);

		//add features
		fJson.put("name", RandomStringUtils.randomAlphabetic(5));		
		String featureID1 = f.addFeature(seasonID, fJson.toString(), mixID, sessionToken);
		Assert.assertFalse(featureID1.contains("error"), "Feature1 was not added to the season: " + featureID1);
		
		fJson.put("name", RandomStringUtils.randomAlphabetic(5));		
		String featureID2 = f.addFeature(seasonID, fJson.toString(), mixID, sessionToken);
		Assert.assertFalse(featureID2.contains("error"), "Feature2 was not added to the season: " + featureID2);
		
		fJson.put("name", RandomStringUtils.randomAlphabetic(5));		
		String featureID3 = f.addFeature(seasonID, fJson.toString(), mixID, sessionToken);
		Assert.assertFalse(featureID3.contains("error"), "Feature3 was not added to the season: " + featureID3);

		String response = br.checkoutFeature(branchID, mixID, sessionToken);
		Assert.assertFalse(response.contains("error"), "MTX was not checked out to branch");
	
		//check that feature was checked out in branch
		response = br.getBranchWithFeatures(branchID, sessionToken);
		JSONObject brJson = new JSONObject(response);
		JSONArray features = brJson.getJSONArray("features");
		Assert.assertTrue(features.size()==1, "Incorrect number of checked out features");
		Assert.assertTrue(features.getJSONObject(0).getString("branchStatus").equals("CHECKED_OUT"), "Feature status is not checked_out in get branch" );
		
		//feature is checked out in get feature from branch
		String feature = f.getFeatureFromBranch(mixID, branchID, sessionToken);
		JSONObject json = new JSONObject(feature);
		Assert.assertTrue(json.getString("branchStatus").equals("CHECKED_OUT"), "Feature status is not checked_out in get feature");
		
		//feature is checked out in get features from branch
		JSONArray featuresInBranch = f.getFeaturesBySeasonFromBranch(seasonID, branchID, sessionToken);
		Assert.assertTrue(featuresInBranch.size()==1, "Incorrect number of checked out features");
		Assert.assertTrue(featuresInBranch.getJSONObject(0).getString("branchStatus").equals("CHECKED_OUT"), "Feature status is not checked_out in get features from branch" );
		
	}
	
	@Test (dependsOnMethods="scenario10", description="Add new feature to root in branch and then move it to MTX")
	public void addNewFeature() throws JSONException, IOException{
		fJson.put("name", "NewFeature4");		
		String featureID4 = f.addFeatureToBranch(seasonID, branchID, fJson.toString(), "ROOT", sessionToken);
		Assert.assertFalse(featureID4.contains("error"), "Feature4 was not added to the branch: " + featureID4);

		//move feature to MTX as the first child
		String feature = f.getFeatureFromBranch(mixID, branchID, sessionToken);
		JSONObject json = new JSONObject(feature);

		JSONObject newFeature = new JSONObject(f.getFeatureFromBranch(featureID4, branchID, sessionToken));
		JSONArray children = json.getJSONArray("features");
		JSONArray newChildren = new JSONArray();		
		
		newChildren.put(newFeature);
		for (int i=0; i< children.length(); i++){
			newChildren.put(children.getJSONObject(i));
		}
		
		json.put("features", newChildren);
		String response = f.updateFeatureInBranch(seasonID, branchID, mixID, json.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Failed to update MTX: " + response);
		
		feature = f.getFeatureFromBranch(mixID, branchID, sessionToken);
		JSONObject fJson = new JSONObject(feature);
		Assert.assertTrue(fJson.getJSONArray("features").size()==4, "Incorrect number of children under MTX");		
		Assert.assertTrue(fJson.getJSONArray("features").getJSONObject(0).getString("uniqueId").equals(featureID4), "Incorrect first child under MTX in branch");
	}
	

	private String addBranch(String branchName) throws JSONException, IOException{
		String branch = FileUtils.fileToString(filePath + "experiments/branch1.txt", "UTF-8", false);
		JSONObject branchJson = new JSONObject(branch);
		branchJson.put("name", branchName);
		return br.createBranch(seasonID, branchJson.toString(), BranchesRestApi.MASTER, sessionToken);

	}

	@AfterTest
	private void reset(){
		baseUtils.reset(productID, sessionToken);
	}
}
