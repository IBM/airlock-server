package tests.restapi.in_app_purchases;

import org.apache.wink.json4j.JSONException;
import org.apache.wink.json4j.JSONObject;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.wink.json4j.JSONArray;
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
import tests.restapi.RuntimeDateUtilities;
import tests.restapi.RuntimeRestApi;


public class CheckPurchaseDeltaInRuntime {
	protected String productID;
	protected String seasonID;
	private String branchID;
	private String inAppPurchaseID1;
	private String inAppPurchaseID2;
	protected String featureID1;
	
	protected FeaturesRestApi f;
	private String iapMixID1;
	private String poMixID1;
	private String purchaseOptionsID1;
	private JSONObject inAppPurJson;
	private JSONObject purOptJson;
	protected String filePath;
	protected String m_url;
	private String sessionToken = "";
	private AirlockUtils baseUtils;
	private BranchesRestApi br ;
	protected InAppPurchasesRestApi purchasesApi;
	
	
	@BeforeClass
	@Parameters({"url", "analyticsUrl", "translationsUrl", "configPath", "sessionToken", "userName", "userPassword", "appName", "productsToDeleteFile"})
	public void init(String url, String analyticsUrl, String translationsUrl, String configPath, String sToken, String userName, String userPassword, String appName, String productsToDeleteFile) throws Exception{
		m_url = url;
		filePath = configPath ;
		purchasesApi = new InAppPurchasesRestApi();
		purchasesApi.setURL(m_url);
		br = new BranchesRestApi();
		br.setURL(m_url);
		f = new FeaturesRestApi();
		f.setURL(m_url);
		
		baseUtils = new AirlockUtils(m_url, analyticsUrl, translationsUrl, configPath, sToken, userName, userPassword, appName, productsToDeleteFile);
		sessionToken = baseUtils.sessionToken;

		productID = baseUtils.createProduct();
		baseUtils.printProductToFile(productID);
		seasonID = baseUtils.createSeason(productID);
		branchID = baseUtils.createBranchInProdEnabledExperiment(analyticsUrl, true).getString("brId");
		
		String inAppPur = FileUtils.fileToString(filePath + "purchases/inAppPurchase1.txt", "UTF-8", false);
		inAppPurJson = new JSONObject(inAppPur);
		
		String purOpt = FileUtils.fileToString(filePath + "purchases/purchaseOptions1.txt", "UTF-8", false);
		purOptJson = new JSONObject(purOpt);		
		
		purOptJson.put("storeProductIds", new JSONArray());
	}
	

	
	@Test (description ="IAP1 ->  IAPMIX1 -> IAP2 -> POMIX1 - > PO1,    F1(premium feature for inAppPurchaseID2") 
	public void addComponents () throws Exception {
		
		inAppPurJson.put("name", "IAP1");
		inAppPurJson.put("stage", "PRODUCTION");
		inAppPurchaseID1 = purchasesApi.addPurchaseItem(seasonID, inAppPurJson.toString(), "ROOT", sessionToken);
		Assert.assertFalse(inAppPurchaseID1.contains("error"), "inAppPurchase was not added to the season: " + inAppPurchaseID1);
	
		String iapMix = FileUtils.fileToString(filePath + "purchases/inAppPurchaseMutual.txt", "UTF-8", false);
		iapMixID1 = purchasesApi.addPurchaseItem(seasonID, iapMix, inAppPurchaseID1, sessionToken);
		Assert.assertFalse(iapMixID1.contains("error"), "inAppPurchase was not added to the season: " + iapMixID1);
				
		inAppPurJson.put("name", "IAP2");
		inAppPurJson.put("stage", "PRODUCTION");
		inAppPurchaseID2 = purchasesApi.addPurchaseItem(seasonID, inAppPurJson.toString(), iapMixID1, sessionToken);
		Assert.assertFalse(inAppPurchaseID2.contains("error"), "inAppPurchase was not added to the season: " + inAppPurchaseID2);
		
		String poMix = FileUtils.fileToString(filePath + "purchases/purchaseOptionsMutual.txt", "UTF-8", false);
		poMixID1 = purchasesApi.addPurchaseItem(seasonID, poMix, inAppPurchaseID2, sessionToken);
		Assert.assertFalse(poMixID1.contains("error"), "purchase options mix was not added to the season: " + poMixID1);
		
		purOptJson.put("name", "PO1");
		purOptJson.put("stage", "PRODUCTION");
		purchaseOptionsID1 = purchasesApi.addPurchaseItem(seasonID, purOptJson.toString(), poMixID1, sessionToken);
		Assert.assertFalse(purchaseOptionsID1.contains("error"), "purchase options was not added to the season: " + purchaseOptionsID1);
		
		String feature = FileUtils.fileToString(filePath + "feature1.txt", "UTF-8", false);
		JSONObject jsonF = new JSONObject(feature);
		jsonF.put("name", "F1");
		jsonF.put("namespace", "ns1");
		jsonF.put("stage", "PRODUCTION");
		jsonF.put("entitlement", inAppPurchaseID2);
		jsonF.put("premium", true);
		JSONObject rule = new JSONObject();
		rule.put("ruleString", "true");
		jsonF.put("premiumRule", rule);
		
		featureID1 = f.addFeature(seasonID, jsonF.toString(), "ROOT", sessionToken);
		Assert.assertTrue(featureID1.contains("error"), "can create feature attached to purcahse without store product id");
		
		String purchaseOptions = FileUtils.fileToString(filePath + "purchases/purchaseOptions1.txt", "UTF-8", false);
		JSONObject jsonIP = new JSONObject(purchaseOptions);
		jsonIP.put("name", RandomStringUtils.randomAlphabetic(5));
		jsonIP.put("stage", "PRODUCTION");
		String purchaseOptionsID = purchasesApi.addPurchaseItem(seasonID, jsonIP.toString(), inAppPurchaseID2, sessionToken);
		Assert.assertFalse (purchaseOptionsID.contains("error"), "Can't add purchaseOptions: " + purchaseOptionsID);

		featureID1 = f.addFeature(seasonID, jsonF.toString(), "ROOT", sessionToken);
		Assert.assertFalse(featureID1.contains("error"), "cannot create feature : " + featureID1);
	}
	
	@Test (dependsOnMethods="addComponents", description="Checkout P01")
	public void checkout() throws JSONException, Exception{				
		String dateFormat = purchasesApi.setDateFormat();
		
		String res = br.checkoutFeature(branchID, purchaseOptionsID1, sessionToken); //checks out the whole tree 
		Assert.assertFalse(res.contains("error") || res.contains("Invalid response"), "purchaseOptionsID1 was not checked out: " + res);
		
		purchasesApi.setSleep();
		RuntimeRestApi.DateModificationResults branchesRuntimeDev = RuntimeDateUtilities.getDevelopmentBranchFileDateModification(m_url, productID, seasonID, branchID, dateFormat, sessionToken);
		Assert.assertTrue(branchesRuntimeDev.code==200, "Branched runtime development file was not changed");
		JSONObject result = new JSONObject(branchesRuntimeDev.message);
		JSONObject inAppPur1Obj = result.getJSONArray("entitlements").getJSONObject(0);
		Assert.assertFalse(inAppPur1Obj.containsKey("rolloutPercentage"));
		Assert.assertFalse(inAppPur1Obj.containsKey("includedEntitlements"));		
		JSONObject purOpt1Obj = inAppPur1Obj.getJSONArray("entitlements").getJSONObject(0).getJSONArray("entitlements").getJSONObject(0).getJSONArray("purchaseOptions").getJSONObject(0).getJSONArray("purchaseOptions").getJSONObject(0);
		Assert.assertFalse(purOpt1Obj.containsKey("noCachedResults"));
		Assert.assertFalse(purOpt1Obj.containsKey("storeProductIds"));
		
		RuntimeRestApi.DateModificationResults branchesRuntimeProd = RuntimeDateUtilities.getProductionBranchFileDateModification(m_url, productID, seasonID, branchID, dateFormat, sessionToken);
		Assert.assertTrue(branchesRuntimeProd.code==200, "Branched runtime production file was not changed");
		result = new JSONObject(branchesRuntimeProd.message);
		inAppPur1Obj = result.getJSONArray("entitlements").getJSONObject(0);
		Assert.assertFalse(inAppPur1Obj.containsKey("rolloutPercentage"));
		Assert.assertFalse(inAppPur1Obj.containsKey("includedEntitlements"));		
		purOpt1Obj = inAppPur1Obj.getJSONArray("entitlements").getJSONObject(0).getJSONArray("entitlements").getJSONObject(0).getJSONArray("purchaseOptions").getJSONObject(0).getJSONArray("purchaseOptions").getJSONObject(0);
		Assert.assertFalse(purOpt1Obj.containsKey("noCachedResults"));
		Assert.assertFalse(purOpt1Obj.containsKey("storeProductIds"));
		
		RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getDevelopmentFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);		
		Assert.assertTrue(responseDev.code==200, "Runtime development file was not changed");
		result = new JSONObject(responseDev.message);
		inAppPur1Obj = result.getJSONArray("branches").getJSONObject(0).getJSONArray("entitlements").getJSONObject(0);
		Assert.assertFalse(inAppPur1Obj.containsKey("rolloutPercentage"));
		Assert.assertFalse(inAppPur1Obj.containsKey("includedEntitlements"));
		purOpt1Obj = inAppPur1Obj.getJSONArray("entitlements").getJSONObject(0).getJSONArray("entitlements").getJSONObject(0).getJSONArray("purchaseOptions").getJSONObject(0).getJSONArray("purchaseOptions").getJSONObject(0);
		Assert.assertFalse(purOpt1Obj.containsKey("noCachedResults"));
		Assert.assertFalse(purOpt1Obj.containsKey("storeProductIds"));
		
		RuntimeRestApi.DateModificationResults responseProd = RuntimeDateUtilities.getProductionFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseProd.code==200, "Runtime production file was changed");
		result = new JSONObject(responseProd.message);
		inAppPur1Obj = result.getJSONArray("branches").getJSONObject(0).getJSONArray("entitlements").getJSONObject(0);
		Assert.assertFalse(inAppPur1Obj.containsKey("rolloutPercentage"));
		Assert.assertFalse(inAppPur1Obj.containsKey("includedEntitlements"));		
		purOpt1Obj = inAppPur1Obj.getJSONArray("entitlements").getJSONObject(0).getJSONArray("entitlements").getJSONObject(0).getJSONArray("purchaseOptions").getJSONObject(0).getJSONArray("purchaseOptions").getJSONObject(0);
		Assert.assertFalse(purOpt1Obj.containsKey("noCachedResults"));
		Assert.assertFalse(purOpt1Obj.containsKey("storeProductIds"));
		
	}
	
	@Test (dependsOnMethods="checkout", description="update IAP1 includedPurcahses")
	public void updateIncludedPurchases() throws JSONException, Exception{				
		String dateFormat = purchasesApi.setDateFormat();
		
		String purStr = purchasesApi.getPurchaseItemFromBranch(inAppPurchaseID1, branchID, sessionToken);
		JSONObject purObj = new JSONObject(purStr);
		JSONArray includedPurchases = purObj.getJSONArray("includedEntitlements");
		includedPurchases.add(inAppPurchaseID2);
		purObj.put("includedEntitlements", includedPurchases);
		purObj.put("rolloutPercentage", 17);
		
		String response = purchasesApi.updatePurchaseItemInBranch(seasonID, branchID,  inAppPurchaseID1, purObj.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "cannot update inAppPurchaseID1 to include inAppPurchaseID2 in branch: " + response);
		
		purchasesApi.setSleep();
		RuntimeRestApi.DateModificationResults branchesRuntimeDev = RuntimeDateUtilities.getDevelopmentBranchFileDateModification(m_url, productID, seasonID, branchID, dateFormat, sessionToken);
		Assert.assertTrue(branchesRuntimeDev.code==200, "Branched runtime development file was not changed");
		JSONObject result = new JSONObject(branchesRuntimeDev.message);
		JSONObject inAppPur1Obj = result.getJSONArray("entitlements").getJSONObject(0);
		Assert.assertTrue(inAppPur1Obj.containsKey("rolloutPercentage"));
		Assert.assertTrue(inAppPur1Obj.containsKey("includedEntitlements"));
		Assert.assertTrue(inAppPur1Obj.getInt("rolloutPercentage") == 17);
		Assert.assertTrue(inAppPur1Obj.getJSONArray("includedEntitlements").getString(0).equals("ns1.IAP2"));
		
		RuntimeRestApi.DateModificationResults branchesRuntimeProd = RuntimeDateUtilities.getProductionBranchFileDateModification(m_url, productID, seasonID, branchID, dateFormat, sessionToken);
		Assert.assertTrue(branchesRuntimeProd.code==200, "Branched runtime production file was not changed");
		result = new JSONObject(branchesRuntimeProd.message);
		inAppPur1Obj = result.getJSONArray("entitlements").getJSONObject(0);
		Assert.assertTrue(inAppPur1Obj.containsKey("rolloutPercentage"));
		Assert.assertTrue(inAppPur1Obj.containsKey("includedEntitlements"));
		Assert.assertTrue(inAppPur1Obj.getInt("rolloutPercentage") == 17);
		Assert.assertTrue(inAppPur1Obj.getJSONArray("includedEntitlements").getString(0).equals("ns1.IAP2"));
		
		RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getDevelopmentFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);		
		Assert.assertTrue(responseDev.code==200, "Runtime development file was not changed");
		result = new JSONObject(responseDev.message);
		inAppPur1Obj = result.getJSONArray("branches").getJSONObject(0).getJSONArray("entitlements").getJSONObject(0);
		Assert.assertTrue(inAppPur1Obj.containsKey("rolloutPercentage"));
		Assert.assertTrue(inAppPur1Obj.containsKey("includedEntitlements"));
		Assert.assertTrue(inAppPur1Obj.getInt("rolloutPercentage") == 17);
		Assert.assertTrue(inAppPur1Obj.getJSONArray("includedEntitlements").getString(0).equals("ns1.IAP2"));
		
		RuntimeRestApi.DateModificationResults responseProd = RuntimeDateUtilities.getProductionFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseProd.code==200, "Runtime production file was changed");
		result = new JSONObject(responseProd.message);
		inAppPur1Obj = result.getJSONArray("branches").getJSONObject(0).getJSONArray("entitlements").getJSONObject(0);
		Assert.assertTrue(inAppPur1Obj.containsKey("rolloutPercentage"));
		Assert.assertTrue(inAppPur1Obj.containsKey("includedEntitlements"));
		Assert.assertTrue(inAppPur1Obj.getInt("rolloutPercentage") == 17);
		Assert.assertTrue(inAppPur1Obj.getJSONArray("includedEntitlements").getString(0).equals("ns1.IAP2"));
		
	}

	@Test (dependsOnMethods="updateIncludedPurchases", description="update PO1 in master")
	public void updatePurchaseOptions() throws JSONException, Exception{				
		String dateFormat = purchasesApi.setDateFormat();
		
		String purStr = purchasesApi.getPurchaseItemFromBranch(purchaseOptionsID1, "MASTER", sessionToken);
		JSONObject purObj = new JSONObject(purStr);
		JSONArray storeProductIdsArr = purObj.getJSONArray("storeProductIds");
		JSONObject storeProdId = new JSONObject();
		storeProdId.put("storeType", "Google Play Store");
		storeProdId.put("productId", "123");
		storeProductIdsArr.add(storeProdId);
		purObj.put("storeProductIds", storeProductIdsArr);
		purObj.put("noCachedResults", true);
		String response = purchasesApi.updatePurchaseItemInBranch(seasonID, "MASTER",  purchaseOptionsID1, purObj.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "cannot update purchaseOptionsID1 in branch: " + response);
		
		purchasesApi.setSleep();
		RuntimeRestApi.DateModificationResults branchesRuntimeDev = RuntimeDateUtilities.getDevelopmentBranchFileDateModification(m_url, productID, seasonID, branchID, dateFormat, sessionToken);
		Assert.assertTrue(branchesRuntimeDev.code==200, "Branched runtime development file was not changed");
		JSONObject result = new JSONObject(branchesRuntimeDev.message);
		JSONObject purOpt1Obj = result.getJSONArray("entitlements").getJSONObject(0).getJSONArray("entitlements").getJSONObject(0).getJSONArray("entitlements").getJSONObject(0).getJSONArray("purchaseOptions").getJSONObject(0).getJSONArray("purchaseOptions").getJSONObject(0);
		Assert.assertTrue(purOpt1Obj.containsKey("noCachedResults"));
		Assert.assertTrue(purOpt1Obj.containsKey("storeProductIds"));
		Assert.assertTrue(purOpt1Obj.getBoolean("noCachedResults") == false);  //true in master false in branch
		Assert.assertTrue(purOpt1Obj.getJSONArray("storeProductIds").size() == 0); //1 in master 0 in branch
		
		RuntimeRestApi.DateModificationResults branchesRuntimeProd = RuntimeDateUtilities.getProductionBranchFileDateModification(m_url, productID, seasonID, branchID, dateFormat, sessionToken);
		Assert.assertTrue(branchesRuntimeProd.code==200, "Branched runtime production file was not changed");
		result = new JSONObject(branchesRuntimeProd.message);
		purOpt1Obj = result.getJSONArray("entitlements").getJSONObject(0).getJSONArray("entitlements").getJSONObject(0).getJSONArray("entitlements").getJSONObject(0).getJSONArray("purchaseOptions").getJSONObject(0).getJSONArray("purchaseOptions").getJSONObject(0);
		Assert.assertTrue(purOpt1Obj.containsKey("noCachedResults"));
		Assert.assertTrue(purOpt1Obj.containsKey("storeProductIds"));
		Assert.assertTrue(purOpt1Obj.getBoolean("noCachedResults") == false); //true in master false in branch
		Assert.assertTrue(purOpt1Obj.getJSONArray("storeProductIds").size() == 0); //1 in master 0 in branch
		
		RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getDevelopmentFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);		
		Assert.assertTrue(responseDev.code==200, "Runtime development file was not changed");
		result = new JSONObject(responseDev.message);
		purOpt1Obj = result.getJSONArray("branches").getJSONObject(0).getJSONArray("entitlements").getJSONObject(0).getJSONArray("entitlements").getJSONObject(0).getJSONArray("entitlements").getJSONObject(0).getJSONArray("purchaseOptions").getJSONObject(0).getJSONArray("purchaseOptions").getJSONObject(0);
		Assert.assertTrue(purOpt1Obj.containsKey("noCachedResults"));
		Assert.assertTrue(purOpt1Obj.containsKey("storeProductIds"));
		Assert.assertTrue(purOpt1Obj.getBoolean("noCachedResults") == false); //true in master false in branch
		Assert.assertTrue(purOpt1Obj.getJSONArray("storeProductIds").size() == 0); //1 in master 0 in branch
		
		RuntimeRestApi.DateModificationResults responseProd = RuntimeDateUtilities.getProductionFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseProd.code==200, "Runtime production file was changed");
		result = new JSONObject(responseProd.message);
		purOpt1Obj = result.getJSONArray("branches").getJSONObject(0).getJSONArray("entitlements").getJSONObject(0).getJSONArray("entitlements").getJSONObject(0).getJSONArray("entitlements").getJSONObject(0).getJSONArray("purchaseOptions").getJSONObject(0).getJSONArray("purchaseOptions").getJSONObject(0);
		Assert.assertTrue(purOpt1Obj.containsKey("noCachedResults"));
		Assert.assertTrue(purOpt1Obj.containsKey("storeProductIds"));
		Assert.assertTrue(purOpt1Obj.getBoolean("noCachedResults") == false); //true in master false in branch
		Assert.assertTrue(purOpt1Obj.getJSONArray("storeProductIds").size() == 0); //1 in master 0 in branch
	}

	
	@Test (dependsOnMethods="updatePurchaseOptions", description="update PO1 in branch")
	public void updatePurchaseOptionsInBranch() throws JSONException, Exception{				
		String dateFormat = purchasesApi.setDateFormat();
		
		String res = br.cancelCheckoutFeature(branchID, purchaseOptionsID1, sessionToken); 
		Assert.assertFalse(res.contains("error") || res.contains("Invalid response"), "purchaseOptionsID1 was not checked out: " + res);
		
		res = br.checkoutFeature(branchID, purchaseOptionsID1, sessionToken); //checks out the whole tree 
		Assert.assertFalse(res.contains("error") || res.contains("Invalid response"), "purchaseOptionsID1 was not checked out: " + res);
		
		String purStr = purchasesApi.getPurchaseItemFromBranch(purchaseOptionsID1, branchID, sessionToken);
		JSONObject purObj = new JSONObject(purStr);
		JSONArray storeProductIdsArr = purObj.getJSONArray("storeProductIds");
		JSONObject storeProdId1 = new JSONObject();
		storeProdId1.put("storeType", "Apple App Store");
		storeProdId1.put("productId", "456");
		storeProductIdsArr.add(storeProdId1);
		purObj.put("storeProductIds", storeProductIdsArr);
		purObj.put("noCachedResults", false);
		String response = purchasesApi.updatePurchaseItemInBranch(seasonID, branchID,  purchaseOptionsID1, purObj.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "cannot update purchaseOptionsID1 in branch: " + response);
		
		purchasesApi.setSleep();
		RuntimeRestApi.DateModificationResults branchesRuntimeDev = RuntimeDateUtilities.getDevelopmentBranchFileDateModification(m_url, productID, seasonID, branchID, dateFormat, sessionToken);
		Assert.assertTrue(branchesRuntimeDev.code==200, "Branched runtime development file was not changed");
		JSONObject result = new JSONObject(branchesRuntimeDev.message);
		JSONObject purOpt1Obj = result.getJSONArray("entitlements").getJSONObject(0).getJSONArray("entitlements").getJSONObject(0).getJSONArray("entitlements").getJSONObject(0).getJSONArray("purchaseOptions").getJSONObject(0).getJSONArray("purchaseOptions").getJSONObject(0);
		Assert.assertTrue(purOpt1Obj.containsKey("noCachedResults"));
		Assert.assertTrue(purOpt1Obj.containsKey("storeProductIds"));
		Assert.assertTrue(purOpt1Obj.getBoolean("noCachedResults") == false);  //true in master false in branch
		Assert.assertTrue(purOpt1Obj.getJSONArray("storeProductIds").size() == 2); //1 in master 2 in branch
		
		RuntimeRestApi.DateModificationResults branchesRuntimeProd = RuntimeDateUtilities.getProductionBranchFileDateModification(m_url, productID, seasonID, branchID, dateFormat, sessionToken);
		Assert.assertTrue(branchesRuntimeProd.code==200, "Branched runtime production file was not changed");
		result = new JSONObject(branchesRuntimeProd.message);
		purOpt1Obj = result.getJSONArray("entitlements").getJSONObject(0).getJSONArray("entitlements").getJSONObject(0).getJSONArray("entitlements").getJSONObject(0).getJSONArray("purchaseOptions").getJSONObject(0).getJSONArray("purchaseOptions").getJSONObject(0);
		Assert.assertTrue(purOpt1Obj.containsKey("noCachedResults"));
		Assert.assertTrue(purOpt1Obj.containsKey("storeProductIds"));
		Assert.assertTrue(purOpt1Obj.getBoolean("noCachedResults") == false); //true in master false in branch
		Assert.assertTrue(purOpt1Obj.getJSONArray("storeProductIds").size() == 2); //1 in master 2 in branch
		
		RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getDevelopmentFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);		
		Assert.assertTrue(responseDev.code==200, "Runtime development file was not changed");
		result = new JSONObject(responseDev.message);
		purOpt1Obj = result.getJSONArray("branches").getJSONObject(0).getJSONArray("entitlements").getJSONObject(0).getJSONArray("entitlements").getJSONObject(0).getJSONArray("entitlements").getJSONObject(0).getJSONArray("purchaseOptions").getJSONObject(0).getJSONArray("purchaseOptions").getJSONObject(0);
		Assert.assertTrue(purOpt1Obj.containsKey("noCachedResults"));
		Assert.assertTrue(purOpt1Obj.containsKey("storeProductIds"));
		Assert.assertTrue(purOpt1Obj.getBoolean("noCachedResults") == false); //true in master false in branch
		Assert.assertTrue(purOpt1Obj.getJSONArray("storeProductIds").size() == 2); //1 in master 2 in branch
		
		RuntimeRestApi.DateModificationResults responseProd = RuntimeDateUtilities.getProductionFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseProd.code==200, "Runtime production file was changed");
		result = new JSONObject(responseProd.message);
		purOpt1Obj = result.getJSONArray("branches").getJSONObject(0).getJSONArray("entitlements").getJSONObject(0).getJSONArray("entitlements").getJSONObject(0).getJSONArray("entitlements").getJSONObject(0).getJSONArray("purchaseOptions").getJSONObject(0).getJSONArray("purchaseOptions").getJSONObject(0);
		Assert.assertTrue(purOpt1Obj.containsKey("noCachedResults"));
		Assert.assertTrue(purOpt1Obj.containsKey("storeProductIds"));
		Assert.assertTrue(purOpt1Obj.getBoolean("noCachedResults") == false); //true in master false in branch
		Assert.assertTrue(purOpt1Obj.getJSONArray("storeProductIds").size() == 2); //1 in master 2 in branch
	}

	@Test (dependsOnMethods="updatePurchaseOptionsInBranch", description="Checkout F1")
	public void checkoutFeature() throws JSONException, Exception{				
		String dateFormat = purchasesApi.setDateFormat();
		
		String res = br.checkoutFeature(branchID, featureID1, sessionToken); 
		Assert.assertFalse(res.contains("error") || res.contains("Invalid response"), "featureID1 was not checked out: " + res);
		
		purchasesApi.setSleep();
		RuntimeRestApi.DateModificationResults branchesRuntimeDev = RuntimeDateUtilities.getDevelopmentBranchFileDateModification(m_url, productID, seasonID, branchID, dateFormat, sessionToken);
		Assert.assertTrue(branchesRuntimeDev.code==200, "Branched runtime development file was not changed");
		JSONObject result = new JSONObject(branchesRuntimeDev.message);
		JSONObject feature1Obj = result.getJSONArray("features").getJSONObject(0);
		Assert.assertFalse(feature1Obj.containsKey("premium"));
		Assert.assertFalse(feature1Obj.containsKey("premiumRule"));
		Assert.assertFalse(feature1Obj.containsKey("entitlement"));
		
		RuntimeRestApi.DateModificationResults branchesRuntimeProd = RuntimeDateUtilities.getProductionBranchFileDateModification(m_url, productID, seasonID, branchID, dateFormat, sessionToken);
		Assert.assertTrue(branchesRuntimeProd.code==200, "Branched runtime production file was not changed");
		result = new JSONObject(branchesRuntimeProd.message);
		feature1Obj = result.getJSONArray("features").getJSONObject(0);
		Assert.assertFalse(feature1Obj.containsKey("premium"));
		Assert.assertFalse(feature1Obj.containsKey("premiumRule"));
		Assert.assertFalse(feature1Obj.containsKey("entitlement"));
		
		RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getDevelopmentFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);		
		Assert.assertTrue(responseDev.code==200, "Runtime development file was not changed");
		result = new JSONObject(responseDev.message);
		feature1Obj = result.getJSONArray("branches").getJSONObject(0).getJSONArray("features").getJSONObject(0);
		Assert.assertFalse(feature1Obj.containsKey("premium"));
		Assert.assertFalse(feature1Obj.containsKey("premiumRule"));
		Assert.assertFalse(feature1Obj.containsKey("entitlement"));
		
		RuntimeRestApi.DateModificationResults responseProd = RuntimeDateUtilities.getProductionFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseProd.code==200, "Runtime production file was changed");
		result = new JSONObject(responseProd.message);
		feature1Obj = result.getJSONArray("branches").getJSONObject(0).getJSONArray("features").getJSONObject(0);
		Assert.assertFalse(feature1Obj.containsKey("premium"));
		Assert.assertFalse(feature1Obj.containsKey("premiumRule"));
		Assert.assertFalse(feature1Obj.containsKey("entitlement"));
		
	}
	
	@Test (dependsOnMethods="checkoutFeature", description="update F1 in branch")
	public void updateFeatureInBranch() throws JSONException, Exception{				
		String dateFormat = purchasesApi.setDateFormat();
				
		String fStr = f.getFeatureFromBranch(featureID1, branchID, sessionToken);
		JSONObject fObj = new JSONObject(fStr);
		fObj.put("premium", false);
		fObj.put("entitlement", "");
		String response = f.updateFeatureInBranch(seasonID, branchID,  featureID1, fObj.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "cannot update featureID1 in branch: " + response);
		
		purchasesApi.setSleep();
		RuntimeRestApi.DateModificationResults branchesRuntimeDev = RuntimeDateUtilities.getDevelopmentBranchFileDateModification(m_url, productID, seasonID, branchID, dateFormat, sessionToken);
		Assert.assertTrue(branchesRuntimeDev.code==200, "Branched runtime development file was not changed");
		JSONObject result = new JSONObject(branchesRuntimeDev.message);
		JSONObject feature1Obj = result.getJSONArray("features").getJSONObject(0);
		Assert.assertTrue(feature1Obj.containsKey("premium"));
		Assert.assertTrue(feature1Obj.getBoolean("premium") == false);
		Assert.assertFalse(feature1Obj.containsKey("premiumRule"));
		Assert.assertFalse(feature1Obj.containsKey("entitlement"));
		
		RuntimeRestApi.DateModificationResults branchesRuntimeProd = RuntimeDateUtilities.getProductionBranchFileDateModification(m_url, productID, seasonID, branchID, dateFormat, sessionToken);
		Assert.assertTrue(branchesRuntimeProd.code==200, "Branched runtime production file was not changed");
		result = new JSONObject(branchesRuntimeProd.message);
		feature1Obj = result.getJSONArray("features").getJSONObject(0);
		Assert.assertTrue(feature1Obj.containsKey("premium"));
		Assert.assertTrue(feature1Obj.getBoolean("premium") == false);
		Assert.assertFalse(feature1Obj.containsKey("premiumRule"));
		Assert.assertFalse(feature1Obj.containsKey("entitlement"));
		
		RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getDevelopmentFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);		
		Assert.assertTrue(responseDev.code==200, "Runtime development file was not changed");
		result = new JSONObject(responseDev.message);
		feature1Obj = result.getJSONArray("branches").getJSONObject(0).getJSONArray("features").getJSONObject(0);
		Assert.assertTrue(feature1Obj.containsKey("premium"));
		Assert.assertTrue(feature1Obj.getBoolean("premium") == false);
		Assert.assertFalse(feature1Obj.containsKey("premiumRule"));
		Assert.assertFalse(feature1Obj.containsKey("entitlement"));
		
		RuntimeRestApi.DateModificationResults responseProd = RuntimeDateUtilities.getProductionFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseProd.code==200, "Runtime production file was changed");
		result = new JSONObject(responseProd.message);
		feature1Obj = result.getJSONArray("branches").getJSONObject(0).getJSONArray("features").getJSONObject(0);
		Assert.assertTrue(feature1Obj.containsKey("premium"));
		Assert.assertTrue(feature1Obj.getBoolean("premium") == false);
		Assert.assertFalse(feature1Obj.containsKey("premiumRule"));
		Assert.assertFalse(feature1Obj.containsKey("entitlement"));
		
	}

	@Test (dependsOnMethods="updateFeatureInBranch", description="update F1 in master")
	public void updateFeatureInMaster() throws JSONException, Exception{				
		String dateFormat = purchasesApi.setDateFormat();
				
		String fStr = f.getFeatureFromBranch(featureID1, "MASTER", sessionToken);
		JSONObject fObj = new JSONObject(fStr);
		fObj.put("entitlement", inAppPurchaseID1);
		JSONObject rule = new JSONObject();
		rule.put("ruleString", "false");
		fObj.put("premiumRule", rule);
		
		String response = f.updateFeatureInBranch(seasonID, "MASTER",  featureID1, fObj.toString(), sessionToken);
		Assert.assertTrue(response.contains("error"), "can create feature attached to purcahse without store product id");
		
		String purchaseOptions = FileUtils.fileToString(filePath + "purchases/purchaseOptions1.txt", "UTF-8", false);
		JSONObject jsonIP = new JSONObject(purchaseOptions);
		jsonIP.put("name", RandomStringUtils.randomAlphabetic(5));
		jsonIP.put("stage", "PRODUCTION");
		String purchaseOptionsID = purchasesApi.addPurchaseItem(seasonID, jsonIP.toString(), inAppPurchaseID1, sessionToken);
		Assert.assertFalse (purchaseOptionsID.contains("error"), "Can't add purchaseOptions: " + purchaseOptionsID);

		response = f.updateFeatureInBranch(seasonID, "MASTER",  featureID1, fObj.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "cannot update feature : " + response);

		purchasesApi.setSleep();
		RuntimeRestApi.DateModificationResults branchesRuntimeDev = RuntimeDateUtilities.getDevelopmentBranchFileDateModification(m_url, productID, seasonID, branchID, dateFormat, sessionToken);
		Assert.assertTrue(branchesRuntimeDev.code==200, "Branched runtime development file was not changed");
		JSONObject result = new JSONObject(branchesRuntimeDev.message);
		JSONObject feature1Obj = result.getJSONArray("features").getJSONObject(0);
		Assert.assertTrue(feature1Obj.containsKey("premium"));
		Assert.assertTrue(feature1Obj.containsKey("premiumRule"));
		Assert.assertFalse(feature1Obj.containsKey("entitlement"));
		Assert.assertTrue(feature1Obj.getBoolean("premium") == false);
		//Assert.assertTrue(feature1Obj.getString("entitlement").equals("ns1.IAP2"));
		Assert.assertTrue(feature1Obj.getJSONObject("premiumRule").getString("ruleString").equals("true"));
		
		RuntimeRestApi.DateModificationResults branchesRuntimeProd = RuntimeDateUtilities.getProductionBranchFileDateModification(m_url, productID, seasonID, branchID, dateFormat, sessionToken);
		Assert.assertTrue(branchesRuntimeProd.code==200, "Branched runtime production file was not changed");
		result = new JSONObject(branchesRuntimeProd.message);
		feature1Obj = result.getJSONArray("features").getJSONObject(0);
		Assert.assertTrue(feature1Obj.containsKey("premium"));
		Assert.assertTrue(feature1Obj.containsKey("premiumRule"));
		Assert.assertFalse(feature1Obj.containsKey("entitlement"));
		Assert.assertTrue(feature1Obj.getBoolean("premium") == false);
		//Assert.assertTrue(feature1Obj.getString("entitlement").equals("ns1.IAP2"));
		Assert.assertTrue(feature1Obj.getJSONObject("premiumRule").getString("ruleString").equals("true"));
		
		RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getDevelopmentFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);		
		Assert.assertTrue(responseDev.code==200, "Runtime development file was not changed");
		result = new JSONObject(responseDev.message);
		feature1Obj = result.getJSONArray("branches").getJSONObject(0).getJSONArray("features").getJSONObject(0);
		Assert.assertTrue(feature1Obj.containsKey("premium"));
		Assert.assertTrue(feature1Obj.containsKey("premiumRule"));
		Assert.assertFalse(feature1Obj.containsKey("entitlement"));
		Assert.assertTrue(feature1Obj.getBoolean("premium") == false);
		//Assert.assertTrue(feature1Obj.getString("entitlement").equals("ns1.IAP2"));
		Assert.assertTrue(feature1Obj.getJSONObject("premiumRule").getString("ruleString").equals("true"));
		
		RuntimeRestApi.DateModificationResults responseProd = RuntimeDateUtilities.getProductionFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseProd.code==200, "Runtime production file was changed");
		result = new JSONObject(responseProd.message);
		feature1Obj = result.getJSONArray("branches").getJSONObject(0).getJSONArray("features").getJSONObject(0);
		Assert.assertTrue(feature1Obj.containsKey("premium"));
		Assert.assertTrue(feature1Obj.containsKey("premiumRule"));
		Assert.assertFalse(feature1Obj.containsKey("entitlement"));
		Assert.assertTrue(feature1Obj.getBoolean("premium") == false);
		//Assert.assertTrue(feature1Obj.getString("entitlement").equals("ns1.IAP2"));
		Assert.assertTrue(feature1Obj.getJSONObject("premiumRule").getString("ruleString").equals("true"));
		
	}

	@AfterTest
	private void reset(){
		baseUtils.reset(productID, sessionToken);
	}
}
