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

public class CheckoutFeatureScenario1 {
	protected String productID;
	protected String seasonID;
	private String branchID;
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


	
	//F1 -> CR1, CR2
	@Test (description ="F1 -> CR1, CR2, checkout F1") 
	public void scenario1 () throws Exception {
		branchID = addBranch("branch1");
		Assert.assertFalse(branchID.contains("error"), "Branch1 was not created: " + branchID);

		//add feature with configuration
		fJson.put("name", RandomStringUtils.randomAlphabetic(5));		
		String featureID1 = f.addFeature(seasonID, fJson.toString(), "ROOT", sessionToken);
		Assert.assertFalse(featureID1.contains("error"), "Feature was not added to the season: " + featureID1);
				
		String configuration = FileUtils.fileToString(filePath + "configuration_rule1.txt", "UTF-8", false);
		String configID1 = f.addFeature(seasonID, configuration, featureID1, sessionToken);
		Assert.assertFalse(configID1.contains("error"), "Configuration1 was not added to the season");
		
		String configuration2 = FileUtils.fileToString(filePath + "configuration_rule2.txt", "UTF-8", false);
		String configID2 = f.addFeature(seasonID, configuration2, featureID1, sessionToken);
		Assert.assertFalse(configID2.contains("error"), "Configuration2 was not added to the season");
		
		String response = br.checkoutFeature(branchID, featureID1, sessionToken);
		Assert.assertFalse(response.contains("error"), "feature was not checked out to branch");
		
		//feature can't be checked out twice
		response = br.checkoutFeature(branchID, featureID1, sessionToken);
		Assert.assertTrue(response.contains("error"), "feature was checked out twice");
		
		//check that feature was checked out in branch
		response = br.getBranchWithFeatures(branchID, sessionToken);
		JSONObject brJson = new JSONObject(response);
		JSONArray features = brJson.getJSONArray("features");
		Assert.assertTrue(features.size()==1, "Incorrect number of checked out features");
		Assert.assertTrue(features.getJSONObject(0).getString("branchStatus").equals("CHECKED_OUT"), "Feature status is not checked_out in get branch" );
		
		//feature is checked out in get feature from branch
		String feature = f.getFeatureFromBranch(featureID1, branchID, sessionToken);
		fJson = new JSONObject(feature);
		Assert.assertTrue(fJson.getString("branchStatus").equals("CHECKED_OUT"), "Feature status is not checked_out in get feature");
		
		//feature is checked out in get features from branch
		JSONArray featuresInBranch = f.getFeaturesBySeasonFromBranch(seasonID, branchID, sessionToken);
		Assert.assertTrue(featuresInBranch.size()==1, "Incorrect number of checked out features");
		Assert.assertTrue(featuresInBranch.getJSONObject(0).getString("branchStatus").equals("CHECKED_OUT"), "Feature status is not checked_out in get features from branch" );
		
		//check that configuration was checked out
		Assert.assertTrue(features.getJSONObject(0).getJSONArray("configurationRules").getJSONObject(0).getString("branchStatus").equals("CHECKED_OUT"), "Configuration rule1 status is not checked_out in get branch" );
		Assert.assertTrue(features.getJSONObject(0).getJSONArray("configurationRules").getJSONObject(1).getString("branchStatus").equals("CHECKED_OUT"), "Configuration rule2 status is not checked_out in get branch" );
		Assert.assertTrue(new JSONObject(f.getFeatureFromBranch(configID1, branchID, sessionToken)).getString("branchStatus").equals("CHECKED_OUT"), "Configuration rule1 status status is not checked_out in get feature");	//get configuration rule from branch
		Assert.assertTrue(new JSONObject(f.getFeatureFromBranch(configID2, branchID, sessionToken)).getString("branchStatus").equals("CHECKED_OUT"), "Configuration rule1 status status is not checked_out in get feature");	//get configuration rule from branch
		Assert.assertTrue(featuresInBranch.getJSONObject(0).getJSONArray("configurationRules").getJSONObject(0).getString("branchStatus").equals("CHECKED_OUT"), "Configuration rule1 status is not checked_out in get features from branch" );
		Assert.assertTrue(featuresInBranch.getJSONObject(0).getJSONArray("configurationRules").getJSONObject(1).getString("branchStatus").equals("CHECKED_OUT"), "Configuration rule2 status is not checked_out in get features from branch" );
		
		
		//uncheckout F1
		String res = br.cancelCheckoutFeature(branchID, featureID1, sessionToken);
		Assert.assertFalse(res.contains("error"), "Feature was not unchecked out: " + res);
		brJson = new JSONObject(br.getBranchWithFeatures(branchID, sessionToken));
		Assert.assertTrue(brJson.getJSONArray("features").size()==0, "Incorrect number of checked out features");
		
		featuresInBranch = f.getFeaturesBySeasonFromBranch(seasonID, branchID, sessionToken);
		Assert.assertTrue(featuresInBranch.size()==1, "Incorrect number of features in branch");
		Assert.assertTrue(featuresInBranch.getJSONObject(0).getString("branchStatus").equals("NONE"), "Feature status is not NONE in get features from branch" );
		Assert.assertTrue(featuresInBranch.getJSONObject(0).getJSONArray("configurationRules").getJSONObject(0).getString("branchStatus").equals("NONE"), "Configuration rule1 status is not NONE in get features from branch" );
		Assert.assertTrue(featuresInBranch.getJSONObject(0).getJSONArray("configurationRules").getJSONObject(1).getString("branchStatus").equals("NONE"), "Configuration rule2 status is not NONE in get features from branch" );
		
		Assert.assertTrue(new JSONObject(f.getFeatureFromBranch(featureID1, branchID, sessionToken)).getString("branchStatus").equals("NONE"), "Feature status status is not NONE in get feature");	
		Assert.assertTrue(new JSONObject(f.getFeatureFromBranch(configID1, branchID, sessionToken)).getString("branchStatus").equals("NONE"), "Configuration rule1 status status is not NONE in get feature");	//get configuration rule from branch
		Assert.assertTrue(new JSONObject(f.getFeatureFromBranch(configID2, branchID, sessionToken)).getString("branchStatus").equals("NONE"), "Configuration rule2 status status is not NONE in get feature");	//get configuration rule from branch

		
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
