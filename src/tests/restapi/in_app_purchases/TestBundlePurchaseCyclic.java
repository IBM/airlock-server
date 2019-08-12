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

public class TestBundlePurchaseCyclic {
	protected String seasonID;
	protected String filePath;
	protected ProductsRestApi p;
	protected FeaturesRestApi f;
	private AirlockUtils baseUtils;
	protected String productID;
	protected String m_url;
	private String sessionToken = "";
	private BranchesRestApi br ;
	protected InAppPurchasesRestApi purchasesApi;
	protected String inAppPurID1;
	protected String inAppPurID2;
	protected String inAppPurID3;
	protected String bundleID;
	protected String branchID1;
	private JSONObject inAppPurJson;
	protected String inAppPurBranchID1;
	protected String inAppPurBranchID2;
	protected String inAppPurBranchID3;
	protected String bundleBranchID;
	
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
		
		String inAppPur = FileUtils.fileToString(filePath + "purchases/inAppPurchase1.txt", "UTF-8", false);
		inAppPurJson = new JSONObject(inAppPur);
		
		
	}
	
	@Test (description = "add 3 in-app-purchase one purchase including the 3")
	public void addComponents() throws JSONException, IOException, InterruptedException{
		inAppPurJson.put("name", "IAP1");
		inAppPurID1 = purchasesApi.addPurchaseItem(seasonID, inAppPurJson.toString(), "ROOT", sessionToken);
		Assert.assertFalse (inAppPurID1.contains("error"), "Can't add inAppPurchase: " + inAppPurID1);
		
		inAppPurJson.put("name", "IAP2");
		inAppPurID2 = purchasesApi.addPurchaseItem(seasonID, inAppPurJson.toString(), "ROOT", sessionToken);
		Assert.assertFalse (inAppPurID2.contains("error"), "Can't add inAppPurchase: " + inAppPurID2);
		
		inAppPurJson.put("name", "IAP3");
		inAppPurID3 = purchasesApi.addPurchaseItem(seasonID, inAppPurJson.toString(), "ROOT", sessionToken);
		Assert.assertFalse (inAppPurID3.contains("error"), "Can't add inAppPurchase: " + inAppPurID3);
		
		String purchaseOptions = FileUtils.fileToString(filePath + "purchases/purchaseOptions1.txt", "UTF-8", false);
		JSONObject jsonIP = new JSONObject(purchaseOptions);
		jsonIP.put("name", "PO1");
		String puOptID = purchasesApi.addPurchaseItem(seasonID, jsonIP.toString(), inAppPurID1, sessionToken);
		Assert.assertFalse (puOptID.contains("error"), "Can't add purchaseOptions: " + puOptID);

		JSONArray includedPurchases = new JSONArray();
		includedPurchases.add(inAppPurID1);
		includedPurchases.add(inAppPurID2);
		includedPurchases.add(inAppPurID3);
		
		inAppPurJson.put("name", "bundle");
		inAppPurJson.put("includedEntitlements", includedPurchases);
		bundleID = purchasesApi.addPurchaseItem(seasonID, inAppPurJson.toString(), "ROOT", sessionToken);
		Assert.assertFalse (bundleID.contains("error"), "Can't add inAppPurchase: " + bundleID);
		
		String bundleStr = purchasesApi.getPurchaseItem(bundleID, sessionToken);
		JSONObject bundleObj = new JSONObject(bundleStr);
		Assert.assertTrue(bundleObj.getJSONArray("includedEntitlements").size()==3, "wrong number of includedPurchases in bundle");
				
		Assert.assertTrue(bundleObj.getJSONArray("includedEntitlements").get(0).equals(inAppPurID1), "wrong includedPurchase");
		Assert.assertTrue(bundleObj.getJSONArray("includedEntitlements").get(1).equals(inAppPurID2), "wrong includedPurchase");
		Assert.assertTrue(bundleObj.getJSONArray("includedEntitlements").get(2).equals(inAppPurID3), "wrong includedPurchase");
		
		inAppPurJson.put("includedEntitlements", new JSONArray());
	}
	
	@Test (dependsOnMethods = "addComponents", description = "update purchase as included purchase to itself")
	public void updatePurchaseAsIncludedToItself() throws JSONException, IOException, InterruptedException{
		String bundleStr = purchasesApi.getPurchaseItem(bundleID, sessionToken);
		JSONObject bundleObj = new JSONObject(bundleStr);
		JSONArray includedPurchases = bundleObj.getJSONArray("includedEntitlements");
		includedPurchases.add(bundleID);
		
		String response = purchasesApi.updatePurchaseItem(seasonID, bundleID, bundleObj.toString(), sessionToken);
		Assert.assertTrue(response.contains("error"), "can update bundle to include itself");
	}
	
	@Test (dependsOnMethods = "updatePurchaseAsIncludedToItself", description = "test cyclic purcahses. inAppPurchase1->inAppPurchase2->inAppPurchase3->inAppPurchase1")
	public void testCyclicPurchases() throws JSONException, IOException, InterruptedException{
		//inAppPurchase1->inAppPurchase2
		String purStr = purchasesApi.getPurchaseItem(inAppPurID1, sessionToken);
		JSONObject purObj = new JSONObject(purStr);
		JSONArray includedPurchases = purObj.getJSONArray("includedEntitlements");
		includedPurchases.add(inAppPurID2);
		
		String response = purchasesApi.updatePurchaseItem(seasonID, inAppPurID1, purObj.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "cannot update inAppPurID1 to include inAppPurID2: " + response);
		
		//inAppPurchase2->inAppPurchase3
		purStr = purchasesApi.getPurchaseItem(inAppPurID2, sessionToken);
		purObj = new JSONObject(purStr);
		includedPurchases = purObj.getJSONArray("includedEntitlements");
		includedPurchases.add(inAppPurID3);
		
		response = purchasesApi.updatePurchaseItem(seasonID, inAppPurID2, purObj.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "cannot update inAppPurID2 to include inAppPurID3: " + response);
		
		//inAppPurchase2->inAppPurchase3
		purStr = purchasesApi.getPurchaseItem(inAppPurID3, sessionToken);
		purObj = new JSONObject(purStr);
		includedPurchases = purObj.getJSONArray("includedEntitlements");
		includedPurchases.add(inAppPurID1);
		
		response = purchasesApi.updatePurchaseItem(seasonID, inAppPurID3, purObj.toString(), sessionToken);
		Assert.assertTrue(response.contains("error"), "can update inAppPurID3 to include inAppPurID1");
	}
	
	@Test (dependsOnMethods = "testCyclicPurchases", description = "add branch and add 3 in-app-purchase one purchase including the 3")
	public void addComponentsToBranch() throws JSONException, IOException, InterruptedException{
		branchID1 = addBranch("branch1");
		Assert.assertFalse(branchID1.contains("error"), "Branch1 was not created: " + branchID1);

		inAppPurJson.put("name", "BRANCHIAP1");
		inAppPurBranchID1 = purchasesApi.addPurchaseItemToBranch(seasonID, branchID1, inAppPurJson.toString(), "ROOT", sessionToken);
		Assert.assertFalse (inAppPurBranchID1.contains("error"), "Can't add inAppPurchase: " + inAppPurBranchID1);
		
		inAppPurJson.put("name", "BRANCHIAP2");
		inAppPurBranchID2 = purchasesApi.addPurchaseItemToBranch(seasonID, branchID1, inAppPurJson.toString(), "ROOT", sessionToken);
		Assert.assertFalse (inAppPurBranchID2.contains("error"), "Can't add inAppPurchase: " + inAppPurBranchID2);
		
		inAppPurJson.put("name", "BRANCHIAP3");
		inAppPurBranchID3 = purchasesApi.addPurchaseItemToBranch(seasonID, branchID1, inAppPurJson.toString(), "ROOT", sessionToken);
		Assert.assertFalse (inAppPurBranchID3.contains("error"), "Can't add inAppPurchase: " + inAppPurBranchID3);
		
		String purchaseOptions = FileUtils.fileToString(filePath + "purchases/purchaseOptions1.txt", "UTF-8", false);
		JSONObject jsonIP = new JSONObject(purchaseOptions);
		jsonIP.put("name", "BRANCHPO1");
		String puOptID = purchasesApi.addPurchaseItemToBranch(seasonID, branchID1, jsonIP.toString(), inAppPurBranchID1, sessionToken);
		Assert.assertFalse (puOptID.contains("error"), "Can't add purchaseOptions: " + puOptID);

		JSONArray includedPurchases = new JSONArray();
		includedPurchases.add(inAppPurBranchID1);
		includedPurchases.add(inAppPurBranchID2);
		includedPurchases.add(inAppPurBranchID3);
		
		inAppPurJson.put("name", "BRANCHbundle");
		inAppPurJson.put("includedEntitlements", includedPurchases);
		bundleBranchID = purchasesApi.addPurchaseItemToBranch(seasonID, branchID1, inAppPurJson.toString(), "ROOT", sessionToken);
		Assert.assertFalse (bundleBranchID.contains("error"), "Can't add inAppPurchase: " + bundleBranchID);
		
		String bundleStr = purchasesApi.getPurchaseItemFromBranch(bundleBranchID, branchID1, sessionToken);
		JSONObject bundleObj = new JSONObject(bundleStr);
		Assert.assertTrue(bundleObj.getJSONArray("includedEntitlements").size()==3, "wrong number of includedPurchases in bundle");
				
		Assert.assertTrue(bundleObj.getJSONArray("includedEntitlements").get(0).equals(inAppPurBranchID1), "wrong includedPurchase");
		Assert.assertTrue(bundleObj.getJSONArray("includedEntitlements").get(1).equals(inAppPurBranchID2), "wrong includedPurchase");
		Assert.assertTrue(bundleObj.getJSONArray("includedEntitlements").get(2).equals(inAppPurBranchID3), "wrong includedPurchase");
	}

	@Test (dependsOnMethods = "addComponentsToBranch", description = "test cyclic purcahses in branch. branchinAppPurchase1->branchinAppPurchase2->branchinAppPurchase3->branchinAppPurchase1")
	public void testCyclicPurchasesInBranch() throws JSONException, IOException, InterruptedException{
		//inAppPurchase1->inAppPurchase2
		String purStr = purchasesApi.getPurchaseItemFromBranch(inAppPurBranchID1, branchID1, sessionToken);
		JSONObject purObj = new JSONObject(purStr);
		JSONArray includedPurchases = purObj.getJSONArray("includedEntitlements");
		includedPurchases.add(inAppPurBranchID2);
		
		String response = purchasesApi.updatePurchaseItemInBranch(seasonID, branchID1, inAppPurBranchID1, purObj.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "cannot update inAppPurBranch ID1 to include inAppPurBranchID2: " + response);
		
		//inAppPurchase2->inAppPurchase3
		purStr = purchasesApi.getPurchaseItemFromBranch(inAppPurBranchID2, branchID1, sessionToken);
		purObj = new JSONObject(purStr);
		includedPurchases = purObj.getJSONArray("includedEntitlements");
		includedPurchases.add(inAppPurBranchID3);
		
		response = purchasesApi.updatePurchaseItemInBranch(seasonID,branchID1,  inAppPurBranchID2, purObj.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "cannot update inAppPurBranchID2 to include inAppPurBranchID3: " + response);
		
		//inAppPurchase2->inAppPurchase3
		purStr = purchasesApi.getPurchaseItemFromBranch(inAppPurBranchID3, branchID1 ,sessionToken);
		purObj = new JSONObject(purStr);
		includedPurchases = purObj.getJSONArray("includedEntitlements");
		includedPurchases.add(inAppPurBranchID1);
		
		response = purchasesApi.updatePurchaseItemInBranch(seasonID, branchID1, inAppPurBranchID3, purObj.toString(), sessionToken);
		Assert.assertTrue(response.contains("error"), "can update inAppPurBranchID3 to include inAppPurBranchID1");
	}
	
	@Test (dependsOnMethods = "testCyclicPurchasesInBranch", description = "add branch and add 3 in-app-purchase one purchase including the 3")
	public void recreateBranch() throws Exception{
		int code = br.deleteBranch(branchID1, sessionToken);
		Assert.assertTrue(code == 200, "cannot delete barnch");
		
		branchID1 = addBranch("branch1");
		Assert.assertFalse(branchID1.contains("error"), "Branch1 was not created: " + branchID1);
		
		
		
	}
	
	@Test (dependsOnMethods = "recreateBranch", description = "test cyclic purcahses in branch and master. branchinAppPurchase1->branchinAppPurchase2->branchinAppPurchase3->branchinAppPurchase1")
	public void testCyclicPurchasesInBranchAndMaster() throws JSONException, IOException, InterruptedException{
		//check out all 3 purchases
		String response = br.checkoutFeature(branchID1, inAppPurID1, sessionToken);
		Assert.assertFalse(response.contains("error"), "inAppPurID1 was not checked out to branch");
		
		response = br.checkoutFeature(branchID1, inAppPurID2, sessionToken);
		Assert.assertFalse(response.contains("error"), "inAppPurID2 was not checked out to branch");
		
		response = br.checkoutFeature(branchID1, inAppPurID3, sessionToken);
		Assert.assertFalse(response.contains("error"), "inAppPurID3 was not checked out to branch");
	
		//inAppPurchase2->inAppPurchase3 in branch
		String purStr = purchasesApi.getPurchaseItemFromBranch(inAppPurID3, branchID1 ,sessionToken);
		JSONObject purObj = new JSONObject(purStr);
		JSONArray includedPurchases = purObj.getJSONArray("includedEntitlements");
		includedPurchases.add(inAppPurID1);
		
		response = purchasesApi.updatePurchaseItemInBranch(seasonID, branchID1, inAppPurID3, purObj.toString(), sessionToken);
		Assert.assertTrue(response.contains("error"), "can update inAppPurID3 to include inAppPurID1 in barnch");
	}
	
	@Test (dependsOnMethods = "testCyclicPurchasesInBranchAndMaster", description = "create bundle with non unique included entitlements")
	public void createBundleWithNonUniqueIncludedEnt() throws JSONException, IOException, InterruptedException{

		JSONArray includedPurchases = new JSONArray();
		includedPurchases.add(inAppPurID1);
		includedPurchases.add(inAppPurID2);
		includedPurchases.add(inAppPurID1);
		
		inAppPurJson.put("name", "bundle2");
		inAppPurJson.put("includedEntitlements", includedPurchases);
		String res = purchasesApi.addPurchaseItem(seasonID, inAppPurJson.toString(), "ROOT", sessionToken);
		Assert.assertTrue (res.contains("error"), "Can add inAppPurchase with non unique entitlements: " + res);
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
