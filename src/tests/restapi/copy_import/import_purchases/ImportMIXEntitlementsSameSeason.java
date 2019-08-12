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

public class ImportMIXEntitlementsSameSeason {
	private String seasonID;
	private String productID;
	private String entitlementID1;
	private String entitlementID3;
	private String mixID1;
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
		Mix entitlements under entitlement - allowed
		Mix entitlements under mix of entitlements - allowed
		Mix entitlements under config - not allowed
		Mix entitlements under mix config - not allowed
		Mix entitlements under root - allowed
	 */
	
	@Test (description="Imort mix of entitlements under another entitlement in the same season.")
	public void importMixUnderEntitlement() throws IOException, JSONException{
		String entitlementMix = FileUtils.fileToString(filePath + "purchases/inAppPurchaseMutual.txt", "UTF-8", false);
		mixID1 = purchasesApi.addPurchaseItemToBranch(seasonID, srcBranchID, entitlementMix, "ROOT", sessionToken);
		Assert.assertFalse(mixID1.contains("error"), "Entitlement mix was not added to the season: " + mixID1);
		
		String entitlement1 = FileUtils.fileToString(filePath + "purchases/inAppPurchase1.txt", "UTF-8", false);
		entitlementID1 = purchasesApi.addPurchaseItemToBranch(seasonID, srcBranchID, entitlement1, mixID1, sessionToken);
		Assert.assertFalse(entitlementID1.contains("error"), "Entitlement was not added to the season");

		String entitlement3 = FileUtils.fileToString(filePath + "purchases/inAppPurchase3.txt", "UTF-8", false);
		entitlementID3 = purchasesApi.addPurchaseItemToBranch(seasonID, srcBranchID, entitlement3, "ROOT", sessionToken);
		Assert.assertFalse(entitlementID3.contains("error"), "Entitlement was not added to the season");

		//should fail import without suffix
		String entitlementToImport = purchasesApi.getPurchaseItemFromBranch(mixID1, srcBranchID, sessionToken);
		String response = f.importFeatureToBranch(entitlementToImport, entitlementID3, "ACT", null, null, true, sessionToken, srcBranchID);
		Assert.assertTrue(response.contains("illegalName"), "Entitlement mix was imported with existing name ");

		//should fail import without overrideids
		response = f.importFeatureToBranch(entitlementToImport, entitlementID3, "ACT", null, null, false, sessionToken, srcBranchID);
		Assert.assertTrue(response.contains("illegalId"), "Entitlement mic was imported with existing name ");
		
		response = f.importFeatureToBranch(entitlementToImport, entitlementID3, "ACT", null, "suffix1", true,  sessionToken, srcBranchID);
		Assert.assertTrue(response.contains("newSubTreeId"), "Entitlement mix was not copied: " + response);
		
		JSONObject result = new JSONObject(response);
		String newEntitlement = purchasesApi.getPurchaseItemFromBranch(result.getString("newSubTreeId"), srcBranchID, sessionToken);
		Assert.assertTrue(f.jsonObjsAreEqual(new JSONObject(newEntitlement), new JSONObject(entitlementToImport)));
	}
	
	@Test (dependsOnMethods="importMixUnderEntitlement", description="Import mix entitlements under mix entitlement in the same season.")
	public void importMixUnderMixEntitlement() throws IOException, JSONException{
		String entitlementMix = FileUtils.fileToString(filePath + "purchases/inAppPurchaseMutual.txt", "UTF-8", false);
		String mixId = purchasesApi.addPurchaseItemToBranch(seasonID, srcBranchID, entitlementMix, entitlementID3, sessionToken);
		Assert.assertFalse(mixId.contains("error"), "Entitlement mix was not added to the season" + mixId);
		
		String entitlementToImport = purchasesApi.getPurchaseItemFromBranch(mixID1, srcBranchID, sessionToken);
		
		//should fail copy without suffix
		String response = f.importFeatureToBranch(entitlementToImport, mixId, "ACT", null, null, true, sessionToken, srcBranchID);
		Assert.assertTrue(response.contains("illegalName"), "Entitlement mix was imported with existing name ");

		response = f.importFeatureToBranch(entitlementToImport, mixId, "ACT", null, null, false, sessionToken, srcBranchID);
		Assert.assertTrue(response.contains("illegalId"), "Entitlement mix was imported with existing name ");
		
		response = f.importFeatureToBranch(entitlementToImport, mixId, "ACT", null, "suffix2", true, sessionToken, srcBranchID);
		Assert.assertTrue(response.contains("newSubTreeId"), "Entitlement mix was not imported: " + response);
		
		JSONObject result = new JSONObject(response);
		String newEntitlement = purchasesApi.getPurchaseItemFromBranch(result.getString("newSubTreeId"), srcBranchID, sessionToken);
		Assert.assertTrue(f.jsonObjsAreEqual(new JSONObject(newEntitlement), new JSONObject(entitlementToImport)));
	}
	
	@Test (dependsOnMethods="importMixUnderMixEntitlement", description="Import mix entitlements under root in the same season. ")
	public void importMixUnderRoot() throws IOException, JSONException{
		String rootId = purchasesApi.getBranchRootId(seasonID, srcBranchID, sessionToken);
		String entitlementToImport = purchasesApi.getPurchaseItemFromBranch(mixID1, srcBranchID, sessionToken);
		
		//should fail copy without suffix
		String response = f.importFeatureToBranch(entitlementToImport, rootId, "ACT", null, null, true, sessionToken, srcBranchID);
		Assert.assertTrue(response.contains("illegalName"), "Entitlement mix was imported with existing name ");
		
		response = f.importFeatureToBranch(entitlementToImport, rootId, "ACT", null, "suffix3", true, sessionToken, srcBranchID);
		Assert.assertTrue(response.contains("newSubTreeId"), "Entitlement mix was not imported: " + response);
		
		JSONObject result = new JSONObject(response);
		String newEntitlement = purchasesApi.getPurchaseItemFromBranch(result.getString("newSubTreeId"), srcBranchID, sessionToken);
		Assert.assertTrue(f.jsonObjsAreEqual(new JSONObject(newEntitlement), new JSONObject(entitlementToImport)));
	}
		
	@Test (dependsOnMethods="importMixUnderRoot", description="Import mix entitlement under configuration in the same season.")
	public void importMixUnderConfiguration() throws IOException{
		String configuration = FileUtils.fileToString(filePath + "configuration_rule1.txt", "UTF-8", false);
		configID = purchasesApi.addPurchaseItemToBranch(seasonID, srcBranchID, configuration, entitlementID3, sessionToken);
		Assert.assertFalse(configID.contains("error"), "cr was not added to the season: " + configID);
		
		String entitlementToImport = purchasesApi.getPurchaseItemFromBranch(mixID1, srcBranchID, sessionToken);
		
		//should fail copy without suffix
		String response = f.importFeatureToBranch(entitlementToImport, configID, "ACT", null, "suffix4", true, sessionToken, srcBranchID);
		Assert.assertTrue(response.contains("error"), "Entitlement mix was imported under configuration " + response);
	}
	
	@Test (dependsOnMethods="importMixUnderConfiguration", description="Import mix entitlement under mix configuration in the same season.")
	public void importMixUnderMixConfiguration() throws IOException{
		String configuration = FileUtils.fileToString(filePath + "configuration_feature-mutual.txt", "UTF-8", false);
		String mixConfigID = purchasesApi.addPurchaseItemToBranch(seasonID, srcBranchID, configuration, entitlementID3, sessionToken);
		Assert.assertFalse(mixConfigID.contains("error"), "cr mix was not added to the season");
			
		String entitlementToImport = purchasesApi.getPurchaseItemFromBranch(mixID1, srcBranchID, sessionToken);
		
		//should fail copy without suffix
		String response = f.importFeatureToBranch(entitlementToImport, mixConfigID, "ACT", null, "suffix5", true, sessionToken, srcBranchID);
		Assert.assertTrue(response.contains("error"), "Entitlement mix was imported under configuration " + response);
	}
	
	@Test (dependsOnMethods="importMixUnderMixConfiguration", description="Import mix entitlement under itself in the same season.")
	public void importMixUnderItself() throws IOException, JSONException{
		String entitlementToImport = purchasesApi.getPurchaseItemFromBranch(mixID1, srcBranchID, sessionToken);
		
		//should fail copy without suffix
		String response = f.importFeatureToBranch(entitlementToImport, mixID1, "ACT", null, null, true, sessionToken, srcBranchID);
		Assert.assertTrue(response.contains("illegalName"), "Entitlement mix was copied with existing name ");
		
		response = f.importFeatureToBranch(entitlementToImport, mixID1, "ACT", null, "suffix6", true, sessionToken, srcBranchID);
		Assert.assertTrue(response.contains("newSubTreeId"), "Entitlement mix was not copied: " + response);
		
		JSONObject result = new JSONObject(response);
		String newEntitlement =  purchasesApi.getPurchaseItemFromBranch(result.getString("newSubTreeId"), srcBranchID, sessionToken);
		Assert.assertTrue(f.jsonObjsAreEqual(new JSONObject(newEntitlement), new JSONObject(entitlementToImport)));
	}
	
	@AfterTest
	private void reset(){
		baseUtils.reset(productID, sessionToken);
	}
}