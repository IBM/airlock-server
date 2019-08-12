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
import tests.restapi.RuntimeDateUtilities;
import tests.restapi.RuntimeRestApi;

public class BranchAddDeleteMasterInAppPurchaseInRuntime {
	protected String productID;
	protected String seasonID;
	private String branchID;
	private String inAppPurchaseID;
	private String configID;
	protected String filePath;
	protected String m_url;
	private String sessionToken = "";
	private AirlockUtils baseUtils;
	private BranchesRestApi br ;
	private InAppPurchasesRestApi purchasesApi;
	
	
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
	}
	
	/*
Branches in runtime:

	Delete dev inAppPurchase from master - it remains in branch and gets a new status
	Delete dev configuration rule from master - it remains in branch and gets a new status
*/

	@Test (description ="Add components") 
	public void addComponents () throws IOException, JSONException, InterruptedException {
		branchID = addBranch("branch1");
		Assert.assertFalse(branchID.contains("error"), "Branch1 was not created: " + branchID);
		
		String inAppPurchaseStr = FileUtils.fileToString(filePath + "purchases/inAppPurchase1.txt", "UTF-8", false);
		inAppPurchaseID = purchasesApi.addPurchaseItem(seasonID, inAppPurchaseStr, "ROOT", sessionToken);
		Assert.assertFalse(inAppPurchaseID.contains("error"), "inAppPurchase was not added to the season");
				
		String configuration = FileUtils.fileToString(filePath + "configuration_rule1.txt", "UTF-8", false);
		configID = purchasesApi.addPurchaseItem(seasonID, configuration, inAppPurchaseID, sessionToken);
		Assert.assertFalse(configID.contains("error"), "Configuration was not added to the season");
	}

	@Test (dependsOnMethods="addComponents", description ="Checkout inAppPurchase with configuration in branch") 
	public void checkoutDevInAppPurchase () throws Exception {
		String dateFormat = purchasesApi.setDateFormat();
		
		String response = br.checkoutFeature(branchID, inAppPurchaseID, sessionToken);
		Assert.assertFalse(response.contains("error"), "inAppPurchase was not checked out to branch");
		
		purchasesApi.setSleep();
		
		JSONObject configRule = new JSONObject(purchasesApi.getPurchaseItem(configID, sessionToken));
		
		RuntimeRestApi.DateModificationResults branchesRuntimeDev = RuntimeDateUtilities.getDevelopmentBranchFileDateModification(m_url, productID, seasonID, branchID, dateFormat, sessionToken);
		Assert.assertTrue(branchesRuntimeDev.code==200, "Branch runtime development file was not changed");
		Assert.assertTrue(getBranchPurchases(branchesRuntimeDev.message).size()==1, "Incorrect number of checked out inAppPurchases in branches1 development runtime file");
		Assert.assertTrue(getBranchPurchases(branchesRuntimeDev.message).getJSONObject(0).getJSONArray("branchConfigurationRuleItems").get(0).equals(configRule.getString("namespace") + "." + configRule.getString("name")), "Incorrect configuraton name listed in branches development runtime file");
		
		RuntimeRestApi.DateModificationResults branchesRuntimeProd = RuntimeDateUtilities.getProductionBranchFileDateModification(m_url, productID, seasonID, branchID, dateFormat, sessionToken);
		Assert.assertTrue(branchesRuntimeProd.code==304, "Branch runtime production file was changed");
		
		Assert.assertTrue(getPurchaseStatus(branchesRuntimeDev.message, inAppPurchaseID).equals("CHECKED_OUT"), "Incorrect inAppPurchase status in runtime development branch file");
	}	
	
	@Test(dependsOnMethods="checkoutDevInAppPurchase", description="Delete configuration rule from master")
	public void deleteMasterConfiguration() throws InterruptedException, IOException, JSONException{
		String dateFormat = purchasesApi.setDateFormat();
		
		int code = purchasesApi.deletePurchaseItem(configID, sessionToken);
		Assert.assertTrue(code == 200, "Configuraiton rule was not deleted from master");
		
		purchasesApi.setSleep();
		RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getDevelopmentFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);		
		Assert.assertTrue(responseDev.code==200, "Runtime development file was not changed");
		RuntimeRestApi.DateModificationResults responseProd = RuntimeDateUtilities.getProductionFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseProd.code==304, "Runtime production file was changed");
		
		RuntimeRestApi.DateModificationResults branchesRuntimeDev = RuntimeDateUtilities.getDevelopmentBranchFileDateModification(m_url, productID, seasonID, branchID, dateFormat, sessionToken);
		Assert.assertTrue(branchesRuntimeDev.code==200, "Branch runtime development file was not changed");
		//configuration status changes to NEW
		Assert.assertTrue(getBranchPurchases(branchesRuntimeDev.message).getJSONObject(0).getJSONArray("configurationRules").getJSONObject(0).getString("branchStatus").equals("NEW"), "Deleted from master configuration rule status in branch is not NEW in runtime" );
		//new uniqueId is assigned
		Assert.assertFalse(getBranchPurchases(branchesRuntimeDev.message).getJSONObject(0).getJSONArray("configurationRules").getJSONObject(0).getString("uniqueId").equals(configID), "Deleted from master configuration rule uniqueId was not changed in runtime" );

		
		RuntimeRestApi.DateModificationResults branchesRuntimeProd = RuntimeDateUtilities.getProductionBranchFileDateModification(m_url, productID, seasonID, branchID, dateFormat, sessionToken);
		Assert.assertTrue(branchesRuntimeProd.code==304, "Branch runtime production file was changed");

	}

	@Test(dependsOnMethods="deleteMasterConfiguration", description="Delete inAppPurchase from master")
	public void deleteMasterInAppPurchase() throws InterruptedException, IOException, JSONException{
		String dateFormat = purchasesApi.setDateFormat();
		
		int code = purchasesApi.deletePurchaseItem(inAppPurchaseID,sessionToken);
		Assert.assertTrue(code == 200, "inAppPurchase was not deleted from master");
		
		purchasesApi.setSleep();
		RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getDevelopmentFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);		
		Assert.assertTrue(responseDev.code==200, "Runtime development file was not changed");
		RuntimeRestApi.DateModificationResults responseProd = RuntimeDateUtilities.getProductionFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseProd.code==304, "Runtime production file was changed");
		
		RuntimeRestApi.DateModificationResults branchesRuntimeDev = RuntimeDateUtilities.getDevelopmentBranchFileDateModification(m_url, productID, seasonID, branchID, dateFormat, sessionToken);
		Assert.assertTrue(branchesRuntimeDev.code==200, "Branch runtime development file was not changed");
		//configuration status changes to NEW
		Assert.assertTrue(getBranchPurchases(branchesRuntimeDev.message).getJSONObject(0).getString("branchStatus").equals("NEW"), "Deleted from master inAppPurchase in branch is not NEW in runtime" );
		//new uniqueId is assigned
		Assert.assertFalse(getBranchPurchases(branchesRuntimeDev.message).getJSONObject(0).getString("uniqueId").equals(configID), "Deleted from master inAppPurchase uniqueId was not changed in runtime" );

		
		RuntimeRestApi.DateModificationResults branchesRuntimeProd = RuntimeDateUtilities.getProductionBranchFileDateModification(m_url, productID, seasonID, branchID, dateFormat, sessionToken);
		Assert.assertTrue(branchesRuntimeProd.code==304, "Branch runtime production file was changed");

	}
	
	private String addBranch(String branchName) throws JSONException, IOException{
		String branch = FileUtils.fileToString(filePath + "experiments/branch1.txt", "UTF-8", false);
		JSONObject branchJson = new JSONObject(branch);
		branchJson.put("name", branchName);
		return br.createBranch(seasonID, branchJson.toString(), BranchesRestApi.MASTER, sessionToken);

	}

	private JSONArray getBranchPurchases(String result) throws JSONException{
		JSONObject json = new JSONObject(result);
		return json.getJSONArray("entitlements");
	}
	
	private String getPurchaseStatus(String result, String id) throws JSONException{
		JSONArray inAppPurchases = new JSONObject(result).getJSONArray("entitlements");
		String status="";
		for(int i =0; i<inAppPurchases.size(); i++){
			if (inAppPurchases.getJSONObject(i).getString("uniqueId").equals(id)){
				status = inAppPurchases.getJSONObject(i).getString("branchStatus");
			}
		}
		
		return status;
	}
	
	@AfterTest
	private void reset(){
		baseUtils.reset(productID, sessionToken);
	}
}
