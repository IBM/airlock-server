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

public class DeletePurchaseFromMaster {
	protected String productID;
	protected String seasonID;
	private String branchID;
	private String entitlementID1;
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

	Delete entitlement from branch
		- no delete entitlement in branch  if entitlement is from master
		 if sub-entitlement is checked-out, no delete: add new entitlement to branch. checkout entitlement from master and update its parent
		 to this new branch entitlement. delete this new branch entitlement - not allowed
		- add entitlement+config to master. check out in branch and delete from master. both entitlement and its configurations should
		 get new ids
		- checkout-out entitlement and delete from master. It should get new uniqueId in branch and status NEW
		- checkout-out entitlement with configuration rule  and delete from master. Configuration rule should get new uniqueId in branch and status NEW
		
		- add entitlement with 2 sub-entitlements to master. Check out all of them. Uncheck parent entitlement and delete from master. In branch it should get status NEW
		and sub-entitlements should get it as a parent
 */

	@Test (description ="Add entitlements to master and add new branch") 
	public void addComponents () throws IOException, JSONException, InterruptedException {
		
		String entitlement = FileUtils.fileToString(filePath + "purchases/inAppPurchase1.txt", "UTF-8", false);
		entitlementID1 = purchasesApi.addPurchaseItem(seasonID, entitlement, "ROOT", sessionToken);
		Assert.assertFalse(entitlementID1.contains("error"), "entitlement was not added to the season: " + entitlementID1);
				
		String configuration = FileUtils.fileToString(filePath + "configuration_rule1.txt", "UTF-8", false);
		configID = purchasesApi.addPurchaseItem(seasonID, configuration, entitlementID1, sessionToken);
		Assert.assertFalse(configID.contains("error"), "Configuration1 was not added to the season: " + configID);

		String branch = FileUtils.fileToString(filePath + "experiments/branch1.txt", "UTF-8", false);
		branchID= br.createBranch(seasonID, branch, BranchesRestApi.MASTER, sessionToken);
		
		String response = br.checkoutFeature(branchID, entitlementID1, sessionToken);
		Assert.assertFalse(response.contains("error"), "entitlement was not checked out to branch");
	}

	
	@Test(dependsOnMethods="addComponents", description ="Delete entitlement from branch when it was created in master - not allowed")
	public void deleteMasterEntitlementFromBranch() throws Exception{
		int codeResponse = purchasesApi.deletePurchaseItemFromBranch(entitlementID1, branchID, sessionToken);
		Assert.assertFalse(codeResponse == 200, "Master entitlement was deleted from branch");
		
		codeResponse = purchasesApi.deletePurchaseItemFromBranch(configID, branchID, sessionToken);
		Assert.assertFalse(codeResponse == 200, "Master configuration rule was deleted from branch");
	}
	

	@Test(dependsOnMethods="deleteMasterEntitlementFromBranch", description ="Delete configuration rule of checked out entitlement from  master should leave it in branch with new status")
	public void deleteMasterConfigurationRule() throws Exception{
		int codeResponse = purchasesApi.deletePurchaseItem(configID, sessionToken);
		Assert.assertTrue(codeResponse == 200, "Configuration rule was not deleted from master");
		
		String response = br.getBranchWithFeatures(branchID, sessionToken);
		JSONObject brJson = new JSONObject(response);
		JSONArray entitlements = brJson.getJSONArray("entitlements");
		Assert.assertTrue(entitlements.size()==1, "Incorrect number of checked out entitlements");
		//status changes to NEW
		Assert.assertTrue(entitlements.getJSONObject(0).getJSONArray("configurationRules").getJSONObject(0).getString("branchStatus").equals("NEW"), "Deleted from master configuration rule status in branch is not NEW" );
		//new uniqueId is assigned
		Assert.assertFalse(entitlements.getJSONObject(0).getJSONArray("configurationRules").getJSONObject(0).getString("uniqueId").equals(configID), "Deleted from master configuration rule uniqueId was not changed" );
	}
	
	@Test(dependsOnMethods="deleteMasterConfigurationRule", description ="Delete entitlement from  master should leave it in branch with new status")
	public void deleteMasterEntitlement() throws Exception{
		int codeResponse = purchasesApi.deletePurchaseItem(entitlementID1, sessionToken);
		Assert.assertTrue(codeResponse == 200, "entitlement was not deleted from master");
		
		String response = br.getBranchWithFeatures(branchID, sessionToken);
		JSONObject brJson = new JSONObject(response);
		JSONArray entitlements = brJson.getJSONArray("entitlements");
		Assert.assertTrue(entitlements.size()==1, "Incorrect number of checked out entitlements");
		//status changes to NEW
		Assert.assertTrue(entitlements.getJSONObject(0).getString("branchStatus").equals("NEW"), "Deleted from master entitlement status in branch is not NEW" );		
		//new uniqueId is assigned
		Assert.assertFalse(entitlements.getJSONObject(0).getString("uniqueId").equals(entitlementID1), "Deleted from master entitlement uniqueId was not changed" );
		
		codeResponse = purchasesApi.deletePurchaseItemFromBranch(entitlements.getJSONObject(0).getString("uniqueId"), branchID, sessionToken);
		Assert.assertTrue(codeResponse == 200, "entitlement was not deleted from branch");
	}

	@Test (dependsOnMethods="deleteMasterEntitlement", description ="Add and delete entitlement with configuration") 
	public void addDeleteEntitlementWithConfiguration () throws Exception {
		//add entitlement and configuration to master
		String entitlement = FileUtils.fileToString(filePath + "purchases/inAppPurchase1.txt", "UTF-8", false);
		entitlementID1 = purchasesApi.addPurchaseItem(seasonID, entitlement, "ROOT", sessionToken);
		Assert.assertFalse(entitlementID1.contains("error"), "entitlement was not added to the season: " + entitlementID1);
				
		String configuration = FileUtils.fileToString(filePath + "configuration_rule1.txt", "UTF-8", false);
		configID = purchasesApi.addPurchaseItem(seasonID, configuration, entitlementID1, sessionToken);
		Assert.assertFalse(configID.contains("error"), "Configuration1 was not added to the season: " + configID);

		//checkout entitlement to branch
		String response = br.checkoutFeature(branchID, entitlementID1, sessionToken);
		Assert.assertFalse(response.contains("error"), "entitlement was not checked out to branch");

		//delete entitlement from master
		int codeResponse = purchasesApi.deletePurchaseItem(entitlementID1, sessionToken);
		Assert.assertTrue(codeResponse == 200, "entitlement was not deleted from master");
		
		//validate statuses and ids of entitlement+configuration in branch
		response = br.getBranchWithFeatures(branchID, sessionToken);
		JSONObject brJson = new JSONObject(response);
		JSONArray entitlements = brJson.getJSONArray("entitlements");
		Assert.assertTrue(entitlements.size()==1, "Incorrect number of checked out entitlements");
		//entitlement status changes to NEW, new uniqueId is assigned
		Assert.assertTrue(entitlements.getJSONObject(0).getString("branchStatus").equals("NEW"), "Deleted from master entitlement status in branch is not NEW" );		
		Assert.assertFalse(entitlements.getJSONObject(0).getString("uniqueId").equals(entitlementID1), "Deleted from master entitlement uniqueId was not changed" );
		
		//configuration status changes to NEW, new uniqueId is assigned
		Assert.assertTrue(entitlements.getJSONObject(0).getJSONArray("configurationRules").getJSONObject(0).getString("branchStatus").equals("NEW"), "Deleted from master configuration status in branch is not NEW" );		
		Assert.assertFalse(entitlements.getJSONObject(0).getJSONArray("configurationRules").getJSONObject(0).getString("uniqueId").equals(configID), "Deleted from master configuration uniqueId in branch is not changed" );
	}
	
	@Test (dependsOnMethods="addDeleteEntitlementWithConfiguration", description="Delete entitlement with sub-entitlements from master")
	public void deleteEntitlementWithSubEntitlements() throws Exception{
		String entitlement = FileUtils.fileToString(filePath + "purchases/inAppPurchase1.txt", "UTF-8", false);
		JSONObject json = new JSONObject(entitlement);
		json.put("name", "masterE");
		String parentEntitlement = purchasesApi.addPurchaseItem(seasonID, json.toString(), "ROOT", sessionToken);
		Assert.assertFalse(parentEntitlement.contains("error"), "Parent entitlement was not added to the season: " + parentEntitlement);
		
		json.put("name", "child1");
		String child1 = purchasesApi.addPurchaseItem(seasonID, json.toString(), parentEntitlement, sessionToken);
		Assert.assertFalse(child1.contains("error"), "entitlement1 was not added to the season: " + child1);
		
		json.put("name", "child2");
		String child2 = purchasesApi.addPurchaseItem(seasonID, json.toString(), parentEntitlement, sessionToken);
		Assert.assertFalse(child2.contains("error"), "entitlement2 was not added to the season: " + child2);
		
		String response = br.checkoutFeature(branchID, child1, sessionToken); //checkout sub-entitlement also checks out its parent
		Assert.assertFalse(response.contains("error"), "cannot checkout");
		br.checkoutFeature(branchID, child2, sessionToken);
		Assert.assertFalse(response.contains("error"), "cannot checkout");
		br.cancelCheckoutFeature(branchID, parentEntitlement, sessionToken);
		Assert.assertFalse(response.contains("error"), "cannot cancel checkout");
		
		int code = purchasesApi.deletePurchaseItem(parentEntitlement, sessionToken);
		Assert.assertTrue(code==200, "Can't delete parent entitlement");
		
		response = br.getBranchWithFeatures(branchID, sessionToken);
		JSONObject brJson = new JSONObject(response);
		JSONArray entitlements = brJson.getJSONArray("entitlements");
		
		boolean found = false;
		for (int i=0; i<entitlements.size(); i++){
			if (entitlements.getJSONObject(i).getString("name").equals("masterE")){
				found = true;
				Assert.assertTrue(entitlements.getJSONObject(i).getString("branchStatus").equals("NEW"));
				Assert.assertFalse(entitlements.getJSONObject(i).getString("uniqueId").equals(parentEntitlement));
				JSONArray subentitlements = entitlements.getJSONObject(i).getJSONArray("entitlements");
				Assert.assertTrue(subentitlements.getJSONObject(0).getString("branchStatus").equals("NEW"), "Child1 status was not changed to NEW");
				Assert.assertTrue(subentitlements.getJSONObject(1).getString("branchStatus").equals("NEW"), "Child2 status was not changed to NEW");
			} 
		}
		
		Assert.assertTrue(found, "Parent entitlement was not found in branch");
	}
	
	
	@Test (dependsOnMethods="deleteEntitlementWithSubEntitlements", description ="Delete configuration rule") 
	public void deleteConfigurationRule () throws Exception {
		//add entitlement and 2 configurations to master
		String entitlement = FileUtils.fileToString(filePath + "purchases/inAppPurchase2.txt", "UTF-8", false);
		entitlementID1 = purchasesApi.addPurchaseItem(seasonID, entitlement, "ROOT", sessionToken);
		Assert.assertFalse(entitlementID1.contains("error"), "entitlement was not added to the season: " + entitlementID1);
				
		JSONObject configuration = new JSONObject(FileUtils.fileToString(filePath + "configuration_rule1.txt", "UTF-8", false));
		configuration.put("name", "configToDel1");
		configID = purchasesApi.addPurchaseItem(seasonID, configuration.toString(), entitlementID1, sessionToken);
		Assert.assertFalse(configID.contains("error"), "Configuration1 was not added to the season: " + configID);
		configuration.put("name", "configToDel2");
		String configID2 = purchasesApi.addPurchaseItem(seasonID, configuration.toString(), entitlementID1, sessionToken);
		Assert.assertFalse(configID2.contains("error"), "Configuration2 was not added to the season: " + configID2);

		//checkout entitlement to branch
		String response = br.checkoutFeature(branchID, entitlementID1, sessionToken);
		Assert.assertFalse(response.contains("error"), "entitlement was not checked out to branch");
		
		//delete configuration from master
		int codeResponse = purchasesApi.deletePurchaseItem(configID2, sessionToken);
		Assert.assertTrue(codeResponse == 200, "Configuration was not deleted from master");
		
		//validate statuses and ids of entitlement+configuration in branch
		JSONObject resp = new JSONObject(purchasesApi.getPurchaseItemFromBranch(entitlementID1, branchID, sessionToken));
		
		//configuration status changes to NEW, new uniqueId is assigned
		Assert.assertTrue(resp.getJSONArray("configurationRules").size()==2, "Configuration deleted from master was also deleted from branch" );
		Assert.assertTrue(resp.getJSONArray("configurationRules").getJSONObject(1).getString("branchStatus").equals("NEW"), "Deleted from master configuration status in branch is not NEW" );			
	}

	@AfterTest
	private void reset(){
		baseUtils.reset(productID, sessionToken);
	}
}
