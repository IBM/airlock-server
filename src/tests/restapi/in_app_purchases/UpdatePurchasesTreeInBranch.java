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


public class UpdatePurchasesTreeInBranch {
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
	 * IAP1->PO1 (configmtx->cr1, cr2), PO2   IAP2->PO3, PO4
	 */

	@Test (description ="Add purchaseOptions with configurations to branch") 
	public void addComponents () throws IOException, JSONException, InterruptedException {
		branchID = addBranch("branch1");
		Assert.assertFalse(branchID.contains("error"), "Branch1 was not created: " + branchID);

		entitlement.put("name", "IAP1");
		entitlementID1 = purchasesApi.addPurchaseItemToBranch(seasonID, branchID, entitlement.toString(), "ROOT", sessionToken);
		Assert.assertFalse(entitlementID1.contains("error"), "entitlement was not added to the season: " + entitlementID1);
		
		purOptJson.put("name", "PO1");
		purchaseOptionsID1 = purchasesApi.addPurchaseItemToBranch(seasonID, branchID, purOptJson.toString(), entitlementID1, sessionToken);
		Assert.assertFalse(purchaseOptionsID1.contains("error"), "entitlement was not added to the season: " + purchaseOptionsID1);
				
		String configurationMix = FileUtils.fileToString(filePath + "configuration_feature-mutual.txt", "UTF-8", false);
		mixConfigID = purchasesApi.addPurchaseItemToBranch(seasonID, branchID, configurationMix, purchaseOptionsID1, sessionToken);
		Assert.assertFalse(mixConfigID.contains("error"), "Configuration mix was not added to the season");

		String configuration1 = FileUtils.fileToString(filePath + "configuration_rule1.txt", "UTF-8", false);
		configID1 = purchasesApi.addPurchaseItemToBranch(seasonID, branchID, configuration1, mixConfigID, sessionToken);
		Assert.assertFalse(configID1.contains("error"), "Configuration rule1 was not added to the season");
				
		String configuration2 = FileUtils.fileToString(filePath + "configuration_rule2.txt", "UTF-8", false);
		configID2 = purchasesApi.addPurchaseItemToBranch(seasonID, branchID, configuration2, mixConfigID, sessionToken);
		Assert.assertFalse(configID2.contains("error"), "purchaseOptions rule1 was not added to the season");
		
		purOptJson.put("name", "PO2");
		purchaseOptionsID2 = purchasesApi.addPurchaseItemToBranch(seasonID, branchID, purOptJson.toString(), entitlementID1, sessionToken);
		Assert.assertFalse(purchaseOptionsID1.contains("error"), "purchaseOptions was not added to the season: " + purchaseOptionsID1);
	
		entitlement.put("name", "IAP2");
		entitlementID2 = purchasesApi.addPurchaseItemToBranch(seasonID, branchID, entitlement.toString(), "ROOT", sessionToken);
		Assert.assertFalse(entitlementID1.contains("error"), "entitlement was not added to the season: " + entitlementID1);
		
		purOptJson.put("name", "PO3");
		purchaseOptionsID3 = purchasesApi.addPurchaseItemToBranch(seasonID, branchID, purOptJson.toString(), entitlementID2, sessionToken);
		Assert.assertFalse(purchaseOptionsID1.contains("error"), "purchaseOptions was not added to the season: " + purchaseOptionsID1);
		
		purOptJson.put("name", "PO4");
		purchaseOptionsID4 = purchasesApi.addPurchaseItemToBranch(seasonID, branchID, purOptJson.toString(), entitlementID2, sessionToken);
		Assert.assertFalse(purchaseOptionsID1.contains("error"), "purchaseOptions was not added to the season: " + purchaseOptionsID1);
		
		JSONArray purchases = purchasesApi.getPurchasesBySeasonFromBranch(seasonID, branchID, sessionToken);
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
	
	@Test(dependsOnMethods="addComponents", description ="Delete purchaseOptions during entitlement update")
	public void deletepPurchaseOptionsDuringUpdate() throws Exception{
		String e1 = purchasesApi.getPurchaseItemFromBranch(entitlementID1, branchID, sessionToken);
		JSONObject e1Obj = new JSONObject(e1);
		e1Obj.put("purchaseOptions", new JSONArray());
		
		String response = purchasesApi.updatePurchaseItemInBranch(seasonID, branchID, entitlementID1, e1Obj.toString(), sessionToken);
		Assert.assertTrue(response.contains("error") && response.contains("is missing"), "can delete purchaseOptions during entitlement update");
	}
	
	@Test(dependsOnMethods="deletepPurchaseOptionsDuringUpdate", description ="Delete entitlements during root update")
	public void deletepEntitlementDuringUpdate() throws Exception{
		String purcahsesRootId = purchasesApi.getBranchRootId(seasonID, branchID, sessionToken);
		String root = purchasesApi.getPurchaseItemFromBranch(purcahsesRootId, branchID, sessionToken);
		JSONObject rootObj = new JSONObject(root);
		rootObj.put("entitlements", new JSONArray());
		
		String response = purchasesApi.updatePurchaseItemInBranch(seasonID, branchID, purcahsesRootId, rootObj.toString(), sessionToken);
		Assert.assertTrue(response.contains("error") && response.contains("is missing"), "can delete entitlements during root update");
	}
	
	/*
	 * IAP1 -> PO2   IAP2->PO3, PO1 (configmtx->cr1, cr2), PO4
	 */

	@Test(dependsOnMethods="deletepPurchaseOptionsDuringUpdate", description ="update purchaseOptions parent")
	public void updatePurcahseOptionsParent() throws Exception{
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
		Assert.assertFalse(response.contains("error"), "Fail updating entitlementID2: " + response);
		
		
		JSONArray purchases = purchasesApi.getPurchasesBySeasonFromBranch(seasonID, branchID, sessionToken);
		Assert.assertEquals(purchases.size(), 2, "The number of purchases is incorrect. " + purchases.size());
		
		JSONObject iap1Obj = purchases.getJSONObject(0);
		Assert.assertTrue(iap1Obj.getString("uniqueId").equals(entitlementID1), "incorrect entitlementID1");
		Assert.assertTrue(iap1Obj.getJSONArray("purchaseOptions").size() == 1, "incorrect number of purchaseOptions");
		JSONObject po2Obj = iap1Obj.getJSONArray("purchaseOptions").getJSONObject(0);
		Assert.assertTrue(po2Obj.getString("uniqueId").equals(purchaseOptionsID2), "incorrect purchaseOptionsID2");
		
		
		JSONObject iap2Obj = purchases.getJSONObject(1);
		Assert.assertTrue(iap2Obj.getString("uniqueId").equals(entitlementID2), "incorrect entitlementID2");
		Assert.assertTrue(iap2Obj.getJSONArray("purchaseOptions").size() == 3, "incorrect number of purchaseOptions");
		JSONObject po3Obj = iap2Obj.getJSONArray("purchaseOptions").getJSONObject(0);
		Assert.assertTrue(po3Obj.getString("uniqueId").equals(purchaseOptionsID3), "incorrect purchaseOptionsID3");
		JSONObject po1Obj = iap2Obj.getJSONArray("purchaseOptions").getJSONObject(1);
		Assert.assertTrue(po1Obj.getString("uniqueId").equals(purchaseOptionsID1), "incorrect purchaseOptionsID1");
		JSONObject po4Obj = iap2Obj.getJSONArray("purchaseOptions").getJSONObject(2);
		Assert.assertTrue(po4Obj.getString("uniqueId").equals(purchaseOptionsID4), "incorrect purchaseOptionsID4");
	
	}

	/*
	 *    IAP2->PO3, PO1 (configmtx->cr1, cr2), PO4    IAP1 -> PO2
	 */

	@Test(dependsOnMethods="updatePurcahseOptionsParent", description ="update entitlements order")
	public void updateEntitlementsOrder() throws Exception{
		String purcahsesRootId = purchasesApi.getBranchRootId(seasonID, branchID, sessionToken);
		
		String response = br.checkoutFeature(branchID, purcahsesRootId, sessionToken);
		Assert.assertFalse(response.contains("error"), "Fail checkout root: " + response);
		
		String root = purchasesApi.getPurchaseItemFromBranch(purcahsesRootId, branchID, sessionToken);
		JSONObject rootObj = new JSONObject(root);
		JSONArray entitlementsArr = rootObj.getJSONArray("entitlements");
		
		JSONArray newEntitlementsArr = new JSONArray();
		newEntitlementsArr.add(entitlementsArr.getJSONObject(1));
		newEntitlementsArr.add(entitlementsArr.getJSONObject(0));
		
		rootObj.put("entitlements", newEntitlementsArr);
		
		response = purchasesApi.updatePurchaseItemInBranch(seasonID, branchID, purcahsesRootId, rootObj.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Fail updating root: " + response);
		
		JSONArray purchases = purchasesApi.getPurchasesBySeasonFromBranch(seasonID, branchID, sessionToken);
		Assert.assertEquals(purchases.size(), 2, "The number of purchases is incorrect. " + purchases.size());
		
		JSONObject iap1Obj = purchases.getJSONObject(1);
		Assert.assertTrue(iap1Obj.getString("uniqueId").equals(entitlementID1), "incorrect entitlementID1");
		Assert.assertTrue(iap1Obj.getJSONArray("purchaseOptions").size() == 1, "incorrect number of purchaseOptions");
		JSONObject po2Obj = iap1Obj.getJSONArray("purchaseOptions").getJSONObject(0);
		Assert.assertTrue(po2Obj.getString("uniqueId").equals(purchaseOptionsID2), "incorrect purchaseOptionsID2");
		
		JSONObject iap2Obj = purchases.getJSONObject(0);
		Assert.assertTrue(iap2Obj.getString("uniqueId").equals(entitlementID2), "incorrect entitlementID2");
		Assert.assertTrue(iap2Obj.getJSONArray("purchaseOptions").size() == 3, "incorrect number of purchaseOptions");
		JSONObject po3Obj = iap2Obj.getJSONArray("purchaseOptions").getJSONObject(0);
		Assert.assertTrue(po3Obj.getString("uniqueId").equals(purchaseOptionsID3), "incorrect purchaseOptionsID3");
		JSONObject po1Obj = iap2Obj.getJSONArray("purchaseOptions").getJSONObject(1);
		Assert.assertTrue(po1Obj.getString("uniqueId").equals(purchaseOptionsID1), "incorrect purchaseOptionsID1");
		JSONObject po4Obj = iap2Obj.getJSONArray("purchaseOptions").getJSONObject(2);
		Assert.assertTrue(po4Obj.getString("uniqueId").equals(purchaseOptionsID4), "incorrect purchaseOptionsID4");
	}

	/*
	 *    IAP2-> PO1 (configmtx->cr1, cr2), PO4, PO3    IAP1 -> PO2
	 */

	@Test(dependsOnMethods="updateEntitlementsOrder", description ="update purchaseOptions order")
	public void updatePurchaseOptionsOrder() throws Exception{
		String e2 = purchasesApi.getPurchaseItemFromBranch(entitlementID2, branchID, sessionToken);
		JSONObject e2Obj = new JSONObject(e2);
		JSONArray purchaseOptionsArr = e2Obj.getJSONArray("purchaseOptions");
		
		JSONArray newPurchaseOptionsArr = new JSONArray();
		newPurchaseOptionsArr.add(purchaseOptionsArr.getJSONObject(1));
		newPurchaseOptionsArr.add(purchaseOptionsArr.getJSONObject(2));
		newPurchaseOptionsArr.add(purchaseOptionsArr.getJSONObject(0));
		
		e2Obj.put("purchaseOptions", newPurchaseOptionsArr);
		
		String response = purchasesApi.updatePurchaseItemInBranch(seasonID, branchID, entitlementID2, e2Obj.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Fail updating entitlementID2: " + response);
		
		JSONArray purchases = purchasesApi.getPurchasesBySeasonFromBranch(seasonID, branchID, sessionToken);
		Assert.assertEquals(purchases.size(), 2, "The number of purchases is incorrect. " + purchases.size());
		
		JSONObject iap1Obj = purchases.getJSONObject(1);
		Assert.assertTrue(iap1Obj.getString("uniqueId").equals(entitlementID1), "incorrect entitlementID1");
		Assert.assertTrue(iap1Obj.getJSONArray("purchaseOptions").size() == 1, "incorrect number of purchaseOptions");
		JSONObject po2Obj = iap1Obj.getJSONArray("purchaseOptions").getJSONObject(0);
		Assert.assertTrue(po2Obj.getString("uniqueId").equals(purchaseOptionsID2), "incorrect purchaseOptionsID2");
		
		JSONObject iap2Obj = purchases.getJSONObject(0);
		Assert.assertTrue(iap2Obj.getString("uniqueId").equals(entitlementID2), "incorrect entitlementID2");
		Assert.assertTrue(iap2Obj.getJSONArray("purchaseOptions").size() == 3, "incorrect number of purchaseOptions");
		JSONObject po3Obj = iap2Obj.getJSONArray("purchaseOptions").getJSONObject(0);
		Assert.assertTrue(po3Obj.getString("uniqueId").equals(purchaseOptionsID1), "incorrect purchaseOptionsID1");
		JSONObject po1Obj = iap2Obj.getJSONArray("purchaseOptions").getJSONObject(1);
		Assert.assertTrue(po1Obj.getString("uniqueId").equals(purchaseOptionsID4), "incorrect purchaseOptionsID4");
		JSONObject po4Obj = iap2Obj.getJSONArray("purchaseOptions").getJSONObject(2);
		Assert.assertTrue(po4Obj.getString("uniqueId").equals(purchaseOptionsID3), "incorrect purchaseOptionsID3");
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
