package tests.restapi.copy_import.import_purchases;

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

public class ImportConfigurationDifferentProduct {
	private String seasonID;
	private String seasonID2;
	private String productID;
	private String productID2;
	private String entitlementID2;
	private String mixId;
	private String mixConfigID;
	private String configID;
	private String configID2;
	private String filePath;
	private String m_url;
	private ProductsRestApi p;
	private FeaturesRestApi f;
	private SeasonsRestApi s;
	private String sessionToken = "";
	private AirlockUtils baseUtils;
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
	Config under config - allowed
	Config under mix of configs - allowed
	Config under root - not allowed
	Config under mix of entitlements - not allowed
		
	 */

	@Test (description="Create first season with entitlement and configuration. Copy season")
	public void addConfiguration() throws IOException, JSONException{
		String entitlement = FileUtils.fileToString(filePath + "purchases/inAppPurchase1.txt", "UTF-8", false);
		String entitlementID1 = purchasesApi.addPurchaseItemToBranch(seasonID, srcBranchID, entitlement, "ROOT", sessionToken);
		Assert.assertFalse(entitlementID1.contains("error"), "entitlement was not added to the season");
		
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

		String entitlement1 = FileUtils.fileToString(filePath + "purchases/inAppPurchase1.txt", "UTF-8", false);
		entitlementID2 = purchasesApi.addPurchaseItemToBranch(seasonID2, destBranchID, entitlement1, "ROOT", sessionToken);
		Assert.assertFalse(entitlementID2.contains("error"), "entitlement was not added to the season");
		
		String entitlementMix = FileUtils.fileToString(filePath + "purchases/inAppPurchaseMutual.txt", "UTF-8", false);
		mixId = purchasesApi.addPurchaseItemToBranch(seasonID2, destBranchID, entitlementMix, "ROOT", sessionToken);
		Assert.assertFalse(mixId.contains("error"), "entitlement mit was not added to the season" + mixId);

		String configuration = FileUtils.fileToString(filePath + "configuration_rule1.txt", "UTF-8", false);
		configID2 = purchasesApi.addPurchaseItemToBranch(seasonID2, destBranchID, configuration, entitlementID2, sessionToken);
		Assert.assertFalse(configID2.contains("error"), "cr was not added to the season");
		
		String mixConfiguration = FileUtils.fileToString(filePath + "configuration_feature-mutual.txt", "UTF-8", false);
		mixConfigID = purchasesApi.addPurchaseItemToBranch(seasonID2, destBranchID, mixConfiguration, entitlementID2, sessionToken);
		Assert.assertFalse(mixConfigID.contains("error"), "cr mix was not added to the season");
	}
	
	@Test (dependsOnMethods="createNewProduct", description="Import configuration under entitlement in the new product.")
	public void importConfigurationUnderEntitlement() throws IOException, JSONException{
		String configToImport = purchasesApi.getPurchaseItemFromBranch(configID, srcBranchID, sessionToken);
		
		String response = f.importFeatureToBranch(configToImport, entitlementID2, "ACT", null, "suffix1", false, sessionToken, destBranchID);			
		Assert.assertTrue(response.contains("illegalId"), "Configuration was copied with existing id ");
		
		response = f.importFeatureToBranch(configToImport, entitlementID2, "ACT", null, "suffix1", true, sessionToken, destBranchID);
		Assert.assertTrue(response.contains("newSubTreeId"), "Entitlement was not copied: " + response);
		
		JSONObject result = new JSONObject(response);
		String newEntitlement = purchasesApi.getPurchaseItemFromBranch(result.getString("newSubTreeId"), destBranchID, sessionToken);
		Assert.assertTrue(f.jsonObjsAreEqual(new JSONObject(newEntitlement), new JSONObject(configToImport)));

	}
	
	@Test (dependsOnMethods="importConfigurationUnderEntitlement", description="Import configuration under mix entitlement in the new product.")
	public void importConfigurationUnderMixEntitlement() throws IOException{
		String entitlementMix = FileUtils.fileToString(filePath + "purchases/inAppPurchaseMutual.txt", "UTF-8", false);
		String mixId = purchasesApi.addPurchaseItemToBranch(seasonID2, destBranchID,  entitlementMix, "ROOT", sessionToken);
		Assert.assertFalse(mixId.contains("error"), "Entitlement was not added to the season" + mixId);
		
		String configToImport = purchasesApi.getPurchaseItem(configID, sessionToken);
		String response = f.importFeatureToBranch(configToImport, mixId, "ACT", null, "suffix2", true, sessionToken, destBranchID);
		Assert.assertTrue(response.contains("error"), "Configuraiton was copied under entitlements mix ");
	}
	
	@Test (dependsOnMethods="importConfigurationUnderMixEntitlement", description="Import configuration under root in the new product.")
	public void importConfigurationUnderRoot() throws IOException{
		String rootId = f.getBranchRootId(seasonID2, destBranchID, sessionToken);
		
		String configToImport = purchasesApi.getPurchaseItem(configID, sessionToken);
		String response = f.importFeatureToBranch(configToImport, rootId, "ACT", null, "suffix3", true, sessionToken, destBranchID);
		Assert.assertTrue(response.contains("error"), "Configuration was copied under root ");
	}
	
	@Test (dependsOnMethods="importConfigurationUnderRoot", description="Import configuration under configuration in the new product.")
	public void importConfigurationUnderConfiguration() throws IOException, JSONException{
		String configToImport = purchasesApi.getPurchaseItemFromBranch(configID, srcBranchID, sessionToken);
		
		String response = f.importFeatureToBranch(configToImport, configID2, "ACT", null, null, true, sessionToken, destBranchID);
		Assert.assertTrue(response.contains("illegalName"), "Configuration was copied with existing name  ");
		
		response = f.importFeatureToBranch(configToImport, configID2, "ACT", null, "suffix4", true, sessionToken, destBranchID);
		Assert.assertTrue(response.contains("newSubTreeId"), "Configuration was not copied: " + response);
		
		JSONObject result = new JSONObject(response);
		String newEntitlement = purchasesApi.getPurchaseItemFromBranch(result.getString("newSubTreeId"), destBranchID, sessionToken);
		Assert.assertTrue(f.jsonObjsAreEqual(new JSONObject(newEntitlement), new JSONObject(configToImport)));
	}
	
	@Test (dependsOnMethods="importConfigurationUnderConfiguration", description="Import configuration under mix configuration in the new product.")
	public void importConfigurationUnderMixConfiguration() throws IOException, JSONException{
		String configToImport = purchasesApi.getPurchaseItemFromBranch(configID, srcBranchID, sessionToken);
		
		String response = f.importFeatureToBranch(configToImport, mixConfigID, "ACT", null, null, true, sessionToken, destBranchID);
		Assert.assertTrue(response.contains("illegalName"), "Configuration was copied with existing name  ");
		
		response = f.importFeatureToBranch(configToImport, mixConfigID, "ACT", null, "suffix5", true, sessionToken, destBranchID);
		Assert.assertTrue(response.contains("newSubTreeId"), "Configuration was not copied: " + response);
		
		JSONObject result = new JSONObject(response);
		String newEntitlement = purchasesApi.getPurchaseItemFromBranch(result.getString("newSubTreeId"), destBranchID, sessionToken);
		Assert.assertTrue(f.jsonObjsAreEqual(new JSONObject(newEntitlement), new JSONObject(configToImport)));

	}
	
	
	@AfterTest
	private void reset(){
		baseUtils.reset(productID, sessionToken);
		baseUtils.reset(productID2, sessionToken);
	}
}