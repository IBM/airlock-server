package tests.restapi.bvt;

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

     Create inAppPurchase in development with sub purchaseOptions and configuration
     Move parent inAppPurchase to production
     Add new development sub purchaseOptions
     Move first sub purchaseOptions to production
     Move configuration to production
     Add new configuration in development under the parent inAppPurchase
     Add new sub configuration in development under the first configuration
 */

public class PurchasesWithConf {
    protected String seasonID;
    protected String inAppPurchaseID1;
    protected String subPurchaseOptionsID1;
    protected String subPurchaseOptionsID2;
    protected String productID;
    protected String configID1;
    protected String configID2;
    protected String configID3;
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


    @Test(description = "Create inAppPurchase in development with sub purchaseOptions and configuration")
    public void addInAppPurchaseWithConf() throws Exception {
        productID = baseUtils.createProduct();
        baseUtils.printProductToFile(productID);
        seasonID = baseUtils.createSeason(productID);
        baseUtils.createSchema(seasonID);

        inAppPurchaseID1 = baseUtils.createInAppPurchase(seasonID);
        Assert.assertNotNull(inAppPurchaseID1);
        Assert.assertFalse(inAppPurchaseID1.contains("error"), "InAppPurcahse was not added: " + inAppPurchaseID1);

        String purchaseOrder = FileUtils.fileToString(config + "purchases/purchaseOptions1.txt", "UTF-8", false);
        subPurchaseOptionsID1 = purchasesApi.addPurchaseItem(seasonID, purchaseOrder, inAppPurchaseID1, sessionToken);
        Assert.assertNotNull(subPurchaseOptionsID1);
        Assert.assertFalse(subPurchaseOptionsID1.contains("error"), "purchaseOrder was not added: " + subPurchaseOptionsID1);


        String configuration = FileUtils.fileToString(config + "configuration_rule1.txt", "UTF-8", false);
        JSONObject jsonConfig = new JSONObject(configuration);
        JSONObject newConfiguration = new JSONObject();
        newConfiguration.put("color", "yellow");
        jsonConfig.put("configuration", newConfiguration);
        configID1 = purchasesApi.addPurchaseItem(seasonID, jsonConfig.toString(), inAppPurchaseID1, sessionToken);
        Assert.assertNotNull(configID1);
        Assert.assertFalse(configID1.contains("error"), "Configuration was not added: " + configID1);

        String feature1 = purchasesApi.getPurchaseItem(inAppPurchaseID1, sessionToken);
        Assert.assertTrue(feature1.contains("color") && feature1.contains("yellow"), "inAppPurchase doesn't contain configuration color:yellow " + feature1);
    }

    @Test(dependsOnMethods = "addInAppPurchaseWithConf", description = "Move parent inAppPurchase to production")
    public void moveFeatureToProd() throws Exception {
        String inAppPurchase = purchasesApi.getPurchaseItem(inAppPurchaseID1, sessionToken);
        JSONObject json = new JSONObject(inAppPurchase);
        json.put("stage", "PRODUCTION");
        String response = purchasesApi.updatePurchaseItem(seasonID, inAppPurchaseID1, json.toString(), sessionToken);
        Assert.assertNotNull(response);
        Assert.assertFalse(response.contains("error"), "inAppPurchase was not moved to prod: " + response);
    }

    @Test(dependsOnMethods = "moveFeatureToProd", description = "Add new development sub purchase option")
    public void addNewSubPurchaseOption() throws Exception {
        String feature = FileUtils.fileToString(config + "purchases/purchaseOptions2.txt", "UTF-8", false);
        subPurchaseOptionsID2 = purchasesApi.addPurchaseItem(seasonID, feature, inAppPurchaseID1, sessionToken);
        Assert.assertNotNull(subPurchaseOptionsID2);
        Assert.assertFalse(subPurchaseOptionsID2.contains("error"), "purchase options was not added: " + subPurchaseOptionsID2);
    }

    @Test(dependsOnMethods = "addNewSubPurchaseOption", description = "Move first sub purcahse options to production")
    public void moveSubPurchaseOptionsToProd() throws Exception {
        String feature = purchasesApi.getPurchaseItem(subPurchaseOptionsID1, sessionToken);
        JSONObject json = new JSONObject(feature);
        json.put("stage", "PRODUCTION");
        String response = purchasesApi.updatePurchaseItem(seasonID, subPurchaseOptionsID1, json.toString(), sessionToken);
        Assert.assertNotNull(response);
        Assert.assertFalse(response.contains("error"), "Purchase options was not moved to prod: " + response);
    }

    @Test(dependsOnMethods = "moveSubPurchaseOptionsToProd", description = "Move configuration to production")
    public void moveConfToProd() throws Exception {
        String feature = purchasesApi.getPurchaseItem(configID1, sessionToken);
        JSONObject json = new JSONObject(feature);
        json.put("stage", "PRODUCTION");
        String response = purchasesApi.updatePurchaseItem(seasonID, configID1, json.toString(), sessionToken);
        Assert.assertNotNull(response);
        Assert.assertFalse(response.contains("error"), "Configuration was not moved to prod: " + response);
    }

    @Test(dependsOnMethods = "moveConfToProd", description = "Add new configuration in development under the first purchaseOptions")
    public void addConfForSubPurchaseOptions() throws Exception {
        String configuration = FileUtils.fileToString(config + "configuration_rule2.txt", "UTF-8", false);
        JSONObject jsonConfig = new JSONObject(configuration);
        JSONObject newConfiguration = new JSONObject();
        newConfiguration.put("color", "green");
        jsonConfig.put("configuration", newConfiguration);
        configID2 = purchasesApi.addPurchaseItem(seasonID, jsonConfig.toString(), subPurchaseOptionsID1, sessionToken);
        Assert.assertNotNull(configID2);
        Assert.assertFalse(configID2.contains("error"), "Configuration was not added: " + configID2);

        String feature1 = purchasesApi.getPurchaseItem(subPurchaseOptionsID1, sessionToken);
        Assert.assertTrue(feature1.contains("color") && feature1.contains("green"), "InAppPurchase doesn't contain configuration color:red " + feature1);
    }

    @Test(dependsOnMethods = "addConfForSubPurchaseOptions", description = "Add new sub configuration in development under the first configuration")
    public void addSubConf() throws Exception {
        String configuration = FileUtils.fileToString(config + "configuration_rule3.txt", "UTF-8", false);
        JSONObject jsonConfig = new JSONObject(configuration);
        JSONObject newConfiguration = new JSONObject();
        newConfiguration.put("color", "blue");
        jsonConfig.put("configuration", newConfiguration);
        configID3 = purchasesApi.addPurchaseItem(seasonID, jsonConfig.toString(), configID1, sessionToken);
        Assert.assertNotNull(configID3);
        Assert.assertFalse(configID3.contains("error"), "Configuration was not added: " + configID3);

        String feature1 = purchasesApi.getPurchaseItem(inAppPurchaseID1,sessionToken);
        Assert.assertTrue(feature1.contains("color") && feature1.contains("blue"), "Feature doesn't contain configuration color:red " + feature1);
    }


    @Test(dependsOnMethods = "addSubConf", description = "Validate features stage")
    public void validateStage() throws Exception {
        String feature1 = purchasesApi.getPurchaseItem(inAppPurchaseID1, sessionToken);
        Assert.assertNotNull(feature1);
        JSONObject feature1Json = new JSONObject(feature1);
        Assert.assertEquals("PRODUCTION", feature1Json.getString("stage"), "Feature stage should be PRODUCTION but it is: " + feature1Json.getString("stage"));

        String subFeature1 = purchasesApi.getPurchaseItem(subPurchaseOptionsID1, sessionToken);
        Assert.assertNotNull(subFeature1);
        JSONObject subFeature1Json = new JSONObject(subFeature1);
        Assert.assertEquals("PRODUCTION", subFeature1Json.getString("stage"), "Feature stage should be PRODUCTION but it is: " + subFeature1Json.getString("stage"));

        String subFeature2 = purchasesApi.getPurchaseItem(subPurchaseOptionsID2, sessionToken);
        Assert.assertNotNull(subFeature2);
        JSONObject subFeature2Json = new JSONObject(subFeature2);
        Assert.assertEquals("DEVELOPMENT", subFeature2Json.getString("stage"), "Feature stage should be DEVELOPMENT but it is: " + subFeature2Json.getString("stage"));

        String config1 = purchasesApi.getPurchaseItem(configID1, sessionToken);
        Assert.assertNotNull(config1);
        JSONObject config1Json = new JSONObject(config1);
        Assert.assertEquals("PRODUCTION", config1Json.getString("stage"), "Feature stage should be PRODUCTION but it is: " + config1Json.getString("stage"));

        String config2 = purchasesApi.getPurchaseItem(configID2, sessionToken);
        Assert.assertNotNull(config2);
        JSONObject config2Json = new JSONObject(config2);
        Assert.assertEquals("DEVELOPMENT", config2Json.getString("stage"), "Feature stage should be DEVELOPMENT but it is: " + config2Json.getString("stage"));

        String config3 = purchasesApi.getPurchaseItem(configID3, sessionToken);
        Assert.assertNotNull(config3);
        JSONObject config3Json = new JSONObject(config3);
        Assert.assertEquals("DEVELOPMENT", config3Json.getString("stage"), "Feature stage should be DEVELOPMENT but it is: " + config3Json.getString("stage"));
    }

    @Test(dependsOnMethods = "validateStage", description = "Delete product")
    public void deleteProduct() throws Exception {
    		purchasesApi.convertAllPurchasesToDevStage(seasonID, sessionToken);
        int response = productApi.deleteProduct(productID, adminToken);
        Assert.assertEquals(response, 200, "deleteProduct failed: code " + response);
    }

}
