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

public class ImportMIXConfigurationDifferentProduct {
	private String seasonID;
	private String seasonID2;
	private String productID;
	private String productID2;
	private String entitlementID2;
	private String configID;
	private String mixConfigID;
	private String mixConfigID2;
	private String mixId;
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
		MIX Config under entitlement - allowed
		MIX Config under config - allowed
		MIX Config under mix of configs - allowed
		MIX Config under root - not allowed
		MIX Config under mix of entitlements - not allowed	
	 */
	@Test (description="Create first season with entitlement and configuration")
	public void addComponents() throws IOException, JSONException{
		String entitlement1 = FileUtils.fileToString(filePath + "purchases/inAppPurchase1.txt", "UTF-8", false);
		String entitlementID1 = purchasesApi.addPurchaseItemToBranch(seasonID, srcBranchID, entitlement1, "ROOT", sessionToken);
		Assert.assertFalse(entitlementID1.contains("error"), "entitlement was not added to the season");
		
		String configurationMix = FileUtils.fileToString(filePath + "configuration_feature-mutual.txt", "UTF-8", false);
		mixConfigID = purchasesApi.addPurchaseItemToBranch(seasonID, srcBranchID, configurationMix, entitlementID1, sessionToken);
		Assert.assertFalse(mixConfigID.contains("error"), "Configuration mix was not added to the season");

		String configuration = FileUtils.fileToString(filePath + "configuration_rule1.txt", "UTF-8", false);
		configID = purchasesApi.addPurchaseItemToBranch(seasonID, srcBranchID, configuration, mixConfigID, sessionToken);
		Assert.assertFalse(configID.contains("error"), "Configuration was not added to the season");
	}
	
	@Test (dependsOnMethods="addComponents", description="Create new product with all components")
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
		Assert.assertFalse(mixId.contains("error"), "entitlement mix was not added to the season" + mixId);

		String configuration = FileUtils.fileToString(filePath + "configuration_rule1.txt", "UTF-8", false);
		configID2 = purchasesApi.addPurchaseItemToBranch(seasonID2, destBranchID, configuration, entitlementID2, sessionToken);
		Assert.assertFalse(configID2.contains("error"), "cr was not added to the season");
		
		String mixConfiguration = FileUtils.fileToString(filePath + "configuration_feature-mutual.txt", "UTF-8", false);
		mixConfigID2 = purchasesApi.addPurchaseItemToBranch(seasonID2, destBranchID, mixConfiguration, entitlementID2, sessionToken);
		Assert.assertFalse(mixConfigID2.contains("error"), "entitlement was not added to the season");
	}

	@Test (dependsOnMethods="createNewProduct", description="Import configuration under entitlement in the new product. First, copy without namesuffix, then copy with namesuffix")
	public void importConfigurationUnderEntitlement() throws IOException, JSONException{
		//should fail copy without suffix
		String configToImport = purchasesApi.getPurchaseItemFromBranch(mixConfigID, srcBranchID, sessionToken);
		
		String response = f.importFeatureToBranch(configToImport, entitlementID2, "ACT", null, null, true, sessionToken, destBranchID);
		Assert.assertTrue(response.contains("illegalName"), "Configuration mix was imported with existing name ");

		response = f.importFeatureToBranch(configToImport, entitlementID2, "ACT", null, "suffix1", false, sessionToken, destBranchID);
		Assert.assertTrue(response.contains("illegalId"), "Configuration mix was not imported: " + response);

		response = f.importFeatureToBranch(configToImport, entitlementID2, "ACT", null, "suffix1", true, sessionToken, destBranchID);
		Assert.assertTrue(response.contains("newSubTreeId"), "Configuration mix was not imported: " + response);
		
		JSONObject result = new JSONObject(response);
		String newEntitlement = purchasesApi.getPurchaseItemFromBranch(result.getString("newSubTreeId"), destBranchID, sessionToken);
		Assert.assertTrue(f.jsonObjsAreEqual(new JSONObject(newEntitlement), new JSONObject(configToImport)));

	}
	
	@Test (dependsOnMethods="importConfigurationUnderEntitlement", description="Import mix configuration under mix entitlement in the same season.")
	public void importConfigurationUnderMixEntitlement() throws IOException{
		String configToImport = purchasesApi.getPurchaseItemFromBranch(mixConfigID, srcBranchID, sessionToken);
		
		String response = f.importFeatureToBranch(configToImport, mixId, "ACT", null, "suffix2", true, sessionToken, destBranchID);
		Assert.assertTrue(response.contains("error"), "Configuraiton mix was imported under entitlements mix ");
	}
	
	@Test (dependsOnMethods="importConfigurationUnderMixEntitlement", description="Import mix configuration under root in the same season.")
	public void importConfigurationUnderRoot() throws IOException{
		String rootId = purchasesApi.getBranchRootId(seasonID2, destBranchID, sessionToken);
		String configToImport = purchasesApi.getPurchaseItemFromBranch(mixConfigID, srcBranchID, sessionToken);
		
		String response = f.importFeatureToBranch(configToImport, rootId, "ACT", null, "suffix3", true, sessionToken, destBranchID);
		Assert.assertTrue(response.contains("error"), "Configuraiton mix was imported under root");
	}
	
	
	@Test (dependsOnMethods="importConfigurationUnderRoot", description="Import mix configuration under configuration in the same entitlement in the same season.")
	public void importConfigurationUnderConfiguration() throws IOException, JSONException{
		String configToImport = purchasesApi.getPurchaseItemFromBranch(mixConfigID, srcBranchID, sessionToken);
		
		//should fail copy without suffix
		String response = f.importFeatureToBranch(configToImport, configID2, "ACT", null, null, true, sessionToken, destBranchID);
		Assert.assertTrue(response.contains("illegalName"), "Configuration mix was imported with existing name");
		
		response = f.importFeatureToBranch(configToImport, configID2, "ACT", null, "suffix5", true, sessionToken, destBranchID);
		Assert.assertTrue(response.contains("newSubTreeId"), "Configuraiton mix was not imported: " + response);
		
		JSONObject result = new JSONObject(response);
		String newEntitlement = purchasesApi.getPurchaseItemFromBranch(result.getString("newSubTreeId"), destBranchID, sessionToken);
		Assert.assertTrue(f.jsonObjsAreEqual(new JSONObject(newEntitlement), new JSONObject(configToImport)));

	}

	@Test (dependsOnMethods="importConfigurationUnderConfiguration", description="Import mix configuration under mix configuration in the same season.")
	public void importSingleEntitlementUnderMixConfiguration() throws IOException, JSONException{
		String configToImport = purchasesApi.getPurchaseItemFromBranch(mixConfigID, srcBranchID, sessionToken);
		
		String response = f.importFeatureToBranch(configToImport, mixConfigID2, "ACT", null, null, true, sessionToken, destBranchID);
		Assert.assertTrue(response.contains("illegalName"), "Configuration mix was imported with existing name ");
		
		response = f.importFeatureToBranch(configToImport, mixConfigID2, "ACT", null, "suffix6", true, sessionToken, destBranchID);
		Assert.assertTrue(response.contains("newSubTreeId"), "Configuration mix was not imported: " + response);
		
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