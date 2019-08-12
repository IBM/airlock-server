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
import tests.restapi.BranchesRestApi;
import tests.restapi.FeaturesRestApi;
import tests.restapi.InAppPurchasesRestApi;
import tests.restapi.ProductsRestApi;

public class TestIncludedPurchasesInMasterBranchMix {
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
	protected String bundleID1;
	protected String bundleID2;
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
		purchasesApi = new InAppPurchasesRestApi();
		purchasesApi.setURL(m_url);
		br = new BranchesRestApi();
		br.setURL(m_url);
		
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
	
	@Test (dependsOnMethods = "addComponents", description = "add  in app purchase to branch and bundle including it in master")
	public void addBundleInMasterInAppPurcahseInBranch() throws JSONException, IOException, InterruptedException{
		
		String inAppPur = FileUtils.fileToString(filePath + "purchases/inAppPurchase1.txt", "UTF-8", false);
		inAppPurID1 = purchasesApi.addPurchaseItemToBranch(seasonID, branchID1, inAppPur, "ROOT", sessionToken);
		Assert.assertFalse (inAppPurID1.contains("error"), "Can't add inAppPurchase: " + inAppPurID1);
		
		String purchaseOptions = FileUtils.fileToString(filePath + "purchases/purchaseOptions1.txt", "UTF-8", false);
		JSONObject jsonIP = new JSONObject(purchaseOptions);
		jsonIP.put("name", "purchaseOptions1");
		String puOptID = purchasesApi.addPurchaseItemToBranch(seasonID, branchID1, jsonIP.toString(), inAppPurID1, sessionToken);
		Assert.assertFalse (puOptID.contains("error"), "Can't add purchaseOptions: " + puOptID);
		
		
		JSONArray includedPurchases = new JSONArray();
		includedPurchases.add(inAppPurID1);
		
		JSONObject iapObj = new JSONObject(inAppPur);
		iapObj.put("name", "bundle1");
		iapObj.put("includedEntitlements", includedPurchases);
		String res = purchasesApi.addPurchaseItem(seasonID, iapObj.toString(), "ROOT", sessionToken);
		Assert.assertTrue (res.contains("error"), "Can add bundle in master that includes purchases in barnch");
		
		bundleID1 = purchasesApi.addPurchaseItemToBranch(seasonID, branchID1, iapObj.toString(), "ROOT", sessionToken);
		Assert.assertFalse (bundleID1.contains("error"), "Cannot add bundle in branch that includes purchases in the same barnch");
		
		res = purchasesApi.addPurchaseItemToBranch(seasonID, branchID2, iapObj.toString(), "ROOT", sessionToken);
		Assert.assertTrue (res.contains("error"), "Can add bundle in branch that includes purchases in another barnch");
	}
	
	@Test (dependsOnMethods = "addBundleInMasterInAppPurcahseInBranch", description = "add in app purchase to master and bundle including it in branch")
	public void addBundleInBranchInAppPurcahseInMaster() throws JSONException, IOException, InterruptedException{
		
		String inAppPur = FileUtils.fileToString(filePath + "purchases/inAppPurchase2.txt", "UTF-8", false);
		inAppPurID2 = purchasesApi.addPurchaseItemToBranch(seasonID, "MASTER", inAppPur, "ROOT", sessionToken);
		Assert.assertFalse (inAppPurID2.contains("error"), "Can't add inAppPurchase: " + inAppPurID2);
		
		String purchaseOptions = FileUtils.fileToString(filePath + "purchases/purchaseOptions2.txt", "UTF-8", false);
		JSONObject jsonIP = new JSONObject(purchaseOptions);
		jsonIP.put("name", "purchaseOptions2");
		String puOptID = purchasesApi.addPurchaseItemToBranch(seasonID, "MASTER", jsonIP.toString(), inAppPurID2, sessionToken);
		Assert.assertFalse (puOptID.contains("error"), "Can't add purchaseOptions: " + puOptID);
		
		
		JSONArray includedPurchases = new JSONArray();
		includedPurchases.add(inAppPurID2);
		
		JSONObject iapObj = new JSONObject(inAppPur);
		iapObj.put("name", "bundle2");
		iapObj.put("includedEntitlements", includedPurchases);
		bundleID2 = purchasesApi.addPurchaseItemToBranch(seasonID, branchID1, iapObj.toString(), "ROOT", sessionToken);
		Assert.assertFalse (bundleID2.contains("error"), "Cannot add bundle in branch that includes purchases in master");
		
		//try to delete purchase from master
		int code = purchasesApi.deletePurchaseItem(inAppPurID2, sessionToken);
		Assert.assertTrue(code!=200, "can delete purcahse from master that is included in by=undle in branch.");
	}
	
	@Test (dependsOnMethods = "addBundleInBranchInAppPurcahseInMaster", description = "checkout in app purchase to branch and delete in master.")
	public void checkoutPurcahseAndDeleteFromMaster() throws Exception{
		int code = br.deleteBranch(branchID2, sessionToken);
		Assert.assertTrue(code==200, "cannot delete branch2");

		String response = br.checkoutFeature(branchID1, inAppPurID2, sessionToken);
		Assert.assertFalse(response.contains("error"), "inAppPurchase was not checked out to branch");

		String bundleInBranch = purchasesApi.getPurchaseItemFromBranch(bundleID2, branchID1, sessionToken);
		JSONObject bundleInBranchJson = new JSONObject(bundleInBranch);
		Assert.assertTrue(bundleInBranchJson.getJSONArray("includedEntitlements").getString(0).equals(inAppPurID2));
		
		JSONArray purcahsesInBranch = purchasesApi.getPurchasesBySeasonFromBranch(seasonID, branchID1, sessionToken);
		Assert.assertTrue(purcahsesInBranch.size() == 4, "wrong purcahses array size in  branch2");
		
		Assert.assertTrue(purcahsesInBranch.getJSONObject(0).getString("branchStatus").equals("CHECKED_OUT"), "wrong purcahses branch status");
		Assert.assertTrue(purcahsesInBranch.getJSONObject(0).getString("name").equals("inAppPurchase2"), "wrong purcahses name");
			
		//delete purchase from master
		code = purchasesApi.deletePurchaseItemFromBranch(inAppPurID2, "MASTER", sessionToken);
		Assert.assertTrue(code==200, "cannot delete purcahse from master that is checked out and included in bundle in branch");
		
		purcahsesInBranch = purchasesApi.getPurchasesBySeasonFromBranch(seasonID, branchID1, sessionToken);
		Assert.assertTrue(purcahsesInBranch.size() == 4, "wrong purcahses array size in  branch2");
		
		Assert.assertTrue(purcahsesInBranch.getJSONObject(3).getString("branchStatus").equals("NEW"), "wrong purcahses branch status");
		Assert.assertTrue(purcahsesInBranch.getJSONObject(3).getString("name").equals("inAppPurchase2"), "wrong purcahses name");
		String newPurId = purcahsesInBranch.getJSONObject(3).getString("uniqueId");
		
		bundleInBranch = purchasesApi.getPurchaseItemFromBranch(bundleID2, branchID1, sessionToken);
		bundleInBranchJson = new JSONObject(bundleInBranch);
		Assert.assertTrue(bundleInBranchJson.getJSONArray("includedEntitlements").getString(0).equals(newPurId));
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
