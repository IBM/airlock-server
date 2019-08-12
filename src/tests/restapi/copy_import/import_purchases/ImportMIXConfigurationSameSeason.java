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
import tests.restapi.AirlockUtils;
import tests.restapi.BranchesRestApi;
import tests.restapi.FeaturesRestApi;
import tests.restapi.InAppPurchasesRestApi;
import tests.restapi.ProductsRestApi;


public class ImportMIXConfigurationSameSeason {
	private String seasonID;
	private String productID;
	private String entitlementID1;
	private String entitlementID2;
	private String configID;
	private String mixConfigID;
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
	MIX Config under entitlement - allowed
	MIX Config under config - allowed
	MIX Config under mix of configs - allowed
	MIX Config under root - not allowed
	MIX Config under mix of entitlements - not allowed
		
	 */
	
	@Test (description="Import mix configuration under another entitlement in the same season.")
	public void importConfigurationUnderEntitlement() throws IOException, JSONException{
		String entitlement1 = FileUtils.fileToString(filePath + "purchases/inAppPurchase1.txt", "UTF-8", false);
		entitlementID1 = purchasesApi.addPurchaseItemToBranch(seasonID, srcBranchID, entitlement1, "ROOT", sessionToken);
		Assert.assertFalse(entitlementID1.contains("error"), "entitlement was not added to the season");
		
		String configurationMix = FileUtils.fileToString(filePath + "configuration_feature-mutual.txt", "UTF-8", false);
		mixConfigID = purchasesApi.addPurchaseItemToBranch(seasonID, srcBranchID, configurationMix, entitlementID1, sessionToken);
		Assert.assertFalse(mixConfigID.contains("error"), "cr mix was not added to the season");

		String configuration = FileUtils.fileToString(filePath + "configuration_rule1.txt", "UTF-8", false);
		configID = purchasesApi.addPurchaseItemToBranch(seasonID, srcBranchID, configuration, mixConfigID, sessionToken);
		Assert.assertFalse(configID.contains("error"), "cr was not added to the season");
				
		String entitlement2 = FileUtils.fileToString(filePath + "purchases/inAppPurchase2.txt", "UTF-8", false);
		entitlementID2 = purchasesApi.addPurchaseItemToBranch(seasonID, srcBranchID, entitlement2, "ROOT", sessionToken);
		Assert.assertFalse(entitlementID2.contains("error"), "Entitlement was not added to the season");
		
		//should fail copy without suffix
		String configToImport = purchasesApi.getPurchaseItemFromBranch(mixConfigID, srcBranchID, sessionToken);
		
		String response = f.importFeatureToBranch(configToImport, entitlementID2, "ACT", null, null, true, sessionToken, srcBranchID);
		Assert.assertTrue(response.contains("illegalName"), "Entitlement was copied with existing name ");

		response = f.importFeatureToBranch(configToImport, entitlementID2, "ACT", null, "suffix1", false, sessionToken, srcBranchID);
		Assert.assertTrue(response.contains("illegalName"), "Entitlement was copied with existing name ");
		
		response = f.importFeatureToBranch(configToImport, entitlementID2, "ACT", null, "suffix1", true, sessionToken, srcBranchID);
		Assert.assertTrue(response.contains("newSubTreeId"), "Entitlement was not copied: " + response);
		
		JSONObject result = new JSONObject(response);
		String newEntitlement = purchasesApi.getPurchaseItemFromBranch(result.getString("newSubTreeId"), srcBranchID, sessionToken);
		Assert.assertTrue(f.jsonObjsAreEqual(new JSONObject(newEntitlement), new JSONObject(configToImport)));
	}
	
	@Test (dependsOnMethods="importConfigurationUnderEntitlement", description="Import mix configuration under mix entitlements in the same season.")
	public void importConfigurationUnderMixEntitlement() throws IOException{
		String entitlementMix = FileUtils.fileToString(filePath + "purchases/inAppPurchaseMutual.txt", "UTF-8", false);
		String mixId = purchasesApi.addPurchaseItemToBranch(seasonID, srcBranchID, entitlementMix, entitlementID2, sessionToken);
		Assert.assertFalse(mixId.contains("error"), "Entitlement mix was not added to the season" + mixId);
		
		String configToImport = purchasesApi.getPurchaseItemFromBranch(mixConfigID, srcBranchID, sessionToken);
		
		String response = f.importFeatureToBranch(configToImport, mixId, "ACT", null, "suffix2", true, sessionToken, srcBranchID);
		Assert.assertTrue(response.contains("error"), "Configuraiton mix was copied under entitlement mix ");
	}
	
	@Test (dependsOnMethods="importConfigurationUnderMixEntitlement", description="Import mix configuration under root in the same season.")
	public void importConfigurationUnderRoot() throws IOException{
		String rootId = purchasesApi.getBranchRootId(seasonID, srcBranchID, sessionToken);
		String configToImport = purchasesApi.getPurchaseItemFromBranch(mixConfigID, srcBranchID, sessionToken);
		
		String response = f.importFeatureToBranch(configToImport, rootId, "ACT", null, "suffix3", true, sessionToken, srcBranchID);
		Assert.assertTrue(response.contains("error"), "Configuraiton mix was copied under root");
	}
	
	
	@Test (dependsOnMethods="importConfigurationUnderRoot", description="Import mix configuration under itself in the same season.")
	public void importConfigurationUnderItself() throws IOException, JSONException{
		//should fail copy without suffix
		String configToImport = purchasesApi.getPurchaseItemFromBranch(mixConfigID, srcBranchID, sessionToken);
		
		String response = f.importFeatureToBranch(configToImport, mixConfigID, "ACT", null, null, true, sessionToken, srcBranchID);
		Assert.assertTrue(response.contains("illegalName"), "Configuraiton mix was copied with existing name ");

		response = f.importFeatureToBranch(configToImport, mixConfigID, "ACT", null, "suffix4", true, sessionToken, srcBranchID);
		Assert.assertTrue(response.contains("newSubTreeId"), "Configuration mix was not copied: " + response);
		
		JSONObject result = new JSONObject(response);
		String newEntitlement = purchasesApi.getPurchaseItemFromBranch(result.getString("newSubTreeId"), srcBranchID, sessionToken);
		JSONObject oldEntitlement = new JSONObject(purchasesApi.getPurchaseItemFromBranch(mixConfigID, srcBranchID, sessionToken));
		Assert.assertTrue(f.jsonObjsAreEqual(new JSONObject(newEntitlement), oldEntitlement.getJSONArray("configurationRules").getJSONObject(1)));
	}
	
	@Test (dependsOnMethods="importConfigurationUnderItself", description="Import mix configuration under configuration in the same entitlement in the same season.")
	public void importConfigurationUnderConfiguration() throws IOException, JSONException{
		String configuration = FileUtils.fileToString(filePath + "configuration_rule2.txt", "UTF-8", false);
		String configID2 = purchasesApi.addPurchaseItemToBranch(seasonID, srcBranchID, configuration, entitlementID1, sessionToken);
		Assert.assertFalse(configID.contains("error"), "cr was not added to the season");
			
		String configToImport = purchasesApi.getPurchaseItemFromBranch(mixConfigID, srcBranchID, sessionToken);
		
		//should fail copy without suffix
		String response = f.importFeatureToBranch(configToImport, configID2, "ACT", null, null, true, sessionToken, srcBranchID);
		Assert.assertTrue(response.contains("illegalName"), "Configuraiton mix was copied with existing name ");
		
		response = f.importFeatureToBranch(configToImport, configID2, "ACT", null, "suffix5", true, sessionToken, srcBranchID);
		Assert.assertTrue(response.contains("newSubTreeId"), "Configuraiton mix was not copied: " + response);
		
		JSONObject result = new JSONObject(response);
		String newEntitlement = purchasesApi.getPurchaseItemFromBranch(result.getString("newSubTreeId"), srcBranchID, sessionToken);
		Assert.assertTrue(f.jsonObjsAreEqual(new JSONObject(newEntitlement), new JSONObject(configToImport)));
	}

	@Test (dependsOnMethods="importConfigurationUnderConfiguration", description="Import mix configuration under mix configuration in the same season.")
	public void importSingleEntitlementUnderMixConfiguration() throws IOException, JSONException{
		String configuration = FileUtils.fileToString(filePath + "configuration_feature-mutual.txt", "UTF-8", false);
		String mixConfigID2 = purchasesApi.addPurchaseItemToBranch(seasonID, srcBranchID, configuration, entitlementID2, sessionToken);
		Assert.assertFalse(mixConfigID2.contains("error"), "cr mix was not added to the season");
		
		String configToImport = purchasesApi.getPurchaseItemFromBranch(mixConfigID, srcBranchID, sessionToken);
		
		String response = f.importFeatureToBranch(configToImport, mixConfigID2, "ACT", null, null, true, sessionToken, srcBranchID);
		Assert.assertTrue(response.contains("illegalName"), "Configuraiton mix was copied with existing name ");
		
		response = f.importFeatureToBranch(configToImport, mixConfigID2, "ACT", null, "suffix6", true, sessionToken, srcBranchID);
		Assert.assertTrue(response.contains("newSubTreeId"), "Configuration mix was not copied: " + response);
		
		JSONObject result = new JSONObject(response);
		String newEntitlement = purchasesApi.getPurchaseItemFromBranch(result.getString("newSubTreeId"), srcBranchID, sessionToken);
		Assert.assertTrue(f.jsonObjsAreEqual(new JSONObject(newEntitlement), new JSONObject(configToImport)));
	}
	
	
	@AfterTest
	private void reset(){
		baseUtils.reset(productID, sessionToken);
	}
}