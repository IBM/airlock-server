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


public class UpdatePurchasesTreeInMasterAndBranch {
	protected String productID;
	protected String seasonID;
	private String branchID;
	private String entitlementID1;
	private String entitlementID2;
	private String purchaseOptionsID1;
	private String purchaseOptionsID2;
	private String purchaseOptionsID3;
	private String purchaseOptionsID4;
	private String mixConfigID;
	private String configID1;
	private String configID2;
	protected String filePath;
	protected String m_url;
	private String sessionToken = "";
	private AirlockUtils baseUtils;
	private JSONObject entitlement;
	private JSONObject purOptJson;
	protected InAppPurchasesRestApi purchasesApi;
	private BranchesRestApi br ;
	
	
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
		entitlement = new JSONObject(FileUtils.fileToString(filePath + "purchases/inAppPurchase1.txt", "UTF-8", false));
		purOptJson = new JSONObject(FileUtils.fileToString(filePath + "purchases/purchaseOptions1.txt", "UTF-8", false));
	}
	
	/*
	 * Master:
	 * IAP1->PO1 (configmtx->cr1, cr2), PO2   IAP2->PO3, PO4
	 */

	@Test (description ="Add purchaseOptions with configurations to branch") 
	public void addComponents () throws IOException, JSONException, InterruptedException {
		branchID = addBranch("branch1");
		Assert.assertFalse(branchID.contains("error"), "Branch1 was not created: " + branchID);

		entitlement.put("name", "IAP1");
		entitlementID1 = purchasesApi.addPurchaseItemToBranch(seasonID, "MASTER", entitlement.toString(), "ROOT", sessionToken);
		Assert.assertFalse(entitlementID1.contains("error"), "entitlement was not added to the season: " + entitlementID1);
		
		purOptJson.put("name", "PO1");
		purchaseOptionsID1 = purchasesApi.addPurchaseItemToBranch(seasonID, "MASTER", purOptJson.toString(), entitlementID1, sessionToken);
		Assert.assertFalse(purchaseOptionsID1.contains("error"), "entitlement was not added to the season: " + purchaseOptionsID1);
				
		String configurationMix = FileUtils.fileToString(filePath + "configuration_feature-mutual.txt", "UTF-8", false);
		mixConfigID = purchasesApi.addPurchaseItemToBranch(seasonID, "MASTER", configurationMix, purchaseOptionsID1, sessionToken);
		Assert.assertFalse(mixConfigID.contains("error"), "Configuration mix was not added to the season");

		String configuration1 = FileUtils.fileToString(filePath + "configuration_rule1.txt", "UTF-8", false);
		configID1 = purchasesApi.addPurchaseItemToBranch(seasonID, "MASTER", configuration1, mixConfigID, sessionToken);
		Assert.assertFalse(configID1.contains("error"), "Configuration rule1 was not added to the season");
				
		String configuration2 = FileUtils.fileToString(filePath + "configuration_rule2.txt", "UTF-8", false);
		configID2 = purchasesApi.addPurchaseItemToBranch(seasonID, "MASTER", configuration2, mixConfigID, sessionToken);
		Assert.assertFalse(configID2.contains("error"), "purchaseOptions rule1 was not added to the season");
		
		purOptJson.put("name", "PO2");
		purchaseOptionsID2 = purchasesApi.addPurchaseItemToBranch(seasonID, "MASTER", purOptJson.toString(), entitlementID1, sessionToken);
		Assert.assertFalse(purchaseOptionsID1.contains("error"), "purchaseOptions was not added to the season: " + purchaseOptionsID1);
	
		entitlement.put("name", "IAP2");
		entitlementID2 = purchasesApi.addPurchaseItemToBranch(seasonID, "MASTER", entitlement.toString(), "ROOT", sessionToken);
		Assert.assertFalse(entitlementID1.contains("error"), "entitlement was not added to the season: " + entitlementID1);
		
		purOptJson.put("name", "PO3");
		purchaseOptionsID3 = purchasesApi.addPurchaseItemToBranch(seasonID, "MASTER", purOptJson.toString(), entitlementID2, sessionToken);
		Assert.assertFalse(purchaseOptionsID1.contains("error"), "purchaseOptions was not added to the season: " + purchaseOptionsID1);
		
		purOptJson.put("name", "PO4");
		purchaseOptionsID4 = purchasesApi.addPurchaseItemToBranch(seasonID, "MASTER", purOptJson.toString(), entitlementID2, sessionToken);
		Assert.assertFalse(purchaseOptionsID1.contains("error"), "purchaseOptions was not added to the season: " + purchaseOptionsID1);
		
		JSONArray purchases = purchasesApi.getPurchasesBySeasonFromBranch(seasonID, "MASTER", sessionToken);
		Assert.assertEquals(purchases.size(), 2, "The number of purchases is incorrect. " + purchases.size());
		
		JSONObject iap1Obj = purchases.getJSONObject(0);
		Assert.assertTrue(iap1Obj.getString("uniqueId").equals(entitlementID1), "incorrect entitlementID1");
		Assert.assertTrue(iap1Obj.getJSONArray("purchaseOptions").size() == 2, "incorrect number of purchaseOptions");
		JSONObject po1Obj = iap1Obj.getJSONArray("purchaseOptions").getJSONObject(0);
		Assert.assertTrue(po1Obj.getString("uniqueId").equals(purchaseOptionsID1), "incorrect purchaseOptionsID1");
		JSONObject po2Obj = iap1Obj.getJSONArray("purchaseOptions").getJSONObject(1);
		Assert.assertTrue(po2Obj.getString("uniqueId").equals(purchaseOptionsID2), "incorrect purchaseOptionsID2");
		
		
		JSONObject iap2Obj = purchases.getJSONObject(1);
		Assert.assertTrue(iap2Obj.getString("uniqueId").equals(entitlementID2), "incorrect entitlementID1");
		Assert.assertTrue(iap2Obj.getJSONArray("purchaseOptions").size() == 2, "incorrect number of purchaseOptions");
		JSONObject po3Obj = iap2Obj.getJSONArray("purchaseOptions").getJSONObject(0);
		Assert.assertTrue(po3Obj.getString("uniqueId").equals(purchaseOptionsID3), "incorrect purchaseOptionsID3");
		JSONObject po4Obj = iap2Obj.getJSONArray("purchaseOptions").getJSONObject(1);
		Assert.assertTrue(po4Obj.getString("uniqueId").equals(purchaseOptionsID4), "incorrect purchaseOptionsID4");
		
	}
	
		
	/*
	 * IAP1 -> PO2   IAP2->PO3, PO1 (configmtx->cr1, cr2), PO4
	 */

	@Test(dependsOnMethods="addComponents", description ="update purchaseOptions parent in branch")
	public void updatePurcahseOptionsParentInBranch() throws Exception{
		String e2 = purchasesApi.getPurchaseItemFromBranch(entitlementID2, branchID, sessionToken);
		JSONObject e2Obj = new JSONObject(e2);
		JSONArray purchaseOptionsArr = e2Obj.getJSONArray("purchaseOptions");
		
		String op1 = purchasesApi.getPurchaseItemFromBranch(purchaseOptionsID1, branchID, sessionToken);
		JSONArray newPurchaseOptionsArr = new JSONArray();
		newPurchaseOptionsArr.add(purchaseOptionsArr.getJSONObject(0));
		newPurchaseOptionsArr.add(new JSONObject(op1));
		newPurchaseOptionsArr.add(purchaseOptionsArr.getJSONObject(1));
		
		e2Obj.put("purchaseOptions", newPurchaseOptionsArr);
		
		String response = purchasesApi.updatePurchaseItemInBranch(seasonID, branchID, entitlementID2, e2Obj.toString(), sessionToken);
		Assert.assertTrue(response.contains("error"), "can move NONE purchaseOptions to NONE entitlement");
		
		response = br.checkoutFeature(branchID, entitlementID2, sessionToken);
		Assert.assertFalse(response.contains("error"), "cannot check out entitlement");
		
		e2 = purchasesApi.getPurchaseItemFromBranch(entitlementID2, branchID, sessionToken);
		e2Obj = new JSONObject(e2);
		purchaseOptionsArr = e2Obj.getJSONArray("purchaseOptions");
		
		op1 = purchasesApi.getPurchaseItemFromBranch(purchaseOptionsID1, branchID, sessionToken);
		newPurchaseOptionsArr = new JSONArray();
		newPurchaseOptionsArr.add(purchaseOptionsArr.getJSONObject(0));
		newPurchaseOptionsArr.add(new JSONObject(op1));
		newPurchaseOptionsArr.add(purchaseOptionsArr.getJSONObject(1));
		
		e2Obj.put("purchaseOptions", newPurchaseOptionsArr);
			
		response = purchasesApi.updatePurchaseItemInBranch(seasonID, branchID, entitlementID2, e2Obj.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "cannot move NONE purchaseOptions to CHECKED_OUT entitlement: " +response);
		
		JSONArray purchases = purchasesApi.getPurchasesBySeasonFromBranch(seasonID, branchID, sessionToken);
		Assert.assertEquals(purchases.size(), 2, "The number of purchases is incorrect. " + purchases.size());
		
		JSONObject iap1Obj = purchases.getJSONObject(0);
		Assert.assertTrue(iap1Obj.getString("uniqueId").equals(entitlementID1), "incorrect entitlementID1");
		Assert.assertTrue(iap1Obj.getString("branchStatus").equals("NONE"), "incorrect branchStatus");
		Assert.assertTrue(iap1Obj.getJSONArray("purchaseOptions").size() == 1, "incorrect number of purchaseOptions");
		JSONObject po2Obj = iap1Obj.getJSONArray("purchaseOptions").getJSONObject(0);
		Assert.assertTrue(po2Obj.getString("uniqueId").equals(purchaseOptionsID2), "incorrect purchaseOptionsID2");
		Assert.assertTrue(po2Obj.getString("branchStatus").equals("NONE"), "incorrect branchStatus");
			
		JSONObject iap2Obj = purchases.getJSONObject(1);
		Assert.assertTrue(iap2Obj.getString("uniqueId").equals(entitlementID2), "incorrect entitlementID2");
		Assert.assertTrue(iap2Obj.getString("branchStatus").equals("CHECKED_OUT"), "incorrect branchStatus");
		
		Assert.assertTrue(iap2Obj.getJSONArray("purchaseOptions").size() == 3, "incorrect number of purchaseOptions");
		JSONObject po3Obj = iap2Obj.getJSONArray("purchaseOptions").getJSONObject(0);
		Assert.assertTrue(po3Obj.getString("uniqueId").equals(purchaseOptionsID3), "incorrect purchaseOptionsID3");
		Assert.assertTrue(po3Obj.getString("branchStatus").equals("NONE"), "incorrect branchStatus");	
		JSONObject po1Obj = iap2Obj.getJSONArray("purchaseOptions").getJSONObject(1);
		Assert.assertTrue(po1Obj.getString("uniqueId").equals(purchaseOptionsID1), "incorrect purchaseOptionsID1");
		Assert.assertTrue(po1Obj.getString("branchStatus").equals("NONE"), "incorrect branchStatus");
		
		JSONObject po4Obj = iap2Obj.getJSONArray("purchaseOptions").getJSONObject(2);
		Assert.assertTrue(po4Obj.getString("uniqueId").equals(purchaseOptionsID4), "incorrect purchaseOptionsID4");
		Assert.assertTrue(po4Obj.getString("branchStatus").equals("NONE"), "incorrect branchStatus");
		
	}
	
	@Test(dependsOnMethods="updatePurcahseOptionsParentInBranch", description ="checkout PO2")
	public void chcekOutPurchaseOptions2() throws Exception{
		String response = br.checkoutFeature(branchID, purchaseOptionsID2, sessionToken);
		Assert.assertFalse(response.contains("error"), "cannot check out purchaseOptions");
		
		JSONArray purchases = purchasesApi.getPurchasesBySeasonFromBranch(seasonID, branchID, sessionToken);
		Assert.assertEquals(purchases.size(), 2, "The number of purchases is incorrect. " + purchases.size());
		
		JSONObject iap1Obj = purchases.getJSONObject(0);
		Assert.assertTrue(iap1Obj.getString("uniqueId").equals(entitlementID1), "incorrect entitlementID1");
		Assert.assertTrue(iap1Obj.getString("branchStatus").equals("CHECKED_OUT"), "incorrect branchStatus");
		Assert.assertTrue(iap1Obj.getJSONArray("purchaseOptions").size() == 1, "incorrect number of purchaseOptions");
		JSONObject po2Obj = iap1Obj.getJSONArray("purchaseOptions").getJSONObject(0);
		Assert.assertTrue(po2Obj.getString("uniqueId").equals(purchaseOptionsID2), "incorrect purchaseOptionsID2");
		Assert.assertTrue(po2Obj.getString("branchStatus").equals("CHECKED_OUT"), "incorrect branchStatus");
			
		JSONObject iap2Obj = purchases.getJSONObject(1);
		Assert.assertTrue(iap2Obj.getString("uniqueId").equals(entitlementID2), "incorrect entitlementID2");
		Assert.assertTrue(iap2Obj.getString("branchStatus").equals("CHECKED_OUT"), "incorrect branchStatus");
		
		Assert.assertTrue(iap2Obj.getJSONArray("purchaseOptions").size() == 3, "incorrect number of purchaseOptions");
		JSONObject po3Obj = iap2Obj.getJSONArray("purchaseOptions").getJSONObject(0);
		Assert.assertTrue(po3Obj.getString("uniqueId").equals(purchaseOptionsID3), "incorrect purchaseOptionsID3");
		Assert.assertTrue(po3Obj.getString("branchStatus").equals("NONE"), "incorrect branchStatus");	
		JSONObject po1Obj = iap2Obj.getJSONArray("purchaseOptions").getJSONObject(1);
		Assert.assertTrue(po1Obj.getString("uniqueId").equals(purchaseOptionsID1), "incorrect purchaseOptionsID1");
		Assert.assertTrue(po1Obj.getString("branchStatus").equals("NONE"), "incorrect branchStatus");
		
		JSONObject po4Obj = iap2Obj.getJSONArray("purchaseOptions").getJSONObject(2);
		Assert.assertTrue(po4Obj.getString("uniqueId").equals(purchaseOptionsID4), "incorrect purchaseOptionsID4");
		Assert.assertTrue(po4Obj.getString("branchStatus").equals("NONE"), "incorrect branchStatus");

		response = br.cancelCheckoutFeature(branchID, entitlementID1, sessionToken);
		Assert.assertFalse(response.contains("error"), "cannot check out entitlement");

		purchases = purchasesApi.getPurchasesBySeasonFromBranch(seasonID, branchID, sessionToken);
		Assert.assertEquals(purchases.size(), 2, "The number of purchases is incorrect. " + purchases.size());
		
		iap1Obj = purchases.getJSONObject(0);
		Assert.assertTrue(iap1Obj.getString("uniqueId").equals(entitlementID1), "incorrect entitlementID1");
		Assert.assertTrue(iap1Obj.getString("branchStatus").equals("NONE"), "incorrect branchStatus");
		Assert.assertTrue(iap1Obj.getJSONArray("purchaseOptions").size() == 1, "incorrect number of purchaseOptions");
		po2Obj = iap1Obj.getJSONArray("purchaseOptions").getJSONObject(0);
		Assert.assertTrue(po2Obj.getString("uniqueId").equals(purchaseOptionsID2), "incorrect purchaseOptionsID2");
		Assert.assertTrue(po2Obj.getString("branchStatus").equals("CHECKED_OUT"), "incorrect branchStatus");
			
		iap2Obj = purchases.getJSONObject(1);
		Assert.assertTrue(iap2Obj.getString("uniqueId").equals(entitlementID2), "incorrect entitlementID2");
		Assert.assertTrue(iap2Obj.getString("branchStatus").equals("CHECKED_OUT"), "incorrect branchStatus");
		
		Assert.assertTrue(iap2Obj.getJSONArray("purchaseOptions").size() == 3, "incorrect number of purchaseOptions");
		po3Obj = iap2Obj.getJSONArray("purchaseOptions").getJSONObject(0);
		Assert.assertTrue(po3Obj.getString("uniqueId").equals(purchaseOptionsID3), "incorrect purchaseOptionsID3");
		Assert.assertTrue(po3Obj.getString("branchStatus").equals("NONE"), "incorrect branchStatus");	
		po1Obj = iap2Obj.getJSONArray("purchaseOptions").getJSONObject(1);
		Assert.assertTrue(po1Obj.getString("uniqueId").equals(purchaseOptionsID1), "incorrect purchaseOptionsID1");
		Assert.assertTrue(po1Obj.getString("branchStatus").equals("NONE"), "incorrect branchStatus");
		
		po4Obj = iap2Obj.getJSONArray("purchaseOptions").getJSONObject(2);
		Assert.assertTrue(po4Obj.getString("uniqueId").equals(purchaseOptionsID4), "incorrect purchaseOptionsID4");
		Assert.assertTrue(po4Obj.getString("branchStatus").equals("NONE"), "incorrect branchStatus");

		JSONObject branch = new JSONObject(br.getBranchWithFeatures(branchID, sessionToken));
		JSONArray entitlements = branch.getJSONArray("entitlements");
		Assert.assertTrue(entitlements.size() == 2, "incorrect number of entitlements in branch");
		Assert.assertTrue(entitlements.getJSONObject(0).getString("uniqueId").equals(entitlementID2), "incorrect branch entitlements");
		Assert.assertTrue(entitlements.getJSONObject(0).getString("branchStatus").equals("CHECKED_OUT"), "incorrect branch status");
		Assert.assertTrue(entitlements.getJSONObject(0).getString("branchFeatureParentName").equals("ROOT"), "incorrect branch parent");
		
		Assert.assertTrue(entitlements.getJSONObject(1).getString("uniqueId").equals(purchaseOptionsID2), "incorrect branch entitlements");
		Assert.assertTrue(entitlements.getJSONObject(1).getString("branchStatus").equals("CHECKED_OUT"), "incorrect branch status");
		Assert.assertTrue(entitlements.getJSONObject(1).getString("branchFeatureParentName").equals("ns1.IAP1"), "incorrect branch parent");
	}

	@Test(dependsOnMethods="chcekOutPurchaseOptions2", description ="update purchaseOptions 2 parent ")
	public void updatePurchaseOptionsParent2() throws Exception{
		String e2 = purchasesApi.getPurchaseItemFromBranch(entitlementID2, branchID, sessionToken);
		JSONObject e2Obj = new JSONObject(e2);
		JSONArray purchaseOptionsArr = e2Obj.getJSONArray("purchaseOptions");
		
		String op2 = purchasesApi.getPurchaseItemFromBranch(purchaseOptionsID2, branchID, sessionToken);
		JSONArray newPurchaseOptionsArr = new JSONArray();
		newPurchaseOptionsArr.add(new JSONObject(op2));
		newPurchaseOptionsArr.add(purchaseOptionsArr.getJSONObject(0));
		newPurchaseOptionsArr.add(purchaseOptionsArr.getJSONObject(1));
		newPurchaseOptionsArr.add(purchaseOptionsArr.getJSONObject(2));
		
		e2Obj.put("purchaseOptions", newPurchaseOptionsArr);
			
		String response = purchasesApi.updatePurchaseItemInBranch(seasonID, branchID, entitlementID2, e2Obj.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "cannot move NONE purchaseOptions to CHECKED_OUT entitlement: " +response);
		
		JSONArray purchases = purchasesApi.getPurchasesBySeasonFromBranch(seasonID, branchID, sessionToken);
		Assert.assertEquals(purchases.size(), 2, "The number of purchases is incorrect. " + purchases.size());
		
		JSONObject iap1Obj = purchases.getJSONObject(0);
		Assert.assertTrue(iap1Obj.getString("uniqueId").equals(entitlementID1), "incorrect entitlementID1");
		Assert.assertTrue(iap1Obj.getString("branchStatus").equals("NONE"), "incorrect branchStatus");
		Assert.assertTrue(iap1Obj.getJSONArray("purchaseOptions").size() == 0, "incorrect number of purchaseOptions");
			
		JSONObject iap2Obj = purchases.getJSONObject(1);
		Assert.assertTrue(iap2Obj.getString("uniqueId").equals(entitlementID2), "incorrect entitlementID2");
		Assert.assertTrue(iap2Obj.getString("branchStatus").equals("CHECKED_OUT"), "incorrect branchStatus");
		
		Assert.assertTrue(iap2Obj.getJSONArray("purchaseOptions").size() == 4, "incorrect number of purchaseOptions");
		JSONObject po2Obj = iap2Obj.getJSONArray("purchaseOptions").getJSONObject(0);
		Assert.assertTrue(po2Obj.getString("uniqueId").equals(purchaseOptionsID2), "incorrect purchaseOptionsID3");
		Assert.assertTrue(po2Obj.getString("branchStatus").equals("CHECKED_OUT"), "incorrect branchStatus");		
		JSONObject po3Obj = iap2Obj.getJSONArray("purchaseOptions").getJSONObject(1);
		Assert.assertTrue(po3Obj.getString("uniqueId").equals(purchaseOptionsID3), "incorrect purchaseOptionsID3");
		Assert.assertTrue(po3Obj.getString("branchStatus").equals("NONE"), "incorrect branchStatus");	
		JSONObject po1Obj = iap2Obj.getJSONArray("purchaseOptions").getJSONObject(2);
		Assert.assertTrue(po1Obj.getString("uniqueId").equals(purchaseOptionsID1), "incorrect purchaseOptionsID1");
		Assert.assertTrue(po1Obj.getString("branchStatus").equals("NONE"), "incorrect branchStatus");	
		JSONObject po4Obj = iap2Obj.getJSONArray("purchaseOptions").getJSONObject(3);
		Assert.assertTrue(po4Obj.getString("uniqueId").equals(purchaseOptionsID4), "incorrect purchaseOptionsID4");
		Assert.assertTrue(po4Obj.getString("branchStatus").equals("NONE"), "incorrect branchStatus");
		
		JSONObject branch = new JSONObject(br.getBranchWithFeatures(branchID, sessionToken));
		JSONArray entitlements = branch.getJSONArray("entitlements");
		Assert.assertTrue(entitlements.size() == 1, "incorrect number of entitlements in branch");
		Assert.assertTrue(entitlements.getJSONObject(0).getString("uniqueId").equals(entitlementID2), "incorrect branch entitlements");
		Assert.assertTrue(entitlements.getJSONObject(0).getString("branchStatus").equals("CHECKED_OUT"), "incorrect branch status");
		Assert.assertTrue(entitlements.getJSONObject(0).getString("branchFeatureParentName").equals("ROOT"), "incorrect branch parent");
		
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
