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

public class ImportComplexStructureDifferentProduct {
	private String seasonID;
	private String seasonID3;
	private String seasonID2;
	private String productID;
	private String productID2;
	private String productID3;
	private String entitlementID1;
	private String targetEntitlementID;
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
	private String poMixID;
	

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
		E1 -> MIX	->E2 -> MIXCR ->CR1, CR2
					->E3 -> CR3 -> CR4
		   -> PO1
		   -> PO_MIX ->PO2, PO3
		              		
	 */
	
	@Test (description="Add components")
	public void addComponents() throws IOException, JSONException{
		//create product2 with season2
		String product = FileUtils.fileToString(filePath + "product1.txt", "UTF-8", false);
		product = JSONUtils.generateUniqueString(product, 5, "codeIdentifier");
		product = JSONUtils.generateUniqueString(product, 8, "name");
		productID2 = p.addProduct(product, sessionToken);
		baseUtils.printProductToFile(productID2);

		JSONObject sJson = new JSONObject();
		sJson.put("minVersion", "2.0");
		
		seasonID2 = s.addSeason(productID2, sJson.toString(), sessionToken);
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
		Assert.assertFalse(entitlementID1.contains("error"), "entitlement was not added to the season");
		
		String entitlementMix = FileUtils.fileToString(filePath + "purchases/inAppPurchaseMutual.txt", "UTF-8", false);
		String mixID1 = purchasesApi.addPurchaseItemToBranch(seasonID, srcBranchID, entitlementMix, entitlementID1, sessionToken);
		Assert.assertFalse(mixID1.contains("error"), "entitlement mix was not added to the season: " + mixID1);
		
		String entitlement2 = FileUtils.fileToString(filePath + "purchases/inAppPurchase2.txt", "UTF-8", false);
		String entitlementID2 = purchasesApi.addPurchaseItemToBranch(seasonID, srcBranchID, entitlement2, mixID1, sessionToken);
		Assert.assertFalse(entitlementID2.contains("error"), "entitlement was not added to the season");

		String entitlement3 = FileUtils.fileToString(filePath + "purchases/inAppPurchase3.txt", "UTF-8", false);
		String entitlementID3 = purchasesApi.addPurchaseItemToBranch(seasonID, srcBranchID, entitlement3, mixID1, sessionToken);
		Assert.assertFalse(entitlementID3.contains("error"), "entitlement was not added to the season");

		String configurationMix = FileUtils.fileToString(filePath + "configuration_feature-mutual.txt", "UTF-8", false);
		String mixConfigID = purchasesApi.addPurchaseItemToBranch(seasonID, srcBranchID, configurationMix, entitlementID2, sessionToken);
		Assert.assertFalse(mixConfigID.contains("error"), "Configuration mix was not added to the season");

		String configuration1 = FileUtils.fileToString(filePath + "configuration_rule1.txt", "UTF-8", false);
		JSONObject jsonCR = new JSONObject(configuration1);
		jsonCR.put("name", "CR1");
		String configID1 = purchasesApi.addPurchaseItemToBranch(seasonID, srcBranchID, jsonCR.toString(), mixConfigID, sessionToken);
		Assert.assertFalse(configID1.contains("error"), "Feature was not added to the season");
				
		jsonCR.put("name", "CR2");
		String configID2 = purchasesApi.addPurchaseItemToBranch(seasonID, srcBranchID, jsonCR.toString(), mixConfigID, sessionToken);
		Assert.assertFalse(configID2.contains("error"), "Feature was not added to the season");

		jsonCR.put("name", "CR3");
		String configID3 = purchasesApi.addPurchaseItemToBranch(seasonID, srcBranchID, jsonCR.toString(),entitlementID3, sessionToken);
		Assert.assertFalse(configID3.contains("error"), "Feature was not added to the season");

		jsonCR.put("name", "CR4");
		String configID4 = purchasesApi.addPurchaseItemToBranch(seasonID, srcBranchID, jsonCR.toString(),configID3, sessionToken);
		Assert.assertFalse(configID4.contains("error"), "Feature was not added to the season");
		
		String purchaseOptions1 = FileUtils.fileToString(filePath + "purchases/purchaseOptions1.txt", "UTF-8", false);
		String purchaseOptionsID1 = purchasesApi.addPurchaseItemToBranch(seasonID, srcBranchID, purchaseOptions1, entitlementID1, sessionToken);
		Assert.assertFalse(purchaseOptionsID1.contains("error"), "purchaseOptions1 was not added to the season: " + purchaseOptionsID1);

		String poMix = FileUtils.fileToString(filePath + "purchases/purchaseOptionsMutual.txt", "UTF-8", false);
		poMixID = purchasesApi.addPurchaseItemToBranch(seasonID, srcBranchID, poMix, entitlementID1, sessionToken);
		Assert.assertFalse(poMixID.contains("error"), "purchase options  mix was not added to the season: " + poMixID);

		String purchaseOptions2 = FileUtils.fileToString(filePath + "purchases/purchaseOptions2.txt", "UTF-8", false);
		String purchaseOptionsID2 = purchasesApi.addPurchaseItemToBranch(seasonID, srcBranchID, purchaseOptions2, poMixID, sessionToken);
		Assert.assertFalse(purchaseOptionsID2.contains("error"), "purchaseOptions2 was not added to the season: " + purchaseOptionsID2);

		String purchaseOptions3 = FileUtils.fileToString(filePath + "purchases/purchaseOptions3.txt", "UTF-8", false);
		String purchaseOptionsID3 = purchasesApi.addPurchaseItemToBranch(seasonID, srcBranchID, purchaseOptions3, poMixID, sessionToken);
		Assert.assertFalse(purchaseOptionsID3.contains("error"), "purchaseOptions3 was not added to the season: " + purchaseOptionsID3);
	}
	
	@Test (dependsOnMethods="addComponents", description="Copy the whole structure under target entitlement")
	public void importEntitlement() throws IOException, JSONException{
		//create target entitlements:
		String targetEntitlement = FileUtils.fileToString(filePath + "purchases/inAppPurchase1.txt", "UTF-8", false);
		JSONObject json = new JSONObject(targetEntitlement);
		json.put("name", "target");
		targetEntitlementID = purchasesApi.addPurchaseItemToBranch(seasonID2, destBranchID, json.toString(), "ROOT", sessionToken);
		Assert.assertFalse(targetEntitlementID.contains("error"), "entitlement was not added to the season" + targetEntitlementID);
		String entitlementToImport = purchasesApi.getPurchaseItemFromBranch(entitlementID1, srcBranchID, sessionToken);
		
		String response = f.importFeatureToBranch(entitlementToImport, targetEntitlementID, "ACT", null, "suffix1", true, sessionToken, destBranchID);
		Assert.assertTrue(response.contains("newSubTreeId"), "Entitlement tree was not imported: " + response);
		
		JSONObject result = new JSONObject(response);
		String newFeature = purchasesApi.getPurchaseItemFromBranch(result.getString("newSubTreeId"), destBranchID, sessionToken);
		Assert.assertTrue(f.jsonObjsAreEqual(new JSONObject(newFeature), new JSONObject(entitlementToImport)));
	}
	
	@Test (dependsOnMethods="importEntitlement", description="Copy the purchase options mtx under target entitlement")
	public void importPurchaseOptionsMtx() throws IOException, JSONException{
		//create product3 with season3
		String product = FileUtils.fileToString(filePath + "product1.txt", "UTF-8", false);
		product = JSONUtils.generateUniqueString(product, 5, "codeIdentifier");
		product = JSONUtils.generateUniqueString(product, 8, "name");
		productID3 = p.addProduct(product, sessionToken);
		baseUtils.printProductToFile(productID3);

		JSONObject sJson = new JSONObject();
		sJson.put("minVersion", "2.0");
		
		seasonID3 = s.addSeason(productID3, sJson.toString(), sessionToken);
		Assert.assertFalse(seasonID3.contains("error"), "The third season was not created: " + seasonID2);

		try {
			if (runOnMaster) {
				destBranchID = BranchesRestApi.MASTER;
			} else {
				baseUtils.setSeasonId(seasonID3);
				destBranchID = baseUtils.addBranch("b1");
			}
		}catch (Exception e){
			destBranchID = null;
		}

		//create target entitlements:
		String targetEntitlement = FileUtils.fileToString(filePath + "purchases/inAppPurchase1.txt", "UTF-8", false);
		JSONObject json = new JSONObject(targetEntitlement);
		json.put("name", "target");
		targetEntitlementID = purchasesApi.addPurchaseItemToBranch(seasonID3, destBranchID, json.toString(), "ROOT", sessionToken);
		Assert.assertFalse(targetEntitlementID.contains("error"), "entitlement was not added to the season" + targetEntitlementID);
		String poMixToImport = purchasesApi.getPurchaseItemFromBranch(poMixID, srcBranchID, sessionToken);
		
		String response = f.importFeatureToBranch(poMixToImport, targetEntitlementID, "ACT", null, "suffix1", true, sessionToken, destBranchID);
		Assert.assertTrue(response.contains("newSubTreeId"), "purchase Options mtx tree was not imported: " + response);
		
		JSONObject result = new JSONObject(response);
		String newFeature = purchasesApi.getPurchaseItemFromBranch(result.getString("newSubTreeId"), destBranchID, sessionToken);
		Assert.assertTrue(f.jsonObjsAreEqual(new JSONObject(newFeature), new JSONObject(poMixToImport)));
	}
	
	@AfterTest
	private void reset(){
		baseUtils.reset(productID, sessionToken);
		baseUtils.reset(productID2, sessionToken);
	}
}