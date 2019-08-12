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

public class ImportMIXEntitlementDifferentProduct {
	private String seasonID;
	private String seasonID2;
	private String productID;
	private String productID2;
	private String entitlementMixToCopyId;
	private String entitlementID2;
	private String mixId;
	private String configID;
	private String mixConfigID;
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
	  	Entitlement under entitlement - allowed
		Entitlement under mix of entitlements - allowed
		Entitlement under root - allowed
		Entitlement under config - not allowed
		Entitlement under mix config - not allowed
		
	 */
	
	@Test (description="Create first season with 1 entitlement")
	public void addEntitlementToBranch() throws IOException, JSONException{
		//this entitlement will be copied  to the new product
		String entitlementMix = FileUtils.fileToString(filePath + "purchases/inAppPurchaseMutual.txt", "UTF-8", false);
		entitlementMixToCopyId = purchasesApi.addPurchaseItemToBranch(seasonID, srcBranchID, entitlementMix, "ROOT", sessionToken);
		Assert.assertFalse(entitlementMixToCopyId.contains("error"), "Entitlement mix was not added to the season: " + entitlementMixToCopyId);
		
		String entitlement1 = FileUtils.fileToString(filePath + "purchases/inAppPurchase1.txt", "UTF-8", false);
		String entitlementID1 = purchasesApi.addPurchaseItemToBranch(seasonID, srcBranchID, entitlement1, entitlementMixToCopyId, sessionToken);
		Assert.assertFalse(entitlementID1.contains("error"), "Entitlement was not added to the season");	
	}
	
	@Test (dependsOnMethods="addEntitlementToBranch", description="Create new product with all components")
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
		Assert.assertFalse(entitlementID2.contains("error"), "Entitlement was not added to the season");
		
		String entitlementMix = FileUtils.fileToString(filePath + "purchases/inAppPurchaseMutual.txt", "UTF-8", false);
		mixId = purchasesApi.addPurchaseItemToBranch(seasonID2, destBranchID, entitlementMix, "ROOT", sessionToken);
		Assert.assertFalse(mixId.contains("error"), "Entitlement mix was not added to the season" + mixId);

		String configuration = FileUtils.fileToString(filePath + "configuration_rule1.txt", "UTF-8", false);
		configID = purchasesApi.addPurchaseItemToBranch(seasonID2, destBranchID, configuration, entitlementID2, sessionToken);
		Assert.assertFalse(configID.contains("error"), "cr was not added to the season");
		
		String mixConfiguration = FileUtils.fileToString(filePath + "configuration_feature-mutual.txt", "UTF-8", false);
		mixConfigID = purchasesApi.addPurchaseItemToBranch(seasonID2, destBranchID, mixConfiguration, entitlementID2, sessionToken);
		Assert.assertFalse(mixConfigID.contains("error"), "cr mix was not added to the season");
	}
	
	@Test (dependsOnMethods="createNewProduct", description="Import mix entitlements under another entitlement in the new products.")
	public void importMixUnderEntitlement() throws IOException, JSONException{
		String entitlementToImport = purchasesApi.getPurchaseItemFromBranch(entitlementMixToCopyId, srcBranchID, sessionToken);
		
		//should fail copy without suffix
		String response = f.importFeatureToBranch(entitlementToImport, entitlementID2, "ACT", null, null, true, sessionToken, destBranchID);
		Assert.assertTrue(response.contains("illegalName"), "Entitlement was imported with existing name ");

		response = f.importFeatureToBranch(entitlementToImport, entitlementID2, "ACT", null, null, false, sessionToken, destBranchID);
		Assert.assertTrue(response.contains("illegalId"), "Entitlement was imported with existing name ");
		
		response = f.importFeatureToBranch(entitlementToImport, entitlementID2, "ACT", null, "suffix1", true, sessionToken, destBranchID);
		Assert.assertTrue(response.contains("newSubTreeId"), "Entitlement was not imported: " + response);
		
		JSONObject result = new JSONObject(response);
		String newEntitlement = purchasesApi.getPurchaseItemFromBranch(result.getString("newSubTreeId"), destBranchID, sessionToken);
		Assert.assertTrue(f.jsonObjsAreEqual(new JSONObject(newEntitlement), new JSONObject(entitlementToImport)));
	}	
	
	@Test (dependsOnMethods="importMixUnderEntitlement", description="Import mix entitlement under mix entitlement in the new products.")
	public void importMixUnderMixEntitlement() throws IOException, JSONException{
		String entitlementToImport = purchasesApi.getPurchaseItemFromBranch(entitlementMixToCopyId, srcBranchID, sessionToken);
		//should fail copy without suffix
		String response = f.importFeatureToBranch(entitlementToImport, mixId, "ACT", null, null, true, sessionToken, destBranchID);
		Assert.assertTrue(response.contains("illegalName"), "Entitlement was imported with existing name ");
		
		response = f.importFeatureToBranch(entitlementToImport, mixId, "ACT", null, "suffix2", true, sessionToken, destBranchID);
		Assert.assertTrue(response.contains("newSubTreeId"), "Entitlement was not imported: " + response);
		
		JSONObject result = new JSONObject(response);
		String newEntitlement = purchasesApi.getPurchaseItemFromBranch(result.getString("newSubTreeId"), destBranchID, sessionToken);
		Assert.assertTrue(f.jsonObjsAreEqual(new JSONObject(newEntitlement), new JSONObject(entitlementToImport)));
	}
	
	@Test (dependsOnMethods="importMixUnderMixEntitlement", description="Copy mix entitlement under root in the new products. First, copy without namesuffix, then copy with namesuffix")
	public void importMixUnderRoot() throws IOException, JSONException{
		String rootId = purchasesApi.getBranchRootId(seasonID2, "MASTER", sessionToken);
		String entitlementToImport = purchasesApi.getPurchaseItemFromBranch(entitlementMixToCopyId, srcBranchID, sessionToken);
		
		//should fail copy without suffix
		String response = f.importFeatureToBranch(entitlementToImport, rootId, "ACT", null, null, true, sessionToken, destBranchID);
		Assert.assertTrue(response.contains("illegalName"), "Entitlement mix was imported with existing name ");

		response = f.importFeatureToBranch(entitlementToImport, rootId, "ACT", null, "suffix3", true, sessionToken, destBranchID);
		Assert.assertTrue(response.contains("newSubTreeId"), "Entitlement mix was not imported: " + response);
		
		JSONObject result = new JSONObject(response);
		String newEntitlement = purchasesApi.getPurchaseItemFromBranch(result.getString("newSubTreeId"), destBranchID, sessionToken);
		Assert.assertTrue(f.jsonObjsAreEqual(new JSONObject(newEntitlement), new JSONObject(entitlementToImport)));
	}
	
	@Test (dependsOnMethods="importMixUnderRoot", description="Import mix entitlement under configuration in the new product.")
	public void importMixUnderConfiguration() throws IOException{
		String entitlementToImport = purchasesApi.getPurchaseItemFromBranch(entitlementMixToCopyId, srcBranchID, sessionToken);
		
		String response = f.importFeatureToBranch(entitlementToImport, configID, "ACT", null, "suffix4", true, sessionToken, destBranchID);
		Assert.assertTrue(response.contains("error"), "Entitlement mix was imported under configuration " + response);
	}
	
	@Test (dependsOnMethods="importMixUnderConfiguration", description="Import mix entitlement under mix configuration in the new products.")
	public void importMixMixConfiguration() throws IOException{
		String entitlementToImport = purchasesApi.getPurchaseItemFromBranch(entitlementMixToCopyId, srcBranchID, sessionToken);
		String response = f.importFeatureToBranch(entitlementToImport, mixConfigID, "ACT", null, "suffix5", true, sessionToken, destBranchID);
		Assert.assertTrue(response.contains("error"), "Entitlement mix was imported under configuration " + response);
	}
	
	@AfterTest
	private void reset(){
		baseUtils.reset(productID, sessionToken);
		baseUtils.reset(productID2, sessionToken);
	}

}