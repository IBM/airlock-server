package tests.restapi.copy_import.copy_purchases;

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
import tests.restapi.FeaturesRestApi;
import tests.restapi.InAppPurchasesRestApi;
import tests.restapi.ProductsRestApi;
import tests.restapi.SeasonsRestApi;

//when running in branches: we are not testing between 2 season of the same product but between 2 branches on the same season
public class CopyConfigurationDifferentSeason {
	private String seasonID;
	private String seasonID2;
	private String productID;
	private String entitlementID1;
	private String entitlementID2;
	private String entitlementID3;
	private String configID;
	private String eMixID;
	private String poMixID;
	private String filePath;
	private String m_url;
	private ProductsRestApi p;
	private FeaturesRestApi f;
	private SeasonsRestApi s;
	private String sessionToken = "";
	private AirlockUtils baseUtils;
	private String configIDSeason2;
	private String srcBranchID;
	private String destBranchID;
	private boolean runOnMaster;
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
		s = new SeasonsRestApi();
		s.setURL(m_url);
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
		this.runOnMaster = runOnMaster;
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

	//E1 -> PO1
	//   -> CR1
	@Test (description="Create first season with entitlement and configuration. Copy season")
	public void copySeason() throws IOException, JSONException{
		String entitlement = FileUtils.fileToString(filePath + "purchases/inAppPurchase1.txt", "UTF-8", false);
		entitlementID1 = purchasesApi.addPurchaseItemToBranch(seasonID, srcBranchID, entitlement, "ROOT", sessionToken);
		Assert.assertFalse(entitlementID1.contains("error"), "entitlement was not added to the season" + entitlementID1);
		
		String purchaseOptions = FileUtils.fileToString(filePath + "purchases/purchaseOptions1.txt", "UTF-8", false);
		String purchaseOptionsID = purchasesApi.addPurchaseItemToBranch(seasonID, srcBranchID, purchaseOptions, entitlementID1, sessionToken);
		Assert.assertFalse(purchaseOptionsID.contains("error"), "purchaseOptions was not added to the season" + purchaseOptionsID);
		
		String configuration = FileUtils.fileToString(filePath + "configuration_rule1.txt", "UTF-8", false);
		configID = purchasesApi.addPurchaseItemToBranch(seasonID, srcBranchID, configuration, entitlementID1, sessionToken);
		Assert.assertFalse(configID.contains("error"), "cr was not added to the season");

		//when running in branches: we are not testing between 2 season of the same product but between 2 branches on the same season
		try {
			if (runOnMaster) {
				JSONObject sJson = new JSONObject();
				sJson.put("minVersion", "2.0");
				
				seasonID2 = s.addSeason(productID, sJson.toString(), sessionToken);
				Assert.assertFalse(seasonID2.contains("error"), "The second season was not created: " + seasonID2);
				
				destBranchID = BranchesRestApi.MASTER;
			} else {
				seasonID2 = seasonID;
				baseUtils.setSeasonId(seasonID2);
				destBranchID = baseUtils.addBranchFromBranch("b2", srcBranchID,seasonID);
			}
		}catch (Exception e){
			destBranchID = null;
		}		
	}
	
	@Test(dependsOnMethods="copySeason", description="Parse new season ids")
	public void getNewEntitlementsIds() throws Exception{
		 JSONArray entitlements = purchasesApi.getPurchasesBySeasonFromBranch(seasonID2, destBranchID, sessionToken);
		 configIDSeason2 = 	entitlements.getJSONObject(0).getJSONArray("configurationRules").getJSONObject(0).getString("uniqueId"); 	
	}
	
	@Test (dependsOnMethods="getNewEntitlementsIds", description="Copy configuration under entitlement in the new season. First, copy without namesuffix, then copy with namesuffix")
	public void copyConfigurationUnderEntitlement() throws IOException, JSONException{
		String entitlement = FileUtils.fileToString(filePath + "purchases/inAppPurchase2.txt", "UTF-8", false);
		entitlementID3 = purchasesApi.addPurchaseItemToBranch(seasonID2, destBranchID, entitlement, "ROOT", sessionToken);
		Assert.assertFalse(entitlementID3.contains("error"), "Entitlement was not added to season" + entitlementID3);

		//should fail copy without suffix
		String response = f.copyItemBetweenBranches(configID, entitlementID3, "ACT", null, null, sessionToken, srcBranchID, destBranchID);
		Assert.assertTrue(response.contains("illegalName"), "Configuration was copied with existing name ");

		response = f.copyItemBetweenBranches(configID, entitlementID3, "ACT", null, "suffix1", sessionToken, srcBranchID, destBranchID);
		Assert.assertTrue(response.contains("newSubTreeId"), "Configuration was not copied: " + response);
		
		JSONObject result = new JSONObject(response);
		String newFeature = purchasesApi.getPurchaseItemFromBranch(result.getString("newSubTreeId"), destBranchID, sessionToken);
		String oldFeature = purchasesApi.getPurchaseItemFromBranch(configID, srcBranchID, sessionToken);
		Assert.assertTrue(f.jsonObjsAreEqual(new JSONObject(newFeature), new JSONObject(oldFeature)));
	}

	@Test (dependsOnMethods="copyConfigurationUnderEntitlement", description="Copy configuration under purchaseOptions in the new season. First, copy without namesuffix, then copy with namesuffix")
	public void copyConfigurationUnderpurchaseOptions() throws IOException, JSONException{
		String purchaseOptions = FileUtils.fileToString(filePath + "purchases/purchaseOptions2.txt", "UTF-8", false);
		String purchaseOptionsID = purchasesApi.addPurchaseItemToBranch(seasonID2, destBranchID, purchaseOptions, entitlementID3, sessionToken);
		Assert.assertFalse(purchaseOptionsID.contains("error"), "purchaseOptions was not added to season" + purchaseOptionsID);

		//should fail copy without suffix
		String response = f.copyItemBetweenBranches(configID, purchaseOptionsID, "ACT", null, null, sessionToken, srcBranchID, destBranchID);
		Assert.assertTrue(response.contains("illegalName"), "Configuration was copied with existing name ");

		response = f.copyItemBetweenBranches(configID, purchaseOptionsID, "ACT", null, "suffix6", sessionToken, srcBranchID, destBranchID);
		Assert.assertTrue(response.contains("newSubTreeId"), "Configuration was not copied: " + response);
		
		JSONObject result = new JSONObject(response);
		String newFeature = purchasesApi.getPurchaseItemFromBranch(result.getString("newSubTreeId"), destBranchID, sessionToken);
		String oldFeature = purchasesApi.getPurchaseItemFromBranch(configID, srcBranchID, sessionToken);
		Assert.assertTrue(f.jsonObjsAreEqual(new JSONObject(newFeature), new JSONObject(oldFeature)));
	}

	@Test (dependsOnMethods="copyConfigurationUnderpurchaseOptions", description="Copy configuration under mix entitlement in the new season. First, copy without namesuffix, then copy with namesuffix")
	public void copyConfigurationUnderMixEntitlement() throws IOException{
		String entitlementMix = FileUtils.fileToString(filePath + "purchases/inAppPurchaseMutual.txt", "UTF-8", false);
		eMixID = purchasesApi.addPurchaseItemToBranch(seasonID2, destBranchID, entitlementMix, "ROOT", sessionToken);
		Assert.assertFalse(eMixID.contains("error"), "entitlement mix was not added to the season" + eMixID);
						
		String response = f.copyItemBetweenBranches(configID, eMixID, "ACT", null, null, sessionToken, srcBranchID, destBranchID);
		Assert.assertTrue(response.contains("illegalName"), "Configuration was copied with existing name ");

		response = f.copyItemBetweenBranches(configID, eMixID, "ACT", null, "suffix2", sessionToken, srcBranchID, destBranchID);
		Assert.assertTrue(response.contains("error"), "Configuraiton was copied under entitlements mix ");
	}

	@Test (dependsOnMethods="copyConfigurationUnderMixEntitlement", description="Copy configuration under mix purchaseOptions in the new season. First, copy without namesuffix, then copy with namesuffix")
	public void copyConfigurationUnderMixPurchaseOptions() throws IOException{
		String purchaseOptionsMix = FileUtils.fileToString(filePath + "purchases/purchaseOptionsMutual.txt", "UTF-8", false);
		poMixID = purchasesApi.addPurchaseItemToBranch(seasonID2, destBranchID, purchaseOptionsMix, entitlementID3, sessionToken);
		Assert.assertFalse(poMixID.contains("error"), "entitlement mix was not added to the season" + poMixID);
						
		String response = f.copyItemBetweenBranches(configID, poMixID, "ACT", null, null, sessionToken, srcBranchID, destBranchID);
		Assert.assertTrue(response.contains("illegalName"), "Configuration was copied with existing name ");

		response = f.copyItemBetweenBranches(configID, poMixID, "ACT", null, "suffix2", sessionToken, srcBranchID, destBranchID);
		Assert.assertTrue(response.contains("error"), "Configuraiton was copied under purchaseOptions mix ");
	}

	@Test (dependsOnMethods="copyConfigurationUnderMixPurchaseOptions", description="Copy configuration under root in the new season. First, copy without namesuffix, then copy with namesuffix")
	public void copyConfigurationUnderRoot() throws IOException{
		String rootId = f.getRootId(seasonID2, sessionToken);
		
		String response = f.copyItemBetweenBranches(configID, rootId, "ACT", null, null, sessionToken, srcBranchID, destBranchID);
		Assert.assertTrue(response.contains("illegalName"), "Configuration was copied with existing name  ");
		
		response = f.copyItemBetweenBranches(configID, rootId, "ACT", null, "suffix3", sessionToken, srcBranchID, destBranchID);
		Assert.assertTrue(response.contains("error"), "Configuration was copied under root ");
	}
	
	@Test (dependsOnMethods="copyConfigurationUnderRoot", description="Copy configuration under itself in the new season. First, copy without namesuffix, then copy with namesuffix")
	public void copyConfigurationUnderItself() throws IOException, JSONException{
		//should fail copy without suffix

		String response = f.copyItemBetweenBranches(configID, configIDSeason2, "ACT", null, null, sessionToken, srcBranchID, destBranchID);
		Assert.assertTrue(response.contains("illegalName"), "Configuration was copied with existing name  ");
		
		response = f.copyItemBetweenBranches(configID, configIDSeason2, "ACT", null, "suffix4", sessionToken, srcBranchID, destBranchID);
		Assert.assertTrue(response.contains("newSubTreeId"), "Configuration was not copied: " + response);
		
		JSONObject result = new JSONObject(response);
		String newFeature = purchasesApi.getPurchaseItemFromBranch(result.getString("newSubTreeId"), destBranchID, sessionToken);
		JSONObject oldFeature = new JSONObject(purchasesApi.getPurchaseItemFromBranch(configIDSeason2, destBranchID, sessionToken));
		Assert.assertTrue(f.jsonObjsAreEqual(new JSONObject(newFeature), oldFeature.getJSONArray("configurationRules").getJSONObject(0)));

	}
	
	@Test (dependsOnMethods="copyConfigurationUnderItself", description="Copy configuration under configuration in the new season. First, copy without namesuffix, then copy with namesuffix")
	public void copyConfigurationUnderConfiguration() throws IOException, JSONException{
		String entitlement = FileUtils.fileToString(filePath + "purchases/inAppPurchase3.txt", "UTF-8", false);
		entitlementID2 = purchasesApi.addPurchaseItemToBranch(seasonID2, destBranchID, entitlement, "ROOT", sessionToken);
		Assert.assertFalse(entitlementID2.contains("error"), "entitlement was not added to season" + entitlementID2);
		
		String configuration = FileUtils.fileToString(filePath + "configuration_rule2.txt", "UTF-8", false);
		String configID2 = purchasesApi.addPurchaseItemToBranch(seasonID2, destBranchID, configuration, entitlementID2, sessionToken);
		Assert.assertFalse(configID.contains("error"), "cr was not added to the season");

		String response = f.copyItemBetweenBranches(configID, configID2, "ACT", null, null, sessionToken, srcBranchID, destBranchID);
		Assert.assertTrue(response.contains("illegalName"), "Configuration was copied with existing name  ");

		response = f.copyItemBetweenBranches(configID, configID2, "ACT", null, "suffix5", sessionToken, srcBranchID, destBranchID);
		Assert.assertTrue(response.contains("newSubTreeId"), "Configuration was not copied: " + response);
		
		JSONObject result = new JSONObject(response);
		String newFeature = purchasesApi.getPurchaseItemFromBranch(result.getString("newSubTreeId"), destBranchID, sessionToken);
		String oldFeature = purchasesApi.getPurchaseItemFromBranch(configID, srcBranchID, sessionToken);
		Assert.assertTrue(f.jsonObjsAreEqual(new JSONObject(newFeature), new JSONObject(oldFeature)));
	}
	
	@Test (dependsOnMethods="copyConfigurationUnderConfiguration", description="Copy configuration under mix configuration in the new season. First, copy without namesuffix, then copy with namesuffix")
	public void copyConfigurationUnderMixConfiguration() throws IOException, JSONException{
		String configuration = FileUtils.fileToString(filePath + "configuration_feature-mutual.txt", "UTF-8", false);
		String mixConfigID = purchasesApi.addPurchaseItemToBranch(seasonID2, destBranchID, configuration, entitlementID2, sessionToken);
		Assert.assertFalse(mixConfigID.contains("error"), "Feature was not added to the season");
			
		String response = f.copyItemBetweenBranches(configID, mixConfigID, "ACT", null, null, sessionToken, srcBranchID, destBranchID);
		Assert.assertTrue(response.contains("illegalName"), "Configuration was copied with existing name  ");

		response = f.copyItemBetweenBranches(configID, mixConfigID, "ACT", null, "suffix8", sessionToken, srcBranchID, destBranchID);
		Assert.assertTrue(response.contains("newSubTreeId"), "Configuration was not copied: " + response);
		
		JSONObject result = new JSONObject(response);
		String newFeature = purchasesApi.getPurchaseItemFromBranch(result.getString("newSubTreeId"), destBranchID, sessionToken);
		String oldFeature = purchasesApi.getPurchaseItemFromBranch(configID, srcBranchID, sessionToken);
		Assert.assertTrue(f.jsonObjsAreEqual(new JSONObject(newFeature), new JSONObject(oldFeature)));
	}
	
	@AfterTest
	private void reset(){
		baseUtils.reset(productID, sessionToken);
	}
}