package tests.restapi.in_app_purchases;

import java.io.IOException;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.wink.json4j.JSONException;
import org.apache.wink.json4j.JSONObject;
import org.apache.wink.json4j.JSONArray;

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

public class TestFeaturePremiumFields {
	protected String seasonID;
	protected String filePath;
	protected ProductsRestApi p;
	protected FeaturesRestApi f;
	private AirlockUtils baseUtils;
	protected String productID;
	protected String m_url;
	private String sessionToken = "";
	protected InAppPurchasesRestApi purchasesApi;
	protected String inAppPurID1;
	protected String inAppPurID2;
	protected String inAppPurID3;
	protected String inAppPurID4;
	protected String featureID1;
	protected String featureID2;
	protected String featureID3;
	protected String featureID4;
	private String puOptID1;
	private String puOptID2;
	private String puOptID3;
	private String puOptID5;
	private String puOptID6;
	private String puOptMtxID1;
	private String inAppPurID5;
	private String puOptID7;
	private String featureID5;
	
	
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
		
		baseUtils = new AirlockUtils(url, analyticsUrl, translationsUrl, configPath, sToken, userName, userPassword, appName, productsToDeleteFile);
		sessionToken = baseUtils.sessionToken;

		productID = baseUtils.createProduct();
		baseUtils.printProductToFile(productID);
		seasonID = baseUtils.createSeason(productID);
	}
	
	@Test (description = "add 2 in-app-purchase and 2 feature pointing to it")
	public void addComponents() throws JSONException, IOException, InterruptedException{
		String inAppPur = FileUtils.fileToString(filePath + "purchases/inAppPurchase1.txt", "UTF-8", false);
		inAppPurID1 = purchasesApi.addPurchaseItem(seasonID, inAppPur, "ROOT", sessionToken);
		Assert.assertFalse (inAppPurID1.contains("error"), "Can't add inAppPurchase: " + inAppPurID1);
		
		inAppPur = FileUtils.fileToString(filePath + "purchases/inAppPurchase2.txt", "UTF-8", false);
		inAppPurID2 = purchasesApi.addPurchaseItem(seasonID, inAppPur, "ROOT", sessionToken);
		Assert.assertFalse (inAppPurID2.contains("error"), "Can't add inAppPurchase: " + inAppPurID2);
		
		String purchaseOptions = FileUtils.fileToString(filePath + "purchases/purchaseOptions1.txt", "UTF-8", false);
		JSONObject jsonIP = new JSONObject(purchaseOptions);
		jsonIP.put("name", RandomStringUtils.randomAlphabetic(5));
		puOptID3 = purchasesApi.addPurchaseItem(seasonID, jsonIP.toString(), inAppPurID1, sessionToken);
		Assert.assertFalse (puOptID3.contains("error"), "Can't add purchaseOptions: " + puOptID3);
		
		
		String feature = FileUtils.fileToString(filePath + "feature1.txt", "UTF-8", false);
		JSONObject jsonF = new JSONObject(feature);
		jsonF.put("name", "F1");
		jsonF.put("namespace", "ns1");
		jsonF.put("entitlement", inAppPurID1);
		jsonF.put("premium", true);
		JSONObject rule = new JSONObject();
		rule.put("ruleString", "true");
		jsonF.put("premiumRule", rule);
		featureID1 = f.addFeature(seasonID, jsonF.toString(), "ROOT", sessionToken);
		Assert.assertFalse(featureID1.contains("error"), "cannot create feature: " + featureID1);
		
		String featureStr = f.getFeature(featureID1, sessionToken);
		JSONObject featureObj = new JSONObject(featureStr);
		Assert.assertTrue(featureObj.getString("entitlement").equals(inAppPurID1), "the purchase is in the feature is wrong");
			
		jsonF.put("name", "F2");
		jsonF.put("namespace", "ns1");
		jsonF.put("entitlement", inAppPurID2);
		jsonF.put("premium", true);
		featureID2 = f.addFeature(seasonID, jsonF.toString(), "ROOT", sessionToken);
		Assert.assertTrue(featureID2.contains("error"), "can add feature attached to entitlement with no store product id");
		
		jsonIP.put("name", RandomStringUtils.randomAlphabetic(5));
		puOptID5 = purchasesApi.addPurchaseItem(seasonID, jsonIP.toString(), inAppPurID2, sessionToken);
		Assert.assertFalse (puOptID5.contains("error"), "Can't add purchaseOptions: " + puOptID5);	
		
		featureID2 = f.addFeature(seasonID, jsonF.toString(), "ROOT", sessionToken);
		Assert.assertFalse(featureID2.contains("error"), "cannot create feature: " + featureID2);
		
		featureStr = f.getFeature(featureID2, sessionToken);
		featureObj = new JSONObject(featureStr);
		Assert.assertTrue(featureObj.getString("entitlement").equals(inAppPurID2), "the purchase is in the feature is wrong");
	}

	@Test (dependsOnMethods = "addComponents", description = "update features premium to false")
	public void updateFeaturePremiumFlag() throws JSONException, IOException, InterruptedException{
		String featureStr = f.getFeature(featureID1, sessionToken);
		JSONObject featureObj = new JSONObject(featureStr);
		Assert.assertTrue(featureObj.getBoolean("premium")==true, "feature is not premium");
		Assert.assertTrue(featureObj.getString("entitlement").equals(inAppPurID1), "wrong inAppPurchase");
		Assert.assertTrue(featureObj.getJSONObject("premiumRule").getString("ruleString").equals("true"), "wrong rule");
		
		
		featureObj.put("premium", false);
		String response = f.updateFeature(seasonID, featureID1, featureObj.toString(), sessionToken);
		Assert.assertTrue(response.contains("error"), "can specify entitlement for non premium feature");
		
		featureObj.put("entitlement", "");
		response = f.updateFeature(seasonID, featureID1, featureObj.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "cannot update feature: " + response);
		
		featureStr = f.getFeature(featureID1, sessionToken);
		featureObj = new JSONObject(featureStr);
		Assert.assertTrue(featureObj.getBoolean("premium")==false, "feature is not premium");
		Assert.assertTrue(featureObj.getString("entitlement").equals(""), "wrong inAppPurchase");
		Assert.assertTrue(featureObj.getJSONObject("premiumRule").getString("ruleString").equals("true"), "wrong rule");
	}

	@Test (dependsOnMethods = "updateFeaturePremiumFlag", description = "update feature satge to production")
	public void updateFeatureToProduction() throws JSONException, IOException, InterruptedException{
		//inAppPurID1
		String featureStr = f.getFeature(featureID1, sessionToken);
		JSONObject featureObj = new JSONObject(featureStr);
		featureObj.put("premium", true);
		featureObj.put("entitlement", inAppPurID1);
		String response = f.updateFeature(seasonID, featureID1, featureObj.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "cannot update feature: " + response);
		
		featureStr = f.getFeature(featureID1, sessionToken);
		featureObj = new JSONObject(featureStr);
		featureObj.put("stage", "PRODUCTION");
		response = f.updateFeature(seasonID, featureID1, featureObj.toString(), sessionToken);
		Assert.assertTrue(response.contains("error") && response.contains("production") && response.contains("development"), "can update feature: " + response);
	}

	@Test (dependsOnMethods = "updateFeatureToProduction", description = "update purcahse satge to production")
	public void updatePurcahseToProduction() throws JSONException, IOException, InterruptedException{
		String pur = purchasesApi.getPurchaseItem(inAppPurID1, sessionToken);
		JSONObject purJson = new JSONObject(pur);
		purJson.put("stage", "PRODUCTION");
		String response = purchasesApi.updatePurchaseItem(seasonID, inAppPurID1, purJson.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "cannot update purcahse: " + response);

		String featureStr = f.getFeature(featureID1, sessionToken);
		JSONObject featureObj = new JSONObject(featureStr);
		featureObj.put("stage", "PRODUCTION");
		response = f.updateFeature(seasonID, featureID1, featureObj.toString(), sessionToken);
		Assert.assertTrue(response.contains("error"), "can update feature to production even though the purchase options with the store ids is in development: " + response);
		
		//move purchase options to production
		String purOpt = purchasesApi.getPurchaseItem(puOptID3, sessionToken);
		JSONObject purOptJson = new JSONObject(purOpt);
		purOptJson.put("stage", "PRODUCTION");
		response = purchasesApi.updatePurchaseItem(seasonID, puOptID3, purOptJson.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "cannot update purcahse options: " + response);

		response = f.updateFeature(seasonID, featureID1, featureObj.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "cannot update feature: " + response);
	}

	@Test (dependsOnMethods = "updatePurcahseToProduction", description = "update purcahse satge to development")
	public void updatePurchaseToDevelopment() throws JSONException, IOException, InterruptedException{
		String featureStr = f.getFeature(featureID1, sessionToken);
		JSONObject featureObj = new JSONObject(featureStr);
		featureObj.put("premium", true);
		String response = f.updateFeature(seasonID, featureID1, featureObj.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "cannot update feature: " + response);
		
		String pur = purchasesApi.getPurchaseItem(inAppPurID1, sessionToken);
		JSONObject purJson = new JSONObject(pur);
		purJson.put("stage", "DEVELOPMENT");
		response = purchasesApi.updatePurchaseItem(seasonID, inAppPurID1, purJson.toString(), sessionToken);
		Assert.assertTrue(response.contains("error") && response.contains("production") && response.contains("development") && response.contains("ns1.F1"), "can update purcahse: " + response);
	}
	
	@Test (dependsOnMethods = "updatePurchaseToDevelopment", description = "update purcahse satge to production")
	public void updateFeaturePurcahseToDevelopment() throws JSONException, IOException, InterruptedException{

		String featureStr = f.getFeature(featureID1, sessionToken);
		JSONObject featureObj = new JSONObject(featureStr);
		featureObj.put("stage", "DEVELOPMENT");
		String response = f.updateFeature(seasonID, featureID1, featureObj.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "cannot update feature: " + response);
		
		String pur = purchasesApi.getPurchaseItem(inAppPurID1, sessionToken);
		JSONObject purJson = new JSONObject(pur);
		purJson.put("stage", "DEVELOPMENT");
		response = purchasesApi.updatePurchaseItem(seasonID, inAppPurID1, purJson.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "cannot update purcahse: " + response);
	}

	@Test (dependsOnMethods = "updateFeaturePurcahseToDevelopment", description = "add purcahse without rule")
	public void addPremiumFeatureWithoutRule() throws JSONException, IOException, InterruptedException{
		String feature = FileUtils.fileToString(filePath + "feature1.txt", "UTF-8", false);
		JSONObject jsonF = new JSONObject(feature);
		jsonF.put("name", "F11");
		jsonF.put("namespace", "ns1");
		jsonF.put("entitlement", inAppPurID1);
		jsonF.put("premium", true);
		jsonF.remove("premiumRule");
		String res = f.addFeature(seasonID, jsonF.toString(), "ROOT", sessionToken);
		Assert.assertTrue(res.contains("error"), "can create premium feature without rule");
		
		jsonF.put("premiumRule", JSONObject.NULL);
		res = f.addFeature(seasonID, jsonF.toString(), "ROOT", sessionToken);
		Assert.assertTrue(res.contains("error"), "can create premium feature with null rule");
	}
	
	@Test (dependsOnMethods = "addPremiumFeatureWithoutRule", description = "update purcahse without rule")
	public void updatePremiumFeatureWithoutRule() throws JSONException, IOException, InterruptedException{
		String feature = f.getFeature(featureID1, sessionToken);
		JSONObject jsonF = new JSONObject(feature);
		jsonF.put("name", "F11");
		jsonF.put("namespace", "ns1");
		jsonF.put("entitlement", inAppPurID1);
		jsonF.put("premium", true);
		jsonF.remove("premiumRule");
		String res = f.updateFeature(seasonID, featureID1, jsonF.toString(), sessionToken);
		Assert.assertTrue(res.contains("error"), "can create premium feature without rule");
		
		jsonF.put("premiumRule", JSONObject.NULL);
		res = f.updateFeature(seasonID, featureID1, jsonF.toString(), sessionToken);
		Assert.assertTrue(res.contains("error"), "can create premium feature with null rule");
	}
	
	@Test (dependsOnMethods = "updatePremiumFeatureWithoutRule", description = "update feature to entitlement without store product id")
	public void updatePremiumFeatureToEntitlementWithoutStoreProdId() throws JSONException, IOException, InterruptedException{
		
		String inAppPur = FileUtils.fileToString(filePath + "purchases/inAppPurchase3.txt", "UTF-8", false);
		inAppPurID3 = purchasesApi.addPurchaseItem(seasonID, inAppPur, "ROOT", sessionToken);
		Assert.assertFalse (inAppPurID3.contains("error"), "Can't add inAppPurchase: " + inAppPurID3);
		
		String purchaseOptions = FileUtils.fileToString(filePath + "purchases/purchaseOptions1.txt", "UTF-8", false);
		JSONObject jsonIP = new JSONObject(purchaseOptions);
		jsonIP.put("name", RandomStringUtils.randomAlphabetic(5));
		jsonIP.put("storeProductIds", new JSONArray());
		puOptID1 = purchasesApi.addPurchaseItem(seasonID, jsonIP.toString(), inAppPurID3, sessionToken);
		Assert.assertFalse (puOptID1.contains("error"), "Can't add purchaseOptions: " + puOptID1);
		
		String feature = f.getFeature(featureID1, sessionToken);
		JSONObject jsonF = new JSONObject(feature);
		jsonF.put("entitlement", inAppPurID3);
		String res = f.updateFeature(seasonID, featureID1, jsonF.toString(), sessionToken);
		Assert.assertTrue(res.contains("error"), "can update feature attached to entitlement with no store product id");
		
		String purchaseOptionsMix = FileUtils.fileToString(filePath + "purchases/purchaseOptionsMutual.txt", "UTF-8", false);
		puOptMtxID1 = purchasesApi.addPurchaseItem(seasonID, purchaseOptionsMix, inAppPurID3, sessionToken);
		Assert.assertFalse (puOptMtxID1.contains("error"), "Can't add purchaseOptions mix: " + puOptMtxID1);
		
		res = f.updateFeature(seasonID, featureID1, jsonF.toString(), sessionToken);
		Assert.assertTrue(res.contains("error"), "can update feature attached to entitlement with no store product id");
		
		jsonIP = new JSONObject(purchaseOptions); //with storeProductIds
		jsonIP.put("name", RandomStringUtils.randomAlphabetic(5));
		puOptID2 = purchasesApi.addPurchaseItem(seasonID, jsonIP.toString(), puOptMtxID1, sessionToken);
		Assert.assertFalse (puOptID2.contains("error"), "Can't add purchaseOptions: " + puOptID2);
		
		res = f.updateFeature(seasonID, featureID1, jsonF.toString(), sessionToken);
		Assert.assertFalse(res.contains("error"), "cannot update feature attached to entitlement with store product id: " + res);
	}
	
	@Test (dependsOnMethods = "updatePremiumFeatureToEntitlementWithoutStoreProdId", description = "delete  purchase options of entitlement attached to premium feature")
	public void deletePurchaseOptions() throws JSONException, IOException, InterruptedException{
		int code = purchasesApi.deletePurchaseItem(puOptID2, sessionToken);
		Assert.assertTrue(code!=200, "can delete purchase options that belongs to entitlement attached to feature and cause entitlement to be without any store product id");
		
		code = purchasesApi.deletePurchaseItem(puOptMtxID1, sessionToken);
		Assert.assertTrue(code!=200, "can delete purchase options mtx that belongs to entitlement attached to feature and cause entitlement to be without any store product id");
		
		code = purchasesApi.deletePurchaseItem(puOptID1, sessionToken);
		Assert.assertTrue(code==200, "cannot delete purchase options");
	}
	
	@Test (dependsOnMethods = "deletePurchaseOptions", description = "update  purchase options of entitlement attached to premium feature")
	public void updatePurchaseOptions() throws JSONException, IOException, InterruptedException{
		String pur = purchasesApi.getPurchaseItem(puOptID2, sessionToken);
		JSONObject purJson = new JSONObject(pur);
		purJson.put("storeProductIds", new JSONArray());
		String response = purchasesApi.updatePurchaseItem(seasonID, puOptID2, purJson.toString(), sessionToken);
		Assert.assertTrue(response.contains("error"), "can update purcahse that removes all store product id from entitlement attached to premium feature ");
	}
	
	@Test (dependsOnMethods = "updatePurchaseOptions", description = "create feature in production while the purchaseOptions is in dev")
	public void createFeatureInProdPurchaseOptionsInDev() throws JSONException, IOException, InterruptedException{
		String inAppPur = FileUtils.fileToString(filePath + "purchases/inAppPurchase1.txt", "UTF-8", false);
		JSONObject inJson = new JSONObject(inAppPur);
		inJson.put("stage", "PRODUCTION");
		inJson.put("name", "IAP4");
		inAppPurID4 = purchasesApi.addPurchaseItem(seasonID, inJson.toString(), "ROOT", sessionToken);
		Assert.assertFalse (inAppPurID4.contains("error"), "Can't add inAppPurchase: " + inAppPurID4);
		
		String purchaseOptions = FileUtils.fileToString(filePath + "purchases/purchaseOptions1.txt", "UTF-8", false);
		JSONObject jsonIP = new JSONObject(purchaseOptions);
		jsonIP.put("name", RandomStringUtils.randomAlphabetic(5));
		jsonIP.put("stage", "DEVELOPMENT");
		puOptID6 = purchasesApi.addPurchaseItem(seasonID, jsonIP.toString(), inAppPurID4, sessionToken);
		Assert.assertFalse (puOptID6.contains("error"), "Can't add purchaseOptions: " + puOptID6);
		
		String feature = FileUtils.fileToString(filePath + "feature1.txt", "UTF-8", false);
		JSONObject jsonF = new JSONObject(feature);
		jsonF.put("name", "F6");
		jsonF.put("namespace", "ns1");
		jsonF.put("entitlement", inAppPurID4);
		jsonF.put("premium", true);
		JSONObject rule = new JSONObject();
		rule.put("ruleString", "true");
		jsonF.put("premiumRule", rule);
		jsonF.put("stage", "PRODUCTION");
		featureID3 = f.addFeature(seasonID, jsonF.toString(), "ROOT", sessionToken);
		Assert.assertTrue(featureID3.contains("error"), "can craetre feature in production even though the purchase options with the store ids is in development");		
	}

	@Test (dependsOnMethods = "createFeatureInProdPurchaseOptionsInDev", description = "update purchase options to dev")
	public void updatePurchaseOptionsToDev() throws JSONException, IOException, InterruptedException{
		String purOpt = purchasesApi.getPurchaseItem(puOptID6, sessionToken);
		JSONObject purJson = new JSONObject(purOpt);
		purJson.put("stage", "PRODUCTION");
		String res = purchasesApi.updatePurchaseItem(seasonID, puOptID6, purJson.toString(), sessionToken);
		Assert.assertFalse (res.contains("error"), "Can't add purchaseOptions: " + res);
		
		String feature = FileUtils.fileToString(filePath + "feature1.txt", "UTF-8", false);
		JSONObject jsonF = new JSONObject(feature);
		jsonF.put("name", "F6");
		jsonF.put("namespace", "ns1");
		jsonF.put("entitlement", inAppPurID4);
		jsonF.put("premium", true);
		JSONObject rule = new JSONObject();
		rule.put("ruleString", "true");
		jsonF.put("premiumRule", rule);
		jsonF.put("stage", "PRODUCTION");
		featureID4 = f.addFeature(seasonID, jsonF.toString(), "ROOT", sessionToken);
		Assert.assertFalse(featureID4.contains("error"), "cannot craetre feature in production even though the purchase options with the store ids is in production: " + featureID4);
		
		purOpt = purchasesApi.getPurchaseItem(puOptID6, sessionToken);
		purJson = new JSONObject(purOpt);
		purJson.put("stage", "DEVELOPMENT");
		res = purchasesApi.updatePurchaseItem(seasonID, puOptID6, purJson.toString(), sessionToken);
		Assert.assertTrue (res.contains("error"), "can update purchase options to devlopment even though it leaves feature without storeProductId in production");		
	}
	
	@Test (dependsOnMethods = "updatePurchaseOptionsToDev", description = "create feature in production while the purchaseOptions is in dev")
	public void createFeatureInProdPurchaseOptionsInProd() throws JSONException, IOException, InterruptedException{
		String inAppPur = FileUtils.fileToString(filePath + "purchases/inAppPurchase1.txt", "UTF-8", false);
		JSONObject inJson = new JSONObject(inAppPur);
		inJson.put("stage", "PRODUCTION");
		inJson.put("name", "IAP5");
		inAppPurID5 = purchasesApi.addPurchaseItem(seasonID, inJson.toString(), "ROOT", sessionToken);
		Assert.assertFalse (inAppPurID5.contains("error"), "Can't add inAppPurchase: " + inAppPurID5);
		
		String purchaseOptions = FileUtils.fileToString(filePath + "purchases/purchaseOptions1.txt", "UTF-8", false);
		JSONObject jsonIP = new JSONObject(purchaseOptions);
		jsonIP.put("name", RandomStringUtils.randomAlphabetic(5));
		jsonIP.put("stage", "PRODUCTION");
		puOptID7 = purchasesApi.addPurchaseItem(seasonID, jsonIP.toString(), inAppPurID5, sessionToken);
		Assert.assertFalse (puOptID7.contains("error"), "Can't add purchaseOptions: " + puOptID7);
		
		String feature = FileUtils.fileToString(filePath + "feature1.txt", "UTF-8", false);
		JSONObject jsonF = new JSONObject(feature);
		jsonF.put("name", "F7");
		jsonF.put("namespace", "ns1");
		jsonF.put("entitlement", inAppPurID5);
		jsonF.put("premium", true);
		JSONObject rule = new JSONObject();
		rule.put("ruleString", "true");
		jsonF.put("premiumRule", rule);
		jsonF.put("stage", "PRODUCTION");
		featureID5 = f.addFeature(seasonID, jsonF.toString(), "ROOT", sessionToken);
		Assert.assertFalse(featureID5.contains("error"), "can craetre feature :" + featureID5);		
		
		//try to move entitlement to dev
		String entitlement = purchasesApi.getPurchaseItem(inAppPurID5, sessionToken);
		JSONObject entitlementJson = new JSONObject(entitlement);
		entitlementJson.put("stage", "DEVELOPMENT");
		String res = purchasesApi.updatePurchaseItem(seasonID, inAppPurID5, entitlementJson.toString(), sessionToken);
		Assert.assertTrue (res.contains("error"), "can update entitlement to devlopment even though premium feature in production");
		
		//update feature premium = false
		String featureStr = f.getFeature(featureID5, sessionToken);
		JSONObject fJson = new JSONObject(featureStr);
		fJson.put("premium", false);
		fJson.put("entitlement", "");
		res = f.updateFeature(seasonID, featureID5, fJson.toString(), sessionToken);
		Assert.assertFalse(res.contains("error"), "cannot update feature :" + res);		
		
		//update entitlement to dev
		res = purchasesApi.updatePurchaseItem(seasonID, inAppPurID5, entitlementJson.toString(), sessionToken);
		Assert.assertFalse (res.contains("error"), "cannot update entitlement to devlopment even though feature in production not premium any more:" + res);
		
		//update feature premium = false
		featureStr = f.getFeature(featureID5, sessionToken);
		fJson = new JSONObject(featureStr);
		fJson.put("premium", true);
		fJson.put("entitlement", inAppPurID5);
		res = f.updateFeature(seasonID, featureID5, fJson.toString(), sessionToken);
		Assert.assertTrue(res.contains("error"), "can update production feature to premium with dev entitlements");		
	}

	@Test (dependsOnMethods = "createFeatureInProdPurchaseOptionsInProd", description = "delete Entitlement Attached To Premium Feature")
	public void deleteEntitlementAttachedToPremiumFeature() throws JSONException, IOException, InterruptedException{
		//update feature to dev
		String featureStr = f.getFeature(featureID5, sessionToken);
		JSONObject fJson = new JSONObject(featureStr);
		fJson.put("stage", "DEVELOPMENT");
		fJson.put("premium", true);
		fJson.put("entitlement", inAppPurID5);
		String res = f.updateFeature(seasonID, featureID5, fJson.toString(), sessionToken);
		Assert.assertFalse(res.contains("error"), "can not update feature to development");
		
		//update purchase options to dev
		String purchaseOptions = purchasesApi.getPurchaseItem(puOptID7, sessionToken);
		JSONObject purchaseOptionsJson = new JSONObject(purchaseOptions);
		purchaseOptionsJson.put("stage", "DEVELOPMENT");
		res = purchasesApi.updatePurchaseItem(seasonID, puOptID7, purchaseOptionsJson.toString(), sessionToken);
		Assert.assertFalse (res.contains("error"), "cannot update purchaseOptions to devlopment:" + res);
		
		int code = purchasesApi.deletePurchaseItem(inAppPurID5, sessionToken);
		Assert.assertFalse(code == 200, "can delete entitlement taht is attached to premium feature"); 
		
		//update feature premium = true
		featureStr = f.getFeature(featureID5, sessionToken);
		fJson = new JSONObject(featureStr);
		fJson.put("premium", false);
		fJson.put("entitlement", JSONObject.NULL);
		res = f.updateFeature(seasonID, featureID5, fJson.toString(), sessionToken);
		Assert.assertFalse(res.contains("error"), "cannot update feature to non-premium: " + res);
		
		code = purchasesApi.deletePurchaseItem(inAppPurID5, sessionToken);
		Assert.assertTrue(code == 200, "cannot delete entitlement that is attached to non-premium feature"); 
	}
	
	
	@AfterTest
	private void reset(){
		baseUtils.reset(productID, sessionToken);
	}
}
