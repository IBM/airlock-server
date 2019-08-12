package tests.restapi.scenarios.experiments;

import java.io.IOException;



import org.apache.commons.lang3.RandomStringUtils;
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


public class CheckoutFeatureScenario13 {
	protected String productID;
	protected String seasonID;
	private String branchID;
	private String featureID1;
	private String featureID2;
	private String featureID3;
	private String featureID4;
	private String configID1;
	private String configID2;
	private String configID3;
	private String configID4;
	private String mixConfigID1;
	private String mixConfigID2;
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
	

	
	@Test (description ="F1 + (C4 + MTXCR1->C1+MTXCR2 -> C2+C3) -> MTX1 -> (F2 + MTX2 -> (F3 + F4) ),  checkout MTX2 ") 
	public void scenario13 () throws Exception {

		branchID = addBranch("branch1");
		Assert.assertFalse(branchID.contains("error"), "Branch1 was not created: " + branchID);

		fJson.put("name", RandomStringUtils.randomAlphabetic(5));
		featureID1 = f.addFeature(seasonID, fJson.toString(), "ROOT", sessionToken);
		Assert.assertFalse(featureID1.contains("error"), "Feature was not added to the season: " + featureID1);
		

		
		String configurationMix = FileUtils.fileToString(filePath + "configuration_feature-mutual.txt", "UTF-8", false);
		mixConfigID1 = f.addFeature(seasonID, configurationMix, featureID1, sessionToken);
		Assert.assertFalse(mixConfigID1.contains("error"), "Configuration mix1 was not added to the season");

		String configuration1 = FileUtils.fileToString(filePath + "configuration_rule1.txt", "UTF-8", false);
		JSONObject jsonCR = new JSONObject(configuration1);
		
		
		
		jsonCR.put("name", "CR1");
		configID1 = f.addFeature(seasonID, jsonCR.toString(), mixConfigID1, sessionToken);
		Assert.assertFalse(configID1.contains("error"), "Configuration rule1 was not added to the season");
		
		mixConfigID2 = f.addFeature(seasonID, configurationMix, mixConfigID1, sessionToken);
		Assert.assertFalse(mixConfigID2.contains("error"), "Configuration mix2 was not added to the season");
		
		jsonCR.put("name", "CR2");
		configID2 = f.addFeature(seasonID, jsonCR.toString(), mixConfigID2, sessionToken);
		Assert.assertFalse(configID2.contains("error"), "Configuration rule2 was not added to the season");
				
		jsonCR.put("name", "CR3");
		configID3 = f.addFeature(seasonID, jsonCR.toString(), mixConfigID2, sessionToken);
		Assert.assertFalse(configID3.contains("error"), "Configuration rule3 was not added to the season");

		jsonCR.put("name", "CR4");
		configID4 = f.addFeature(seasonID, jsonCR.toString(), featureID1, sessionToken);
		Assert.assertFalse(configID4.contains("error"), "Configuration rule4 was not added to the season");

		
		String featureMix = FileUtils.fileToString(filePath + "feature-mutual.txt", "UTF-8", false);
		mixID1 = f.addFeature(seasonID, featureMix, featureID1, sessionToken);
		Assert.assertFalse(mixID1.contains("error"), "Feature was not added to the season: " + mixID1);
				
		fJson.put("name", RandomStringUtils.randomAlphabetic(5));
		featureID2 = f.addFeature(seasonID, fJson.toString(), mixID1, sessionToken);
		Assert.assertFalse(featureID2.contains("error"), "Feature was not added to the season: " + featureID2);
		
		featureMix = FileUtils.fileToString(filePath + "feature-mutual.txt", "UTF-8", false);
		mixID2 = f.addFeature(seasonID, featureMix, mixID1, sessionToken);
		Assert.assertFalse(mixID2.contains("error"), "Feature was not added to the season: " + mixID2);

		fJson.put("name", RandomStringUtils.randomAlphabetic(5));
		featureID3 = f.addFeature(seasonID, fJson.toString(), mixID2, sessionToken);
		Assert.assertFalse(featureID3.contains("error"), "Feature was not added to the season: " + featureID3);
		
		fJson.put("name", RandomStringUtils.randomAlphabetic(5));
		featureID4 = f.addFeature(seasonID, fJson.toString(), mixID2, sessionToken);
		Assert.assertFalse(featureID4.contains("error"), "Feature was not added to the season: " + featureID4);
		
	}
	
	@Test (dependsOnMethods="scenario13", description="Checkout mixID2")
	public void checkout() throws JSONException, Exception{				

		String res = br.checkoutFeature(branchID, mixID2, sessionToken); //checks out the whole tree 
		Assert.assertFalse(res.contains("error") || res.contains("Invalid response"), "mixID2 was not checked out: " + res);
		
		
		JSONObject F1 = new JSONObject(f.getFeatureFromBranch(featureID1, branchID, sessionToken));
		Assert.assertTrue(F1.getString("branchStatus").equals("CHECKED_OUT"), "Feature1 incorrect status");		
		JSONObject F2 = new JSONObject(f.getFeatureFromBranch(featureID2, branchID, sessionToken));
		Assert.assertTrue(F2.getString("branchStatus").equals("CHECKED_OUT"), "Feature2 incorrect status");
		JSONObject F3 = new JSONObject(f.getFeatureFromBranch(featureID3, branchID, sessionToken));
		Assert.assertTrue(F3.getString("branchStatus").equals("CHECKED_OUT"), "Feature3 incorrect status");
		JSONObject F4 = new JSONObject(f.getFeatureFromBranch(featureID4, branchID, sessionToken));
		Assert.assertTrue(F4.getString("branchStatus").equals("CHECKED_OUT"), "Feature4 incorrect status");	
		
		JSONObject mix1 = new JSONObject(f.getFeatureFromBranch(mixID1, branchID, sessionToken));
		Assert.assertTrue(mix1.getString("branchStatus").equals("CHECKED_OUT"), "mix1 incorrect status");
		JSONObject mix2 = new JSONObject(f.getFeatureFromBranch(mixID2, branchID, sessionToken));
		Assert.assertTrue(mix2.getString("branchStatus").equals("CHECKED_OUT"), "mix2 incorrect status");
		
		JSONObject crmtx1 = new JSONObject(f.getFeatureFromBranch(mixConfigID1, branchID, sessionToken));
		Assert.assertTrue(crmtx1.getString("branchStatus").equals("CHECKED_OUT"), "crmtx1 incorrect status");		
		JSONObject crmtx2 = new JSONObject(f.getFeatureFromBranch(mixConfigID2, branchID, sessionToken));
		Assert.assertTrue(crmtx2.getString("branchStatus").equals("CHECKED_OUT"), "crmtx2 incorrect status");
		
		JSONObject cr1 = new JSONObject(f.getFeatureFromBranch(configID1, branchID, sessionToken));
		Assert.assertTrue(cr1.getString("branchStatus").equals("CHECKED_OUT"), "cr1 incorrect status");
		JSONObject cr2 = new JSONObject(f.getFeatureFromBranch(configID2, branchID, sessionToken));
		Assert.assertTrue(cr2.getString("branchStatus").equals("CHECKED_OUT"), "cr2 incorrect status");
		JSONObject cr3 = new JSONObject(f.getFeatureFromBranch(configID3, branchID, sessionToken));
		Assert.assertTrue(cr3.getString("branchStatus").equals("CHECKED_OUT"), "cr3 incorrect status");
		JSONObject cr4 = new JSONObject(f.getFeatureFromBranch(configID4, branchID, sessionToken));
		Assert.assertTrue(cr4.getString("branchStatus").equals("CHECKED_OUT"), "cr4 incorrect status");

	}	
	
	
	@Test (dependsOnMethods="checkout", description="Cancel checkout")
	public void cancelCheckout() throws JSONException, Exception{				

		String res = br.cancelCheckoutFeatureWithSubFeatures(branchID, featureID1, sessionToken);
		Assert.assertFalse(res.contains("error") || res.contains("Invalid response"), "Feature1 was not unchecked out: " + res);
		
		
		JSONObject F1 = new JSONObject(f.getFeatureFromBranch(featureID1, branchID, sessionToken));
		Assert.assertTrue(F1.getString("branchStatus").equals("NONE"), "Feature1 incorrect status");		
		JSONObject F2 = new JSONObject(f.getFeatureFromBranch(featureID2, branchID, sessionToken));
		Assert.assertTrue(F2.getString("branchStatus").equals("NONE"), "Feature2 incorrect status");
		JSONObject F3 = new JSONObject(f.getFeatureFromBranch(featureID3, branchID, sessionToken));
		Assert.assertTrue(F3.getString("branchStatus").equals("NONE"), "Feature3 incorrect status");
		JSONObject F4 = new JSONObject(f.getFeatureFromBranch(featureID4, branchID, sessionToken));
		Assert.assertTrue(F4.getString("branchStatus").equals("NONE"), "Feature4 incorrect status");	
		
		JSONObject mix1 = new JSONObject(f.getFeatureFromBranch(mixID1, branchID, sessionToken));
		Assert.assertTrue(mix1.getString("branchStatus").equals("NONE"), "mix1 incorrect status");
		JSONObject mix2 = new JSONObject(f.getFeatureFromBranch(mixID2, branchID, sessionToken));
		Assert.assertTrue(mix2.getString("branchStatus").equals("NONE"), "mix2 incorrect status");
		
		JSONObject crmtx1 = new JSONObject(f.getFeatureFromBranch(mixConfigID1, branchID, sessionToken));
		Assert.assertTrue(crmtx1.getString("branchStatus").equals("NONE"), "crmtx1 incorrect status");		
		JSONObject crmtx2 = new JSONObject(f.getFeatureFromBranch(mixConfigID2, branchID, sessionToken));
		Assert.assertTrue(crmtx2.getString("branchStatus").equals("NONE"), "crmtx2 incorrect status");
		
		JSONObject cr1 = new JSONObject(f.getFeatureFromBranch(configID1, branchID, sessionToken));
		Assert.assertTrue(cr1.getString("branchStatus").equals("NONE"), "cr1 incorrect status");
		JSONObject cr2 = new JSONObject(f.getFeatureFromBranch(configID2, branchID, sessionToken));
		Assert.assertTrue(cr2.getString("branchStatus").equals("NONE"), "cr2 incorrect status");
		JSONObject cr3 = new JSONObject(f.getFeatureFromBranch(configID3, branchID, sessionToken));
		Assert.assertTrue(cr3.getString("branchStatus").equals("NONE"), "cr3 incorrect status");
		JSONObject cr4 = new JSONObject(f.getFeatureFromBranch(configID4, branchID, sessionToken));
		Assert.assertTrue(cr4.getString("branchStatus").equals("NONE"), "cr4 incorrect status");


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
