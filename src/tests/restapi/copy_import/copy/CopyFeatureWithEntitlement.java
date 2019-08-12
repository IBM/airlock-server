package tests.restapi.copy_import.copy;

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

public class CopyFeatureWithEntitlement {
	protected String seasonID1;
	protected String seasonID2;
	protected String seasonID3;
	protected String productID1;
	protected String productID2;
	//protected String featureID1;
	protected String filePath;
	protected String m_url;
	protected ProductsRestApi p;
	protected FeaturesRestApi f;
	BranchesRestApi br ;
	protected InAppPurchasesRestApi purchasesApi;
	protected SeasonsRestApi s;
	private String sessionToken = "";
	protected AirlockUtils baseUtils;
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
	 * copy premium feature in s1
	 * create season s2 and try to copy the premium feature to it
	 * create new product and s3 and try to copy the premium feature to it 	
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
	
	@Test (dependsOnMethods="createComponemts", description="Copy premium feature within the same season under root")
	public void copyFeatureSameSeason() throws IOException, JSONException{
		//at the beginning copy from branch to itself or from master to itself
		destBranchID = srcBranchID;
		
		String suffix = "suffix1";
		String rootId = f.getRootId(seasonID1, sessionToken);
		String response = f.copyItemBetweenBranches(premuimFeatureID, rootId, "ACT", null, suffix, sessionToken, srcBranchID, destBranchID);
		Assert.assertTrue(response.contains("newSubTreeId"), "Feature was not copied: " + response);
		
		JSONArray featuresArr = f.getFeaturesBySeasonFromBranch(seasonID1, destBranchID, sessionToken);
		Assert.assertTrue(featuresArr.size() == 2, "wrong features number after copy");

		Assert.assertTrue(featuresArr.getJSONObject(0).getString("uniqueId").equals(premuimFeatureID), "wrong feature id");
		Assert.assertFalse(featuresArr.getJSONObject(1).getString("uniqueId").equals(premuimFeatureID), "wrong feature id");
		Assert.assertTrue(featuresArr.getJSONObject(0).getString("entitlement").equals(entitlementID1), "wrong entitlement id");
		Assert.assertTrue(featuresArr.getJSONObject(1).getString("entitlement").equals(entitlementID1), "wrong entitlement id");
		Assert.assertTrue(featuresArr.getJSONObject(0).getString("name").equals(featureName), "wrong feature name");
		Assert.assertTrue(featuresArr.getJSONObject(1).getString("name").equals(featureName+suffix), "wrong feature name");
	}
	
	@Test (dependsOnMethods="copyFeatureSameSeason", description="Copy premium feature to another season under root")
	public void copyFeatureOtherSeason() throws Exception{
		
		if (runOnMaster) {
			destBranchID = BranchesRestApi.MASTER;
		} else {
			String allBranches = br.getAllBranches(seasonID2, sessionToken);
			JSONObject jsonBranch = new JSONObject(allBranches);
			destBranchID = jsonBranch.getJSONArray("branches").getJSONObject(1).getString("uniqueId");
		}
		
		String suffix = "suffix2";
		String rootId = f.getRootId(seasonID2, sessionToken);
		String response = f.copyItemBetweenBranches(premuimFeatureID, rootId, "ACT", null, suffix, sessionToken, srcBranchID, destBranchID);
		Assert.assertTrue(response.contains("newSubTreeId"), "Feature was not copied: " + response);
		
		JSONArray featuresArr = f.getFeaturesBySeasonFromBranch(seasonID2, destBranchID, sessionToken);
		Assert.assertTrue(featuresArr.size() == 2, "wrong features number after copy");
		
		String premuimFeatureIDInNewSeason = featuresArr.getJSONObject(0).getString("uniqueId");
		String copiedPremuimFeatureIDInNewSeason = featuresArr.getJSONObject(1).getString("uniqueId");

		JSONArray purchasesArr = purchasesApi.getPurchasesBySeasonFromBranch(seasonID2, destBranchID, sessionToken);
		Assert.assertTrue(purchasesArr.size() == 1, "wrong entitlements number after copy");
		String entitlementIDInNewSeason = purchasesArr.getJSONObject(0).getString("uniqueId");
		Assert.assertTrue(!entitlementIDInNewSeason.equals(entitlementID1), "entitlementID was  not changed during seasons duplication");
				
		Assert.assertTrue(!premuimFeatureIDInNewSeason.equals(premuimFeatureID), "wrong feature id");
		Assert.assertTrue(!copiedPremuimFeatureIDInNewSeason.equals(premuimFeatureID) && !copiedPremuimFeatureIDInNewSeason.equals(premuimFeatureIDInNewSeason) , "wrong feature id");
		Assert.assertTrue(featuresArr.getJSONObject(0).getString("entitlement").equals(entitlementIDInNewSeason), "wrong entitlement id");
		Assert.assertTrue(featuresArr.getJSONObject(1).getString("entitlement").equals(entitlementIDInNewSeason), "wrong entitlement id");
		Assert.assertTrue(featuresArr.getJSONObject(0).getString("name").equals(featureName), "wrong feature name");
		Assert.assertTrue(featuresArr.getJSONObject(1).getString("name").equals(featureName+suffix), "wrong feature name");
	}
	
	@Test (dependsOnMethods="copyFeatureOtherSeason", description="Copy premium feature to another product")
	public void copyFeatureDifferentProduct() throws IOException, JSONException{
		
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
		String response = f.copyItemBetweenBranches(premuimFeatureID, rootId, "ACT", null, suffix, sessionToken, srcBranchID, destBranchID);
		Assert.assertTrue(response.contains("error") && response.contains("entitlement") , "premium feature was not copied even when its entitlement is missing");
		
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

		response = f.copyItemBetweenBranches(premuimFeatureID, rootId, "ACT", null, suffix, sessionToken, srcBranchID, destBranchID);
		Assert.assertFalse(response.contains("error") , "premium feature was not copied even when its entitlement is not missing: " + response);
		
		JSONArray featuresArr = f.getFeaturesBySeasonFromBranch(seasonID3, destBranchID, sessionToken);
		Assert.assertTrue(featuresArr.size() == 1, "wrong features number after copy");
		Assert.assertFalse(featuresArr.getJSONObject(0).getString("uniqueId").equals(premuimFeatureID), "wrong feature id");
		Assert.assertTrue(featuresArr.getJSONObject(0).getString("entitlement").equals(entitlementIDInNewProd), "wrong entitlement id");
		Assert.assertTrue(featuresArr.getJSONObject(0).getString("name").equals(featureName+suffix), "wrong feature name");
	}
	/*
	@Test(dependsOnMethods="copyFeature", description="Validate all seasons")
	public void checkFeaturesInSeasons() throws Exception{
			
			//get features in all seasons
		 	JSONArray features = f.getFeaturesBySeason(seasonID1, sessionToken);
		 	Assert.assertTrue(features.size()==2, "Incorrect number of features in season1");

		 	features = f.getFeaturesBySeason(seasonID2, sessionToken);
		 	Assert.assertTrue(features.size()==1, "Incorrect number of features in season2");

		 	features = f.getFeaturesBySeason(seasonID3, sessionToken);
		 	Assert.assertTrue(features.size()==1, "Incorrect number of features in season3");
		 	
		 	//Add feature to each season
		 	String feature3 = FileUtils.fileToString(filePath + "feature3.txt", "UTF-8", false);
		 	String f3S1Id = f.addFeature(seasonID1, feature3, "ROOT", sessionToken);
		 	Assert.assertFalse(f3S1Id.contains("error"), "Feature was not added to season1 " + f3S1Id);
		 	String f3S2Id = f.addFeature(seasonID2, feature3, "ROOT", sessionToken);
		 	Assert.assertFalse(f3S2Id.contains("error"), "Feature was not added to season1 " + f3S2Id);
		 	String f3S3Id = f.addFeature(seasonID3, feature3, "ROOT", sessionToken);
		 	Assert.assertFalse(f3S3Id.contains("error"), "Feature was not added to season1 " + f3S3Id);
		 	
		 	//Update feature in each season
		 	String f3S1 = f.getFeature(f3S1Id, sessionToken);
		 	JSONObject f3S1Json = new JSONObject(f3S1);
		 	f3S1Json.put("name", "feature3 in season1");
		 	String response = f.updateFeature(seasonID1, f3S1Id, f3S1Json.toString(), sessionToken);
		 	Assert.assertFalse(response.contains("error"), "Feature was not updated in season1 " + response);
		 	
		 	String f3S2 = f.getFeature(f3S2Id, sessionToken);
		 	JSONObject f3S2Json = new JSONObject(f3S2);
		 	f3S2Json.put("name", "feature3 in season2");
		 	response = f.updateFeature(seasonID2, f3S2Id, f3S2Json.toString(), sessionToken);
		 	Assert.assertFalse(response.contains("error"), "Feature was not updated in season2 " + response);
		 	
		 	String f3S3 = f.getFeature(f3S3Id, sessionToken);
		 	JSONObject f3S3Json = new JSONObject(f3S3);
		 	f3S3Json.put("name", "feature3 in season3");
		 	response = f.updateFeature(seasonID3, f3S3Id, f3S3Json.toString(), sessionToken);
		 	Assert.assertFalse(response.contains("error"), "Feature was not updated in season3 " + response);
		 	
		 	//delete feature in all seasons
		 	int respCode = f.deleteFeature(f3S1Id, sessionToken);
		 	Assert.assertTrue(respCode==200, "Feature3 was not deleted from season1");
		 	respCode = f.deleteFeature(f3S2Id, sessionToken);
		 	Assert.assertTrue(respCode==200, "Feature3 was not deleted from season2");
		 	respCode = f.deleteFeature(f3S3Id, sessionToken);
		 	Assert.assertTrue(respCode==200, "Feature3 was not deleted from season3");
		 	
			//get features in all seasons
		 	features = f.getFeaturesBySeason(seasonID1, sessionToken);
		 	Assert.assertTrue(features.size()==2, "Incorrect number of features in season1");

		 	features = f.getFeaturesBySeason(seasonID2, sessionToken);
		 	Assert.assertTrue(features.size()==1, "Incorrect number of features in season2");

		 	features = f.getFeaturesBySeason(seasonID3, sessionToken);
		 	Assert.assertTrue(features.size()==1, "Incorrect number of features in season3");

	}
	*/
	@AfterTest
	private void reset(){
		baseUtils.reset(productID1, sessionToken);
	}

}