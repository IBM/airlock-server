package tests.restapi.copy_import.copy_purchases;


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
import tests.restapi.FeaturesRestApi;
import tests.restapi.InAppPurchasesRestApi;
import tests.restapi.ProductsRestApi;
import tests.restapi.SeasonsRestApi;

public class CopyWithMinVersion {
	private String seasonID;
	private String seasonID2;
	private String productID;
	private String entitlementID1;
	private String entitlementID2;
	private String filePath;
	private String m_url;
	private ProductsRestApi p;
	private FeaturesRestApi f;
	private SeasonsRestApi s;
	private BranchesRestApi br ;	
	private String sessionToken = "";
	private AirlockUtils baseUtils;
	private String srcBranchID;
	private String destBranchID;
	private boolean runOnMaster;
	private InAppPurchasesRestApi purchasesApi;
	

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
	
	
	@Test(description = "Add entitlement with minAppVersion=1")
	public void addEntitlement() throws IOException, JSONException{
		String entitlement = FileUtils.fileToString(filePath + "purchases/inAppPurchase1.txt", "UTF-8", false);
		JSONObject eJson = new JSONObject(entitlement);
		eJson.put("minAppVersion", "1.0");
		entitlementID1 = purchasesApi.addPurchaseItemToBranch(seasonID, srcBranchID, eJson.toString(), "ROOT", sessionToken);
		Assert.assertFalse(entitlementID1.contains("error"), "entitlement was not created: " + entitlementID1 );
	}
	
	@Test (dependsOnMethods = "addEntitlement", description="Create new season with minVersion=0.85")
	public void createNewProduct() throws Exception{
		//add season to second product
		JSONObject sJson = new JSONObject();
		sJson.put("minVersion", "0.85");		
		seasonID2 = s.addSeason(productID, sJson.toString(), sessionToken);
		Assert.assertFalse(seasonID2.contains("error"), "The season was not created in the new product: " + seasonID2);
		
		if (runOnMaster) {
			destBranchID = BranchesRestApi.MASTER;
		}
		else {
			String allBranches = br.getAllBranches(seasonID2,sessionToken);
	        JSONObject jsonBranch = new JSONObject(allBranches);
	        destBranchID = jsonBranch.getJSONArray("branches").getJSONObject(1).getString("uniqueId");		 	
		}
	}
	
	@Test (dependsOnMethods="createNewProduct", description="Copy entitlement to the same season with high version in closed season")
	public void copyWithHighGivenVersion() throws IOException{
		//target season range- 0.8-0.85, given version=5.0
		String rootId = purchasesApi.getBranchRootId(seasonID, srcBranchID, sessionToken);
		String response = f.copyItemBetweenBranches(entitlementID1, rootId, "ACT", "5.0", "suffix1", sessionToken, srcBranchID, srcBranchID);
		Assert.assertTrue(response.contains("illegalGivenMinAppVersion"), "entitlement was copied with illegal Given MinAppVersion ");
	}
	
	@Test(dependsOnMethods="copyWithHighGivenVersion", description = "Copy entitlement  with high minVersion")
	public void copyWithHighEntitlementVersion() throws IOException, JSONException{
		//target season range- 0.8-0.85, entitlement version=5.0 
		String entitlement = FileUtils.fileToString(filePath + "purchases/inAppPurchase2.txt", "UTF-8", false);
		JSONObject eJson = new JSONObject(entitlement);
		eJson.put("minAppVersion", "5.0");
		entitlementID2 = purchasesApi.addPurchaseItemToBranch(seasonID2, destBranchID, eJson.toString(), "ROOT", sessionToken);
		Assert.assertFalse(entitlementID2.contains("error"), "entitlement was not created: " + entitlementID2 );
		
		String rootId = purchasesApi.getBranchRootId(seasonID, srcBranchID, sessionToken);
		String response = f.copyItemBetweenBranches(entitlementID2, rootId, "ACT", null, "suffix2", sessionToken, destBranchID, srcBranchID);
		Assert.assertTrue(response.contains("illegalMinAppVersion"), "entitlement was copied with illegal MinAppVersion ");
	}
	
	@Test(dependsOnMethods="copyWithHighEntitlementVersion", description = "Copy entitlement withgiven minAppVersion=0.1 to the new season. ")
	public void copyWithLowGivenVersion() throws IOException, JSONException{
		//target season range- 0.85-, given version=0.05 
		String rootId = purchasesApi.getBranchRootId(seasonID2, destBranchID, sessionToken);
		String response = f.copyItemBetweenBranches(entitlementID1, rootId, "ACT", "0.05", "suffix3", sessionToken, srcBranchID, destBranchID);
		Assert.assertTrue(response.contains("newSubTreeId"), "Entitlement was not copied with legal MinAppVersion ");
	}
	
	@AfterTest
	private void reset(){
		baseUtils.reset(productID, sessionToken);
	}

}