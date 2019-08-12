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

public class CheckoutFeatureScenario9 {
	protected String productID;
	protected String seasonID;
	private String branchID;
	private String featureID1;
	private String featureID2;
	private String featureID3;
	private String featureID4;
	private String featureID5;
	private String featureID6;
	private String mixID1;
	private String mixID2;
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
	

	
	@Test (description ="F1 -> MIX -> (F2 + F3), F4 -> MIX -> (F5 + F6); checkout everything and create MTX from F1 & F4 ") 
	public void scenario9 () throws Exception {

		branchID = addBranch("branch1");
		Assert.assertFalse(branchID.contains("error"), "Branch1 was not created: " + branchID);

		fJson.put("name", "F1");
		featureID1 = f.addFeature(seasonID, fJson.toString(), "ROOT", sessionToken);
		Assert.assertFalse(featureID1.contains("error"), "Feature was not added to the season: " + featureID1);
	
		String featureMix = FileUtils.fileToString(filePath + "feature-mutual.txt", "UTF-8", false);
		mixID1 = f.addFeature(seasonID, featureMix, featureID1, sessionToken);
		Assert.assertFalse(mixID1.contains("error"), "Feature was not added to the season: " + mixID1);
				
		fJson.put("name", "F2");
		featureID2 = f.addFeature(seasonID, fJson.toString(), mixID1, sessionToken);
		Assert.assertFalse(featureID2.contains("error"), "Feature was not added to the season: " + featureID2);
		fJson.put("name", "F3");
		featureID3 = f.addFeature(seasonID, fJson.toString(), mixID1, sessionToken);
		Assert.assertFalse(featureID3.contains("error"), "Feature was not added to the season: " + featureID3);


		
		fJson.put("name", "F4");
		featureID4 = f.addFeature(seasonID, fJson.toString(), "ROOT", sessionToken);
		Assert.assertFalse(featureID4.contains("error"), "Feature4 was not added to the season: " + featureID4);
	
		mixID2 = f.addFeature(seasonID, featureMix, featureID4, sessionToken);
		Assert.assertFalse(mixID1.contains("error"), "Feature was not added to the season: " + mixID1);
				
		fJson.put("name", "F5");
		featureID5 = f.addFeature(seasonID, fJson.toString(), mixID2, sessionToken);
		Assert.assertFalse(featureID5.contains("error"), "Feature was not added to the season: " + featureID5);
		fJson.put("name", "F6");
		featureID6 = f.addFeature(seasonID, fJson.toString(), mixID2, sessionToken);
		Assert.assertFalse(featureID6.contains("error"), "Feature was not added to the season: " + featureID6);


	}
	
	@Test (dependsOnMethods="scenario9", description="Checkout everything")
	public void checkout() throws JSONException, Exception{				
		
		String res = br.checkoutFeature(branchID, featureID2, sessionToken); //checks out MTX, F3 and F1
		Assert.assertFalse(res.contains("error"), "Feature2 was not unchecked out: " + res);
		res = br.checkoutFeature(branchID, featureID5, sessionToken); //checks out MTX, F6 and F4
		Assert.assertFalse(res.contains("error"), "Feature5 was not unchecked out: " + res);
		
		
		
		String featureMix = FileUtils.fileToString(filePath + "feature-mutual.txt", "UTF-8", false);
		String mixID3 = f.addFeatureToBranch(seasonID, branchID, featureMix, "ROOT", sessionToken);
		
		JSONObject F1 = new JSONObject(f.getFeatureFromBranch(featureID1, branchID, sessionToken));
		JSONObject F4 = new JSONObject(f.getFeatureFromBranch(featureID4, branchID, sessionToken));
		JSONObject mix = new JSONObject(f.getFeatureFromBranch(mixID3, branchID, sessionToken));
		JSONArray children = mix.getJSONArray("features");
		children.put(F1);
		children.put(F4);
		mix.put("features", children);
		
		String response = f.updateFeatureInBranch(seasonID, branchID, mixID3, mix.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Can't create MTX group");
		JSONObject brJson = new JSONObject(br.getBranchWithFeatures(branchID, sessionToken));
		
		F1 = new JSONObject(f.getFeatureFromBranch(featureID1, branchID, sessionToken));
		Assert.assertTrue(F1.getString("branchStatus").equals("CHECKED_OUT"), "Feature1 incorrect status");
		JSONObject F2 = new JSONObject(f.getFeatureFromBranch(featureID2, branchID, sessionToken));
		Assert.assertTrue(F2.getString("branchStatus").equals("CHECKED_OUT"), "Feature2 incorrect status");
		JSONObject F3 = new JSONObject(f.getFeatureFromBranch(featureID3, branchID, sessionToken));
		Assert.assertTrue(F3.getString("branchStatus").equals("CHECKED_OUT"), "Feature3 incorrect status");
		F4 = new JSONObject(f.getFeatureFromBranch(featureID4, branchID, sessionToken));
		Assert.assertTrue(F4.getString("branchStatus").equals("CHECKED_OUT"), "Feature4 incorrect status");	
		JSONObject F5 = new JSONObject(f.getFeatureFromBranch(featureID5, branchID, sessionToken));
		Assert.assertTrue(F5.getString("branchStatus").equals("CHECKED_OUT"), "Feature5 incorrect status");
		JSONObject F6 = new JSONObject(f.getFeatureFromBranch(featureID6, branchID, sessionToken));
		Assert.assertTrue(F6.getString("branchStatus").equals("CHECKED_OUT"), "Feature6 incorrect status");
		JSONObject mix1 = new JSONObject(f.getFeatureFromBranch(mixID1, branchID, sessionToken));
		Assert.assertTrue(mix1.getString("branchStatus").equals("CHECKED_OUT"), "mix1 incorrect status");
		JSONObject mix2 = new JSONObject(f.getFeatureFromBranch(mixID2, branchID, sessionToken));
		Assert.assertTrue(mix2.getString("branchStatus").equals("CHECKED_OUT"), "mix2 incorrect status");
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
