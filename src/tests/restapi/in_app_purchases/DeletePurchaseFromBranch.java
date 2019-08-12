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


public class DeletePurchaseFromBranch {
	protected String productID;
	protected String seasonID;
	private String branchID;
	private String entitlementID1;
	private String entitlementID2;
	private String mixConfigID;
	private String configID1;
	private String configID2;
	protected String filePath;
	protected String m_url;
	private String sessionToken = "";
	private AirlockUtils baseUtils;
	private BranchesRestApi br ;
	private JSONObject entitlement;
	protected InAppPurchasesRestApi purchasesApi;
	

	
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
		entitlement = new JSONObject(FileUtils.fileToString(filePath + "purchases/inAppPurchase1.txt", "UTF-8", false));
	}
	
	/*
	 * add entitlement + configs to branch and delete - allowed
 */

	@Test (description ="Add entitlement with configurations to branch") 
	public void addComponents () throws IOException, JSONException, InterruptedException {
		String branch = FileUtils.fileToString(filePath + "experiments/branch1.txt", "UTF-8", false);
		branchID= br.createBranch(seasonID, branch, BranchesRestApi.MASTER, sessionToken);

		entitlement.put("name", "IAP1");
		entitlementID1 = purchasesApi.addPurchaseItemToBranch(seasonID, branchID, entitlement.toString(), "ROOT", sessionToken);
		Assert.assertFalse(entitlementID1.contains("error"), "entitlement was not added to the season: " + entitlementID1);
				
		String configurationMix = FileUtils.fileToString(filePath + "configuration_feature-mutual.txt", "UTF-8", false);
		mixConfigID = purchasesApi.addPurchaseItemToBranch(seasonID, branchID, configurationMix, entitlementID1, sessionToken);
		Assert.assertFalse(mixConfigID.contains("error"), "Configuration mix was not added to the season");

		String configuration1 = FileUtils.fileToString(filePath + "configuration_rule1.txt", "UTF-8", false);
		configID1 = purchasesApi.addPurchaseItemToBranch(seasonID, branchID, configuration1, mixConfigID, sessionToken);
		Assert.assertFalse(configID1.contains("error"), "Configuration rule1 was not added to the season");
				
		String configuration2 = FileUtils.fileToString(filePath + "configuration_rule2.txt", "UTF-8", false);
		configID2 = purchasesApi.addPurchaseItemToBranch(seasonID, branchID, configuration2, mixConfigID, sessionToken);
		Assert.assertFalse(configID2.contains("error"), "Configuration rule1 was not added to the season");
	}
	
	@Test(dependsOnMethods="addComponents", description ="Delete configuration rule")
	public void deleteConfiguration() throws Exception{
		
		int codeResponse = purchasesApi.deletePurchaseItemFromBranch(configID2, branchID, sessionToken);
		Assert.assertTrue(codeResponse == 200, "Configuraton was not deleted from branch");
		
		String response = br.getBranchWithFeatures(branchID, sessionToken);
		JSONObject brJson = new JSONObject(response);
		JSONArray entitlements = brJson.getJSONArray("entitlements");

		Assert.assertTrue(entitlements.getJSONObject(0).getJSONArray("configurationRules").getJSONObject(0).getJSONArray("configurationRules").size() == 1, "Configuration rule was not deleted");
	}
	
	@Test(dependsOnMethods="deleteConfiguration", description ="Delete mix of configuration rules")
	public void deleteConfigurationMix() throws Exception{
		
		int codeResponse = purchasesApi.deletePurchaseItemFromBranch(mixConfigID, branchID, sessionToken);
		Assert.assertTrue(codeResponse == 200, "Configuraton mix was not deleted from branch");
		
		String response = br.getBranchWithFeatures(branchID, sessionToken);
		JSONObject brJson = new JSONObject(response);
		JSONArray entitlements = brJson.getJSONArray("entitlements");

		Assert.assertTrue(entitlements.getJSONObject(0).getJSONArray("configurationRules").size() == 0, "Configuration rule was not deleted");
		
		response = purchasesApi.getPurchaseItem(configID2, sessionToken);
		Assert.assertTrue(response.contains("error"), "Second configuration was not deleted from branch");
	}
	
	@Test(dependsOnMethods="deleteConfigurationMix", description ="Delete entitlement with configurations")
	public void deleteEntitlement() throws Exception{
		//configurations were deleted by previous tests, add them again
		String configurationMix = FileUtils.fileToString(filePath + "configuration_feature-mutual.txt", "UTF-8", false);
		mixConfigID =  purchasesApi.addPurchaseItemToBranch(seasonID, branchID, configurationMix, entitlementID1, sessionToken);
		Assert.assertFalse(mixConfigID.contains("error"), "Configuration mix was not added to the season: " + mixConfigID);

		String configuration1 = FileUtils.fileToString(filePath + "configuration_rule1.txt", "UTF-8", false);
		configID1 =  purchasesApi.addPurchaseItemToBranch(seasonID, branchID, configuration1, mixConfigID, sessionToken);
		Assert.assertFalse(configID1.contains("error"), "Configuration rule1 was not added to the season: " + configID1);
				
		String configuration2 = FileUtils.fileToString(filePath + "configuration_rule2.txt", "UTF-8", false);
		configID2 =  purchasesApi.addPurchaseItemToBranch(seasonID, branchID, configuration2, mixConfigID, sessionToken);
		Assert.assertFalse(configID2.contains("error"), "Configuration rule1 was not added to the season: " + configID2);

		//delete entitlement
		int codeResponse = purchasesApi.deletePurchaseItemFromBranch(entitlementID1, branchID, sessionToken);
		Assert.assertTrue(codeResponse == 200, "entitlement was not deleted from branch");
		
		String response = br.getBranchWithFeatures(branchID, sessionToken);
		JSONObject brJson = new JSONObject(response);
		JSONArray entitlements = brJson.getJSONArray("entitlements");

		Assert.assertTrue(entitlements.size() == 0, "entitlement was not deleted");
		
		response = purchasesApi.getPurchaseItem(configID1, sessionToken);
		Assert.assertTrue(response.contains("error"), "Configuration1 was not deleted  from branch");
		response = purchasesApi.getPurchaseItem(configID2, sessionToken);
		Assert.assertTrue(response.contains("error"), "Configuration2 was not deleted  from branch");
		response = purchasesApi.getPurchaseItem(mixConfigID, sessionToken);
		Assert.assertTrue(response.contains("error"), "Configuration mix was not deleted  from branch");

	}
	
	@Test (dependsOnMethods="deleteEntitlement", description="Checked out entitlement can't be deleted from branch")
	public void deleteCheckedOutEntitlementFromBranch() throws IOException, JSONException{
		entitlement.put("name", "IAP2");
		entitlementID2 =  purchasesApi.addPurchaseItem(seasonID, entitlement.toString(), "ROOT", sessionToken);
		Assert.assertFalse(entitlementID2.contains("error"), "entitlement was not added to the season: " + entitlementID2);
		
		String respons = br.checkoutFeature(branchID, entitlementID2, sessionToken);
		Assert.assertFalse(respons.contains("error"), "cannot check out entitlement");
		
		int response = purchasesApi.deletePurchaseItemFromBranch(entitlementID2, branchID, sessionToken);
		Assert.assertTrue(response != 200, "Deleted checked out entitlement from branch");
	}
	
	@Test (dependsOnMethods="deleteCheckedOutEntitlementFromBranch", description="entitlement with status NONE can't be deleted from branch")
	public void deleteNoneEntitlementFromBranch() throws IOException{
		
		String res = br.cancelCheckoutFeature(branchID, entitlementID2, sessionToken);
		Assert.assertFalse(res.contains("error"), "cannot checkout");
		
		int resp = purchasesApi.deletePurchaseItemFromBranch(entitlementID2, branchID, sessionToken);
		Assert.assertTrue(resp != 200, "Deleted NONE entitlement from branch");

	}
	
	//Validate that new entitlement in barnch cannot be deleted if it has chceked_out children.
	@Test (dependsOnMethods="deleteNoneEntitlementFromBranch", description="Checked out subentitlement can't be deleted from branch")
	public void deleteSubentitlementFromBranch() throws IOException, JSONException{
		entitlement.put("name", "IAP3");
		String entitlementID3 = purchasesApi.addPurchaseItemToBranch(seasonID, branchID, entitlement.toString(), "ROOT", sessionToken);
		Assert.assertFalse(entitlementID3.contains("error"), "entitlement was not added to the season: " + entitlementID3);
		
		br.checkoutFeature(branchID, entitlementID2, sessionToken);
		
		JSONObject entitlement3 = new JSONObject(purchasesApi.getPurchaseItemFromBranch(entitlementID3, branchID, sessionToken));
		JSONObject entitlement2 = new JSONObject(purchasesApi.getPurchaseItemFromBranch(entitlementID2, branchID, sessionToken));
		JSONArray children = new JSONArray();
		children.add(entitlement2);
		entitlement3.put("entitlements", children);
		String response = purchasesApi.updatePurchaseItemInBranch(seasonID, branchID, entitlementID3, entitlement3.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Can't move subentitlement to new entitlement in branch: " + response);
		int resp = purchasesApi.deletePurchaseItemFromBranch(entitlementID3, branchID, sessionToken);
		Assert.assertTrue(resp != 200, "New entitlement in branch with subentitlement in status NONE was deleted from branch");

	}
	


	@AfterTest
	private void reset(){
		baseUtils.reset(productID, sessionToken);
	}
}
