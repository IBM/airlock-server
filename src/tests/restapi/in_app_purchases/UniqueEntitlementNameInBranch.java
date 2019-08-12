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
import tests.restapi.ExperimentsRestApi;
import tests.restapi.InAppPurchasesRestApi;
import tests.restapi.SeasonsRestApi;

public class UniqueEntitlementNameInBranch {
	private String productID;
	private String seasonID;
	private String branchID;
	private String filePath;
	private SeasonsRestApi s;
	private String m_url;
	private String sessionToken = "";
	private AirlockUtils baseUtils;
	private ExperimentsRestApi exp ;
	private BranchesRestApi br ;
	private InAppPurchasesRestApi purchasesApi;
	
	@BeforeClass
	@Parameters({"url", "analyticsUrl", "translationsUrl", "configPath", "sessionToken", "userName", "userPassword", "appName", "productsToDeleteFile"})
	public void init(String url, String analyticsUrl, String translationsUrl, String configPath, String sToken, String userName, String userPassword, String appName, String productsToDeleteFile) throws Exception{
		m_url = url;
		filePath = configPath ;
		s = new SeasonsRestApi();
		s.setURL(m_url);
		exp = new ExperimentsRestApi();
		exp.setURL(analyticsUrl); 
		br = new BranchesRestApi();
		br.setURL(m_url);
		purchasesApi = new InAppPurchasesRestApi();
		purchasesApi.setURL(m_url);
		baseUtils = new AirlockUtils(m_url, analyticsUrl, translationsUrl, configPath, sToken, userName, userPassword, appName, productsToDeleteFile);
		sessionToken = baseUtils.sessionToken;
		productID = baseUtils.createProduct();
		baseUtils.printProductToFile(productID);
		seasonID = baseUtils.createSeason(productID);
	}


	@Test (description ="Add components") 
	public void addComponents () throws IOException, JSONException {
		branchID = addBranch("branch1");
		Assert.assertFalse(branchID.contains("error"), "Experiment was not created: " + branchID);
		
		String entitlement1 = FileUtils.fileToString(filePath + "purchases/inAppPurchase1.txt", "UTF-8", false);
		String entitlementID1 = purchasesApi.addPurchaseItemToBranch(seasonID, branchID, entitlement1, "ROOT", sessionToken);
		Assert.assertFalse(entitlementID1.contains("error"), "entitlement1 was not added to branch: " + entitlementID1);
		
		String response = purchasesApi.addPurchaseItem(seasonID, entitlement1, "ROOT", sessionToken);
		Assert.assertTrue(response.contains("error"), "entitlement1 was added to master ");
		
		String entitlement2 = FileUtils.fileToString(filePath + "purchases/inAppPurchase2.txt", "UTF-8", false);
		String entitlementID2 = purchasesApi.addPurchaseItem(seasonID, entitlement2, "ROOT", sessionToken);
		Assert.assertFalse(entitlementID2.contains("error"), "entitlement2 was not added to master: " + entitlementID2);
		
		response = purchasesApi.addPurchaseItemToBranch(seasonID, branchID, entitlement2, "ROOT", sessionToken);
		Assert.assertTrue(response.contains("error"), "entitlement2 was added twice");
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
