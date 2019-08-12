//package tests.restapi.scenarios.notificationMail;
package tests.restapi.known_issues;

import com.ibm.airlock.Constants;


import org.apache.wink.json4j.JSONObject;
import org.testng.Assert;
import org.testng.annotations.*;

import tests.com.ibm.qautils.FileUtils;
import tests.restapi.AirlockUtils;
import tests.restapi.FeaturesRestApi;
import tests.restapi.ProductsRestApi;

/**
 * Created by amitaim on 12/03/2017.
 */
public class Notification {
    private String sessionToken = "";
    private String adminToken = "";
    private String operationsUrl;
    private String productID;
    private String seasonID;
    private String featureID;
    private String configID;
    private ProductsRestApi productsApi;
    private FeaturesRestApi featuresApi;
    private AirlockUtils baseUtils;
    protected String m_url;
    private String m_appName = "backend_dev";

    @BeforeClass
	@Parameters({"url", "analyticsUrl", "translationsUrl", "configPath", "sessionToken", "userName", "userPassword", "appName", "adminUser", "adminPassword", "productsToDeleteFile"})
	public void init(String url, String analyticsUrl, String translationsUrl, String configPath, String sToken, String userName, String userPassword, String appName, String adminUser, String adminPassword, String productsToDeleteFile) throws Exception{

         m_url = url;
        if(appName != null){
            m_appName = appName;
        }
        baseUtils = new AirlockUtils(url, analyticsUrl, translationsUrl, configPath, sToken, userName, userPassword, appName, productsToDeleteFile);
        adminToken = baseUtils.getJWTToken(adminUser, adminPassword,m_appName);
        sessionToken = baseUtils.getJWTToken(userName, userPassword,m_appName);
        baseUtils.setSessionToken(adminToken);
        productID = baseUtils.createProduct();
		baseUtils.printProductToFile(productID);
        seasonID = baseUtils.createSeason(productID);
        featureID = baseUtils.createFeature(seasonID);
        productsApi = new ProductsRestApi();
        productsApi.setURL(m_url);
        featuresApi = new FeaturesRestApi();
        featuresApi.setURL(m_url);
        String configuration = FileUtils.fileToString(configPath + "configuration_rule1.txt", "UTF-8", false);
        JSONObject jsonConfig = new JSONObject(configuration);
        JSONObject newConfiguration = new JSONObject();
        jsonConfig.put("configuration", newConfiguration);
        configID =featuresApi.addFeature(seasonID, jsonConfig.toString(), featureID, adminToken);
    }

    @Test(description = "test product apis")
    public void testNotificationProductApi() {
        try {
            // follow product
            String response = productsApi.followProduct(productID, adminToken);
            Assert.assertTrue(response.equals(""), "failed to follow");
            //get and see it is followed and current user is following
            response = productsApi.getProductFollowers(productID, adminToken);
            JSONObject responseJson = new JSONObject(response);
            String followers = responseJson.get(Constants.JSON_FEATURE_FIELD_FOLLOWERS).toString();
            Assert.assertTrue(followers.contains("user4@weather.com"), "failed to follow");
            Assert.assertTrue(responseJson.getBoolean(Constants.JSON_PRODUCT_FIELD_IS_FOLLOWING), "failed to follow");
            //switch to jer3
            //add it
            response = productsApi.followProduct(productID, sessionToken);
            Assert.assertTrue(response.equals(""), "failed to follow");
            //see added
            response = productsApi.getProductFollowers(productID, sessionToken);
            responseJson = new JSONObject(response);
            followers = responseJson.get(Constants.JSON_FEATURE_FIELD_FOLLOWERS).toString();
            Assert.assertTrue(followers.contains("user4@weather.com") && followers.contains("user3@weather.com"), "failed to follow");
            Assert.assertTrue(responseJson.getBoolean(Constants.JSON_PRODUCT_FIELD_IS_FOLLOWING), "failed to follow");
            //remove jer3
            int responseCode = productsApi.unfollowProduct(productID, sessionToken);
            Assert.assertTrue(responseCode==200, "failed to unfollow");
            //see that it is delete but not jer4
            response = productsApi.getProductFollowers(productID, sessionToken);
            responseJson = new JSONObject(response);
            followers = responseJson.get(Constants.JSON_FEATURE_FIELD_FOLLOWERS).toString();
            Assert.assertTrue(followers.contains("user4@weather.com") && !followers.contains("user3@weather.com"), "failed to follow");
            Assert.assertTrue(!responseJson.getBoolean(Constants.JSON_PRODUCT_FIELD_IS_FOLLOWING), "failed to follow");

        } catch (Exception e) {
            Assert.fail("follow product api test failed");
        }
    }

    @Test(dependsOnMethods="testNotificationProductApi",description = "test feature apis")
    public void testNotificationFeatureApi() {
        try {
            // follow product
            String response = featuresApi.followFeature(featureID, adminToken);
            Assert.assertTrue(response.equals(""), "failed to follow");
            //get and see it is followed and current user is following
            response = featuresApi.getFeatureFollowers(featureID, adminToken);
            JSONObject responseJson = new JSONObject(response);
            String followers = responseJson.get(Constants.JSON_FEATURE_FIELD_FOLLOWERS).toString();
            Assert.assertTrue(followers.contains("user4@weather.com"), "failed to follow");
            Assert.assertTrue(responseJson.getBoolean(Constants.JSON_PRODUCT_FIELD_IS_FOLLOWING), "failed to follow");
            //switch to jer3
            //add it
            response = featuresApi.followFeature(featureID, sessionToken);
            Assert.assertTrue(response.equals(""), "failed to follow");
            //see added
            response = featuresApi.getFeatureFollowers(featureID, sessionToken);
            responseJson = new JSONObject(response);
            followers = responseJson.get(Constants.JSON_FEATURE_FIELD_FOLLOWERS).toString();
            Assert.assertTrue(followers.contains("user4@weather.com") && followers.contains("user3@weather.com"), "failed to follow");
            Assert.assertTrue(responseJson.getBoolean(Constants.JSON_PRODUCT_FIELD_IS_FOLLOWING), "failed to follow");
            //remove jer3
            int responseCode = featuresApi.unfollowFeature(featureID, sessionToken);
            Assert.assertTrue(responseCode==200, "failed to unfollow");
            //see that it is delete but not jer4
            response = featuresApi.getFeatureFollowers(featureID, sessionToken);
            responseJson = new JSONObject(response);
            followers = responseJson.get(Constants.JSON_FEATURE_FIELD_FOLLOWERS).toString();
            Assert.assertTrue(followers.contains("user4@weather.com") && !followers.contains("user3@weather.com"), "failed to follow");
            Assert.assertTrue(!responseJson.getBoolean(Constants.JSON_PRODUCT_FIELD_IS_FOLLOWING), "failed to follow");

        } catch (Exception e) {
            Assert.fail("follow product api test failed");
        }
    }

    @Test(dependsOnMethods="testNotificationFeatureApi",description = "test feature apis")
    public void negativeTests() {
        String response =  featuresApi.getFeatureFollowers(configID,adminToken);
        Assert.assertTrue(response.contains("error"),"negative failed");
        response =  featuresApi.followFeature(configID,adminToken);
        Assert.assertTrue(response.contains("error"),"negative failed");
        int responseCode =  featuresApi.unfollowFeature(configID,adminToken);
        Assert.assertTrue(responseCode!=200,"negative failed");
        int res =  productsApi.deleteProduct(productID,adminToken);
        // bad id
        response = featuresApi.getFeatureFollowers("a", adminToken);
        Assert.assertTrue(response.contains("error"),"negative failed");
        //missing product
        response =featuresApi.getFeatureFollowers(featureID,adminToken);
        Assert.assertTrue(response.contains("error"),"negative failed");
        response =featuresApi.followFeature("a",adminToken);
        Assert.assertTrue(response.contains("error"),"negative failed");
        response =featuresApi.followFeature(featureID,adminToken);
        Assert.assertTrue(response.contains("error"),"negative failed");
        responseCode =featuresApi.unfollowFeature("a",adminToken);
        Assert.assertTrue(responseCode!=200,"negative failed");
        responseCode =featuresApi.unfollowFeature(featureID,adminToken);
        Assert.assertTrue(responseCode!=200,"negative failed");
        // bad id
        response = productsApi.getProductFollowers("a", adminToken);
        Assert.assertTrue(response.contains("error"),"negative failed");
        //missing product
        response =productsApi.getProductFollowers(productID,adminToken);
        Assert.assertTrue(response.contains("error"),"negative failed");
        response =productsApi.followProduct("a",adminToken);
        Assert.assertTrue(response.contains("error"),"negative failed");
        response =productsApi.followProduct(productID,adminToken);
        Assert.assertTrue(response.contains("error"),"negative failed");
        responseCode =productsApi.unfollowProduct("a",adminToken);
        Assert.assertTrue(responseCode!=200,"negative failed");
        responseCode =productsApi.unfollowProduct(productID,adminToken);
        Assert.assertTrue(responseCode!=200,"negative failed");
    }

    // @Test(description = "test notification")
    public void testNotification() throws Exception{

        // 6 api test add/delete/get followers for product/features
        //add product
        // follow the product
       //add season -> get mail
        //Switch user
        // add feature1 in DEV -> no mail
        // follow  it.
        //add subfeature1 in Dev -> get mail for feature1
        // follow  subfeature1
        // TODO: modify roolout -> get mail for feature1 and for subfeature1 with cause
        //add subfeature 2 -> get mail feature1
        //follow subfeature2
        // reorder sub1 and sub 2 -> get mail for f1 and for each sub1 and sub2 with cause
        //other change from the father???
        // add config rule to sub2 -> get email to sub2 followers
        // modify config rule -> get email to sub2 followers
        // delete config rule -> get email to sub2 followers
        // unfollow sub2
        //modify sub2 -> no mail
        //upgrade f1 to prod : get prod email to f1 suscribes AND product followers
        //downgrade f1 from prod : get prod email to f1 followers AND product followers
        //delete f1 -> get email for each feature and subfeature(without cause)
        //add f2 in prod. -> prod email for product followers
        //follow  f2
        // modify f2 -> prod email to f2 and product followers
        // downgrade f2 ->mail
        // delete f2 -> no mail
        //add f3 in dev  -> no email
        //follow f3
        //copy season
        // check something
        //copy feature?
        // delete season -> email for season to prod followers and email for deleted feature to f3 followers
        // delete product -> email to prod followers
    }


    @AfterTest
    private void reset(){
    	baseUtils.reset(productID, sessionToken);
    }
}
