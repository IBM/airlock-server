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

public class CheckoutFeatureScenario6 {
	protected String productID;
	protected String seasonID;
	private String branchID;
	private String featureID1;
	private String configID1;
	private String configID2;
	private String configID3;
	private String mixConfigID;
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
	

	@Test (description ="F1 -> CR1 -> MIXCR ->(CR2+ CR3)") 
	public void scenario6 () throws Exception {
		branchID = addBranch("branch1");
		Assert.assertFalse(branchID.contains("error"), "Branch1 was not created: " + branchID);
		String configuration1 = FileUtils.fileToString(filePath + "configuration_rule1.txt", "UTF-8", false);

		fJson.put("name", RandomStringUtils.randomAlphabetic(5));
		featureID1 = f.addFeature(seasonID, fJson.toString(), "ROOT", sessionToken);
		Assert.assertFalse(featureID1.contains("error"), "Feature was not added to the season");
		
		JSONObject jsonCR = new JSONObject(configuration1);
		jsonCR.put("name", "CR1");
		configID1 = f.addFeature(seasonID, jsonCR.toString(), featureID1, sessionToken);
		Assert.assertFalse(configID1.contains("error"), "Configuration rule1 was not added to the season");
		
		String configurationMix = FileUtils.fileToString(filePath + "configuration_feature-mutual.txt", "UTF-8", false);
		mixConfigID = f.addFeature(seasonID, configurationMix, configID1, sessionToken);
		Assert.assertFalse(mixConfigID.contains("error"), "Configuration mix was not added to the season");

		jsonCR.put("name", "CR2");
		configID2 = f.addFeature(seasonID, jsonCR.toString(), mixConfigID, sessionToken);
		Assert.assertFalse(configID2.contains("error"), "Configuration rule2 was not added to the season");
				
		jsonCR.put("name", "CR3");
		configID3 = f.addFeature(seasonID, jsonCR.toString(), mixConfigID, sessionToken);
		Assert.assertFalse(configID3.contains("error"), "Configuration rule3 was not added to the season");
		
		String response = br.checkoutFeature(branchID, featureID1, sessionToken);
		Assert.assertFalse(response.contains("error"), "feature was not checked out to branch");
		
		JSONArray featuresInBranch = f.getFeaturesBySeasonFromBranch(seasonID, branchID, sessionToken);
		
		//check that configuration rules were checked out
		response = br.getBranchWithFeatures(branchID, sessionToken);
		JSONObject brJson = new JSONObject(response);
		JSONArray features = brJson.getJSONArray("features");
		
		//F1
		Assert.assertTrue(features.getJSONObject(0).getString("branchStatus").equals("CHECKED_OUT"), "Feature status is not checked_out in get branch" );
		Assert.assertTrue(featuresInBranch.getJSONObject(0).getString("branchStatus").equals("CHECKED_OUT"), "Feature status is not checked_out in get features" );
		
		//CR1
		Assert.assertTrue(features.getJSONObject(0).getJSONArray("configurationRules").getJSONObject(0).getString("branchStatus").equals("CHECKED_OUT"), "Configuration rule1 status is not checked_out" );
		Assert.assertTrue(featuresInBranch.getJSONObject(0).getJSONArray("configurationRules").getJSONObject(0).getString("branchStatus").equals("CHECKED_OUT"), "Configuration rule1 status is not checked_out" );
		Assert.assertTrue(new JSONObject(f.getFeatureFromBranch(configID1, branchID, sessionToken)).getString("branchStatus").equals("CHECKED_OUT"), "Configuration rule1 status status is not checked_out in get feature");	//get configuration rule from branch
		
		
		//CR2
		Assert.assertTrue(features.getJSONObject(0).getJSONArray("configurationRules").getJSONObject(0)
				.getJSONArray("configurationRules").getJSONObject(0).getJSONArray("configurationRules").getJSONObject(0)
				.getString("branchStatus").equals("CHECKED_OUT"), "Configuration rule2 status is not checked_out" );
		Assert.assertTrue(featuresInBranch.getJSONObject(0).getJSONArray("configurationRules").getJSONObject(0)
				.getJSONArray("configurationRules").getJSONObject(0).getJSONArray("configurationRules").getJSONObject(0)
				.getString("branchStatus").equals("CHECKED_OUT"), "Configuration rule2 status is not checked_out" );
		Assert.assertTrue(new JSONObject(f.getFeatureFromBranch(configID2, branchID, sessionToken)).getString("branchStatus").equals("CHECKED_OUT"), "Configuration rule2 status status is not checked_out in get feature");	//get configuration rule from branch
		
		//CR3
		Assert.assertTrue(features.getJSONObject(0).getJSONArray("configurationRules").getJSONObject(0)
				.getJSONArray("configurationRules").getJSONObject(0).getJSONArray("configurationRules").getJSONObject(1)
				.getString("branchStatus").equals("CHECKED_OUT"), "Configuration rule2 status is not checked_out" );
		Assert.assertTrue(featuresInBranch.getJSONObject(0).getJSONArray("configurationRules").getJSONObject(0)
				.getJSONArray("configurationRules").getJSONObject(0).getJSONArray("configurationRules").getJSONObject(1)
				.getString("branchStatus").equals("CHECKED_OUT"), "Configuration rule2 status is not checked_out" );
		Assert.assertTrue(new JSONObject(f.getFeatureFromBranch(configID3, branchID, sessionToken)).getString("branchStatus").equals("CHECKED_OUT"), "Configuration rule3 status status is not checked_out in get feature");	//get configuration rule from branch
	
		//MIXCR
		//MIXCR
		Assert.assertTrue(featuresInBranch.getJSONObject(0).getJSONArray("configurationRules").getJSONObject(0)
				.getJSONArray("configurationRules").getJSONObject(0)
				.getString("branchStatus").equals("CHECKED_OUT"), "MIX configuration status is not CHECKED_OUT" );
		Assert.assertTrue(new JSONObject(f.getFeatureFromBranch(mixConfigID, branchID, sessionToken)).getString("branchStatus").equals("CHECKED_OUT"), "Configuration rule3 status status is not CHECKED_OUT in get feature");	//get configuration rule from branch

	}
	
	@Test (dependsOnMethods="scenario6", description="Uncheck F1")
	public void uncheckF1() throws JSONException, Exception{				
		
		//uncheckout F1
		String res = br.cancelCheckoutFeature(branchID, featureID1, sessionToken);
		Assert.assertFalse(res.contains("error"), "Feature was not unchecked out: " + res);
		JSONObject brJson = new JSONObject(br.getBranchWithFeatures(branchID, sessionToken));
		
		
		JSONArray featuresInBranch = f.getFeaturesBySeasonFromBranch(seasonID, branchID, sessionToken);		
		JSONObject featureFromBranch = new JSONObject( f.getFeatureFromBranch(featureID1, branchID, sessionToken));
		//F1
		Assert.assertTrue(featureFromBranch.getString("branchStatus").equals("NONE"), "Incorrect feature2 status in get feature from branch");
		
		//CR1
		Assert.assertTrue(featuresInBranch.getJSONObject(0).getJSONArray("configurationRules").getJSONObject(0).getString("branchStatus").equals("NONE"), "Configuration rule1 status is not NONE" );
		Assert.assertTrue(new JSONObject(f.getFeatureFromBranch(configID1, branchID, sessionToken)).getString("branchStatus").equals("NONE"), "Configuration rule1 status status is not NONE in get feature");	//get configuration rule from branch

		//CR2
		Assert.assertTrue(featuresInBranch.getJSONObject(0).getJSONArray("configurationRules").getJSONObject(0)
				.getJSONArray("configurationRules").getJSONObject(0).getJSONArray("configurationRules").getJSONObject(0)
				.getString("branchStatus").equals("NONE"), "Configuration rule2 status is not NONE" );
		Assert.assertTrue(new JSONObject(f.getFeatureFromBranch(configID2, branchID, sessionToken)).getString("branchStatus").equals("NONE"), "Configuration rule2 status status is not NONE in get feature");	//get configuration rule from branch

		//CR3
		Assert.assertTrue(featuresInBranch.getJSONObject(0).getJSONArray("configurationRules").getJSONObject(0)
				.getJSONArray("configurationRules").getJSONObject(0).getJSONArray("configurationRules").getJSONObject(1)
				.getString("branchStatus").equals("NONE"), "Configuration rule2 status is not NONE" );
		Assert.assertTrue(new JSONObject(f.getFeatureFromBranch(configID3, branchID, sessionToken)).getString("branchStatus").equals("NONE"), "Configuration rule3 status status is not NONE in get feature");	//get configuration rule from branch

		//MIXCR
		Assert.assertTrue(featuresInBranch.getJSONObject(0).getJSONArray("configurationRules").getJSONObject(0)
				.getJSONArray("configurationRules").getJSONObject(0)
				.getString("branchStatus").equals("NONE"), "MIX configuration status is not NONE" );
		Assert.assertTrue(new JSONObject(f.getFeatureFromBranch(mixConfigID, branchID, sessionToken)).getString("branchStatus").equals("NONE"), "Configuration rule3 status status is not NONE in get feature");	//get configuration rule from branch

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
