package tests.restapi.in_app_purchases;

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
import tests.restapi.InAppPurchasesRestApi;

//TODO: extend test to include purchaseOptions

public class CheckoutPurchaseScenario9 {
	protected String productID;
	protected String seasonID;
	private String branchID;
	private String inAppPurchaseID1;
	private String inAppPurchaseID2;
	private String inAppPurchaseID3;
	private String inAppPurchaseID4;
	private String inAppPurchaseID5;
	private String inAppPurchaseID6;
	private String mixID1;
	private String mixID2;
	private JSONObject purOptJson;
	private JSONObject inAppPurJson;
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
	

	
	@Test (description ="IAP1 -> MIX -> (IAP2 + IAP3), IAP4 -> MIX -> (IAP5 + IAP6); checkout everything and create MTX from IAP1 & IAP4 ") 
	public void scenario9 () throws Exception {

		branchID = addBranch("branch1");
		Assert.assertFalse(branchID.contains("error"), "Branch1 was not created: " + branchID);

		inAppPurJson.put("name", "IAP1");
		inAppPurchaseID1 = purchasesApi.addPurchaseItem(seasonID, inAppPurJson.toString(), "ROOT", sessionToken);
		Assert.assertFalse(inAppPurchaseID1.contains("error"), "inAppPurchase was not added to the season: " + inAppPurchaseID1);
	
		String inAppPurchaseMix = FileUtils.fileToString(filePath + "purchases/inAppPurchaseMutual.txt", "UTF-8", false);
		mixID1 = purchasesApi.addPurchaseItem(seasonID, inAppPurchaseMix, inAppPurchaseID1, sessionToken);
		Assert.assertFalse(mixID1.contains("error"), "inAppPurchase was not added to the season: " + mixID1);
				
		inAppPurJson.put("name", "IAP2");
		inAppPurchaseID2 = purchasesApi.addPurchaseItem(seasonID, inAppPurJson.toString(), mixID1, sessionToken);
		Assert.assertFalse(inAppPurchaseID2.contains("error"), "inAppPurchase was not added to the season: " + inAppPurchaseID2);
		inAppPurJson.put("name", "IAP3");
		inAppPurchaseID3 = purchasesApi.addPurchaseItem(seasonID, inAppPurJson.toString(), mixID1, sessionToken);
		Assert.assertFalse(inAppPurchaseID3.contains("error"), "inAppPurchase was not added to the season: " + inAppPurchaseID3);


		
		inAppPurJson.put("name", "IAP4");
		inAppPurchaseID4 = purchasesApi.addPurchaseItem(seasonID, inAppPurJson.toString(), "ROOT", sessionToken);
		Assert.assertFalse(inAppPurchaseID4.contains("error"), "inAppPurchase4 was not added to the season: " + inAppPurchaseID4);
	
		mixID2 = purchasesApi.addPurchaseItem(seasonID, inAppPurchaseMix, inAppPurchaseID4, sessionToken);
		Assert.assertFalse(mixID1.contains("error"), "inAppPurchase was not added to the season: " + mixID1);
				
		inAppPurJson.put("name", "IAP5");
		inAppPurchaseID5 = purchasesApi.addPurchaseItem(seasonID, inAppPurJson.toString(), mixID2, sessionToken);
		Assert.assertFalse(inAppPurchaseID5.contains("error"), "inAppPurchase was not added to the season: " + inAppPurchaseID5);
		inAppPurJson.put("name", "IAP6");
		inAppPurchaseID6 = purchasesApi.addPurchaseItem(seasonID, inAppPurJson.toString(), mixID2, sessionToken);
		Assert.assertFalse(inAppPurchaseID6.contains("error"), "inAppPurchase was not added to the season: " + inAppPurchaseID6);


	}
	
	@Test (dependsOnMethods="scenario9", description="Checkout everything")
	public void checkout() throws JSONException, Exception{				
		
		String res = br.checkoutFeature(branchID, inAppPurchaseID2, sessionToken); //checks out MTX, IAP3 and IAP1
		Assert.assertFalse(res.contains("error"), "inAppPurchase2 was not unchecked out: " + res);
		res = br.checkoutFeature(branchID, inAppPurchaseID5, sessionToken); //checks out MTX, IAP6 and IAP4
		Assert.assertFalse(res.contains("error"), "inAppPurchase5 was not unchecked out: " + res);
		
		
		
		String iapMix = FileUtils.fileToString(filePath + "purchases/inAppPurchaseMutual.txt", "UTF-8", false);
		String mixID3 = purchasesApi.addPurchaseItemToBranch(seasonID, branchID, iapMix, "ROOT", sessionToken);
		
		JSONObject IAP1 = new JSONObject(purchasesApi.getPurchaseItemFromBranch(inAppPurchaseID1, branchID, sessionToken));
		JSONObject IAP4 = new JSONObject(purchasesApi.getPurchaseItemFromBranch(inAppPurchaseID4, branchID, sessionToken));
		JSONObject mix = new JSONObject(purchasesApi.getPurchaseItemFromBranch(mixID3, branchID, sessionToken));
		JSONArray children = mix.getJSONArray("entitlements");
		children.put(IAP1);
		children.put(IAP4);
		mix.put("entitlements", children);
		
		String response = purchasesApi.updatePurchaseItemInBranch(seasonID, branchID, mixID3, mix.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Can't create MTX group");
		JSONObject brJson = new JSONObject(br.getBranchWithFeatures(branchID, sessionToken));
		
		IAP1 = new JSONObject(purchasesApi.getPurchaseItemFromBranch(inAppPurchaseID1, branchID, sessionToken));
		Assert.assertTrue(IAP1.getString("branchStatus").equals("CHECKED_OUT"), "inAppPurchase1 incorrect status");
		JSONObject IAP2 = new JSONObject(purchasesApi.getPurchaseItemFromBranch(inAppPurchaseID2, branchID, sessionToken));
		Assert.assertTrue(IAP2.getString("branchStatus").equals("CHECKED_OUT"), "inAppPurchase2 incorrect status");
		JSONObject IAP3 = new JSONObject(purchasesApi.getPurchaseItemFromBranch(inAppPurchaseID3, branchID, sessionToken));
		Assert.assertTrue(IAP3.getString("branchStatus").equals("CHECKED_OUT"), "inAppPurchase3 incorrect status");
		IAP4 = new JSONObject(purchasesApi.getPurchaseItemFromBranch(inAppPurchaseID4, branchID, sessionToken));
		Assert.assertTrue(IAP4.getString("branchStatus").equals("CHECKED_OUT"), "inAppPurchase4 incorrect status");	
		JSONObject IAP5 = new JSONObject(purchasesApi.getPurchaseItemFromBranch(inAppPurchaseID5, branchID, sessionToken));
		Assert.assertTrue(IAP5.getString("branchStatus").equals("CHECKED_OUT"), "inAppPurchase5 incorrect status");
		JSONObject IAP6 = new JSONObject(purchasesApi.getPurchaseItemFromBranch(inAppPurchaseID6, branchID, sessionToken));
		Assert.assertTrue(IAP6.getString("branchStatus").equals("CHECKED_OUT"), "inAppPurchase6 incorrect status");
		JSONObject mix1 = new JSONObject(purchasesApi.getPurchaseItemFromBranch(mixID1, branchID, sessionToken));
		Assert.assertTrue(mix1.getString("branchStatus").equals("CHECKED_OUT"), "mix1 incorrect status");
		JSONObject mix2 = new JSONObject(purchasesApi.getPurchaseItemFromBranch(mixID2, branchID, sessionToken));
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
