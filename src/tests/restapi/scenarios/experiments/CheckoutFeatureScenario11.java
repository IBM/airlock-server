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

public class CheckoutFeatureScenario11 {
	protected String productID;
	protected String seasonID;
	private String branchID;
	private String featureID1;
	private String featureID2;
	private String featureID3;
	private String featureID4;
	private String featureID5;
	private String featureID6;
	private String featureID7;
	private String featureID8;
	private String featureID9;
	private String featureID10;
	private String mixID1;
	private String mixID2;
	private String mixID3;
	private String mixID4;
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
	

	
	@Test (description ="F1 -> F10 + MIX -> F2 + F3 -> MIX -> F4 + F5 -> MTX -> F6, F7, MTX -> F8+F9 ") 
	public void scenario11 () throws Exception {
		branchID = addBranch("branch1");
		Assert.assertFalse(branchID.contains("error"), "Branch1 was not created: " + branchID);


		fJson.put("name", "F1");
		featureID1 = f.addFeature(seasonID, fJson.toString(), "ROOT", sessionToken);
		Assert.assertFalse(featureID1.contains("error"), "Feature was not added to the season: " + featureID1);
	
		fJson.put("name", "F10");
		featureID10 = f.addFeature(seasonID, fJson.toString(), featureID1, sessionToken);
		Assert.assertFalse(featureID10.contains("error"), "Feature was not added to the season: " + featureID10);

		
		String featureMix = FileUtils.fileToString(filePath + "feature-mutual.txt", "UTF-8", false);
		mixID1 = f.addFeature(seasonID, featureMix, featureID1, sessionToken);
		Assert.assertFalse(mixID1.contains("error"), "Feature was not added to the season: " + mixID1);
				
		fJson.put("name", "F2");
		featureID2 = f.addFeature(seasonID, fJson.toString(), mixID1, sessionToken);
		Assert.assertFalse(featureID2.contains("error"), "Feature was not added to the season: " + featureID2);
		fJson.put("name", "F3");
		featureID3 = f.addFeature(seasonID, fJson.toString(), mixID1, sessionToken);
		Assert.assertFalse(featureID3.contains("error"), "Feature was not added to the season: " + featureID3);
	
		mixID2 = f.addFeature(seasonID, featureMix, featureID3, sessionToken);
		Assert.assertFalse(mixID1.contains("error"), "Feature was not added to the season: " + mixID1);
				
		fJson.put("name", "F4");
		featureID4 = f.addFeature(seasonID, fJson.toString(), mixID2, sessionToken);
		Assert.assertFalse(featureID4.contains("error"), "Feature was not added to the season: " + featureID4);
		fJson.put("name", "F5");
		featureID5 = f.addFeature(seasonID, fJson.toString(), mixID2, sessionToken);
		Assert.assertFalse(featureID5.contains("error"), "Feature was not added to the season: " + featureID5);

		mixID3 = f.addFeature(seasonID, featureMix, featureID5, sessionToken);
		Assert.assertFalse(mixID3.contains("error"), "Feature was not added to the season: " + mixID3);
		
		mixID4 = f.addFeature(seasonID, featureMix, mixID3, sessionToken);
		Assert.assertFalse(mixID4.contains("error"), "Feature was not added to the season: " + mixID4);
		
		fJson.put("name", "F6");
		featureID6 = f.addFeature(seasonID, fJson.toString(), mixID3, sessionToken);
		Assert.assertFalse(featureID6.contains("error"), "Feature was not added to the season: " + featureID6);

		fJson.put("name", "F7");
		featureID7 = f.addFeature(seasonID, fJson.toString(), mixID3, sessionToken);
		Assert.assertFalse(featureID7.contains("error"), "Feature was not added to the season: " + featureID7);

		
		fJson.put("name", "F8");
		featureID8 = f.addFeature(seasonID, fJson.toString(), mixID4, sessionToken);
		Assert.assertFalse(featureID8.contains("error"), "Feature was not added to the season: " + featureID8);

		fJson.put("name", "F9");
		featureID9 = f.addFeature(seasonID, fJson.toString(), mixID4, sessionToken);
		Assert.assertFalse(featureID9.contains("error"), "Feature was not added to the season: " + featureID9);

	}
	
	@Test (dependsOnMethods="scenario11", description="Checkout MTX4")
	public void checkout() throws JSONException, Exception{				

		String res = br.checkoutFeature(branchID, mixID4, sessionToken); //checks out the whole tree except for F10
		Assert.assertFalse(res.contains("error") || res.contains("Invalid response"), "MTX4 was not checked out: " + res);
		
		
		JSONObject F1 = new JSONObject(f.getFeatureFromBranch(featureID1, branchID, sessionToken));
		Assert.assertTrue(F1.getString("branchStatus").equals("CHECKED_OUT"), "Feature1 incorrect status");		
		JSONObject F2 = new JSONObject(f.getFeatureFromBranch(featureID2, branchID, sessionToken));
		Assert.assertTrue(F2.getString("branchStatus").equals("CHECKED_OUT"), "Feature2 incorrect status");
		JSONObject F3 = new JSONObject(f.getFeatureFromBranch(featureID3, branchID, sessionToken));
		Assert.assertTrue(F3.getString("branchStatus").equals("CHECKED_OUT"), "Feature3 incorrect status");
		JSONObject F4 = new JSONObject(f.getFeatureFromBranch(featureID4, branchID, sessionToken));
		Assert.assertTrue(F4.getString("branchStatus").equals("CHECKED_OUT"), "Feature4 incorrect status");	
		JSONObject F5 = new JSONObject(f.getFeatureFromBranch(featureID5, branchID, sessionToken));
		Assert.assertTrue(F5.getString("branchStatus").equals("CHECKED_OUT"), "Feature5 incorrect status");
		JSONObject F6 = new JSONObject(f.getFeatureFromBranch(featureID6, branchID, sessionToken));
		Assert.assertTrue(F6.getString("branchStatus").equals("CHECKED_OUT"), "Feature6 incorrect status");
		JSONObject F7 = new JSONObject(f.getFeatureFromBranch(featureID7, branchID, sessionToken));
		Assert.assertTrue(F7.getString("branchStatus").equals("CHECKED_OUT"), "Feature7 incorrect status");
		JSONObject F8 = new JSONObject(f.getFeatureFromBranch(featureID8, branchID, sessionToken));
		Assert.assertTrue(F8.getString("branchStatus").equals("CHECKED_OUT"), "Feature8 incorrect status");
		JSONObject F9 = new JSONObject(f.getFeatureFromBranch(featureID9, branchID, sessionToken));
		Assert.assertTrue(F9.getString("branchStatus").equals("CHECKED_OUT"), "Feature9 incorrect status");
		
		JSONObject mix1 = new JSONObject(f.getFeatureFromBranch(mixID1, branchID, sessionToken));
		Assert.assertTrue(mix1.getString("branchStatus").equals("CHECKED_OUT"), "mix1 incorrect status");
		JSONObject mix2 = new JSONObject(f.getFeatureFromBranch(mixID2, branchID, sessionToken));
		Assert.assertTrue(mix2.getString("branchStatus").equals("CHECKED_OUT"), "mix2 incorrect status");
		JSONObject mix3 = new JSONObject(f.getFeatureFromBranch(mixID3, branchID, sessionToken));
		Assert.assertTrue(mix3.getString("branchStatus").equals("CHECKED_OUT"), "mix3 incorrect status");
		JSONObject mix4 = new JSONObject(f.getFeatureFromBranch(mixID4, branchID, sessionToken));
		Assert.assertTrue(mix4.getString("branchStatus").equals("CHECKED_OUT"), "mix4 incorrect status");

		JSONObject F10 = new JSONObject(f.getFeatureFromBranch(featureID10, branchID, sessionToken));
		Assert.assertTrue(F10.getString("branchStatus").equals("NONE"), "Feature10 incorrect status");		

	}	
	
	
	@Test (dependsOnMethods="checkout", description="Cancel checkout")
	public void cancelCheckout() throws JSONException, Exception{				

		String res = br.cancelCheckoutFeatureWithSubFeatures(branchID, featureID1, sessionToken);
		Assert.assertFalse(res.contains("error") || res.contains("Invalid response"), "MTX4 was not unchecked out: " + res);
		
		
		JSONObject F1 = new JSONObject(f.getFeatureFromBranch(featureID1, branchID, sessionToken));
		Assert.assertTrue(F1.getString("branchStatus").equals("NONE"), "Feature1 incorrect status");		
		JSONObject F2 = new JSONObject(f.getFeatureFromBranch(featureID2, branchID, sessionToken));
		Assert.assertTrue(F2.getString("branchStatus").equals("NONE"), "Feature2 incorrect status");
		JSONObject F3 = new JSONObject(f.getFeatureFromBranch(featureID3, branchID, sessionToken));
		Assert.assertTrue(F3.getString("branchStatus").equals("NONE"), "Feature3 incorrect status");
		JSONObject F4 = new JSONObject(f.getFeatureFromBranch(featureID4, branchID, sessionToken));
		Assert.assertTrue(F4.getString("branchStatus").equals("NONE"), "Feature4 incorrect status");	
		JSONObject F5 = new JSONObject(f.getFeatureFromBranch(featureID5, branchID, sessionToken));
		Assert.assertTrue(F5.getString("branchStatus").equals("NONE"), "Feature5 incorrect status");
		JSONObject F6 = new JSONObject(f.getFeatureFromBranch(featureID6, branchID, sessionToken));
		Assert.assertTrue(F6.getString("branchStatus").equals("NONE"), "Feature6 incorrect status");
		JSONObject F7 = new JSONObject(f.getFeatureFromBranch(featureID7, branchID, sessionToken));
		Assert.assertTrue(F7.getString("branchStatus").equals("NONE"), "Feature7 incorrect status");
		JSONObject F8 = new JSONObject(f.getFeatureFromBranch(featureID8, branchID, sessionToken));
		Assert.assertTrue(F8.getString("branchStatus").equals("NONE"), "Feature8 incorrect status");
		JSONObject F9 = new JSONObject(f.getFeatureFromBranch(featureID9, branchID, sessionToken));
		Assert.assertTrue(F9.getString("branchStatus").equals("NONE"), "Feature9 incorrect status");
		
		JSONObject mix1 = new JSONObject(f.getFeatureFromBranch(mixID1, branchID, sessionToken));
		Assert.assertTrue(mix1.getString("branchStatus").equals("NONE"), "mix1 incorrect status");
		JSONObject mix2 = new JSONObject(f.getFeatureFromBranch(mixID2, branchID, sessionToken));
		Assert.assertTrue(mix2.getString("branchStatus").equals("NONE"), "mix2 incorrect status");
		JSONObject mix3 = new JSONObject(f.getFeatureFromBranch(mixID3, branchID, sessionToken));
		Assert.assertTrue(mix3.getString("branchStatus").equals("NONE"), "mix3 incorrect status");
		JSONObject mix4 = new JSONObject(f.getFeatureFromBranch(mixID4, branchID, sessionToken));
		Assert.assertTrue(mix4.getString("branchStatus").equals("NONE"), "mix4 incorrect status");

		JSONObject F10 = new JSONObject(f.getFeatureFromBranch(featureID10, branchID, sessionToken));
		Assert.assertTrue(F10.getString("branchStatus").equals("NONE"), "Feature10 incorrect status");		

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
