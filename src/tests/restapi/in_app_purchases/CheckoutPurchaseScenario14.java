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
import tests.restapi.InAppPurchasesRestApi;

//TODO: extend test to include purchaseOptions

public class CheckoutPurchaseScenario14 {
	protected String productID;
	protected String seasonID;
	private String branchID;
	private String inAppPurchase1;
	private String inAppPurchase2;
	private String inAppPurchase3;
	private String inAppPurchase4;
	private String mixID1;
	private JSONObject inAppPurchaseJson;
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
		purchasesApi = new InAppPurchasesRestApi();
		purchasesApi.setURL(m_url);

		br = new BranchesRestApi();
		br.setURL(m_url);

		baseUtils = new AirlockUtils(m_url, analyticsUrl, translationsUrl, configPath, sToken, userName, userPassword, appName, productsToDeleteFile);
		sessionToken = baseUtils.sessionToken;

		productID = baseUtils.createProduct();
		baseUtils.printProductToFile(productID);
		seasonID = baseUtils.createSeason(productID);
		
		String inAppPurchase = FileUtils.fileToString(filePath + "purchases/inAppPurchase1.txt", "UTF-8", false);
		inAppPurchaseJson = new JSONObject(inAppPurchase);		
	}
	

	
	@Test (description ="MIX -> (IAP1+ IAP2 + IAP3) checkout IAP1") 
	public void scenario14 () throws Exception {

		branchID = addBranch("branch1");
		Assert.assertFalse(branchID.contains("error"), "Branch1 was not created: " + branchID);

		
		String iapeMix = FileUtils.fileToString(filePath + "purchases/inAppPurchaseMutual.txt", "UTF-8", false);
		mixID1 = purchasesApi.addPurchaseItem(seasonID, iapeMix, "ROOT", sessionToken);
		Assert.assertFalse(mixID1.contains("error"), "inAppPurchase mtx was not added to the season: " + mixID1);
		
		inAppPurchaseJson.put("name", "IAP1");
		inAppPurchase1 = purchasesApi.addPurchaseItem(seasonID, inAppPurchaseJson.toString(), mixID1, sessionToken);
		Assert.assertFalse(inAppPurchase1.contains("error"), "inAppPurchase was not added to the season: " + inAppPurchase1);
		inAppPurchaseJson.put("name", "IAP2");
		inAppPurchase2 = purchasesApi.addPurchaseItem(seasonID, inAppPurchaseJson.toString(), mixID1, sessionToken);
		Assert.assertFalse(inAppPurchase2.contains("error"), "inAppPurchase was not added to the season: " + inAppPurchase2);
		inAppPurchaseJson.put("name", "IAP3");
		inAppPurchase3 = purchasesApi.addPurchaseItem(seasonID, inAppPurchaseJson.toString(), mixID1, sessionToken);
		Assert.assertFalse(inAppPurchase3.contains("error"), "inAppPurchase was not added to the season: " + inAppPurchase3);
	}
	
	@Test (dependsOnMethods="scenario14", description="Checkout everything")
	public void checkout() throws JSONException, Exception{				
		
		String res = br.checkoutFeature(branchID, inAppPurchase1, sessionToken); //checks out MTX, IAP1, IAP2 and IAP3
		Assert.assertFalse(res.contains("error"), "inAppPurchase1 was not unchecked out: " + res);
		
		
		JSONObject mix = new JSONObject(purchasesApi.getPurchaseItemFromBranch(mixID1, branchID, sessionToken));
		Assert.assertTrue(mix.getString("branchStatus").equals("CHECKED_OUT"), "inAppPurchase1 incorrect status");
		JSONObject IAP1 = new JSONObject(purchasesApi.getPurchaseItemFromBranch(inAppPurchase1, branchID, sessionToken));
		Assert.assertTrue(IAP1.getString("branchStatus").equals("CHECKED_OUT"), "inAppPurchase1 incorrect status");
		JSONObject IAP2 = new JSONObject(purchasesApi.getPurchaseItemFromBranch(inAppPurchase2, branchID, sessionToken));
		Assert.assertTrue(IAP2.getString("branchStatus").equals("CHECKED_OUT"), "inAppPurchase2 incorrect status");
		JSONObject IAP3 = new JSONObject(purchasesApi.getPurchaseItemFromBranch(inAppPurchase3, branchID, sessionToken));
		Assert.assertTrue(IAP3.getString("branchStatus").equals("CHECKED_OUT"), "inAppPurchase3 incorrect status");
		
		//find subtree root in branch
		JSONObject branch = new JSONObject(br.getBranchWithFeatures(branchID, sessionToken));
		JSONArray inAppPurchases = branch.getJSONArray("entitlements");
		Assert.assertTrue(inAppPurchases.size() ==1, "More than one sub tree in branch");
		Assert.assertTrue(inAppPurchases.getJSONObject(0).getString("uniqueId").equals(mixID1), "MTX is not the branch subTree root");
	}	

	

	@Test (dependsOnMethods="checkout", description="Cancel checkout mtx")
	public void cancelCheckoutMTX() throws JSONException, Exception{				
		
		String res = br.cancelCheckoutFeature(branchID, mixID1, sessionToken);
		Assert.assertFalse(res.contains("error"), "mix was not unchecked out: " + res);
		
		JSONObject mix = new JSONObject(purchasesApi.getPurchaseItemFromBranch(mixID1, branchID, sessionToken));
		Assert.assertTrue(mix.getString("branchStatus").equals("NONE"), "inAppPurchase1 incorrect status");
		JSONObject IAP1 = new JSONObject(purchasesApi.getPurchaseItemFromBranch(inAppPurchase1, branchID, sessionToken));
		Assert.assertTrue(IAP1.getString("branchStatus").equals("CHECKED_OUT"), "inAppPurchase1 incorrect status");
		JSONObject IAP2 = new JSONObject(purchasesApi.getPurchaseItemFromBranch(inAppPurchase2, branchID, sessionToken));
		Assert.assertTrue(IAP2.getString("branchStatus").equals("CHECKED_OUT"), "inAppPurchase2 incorrect status");
		JSONObject IAP3 = new JSONObject(purchasesApi.getPurchaseItemFromBranch(inAppPurchase3, branchID, sessionToken));
		Assert.assertTrue(IAP3.getString("branchStatus").equals("CHECKED_OUT"), "inAppPurchase3 incorrect status");
		
		//find subtree root in branch
		JSONObject branch = new JSONObject(br.getBranchWithFeatures(branchID, sessionToken));
		JSONArray inAppPurchases = branch.getJSONArray("entitlements");
		Assert.assertTrue(inAppPurchases.size() ==3, "number of subTrees in branch should be 3");
		Assert.assertTrue(inAppPurchases.getJSONObject(0).getString("uniqueId").equals(inAppPurchase1), "wrong subTree root 1");
		Assert.assertTrue(inAppPurchases.getJSONObject(1).getString("uniqueId").equals(inAppPurchase2), "wrong subTree root 2");
		Assert.assertTrue(inAppPurchases.getJSONObject(2).getString("uniqueId").equals(inAppPurchase3), "wrong subTree root 3");
	}	


	@Test (dependsOnMethods="cancelCheckoutMTX", description="Cancel checkout F1 and F3")
	public void cancelCheckoutSiblings() throws JSONException, Exception{				
		
		String res = br.cancelCheckoutFeature(branchID, inAppPurchase1, sessionToken);
		Assert.assertFalse(res.contains("error"), "inAppPurchase1 was not unchecked out: " + res);
		
	    res = br.cancelCheckoutFeature(branchID, inAppPurchase3, sessionToken);
		Assert.assertFalse(res.contains("error"), "inAppPurchase3 was not unchecked out: " + res);
		
		
		JSONObject mix = new JSONObject(purchasesApi.getPurchaseItemFromBranch(mixID1, branchID, sessionToken));
		Assert.assertTrue(mix.getString("branchStatus").equals("NONE"), "inAppPurchase1 incorrect status");
		JSONObject IAP1 = new JSONObject(purchasesApi.getPurchaseItemFromBranch(inAppPurchase1, branchID, sessionToken));
		Assert.assertTrue(IAP1.getString("branchStatus").equals("NONE"), "inAppPurchase1 incorrect status");
		JSONObject IAP2 = new JSONObject(purchasesApi.getPurchaseItemFromBranch(inAppPurchase2, branchID, sessionToken));
		Assert.assertTrue(IAP2.getString("branchStatus").equals("CHECKED_OUT"), "inAppPurchase2 incorrect status");
		JSONObject IAP3 = new JSONObject(purchasesApi.getPurchaseItemFromBranch(inAppPurchase3, branchID, sessionToken));
		Assert.assertTrue(IAP3.getString("branchStatus").equals("NONE"), "inAppPurchase3 incorrect status");
		
		//find subtree root in branch
		JSONObject branch = new JSONObject(br.getBranchWithFeatures(branchID, sessionToken));
		JSONArray inAppPurchases = branch.getJSONArray("entitlements");
		Assert.assertTrue(inAppPurchases.size() ==1, "number of subTrees in branch should be 1");
		Assert.assertTrue(inAppPurchases.getJSONObject(0).getString("uniqueId").equals(inAppPurchase2), "wrong subTree root 2");
	}	


	@Test (dependsOnMethods="cancelCheckoutSiblings", description="Checkout everything")
	public void reCheckout() throws JSONException, Exception{				
		
		inAppPurchaseJson.put("name", "F4");
		inAppPurchase4 = purchasesApi.addPurchaseItemToBranch(seasonID, branchID, inAppPurchaseJson.toString(), inAppPurchase2, sessionToken);
		Assert.assertFalse(inAppPurchase4.contains("error"), "inAppPurchase was not added to the season: " + inAppPurchase1);
		
		String res = br.checkoutFeature(branchID, inAppPurchase1, sessionToken); //checks out MTX, F1, F2 and F3
		Assert.assertFalse(res.contains("error"), "inAppPurchase1 was not unchecked out: " + res);
		
		
		JSONObject mix = new JSONObject(purchasesApi.getPurchaseItemFromBranch(mixID1, branchID, sessionToken));
		Assert.assertTrue(mix.getString("branchStatus").equals("CHECKED_OUT"), "inAppPurchase1 incorrect status");
		JSONObject IAP1 = new JSONObject(purchasesApi.getPurchaseItemFromBranch(inAppPurchase1, branchID, sessionToken));
		Assert.assertTrue(IAP1.getString("branchStatus").equals("CHECKED_OUT"), "inAppPurchase1 incorrect status");
		JSONObject IAP2 = new JSONObject(purchasesApi.getPurchaseItemFromBranch(inAppPurchase2, branchID, sessionToken));
		Assert.assertTrue(IAP2.getString("branchStatus").equals("CHECKED_OUT"), "inAppPurchase2 incorrect status");
		JSONObject IAP3 = new JSONObject(purchasesApi.getPurchaseItemFromBranch(inAppPurchase3, branchID, sessionToken));
		Assert.assertTrue(IAP3.getString("branchStatus").equals("CHECKED_OUT"), "inAppPurchase3 incorrect status");
		JSONObject IAP4 = new JSONObject(purchasesApi.getPurchaseItemFromBranch(inAppPurchase4, branchID, sessionToken));
		Assert.assertTrue(IAP4.getString("branchStatus").equals("NEW"), "inAppPurchase4 incorrect status");
		
		//find subtree root in branch
		JSONObject branch = new JSONObject(br.getBranchWithFeatures(branchID, sessionToken));
		JSONArray inAppPurchases = branch.getJSONArray("entitlements");
		Assert.assertTrue(inAppPurchases.size() ==1, "More than one sub tree in branch");
		Assert.assertTrue(inAppPurchases.getJSONObject(0).getString("uniqueId").equals(mixID1), "MTX is not the branch subTree root");
		
		Assert.assertTrue(inAppPurchases.getJSONObject(0).getJSONArray("entitlements").size() == 3, "MTX should have 3 sub inAppPurchases");
		Assert.assertTrue(inAppPurchases.getJSONObject(0).getJSONArray("entitlements").getJSONObject(0).getString("uniqueId").equals(inAppPurchase1), "inAppPurchase 1 is not the branch subTree root");
		Assert.assertTrue(inAppPurchases.getJSONObject(0).getJSONArray("entitlements").getJSONObject(1).getString("uniqueId").equals(inAppPurchase2), "inAppPurchase 2 is not the branch subTree root");
		Assert.assertTrue(inAppPurchases.getJSONObject(0).getJSONArray("entitlements").getJSONObject(2).getString("uniqueId").equals(inAppPurchase3), "inAppPurchase 3 is not the branch subTree root");
		
		Assert.assertTrue(inAppPurchases.getJSONObject(0).getJSONArray("entitlements").getJSONObject(1).getJSONArray("entitlements").size() == 1, "inAppPurchase 2 should have inAppPurchase 4 under it");
		
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
