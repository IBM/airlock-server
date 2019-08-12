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
import tests.restapi.InAppPurchasesRestApi;

//TODO: extend test to include purchaseOptions
public class CheckoutPurchaseScenario10 {
	protected String productID;
	protected String seasonID;
	private String branchID;
	private String mixID;
	private JSONObject inAppPurJson;
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
		br = new BranchesRestApi();
		br.setURL(m_url);
		purchasesApi = new InAppPurchasesRestApi();
		purchasesApi.setURL(m_url);

		baseUtils = new AirlockUtils(m_url, analyticsUrl, translationsUrl, configPath, sToken, userName, userPassword, appName, productsToDeleteFile);
		sessionToken = baseUtils.sessionToken;

		productID = baseUtils.createProduct();
		baseUtils.printProductToFile(productID);
		seasonID = baseUtils.createSeason(productID);
		
		String inAppPur = FileUtils.fileToString(filePath + "purchases/inAppPurchase1.txt", "UTF-8", false);
		inAppPurJson = new JSONObject(inAppPur);
	}

	
	/*
	 * In master add mtx and under it 3 inAppPurchases.
    	Check the mtx out
    	add new inAppPurchase under root to branch
    	move the new inAppPurchase to the checked out mtx in barnch
	 */
	@Test (description ="MTX -> IAP1, IAP2, IAP3 checkout MTX") 
	public void scenario10 () throws Exception {
		branchID = addBranch("branch1");
		Assert.assertFalse(branchID.contains("error"), "Branch1 was not created: " + branchID);

		String mix = FileUtils.fileToString(filePath + "purchases/inAppPurchaseMutual.txt", "UTF-8", false);
		mixID = purchasesApi.addPurchaseItem(seasonID, mix, "ROOT", sessionToken);
		Assert.assertFalse(mixID.contains("error"), "MTX was not added to the season: " + mixID);

		//add inAppPurchases
		inAppPurJson.put("name", RandomStringUtils.randomAlphabetic(5));		
		String inAppPurchaseID1 = purchasesApi.addPurchaseItem(seasonID, inAppPurJson.toString(), mixID, sessionToken);
		Assert.assertFalse(inAppPurchaseID1.contains("error"), "inAppPurchase1 was not added to the season: " + inAppPurchaseID1);
		
		inAppPurJson.put("name", RandomStringUtils.randomAlphabetic(5));		
		String inAppPurchaseID2 = purchasesApi.addPurchaseItem(seasonID, inAppPurJson.toString(), mixID, sessionToken);
		Assert.assertFalse(inAppPurchaseID2.contains("error"), "inAppPurchase2 was not added to the season: " + inAppPurchaseID2);
		
		inAppPurJson.put("name", RandomStringUtils.randomAlphabetic(5));		
		String inAppPurchaseID3 = purchasesApi.addPurchaseItem(seasonID, inAppPurJson.toString(), mixID, sessionToken);
		Assert.assertFalse(inAppPurchaseID3.contains("error"), "inAppPurchase3 was not added to the season: " + inAppPurchaseID3);

		String response = br.checkoutFeature(branchID, mixID, sessionToken);
		Assert.assertFalse(response.contains("error"), "MTX was not checked out to branch");
	
		//check that inAppPurchase was checked out in branch
		response = br.getBranchWithFeatures(branchID, sessionToken);
		JSONObject brJson = new JSONObject(response);
		JSONArray inAppPurchases = brJson.getJSONArray("entitlements");
		Assert.assertTrue(inAppPurchases.size()==1, "Incorrect number of checked out inAppPurchases");
		Assert.assertTrue(inAppPurchases.getJSONObject(0).getString("branchStatus").equals("CHECKED_OUT"), "inAppPurchase status is not checked_out in get branch" );
		
		//inAppPurchase is checked out in get inAppPurchase from branch
		String inAppPurchase = purchasesApi.getPurchaseItemFromBranch(mixID, branchID, sessionToken);
		JSONObject json = new JSONObject(inAppPurchase);
		Assert.assertTrue(json.getString("branchStatus").equals("CHECKED_OUT"), "inAppPurchase status is not checked_out in get inAppPurchase");
		
		//inAppPurchase is checked out in get inAppPurchases from branch
		JSONArray inAppPurchasesInBranch = purchasesApi.getPurchasesBySeasonFromBranch(seasonID, branchID, sessionToken);
		Assert.assertTrue(inAppPurchasesInBranch.size()==1, "Incorrect number of checked out inAppPurchases");
		Assert.assertTrue(inAppPurchasesInBranch.getJSONObject(0).getString("branchStatus").equals("CHECKED_OUT"), "inAppPurchase status is not checked_out in get inAppPurchases from branch" );
		
	}
	
	@Test (dependsOnMethods="scenario10", description="Add new inAppPurchase to root in branch and then move it to MTX")
	public void addNewInAppPurchase() throws JSONException, IOException{
		inAppPurJson.put("name", "IAP4");		
		String inAppPurchaseID4 = purchasesApi.addPurchaseItemToBranch(seasonID, branchID, inAppPurJson.toString(), "ROOT", sessionToken);
		Assert.assertFalse(inAppPurchaseID4.contains("error"), "inAppPurchase4 was not added to the branch: " + inAppPurchaseID4);

		//move inAppPurchase to MTX as the first child
		String feature = purchasesApi.getPurchaseItemFromBranch(mixID, branchID, sessionToken);
		JSONObject json = new JSONObject(feature);

		JSONObject newInAppPurchase = new JSONObject(purchasesApi.getPurchaseItemFromBranch(inAppPurchaseID4, branchID, sessionToken));
		JSONArray children = json.getJSONArray("entitlements");
		JSONArray newChildren = new JSONArray();		
		
		newChildren.put(newInAppPurchase);
		for (int i=0; i< children.length(); i++){
			newChildren.put(children.getJSONObject(i));
		}
		
		json.put("entitlements", newChildren);
		String response = purchasesApi.updatePurchaseItemInBranch(seasonID, branchID, mixID, json.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Failed to update MTX: " + response);
		
		String inAppPurchase = purchasesApi.getPurchaseItemFromBranch(mixID, branchID, sessionToken);
		JSONObject fJson = new JSONObject(inAppPurchase);
		Assert.assertTrue(fJson.getJSONArray("entitlements").size()==4, "Incorrect number of children under MTX");		
		Assert.assertTrue(fJson.getJSONArray("entitlements").getJSONObject(0).getString("uniqueId").equals(inAppPurchaseID4), "Incorrect first child under MTX in branch");
		Assert.assertTrue(fJson.getJSONArray("entitlements").getJSONObject(0).getString("branchStatus").equals("NEW"), "Incorrect first child's status under MTX in branch");
		
		Assert.assertTrue(fJson.getJSONArray("branchEntitlementItems").size()==4, "Incorrect number of branchInAppPurchaseItems under MTX");		
		
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
