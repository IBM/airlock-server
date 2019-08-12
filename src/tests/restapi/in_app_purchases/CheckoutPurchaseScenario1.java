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

public class CheckoutPurchaseScenario1 {
	protected String productID;
	protected String seasonID;
	private String branchID;
	private JSONObject inAppPurJson;
	private JSONObject purOptJson;
	protected String filePath;
	protected String m_url;
	private String sessionToken = "";
	private AirlockUtils baseUtils;
	private BranchesRestApi br ;
	protected InAppPurchasesRestApi purchasesApi;
	private String inAppPurID1;


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
		
		String purOpt = FileUtils.fileToString(filePath + "purchases/purchaseOptions1.txt", "UTF-8", false);
		purOptJson = new JSONObject(purOpt);
	}

	//IP1 -> CR1, CR2
	@Test (description ="IP1 -> CR1, CR2, checkout IP1") 
	public void scenario1 () throws Exception {
		branchID = addBranch("branch1");
		Assert.assertFalse(branchID.contains("error"), "Branch1 was not created: " + branchID);

		//add inAppPurchase with configuration
		inAppPurJson.put("name", RandomStringUtils.randomAlphabetic(5));		
		inAppPurID1 = purchasesApi.addPurchaseItem(seasonID, inAppPurJson.toString(), "ROOT", sessionToken);
		Assert.assertFalse(inAppPurID1.contains("error"), "inAppPurchase was not added to the season: " + inAppPurID1);

		String configuration = FileUtils.fileToString(filePath + "configuration_rule1.txt", "UTF-8", false);
		String configID1 = purchasesApi.addPurchaseItem(seasonID, configuration, inAppPurID1, sessionToken);
		Assert.assertFalse(configID1.contains("error"), "Configuration1 was not added to the season");

		String configuration2 = FileUtils.fileToString(filePath + "configuration_rule2.txt", "UTF-8", false);
		String configID2 = purchasesApi.addPurchaseItem(seasonID, configuration2, inAppPurID1, sessionToken);
		Assert.assertFalse(configID2.contains("error"), "Configuration2 was not added to the season");

		String response = br.checkoutFeature(branchID, inAppPurID1, sessionToken);
		Assert.assertFalse(response.contains("error"), "inAppPurchase was not checked out to branch");

		//inAppPurchase can't be checked out twice
		response = br.checkoutFeature(branchID, inAppPurID1, sessionToken);
		Assert.assertTrue(response.contains("error"), "inAppPurchase was checked out twice");

		//check that inAppPurchase was checked out in branch
		response = br.getBranchWithFeatures(branchID, sessionToken);
		JSONObject brJson = new JSONObject(response);
		JSONArray inAppPurchases = brJson.getJSONArray("entitlements");
		Assert.assertTrue(inAppPurchases.size()==1, "Incorrect number of checked out inAppPurchases");
		Assert.assertTrue(inAppPurchases.getJSONObject(0).getString("branchStatus").equals("CHECKED_OUT"), "inAppPurchase status is not checked_out in get branch" );

		//inAppPurchase is checked out in get inAppPurchase from branch
		String inAppPurchase = purchasesApi.getPurchaseItemFromBranch(inAppPurID1, branchID, sessionToken);
		inAppPurJson = new JSONObject(inAppPurchase);
		Assert.assertTrue(inAppPurJson.getString("branchStatus").equals("CHECKED_OUT"), "inAppPurchase status is not checked_out in get inAppPurchase");

		//inAppPurchase is checked out in get inAppPurchases from branch
		JSONArray inAppPurchasesInBranch = purchasesApi.getPurchasesBySeasonFromBranch(seasonID, branchID, sessionToken);
		Assert.assertTrue(inAppPurchasesInBranch.size()==1, "Incorrect number of checked out inAppPurchases");
		Assert.assertTrue(inAppPurchasesInBranch.getJSONObject(0).getString("branchStatus").equals("CHECKED_OUT"), "inAppPurchase status is not checked_out in get inAppPurchases from branch" );

		//check that configuration was checked out
		Assert.assertTrue(inAppPurchases.getJSONObject(0).getJSONArray("configurationRules").getJSONObject(0).getString("branchStatus").equals("CHECKED_OUT"), "Configuration rule1 status is not checked_out in get branch" );
		Assert.assertTrue(inAppPurchases.getJSONObject(0).getJSONArray("configurationRules").getJSONObject(1).getString("branchStatus").equals("CHECKED_OUT"), "Configuration rule2 status is not checked_out in get branch" );
		Assert.assertTrue(new JSONObject(purchasesApi.getPurchaseItemFromBranch(configID1, branchID, sessionToken)).getString("branchStatus").equals("CHECKED_OUT"), "Configuration rule1 status status is not checked_out in get inAppPurchase");	//get configuration rule from branch
		Assert.assertTrue(new JSONObject(purchasesApi.getPurchaseItemFromBranch(configID2, branchID, sessionToken)).getString("branchStatus").equals("CHECKED_OUT"), "Configuration rule1 status status is not checked_out in get inAppPurchase");	//get configuration rule from branch
		Assert.assertTrue(inAppPurchasesInBranch.getJSONObject(0).getJSONArray("configurationRules").getJSONObject(0).getString("branchStatus").equals("CHECKED_OUT"), "Configuration rule1 status is not checked_out in get inAppPurchases from branch" );
		Assert.assertTrue(inAppPurchasesInBranch.getJSONObject(0).getJSONArray("configurationRules").getJSONObject(1).getString("branchStatus").equals("CHECKED_OUT"), "Configuration rule2 status is not checked_out in get inAppPurchases from branch" );


		//uncheckout PI1
		String res = br.cancelCheckoutFeature(branchID, inAppPurID1, sessionToken);
		Assert.assertFalse(res.contains("error"), "inAppPurchase was not unchecked out: " + res);
		brJson = new JSONObject(br.getBranchWithFeatures(branchID, sessionToken));
		Assert.assertTrue(brJson.getJSONArray("entitlements").size()==0, "Incorrect number of checked out inAppPurchases");

		inAppPurchasesInBranch = purchasesApi.getPurchasesBySeasonFromBranch(seasonID, branchID, sessionToken);
		Assert.assertTrue(inAppPurchasesInBranch.size()==1, "Incorrect number of inAppPurchases in branch");
		Assert.assertTrue(inAppPurchasesInBranch.getJSONObject(0).getString("branchStatus").equals("NONE"), "inAppPurchase status is not NONE in get inAppPurchases from branch" );
		Assert.assertTrue(inAppPurchasesInBranch.getJSONObject(0).getJSONArray("configurationRules").getJSONObject(0).getString("branchStatus").equals("NONE"), "Configuration rule1 status is not NONE in get inAppPurchases from branch" );
		Assert.assertTrue(inAppPurchasesInBranch.getJSONObject(0).getJSONArray("configurationRules").getJSONObject(1).getString("branchStatus").equals("NONE"), "Configuration rule2 status is not NONE in get inAppPurchases from branch" );

		Assert.assertTrue(new JSONObject(purchasesApi.getPurchaseItemFromBranch(inAppPurID1, branchID, sessionToken)).getString("branchStatus").equals("NONE"), "Feature status status is not NONE in get inAppPurchase");	
		Assert.assertTrue(new JSONObject(purchasesApi.getPurchaseItemFromBranch(configID1, branchID, sessionToken)).getString("branchStatus").equals("NONE"), "Configuration rule1 status status is not NONE in get inAppPurchase");	//get configuration rule from branch
		Assert.assertTrue(new JSONObject(purchasesApi.getPurchaseItemFromBranch(configID2, branchID, sessionToken)).getString("branchStatus").equals("NONE"), "Configuration rule2 status status is not NONE in get inAppPurchase");	//get configuration rule from branch	
	}

	//IP1 -> CR1, CR2 -> PO1 -> CR3, CR4
	@Test (dependsOnMethods = "scenario1", description ="IP1 -> CR1, CR2 -> PO1 -> CR3, CR4, checkout PO1") 
	public void scenario2 () throws Exception {
		//add purchaseOptions with configuration
		purOptJson.put("name", RandomStringUtils.randomAlphabetic(5));		
		String purOptID1 = purchasesApi.addPurchaseItem(seasonID, purOptJson.toString(), inAppPurID1, sessionToken);
		Assert.assertFalse(inAppPurID1.contains("error"), "purchaseOptions was not added to the season: " + inAppPurID1);

		String configuration = FileUtils.fileToString(filePath + "configuration_rule1.txt", "UTF-8", false);
		JSONObject configObj = new JSONObject(configuration);
		configObj.put("name", "CR3");
		String configID3 = purchasesApi.addPurchaseItem(seasonID, configObj.toString(), purOptID1, sessionToken);
		Assert.assertFalse(configID3.contains("error"), "Configuration3 was not added to the season");

		String configuration2 = FileUtils.fileToString(filePath + "configuration_rule2.txt", "UTF-8", false);
		configObj = new JSONObject(configuration2);
		configObj.put("name", "CR4");
		String configID4 = purchasesApi.addPurchaseItem(seasonID, configObj.toString(), purOptID1, sessionToken);
		Assert.assertFalse(configID4.contains("error"), "Configuration4 was not added to the season");

		String response = br.checkoutFeature(branchID, purOptID1, sessionToken);
		Assert.assertFalse(response.contains("error"), "purchaseOptions was not checked out to branch");

		//inAppPurchase can't be checked out twice
		response = br.checkoutFeature(branchID, purOptID1, sessionToken);
		Assert.assertTrue(response.contains("error"), "purchaseOptions was checked out twice");

		//check that purchaseOptions was checked out in branch
		response = br.getBranchWithFeatures(branchID, sessionToken);
		JSONObject brJson = new JSONObject(response);
		JSONArray inAppPurchases = brJson.getJSONArray("entitlements");
		Assert.assertTrue(inAppPurchases.size()==1, "Incorrect number of checked out inAppPurchases");
		Assert.assertTrue(inAppPurchases.getJSONObject(0).getString("branchStatus").equals("CHECKED_OUT"), "inAppPurchase status is not checked_out in get branch" );
		
		Assert.assertTrue(inAppPurchases.getJSONObject(0).getJSONArray("configurationRules").getJSONObject(0).getString("branchStatus").equals("CHECKED_OUT"), "Configuration rule3 status is not checked_out in get branch" );
		Assert.assertTrue(inAppPurchases.getJSONObject(0).getJSONArray("configurationRules").getJSONObject(1).getString("branchStatus").equals("CHECKED_OUT"), "Configuration rule4 status is not checked_out in get branch" );
				
		JSONArray purOptsArr = inAppPurchases.getJSONObject(0).getJSONArray("purchaseOptions");
		Assert.assertTrue(purOptsArr.size()==1, "Incorrect number of checked out purchaseOptions");
		Assert.assertTrue(purOptsArr.getJSONObject(0).getString("branchStatus").equals("CHECKED_OUT"), "purchaseOptions status is not checked_out in get branch" );
		
		JSONArray configArr = purOptsArr.getJSONObject(0).getJSONArray("configurationRules");
		Assert.assertTrue(configArr.size()==2, "Incorrect number config rules");
		Assert.assertTrue(configArr.getJSONObject(0).getString("branchStatus").equals("CHECKED_OUT"), "config rule status is not checked_out in get branch" );
		Assert.assertTrue(configArr.getJSONObject(1).getString("branchStatus").equals("CHECKED_OUT"), "config rule status is not checked_out in get branch" );
			
		//purchaseOptions is checked out in get  purchases from branch
		String purOpt = purchasesApi.getPurchaseItemFromBranch(purOptID1, branchID, sessionToken);
		purOptJson = new JSONObject(purOpt);
		Assert.assertTrue(purOptJson.getString("branchStatus").equals("CHECKED_OUT"), "purchaseOptions status is not checked_out in get inAppPurchase");

		//purchaseOptions is checked out in get Purchases from branch
		JSONArray inAppPurchasesInBranch = purchasesApi.getPurchasesBySeasonFromBranch(seasonID, branchID, sessionToken);
		Assert.assertTrue(inAppPurchasesInBranch.size()==1, "Incorrect number of checked out inAppPurchases");
		Assert.assertTrue(inAppPurchasesInBranch.getJSONObject(0).getString("branchStatus").equals("CHECKED_OUT"), "inAppPurchase status is not checked_out in get inAppPurchases from branch" );

		purOptsArr = inAppPurchasesInBranch.getJSONObject(0).getJSONArray("purchaseOptions");
		Assert.assertTrue(purOptsArr.size()==1, "Incorrect number of checked out purchaseOptions");
		Assert.assertTrue(purOptsArr.getJSONObject(0).getString("branchStatus").equals("CHECKED_OUT"), "purchaseOptions status is not checked_out in get branch" );
		
		configArr = purOptsArr.getJSONObject(0).getJSONArray("configurationRules");
		Assert.assertTrue(configArr.size()==2, "Incorrect number config rules");
		Assert.assertTrue(configArr.getJSONObject(0).getString("branchStatus").equals("CHECKED_OUT"), "config rule status is not checked_out in get branch" );
		Assert.assertTrue(configArr.getJSONObject(1).getString("branchStatus").equals("CHECKED_OUT"), "config rule status is not checked_out in get branch" );
		Assert.assertTrue(new JSONObject(purchasesApi.getPurchaseItemFromBranch(configID3, branchID, sessionToken)).getString("branchStatus").equals("CHECKED_OUT"), "Configuration rule3 status status is not checked_out in get inAppPurchase");	//get configuration rule from branch
		Assert.assertTrue(new JSONObject(purchasesApi.getPurchaseItemFromBranch(configID4, branchID, sessionToken)).getString("branchStatus").equals("CHECKED_OUT"), "Configuration rule4 status status is not checked_out in get inAppPurchase");	//get configuration rule from branch
		
		//uncheckout PO1
		String res = br.cancelCheckoutFeature(branchID, purOptID1, sessionToken);
		Assert.assertFalse(res.contains("error"), "purchaseOptions was not unchecked out");

		response = br.getBranchWithFeatures(branchID, sessionToken);
		brJson = new JSONObject(response);
		inAppPurchases = brJson.getJSONArray("entitlements");
		Assert.assertTrue(inAppPurchases.size()==1, "Incorrect number of checked out inAppPurchases");
		Assert.assertTrue(inAppPurchases.getJSONObject(0).getString("branchStatus").equals("CHECKED_OUT"), "inAppPurchase status is not checked_out in get branch" );
		Assert.assertTrue(inAppPurchases.getJSONObject(0).getJSONArray("configurationRules").getJSONObject(0).getString("branchStatus").equals("CHECKED_OUT"), "Configuration rule3 status is not checked_out in get branch" );
		Assert.assertTrue(inAppPurchases.getJSONObject(0).getJSONArray("configurationRules").getJSONObject(1).getString("branchStatus").equals("CHECKED_OUT"), "Configuration rule4 status is not checked_out in get branch" );
		
		purOptsArr = inAppPurchases.getJSONObject(0).getJSONArray("purchaseOptions");
		Assert.assertTrue(purOptsArr.size()==0, "Incorrect number of checked out purchaseOptions");
		
		//purchaseOptions is not checked out in get Purchases from branch
		inAppPurchasesInBranch = purchasesApi.getPurchasesBySeasonFromBranch(seasonID, branchID, sessionToken);
		Assert.assertTrue(inAppPurchasesInBranch.size()==1, "Incorrect number of checked out inAppPurchases");
		Assert.assertTrue(inAppPurchasesInBranch.getJSONObject(0).getString("branchStatus").equals("CHECKED_OUT"), "inAppPurchase status is not checked_out in get inAppPurchases from branch" );
		Assert.assertTrue(inAppPurchasesInBranch.getJSONObject(0).getJSONArray("configurationRules").getJSONObject(0).getString("branchStatus").equals("CHECKED_OUT"), "Configuration rule3 status is not checked_out in get branch" );
		Assert.assertTrue(inAppPurchasesInBranch.getJSONObject(0).getJSONArray("configurationRules").getJSONObject(1).getString("branchStatus").equals("CHECKED_OUT"), "Configuration rule4 status is not checked_out in get branch" );
		
		
		purOptsArr = inAppPurchasesInBranch.getJSONObject(0).getJSONArray("purchaseOptions");
		Assert.assertTrue(purOptsArr.size()==1, "Incorrect number of checked out purchaseOptions");
		Assert.assertTrue(purOptsArr.getJSONObject(0).getString("branchStatus").equals("NONE"), "purchaseOptions status is not checked_out in get branch" );
		
		configArr = purOptsArr.getJSONObject(0).getJSONArray("configurationRules");
		Assert.assertTrue(configArr.size()==2, "Incorrect number config rules");
		Assert.assertTrue(configArr.getJSONObject(0).getString("branchStatus").equals("NONE"), "config rule status is not checked_out in get branch" );
		Assert.assertTrue(configArr.getJSONObject(1).getString("branchStatus").equals("NONE"), "config rule status is not checked_out in get branch" );
		
		Assert.assertTrue(new JSONObject(purchasesApi.getPurchaseItemFromBranch(configID3, branchID, sessionToken)).getString("branchStatus").equals("NONE"), "Configuration rule3 status status is not checked_out in get inAppPurchase");	//get configuration rule from branch
		Assert.assertTrue(new JSONObject(purchasesApi.getPurchaseItemFromBranch(configID4, branchID, sessionToken)).getString("branchStatus").equals("NONE"), "Configuration rule4 status status is not checked_out in get inAppPurchase");	//get configuration rule from branch
		
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
