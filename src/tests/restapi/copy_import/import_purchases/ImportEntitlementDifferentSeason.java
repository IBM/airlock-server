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

public class ImportEntitlementDifferentSeason {
	private String seasonID;
	private String seasonID2;
	private String productID;
	private String entitlementID1;
	private String filePath;
	private String m_url;
	private ProductsRestApi p;
	private FeaturesRestApi f;
	private BranchesRestApi br ;
	private SeasonsRestApi s;
	private String sessionToken = "";
	private AirlockUtils baseUtils;
	private InAppPurchasesRestApi purchasesApi;

	//in new season
	private String rootId;
	private String entitlementID2Season2;
	private String mixEntitlementID2Season2;
	private String configIDSeason2;
	private String mixConfigIDSeason2;

	private boolean runOnMaster;
	private String srcBranchID;
	private String destBranchID;

	@BeforeClass
	@Parameters({"url", "analyticsUrl", "translationsUrl", "configPath", "sessionToken", "userName", "userPassword", "appName", "productsToDeleteFile", "runOnMaster"})
	public void init(String url, String analyticsUrl, String translationsUrl, String configPath, String sToken, String userName, String userPassword, String appName, String productsToDeleteFile, Boolean onMaster) throws Exception{
		m_url = url;
		filePath = configPath;
		p = new ProductsRestApi();
		p.setURL(m_url);
		f = new FeaturesRestApi();
		f.setURL(m_url);
		s = new SeasonsRestApi();
		s.setURL(m_url);
		runOnMaster = onMaster;
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
	}

	/*
	  	Entitlement under entitlement - allowed
		Entitlement under mix of entitlements - allowed
		Entitlement under root - allowed
		Entitlement under config - not allowed
		Entitlement under mix config - not allowed

	 */

	@Test (description="Create first season with 2 entitlement. Copy season")
	public void copySeason() throws Exception{
		String entitlement2 = FileUtils.fileToString(filePath + "purchases/inAppPurchase2.txt", "UTF-8", false);
		String entitlementID2 = purchasesApi.addPurchaseItemToBranch(seasonID, srcBranchID, entitlement2, "ROOT", sessionToken);
		Assert.assertFalse(entitlementID2.contains("error"), "Entitlement was not added to the season");

		String entitlementMix = FileUtils.fileToString(filePath + "purchases/inAppPurchaseMutual.txt", "UTF-8", false);
		String mixId = purchasesApi.addPurchaseItemToBranch(seasonID, srcBranchID, entitlementMix, "ROOT", sessionToken);
		Assert.assertFalse(mixId.contains("error"), "Entitlement mix was not added to the season" + mixId);

		String configuration = FileUtils.fileToString(filePath + "configuration_rule1.txt", "UTF-8", false);
		String configID = purchasesApi.addPurchaseItemToBranch(seasonID, srcBranchID, configuration, entitlementID2, sessionToken);
		Assert.assertFalse(configID.contains("error"), "cr was not added to the season");

		String mixConfiguration = FileUtils.fileToString(filePath + "configuration_feature-mutual.txt", "UTF-8", false);
		String mixConfigID = purchasesApi.addPurchaseItemToBranch(seasonID, srcBranchID, mixConfiguration, entitlementID2, sessionToken);
		Assert.assertFalse(mixConfigID.contains("error"), "cr mix was not added to the season");

		//this entitlement will be copied from the first season to the second
		String entitlement1 = FileUtils.fileToString(filePath + "purchases/inAppPurchase1.txt", "UTF-8", false);
		entitlementID1 = purchasesApi.addPurchaseItemToBranch(seasonID, srcBranchID, entitlement1, "ROOT", sessionToken);
		Assert.assertFalse(entitlementID1.contains("error"), "Entitlement was not added to the season");

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
	public void getnewEntitlementsIds() throws Exception{
		rootId = purchasesApi.getBranchRootId(seasonID2, destBranchID, sessionToken);

		JSONArray entitlements = purchasesApi.getPurchasesBySeasonFromBranch(seasonID2, destBranchID, sessionToken);

		entitlementID2Season2 = entitlements.getJSONObject(0).getString("uniqueId");
		mixEntitlementID2Season2 =entitlements.getJSONObject(1).getString("uniqueId");

		JSONArray configurations = entitlements.getJSONObject(0).getJSONArray("configurationRules");
		for (Object el: configurations){
			JSONObject config = new JSONObject(el);
			if (config.getString("type").equals("CONFIGURATION_RULE"))
				configIDSeason2 = config.getString("uniqueId");
			else if (config.getString("type").equals("CONFIG_MUTUAL_EXCLUSION_GROUP"))
				mixConfigIDSeason2 = config.getString("uniqueId");
		}
	}

	@Test (dependsOnMethods="getnewEntitlementsIds", description="Import single entitlement under another entitlement in the second season. ")
	public void importSingleEntitlementUnderEntitlement() throws IOException, JSONException{
		String entitlementToImport = purchasesApi.getPurchaseItemFromBranch(entitlementID1, srcBranchID, sessionToken);

		//should fail copy without suffix
		String response = f.importFeatureToBranch(entitlementToImport, entitlementID2Season2, "ACT", null, null, true, sessionToken, destBranchID);
		Assert.assertTrue(response.contains("illegalName"), "Entitlement was imported with existing name ");

		//should fail copy without overrideids
		response = f.importFeatureToBranch(entitlementToImport, entitlementID2Season2, "ACT", null, "suffix1", false, sessionToken, destBranchID);
		Assert.assertTrue(response.contains("illegalName"), "Entitlement was imported with existing name ");

		response = f.importFeatureToBranch(entitlementToImport, entitlementID2Season2, "ACT", null, "suffix1", true, sessionToken, destBranchID);
		Assert.assertTrue(response.contains("newSubTreeId"), "Entitlement was not imported: " + response);

		JSONObject result = new JSONObject(response);
		String newEntitlement = purchasesApi.getPurchaseItemFromBranch(result.getString("newSubTreeId"), destBranchID, sessionToken);
		Assert.assertTrue(f.jsonObjsAreEqual(new JSONObject(newEntitlement), new JSONObject(entitlementToImport)));
	}


	@Test (dependsOnMethods="importSingleEntitlementUnderEntitlement", description="Import single entitlement under mix entitlement in the second season.")
	public void importSingleEntitlementUnderMixEntitlement() throws IOException, JSONException{
		String entitlementToImport = purchasesApi.getPurchaseItemFromBranch(entitlementID1, srcBranchID, sessionToken);

		//should fail copy without suffix
		String response = f.importFeatureToBranch(entitlementToImport, mixEntitlementID2Season2, "ACT", null, null, true,  sessionToken, destBranchID);
		Assert.assertTrue(response.contains("illegalName"), "Entitlement was copied with existing name ");

		response = f.importFeatureToBranch(entitlementToImport, mixEntitlementID2Season2, "ACT", null, "suffix2", true, sessionToken, destBranchID);
		Assert.assertTrue(response.contains("newSubTreeId"), "Entitlement was not copied: " + response);

		JSONObject result = new JSONObject(response);
		String newEntitlement = purchasesApi.getPurchaseItemFromBranch(result.getString("newSubTreeId"), destBranchID, sessionToken);
		Assert.assertTrue(f.jsonObjsAreEqual(new JSONObject(newEntitlement), new JSONObject(entitlementToImport)));
	}

	@Test (dependsOnMethods="importSingleEntitlementUnderMixEntitlement", description="Import single Entitlement under root in the second season.")
	public void importSingleEntitlementUnderRoot() throws IOException, JSONException{
		String entitlementToImport = purchasesApi.getPurchaseItemFromBranch(entitlementID1, srcBranchID, sessionToken);

		//should fail copy without suffix
		String response = f.importFeatureToBranch(entitlementToImport, rootId, "ACT", null, null, true, sessionToken, destBranchID);
		Assert.assertTrue(response.contains("illegalName"), "Entitlement was imported with existing name ");

		response = f.importFeatureToBranch(entitlementToImport, rootId, "ACT", null, "suffix3", true, sessionToken, destBranchID);
		Assert.assertTrue(response.contains("newSubTreeId"), "Entitlement was not imported: " + response);

		JSONObject result = new JSONObject(response);
		String newEntitlement = purchasesApi.getPurchaseItemFromBranch(result.getString("newSubTreeId"), destBranchID, sessionToken);
		Assert.assertTrue(f.jsonObjsAreEqual(new JSONObject(newEntitlement), new JSONObject(entitlementToImport)));
	}

	@Test (dependsOnMethods="importSingleEntitlementUnderRoot", description="Import single entitlement under configuration in the second season.")
	public void importSingleEntitlementUnderConfiguration() throws IOException{
		String entitlementToImport = purchasesApi.getPurchaseItemFromBranch(entitlementID1, srcBranchID, sessionToken);

		//should fail copy without suffix
		String response = f.importFeatureToBranch(entitlementToImport, configIDSeason2, "ACT", null, null, true, sessionToken, destBranchID);
		Assert.assertTrue(response.contains("illegalName"), "Entitlement was imported with existing name ");

		response = f.importFeatureToBranch(entitlementToImport, configIDSeason2, "ACT", null, "suffix4", true, sessionToken, destBranchID);
		Assert.assertTrue(response.contains("error"), "Entitlement was imported under configuration " + response);
	}

	@Test (dependsOnMethods="importSingleEntitlementUnderConfiguration", description="Import single entitlement under mix configuration in the second season.")
	public void importSingleEntitlementUnderMixConfiguration() throws IOException{
		String entitlementToImport = purchasesApi.getPurchaseItemFromBranch(entitlementID1, srcBranchID, sessionToken);

		String response = f.importFeatureToBranch(entitlementToImport, mixConfigIDSeason2, "ACT", null, "suffix5", true, sessionToken, destBranchID);
		Assert.assertTrue(response.contains("error"), "Entitlement was imported under configuration " + response);
	}

	@AfterTest
	private void reset(){
		baseUtils.reset(productID, sessionToken);
	}

}