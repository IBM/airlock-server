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

public class CopyBundle {
	private String seasonID1;
	private String seasonID2;
	private String seasonID3;
	private String productID1;
	private String productID2;
	private String filePath;
	private String m_url;
	private ProductsRestApi p;
	private FeaturesRestApi f;
	private BranchesRestApi br ;
	private InAppPurchasesRestApi purchasesApi;
	private SeasonsRestApi s;
	private String sessionToken = "";
	private AirlockUtils baseUtils;
	private String srcBranchID;
	private String destBranchID;
	private boolean runOnMaster;
	private String entitlementID1;
	private String entitlementID2;
	private String bundleID;
	
	@BeforeClass
	@Parameters({"url", "analyticsUrl", "translationsUrl", "configPath", "sessionToken", "userName", "userPassword", "appName", "productsToDeleteFile", "runOnMaster"})
	public void init(String url, String analyticsUrl, String translationsUrl, String configPath, String sToken, String userName, String userPassword, String appName, String productsToDeleteFile, Boolean runOnMaster) throws Exception{
		m_url = url;
		filePath = configPath;
		p = new ProductsRestApi();
		p.setURL(m_url);
		s = new SeasonsRestApi();
		s.setURL(m_url);
		f = new FeaturesRestApi();
		f.setURL(m_url);
		purchasesApi = new InAppPurchasesRestApi();
		purchasesApi.setURL(m_url);
		br = new BranchesRestApi();
		br.setURL(url);
		
		baseUtils = new AirlockUtils(url, analyticsUrl, translationsUrl, configPath, sToken, userName, userPassword, appName, productsToDeleteFile);
		sessionToken = baseUtils.sessionToken;
		productID1 = baseUtils.createProduct();
		baseUtils.printProductToFile(productID1);
		seasonID1 = baseUtils.createSeason(productID1);
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
	 * create season s1 with entitlements and bundle
	 * copy bundle to s1
	 * create season s2 and try to copy the bundle to it
	 * create new product and s3 and try to copy the bundle to it 	
	 */
	
	@Test (description="Create entitlements E1 and E2 and bundle in s1")
	public void createComponemts() throws IOException, JSONException{
		
		//add entitlements to season1
		String entitlement = FileUtils.fileToString(filePath + "purchases/inAppPurchase1.txt", "UTF-8", false);
		JSONObject jsonE = new JSONObject(entitlement);
		jsonE.put("name", "E1");
		jsonE.put("stage", "PRODUCTION");
		entitlementID1 = purchasesApi.addPurchaseItemToBranch(seasonID1, srcBranchID, jsonE.toString(), "ROOT", sessionToken);
		Assert.assertFalse(entitlementID1.contains("error"), "Entitlement1 was not added to the season " + entitlementID1);
		
		jsonE.put("name", "E2");
		jsonE.put("stage", "PRODUCTION");
		entitlementID2 = purchasesApi.addPurchaseItemToBranch(seasonID1, srcBranchID, jsonE.toString(), "ROOT", sessionToken);
		Assert.assertFalse(entitlementID2.contains("error"), "Entitlement2 was not added to the season " + entitlementID2);
		
		jsonE.put("name", "bundle");
		jsonE.put("stage", "PRODUCTION");
		JSONArray eArr = new JSONArray();
		eArr.add(entitlementID1);
		eArr.add(entitlementID2);
		jsonE.put("includedEntitlements", eArr);
		bundleID = purchasesApi.addPurchaseItemToBranch(seasonID1, srcBranchID, jsonE.toString(), "ROOT", sessionToken);
		Assert.assertFalse(bundleID.contains("error"), "bundleID was not added to the season " + bundleID);
				
		//create season2
		JSONObject sJson = new JSONObject();
		sJson.put("minVersion", "2.0");		
		seasonID2 = s.addSeason(productID1, sJson.toString(), sessionToken);
		Assert.assertFalse(seasonID2.contains("error"), "The second season was not created: " + seasonID2);
	}
	
	@Test (dependsOnMethods="createComponemts", description="Copy bundle within the same season under root")
	public void copyBundleSameSeason() throws IOException, JSONException{
		//at the beginning copy from branch to itself or from master to itself
		destBranchID = srcBranchID;
		
		String suffix = "suffix1";
		String rootId = purchasesApi.getBranchRootId(seasonID1, "MASTER", sessionToken);

		String response = f.copyItemBetweenBranches(bundleID, rootId, "ACT", null, suffix, sessionToken, srcBranchID, destBranchID);
		Assert.assertTrue(response.contains("newSubTreeId"), "Bundle was not copied: " + response);
		
		JSONArray entitlementsArr = purchasesApi.getPurchasesBySeasonFromBranch(seasonID1, destBranchID, sessionToken);
		Assert.assertTrue(entitlementsArr.size() == 4, "wrong entitlements number after copy");

		Assert.assertTrue(entitlementsArr.getJSONObject(0).getString("uniqueId").equals(entitlementID1), "wrong entitlement id");
		Assert.assertTrue(entitlementsArr.getJSONObject(0).getString("name").equals("E1"), "wrong entitlement name");
		Assert.assertTrue(entitlementsArr.getJSONObject(1).getString("uniqueId").equals(entitlementID2), "wrong entitlement id");
		Assert.assertTrue(entitlementsArr.getJSONObject(1).getString("name").equals("E2"), "wrong entitlement name");
		Assert.assertTrue(entitlementsArr.getJSONObject(2).getString("uniqueId").equals(bundleID), "wrong entitlement id");
		Assert.assertTrue(entitlementsArr.getJSONObject(2).getString("name").equals("bundle"), "wrong entitlement name");
		Assert.assertTrue(entitlementsArr.getJSONObject(2).getJSONArray("includedEntitlements").size() == 2, "wrong includedEntitlements size");
		Assert.assertTrue(entitlementsArr.getJSONObject(2).getJSONArray("includedEntitlements").getString(0).equals(entitlementID1), "wrong includedEntitlements id");
		Assert.assertTrue(entitlementsArr.getJSONObject(2).getJSONArray("includedEntitlements").getString(1).equals(entitlementID2), "wrong includedEntitlements id");
		
		Assert.assertFalse(entitlementsArr.getJSONObject(3).getString("uniqueId").equals(bundleID), "wrong entitlement id");
		Assert.assertTrue(entitlementsArr.getJSONObject(3).getString("name").equals("bundlesuffix1"), "wrong entitlement name");
		Assert.assertTrue(entitlementsArr.getJSONObject(3).getJSONArray("includedEntitlements").size() == 2, "wrong includedEntitlements size");
		Assert.assertTrue(entitlementsArr.getJSONObject(3).getJSONArray("includedEntitlements").getString(0).equals(entitlementID1), "wrong includedEntitlements id");
		Assert.assertTrue(entitlementsArr.getJSONObject(3).getJSONArray("includedEntitlements").getString(1).equals(entitlementID2), "wrong includedEntitlements id");	
	}
	
	@Test (dependsOnMethods="copyBundleSameSeason", description="copy bundle to another season under root")
	public void copyFeatureOtherSeason() throws Exception{
		
		if (runOnMaster) {
			destBranchID = BranchesRestApi.MASTER;
		} else {
			String allBranches = br.getAllBranches(seasonID2, sessionToken);
			JSONObject jsonBranch = new JSONObject(allBranches);
			destBranchID = jsonBranch.getJSONArray("branches").getJSONObject(1).getString("uniqueId");
		}
		
		String suffix = "suffix2";
		String rootId = purchasesApi.getBranchRootId(seasonID2, "MASTER", sessionToken);
		
		String response = f.copyItemBetweenBranches(bundleID, rootId, "ACT", null, suffix, sessionToken, srcBranchID, destBranchID);
		Assert.assertTrue(response.contains("newSubTreeId"), "Bundle was not copied: " + response);
		
		JSONArray entitlementsArr = purchasesApi.getPurchasesBySeasonFromBranch(seasonID2, destBranchID, sessionToken);
		Assert.assertTrue(entitlementsArr.size() == 4, "wrong entitlements number after copy");
		
		String entitlementID1InNewSeason = entitlementsArr.getJSONObject(0).getString("uniqueId");
		Assert.assertTrue(entitlementsArr.getJSONObject(0).getString("name").equals("E1"), "wrong entitlement name");
		Assert.assertFalse(entitlementID1InNewSeason.equals(entitlementID1), "wrong entitlement id");
		String entitlementID2InNewSeason = entitlementsArr.getJSONObject(1).getString("uniqueId");
		Assert.assertTrue(entitlementsArr.getJSONObject(1).getString("name").equals("E2"), "wrong entitlement name");
		Assert.assertFalse(entitlementID2InNewSeason.equals(entitlementID2), "wrong entitlement id");
		String bundleIDInNewSeason = entitlementsArr.getJSONObject(2).getString("uniqueId");
		Assert.assertTrue(entitlementsArr.getJSONObject(2).getString("name").equals("bundle"), "wrong entitlement name");
		Assert.assertFalse(bundleIDInNewSeason.equals(bundleID), "wrong entitlement id");
		Assert.assertTrue(entitlementsArr.getJSONObject(2).getJSONArray("includedEntitlements").size() == 2, "wrong includedEntitlements size");
		Assert.assertTrue(entitlementsArr.getJSONObject(2).getJSONArray("includedEntitlements").getString(0).equals(entitlementID1InNewSeason), "wrong includedEntitlements id");
		Assert.assertTrue(entitlementsArr.getJSONObject(2).getJSONArray("includedEntitlements").getString(1).equals(entitlementID2InNewSeason), "wrong includedEntitlements id");
		
		String copiedBundleIDInNewSeason = entitlementsArr.getJSONObject(3).getString("uniqueId");
		Assert.assertTrue(entitlementsArr.getJSONObject(3).getString("name").equals("bundle"+suffix), "wrong newentitlement name");
		Assert.assertFalse(copiedBundleIDInNewSeason.equals(bundleIDInNewSeason), "wrong new entitlement id");
		Assert.assertTrue(entitlementsArr.getJSONObject(3).getJSONArray("includedEntitlements").size() == 2, "wrong includedEntitlements size");
		Assert.assertTrue(entitlementsArr.getJSONObject(3).getJSONArray("includedEntitlements").getString(0).equals(entitlementID1InNewSeason), "wrong includedEntitlements id");
		Assert.assertTrue(entitlementsArr.getJSONObject(3).getJSONArray("includedEntitlements").getString(1).equals(entitlementID2InNewSeason), "wrong includedEntitlements id");
	}
	
	@Test (dependsOnMethods="copyFeatureOtherSeason", description="copy bundle to another product")
	public void copyBundleDifferentProduct() throws IOException, JSONException{
		
		productID2 = baseUtils.createProduct();
		baseUtils.printProductToFile(productID2);

		JSONObject sJson = new JSONObject();
		sJson.put("minVersion", "2.0");
		
		seasonID3 = s.addSeason(productID2, sJson.toString(), sessionToken);
		Assert.assertFalse(seasonID3.contains("error"), "The new season was not created: " + seasonID3);
		
		if (runOnMaster) {
			destBranchID = BranchesRestApi.MASTER;
		} else {
			baseUtils.setSeasonId(seasonID3);
			destBranchID = baseUtils.addBranch("b3");
		}
		
		String suffix = "suffix3";
		String rootId = purchasesApi.getBranchRootId(seasonID3, "MASTER", sessionToken);
		
		String response = f.copyItemBetweenBranches(bundleID, rootId, "ACT", null, suffix, sessionToken, srcBranchID, destBranchID);
		Assert.assertTrue(response.contains("error") && response.contains("entitlement") , "bundle was copied even when its included entitlement is missing");
		
		//add entitlements to season3
		response = f.copyItemBetweenBranches(entitlementID1, rootId, "ACT", null, "", sessionToken, srcBranchID, destBranchID);
		Assert.assertTrue(response.contains("newSubTreeId") && !response.contains("error") , "entitlement1 was not imported : "+ response);
		
		response = f.copyItemBetweenBranches(entitlementID2, rootId, "ACT", null, "", sessionToken, srcBranchID, destBranchID);
		Assert.assertTrue(response.contains("newSubTreeId") && !response.contains("error") , "entitlement2 was not imported : "+ response);
		
		JSONArray entitlementsArr = purchasesApi.getPurchasesBySeasonFromBranch(seasonID3, destBranchID, sessionToken);
		Assert.assertTrue(entitlementsArr.size() == 2, "wrong entitlements number after import");
		
		String entitlementID1InNewProduct = entitlementsArr.getJSONObject(0).getString("uniqueId");
		Assert.assertTrue(entitlementsArr.getJSONObject(0).getString("name").equals("E1"), "wrong entitlement name");
		Assert.assertFalse(entitlementID1InNewProduct.equals(entitlementID1), "wrong entitlement id");
		String entitlementID2InNewProduct = entitlementsArr.getJSONObject(1).getString("uniqueId");
		Assert.assertTrue(entitlementsArr.getJSONObject(1).getString("name").equals("E2"), "wrong entitlement name");
		Assert.assertFalse(entitlementID2InNewProduct.equals(entitlementID2), "wrong entitlement id");
		
		//copy bundle again
		response = f.copyItemBetweenBranches(bundleID, rootId, "ACT", null, suffix, sessionToken, srcBranchID, destBranchID);
		Assert.assertFalse(response.contains("error"), "bundle was not copied even when its included entitlement is exist:" +response);
		
		entitlementsArr = purchasesApi.getPurchasesBySeasonFromBranch(seasonID3, destBranchID, sessionToken);
		Assert.assertTrue(entitlementsArr.size() == 3, "wrong entitlements number after copy");
		
		Assert.assertTrue(entitlementsArr.getJSONObject(0).getString("name").equals("E1"), "wrong entitlement name");
		Assert.assertTrue(entitlementsArr.getJSONObject(1).getString("name").equals("E2"), "wrong entitlement name");
		String bundleIDInNewSeason = entitlementsArr.getJSONObject(2).getString("uniqueId");
		Assert.assertTrue(entitlementsArr.getJSONObject(2).getString("name").equals("bundle"+suffix), "wrong entitlement name");
		Assert.assertFalse(bundleIDInNewSeason.equals(bundleID), "wrong entitlement id");
		Assert.assertTrue(entitlementsArr.getJSONObject(2).getJSONArray("includedEntitlements").size() == 2, "wrong includedEntitlements size");
		Assert.assertTrue(entitlementsArr.getJSONObject(2).getJSONArray("includedEntitlements").getString(0).equals(entitlementID1InNewProduct), "wrong includedEntitlements id");
		Assert.assertTrue(entitlementsArr.getJSONObject(2).getJSONArray("includedEntitlements").getString(1).equals(entitlementID2InNewProduct), "wrong includedEntitlements id");
	}

	@AfterTest
	private void reset(){
		baseUtils.reset(productID1, sessionToken);
	}

}