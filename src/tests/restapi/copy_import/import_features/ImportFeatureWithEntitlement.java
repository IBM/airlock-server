package tests.restapi.copy_import.import_features;

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
import tests.restapi.BranchesRestApi;
import tests.restapi.FeaturesRestApi;
import tests.restapi.InAppPurchasesRestApi;
import tests.restapi.ProductsRestApi;
import tests.restapi.SeasonsRestApi;

public class ImportFeatureWithEntitlement {
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
	private String premuimFeatureID;
	private String featureName = "premiumFeature";

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
	 * create season s1 with entitlement and premium feature
	 * import premium feature to s1
	 * create season s2 and try to import the premium feature to it
	 * create new product and s3 and try to import the premium feature to it 	
	 */
	
	@Test (description="Create premium feature and entitlement in s1")
	public void createComponemts() throws IOException, JSONException{
		
		//add entitlements to season1
		String entitlement = FileUtils.fileToString(filePath + "purchases/inAppPurchase1.txt", "UTF-8", false);
		JSONObject jsonE = new JSONObject(entitlement);
		jsonE.put("name", "Entitlement1");
		jsonE.put("stage", "PRODUCTION");
		entitlementID1 = purchasesApi.addPurchaseItemToBranch(seasonID1, srcBranchID, jsonE.toString(), "ROOT", sessionToken);
		Assert.assertFalse(entitlementID1.contains("error"), "Entitlement1 was not added to the season " + entitlementID1);
		
		String purchaseOptions = FileUtils.fileToString(filePath + "purchases/purchaseOptions1.txt", "UTF-8", false);
		JSONObject jsonIP = new JSONObject(purchaseOptions);
		jsonIP.put("name", RandomStringUtils.randomAlphabetic(5));
		String puOptID = purchasesApi.addPurchaseItemToBranch(seasonID1, srcBranchID, jsonIP.toString(), entitlementID1, sessionToken);
		Assert.assertFalse (puOptID.contains("error"), "Can't add purchaseOptions: " + puOptID);
		
		String feature1 = FileUtils.fileToString(filePath + "feature1.txt", "UTF-8", false);
		JSONObject premiumFeatureObj = new JSONObject(feature1);
		premiumFeatureObj.put("entitlement", entitlementID1);
		premiumFeatureObj.put("premium", true);
		JSONObject premiumRule = new JSONObject();
		premiumRule.put("ruleString", "true;");
		premiumFeatureObj.put("premiumRule", premiumRule);

		premiumFeatureObj.put("name", featureName);
		
		premuimFeatureID = f.addFeatureToBranch(seasonID1, srcBranchID, premiumFeatureObj.toString(), "ROOT", sessionToken);
		Assert.assertFalse(premuimFeatureID.contains("error"), "Feature was not added to the season " + premuimFeatureID);

		String configuration = FileUtils.fileToString(filePath + "configuration_rule1.txt", "UTF-8", false);
		String configID = f.addFeatureToBranch(seasonID1, srcBranchID, configuration, premuimFeatureID, sessionToken);
		Assert.assertFalse(configID.contains("error"), "Feature was not added to the season");
		
		//create season2
		JSONObject sJson = new JSONObject();
		sJson.put("minVersion", "2.0");		
		seasonID2 = s.addSeason(productID1, sJson.toString(), sessionToken);
		Assert.assertFalse(seasonID2.contains("error"), "The second season was not created: " + seasonID2);
	}
	
	@Test (dependsOnMethods="createComponemts", description="Import premium feature within the same season under root")
	public void importFeatureSameSeason() throws IOException, JSONException{
		//at the beginning import from branch to itself or from master to itself
		destBranchID = srcBranchID;
		
		String suffix = "suffix1";
		String rootId = f.getRootId(seasonID1, sessionToken);
		String featureToImport = f.getFeatureFromBranch(premuimFeatureID, srcBranchID, sessionToken);
		
		String response = f.importFeatureToBranch(featureToImport, rootId, "ACT", null, suffix, true, sessionToken, destBranchID);
		Assert.assertTrue(response.contains("newSubTreeId"), "Feature was not imported: " + response);
		
		JSONArray featuresArr = f.getFeaturesBySeasonFromBranch(seasonID1, destBranchID, sessionToken);
		Assert.assertTrue(featuresArr.size() == 2, "wrong features number after import");

		Assert.assertTrue(featuresArr.getJSONObject(0).getString("uniqueId").equals(premuimFeatureID), "wrong feature id");
		Assert.assertFalse(featuresArr.getJSONObject(1).getString("uniqueId").equals(premuimFeatureID), "wrong feature id");
		Assert.assertTrue(featuresArr.getJSONObject(0).getString("entitlement").equals(entitlementID1), "wrong entitlement id");
		Assert.assertTrue(featuresArr.getJSONObject(1).getString("entitlement").equals(entitlementID1), "wrong entitlement id");
		Assert.assertTrue(featuresArr.getJSONObject(0).getString("name").equals(featureName), "wrong feature name");
		Assert.assertTrue(featuresArr.getJSONObject(1).getString("name").equals(featureName+suffix), "wrong feature name");
	}
	
	@Test (dependsOnMethods="importFeatureSameSeason", description="import premium feature to another season under root")
	public void importFeatureOtherSeason() throws Exception{
		
		if (runOnMaster) {
			destBranchID = BranchesRestApi.MASTER;
		} else {
			String allBranches = br.getAllBranches(seasonID2, sessionToken);
			JSONObject jsonBranch = new JSONObject(allBranches);
			destBranchID = jsonBranch.getJSONArray("branches").getJSONObject(1).getString("uniqueId");
		}
		
		String suffix = "suffix2";
		String rootId = f.getRootId(seasonID2, sessionToken);
		String featureToImport = f.getFeatureFromBranch(premuimFeatureID, srcBranchID, sessionToken);
		
		String response = f.importFeatureToBranch(featureToImport, rootId, "ACT", null, suffix, true, sessionToken, destBranchID);
		Assert.assertTrue(response.contains("newSubTreeId"), "Feature was not imported: " + response);
		
		JSONArray featuresArr = f.getFeaturesBySeasonFromBranch(seasonID2, destBranchID, sessionToken);
		Assert.assertTrue(featuresArr.size() == 2, "wrong features number after import");
		
		String premuimFeatureIDInNewSeason = featuresArr.getJSONObject(0).getString("uniqueId");
		String copiedPremuimFeatureIDInNewSeason = featuresArr.getJSONObject(1).getString("uniqueId");

		JSONArray purchasesArr = purchasesApi.getPurchasesBySeasonFromBranch(seasonID2, destBranchID, sessionToken);
		Assert.assertTrue(purchasesArr.size() == 1, "wrong entitlements number after import");
		String entitlementIDInNewSeason = purchasesArr.getJSONObject(0).getString("uniqueId");
		Assert.assertTrue(!entitlementIDInNewSeason.equals(entitlementID1), "entitlementID was  not changed during seasons duplication");
				
		Assert.assertTrue(!premuimFeatureIDInNewSeason.equals(premuimFeatureID), "wrong feature id");
		Assert.assertTrue(!copiedPremuimFeatureIDInNewSeason.equals(premuimFeatureID) && !copiedPremuimFeatureIDInNewSeason.equals(premuimFeatureIDInNewSeason) , "wrong feature id");
		Assert.assertTrue(featuresArr.getJSONObject(0).getString("entitlement").equals(entitlementIDInNewSeason), "wrong entitlement id");
		Assert.assertTrue(featuresArr.getJSONObject(1).getString("entitlement").equals(entitlementIDInNewSeason), "wrong entitlement id");
		Assert.assertTrue(featuresArr.getJSONObject(0).getString("name").equals(featureName), "wrong feature name");
		Assert.assertTrue(featuresArr.getJSONObject(1).getString("name").equals(featureName+suffix), "wrong feature name");
	}
	
	@Test (dependsOnMethods="importFeatureOtherSeason", description="import premium feature to another product")
	public void importFeatureDifferentProduct() throws IOException, JSONException{
		
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
		String rootId = f.getRootId(seasonID3, sessionToken);
		String featureToImport = f.getFeatureFromBranch(premuimFeatureID, srcBranchID, sessionToken);
		
		String response = f.importFeatureToBranch(featureToImport, rootId, "ACT", null, suffix, true, sessionToken, destBranchID);
		Assert.assertTrue(response.contains("error") && response.contains("entitlement") , "premium feature was imported even when its entitlement is missing");
		
		//add entitlements to season3
		String entitlement = FileUtils.fileToString(filePath + "purchases/inAppPurchase1.txt", "UTF-8", false);
		JSONObject jsonE = new JSONObject(entitlement);
		jsonE.put("name", "Entitlement1");
		jsonE.put("stage", "PRODUCTION");
		String entitlementIDInNewProd = purchasesApi.addPurchaseItemToBranch(seasonID3, destBranchID, jsonE.toString(), "ROOT", sessionToken);
		Assert.assertFalse(entitlementIDInNewProd.contains("error"), "Entitlement1 was not added to the season " + entitlementIDInNewProd);
		
		String purchaseOptions = FileUtils.fileToString(filePath + "purchases/purchaseOptions1.txt", "UTF-8", false);
		JSONObject jsonIP = new JSONObject(purchaseOptions);
		jsonIP.put("name", RandomStringUtils.randomAlphabetic(5));
		String puOptID = purchasesApi.addPurchaseItemToBranch(seasonID3, destBranchID, jsonIP.toString(), entitlementIDInNewProd, sessionToken);
		Assert.assertFalse (puOptID.contains("error"), "Can't add purchaseOptions: " + puOptID);
	
		Assert.assertFalse(entitlementIDInNewProd.equals(entitlementID1), "wrong entitlementID");
		featureToImport = f.getFeatureFromBranch(premuimFeatureID, srcBranchID, sessionToken);
		
		response = f.importFeatureToBranch(featureToImport, rootId, "ACT", null, suffix, true, sessionToken, destBranchID);
		Assert.assertFalse(response.contains("error") , "premium feature was not imported even when its entitlement is not missing: " + response);
		
		JSONArray featuresArr = f.getFeaturesBySeasonFromBranch(seasonID3, destBranchID, sessionToken);
		Assert.assertTrue(featuresArr.size() == 1, "wrong features number after import");
		Assert.assertFalse(featuresArr.getJSONObject(0).getString("uniqueId").equals(premuimFeatureID), "wrong feature id");
		Assert.assertTrue(featuresArr.getJSONObject(0).getString("entitlement").equals(entitlementIDInNewProd), "wrong entitlement id");
		Assert.assertTrue(featuresArr.getJSONObject(0).getString("name").equals(featureName+suffix), "wrong feature name");
	}

	@AfterTest
	private void reset(){
		baseUtils.reset(productID1, sessionToken);
	}

}