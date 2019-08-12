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

public class ImportMIXEntitlementDifferentSeason {
	private String seasonID;
	private String seasonID2;
	private String productID;
	private String entitlementID1;
	private String mixID1;
	private String filePath;
	private String m_url;
	private ProductsRestApi p;
	private FeaturesRestApi f;
	private SeasonsRestApi s;
	private BranchesRestApi br ;
	private String sessionToken = "";
	private AirlockUtils baseUtils;
	private InAppPurchasesRestApi purchasesApi;
	
	//in new season
	private String rootId;
	private String entitlement3ID2Season2;
	private String mixEntitlementID2Season2;
	private String configIDSeason2;
	private String mixConfigIDSeason2;

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
	Mix entitlements under entitlement - allowed
	Mix entitlements under mix of entitlements - allowed
	Mix entitlements under config - not allowed
	Mix entitlements under mix config - not allowed
	Mix entitlements under root - allowed
		
	 */
	
	@Test (description="Create first season with 2 entitlement. Copy season")
	public void copySeason() throws Exception{
		String entitlementMix = FileUtils.fileToString(filePath + "purchases/inAppPurchaseMutual.txt", "UTF-8", false);
		mixID1 = purchasesApi.addPurchaseItemToBranch(seasonID, srcBranchID, entitlementMix, "ROOT", sessionToken);
		Assert.assertFalse(mixID1.contains("error"), "Entitlement mix was not added to the season: " + mixID1);
		
		String entitlement1 = FileUtils.fileToString(filePath + "purchases/inAppPurchase1.txt", "UTF-8", false);
		entitlementID1 = purchasesApi.addPurchaseItemToBranch(seasonID, srcBranchID, entitlement1, mixID1, sessionToken);
		Assert.assertFalse(entitlementID1.contains("error"), "entitlement was not added to the season");

		
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
	
	@Test(dependsOnMethods="copySeason", description="Create entitlements in new season. MIX will be imported under them")
	public void createEntitlementsInNewSeason() throws Exception{
		//get root in the new season:
	 	rootId = purchasesApi.getBranchRootId(seasonID2, destBranchID, sessionToken);
	 	
	 	JSONArray entitlements = purchasesApi.getPurchasesBySeasonFromBranch(seasonID2, destBranchID, sessionToken);
	 	
	 	mixEntitlementID2Season2 =entitlements.getJSONObject(0).getString("uniqueId");	 		
	 	
	 	//create new entitlements in the new season
		String entitlement3 = FileUtils.fileToString(filePath + "purchases/inAppPurchase3.txt", "UTF-8", false);
		entitlement3ID2Season2 = purchasesApi.addPurchaseItemToBranch(seasonID2, destBranchID, entitlement3, "ROOT", sessionToken);
		Assert.assertFalse(entitlement3ID2Season2.contains("error"), "Entitlement was not added to the season" + entitlement3ID2Season2);
		
		String mixConfiguration = FileUtils.fileToString(filePath + "configuration_feature-mutual.txt", "UTF-8", false);
		mixConfigIDSeason2 = purchasesApi.addPurchaseItemToBranch(seasonID2, destBranchID, mixConfiguration, entitlement3ID2Season2, sessionToken);
		Assert.assertFalse(mixConfigIDSeason2.contains("error"), "cr mix was not added to the season" + mixConfigIDSeason2);
		
		String configuration = FileUtils.fileToString(filePath + "configuration_rule1.txt", "UTF-8", false);
		configIDSeason2 = purchasesApi.addPurchaseItemToBranch(seasonID2, destBranchID, configuration, entitlement3ID2Season2, sessionToken);
		Assert.assertFalse(configIDSeason2.contains("error"), "cr was not added to the season" + configIDSeason2);
	}
	
	@Test (dependsOnMethods="createEntitlementsInNewSeason", description="Import mix entitlement under another entitlement in the second season. First, copy without namesuffix, then copy with namesuffix")
	public void importMixUnderEntitlement() throws IOException, JSONException{
		String entitlementToImport = purchasesApi.getPurchaseItemFromBranch(mixID1, srcBranchID, sessionToken);
		String response = f.importFeatureToBranch(entitlementToImport, entitlement3ID2Season2, "ACT", null, "suffix1", true, sessionToken, destBranchID);
		Assert.assertTrue(response.contains("newSubTreeId"), "entitlement was not imported: " + response);
		
		JSONObject result = new JSONObject(response);
		String newEntitlement = purchasesApi.getPurchaseItemFromBranch(result.getString("newSubTreeId"), destBranchID, sessionToken);
		Assert.assertTrue(f.jsonObjsAreEqual(new JSONObject(newEntitlement), new JSONObject(entitlementToImport)));
	}
	
	@Test (dependsOnMethods="importMixUnderEntitlement", description="Import mix entitlement under mix entitlement in the second season.")
	public void importMixUnderMixEntitlement() throws IOException, JSONException{
		String entitlementToImport = purchasesApi.getPurchaseItemFromBranch(mixID1, srcBranchID, sessionToken);
		
		//should fail copy without suffix
		String response = f.importFeatureToBranch(entitlementToImport, mixEntitlementID2Season2, "ACT", null, null, true, sessionToken, destBranchID);
		Assert.assertTrue(response.contains("illegalName"), "Entitlement mix was imported with existing name ");
		
		response = f.importFeatureToBranch(entitlementToImport, mixEntitlementID2Season2, "ACT", null, "suffix2", true, sessionToken, destBranchID);
		Assert.assertTrue(response.contains("newSubTreeId"), "Entitlement mix was not imported: " + response);
		
		JSONObject result = new JSONObject(response);
		String newEntitlement = purchasesApi.getPurchaseItemFromBranch(result.getString("newSubTreeId"), destBranchID, sessionToken);
		Assert.assertTrue(f.jsonObjsAreEqual(new JSONObject(newEntitlement), new JSONObject(entitlementToImport)));
	}
	
	@Test (dependsOnMethods="importMixUnderMixEntitlement", description="Import mix entitlement under root in the second season.")
	public void importMixUnderRoot() throws IOException, JSONException{
		String entitlementToImport = purchasesApi.getPurchaseItemFromBranch(mixID1, srcBranchID, sessionToken);
		
		//should fail copy without suffix
		String response = f.importFeatureToBranch(entitlementToImport, rootId, "ACT", null, null, true, sessionToken, destBranchID);
		Assert.assertTrue(response.contains("illegalName"), "Entitlement mix was imported with existing name ");

		
		response = f.importFeatureToBranch(entitlementToImport, rootId, "ACT", null, "suffix3", true, sessionToken, destBranchID);
		Assert.assertTrue(response.contains("newSubTreeId"), "Entitlement mix was not imported: " + response);
		
		JSONObject result = new JSONObject(response);
		String newEntitlement = purchasesApi.getPurchaseItemFromBranch(result.getString("newSubTreeId"), destBranchID, sessionToken);
		Assert.assertTrue(f.jsonObjsAreEqual(new JSONObject(newEntitlement), new JSONObject(entitlementToImport)));
	}	
	
	@Test (dependsOnMethods="importMixUnderRoot", description="Import mix entitlement under configuration in the second season.")
	public void importMixUnderConfiguration() throws IOException{
		String entitlementToImport = purchasesApi.getPurchaseItemFromBranch(mixID1, srcBranchID, sessionToken);
		
		String response = f.importFeatureToBranch(entitlementToImport, configIDSeason2, "ACT", null, "suffix4", true, sessionToken, destBranchID);
		Assert.assertTrue(response.contains("error"), "entitlement mix was imported under configuration " + response);
	}
	
	@Test (dependsOnMethods="importMixUnderConfiguration", description="Import mix entitlement under mix configuration in the second season.")
	public void importMixUnderMixConfiguration() throws IOException{
		String entitlementToImport = purchasesApi.getPurchaseItemFromBranch(mixID1, srcBranchID, sessionToken);
		String response = f.importFeatureToBranch(entitlementToImport, mixConfigIDSeason2, "ACT", null, "suffix5", true, sessionToken, destBranchID);
		Assert.assertTrue(response.contains("error"), "entitlement mix was imported under configuration " + response);
	}
	
	@AfterTest
	private void reset(){
		baseUtils.reset(productID, sessionToken);
	}

}