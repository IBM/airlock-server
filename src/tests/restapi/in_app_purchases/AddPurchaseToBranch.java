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


public class AddPurchaseToBranch {
	protected String productID;
	protected String seasonID;
	private String branchID;
	private String branchID2;
	private String inAppPurchaseIDMaster;
	private String inAppPurchaseIDBranch;
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
	
	/*
	Add new inAppPurchase to branch
	Add same inAppPurchase to branch twice
	Add inAppPurchase to master and checkout in branch
	Add inAppPurchase to branch with name like in master
	Add all purchases types
	- checkout from master and add new inAppPurchase under it to branch
	-add new inAppPurchase to branch with master inAppPurchase as parent
	 */

	@Test (description ="Add new branch") 
	public void addBranch () throws IOException, JSONException, InterruptedException {
		String branch = FileUtils.fileToString(filePath + "experiments/branch1.txt", "UTF-8", false);
		branchID= br.createBranch(seasonID, branch, BranchesRestApi.MASTER, sessionToken);
		
		branch = FileUtils.fileToString(filePath + "experiments/branch2.txt", "UTF-8", false);
		branchID2= br.createBranch(seasonID, branch, BranchesRestApi.MASTER, sessionToken);
	}
	
	@Test (dependsOnMethods="addBranch", description ="Add inAppPurchase to master") 
	public void addInAppPurcahseToMaster () throws IOException, JSONException, InterruptedException {
		String inAppPurchaseStr = FileUtils.fileToString(filePath + "purchases/inAppPurchase1.txt", "UTF-8", false);
		inAppPurchaseIDMaster = purchasesApi.addPurchaseItem(seasonID, inAppPurchaseStr, "ROOT", sessionToken);
		Assert.assertFalse(inAppPurchaseIDMaster.contains("error"), "inAppPurchase was not added to master: " + inAppPurchaseIDMaster);
	}
	
	@Test(dependsOnMethods="addInAppPurcahseToMaster", description ="Add new inAppPurchase to branch")
	public void addInAppPurchaseToBranch() throws Exception{
		String inAppPurchaseStr = FileUtils.fileToString(filePath + "purchases/inAppPurchase2.txt", "UTF-8", false);
		inAppPurchaseIDBranch = purchasesApi.addPurchaseItemToBranch(seasonID, branchID, inAppPurchaseStr, "ROOT", sessionToken);
		Assert.assertFalse(inAppPurchaseIDBranch.contains("error"), "inAppPurchase was not added to the branch: " + inAppPurchaseIDBranch);
		
		String response = br.getBranchWithFeatures(branchID, sessionToken);
		JSONObject brJson = new JSONObject(response);
		JSONArray purchases = brJson.getJSONArray("entitlements");
		Assert.assertTrue(purchases.size()==1, "Incorrect number of purchases in the first branch: " + purchases.size());
		Assert.assertTrue(purchases.getJSONObject(0).getString("branchStatus").equals("NEW"), "inAppPurchases status is not NEW" );
		
		//get inAppPurchase from branch
		String inAppPurchaseFromBranch = purchasesApi.getPurchaseItemFromBranch(inAppPurchaseIDBranch, branchID, sessionToken);
		JSONObject json = new JSONObject(inAppPurchaseFromBranch);
		Assert.assertTrue(json.getString("branchStatus").equals("NEW"), "inAppPurchase status is not NEW");
		
		//check that inAppPurchase was not added to master
		JSONArray inAppPurchasesInMaster = purchasesApi.getPurchasesBySeason(seasonID, sessionToken);
		Assert.assertTrue(inAppPurchasesInMaster.size()==1, "Incorrect number of purchases in master: " + inAppPurchasesInMaster.size());
		
		//check that inAppPurchase was not added to second branch
		response = br.getBranchWithFeatures(branchID2, sessionToken);
		brJson = new JSONObject(response);
		purchases = brJson.getJSONArray("entitlements");
		Assert.assertTrue(purchases.size()==0, "Incorrect number of purchases in the second branch: " + purchases.size());
	}
	
	@Test(dependsOnMethods="addInAppPurchaseToBranch", description ="Add inAppPurchases to branch twice")
	public void addInAppPurchasesToBranchTwice() throws Exception{
		String inAppPurchaseStr = FileUtils.fileToString(filePath + "purchases/inAppPurchase2.txt", "UTF-8", false);
		String response  = purchasesApi.addPurchaseItemToBranch(seasonID, branchID, inAppPurchaseStr, "ROOT", sessionToken);
		Assert.assertTrue(response.contains("error"), "inAppPurchase was not added to the branch twice ");			
	}
	
	@Test(dependsOnMethods="addInAppPurchasesToBranchTwice", description ="Checkout inAppPurchases from master")
	public void checkoutInAppPurchaseFromMaster() throws Exception{
		br.checkoutFeature(branchID, inAppPurchaseIDMaster, sessionToken);
		String response = br.getBranchWithFeatures(branchID, sessionToken);
		JSONObject brJson = new JSONObject(response);
		JSONArray inAppPurchases = brJson.getJSONArray("entitlements");
		Assert.assertTrue(inAppPurchases.size()==2, "Incorrect number of inAppPurchases in the second branch: " + inAppPurchases.size());
		
		response = br.cancelCheckoutFeature(branchID, inAppPurchaseIDMaster, sessionToken);
		Assert.assertFalse(response.contains("error"), "cannot checkout");
	}
	
	@Test(dependsOnMethods="checkoutInAppPurchaseFromMaster", description ="Checkout inAppPurchase from branch")
	public void checkoutInAppPurchaseFromBranch() throws Exception{
		String response = br.checkoutFeature(branchID, inAppPurchaseIDBranch, sessionToken);
		Assert.assertTrue(response.contains("not found"), "New inAppPurchase in branch was checked out");
	}
	
	@Test(dependsOnMethods="checkoutInAppPurchaseFromBranch", description ="Add configuration mix")
	public void addConfigurationMixToBranch() throws Exception{
		String configurationMix = FileUtils.fileToString(filePath + "configuration_feature-mutual.txt", "UTF-8", false);
		String mixConfigID = purchasesApi.addPurchaseItemToBranch(seasonID, branchID, configurationMix, inAppPurchaseIDBranch, sessionToken);
		Assert.assertFalse(mixConfigID.contains("error"), "Configuration mix was not added to the branch");
	}
	
	@Test(dependsOnMethods="addConfigurationMixToBranch", description ="Add configuration rule")
	public void addConfigurationToBranch() throws Exception{
		String configurationMix = FileUtils.fileToString(filePath + "configuration_rule1.txt", "UTF-8", false);
		String configID = purchasesApi.addPurchaseItemToBranch(seasonID, branchID, configurationMix, inAppPurchaseIDBranch, sessionToken);
		Assert.assertFalse(configID.contains("error"), "Configuration rule was not added to the branchL: " + configID);
	
		int codeResponse = purchasesApi.deletePurchaseItemFromBranch(inAppPurchaseIDBranch, branchID, sessionToken);
		Assert.assertTrue(codeResponse==200, "inAppPurchase was not deleted");
	}
	

	@Test(dependsOnMethods="addConfigurationToBranch", description ="Add and delete configuration rule")
	public void deleteConfigurationFromBranch() throws Exception{
		String inAppPurchases2 = FileUtils.fileToString(filePath + "purchases/inAppPurchase2.txt", "UTF-8", false);
		inAppPurchaseIDBranch = purchasesApi.addPurchaseItemToBranch(seasonID, branchID, inAppPurchases2, "ROOT", sessionToken);
		Assert.assertFalse(inAppPurchaseIDBranch.contains("error"), "inAppPurchase was not added to the branch: " + inAppPurchaseIDBranch);
		
		String configuration = FileUtils.fileToString(filePath + "configuration_rule1.txt", "UTF-8", false);
		String configID = purchasesApi.addPurchaseItemToBranch(seasonID, branchID, configuration, inAppPurchaseIDBranch, sessionToken);
		Assert.assertFalse(configID.contains("error"), "Configuration rule was not added to the branchL: " + configID);
	
		int codeResponse = purchasesApi.deletePurchaseItemFromBranch(configID, branchID, sessionToken);
		Assert.assertTrue(codeResponse==200, "Configuration rule was not deleted");
		
		String inAppPurchaseFromBranch = purchasesApi.getPurchaseItemFromBranch(inAppPurchaseIDBranch, branchID, sessionToken);
		Assert.assertFalse(inAppPurchaseFromBranch.contains("error"), "inAppPurchase not found?");

		codeResponse = purchasesApi.deletePurchaseItemFromBranch(inAppPurchaseIDBranch, branchID, sessionToken);
		Assert.assertTrue(codeResponse==200, "inAppPurchase was not deleted");
		
	}
	
	@Test(dependsOnMethods="deleteConfigurationFromBranch", description ="Add mix of inAppPurchases")
	public void addInAppPurcahseMixToBranch() throws Exception{
		String ipMtx = FileUtils.fileToString(filePath + "purchases/inAppPurchaseMutual.txt", "UTF-8", false);
		String mixID = purchasesApi.addPurchaseItemToBranch(seasonID, branchID, ipMtx, "ROOT", sessionToken);
		Assert.assertFalse(mixID.contains("error"), "Mix of inAppPurcahses was not added to the branch: " + mixID);	
		
		int codeResponse = purchasesApi.deletePurchaseItemFromBranch(mixID, branchID, sessionToken);
		Assert.assertTrue(codeResponse==200, "mix was not deleted");
	}
	
	@Test (dependsOnMethods="addInAppPurcahseMixToBranch", description ="Add inAppPurcahse to branch with name like in master") 
	public void addInAppPurcahseToBranchNameAsInMaster () throws IOException, JSONException, InterruptedException {
		String inAppPurchaseStr = FileUtils.fileToString(filePath + "purchases/inAppPurchase1.txt", "UTF-8", false);
		String response = purchasesApi.addPurchaseItemToBranch(seasonID, branchID, inAppPurchaseStr, "ROOT", sessionToken);
		Assert.assertTrue(response.contains("error"), "inAppPurchase was added to branch with name like in master: " + response);
			
	}
	
	@Test (dependsOnMethods="addInAppPurcahseMixToBranch", description ="add new inAppPurchase to branch with master inAppPurchase as parent") 
	public void addInAppPurchaseToBranchParentInMaster () throws Exception {
		String inAppPurchaseStr = FileUtils.fileToString(filePath + "purchases/inAppPurchase3.txt", "UTF-8", false);
		String id = purchasesApi.addPurchaseItemToBranch(seasonID, branchID, inAppPurchaseStr, inAppPurchaseIDMaster, sessionToken);
		Assert.assertTrue(id.contains("error"), "InAppPurcahse was not added to branch with parent in master: " + id);
	}
	
	@Test (dependsOnMethods="addInAppPurchaseToBranchParentInMaster", description ="checkout from master and add new inAppPurchase under it to branch") 
	public void addInAppPurchaseToBranchCheckoutParent () throws Exception {
		br.checkoutFeature(branchID, inAppPurchaseIDMaster, sessionToken);
		
		JSONObject parentPurchase = new JSONObject(purchasesApi.getPurchaseItem(inAppPurchaseIDMaster, sessionToken));
		
		String inAppPurchaseStr = FileUtils.fileToString(filePath + "purchases/inAppPurchase1.txt", "UTF-8", false);
		JSONObject json = new JSONObject(inAppPurchaseStr);
		json.put("name", "inAppPurchase4");
		String id = purchasesApi.addPurchaseItemToBranch(seasonID, branchID, json.toString(), inAppPurchaseIDMaster, sessionToken);
		Assert.assertFalse(id.contains("error"), "inAppPurchase was not added to branch with parent in master: " + id);
		
		String response = br.getBranchWithFeatures(branchID, sessionToken);
		JSONObject brJson = new JSONObject(response);
		JSONArray inAppPurchases = brJson.getJSONArray("entitlements");
		//in inAppPurchases array - parent inAppPurchase checked out from master with 2 branch new inAppPurchases
		Assert.assertTrue(inAppPurchases.size()==1, "Incorrect number of inAppPurchases in the second branch: " + inAppPurchases.size());
		Assert.assertTrue(inAppPurchases.getJSONObject(0).getJSONArray("entitlements").getJSONObject(0).getString("branchStatus").equals("NEW"), "inAppPurchase status is not NEW in branch ");
		
		String purchaseFromBranch = purchasesApi.getPurchaseItemFromBranch(id, branchID, sessionToken);
		json = new JSONObject(purchaseFromBranch);
		Assert.assertTrue(json.getString("branchStatus").equals("NEW"), "inAppPurchase status is not NEW");
	}
	
	//add configuration to master, add inAppPurchase to branch with this configuration as a parent
	@Test (dependsOnMethods="addInAppPurchaseToBranchCheckoutParent", description ="add configuration to master, add inAppPurchase to branch with this configuration as a parent ") 
	public void addInAppPurchaseToConfiguation () throws Exception {
		
		String configurationMix = FileUtils.fileToString(filePath + "configuration_feature-mutual.txt", "UTF-8", false);
		String mixConfigID = purchasesApi.addPurchaseItem(seasonID, configurationMix, inAppPurchaseIDMaster, sessionToken);
		Assert.assertFalse(mixConfigID.contains("error"), "Configuration mix was not added to the season");

		String configuration1 = FileUtils.fileToString(filePath + "configuration_rule1.txt", "UTF-8", false);
		String configID = purchasesApi.addPurchaseItem(seasonID, configuration1, mixConfigID, sessionToken);
		Assert.assertFalse(configID.contains("error"), "Configuration rule1 was not added to the season");
		
		String inAppPurchaseStr3 = FileUtils.fileToString(filePath + "purchases/inAppPurchase1.txt", "UTF-8", false);
		String response = purchasesApi.addPurchaseItemToBranch(seasonID, branchID, inAppPurchaseStr3, mixConfigID, sessionToken);
		Assert.assertTrue(response.contains("error"), "inAppPurchase was added to the branch under mix of configuration rules: " + response);

		response =purchasesApi.addPurchaseItemToBranch(seasonID, branchID, inAppPurchaseStr3, configID, sessionToken);
		Assert.assertTrue(response.contains("error"), "inAppPurchase was added to the branch under configuration rule: " + response);

	}
	
	@AfterTest
	private void reset(){
		baseUtils.reset(productID, sessionToken);
	}
}
