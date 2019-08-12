package tests.restapi.copy_import.copy_purchases;

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
import tests.restapi.FeaturesRestApi;
import tests.restapi.InAppPurchasesRestApi;
import tests.restapi.ProductsRestApi;

public class CopyEntitlementSameSeason {
	private String seasonID;
	private String productID;
	private String entitlementID1;
	private String entitlementID2;
	private String configID;
	private String filePath;
	private String m_url;
	private ProductsRestApi p;
	private FeaturesRestApi f;
	private String sessionToken = "";
	private AirlockUtils baseUtils;
	private String srcBranchID;
	private InAppPurchasesRestApi purchasesApi;	
	
	@BeforeClass
	@Parameters({"url", "analyticsUrl", "translationsUrl", "configPath", "sessionToken", "userName", "userPassword", "appName", "productsToDeleteFile", "runOnMaster"})
	public void init(String url, String analyticsUrl, String translationsUrl, String configPath, String sToken, String userName, String userPassword, String appName, String productsToDeleteFile, Boolean runOnMaster) throws Exception{
		m_url = url;
		filePath = configPath;
		p = new ProductsRestApi();
		p.setURL(m_url);
		f = new FeaturesRestApi();
		f.setURL(m_url);
		purchasesApi = new InAppPurchasesRestApi();
		purchasesApi.setURL(m_url);
	    
		baseUtils = new AirlockUtils(url, analyticsUrl, translationsUrl, configPath, sToken, userName, userPassword, appName, productsToDeleteFile);
		sessionToken = baseUtils.sessionToken;
		productID = baseUtils.createProduct();
		baseUtils.printProductToFile(productID);
		seasonID = baseUtils.createSeason(productID);
		try {
			if (runOnMaster) {
				srcBranchID = BranchesRestApi.MASTER;
			} else {
				srcBranchID = baseUtils.createBranchInExperiment(analyticsUrl);
			}
		}catch (Exception e){
			srcBranchID = null;
		}
	}
	
	/*
	  	Entitlement under entitlement - allowed
		Entitlement under mix of entitlements - allowed
		Entitlement under root - allowed
		Entitlement under config - not allowed
		Entitlement under mix config - not allowed
		Entitlement under itself - allowed
	 */
	
	@Test (description="Copy single entitlement under another entitlement in the same season. First, copy without namesuffix, then copy with namesuffix")
	public void copySingleEntitlementUnderEntitlement() throws IOException, JSONException{
		String entitlement1 = FileUtils.fileToString(filePath + "purchases/inAppPurchase1.txt", "UTF-8", false);
		entitlementID1 = purchasesApi.addPurchaseItemToBranch(seasonID, srcBranchID, entitlement1, "ROOT", sessionToken);
		Assert.assertFalse(entitlementID1.contains("error"), "entitlement was not added to the season");
		
		String entitlement2 = FileUtils.fileToString(filePath + "purchases/inAppPurchase2.txt", "UTF-8", false);
		entitlementID2 = purchasesApi.addPurchaseItemToBranch(seasonID, srcBranchID, entitlement2, "ROOT", sessionToken);
		Assert.assertFalse(entitlementID2.contains("error"), "entitlement was not added to the season");
		
		//should fail copy without suffix
		String response = f.copyItemBetweenBranches(entitlementID1, entitlementID2, "ACT", null, null, sessionToken, srcBranchID, srcBranchID);
		Assert.assertTrue(response.contains("illegalName"), "entitlement was copied with existing name ");
		
		response = f.copyItemBetweenBranches(entitlementID1, entitlementID2, "ACT", null, "suffix1", sessionToken, srcBranchID, srcBranchID);
		Assert.assertTrue(response.contains("newSubTreeId"), "entitlement was not copied: " + response);
		
		JSONObject result = new JSONObject(response);
		String newEntitlement = purchasesApi.getPurchaseItemFromBranch(result.getString("newSubTreeId"), srcBranchID, sessionToken);
		String oldEntitlement = purchasesApi.getPurchaseItemFromBranch(entitlementID1, srcBranchID, sessionToken);
		Assert.assertTrue(f.jsonObjsAreEqual(new JSONObject(newEntitlement), new JSONObject(oldEntitlement)));
	}
	
	@Test (dependsOnMethods="copySingleEntitlementUnderEntitlement", description="Copy single entitlement under mix entitlement in the same season. First, copy without namesuffix, then copy with namesuffix")
	public void copySingleEntitlementUnderMixEntitlement() throws IOException, JSONException{
		String entitlement = FileUtils.fileToString(filePath + "purchases/inAppPurchaseMutual.txt", "UTF-8", false);
		String mixId = purchasesApi.addPurchaseItemToBranch(seasonID, srcBranchID, entitlement, entitlementID2, sessionToken);
		Assert.assertFalse(mixId.contains("error"), "Entitlement mix was not added to the season" + mixId);
				
		//should fail copy without suffix
		String response = f.copyItemBetweenBranches(entitlementID1, mixId, "ACT", null, null, sessionToken, srcBranchID, srcBranchID);
		Assert.assertTrue(response.contains("illegalName"), "Entitlement was copied with existing name ");

		response = f.copyItemBetweenBranches(entitlementID1, mixId, "ACT", null, "suffix2", sessionToken, srcBranchID, srcBranchID);
		Assert.assertTrue(response.contains("newSubTreeId"), "Entitlement was not copied: " + response);
		
		JSONObject result = new JSONObject(response);
		String newEntitlement = purchasesApi.getPurchaseItemFromBranch(result.getString("newSubTreeId"), srcBranchID, sessionToken);
		String oldEntitlement = purchasesApi.getPurchaseItemFromBranch(entitlementID1, srcBranchID, sessionToken);
		Assert.assertTrue(f.jsonObjsAreEqual(new JSONObject(newEntitlement), new JSONObject(oldEntitlement)));
	}
	
	@Test (dependsOnMethods="copySingleEntitlementUnderMixEntitlement", description="Copy single entitlement under root in the same season. First, copy without namesuffix, then copy with namesuffix")
	public void copySingleEntitlementUnderRoot() throws IOException, JSONException{
		String rootId = purchasesApi.getBranchRootId(seasonID, "MASTER", sessionToken);
		//should fail copy without suffix
		String response = f.copyItemBetweenBranches(entitlementID1, rootId, "ACT", null, null, sessionToken, srcBranchID, srcBranchID);
		Assert.assertTrue(response.contains("illegalName"), "Entitlement was copied with existing name ");
		
		response = f.copyItemBetweenBranches(entitlementID1, rootId, "ACT", null, "suffix3", sessionToken, srcBranchID, srcBranchID);
		Assert.assertTrue(response.contains("newSubTreeId"), "Entitlement was not copied: " + response);
		
		JSONObject result = new JSONObject(response);
		String newEntitlement = purchasesApi.getPurchaseItemFromBranch(result.getString("newSubTreeId"), srcBranchID, sessionToken);
		String oldEntitlement = purchasesApi.getPurchaseItemFromBranch(entitlementID1, srcBranchID, sessionToken);
		Assert.assertTrue(f.jsonObjsAreEqual(new JSONObject(newEntitlement), new JSONObject(oldEntitlement)));
	}
	
	@Test (dependsOnMethods="copySingleEntitlementUnderRoot", description="Copy single entitlement under configuration in the same season. First, copy without namesuffix, then copy with namesuffix")
	public void copySingleEntitlementUnderConfiguration() throws IOException{
		String configuration = FileUtils.fileToString(filePath + "configuration_rule1.txt", "UTF-8", false);
		configID = purchasesApi.addPurchaseItemToBranch(seasonID, srcBranchID, configuration, entitlementID2, sessionToken);
		Assert.assertFalse(configID.contains("error"), "Entitlement was not added to the season");
				
		//should fail copy without suffix
		String response = f.copyItemBetweenBranches(entitlementID1, configID, "ACT", null, null, sessionToken, srcBranchID, srcBranchID);
		Assert.assertTrue(response.contains("illegalName"), "Entitlement was copied with existing name ");
		
		response = f.copyItemBetweenBranches(entitlementID1, configID, "ACT", null, "suffix4", sessionToken, srcBranchID, srcBranchID);
		Assert.assertTrue(response.contains("error"), "Entitlement was copied under configuration " + response);
	}
	
	@Test (dependsOnMethods="copySingleEntitlementUnderConfiguration", description="Copy single entitlement under mix configuration in the same season. First, copy without namesuffix, then copy with namesuffix")
	public void copySingleEntitlementUnderMixConfiguration() throws IOException{
		String configuration = FileUtils.fileToString(filePath + "configuration_feature-mutual.txt", "UTF-8", false);
		String mixConfigID = purchasesApi.addPurchaseItemToBranch(seasonID, srcBranchID, configuration, entitlementID2, sessionToken);
		Assert.assertFalse(mixConfigID.contains("error"), "Entitlement was not added to the season");
		
		String response = f.copyItemBetweenBranches(entitlementID1, mixConfigID, "ACT", null, null, sessionToken, srcBranchID, srcBranchID);
		Assert.assertTrue(response.contains("illegalName"), "Entitlement was copied with existing name ");
		
		response = f.copyItemBetweenBranches(entitlementID1, mixConfigID, "ACT", null, "suffix5", sessionToken, srcBranchID, srcBranchID);
		Assert.assertTrue(response.contains("error"), "Entitlement was copied under configuration " + response);
	}
	
	@Test (dependsOnMethods="copySingleEntitlementUnderMixConfiguration", description="Copy single entitlement under itself in the same season. First, copy without namesuffix, then copy with namesuffix")
	public void copySingleEntitlementUnderItself() throws IOException, JSONException{
		//should fail copy without suffix
		String response = f.copyItemBetweenBranches(entitlementID1, entitlementID1, "ACT", null, null, sessionToken, srcBranchID, srcBranchID);
		Assert.assertTrue(response.contains("illegalName"), "entitlement was copied with existing name ");

		response = f.copyItemBetweenBranches(entitlementID1, entitlementID1, "ACT", null, "suffix6", sessionToken, srcBranchID, srcBranchID);
		Assert.assertTrue(response.contains("newSubTreeId"), "entitlement was not copied: " + response);
		
		JSONObject result = new JSONObject(response);
		String newEntitlement = purchasesApi.getPurchaseItemFromBranch(result.getString("newSubTreeId"), srcBranchID, sessionToken);
		JSONObject oldEntitlement = new JSONObject(purchasesApi.getPurchaseItemFromBranch(entitlementID1, srcBranchID, sessionToken));
		Assert.assertTrue(f.jsonObjsAreEqual(new JSONObject(newEntitlement), oldEntitlement.getJSONArray("entitlements").getJSONObject(0)));
	}
	
	@AfterTest
	private void reset(){
		baseUtils.reset(productID, sessionToken);
	}
}