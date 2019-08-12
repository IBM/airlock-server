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

public class BranchCheckoutInAppPurchaseInRuntime {
	protected String productID;
	protected String seasonID;
	private String branchID;
	private String branchID2;
	private String inAppPurchaseID;
	private String configID;
	private String prodInAppPurchaseID;
	private String prodConfigID;
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
		Add 2 branches
		Update branch name
		Checkout inAppPurchase in development
		Checkout inAppPurchase in production
		Move inAppPurchase from production to development
		Uncheckout inAppPurchase in development
		Uncheckout inAppPurchase in production
	 */

	@Test (description ="Add components") 
	public void addComponents () throws IOException, JSONException, InterruptedException {
		String dateFormat = purchasesApi.setDateFormat();
		
		branchID = addBranch("branch1");
		Assert.assertFalse(branchID.contains("error"), "Branch1 was not created: " + branchID);
		
		branchID2 = addBranch("branch2");
		Assert.assertFalse(branchID2.contains("error"), "Branch2 was not created: " + branchID2);

		//check if files were changed
		purchasesApi.setSleep();
		RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getDevelopmentFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseDev.code==304, "Runtime development file was changed");
		RuntimeRestApi.DateModificationResults responseProd = RuntimeDateUtilities.getProductionFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseProd.code==304, "Runtime production file was changed");
		
		RuntimeRestApi.DateModificationResults branchesRuntimeDev = RuntimeDateUtilities.getRuntimeBranchFileContent(RuntimeDateUtilities.RUNTIME_BRANCHES_DEVELOPMENT,  m_url, productID, seasonID, branchID, sessionToken);
		Assert.assertTrue(getBranchPurchases(branchesRuntimeDev.message).size()==0, "Incorrect number of checked out inAppPurchases in dev branches1 runtime file");
		RuntimeRestApi.DateModificationResults branchesRuntimeProd = RuntimeDateUtilities.getRuntimeBranchFileContent(RuntimeDateUtilities.RUNTIME_BRANCHES_PRODUCTION,  m_url, productID, seasonID, branchID, sessionToken);
		Assert.assertTrue(getBranchPurchases(branchesRuntimeProd.message).size()==0, "Incorrect number of checked out inAppPurchases in prod branches1 runtime file");

		branchesRuntimeDev = RuntimeDateUtilities.getRuntimeBranchFileContent(RuntimeDateUtilities.RUNTIME_BRANCHES_DEVELOPMENT,  m_url, productID, seasonID, branchID2, sessionToken);
		Assert.assertTrue(getBranchPurchases(branchesRuntimeDev.message).size()==0, "Incorrect number of checked out inAppPurchases in dev branches1 runtime file");
		branchesRuntimeProd = RuntimeDateUtilities.getRuntimeBranchFileContent(RuntimeDateUtilities.RUNTIME_BRANCHES_PRODUCTION,  m_url, productID, seasonID, branchID2, sessionToken);
		Assert.assertTrue(getBranchPurchases(branchesRuntimeProd.message).size()==0, "Incorrect number of checked out inAppPurchases in prod branches1 runtime file");
	}
	
	@Test (dependsOnMethods="addComponents", description ="Update branch name ") 
	public void updateBranchName () throws Exception {
		String dateFormat = purchasesApi.setDateFormat();
		
		String branch = br.getBranch(branchID, sessionToken);
		JSONObject json = new JSONObject(branch);
		json.put("name", "branch1a");
		String response = br.updateBranch(branchID, json.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Branch1 was not updated: " + response);
				
		purchasesApi.setSleep();
		RuntimeRestApi.DateModificationResults branchesRuntimeDev = RuntimeDateUtilities.getDevelopmentBranchFileDateModification(m_url, productID, seasonID, branchID, dateFormat, sessionToken);
		Assert.assertTrue(branchesRuntimeDev.code==200, "Branched runtime development file was not changed");
		JSONObject result = new JSONObject(branchesRuntimeDev.message);
		Assert.assertTrue(result.getString("name").equals("branch1a"), "Branch name was not updated in branches1 runtime file");
		
		RuntimeRestApi.DateModificationResults branchesRuntimeProd = RuntimeDateUtilities.getProductionBranchFileDateModification(m_url, productID, seasonID, branchID, dateFormat, sessionToken);
		Assert.assertTrue(branchesRuntimeProd.code==200, "Branched runtime production file was not changed");
		result = new JSONObject(branchesRuntimeProd.message);
		Assert.assertTrue(result.getString("name").equals("branch1a"), "Branch name was not updated in branches1 runtime file");
				
	}
	
	@Test (dependsOnMethods="updateBranchName", description ="Add development inAppPurchase with configuration rules and check it out ") 
	public void checkoutDevInAppPurchase () throws Exception {
		//add inAppPurchase with configuration
		String inAppPurchaseStr = FileUtils.fileToString(filePath + "purchases/inAppPurchase1.txt", "UTF-8", false);
		inAppPurchaseID = purchasesApi.addPurchaseItem(seasonID, inAppPurchaseStr, "ROOT", sessionToken);
		Assert.assertFalse(inAppPurchaseID.contains("error"), "inAppPurchase was not added to the season");
				
		String configuration = FileUtils.fileToString(filePath + "configuration_rule1.txt", "UTF-8", false);
		configID = purchasesApi.addPurchaseItem(seasonID, configuration, inAppPurchaseID, sessionToken);
		Assert.assertFalse(configID.contains("error"), "Configuration was not added to the season");
		
		purchasesApi.setSleep();
		String dateFormat = purchasesApi.setDateFormat();
		
		String response = br.checkoutFeature(branchID, inAppPurchaseID, sessionToken);
		Assert.assertFalse(response.contains("error"), "inAppPurchase was not checked out to branch");
		
		purchasesApi.setSleep();
		RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getDevelopmentFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);		
		Assert.assertTrue(responseDev.code==304, "Runtime development file was changed");
		RuntimeRestApi.DateModificationResults responseProd = RuntimeDateUtilities.getProductionFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseProd.code==304, "Runtime production file was changed");
		
		RuntimeRestApi.DateModificationResults branchesRuntimeDev = RuntimeDateUtilities.getDevelopmentBranchFileDateModification(m_url, productID, seasonID, branchID, dateFormat, sessionToken);
		Assert.assertTrue(branchesRuntimeDev.code==200, "Branch runtime development file was not changed");
		Assert.assertTrue(getBranchPurchases(branchesRuntimeDev.message).size()==1, "Incorrect number of checked out inAppPurchases in branches1 development runtime file");
		RuntimeRestApi.DateModificationResults branchesRuntimeProd = RuntimeDateUtilities.getProductionBranchFileDateModification(m_url, productID, seasonID, branchID, dateFormat, sessionToken);
		Assert.assertTrue(branchesRuntimeProd.code==304, "Branch runtime production file was changed");
		
		Assert.assertTrue(getPurchaseStatus(branchesRuntimeDev.message, inAppPurchaseID).equals("CHECKED_OUT"), "Incorrect inAppPurchase status in runtime development branch file");
	}
	
	@Test (dependsOnMethods="checkoutDevInAppPurchase", description ="Add production inAppPurchase with configuration rules and check it out ") 
	public void checkoutProdInAppPurchase () throws Exception {
				
		//add inAppPurchase with configuration
		String inAppPurchaseStr = FileUtils.fileToString(filePath + "purchases/inAppPurchase2.txt", "UTF-8", false);
		JSONObject json = new JSONObject(inAppPurchaseStr) ;
		json.put("stage", "PRODUCTION");
		prodInAppPurchaseID = purchasesApi.addPurchaseItem(seasonID, json.toString(), "ROOT", sessionToken);
		Assert.assertFalse(prodInAppPurchaseID.contains("error"), "inAppPurchase was not added to the season");
				
		String configuration = FileUtils.fileToString(filePath + "configuration_rule2.txt", "UTF-8", false);
		JSONObject jsonCR = new JSONObject(configuration) ;
		jsonCR.put("stage", "PRODUCTION");
		prodConfigID = purchasesApi.addPurchaseItem(seasonID, jsonCR.toString(), prodInAppPurchaseID, sessionToken);
		Assert.assertFalse(prodConfigID.contains("error"), "Configuration was not added to the season");
		
		purchasesApi.setSleep();
		String dateFormat = purchasesApi.setDateFormat();
		
		String response = br.checkoutFeature(branchID, prodInAppPurchaseID, sessionToken);
		Assert.assertFalse(response.contains("error"), "inAppPurchase was not checked out to branch");
		
		purchasesApi.setSleep();
		RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getDevelopmentFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);		
		Assert.assertTrue(responseDev.code==304, "Runtime development file was changed");
		RuntimeRestApi.DateModificationResults responseProd = RuntimeDateUtilities.getProductionFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseProd.code==304, "Runtime production file was changed");
		
		RuntimeRestApi.DateModificationResults branchesRuntimeDev = RuntimeDateUtilities.getDevelopmentBranchFileDateModification(m_url, productID, seasonID, branchID, dateFormat, sessionToken);
		Assert.assertTrue(branchesRuntimeDev.code==200, "Branch runtime development file was not changed");
		Assert.assertTrue(getBranchPurchases(branchesRuntimeDev.message).size()==2, "Incorrect number of checked out inAppPurchases in branches1 development runtime file");
		RuntimeRestApi.DateModificationResults branchesRuntimeProd = RuntimeDateUtilities.getProductionBranchFileDateModification(m_url, productID, seasonID, branchID, dateFormat, sessionToken);
		Assert.assertTrue(branchesRuntimeProd.code==200, "Branch runtime production file was changed");
		Assert.assertTrue(getBranchPurchases(branchesRuntimeProd.message).size()==1, "Incorrect number of checked out inAppPurchases in branches1 production runtime file");
		
		Assert.assertTrue(getPurchaseStatus(branchesRuntimeDev.message, prodInAppPurchaseID).equals("CHECKED_OUT"), "Incorrect inAppPurchase status in runtime development branch file");
		Assert.assertTrue(getPurchaseStatus(branchesRuntimeProd.message, prodInAppPurchaseID).equals("CHECKED_OUT"), "Incorrect inAppPurchase status in runtime production branch file");
	}
	
	@Test (dependsOnMethods="checkoutProdInAppPurchase", description ="Move inAppPurchase from prod to dev in master - doesn't influence branch ") 
	public void moveProdInAppPurchaseToDev () throws Exception {
		String dateFormat = purchasesApi.setDateFormat();
		
		//update inAppPurchase with configuration
		String configuration = purchasesApi.getPurchaseItem(prodConfigID, sessionToken);
		JSONObject jsonCR = new JSONObject(configuration) ;
		jsonCR.put("stage", "DEVELOPMENT");
		prodConfigID = purchasesApi.updatePurchaseItem(seasonID, prodConfigID,  jsonCR.toString(), sessionToken);
		Assert.assertFalse(prodConfigID.contains("error"), "Configuration was not updated: " + prodConfigID);

		
		String inAppPurchase1 = purchasesApi.getPurchaseItem(prodInAppPurchaseID, sessionToken);
		JSONObject json = new JSONObject(inAppPurchase1) ;
		json.put("stage", "DEVELOPMENT");
		prodInAppPurchaseID = purchasesApi.updatePurchaseItem(seasonID, prodInAppPurchaseID, json.toString(), sessionToken);
		Assert.assertFalse(prodInAppPurchaseID.contains("error"), "inAppPurchase was not updated: "+ prodInAppPurchaseID);
				
		purchasesApi.setSleep();
		RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getDevelopmentFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);		
		Assert.assertTrue(responseDev.code==200, "Runtime development file was changed");
		RuntimeRestApi.DateModificationResults responseProd = RuntimeDateUtilities.getProductionFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseProd.code==200, "Runtime production file was changed");
		
		//branch runtime files were updated because the delta from the master was changed
		RuntimeRestApi.DateModificationResults branchesRuntimeDev = RuntimeDateUtilities.getDevelopmentBranchFileDateModification(m_url, productID, seasonID, branchID, dateFormat, sessionToken);
		Assert.assertTrue(branchesRuntimeDev.code==200, "Branch runtime development file was not changed");
		RuntimeRestApi.DateModificationResults branchesRuntimeProd = RuntimeDateUtilities.getProductionBranchFileDateModification(m_url, productID, seasonID, branchID, dateFormat, sessionToken);
		Assert.assertTrue(branchesRuntimeProd.code==200, "Branch runtime production file was changed");		
	}
	
	@Test (dependsOnMethods="moveProdInAppPurchaseToDev", description ="Uncheck dev inAppPurchase ") 
	public void uncheckDevInAppPurchase () throws Exception {
		String dateFormat = purchasesApi.setDateFormat();
		
		String response = br.cancelCheckoutFeature(branchID, inAppPurchaseID, sessionToken);
		Assert.assertFalse(response.contains("error"), "inAppPurchase was not checked out to branch");

		purchasesApi.setSleep();
		RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getDevelopmentFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);		
		Assert.assertTrue(responseDev.code==304, "Runtime development file was changed");
		RuntimeRestApi.DateModificationResults responseProd = RuntimeDateUtilities.getProductionFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseProd.code==304, "Runtime production file was changed");
		
		RuntimeRestApi.DateModificationResults branchesRuntimeDev = RuntimeDateUtilities.getDevelopmentBranchFileDateModification(m_url, productID, seasonID, branchID, dateFormat, sessionToken);
		Assert.assertTrue(branchesRuntimeDev.code==200, "Branch runtime development file was not changed");
		Assert.assertTrue(getBranchPurchases(branchesRuntimeDev.message).size()==1, "Incorrect number of checked out inAppPurchases in branches1 development runtime file");
		RuntimeRestApi.DateModificationResults branchesRuntimeProd = RuntimeDateUtilities.getProductionBranchFileDateModification(m_url, productID, seasonID, branchID, dateFormat, sessionToken);
		Assert.assertTrue(branchesRuntimeProd.code==304, "Branch runtime production file was changed");
		
	}
	
	@Test (dependsOnMethods="uncheckDevInAppPurchase", description ="Uncheck prod inAppPurchase ") 
	public void uncheckProdInAppPurchase () throws Exception {
		String dateFormat = purchasesApi.setDateFormat();
		
		String response = br.cancelCheckoutFeature(branchID, prodInAppPurchaseID, sessionToken);
		Assert.assertFalse(response.contains("error"), "inAppPurchase was not checked out to branch");

		purchasesApi.setSleep();
		RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getDevelopmentFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);		
		Assert.assertTrue(responseDev.code==304, "Runtime development file was changed");
		RuntimeRestApi.DateModificationResults responseProd = RuntimeDateUtilities.getProductionFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseProd.code==304, "Runtime production file was changed");
		
		RuntimeRestApi.DateModificationResults branchesRuntimeDev = RuntimeDateUtilities.getDevelopmentBranchFileDateModification(m_url, productID, seasonID, branchID, dateFormat, sessionToken);
		Assert.assertTrue(branchesRuntimeDev.code==200, "Branch runtime development file was not changed");
		Assert.assertTrue(getBranchPurchases(branchesRuntimeDev.message).size()==0, "Incorrect number of checked out inAppPurchases in branches1 development runtime file");
		RuntimeRestApi.DateModificationResults branchesRuntimeProd = RuntimeDateUtilities.getProductionBranchFileDateModification(m_url, productID, seasonID, branchID, dateFormat, sessionToken);
		Assert.assertTrue(branchesRuntimeProd.code==200, "Branch runtime production file was changed");
		Assert.assertTrue(getBranchPurchases(branchesRuntimeDev.message).size()==0, "Incorrect number of checked out inAppPurchases in branches1 production runtime file");		
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
