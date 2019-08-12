package tests.restapi.in_app_purchases;

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
import tests.restapi.InAppPurchasesRestApi;

//TODO: extend test to include purchaseOptions

public class CheckoutPurchaseScenario13 {
	protected String productID;
	protected String seasonID;
	private String branchID;
	private String inAppPurchaseID1;
	private String inAppPurchaseID2;
	private String inAppPurchaseID3;
	private String inAppPurchaseID4;
	private String configID1;
	private String configID2;
	private String configID3;
	private String configID4;
	private String mixConfigID1;
	private String mixConfigID2;
	private String mixID1;
	private String mixID2;
	private JSONObject inAppPurJson;
	private JSONObject purOptJson;
	protected String filePath;
	protected String m_url;
	private String sessionToken = "";
	private AirlockUtils baseUtils;
	private BranchesRestApi br ;
	protected InAppPurchasesRestApi purchasesApi;
	
	
	@BeforeClass
	@Parameters({"url", "analyticsUrl", "translationsUrl", "configPath", "sessionToken", "userName", "userPassword", "appName", "productsToDeleteFile"})
	public void init(String url, String analyticsUrl, String translationsUrl, String configPath, String sToken, String userName, String userPassword, String appName, String productsToDeleteFile) throws Exception{
		m_url = url;
		filePath = configPath ;
		purchasesApi = new InAppPurchasesRestApi();
		purchasesApi.setURL(m_url);
		br = new BranchesRestApi();
		br.setURL(m_url);

		baseUtils = new AirlockUtils(m_url, analyticsUrl, translationsUrl, configPath, sToken, userName, userPassword, appName, productsToDeleteFile);
		sessionToken = baseUtils.sessionToken;

		productID = baseUtils.createProduct();
		baseUtils.printProductToFile(productID);
		seasonID = baseUtils.createSeason(productID);
		
		String inAppPur = FileUtils.fileToString(filePath + "purchases/inAppPurchase1.txt", "UTF-8", false);
		inAppPurJson = new JSONObject(inAppPur);
		
		String purOpt = FileUtils.fileToString(filePath + "purchases/purchaseOptions1.txt", "UTF-8", false);
		purOptJson = new JSONObject(purOpt);		
	}
	

	
	@Test (description ="IAP1 + (C4 + MTXCR1->C1+MTXCR2 -> C2+C3) -> MTX1 -> (IAP2 + MTX2 -> (IAP3 + IAP4) ),  checkout MTX2 ") 
	public void scenario13 () throws Exception {

		branchID = addBranch("branch1");
		Assert.assertFalse(branchID.contains("error"), "Branch1 was not created: " + branchID);

		inAppPurJson.put("name", RandomStringUtils.randomAlphabetic(5));
		inAppPurchaseID1 = purchasesApi.addPurchaseItem(seasonID, inAppPurJson.toString(), "ROOT", sessionToken);
		Assert.assertFalse(inAppPurchaseID1.contains("error"), "inAppPurchase was not added to the season: " + inAppPurchaseID1);
		

		
		String configurationMix = FileUtils.fileToString(filePath + "configuration_feature-mutual.txt", "UTF-8", false);
		mixConfigID1 = purchasesApi.addPurchaseItem(seasonID, configurationMix, inAppPurchaseID1, sessionToken);
		Assert.assertFalse(mixConfigID1.contains("error"), "Configuration mix1 was not added to the season");

		String configuration1 = FileUtils.fileToString(filePath + "configuration_rule1.txt", "UTF-8", false);
		JSONObject jsonCR = new JSONObject(configuration1);
		
		
		
		jsonCR.put("name", "CR1");
		configID1 = purchasesApi.addPurchaseItem(seasonID, jsonCR.toString(), mixConfigID1, sessionToken);
		Assert.assertFalse(configID1.contains("error"), "Configuration rule1 was not added to the season");
		
		mixConfigID2 = purchasesApi.addPurchaseItem(seasonID, configurationMix, mixConfigID1, sessionToken);
		Assert.assertFalse(mixConfigID2.contains("error"), "Configuration mix2 was not added to the season");
		
		jsonCR.put("name", "CR2");
		configID2 = purchasesApi.addPurchaseItem(seasonID, jsonCR.toString(), mixConfigID2, sessionToken);
		Assert.assertFalse(configID2.contains("error"), "Configuration rule2 was not added to the season");
				
		jsonCR.put("name", "CR3");
		configID3 = purchasesApi.addPurchaseItem(seasonID, jsonCR.toString(), mixConfigID2, sessionToken);
		Assert.assertFalse(configID3.contains("error"), "Configuration rule3 was not added to the season");

		jsonCR.put("name", "CR4");
		configID4 = purchasesApi.addPurchaseItem(seasonID, jsonCR.toString(), inAppPurchaseID1, sessionToken);
		Assert.assertFalse(configID4.contains("error"), "Configuration rule4 was not added to the season");

		
		String iapeMix = FileUtils.fileToString(filePath + "purchases/inAppPurchaseMutual.txt", "UTF-8", false);
		mixID1 = purchasesApi.addPurchaseItem(seasonID, iapeMix, inAppPurchaseID1, sessionToken);
		Assert.assertFalse(mixID1.contains("error"), "inAppPurchase mtx was not added to the season: " + mixID1);
				
		inAppPurJson.put("name", RandomStringUtils.randomAlphabetic(5));
		inAppPurchaseID2 = purchasesApi.addPurchaseItem(seasonID, inAppPurJson.toString(), mixID1, sessionToken);
		Assert.assertFalse(inAppPurchaseID2.contains("error"), "inAppPurchase was not added to the season: " + inAppPurchaseID2);
		
		iapeMix = FileUtils.fileToString(filePath + "purchases/inAppPurchaseMutual.txt", "UTF-8", false);
		mixID2 = purchasesApi.addPurchaseItem(seasonID, iapeMix, mixID1, sessionToken);
		Assert.assertFalse(mixID2.contains("error"), "inAppPurchase mtx was not added to the season: " + mixID2);

		inAppPurJson.put("name", RandomStringUtils.randomAlphabetic(5));
		inAppPurchaseID3 = purchasesApi.addPurchaseItem(seasonID, inAppPurJson.toString(), mixID2, sessionToken);
		Assert.assertFalse(inAppPurchaseID3.contains("error"), "inAppPurchase was not added to the season: " + inAppPurchaseID3);
		
		inAppPurJson.put("name", RandomStringUtils.randomAlphabetic(5));
		inAppPurchaseID4 = purchasesApi.addPurchaseItem(seasonID, inAppPurJson.toString(), mixID2, sessionToken);
		Assert.assertFalse(inAppPurchaseID4.contains("error"), "inAppPurchase was not added to the season: " + inAppPurchaseID4);
		
	}
	
	@Test (dependsOnMethods="scenario13", description="Checkout mixID2")
	public void checkout() throws JSONException, Exception{				

		String res = br.checkoutFeature(branchID, mixID2, sessionToken); //checks out the whole tree 
		Assert.assertFalse(res.contains("error") || res.contains("Invalid response"), "mixID2 was not checked out: " + res);
		
		
		JSONObject iap1 = new JSONObject(purchasesApi.getPurchaseItemFromBranch(inAppPurchaseID1, branchID, sessionToken));
		Assert.assertTrue(iap1.getString("branchStatus").equals("CHECKED_OUT"), "inAppPurchase1 incorrect status");		
		JSONObject iap2 = new JSONObject(purchasesApi.getPurchaseItemFromBranch(inAppPurchaseID2, branchID, sessionToken));
		Assert.assertTrue(iap2.getString("branchStatus").equals("CHECKED_OUT"), "inAppPurchase2 incorrect status");
		JSONObject iap3 = new JSONObject(purchasesApi.getPurchaseItemFromBranch(inAppPurchaseID3, branchID, sessionToken));
		Assert.assertTrue(iap3.getString("branchStatus").equals("CHECKED_OUT"), "inAppPurchase3 incorrect status");
		JSONObject iap4 = new JSONObject(purchasesApi.getPurchaseItemFromBranch(inAppPurchaseID4, branchID, sessionToken));
		Assert.assertTrue(iap4.getString("branchStatus").equals("CHECKED_OUT"), "inAppPurchase4 incorrect status");	
		
		JSONObject mix1 = new JSONObject(purchasesApi.getPurchaseItemFromBranch(mixID1, branchID, sessionToken));
		Assert.assertTrue(mix1.getString("branchStatus").equals("CHECKED_OUT"), "mix1 incorrect status");
		JSONObject mix2 = new JSONObject(purchasesApi.getPurchaseItemFromBranch(mixID2, branchID, sessionToken));
		Assert.assertTrue(mix2.getString("branchStatus").equals("CHECKED_OUT"), "mix2 incorrect status");
		
		JSONObject crmtx1 = new JSONObject(purchasesApi.getPurchaseItemFromBranch(mixConfigID1, branchID, sessionToken));
		Assert.assertTrue(crmtx1.getString("branchStatus").equals("CHECKED_OUT"), "crmtx1 incorrect status");		
		JSONObject crmtx2 = new JSONObject(purchasesApi.getPurchaseItemFromBranch(mixConfigID2, branchID, sessionToken));
		Assert.assertTrue(crmtx2.getString("branchStatus").equals("CHECKED_OUT"), "crmtx2 incorrect status");
		
		JSONObject cr1 = new JSONObject(purchasesApi.getPurchaseItemFromBranch(configID1, branchID, sessionToken));
		Assert.assertTrue(cr1.getString("branchStatus").equals("CHECKED_OUT"), "cr1 incorrect status");
		JSONObject cr2 = new JSONObject(purchasesApi.getPurchaseItemFromBranch(configID2, branchID, sessionToken));
		Assert.assertTrue(cr2.getString("branchStatus").equals("CHECKED_OUT"), "cr2 incorrect status");
		JSONObject cr3 = new JSONObject(purchasesApi.getPurchaseItemFromBranch(configID3, branchID, sessionToken));
		Assert.assertTrue(cr3.getString("branchStatus").equals("CHECKED_OUT"), "cr3 incorrect status");
		JSONObject cr4 = new JSONObject(purchasesApi.getPurchaseItemFromBranch(configID4, branchID, sessionToken));
		Assert.assertTrue(cr4.getString("branchStatus").equals("CHECKED_OUT"), "cr4 incorrect status");

	}	
	
	
	@Test (dependsOnMethods="checkout", description="Cancel checkout")
	public void cancelCheckout() throws JSONException, Exception{				

		String res = br.cancelCheckoutFeatureWithSubFeatures(branchID, inAppPurchaseID1, sessionToken);
		Assert.assertFalse(res.contains("error") || res.contains("Invalid response"), "Feature1 was not unchecked out: " + res);
		
		
		JSONObject iap1 = new JSONObject(purchasesApi.getPurchaseItemFromBranch(inAppPurchaseID1, branchID, sessionToken));
		Assert.assertTrue(iap1.getString("branchStatus").equals("NONE"), "inAppPurchase1 incorrect status");		
		JSONObject iap2 = new JSONObject(purchasesApi.getPurchaseItemFromBranch(inAppPurchaseID2, branchID, sessionToken));
		Assert.assertTrue(iap2.getString("branchStatus").equals("NONE"), "inAppPurchase2 incorrect status");
		JSONObject iap3 = new JSONObject(purchasesApi.getPurchaseItemFromBranch(inAppPurchaseID3, branchID, sessionToken));
		Assert.assertTrue(iap3.getString("branchStatus").equals("NONE"), "inAppPurchase3 incorrect status");
		JSONObject iap4 = new JSONObject(purchasesApi.getPurchaseItemFromBranch(inAppPurchaseID4, branchID, sessionToken));
		Assert.assertTrue(iap4.getString("branchStatus").equals("NONE"), "inAppPurchase4 incorrect status");	
		
		JSONObject mix1 = new JSONObject(purchasesApi.getPurchaseItemFromBranch(mixID1, branchID, sessionToken));
		Assert.assertTrue(mix1.getString("branchStatus").equals("NONE"), "mix1 incorrect status");
		JSONObject mix2 = new JSONObject(purchasesApi.getPurchaseItemFromBranch(mixID2, branchID, sessionToken));
		Assert.assertTrue(mix2.getString("branchStatus").equals("NONE"), "mix2 incorrect status");
		
		JSONObject crmtx1 = new JSONObject(purchasesApi.getPurchaseItemFromBranch(mixConfigID1, branchID, sessionToken));
		Assert.assertTrue(crmtx1.getString("branchStatus").equals("NONE"), "crmtx1 incorrect status");		
		JSONObject crmtx2 = new JSONObject(purchasesApi.getPurchaseItemFromBranch(mixConfigID2, branchID, sessionToken));
		Assert.assertTrue(crmtx2.getString("branchStatus").equals("NONE"), "crmtx2 incorrect status");
		
		JSONObject cr1 = new JSONObject(purchasesApi.getPurchaseItemFromBranch(configID1, branchID, sessionToken));
		Assert.assertTrue(cr1.getString("branchStatus").equals("NONE"), "cr1 incorrect status");
		JSONObject cr2 = new JSONObject(purchasesApi.getPurchaseItemFromBranch(configID2, branchID, sessionToken));
		Assert.assertTrue(cr2.getString("branchStatus").equals("NONE"), "cr2 incorrect status");
		JSONObject cr3 = new JSONObject(purchasesApi.getPurchaseItemFromBranch(configID3, branchID, sessionToken));
		Assert.assertTrue(cr3.getString("branchStatus").equals("NONE"), "cr3 incorrect status");
		JSONObject cr4 = new JSONObject(purchasesApi.getPurchaseItemFromBranch(configID4, branchID, sessionToken));
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
