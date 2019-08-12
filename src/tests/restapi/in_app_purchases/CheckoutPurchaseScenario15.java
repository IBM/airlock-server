package tests.restapi.in_app_purchases;

import java.io.IOException;


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

public class CheckoutPurchaseScenario15 {
	private String productID;
	private String seasonID;
	private String branchID;
	private String inAppPurchaseID1;
	private String purchaseOptionsID1;
	private String bundleID1;
	private JSONObject inAppPurchaseJson;
	private JSONObject purchaseOptionsJson;
	private String filePath;
	private String m_url;
	private String sessionToken = "";
	private AirlockUtils baseUtils;
	private BranchesRestApi br ;
	private InAppPurchasesRestApi purchasesApi;
	private FeaturesRestApi f;
	private String featureID1;
	
	
	@BeforeClass
	@Parameters({"url", "analyticsUrl", "translationsUrl", "configPath", "sessionToken", "userName", "userPassword", "appName", "productsToDeleteFile"})
	public void init(String url, String analyticsUrl, String translationsUrl, String configPath, String sToken, String userName, String userPassword, String appName, String productsToDeleteFile) throws Exception{
		m_url = url;
		filePath = configPath ;
		purchasesApi = new InAppPurchasesRestApi();
		purchasesApi.setURL(m_url);
		f = new FeaturesRestApi();
		f.setURL(m_url);
		br = new BranchesRestApi();
		br.setURL(m_url);

		baseUtils = new AirlockUtils(m_url, analyticsUrl, translationsUrl, configPath, sToken, userName, userPassword, appName, productsToDeleteFile);
		sessionToken = baseUtils.sessionToken;

		productID = baseUtils.createProduct();
		baseUtils.printProductToFile(productID);
		seasonID = baseUtils.createSeason(productID);
		
		String inAppPurchase = FileUtils.fileToString(filePath + "purchases/inAppPurchase1.txt", "UTF-8", false);
		inAppPurchaseJson = new JSONObject(inAppPurchase);
		
		String purchaseOptions = FileUtils.fileToString(filePath + "purchases/purchaseOptions1.txt", "UTF-8", false);
		purchaseOptionsJson = new JSONObject(purchaseOptions);
	}
	

	
	@Test (description ="IAP1->PO1  in master") 
	public void scenario15 () throws Exception {
		branchID = addBranch("branch1");
		Assert.assertFalse(branchID.contains("error"), "Branch1 was not created: " + branchID);
		
		inAppPurchaseJson.put("name", "IAP1");
		inAppPurchaseID1 = purchasesApi.addPurchaseItem(seasonID, inAppPurchaseJson.toString(), "ROOT", sessionToken);
		Assert.assertFalse(inAppPurchaseID1.contains("error"), "inAppPurchase was not added to the season: " + inAppPurchaseID1);
		purchaseOptionsJson.put("name", "PO1");
		purchaseOptionsID1 = purchasesApi.addPurchaseItem(seasonID, purchaseOptionsJson.toString(), inAppPurchaseID1, sessionToken);
		Assert.assertFalse(purchaseOptionsID1.contains("error"), "purchaseOptions was not added to the season: " + purchaseOptionsID1);
	}
	
	@Test (dependsOnMethods="scenario15", description="checkout IAP1")
	public void checkout() throws JSONException, Exception{				
		String res = br.checkoutFeature(branchID, inAppPurchaseID1, sessionToken); //checks out IAP1
		Assert.assertFalse(res.contains("error"), "inAppPurchase1 was not unchecked out: " + res);
		
		JSONObject IAP1 = new JSONObject(purchasesApi.getPurchaseItemFromBranch(inAppPurchaseID1, branchID, sessionToken));
		Assert.assertTrue(IAP1.getString("branchStatus").equals("CHECKED_OUT"), "inAppPurchase1 incorrect status");
		JSONObject PO1 = new JSONObject(purchasesApi.getPurchaseItemFromBranch(purchaseOptionsID1, branchID, sessionToken));
		Assert.assertTrue(PO1.getString("branchStatus").equals("NONE"), "inAppPurchase2 incorrect status");
		
		//find subtree root in branch
		JSONObject branch = new JSONObject(br.getBranchWithFeatures(branchID, sessionToken));
		JSONArray inAppPurchases = branch.getJSONArray("entitlements");
		Assert.assertTrue(inAppPurchases.size() ==1, "More than one sub tree in branch");
		Assert.assertTrue(inAppPurchases.getJSONObject(0).getString("uniqueId").equals(inAppPurchaseID1), "entitlement is not the branch subTree root");
	}	

	@Test (dependsOnMethods="checkout", description="add bundle and premium feature to branch")
	public void addBundleToBranch() throws JSONException, Exception{	
		String feature = FileUtils.fileToString(filePath + "feature1.txt", "UTF-8", false);
		JSONObject jsonF = new JSONObject(feature);
		jsonF.put("name", "F1");
		jsonF.put("namespace", "ns1");
		jsonF.put("entitlement", inAppPurchaseID1);
		jsonF.put("premium", true);
		JSONObject rule = new JSONObject();
		rule.put("ruleString", "true");
		jsonF.put("premiumRule", rule);
		featureID1 = f.addFeatureToBranch(seasonID, branchID, jsonF.toString(), "ROOT", sessionToken);
		Assert.assertFalse(featureID1.contains("error"), "cannot create feature: " + featureID1);
		
		inAppPurchaseJson.put("name", "bundle");
		JSONArray includedEntitlementsArr = new JSONArray();
		includedEntitlementsArr.add(inAppPurchaseID1);
		inAppPurchaseJson.put("includedEntitlements", includedEntitlementsArr);
		bundleID1 = purchasesApi.addPurchaseItemToBranch(seasonID, branchID, inAppPurchaseJson.toString(), "ROOT", sessionToken);
		Assert.assertFalse(bundleID1.contains("error"), "inAppPurchase was not added to the season: " + bundleID1);
		
		JSONObject IAP1 = new JSONObject(purchasesApi.getPurchaseItemFromBranch(inAppPurchaseID1, branchID, sessionToken));
		Assert.assertTrue(IAP1.getString("branchStatus").equals("CHECKED_OUT"), "inAppPurchase1 incorrect status");
		JSONObject PO1 = new JSONObject(purchasesApi.getPurchaseItemFromBranch(purchaseOptionsID1, branchID, sessionToken));
		Assert.assertTrue(PO1.getString("branchStatus").equals("NONE"), "inAppPurchase2 incorrect status");
		JSONObject bundle = new JSONObject(purchasesApi.getPurchaseItemFromBranch(bundleID1, branchID, sessionToken));
		Assert.assertTrue(bundle.getString("branchStatus").equals("NEW"), "bundle incorrect status");
		Assert.assertTrue(bundle.getJSONArray("includedEntitlements").size() == 1, "bundle incorrect includedEntitlements size");
		Assert.assertTrue(bundle.getJSONArray("includedEntitlements").getString(0).equals(inAppPurchaseID1), "bundle incorrect includedEntitlements ud");
		
		JSONObject featureObj = new JSONObject(f.getFeatureFromBranch(featureID1, branchID, sessionToken));
		Assert.assertTrue(featureObj.getString("branchStatus").equals("NEW"), "feature incorrect status");
		Assert.assertTrue(featureObj.getString("entitlement").equals(inAppPurchaseID1), "wrong entitlement in premium feature");
		
		//find subtree root in branch
		JSONObject branch = new JSONObject(br.getBranchWithFeatures(branchID, sessionToken));
		JSONArray inAppPurchases = branch.getJSONArray("entitlements");
		Assert.assertTrue(inAppPurchases.size() ==2, "More than 2 sub tree in branch");
		Assert.assertTrue(inAppPurchases.getJSONObject(0).getString("uniqueId").equals(inAppPurchaseID1), "entitlement is not the branch subTree root");
		Assert.assertTrue(inAppPurchases.getJSONObject(1).getString("uniqueId").equals(bundleID1), "bundle is not the branch subTree root");
		
		JSONArray features = branch.getJSONArray("features");
		Assert.assertTrue(features.size() ==1, "More than 1 sub tree in branch");
		Assert.assertTrue(features.getJSONObject(0).getString("uniqueId").equals(featureID1), "feature is not the branch subTree root");
	}	


	@Test (dependsOnMethods="addBundleToBranch", description="delete IAP1 from master")
	public void deleteEntitlemenFromMaster() throws JSONException, Exception{				
		int res = purchasesApi.deletePurchaseItemFromBranch(inAppPurchaseID1, "MASTER", sessionToken);
		Assert.assertTrue(res==200, "cannot delete entitlement from master");
		
		//find subtree root in branch
		JSONObject branch = new JSONObject(br.getBranchWithFeatures(branchID, sessionToken));
		JSONArray inAppPurchases = branch.getJSONArray("entitlements");
		Assert.assertTrue(inAppPurchases.size() ==2, "More than 2 sub tree in branch");
		Assert.assertFalse(inAppPurchases.getJSONObject(0).getString("uniqueId").equals(inAppPurchaseID1), "entitlement is not the branch subTree root");
		Assert.assertTrue(inAppPurchases.getJSONObject(0).getString("name").equals("IAP1"), "wrong entitlement name.");
		Assert.assertTrue(inAppPurchases.getJSONObject(0).getString("branchStatus").equals("NEW"), "wrong entitlement name.");
		Assert.assertTrue(inAppPurchases.getJSONObject(1).getString("uniqueId").equals(bundleID1), "bundle is not the branch subTree root");
		
		JSONArray features = branch.getJSONArray("features");
		Assert.assertTrue(features.size() ==1, "More than 1 sub tree in branch");
		Assert.assertTrue(features.getJSONObject(0).getString("uniqueId").equals(featureID1), "feature is not the branch subTree root");
		Assert.assertTrue(features.getJSONObject(0).getString("branchStatus").equals("NEW"), "wrong feature status");
		Assert.assertTrue(features.getJSONObject(0).getString("name").equals("F1"), "wrong feature name");
		
		String inAppPurchaseID1inBranch = inAppPurchases.getJSONObject(0).getString("uniqueId");
		JSONObject IAP1 = new JSONObject(purchasesApi.getPurchaseItemFromBranch(inAppPurchaseID1inBranch, branchID, sessionToken));
		Assert.assertTrue(IAP1.getString("branchStatus").equals("NEW"), "inAppPurchase1 incorrect status");
		Assert.assertTrue(IAP1.getString("name").equals("IAP1"), "inAppPurchase1 incorrect name");
		JSONObject bundle = new JSONObject(purchasesApi.getPurchaseItemFromBranch(bundleID1, branchID, sessionToken));
		Assert.assertTrue(bundle.getString("branchStatus").equals("NEW"), "bundle incorrect status");
		Assert.assertTrue(bundle.getJSONArray("includedEntitlements").size() == 1, "bundle incorrect includedEntitlements size");
		Assert.assertTrue(bundle.getJSONArray("includedEntitlements").getString(0).equals(inAppPurchaseID1inBranch), "bundle incorrect includedEntitlements ud");
		
		JSONObject featureObj = new JSONObject(f.getFeatureFromBranch(featureID1, branchID, sessionToken));
		Assert.assertTrue(featureObj.getString("branchStatus").equals("NEW"), "bundle incorrect status");
		Assert.assertTrue(featureObj.getString("entitlement").equals(inAppPurchaseID1inBranch), "wrong entitlement in premium feature");
	}	

	@Test (dependsOnMethods="deleteEntitlemenFromMaster", description ="recreate branch and add IAP1->PO1  in master") 
	public void cleanAndAddComponents () throws Exception {
		int code = br.deleteBranch(branchID, sessionToken);
		Assert.assertTrue(code == 200, "Branch1 was not deleted");
		
		branchID = addBranch("branch1");
		Assert.assertFalse(branchID.contains("error"), "Branch1 was not created: " + branchID);
		
		inAppPurchaseJson.put("includedEntitlements", new JSONArray());
		inAppPurchaseJson.put("name", "IAP1");
		inAppPurchaseID1 = purchasesApi.addPurchaseItem(seasonID, inAppPurchaseJson.toString(), "ROOT", sessionToken);
		Assert.assertFalse(inAppPurchaseID1.contains("error"), "inAppPurchase was not added to the season: " + inAppPurchaseID1);
		purchaseOptionsJson.put("name", "PO1");
		purchaseOptionsID1 = purchasesApi.addPurchaseItem(seasonID, purchaseOptionsJson.toString(), inAppPurchaseID1, sessionToken);
		Assert.assertFalse(purchaseOptionsID1.contains("error"), "purchaseOptions was not added to the season: " + purchaseOptionsID1);
	}
	
	@Test (dependsOnMethods="cleanAndAddComponents", description="add bundle and premium feature to branch")
	public void addBundleAndFeatureToBranch() throws JSONException, Exception{	
		String feature = FileUtils.fileToString(filePath + "feature1.txt", "UTF-8", false);
		JSONObject jsonF = new JSONObject(feature);
		jsonF.put("name", "F1");
		jsonF.put("namespace", "ns1");
		jsonF.put("entitlement", inAppPurchaseID1);
		jsonF.put("premium", true);
		JSONObject rule = new JSONObject();
		rule.put("ruleString", "true");
		jsonF.put("premiumRule", rule);
		featureID1 = f.addFeatureToBranch(seasonID, branchID, jsonF.toString(), "ROOT", sessionToken);
		Assert.assertFalse(featureID1.contains("error"), "cannot create feature: " + featureID1);
		
		inAppPurchaseJson.put("name", "bundle");
		JSONArray includedEntitlementsArr = new JSONArray();
		includedEntitlementsArr.add(inAppPurchaseID1);
		inAppPurchaseJson.put("includedEntitlements", includedEntitlementsArr);
		bundleID1 = purchasesApi.addPurchaseItemToBranch(seasonID, branchID, inAppPurchaseJson.toString(), "ROOT", sessionToken);
		Assert.assertFalse(bundleID1.contains("error"), "inAppPurchase was not added to the season: " + bundleID1);
		
		JSONObject IAP1 = new JSONObject(purchasesApi.getPurchaseItemFromBranch(inAppPurchaseID1, branchID, sessionToken));
		Assert.assertTrue(IAP1.getString("branchStatus").equals("NONE"), "inAppPurchase1 incorrect status");
		JSONObject PO1 = new JSONObject(purchasesApi.getPurchaseItemFromBranch(purchaseOptionsID1, branchID, sessionToken));
		Assert.assertTrue(PO1.getString("branchStatus").equals("NONE"), "inAppPurchase2 incorrect status");
		JSONObject bundle = new JSONObject(purchasesApi.getPurchaseItemFromBranch(bundleID1, branchID, sessionToken));
		Assert.assertTrue(bundle.getString("branchStatus").equals("NEW"), "bundle incorrect status");
		Assert.assertTrue(bundle.getJSONArray("includedEntitlements").size() == 1, "bundle incorrect includedEntitlements size");
		Assert.assertTrue(bundle.getJSONArray("includedEntitlements").getString(0).equals(inAppPurchaseID1), "bundle incorrect includedEntitlements ud");
		
		JSONObject featureObj = new JSONObject(f.getFeatureFromBranch(featureID1, branchID, sessionToken));
		Assert.assertTrue(featureObj.getString("branchStatus").equals("NEW"), "feature incorrect status");
		Assert.assertTrue(featureObj.getString("entitlement").equals(inAppPurchaseID1), "wrong entitlement in premium feature");
		
		//find subtree root in branch
		JSONObject branch = new JSONObject(br.getBranchWithFeatures(branchID, sessionToken));
		JSONArray inAppPurchases = branch.getJSONArray("entitlements");
		Assert.assertTrue(inAppPurchases.size() ==1, "More than 1 sub tree in branch");
		Assert.assertTrue(inAppPurchases.getJSONObject(0).getString("uniqueId").equals(bundleID1), "bundle is not the branch subTree root");
		
		JSONArray features = branch.getJSONArray("features");
		Assert.assertTrue(features.size() ==1, "More than 1 sub tree in branch");
		Assert.assertTrue(features.getJSONObject(0).getString("uniqueId").equals(featureID1), "feature is not the branch subTree root");
	}
	
	@Test (dependsOnMethods="addBundleAndFeatureToBranch", description="delete IAP1 from master")
	public void deleteEntitlemenFromMaster2() throws JSONException, Exception{				
		int res = purchasesApi.deletePurchaseItemFromBranch(inAppPurchaseID1, "MASTER", sessionToken);
		Assert.assertFalse(res==200, "can delete entitlement from master");
		
		res = f.deleteFeatureFromBranch(featureID1, branchID, sessionToken);
		Assert.assertTrue(res==200, "cannot delete feature from branch");
		
		res = purchasesApi.deletePurchaseItemFromBranch(inAppPurchaseID1, "MASTER", sessionToken);
		Assert.assertFalse(res==200, "can delete entitlement from master");
	}	

	private String addBranch(String branchName) throws JSONException, IOException{
		String branch = FileUtils.fileToString(filePath + "experiments/branch1.txt", "UTF-8", false);
		JSONObject branchJson = new JSONObject(branch);
		branchJson.put("name", branchName);
		return br.createBranch(seasonID, branchJson.toString(), BranchesRestApi.MASTER, sessionToken);

	}

	@AfterTest
	private void reset(){
		baseUtils.reset(productID, sessionToken);
	}
}
