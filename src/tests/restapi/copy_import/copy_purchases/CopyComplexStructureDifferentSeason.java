package tests.restapi.copy_import.copy_purchases;

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
import tests.restapi.SeasonsRestApi;

public class CopyComplexStructureDifferentSeason {
	private String seasonID;
	private String seasonID2;
	private String productID;
	private String entitlementID1;
	private String targetEntitlementID;
	private String filePath;
	private String m_url;
	private ProductsRestApi p;
	private FeaturesRestApi f;
	private SeasonsRestApi s;
	private String sessionToken = "";
	private AirlockUtils baseUtils;
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
		E1 -> E_MIX	->E2 -> MIXCR ->CR1, CR2
					->E3 -> CR3 -> CR4
		   -> PO_MIX -> PO1 -> MIXCR ->CR5, CR6
					 -> PO2 -> CR7 -> CR8
	 */

	@Test (description="Add components")
	public void addComponents() throws IOException, JSONException{

		//create season2
		JSONObject sJson = new JSONObject();
		sJson.put("minVersion", "2.0");

		seasonID2 = s.addSeason(productID, sJson.toString(), sessionToken);
		Assert.assertFalse(seasonID2.contains("error"), "The second season was not created: " + seasonID2);

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

		//create entitlement tree in season1
		String entitlement1 = FileUtils.fileToString(filePath + "purchases/inAppPurchase1.txt", "UTF-8", false);
		entitlementID1 = purchasesApi.addPurchaseItemToBranch(seasonID, srcBranchID, entitlement1, "ROOT", sessionToken);
		Assert.assertFalse(entitlementID1.contains("error"), "Entitlement was not added to the season");

		String eMix = FileUtils.fileToString(filePath + "purchases/inAppPurchaseMutual.txt", "UTF-8", false);
		String mixID1 = purchasesApi.addPurchaseItemToBranch(seasonID, srcBranchID, eMix, entitlementID1, sessionToken);
		Assert.assertFalse(mixID1.contains("error"), "entitlements mix was not added to the season: " + mixID1);

		String entitlement2 = FileUtils.fileToString(filePath + "purchases/inAppPurchase2.txt", "UTF-8", false);
		String entitlementID2 = purchasesApi.addPurchaseItemToBranch(seasonID, srcBranchID, entitlement2, mixID1, sessionToken);
		Assert.assertFalse(entitlementID2.contains("error"), "Entitlement was not added to the season");

		String entitlement3 = FileUtils.fileToString(filePath + "purchases/inAppPurchase3.txt", "UTF-8", false);
		String entitlementID3 = purchasesApi.addPurchaseItemToBranch(seasonID, srcBranchID, entitlement3, mixID1, sessionToken);
		Assert.assertFalse(entitlementID3.contains("error"), "Entitlement was not added to the season");

		String configurationMix = FileUtils.fileToString(filePath + "configuration_feature-mutual.txt", "UTF-8", false);
		String mixConfigID = purchasesApi.addPurchaseItemToBranch(seasonID, srcBranchID, configurationMix, entitlementID2, sessionToken);
		Assert.assertFalse(mixConfigID.contains("error"), "Configuration mix was not added to the season");

		String configuration1 = FileUtils.fileToString(filePath + "configuration_rule1.txt", "UTF-8", false);
		JSONObject jsonCR = new JSONObject(configuration1);
		jsonCR.put("name", "CR1");
		String configID1 = purchasesApi.addPurchaseItemToBranch(seasonID, srcBranchID, jsonCR.toString(), mixConfigID, sessionToken);
		Assert.assertFalse(configID1.contains("error"), "cr was not added to the season:" + configID1);

		jsonCR.put("name", "CR2");
		String configID2 = purchasesApi.addPurchaseItemToBranch(seasonID, srcBranchID, jsonCR.toString(), mixConfigID, sessionToken);
		Assert.assertFalse(configID2.contains("error"), "cr was not added to the season:" + configID2);

		jsonCR.put("name", "CR3");
		String configID3 = purchasesApi.addPurchaseItemToBranch(seasonID, srcBranchID, jsonCR.toString(),entitlementID3, sessionToken);
		Assert.assertFalse(configID3.contains("error"), "cr was not added to the season:" + configID3);

		jsonCR.put("name", "CR4");
		String configID4 = purchasesApi.addPurchaseItemToBranch(seasonID, srcBranchID, jsonCR.toString(),configID3, sessionToken);
		Assert.assertFalse(configID4.contains("error"), "cr was not added to the season:" + configID4);

		String poMix = FileUtils.fileToString(filePath + "purchases/purchaseOptionsMutual.txt", "UTF-8", false);
		String poMixID = purchasesApi.addPurchaseItemToBranch(seasonID, srcBranchID, poMix, entitlementID1, sessionToken);
		Assert.assertFalse(poMixID.contains("error"), "purchaseOptions mix was not added to the season: " + poMixID);

		String purchaseOptions1 = FileUtils.fileToString(filePath + "purchases/purchaseOptions1.txt", "UTF-8", false);
		String purchaseOptionsID1 = purchasesApi.addPurchaseItemToBranch(seasonID, srcBranchID, purchaseOptions1, poMixID, sessionToken);
		Assert.assertFalse(purchaseOptionsID1.contains("error"), "Entitlement was not added to the season: " + purchaseOptionsID1);

		String purchaseOptions2 = FileUtils.fileToString(filePath + "purchases/purchaseOptions2.txt", "UTF-8", false);
		String purchaseOptionsID2 = purchasesApi.addPurchaseItemToBranch(seasonID, srcBranchID, purchaseOptions2, poMixID, sessionToken);
		Assert.assertFalse(purchaseOptionsID2.contains("error"), "Entitlement was not added to the season:" + purchaseOptionsID2);

		String configurationMix2 = FileUtils.fileToString(filePath + "configuration_feature-mutual.txt", "UTF-8", false);
		String mixConfigID2 = purchasesApi.addPurchaseItemToBranch(seasonID, srcBranchID, configurationMix2, purchaseOptionsID1, sessionToken);
		Assert.assertFalse(mixConfigID2.contains("error"), "Configuration mix was not added to the season: " + mixConfigID2);

		jsonCR.put("name", "CR5");
		String configID5 = purchasesApi.addPurchaseItemToBranch(seasonID, srcBranchID, jsonCR.toString(), mixConfigID2, sessionToken);
		Assert.assertFalse(configID5.contains("error"), "cr was not added to the season:" + configID5);

		jsonCR.put("name", "CR6");
		String configID6 = purchasesApi.addPurchaseItemToBranch(seasonID, srcBranchID, jsonCR.toString(), mixConfigID2, sessionToken);
		Assert.assertFalse(configID6.contains("error"), "cr was not added to the season:" + configID6);

		jsonCR.put("name", "CR7");
		String configID7 = purchasesApi.addPurchaseItemToBranch(seasonID, srcBranchID, jsonCR.toString(),purchaseOptionsID2, sessionToken);
		Assert.assertFalse(configID7.contains("error"), "cr was not added to the season:" + configID7);

		jsonCR.put("name", "CR8");
		String configID8 = purchasesApi.addPurchaseItemToBranch(seasonID, srcBranchID, jsonCR.toString(),configID7, sessionToken);
		Assert.assertFalse(configID8.contains("error"), "cr was not added to the season:" + configID8);
	}

	@Test (dependsOnMethods="addComponents", description="Copy the whole structure under target feature")
	public void copyEntitlement() throws IOException, JSONException{
		//create target entitlement:
		String targetFeature = FileUtils.fileToString(filePath + "purchases/inAppPurchase1.txt", "UTF-8", false);
		JSONObject json = new JSONObject(targetFeature);
		json.put("name", "target");
		targetEntitlementID = purchasesApi.addPurchaseItemToBranch(seasonID2, destBranchID, json.toString(), "ROOT", sessionToken);
		Assert.assertFalse(targetEntitlementID.contains("error"), "entitlement was not added to the season" + targetEntitlementID);

		String response = f.copyItemBetweenBranches(entitlementID1, targetEntitlementID, "ACT", null, "suffix1", sessionToken, srcBranchID, destBranchID);
		Assert.assertTrue(response.contains("newSubTreeId"), "entitlement tree was not copied: " + response);

		JSONObject result = new JSONObject(response);
		String newEntitlement = purchasesApi.getPurchaseItemFromBranch(result.getString("newSubTreeId"), destBranchID, sessionToken);
		String oldEntitlement = purchasesApi.getPurchaseItemFromBranch(entitlementID1, srcBranchID, sessionToken);
		Assert.assertTrue(f.jsonObjsAreEqual(new JSONObject(newEntitlement), new JSONObject(oldEntitlement)));
	}

	@AfterTest
	private void reset(){
		baseUtils.reset(productID, sessionToken);
	}
}