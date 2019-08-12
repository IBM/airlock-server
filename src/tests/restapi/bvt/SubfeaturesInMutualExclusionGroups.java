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

     Create feature in development
     Add MTX of 2 features under the parent feature
     Move parent to production
     Move both MTX features to production
     Add new MTX group of development features under the parent feature
 */

public class SubfeaturesInMutualExclusionGroups {
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


    @Test(description = "Create feature in development")
    public void addFeature() throws Exception {
        productID = baseUtils.createProduct();
        baseUtils.printProductToFile(productID);
        seasonID = baseUtils.createSeason(productID);
        baseUtils.createSchema(seasonID);
        featureID1 = baseUtils.createFeature(seasonID);

        Assert.assertNotNull(featureID1);
        Assert.assertFalse(featureID1.contains("error"), "Feature was not added: " + featureID1);
    }

    @Test(dependsOnMethods = "addFeature", description = "Add MTX of 2 features under the parent feature")
    public void addMtxOfFutures() throws Exception {
        String parent = FileUtils.fileToString(config + "feature-mutual.txt", "UTF-8", false);
        String featureMTX = featureApi.addFeature(seasonID, parent, "ROOT", sessionToken);

        String child1 = FileUtils.fileToString(config + "feature1.txt", "UTF-8", false);
        subFeatureID1 = featureApi.addFeature(seasonID, child1, featureMTX, sessionToken);
        Assert.assertNotNull(subFeatureID1);
        Assert.assertFalse(subFeatureID1.contains("error"), "Feature was not added: " + subFeatureID1);

        String child2 = FileUtils.fileToString(config + "feature2.txt", "UTF-8", false);
        subFeatureID2 = featureApi.addFeature(seasonID, child2, featureMTX, sessionToken);
        Assert.assertNotNull(subFeatureID2);
        Assert.assertFalse(subFeatureID2.contains("error"), "Feature was not added: " + subFeatureID2);

        parent = featureApi.getFeature(featureMTX, sessionToken);
        JSONObject json = new JSONObject(parent);
        JSONArray children = json.getJSONArray("features");
        Assert.assertEquals(children.size(), 2, "Sub-features were not assigned to a mutually exclusive parent.");
    }

    @Test(dependsOnMethods = "addMtxOfFutures", description = "Move parent to production")
    public void moveParentToProd() throws Exception {
        String feature = featureApi.getFeature(featureID1, sessionToken);
        JSONObject json = new JSONObject(feature);
        json.put("stage", "PRODUCTION");
        String response = featureApi.updateFeature(seasonID, featureID1, json.toString(), sessionToken);
        Assert.assertNotNull(response);
        Assert.assertFalse(response.contains("error"), "Feature was not moved to prod: " + response);
    }

    @Test(dependsOnMethods = "moveParentToProd", description = "Move both MTX features to production")
    public void moveBothMtxFeaturesToProd() throws Exception {
        String feature1 = featureApi.getFeature(subFeatureID1, sessionToken);
        JSONObject json1 = new JSONObject(feature1);
        json1.put("stage", "PRODUCTION");
        String response1 = featureApi.updateFeature(seasonID, subFeatureID1, json1.toString(), sessionToken);
        Assert.assertNotNull(response1);
        Assert.assertFalse(response1.contains("error"), "Feature was not moved to prod: " + response1);

        String feature2 = featureApi.getFeature(subFeatureID2, sessionToken);
        JSONObject json2 = new JSONObject(feature2);
        json2.put("stage", "PRODUCTION");
        String response2 = featureApi.updateFeature(seasonID, subFeatureID2, json2.toString(), sessionToken);
        Assert.assertNotNull(response2);
        Assert.assertFalse(response2.contains("error"), "Feature was not moved to prod: " + response2);
    }

    @Test(dependsOnMethods = "moveBothMtxFeaturesToProd", description = "Add new MTX group of development features under the parent feature")
    public void addNewMtxGroupToDev() throws Exception {
        String parent = FileUtils.fileToString(config + "feature-mutual.txt", "UTF-8", false);
        String featureMTX = featureApi.addFeature(seasonID, parent, "ROOT", sessionToken);

        String child1 = FileUtils.fileToString(config + "feature3.txt", "UTF-8", false);
        subFeatureID3 = featureApi.addFeature(seasonID, child1, featureMTX, sessionToken);
        Assert.assertNotNull(subFeatureID3);
        Assert.assertFalse(subFeatureID3.contains("error"), "Feature was not added: " + subFeatureID3);

        String child2 = FileUtils.fileToString(config + "feature4.txt", "UTF-8", false);
        subFeatureID4 = featureApi.addFeature(seasonID, child2, featureMTX, sessionToken);
        Assert.assertNotNull(subFeatureID4);
        Assert.assertFalse(subFeatureID4.contains("error"), "Feature was not added: " + subFeatureID4);

        parent = featureApi.getFeature(featureMTX, sessionToken);
        JSONObject json = new JSONObject(parent);
        JSONArray children = json.getJSONArray("features");
        Assert.assertEquals(children.size(), 2, "Sub-features were not assigned to a mutually exclusive parent.");
    }

    @Test(dependsOnMethods = "addNewMtxGroupToDev", description = "Validate features stage")
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
        Assert.assertEquals("PRODUCTION", subFeature2Json.getString("stage"), "Feature stage should be PRODUCTION but it is: " + subFeature2Json.getString("stage"));

        String subFeature3 = featureApi.getFeature(subFeatureID3, sessionToken);
        Assert.assertNotNull(subFeature3);
        JSONObject subFeature3Json = new JSONObject(subFeature3);
        Assert.assertEquals("DEVELOPMENT", subFeature3Json.getString("stage"), "Feature stage should be DEVELOPMENT but it is: " + subFeature3Json.getString("stage"));

        String subFeature4 = featureApi.getFeature(subFeatureID4, sessionToken);
        Assert.assertNotNull(subFeature4);
        JSONObject subFeature4Json = new JSONObject(subFeature4);
        Assert.assertEquals("DEVELOPMENT", subFeature4Json.getString("stage"), "Feature stage should be DEVELOPMENT but it is: " + subFeature4Json.getString("stage"));
    }

    @Test(dependsOnMethods = "validateStage", description = "Delete product")
    public void deleteProduct() throws Exception {
        featureApi.convertAllFeaturesToDevStage(seasonID, sessionToken);
        int response = productApi.deleteProduct(productID, adminToken);
        Assert.assertEquals(response, 200, "deleteProduct failed: code " + response);
    }

}
