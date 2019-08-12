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

public class CopyPurchaseOptionsDifferentSeason {
	private String seasonID;
	private String seasonID2;
	private String productID;
	private String entitlementID1;
	private String entitlementID2;
	private String purchaseOptionsID1;
	private String purchaseOptionsID2;
	private String purchaseOptionsMixID;
	private String filePath;
	private String m_url;
	private ProductsRestApi p;
	private InAppPurchasesRestApi purchasesApi;
	private SeasonsRestApi s;
	private BranchesRestApi br ;
	private FeaturesRestApi f;
	private String sessionToken = "";
	private AirlockUtils baseUtils;
	
	//in new season
	private String rootId;
	private String entitlement2ID2Season2;
	private String mixentitlementID2Season2;
	private String purchaseOptionsID1Season2;
	private String purchaseOptionsID2Season2;
	private String mixPurchaseOptionsID1Season2;
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
		purchasesApi = new InAppPurchasesRestApi();
		purchasesApi.setURL(m_url);
		s = new SeasonsRestApi();
		s.setURL(m_url);
		br = new BranchesRestApi();
		br.setURL(url);
		f = new FeaturesRestApi();
		f.setURL(m_url);
		
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
	  	purchaseOptions under entitlement - allowed
		purchaseOptions under mix of purchaseOptions - allowed
		purchaseOptions under root - not allowed
		purchaseOptions under config - not allowed
		purchaseOptions under mix config - not allowed
		purchaseOptions under mix entitlement - not allowed
		purchaseOptions under purchaseOptions - not allowed
	 */
	
	//  E1 - > PO1 -> CR1, CR_MIX
	//  E2 - > PO2, PO_MTX
	//  E_MIX
	@Test (description="Create first season with 2 entitlements and 2 purchaseOptions. Copy season")
	public void copySeason() throws Exception{
		String entitlement1 = FileUtils.fileToString(filePath + "purchases/inAppPurchase1.txt", "UTF-8", false);
		entitlementID1 = purchasesApi.addPurchaseItemToBranch(seasonID, srcBranchID, entitlement1, "ROOT", sessionToken);
		Assert.assertFalse(entitlementID1.contains("error"), "entitlement was not added to the season");

		String entitlement2 = FileUtils.fileToString(filePath + "purchases/inAppPurchase2.txt", "UTF-8", false);
		entitlementID2 = purchasesApi.addPurchaseItemToBranch(seasonID, srcBranchID, entitlement2, "ROOT", sessionToken);
		Assert.assertFalse(entitlementID2.contains("error"), "entitlement was not added to the season");

		//this purchaseOptions will be copied from the first season to the second
		String purchaseOptions1 = FileUtils.fileToString(filePath + "purchases/purchaseOptions1.txt", "UTF-8", false);
		purchaseOptionsID1 = purchasesApi.addPurchaseItemToBranch(seasonID, srcBranchID, purchaseOptions1, entitlementID1, sessionToken);
		Assert.assertFalse(entitlementID1.contains("error"), "purchaseOptions1 was not added to the season");

		String purchaseOptions2 = FileUtils.fileToString(filePath + "purchases/purchaseOptions2.txt", "UTF-8", false);
		purchaseOptionsID2 = purchasesApi.addPurchaseItemToBranch(seasonID, srcBranchID, purchaseOptions2, entitlementID2, sessionToken);
		Assert.assertFalse(purchaseOptionsID2.contains("error"), "purchaseOptions2 was not added to the season : " + purchaseOptionsID2);

		String purchaseOptionsMix = FileUtils.fileToString(filePath + "purchases/purchaseOptionsMutual.txt", "UTF-8", false);
		purchaseOptionsMixID = purchasesApi.addPurchaseItemToBranch(seasonID, srcBranchID, purchaseOptionsMix, entitlementID2, sessionToken);
		Assert.assertFalse(purchaseOptionsMixID.contains("error"), "purchaseOptionsMixID was not added to the season");

		String entitlementMix = FileUtils.fileToString(filePath + "purchases/inAppPurchaseMutual.txt", "UTF-8", false);
		String mixId = purchasesApi.addPurchaseItemToBranch(seasonID, srcBranchID, entitlementMix, "ROOT", sessionToken);
		Assert.assertFalse(mixId.contains("error"), "Feature was not added to the season" + mixId);

		String configuration = FileUtils.fileToString(filePath + "configuration_rule1.txt", "UTF-8", false);
		String configID = purchasesApi.addPurchaseItemToBranch(seasonID, srcBranchID, configuration, entitlementID1, sessionToken);
		Assert.assertFalse(configID.contains("error"), "Feature was not added to the season");
		
		String mixConfiguration = FileUtils.fileToString(filePath + "configuration_feature-mutual.txt", "UTF-8", false);
		String mixConfigID = purchasesApi.addPurchaseItemToBranch(seasonID, srcBranchID, mixConfiguration, entitlementID1, sessionToken);
		Assert.assertFalse(mixConfigID.contains("error"), "entitlement was not added to the season");
		
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
	public void getNewItemsIds() throws Exception{
	 	rootId = purchasesApi.getBranchRootId(seasonID2, destBranchID, sessionToken);
	 	
	 	JSONArray entitlements = purchasesApi.getPurchasesBySeasonFromBranch(seasonID2, destBranchID, sessionToken);
	 	
	 	entitlement2ID2Season2 = entitlements.getJSONObject(1).getString("uniqueId");
	 	mixentitlementID2Season2 =entitlements.getJSONObject(2).getString("uniqueId");
	 	purchaseOptionsID1Season2 = entitlements.getJSONObject(0).getJSONArray("purchaseOptions").getJSONObject(0).getString("uniqueId");
	 	purchaseOptionsID2Season2 = entitlements.getJSONObject(1).getJSONArray("purchaseOptions").getJSONObject(0).getString("uniqueId");
	 	mixPurchaseOptionsID1Season2 = entitlements.getJSONObject(1).getJSONArray("purchaseOptions").getJSONObject(1).getString("uniqueId");
	 			 	
	 	JSONArray configurations = entitlements.getJSONObject(0).getJSONArray("configurationRules");
	 	for (Object el:configurations){
	 		JSONObject config = new JSONObject(el);
	 		if (config.getString("type").equals("CONFIGURATION_RULE"))
	 			configIDSeason2 = config.getString("uniqueId");
	 		else if (config.getString("type").equals("CONFIG_MUTUAL_EXCLUSION_GROUP"))
	 			mixConfigIDSeason2 = config.getString("uniqueId");
	 	}		
	}
	
	@Test (dependsOnMethods="getNewItemsIds", description="Copy single purchaseOptions under another purchaseOptions in the second season. First, copy without namesuffix, then copy with namesuffix")
	public void copySinglePurchaseOptionsUnderPurchaseOptions() throws IOException, JSONException{		
		//should fail copy without suffix
		String response = f.copyItemBetweenBranches(purchaseOptionsID1, purchaseOptionsID2Season2, "ACT", null, null, sessionToken, srcBranchID, destBranchID);
		Assert.assertTrue(response.contains("illegalName"), "purchaseOptions was copied with existing name ");

		//should fail copy copying purchaseOptions under purchaseOptions
		response = f.copyItemBetweenBranches(purchaseOptionsID1, purchaseOptionsID2Season2, "ACT", null, "suffix1", sessionToken, srcBranchID, destBranchID);
		Assert.assertTrue(response.contains("error") && response.contains("Illegal parent"), "can copy purchaseOptionsunder purchaseOptions");
	}
	
	@Test (dependsOnMethods="copySinglePurchaseOptionsUnderPurchaseOptions", description="Copy single purchaseOptions under another entitlement in the second season. First, copy without namesuffix, then copy with namesuffix")
	public void copySinglePurchaseOptionsUnderEntitlement() throws IOException, JSONException{		
		//should fail copy without suffix
		String response = f.copyItemBetweenBranches(purchaseOptionsID1, entitlement2ID2Season2, "ACT", null, null, sessionToken, srcBranchID, destBranchID);
		Assert.assertTrue(response.contains("illegalName"), "purchaseOptions was copied with existing name ");

		response = f.copyItemBetweenBranches(purchaseOptionsID1, entitlement2ID2Season2, "ACT", null, "suffix1", sessionToken, srcBranchID, destBranchID);
		Assert.assertTrue(response.contains("newSubTreeId"), "purchaseOptions was not copied: " + response);
		
		JSONObject result = new JSONObject(response);
		String newPurchaseOptions = purchasesApi.getPurchaseItemFromBranch(result.getString("newSubTreeId"), destBranchID, sessionToken);
		String oldPurchaseOptions = purchasesApi.getPurchaseItemFromBranch(purchaseOptionsID1, srcBranchID, sessionToken);
		Assert.assertTrue(f.jsonObjsAreEqual(new JSONObject(newPurchaseOptions), new JSONObject(oldPurchaseOptions)));
		
		JSONObject updatedSeasonEntitlements = result.getJSONObject("updatedSeasonsEntitlements");
		JSONArray updatedEntitlementsArr = updatedSeasonEntitlements.getJSONArray("entitlements");
		Assert.assertTrue(updatedEntitlementsArr.size() == 3, "wrong updated sentitlements size");
		
		JSONObject entitlement2Obj = updatedEntitlementsArr.getJSONObject(1);
		Assert.assertTrue(entitlement2Obj.getJSONArray("purchaseOptions").size() == 3, "wrong updated purchaseOptions size");
		Assert.assertTrue(entitlement2Obj.getJSONArray("purchaseOptions").getJSONObject(0).getString("name").equals("purchaseOptions2"), "wrong copied purchaseOptions's name");
		Assert.assertTrue(entitlement2Obj.getJSONArray("purchaseOptions").getJSONObject(2).getString("name").equals("purchaseOptions1suffix1"), "wrong copied purchaseOptions's name");
	}
	
	@Test (dependsOnMethods="copySinglePurchaseOptionsUnderEntitlement", description="Copy single purchaseOptions under mix purchaseOptions in the second season. First, copy without namesuffix, then copy with namesuffix")
	public void copySingleEntitlementUnderMixPurchaseOptions() throws IOException, JSONException{
				
		//should fail copy without suffix
		String response = f.copyItemBetweenBranches(purchaseOptionsID1, mixPurchaseOptionsID1Season2, "ACT", null, null, sessionToken, srcBranchID, destBranchID);
		Assert.assertTrue(response.contains("illegalName"), "purchaseOptions was copied with existing name ");
		
		response = f.copyItemBetweenBranches(purchaseOptionsID1, mixPurchaseOptionsID1Season2, "ACT", null, "suffix2", sessionToken, srcBranchID, destBranchID);
		Assert.assertTrue(response.contains("newSubTreeId"), "purchaseOptions was not copied: " + response);
		
		JSONObject result = new JSONObject(response);
		String newPurchaseOptions = purchasesApi.getPurchaseItemFromBranch(result.getString("newSubTreeId"), destBranchID, sessionToken);
		String oldPurchaseOptions = purchasesApi.getPurchaseItemFromBranch(purchaseOptionsID1, srcBranchID, sessionToken);
		Assert.assertTrue(f.jsonObjsAreEqual(new JSONObject(newPurchaseOptions), new JSONObject(oldPurchaseOptions)));
		
		JSONObject updatedSeasonEntitlements = result.getJSONObject("updatedSeasonsEntitlements");
		JSONArray updatedEntitlementsArr = updatedSeasonEntitlements.getJSONArray("entitlements");
		Assert.assertTrue(updatedEntitlementsArr.size() == 3, "wrong updated sentitlements size");
		
		JSONObject purchaseOptionsMix = updatedEntitlementsArr.getJSONObject(1).getJSONArray("purchaseOptions").getJSONObject(1);
		Assert.assertTrue(purchaseOptionsMix.getJSONArray("purchaseOptions").size() == 1, "wrong updated sentitlements size");
		Assert.assertTrue(purchaseOptionsMix.getJSONArray("purchaseOptions").getJSONObject(0).getString("name").equals("purchaseOptions1suffix2"), "wrong copied sentitlement's name");
	}
	
	@Test (dependsOnMethods="copySingleEntitlementUnderMixPurchaseOptions", description="Copy single purchaseOptions under root in the second season. First, copy without namesuffix, then copy with namesuffix")
	public void copySinglePurchaseOptionsUnderRoot() throws IOException, JSONException{			
		//should fail copy without suffix
		String response = f.copyItemBetweenBranches(purchaseOptionsID1, rootId, "ACT", null, null, sessionToken, srcBranchID, destBranchID);
		Assert.assertTrue(response.contains("illegalName"), "purchaseOptions was copied with existing name ");
		
		//should fail copy purchaseOptionsunder root
		response = f.copyItemBetweenBranches(purchaseOptionsID1, rootId, "ACT", null, "suffix3", sessionToken, srcBranchID, destBranchID);
		Assert.assertTrue(response.contains("error") && response.contains("Illegal parent"), "purchaseOptions was not copied");
	}
	
	@Test (dependsOnMethods="copySinglePurchaseOptionsUnderRoot", description="Copy single purchaseOptions under configuration in the second season. First, copy without namesuffix, then copy with namesuffix")
	public void copySinglePurchaseOptionsID1UnderConfiguration() throws IOException{
		//should fail copy without suffix
		String response = f.copyItemBetweenBranches(purchaseOptionsID1, configIDSeason2, "ACT", null, null, sessionToken, srcBranchID, destBranchID);
		Assert.assertTrue(response.contains("illegalName"), "purchaseOptions was copied with existing name ");
		
		//should fail copy purchaseOptions under config 
		response = f.copyItemBetweenBranches(purchaseOptionsID1, configIDSeason2, "ACT", null, "suffix4", sessionToken, srcBranchID, destBranchID);
		Assert.assertTrue(response.contains("error") && response.contains("Illegal parent"), "purchaseOptions was copied under configuration " + response);
	}
	
	@Test (dependsOnMethods="copySinglePurchaseOptionsID1UnderConfiguration", description="Copy single entitlement under mix configuration in the second season. First, copy without namesuffix, then copy with namesuffix")
	public void copySinglePurchaseOptionsUnderMixConfiguration() throws IOException{
		//should fail copy without suffix
		String response = f.copyItemBetweenBranches(purchaseOptionsID1, mixConfigIDSeason2, "ACT", null, null, sessionToken, srcBranchID, destBranchID);
		Assert.assertTrue(response.contains("illegalName"), "purchaseOptions was copied with existing name ");
		
		//should fail copy purchaseOptions under config mix
		response = f.copyItemBetweenBranches(purchaseOptionsID1, mixConfigIDSeason2, "ACT", null, "suffix5", sessionToken, srcBranchID, destBranchID);
		Assert.assertTrue(response.contains("error") && response.contains("Illegal parent"), "purchaseOptions was copied under mix onfiguration " + response);
	}
	
	@Test (dependsOnMethods="copySinglePurchaseOptionsUnderMixConfiguration", description="Copy single purchaseOptions under mix entitlements in the second season. First, copy without namesuffix, then copy with namesuffix")
	public void copySinglePurchaseOptionsUnderMixEntitlement() throws IOException{
		//should fail copy without suffix
		String response = f.copyItemBetweenBranches(purchaseOptionsID1, mixentitlementID2Season2, "ACT", null, null, sessionToken, srcBranchID, destBranchID);
		Assert.assertTrue(response.contains("illegalName"), "purchaseOptions was copied with existing name ");
		
		//should fail copy purchaseOptions under config mix
		response = f.copyItemBetweenBranches(purchaseOptionsID1, mixentitlementID2Season2, "ACT", null, "suffix5", sessionToken, srcBranchID, destBranchID);
		Assert.assertTrue(response.contains("error") && response.contains("Illegal parent"), "purchaseOptions was copied under mix entitlements " + response);
	}
	
	@AfterTest
	private void reset(){
		baseUtils.reset(productID, sessionToken);
	}

}