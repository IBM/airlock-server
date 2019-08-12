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

//when running with branches this is a copy with in the same branch
public class CopyConfigurationSameSeason {
	private String seasonID;
	private String productID;
	private String entitlementID1;
	private String entitlementID2;
	private String purchaseOptionsID1;
	private String purchaseOptionsID2;
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
		Config under entitlement - allowed
		Config under purchaseOptions - allowed
		Config under config - allowed
		Config under mix of configs - allowed
		Config under root - not allowed
		Config under mix of entitlements - not allowed
		Config under mix of purchaseOptions - not allowed	
	 */

	@Test (description="Copy configuration under another entitlement in the same season. First, copy without namesuffix, then copy with namesuffix")
	public void copyConfigurationUnderEntitlement() throws IOException, JSONException{
		String entitlement1 = FileUtils.fileToString(filePath + "purchases/inAppPurchase1.txt", "UTF-8", false);
		entitlementID1 = purchasesApi.addPurchaseItemToBranch(seasonID, srcBranchID, entitlement1, "ROOT", sessionToken);
		Assert.assertFalse(entitlementID1.contains("error"), "Entitlement was not added to the season");

		String configuration = FileUtils.fileToString(filePath + "configuration_rule1.txt", "UTF-8", false);
		configID = purchasesApi.addPurchaseItemToBranch(seasonID, srcBranchID, configuration, entitlementID1, sessionToken);
		Assert.assertFalse(configID.contains("error"), "Entitlement was not added to the season");

		String entitlement2 = FileUtils.fileToString(filePath + "purchases/inAppPurchase2.txt", "UTF-8", false);
		entitlementID2 = purchasesApi.addPurchaseItemToBranch(seasonID, srcBranchID, entitlement2, "ROOT", sessionToken);
		Assert.assertFalse(entitlementID2.contains("error"), "Entitlement was not added to the season");

		//should fail copy without suffix
		String response = f.copyItemBetweenBranches(configID, entitlementID2, "ACT", null, null, sessionToken, srcBranchID, srcBranchID);
		Assert.assertTrue(response.contains("illegalName"), "Configuration was copied with existing name ");

		response = f.copyItemBetweenBranches(configID, entitlementID2, "ACT", null, "suffix1", sessionToken, srcBranchID, srcBranchID);
		Assert.assertTrue(response.contains("newSubTreeId"), "Configuration was not copied: " + response);

		JSONObject result = new JSONObject(response);
		String newEntitlement = purchasesApi.getPurchaseItemFromBranch(result.getString("newSubTreeId"), srcBranchID, sessionToken);
		String oldEntitlement = purchasesApi.getPurchaseItemFromBranch(configID, srcBranchID, sessionToken);
		Assert.assertTrue(f.jsonObjsAreEqual(new JSONObject(newEntitlement), new JSONObject(oldEntitlement)));
	}

	@Test (dependsOnMethods = "copyConfigurationUnderEntitlement", description="Copy configuration under another purchaseOptions in the same season. First, copy without namesuffix, then copy with namesuffix")
	public void copyConfigurationUnderPurchaseOptions() throws IOException, JSONException{
		String purchaseOptions1 = FileUtils.fileToString(filePath + "purchases/purchaseOptions1.txt", "UTF-8", false);
		purchaseOptionsID1 = purchasesApi.addPurchaseItemToBranch(seasonID, srcBranchID, purchaseOptions1, entitlementID1, sessionToken);
		Assert.assertFalse(purchaseOptionsID1.contains("error"), "Entitlement was not added to the season: " + purchaseOptionsID1);

		String configuration = FileUtils.fileToString(filePath + "configuration_rule2.txt", "UTF-8", false);
		configID = purchasesApi.addPurchaseItemToBranch(seasonID, srcBranchID, configuration, purchaseOptionsID1, sessionToken);
		Assert.assertFalse(configID.contains("error"), "Entitlement was not added to the season: " + configID);

		String purchaseOptions2 = FileUtils.fileToString(filePath + "purchases/purchaseOptions2.txt", "UTF-8", false);
		purchaseOptionsID2 = purchasesApi.addPurchaseItemToBranch(seasonID, srcBranchID, purchaseOptions2, entitlementID1, sessionToken);
		Assert.assertFalse(purchaseOptionsID2.contains("error"), "purchaseOptions was not added to the season: " + purchaseOptionsID2);

		//should fail copy without suffix
		String response = f.copyItemBetweenBranches(configID, purchaseOptionsID2, "ACT", null, null, sessionToken, srcBranchID, srcBranchID);
		Assert.assertTrue(response.contains("illegalName"), "Configuration was copied with existing name ");

		response = f.copyItemBetweenBranches(configID, purchaseOptionsID2, "ACT", null, "suffix2", sessionToken, srcBranchID, srcBranchID);
		Assert.assertTrue(response.contains("newSubTreeId"), "Configuration was not copied: " + response);

		JSONObject result = new JSONObject(response);
		String newEntitlement = purchasesApi.getPurchaseItemFromBranch(result.getString("newSubTreeId"), srcBranchID, sessionToken);
		String oldEntitlement = purchasesApi.getPurchaseItemFromBranch(configID, srcBranchID, sessionToken);
		Assert.assertTrue(f.jsonObjsAreEqual(new JSONObject(newEntitlement), new JSONObject(oldEntitlement)));
	}

	@Test (dependsOnMethods="copyConfigurationUnderPurchaseOptions", description="Copy configuration under mix entitlements in the same season. First, copy without namesuffix, then copy with namesuffix")
	public void copyConfigurationUnderMixEntitlement() throws IOException{
		String entitlementMix = FileUtils.fileToString(filePath + "purchases/inAppPurchaseMutual.txt", "UTF-8", false);
		String eMixId = purchasesApi.addPurchaseItemToBranch(seasonID, srcBranchID, entitlementMix, entitlementID2, sessionToken);
		Assert.assertFalse(eMixId.contains("error"), "Entitlement mix was not added to the season" + eMixId);

		String response = f.copyItemBetweenBranches(configID, eMixId, "ACT", null, null, sessionToken, srcBranchID, srcBranchID);
		Assert.assertTrue(response.contains("illegalName"), "Configuraiton was copied with existing name ");

		response = f.copyItemBetweenBranches(configID, eMixId, "ACT", null, "suffix13", sessionToken, srcBranchID, srcBranchID);
		Assert.assertTrue(response.contains("error"), "Configuraiton was copied under entitlements mix ");
	}

	@Test (dependsOnMethods="copyConfigurationUnderMixEntitlement", description="Copy configuration under mix purchaseOptions in the same season. First, copy without namesuffix, then copy with namesuffix")
	public void copyConfigurationUnderMixPurchaseOptions() throws IOException{
		String purchaseOptionsMix = FileUtils.fileToString(filePath + "purchases/purchaseOptionsMutual.txt", "UTF-8", false);
		String poMixId = purchasesApi.addPurchaseItemToBranch(seasonID, srcBranchID, purchaseOptionsMix, entitlementID2, sessionToken);
		Assert.assertFalse(poMixId.contains("error"), "purchaseOptions mix was not added to the season" + poMixId);

		String response = f.copyItemBetweenBranches(configID, poMixId, "ACT", null, null, sessionToken, srcBranchID, srcBranchID);
		Assert.assertTrue(response.contains("illegalName"), "Configuraiton was copied with existing name ");

		response = f.copyItemBetweenBranches(configID, poMixId, "ACT", null, "suffix12", sessionToken, srcBranchID, srcBranchID);
		Assert.assertTrue(response.contains("error"), "Configuraiton was copied under purchaseOptions mix ");
	}

	@Test (dependsOnMethods="copyConfigurationUnderMixPurchaseOptions", description="Copy configuration under root in the same season. First, copy without namesuffix, then copy with namesuffix")
	public void copyConfigurationUnderRoot() throws IOException{
		String rootId = f.getRootId(seasonID, sessionToken);

		String response = f.copyItemBetweenBranches(configID, rootId, "ACT", null, null, sessionToken, srcBranchID, srcBranchID);
		Assert.assertTrue(response.contains("illegalName"), "Configuraiton was copied with existing name ");

		response = f.copyItemBetweenBranches(configID, rootId, "ACT", null, "suffix3", sessionToken, srcBranchID, srcBranchID);
		Assert.assertTrue(response.contains("error"), "Configuraiton was copied under features mix ");
	}

	@Test (dependsOnMethods="copyConfigurationUnderRoot", description="Copy configuration under itself in the same season. First, copy without namesuffix, then copy with namesuffix")
	public void copyConfigurationUnderItself() throws IOException, JSONException{
		//should fail copy without suffix

		String response = f.copyItemBetweenBranches(configID, configID, "ACT", null, null, sessionToken, srcBranchID, srcBranchID);
		Assert.assertTrue(response.contains("illegalName"), "Configuraiton was copied with existing name ");

		response = f.copyItemBetweenBranches(configID, configID, "ACT", null, "suffix4", sessionToken, srcBranchID, srcBranchID);
		Assert.assertTrue(response.contains("newSubTreeId"), "Configuraiton was not copied: " + response);

		JSONObject result = new JSONObject(response);
		String newEntitlement = purchasesApi.getPurchaseItemFromBranch(result.getString("newSubTreeId"), srcBranchID, sessionToken);
		JSONObject oldEntitlement = new JSONObject(purchasesApi.getPurchaseItemFromBranch(configID, srcBranchID, sessionToken));
		Assert.assertTrue(f.jsonObjsAreEqual(new JSONObject(newEntitlement), oldEntitlement.getJSONArray("configurationRules").getJSONObject(0)));

	}

	@Test (dependsOnMethods="copyConfigurationUnderItself", description="Copy configuration under configuration in the same feature in the same season. First, copy without namesuffix, then copy with namesuffix")
	public void copyConfigurationUnderConfiguration() throws IOException, JSONException{
		String entitlement = FileUtils.fileToString(filePath + "purchases/inAppPurchase3.txt", "UTF-8", false);
		String entitlementID3 = purchasesApi.addPurchaseItemToBranch(seasonID, srcBranchID, entitlement, "ROOT", sessionToken);
		Assert.assertFalse(entitlementID3.contains("error"), "entitlement was not added to season" + entitlementID3);
		
		JSONObject configurationObj = new JSONObject(FileUtils.fileToString(filePath + "configuration_rule2.txt", "UTF-8", false));
		configurationObj.put("name", "CR3");
		String configID2 = purchasesApi.addPurchaseItemToBranch(seasonID, srcBranchID, configurationObj.toString(), entitlementID3, sessionToken);
		Assert.assertFalse(configID2.contains("error"), "Configuraiton was not added to the season:"+configID2);

		//should fail copy without suffix
		String response = f.copyItemBetweenBranches(configID, configID2, "ACT", null, null, sessionToken, srcBranchID, srcBranchID);
		Assert.assertTrue(response.contains("illegalName"), "Configuraiton was copied with existing name ");

		response = f.copyItemBetweenBranches(configID, configID2, "ACT", null, "suffix5", sessionToken, srcBranchID, srcBranchID);
		Assert.assertTrue(response.contains("newSubTreeId"), "Configuraiton was not copied: " + response);

		JSONObject result = new JSONObject(response);
		String newEntitlement = purchasesApi.getPurchaseItemFromBranch(result.getString("newSubTreeId"), srcBranchID, sessionToken);
		String oldEntitlement = purchasesApi.getPurchaseItemFromBranch(configID, srcBranchID, sessionToken);
		Assert.assertTrue(f.jsonObjsAreEqual(new JSONObject(newEntitlement), new JSONObject(oldEntitlement)));
	}

	@Test (dependsOnMethods="copyConfigurationUnderConfiguration", description="Copy configuration under mix configuration in the same season. First, copy without namesuffix, then copy with namesuffix")
	public void copySingleConfigurationUnderMixConfiguration() throws IOException, JSONException{
		String configuration = FileUtils.fileToString(filePath + "configuration_feature-mutual.txt", "UTF-8", false);
		String mixConfigID = purchasesApi.addPurchaseItemToBranch(seasonID, srcBranchID, configuration, entitlementID2, sessionToken);
		Assert.assertFalse(mixConfigID.contains("error"), "cr mix was not added to the season");

		String response = f.copyItemBetweenBranches(configID, mixConfigID, "ACT", null, null, sessionToken, srcBranchID, srcBranchID);
		Assert.assertTrue(response.contains("illegalName"), "Configuraiton was copied with existing name ");

		response = f.copyItemBetweenBranches(configID, mixConfigID, "ACT", null, "suffix6", sessionToken, srcBranchID, srcBranchID);
		Assert.assertTrue(response.contains("newSubTreeId"), "Configuraiton was not copied: " + response);

		JSONObject result = new JSONObject(response);
		String newEntitlement = purchasesApi.getPurchaseItemFromBranch(result.getString("newSubTreeId"), srcBranchID, sessionToken);
		String oldEntitlement = purchasesApi.getPurchaseItemFromBranch(configID, srcBranchID, sessionToken);
		Assert.assertTrue(f.jsonObjsAreEqual(new JSONObject(newEntitlement), new JSONObject(oldEntitlement)));
	}

	@AfterTest
	private void reset(){
		baseUtils.reset(productID, sessionToken);
	}
}