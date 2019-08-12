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

public class CheckoutFeatureScenario4 {
	protected String productID;
	protected String seasonID;
	private String branchID;
	private String featureID1;
	private String featureID2;
	private String featureID3;
	private String featureID4;
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
	

	
	@Test (description ="F1 -> F2 -> (F3 + F4), checkout F3 then checkout F4") 
	public void scenario4 () throws Exception {

		branchID = addBranch("branch1");
		Assert.assertFalse(branchID.contains("error"), "Branch1 was not created: " + branchID);

		fJson.put("name", RandomStringUtils.randomAlphabetic(5));
		featureID1 = f.addFeature(seasonID, fJson.toString(), "ROOT", sessionToken);
		Assert.assertFalse(featureID1.contains("error"), "Feature was not added to the season: " + featureID1);
		
		fJson.put("name", RandomStringUtils.randomAlphabetic(5));
		featureID2 = f.addFeature(seasonID, fJson.toString(), featureID1, sessionToken);
		Assert.assertFalse(featureID2.contains("error"), "Feature was not added to the season: " + featureID2);
				
		fJson.put("name", RandomStringUtils.randomAlphabetic(5));
		featureID3 = f.addFeature(seasonID, fJson.toString(), featureID2, sessionToken);
		Assert.assertFalse(featureID2.contains("error"), "Feature was not added to the season: " + featureID2);
		
		fJson.put("name", RandomStringUtils.randomAlphabetic(5));
		featureID4 = f.addFeature(seasonID, fJson.toString(), featureID2, sessionToken);
		Assert.assertFalse(featureID4.contains("error"), "Feature was not added to the season: " + featureID4);
		
		String response = br.checkoutFeature(branchID, featureID3, sessionToken);
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
		
		response = br.checkoutFeature(branchID, featureID1, sessionToken);
		Assert.assertTrue(response.contains("error"), "feature1 was checked out twice");
				
		//F2
		Assert.assertTrue(features.getJSONObject(0).getJSONArray("features").getJSONObject(0)
				.getString("branchStatus").equals("CHECKED_OUT"), "Feature2 status is not checked_out in get branch" );
		Assert.assertTrue(featuresInBranch.getJSONObject(0).getJSONArray("features").getJSONObject(0)
				.getString("branchStatus").equals("CHECKED_OUT"), "Feature2 status is not checked_out in get features" );

		Assert.assertTrue(new JSONObject(f.getFeatureFromBranch(featureID3, branchID, sessionToken)).getString("branchStatus").equals("CHECKED_OUT"), "Feature3 status is not checked_out in get feature");	//get feature from branch
		response = br.checkoutFeature(branchID, featureID2, sessionToken);
		Assert.assertTrue(response.contains("error"), "feature2 was checked out twice");

		//F3
		Assert.assertTrue(features.getJSONObject(0).getJSONArray("features").getJSONObject(0).getJSONArray("features").getJSONObject(0)
				.getString("branchStatus").equals("CHECKED_OUT"), "Feature3 status is not checked_out in get branch" );
		Assert.assertTrue(featuresInBranch.getJSONObject(0).getJSONArray("features").getJSONObject(0).getJSONArray("features").getJSONObject(0)
				.getString("branchStatus").equals("CHECKED_OUT"), "Feature3 status is not checked_out in get features" );
		Assert.assertTrue(new JSONObject(f.getFeatureFromBranch(featureID3, branchID, sessionToken)).getString("branchStatus").equals("CHECKED_OUT"), "Feature3 status was incorrectly changed in get feature");	//get feature from branch
		
		//F4 is not in branch array
		Assert.assertTrue(features.getJSONObject(0).getJSONArray("features").getJSONObject(0).getJSONArray("features").size()==1 );
		Assert.assertTrue(featuresInBranch.getJSONObject(0).getJSONArray("features").getJSONObject(0).getJSONArray("features").getJSONObject(1)
				.getString("branchStatus").equals("NONE"), "Feature4 status is not NONE in get features" );
		
		//checkout F4. It's parent features are already checked out
		response = br.checkoutFeature(branchID, featureID4, sessionToken);
		Assert.assertFalse(response.contains("error"), "feature4 was not checked out to branch");
		response = br.getBranchWithFeatures(branchID, sessionToken);
		brJson = new JSONObject(response);
		features = brJson.getJSONArray("features");

		Assert.assertTrue(features.getJSONObject(0).getJSONArray("features").getJSONObject(0).getJSONArray("features").size()==2 );

	}
	
	@Test (dependsOnMethods="scenario4", description="Uncheck F2")
	public void uncheckF2() throws JSONException, Exception{				
		
		//uncheckout F2
		String res = br.cancelCheckoutFeature(branchID, featureID2, sessionToken);
		Assert.assertFalse(res.contains("error"), "Feature was not unchecked out: " + res);
		JSONObject brJson = new JSONObject(br.getBranchWithFeatures(branchID, sessionToken));
		
		
		JSONArray featuresInBranch = f.getFeaturesBySeasonFromBranch(seasonID, branchID, sessionToken);		
		JSONObject featureFromBranch = new JSONObject( f.getFeatureFromBranch(featureID2, branchID, sessionToken));
		
		Assert.assertTrue(featureFromBranch.getString("branchStatus").equals("NONE"), "Incorrect feature2 status in get feature from branch");		
		Assert.assertTrue(brJson.getJSONArray("features").size()==3, "Incorrect number of checked out feature in branch");
		//F1
		Assert.assertTrue(brJson.getJSONArray("features").getJSONObject(0).getString("branchFeatureParentName").equals("ROOT"), "Incorrect branchFeatureParentName for feature1");
		//F3
		Assert.assertTrue(brJson.getJSONArray("features").getJSONObject(1).getString("branchFeatureParentName").equals(featureFromBranch.getString("namespace")+"."+featureFromBranch.getString("name")), "Incorrect parent name for feature 3");
		//F4
		Assert.assertTrue(brJson.getJSONArray("features").getJSONObject(2).getString("branchFeatureParentName").equals(featureFromBranch.getString("namespace")+"."+featureFromBranch.getString("name")), "Incorrect parent name for feature 3");

		//F2
		Assert.assertTrue(featuresInBranch.getJSONObject(0).getJSONArray("features").getJSONObject(0)
				.getString("branchStatus").equals("NONE"), "Feature2 status is not checked_out in get features" );
		//F3
		Assert.assertTrue(featuresInBranch.getJSONObject(0).getJSONArray("features").getJSONObject(0).getJSONArray("features").getJSONObject(0)
				.getString("branchStatus").equals("CHECKED_OUT"), "Feature3 status is not checked_out in get features" );
		//F4
		Assert.assertTrue(featuresInBranch.getJSONObject(0).getJSONArray("features").getJSONObject(0).getJSONArray("features").getJSONObject(1)
				.getString("branchStatus").equals("CHECKED_OUT"), "Feature4 status is not checked_out in get features" );


	}
	
	@Test (dependsOnMethods="uncheckF2", description="Uncheck F3")
	public void uncheckF3() throws JSONException, Exception{				
		
		//uncheckout F2
		String res = br.cancelCheckoutFeature(branchID, featureID3, sessionToken);
		Assert.assertFalse(res.contains("error"), "Feature was not unchecked out: " + res);
		JSONObject brJson = new JSONObject(br.getBranchWithFeatures(branchID, sessionToken));
		
		
		JSONArray featuresInBranch = f.getFeaturesBySeasonFromBranch(seasonID, branchID, sessionToken);		
		JSONObject featureFromBranch = new JSONObject( f.getFeatureFromBranch(featureID3, branchID, sessionToken));
		JSONObject parentF2feature = new JSONObject( f.getFeatureFromBranch(featureID2, branchID, sessionToken));
		
		Assert.assertTrue(featureFromBranch.getString("branchStatus").equals("NONE"), "Incorrect feature3 status in get feature from branch");		
		Assert.assertTrue(brJson.getJSONArray("features").size()==2, "Incorrect number of checked out feature in branch");
		//F1
		Assert.assertTrue(brJson.getJSONArray("features").getJSONObject(0).getString("branchFeatureParentName").equals("ROOT"), "Incorrect branchFeatureParentName for feature1");
		//F4
		Assert.assertTrue(brJson.getJSONArray("features").getJSONObject(1).getString("branchFeatureParentName").equals(parentF2feature.getString("namespace")+"."+parentF2feature.getString("name")), "Incorrect parent name for feature4");

		//F2
		Assert.assertTrue(featuresInBranch.getJSONObject(0).getJSONArray("features").getJSONObject(0)
				.getString("branchStatus").equals("NONE"), "Feature2 status is not checked_out in get features" );
		//F3
		Assert.assertTrue(featuresInBranch.getJSONObject(0).getJSONArray("features").getJSONObject(0).getJSONArray("features").getJSONObject(0)
				.getString("branchStatus").equals("NONE"), "Feature3 status is not NONE in get features" );
		//F4
		Assert.assertTrue(featuresInBranch.getJSONObject(0).getJSONArray("features").getJSONObject(0).getJSONArray("features").getJSONObject(1)
				.getString("branchStatus").equals("CHECKED_OUT"), "Feature4 status is not checked_out in get features" );

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
