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
import tests.restapi.ProductsRestApi;

public class TestInAppPurchaseInFeatureBranchMasterMix {
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
	private String branchID1;
	private String branchID2;
	private BranchesRestApi br ;
	
	
	@BeforeClass
	@Parameters({"url", "analyticsUrl", "translationsUrl", "configPath", "sessionToken", "userName", "userPassword", "appName", "productsToDeleteFile"})
	public void init(String url, String analyticsUrl, String translationsUrl, String configPath, String sToken, String userName, String userPassword, String appName, String productsToDeleteFile) throws Exception{
		m_url = url;
		filePath = configPath;
		f = new FeaturesRestApi();
		f.setURL(m_url);
		p = new ProductsRestApi();
		p.setURL(m_url);
		br = new BranchesRestApi();
		br.setURL(m_url);
		purchasesApi = new InAppPurchasesRestApi();
		purchasesApi.setURL(m_url);
		
		baseUtils = new AirlockUtils(url, analyticsUrl, translationsUrl, configPath, sToken, userName, userPassword, appName, productsToDeleteFile);
		sessionToken = baseUtils.sessionToken;

		productID = baseUtils.createProduct();
		baseUtils.printProductToFile(productID);
		seasonID = baseUtils.createSeason(productID);
	}
	
	@Test (description = "add branch")
	public void addComponents() throws JSONException, IOException, InterruptedException{
		branchID1 = addBranch("branch1");
		Assert.assertFalse(branchID1.contains("error"), "Branch1 was not created: " + branchID1);

		
		branchID2 = addBranch("branch2");
		Assert.assertFalse(branchID1.contains("error"), "Branch1 was not created: " + branchID1);

	}
	
	@Test (dependsOnMethods = "addComponents", description = "in app purchase in branch and feature poinint to it in master.")
	public void addFeatureInMasterPurchaseInBranch() throws JSONException, IOException, InterruptedException{
	
		//add inAppPurcahse to branch
		String inAppPur = FileUtils.fileToString(filePath + "purchases/inAppPurchase1.txt", "UTF-8", false);
		inAppPurID1 = purchasesApi.addPurchaseItemToBranch(seasonID, branchID1, inAppPur, "ROOT", sessionToken);
		Assert.assertFalse (inAppPurID1.contains("error"), "Can't add inAppPurchase to branch: " + inAppPurID1);
		
		String purchaseOptions = FileUtils.fileToString(filePath + "purchases/purchaseOptions1.txt", "UTF-8", false);
		JSONObject jsonIP = new JSONObject(purchaseOptions);
		jsonIP.put("name", "PO1");
		String purchaseOptionsID = purchasesApi.addPurchaseItemToBranch(seasonID, branchID1, jsonIP.toString(), inAppPurID1, sessionToken);
		Assert.assertFalse (purchaseOptionsID.contains("error"), "Can't add purchaseOptions: " + purchaseOptionsID);

		//add feature in master
		String feature = FileUtils.fileToString(filePath + "feature1.txt", "UTF-8", false);
		JSONObject jsonF = new JSONObject(feature);
		jsonF.put("name", "F1");
		jsonF.put("namespace", "ns1");
		jsonF.put("entitlement", inAppPurID1);
		jsonF.put("premium", true);
		JSONObject premiumRule = new JSONObject();
		premiumRule.put("ruleString", "true;");
		jsonF.put("premiumRule", premiumRule);

		String res = f.addFeature(seasonID, jsonF.toString(), "ROOT", sessionToken);
		Assert.assertTrue(res.contains("error"), "can create feature in master attached to purcahse in branch");
		
		//add feature to branch1
		featureID1 = f.addFeatureToBranch(seasonID, branchID1, jsonF.toString(), "ROOT", sessionToken);
		Assert.assertFalse(featureID1.contains("error"), "cannot create feature in branch attached to purcahse in branch");
		
		//add feature to branch2
		res = f.addFeatureToBranch(seasonID, branchID2, jsonF.toString(), "ROOT", sessionToken);
		Assert.assertTrue(res.contains("error"), "can create feature in branch attached to purcahse in another branch");
	}

	@Test (dependsOnMethods = "addFeatureInMasterPurchaseInBranch", description = "in app purchase in master and feature poinint to it in branch.")
	public void addFeatureInBranchPurchaseInMaster() throws JSONException, IOException, InterruptedException{
	
		//add inAppPurcahse to branch
		String inAppPur = FileUtils.fileToString(filePath + "purchases/inAppPurchase2.txt", "UTF-8", false);
		inAppPurID2 = purchasesApi.addPurchaseItemToBranch(seasonID, "MASTER", inAppPur, "ROOT", sessionToken);
		Assert.assertFalse (inAppPurID2.contains("error"), "Can't add inAppPurchase to branch: " + inAppPurID1);
		
		String purchaseOptions = FileUtils.fileToString(filePath + "purchases/purchaseOptions1.txt", "UTF-8", false);
		JSONObject jsonIP = new JSONObject(purchaseOptions);
		jsonIP.put("name", "PO2");
		String purchaseOptionsID = purchasesApi.addPurchaseItemToBranch(seasonID, "MASTER", jsonIP.toString(), inAppPurID2, sessionToken);
		Assert.assertFalse (purchaseOptionsID.contains("error"), "Can't add purchaseOptions: " + purchaseOptionsID);

		
		String feature = FileUtils.fileToString(filePath + "feature2.txt", "UTF-8", false);
		JSONObject jsonF = new JSONObject(feature);
		jsonF.put("name", "F2");
		jsonF.put("namespace", "ns1");
		jsonF.put("entitlement", inAppPurID2);
		jsonF.put("premium", true);
		JSONObject premiumRule = new JSONObject();
		premiumRule.put("ruleString", "true;");
		jsonF.put("premiumRule", premiumRule);

		featureID2 = f.addFeatureToBranch(seasonID, branchID1, jsonF.toString(), "ROOT", sessionToken);
		Assert.assertFalse(featureID2.contains("error"), "cannot create feature in branch attached to purcahse in master");
		
		//try to delete purchase from master
		int code = purchasesApi.deletePurchaseItem(inAppPurID2, sessionToken);
		Assert.assertTrue(code!=200, "can delete feature from master that attached to feature in bramnch");
	}
	
	@Test (dependsOnMethods = "addFeatureInMasterPurchaseInBranch", description = "checkout in app purchase to branch and delete in master.")
	public void checkoutPurcahseAndDeleteFromMaster() throws Exception{
		int code = br.deleteBranch(branchID2, sessionToken);
		Assert.assertTrue(code==200, "cannot delete branch2");

		String response = br.checkoutFeature(branchID1, inAppPurID2, sessionToken);
		Assert.assertFalse(response.contains("error"), "inAppPurchase was not checked out to branch");

		String featureInBranch = f.getFeatureFromBranch(featureID2, branchID1, sessionToken);
		JSONObject featureInBranchJson = new JSONObject(featureInBranch);
		Assert.assertTrue(featureInBranchJson.getString("entitlement").equals(inAppPurID2));
		
		JSONArray purcahsesInBranch = purchasesApi.getPurchasesBySeasonFromBranch(seasonID, branchID1, sessionToken);
		Assert.assertTrue(purcahsesInBranch.size() == 2, "wrong purcahses array size in  branch2");
		
		Assert.assertTrue(purcahsesInBranch.getJSONObject(0).getString("branchStatus").equals("CHECKED_OUT"), "wrong purcahses branch status");
		Assert.assertTrue(purcahsesInBranch.getJSONObject(0).getString("name").equals("inAppPurchase2"), "wrong purcahses name");
		
		Assert.assertTrue(purcahsesInBranch.getJSONObject(1).getString("branchStatus").equals("NEW"), "wrong purcahses branch status");
		Assert.assertTrue(purcahsesInBranch.getJSONObject(1).getString("name").equals("inAppPurchase1"), "wrong purcahses name");
		
		//delete purchase from master
		code = purchasesApi.deletePurchaseItemFromBranch(inAppPurID2, "MASTER", sessionToken);
		Assert.assertTrue(code==200, "cannot delete purcahse from master that is checked out and attached to feature in branch");
		
		purcahsesInBranch = purchasesApi.getPurchasesBySeasonFromBranch(seasonID, branchID1, sessionToken);
		Assert.assertTrue(purcahsesInBranch.size() == 2, "wrong purcahses array size in  branch2");
		
		Assert.assertTrue(purcahsesInBranch.getJSONObject(0).getString("branchStatus").equals("NEW"), "wrong purcahses branch status");
		Assert.assertTrue(purcahsesInBranch.getJSONObject(0).getString("name").equals("inAppPurchase1"), "wrong purcahses name");
		
		Assert.assertTrue(purcahsesInBranch.getJSONObject(1).getString("branchStatus").equals("NEW"), "wrong purcahses branch status");
		Assert.assertTrue(purcahsesInBranch.getJSONObject(1).getString("name").equals("inAppPurchase2"), "wrong purcahses name");
		String newPurId = purcahsesInBranch.getJSONObject(1).getString("uniqueId");
		
		featureInBranch = f.getFeatureFromBranch(featureID2, branchID1, sessionToken);
		featureInBranchJson = new JSONObject(featureInBranch);
		Assert.assertTrue(featureInBranchJson.getString("entitlement").equals(newPurId));
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
