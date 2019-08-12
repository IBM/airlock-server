package tests.restapi.in_app_purchases;

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
import tests.restapi.InAppPurchasesRestApi;

public class DeleteParentEntitlementFromMaster {
	protected String productID;
	protected String seasonID;
	private String branchID;
	private String entitlementID1;
	private String entitlementID2;
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
		br = new BranchesRestApi();
		br.setURL(m_url);

		baseUtils = new AirlockUtils(m_url, analyticsUrl, translationsUrl, configPath, sToken, userName, userPassword, appName, productsToDeleteFile);
		sessionToken = baseUtils.sessionToken;
		purchasesApi = new InAppPurchasesRestApi();
		purchasesApi.setURL(m_url);

		productID = baseUtils.createProduct();
		baseUtils.printProductToFile(productID);
		seasonID = baseUtils.createSeason(productID);
	}
	
	/*
		create entitlement in master, check it out in branch and then delete is from master. validate that is now new in branch
	 */


	@Test (description ="Add  E1 entitlement to master, check it out and add E2 under E1 in branch and add new branch") 
	public void addComponents () throws IOException, JSONException, InterruptedException {
		
		String entitlement = FileUtils.fileToString(filePath + "purchases/inAppPurchase1.txt", "UTF-8", false);
		entitlementID1 = purchasesApi.addPurchaseItem(seasonID, entitlement, "ROOT", sessionToken);
		Assert.assertFalse(entitlementID1.contains("error"), "entitlement was not added to the season: " + entitlementID1);
				
		String branch = FileUtils.fileToString(filePath + "experiments/branch1.txt", "UTF-8", false);
		branchID= br.createBranch(seasonID, branch, BranchesRestApi.MASTER, sessionToken);
		
		String response = br.checkoutFeature(branchID, entitlementID1, sessionToken);
		Assert.assertFalse(response.contains("error"), "entitlement was not checked out to branch");

		JSONObject entitlementObj = new JSONObject(entitlement);
		entitlementObj.put("name", "E2");
		entitlementID2 = purchasesApi.addPurchaseItemToBranch(seasonID, branchID, entitlementObj.toString(), entitlementID1, sessionToken);
		Assert.assertFalse(entitlementID2.contains("error"), "entitlement2 was not added to the season: " + entitlementID2);
	}

	
	@Test(dependsOnMethods="addComponents", description ="Uncheckout E1 entitlement from branch")
	public void unChcekoutEntitlementWithNewSubEntitlement() throws Exception{
		
		String response = br.cancelCheckoutFeature(branchID, entitlementID1, sessionToken);
		Assert.assertTrue(response.contains("error") && response.contains("Unable to cancel checkout. The item has NEW sub-items in the branch."), "entitlement was un-checked from to branch even though it gas new sub entitlement");

		int codeResponse = purchasesApi.deletePurchaseItemFromBranch(entitlementID2, branchID, sessionToken);
		Assert.assertTrue(codeResponse == 200, "entitlement2 was not deleted from branch");
	}
	
	@Test(dependsOnMethods="unChcekoutEntitlementWithNewSubEntitlement", description ="Uncheckout E1 entitlement from branch")
	public void unChcekoutEntitlementWithNewSubConfigurationRule() throws Exception{
		
		String configuration = FileUtils.fileToString(filePath + "configuration_rule1.txt", "UTF-8", false);
		String configID = purchasesApi.addPurchaseItemToBranch(seasonID, branchID, configuration, entitlementID1, sessionToken);
		Assert.assertFalse(configID.contains("error"), "Configuration1 was not added to the season: " + configID);

		String response = br.cancelCheckoutFeature(branchID, entitlementID1, sessionToken);
		Assert.assertTrue(response.contains("error") && response.contains("Unable to cancel checkout. The item has NEW sub-items in the branch."), "entitlement was un-checked from to branch even though it gas new sub entitlement");
		
		int codeResponse = purchasesApi.deletePurchaseItemFromBranch(configID, branchID, sessionToken);
		Assert.assertTrue(codeResponse == 200, "entitlement2 was not deleted from branch");	
	}

	@Test(dependsOnMethods="unChcekoutEntitlementWithNewSubConfigurationRule", description ="Uncheckout E1 entitlement from branch")
	public void unChcekoutEntitlementWithNewSubPurchaseOptions() throws Exception{
		String entitlement = FileUtils.fileToString(filePath + "purchases/inAppPurchase1.txt", "UTF-8", false);
		
		JSONObject entitlementObj = new JSONObject(entitlement);
		entitlementObj.put("name", "E2");
		entitlementID2 = purchasesApi.addPurchaseItem(seasonID, entitlementObj.toString(), entitlementID1, sessionToken);
		Assert.assertFalse(entitlementID2.contains("error"), "entitlement2 was not added to the season: " + entitlementID2);
		
		JSONObject purOptJson = new JSONObject(FileUtils.fileToString(filePath + "purchases/purchaseOptions1.txt", "UTF-8", false));
		purOptJson.put("name", "PO1");
		String purchaseOptionsID1 = purchasesApi.addPurchaseItemToBranch(seasonID, branchID, purOptJson.toString(), entitlementID1, sessionToken);
		Assert.assertFalse(purchaseOptionsID1.contains("error"), "purchaseOptions was not added to the season: " + purchaseOptionsID1);
		
		String response = br.cancelCheckoutFeature(branchID, entitlementID1, sessionToken);
		Assert.assertTrue(response.contains("error") && response.contains("Unable to cancel checkout. The item has NEW sub-items in the branch."), "entitlement was un-checked from to branch even though it gas new sub entitlement");
	}

	@AfterTest
	private void reset(){
		//baseUtils.reset(productID, sessionToken);
	}
}
