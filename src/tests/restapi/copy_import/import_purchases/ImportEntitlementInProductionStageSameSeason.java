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


public class ImportEntitlementInProductionStageSameSeason {
	private String seasonID;
	private String productID;
	private String mixIdToCopy;
	private String entitlementID1;
	private String entitlementID2;
	private String configID;
	private String filePath;
	private String m_url;
	private ProductsRestApi p;
	private FeaturesRestApi f;
	private String sessionToken = "";
	private AirlockUtils baseUtils;
	private String srcBranchID;
	private InAppPurchasesRestApi purchasesApi;
	private String purchaseOptionsID2;
	private String purchaseOptionsID3;
	
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
	
	@Test (description="Create entitlement structure in production stage ")
	public void addProductionComponents() throws IOException, JSONException{
		String entitlementMix = FileUtils.fileToString(filePath + "purchases/inAppPurchaseMutual.txt", "UTF-8", false);
		mixIdToCopy = purchasesApi.addPurchaseItemToBranch(seasonID, srcBranchID, entitlementMix, "ROOT", sessionToken);
		Assert.assertFalse(mixIdToCopy.contains("error"), "Entitlement was not added to the season" + mixIdToCopy);
		
		String entitlement1 = FileUtils.fileToString(filePath + "purchases/inAppPurchase1.txt", "UTF-8", false);
		JSONObject jsonE = new JSONObject(entitlement1);
		jsonE.put("stage", "PRODUCTION");
		entitlementID1 = purchasesApi.addPurchaseItemToBranch(seasonID, srcBranchID, jsonE.toString(), mixIdToCopy, sessionToken);
		Assert.assertFalse(entitlementID1.contains("error"), "Entitlement1 was not added to the season" + entitlementID1);
		
		String configurationMix = FileUtils.fileToString(filePath + "configuration_feature-mutual.txt", "UTF-8", false);
		String mixConfigID = purchasesApi.addPurchaseItemToBranch(seasonID, srcBranchID, configurationMix, entitlementID1, sessionToken);
		Assert.assertFalse(mixConfigID.contains("error"), "Configuration mix was not added to the season" + mixConfigID);

		String configuration = FileUtils.fileToString(filePath + "configuration_rule1.txt", "UTF-8", false);
		JSONObject jsonCR = new JSONObject(configuration);
		jsonCR.put("stage", "PRODUCTION");
		configID = purchasesApi.addPurchaseItemToBranch(seasonID, srcBranchID, jsonCR.toString(), mixConfigID, sessionToken);
		Assert.assertFalse(configID.contains("error"), "Configuration rule was not added to the season" + configID);

		String configuration2 = FileUtils.fileToString(filePath + "configuration_rule2.txt", "UTF-8", false);
		JSONObject jsonCR2 = new JSONObject(configuration2);
		jsonCR.put("stage", "PRODUCTION");
		String configID2 = purchasesApi.addPurchaseItemToBranch(seasonID, srcBranchID, jsonCR2.toString(), configID, sessionToken);
		Assert.assertFalse(configID2.contains("error"), "Configuration rule was not added to the season" + configID2);
		
		String poMix = FileUtils.fileToString(filePath + "purchases/purchaseOptionsMutual.txt", "UTF-8", false);
		String poMixID = purchasesApi.addPurchaseItemToBranch(seasonID, srcBranchID, poMix, entitlementID1, sessionToken);
		Assert.assertFalse(poMixID.contains("error"), "purchase options  mix was not added to the season: " + poMixID);

		String purchaseOptions2 = FileUtils.fileToString(filePath + "purchases/purchaseOptions2.txt", "UTF-8", false);
		JSONObject jsonPO2 = new JSONObject(purchaseOptions2);
		jsonPO2.put("stage", "PRODUCTION");
		purchaseOptionsID2 = purchasesApi.addPurchaseItemToBranch(seasonID, srcBranchID, jsonPO2.toString(), poMixID, sessionToken);
		Assert.assertFalse(purchaseOptionsID2.contains("error"), "purchaseOptions2 was not added to the season: " + purchaseOptionsID2);

		String purchaseOptions3 = FileUtils.fileToString(filePath + "purchases/purchaseOptions3.txt", "UTF-8", false);
		JSONObject jsonPO3 = new JSONObject(purchaseOptions3);
		jsonPO3.put("stage", "PRODUCTION");
		purchaseOptionsID3 = purchasesApi.addPurchaseItemToBranch(seasonID, srcBranchID, jsonPO3.toString(), poMixID, sessionToken);
		Assert.assertFalse(purchaseOptionsID3.contains("error"), "purchaseOptions3 was not added to the season: " + purchaseOptionsID3);
		
		//mixId tree will be copied under entitlement2
		String entitlement2 = FileUtils.fileToString(filePath + "purchases/inAppPurchase2.txt", "UTF-8", false);
		JSONObject jsonE2 = new JSONObject(entitlement2);
		jsonE2.put("stage", "PRODUCTION");
		entitlementID2 = purchasesApi.addPurchaseItemToBranch(seasonID, srcBranchID, jsonE2.toString(), "ROOT", sessionToken);
		Assert.assertFalse(entitlementID2.contains("error"), "Entitlement 2 was not added to the season" + entitlementID2);
	}
	
	@Test (dependsOnMethods="addProductionComponents", description="Copy entitlement tree in production stage under another entitlement")
	public void importEntitlement() throws IOException{
		String entitlementToImport = purchasesApi.getPurchaseItemFromBranch(mixIdToCopy, srcBranchID, sessionToken);
		
		String response = f.importFeatureToBranch(entitlementToImport, entitlementID2, "ACT", null, "suffix1", true, sessionToken, srcBranchID);
		Assert.assertTrue(response.contains("newSubTreeId"), "Entitlement was not copied: " + response);
	}
	
	
	@Test (dependsOnMethods="importEntitlement", description="Validate entitlement tree that should be converted to development")
	public void validateEntitlementsStage() throws IOException, JSONException{
		String entitlement2 = purchasesApi.getPurchaseItemFromBranch(entitlementID2, srcBranchID, sessionToken);
		JSONObject jsonE2 = new JSONObject(entitlement2);
		Assert.assertFalse(jsonE2.getString("stage").equals("DEVELOPMENT"), "The target entitlement was converted to development during copy");
		
		JSONObject jsonE1 = jsonE2.getJSONArray("entitlements").getJSONObject(0).getJSONArray("entitlements").getJSONObject(0);
		Assert.assertTrue(jsonE1.getString("stage").equals("DEVELOPMENT"), "The entitlement was not converted to development during copy");
		
		JSONObject jsonCR = jsonE1.getJSONArray("configurationRules").getJSONObject(0).getJSONArray("configurationRules").getJSONObject(0);	
		Assert.assertTrue(jsonCR.getString("stage").equals("DEVELOPMENT"), "The configuration rule was not converted to development during copy");

		JSONObject jsonCR2 = jsonCR.getJSONArray("configurationRules").getJSONObject(0);	
		Assert.assertTrue(jsonCR2.getString("stage").equals("DEVELOPMENT"), "The configuration rule was not converted to development during copy");
		
		JSONObject jsonPO2 = jsonE1.getJSONArray("purchaseOptions").getJSONObject(0).getJSONArray("purchaseOptions").getJSONObject(0);	
		Assert.assertTrue(jsonPO2.getString("stage").equals("DEVELOPMENT"), "The purchaseOptions was not converted to development during copy");
		Assert.assertFalse(jsonPO2.getString("uniqueId").equals(purchaseOptionsID2), "The purchaseOptions id wasnt changed");

		JSONObject jsonPO3 = jsonE1.getJSONArray("purchaseOptions").getJSONObject(0).getJSONArray("purchaseOptions").getJSONObject(1);	
		Assert.assertTrue(jsonPO3.getString("stage").equals("DEVELOPMENT"), "The purchaseOptions was not converted to development during copy");
		Assert.assertFalse(jsonPO3.getString("uniqueId").equals(purchaseOptionsID3), "The purchaseOptions id wasnt changed");
	}
	
	@AfterTest
	private void reset(){
		baseUtils.reset(productID, sessionToken);
	}
}