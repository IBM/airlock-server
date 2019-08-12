package tests.restapi.bvt;

import org.apache.wink.json4j.JSONArray;

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
 * <p>
 * Create MTX  of 2 features
 * Move 1 feature to production
 * Add new dev feature under MTX
 * Add new prod feature to MTX
 */

public class MutualExclusionGroups {
    protected String seasonID;
    protected String featureID1;
    protected String subFeatureID1;
    protected String subFeatureID2;
    protected String subFeatureID3;
    protected String subFeatureID4;
    protected String productID;
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

    @Test(description = "Create MTX  of 2 features")
    public void addMTXFeatures() throws Exception {
        productID = baseUtils.createProduct();
        baseUtils.printProductToFile(productID);
        seasonID = baseUtils.createSeason(productID);
        baseUtils.createSchema(seasonID);

        String parent = FileUtils.fileToString(config + "feature-mutual.txt", "UTF-8", false);
        featureID1 = featureApi.addFeature(seasonID, parent, "ROOT", sessionToken);

        String child1 = FileUtils.fileToString(config + "feature1.txt", "UTF-8", false);
        subFeatureID1 = featureApi.addFeature(seasonID, child1, featureID1, sessionToken);
        String child2 = FileUtils.fileToString(config + "feature2.txt", "UTF-8", false);
        subFeatureID2 = featureApi.addFeature(seasonID, child2, featureID1, sessionToken);

        parent = featureApi.getFeature(featureID1, sessionToken);
        JSONObject json = new JSONObject(parent);
        JSONArray children = json.getJSONArray("features");
        Assert.assertEquals(children.size(), 2, "Sub-features were not assigned to a mutually exclusive parent.");

    }

    @Test(dependsOnMethods = "addMTXFeatures", description = "Move 1 feature to production")
    public void moveFeatureToProd() throws Exception {
        String feature = featureApi.getFeature(subFeatureID1, sessionToken);
        JSONObject json = new JSONObject(feature);
        json.put("stage", "PRODUCTION");
        String response = featureApi.updateFeature(seasonID, subFeatureID1, json.toString(), sessionToken);
        Assert.assertNotNull(response);
        Assert.assertFalse(response.contains("error"), "Feature was not moved to prod: " + response);
    }

    @Test(dependsOnMethods = "moveFeatureToProd", description = "Add new dev feature under MTX")
    public void addDevFeatureToMTX() throws Exception {
        String child3 = FileUtils.fileToString(config + "feature3.txt", "UTF-8", false);
        subFeatureID3 = featureApi.addFeature(seasonID, child3, featureID1, sessionToken);

        Assert.assertNotNull(subFeatureID3);
        Assert.assertFalse(subFeatureID3.contains("error"), "Feature was not added: " + subFeatureID3);
    }

    @Test(dependsOnMethods = "addDevFeatureToMTX", description = "Add new prod feature to MTX")
    public void addProdFeatureToMTX() throws Exception {
        String child4 = FileUtils.fileToString(config + "feature_production.txt", "UTF-8", false);
        subFeatureID4 = featureApi.addFeature(seasonID, child4, featureID1, sessionToken);

        Assert.assertNotNull(subFeatureID4);
        Assert.assertFalse(subFeatureID4.contains("error"), "Feature was not added: " + subFeatureID4);
    }

    @Test(dependsOnMethods = "addProdFeatureToMTX", description = "Validate features stage")
    public void validateStage() throws Exception {
        String subFeature1 = featureApi.getFeature(subFeatureID1, sessionToken);
        Assert.assertNotNull(subFeature1);
        JSONObject subFeature1Json = new JSONObject(subFeature1);
        Assert.assertEquals("PRODUCTION", subFeature1Json.getString("stage"), "Feature stage should be PRODUCTION but it is: " + subFeature1Json.getString("stage"));

        String subFeature2 = featureApi.getFeature(subFeatureID2, sessionToken);
        Assert.assertNotNull(subFeature2);
        JSONObject subFeature2Json = new JSONObject(subFeature2);
        Assert.assertEquals("DEVELOPMENT", subFeature2Json.getString("stage"), "Feature stage should be DEVELOPMENT but it is: " + subFeature2Json.getString("stage"));

        String subFeature3 = featureApi.getFeature(subFeatureID3, sessionToken);
        Assert.assertNotNull(subFeature3);
        JSONObject subFeature3Json = new JSONObject(subFeature3);
        Assert.assertEquals("DEVELOPMENT", subFeature3Json.getString("stage"), "Feature stage should be DEVELOPMENT but it is: " + subFeature3Json.getString("stage"));

        String subFeature4 = featureApi.getFeature(subFeatureID4, sessionToken);
        Assert.assertNotNull(subFeature4);
        JSONObject subFeature4Json = new JSONObject(subFeature4);
        Assert.assertEquals("PRODUCTION", subFeature4Json.getString("stage"), "Feature stage should be PRODUCTION but it is: " + subFeature4Json.getString("stage"));
    }

    @Test(dependsOnMethods = "validateStage", description = "Delete product")
    public void deleteProduct() throws Exception {
        featureApi.convertAllFeaturesToDevStage(seasonID, sessionToken);
        int response = productApi.deleteProduct(productID, adminToken);
        Assert.assertEquals(response, 200, "deleteProduct failed: code " + response);
    }

}
