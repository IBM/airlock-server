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

public class DeleteCheckedoutEntitlementFromMaster {
	protected String productID;
	protected String seasonID;
	private String branchID;
	private String entitlementID1;
	private String entitlementID2;
	private String configID;
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

	//IAP1->CR1
	@Test (description ="Add entitlements to master and add new branch") 
	public void addComponents () throws IOException, JSONException, InterruptedException {
		
		String entitlementStr = FileUtils.fileToString(filePath + "purchases/inAppPurchase1.txt", "UTF-8", false);
		entitlementID1 = purchasesApi.addPurchaseItem(seasonID, entitlementStr, "ROOT", sessionToken);
		Assert.assertFalse(entitlementID1.contains("error"), "entitlement was not added to the season: " + entitlementID1);
				
		String configuration = FileUtils.fileToString(filePath + "configuration_rule1.txt", "UTF-8", false);
		configID = purchasesApi.addPurchaseItem(seasonID, configuration, entitlementID1, sessionToken);
		Assert.assertFalse(configID.contains("error"), "Configuration1 was not added to the season: " + configID);

		String branch = FileUtils.fileToString(filePath + "experiments/branch1.txt", "UTF-8", false);
		branchID= br.createBranch(seasonID, branch, BranchesRestApi.MASTER, sessionToken);
		
		String response = br.checkoutFeature(branchID, entitlementID1, sessionToken);
		Assert.assertFalse(response.contains("error"), "entitlement was not checked out to branch");
	}
	
	@Test(dependsOnMethods="addComponents", description ="Delete entitlement from master")
	public void deleteEntitlementFromMaster() throws Exception{
		
		int codeResponse = purchasesApi.deletePurchaseItem(entitlementID1, sessionToken);
		Assert.assertTrue(codeResponse == 200, "Master entitlement was deleted");
		
		//verify that entitlement is now NEW in  branch
		String response = br.getBranchWithFeatures(branchID, sessionToken);
		JSONObject brJson = new JSONObject(response);
		JSONArray entitlements = brJson.getJSONArray("entitlements");
		Assert.assertTrue(entitlements.size()==1, "Incorrect number of checked out entitlements");
		Assert.assertTrue(entitlements.getJSONObject(0).getJSONArray("configurationRules").size()==1, "Incorrect number of checked out configuration rules");
		
		
		//status changes to NEW
		Assert.assertTrue(entitlements.getJSONObject(0).getString("branchStatus").equals("NEW"), "Deleted from master entitlement status in branch is not NEW" );
		Assert.assertTrue(entitlements.getJSONObject(0).getJSONArray("configurationRules").getJSONObject(0).getString("branchStatus").equals("NEW"), "Deleted from master configuration rule status in branch is not NEW" );
		//new uniqueId is assigned
		Assert.assertFalse(entitlements.getJSONObject(0).getString("uniqueId").equals(entitlementID1), "Deleted from master entitlement uniqueId was not changed" );
		Assert.assertFalse(entitlements.getJSONObject(0).getJSONArray("configurationRules").getJSONObject(0).getString("uniqueId").equals(configID), "Deleted from master configuration rule uniqueId was not changed" );

		String newEntitlementIdInBranch = entitlements.getJSONObject(0).getString("uniqueId");
		
		codeResponse = purchasesApi.deletePurchaseItemFromBranch(newEntitlementIdInBranch, branchID, sessionToken);
		Assert.assertTrue(codeResponse == 200, "Master entitlement was deleted from branch");
		
		response = br.getBranchWithFeatures(branchID, sessionToken);
		brJson = new JSONObject(response);
		entitlements = brJson.getJSONArray("entitlements");
		Assert.assertTrue(entitlements.size()==0, "Incorrect number of checked out entitlements");	
	}
	
	//IAP1->IAP2
	@Test (dependsOnMethods="deleteEntitlementFromMaster", description ="Add 2 entitlements to master, checkout IAP2 and uncheckout IAP1") 
	public void addComponents2 () throws IOException, JSONException, InterruptedException {
		
		String entitlementStr = FileUtils.fileToString(filePath + "purchases/inAppPurchase1.txt", "UTF-8", false);
		entitlementID1 = purchasesApi.addPurchaseItem(seasonID, entitlementStr, "ROOT", sessionToken);
		Assert.assertFalse(entitlementID1.contains("error"), "entitlement1 was not added to the season: " + entitlementID1);
				
		JSONObject entitlementObj = new JSONObject(entitlementStr);
		entitlementObj.put("name", "IAP2");
		entitlementID2 = purchasesApi.addPurchaseItem(seasonID, entitlementObj.toString(), entitlementID1, sessionToken);
		Assert.assertFalse(configID.contains("error"), "entitlement2 was not added to the season: " + configID);

		String response = br.checkoutFeature(branchID, entitlementID2, sessionToken);
		Assert.assertFalse(response.contains("error"), "entitlement2 was not checked out to branch");
		
		response = br.cancelCheckoutFeature(branchID, entitlementID1, sessionToken);
		Assert.assertFalse(response.contains("error"), "entitlement1 was not un-checked out to branch");
	}
	
	@Test(dependsOnMethods="addComponents2", description ="Delete entitlement from master")
	public void deleteEntitlement2FromMaster() throws Exception{
		
		int codeResponse = purchasesApi.deletePurchaseItem(entitlementID2, sessionToken);
		Assert.assertTrue(codeResponse == 200, "Master entitlement2 was not deleted");
		
		//verify that entitlement is now NEW in  branch
		String response = br.getBranchWithFeatures(branchID, sessionToken);
		JSONObject brJson = new JSONObject(response);
		JSONArray entitlements = brJson.getJSONArray("entitlements");
		Assert.assertTrue(entitlements.size()==1, "Incorrect number of checked out entitlements");
		Assert.assertTrue(entitlements.getJSONObject(0).getString("branchStatus").equals("NEW"), "Deleted from master entitlement status in branch is not NEW" );
		Assert.assertFalse(entitlements.getJSONObject(0).getString("uniqueId").equals(entitlementID2), "Deleted from master entitlement uniqueId was not changed" );
		Assert.assertTrue(entitlements.getJSONObject(0).getString("name").equals("IAP2"), "Deleted from master entitlement uniqueId was not changed" );		
	}

	@AfterTest
	private void reset(){
		baseUtils.reset(productID, sessionToken);
	}
}
