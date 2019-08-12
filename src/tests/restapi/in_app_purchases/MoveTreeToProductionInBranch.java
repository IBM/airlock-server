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

public class MoveTreeToProductionInBranch {
	private String productID;
	private String seasonID;
	private String branchID;
	private String entitlementID1;
	private String filePath;
	private String m_url;
	private String sessionToken = "";
	private AirlockUtils baseUtils;
	private BranchesRestApi br ;
	private InAppPurchasesRestApi purchasesApi;
    
	@BeforeClass
	@Parameters({"url", "analyticsUrl", "translationsUrl", "configPath", "sessionToken", "userName", "userPassword", "appName", "productsToDeleteFile"})
	public void init(String url, String analyticsUrl, String translationsUrl, String configPath, String sToken, String userName, String userPassword, String appName, String productsToDeleteFile) throws Exception{
		m_url = url;
		filePath = configPath ;
		br = new BranchesRestApi();
		br.setURL(m_url);
		purchasesApi = new InAppPurchasesRestApi();
		purchasesApi.setURL(m_url);

		baseUtils = new AirlockUtils(m_url, analyticsUrl, translationsUrl, configPath, sToken, userName, userPassword, appName, productsToDeleteFile);
		sessionToken = baseUtils.sessionToken;
		productID = baseUtils.createProduct();
		baseUtils.printProductToFile(productID);
		seasonID = baseUtils.createSeason(productID);
	}

	/*	
		E1->E2+E3 in master. Add E4 under E1 in branch. All entitlements in production.
		Checkout E1 & E2, E3 has status NONE
		Get the whole entitlement tree from branch (including new and none), change stage to development and update
		It should fail as F3 is not checked out, is in production and can't change stage in branch
	 */
	
	@Test (description ="Add components") 
	public void addComponents () throws IOException, JSONException {
		String branch = FileUtils.fileToString(filePath + "experiments/branch1.txt", "UTF-8", false);
		JSONObject branchJson = new JSONObject(branch);
		branchJson.put("name", "branch1");
		branchID= br.createBranch(seasonID, branchJson.toString(), BranchesRestApi.MASTER, sessionToken);

		Assert.assertFalse(branchID.contains("error"), "Branch was not created: " + branchID);

		String entitlement1 = FileUtils.fileToString(filePath + "purchases/inAppPurchase1.txt", "UTF-8", false);
		JSONObject jsonE = new JSONObject(entitlement1);
		jsonE.put("stage", "PRODUCTION");
		
		jsonE.put("name", "E1");
		entitlementID1 = purchasesApi.addPurchaseItem(seasonID, jsonE.toString(), "ROOT", sessionToken);
		Assert.assertFalse(entitlementID1.contains("error"), "entitlement1 was not added: " + entitlementID1);
		
		jsonE.put("name", "E2");
		String entitlementID2 = purchasesApi.addPurchaseItem(seasonID, jsonE.toString(), entitlementID1, sessionToken);
		Assert.assertFalse(entitlementID2.contains("error"), "entitlement2 was not added: " + entitlementID2);
		
		jsonE.put("name", "F3");
		String entitlementID3 = purchasesApi.addPurchaseItem(seasonID, jsonE.toString(), entitlementID1, sessionToken);
		Assert.assertFalse(entitlementID3.contains("error"), "entitlement3 was not added: " + entitlementID3);

		br.checkoutFeature(branchID, entitlementID1, sessionToken);
		br.checkoutFeature(branchID, entitlementID2, sessionToken);
		
		jsonE.put("name", "E4");
		String entitlementID4 = purchasesApi.addPurchaseItemToBranch(seasonID, branchID, jsonE.toString(), entitlementID1, sessionToken);
		Assert.assertFalse(entitlementID4.contains("error"), "entitlement4 was not added to branch: " + entitlementID4);
	}
		
	@Test (dependsOnMethods="addComponents", description ="Move entitlement tree to production stage in branch") 
	public void updateStageInBranch () throws Exception {
		String entitlement = purchasesApi.getPurchaseItemFromBranch(entitlementID1, branchID, sessionToken);
		entitlement = entitlement.replaceAll("PRODUCTION", "DEVELOPMENT");
		String response = purchasesApi.updatePurchaseItemInBranch(seasonID, branchID, entitlementID1, entitlement, sessionToken);
		Assert.assertTrue(response.contains("cannot update an item"), "entitlement tree was incorrectly updated");
	}
	
	@AfterTest
	private void reset(){
		baseUtils.reset(productID, sessionToken);
	}
}
