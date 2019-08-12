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

public class DeleteCheckedoutPurchaseOptionsFromMaster {
	protected String productID;
	protected String seasonID;
	private String branchID;
	private String purchaseOptionsID1;
	private String purchaseOptionsID2;
	private String configID;
	protected String filePath;
	protected String m_url;
	private String sessionToken = "";
	private AirlockUtils baseUtils;
	private BranchesRestApi br ;
	protected InAppPurchasesRestApi purchasesApi;
	private String entitlementID1;
	private String purchaseOptionsMTXID1;

	
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

	//IAP1->PO1->CR1
	@Test (description ="Add entitlements to master and add new branch") 
	public void addComponents () throws IOException, JSONException, InterruptedException {
		
		String entitlementStr = FileUtils.fileToString(filePath + "purchases/inAppPurchase1.txt", "UTF-8", false);
		entitlementID1 = purchasesApi.addPurchaseItem(seasonID, entitlementStr, "ROOT", sessionToken);;
		
		String purchaseOptionsStr = FileUtils.fileToString(filePath + "purchases/purchaseOptions1.txt", "UTF-8", false);
		purchaseOptionsID1 = purchasesApi.addPurchaseItem(seasonID, purchaseOptionsStr, entitlementID1, sessionToken);
		Assert.assertFalse(purchaseOptionsID1.contains("error"), "purchaseOptions was not added to the season: " + purchaseOptionsID1);
				
		String configuration = FileUtils.fileToString(filePath + "configuration_rule1.txt", "UTF-8", false);
		configID = purchasesApi.addPurchaseItem(seasonID, configuration, purchaseOptionsID1, sessionToken);
		Assert.assertFalse(configID.contains("error"), "Configuration1 was not added to the season: " + configID);

		String branch = FileUtils.fileToString(filePath + "experiments/branch1.txt", "UTF-8", false);
		branchID= br.createBranch(seasonID, branch, BranchesRestApi.MASTER, sessionToken);
		
		String response = br.checkoutFeature(branchID, purchaseOptionsID1, sessionToken);
		Assert.assertFalse(response.contains("error"), "purchaseOptions was not checked out to branch");
	}

	
	@Test(dependsOnMethods="addComponents", description ="Delete purchaseOptions from master")
	public void deletePurchaseOptionsFromMaster() throws Exception{
		
		int codeResponse = purchasesApi.deletePurchaseItem(purchaseOptionsID1, sessionToken);
		Assert.assertTrue(codeResponse == 200, "Master purchaseOptions was deleted");
		
		//verify that purchaseOptions is now NEW in  branch
		String response = br.getBranchWithFeatures(branchID, sessionToken);
		JSONObject brJson = new JSONObject(response);
		JSONArray entitlements = brJson.getJSONArray("entitlements");
		Assert.assertTrue(entitlements.size()==1, "Incorrect number of checked out entitlements");
		Assert.assertTrue(entitlements.getJSONObject(0).getString("branchStatus").equals("CHECKED_OUT"), "Deleted from master purchaseOptions status in branch is not NEW" );
		
		JSONArray purchaseOptionsArr = entitlements.getJSONObject(0).getJSONArray("purchaseOptions");
		Assert.assertTrue(purchaseOptionsArr.size()==1, "Incorrect number of checked out purchaseOptionsArr");
		Assert.assertTrue(purchaseOptionsArr.getJSONObject(0).getJSONArray("configurationRules").size()==1, "Incorrect number of checked out configuration rules");
		
		
		//status changes to NEW
		Assert.assertTrue(purchaseOptionsArr.getJSONObject(0).getString("branchStatus").equals("NEW"), "Deleted from master purchaseOptions status in branch is not NEW" );
		Assert.assertTrue(purchaseOptionsArr.getJSONObject(0).getJSONArray("configurationRules").getJSONObject(0).getString("branchStatus").equals("NEW"), "Deleted from master configuration rule status in branch is not NEW" );
		
		//new uniqueId is assigned
		Assert.assertFalse(purchaseOptionsArr.getJSONObject(0).getString("uniqueId").equals(purchaseOptionsID1), "Deleted from master purchaseOptions uniqueId was not changed" );
		Assert.assertFalse(purchaseOptionsArr.getJSONObject(0).getJSONArray("configurationRules").getJSONObject(0).getString("uniqueId").equals(configID), "Deleted from master configuration rule uniqueId was not changed" );

		String newEntitlementIdInBranch = purchaseOptionsArr.getJSONObject(0).getString("uniqueId");
		
		codeResponse = purchasesApi.deletePurchaseItemFromBranch(newEntitlementIdInBranch, branchID, sessionToken);
		Assert.assertTrue(codeResponse == 200, "Master purchaseOptions was deleted from branch");
		
		response = br.getBranchWithFeatures(branchID, sessionToken);
		brJson = new JSONObject(response);
		entitlements = brJson.getJSONArray("entitlements");
		Assert.assertTrue(entitlements.size()==1, "Incorrect number of checked out entitlements");	
		
		purchaseOptionsArr = entitlements.getJSONObject(0).getJSONArray("purchaseOptions");
		Assert.assertTrue(purchaseOptionsArr.size()==0, "Incorrect number of checked out purchaseOptionsArr");
	}
	
	//IAP1->MTXPO->PO1,PO2
	@Test (dependsOnMethods="deletePurchaseOptionsFromMaster", description ="Add 2 purchaseOptions and mtx to master, checkout PO2 and uncheckout OP1 and mtx") 
	public void addComponents2 () throws IOException, JSONException, InterruptedException {
		String response = br.cancelCheckoutFeature(branchID, entitlementID1, sessionToken);
		Assert.assertFalse(response.contains("error"), "entitlementID1" + response);
		
		String mtx = FileUtils.fileToString(filePath + "purchases/purchaseOptionsMutual.txt", "UTF-8", false);
        purchaseOptionsMTXID1 = purchasesApi.addPurchaseItem(seasonID, mtx, entitlementID1, sessionToken);
        Assert.assertFalse(purchaseOptionsMTXID1.contains("error"), "purchaseOptionsMTX1 was not added to the season: " + purchaseOptionsMTXID1);
		
		String purchaseOptionsStr = FileUtils.fileToString(filePath + "purchases/purchaseOptions1.txt", "UTF-8", false);
		purchaseOptionsID1 = purchasesApi.addPurchaseItem(seasonID, purchaseOptionsStr, purchaseOptionsMTXID1, sessionToken);
		Assert.assertFalse(purchaseOptionsID1.contains("error"), "purchaseOptions1 was not added to the season: " + purchaseOptionsID1);
				
		JSONObject purchaseOptionsObj = new JSONObject(purchaseOptionsStr);
		purchaseOptionsObj.put("name", "PO2");
		purchaseOptionsID2 = purchasesApi.addPurchaseItem(seasonID, purchaseOptionsObj.toString(), purchaseOptionsMTXID1, sessionToken);
		Assert.assertFalse(purchaseOptionsID2.contains("error"), "purchaseOptions2 was not added to the season: " + configID);

		response = br.checkoutFeature(branchID, purchaseOptionsID2, sessionToken);
		Assert.assertFalse(response.contains("error"), "purchaseOptions2 was not checked out to branch: " + response);
		
		response = br.cancelCheckoutFeature(branchID, purchaseOptionsID1, sessionToken);
		Assert.assertFalse(response.contains("error"), "purchaseOptions1 was not un-checked out to branch: " + response);
		
		response = br.cancelCheckoutFeature(branchID, purchaseOptionsMTXID1, sessionToken);
		Assert.assertFalse(response.contains("error"), "purchaseOptions1 was not un-checked out to branch: " + response);
	}
	
	@Test(dependsOnMethods="addComponents2", description ="Delete purchaseOptions from master")
	public void deletePurchaseOptions2FromMaster() throws Exception{
		
		int codeResponse = purchasesApi.deletePurchaseItem(purchaseOptionsID2, sessionToken);
		Assert.assertTrue(codeResponse == 200, "Master purchaseOptions2 was not deleted");
		
		//verify that purchaseOptions is now NEW in  branch
		String response = br.getBranchWithFeatures(branchID, sessionToken);
		JSONObject brJson = new JSONObject(response);
		JSONArray entitlements = brJson.getJSONArray("entitlements");
		Assert.assertTrue(entitlements.size()==2, "Incorrect number of checked out entitlements");
		Assert.assertTrue(entitlements.getJSONObject(0).getString("branchStatus").equals("CHECKED_OUT"), "Deleted from master entitlement status in branch is not NEW" );
		Assert.assertTrue(entitlements.getJSONObject(0).getString("uniqueId").equals(entitlementID1), "Deleted from master entitlement uniqueId was changed" );
		
		Assert.assertTrue(entitlements.getJSONObject(1).getString("branchStatus").equals("NEW"), "Deleted from master purchaseOptions status in branch is not NEW" );
		Assert.assertFalse(entitlements.getJSONObject(1).getString("uniqueId").equals(purchaseOptionsID2), "Deleted from master purchaseOptions uniqueId was not changed" );
		Assert.assertTrue(entitlements.getJSONObject(1).getString("name").equals("PO2"), "Deleted from master purchaseOptions uniqueId was not changed" );				
	}

	@AfterTest
	private void reset(){
		baseUtils.reset(productID, sessionToken);
	}
}
