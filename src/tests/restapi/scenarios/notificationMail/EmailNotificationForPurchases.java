package tests.restapi.scenarios.notificationMail;

import com.ibm.airlock.Constants;

import java.io.IOException;

import org.apache.wink.json4j.JSONException;
import org.apache.wink.json4j.JSONObject;
import org.testng.Assert;
import org.testng.annotations.*;

import tests.com.ibm.qautils.FileUtils;
import tests.restapi.AirlockUtils;
import tests.restapi.FeaturesRestApi;
import tests.restapi.InAppPurchasesRestApi;
import tests.restapi.ProductsRestApi;

public class EmailNotificationForPurchases {
    private String sessionToken = "";
    private String adminToken = "";
    private String productID;
    private String seasonID;
    private String entitlementID;
    private String purchaseOptionsID;
    private String configID1;
    private String configID2;
    private ProductsRestApi productsApi;
    private InAppPurchasesRestApi purchasesApi;
	private FeaturesRestApi featuresApi;
    private AirlockUtils baseUtils;
    protected String m_url;
    private String admin2follow;
    private String prodLead2follow;
    private String filePath;

    @BeforeClass
	@Parameters({"url", "analyticsUrl", "translationsUrl", "configPath", "sessionToken", "userName", "userPassword", "appName", "adminUser", "adminPassword", "productLeadName", "productLeadPassword", "productsToDeleteFile"})
	public void init(String url, String analyticsUrl, String translationsUrl, String configPath, String sToken, String userName, String userPassword, String appName, String adminUser, String adminPassword, String productLeadName, String productLeadPassword, String productsToDeleteFile) throws Exception{

         m_url = url;
        admin2follow = adminUser;
        prodLead2follow = productLeadName;
        baseUtils = new AirlockUtils(url, analyticsUrl, translationsUrl, configPath, sToken, userName, userPassword, appName, productsToDeleteFile);
        adminToken = baseUtils.setNewJWTToken(adminUser, adminPassword,appName);
        sessionToken = baseUtils.setNewJWTToken(productLeadName, productLeadPassword,appName);
        baseUtils.setSessionToken(adminToken);
        productID = baseUtils.createProduct();
		baseUtils.printProductToFile(productID);
        seasonID = baseUtils.createSeason(productID);
        productsApi = new ProductsRestApi();
        productsApi.setURL(m_url);
        featuresApi = new FeaturesRestApi();
        featuresApi.setURL(m_url);
        purchasesApi = new InAppPurchasesRestApi();
		purchasesApi.setURL(m_url);
        
        filePath = configPath;
    }

    @Test (description ="Add inAppPurchase to master") 
	public void addPurchaseItems () throws IOException, JSONException, InterruptedException {
		JSONObject entitlementObj = new JSONObject(FileUtils.fileToString(filePath + "purchases/inAppPurchase1.txt", "UTF-8", false));
		entitlementObj.put("name", "entitlement1");
		entitlementID = purchasesApi.addPurchaseItem(seasonID, entitlementObj.toString(), "ROOT", sessionToken);
		Assert.assertFalse(entitlementID.contains("error"), "entitlement was not added: " + entitlementID);
		
        String configuration = FileUtils.fileToString(filePath + "configuration_rule1.txt", "UTF-8", false);
        JSONObject jsonConfig = new JSONObject(configuration);
        JSONObject newConfiguration = new JSONObject();
        jsonConfig.put("configuration", newConfiguration);
        jsonConfig.put("name", "CR1");
        configID1 = purchasesApi.addPurchaseItem(seasonID, jsonConfig.toString(), entitlementID, adminToken);
        Assert.assertFalse(configID1.contains("error"), "config was not added: " + configID1);
		
        JSONObject purOptJson = new JSONObject(FileUtils.fileToString(filePath + "purchases/purchaseOptions1.txt", "UTF-8", false));
        purOptJson.put("name", "PO1");
		purchaseOptionsID = purchasesApi.addPurchaseItem(seasonID, purOptJson.toString(), entitlementID, sessionToken);
		Assert.assertFalse(purchaseOptionsID.contains("error"), "entitlement was not added to the season: " + purchaseOptionsID);
		
		jsonConfig.put("name", "CR2");
        configID2 = purchasesApi.addPurchaseItem(seasonID, jsonConfig.toString(), purchaseOptionsID, adminToken);
        Assert.assertFalse(configID2.contains("error"), "config was not added: " + configID2);
	}
    
    @Test(dependsOnMethods = "addPurchaseItems", description = "test product apis")
    public void testNotificationProductApi() {
        try {
            // follow product
            String response = productsApi.followProduct(productID, adminToken);
            Assert.assertTrue(response.equals(""), "failed to follow: " + response);
            //get and see it is followed and current user is following
            response = productsApi.getProductFollowers(productID, adminToken);
            JSONObject responseJson = new JSONObject(response);
            String followers = responseJson.get(Constants.JSON_FEATURE_FIELD_FOLLOWERS).toString();
            Assert.assertTrue(followers.contains(admin2follow), "failed to follow");
            Assert.assertTrue(responseJson.getBoolean(Constants.JSON_PRODUCT_FIELD_IS_FOLLOWING), "failed to follow");
            //switch to jer3
            //add it
            response = productsApi.followProduct(productID, sessionToken);
            Assert.assertTrue(response.equals(""), "failed to follow");
            //see added
            response = productsApi.getProductFollowers(productID, sessionToken);
            responseJson = new JSONObject(response);
            followers = responseJson.get(Constants.JSON_FEATURE_FIELD_FOLLOWERS).toString();
            Assert.assertTrue(followers.contains(admin2follow) && followers.contains(prodLead2follow), "failed to follow");
            Assert.assertTrue(responseJson.getBoolean(Constants.JSON_PRODUCT_FIELD_IS_FOLLOWING), "failed to follow");
            //remove jer3
            int responseCode = productsApi.unfollowProduct(productID, sessionToken);
            Assert.assertTrue(responseCode==200, "failed to unfollow");
            //see that it is delete but not jer4
            response = productsApi.getProductFollowers(productID, sessionToken);
            responseJson = new JSONObject(response);
            followers = responseJson.get(Constants.JSON_FEATURE_FIELD_FOLLOWERS).toString();
            Assert.assertTrue(followers.contains(admin2follow) && !followers.contains(prodLead2follow), "failed to follow");
            Assert.assertTrue(!responseJson.getBoolean(Constants.JSON_PRODUCT_FIELD_IS_FOLLOWING), "failed to follow");

        } catch (Exception e) {
            Assert.fail("follow product api test failed");
        }
    }

    @Test(dependsOnMethods="testNotificationProductApi",description = "test entitlement apis")
    public void testNotificationEntitlement() {
        try {
            String response = featuresApi.followFeature(entitlementID, adminToken);
            Assert.assertFalse(response.contains("error"), "failed to follow : " + response);
          
            //get and see it is followed and current user is following
            response = featuresApi.getFeatureFollowers(entitlementID, adminToken);
            JSONObject responseJson = new JSONObject(response);
            String followers = responseJson.get(Constants.JSON_FEATURE_FIELD_FOLLOWERS).toString();
            Assert.assertTrue(followers.contains(admin2follow), "failed to follow");
            Assert.assertTrue(responseJson.getBoolean(Constants.JSON_PRODUCT_FIELD_IS_FOLLOWING), "failed to follow");
            //switch to jer3
            //add it
            response = featuresApi.followFeature(entitlementID, sessionToken);
            Assert.assertFalse(response.contains("error"), "failed to follow : " + response);
            
            //see added
            response = featuresApi.getFeatureFollowers(entitlementID, sessionToken);
            responseJson = new JSONObject(response);
            followers = responseJson.get(Constants.JSON_FEATURE_FIELD_FOLLOWERS).toString();
            Assert.assertTrue(followers.contains(admin2follow) && followers.contains(prodLead2follow), "failed to follow");
            Assert.assertTrue(responseJson.getBoolean(Constants.JSON_PRODUCT_FIELD_IS_FOLLOWING), "failed to follow");
            //remove jer3
            int responseCode = featuresApi.unfollowFeature(entitlementID, sessionToken);
            Assert.assertTrue(responseCode==200, "failed to unfollow");
            //see that it is delete but not jer4
            response = featuresApi.getFeatureFollowers(entitlementID, sessionToken);
            responseJson = new JSONObject(response);
            followers = responseJson.get(Constants.JSON_FEATURE_FIELD_FOLLOWERS).toString();
            Assert.assertTrue(followers.contains(admin2follow) && !followers.contains(prodLead2follow), "failed to follow");
            Assert.assertTrue(!responseJson.getBoolean(Constants.JSON_PRODUCT_FIELD_IS_FOLLOWING), "failed to follow");

        } catch (Exception e) {
            Assert.fail("follow product api test failed");
        }
    }

    @Test(dependsOnMethods="testNotificationEntitlement",description = "test purchaseOptions apis")
    public void testNotificationPurchaseOptions() {
        try {
            String response = featuresApi.followFeature(purchaseOptionsID, adminToken);
            Assert.assertFalse(response.contains("error"), "failed to follow : " + response);
            
            //get and see it is followed and current user is following
            response = featuresApi.getFeatureFollowers(purchaseOptionsID, adminToken);
            JSONObject responseJson = new JSONObject(response);
            String followers = responseJson.get(Constants.JSON_FEATURE_FIELD_FOLLOWERS).toString();
            Assert.assertTrue(followers.contains(admin2follow), "failed to follow");
            Assert.assertTrue(responseJson.getBoolean(Constants.JSON_PRODUCT_FIELD_IS_FOLLOWING), "failed to follow");
            //switch to jer3
            //add it
            response = featuresApi.followFeature(purchaseOptionsID, sessionToken);
            Assert.assertFalse(response.contains("error"), "failed to follow : " + response);
            
            //see added
            response = featuresApi.getFeatureFollowers(purchaseOptionsID, sessionToken);
            responseJson = new JSONObject(response);
            followers = responseJson.get(Constants.JSON_FEATURE_FIELD_FOLLOWERS).toString();
            Assert.assertTrue(followers.contains(admin2follow) && followers.contains(prodLead2follow), "failed to follow");
            Assert.assertTrue(responseJson.getBoolean(Constants.JSON_PRODUCT_FIELD_IS_FOLLOWING), "failed to follow");
            //remove jer3
            int responseCode = featuresApi.unfollowFeature(purchaseOptionsID, sessionToken);
            Assert.assertTrue(responseCode==200, "failed to unfollow");
            //see that it is delete but not jer4
            response = featuresApi.getFeatureFollowers(purchaseOptionsID, sessionToken);
            responseJson = new JSONObject(response);
            followers = responseJson.get(Constants.JSON_FEATURE_FIELD_FOLLOWERS).toString();
            Assert.assertTrue(followers.contains(admin2follow) && !followers.contains(prodLead2follow), "failed to follow");
            Assert.assertTrue(!responseJson.getBoolean(Constants.JSON_PRODUCT_FIELD_IS_FOLLOWING), "failed to follow");

        } catch (Exception e) {
            Assert.fail("follow product api test failed");
        }
    }

    @Test(dependsOnMethods="testNotificationPurchaseOptions",description = "test entitlement apis - negative")
    public void negativeTests() {
    		//follow product twice
    		String response = productsApi.followProduct(productID, adminToken);
        Assert.assertTrue(response.contains("error"), "can follow product twice");

        //follow entitlement twice
		response = featuresApi.followFeature(entitlementID, adminToken);
		Assert.assertTrue(response.contains("error"), "can follow entitlement twice");
    
		//follow purchaseOptions twice
		response = featuresApi.followFeature(purchaseOptionsID, adminToken);
		Assert.assertTrue(response.contains("error"), "can follow purchaseOptions twice");
    
        response =  featuresApi.getFeatureFollowers(configID1,adminToken);
        Assert.assertTrue(response.contains("error"),"negative failed");
        response =  featuresApi.followFeature(configID1,adminToken);
        Assert.assertTrue(response.contains("error"),"negative failed");
        int responseCode =  featuresApi.unfollowFeature(configID1,adminToken);
        Assert.assertTrue(responseCode!=200,"negative failed");
        response =  featuresApi.getFeatureFollowers(configID2,adminToken);
        Assert.assertTrue(response.contains("error"),"negative failed");
        response =  featuresApi.followFeature(configID2,adminToken);
        Assert.assertTrue(response.contains("error"),"negative failed");
        responseCode =  featuresApi.unfollowFeature(configID2,adminToken);
        Assert.assertTrue(responseCode!=200,"negative failed");
       
        //delete product
        responseCode =  productsApi.deleteProduct(productID,adminToken);
        Assert.assertTrue(responseCode == 200, "cannot delete product");
        
        // bad id
        response = featuresApi.getFeatureFollowers("a", adminToken);
        Assert.assertTrue(response.contains("error"),"negative failed");
        response =featuresApi.followFeature("a",adminToken);
        Assert.assertTrue(response.contains("error"),"negative failed");
        responseCode =featuresApi.unfollowFeature("a",adminToken);
        Assert.assertTrue(responseCode!=200,"negative failed");
        response = productsApi.getProductFollowers("a", adminToken);
        Assert.assertTrue(response.contains("error"),"negative failed");
        response =productsApi.followProduct("a",adminToken);
        Assert.assertTrue(response.contains("error"),"negative failed");
        responseCode =productsApi.unfollowProduct("a",adminToken);
        Assert.assertTrue(responseCode!=200,"negative failed");
        
        //missing product
        response =featuresApi.getFeatureFollowers(entitlementID,adminToken);
        Assert.assertTrue(response.contains("error"),"negative failed");
        response =featuresApi.followFeature(entitlementID,adminToken);
        Assert.assertTrue(response.contains("error"),"negative failed");
        responseCode =featuresApi.unfollowFeature(entitlementID,adminToken);
        Assert.assertTrue(responseCode!=200,"negative failed");
        response =featuresApi.getFeatureFollowers(purchaseOptionsID,adminToken);
        Assert.assertTrue(response.contains("error"),"negative failed");
        response =featuresApi.followFeature(purchaseOptionsID,adminToken);
        Assert.assertTrue(response.contains("error"),"negative failed");
        responseCode =featuresApi.unfollowFeature(purchaseOptionsID,adminToken);
        Assert.assertTrue(responseCode!=200,"negative failed");
        
        //missing product
        response =productsApi.getProductFollowers(productID,adminToken);
        Assert.assertTrue(response.contains("error"),"negative failed");
        response =productsApi.followProduct(productID,adminToken);
        Assert.assertTrue(response.contains("error"),"negative failed");
        responseCode =productsApi.unfollowProduct(productID,adminToken);
        Assert.assertTrue(responseCode!=200,"negative failed");
    }

    @AfterTest
    private void reset(){
    		baseUtils.reset(productID, sessionToken);
    }
}
