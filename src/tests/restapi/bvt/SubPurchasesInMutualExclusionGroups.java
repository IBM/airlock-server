package tests.restapi.bvt;

import org.apache.wink.json4j.JSONArray;

import org.apache.wink.json4j.JSONObject;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;

import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import tests.com.ibm.qautils.FileUtils;
import tests.restapi.AirlockUtils;
import tests.restapi.InAppPurchasesRestApi;
import tests.restapi.ProductsRestApi;

/**
 * This is end to end test that verifies next scenario:

     Create inAppPurchase in development
     Add MTX of 2 purchase options under the parent inAppPurchase
     Move parent to production
     Move both MTX purchaseOptions to production
     Add new MTX group of development purchase options under the parent inAppPurchase
 */

public class SubPurchasesInMutualExclusionGroups {
	protected String seasonID;
	protected String inAppPurchaseID1;
	protected String purchaseOptionsID1;
	protected String purchaseOptionsID2;
	protected String purchaseOptionsID3;
	protected String purchaseOptionsID4;
	protected String productID;
	protected String config;
	protected InAppPurchasesRestApi purchasesApi;
	protected ProductsRestApi productApi;
	protected AirlockUtils baseUtils;
	protected String sessionToken = "";
	protected String adminToken;

	@BeforeClass
	@Parameters({"url", "analyticsUrl", "translationsUrl", "configPath", "sessionToken", "userName", "userPassword", "appName", "productsToDeleteFile", "adminUser", "adminPassword"})
	public void init(String url, String analyticsUrl, String translationsUrl, String configPath, String sToken, String userName, String userPassword, String appName, String productsToDeleteFile, String adminUser, String adminPassword) throws Exception{

		config = configPath;
		productApi = new ProductsRestApi();
		purchasesApi = new InAppPurchasesRestApi();
		baseUtils = new AirlockUtils(url, analyticsUrl, translationsUrl, configPath, sToken, userName, userPassword, appName, productsToDeleteFile);
		sessionToken = baseUtils.sessionToken;
		productApi.setURL(url);
		purchasesApi.setURL(url);

		adminToken = baseUtils.getJWTToken(adminUser, adminPassword, appName);
	}


	@Test(description = "Create inAppPurchase in development")
	public void addInAppPurchase() throws Exception {
		productID = baseUtils.createProduct();
		baseUtils.printProductToFile(productID);
		seasonID = baseUtils.createSeason(productID);
		baseUtils.createSchema(seasonID);
		inAppPurchaseID1 = baseUtils.createInAppPurchase(seasonID);

		Assert.assertNotNull(inAppPurchaseID1);
		Assert.assertFalse(inAppPurchaseID1.contains("error"), "InAppPurchase was not added: " + inAppPurchaseID1);
	}

	@Test(dependsOnMethods = "addInAppPurchase", description = "Add MTX of 2 purchase options under the parent inAppPurchase")
	public void addMtxOfPurchaseOptions() throws Exception {
		String poMtx = FileUtils.fileToString(config + "purchases/purchaseOptionsMutual.txt", "UTF-8", false);
		String purOptMTX = purchasesApi.addPurchaseItem(seasonID, poMtx, inAppPurchaseID1, sessionToken);
		Assert.assertNotNull(purOptMTX);
		Assert.assertFalse(purOptMTX.contains("error"), "mtx was not added: " + purchaseOptionsID1);

		String child1 = FileUtils.fileToString(config + "purchases/purchaseOptions1.txt", "UTF-8", false);
		purchaseOptionsID1 = purchasesApi.addPurchaseItem(seasonID, child1, purOptMTX, sessionToken);
		Assert.assertNotNull(purchaseOptionsID1);
		Assert.assertFalse(purchaseOptionsID1.contains("error"), "purchaseOptions was not added: " + purchaseOptionsID1);

		String child2 = FileUtils.fileToString(config + "purchases/purchaseOptions2.txt", "UTF-8", false);
		purchaseOptionsID2 = purchasesApi.addPurchaseItem(seasonID, child2, purOptMTX, sessionToken);
		Assert.assertNotNull(purchaseOptionsID2);
		Assert.assertFalse(purchaseOptionsID2.contains("error"), "purchaseOptions was not added: " + purchaseOptionsID2);

		poMtx = purchasesApi.getPurchaseItem(purOptMTX, sessionToken);
		JSONObject json = new JSONObject(poMtx);
		JSONArray children = json.getJSONArray("purchaseOptions");
		Assert.assertEquals(children.size(), 2, "purchaseOptions were not assigned to a mutually exclusive parent.");
	}

	@Test(dependsOnMethods = "addMtxOfPurchaseOptions", description = "Move parent to production")
	public void moveParentToProd() throws Exception {
		String inAppPur = purchasesApi.getPurchaseItem(inAppPurchaseID1, sessionToken);
		JSONObject json = new JSONObject(inAppPur);
		json.put("stage", "PRODUCTION");
		String response = purchasesApi.updatePurchaseItem(seasonID, inAppPurchaseID1, json.toString(), sessionToken);
		Assert.assertNotNull(response);
		Assert.assertFalse(response.contains("error"), "inAppPurchase was not moved to prod: " + response);
	}

	@Test(dependsOnMethods = "moveParentToProd", description = "Move both MTX purchaseOptions to production")
	public void moveBothMtxPurchaseOptionsToProd() throws Exception {
		String feature1 = purchasesApi.getPurchaseItem(purchaseOptionsID1, sessionToken);
		JSONObject json1 = new JSONObject(feature1);
		json1.put("stage", "PRODUCTION");
		String response1 = purchasesApi.updatePurchaseItem(seasonID, purchaseOptionsID1, json1.toString(), sessionToken);
		Assert.assertNotNull(response1);
		Assert.assertFalse(response1.contains("error"), "purchase options was not moved to prod: " + response1);

		String feature2 = purchasesApi.getPurchaseItem(purchaseOptionsID2, sessionToken);
		JSONObject json2 = new JSONObject(feature2);
		json2.put("stage", "PRODUCTION");
		String response2 = purchasesApi.updatePurchaseItem(seasonID, purchaseOptionsID2, json2.toString(), sessionToken);
		Assert.assertNotNull(response2);
		Assert.assertFalse(response2.contains("error"), "purchase options was not moved to prod: " + response2);
	}

	@Test(dependsOnMethods = "moveBothMtxPurchaseOptionsToProd", description = "Add new MTX group of development purchaseOptions under the parent inAppPurchase")
	public void addNewMtxGroupToDev() throws Exception {
		String poMtx = FileUtils.fileToString(config + "purchases/purchaseOptionsMutual.txt", "UTF-8", false);
		String purOptMTX = purchasesApi.addPurchaseItem(seasonID, poMtx, inAppPurchaseID1, sessionToken);
		Assert.assertNotNull(purOptMTX);
		Assert.assertFalse(purOptMTX.contains("error"), "mtx was not added: " + purchaseOptionsID1);

		String child1 = FileUtils.fileToString(config + "purchases/purchaseOptions3.txt", "UTF-8", false);
		purchaseOptionsID3 = purchasesApi.addPurchaseItem(seasonID, child1, purOptMTX, sessionToken);
		Assert.assertNotNull(purchaseOptionsID3);
		Assert.assertFalse(purchaseOptionsID3.contains("error"), "purchaseOptions was not added: " + purchaseOptionsID3);

		String child2 = FileUtils.fileToString(config + "purchases/purchaseOptions4.txt", "UTF-8", false);
		purchaseOptionsID4 = purchasesApi.addPurchaseItem(seasonID, child2, purOptMTX, sessionToken);
		Assert.assertNotNull(purchaseOptionsID4);
		Assert.assertFalse(purchaseOptionsID4.contains("error"), "purchaseOptions was not added: " + purchaseOptionsID4);

		poMtx = purchasesApi.getPurchaseItem(purOptMTX, sessionToken);
		JSONObject json = new JSONObject(poMtx);
		JSONArray children = json.getJSONArray("purchaseOptions");
		Assert.assertEquals(children.size(), 2, "purchaseOptions were not assigned to a mutually exclusive parent.");
	}

	@Test(dependsOnMethods = "addNewMtxGroupToDev", description = "Validate features stage")
	public void validateStage() throws Exception {
		String inAppPurchase = purchasesApi.getPurchaseItem(inAppPurchaseID1, sessionToken);
		Assert.assertNotNull(inAppPurchase);
		JSONObject inAppPurchase1Json = new JSONObject(inAppPurchase);
		Assert.assertEquals("PRODUCTION", inAppPurchase1Json.getString("stage"), "inAppPurchase stage should be PRODUCTION but it is: " + inAppPurchase1Json.getString("stage"));

		String purchaseOption1 = purchasesApi.getPurchaseItem(purchaseOptionsID1, sessionToken);
		Assert.assertNotNull(purchaseOption1);
		JSONObject purchaseOption1Json = new JSONObject(purchaseOption1);
		Assert.assertEquals("PRODUCTION", purchaseOption1Json.getString("stage"), "Feature stage should be PRODUCTION but it is: " + purchaseOption1Json.getString("stage"));

		String purchaseOption2 = purchasesApi.getPurchaseItem(purchaseOptionsID2, sessionToken);
		Assert.assertNotNull(purchaseOption2);
		JSONObject purchaseOption2Json = new JSONObject(purchaseOption2);
		Assert.assertEquals("PRODUCTION", purchaseOption2Json.getString("stage"), "Feature stage should be PRODUCTION but it is: " + purchaseOption2Json.getString("stage"));

		String purchaseOption3 = purchasesApi.getPurchaseItem(purchaseOptionsID3, sessionToken);
		Assert.assertNotNull(purchaseOption3);
		JSONObject purchaseOption3Json = new JSONObject(purchaseOption3);
		Assert.assertEquals("DEVELOPMENT", purchaseOption3Json.getString("stage"), "Feature stage should be DEVELOPMENT but it is: " + purchaseOption3Json.getString("stage"));

		String purchaseOption4 = purchasesApi.getPurchaseItem(purchaseOptionsID4, sessionToken);
		Assert.assertNotNull(purchaseOption4);
		JSONObject purchaseOption4Json = new JSONObject(purchaseOption4);
		Assert.assertEquals("DEVELOPMENT", purchaseOption4Json.getString("stage"), "Feature stage should be DEVELOPMENT but it is: " + purchaseOption4Json.getString("stage"));
	}

	@Test(dependsOnMethods = "validateStage", description = "Delete product")
	public void deleteProduct() throws Exception {
		purchasesApi.convertAllPurchasesToDevStage(seasonID, sessionToken);
		int response = productApi.deleteProduct(productID, adminToken);
		Assert.assertEquals(response, 200, "deleteProduct failed: code " + response);
	}

}
