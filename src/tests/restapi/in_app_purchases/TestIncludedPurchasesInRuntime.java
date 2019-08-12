package tests.restapi.in_app_purchases;

import java.io.IOException;

import org.apache.commons.lang3.RandomStringUtils;
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
import tests.restapi.FeaturesRestApi;
import tests.restapi.InAppPurchasesRestApi;
import tests.restapi.ProductsRestApi;
import tests.restapi.RuntimeDateUtilities;
import tests.restapi.RuntimeRestApi;
import tests.restapi.SeasonsRestApi;

public class TestIncludedPurchasesInRuntime {
	protected String seasonID;
	protected String filePath;
	protected ProductsRestApi p;
	protected FeaturesRestApi f;
	protected SeasonsRestApi s;
	private AirlockUtils baseUtils;
	protected String productID;
	protected String m_url;
	private String sessionToken = "";
	protected InAppPurchasesRestApi purchasesApi;
	protected String inAppPurID1;
	protected String inAppPurID2;
	protected String inAppPurID3;
	protected String bundleID;
	
	@BeforeClass
	@Parameters({"url", "analyticsUrl", "translationsUrl", "configPath", "sessionToken", "userName", "userPassword", "appName", "productsToDeleteFile"})
	public void init(String url, String analyticsUrl, String translationsUrl, String configPath, String sToken, String userName, String userPassword, String appName, String productsToDeleteFile) throws Exception{
		m_url = url;
		filePath = configPath;
		f = new FeaturesRestApi();
		f.setURL(m_url);
		p = new ProductsRestApi();
		p.setURL(m_url);
		purchasesApi = new InAppPurchasesRestApi();
		purchasesApi.setURL(m_url);
		s = new SeasonsRestApi();
		s.setURL(m_url);
		
		
		baseUtils = new AirlockUtils(url, analyticsUrl, translationsUrl, configPath, sToken, userName, userPassword, appName, productsToDeleteFile);
		sessionToken = baseUtils.sessionToken;

		productID = baseUtils.createProduct();
		baseUtils.printProductToFile(productID);
		seasonID = baseUtils.createSeason(productID);
	}
	
	@Test (description = "add 2 in-app-purchase and 2 feature pointing to it")
	public void addComponents() throws JSONException, IOException, InterruptedException{
		String dateFormat = f.setDateFormat();
		
		String inAppPur = FileUtils.fileToString(filePath + "purchases/inAppPurchase1.txt", "UTF-8", false);
		inAppPurID1 = purchasesApi.addPurchaseItem(seasonID, inAppPur, "ROOT", sessionToken);
		Assert.assertFalse (inAppPurID1.contains("error"), "Can't add inAppPurchase: " + inAppPurID1);
		
		inAppPur = FileUtils.fileToString(filePath + "purchases/inAppPurchase2.txt", "UTF-8", false);
		inAppPurID2 = purchasesApi.addPurchaseItem(seasonID, inAppPur, "ROOT", sessionToken);
		Assert.assertFalse (inAppPurID2.contains("error"), "Can't add inAppPurchase: " + inAppPurID2);
		
		String purchaseOptions = FileUtils.fileToString(filePath + "purchases/purchaseOptions1.txt", "UTF-8", false);
		JSONObject jsonIP = new JSONObject(purchaseOptions);
		jsonIP.put("name", RandomStringUtils.randomAlphabetic(5));
		String puOptID = purchasesApi.addPurchaseItem(seasonID, jsonIP.toString(), inAppPurID1, sessionToken);
		Assert.assertFalse (puOptID.contains("error"), "Can't add purchaseOptions: " + puOptID);
		
		
		JSONArray includedPurchases = new JSONArray();
		includedPurchases.add(inAppPurID1);
		includedPurchases.add(inAppPurID2);
		
		JSONObject iapObj = new JSONObject(inAppPur);
		iapObj.put("name", "bundle");
		iapObj.put("includedEntitlements", includedPurchases);
		bundleID = purchasesApi.addPurchaseItem(seasonID, iapObj.toString(), "ROOT", sessionToken);
		Assert.assertFalse (bundleID.contains("error"), "Can't add inAppPurchase: " + bundleID);
		
		String bundleStr = purchasesApi.getPurchaseItem(bundleID, sessionToken);
		JSONObject bundleObj = new JSONObject(bundleStr);
		Assert.assertTrue(bundleObj.getJSONArray("includedEntitlements").size()==2, "wrong number of includedPurchases in bundle");
				
		Assert.assertTrue(bundleObj.getJSONArray("includedEntitlements").get(0).equals(inAppPurID1), "wrong includedPurchase");
		Assert.assertTrue(bundleObj.getJSONArray("includedEntitlements").get(1).equals(inAppPurID2), "wrong includedPurchase");
		
		String defaults = s.getDefaults(seasonID, sessionToken);
		JSONObject defaultObj = new JSONObject(defaults);
		JSONArray purArr = defaultObj.getJSONObject("entitlementsRoot").getJSONArray("entitlements");
		Assert.assertTrue(purArr.size() == 3, "wrong number of purchases in runtime");
		JSONArray includedPur = purArr.getJSONObject(2).getJSONArray("includedEntitlements"); 
		Assert.assertTrue(includedPur.getString(0).equals("ns1.inAppPurchase1"), "wrong inAppPurchases in runtime");
		Assert.assertTrue(includedPur.getString(1).equals("ns1.inAppPurchase2"), "wrong inAppPurchases in runtime");
		
		f.setSleep();
		RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getDevelopmentBranchFileDateModification(m_url, productID, seasonID, "MASTER", dateFormat, sessionToken);
		Assert.assertTrue(responseDev.code ==200, "Runtime development feature file was not updated");
		JSONObject root = RuntimeDateUtilities.getInAppPurchasesList(responseDev.message);
		purArr = root.getJSONArray("entitlements");
		Assert.assertTrue(purArr.size() == 3, "wrong number of purchases in runtime");
		includedPur = purArr.getJSONObject(2).getJSONArray("includedEntitlements"); 
		Assert.assertTrue(includedPur.getString(0).equals("ns1.inAppPurchase1"), "wrong inAppPurchases in runtime");
		Assert.assertTrue(includedPur.getString(1).equals("ns1.inAppPurchase2"), "wrong inAppPurchases in runtime");
		
		RuntimeRestApi.DateModificationResults responseProd = RuntimeDateUtilities.getProductionBranchFileDateModification(m_url, productID, seasonID, "MASTER", dateFormat, sessionToken);
		Assert.assertTrue(responseProd.code ==304, "Runtime production feature file was changed");
		
		RuntimeRestApi.DateModificationResults prodChanged = RuntimeDateUtilities.getProductionChangedDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(prodChanged.code ==304, "productionChanged.txt file was changed");
	}
	
	@Test (dependsOnMethods = "addComponents", description = "add inAppPurchase to bundle")
	public void updateBundle() throws JSONException, IOException, InterruptedException{
		String dateFormat = f.setDateFormat();
		
		String inAppPur = FileUtils.fileToString(filePath + "purchases/inAppPurchase3.txt", "UTF-8", false);
		inAppPurID3 = purchasesApi.addPurchaseItem(seasonID, inAppPur, "ROOT", sessionToken);
		Assert.assertFalse (inAppPurID3.contains("error"), "Can't add inAppPurchase: " + inAppPurID3);
		
		String purchaseOptions = FileUtils.fileToString(filePath + "purchases/purchaseOptions1.txt", "UTF-8", false);
		JSONObject jsonIP = new JSONObject(purchaseOptions);
		jsonIP.put("name", RandomStringUtils.randomAlphabetic(5));
		String puOptID = purchasesApi.addPurchaseItem(seasonID, jsonIP.toString(), inAppPurID3, sessionToken);
		Assert.assertFalse (puOptID.contains("error"), "Can't add purchaseOptions: " + puOptID);
		
		jsonIP.put("name", RandomStringUtils.randomAlphabetic(5));
		puOptID = purchasesApi.addPurchaseItem(seasonID, jsonIP.toString(), inAppPurID3, sessionToken);
		Assert.assertFalse (puOptID.contains("error"), "Can't add purchaseOptions: " + puOptID);
		
		String bundle = purchasesApi.getPurchaseItem(bundleID, sessionToken);
		JSONObject bundleJson = new JSONObject(bundle);
		
		JSONArray includedPurchases = bundleJson.getJSONArray("includedEntitlements");
		includedPurchases.add(inAppPurID3);	
		bundleJson.put("includedEntitlements", includedPurchases);
		
		String response = purchasesApi.updatePurchaseItem(seasonID, bundleID, bundleJson.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "cannot update bundle: " + response);
		
		String bundleStr = purchasesApi.getPurchaseItem(bundleID, sessionToken);
		JSONObject bundleObj = new JSONObject(bundleStr);
		Assert.assertTrue(bundleObj.getJSONArray("includedEntitlements").size()==3, "wrong number of includedPurchases in bundle");
				
		Assert.assertTrue(bundleObj.getJSONArray("includedEntitlements").get(0).equals(inAppPurID1), "wrong includedPurchase");
		Assert.assertTrue(bundleObj.getJSONArray("includedEntitlements").get(1).equals(inAppPurID2), "wrong includedPurchase");
		Assert.assertTrue(bundleObj.getJSONArray("includedEntitlements").get(2).equals(inAppPurID3), "wrong includedPurchase");
		
		String defaults = s.getDefaults(seasonID, sessionToken);
		JSONObject defaultObj = new JSONObject(defaults);
		JSONArray purArr = defaultObj.getJSONObject("entitlementsRoot").getJSONArray("entitlements");
		Assert.assertTrue(purArr.size() == 4, "wrong number of purchases in runtime");
		JSONArray includedPur = purArr.getJSONObject(2).getJSONArray("includedEntitlements");
		Assert.assertTrue(includedPur.size()==3, "wrong number of inAppPurchases in runtime");
		Assert.assertTrue(includedPur.getString(0).equals("ns1.inAppPurchase1"), "wrong inAppPurchases in runtime");
		Assert.assertTrue(includedPur.getString(1).equals("ns1.inAppPurchase2"), "wrong inAppPurchases in runtime");
		Assert.assertTrue(includedPur.getString(2).equals("ns1.inAppPurchase3"), "wrong inAppPurchases in runtime");
				
		f.setSleep();
		RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getDevelopmentBranchFileDateModification(m_url, productID, seasonID, "MASTER", dateFormat, sessionToken);
		Assert.assertTrue(responseDev.code ==200, "Runtime development feature file was not updated");
		JSONObject root = RuntimeDateUtilities.getInAppPurchasesList(responseDev.message);
		purArr = root.getJSONArray("entitlements");
		Assert.assertTrue(purArr.size() == 4, "wrong number of purchases in runtime");
		includedPur = purArr.getJSONObject(2).getJSONArray("includedEntitlements");
		Assert.assertTrue(includedPur.size()==3, "wrong number of inAppPurchases in runtime");
		
		Assert.assertTrue(includedPur.getString(0).equals("ns1.inAppPurchase1"), "wrong inAppPurchases in runtime");
		Assert.assertTrue(includedPur.getString(1).equals("ns1.inAppPurchase2"), "wrong inAppPurchases in runtime");
		Assert.assertTrue(includedPur.getString(2).equals("ns1.inAppPurchase3"), "wrong inAppPurchases in runtime");
		
		RuntimeRestApi.DateModificationResults responseProd = RuntimeDateUtilities.getProductionBranchFileDateModification(m_url, productID, seasonID, "MASTER", dateFormat, sessionToken);
		Assert.assertTrue(responseProd.code ==304, "Runtime production feature file was changed");
		
		RuntimeRestApi.DateModificationResults prodChanged = RuntimeDateUtilities.getProductionChangedDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(prodChanged.code ==304, "productionChanged.txt file was changed");
	}
	
	@Test (dependsOnMethods = "updateBundle", description = "update purcahse name")
	public void updatePurchaseName() throws JSONException, IOException, InterruptedException{
		String dateFormat = f.setDateFormat();
				
		String purchaseStr = purchasesApi.getPurchaseItem(inAppPurID2, sessionToken);
		JSONObject purchaseObj = new JSONObject(purchaseStr);
		purchaseObj.put("name", "kuku");
		inAppPurID2 = purchasesApi.updatePurchaseItem(seasonID, inAppPurID2, purchaseObj.toString(), sessionToken);
		Assert.assertFalse(inAppPurID2.contains("error"), "cannot update inAppPurID1: " + inAppPurID2);
		
		purchaseStr = purchasesApi.getPurchaseItem(inAppPurID2, sessionToken);
		purchaseObj = new JSONObject(purchaseStr);
		Assert.assertTrue(purchaseObj.getString("name").equals("kuku"), "the purchase name is wrong");
		
		String defaults = s.getDefaults(seasonID, sessionToken);
		JSONObject defaultObj = new JSONObject(defaults);
		JSONArray purArr = defaultObj.getJSONObject("entitlementsRoot").getJSONArray("entitlements");
		Assert.assertTrue(purArr.size() == 4, "wrong number of purchases in runtime");
		JSONArray includedPur = purArr.getJSONObject(2).getJSONArray("includedEntitlements");
		Assert.assertTrue(includedPur.size()==3, "wrong number of inAppPurchases in runtime");
		Assert.assertTrue(includedPur.getString(0).equals("ns1.inAppPurchase1"), "wrong inAppPurchases in runtime");
		Assert.assertTrue(includedPur.getString(1).equals("ns1.kuku"), "wrong inAppPurchases in runtime");
		Assert.assertTrue(includedPur.getString(2).equals("ns1.inAppPurchase3"), "wrong inAppPurchases in runtime");
		
		f.setSleep();
		RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getDevelopmentBranchFileDateModification(m_url, productID, seasonID, "MASTER", dateFormat, sessionToken);
		Assert.assertTrue(responseDev.code ==200, "Runtime development feature file was not updated");
		JSONObject root = RuntimeDateUtilities.getInAppPurchasesList(responseDev.message);
		purArr = root.getJSONArray("entitlements");
		Assert.assertTrue(purArr.size() == 4, "wrong number of purchases in runtime");
		includedPur = purArr.getJSONObject(2).getJSONArray("includedEntitlements");
		Assert.assertTrue(includedPur.size()==3, "wrong number of inAppPurchases in runtime");
		
		Assert.assertTrue(includedPur.getString(0).equals("ns1.inAppPurchase1"), "wrong inAppPurchases in runtime");
		Assert.assertTrue(includedPur.getString(1).equals("ns1.kuku"), "wrong inAppPurchases in runtime");
		Assert.assertTrue(includedPur.getString(2).equals("ns1.inAppPurchase3"), "wrong inAppPurchases in runtime");
		
		RuntimeRestApi.DateModificationResults responseProd = RuntimeDateUtilities.getProductionBranchFileDateModification(m_url, productID, seasonID, "MASTER", dateFormat, sessionToken);
		Assert.assertTrue(responseProd.code ==304, "Runtime production feature file was changed");
		
		RuntimeRestApi.DateModificationResults prodChanged = RuntimeDateUtilities.getProductionChangedDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(prodChanged.code ==304, "productionChanged.txt file was changed");
	}
	
	@Test (dependsOnMethods = "updatePurchaseName", description = "update bundle stage")
	public void updateBundleStage() throws JSONException, IOException, InterruptedException{
		String dateFormat = f.setDateFormat();
		
		
		String bundle = purchasesApi.getPurchaseItem(bundleID, sessionToken);
		JSONObject bundleJson = new JSONObject(bundle);
		
		bundleJson.put("stage", "PRODUCTION");
		
		String response = purchasesApi.updatePurchaseItem(seasonID, bundleID, bundleJson.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "cannot update bundle: " + response);
		
		String bundleStr = purchasesApi.getPurchaseItem(bundleID, sessionToken);
		JSONObject bundleObj = new JSONObject(bundleStr);
		Assert.assertTrue(bundleObj.getJSONArray("includedEntitlements").size()==3, "wrong number of includedPurchases in bundle");
				
		Assert.assertTrue(bundleObj.getJSONArray("includedEntitlements").get(0).equals(inAppPurID1), "wrong includedPurchase");
		Assert.assertTrue(bundleObj.getJSONArray("includedEntitlements").get(1).equals(inAppPurID2), "wrong includedPurchase");
		Assert.assertTrue(bundleObj.getJSONArray("includedEntitlements").get(2).equals(inAppPurID3), "wrong includedPurchase");
		
		String defaults = s.getDefaults(seasonID, sessionToken);
		JSONObject defaultObj = new JSONObject(defaults);
		JSONArray purArr = defaultObj.getJSONObject("entitlementsRoot").getJSONArray("entitlements");
		Assert.assertTrue(purArr.size() == 4, "wrong number of purchases in runtime");
		JSONArray includedPur = purArr.getJSONObject(2).getJSONArray("includedEntitlements");
		Assert.assertTrue(includedPur.size()==3, "wrong number of inAppPurchases in runtime");
		
		Assert.assertTrue(includedPur.getString(0).equals("ns1.inAppPurchase1"), "wrong inAppPurchases in runtime");
		Assert.assertTrue(includedPur.getString(1).equals("ns1.kuku"), "wrong inAppPurchases in runtime");
		Assert.assertTrue(includedPur.getString(2).equals("ns1.inAppPurchase3"), "wrong inAppPurchases in runtime");
		
		
		f.setSleep();
		RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getDevelopmentBranchFileDateModification(m_url, productID, seasonID, "MASTER", dateFormat, sessionToken);
		Assert.assertTrue(responseDev.code ==200, "Runtime development feature file was not updated");
		JSONObject root = RuntimeDateUtilities.getInAppPurchasesList(responseDev.message);
		purArr = root.getJSONArray("entitlements");
		Assert.assertTrue(purArr.size() == 4, "wrong number of purchases in runtime");
		includedPur = purArr.getJSONObject(2).getJSONArray("includedEntitlements");
		Assert.assertTrue(includedPur.size()==3, "wrong number of inAppPurchases in runtime");
		
		Assert.assertTrue(includedPur.getString(0).equals("ns1.inAppPurchase1"), "wrong inAppPurchases in runtime");
		Assert.assertTrue(includedPur.getString(1).equals("ns1.kuku"), "wrong inAppPurchases in runtime");
		Assert.assertTrue(includedPur.getString(2).equals("ns1.inAppPurchase3"), "wrong inAppPurchases in runtime");
		
		
		RuntimeRestApi.DateModificationResults responseprod = RuntimeDateUtilities.getProductionBranchFileDateModification(m_url, productID, seasonID, "MASTER", dateFormat, sessionToken);
		Assert.assertTrue(responseprod.code ==200, "Runtime production feature file was not updated");
		root = RuntimeDateUtilities.getInAppPurchasesList(responseprod.message);
		purArr = root.getJSONArray("entitlements");
		Assert.assertTrue(purArr.size() == 1, "wrong number of purchases in runtime");
		includedPur = purArr.getJSONObject(0).getJSONArray("includedEntitlements");
		Assert.assertTrue(includedPur.size()==0, "wrong number of inAppPurchases in prod runtime");
		
		RuntimeRestApi.DateModificationResults prodChanged = RuntimeDateUtilities.getProductionChangedDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(prodChanged.code ==200, "productionChanged.txt file was changed");
	}
	
	@Test (dependsOnMethods = "updateBundleStage", description = "update purchase stage")
	public void updatePurchaseStage() throws JSONException, IOException, InterruptedException{
		String dateFormat = f.setDateFormat();
		
		String pur = purchasesApi.getPurchaseItem(inAppPurID1, sessionToken);
		JSONObject purJson = new JSONObject(pur);
		purJson.put("stage", "PRODUCTION");
		String response = purchasesApi.updatePurchaseItem(seasonID, inAppPurID1, purJson.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "cannot update purcahse: " + response);
		
		pur = purchasesApi.getPurchaseItem(inAppPurID3, sessionToken);
		purJson = new JSONObject(pur);		
		purJson.put("stage", "PRODUCTION");		
		response = purchasesApi.updatePurchaseItem(seasonID, inAppPurID3, purJson.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "cannot update purcahse: " + response);
		
		
		String bundleStr = purchasesApi.getPurchaseItem(bundleID, sessionToken);
		JSONObject bundleObj = new JSONObject(bundleStr);
		Assert.assertTrue(bundleObj.getJSONArray("includedEntitlements").size()==3, "wrong number of includedPurchases in bundle");
				
		Assert.assertTrue(bundleObj.getJSONArray("includedEntitlements").get(0).equals(inAppPurID1), "wrong includedPurchase");
		Assert.assertTrue(bundleObj.getJSONArray("includedEntitlements").get(1).equals(inAppPurID2), "wrong includedPurchase");
		Assert.assertTrue(bundleObj.getJSONArray("includedEntitlements").get(2).equals(inAppPurID3), "wrong includedPurchase");
		
		String defaults = s.getDefaults(seasonID, sessionToken);
		JSONObject defaultObj = new JSONObject(defaults);
		JSONArray purArr = defaultObj.getJSONObject("entitlementsRoot").getJSONArray("entitlements");
		Assert.assertTrue(purArr.size() == 4, "wrong number of purchases in runtime");
		JSONArray includedPur = purArr.getJSONObject(2).getJSONArray("includedEntitlements");
		Assert.assertTrue(includedPur.size()==3, "wrong number of inAppPurchases in runtime");
		Assert.assertTrue(includedPur.getString(0).equals("ns1.inAppPurchase1"), "wrong inAppPurchases in runtime");
		Assert.assertTrue(includedPur.getString(1).equals("ns1.kuku"), "wrong inAppPurchases in runtime");
		Assert.assertTrue(includedPur.getString(2).equals("ns1.inAppPurchase3"), "wrong inAppPurchases in runtime");
		
		
		f.setSleep();
		RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getDevelopmentBranchFileDateModification(m_url, productID, seasonID, "MASTER", dateFormat, sessionToken);
		Assert.assertTrue(responseDev.code ==200, "Runtime development feature file was not updated");
		JSONObject root = RuntimeDateUtilities.getInAppPurchasesList(responseDev.message);
		purArr = root.getJSONArray("entitlements");
		Assert.assertTrue(purArr.size() == 4, "wrong number of purchases in runtime");
		includedPur = purArr.getJSONObject(2).getJSONArray("includedEntitlements");
		Assert.assertTrue(includedPur.size()==3, "wrong number of inAppPurchases in runtime");
		
		Assert.assertTrue(includedPur.getString(0).equals("ns1.inAppPurchase1"), "wrong inAppPurchases in runtime");
		Assert.assertTrue(includedPur.getString(1).equals("ns1.kuku"), "wrong inAppPurchases in runtime");
		Assert.assertTrue(includedPur.getString(2).equals("ns1.inAppPurchase3"), "wrong inAppPurchases in runtime");
		
		
		RuntimeRestApi.DateModificationResults responseprod = RuntimeDateUtilities.getProductionBranchFileDateModification(m_url, productID, seasonID, "MASTER", dateFormat, sessionToken);
		Assert.assertTrue(responseprod.code ==200, "Runtime production feature file was not updated");
		root = RuntimeDateUtilities.getInAppPurchasesList(responseprod.message);
		purArr = root.getJSONArray("entitlements");
		Assert.assertTrue(purArr.size() == 3, "wrong number of purchases in runtime");
		includedPur = purArr.getJSONObject(1).getJSONArray("includedEntitlements");
		Assert.assertTrue(includedPur.size()==2, "wrong number of inAppPurchases in prod runtime");
		
		Assert.assertTrue(includedPur.getString(0).equals("ns1.inAppPurchase1"), "wrong inAppPurchases in runtime");
		Assert.assertTrue(includedPur.getString(1).equals("ns1.inAppPurchase3"), "wrong inAppPurchases in runtime");
		
		RuntimeRestApi.DateModificationResults prodChanged = RuntimeDateUtilities.getProductionChangedDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(prodChanged.code ==200, "productionChanged.txt file was changed");
	}
	
	@AfterTest
	private void reset(){
		baseUtils.reset(productID, sessionToken);
	}
}
