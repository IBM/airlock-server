package tests.restapi.in_app_purchases;

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
import tests.restapi.InAppPurchasesRestApi;


public class CheckoutPurchaseScenario3 {
	protected String productID;
	protected String seasonID;
	private String branchID;
	private String inAppPurchaseID1;
	private String inAppPurchaseID2;
	private String inAppPurchaseID3;
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
	

	@Test (description ="IAP1 -> MIX -> (IAP2 + IAP3), checkout IAP2 ") 
	public void scenario3 () throws Exception {

		branchID = addBranch("branch1");
		Assert.assertFalse(branchID.contains("error"), "Branch1 was not created: " + branchID);

		inAppPurJson.put("name", RandomStringUtils.randomAlphabetic(5));
		inAppPurchaseID1 = purchasesApi.addPurchaseItem(seasonID, inAppPurJson.toString(), "ROOT", sessionToken);
		Assert.assertFalse(inAppPurchaseID1.contains("error"), "inAppPurchase was not added to the season: " + inAppPurchaseID1);
		
		String iapMix = FileUtils.fileToString(filePath + "purchases/inAppPurchaseMutual.txt", "UTF-8", false);
		String mixID1 = purchasesApi.addPurchaseItem(seasonID, iapMix, inAppPurchaseID1, sessionToken);
		Assert.assertFalse(mixID1.contains("error"), "inAppPurchase was not added to the season: " + mixID1);
				
		inAppPurJson.put("name", RandomStringUtils.randomAlphabetic(5));
		inAppPurchaseID2 = purchasesApi.addPurchaseItem(seasonID, inAppPurJson.toString(), mixID1, sessionToken);
		Assert.assertFalse(inAppPurchaseID2.contains("error"), "inAppPurchase was not added to the season: " + inAppPurchaseID2);
		
		inAppPurJson.put("name", RandomStringUtils.randomAlphabetic(5));
		inAppPurchaseID3 = purchasesApi.addPurchaseItem(seasonID, inAppPurJson.toString(), mixID1, sessionToken);
		Assert.assertFalse(inAppPurchaseID3.contains("error"), "inAppPurchase was not added to the season: " + inAppPurchaseID3);
		
		String response = br.checkoutFeature(branchID, inAppPurchaseID2, sessionToken);
		Assert.assertFalse(response.contains("error"), "inAppPurchase was not checked out to branch");
		
		//check that inAppPurchase was checked out
		response = br.getBranchWithFeatures(branchID, sessionToken);
		JSONObject brJson = new JSONObject(response);
		JSONArray inAppPurchases = brJson.getJSONArray("entitlements");
		
		//inAppPurchase is checked out in get inAppPurchases from branch
		JSONArray inAppPurchasesInBranch = purchasesApi.getPurchasesBySeasonFromBranch(seasonID, branchID, sessionToken);

		
		//IAP1		
		Assert.assertTrue(inAppPurchases.getJSONObject(0).getString("branchStatus").equals("CHECKED_OUT"), "inAppPurchase1 status is not checked_out in get branch" );	//get branch
		Assert.assertTrue(inAppPurchasesInBranch.getJSONObject(0).getString("branchStatus").equals("CHECKED_OUT"), "inAppPurchase1 status is not checked_out in get inAppPurchases" );	//get inAppPurchases
		Assert.assertTrue(new JSONObject(purchasesApi.getPurchaseItemFromBranch(inAppPurchaseID1, branchID, sessionToken)).getString("branchStatus").equals("CHECKED_OUT"), "inAppPurchase1 status is not checked_out in get inAppPurchase");	//get inAppPurchase from branch
		
		response = br.checkoutFeature(branchID, inAppPurchaseID1, sessionToken);
		Assert.assertTrue(response.contains("error"), "inAppPurchase1 was checked out twice");
				
		//IAP2
		Assert.assertTrue(inAppPurchases.getJSONObject(0).getJSONArray("entitlements").getJSONObject(0).getJSONArray("entitlements").getJSONObject(0)
				.getString("branchStatus").equals("CHECKED_OUT"), "inAppPurchase2 status is not checked_out in get branch" );
		Assert.assertTrue(new JSONObject(purchasesApi.getPurchaseItemFromBranch(inAppPurchaseID3, branchID, sessionToken)).getString("branchStatus").equals("CHECKED_OUT"), "inAppPurchase3 status is not checked_out in get inAppPurchase");	//get inAppPurchase from branch
		Assert.assertTrue(inAppPurchasesInBranch.getJSONObject(0).getJSONArray("entitlements").getJSONObject(0).getJSONArray("entitlements").getJSONObject(0)
				.getString("branchStatus").equals("CHECKED_OUT"), "inAppPurchase2 status is not checked_out in get inAppPurchases" );

		response = br.checkoutFeature(branchID, inAppPurchaseID2, sessionToken);
		Assert.assertTrue(response.contains("error"), "inAppPurchase2 was checked out twice");

		//IAP3
		Assert.assertTrue(inAppPurchases.getJSONObject(0).getJSONArray("entitlements").getJSONObject(0).getJSONArray("entitlements").getJSONObject(1)
				.getString("branchStatus").equals("CHECKED_OUT"), "Feature3 status is not checked_out in get branch" );
		Assert.assertTrue(new JSONObject(purchasesApi.getPurchaseItemFromBranch(inAppPurchaseID3, branchID, sessionToken)).getString("branchStatus").equals("CHECKED_OUT"), "inAppPurchase3 status was incorrectly changed in get inAppPurchase");	//get inAppPurchase from branch
		Assert.assertTrue(inAppPurchasesInBranch.getJSONObject(0).getJSONArray("entitlements").getJSONObject(0).getJSONArray("entitlements").getJSONObject(0)
				.getString("branchStatus").equals("CHECKED_OUT"), "inAppPurchase3 status is not checked_out in get inAppPurchases" );

	}
	
	@Test (dependsOnMethods="scenario3", description="Uncheck IAP1")
	public void uncheckInAppPurchase1() throws JSONException, Exception{				
		
		//uncheckout IAP1
		String res = br.cancelCheckoutFeature(branchID, inAppPurchaseID1, sessionToken);
		Assert.assertFalse(res.contains("error"), "inAppPurchase was not unchecked out: " + res);
		JSONObject brJson = new JSONObject(br.getBranchWithFeatures(branchID, sessionToken));
		
		
		JSONArray inAppPurchasesInBranch = purchasesApi.getPurchasesBySeasonFromBranch(seasonID, branchID, sessionToken);		
		JSONObject featureFromBranch = new JSONObject( purchasesApi.getPurchaseItemFromBranch(inAppPurchaseID1, branchID, sessionToken));
		
		Assert.assertTrue(featureFromBranch.getString("branchStatus").equals("NONE"), "Incorrect inAppPurchase1 status in get feature from branch");
		
		Assert.assertTrue(brJson.getJSONArray("entitlements").getJSONObject(0).getString("branchFeatureParentName").equals(featureFromBranch.getString("namespace")+"."+featureFromBranch.getString("name")), "Incorrect branchFeatureParentName for mix of features");
		Assert.assertTrue(brJson.getJSONArray("entitlements").getJSONObject(0).getString("branchStatus").equals("CHECKED_OUT"), "Incorrect status for feature2");
		Assert.assertTrue(brJson.getJSONArray("entitlements").getJSONObject(0).getJSONArray("entitlements").size()==2, "Incorrect number of checked out inAppPurchases in mix");
		Assert.assertTrue(inAppPurchasesInBranch.getJSONObject(0).getString("branchStatus").equals("NONE"), "inAppPurchase1 was not unchecked in get inAppPurchases from branch");

		//checkout IAP1
		res = br.checkoutFeature(branchID, inAppPurchaseID1, sessionToken);
		Assert.assertFalse(res.contains("error"), "inAppPurchase was not unchecked out: " + res);
	}

	@Test (dependsOnMethods="uncheckInAppPurchase1", description="Uncheck F2")
	public void uncheckInAppPurchase2() throws JSONException, Exception{				
		
		//uncheckout IAP2
		String res = br.cancelCheckoutFeature(branchID, inAppPurchaseID2, sessionToken);
		Assert.assertFalse(res.contains("error"), "inAppPurchase was not unchecked out: " + res);
		JSONObject brJson = new JSONObject(br.getBranchWithFeatures(branchID, sessionToken));
		
		
		JSONArray inAppPurchasesInBranch = purchasesApi.getPurchasesBySeasonFromBranch(seasonID, branchID, sessionToken);		
		JSONObject inAppPurchaseFromBranch = new JSONObject( purchasesApi.getPurchaseItemFromBranch(inAppPurchaseID2, branchID, sessionToken));
		
		Assert.assertTrue(inAppPurchaseFromBranch.getString("branchStatus").equals("NONE"), "Incorrect inAppPurchase2 status in get inAppPurchase from branch");		
		Assert.assertTrue(brJson.getJSONArray("entitlements").getJSONObject(0).getString("branchFeatureParentName").equals("ROOT"), "Incorrect branchFeatureParentName for inAppPurchase1");
		Assert.assertTrue(brJson.getJSONArray("entitlements").getJSONObject(0).getString("branchStatus").equals("CHECKED_OUT"), "Incorrect status for inAppPurchase1");
		Assert.assertTrue(brJson.getJSONArray("entitlements").getJSONObject(0).getJSONArray("entitlements").getJSONObject(0).getString("branchStatus").equals("CHECKED_OUT"), "Incorrect status for inAppPurchase2");
		Assert.assertTrue(brJson.getJSONArray("entitlements").getJSONObject(0).getJSONArray("entitlements").size()==1, "Incorrect number of checked out inAppPurchases in mix");
		Assert.assertTrue(inAppPurchasesInBranch.getJSONObject(0).getJSONArray("entitlements").getJSONObject(0).getJSONArray("entitlements").getJSONObject(0)
				.getString("branchStatus").equals("NONE"), "inAppPurchase3 status is not NONE in get inAppPurchases" );

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
