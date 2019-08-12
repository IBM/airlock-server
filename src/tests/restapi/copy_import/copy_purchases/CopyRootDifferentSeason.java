package tests.restapi.copy_import.copy_purchases;



import org.apache.wink.json4j.JSONArray;

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
import tests.restapi.SeasonsRestApi;

public class CopyRootDifferentSeason {
	private String seasonID;
	private String productID;
	private String filePath;
	private String m_url;
	private ProductsRestApi p;
	private FeaturesRestApi f;
	private SeasonsRestApi s;
	private BranchesRestApi br ;
	private String sessionToken = "";
	private AirlockUtils baseUtils;
	private InAppPurchasesRestApi purchasesApi;
	private String srcBranchID;
	private String destBranchID;
	private boolean runOnMaster;
	
	@BeforeClass
	@Parameters({"url", "analyticsUrl", "translationsUrl", "configPath", "sessionToken", "userName", "userPassword", "appName", "productsToDeleteFile", "runOnMaster"})
	public void init(String url, String analyticsUrl, String translationsUrl, String configPath, String sToken, String userName, String userPassword, String appName, String productsToDeleteFile, Boolean runOnMaster) throws Exception{
		m_url = url;
		filePath = configPath;
		p = new ProductsRestApi();
		p.setURL(m_url);
		f = new FeaturesRestApi();
		f.setURL(m_url);
		s = new SeasonsRestApi();
		s.setURL(m_url);
		br = new BranchesRestApi();
		br.setURL(url);
		purchasesApi = new InAppPurchasesRestApi();
		purchasesApi.setURL(m_url);

		baseUtils = new AirlockUtils(url, analyticsUrl, translationsUrl, configPath, sToken, userName, userPassword, appName, productsToDeleteFile);
		sessionToken = baseUtils.sessionToken;
		productID = baseUtils.createProduct();
		baseUtils.printProductToFile(productID);
		seasonID = baseUtils.createSeason(productID);
		try {
			if (runOnMaster) {
				srcBranchID = BranchesRestApi.MASTER;
			} else {
				srcBranchID = baseUtils.createBranchInExperiment(analyticsUrl);
			}
		}catch (Exception e){
			srcBranchID = null;
		}
		this.runOnMaster = runOnMaster;
	}
	
	/*
	  	Copy root under entitlement - not allowed
	  	Copy root under itself - not allowed
	 */
	
	@Test (description="Copy root to a different season")
	public void copyRoot() throws Exception{
		String entitlement1 = FileUtils.fileToString(filePath + "purchases/inAppPurchase1.txt", "UTF-8", false);
		String entitlementID1 = purchasesApi.addPurchaseItemToBranch(seasonID, srcBranchID, entitlement1, "ROOT", sessionToken);
		Assert.assertFalse(entitlementID1.contains("error"), "entitlement was not added to the season");
	
		String rootId = purchasesApi.getBranchRootId(seasonID, srcBranchID, sessionToken);
		
		//create new season
		JSONObject sJson = new JSONObject();
		sJson.put("minVersion", "2.0");
		String seasonID2 = s.addSeason(productID, sJson.toString(), sessionToken);
		Assert.assertFalse(seasonID2.contains("error"), "The second season was not created: " + seasonID2);

		if (runOnMaster) {
			destBranchID = BranchesRestApi.MASTER;
		}
		else {
			String allBranches = br.getAllBranches(seasonID2,sessionToken);
	        JSONObject jsonBranch = new JSONObject(allBranches);
	        destBranchID = jsonBranch.getJSONArray("branches").getJSONObject(1).getString("uniqueId");		 	
		}
		
		String rootId2 = purchasesApi.getBranchRootId(seasonID2, destBranchID, sessionToken);
	 	JSONArray entitlements = purchasesApi.getPurchasesBySeasonFromBranch(seasonID2, destBranchID, sessionToken);
	 	String entitlementIDSeason2 = entitlements.getJSONObject(0).getString("uniqueId");

		//can't copy root under entitlement
		String response = f.copyItemBetweenBranches(rootId, entitlementIDSeason2, "ACT", null, null, sessionToken,srcBranchID, destBranchID);
		Assert.assertTrue(response.contains("error"), "Root was copied: " + response);
		
		//can't copy root under root
		response = f.copyItemBetweenBranches(rootId, rootId2, "ACT", null, null, sessionToken,srcBranchID, destBranchID);
		Assert.assertTrue(response.contains("error"), "Root was copied: " + response);
	}
	
	
	@AfterTest
	private void reset(){
		baseUtils.reset(productID, sessionToken);
	}
}