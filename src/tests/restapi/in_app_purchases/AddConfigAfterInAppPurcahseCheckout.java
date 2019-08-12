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


public class AddConfigAfterInAppPurcahseCheckout {
	protected String productID;
	protected String seasonID;
	private String branchID;
	private String inAppPurchaseIDMaster;
	private String purchaseOptionsIDMaster;
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
	}
	
	/*
	 Add inAppPurchase to master
	 Add configuration to inAppPurchase in master
	 Checkout inAppPurchase
	 Add configuration to inAppPurchase in master and try to add the same config name to inAppPurchase in branch
	 Add configuration to inAppPurchase in branch and try to add the same config name to inAppPurchase in matser 
	*/
	

	@Test (description ="Add new branch") 
	public void addBranch () throws IOException, JSONException, InterruptedException {
		String branch = FileUtils.fileToString(filePath + "experiments/branch1.txt", "UTF-8", false);
		branchID= br.createBranch(seasonID, branch, BranchesRestApi.MASTER, sessionToken);
		Assert.assertFalse(branchID.contains("error"), "branch was not created");
	}
	
	@Test (dependsOnMethods="addBranch", description ="Add inAppPurchase and configuration to master and check them out") 
	public void addInAppPurchaseAndConfigToMaster () throws IOException, JSONException, InterruptedException {
		String inAppPurchaseStr = FileUtils.fileToString(filePath + "purchases/inAppPurchase1.txt", "UTF-8", false);
		inAppPurchaseIDMaster = purchasesApi.addPurchaseItem(seasonID, inAppPurchaseStr, "ROOT", sessionToken);
		Assert.assertFalse(inAppPurchaseIDMaster.contains("error"), "inAppPurchase was not added to master: " + inAppPurchaseIDMaster);
			
		String configuration = FileUtils.fileToString(filePath + "configuration_rule1.txt", "UTF-8", false);
		JSONObject jsonCR = new JSONObject(configuration);
		jsonCR.put("name", "CR1");
		
		String configID = purchasesApi.addPurchaseItemToBranch(seasonID, "MASTER", jsonCR.toString(), inAppPurchaseIDMaster, sessionToken);
		Assert.assertFalse(configID.contains("error"), "Configuration rule was not added to the master: " + configID);
	
		String response = br.checkoutFeature(branchID, inAppPurchaseIDMaster, sessionToken);
		Assert.assertFalse(response.contains("error"), "inAppPurchase was not checked out");
		
	}
	
	@Test(dependsOnMethods="addInAppPurchaseAndConfigToMaster", description ="Add configuration to inAppPurchase in master and try to add the same config name to inAppPurchase in branch")
	public void addConfigToMasterAndBranch() throws Exception{
		String configuration = FileUtils.fileToString(filePath + "configuration_rule1.txt", "UTF-8", false);
		JSONObject jsonCR = new JSONObject(configuration);
		jsonCR.put("name", "CR2");
		
		String configIDInMaster = purchasesApi.addPurchaseItemToBranch(seasonID, "MASTER", jsonCR.toString(), inAppPurchaseIDMaster, sessionToken);
		Assert.assertFalse(configIDInMaster.contains("error"), "Configuration rule was not added to the master: " + configIDInMaster);
	
		String configID = purchasesApi.addPurchaseItemToBranch(seasonID, branchID, jsonCR.toString(), inAppPurchaseIDMaster, sessionToken);
		Assert.assertTrue(configID.contains("error"), "Configuration rule was added to the branch even though the same config name exists in master");
	}
	
	@Test(dependsOnMethods="addConfigToMasterAndBranch", description ="Add configuration to inAppPurchase in branch and try to add the same config name to inAppPurchase in matser")
	public void addConfigToBranchAndMaster() throws Exception{
		String configuration = FileUtils.fileToString(filePath + "configuration_rule1.txt", "UTF-8", false);
		JSONObject jsonCR = new JSONObject(configuration);
		jsonCR.put("name", "CR3");
		
		String configIDInBranch = purchasesApi.addPurchaseItemToBranch(seasonID, branchID, jsonCR.toString(), inAppPurchaseIDMaster, sessionToken);		
		Assert.assertFalse(configIDInBranch.contains("error"), "Configuration rule was not added to the branch: " + configIDInBranch);
	
		String configIDInMaster = purchasesApi.addPurchaseItemToBranch(seasonID, "MASTER", jsonCR.toString(), inAppPurchaseIDMaster, sessionToken);		
		Assert.assertTrue(configIDInMaster.contains("error"), "Configuration rule was added to the master even though the same config name exists in branch");
	}	
	
	@Test (dependsOnMethods="addConfigToBranchAndMaster", description ="Add PurchaseOptions and configuration to master and check them out") 
	public void addPurchaseOptionsAndConfigToMaster () throws IOException, JSONException, InterruptedException {
		
		String inAppPurchaseStr = FileUtils.fileToString(filePath + "purchases/inAppPurchase2.txt", "UTF-8", false);
		String inAppPurchaseIDMaster2 = purchasesApi.addPurchaseItem(seasonID, inAppPurchaseStr, "ROOT", sessionToken);
		Assert.assertFalse(inAppPurchaseIDMaster2.contains("error"), "inAppPurchase was not added to master: " + inAppPurchaseIDMaster2);
		
		String purchaseOptions = FileUtils.fileToString(filePath + "purchases/purchaseOptions1.txt", "UTF-8", false);
		purchaseOptionsIDMaster = purchasesApi.addPurchaseItem(seasonID, purchaseOptions, inAppPurchaseIDMaster2, sessionToken);
		Assert.assertFalse (purchaseOptionsIDMaster.contains("error"), "Can't add purchaseOptions: " + purchaseOptionsIDMaster);
		
		String configuration = FileUtils.fileToString(filePath + "configuration_rule1.txt", "UTF-8", false);
		JSONObject jsonCR = new JSONObject(configuration);
		jsonCR.put("name", "CR4");
		
		String configID = purchasesApi.addPurchaseItemToBranch(seasonID, "MASTER", jsonCR.toString(), purchaseOptionsIDMaster, sessionToken);
		Assert.assertFalse(configID.contains("error"), "Configuration rule was not added to the master: " + configID);
	
		String response = br.checkoutFeature(branchID, purchaseOptionsIDMaster, sessionToken);
		Assert.assertFalse(response.contains("error"), "purchaseOptions was not checked out: " + response);
	}
	
	@Test(dependsOnMethods="addPurchaseOptionsAndConfigToMaster", description ="Add configuration to purchaseOptions in master and try to add the same config name to purchaseOptions in branch")
	public void addConfigToMasterAndBranch2() throws Exception{
		String configuration = FileUtils.fileToString(filePath + "configuration_rule1.txt", "UTF-8", false);
		JSONObject jsonCR = new JSONObject(configuration);
		jsonCR.put("name", "CR5");
		
		String configIDInMaster = purchasesApi.addPurchaseItemToBranch(seasonID, "MASTER", jsonCR.toString(), purchaseOptionsIDMaster, sessionToken);
		Assert.assertFalse(configIDInMaster.contains("error"), "Configuration rule was not added to the master: " + configIDInMaster);
	
		String configID = purchasesApi.addPurchaseItemToBranch(seasonID, branchID, jsonCR.toString(), purchaseOptionsIDMaster, sessionToken);
		Assert.assertTrue(configID.contains("error"), "Configuration rule was added to the branch even though the same config name exists in master");
	}
	
	@Test(dependsOnMethods="addConfigToMasterAndBranch2", description ="Add configuration to inAppPurchase in branch and try to add the same config name to inAppPurchase in matser")
	public void addConfigToBranchAndMaster2() throws Exception{
		String configuration = FileUtils.fileToString(filePath + "configuration_rule1.txt", "UTF-8", false);
		JSONObject jsonCR = new JSONObject(configuration);
		jsonCR.put("name", "CR6");
		
		String configIDInBranch = purchasesApi.addPurchaseItemToBranch(seasonID, branchID, jsonCR.toString(), purchaseOptionsIDMaster, sessionToken);		
		Assert.assertFalse(configIDInBranch.contains("error"), "Configuration rule was not added to the branch: " + configIDInBranch);
	
		String configIDInMaster = purchasesApi.addPurchaseItemToBranch(seasonID, "MASTER", jsonCR.toString(), purchaseOptionsIDMaster, sessionToken);		
		Assert.assertTrue(configIDInMaster.contains("error"), "Configuration rule was added to the master even though the same config name exists in branch");
	}	
	
	@AfterTest
	private void reset(){
		baseUtils.reset(productID, sessionToken);
	}
}
