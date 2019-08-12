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


public class CheckoutRoot {
	protected String productID;
	protected String seasonID;
	private String branchID;
	private String entitlementID1;
	private String entitlementID2;
	private String entitlementID3;
	private String entitlementID4;
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
	}
	
	@Test (description ="IAP1, IAP2 -> IAP3") 
	public void addComponents () throws Exception {
		branchID = addBranch("branch1");
		Assert.assertFalse(branchID.contains("error"), "Branch1 was not created: " + branchID);

		inAppPurJson.put("name", "IAP1");
		entitlementID1 = purchasesApi.addPurchaseItem(seasonID, inAppPurJson.toString(), "ROOT", sessionToken);
		Assert.assertFalse(entitlementID1.contains("error"), "entitlement was not added to the season: " + entitlementID1);
				
		inAppPurJson.put("name", "IAP2");
		entitlementID2 = purchasesApi.addPurchaseItem(seasonID, inAppPurJson.toString(), "ROOT", sessionToken);
		Assert.assertFalse(entitlementID2.contains("error"), "entitlement was not added to the season: " + entitlementID2);
		
		inAppPurJson.put("name", "IAP3");
		entitlementID3 = purchasesApi.addPurchaseItem(seasonID, inAppPurJson.toString(), entitlementID2, sessionToken);
		Assert.assertFalse(entitlementID3.contains("error"), "entitlement was not added to the season: " + entitlementID3);
	}
	
	@Test (dependsOnMethods="addComponents", description="Add IAP4 to branch and move IAP2 to IAP4 in branch")
	public void addEntitlementToBranch() throws JSONException, Exception{				
		inAppPurJson.put("name", "IAP4");
		entitlementID4 = purchasesApi.addPurchaseItemToBranch(seasonID, branchID,  inAppPurJson.toString(), "ROOT", sessionToken);
		Assert.assertFalse(entitlementID1.contains("error"), "entitlement was not added to the branch: " + entitlementID1);
	
		JSONObject entitlement4 = new JSONObject(purchasesApi.getPurchaseItemFromBranch(entitlementID4, branchID, sessionToken));
		JSONObject entitlement2 = new JSONObject(purchasesApi.getPurchaseItemFromBranch(entitlementID2, branchID, sessionToken));
		
		JSONArray children = entitlement4.getJSONArray("entitlements");
		children.put(entitlement2);
		entitlement4.put("entitlements", children);
		
		String response = purchasesApi.updatePurchaseItemInBranch(seasonID, branchID, entitlementID4, entitlement4.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Couldn't move unchecked entitlement under new feature");
		
	}
	
	
	@Test (dependsOnMethods="addEntitlementToBranch", description="Checkout root")
	public void checkoutRoot() throws JSONException, Exception{				
		String rootId = purchasesApi.getBranchRootId(seasonID, "MASTER", sessionToken);
		String response = br.checkoutFeature(branchID, rootId, sessionToken);
		Assert.assertFalse(response.contains("error"), "can't check out root");
		
		JSONObject root = new JSONObject(purchasesApi.getPurchaseItemFromBranch(rootId, branchID, sessionToken));
		JSONArray expected = new JSONArray();
		expected.add("ns1.IAP1");
		expected.add("ns1.IAP4");
		Assert.assertEqualsNoOrder(expected.toArray(), root.getJSONArray("branchEntitlementItems").toArray(), "Incorrect items in branchEntitlementItems");
		
		JSONObject f1 = new JSONObject(purchasesApi.getPurchaseItemFromBranch(entitlementID1, branchID, sessionToken));
		Assert.assertTrue(f1.getJSONArray("entitlements").size()==0, "Incorrect number of entitlements under IAP1");
		JSONObject f4 = new JSONObject(purchasesApi.getPurchaseItemFromBranch(entitlementID4, branchID, sessionToken));
		Assert.assertTrue(f4.getJSONArray("entitlements").size()==1, "Incorrect number of entitlements under IAP4");
		JSONObject f2 = new JSONObject(purchasesApi.getPurchaseItemFromBranch(entitlementID2, branchID, sessionToken));
		Assert.assertTrue(f2.getJSONArray("entitlements").size()==1, "Incorrect number of entitlements under IAP2");

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
