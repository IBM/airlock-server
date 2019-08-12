package tests.restapi.copy_import.import_purchases;

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

public class ImportConfigurationDifferentSeason {
	private String seasonID;
	private String seasonID2;
	private String productID;
	private String entitlementID2;
	private String configID;
	private String filePath;
	private String m_url;
	private ProductsRestApi p;
	private FeaturesRestApi f;
	private SeasonsRestApi s;
	private BranchesRestApi br ;
	private String sessionToken = "";
	private AirlockUtils baseUtils;
	private String configIDSeason2;
	private boolean runOnMaster;
	private String srcBranchID;
	private String destBranchID;
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
		br = new BranchesRestApi();
		br.setURL(url);
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
	public void copySeason() throws Exception{
		String entitlement = FileUtils.fileToString(filePath + "purchases/inAppPurchase1.txt", "UTF-8", false);
		String entitlementID1 = purchasesApi.addPurchaseItemToBranch(seasonID, srcBranchID, entitlement, "ROOT", sessionToken);
		Assert.assertFalse(entitlementID1.contains("error"), "entitlement was not added to the season");
		
		String configuration = FileUtils.fileToString(filePath + "configuration_rule1.txt", "UTF-8", false);
		configID = purchasesApi.addPurchaseItemToBranch(seasonID, srcBranchID, configuration, entitlementID1, sessionToken);
		Assert.assertFalse(configID.contains("error"), "cr was not added to the season");
		
		JSONObject sJson = new JSONObject();
		sJson.put("minVersion", "2.0");
		
		seasonID2 = s.addSeason(productID, sJson.toString(), sessionToken);
		Assert.assertFalse(seasonID2.contains("error"), "The second season was not created: " + seasonID2);
		
		if (runOnMaster) {
			destBranchID = BranchesRestApi.MASTER;
		}
		else {
			String allBranches = br.getAllBranches(seasonID2,sessionToken);
			JSONObject jsonBranch = new JSONObject(allBranches);
			destBranchID = jsonBranch.getJSONArray("branches").getJSONObject(1).getString("uniqueId");
		}
	}
	
	@Test(dependsOnMethods="copySeason", description="Parse new season ids")
	public void getNewEntitlementsIds() throws Exception{
		 JSONArray entitlements = purchasesApi.getPurchasesBySeasonFromBranch(seasonID2, destBranchID, sessionToken);
		 configIDSeason2 = 	entitlements.getJSONObject(0).getJSONArray("configurationRules").getJSONObject(0).getString("uniqueId"); 	
	}
	
	
	@Test (dependsOnMethods="getNewEntitlementsIds", description="Import configuration under entitlement in the new season.")
	public void importConfigurationUnderEntitlement() throws IOException, JSONException{
		String entitlement = FileUtils.fileToString(filePath + "purchases/inAppPurchase2.txt", "UTF-8", false);
		String entitlementID = purchasesApi.addPurchaseItemToBranch(seasonID2, destBranchID, entitlement, "ROOT", sessionToken);
		Assert.assertFalse(entitlementID.contains("error"), "entitlement was not added to season" + entitlementID);

		String configToImport = purchasesApi.getPurchaseItemFromBranch(configID, srcBranchID, sessionToken);
		
		//should fail copy without suffix
		String response = f.importFeatureToBranch(configToImport, entitlementID, "ACT", null, null, true, sessionToken, destBranchID);
		Assert.assertTrue(response.contains("illegalName"), "Configuration was imported with existing name ");
		
		response = f.importFeatureToBranch(configToImport, entitlementID, "ACT", null, "suffix1", false, sessionToken, destBranchID);
		Assert.assertTrue(response.contains("illegalName"), "Configuration was imported with existing name ");

		response = f.importFeatureToBranch(configToImport, entitlementID, "ACT", null, "suffix1", true, sessionToken, destBranchID);
		Assert.assertTrue(response.contains("newSubTreeId"), "Configuration was not imported: " + response);
		
		JSONObject result = new JSONObject(response);
		String newEntitlement = purchasesApi.getPurchaseItemFromBranch(result.getString("newSubTreeId"), destBranchID, sessionToken);
		String oldEntitlement = purchasesApi.getPurchaseItemFromBranch(configID, srcBranchID, sessionToken);
		Assert.assertTrue(f.jsonObjsAreEqual(new JSONObject(newEntitlement), new JSONObject(oldEntitlement)));
	}
	
	@Test (dependsOnMethods="importConfigurationUnderEntitlement", description="Import configuration under mix entitlement in the new season.")
	public void importConfigurationUnderMixEntitlement() throws IOException{
		String entitlementMix = FileUtils.fileToString(filePath + "purchases/inAppPurchaseMutual.txt", "UTF-8", false);
		String mixId = purchasesApi.addPurchaseItemToBranch(seasonID2, destBranchID, entitlementMix, "ROOT", sessionToken);
		Assert.assertFalse(mixId.contains("error"), "Entitlement mix was not added to the season" + mixId);
		
		String configToImport = purchasesApi.getPurchaseItemFromBranch(configID, srcBranchID, sessionToken);

		String response = f.importFeatureToBranch(configToImport, mixId, "ACT", null, "suffix2", true, sessionToken, destBranchID);
		Assert.assertTrue(response.contains("error"), "Configuraiton was imported under entitlements mix ");
	}
	
	@Test (dependsOnMethods="importConfigurationUnderMixEntitlement", description="Import configuration under root in the new season.")
	public void importConfigurationUnderRoot() throws IOException{
		String rootId = f.getBranchRootId(seasonID2, destBranchID, sessionToken);
		String configToImport = purchasesApi.getPurchaseItemFromBranch(configID, srcBranchID, sessionToken);
		
		String response = f.importFeatureToBranch(configToImport, rootId, "ACT", null, "suffix3", true, sessionToken, destBranchID);
		Assert.assertTrue(response.contains("error"), "Configuration was imported under root ");
	}
	
	
	@Test (dependsOnMethods="importConfigurationUnderRoot", description="Import configuration under itself in the new season.")
	public void importConfigurationUnderItself() throws IOException, JSONException{
		//should fail copy without suffix
		String configToImport = purchasesApi.getPurchaseItemFromBranch(configID, srcBranchID, sessionToken);
		
		String response = f.importFeatureToBranch(configToImport, configIDSeason2, "ACT", null, null, true, sessionToken, destBranchID);
		Assert.assertTrue(response.contains("illegalName"), "Configuration was imported with existing name  ");
		
		response = f.importFeatureToBranch(configToImport, configIDSeason2, "ACT", null, "suffix4", true, sessionToken, destBranchID);
		Assert.assertTrue(response.contains("newSubTreeId"), "Configuration was not imported: " + response);
		
		JSONObject result = new JSONObject(response);
		String newEntitlement = purchasesApi.getPurchaseItemFromBranch(result.getString("newSubTreeId"), destBranchID, sessionToken);
		JSONObject oldEntitlement = new JSONObject(purchasesApi.getPurchaseItemFromBranch(configIDSeason2, destBranchID, sessionToken));
		Assert.assertTrue(f.jsonObjsAreEqual(new JSONObject(newEntitlement), oldEntitlement.getJSONArray("configurationRules").getJSONObject(0)));
	}
	
	@Test (dependsOnMethods="importConfigurationUnderItself", description="Import configuration under configuration in the new season.")
	public void importConfigurationUnderConfiguration() throws IOException, JSONException{
		String entitlement = FileUtils.fileToString(filePath + "purchases/inAppPurchase3.txt", "UTF-8", false);
		entitlementID2 = purchasesApi.addPurchaseItemToBranch(seasonID2, destBranchID, entitlement, "ROOT", sessionToken);
		Assert.assertFalse(entitlementID2.contains("error"), "entitlement was not added to season" + entitlementID2);
		
		String configuration = FileUtils.fileToString(filePath + "configuration_rule2.txt", "UTF-8", false);
		String configID2 = purchasesApi.addPurchaseItemToBranch(seasonID2, destBranchID, configuration, entitlementID2, sessionToken);
		Assert.assertFalse(configID.contains("error"), "cr was not added to the season");

		String configToImport = purchasesApi.getPurchaseItemFromBranch(configID, srcBranchID, sessionToken);
		
		String response = f.importFeatureToBranch(configToImport, configID2, "ACT", null, null, true, sessionToken, destBranchID);
		Assert.assertTrue(response.contains("illegalName"), "Configuration was imported with existing name  ");

		response = f.importFeatureToBranch(configToImport, configID2, "ACT", null, "suffix5", true, sessionToken, destBranchID);
		Assert.assertTrue(response.contains("newSubTreeId"), "Configuration was not imported: " + response);
		
		JSONObject result = new JSONObject(response);
		String newEntitlement = purchasesApi.getPurchaseItemFromBranch(result.getString("newSubTreeId"), destBranchID, sessionToken);
		Assert.assertTrue(f.jsonObjsAreEqual(new JSONObject(newEntitlement), new JSONObject(configToImport)));
	}
	
	@Test (dependsOnMethods="importConfigurationUnderConfiguration", description="Import configuration under mix configuration in the new season.")
	public void importConfigurationUnderMixConfiguration() throws IOException, JSONException{
		String configuration = FileUtils.fileToString(filePath + "configuration_feature-mutual.txt", "UTF-8", false);
		String mixConfigID = purchasesApi.addPurchaseItemToBranch(seasonID2, destBranchID, configuration, entitlementID2, sessionToken);
		Assert.assertFalse(mixConfigID.contains("error"), "cr mix was not added to the season");
		
		String configToImport = purchasesApi.getPurchaseItemFromBranch(configID, srcBranchID, sessionToken);
		
		String response = f.importFeatureToBranch(configToImport, mixConfigID, "ACT", null, null, true, sessionToken, destBranchID);
		Assert.assertTrue(response.contains("illegalName"), "Configuration was imported with existing name  ");

		response = f.importFeatureToBranch(configToImport, mixConfigID, "ACT", null, "suffix6", true, sessionToken, destBranchID);
		Assert.assertTrue(response.contains("newSubTreeId"), "Configuration was not imported: " + response);
		
		JSONObject result = new JSONObject(response);
		String newEntitlement = purchasesApi.getPurchaseItemFromBranch(result.getString("newSubTreeId"), destBranchID, sessionToken);
		Assert.assertTrue(f.jsonObjsAreEqual(new JSONObject(newEntitlement), new JSONObject(configToImport)));
	}
	
	@AfterTest
	private void reset(){
		baseUtils.reset(productID, sessionToken);
	}
}