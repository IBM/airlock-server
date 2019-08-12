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

public class CheckoutFeatureScenario8 {
	protected String productID;
	protected String seasonID;
	private String branchID;
	private String featureID1;
	private String featureID2;
	private String configID2;
	private String configID1;
	private String mixConfigID;
	private String mixID1;
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
	

	
	@Test (description ="F1 -> MIX->F2+(MIXCR-> CR1+CR2), checkout F2 ") 
	public void scenario8 () throws Exception {

		branchID = addBranch("branch1");
		Assert.assertFalse(branchID.contains("error"), "Branch1 was not created: " + branchID);

		fJson.put("name", RandomStringUtils.randomAlphabetic(5));
		featureID1 = f.addFeature(seasonID, fJson.toString(), "ROOT", sessionToken);
		Assert.assertFalse(featureID1.contains("error"), "Feature was not added to the season: " + featureID1);

		String featureMix = FileUtils.fileToString(filePath + "feature-mutual.txt", "UTF-8", false);
		mixID1 = f.addFeature(seasonID, featureMix, featureID1, sessionToken);
		Assert.assertFalse(mixID1.contains("error"), "Feature was not added to the season: " + mixID1);

		fJson.put("name", RandomStringUtils.randomAlphabetic(5));
		featureID2 = f.addFeature(seasonID, fJson.toString(), mixID1, sessionToken);
		Assert.assertFalse(featureID2.contains("error"), "Feature was not added to the season: " + featureID2);
			
		String configurationMix = FileUtils.fileToString(filePath + "configuration_feature-mutual.txt", "UTF-8", false);
		mixConfigID = f.addFeature(seasonID, configurationMix, featureID2, sessionToken);
		Assert.assertFalse(mixConfigID.contains("error"), "Configuration mix was not added to the season");

		String configuration1 = FileUtils.fileToString(filePath + "configuration_rule1.txt", "UTF-8", false);
		configID1 = f.addFeature(seasonID, configuration1, mixConfigID, sessionToken);
		Assert.assertFalse(configID1.contains("error"), "Configuration rule1 was not added to the season");

		String configuration2 = FileUtils.fileToString(filePath + "configuration_rule2.txt", "UTF-8", false);
		configID2 = f.addFeature(seasonID, configuration2, mixConfigID, sessionToken);
		Assert.assertFalse(configID2.contains("error"), "Configuration rule2 was not added to the season");

		String response = br.checkoutFeature(branchID, featureID2, sessionToken);
		Assert.assertFalse(response.contains("error"), "feature was not checked out to branch");
		
		//check that feature was checked out
		response = br.getBranchWithFeatures(branchID, sessionToken);
		JSONObject brJson = new JSONObject(response);
		JSONArray features = brJson.getJSONArray("features");
		
		JSONArray featuresInBranch = f.getFeaturesBySeasonFromBranch(seasonID, branchID, sessionToken);
		
		//F1		
		Assert.assertTrue(features.getJSONObject(0).getString("branchStatus").equals("CHECKED_OUT"), "Feature1 status is not checked_out in get branch" );	//get branch
		Assert.assertTrue(featuresInBranch.getJSONObject(0).getString("branchStatus").equals("CHECKED_OUT"), "Feature1 status is not checked_out in get features" );	//get branch
		Assert.assertTrue(new JSONObject(f.getFeatureFromBranch(featureID1, branchID, sessionToken)).getString("branchStatus").equals("CHECKED_OUT"), "Feature1 status is not checked_out in get feature");	//get feature from branch
		
		//MIX
		Assert.assertTrue(features.getJSONObject(0).getJSONArray("features").getJSONObject(0)
				.getString("branchStatus").equals("CHECKED_OUT"), "MIX status is not checked_out in get branch" );
		Assert.assertTrue(featuresInBranch.getJSONObject(0).getJSONArray("features").getJSONObject(0)
				.getString("branchStatus").equals("CHECKED_OUT"), "Feature2 status is not checked_out in get features" );
		Assert.assertTrue(new JSONObject(f.getFeatureFromBranch(mixID1, branchID, sessionToken)).getString("branchStatus").equals("CHECKED_OUT"), "Feature3 status is not checked_out in get feature");	//get feature from branch

		//F2
		Assert.assertTrue(features.getJSONObject(0).getJSONArray("features").getJSONObject(0)
				.getJSONArray("features").getJSONObject(0)
				.getString("branchStatus").equals("CHECKED_OUT"), "Feature2 status is not checked_out in get branch" );
		Assert.assertTrue(featuresInBranch.getJSONObject(0).getJSONArray("features").getJSONObject(0)
				.getJSONArray("features").getJSONObject(0)
				.getString("branchStatus").equals("CHECKED_OUT"), "Feature2 status is not checked_out in get features" );
		Assert.assertTrue(new JSONObject(f.getFeatureFromBranch(featureID2, branchID, sessionToken)).getString("branchStatus").equals("CHECKED_OUT"), "Feature3 status is not checked_out in get feature");	//get feature from branch
		
		//MIXCR
		Assert.assertTrue(features.getJSONObject(0).getJSONArray("features").getJSONObject(0)
				.getJSONArray("features").getJSONObject(0)
				.getJSONArray("configurationRules").getJSONObject(0)
				.getString("branchStatus").equals("CHECKED_OUT"), "MIXCR status is not checked_out in get branch" );
		Assert.assertTrue(featuresInBranch.getJSONObject(0).getJSONArray("features").getJSONObject(0)
				.getJSONArray("features").getJSONObject(0)
				.getJSONArray("configurationRules").getJSONObject(0)
				.getString("branchStatus").equals("CHECKED_OUT"), "MIXCR status is not checked_out in get features" );
		Assert.assertTrue(new JSONObject(f.getFeatureFromBranch(mixConfigID, branchID, sessionToken)).getString("branchStatus").equals("CHECKED_OUT"), "MIXCR status is not checked_out in get feature");	//get feature from branch

		//CR1
		Assert.assertTrue(features.getJSONObject(0).getJSONArray("features").getJSONObject(0)
				.getJSONArray("features").getJSONObject(0)
				.getJSONArray("configurationRules").getJSONObject(0)
				.getJSONArray("configurationRules").getJSONObject(0)
				.getString("branchStatus").equals("CHECKED_OUT"), "config1 status is not checked_out in get branch" );
		Assert.assertTrue(featuresInBranch.getJSONObject(0).getJSONArray("features").getJSONObject(0)
				.getJSONArray("features").getJSONObject(0)
				.getJSONArray("configurationRules").getJSONObject(0)
				.getJSONArray("configurationRules").getJSONObject(0)
				.getString("branchStatus").equals("CHECKED_OUT"), "config1 status is not checked_out in get features" );
		Assert.assertTrue(new JSONObject(f.getFeatureFromBranch(configID1, branchID, sessionToken)).getString("branchStatus").equals("CHECKED_OUT"), "config1 status is not checked_out in get feature");	//get feature from branch

		
		//CR2
		Assert.assertTrue(features.getJSONObject(0).getJSONArray("features").getJSONObject(0)
				.getJSONArray("features").getJSONObject(0)
				.getJSONArray("configurationRules").getJSONObject(0)
				.getJSONArray("configurationRules").getJSONObject(1)
				.getString("branchStatus").equals("CHECKED_OUT"), "config2 status is not checked_out in get branch" );
		Assert.assertTrue(featuresInBranch.getJSONObject(0).getJSONArray("features").getJSONObject(0)
				.getJSONArray("features").getJSONObject(0)
				.getJSONArray("configurationRules").getJSONObject(0)
				.getJSONArray("configurationRules").getJSONObject(1)
				.getString("branchStatus").equals("CHECKED_OUT"), "config2 status is not checked_out in get features" );
		Assert.assertTrue(new JSONObject(f.getFeatureFromBranch(configID2, branchID, sessionToken)).getString("branchStatus").equals("CHECKED_OUT"), "config2 status is not checked_out in get feature");	//get feature from branch

	}
	
	@Test (dependsOnMethods="scenario8", description="Uncheck F1")
	public void uncheckF1() throws JSONException, Exception{				
		
		//uncheckout F1
		String res = br.cancelCheckoutFeature(branchID, featureID1, sessionToken);
		Assert.assertFalse(res.contains("error"), "Feature was not unchecked out: " + res);
		JSONObject brJson = new JSONObject(br.getBranchWithFeatures(branchID, sessionToken));
		
		
		JSONArray featuresInBranch = f.getFeaturesBySeasonFromBranch(seasonID, branchID, sessionToken);		
		JSONObject featureFromBranch = new JSONObject( f.getFeatureFromBranch(featureID1, branchID, sessionToken));
		//F1
		Assert.assertTrue(featureFromBranch.getString("branchStatus").equals("NONE"), "Incorrect feature1 status in get feature from branch");
		

		//first mix
		Assert.assertTrue(brJson.getJSONArray("features").getJSONObject(0)
				.getString("branchStatus").equals("CHECKED_OUT"), "MIX1 status is not checked_out" );
		Assert.assertTrue(brJson.getJSONArray("features").getJSONObject(0)
				.getString("branchFeatureParentName").equals(featureFromBranch.getString("namespace")+"."+featureFromBranch.getString("name")), "MIX1 status is not checked_out" );
		//F2
		Assert.assertTrue(brJson.getJSONArray("features").getJSONObject(0)
				.getJSONArray("features").getJSONObject(0)
				.getString("branchStatus").equals("CHECKED_OUT"), "Feature2 status is not checked_out" );
		Assert.assertTrue(featuresInBranch.getJSONObject(0).getJSONArray("features").getJSONObject(0).getJSONArray("features").getJSONObject(0)
				.getString("branchStatus").equals("CHECKED_OUT"), "Feature2 status is not CHECKED_OUT in get features" );

		//MIXCR
		Assert.assertTrue(brJson.getJSONArray("features").getJSONObject(0)
				.getJSONArray("features").getJSONObject(0)
				.getJSONArray("configurationRules").getJSONObject(0)
				.getString("branchStatus").equals("CHECKED_OUT"), "MIXCR status is not checked_out in get branch" );

	}	

	@Test (dependsOnMethods="uncheckF1", description="Uncheck F2")
	public void uncheckF2() throws JSONException, Exception{				
		
		//uncheckout F2
		String res = br.cancelCheckoutFeature(branchID, featureID2, sessionToken);
		Assert.assertFalse(res.contains("error"), "Feature was not unchecked out: " + res);
		JSONObject brJson = new JSONObject(br.getBranchWithFeatures(branchID, sessionToken));
		
		
		JSONArray featuresInBranch = f.getFeaturesBySeasonFromBranch(seasonID, branchID, sessionToken);		
		JSONObject featureFromBranch = new JSONObject( f.getFeatureFromBranch(featureID2, branchID, sessionToken));
		
		//F2
		Assert.assertTrue(featureFromBranch.getString("branchStatus").equals("NONE"), "Incorrect feature2 status in get feature from branch");
		Assert.assertTrue(featuresInBranch.getJSONObject(0).getJSONArray("features").getJSONObject(0).getJSONArray("features").getJSONObject(0)
				.getString("branchStatus").equals("NONE"), "Feature2 status is not NONE in get features" );
		

		//first mix
		Assert.assertTrue(brJson.getJSONArray("features").getJSONObject(0)
				.getString("branchStatus").equals("CHECKED_OUT"), "MIX1 status is not checked_out" );

		//MIXCR

		Assert.assertTrue(featuresInBranch.getJSONObject(0).getJSONArray("features").getJSONObject(0)
				.getJSONArray("features").getJSONObject(0)
				.getJSONArray("configurationRules").getJSONObject(0)
				.getString("branchStatus").equals("NONE"), "MIXCR status is not NONE in get features" );
		Assert.assertTrue(new JSONObject(f.getFeatureFromBranch(mixConfigID, branchID, sessionToken)).getString("branchStatus").equals("NONE"), "MIXCR status is not checked_out in get feature");	//get feature from branch

		//CR1
		Assert.assertTrue(featuresInBranch.getJSONObject(0).getJSONArray("features").getJSONObject(0)
				.getJSONArray("features").getJSONObject(0)
				.getJSONArray("configurationRules").getJSONObject(0)
				.getJSONArray("configurationRules").getJSONObject(0)
				.getString("branchStatus").equals("NONE"), "config1 status is not NONE in get features" );
		Assert.assertTrue(new JSONObject(f.getFeatureFromBranch(configID1, branchID, sessionToken)).getString("branchStatus").equals("NONE"), "CR1 status is not checked_out in get feature");	//get feature from branch

		
		//CR2
		Assert.assertTrue(featuresInBranch.getJSONObject(0).getJSONArray("features").getJSONObject(0)
				.getJSONArray("features").getJSONObject(0)
				.getJSONArray("configurationRules").getJSONObject(0)
				.getJSONArray("configurationRules").getJSONObject(1)
				.getString("branchStatus").equals("NONE"), "config2 status is not NONE in get features" );
		Assert.assertTrue(new JSONObject(f.getFeatureFromBranch(configID2, branchID, sessionToken)).getString("branchStatus").equals("NONE"), "CR2 status is not checked_out in get feature");	//get feature from branch

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
