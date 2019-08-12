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
import tests.com.ibm.qautils.JSONUtils;
import tests.restapi.AirlockUtils;
import tests.restapi.BranchesRestApi;
import tests.restapi.FeaturesRestApi;
import tests.restapi.InAppPurchasesRestApi;
import tests.restapi.ProductsRestApi;
import tests.restapi.SeasonsRestApi;

public class CopyConfigurationDifferentProduct {
	private String seasonID;
	private String seasonID2;
	private String productID;
	private String productID2;
	private String entitlementID1;
	private String entitlementID2;
	private String purchaseOptionsID2;
	private String purchaseOptionsMixID;
	private String mixId;
	private String mixConfigID;
	private String configID;
	private String configID2;
	private String filePath;
	private String m_url;
	private ProductsRestApi p;
	private FeaturesRestApi f;
	private SeasonsRestApi s;
	private InAppPurchasesRestApi purchasesApi;
	private String sessionToken = "";
	private AirlockUtils baseUtils;
	private String srcBranchID;
	private String destBranchID;
	private boolean runOnMaster;


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
	@Test (description="Create first season with entitlement, purchaseOptions and configuration. Copy season")
	public void addConfiguration() throws IOException, JSONException{
		String entitlement = FileUtils.fileToString(filePath + "purchases/inAppPurchase1.txt", "UTF-8", false);
		entitlementID1 = purchasesApi.addPurchaseItemToBranch(seasonID, srcBranchID, entitlement, "ROOT", sessionToken);
		Assert.assertFalse(entitlementID1.contains("error"), "entitlement was not added to the season" + entitlementID1);
		
		String purchaseOptions = FileUtils.fileToString(filePath + "purchases/purchaseOptions1.txt", "UTF-8", false);
		String purchaseOptionsID = purchasesApi.addPurchaseItemToBranch(seasonID, srcBranchID, purchaseOptions, entitlementID1, sessionToken);
		Assert.assertFalse(purchaseOptionsID.contains("error"), "purchaseOptions was not added to the season" + purchaseOptionsID);
		
		String configuration = FileUtils.fileToString(filePath + "configuration_rule1.txt", "UTF-8", false);
		configID = purchasesApi.addPurchaseItemToBranch(seasonID, srcBranchID, configuration, entitlementID1, sessionToken);
		Assert.assertFalse(configID.contains("error"), "cr was not added to the season");
	}
	
	@Test (dependsOnMethods="addConfiguration", description="Create new product with all components")
	public void createNewProduct() throws IOException, JSONException{
		//create second product
		String product = FileUtils.fileToString(filePath + "product1.txt", "UTF-8", false);
		product = JSONUtils.generateUniqueString(product, 5, "codeIdentifier");
		product = JSONUtils.generateUniqueString(product, 8, "name");
		productID2 = p.addProduct(product, sessionToken);
		baseUtils.printProductToFile(productID2);
		
		//add season to second product
		JSONObject sJson = new JSONObject();
		sJson.put("minVersion", "2.0");		
		seasonID2 = s.addSeason(productID2, sJson.toString(), sessionToken);
		Assert.assertFalse(seasonID2.contains("error"), "The season was not created in the new product: " + seasonID2);

		try {
			if (runOnMaster) {
				destBranchID = BranchesRestApi.MASTER;
			} else {
				baseUtils.setSeasonId(seasonID2);
				destBranchID = baseUtils.addBranch("b1");
			}
		}catch (Exception e){
			destBranchID = null;
		}
		
		//E1 -> CR, CR_MTX
		//   ->  PO, PO_MTX
		//E_MTX
		String entitlement1 = FileUtils.fileToString(filePath + "purchases/inAppPurchase1.txt", "UTF-8", false);
		entitlementID2 = purchasesApi.addPurchaseItemToBranch(seasonID2, destBranchID,  entitlement1, "ROOT", sessionToken);
		Assert.assertFalse(entitlementID2.contains("error"), "entitlement was not added to the season");
		
		String entitlementMix = FileUtils.fileToString(filePath + "purchases/inAppPurchaseMutual.txt", "UTF-8", false);
		mixId = purchasesApi.addPurchaseItemToBranch(seasonID2, destBranchID, entitlementMix, "ROOT", sessionToken);
		Assert.assertFalse(entitlementMix.contains("error"), "entitlementMix was not added to the season" + entitlementMix);

		String configuration = FileUtils.fileToString(filePath + "configuration_rule1.txt", "UTF-8", false);
		configID2 = purchasesApi.addPurchaseItemToBranch(seasonID2, destBranchID, configuration, entitlementID2, sessionToken);
		Assert.assertFalse(configID2.contains("error"), "Feature was not added to the season");
		
		String mixConfiguration = FileUtils.fileToString(filePath + "configuration_feature-mutual.txt", "UTF-8", false);
		mixConfigID = purchasesApi.addPurchaseItemToBranch(seasonID2, destBranchID, mixConfiguration, entitlementID2, sessionToken);
		Assert.assertFalse(mixConfigID.contains("error"), "Feature was not added to the season");
		
		String purchaseOptions1 = FileUtils.fileToString(filePath + "purchases/purchaseOptions1.txt", "UTF-8", false);
		purchaseOptionsID2 = purchasesApi.addPurchaseItemToBranch(seasonID2, destBranchID,  purchaseOptions1, entitlementID2, sessionToken);
		Assert.assertFalse(purchaseOptionsID2.contains("error"), "purchaseOptions was not added to the season");
		
		String purchaseOptionsMix = FileUtils.fileToString(filePath + "purchases/purchaseOptionsMutual.txt", "UTF-8", false);
		purchaseOptionsMixID = purchasesApi.addPurchaseItemToBranch(seasonID2, destBranchID,  purchaseOptionsMix, entitlementID2, sessionToken);
		Assert.assertFalse(purchaseOptionsMixID.contains("error"), "purchaseOptions mix was not added to the season");
	}
	
	@Test (dependsOnMethods="createNewProduct", description="Copy configuration under entitlement in the new product. First, copy without namesuffix, then copy with namesuffix")
	public void copyConfigurationUnderEntitlement() throws IOException, JSONException{
		//should fail copy without suffix
		String response = f.copyItemBetweenBranches(configID, entitlementID2, "ACT", null, null, sessionToken, srcBranchID, destBranchID);
		Assert.assertTrue(response.contains("illegalName"), "Configuration was copied with existing name ");

		response = f.copyItemBetweenBranches(configID, entitlementID2, "ACT", null, "suffix1", sessionToken, srcBranchID, destBranchID);
		Assert.assertTrue(response.contains("newSubTreeId"), "cr was not copied: " + response);
		
		JSONObject result = new JSONObject(response);
		String newEntitlement = purchasesApi.getPurchaseItemFromBranch(result.getString("newSubTreeId"), destBranchID, sessionToken);
		String oldPurchaseOptions = purchasesApi.getPurchaseItemFromBranch(configID, srcBranchID, sessionToken);
		Assert.assertTrue(f.jsonObjsAreEqual(new JSONObject(newEntitlement), new JSONObject(oldPurchaseOptions)));
	}

	@Test (dependsOnMethods="copyConfigurationUnderEntitlement", description="Copy configuration under purchaseOptions in the new product. First, copy without namesuffix, then copy with namesuffix")
	public void copyConfigurationUnderPurchaseOptions() throws IOException, JSONException{
		//should fail copy without suffix
		String response = f.copyItemBetweenBranches(configID, purchaseOptionsID2, "ACT", null, null, sessionToken, srcBranchID, destBranchID);
		Assert.assertTrue(response.contains("illegalName"), "Configuration was copied with existing name ");

		response = f.copyItemBetweenBranches(configID, purchaseOptionsID2, "ACT", null, "suffix7", sessionToken, srcBranchID, destBranchID);
		Assert.assertTrue(response.contains("newSubTreeId"), "cr was not copied: " + response);
		
		JSONObject result = new JSONObject(response);
		String newEntitlement = purchasesApi.getPurchaseItemFromBranch(result.getString("newSubTreeId"), destBranchID, sessionToken);
		String oldPurchaseOptions = purchasesApi.getPurchaseItemFromBranch(configID, srcBranchID, sessionToken);
		Assert.assertTrue(f.jsonObjsAreEqual(new JSONObject(newEntitlement), new JSONObject(oldPurchaseOptions)));
	}

	@Test (dependsOnMethods="copyConfigurationUnderPurchaseOptions", description="Copy configuration under mix feature in the new product. First, copy without namesuffix, then copy with namesuffix")
	public void copyConfigurationUnderMixEntitlement() throws IOException{
		String entitlementMix = FileUtils.fileToString(filePath + "purchases/inAppPurchaseMutual.txt", "UTF-8", false);
		mixId = purchasesApi.addPurchaseItemToBranch(seasonID2, destBranchID, entitlementMix, "ROOT", sessionToken);
		Assert.assertFalse(mixId.contains("error"), "Entitlement mix was not added to the season" + mixId);
						
		String response = f.copyItemBetweenBranches(configID, mixId, "ACT", null, null, sessionToken, srcBranchID, destBranchID);
		Assert.assertTrue(response.contains("illegalName"), "Configuration was copied with existing name ");

		response = f.copyItemBetweenBranches(configID, mixId, "ACT", null, "suffix2", sessionToken, srcBranchID, destBranchID);
		Assert.assertTrue(response.contains("error"), "Configuraiton was copied under entitlements mix ");
	}
	
	@Test (dependsOnMethods="copyConfigurationUnderMixEntitlement", description="Copy configuration under root in the new product. First, copy without namesuffix, then copy with namesuffix")
	public void copyConfigurationUnderRoot() throws IOException{
		String rootId = f.getRootId(seasonID2, sessionToken);
		
		String response = f.copyItemBetweenBranches(configID, rootId, "ACT", null, null, sessionToken, srcBranchID, destBranchID);
		Assert.assertTrue(response.contains("illegalName"), "Configuration was copied with existing name ");
		
		response = f.copyItemBetweenBranches(configID, rootId, "ACT", null, "suffix3", sessionToken, srcBranchID, destBranchID);
		Assert.assertTrue(response.contains("error"), "Configuration was copied under root ");
	}
	
	@Test (dependsOnMethods="copyConfigurationUnderRoot", description="Copy configuration under root in the new product. First, copy without namesuffix, then copy with namesuffix")
	public void copyConfigurationUnderPurchaseOptionsMix() throws IOException{
		String response = f.copyItemBetweenBranches(configID, purchaseOptionsMixID, "ACT", null, null, sessionToken, srcBranchID, destBranchID);
		Assert.assertTrue(response.contains("illegalName"), "Configuration was copied with existing name ");
		
		response = f.copyItemBetweenBranches(configID, purchaseOptionsMixID, "ACT", null, "suffix3", sessionToken, srcBranchID, destBranchID);
		Assert.assertTrue(response.contains("error"), "Configuration was copied under purchaseOptionsMix ");
	}
	
	@Test (dependsOnMethods="copyConfigurationUnderPurchaseOptionsMix", description="Copy configuration under configuration in the new product. First, copy without namesuffix, then copy with namesuffix")
	public void copyConfigurationUnderConfiguration() throws IOException, JSONException{

		String response = f.copyItemBetweenBranches(configID, configID2, "ACT", null, null, sessionToken, srcBranchID, destBranchID);
		Assert.assertTrue(response.contains("illegalName"), "Configuration was copied with existing name  ");

		response = f.copyItemBetweenBranches(configID, configID2, "ACT", null, "suffix4", sessionToken, srcBranchID, destBranchID);
		Assert.assertTrue(response.contains("newSubTreeId"), "COnfiguration was not copied: " + response);
		
		JSONObject result = new JSONObject(response);
		String newEntitlement = purchasesApi.getPurchaseItemFromBranch(result.getString("newSubTreeId"), destBranchID, sessionToken);
		String oldPurchaseOptions = purchasesApi.getPurchaseItemFromBranch(configID, srcBranchID, sessionToken);
		Assert.assertTrue(f.jsonObjsAreEqual(new JSONObject(newEntitlement), new JSONObject(oldPurchaseOptions)));
	}
	
	public void copyConfigurationUnderMixConfiguration() throws IOException, JSONException{	
		String response = f.copyItemBetweenBranches(configID, mixConfigID, "ACT", null, null, sessionToken, srcBranchID, destBranchID);
		Assert.assertTrue(response.contains("illegalName"), "Configuration was copied with existing name  ");

		response = f.copyItemBetweenBranches(configID, mixConfigID, "ACT", null, "suffix5", sessionToken, srcBranchID, destBranchID);
		Assert.assertTrue(response.contains("newSubTreeId"), "Configuration was not copied: " + response);
		
		JSONObject result = new JSONObject(response);
		String newEntitlement = purchasesApi.getPurchaseItemFromBranch(result.getString("newSubTreeId"), destBranchID, sessionToken);
		String oldPurchaseOptions = purchasesApi.getPurchaseItemFromBranch(configID, srcBranchID, sessionToken);
		Assert.assertTrue(f.jsonObjsAreEqual(new JSONObject(newEntitlement), new JSONObject(oldPurchaseOptions)));
	}	
	
	@AfterTest
	private void reset(){
		baseUtils.reset(productID, sessionToken);
		baseUtils.reset(productID2, sessionToken);
	}
}