package tests.restapi.in_app_purchases;


import org.apache.wink.json4j.JSONArray;
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

public class PurchaseItemsDuplicationUponSeasonCreation {
	protected String productID;
	protected String seasonID1;
	protected String seasonID2;
	private String inAppPurchaseID1;
	private String inAppPurchaseID2;
	private String bundleID;
	private String purchaseOptID1;
	private String purchaseOptID2;
	private String purchaseOptID3;
	private String purchaseOptID4;
	private String configID1;
	private String configID2;
	private String configID3;
	private String configID4;
	private String iapMixID1;
	private String poMixID1;
	private String featureID1;
	private String configMixID1;
	private JSONObject inAppPurJson;
	private JSONObject purOptJson;
	private JSONObject configJson;
	protected String filePath;
	protected String m_url;
	private String sessionToken = "";
	private AirlockUtils baseUtils;
	protected InAppPurchasesRestApi purchasesApi;
	protected FeaturesRestApi f;
	
	
	@BeforeClass
	@Parameters({"url", "analyticsUrl", "translationsUrl", "configPath", "sessionToken", "userName", "userPassword", "appName", "productsToDeleteFile"})
	public void init(String url, String analyticsUrl, String translationsUrl, String configPath, String sToken, String userName, String userPassword, String appName, String productsToDeleteFile) throws Exception{
		m_url = url;
		filePath = configPath ;
		purchasesApi = new InAppPurchasesRestApi();
		purchasesApi.setURL(m_url);
		f = new FeaturesRestApi();
		f.setURL(m_url);
		
		baseUtils = new AirlockUtils(m_url, analyticsUrl, translationsUrl, configPath, sToken, userName, userPassword, appName, productsToDeleteFile);
		sessionToken = baseUtils.sessionToken;

		productID = baseUtils.createProduct();
		baseUtils.printProductToFile(productID);
		seasonID1 = baseUtils.createSeason(productID);
		
		String inAppPur = FileUtils.fileToString(filePath + "purchases/inAppPurchase1.txt", "UTF-8", false);
		inAppPurJson = new JSONObject(inAppPur);
		
		String purOpt = FileUtils.fileToString(filePath + "purchases/purchaseOptions1.txt", "UTF-8", false);
		purOptJson = new JSONObject(purOpt);
		
		String config = FileUtils.fileToString(filePath + "configuration_rule1.txt", "UTF-8", false);
		configJson = new JSONObject(config);
	}
	

	/*
	 * 								 ROOT                           ROOT
	 * 								|   |                             |
	 * 							IAPMTX  BUNDEL                       F1
	 *                          |     |
	 *                        IAP1   IAP2
	 *                      |  |  |     |
	 *                    CR1 PO1 PO2   POMTX
	 *                    	   |		   |   |
	 *                         CR2    PO3  PO4
	 *								   |
	 *								 CONFIGMIX
	 *								  |    |
	 *								CR3   CR4                      
	 */
	@Test (description ="add purcahses and feature to master")
	public void addComponents () throws Exception {
		
		String iapMix = FileUtils.fileToString(filePath + "purchases/inAppPurchaseMutual.txt", "UTF-8", false);
		iapMixID1 = purchasesApi.addPurchaseItem(seasonID1, iapMix, "ROOT", sessionToken);
		Assert.assertFalse(iapMixID1.contains("error"), "inAppPurchase  mtx was not added to the season: " + iapMixID1);
		
		inAppPurJson.put("name", "IAP1");
		inAppPurchaseID1 = purchasesApi.addPurchaseItem(seasonID1, inAppPurJson.toString(), iapMixID1, sessionToken);
		Assert.assertFalse(inAppPurchaseID1.contains("error"), "inAppPurchase was not added to the season: " + inAppPurchaseID1);
	
		inAppPurJson.put("name", "IAP2");
		inAppPurchaseID2 = purchasesApi.addPurchaseItem(seasonID1, inAppPurJson.toString(), iapMixID1, sessionToken);
		Assert.assertFalse(inAppPurchaseID2.contains("error"), "inAppPurchase was not added to the season: " + inAppPurchaseID2);
		
		inAppPurJson.put("name", "bundle");
		JSONArray includedPurchases = new JSONArray();
		includedPurchases.add(inAppPurchaseID1);
		includedPurchases.add(inAppPurchaseID2);
		inAppPurJson.put("includedEntitlements", includedPurchases);
		
		bundleID = purchasesApi.addPurchaseItem(seasonID1, inAppPurJson.toString(), "ROOT", sessionToken);
		Assert.assertFalse(bundleID.contains("error"), "bundle inAppPurchase was not added to the season: " + bundleID);
		
		inAppPurJson.put("includedEntitlements", new JSONArray());
		
		purOptJson.put("name", "PO1");
		purchaseOptID1 = purchasesApi.addPurchaseItem(seasonID1, purOptJson.toString(), inAppPurchaseID1, sessionToken);
		Assert.assertFalse(purchaseOptID1.contains("error"), "purchase options was not added to the season: "+purchaseOptID1);

		purOptJson.put("name", "PO2");
		purchaseOptID2 = purchasesApi.addPurchaseItem(seasonID1, purOptJson.toString(), inAppPurchaseID1, sessionToken);
		Assert.assertFalse(purchaseOptID2.contains("error"), "purchase options was not added to the season: " + purchaseOptID2);

		configJson.put("name", "CR1");
		configID1 = purchasesApi.addPurchaseItem(seasonID1, configJson.toString(), inAppPurchaseID1, sessionToken);
		Assert.assertFalse(configID1.contains("error"), "Configuration rule1 was not added to the season: "  +configID1);

		configJson.put("name", "CR2");
		configID2 = purchasesApi.addPurchaseItem(seasonID1, configJson.toString(), purchaseOptID1, sessionToken);
		Assert.assertFalse(configID2.contains("error"), "Configuration rule2 was not added to the season: " + configID2);

		String purchaseOptionsMix = FileUtils.fileToString(filePath + "purchases/purchaseOptionsMutual.txt", "UTF-8", false);
		poMixID1 = purchasesApi.addPurchaseItem(seasonID1, purchaseOptionsMix, inAppPurchaseID2, sessionToken);
		Assert.assertFalse(poMixID1.contains("error"), "purchase options mtx was not added to the season: " + poMixID1);
		
		purOptJson.put("name", "PO3");
		purchaseOptID3 = purchasesApi.addPurchaseItem(seasonID1, purOptJson.toString(), poMixID1, sessionToken);
		Assert.assertFalse(purchaseOptID3.contains("error"), "purchase options was not added to the season: "+purchaseOptID3);

		purOptJson.put("name", "PO4");
		purchaseOptID4 = purchasesApi.addPurchaseItem(seasonID1, purOptJson.toString(), poMixID1, sessionToken);
		Assert.assertFalse(purchaseOptID4.contains("error"), "purchase options was not added to the season: " + purchaseOptID4);

		
		String configurationMix = FileUtils.fileToString(filePath + "configuration_feature-mutual.txt", "UTF-8", false);
		configMixID1 = purchasesApi.addPurchaseItem(seasonID1, configurationMix, purchaseOptID3, sessionToken);
		Assert.assertFalse(configMixID1.contains("error"), "Configuration mix was not added to the season: " + configMixID1);

		configJson.put("name", "CR3");
		configID3 = purchasesApi.addPurchaseItem(seasonID1, configJson.toString(), configMixID1, sessionToken);
		Assert.assertFalse(configID3.contains("error"), "Configuration rule1 was not added to the season : " + configID3);

		configJson.put("name", "CR4");
		configID4 = purchasesApi.addPurchaseItem(seasonID1, configJson.toString(), configMixID1, sessionToken);
		Assert.assertFalse(configID4.contains("error"), "Configuration rule2 was not added to the season: " + configID4);
		
		String feature = FileUtils.fileToString(filePath + "feature1.txt", "UTF-8", false);
		JSONObject jsonF = new JSONObject(feature);
		jsonF.put("name", "F1");
		jsonF.put("namespace", "ns1");
		jsonF.put("entitlement", inAppPurchaseID2);
		jsonF.put("premium", true);
		JSONObject rule = new JSONObject();
		rule.put("ruleString", "true");
		jsonF.put("premiumRule", rule);
		featureID1 = f.addFeature(seasonID1, jsonF.toString(), "ROOT", sessionToken);
		Assert.assertFalse(featureID1.contains("error"), "cannot create feature: " + featureID1);
		
	}
	
	@Test (dependsOnMethods = "addComponents", description ="create new season")
	public void createNewSeason () throws Exception {
		seasonID2 = baseUtils.createSeason(productID, "1.1");
		Assert.assertFalse(seasonID2.contains("error"), "Fail craeting new season: " + seasonID2);
		
	}
	
	@Test (dependsOnMethods = "createNewSeason", description ="check that the items in the new branch has different ids")
	public void checkNewIds () throws Exception {
		JSONArray purchaseItems = purchasesApi.getPurchasesBySeason(seasonID2, sessionToken);
		Assert.assertTrue(purchaseItems.size()==2, "wrong size of purchase items under root");
		
		JSONObject iapMixObj	= 	purchaseItems.getJSONObject(0);
		JSONObject bundleObj	= 	purchaseItems.getJSONObject(1);
		
		Assert.assertTrue(iapMixObj.getString("type").equals("ENTITLEMENT_MUTUAL_EXCLUSION_GROUP"), "wrong iap mix type");
		Assert.assertFalse(iapMixObj.getString("uniqueId").equals(iapMixID1), "wrong iap mix id");
		
		Assert.assertTrue(bundleObj.getString("name").equals("bundle"), "wrong bundle name");
		Assert.assertTrue(bundleObj.getString("type").equals("ENTITLEMENT"), "wrong bundle type");
		Assert.assertFalse(bundleObj.getString("uniqueId").equals(bundleID), "wrong bundle id");
		
		Assert.assertTrue(bundleObj.getJSONArray("entitlements").size()==0, "wrong size of purchase items under bundle");
		Assert.assertTrue(iapMixObj.getJSONArray("entitlements").size()==2, "wrong size of purchase items under mtx");
		
		JSONObject inAppPurcahseObj1	= iapMixObj.getJSONArray("entitlements").getJSONObject(0);
		JSONObject inAppPurcahseObj2	= iapMixObj.getJSONArray("entitlements").getJSONObject(1);
		
		Assert.assertTrue(inAppPurcahseObj1.getString("name").equals("IAP1"), "wrong inAppPurcahse1 name");
		Assert.assertTrue(inAppPurcahseObj1.getString("type").equals("ENTITLEMENT"), "wrong inAppPurcahse1 type");
		Assert.assertFalse(inAppPurcahseObj1.getString("uniqueId").equals(inAppPurchaseID1), "wrong inAppPurcahse1 id");
		Assert.assertTrue(inAppPurcahseObj1.getJSONArray("configurationRules").size()==1, "wrong size of configurationRules items under inAppPurcahse1");
		Assert.assertTrue(inAppPurcahseObj1.getJSONArray("purchaseOptions").size()==2, "wrong size of purchaseOptions items under inAppPurcahse1");
		
		Assert.assertTrue(inAppPurcahseObj2.getString("name").equals("IAP2"), "wrong inAppPurcahse1 name");
		Assert.assertTrue(inAppPurcahseObj2.getString("type").equals("ENTITLEMENT"), "wrong inAppPurcahse1 type");
		Assert.assertFalse(inAppPurcahseObj2.getString("uniqueId").equals(inAppPurchaseID2), "wrong inAppPurcahse1 id");
		Assert.assertTrue(inAppPurcahseObj2.getJSONArray("purchaseOptions").size()==1, "wrong size of purchaseOptions items under inAppPurcahse2");
		
		JSONObject configObj1 = inAppPurcahseObj1.getJSONArray("configurationRules").getJSONObject(0);
		Assert.assertTrue(configObj1.getString("name").equals("CR1"), "wrong config rule1 name");
		Assert.assertTrue(configObj1.getString("type").equals("CONFIGURATION_RULE"), "wrong config rule1 type");
		Assert.assertFalse(configObj1.getString("uniqueId").equals(configID1), "wrong config rule1 id");
		
		JSONObject purOptObj1 = inAppPurcahseObj1.getJSONArray("purchaseOptions").getJSONObject(0);
		Assert.assertTrue(purOptObj1.getString("name").equals("PO1"), "wrong purchaseOption1 name");
		Assert.assertTrue(purOptObj1.getString("type").equals("PURCHASE_OPTIONS"), "wrong purchaseOption1 type");
		Assert.assertFalse(purOptObj1.getString("uniqueId").equals(purchaseOptID1), "wrong purchaseOption1 id");
		Assert.assertTrue(purOptObj1.getJSONArray("configurationRules").size()==1, "wrong size of configurationRules items under purchaseOption1");
		
		JSONObject purOptObj2 = inAppPurcahseObj1.getJSONArray("purchaseOptions").getJSONObject(1);
		Assert.assertTrue(purOptObj2.getString("name").equals("PO2"), "wrong purchaseOption2 name");
		Assert.assertTrue(purOptObj2.getString("type").equals("PURCHASE_OPTIONS"), "wrong purchaseOption2 type");
		Assert.assertFalse(purOptObj2.getString("uniqueId").equals(purchaseOptID2), "wrong purchaseOption2 id");
		
		JSONObject configObj2 = purOptObj1.getJSONArray("configurationRules").getJSONObject(0);
		Assert.assertTrue(configObj2.getString("name").equals("CR2"), "wrong config rule2 name");
		Assert.assertTrue(configObj2.getString("type").equals("CONFIGURATION_RULE"), "wrong config rule2 type");
		Assert.assertFalse(configObj2.getString("uniqueId").equals(configID2), "wrong config rule2 id");
		
		JSONObject purOptMixObj = inAppPurcahseObj2.getJSONArray("purchaseOptions").getJSONObject(0);
		Assert.assertTrue(purOptMixObj.getString("type").equals("PURCHASE_OPTIONS_MUTUAL_EXCLUSION_GROUP"), "wrong purchase options mix type");
		Assert.assertFalse(iapMixObj.getString("uniqueId").equals(poMixID1), "wrong purchase options mix id");
		Assert.assertTrue(purOptMixObj.getJSONArray("purchaseOptions").size()==2, "wrong size of purchaseOptions items under purchaseOptions mix");
		
		JSONObject purOptObj3 = purOptMixObj.getJSONArray("purchaseOptions").getJSONObject(0);
		Assert.assertTrue(purOptObj3.getString("name").equals("PO3"), "wrong purchaseOption3 name");
		Assert.assertTrue(purOptObj3.getString("type").equals("PURCHASE_OPTIONS"), "wrong purchaseOption3 type");
		Assert.assertFalse(purOptObj3.getString("uniqueId").equals(purchaseOptID3), "wrong purchaseOption3 id");
		Assert.assertTrue(purOptObj3.getJSONArray("configurationRules").size()==1, "wrong size of configurationRules items under purchaseOption3");
		
		JSONObject purOptObj4 = purOptMixObj.getJSONArray("purchaseOptions").getJSONObject(1);
		Assert.assertTrue(purOptObj4.getString("name").equals("PO4"), "wrong purchaseOption4 name");
		Assert.assertTrue(purOptObj4.getString("type").equals("PURCHASE_OPTIONS"), "wrong purchaseOption4 type");
		Assert.assertFalse(purOptObj4.getString("uniqueId").equals(purchaseOptID2), "wrong purchaseOption4 id");
		
		JSONObject configMixObj = purOptObj3.getJSONArray("configurationRules").getJSONObject(0);
		Assert.assertTrue(configMixObj.getString("type").equals("CONFIG_MUTUAL_EXCLUSION_GROUP"), "wrong configuration rules mix type");
		Assert.assertFalse(configMixObj.getString("uniqueId").equals(configMixID1), "wrong configuration rules mix id");
		Assert.assertTrue(configMixObj.getJSONArray("configurationRules").size()==2, "wrong size of configurationRules items under configurationRules mix");
		
		JSONObject configObj3 = configMixObj.getJSONArray("configurationRules").getJSONObject(0);
		Assert.assertTrue(configObj3.getString("name").equals("CR3"), "wrong config rule3 name");
		Assert.assertTrue(configObj3.getString("type").equals("CONFIGURATION_RULE"), "wrong config rule3 type");
		Assert.assertFalse(configObj3.getString("uniqueId").equals(configID3), "wrong config rule3 id");
		
		JSONObject configObj4 = configMixObj.getJSONArray("configurationRules").getJSONObject(1);
		Assert.assertTrue(configObj4.getString("name").equals("CR4"), "wrong config rule4 name");
		Assert.assertTrue(configObj4.getString("type").equals("CONFIGURATION_RULE"), "wrong config rule4 type");
		Assert.assertFalse(configObj4.getString("uniqueId").equals(configID4), "wrong config rule4 id");
		
		//check new included purchases ids in the bundle 
		String newInAppPurchaseID1 = inAppPurcahseObj1.getString("uniqueId");
		String newInAppPurchaseID2 = inAppPurcahseObj2.getString("uniqueId");
		JSONArray newIncludedPurcahses = bundleObj.getJSONArray("includedEntitlements");
		Assert.assertTrue(newIncludedPurcahses.size()==2, "wrong included purcahses number");
		Assert.assertTrue(newIncludedPurcahses.getString(0).equals(newInAppPurchaseID1), "wrong included purcahses id 1");
		Assert.assertTrue(newIncludedPurcahses.getString(1).equals(newInAppPurchaseID2), "wrong included purcahses id 2");
		
		//check newcvinAppPurchase id in feature
		JSONArray featureItems = f.getFeaturesBySeason(seasonID2, sessionToken);
		Assert.assertTrue(featureItems.size()==1, "wrong size of features items under root");
		JSONObject featureObj1 = featureItems.getJSONObject(0);
		String newinAppPurchase = featureObj1.getString("entitlement");
		Assert.assertTrue(newinAppPurchase.equals(newInAppPurchaseID2), "wrong inAppPurcahse in feature");
	}
	
	@AfterTest
	private void reset(){
		baseUtils.reset(productID, sessionToken);
	}
}
