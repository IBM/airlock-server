package tests.restapi.in_app_purchases;

import java.io.IOException;


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

public class CheckoutPurchaseScenario12 {
	protected String productID;
	protected String seasonID;
	private String branchID;
	private String inAppPurchaseID1;
	private String inAppPurchaseID2;
	private String inAppPurchaseID3;
	private String inAppPurchaseID4;
	private String inAppPurchaseID5;
	private String inAppPurchaseID6;
	private String inAppPurchaseID7;
	private String inAppPurchaseID8;
	private String inAppPurchaseID9;
	private String inAppPurchaseID10;
	private String mixID1;
	private String mixID2;
	private String mixID3;
	private String mixID4;
	private JSONObject inAppPurJson;
	private JSONObject purOptJson;
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
		
		String inAppPur = FileUtils.fileToString(filePath + "purchases/inAppPurchase1.txt", "UTF-8", false);
		inAppPurJson = new JSONObject(inAppPur);
		
		String purOpt = FileUtils.fileToString(filePath + "purchases/purchaseOptions1.txt", "UTF-8", false);
		purOptJson = new JSONObject(purOpt);		
	}
	

	
	@Test (description ="IAP1 -> IAP10 + MIX -> IAP2 + IAP3 -> MIX -> IAP4 + IAP5 -> MTX -> IAP6, IAP7, MTX -> IAP8+IAP9 ") 
	public void scenario12 () throws Exception {
		branchID = addBranch("branch1");
		Assert.assertFalse(branchID.contains("error"), "Branch1 was not created: " + branchID);


		inAppPurJson.put("name", "IAP1");
		inAppPurchaseID1 = purchasesApi.addPurchaseItem(seasonID, inAppPurJson.toString(), "ROOT", sessionToken);
		Assert.assertFalse(inAppPurchaseID1.contains("error"), "inAppPurchase was not added to the season: " + inAppPurchaseID1);
	
		inAppPurJson.put("name", "IAP10");
		inAppPurchaseID10 = purchasesApi.addPurchaseItem(seasonID, inAppPurJson.toString(), inAppPurchaseID1, sessionToken);
		Assert.assertFalse(inAppPurchaseID10.contains("error"), "inAppPurchase was not added to the season: " + inAppPurchaseID10);

		
		String featureMix = FileUtils.fileToString(filePath + "purchases/inAppPurchaseMutual.txt", "UTF-8", false);
		mixID1 = purchasesApi.addPurchaseItem(seasonID, featureMix, inAppPurchaseID1, sessionToken);
		Assert.assertFalse(mixID1.contains("error"), "inAppPurchase was not added to the season: " + mixID1);
				
		inAppPurJson.put("name", "IAP2");
		inAppPurchaseID2 = purchasesApi.addPurchaseItem(seasonID, inAppPurJson.toString(), mixID1, sessionToken);
		Assert.assertFalse(inAppPurchaseID2.contains("error"), "inAppPurchase was not added to the season: " + inAppPurchaseID2);
		inAppPurJson.put("name", "IAP3");
		inAppPurchaseID3 = purchasesApi.addPurchaseItem(seasonID, inAppPurJson.toString(), mixID1, sessionToken);
		Assert.assertFalse(inAppPurchaseID3.contains("error"), "inAppPurchase was not added to the season: " + inAppPurchaseID3);
	
		mixID2 = purchasesApi.addPurchaseItem(seasonID, featureMix, inAppPurchaseID3, sessionToken);
		Assert.assertFalse(mixID1.contains("error"), "inAppPurchase was not added to the season: " + mixID1);
				
		inAppPurJson.put("name", "IAP4");
		inAppPurchaseID4 = purchasesApi.addPurchaseItem(seasonID, inAppPurJson.toString(), mixID2, sessionToken);
		Assert.assertFalse(inAppPurchaseID4.contains("error"), "inAppPurchase was not added to the season: " + inAppPurchaseID4);
		inAppPurJson.put("name", "IAP5");
		inAppPurchaseID5 = purchasesApi.addPurchaseItem(seasonID, inAppPurJson.toString(), mixID2, sessionToken);
		Assert.assertFalse(inAppPurchaseID5.contains("error"), "inAppPurchase was not added to the season: " + inAppPurchaseID5);

		mixID3 = purchasesApi.addPurchaseItem(seasonID, featureMix, inAppPurchaseID5, sessionToken);
		Assert.assertFalse(mixID3.contains("error"), "inAppPurchase was not added to the season: " + mixID3);
		
		mixID4 = purchasesApi.addPurchaseItem(seasonID, featureMix, mixID3, sessionToken);
		Assert.assertFalse(mixID4.contains("error"), "inAppPurchase was not added to the season: " + mixID4);
		
		inAppPurJson.put("name", "IAP6");
		inAppPurchaseID6 = purchasesApi.addPurchaseItem(seasonID, inAppPurJson.toString(), mixID3, sessionToken);
		Assert.assertFalse(inAppPurchaseID6.contains("error"), "inAppPurchase was not added to the season: " + inAppPurchaseID6);

		inAppPurJson.put("name", "IAP7");
		inAppPurchaseID7 = purchasesApi.addPurchaseItem(seasonID, inAppPurJson.toString(), mixID3, sessionToken);
		Assert.assertFalse(inAppPurchaseID7.contains("error"), "inAppPurchase was not added to the season: " + inAppPurchaseID7);

		
		inAppPurJson.put("name", "IAP8");
		inAppPurchaseID8 = purchasesApi.addPurchaseItem(seasonID, inAppPurJson.toString(), mixID4, sessionToken);
		Assert.assertFalse(inAppPurchaseID8.contains("error"), "inAppPurchase was not added to the season: " + inAppPurchaseID8);

		inAppPurJson.put("name", "IAP9");
		inAppPurchaseID9 = purchasesApi.addPurchaseItem(seasonID, inAppPurJson.toString(), mixID4, sessionToken);
		Assert.assertFalse(inAppPurchaseID9.contains("error"), "inAppPurchase was not added to the season: " + inAppPurchaseID9);

	}
	
	@Test (dependsOnMethods="scenario12", description="Checkout IAP8")
	public void checkout() throws JSONException, Exception{				

		String res = br.checkoutFeature(branchID, inAppPurchaseID8, sessionToken); //checks out the whole tree except for IAP10
		Assert.assertFalse(res.contains("error") || res.contains("Invalid response"), "featureID8 was not checked out: " + res);
		
		
		JSONObject IAP1 = new JSONObject(purchasesApi.getPurchaseItemFromBranch(inAppPurchaseID1, branchID, sessionToken));
		Assert.assertTrue(IAP1.getString("branchStatus").equals("CHECKED_OUT"), "inAppPurchase1 incorrect status");		
		JSONObject IAP2 = new JSONObject(purchasesApi.getPurchaseItemFromBranch(inAppPurchaseID2, branchID, sessionToken));
		Assert.assertTrue(IAP2.getString("branchStatus").equals("CHECKED_OUT"), "inAppPurchase2 incorrect status");
		JSONObject IAP3 = new JSONObject(purchasesApi.getPurchaseItemFromBranch(inAppPurchaseID3, branchID, sessionToken));
		Assert.assertTrue(IAP3.getString("branchStatus").equals("CHECKED_OUT"), "inAppPurchase3 incorrect status");
		JSONObject IAP4 = new JSONObject(purchasesApi.getPurchaseItemFromBranch(inAppPurchaseID4, branchID, sessionToken));
		Assert.assertTrue(IAP4.getString("branchStatus").equals("CHECKED_OUT"), "inAppPurchase4 incorrect status");	
		JSONObject IAP5 = new JSONObject(purchasesApi.getPurchaseItemFromBranch(inAppPurchaseID5, branchID, sessionToken));
		Assert.assertTrue(IAP5.getString("branchStatus").equals("CHECKED_OUT"), "inAppPurchase5 incorrect status");
		JSONObject IAP6 = new JSONObject(purchasesApi.getPurchaseItemFromBranch(inAppPurchaseID6, branchID, sessionToken));
		Assert.assertTrue(IAP6.getString("branchStatus").equals("CHECKED_OUT"), "inAppPurchase6 incorrect status");
		JSONObject IAP7 = new JSONObject(purchasesApi.getPurchaseItemFromBranch(inAppPurchaseID7, branchID, sessionToken));
		Assert.assertTrue(IAP7.getString("branchStatus").equals("CHECKED_OUT"), "inAppPurchase7 incorrect status");
		JSONObject IAP8 = new JSONObject(purchasesApi.getPurchaseItemFromBranch(inAppPurchaseID8, branchID, sessionToken));
		Assert.assertTrue(IAP8.getString("branchStatus").equals("CHECKED_OUT"), "inAppPurchase8 incorrect status");
		JSONObject IAP9 = new JSONObject(purchasesApi.getPurchaseItemFromBranch(inAppPurchaseID9, branchID, sessionToken));
		Assert.assertTrue(IAP9.getString("branchStatus").equals("CHECKED_OUT"), "inAppPurchase9 incorrect status");
		
		JSONObject mix1 = new JSONObject(purchasesApi.getPurchaseItemFromBranch(mixID1, branchID, sessionToken));
		Assert.assertTrue(mix1.getString("branchStatus").equals("CHECKED_OUT"), "mix1 incorrect status");
		JSONObject mix2 = new JSONObject(purchasesApi.getPurchaseItemFromBranch(mixID2, branchID, sessionToken));
		Assert.assertTrue(mix2.getString("branchStatus").equals("CHECKED_OUT"), "mix2 incorrect status");
		JSONObject mix3 = new JSONObject(purchasesApi.getPurchaseItemFromBranch(mixID3, branchID, sessionToken));
		Assert.assertTrue(mix3.getString("branchStatus").equals("CHECKED_OUT"), "mix3 incorrect status");
		JSONObject mix4 = new JSONObject(purchasesApi.getPurchaseItemFromBranch(mixID4, branchID, sessionToken));
		Assert.assertTrue(mix4.getString("branchStatus").equals("CHECKED_OUT"), "mix4 incorrect status");

		JSONObject IAP10 = new JSONObject(purchasesApi.getPurchaseItemFromBranch(inAppPurchaseID10, branchID, sessionToken));
		Assert.assertTrue(IAP10.getString("branchStatus").equals("NONE"), "inAppPurchase10 incorrect status");		

	}	
	
	
	@Test (dependsOnMethods="checkout", description="Cancel checkout")
	public void cancelCheckout() throws JSONException, Exception{				

		String res = br.cancelCheckoutFeatureWithSubFeatures(branchID, inAppPurchaseID1, sessionToken);
		Assert.assertFalse(res.contains("error") || res.contains("Invalid response"), "MTX4 was not unchecked out: " + res);
		
		
		JSONObject IAP1 = new JSONObject(purchasesApi.getPurchaseItemFromBranch(inAppPurchaseID1, branchID, sessionToken));
		Assert.assertTrue(IAP1.getString("branchStatus").equals("NONE"), "inAppPurchase1 incorrect status");		
		JSONObject IAP2 = new JSONObject(purchasesApi.getPurchaseItemFromBranch(inAppPurchaseID2, branchID, sessionToken));
		Assert.assertTrue(IAP2.getString("branchStatus").equals("NONE"), "inAppPurchase2 incorrect status");
		JSONObject IAP3 = new JSONObject(purchasesApi.getPurchaseItemFromBranch(inAppPurchaseID3, branchID, sessionToken));
		Assert.assertTrue(IAP3.getString("branchStatus").equals("NONE"), "inAppPurchase3 incorrect status");
		JSONObject IAP4 = new JSONObject(purchasesApi.getPurchaseItemFromBranch(inAppPurchaseID4, branchID, sessionToken));
		Assert.assertTrue(IAP4.getString("branchStatus").equals("NONE"), "inAppPurchase4 incorrect status");	
		JSONObject IAP5 = new JSONObject(purchasesApi.getPurchaseItemFromBranch(inAppPurchaseID5, branchID, sessionToken));
		Assert.assertTrue(IAP5.getString("branchStatus").equals("NONE"), "inAppPurchase5 incorrect status");
		JSONObject IAP6 = new JSONObject(purchasesApi.getPurchaseItemFromBranch(inAppPurchaseID6, branchID, sessionToken));
		Assert.assertTrue(IAP6.getString("branchStatus").equals("NONE"), "inAppPurchase6 incorrect status");
		JSONObject IAP7 = new JSONObject(purchasesApi.getPurchaseItemFromBranch(inAppPurchaseID7, branchID, sessionToken));
		Assert.assertTrue(IAP7.getString("branchStatus").equals("NONE"), "inAppPurchase7 incorrect status");
		JSONObject IAP8 = new JSONObject(purchasesApi.getPurchaseItemFromBranch(inAppPurchaseID8, branchID, sessionToken));
		Assert.assertTrue(IAP8.getString("branchStatus").equals("NONE"), "inAppPurchase8 incorrect status");
		JSONObject IAP9 = new JSONObject(purchasesApi.getPurchaseItemFromBranch(inAppPurchaseID9, branchID, sessionToken));
		Assert.assertTrue(IAP9.getString("branchStatus").equals("NONE"), "inAppPurchase9 incorrect status");
		
		JSONObject mix1 = new JSONObject(purchasesApi.getPurchaseItemFromBranch(mixID1, branchID, sessionToken));
		Assert.assertTrue(mix1.getString("branchStatus").equals("NONE"), "mix1 incorrect status");
		JSONObject mix2 = new JSONObject(purchasesApi.getPurchaseItemFromBranch(mixID2, branchID, sessionToken));
		Assert.assertTrue(mix2.getString("branchStatus").equals("NONE"), "mix2 incorrect status");
		JSONObject mix3 = new JSONObject(purchasesApi.getPurchaseItemFromBranch(mixID3, branchID, sessionToken));
		Assert.assertTrue(mix3.getString("branchStatus").equals("NONE"), "mix3 incorrect status");
		JSONObject mix4 = new JSONObject(purchasesApi.getPurchaseItemFromBranch(mixID4, branchID, sessionToken));
		Assert.assertTrue(mix4.getString("branchStatus").equals("NONE"), "mix4 incorrect status");

		JSONObject IAP10 = new JSONObject(purchasesApi.getPurchaseItemFromBranch(inAppPurchaseID10, branchID, sessionToken));
		Assert.assertTrue(IAP10.getString("branchStatus").equals("NONE"), "inAppPurchase10 incorrect status");		

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
