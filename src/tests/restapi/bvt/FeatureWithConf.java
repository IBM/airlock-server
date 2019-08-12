package tests.restapi.bvt;

import org.apache.wink.json4j.JSONObject;

import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import tests.com.ibm.qautils.FileUtils;
import tests.restapi.AirlockUtils;
import tests.restapi.FeaturesRestApi;
import tests.restapi.ProductsRestApi;

/**
 * This is end to end test that verifies next scenario:

     Create feature in development with sub feature and configuration
     Move parent feature to production
     Add new development sub feature
     Move first sub feature to production
     Move configuration to production
     Add new configuration in development under the parent feature
     Add new sub configuration in development under the first configuration
 */

public class FeatureWithConf {
    protected String seasonID;
    protected String featureID1;
    protected String subFeatureID1;
    protected String subFeatureID2;
    protected String productID;
    protected String configID1;
    protected String configID2;
    protected String configID3;
    protected String config;
    protected FeaturesRestApi featureApi;
    protected ProductsRestApi productApi;
    protected AirlockUtils baseUtils;
    protected String sessionToken = "";
    protected String adminToken;

    @BeforeClass
	@Parameters({"url", "analyticsUrl", "translationsUrl", "configPath", "sessionToken", "userName", "userPassword", "appName", "productsToDeleteFile", "adminUser", "adminPassword"})
	public void init(String url, String analyticsUrl, String translationsUrl, String configPath, String sToken, String userName, String userPassword, String appName, String productsToDeleteFile, String adminUser, String adminPassword) throws Exception{
    	
        config = configPath;
        productApi = new ProductsRestApi();
        featureApi = new FeaturesRestApi();
		baseUtils = new AirlockUtils(url, analyticsUrl, translationsUrl, configPath, sToken, userName, userPassword, appName, productsToDeleteFile);
		sessionToken = baseUtils.sessionToken;
		productApi.setURL(url);
        featureApi.setURL(url);

        adminToken = baseUtils.getJWTToken(adminUser, adminPassword, appName);
    }


    @Test(description = "Create feature in development with sub feature and configuration")
    public void addFeatureWithConf() throws Exception {
        productID = baseUtils.createProduct();
        baseUtils.printProductToFile(productID);
        seasonID = baseUtils.createSeason(productID);
        baseUtils.createSchema(seasonID);

        featureID1 = baseUtils.createFeature(seasonID);
        Assert.assertNotNull(featureID1);
        Assert.assertFalse(featureID1.contains("error"), "Feature was not added: " + featureID1);

        String feature = FileUtils.fileToString(config + "feature1.txt", "UTF-8", false);
        subFeatureID1 = featureApi.addFeature(seasonID, feature, featureID1, sessionToken);
        Assert.assertNotNull(subFeatureID1);
        Assert.assertFalse(subFeatureID1.contains("error"), "Feature was not added: " + subFeatureID1);


        String configuration = FileUtils.fileToString(config + "configuration_rule1.txt", "UTF-8", false);
        JSONObject jsonConfig = new JSONObject(configuration);
        JSONObject newConfiguration = new JSONObject();
        newConfiguration.put("color", "red");
        jsonConfig.put("configuration", newConfiguration);
        configID1 = featureApi.addFeature(seasonID, jsonConfig.toString(), featureID1, sessionToken);
        Assert.assertNotNull(configID1);
        Assert.assertFalse(configID1.contains("error"), "Configuration was not added: " + configID1);

        String feature1 = featureApi.getFeature(featureID1,sessionToken);
        Assert.assertTrue(feature1.contains("color") && feature1.contains("red"), "Feature doesn't contain configuration color:red " + feature1);
    }

    @Test(dependsOnMethods = "addFeatureWithConf", description = "Move parent feature to production")
    public void moveFeatureToProd() throws Exception {
        String feature = featureApi.getFeature(featureID1, sessionToken);
        JSONObject json = new JSONObject(feature);
        json.put("stage", "PRODUCTION");
        String response = featureApi.updateFeature(seasonID, featureID1, json.toString(), sessionToken);
        Assert.assertNotNull(response);
        Assert.assertFalse(response.contains("error"), "Feature was not moved to prod: " + response);
    }

    @Test(dependsOnMethods = "moveFeatureToProd", description = "Add new development sub feature")
    public void addNewSubFeature() throws Exception {
        String feature = FileUtils.fileToString(config + "feature2.txt", "UTF-8", false);
        subFeatureID2 = featureApi.addFeature(seasonID, feature, featureID1, sessionToken);
        Assert.assertNotNull(subFeatureID2);
        Assert.assertFalse(subFeatureID2.contains("error"), "Feature was not added: " + subFeatureID2);
    }

    @Test(dependsOnMethods = "addNewSubFeature", description = "Move first sub feature to production")
    public void moveSubFeatureToProd() throws Exception {
        String feature = featureApi.getFeature(subFeatureID1, sessionToken);
        JSONObject json = new JSONObject(feature);
        json.put("stage", "PRODUCTION");
        String response = featureApi.updateFeature(seasonID, subFeatureID1, json.toString(), sessionToken);
        Assert.assertNotNull(response);
        Assert.assertFalse(response.contains("error"), "Feature was not moved to prod: " + response);
    }

    @Test(dependsOnMethods = "moveSubFeatureToProd", description = "Move configuration to production")
    public void moveConfToProd() throws Exception {
        String feature = featureApi.getFeature(configID1, sessionToken);
        JSONObject json = new JSONObject(feature);
        json.put("stage", "PRODUCTION");
        String response = featureApi.updateFeature(seasonID, configID1, json.toString(), sessionToken);
        Assert.assertNotNull(response);
        Assert.assertFalse(response.contains("error"), "Configuration was not moved to prod: " + response);
    }

    @Test(dependsOnMethods = "moveConfToProd", description = "Add new configuration in development under the parent feature")
    public void addConfForSubFeature() throws Exception {
        String configuration = FileUtils.fileToString(config + "configuration_rule2.txt", "UTF-8", false);
        JSONObject jsonConfig = new JSONObject(configuration);
        JSONObject newConfiguration = new JSONObject();
        newConfiguration.put("color", "green");
        jsonConfig.put("configuration", newConfiguration);
        configID2 = featureApi.addFeature(seasonID, jsonConfig.toString(), featureID1, sessionToken);
        Assert.assertNotNull(configID2);
        Assert.assertFalse(configID2.contains("error"), "Configuration was not added: " + configID2);

        String feature1 = featureApi.getFeature(featureID1,sessionToken);
        Assert.assertTrue(feature1.contains("color") && feature1.contains("green"), "Feature doesn't contain configuration color:red " + feature1);
    }

    @Test(dependsOnMethods = "addConfForSubFeature", description = "Add new sub configuration in development under the first configuration")
    public void addSubConf() throws Exception {
        String configuration = FileUtils.fileToString(config + "configuration_rule3.txt", "UTF-8", false);
        JSONObject jsonConfig = new JSONObject(configuration);
        JSONObject newConfiguration = new JSONObject();
        newConfiguration.put("color", "blue");
        jsonConfig.put("configuration", newConfiguration);
        configID3 = featureApi.addFeature(seasonID, jsonConfig.toString(), configID1, sessionToken);
        Assert.assertNotNull(configID3);
        Assert.assertFalse(configID3.contains("error"), "Configuration was not added: " + configID3);

        String feature1 = featureApi.getFeature(featureID1,sessionToken);
        Assert.assertTrue(feature1.contains("color") && feature1.contains("blue"), "Feature doesn't contain configuration color:red " + feature1);
    }


    @Test(dependsOnMethods = "addSubConf", description = "Validate features stage")
    public void validateStage() throws Exception {
        String feature1 = featureApi.getFeature(featureID1, sessionToken);
        Assert.assertNotNull(feature1);
        JSONObject feature1Json = new JSONObject(feature1);
        Assert.assertEquals("PRODUCTION", feature1Json.getString("stage"), "Feature stage should be PRODUCTION but it is: " + feature1Json.getString("stage"));

        String subFeature1 = featureApi.getFeature(subFeatureID1, sessionToken);
        Assert.assertNotNull(subFeature1);
        JSONObject subFeature1Json = new JSONObject(subFeature1);
        Assert.assertEquals("PRODUCTION", subFeature1Json.getString("stage"), "Feature stage should be PRODUCTION but it is: " + subFeature1Json.getString("stage"));

        String subFeature2 = featureApi.getFeature(subFeatureID2, sessionToken);
        Assert.assertNotNull(subFeature2);
        JSONObject subFeature2Json = new JSONObject(subFeature2);
        Assert.assertEquals("DEVELOPMENT", subFeature2Json.getString("stage"), "Feature stage should be DEVELOPMENT but it is: " + subFeature2Json.getString("stage"));

        String config1 = featureApi.getFeature(configID1, sessionToken);
        Assert.assertNotNull(config1);
        JSONObject config1Json = new JSONObject(config1);
        Assert.assertEquals("PRODUCTION", config1Json.getString("stage"), "Feature stage should be PRODUCTION but it is: " + config1Json.getString("stage"));

        String config2 = featureApi.getFeature(configID2, sessionToken);
        Assert.assertNotNull(config2);
        JSONObject config2Json = new JSONObject(config2);
        Assert.assertEquals("DEVELOPMENT", config2Json.getString("stage"), "Feature stage should be DEVELOPMENT but it is: " + config2Json.getString("stage"));

        String config3 = featureApi.getFeature(configID3, sessionToken);
        Assert.assertNotNull(config3);
        JSONObject config3Json = new JSONObject(config3);
        Assert.assertEquals("DEVELOPMENT", config3Json.getString("stage"), "Feature stage should be DEVELOPMENT but it is: " + config3Json.getString("stage"));
    }

    @Test(dependsOnMethods = "validateStage", description = "Delete product")
    public void deleteProduct() throws Exception {
        featureApi.convertAllFeaturesToDevStage(seasonID, sessionToken);
        int response = productApi.deleteProduct(productID, adminToken);
        Assert.assertEquals(response, 200, "deleteProduct failed: code " + response);
    }

}
