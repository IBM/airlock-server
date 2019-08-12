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
import tests.restapi.FeaturesRestApi;
import tests.restapi.InAppPurchasesRestApi;
import tests.restapi.RuntimeDateUtilities;
import tests.restapi.RuntimeRestApi;


public class BranchFeatureParentName {
	private String productID;
	private String seasonID;
	private String branchID;
	private String entitlementID1;
	private String entitlementID2;
	private String entitlementID3;
	private String entitlementID4;
	private String entitlementMtxID;
	private String filePath;
	private String m_url;
	private String sessionToken = "";
	private AirlockUtils baseUtils;
	private BranchesRestApi br ;
	private FeaturesRestApi f;
	private InAppPurchasesRestApi purchasesApi;
	

	@BeforeClass
	@Parameters({"url", "analyticsUrl", "translationsUrl", "configPath", "sessionToken", "userName", "userPassword", "appName", "productsToDeleteFile"})
	public void init(String url, String analyticsUrl, String translationsUrl, String configPath, String sToken, String userName, String userPassword, String appName, String productsToDeleteFile) throws Exception{
		m_url = url;
		filePath = configPath ;
		f = new FeaturesRestApi();
		f.setURL(m_url);
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
	 * Add in branch:
	 * ROOT->E1
	 *     ->E2
	 *     ->E_MTX
	 */

	@Test (description ="Add components") 
	public void addComponents () throws IOException, JSONException, InterruptedException {
		branchID = addBranch("branch1");
		Assert.assertFalse(branchID.contains("error"), "Branch1 was not created: " + branchID);	
	}


	@Test(dependsOnMethods="addComponents", description="add development entitlements to branch")
	public void addEntitlementsToBranch() throws InterruptedException, IOException, JSONException{
		String dateFormat = f.setDateFormat();

		String entitlement1 = FileUtils.fileToString(filePath + "purchases/inAppPurchase1.txt", "UTF-8", false);
		entitlementID1 = purchasesApi.addPurchaseItemToBranch(seasonID, branchID, entitlement1, "ROOT", sessionToken);
		Assert.assertFalse(entitlementID1.contains("error"), "entitlement was not added to the season");

		String entitlement2 = FileUtils.fileToString(filePath + "purchases/inAppPurchase2.txt", "UTF-8", false);
		entitlementID2 = purchasesApi.addPurchaseItemToBranch(seasonID, branchID, entitlement2, "ROOT", sessionToken);
		Assert.assertFalse(entitlementID2.contains("error"), "entitlement was not added to the season");

		String entitlementMTX = FileUtils.fileToString(filePath + "purchases/inAppPurchaseMutual.txt", "UTF-8", false);
		entitlementMtxID = purchasesApi.addPurchaseItemToBranch(seasonID, branchID, entitlementMTX, "ROOT", sessionToken);
		Assert.assertFalse(entitlementMtxID.contains("error"), "entitlement mtx was not added to the season");

		f.setSleep();
		RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getDevelopmentFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);		
		Assert.assertTrue(responseDev.code==304, "Runtime development file was changed");
		RuntimeRestApi.DateModificationResults responseProd = RuntimeDateUtilities.getProductionFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseProd.code==304, "Runtime production file was changed");

		RuntimeRestApi.DateModificationResults branchesRuntimeDev = RuntimeDateUtilities.getDevelopmentBranchFileDateModification(m_url, productID, seasonID, branchID, dateFormat, sessionToken);
		Assert.assertTrue(branchesRuntimeDev.code==200, "Branch runtime development file was not changed");
		JSONArray branchEntitlements = getBranchEntitlements(branchesRuntimeDev.message);
		Assert.assertTrue(branchEntitlements.size()==3, "Incorrect number of entitlements in branch development runtime file");
		Assert.assertTrue(branchEntitlements.getJSONObject(0).getString("branchFeatureParentName").equals("ROOT"), "Incorrect branchFeatureParentName");
		Assert.assertTrue(branchEntitlements.getJSONObject(1).getString("branchFeatureParentName").equals("ROOT"), "Incorrect branchFeatureParentName");
		Assert.assertTrue(branchEntitlements.getJSONObject(2).getString("branchFeatureParentName").equals("ROOT"), "Incorrect branchFeatureParentName");

		RuntimeRestApi.DateModificationResults branchesRuntimeProd = RuntimeDateUtilities.getProductionBranchFileDateModification(m_url, productID, seasonID, branchID, dateFormat, sessionToken);
		Assert.assertTrue(branchesRuntimeProd.code==304, "Branch runtime production file was changed");
	}


	@Test(dependsOnMethods="addEntitlementsToBranch", description="move entitlements to mtx")
	public void moveEntitlementsToMTXInBranch() throws InterruptedException, IOException, JSONException{
		String dateFormat = f.setDateFormat();

		JSONObject feature1 = new JSONObject(purchasesApi.getPurchaseItemFromBranch(entitlementID1, branchID, sessionToken));
		JSONObject feature2 = new JSONObject(purchasesApi.getPurchaseItemFromBranch(entitlementID2, branchID, sessionToken));
		JSONObject featureMTX = new JSONObject (purchasesApi.getPurchaseItemFromBranch(entitlementMtxID, branchID, sessionToken));

		JSONArray mtxFeatures = new JSONArray();
		mtxFeatures.add(feature1);
		mtxFeatures.add(feature2);

		featureMTX.put("entitlements", mtxFeatures);

		String response = purchasesApi.updatePurchaseItemInBranch(seasonID, branchID, entitlementMtxID, featureMTX.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "cannot update mtx in branch:" + response);

		f.setSleep();
		RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getDevelopmentFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);		
		Assert.assertTrue(responseDev.code==304, "Runtime development file was changed");
		RuntimeRestApi.DateModificationResults responseProd = RuntimeDateUtilities.getProductionFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseProd.code==304, "Runtime production file was changed");

		RuntimeRestApi.DateModificationResults branchesRuntimeDev = RuntimeDateUtilities.getDevelopmentBranchFileDateModification(m_url, productID, seasonID, branchID, dateFormat, sessionToken);
		Assert.assertTrue(branchesRuntimeDev.code==200, "Branch runtime development file was not changed");
		JSONArray branchEntitlements = getBranchEntitlements(branchesRuntimeDev.message);
		Assert.assertTrue(branchEntitlements.size()==1, "Incorrect number of entitlements in branch development runtime file");
		Assert.assertTrue(branchEntitlements.getJSONObject(0).getString("branchFeatureParentName").equals("ROOT"), "Incorrect branchFeatureParentName");
		JSONArray mtxSubFeatures = branchEntitlements.getJSONObject(0).getJSONArray("entitlements");
		Assert.assertTrue(mtxSubFeatures.size()==2, "Incorrect number of mtx sub entitlements in branch development runtime file");
		Assert.assertFalse(mtxSubFeatures.getJSONObject(0).containsKey("branchFeatureParentName"), "Incorrect branchFeatureParentName");
		Assert.assertFalse(mtxSubFeatures.getJSONObject(1).containsKey("branchFeatureParentName"), "Incorrect branchFeatureParentName");	
	}

	//ROOT->E1->E2, E3
	@Test(dependsOnMethods="moveEntitlementsToMTXInBranch", description="add development entitlement to master")
	public void addEntitlementsToMaster() throws InterruptedException, IOException, JSONException{
		String dateFormat = f.setDateFormat();

		int code = purchasesApi.deletePurchaseItemFromBranch(entitlementMtxID, branchID, sessionToken);
		Assert.assertTrue(code == 200, "cannot delete mtx bfrom branch");

		String feature1 = FileUtils.fileToString(filePath + "purchases/inAppPurchase1.txt", "UTF-8", false);
		entitlementID1 = purchasesApi.addPurchaseItemToBranch(seasonID, "MASTER", feature1, "ROOT", sessionToken);
		Assert.assertFalse(entitlementID1.contains("error"), "Feature was not added to the season: " + entitlementID1);

		String feature2 = FileUtils.fileToString(filePath + "purchases/inAppPurchase2.txt", "UTF-8", false);
		entitlementID2 = purchasesApi.addPurchaseItemToBranch(seasonID, "MASTER", feature2, entitlementID1, sessionToken);
		Assert.assertFalse(entitlementID2.contains("error"), "Feature was not added to the season: " + entitlementID2);

		String feature3 = FileUtils.fileToString(filePath + "purchases/inAppPurchase3.txt", "UTF-8", false);
		entitlementID3 = purchasesApi.addPurchaseItemToBranch(seasonID, "MASTER", feature3, entitlementID1, sessionToken);
		Assert.assertFalse(entitlementID3.contains("error"), "Feature was not added to the season: " + entitlementID3);

		String featureMTX = FileUtils.fileToString(filePath + "purchases/inAppPurchaseMutual.txt", "UTF-8", false);
		entitlementMtxID = purchasesApi.addPurchaseItemToBranch(seasonID, "MASTER", featureMTX, "ROOT", sessionToken);
		Assert.assertFalse(entitlementMtxID.contains("error"), "Feature mtx was not added to the season");

		f.setSleep();
		RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getDevelopmentFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);		
		Assert.assertTrue(responseDev.code==200, "Runtime development file was changed");
		RuntimeRestApi.DateModificationResults responseProd = RuntimeDateUtilities.getProductionFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseProd.code==304, "Runtime production file was changed");

		RuntimeRestApi.DateModificationResults branchesRuntimeDev = RuntimeDateUtilities.getDevelopmentBranchFileDateModification(m_url, productID, seasonID, branchID, dateFormat, sessionToken);
		Assert.assertTrue(branchesRuntimeDev.code==200, "Branch runtime development file was not changed");
		JSONArray branchEntitlements = getBranchEntitlements(branchesRuntimeDev.message);
		Assert.assertTrue(branchEntitlements.size()==0, "Incorrect number of features in branch development runtime file");

		RuntimeRestApi.DateModificationResults branchesRuntimeProd = RuntimeDateUtilities.getProductionBranchFileDateModification(m_url, productID, seasonID, branchID, dateFormat, sessionToken);
		Assert.assertTrue(branchesRuntimeProd.code==304, "Branch runtime production file was changed");
	}

	@Test(dependsOnMethods="addEntitlementsToMaster", description="chceckout entitlement2")
	public void checkoutEntitlement2() throws InterruptedException, IOException, JSONException{
		String dateFormat = f.setDateFormat();

		String  response = br.checkoutFeature(branchID, entitlementID2, sessionToken);
		Assert.assertFalse(response.contains("error"), "cannot checkout entitlement: " + response);

		f.setSleep();
		RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getDevelopmentFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);		
		Assert.assertTrue(responseDev.code==304, "Runtime development file was changed");
		RuntimeRestApi.DateModificationResults responseProd = RuntimeDateUtilities.getProductionFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseProd.code==304, "Runtime production file was changed");

		RuntimeRestApi.DateModificationResults branchesRuntimeDev = RuntimeDateUtilities.getDevelopmentBranchFileDateModification(m_url, productID, seasonID, branchID, dateFormat, sessionToken);
		Assert.assertTrue(branchesRuntimeDev.code==200, "Branch runtime development file was not changed");
		JSONArray branchEntitlements = getBranchEntitlements(branchesRuntimeDev.message);
		Assert.assertTrue(branchEntitlements.size()==1, "Incorrect number of entitlements in branch development runtime file");
		Assert.assertTrue(branchEntitlements.getJSONObject(0).getString("branchFeatureParentName").equals("ROOT"), "Incorrect branchFeatureParentName");
		Assert.assertTrue(branchEntitlements.getJSONObject(0).getString("name").equals("inAppPurchase1"), "Incorrect name");

		JSONArray feature1SubFeatures = branchEntitlements.getJSONObject(0).getJSONArray("entitlements");
		Assert.assertTrue(feature1SubFeatures.size()==1, "Incorrect number of entitlements in branch development runtime file");
		Assert.assertFalse(feature1SubFeatures.getJSONObject(0).containsKey("branchFeatureParentName"), "Incorrect branchFeatureParentName");
		Assert.assertTrue(feature1SubFeatures.getJSONObject(0).getString("name").equals("inAppPurchase2"), "Incorrect name");

		RuntimeRestApi.DateModificationResults branchesRuntimeProd = RuntimeDateUtilities.getProductionBranchFileDateModification(m_url, productID, seasonID, branchID, dateFormat, sessionToken);
		Assert.assertTrue(branchesRuntimeProd.code==304, "Branch runtime production file was changed");
	}

	@Test(dependsOnMethods="checkoutEntitlement2", description="chceckout entitlement3")
	public void checkoutEntitlement3() throws InterruptedException, IOException, JSONException{
		String dateFormat = f.setDateFormat();

		String  response = br.checkoutFeature(branchID, entitlementID3, sessionToken);
		Assert.assertFalse(response.contains("error"), "cannot checkout features: " + response);

		f.setSleep();
		RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getDevelopmentFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);		
		Assert.assertTrue(responseDev.code==304, "Runtime development file was changed");
		RuntimeRestApi.DateModificationResults responseProd = RuntimeDateUtilities.getProductionFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseProd.code==304, "Runtime production file was changed");

		RuntimeRestApi.DateModificationResults branchesRuntimeDev = RuntimeDateUtilities.getDevelopmentBranchFileDateModification(m_url, productID, seasonID, branchID, dateFormat, sessionToken);
		Assert.assertTrue(branchesRuntimeDev.code==200, "Branch runtime development file was not changed");
		JSONArray branchEntitlements = getBranchEntitlements(branchesRuntimeDev.message);
		Assert.assertTrue(branchEntitlements.size()==1, "Incorrect number of entitlements in branch development runtime file");
		Assert.assertTrue(branchEntitlements.getJSONObject(0).getString("branchFeatureParentName").equals("ROOT"), "Incorrect branchFeatureParentName");
		Assert.assertTrue(branchEntitlements.getJSONObject(0).getString("name").equals("inAppPurchase1"), "Incorrect name");

		JSONArray feature1SubFeatures = branchEntitlements.getJSONObject(0).getJSONArray("entitlements");
		Assert.assertTrue(feature1SubFeatures.size()==2, "Incorrect number of entitlements in branch development runtime file");
		Assert.assertFalse(feature1SubFeatures.getJSONObject(0).containsKey("branchFeatureParentName"), "Incorrect branchFeatureParentName");
		Assert.assertTrue(feature1SubFeatures.getJSONObject(0).getString("name").equals("inAppPurchase2"), "Incorrect name");
		Assert.assertFalse(feature1SubFeatures.getJSONObject(1).containsKey("branchFeatureParentName"), "Incorrect branchFeatureParentName");
		Assert.assertTrue(feature1SubFeatures.getJSONObject(1).getString("name").equals("inAppPurchase3"), "Incorrect name");

		RuntimeRestApi.DateModificationResults branchesRuntimeProd = RuntimeDateUtilities.getProductionBranchFileDateModification(m_url, productID, seasonID, branchID, dateFormat, sessionToken);
		Assert.assertTrue(branchesRuntimeProd.code==304, "Branch runtime production file was changed");
	}

	//ROOT->E1->E2, E3 c_o
	//add:
	//     ->E4 new
	//     ->MTX c_o
	@Test(dependsOnMethods="checkoutEntitlement3", description="add development entitlement to master")
	public void addNewEntitlementsToMaster() throws InterruptedException, IOException, JSONException{
		String dateFormat = f.setDateFormat();

		String feature4 = FileUtils.fileToString(filePath + "purchases/inAppPurchase4.txt", "UTF-8", false);
		entitlementID4 = purchasesApi.addPurchaseItemToBranch(seasonID, branchID, feature4, "ROOT", sessionToken);
		Assert.assertFalse(entitlementID4.contains("error"), "Feature was not added to the season: " + entitlementID4);

		String  response = br.checkoutFeature(branchID, entitlementMtxID, sessionToken);
		Assert.assertFalse(response.contains("error"), "cannot checkout features mtx: " + response);

		f.setSleep();
		RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getDevelopmentFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);		
		Assert.assertTrue(responseDev.code==304, "Runtime development file was changed");
		RuntimeRestApi.DateModificationResults responseProd = RuntimeDateUtilities.getProductionFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseProd.code==304, "Runtime production file was changed");

		RuntimeRestApi.DateModificationResults branchesRuntimeDev = RuntimeDateUtilities.getDevelopmentBranchFileDateModification(m_url, productID, seasonID, branchID, dateFormat, sessionToken);
		Assert.assertTrue(branchesRuntimeDev.code==200, "Branch runtime development file was not changed");
		JSONArray branchEntitlements = getBranchEntitlements(branchesRuntimeDev.message);
		Assert.assertTrue(branchEntitlements.size()==3, "Incorrect number of entitlements in branch development runtime file");
		Assert.assertTrue(branchEntitlements.getJSONObject(0).getString("branchFeatureParentName").equals("ROOT"), "Incorrect branchFeatureParentName");
		Assert.assertTrue(branchEntitlements.getJSONObject(0).getString("name").equals("inAppPurchase1"), "Incorrect name");
		Assert.assertTrue(branchEntitlements.getJSONObject(2).getString("branchFeatureParentName").equals("ROOT"), "Incorrect branchFeatureParentName");
		Assert.assertTrue(branchEntitlements.getJSONObject(2).getString("branchStatus").equals("CHECKED_OUT"), "Incorrect branchStatus");
		Assert.assertTrue(branchEntitlements.getJSONObject(1).getString("branchFeatureParentName").equals("ROOT"), "Incorrect branchFeatureParentName");
		Assert.assertTrue(branchEntitlements.getJSONObject(1).getString("name").equals("inAppPurchase4"), "Incorrect name");
		Assert.assertTrue(branchEntitlements.getJSONObject(1).getString("branchStatus").equals("NEW"), "Incorrect branchStatus");
		
		JSONArray feature1SubFeatures = branchEntitlements.getJSONObject(0).getJSONArray("entitlements");
		Assert.assertTrue(feature1SubFeatures.size()==2, "Incorrect number of entitlements in branch development runtime file");
		Assert.assertFalse(feature1SubFeatures.getJSONObject(0).containsKey("branchFeatureParentName"), "Incorrect branchFeatureParentName");
		Assert.assertTrue(feature1SubFeatures.getJSONObject(0).getString("name").equals("inAppPurchase2"), "Incorrect name");
		Assert.assertFalse(feature1SubFeatures.getJSONObject(1).containsKey("branchFeatureParentName"), "Incorrect branchFeatureParentName");
		Assert.assertTrue(feature1SubFeatures.getJSONObject(1).getString("name").equals("inAppPurchase3"), "Incorrect name");

		RuntimeRestApi.DateModificationResults branchesRuntimeProd = RuntimeDateUtilities.getProductionBranchFileDateModification(m_url, productID, seasonID, branchID, dateFormat, sessionToken);
		Assert.assertTrue(branchesRuntimeProd.code==304, "Branch runtime production file was changed");

	}

	@Test(dependsOnMethods="addNewEntitlementsToMaster", description="move entitlements to mtx")
	public void moveEntitlementsToMTXInBranch2() throws InterruptedException, IOException, JSONException{
		String dateFormat = f.setDateFormat();

		JSONObject feature1 = new JSONObject(purchasesApi.getPurchaseItemFromBranch(entitlementID1, branchID, sessionToken));
		JSONObject feature4 = new JSONObject(purchasesApi.getPurchaseItemFromBranch(entitlementID4, branchID, sessionToken));
		JSONObject featureMTX = new JSONObject (purchasesApi.getPurchaseItemFromBranch(entitlementMtxID, branchID, sessionToken));

		JSONArray mtxFeatures = new JSONArray();
		mtxFeatures.add(feature1);
		mtxFeatures.add(feature4);

		featureMTX.put("entitlements", mtxFeatures);

		String response = purchasesApi.updatePurchaseItemInBranch(seasonID, branchID, entitlementMtxID, featureMTX.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "cannot update mtx in branch:" + response);

		f.setSleep();
		RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getDevelopmentFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);		
		Assert.assertTrue(responseDev.code==304, "Runtime development file was changed");
		RuntimeRestApi.DateModificationResults responseProd = RuntimeDateUtilities.getProductionFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseProd.code==304, "Runtime production file was changed");

		RuntimeRestApi.DateModificationResults branchesRuntimeDev = RuntimeDateUtilities.getDevelopmentBranchFileDateModification(m_url, productID, seasonID, branchID, dateFormat, sessionToken);
		Assert.assertTrue(branchesRuntimeDev.code==200, "Branch runtime development file was not changed");
		JSONArray branchEntitlements = getBranchEntitlements(branchesRuntimeDev.message);
		Assert.assertTrue(branchEntitlements.size()==1, "Incorrect number of features in branch development runtime file");
		Assert.assertTrue(branchEntitlements.getJSONObject(0).getString("branchFeatureParentName").equals("ROOT"), "Incorrect branchFeatureParentName");
		JSONArray mtxSubFeatures = branchEntitlements.getJSONObject(0).getJSONArray("entitlements");
		Assert.assertTrue(mtxSubFeatures.size()==2, "Incorrect number of mtx sub entitlements in branch development runtime file");
		Assert.assertFalse(mtxSubFeatures.getJSONObject(0).containsKey("branchFeatureParentName"), "Incorrect branchFeatureParentName");
		Assert.assertFalse(mtxSubFeatures.getJSONObject(1).containsKey("branchFeatureParentName"), "Incorrect branchFeatureParentName");	
	}

	private String addBranch(String branchName) throws JSONException, IOException{
		String branch = FileUtils.fileToString(filePath + "experiments/branch1.txt", "UTF-8", false);
		JSONObject branchJson = new JSONObject(branch);
		branchJson.put("name", branchName);
		return br.createBranch(seasonID, branchJson.toString(), BranchesRestApi.MASTER, sessionToken);

	}

	private JSONArray getBranchEntitlements(String result) throws JSONException{
		JSONObject json = new JSONObject(result);
		return json.getJSONArray("entitlements");
	}

	@AfterTest
	private void reset(){
		baseUtils.reset(productID, sessionToken);
	}
}
