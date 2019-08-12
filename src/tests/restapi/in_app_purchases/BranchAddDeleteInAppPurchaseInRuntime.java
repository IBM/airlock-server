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


public class BranchAddDeleteInAppPurchaseInRuntime {
	protected String productID;
	protected String seasonID;
	private String branchID;
	private String inAppPurchaseID1;
	private String inAppPurchaseID2;
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
Branches in runtime:
		Add new dev inAppPurchase to branch
		Add new prod inAppPurchase to branch
		Move to prod
		Move to dev
		Delete new dev inAppPurchase from branch
*/

	@Test (description ="Add components") 
	public void addComponents () throws IOException, JSONException, InterruptedException {
		branchID = addBranch("branch1");
		Assert.assertFalse(branchID.contains("error"), "Branch1 was not created: " + branchID);	
	}
	
	
	@Test(dependsOnMethods="addComponents", description="add development inAppPurchase to branch")
	public void addDevInAppPurchase() throws InterruptedException, IOException, JSONException{
		String dateFormat = purchasesApi.setDateFormat();
		
		String inAppPurchaseStr = FileUtils.fileToString(filePath + "purchases/inAppPurchase1.txt", "UTF-8", false);
		inAppPurchaseID1 = purchasesApi.addPurchaseItemToBranch(seasonID, branchID, inAppPurchaseStr, "ROOT", sessionToken);
		Assert.assertFalse(inAppPurchaseID1.contains("error"), "inAppPurchase was not added to the season");

		purchasesApi.setSleep();
		RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getDevelopmentFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);		
		Assert.assertTrue(responseDev.code==304, "Runtime development file was changed");
		RuntimeRestApi.DateModificationResults responseProd = RuntimeDateUtilities.getProductionFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseProd.code==304, "Runtime production file was changed");
		
		RuntimeRestApi.DateModificationResults branchesRuntimeDev = RuntimeDateUtilities.getDevelopmentBranchFileDateModification(m_url, productID, seasonID, branchID, dateFormat, sessionToken);
		Assert.assertTrue(branchesRuntimeDev.code==200, "Branch runtime development file was not changed");
		Assert.assertTrue(getBranchPurchases(branchesRuntimeDev.message).size()==1, "Incorrect number of inAppPurchases in branches1 development runtime file");
		RuntimeRestApi.DateModificationResults branchesRuntimeProd = RuntimeDateUtilities.getProductionBranchFileDateModification(m_url, productID, seasonID, branchID, dateFormat, sessionToken);
		Assert.assertTrue(branchesRuntimeProd.code==304, "Branch runtime production file was changed");
		
		Assert.assertTrue(getPurchaseStatus(branchesRuntimeDev.message, inAppPurchaseID1).equals("NEW"), "Incorrect inAppPurchase status in runtime development branch file");

	}
	
	@Test(dependsOnMethods="addDevInAppPurchase", description="add production inAppPurchase to branch")
	public void addProdInAppPurchase() throws InterruptedException, IOException, JSONException{
		String dateFormat = purchasesApi.setDateFormat();
		
		String inAppPurchaseStr = FileUtils.fileToString(filePath + "purchases/inAppPurchase2.txt", "UTF-8", false);
		JSONObject json = new JSONObject(inAppPurchaseStr);
		json.put("stage", "PRODUCTION");
		inAppPurchaseID2 = purchasesApi.addPurchaseItemToBranch(seasonID, branchID, json.toString(), "ROOT", sessionToken);
		Assert.assertFalse(inAppPurchaseID2.contains("error"), "inAppPurchase was not added to the season");

		purchasesApi.setSleep();
		RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getDevelopmentFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);		
		Assert.assertTrue(responseDev.code==304, "Runtime development file was changed");
		RuntimeRestApi.DateModificationResults responseProd = RuntimeDateUtilities.getProductionFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseProd.code==304, "Runtime production file was changed");
		
		RuntimeRestApi.DateModificationResults branchesRuntimeDev = RuntimeDateUtilities.getDevelopmentBranchFileDateModification(m_url, productID, seasonID, branchID, dateFormat, sessionToken);
		Assert.assertTrue(branchesRuntimeDev.code==200, "Branch runtime development file was not changed");
		Assert.assertTrue(getBranchPurchases(branchesRuntimeDev.message).size()==2, "Incorrect number of inAppPurchases in branches1 development runtime file");
		RuntimeRestApi.DateModificationResults branchesRuntimeProd = RuntimeDateUtilities.getProductionBranchFileDateModification(m_url, productID, seasonID, branchID, dateFormat, sessionToken);
		Assert.assertTrue(branchesRuntimeProd.code==200, "Branch runtime production file was changed");
		
		Assert.assertTrue(getPurchaseStatus(branchesRuntimeProd.message, inAppPurchaseID2).equals("NEW"), "Incorrect inAppPurchase status in runtime production branch file");

	}
	
	@Test(dependsOnMethods="addProdInAppPurchase", description="Move production inAppPurchase to development")
	public void moveProdInAppPurchase() throws InterruptedException, IOException, JSONException{
		String dateFormat = purchasesApi.setDateFormat();
		
		String inAppPurchase2 = purchasesApi.getPurchaseItemFromBranch(inAppPurchaseID2, branchID, sessionToken);
		JSONObject json = new JSONObject(inAppPurchase2);
		json.put("stage", "DEVELOPMENT");
		String response = purchasesApi.updatePurchaseItemInBranch(seasonID, branchID, inAppPurchaseID2, json.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Production inAppPurchase was not moved to development");
		
		purchasesApi.setSleep();
		RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getDevelopmentFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);		
		Assert.assertTrue(responseDev.code==304, "Runtime development file was changed");
		RuntimeRestApi.DateModificationResults responseProd = RuntimeDateUtilities.getProductionFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseProd.code==304, "Runtime production file was changed");
		
		RuntimeRestApi.DateModificationResults branchesRuntimeDev = RuntimeDateUtilities.getDevelopmentBranchFileDateModification(m_url, productID, seasonID, branchID, dateFormat, sessionToken);
		Assert.assertTrue(branchesRuntimeDev.code==200, "Branch runtime development file was not changed");
		Assert.assertTrue(getBranchPurchases(branchesRuntimeDev.message).size()==2, "Incorrect number of inAppPurchases in branches1 development runtime file");
		RuntimeRestApi.DateModificationResults branchesRuntimeProd = RuntimeDateUtilities.getProductionBranchFileDateModification(m_url, productID, seasonID, branchID, dateFormat, sessionToken);
		Assert.assertTrue(branchesRuntimeProd.code==200, "Branch runtime production file was changed");
		Assert.assertTrue(getBranchPurchases(branchesRuntimeProd.message).size()==0, "Incorrect number of inAppPurchases in runtime production branch file");
	}
	
	@Test(dependsOnMethods="moveProdInAppPurchase", description="Delete development inAppPurchase from branch")
	public void deleteDevInAppPurchase() throws InterruptedException, IOException, JSONException{
		String dateFormat = purchasesApi.setDateFormat();
		
		int code = purchasesApi.deletePurchaseItemFromBranch(inAppPurchaseID2, branchID, sessionToken);
		Assert.assertTrue(code == 200, "Development inAppPurchase was not deleted from branch");
		
		purchasesApi.setSleep();
		RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getDevelopmentFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);		
		Assert.assertTrue(responseDev.code==304, "Runtime development file was changed");
		RuntimeRestApi.DateModificationResults responseProd = RuntimeDateUtilities.getProductionFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseProd.code==304, "Runtime production file was changed");
		
		RuntimeRestApi.DateModificationResults branchesRuntimeDev = RuntimeDateUtilities.getDevelopmentBranchFileDateModification(m_url, productID, seasonID, branchID, dateFormat, sessionToken);
		Assert.assertTrue(branchesRuntimeDev.code==200, "Branch runtime development file was not changed");
		Assert.assertTrue(getBranchPurchases(branchesRuntimeDev.message).size()==1, "Incorrect number of inAppPurchases in branches1 development runtime file");
		RuntimeRestApi.DateModificationResults branchesRuntimeProd = RuntimeDateUtilities.getProductionBranchFileDateModification(m_url, productID, seasonID, branchID, dateFormat, sessionToken);
		Assert.assertTrue(branchesRuntimeProd.code==304, "Branch runtime production file was changed");
	}

	@Test(dependsOnMethods="deleteDevInAppPurchase", description="Move development inAppPurchase to production")
	public void moveDevInAppPurchaseToProd() throws InterruptedException, IOException, JSONException{
		String dateFormat = purchasesApi.setDateFormat();
		
		String inAppPurchase2 = purchasesApi.getPurchaseItemFromBranch(inAppPurchaseID1, branchID, sessionToken);
		JSONObject json = new JSONObject(inAppPurchase2);
		json.put("stage", "PRODUCTION");
		String response = purchasesApi.updatePurchaseItemInBranch(seasonID, branchID, inAppPurchaseID1, json.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Production inAppPurchase was not moved to development");
		
		purchasesApi.setSleep();
		RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getDevelopmentFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);		
		Assert.assertTrue(responseDev.code==304, "Runtime development file was changed");
		RuntimeRestApi.DateModificationResults responseProd = RuntimeDateUtilities.getProductionFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseProd.code==304, "Runtime production file was changed");
		
		RuntimeRestApi.DateModificationResults branchesRuntimeDev = RuntimeDateUtilities.getDevelopmentBranchFileDateModification(m_url, productID, seasonID, branchID, dateFormat, sessionToken);
		Assert.assertTrue(branchesRuntimeDev.code==200, "Branch runtime development file was not changed");
		Assert.assertTrue(getBranchPurchases(branchesRuntimeDev.message).size()==1, "Incorrect number of inAppPurchases in branches1 development runtime file");
		RuntimeRestApi.DateModificationResults branchesRuntimeProd = RuntimeDateUtilities.getProductionBranchFileDateModification(m_url, productID, seasonID, branchID, dateFormat, sessionToken);
		Assert.assertTrue(branchesRuntimeProd.code==200, "Branch runtime production file was changed");
		
		Assert.assertTrue(getPurchaseStatus(branchesRuntimeProd.message, inAppPurchaseID1).equals("NEW"), "Incorrect inAppPurchase status in runtime production branch file");
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
