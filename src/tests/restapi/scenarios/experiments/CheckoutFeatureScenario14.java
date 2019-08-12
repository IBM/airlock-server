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

public class CheckoutFeatureScenario14 {
	protected String productID;
	protected String seasonID;
	private String branchID;
	private String featureID1;
	private String featureID2;
	private String featureID3;
	private String featureID4;
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
	

	
	@Test (description ="MIX -> (F1+ F2 + F3) checkout F1") 
	public void scenario14 () throws Exception {

		branchID = addBranch("branch1");
		Assert.assertFalse(branchID.contains("error"), "Branch1 was not created: " + branchID);

		
		String featureMix = FileUtils.fileToString(filePath + "feature-mutual.txt", "UTF-8", false);
		mixID1 = f.addFeature(seasonID, featureMix, "ROOT", sessionToken);
		Assert.assertFalse(mixID1.contains("error"), "Feature was not added to the season: " + mixID1);
		
		fJson.put("name", "F1");
		featureID1 = f.addFeature(seasonID, fJson.toString(), mixID1, sessionToken);
		Assert.assertFalse(featureID1.contains("error"), "Feature was not added to the season: " + featureID1);
		fJson.put("name", "F2");
		featureID2 = f.addFeature(seasonID, fJson.toString(), mixID1, sessionToken);
		Assert.assertFalse(featureID2.contains("error"), "Feature was not added to the season: " + featureID2);
		fJson.put("name", "F3");
		featureID3 = f.addFeature(seasonID, fJson.toString(), mixID1, sessionToken);
		Assert.assertFalse(featureID3.contains("error"), "Feature was not added to the season: " + featureID3);
	}
	
	@Test (dependsOnMethods="scenario14", description="Checkout everything")
	public void checkout() throws JSONException, Exception{				
		
		String res = br.checkoutFeature(branchID, featureID1, sessionToken); //checks out MTX, F1, F2 and F3
		Assert.assertFalse(res.contains("error"), "Feature1 was not unchecked out: " + res);
		
		
		JSONObject mix = new JSONObject(f.getFeatureFromBranch(mixID1, branchID, sessionToken));
		Assert.assertTrue(mix.getString("branchStatus").equals("CHECKED_OUT"), "Feature1 incorrect status");
		JSONObject F1 = new JSONObject(f.getFeatureFromBranch(featureID1, branchID, sessionToken));
		Assert.assertTrue(F1.getString("branchStatus").equals("CHECKED_OUT"), "Feature1 incorrect status");
		JSONObject F2 = new JSONObject(f.getFeatureFromBranch(featureID2, branchID, sessionToken));
		Assert.assertTrue(F2.getString("branchStatus").equals("CHECKED_OUT"), "Feature2 incorrect status");
		JSONObject F3 = new JSONObject(f.getFeatureFromBranch(featureID3, branchID, sessionToken));
		Assert.assertTrue(F3.getString("branchStatus").equals("CHECKED_OUT"), "Feature3 incorrect status");
		
		//find subtree root in branch
		JSONObject branch = new JSONObject(br.getBranchWithFeatures(branchID, sessionToken));
		JSONArray features = branch.getJSONArray("features");
		Assert.assertTrue(features.size() ==1, "More than one sub tree in branch");
		Assert.assertTrue(features.getJSONObject(0).getString("uniqueId").equals(mixID1), "MTX is not the branch subTree root");
	}	

	

	@Test (dependsOnMethods="checkout", description="Cancel checkout mtx")
	public void cancelCheckoutMTX() throws JSONException, Exception{				
		
		String res = br.cancelCheckoutFeature(branchID, mixID1, sessionToken);
		Assert.assertFalse(res.contains("error"), "mix was not unchecked out: " + res);
		
		
		JSONObject mix = new JSONObject(f.getFeatureFromBranch(mixID1, branchID, sessionToken));
		Assert.assertTrue(mix.getString("branchStatus").equals("NONE"), "Feature1 incorrect status");
		JSONObject F1 = new JSONObject(f.getFeatureFromBranch(featureID1, branchID, sessionToken));
		Assert.assertTrue(F1.getString("branchStatus").equals("CHECKED_OUT"), "Feature1 incorrect status");
		JSONObject F2 = new JSONObject(f.getFeatureFromBranch(featureID2, branchID, sessionToken));
		Assert.assertTrue(F2.getString("branchStatus").equals("CHECKED_OUT"), "Feature2 incorrect status");
		JSONObject F3 = new JSONObject(f.getFeatureFromBranch(featureID3, branchID, sessionToken));
		Assert.assertTrue(F3.getString("branchStatus").equals("CHECKED_OUT"), "Feature3 incorrect status");
		
		//find subtree root in branch
		JSONObject branch = new JSONObject(br.getBranchWithFeatures(branchID, sessionToken));
		JSONArray features = branch.getJSONArray("features");
		Assert.assertTrue(features.size() ==3, "number of subTrees in branch should be 3");
		Assert.assertTrue(features.getJSONObject(0).getString("uniqueId").equals(featureID1), "wrong subTree root 1");
		Assert.assertTrue(features.getJSONObject(1).getString("uniqueId").equals(featureID2), "wrong subTree root 2");
		Assert.assertTrue(features.getJSONObject(2).getString("uniqueId").equals(featureID3), "wrong subTree root 3");
	}	


	@Test (dependsOnMethods="cancelCheckoutMTX", description="Cancel checkout F1 and F3")
	public void cancelCheckoutSiblings() throws JSONException, Exception{				
		
		String res = br.cancelCheckoutFeature(branchID, featureID1, sessionToken);
		Assert.assertFalse(res.contains("error"), "feature1 was not unchecked out: " + res);
		
	    res = br.cancelCheckoutFeature(branchID, featureID3, sessionToken);
		Assert.assertFalse(res.contains("error"), "feature3 was not unchecked out: " + res);
		
		
		JSONObject mix = new JSONObject(f.getFeatureFromBranch(mixID1, branchID, sessionToken));
		Assert.assertTrue(mix.getString("branchStatus").equals("NONE"), "Feature1 incorrect status");
		JSONObject F1 = new JSONObject(f.getFeatureFromBranch(featureID1, branchID, sessionToken));
		Assert.assertTrue(F1.getString("branchStatus").equals("NONE"), "Feature1 incorrect status");
		JSONObject F2 = new JSONObject(f.getFeatureFromBranch(featureID2, branchID, sessionToken));
		Assert.assertTrue(F2.getString("branchStatus").equals("CHECKED_OUT"), "Feature2 incorrect status");
		JSONObject F3 = new JSONObject(f.getFeatureFromBranch(featureID3, branchID, sessionToken));
		Assert.assertTrue(F3.getString("branchStatus").equals("NONE"), "Feature3 incorrect status");
		
		//find subtree root in branch
		JSONObject branch = new JSONObject(br.getBranchWithFeatures(branchID, sessionToken));
		JSONArray features = branch.getJSONArray("features");
		Assert.assertTrue(features.size() ==1, "number of subTrees in branch should be 1");
		Assert.assertTrue(features.getJSONObject(0).getString("uniqueId").equals(featureID2), "wrong subTree root 2");
	}	


	@Test (dependsOnMethods="cancelCheckoutSiblings", description="Checkout everything")
	public void reCheckout() throws JSONException, Exception{				
		
		fJson.put("name", "F4");
		featureID4 = f.addFeatureToBranch(seasonID, branchID, fJson.toString(), featureID2, sessionToken);
		Assert.assertFalse(featureID4.contains("error"), "Feature was not added to the season: " + featureID1);
		
		String res = br.checkoutFeature(branchID, featureID1, sessionToken); //checks out MTX, F1, F2 and F3
		Assert.assertFalse(res.contains("error"), "Feature1 was not unchecked out: " + res);
		
		
		JSONObject mix = new JSONObject(f.getFeatureFromBranch(mixID1, branchID, sessionToken));
		Assert.assertTrue(mix.getString("branchStatus").equals("CHECKED_OUT"), "Feature1 incorrect status");
		JSONObject F1 = new JSONObject(f.getFeatureFromBranch(featureID1, branchID, sessionToken));
		Assert.assertTrue(F1.getString("branchStatus").equals("CHECKED_OUT"), "Feature1 incorrect status");
		JSONObject F2 = new JSONObject(f.getFeatureFromBranch(featureID2, branchID, sessionToken));
		Assert.assertTrue(F2.getString("branchStatus").equals("CHECKED_OUT"), "Feature2 incorrect status");
		JSONObject F3 = new JSONObject(f.getFeatureFromBranch(featureID3, branchID, sessionToken));
		Assert.assertTrue(F3.getString("branchStatus").equals("CHECKED_OUT"), "Feature3 incorrect status");
		JSONObject F4 = new JSONObject(f.getFeatureFromBranch(featureID4, branchID, sessionToken));
		Assert.assertTrue(F4.getString("branchStatus").equals("NEW"), "Feature4 incorrect status");
		
		//find subtree root in branch
		JSONObject branch = new JSONObject(br.getBranchWithFeatures(branchID, sessionToken));
		JSONArray features = branch.getJSONArray("features");
		Assert.assertTrue(features.size() ==1, "More than one sub tree in branch");
		Assert.assertTrue(features.getJSONObject(0).getString("uniqueId").equals(mixID1), "MTX is not the branch subTree root");
		
		Assert.assertTrue(features.getJSONObject(0).getJSONArray("features").size() == 3, "MTX should have 3 sub features");
		Assert.assertTrue(features.getJSONObject(0).getJSONArray("features").getJSONObject(0).getString("uniqueId").equals(featureID1), "feature 1 is not the branch subTree root");
		Assert.assertTrue(features.getJSONObject(0).getJSONArray("features").getJSONObject(1).getString("uniqueId").equals(featureID2), "feature 2 is not the branch subTree root");
		Assert.assertTrue(features.getJSONObject(0).getJSONArray("features").getJSONObject(2).getString("uniqueId").equals(featureID3), "feature 3 is not the branch subTree root");
		
		Assert.assertTrue(features.getJSONObject(0).getJSONArray("features").getJSONObject(1).getJSONArray("features").size() == 1, "feature 2 should have feature 4 under it");
		
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
