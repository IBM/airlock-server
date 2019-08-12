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

public class TestInAppPurchaseInFeatureRuntime {
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
	protected String featureID1;
	protected String featureID2;
	
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

		jsonIP = new JSONObject(purchaseOptions);
		jsonIP.put("name", RandomStringUtils.randomAlphabetic(5));
		puOptID = purchasesApi.addPurchaseItem(seasonID, jsonIP.toString(), inAppPurID2, sessionToken);
		Assert.assertFalse (puOptID.contains("error"), "Can't add purchaseOptions: " + puOptID);
		
		
		String feature = FileUtils.fileToString(filePath + "feature1.txt", "UTF-8", false);
		JSONObject jsonF = new JSONObject(feature);
		jsonF.put("name", "F1");
		jsonF.put("namespace", "ns1");
		jsonF.put("entitlement", inAppPurID1);
		jsonF.put("premium", true);
		JSONObject premiumRule = new JSONObject();
		premiumRule.put("ruleString", "true;");
		jsonF.put("premiumRule", premiumRule);

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
		Assert.assertFalse(featureID2.contains("error"), "cannot create feature: " + featureID2);
		
		featureStr = f.getFeature(featureID2, sessionToken);
		featureObj = new JSONObject(featureStr);
		Assert.assertTrue(featureObj.getString("entitlement").equals(inAppPurID2), "the purchase is in the feature is wrong");
		
		f.setSleep();
		RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getDevelopmentBranchFileDateModification(m_url, productID, seasonID, "MASTER", dateFormat, sessionToken);
		Assert.assertTrue(responseDev.code ==200, "Runtime development feature file was not updated");
		JSONObject root = RuntimeDateUtilities.getFeaturesList(responseDev.message);
		JSONArray featursArr = root.getJSONArray("features");
		Assert.assertTrue(featursArr.size() == 2, "wrong number of features in runtime");
		Assert.assertTrue(featursArr.getJSONObject(0).getString("entitlement").equals("ns1.inAppPurchase1"), "wrong inAppPurchases in runtime");
		Assert.assertTrue(featursArr.getJSONObject(1).getString("entitlement").equals("ns1.inAppPurchase2"), "wrong inAppPurchases in runtime");
		
		RuntimeRestApi.DateModificationResults responseProd = RuntimeDateUtilities.getProductionBranchFileDateModification(m_url, productID, seasonID, "MASTER", dateFormat, sessionToken);
		Assert.assertTrue(responseProd.code ==304, "Runtime production feature file was changed");
		
		RuntimeRestApi.DateModificationResults prodChanged = RuntimeDateUtilities.getProductionChangedDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(prodChanged.code ==304, "productionChanged.txt file was changed");
	}

	@Test (dependsOnMethods = "addComponents", description = "update features to point on different purchase")
	public void updateFeaturePurchase() throws JSONException, IOException, InterruptedException{
		String dateFormat = f.setDateFormat();
				
		String featureStr = f.getFeature(featureID1, sessionToken);
		JSONObject featureObj = new JSONObject(featureStr);
		featureObj.put("entitlement", inAppPurID2);
		featureID1 = f.updateFeature(seasonID, featureID1, featureObj.toString(), sessionToken);
		Assert.assertFalse(featureID1.contains("error"), "cannot update feature: " + featureID1);
		
		featureStr = f.getFeature(featureID1, sessionToken);
		featureObj = new JSONObject(featureStr);
		Assert.assertTrue(featureObj.getString("entitlement").equals(inAppPurID2), "the purchase is in the feature is wrong");
		
		f.setSleep();
		RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getDevelopmentBranchFileDateModification(m_url, productID, seasonID, "MASTER", dateFormat, sessionToken);
		Assert.assertTrue(responseDev.code ==200, "Runtime development feature file was not updated");
		JSONObject root = RuntimeDateUtilities.getFeaturesList(responseDev.message);
		JSONArray featursArr = root.getJSONArray("features");
		Assert.assertTrue(featursArr.size() == 2, "wrong number of features in runtime");
		Assert.assertTrue(featursArr.getJSONObject(0).getString("entitlement").equals("ns1.inAppPurchase2"), "wrong inAppPurchases in runtime");
		Assert.assertTrue(featursArr.getJSONObject(1).getString("entitlement").equals("ns1.inAppPurchase2"), "wrong inAppPurchases in runtime");
		
		RuntimeRestApi.DateModificationResults responseProd = RuntimeDateUtilities.getProductionBranchFileDateModification(m_url, productID, seasonID, "MASTER", dateFormat, sessionToken);
		Assert.assertTrue(responseProd.code ==304, "Runtime production feature file was changed");
		
		RuntimeRestApi.DateModificationResults prodChanged = RuntimeDateUtilities.getProductionChangedDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(prodChanged.code ==304, "productionChanged.txt file was changed");
	}

	@Test (dependsOnMethods = "addComponents", description = "update features to point on different purchase")
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
		
		f.setSleep();
		RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getDevelopmentBranchFileDateModification(m_url, productID, seasonID, "MASTER", dateFormat, sessionToken);
		Assert.assertTrue(responseDev.code ==200, "Runtime development feature file was not updated");
		JSONObject root = RuntimeDateUtilities.getFeaturesList(responseDev.message);
		JSONArray featursArr = root.getJSONArray("features");
		Assert.assertTrue(featursArr.size() == 2, "wrong number of features in runtime");
		Assert.assertTrue(featursArr.getJSONObject(0).getString("entitlement").equals("ns1.kuku"), "wrong inAppPurchases in runtime");
		Assert.assertTrue(featursArr.getJSONObject(1).getString("entitlement").equals("ns1.kuku"), "wrong inAppPurchases in runtime");
		
		RuntimeRestApi.DateModificationResults responseProd = RuntimeDateUtilities.getProductionBranchFileDateModification(m_url, productID, seasonID, "MASTER", dateFormat, sessionToken);
		Assert.assertTrue(responseProd.code ==304, "Runtime production feature file was changed");
		
		RuntimeRestApi.DateModificationResults prodChanged = RuntimeDateUtilities.getProductionChangedDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(prodChanged.code ==304, "productionChanged.txt file was changed");
	}
	

	@Test (dependsOnMethods = "updatePurchaseName", description = "try deleting purchases")
	public void deletePurchases() throws JSONException, IOException, InterruptedException{
		int res = purchasesApi.deletePurchaseItem(inAppPurID1, sessionToken);
		Assert.assertTrue(res == 200, "cannot delete inAppPurID1");
		
		res = purchasesApi.deletePurchaseItem(inAppPurID2, sessionToken);
		Assert.assertTrue(res != 200, "can delete inAppPurID1 even though attached to feature");
	}
	

	
	@AfterTest
	private void reset(){
		baseUtils.reset(productID, sessionToken);
	}
}
