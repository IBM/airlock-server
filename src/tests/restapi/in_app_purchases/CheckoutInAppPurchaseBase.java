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

public class CheckoutInAppPurchaseBase {
	protected String seasonID;
	protected String filePath;
	protected ProductsRestApi p;
	protected FeaturesRestApi f;
	private BranchesRestApi br ;
	private AirlockUtils baseUtils;
	protected String productID;
	protected String m_url;
	private String sessionToken = "";
	protected InAppPurchasesRestApi purchasesApi;
	protected String inAppPurID1;
	protected String inAppPurID2;
	protected String bundleID;
	private String branchID;
	protected String purchaseOptionsID1;
	
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
	
	/*
	 * root-> inAppPurID1, inAppPurID2, bundleID
	 *             |
	 *       purchaseOptionsID1     
	 */
	@Test (description = "add 2 in-app-purchase one purchase including the 2 to master and branch1")
	public void addComponents() throws Exception{
		String inAppPur = FileUtils.fileToString(filePath + "purchases/inAppPurchase1.txt", "UTF-8", false);
		inAppPurID1 = purchasesApi.addPurchaseItem(seasonID, inAppPur, "ROOT", sessionToken);
		Assert.assertFalse (inAppPurID1.contains("error"), "Can't add inAppPurchase: " + inAppPurID1);
		
		inAppPur = FileUtils.fileToString(filePath + "purchases/inAppPurchase2.txt", "UTF-8", false);
		inAppPurID2 = purchasesApi.addPurchaseItem(seasonID, inAppPur, "ROOT", sessionToken);
		Assert.assertFalse (inAppPurID2.contains("error"), "Can't add inAppPurchase: " + inAppPurID2);
		
		String purchaseOptions = FileUtils.fileToString(filePath + "purchases/purchaseOptions1.txt", "UTF-8", false);
		purchaseOptionsID1 = purchasesApi.addPurchaseItem(seasonID, purchaseOptions, inAppPurID1, sessionToken);
		Assert.assertFalse (purchaseOptionsID1.contains("error"), "Can't add purchaseOptions: " + purchaseOptionsID1);

		JSONArray includedPurchases = new JSONArray();
		includedPurchases.add(inAppPurID1);
		includedPurchases.add(inAppPurID2);
		
		JSONObject iapObj = new JSONObject(inAppPur);
		iapObj.put("name", "bundle");
		iapObj.put("includedEntitlements", includedPurchases);
		bundleID = purchasesApi.addPurchaseItem(seasonID, iapObj.toString(), "ROOT", sessionToken);
		Assert.assertFalse (bundleID.contains("error"), "Can't add inAppPurchase: " + bundleID);
		
		String bundleStr = purchasesApi.getPurchaseItem(bundleID, sessionToken);
		JSONObject bundleObj = new JSONObject(bundleStr);
		Assert.assertTrue(bundleObj.getJSONArray("includedEntitlements").size()==2, "wrong number of includedPurchases in bundle");
				
		Assert.assertTrue(bundleObj.getJSONArray("includedEntitlements").get(0).equals(inAppPurID1), "wrong includedPurchase");
		Assert.assertTrue(bundleObj.getJSONArray("includedEntitlements").get(1).equals(inAppPurID2), "wrong includedPurchase");	
			
		branchID = addBranch("branch1");
		Assert.assertFalse(branchID.contains("error"), "Branch1 was not created: " + branchID);

		String response = br.getBranchWithFeatures(branchID, sessionToken);
		Assert.assertFalse(response.contains("error"), "getBranch failed: " + response);
		
		JSONObject branchObj = new JSONObject(response);
		JSONArray purchasesSubTrees = branchObj.getJSONArray("entitlements");
		
		Assert.assertTrue(purchasesSubTrees.size() == 0 , "wrong purchasesSubTrees size");
	}
	
	@Test (dependsOnMethods = "addComponents", description = "chcekout inAppPurID1")
	public void checkoutPurcahse() throws Exception{
		String response = br.checkoutFeature(branchID, inAppPurID1, sessionToken);
		Assert.assertFalse(response.contains("error"), "purchase was not checked out to branch: " + response);
		
		response = br.getBranchWithFeatures(branchID, sessionToken);
		Assert.assertFalse(response.contains("error"), "getBranch failed: " + response);
		
		JSONObject branchObj = new JSONObject(response);
		JSONArray purchasesSubTrees = branchObj.getJSONArray("entitlements");
		
		Assert.assertTrue(purchasesSubTrees.size() == 1 , "wrong purchasesSubTrees size");
		Assert.assertTrue(purchasesSubTrees.getJSONObject(0).getString("uniqueId").equals(inAppPurID1) , "wrong checked out purchase id");
		Assert.assertTrue(purchasesSubTrees.getJSONObject(0).getString("branchStatus").equals("CHECKED_OUT") , "wrong checked out purchase status");
		
		JSONArray purcahsesInBranch = purchasesApi.getPurchasesBySeasonFromBranch(seasonID, branchID, sessionToken);
		Assert.assertTrue(purcahsesInBranch.size()==3, "Incorrect number of checked out features");
		Assert.assertTrue(purcahsesInBranch.getJSONObject(0).getString("branchStatus").equals("CHECKED_OUT"), "purchase status is not checked_out in get features from branch" );
		Assert.assertTrue(purcahsesInBranch.getJSONObject(0).getString("uniqueId").equals(inAppPurID1), "purchase id is incorrect in branch" );
		Assert.assertTrue(purcahsesInBranch.getJSONObject(1).getString("branchStatus").equals("NONE"), "purchase status is not checked_out in get features from branch" );
		Assert.assertTrue(purcahsesInBranch.getJSONObject(1).getString("uniqueId").equals(inAppPurID2), "purchase id is incorrect in branch" );
		Assert.assertTrue(purcahsesInBranch.getJSONObject(2).getString("branchStatus").equals("NONE"), "purchase status is not checked_out in get features from branch" );
		Assert.assertTrue(purcahsesInBranch.getJSONObject(2).getString("uniqueId").equals(bundleID), "purchase id is incorrect in branch" );
		
		Assert.assertTrue(purcahsesInBranch.getJSONObject(0).getJSONArray("purchaseOptions").size() == 1, "incorrect prurchase options size in branch");
		Assert.assertTrue(purcahsesInBranch.getJSONObject(0).getJSONArray("purchaseOptions").getJSONObject(0).getString("branchStatus").equals("NONE"), "purchase options status is not checked_out in get features from branch" );
		Assert.assertTrue(purcahsesInBranch.getJSONObject(0).getJSONArray("purchaseOptions").getJSONObject(0).getString("uniqueId").equals(purchaseOptionsID1), "purchase option id is incorrect in branch" );
		
		Assert.assertTrue(purcahsesInBranch.getJSONObject(0).getJSONArray("branchPurchaseOptionsItems").size() == 1, "incorrect prurchase options size in branch");
		Assert.assertTrue(purcahsesInBranch.getJSONObject(0).getJSONArray("branchPurchaseOptionsItems").getString(0).equals("ns1.purchaseOptions1"), "wrong purchase options name in branch" );
	}
	
	
	@Test (dependsOnMethods = "checkoutPurcahse", description = "cancel chcekout inAppPurID1")
	public void cancelCheckoutPurcahse() throws Exception{
		String response = br.cancelCheckoutFeature(branchID, inAppPurID1, sessionToken);
		Assert.assertFalse(response.contains("error"), "purcahse  was not canceled checked out from branch: " + response);
		
		response = br.getBranchWithFeatures(branchID, sessionToken);
		Assert.assertFalse(response.contains("error"), "getBranch failed: " + response);
		
		JSONObject branchObj = new JSONObject(response);
		JSONArray purchasesSubTrees = branchObj.getJSONArray("entitlements");
		
		Assert.assertTrue(purchasesSubTrees.size() == 0 , "wrong purchasesSubTrees size");
		
		JSONArray purcahsesInBranch = purchasesApi.getPurchasesBySeasonFromBranch(seasonID, branchID, sessionToken);
		Assert.assertTrue(purcahsesInBranch.size()==3, "Incorrect number of checked out features");
		Assert.assertTrue(purcahsesInBranch.getJSONObject(0).getString("branchStatus").equals("NONE"), "purchase status is not checked_out in get features from branch" );
		Assert.assertTrue(purcahsesInBranch.getJSONObject(0).getString("uniqueId").equals(inAppPurID1), "purchase id is incorrect in branch" );
		Assert.assertTrue(purcahsesInBranch.getJSONObject(1).getString("branchStatus").equals("NONE"), "purchase status is not checked_out in get features from branch" );
		Assert.assertTrue(purcahsesInBranch.getJSONObject(1).getString("uniqueId").equals(inAppPurID2), "purchase id is incorrect in branch" );
		Assert.assertTrue(purcahsesInBranch.getJSONObject(2).getString("branchStatus").equals("NONE"), "purchase status is not checked_out in get features from branch" );
		Assert.assertTrue(purcahsesInBranch.getJSONObject(2).getString("uniqueId").equals(bundleID), "purchase id is incorrect in branch" );
		
		Assert.assertTrue(purcahsesInBranch.getJSONObject(0).getJSONArray("purchaseOptions").size() == 1, "incorrect prurchase options size in branch");
		Assert.assertTrue(purcahsesInBranch.getJSONObject(0).getJSONArray("purchaseOptions").getJSONObject(0).getString("branchStatus").equals("NONE"), "purchase options status is not checked_out in get features from branch" );
		Assert.assertTrue(purcahsesInBranch.getJSONObject(0).getJSONArray("purchaseOptions").getJSONObject(0).getString("uniqueId").equals(purchaseOptionsID1), "purchase option id is incorrect in branch" );
	}
	
	@Test (dependsOnMethods = "cancelCheckoutPurcahse", description = "chcekout purchaseOptionsID1")
	public void checkoutPurcahseOptions() throws Exception{
		String response = br.checkoutFeature(branchID, purchaseOptionsID1, sessionToken);
		Assert.assertFalse(response.contains("error"), "purchaseOptions was not checked out to branch: " + response);
		
		response = br.getBranchWithFeatures(branchID, sessionToken);
		Assert.assertFalse(response.contains("error"), "getBranch failed: " + response);
		
		JSONObject branchObj = new JSONObject(response);
		JSONArray purchasesSubTrees = branchObj.getJSONArray("entitlements");
		
		Assert.assertTrue(purchasesSubTrees.size() == 1 , "wrong purchasesSubTrees size");
		Assert.assertTrue(purchasesSubTrees.getJSONObject(0).getString("uniqueId").equals(inAppPurID1) , "wrong checked out purchase id");
		Assert.assertTrue(purchasesSubTrees.getJSONObject(0).getString("branchStatus").equals("CHECKED_OUT") , "wrong checked out purchase status");
		
		Assert.assertTrue(purchasesSubTrees.getJSONObject(0).getJSONArray("purchaseOptions").size() == 1, "incorrect prurchase options size in branch");
		Assert.assertTrue(purchasesSubTrees.getJSONObject(0).getJSONArray("purchaseOptions").getJSONObject(0).getString("branchStatus").equals("CHECKED_OUT"), "purchase options status is not checked_out in get features from branch" );
		Assert.assertTrue(purchasesSubTrees.getJSONObject(0).getJSONArray("purchaseOptions").getJSONObject(0).getString("uniqueId").equals(purchaseOptionsID1), "purchase option id is incorrect in branch" );
		
		JSONArray purcahsesInBranch = purchasesApi.getPurchasesBySeasonFromBranch(seasonID, branchID, sessionToken);
		Assert.assertTrue(purcahsesInBranch.size()==3, "Incorrect number of checked out features");
		Assert.assertTrue(purcahsesInBranch.getJSONObject(0).getString("branchStatus").equals("CHECKED_OUT"), "purchase status is not checked_out in get features from branch" );
		Assert.assertTrue(purcahsesInBranch.getJSONObject(0).getString("uniqueId").equals(inAppPurID1), "purchase id is incorrect in branch" );
		Assert.assertTrue(purcahsesInBranch.getJSONObject(1).getString("branchStatus").equals("NONE"), "purchase status is not checked_out in get features from branch" );
		Assert.assertTrue(purcahsesInBranch.getJSONObject(1).getString("uniqueId").equals(inAppPurID2), "purchase id is incorrect in branch" );
		Assert.assertTrue(purcahsesInBranch.getJSONObject(2).getString("branchStatus").equals("NONE"), "purchase status is not checked_out in get features from branch" );
		Assert.assertTrue(purcahsesInBranch.getJSONObject(2).getString("uniqueId").equals(bundleID), "purchase id is incorrect in branch" );
		
		Assert.assertTrue(purcahsesInBranch.getJSONObject(0).getJSONArray("purchaseOptions").size() == 1, "incorrect prurchase options size in branch");
		Assert.assertTrue(purcahsesInBranch.getJSONObject(0).getJSONArray("purchaseOptions").getJSONObject(0).getString("branchStatus").equals("CHECKED_OUT"), "purchase options status is not checked_out in get features from branch" );
		Assert.assertTrue(purcahsesInBranch.getJSONObject(0).getJSONArray("purchaseOptions").getJSONObject(0).getString("uniqueId").equals(purchaseOptionsID1), "purchase option id is incorrect in branch" );
		
		Assert.assertTrue(purcahsesInBranch.getJSONObject(0).getJSONArray("branchPurchaseOptionsItems").size() == 1, "incorrect prurchase options size in branch");
		Assert.assertTrue(purcahsesInBranch.getJSONObject(0).getJSONArray("branchPurchaseOptionsItems").getString(0).equals("ns1.purchaseOptions1"), "wrong purchase options name in branch" );
	}
	
	@Test (dependsOnMethods = "checkoutPurcahseOptions", description = "cancel chcekout purchaseOptionsID1")
	public void cancelCheckoutPurcahseOptions() throws Exception{
		String response = br.cancelCheckoutFeature(branchID, purchaseOptionsID1, sessionToken);
		Assert.assertFalse(response.contains("error"), "purcahse  was not canceled checked out from branch: " + response);
		
		response = br.getBranchWithFeatures(branchID, sessionToken);
		Assert.assertFalse(response.contains("error"), "getBranch failed: " + response);
		
		JSONObject branchObj = new JSONObject(response);
		JSONArray purchasesSubTrees = branchObj.getJSONArray("entitlements");
		
		Assert.assertTrue(purchasesSubTrees.size() == 1 , "wrong purchasesSubTrees size");
		Assert.assertTrue(purchasesSubTrees.getJSONObject(0).getString("uniqueId").equals(inAppPurID1) , "wrong checked out purchase id");
		Assert.assertTrue(purchasesSubTrees.getJSONObject(0).getString("branchStatus").equals("CHECKED_OUT") , "wrong checked out purchase status");
		
		JSONArray purcahsesInBranch = purchasesApi.getPurchasesBySeasonFromBranch(seasonID, branchID, sessionToken);
		Assert.assertTrue(purcahsesInBranch.size()==3, "Incorrect number of checked out features");
		Assert.assertTrue(purcahsesInBranch.getJSONObject(0).getString("branchStatus").equals("CHECKED_OUT"), "purchase status is not checked_out in get features from branch" );
		Assert.assertTrue(purcahsesInBranch.getJSONObject(0).getString("uniqueId").equals(inAppPurID1), "purchase id is incorrect in branch" );
		Assert.assertTrue(purcahsesInBranch.getJSONObject(1).getString("branchStatus").equals("NONE"), "purchase status is not checked_out in get features from branch" );
		Assert.assertTrue(purcahsesInBranch.getJSONObject(1).getString("uniqueId").equals(inAppPurID2), "purchase id is incorrect in branch" );
		Assert.assertTrue(purcahsesInBranch.getJSONObject(2).getString("branchStatus").equals("NONE"), "purchase status is not checked_out in get features from branch" );
		Assert.assertTrue(purcahsesInBranch.getJSONObject(2).getString("uniqueId").equals(bundleID), "purchase id is incorrect in branch" );
		
		Assert.assertTrue(purcahsesInBranch.getJSONObject(0).getJSONArray("purchaseOptions").size() == 1, "incorrect prurchase options size in branch");
		Assert.assertTrue(purcahsesInBranch.getJSONObject(0).getJSONArray("purchaseOptions").getJSONObject(0).getString("branchStatus").equals("NONE"), "purchase options status is not checked_out in get features from branch" );
		Assert.assertTrue(purcahsesInBranch.getJSONObject(0).getJSONArray("purchaseOptions").getJSONObject(0).getString("uniqueId").equals(purchaseOptionsID1), "purchase option id is incorrect in branch" );
		
		Assert.assertTrue(purcahsesInBranch.getJSONObject(0).getJSONArray("branchPurchaseOptionsItems").size() == 1, "incorrect prurchase options size in branch");
		Assert.assertTrue(purcahsesInBranch.getJSONObject(0).getJSONArray("branchPurchaseOptionsItems").getString(0).equals("ns1.purchaseOptions1"), "wrong purchase options name in branch" );
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
