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

public class BranchUpdateInAppPurchaseFieldsInRuntime {
	protected String productID;
	protected String seasonID;
	private String branchID;
	private String inAppPurchaseID1;
	private String inAppPurchaseID2;
	private String mixID;
	private String mixConfigID;
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
		Updated checked out inAppPurchase fields: check deltas in runtime
			- updated inAppPurchase fields
			- update configuration rule fields
			- update MIX fields (maxFeaturesOn)
			- update MIXCR fields (maxFeaturesOn)
	 */

	@Test (description ="Add components") 
	public void addComponents () throws IOException, JSONException, InterruptedException {
		branchID = addBranch("branch1");
		Assert.assertFalse(branchID.contains("error"), "Branch1 was not created: " + branchID);

		String inAppPurchaseStr = FileUtils.fileToString(filePath + "purchases/inAppPurchase1.txt", "UTF-8", false);
		inAppPurchaseID1 = purchasesApi.addPurchaseItem(seasonID, inAppPurchaseStr, "ROOT", sessionToken);
		Assert.assertFalse(inAppPurchaseID1.contains("error"), "inAppPurchaseID1 was not added to the season");
		
		String iapMix = FileUtils.fileToString(filePath + "purchases/inAppPurchaseMutual.txt", "UTF-8", false);
		mixID = purchasesApi.addPurchaseItem(seasonID, iapMix, inAppPurchaseID1, sessionToken);
		Assert.assertFalse(mixID.contains("error"), "mix was not added to the season: " + mixID);

		String inAppPurchaseStr2 = FileUtils.fileToString(filePath + "purchases/inAppPurchase2.txt", "UTF-8", false);
		inAppPurchaseID2 = purchasesApi.addPurchaseItem(seasonID, inAppPurchaseStr2, mixID, sessionToken);
		Assert.assertFalse(inAppPurchaseID2.contains("error"), "inAppPurchaseID2 was not added to the season");

		String configurationMix = FileUtils.fileToString(filePath + "configuration_feature-mutual.txt", "UTF-8", false);
		mixConfigID = purchasesApi.addPurchaseItem(seasonID, configurationMix, inAppPurchaseID1, sessionToken);
		Assert.assertFalse(mixConfigID.contains("error"), "Configuration mix was not added to the season");

		String configuration = FileUtils.fileToString(filePath + "configuration_rule1.txt", "UTF-8", false);
		configID = purchasesApi.addPurchaseItem(seasonID, configuration, inAppPurchaseID1, sessionToken);
		Assert.assertFalse(configID.contains("error"), "Configuration was not added to the season");

		//checkout inAppPurchase
		String response = br.checkoutFeature(branchID, inAppPurchaseID1, sessionToken);
		Assert.assertFalse(response.contains("error"), "inAppPurchase was not checked out to branch");
	}
	
	@Test (dependsOnMethods="addComponents", description ="Checkout inAppPurchase and updated its fields in branch") 
	public void updateInAppPurchaseInBranch() throws JSONException, IOException, InterruptedException{
		String dateFormat = purchasesApi.setDateFormat();
		
		String purchase1 = purchasesApi.getPurchaseItemFromBranch(inAppPurchaseID1, branchID, sessionToken);
		JSONObject jsonF = new JSONObject(purchase1);
		jsonF.put("description", "new description");
		jsonF.put("defaultConfiguration", "{\"color\":\"red\"}");
		jsonF.put("defaultIfAirlockSystemIsDown", true);
		jsonF.put("enabled", false);

		JSONArray groups = new JSONArray();
		groups.add("QA");
		jsonF.put("internalUserGroups", groups);
		jsonF.put("minAppVersion", "1.0");
		jsonF.put("noCachedResults", true);
		jsonF.put("rolloutPercentage", 100);
		jsonF.put("stage", "PRODUCTION");
		jsonF.put("displayName", "F1");
		//JSONObject premiumRule = new JSONObject();
		//premiumRule.put("ruleString", "false;");
		//jsonF.put("premiumRule", premiumRule);

		String response = purchasesApi.updatePurchaseItemInBranch(seasonID, branchID, inAppPurchaseID1, jsonF.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "inAppPurchase was not updated");
		
		JSONArray afterUpdate = purchasesApi.getPurchasesBySeasonFromBranch(seasonID, branchID, sessionToken);
		JSONObject iapFromBranch = afterUpdate.getJSONObject(0);
		
		purchasesApi.setSleep();
		RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getDevelopmentFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);		
		Assert.assertTrue(responseDev.code==304, "Runtime development file was changed");
		RuntimeRestApi.DateModificationResults responseProd = RuntimeDateUtilities.getProductionFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseProd.code==304, "Runtime production file was changed");
		
		RuntimeRestApi.DateModificationResults branchesRuntimeDev = RuntimeDateUtilities.getDevelopmentBranchFileDateModification(m_url, productID, seasonID, branchID, dateFormat, sessionToken);
		Assert.assertTrue(branchesRuntimeDev.code==200, "Branch runtime development file was not changed");
		
		RuntimeRestApi.DateModificationResults branchesRuntimeProd = RuntimeDateUtilities.getProductionBranchFileDateModification(m_url, productID, seasonID, branchID, dateFormat, sessionToken);
		Assert.assertTrue(branchesRuntimeProd.code==200, "Branch runtime production file was changed");

		//validate field values in production file
		JSONObject inAppPurchaseProd = new JSONObject(branchesRuntimeDev.message).getJSONArray("entitlements").getJSONObject(0);
		validateFields(inAppPurchaseProd, iapFromBranch, "production");
		
		//validate field values in development file
		JSONObject inAppPurchaseDev = new JSONObject(branchesRuntimeDev.message).getJSONArray("entitlements").getJSONObject(0);
		validateFields(inAppPurchaseDev, iapFromBranch, "development");
		
		
		//check there are no changes in master inAppPurchase
		JSONObject inAppPurchaseInBranch = new JSONObject(purchasesApi.getPurchaseItemFromBranch(inAppPurchaseID1, branchID, sessionToken));
		JSONObject inAppPurchaseInMaster = new JSONObject(purchasesApi.getPurchaseItem(inAppPurchaseID1, sessionToken));

		Assert.assertTrue(!inAppPurchaseInBranch.getString("description").equals(inAppPurchaseInMaster.getString("description")), "Description was changed in master inAppPurchase");
		Assert.assertTrue(inAppPurchaseInMaster.isNull("defaultConfiguration"), "defaultConfiguration was changed in master inAppPurchase");
		Assert.assertTrue(!inAppPurchaseInBranch.getString("minAppVersion").equals(inAppPurchaseInMaster.getString("minAppVersion")), "minAppVersion was changed in master inAppPurchase");
		Assert.assertTrue(!inAppPurchaseInBranch.getString("stage").equals(inAppPurchaseInMaster.getString("stage")), "stage was changed in master inAppPurchase");
		Assert.assertTrue(inAppPurchaseInMaster.isNull("displayName"), "displayName was changed in master inAppPurchase");
		Assert.assertTrue(inAppPurchaseInBranch.getBoolean("defaultIfAirlockSystemIsDown")!=inAppPurchaseInMaster.getBoolean("defaultIfAirlockSystemIsDown"), "Description was changed in master inAppPurchase");
		Assert.assertTrue(inAppPurchaseInBranch.getBoolean("enabled")!=inAppPurchaseInMaster.getBoolean("enabled"), "enabled was changed in master inAppPurchase");
		Assert.assertTrue(inAppPurchaseInBranch.getBoolean("noCachedResults")!=inAppPurchaseInMaster.getBoolean("noCachedResults"), "noCachedResults was changed in master inAppPurchase");
		Assert.assertNotEquals(inAppPurchaseInBranch.getJSONArray("internalUserGroups"), inAppPurchaseInMaster.getJSONArray("internalUserGroups"), "internalUserGroups was changed in master inAppPurchase");
		//Assert.assertNotEquals(inAppPurchaseInBranch.getJSONObject("premiumRule"), inAppPurchaseInMaster.getJSONObject("premiumRule"), "premiumRule was changed in master inAppPurchase");
	}

	@Test (dependsOnMethods="updateInAppPurchaseInBranch", description ="Checkout inAppPurchase and updated its fields in master and check delta") 
	public void updateInAppPurchaseInMaster() throws Exception{
		//recreate branch
		int delBranchCode = br.deleteBranch(branchID, sessionToken);
		Assert.assertTrue (delBranchCode==200, "branch was  not deleted");
		
		branchID = addBranch("branch1");
		Assert.assertFalse(branchID.contains("error"), "Branch1 was not created: " + branchID);


		//move inAppPurchase to production so all runtime files should be updated
		String inAppPurchase1 = purchasesApi.getPurchaseItem(inAppPurchaseID1, sessionToken);
		JSONObject jsonF = new JSONObject(inAppPurchase1);
		jsonF.put("stage", "PRODUCTION"); 
		String response = purchasesApi.updatePurchaseItem(seasonID, inAppPurchaseID1, jsonF.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "inAppPurchase was not updated to production");
		
		//checkout inAppPurchase
		response = br.checkoutFeature(branchID, inAppPurchaseID1, sessionToken);
		Assert.assertFalse(response.contains("error"), "inAppPurchase was not checked out to branch");
		

		String dateFormat = purchasesApi.setDateFormat();
		
		inAppPurchase1 = purchasesApi.getPurchaseItem(inAppPurchaseID1, sessionToken);
		jsonF = new JSONObject(inAppPurchase1);
		jsonF.put("enabled", false); //changing field in the master and verifying that the branch runtime includes the delta

	    response = purchasesApi.updatePurchaseItem(seasonID, inAppPurchaseID1, jsonF.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "inAppPurchase was not updated");
		
		purchasesApi.setSleep();
		RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getDevelopmentFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);		
		Assert.assertTrue(responseDev.code==200, "Runtime development file was not changed");
		RuntimeRestApi.DateModificationResults responseProd = RuntimeDateUtilities.getProductionFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseProd.code==200, "Runtime production file was not changed");
		
		RuntimeRestApi.DateModificationResults branchesRuntimeDev = RuntimeDateUtilities.getDevelopmentBranchFileDateModification(m_url, productID, seasonID, branchID, dateFormat, sessionToken);
		Assert.assertTrue(branchesRuntimeDev.code==200, "Branch runtime development file was not changed");
		
		RuntimeRestApi.DateModificationResults branchesRuntimeProd = RuntimeDateUtilities.getProductionBranchFileDateModification(m_url, productID, seasonID, branchID, dateFormat, sessionToken);
		Assert.assertTrue(branchesRuntimeProd.code==200, "Branch runtime production file was changed");

		//validate field values in branch production file
		JSONObject inAppPurchaseProd = new JSONObject(branchesRuntimeDev.message).getJSONArray("entitlements").getJSONObject(0);
		Assert.assertTrue(inAppPurchaseProd.getBoolean("enabled") == true, "enabled not updated in runtime " + "production" + " branch file");
		
		//validate field values in branch development file
		JSONObject inAppPurchaseDev = new JSONObject(branchesRuntimeDev.message).getJSONArray("entitlements").getJSONObject(0);
		Assert.assertTrue(inAppPurchaseDev.getBoolean("enabled") == true, "enabled not updated in runtime " + "development" + " branch file");
	}

	@Test (dependsOnMethods="updateInAppPurchaseInMaster", description ="Checkout Config rule and updated its description field in master and check delta - nothing should be changed since desc is not in runtime file") 
	public void updateConfigRuleInMaster() throws Exception{
		//recreate branch
		int delBranchCode = br.deleteBranch(branchID, sessionToken);
		Assert.assertTrue (delBranchCode==200, "branch was  not deleted");
		
		branchID = addBranch("branch1");
		Assert.assertFalse(branchID.contains("error"), "Branch1 was not created: " + branchID);

		//checkout inAppPurchase
		String response = br.checkoutFeature(branchID, inAppPurchaseID1, sessionToken);
		Assert.assertFalse(response.contains("error"), "inAppPurchase was not checked out to branch");

		String dateFormat = purchasesApi.setDateFormat();
		
		String configRule = purchasesApi.getPurchaseItem(configID, sessionToken);
		JSONObject crJson = new JSONObject(configRule);
		crJson.put("description", "ttt"); //changing field in the master and verifying that the branch runtime includes the delta

	    response = purchasesApi.updatePurchaseItem(seasonID, configID, crJson.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Configuration rule was not updated");
		
		purchasesApi.setSleep();
		RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getDevelopmentFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);		
		Assert.assertTrue(responseDev.code==200, "Runtime development file was not changed");
		RuntimeRestApi.DateModificationResults responseProd = RuntimeDateUtilities.getProductionFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseProd.code==304, "Runtime production file was not changed");
		
		RuntimeRestApi.DateModificationResults branchesRuntimeDev = RuntimeDateUtilities.getDevelopmentBranchFileDateModification(m_url, productID, seasonID, branchID, dateFormat, sessionToken);
		Assert.assertTrue(branchesRuntimeDev.code==304, "Branch runtime development file was changed");
		
		RuntimeRestApi.DateModificationResults branchesRuntimeProd = RuntimeDateUtilities.getProductionBranchFileDateModification(m_url, productID, seasonID, branchID, dateFormat, sessionToken);
		//nothing should be changed since desc is not in runtime file
		Assert.assertTrue(branchesRuntimeProd.code==304, "Branch runtime production file changed");
	}

	@Test (dependsOnMethods="updateConfigRuleInMaster", description ="Checkout mtx and updated its fields in master and check delta") 
	public void updateMTXInMaster() throws Exception{
		//recreate branch
		int delBranchCode = br.deleteBranch(branchID, sessionToken);
		Assert.assertTrue (delBranchCode==200, "branch was  not deleted");
		
		branchID = addBranch("branch1");
		Assert.assertFalse(branchID.contains("error"), "Branch1 was not created: " + branchID);

		//checkout inAppPurchase
		String response = br.checkoutFeature(branchID, inAppPurchaseID2, sessionToken);
		Assert.assertFalse(response.contains("error"), "inAppPurchase was not checked out to branch");
		

		String dateFormat = purchasesApi.setDateFormat();
		
		String mtx = purchasesApi.getPurchaseItem(mixID, sessionToken);
		JSONObject mtxJson = new JSONObject(mtx);
		mtxJson.put("maxFeaturesOn", 33); //changing field in the master and verifying that the branch runtime includes the delta

	    response = purchasesApi.updatePurchaseItem(seasonID, mixID, mtxJson.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "MTX was not updated");
		
		purchasesApi.setSleep();
		RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getDevelopmentFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);		
		Assert.assertTrue(responseDev.code==200, "Runtime development file was not changed");
		RuntimeRestApi.DateModificationResults responseProd = RuntimeDateUtilities.getProductionFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseProd.code==304, "Runtime production file was changed");
		
		RuntimeRestApi.DateModificationResults branchesRuntimeDev = RuntimeDateUtilities.getDevelopmentBranchFileDateModification(m_url, productID, seasonID, branchID, dateFormat, sessionToken);
		Assert.assertTrue(branchesRuntimeDev.code==200, "Branch runtime development file was not changed");
		
		RuntimeRestApi.DateModificationResults branchesRuntimeProd = RuntimeDateUtilities.getProductionBranchFileDateModification(m_url, productID, seasonID, branchID, dateFormat, sessionToken);
		Assert.assertTrue(branchesRuntimeProd.code==200, "Branch runtime production file was changed");

		//validate field values in branch development file
		JSONObject mtxDev = new JSONObject(branchesRuntimeDev.message).getJSONArray("entitlements").getJSONObject(0).getJSONArray("entitlements").getJSONObject(0);
		Assert.assertTrue(mtxDev.getInt("maxFeaturesOn") == 1, "maxFeaturesOn not updated in runtime " + "development" + " branch file");
		
		//validate field values in branch production file
		JSONObject mtxProd = new JSONObject(branchesRuntimeDev.message).getJSONArray("entitlements").getJSONObject(0).getJSONArray("entitlements").getJSONObject(0);
		Assert.assertTrue(mtxProd.getInt("maxFeaturesOn") == 1, "maxFeaturesOn not updated in runtime " + "production" + " branch file");
	}

	@Test (dependsOnMethods="updateMTXInMaster", description ="Checkout purchase options and updated its fields in master and check delta") 
	public void updatePurchaseOptionsInMaster() throws Exception{
		//recreate branch
		int delBranchCode = br.deleteBranch(branchID, sessionToken);
		Assert.assertTrue (delBranchCode==200, "branch was  not deleted");
		
		branchID = addBranch("branch1");
		Assert.assertFalse(branchID.contains("error"), "Branch1 was not created: " + branchID);

		String purchaseOptionsStr = FileUtils.fileToString(filePath + "purchases/purchaseOptions1.txt", "UTF-8", false);
		JSONObject jsonPO = new JSONObject(purchaseOptionsStr);
		String purchaseOptionsID1 = purchasesApi.addPurchaseItem(seasonID, jsonPO.toString(), inAppPurchaseID1, sessionToken);
		Assert.assertFalse(purchaseOptionsID1.contains("error"), "Can't create purchaseOptions  " + purchaseOptionsID1);
		
		//checkout feature
		String response = br.checkoutFeature(branchID, purchaseOptionsID1, sessionToken);
		Assert.assertFalse(response.contains("error"), "purchaseOptions was not checked out to branch");
		

		String dateFormat = purchasesApi.setDateFormat();
		
		String purchaseOptions = purchasesApi.getPurchaseItem(purchaseOptionsID1, sessionToken);
		JSONObject poJson = new JSONObject(purchaseOptions);
		poJson.put("rolloutPercentage", 33); //changing field in the master and verifying that the branch runtime includes the delta
		/*JSONArray storeProductIdsArr = new JSONArray();
		JSONObject storeProdId = new JSONObject();
		storeProdId.put("storeType", "store1");
		storeProdId.put("productId", "123");
		storeProductIdsArr.add(storeProdId);
		poJson.put("storeProductIds", storeProductIdsArr);
		*/
	    response = purchasesApi.updatePurchaseItem(seasonID, purchaseOptionsID1, poJson.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "purchaseOptions was not updated");
		
		purchasesApi.setSleep();
		RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getDevelopmentFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);		
		Assert.assertTrue(responseDev.code==200, "Runtime development file was not changed");
		RuntimeRestApi.DateModificationResults responseProd = RuntimeDateUtilities.getProductionFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseProd.code==304, "Runtime production file was not changed");
		
		RuntimeRestApi.DateModificationResults branchesRuntimeDev = RuntimeDateUtilities.getDevelopmentBranchFileDateModification(m_url, productID, seasonID, branchID, dateFormat, sessionToken);
		Assert.assertTrue(branchesRuntimeDev.code==200, "Branch runtime development file was not changed");
		
		RuntimeRestApi.DateModificationResults branchesRuntimeProd = RuntimeDateUtilities.getProductionBranchFileDateModification(m_url, productID, seasonID, branchID, dateFormat, sessionToken);
		Assert.assertTrue(branchesRuntimeProd.code==304, "Branch runtime production file was changed");

		//validate field values in branch development file
		JSONObject poDev = new JSONObject(branchesRuntimeDev.message).getJSONArray("entitlements").getJSONObject(0).getJSONArray("purchaseOptions").getJSONObject(0);
		Assert.assertTrue(poDev.getInt("rolloutPercentage") == 10, "rolloutPercentage not updated in runtime " + "development" + " branch file");
	}

	private void validateFields(JSONObject inAppPurchase, JSONObject inAppPurchaseFromBranch, String stage) throws JSONException{
		Assert.assertTrue(inAppPurchase.getBoolean("defaultIfAirlockSystemIsDown") == inAppPurchaseFromBranch.getBoolean("defaultIfAirlockSystemIsDown"), "defaultIfAirlockSystemIsDown not updated in runtime " + stage + " branch file");
		Assert.assertTrue(inAppPurchase.getBoolean("enabled") == inAppPurchaseFromBranch.getBoolean("enabled"), "enabled not updated in runtime " + stage + " branch file");
		Assert.assertTrue(inAppPurchase.getBoolean("noCachedResults") == inAppPurchaseFromBranch.getBoolean("noCachedResults"), "noCachedResults not updated in runtime " + stage + " branch file");
		Assert.assertTrue(inAppPurchase.getDouble("rolloutPercentage") == inAppPurchaseFromBranch.getDouble("rolloutPercentage"), "rolloutPercentage not updated in runtime " + stage + " branch file");
		Assert.assertTrue(inAppPurchase.getString("defaultConfiguration").equals(inAppPurchaseFromBranch.getString("defaultConfiguration")), "defaultConfiguration not updated in runtime " + stage + " branch file");		
		Assert.assertTrue(inAppPurchase.getString("minAppVersion").equals(inAppPurchaseFromBranch.getString("minAppVersion")), "minAppVersion not updated in runtime " + stage + " branch file");
		Assert.assertTrue(inAppPurchase.getString("stage").equals(inAppPurchaseFromBranch.getString("stage")), "stage not updated in runtime " + stage + " branch file");
		Assert.assertEquals(inAppPurchase.getJSONObject("rule"), inAppPurchaseFromBranch.getJSONObject("rule"), "rule not updated in runtime " + stage + " branch file");
		Assert.assertEquals(inAppPurchase.getJSONArray("internalUserGroups"), inAppPurchaseFromBranch.getJSONArray("internalUserGroups"), "internalUserGroups not updated in runtime " + stage + " branch file");
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
