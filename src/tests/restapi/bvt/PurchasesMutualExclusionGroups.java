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
 * <p>
 * Create MTX  of 2 inAppPurchases
 * Move 1 inAppPurchases to production
 * Add new dev inAppPurchases under MTX
 * Add new prod inAppPurchases to MTX
 * ----
 * Create MTX  of 2 purchaseOptions
 * Move 1 purchaseOptions to production
 * Add new dev purchaseOptions under MTX
 * Add new prod purchaseOptions to MTX
 */

public class PurchasesMutualExclusionGroups {
    protected String seasonID;
    protected String inAppPurchasesMTXID1;
    protected String subInAppPurID1;
    protected String subInAppPurID2;
    protected String subInAppPurID3;
    protected String subInAppPurID4;
    protected String purchaseOptionsMTXID1;
    protected String subPurchaseOptionsID1;
    protected String subPurchaseOptionsID2;
    protected String subPurchaseOptionsID3;
    protected String subPurchaseOptionsID4;
    
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

    @Test(description = "Create MTX  of 2 in-app purchases")
    public void addMTXInAppPurchases() throws Exception {
        productID = baseUtils.createProduct();
        baseUtils.printProductToFile(productID);
        seasonID = baseUtils.createSeason(productID);
        baseUtils.createSchema(seasonID);

        String parent = FileUtils.fileToString(config + "purchases/inAppPurchaseMutual.txt", "UTF-8", false);
        inAppPurchasesMTXID1 = purchasesApi.addPurchaseItem(seasonID, parent, "ROOT", sessionToken);

        Assert.assertNotNull(inAppPurchasesMTXID1);
        Assert.assertFalse(inAppPurchasesMTXID1.contains("error"), "Purchase item was not added: " + inAppPurchasesMTXID1);

        String child1 = FileUtils.fileToString(config + "purchases/inAppPurchase1.txt", "UTF-8", false);
        subInAppPurID1 = purchasesApi.addPurchaseItem(seasonID, child1, inAppPurchasesMTXID1, sessionToken);
        Assert.assertNotNull(subInAppPurID1);
        Assert.assertFalse(subInAppPurID1.contains("error"), "Purchase item was not added: " + subInAppPurID1);

        String child2 = FileUtils.fileToString(config + "purchases/inAppPurchase2.txt", "UTF-8", false);
        subInAppPurID2 = purchasesApi.addPurchaseItem(seasonID, child2, inAppPurchasesMTXID1, sessionToken);
        Assert.assertNotNull(subInAppPurID2);
        Assert.assertFalse(subInAppPurID2.contains("error"), "Purchase item was not added: " + subInAppPurID2);

        parent = purchasesApi.getPurchaseItem(inAppPurchasesMTXID1, sessionToken);
        JSONObject json = new JSONObject(parent);
        JSONArray children = json.getJSONArray("entitlements");
        Assert.assertEquals(children.size(), 2, "Sub-inAppPuchases were not assigned to a mutually exclusive parent.");

    }

    @Test(dependsOnMethods = "addMTXInAppPurchases", description = "Move 1 inAppPurchase to production")
    public void moveInAppPurchaseToProd() throws Exception {
        String feature = purchasesApi.getPurchaseItem(subInAppPurID1, sessionToken);
        JSONObject json = new JSONObject(feature);
        json.put("stage", "PRODUCTION");
        String response = purchasesApi.updatePurchaseItem(seasonID, subInAppPurID1, json.toString(), sessionToken);
        Assert.assertNotNull(response);
        Assert.assertFalse(response.contains("error"), "InAppPurchase was not moved to prod: " + response);
    }

    @Test(dependsOnMethods = "moveInAppPurchaseToProd", description = "Add new dev inAppPurchase under MTX")
    public void addDevInAppPurchaseToMTX() throws Exception {
        String child3 = FileUtils.fileToString(config + "purchases/inAppPurchase3.txt", "UTF-8", false);
        subInAppPurID3 = purchasesApi.addPurchaseItem(seasonID, child3, inAppPurchasesMTXID1, sessionToken);

        Assert.assertNotNull(subInAppPurID3);
        Assert.assertFalse(subInAppPurID3.contains("error"), "InAppPurchase was not added: " + subInAppPurID3);
    }

    @Test(dependsOnMethods = "addDevInAppPurchaseToMTX", description = "Add new prod InAppPurchase to MTX")
    public void addProdInAppPurchaseToMTX() throws Exception {
        String child4 = FileUtils.fileToString(config + "purchases/inAppPurchaseProduction.txt", "UTF-8", false);
        subInAppPurID4 = purchasesApi.addPurchaseItem(seasonID, child4, inAppPurchasesMTXID1, sessionToken);

        Assert.assertNotNull(subInAppPurID4);
        Assert.assertFalse(subInAppPurID4.contains("error"), "InAppPurchase was not added: " + subInAppPurID4);
    }

    @Test(dependsOnMethods = "addProdInAppPurchaseToMTX", description = "Validate InAppPurchases stage")
    public void validateStage() throws Exception {
        String subItem1 = purchasesApi.getPurchaseItem(subInAppPurID1, sessionToken);
        Assert.assertNotNull(subItem1);
        JSONObject subItem1Json = new JSONObject(subItem1);
        Assert.assertEquals("PRODUCTION", subItem1Json.getString("stage"), "inAppPurchase stage should be PRODUCTION but it is: " + subItem1Json.getString("stage"));

        String subItem2 = purchasesApi.getPurchaseItem(subInAppPurID2, sessionToken);
        Assert.assertNotNull(subItem2);
        JSONObject subItem2Json = new JSONObject(subItem2);
        Assert.assertEquals("DEVELOPMENT", subItem2Json.getString("stage"), "InAppPurchase stage should be DEVELOPMENT but it is: " + subItem2Json.getString("stage"));

        String subItem3 = purchasesApi.getPurchaseItem(subInAppPurID3, sessionToken);
        Assert.assertNotNull(subItem3);
        JSONObject subItem3Json = new JSONObject(subItem3);
        Assert.assertEquals("DEVELOPMENT", subItem3Json.getString("stage"), "InAppPurchase stage should be DEVELOPMENT but it is: " + subItem3Json.getString("stage"));

        String subItem4 = purchasesApi.getPurchaseItem(subInAppPurID4, sessionToken);
        Assert.assertNotNull(subItem4);
        JSONObject subitem4Json = new JSONObject(subItem4);
        Assert.assertEquals("PRODUCTION", subitem4Json.getString("stage"), "InAppPurchase stage should be PRODUCTION but it is: " + subitem4Json.getString("stage"));
    }

    
    @Test(dependsOnMethods = "validateStage", description = "Create MTX  of 2 purchase options")
    public void addMTXPurchaseOptions() throws Exception {
        String mtx = FileUtils.fileToString(config + "purchases/purchaseOptionsMutual.txt", "UTF-8", false);
        purchaseOptionsMTXID1 = purchasesApi.addPurchaseItem(seasonID, mtx, subInAppPurID1, sessionToken);

        Assert.assertNotNull(purchaseOptionsMTXID1);
        Assert.assertFalse(purchaseOptionsMTXID1.contains("error"), "Purchase item was not added: " + purchaseOptionsMTXID1);

        String child1 = FileUtils.fileToString(config + "purchases/purchaseOptions1.txt", "UTF-8", false);
        subPurchaseOptionsID1 = purchasesApi.addPurchaseItem(seasonID, child1, purchaseOptionsMTXID1, sessionToken);
        Assert.assertNotNull(subPurchaseOptionsID1);
        Assert.assertFalse(subPurchaseOptionsID1.contains("error"), "Purchase item was not added: " + subPurchaseOptionsID1);

        String child2 = FileUtils.fileToString(config + "purchases/purchaseOptions2.txt", "UTF-8", false);
        subPurchaseOptionsID2 = purchasesApi.addPurchaseItem(seasonID, child2, purchaseOptionsMTXID1, sessionToken);
        Assert.assertNotNull(subPurchaseOptionsID2);
        Assert.assertFalse(subPurchaseOptionsID2.contains("error"), "Purchase item was not added: " + subPurchaseOptionsID2);

        mtx = purchasesApi.getPurchaseItem(purchaseOptionsMTXID1, sessionToken);
        JSONObject json = new JSONObject(mtx);
        JSONArray children = json.getJSONArray("purchaseOptions");
        Assert.assertEquals(children.size(), 2, "Sub-purchaseOptions were not assigned to a mutually exclusive parent.");
    }

    @Test(dependsOnMethods = "addMTXPurchaseOptions", description = "Move 1 purchase options to production")
    public void movePurchaseOptionsToProd() throws Exception {
        String feature = purchasesApi.getPurchaseItem(subPurchaseOptionsID1, sessionToken);
        JSONObject json = new JSONObject(feature);
        json.put("stage", "PRODUCTION");
        String response = purchasesApi.updatePurchaseItem(seasonID, subPurchaseOptionsID1, json.toString(), sessionToken);
        Assert.assertNotNull(response);
        Assert.assertFalse(response.contains("error"), "purcahse options was not moved to prod: " + response);
    }

    @Test(dependsOnMethods = "movePurchaseOptionsToProd", description = "Add new dev inAppPurchase under MTX")
    public void addDevPurchaseOptionsToMTX() throws Exception {
        String child3 = FileUtils.fileToString(config + "purchases/purchaseOptions3.txt", "UTF-8", false);
        subPurchaseOptionsID3 = purchasesApi.addPurchaseItem(seasonID, child3, purchaseOptionsMTXID1, sessionToken);

        Assert.assertNotNull(subPurchaseOptionsID3);
        Assert.assertFalse(subPurchaseOptionsID3.contains("error"), "purcahse option was not added: " + subInAppPurID3);
    }

    @Test(dependsOnMethods = "addDevPurchaseOptionsToMTX", description = "Add new prod purchase options to MTX")
    public void addProdPurchaseOptionsToMTX() throws Exception {
        String child4 = FileUtils.fileToString(config + "purchases/purchaseOptionsProduction.txt", "UTF-8", false);
        subPurchaseOptionsID4 = purchasesApi.addPurchaseItem(seasonID, child4, purchaseOptionsMTXID1, sessionToken);

        Assert.assertNotNull(subPurchaseOptionsID4);
        Assert.assertFalse(subPurchaseOptionsID4.contains("error"), "purchase options was not added: " + subInAppPurID4);
    }

    @Test(dependsOnMethods = "addProdPurchaseOptionsToMTX", description = "Validate purchase options stage")
    public void validateStage2() throws Exception {
        String subItem1 = purchasesApi.getPurchaseItem(subPurchaseOptionsID1, sessionToken);
        Assert.assertNotNull(subItem1);
        JSONObject subItem1Json = new JSONObject(subItem1);
        Assert.assertEquals("PRODUCTION", subItem1Json.getString("stage"), "purchase options stage should be PRODUCTION but it is: " + subItem1Json.getString("stage"));

        String subItem2 = purchasesApi.getPurchaseItem(subPurchaseOptionsID2, sessionToken);
        Assert.assertNotNull(subItem2);
        JSONObject subItem2Json = new JSONObject(subItem2);
        Assert.assertEquals("DEVELOPMENT", subItem2Json.getString("stage"), "purchase options stage should be DEVELOPMENT but it is: " + subItem2Json.getString("stage"));

        String subItem3 = purchasesApi.getPurchaseItem(subPurchaseOptionsID3, sessionToken);
        Assert.assertNotNull(subItem3);
        JSONObject subItem3Json = new JSONObject(subItem3);
        Assert.assertEquals("DEVELOPMENT", subItem3Json.getString("stage"), "purchase options stage should be DEVELOPMENT but it is: " + subItem3Json.getString("stage"));

        String subItem4 = purchasesApi.getPurchaseItem(subPurchaseOptionsID4, sessionToken);
        Assert.assertNotNull(subItem4);
        JSONObject subitem4Json = new JSONObject(subItem4);
        Assert.assertEquals("PRODUCTION", subitem4Json.getString("stage"), "purchase options stage should be PRODUCTION but it is: " + subitem4Json.getString("stage"));
    }

    
    
    @Test(dependsOnMethods = "validateStage2", description = "Delete product")
    public void deleteProduct() throws Exception {
    	purchasesApi.convertAllPurchasesToDevStage(seasonID, sessionToken);
        int response = productApi.deleteProduct(productID, adminToken);
        Assert.assertEquals(response, 200, "deleteProduct failed: code " + response);
    }

}
