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


public class DeletePuchaseOptionsFromBranch {
	protected String productID;
	protected String seasonID;
	private String branchID;
	private String entitlementID1;
	private String purchaseOptionsID1;
	private String purchaseOptionsID2;
	private String mixConfigID;
	private String configID1;
	private String configID2;
	protected String filePath;
	protected String m_url;
	private String sessionToken = "";
	private AirlockUtils baseUtils;
	private BranchesRestApi br ;
	private JSONObject entitlement;
	private JSONObject purOptJson;
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
		purOptJson = new JSONObject(FileUtils.fileToString(filePath + "purchases/purchaseOptions1.txt", "UTF-8", false));
	}
	
	/*
	 * IAP1->PO1->mixConfig->CR1, CR2
	 * add entitlement + purchaseOptions + configs to branch and delete - allowed
 */

	@Test (description ="Add purchaseOptions with configurations to branch") 
	public void addComponents () throws IOException, JSONException, InterruptedException {
		String branch = FileUtils.fileToString(filePath + "experiments/branch1.txt", "UTF-8", false);
		branchID= br.createBranch(seasonID, branch, BranchesRestApi.MASTER, sessionToken);

		entitlement.put("name", "IAP1");
		entitlementID1 = purchasesApi.addPurchaseItem(seasonID, entitlement.toString(), "ROOT", sessionToken);
		Assert.assertFalse(entitlementID1.contains("error"), "entitlement was not added to the season: " + entitlementID1);
		
		String response = br.checkoutFeature(branchID, entitlementID1, sessionToken);
		Assert.assertFalse(response.contains("error"), "cannot check out entitlement1: " + response);
				
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
		Assert.assertFalse(configID2.contains("error"), "Configuration rule1 was not added to the season");
	}
	
	@Test(dependsOnMethods="addComponents", description ="Delete configuration rule")
	public void deleteConfiguration() throws Exception{
		
		int codeResponse = purchasesApi.deletePurchaseItemFromBranch(configID2, branchID, sessionToken);
		Assert.assertTrue(codeResponse == 200, "Configuraton was not deleted from branch");
		
		String response = br.getBranchWithFeatures(branchID, sessionToken);
		JSONObject brJson = new JSONObject(response);
		JSONArray entitlements = brJson.getJSONArray("entitlements");
		JSONArray purchaseOptionsArr = entitlements.getJSONObject(0).getJSONArray("purchaseOptions");
		Assert.assertTrue(purchaseOptionsArr.getJSONObject(0).getJSONArray("configurationRules").getJSONObject(0).getJSONArray("configurationRules").size() == 1, "Configuration rule was not deleted");
	}
	
	@Test(dependsOnMethods="deleteConfiguration", description ="Delete mix of configuration rules")
	public void deleteConfigurationMix() throws Exception{
		
		int codeResponse = purchasesApi.deletePurchaseItemFromBranch(mixConfigID, branchID, sessionToken);
		Assert.assertTrue(codeResponse == 200, "Configuraton mix was not deleted from branch");
		
		String response = br.getBranchWithFeatures(branchID, sessionToken);
		JSONObject brJson = new JSONObject(response);
		JSONArray entitlements = brJson.getJSONArray("entitlements");
		JSONArray purchaseOptionsArr = entitlements.getJSONObject(0).getJSONArray("purchaseOptions");
		
		Assert.assertTrue(purchaseOptionsArr.getJSONObject(0).getJSONArray("configurationRules").size() == 0, "Configuration rule was not deleted");
		
		response = purchasesApi.getPurchaseItem(configID2, sessionToken);
		Assert.assertTrue(response.contains("error"), "Second configuration exists in master");
		
		response = purchasesApi.getPurchaseItemFromBranch(configID2, branchID, sessionToken);
		Assert.assertTrue(response.contains("error"), "Second configuration was not deleted from branch");
	}
	
	@Test(dependsOnMethods="deleteConfigurationMix", description ="Delete purchaseOptions with configurations")
	public void deletePurchaseOptions() throws Exception{
		//configurations were deleted by previous tests, add them again
		String configurationMix = FileUtils.fileToString(filePath + "configuration_feature-mutual.txt", "UTF-8", false);
		mixConfigID =  purchasesApi.addPurchaseItemToBranch(seasonID, branchID, configurationMix, purchaseOptionsID1, sessionToken);
		Assert.assertFalse(mixConfigID.contains("error"), "Configuration mix was not added to the season: " + mixConfigID);

		String configuration1 = FileUtils.fileToString(filePath + "configuration_rule1.txt", "UTF-8", false);
		configID1 =  purchasesApi.addPurchaseItemToBranch(seasonID, branchID, configuration1, mixConfigID, sessionToken);
		Assert.assertFalse(configID1.contains("error"), "Configuration rule1 was not added to the season: " + configID1);
				
		String configuration2 = FileUtils.fileToString(filePath + "configuration_rule2.txt", "UTF-8", false);
		configID2 =  purchasesApi.addPurchaseItemToBranch(seasonID, branchID, configuration2, mixConfigID, sessionToken);
		Assert.assertFalse(configID2.contains("error"), "Configuration rule1 was not added to the season: " + configID2);

		//delete purchaseOptions
		int codeResponse = purchasesApi.deletePurchaseItemFromBranch(purchaseOptionsID1, branchID, sessionToken);
		Assert.assertTrue(codeResponse == 200, "purchaseOptions was not deleted from branch");
		
		String response = br.getBranchWithFeatures(branchID, sessionToken);
		JSONObject brJson = new JSONObject(response);
		JSONArray entitlements = brJson.getJSONArray("entitlements");
		JSONArray purchaseOptionsArr = entitlements.getJSONObject(0).getJSONArray("purchaseOptions");
		
		Assert.assertTrue(purchaseOptionsArr.size() == 0, "purchaseOptions was not deleted");
		
		response = purchasesApi.getPurchaseItem(configID1, sessionToken);
		Assert.assertTrue(response.contains("error"), "Configuration1 exists in master");
		response = purchasesApi.getPurchaseItem(configID2, sessionToken);
		Assert.assertTrue(response.contains("error"), "Configuration2 exists in master");
		response = purchasesApi.getPurchaseItem(mixConfigID, sessionToken);
		Assert.assertTrue(response.contains("error"), "Configuration mix exists in master");

		response = purchasesApi.getPurchaseItemFromBranch(configID1, branchID, sessionToken);
		Assert.assertTrue(response.contains("error"), "Configuration1 was not deleted  from branch");
		response = purchasesApi.getPurchaseItemFromBranch(configID2, branchID, sessionToken);
		Assert.assertTrue(response.contains("error"), "Configuration2 was not deleted  from branch");
		response = purchasesApi.getPurchaseItemFromBranch(mixConfigID, branchID, sessionToken);
		Assert.assertTrue(response.contains("error"), "Configuration mix was not deleted  from branch");
	}
	
	@Test (dependsOnMethods="deletePurchaseOptions", description="Checked out purchaseOptions can't be deleted from branch")
	public void deleteCheckedOutPurchaseOptionsFromBranch() throws IOException, JSONException{
		
		String response = br.cancelCheckoutFeature(branchID, entitlementID1, sessionToken);
		Assert.assertFalse(response.contains("error"), "cannot cancel check out entitlement1: " + response);
		
		purOptJson.put("name", "PO2");
		purchaseOptionsID2 =  purchasesApi.addPurchaseItem(seasonID, purOptJson.toString(), entitlementID1, sessionToken);
		Assert.assertFalse(purchaseOptionsID2.contains("error"), "purchaseOptions was not added to the season: " + purchaseOptionsID2);
			
		response = br.checkoutFeature(branchID, purchaseOptionsID2, sessionToken);
		Assert.assertFalse(response.contains("error"), "cannot check out purchaseOptions: " + response);
		
		int code = purchasesApi.deletePurchaseItemFromBranch(purchaseOptionsID2, branchID, sessionToken);
		Assert.assertTrue(code != 200, "Deleted checked out purchaseOptions from branch");
	}
	
	@Test (dependsOnMethods="deleteCheckedOutPurchaseOptionsFromBranch", description="purchaseOptions with status NONE can't be deleted from branch")
	public void deleteNonePurchaseOptionsFromBranch() throws IOException{
		
		String res = br.cancelCheckoutFeature(branchID, purchaseOptionsID2, sessionToken);
		Assert.assertFalse(res.contains("error"), "cannot cancel checkout");
		
		int resp = purchasesApi.deletePurchaseItemFromBranch(purchaseOptionsID2, branchID, sessionToken);
		Assert.assertTrue(resp != 200, "Deleted NONE purchaseOptions from branch");

	}
	
	//Validate that new entitlement in barnch cannot be deleted if it has chceked_out children.
	@Test (dependsOnMethods="deleteNonePurchaseOptionsFromBranch", description="Checked out subpurchaseOptions can't be deleted from branch")
	public void deleteSubentitlementFromBranch() throws IOException, JSONException{
		
		entitlement.put("name", "IAP2");
		String entitlementID2 = purchasesApi.addPurchaseItemToBranch(seasonID, branchID, entitlement.toString(), "ROOT", sessionToken);
		Assert.assertFalse(entitlementID2.contains("error"), "entitlement was not added to the season: " + entitlementID1);
		
		br.checkoutFeature(branchID, purchaseOptionsID2, sessionToken);
		
		JSONObject entitlement2 = new JSONObject(purchasesApi.getPurchaseItemFromBranch(entitlementID2, branchID, sessionToken));
		JSONObject purchaseOptions2 = new JSONObject(purchasesApi.getPurchaseItemFromBranch(purchaseOptionsID2, branchID, sessionToken));
		JSONArray children = new JSONArray();
		children.add(purchaseOptions2);
		entitlement2.put("purchaseOptions", children);
		String response = purchasesApi.updatePurchaseItemInBranch(seasonID, branchID, entitlementID2, entitlement2.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Can't move sub purchaseOptions to new entitlement in branch: " + response);
		int resp = purchasesApi.deletePurchaseItemFromBranch(entitlementID2, branchID, sessionToken);
		Assert.assertTrue(resp != 200, "New entitlement in branch with sub purchaseOptions in status chceked_out was deleted from branch");
	}
	
	@AfterTest
	private void reset(){
		baseUtils.reset(productID, sessionToken);
	}
}
