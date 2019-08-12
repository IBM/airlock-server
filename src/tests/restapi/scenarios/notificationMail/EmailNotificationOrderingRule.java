package tests.restapi.scenarios.notificationMail;

import java.io.IOException;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.wink.json4j.JSONException;
import org.apache.wink.json4j.JSONObject;
import org.testng.Assert;
import org.testng.annotations.*;

import tests.com.ibm.qautils.FileUtils;
import tests.restapi.AirlockUtils;
import tests.restapi.EmailNotification;
import tests.restapi.FeaturesRestApi;
import tests.restapi.ProductsRestApi;

public class EmailNotificationOrderingRule {
    private String sessionToken = "";
    private String adminToken = "";
    private String productID;
    private String seasonID;
    private String featureID;
    private String orderingRuleID1;
    private ProductsRestApi productsApi;
    private FeaturesRestApi featuresApi;
    private AirlockUtils baseUtils;
    protected String m_url;
    private String admin2follow;
    private String prodLead2follow;
    private String filePath;
    protected EmailNotification notification;
    protected String m_notify;

    @BeforeClass
	@Parameters({"url", "analyticsUrl", "translationsUrl", "configPath", "sessionToken", "userName", "userPassword", "appName", "adminUser", "adminPassword", "productLeadName", "productLeadPassword", "productsToDeleteFile", "notify"})
	public void init(String url, String analyticsUrl, String translationsUrl, String configPath, String sToken, String userName, String userPassword, String appName, String adminUser, String adminPassword, String productLeadName, String productLeadPassword, String productsToDeleteFile, String notify) throws Exception{
    	filePath = configPath;
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
       
        if(notify != null){
        	m_notify = notify;
        }
        notification = baseUtils.setNotification(m_notify, m_url, adminToken);
    }
    
    @Test(description="add features")
    public void addComponents() throws IOException, JSONException{
    	
    	String feature = FileUtils.fileToString(filePath + "feature1.txt", "UTF-8", false);
		JSONObject fJson = new JSONObject(feature);			
		fJson.put("name", RandomStringUtils.randomAlphabetic(5));
		featureID = featuresApi.addFeature(seasonID, fJson.toString(), "ROOT", sessionToken);
		Assert.assertFalse(featureID.contains("error"), "parent feature was not added to the season: " + featureID);

	
		String orderingRule = FileUtils.fileToString(filePath + "orderingRule/orderingRule1.txt", "UTF-8", false);
		JSONObject jsonOR = new JSONObject(orderingRule);
		jsonOR.put("name", RandomStringUtils.randomAlphabetic(5));
		orderingRuleID1 = featuresApi.addFeature(seasonID, jsonOR.toString(), featureID, sessionToken);
		Assert.assertFalse(orderingRuleID1.contains("error"), "Can't create orderingRule  " + orderingRuleID1);

    }
    
    @Test(dependsOnMethods="addComponents",description = "negative test feature apis")
    public void negativeTests() {
        String response =  featuresApi.getFeatureFollowers(orderingRuleID1,adminToken);
        Assert.assertTrue(response.contains("error"),"failed in getFeatureFollowers on ordering rule");
        response =  featuresApi.followFeature(orderingRuleID1,adminToken);
        Assert.assertTrue(response.contains("error"),"failed in followFeature on ordering rule");
        int responseCode =  featuresApi.unfollowFeature(orderingRuleID1,adminToken);
        Assert.assertTrue(responseCode!=200,"failed in unfollowFeature on ordering rule");
    }
    
    @Test(dependsOnMethods="negativeTests",description = "test feature apis")
    public void testNotificationFeatureApi() {
        try {
 
            String response = featuresApi.followFeature(featureID, sessionToken);
            Assert.assertTrue(response.equals(""), "failed to follow");
            
            //move ordering rule to production
			String child1 = featuresApi.getFeature(orderingRuleID1, sessionToken);
			JSONObject child1Json = new JSONObject(child1);
			child1Json.put("stage", "PRODUCTION");
			response = featuresApi.updateFeature(seasonID, orderingRuleID1, child1Json.toString(), adminToken);
			Assert.assertFalse(response.contains("error"), "Can't move ordering rule to production");

            //add new ordering rule
			String orderingRule = FileUtils.fileToString(filePath + "orderingRule/orderingRule1.txt", "UTF-8", false);
			JSONObject jsonOR = new JSONObject(orderingRule);
			jsonOR.put("name", RandomStringUtils.randomAlphabetic(5));
			String originalName = jsonOR.getString("namespace")+ "." + jsonOR.getString("name");
			String orderingRuleID2 = featuresApi.addFeature(seasonID, jsonOR.toString(), orderingRuleID1, sessionToken);
			Assert.assertFalse(orderingRuleID2.contains("error"), "Can't create orderingRule2  " + orderingRuleID2);

			//update second ordering rule
			String child2 = featuresApi.getFeature(orderingRuleID2, sessionToken);
			JSONObject child2Json = new JSONObject(child2);
			child2Json.put("name", "new ordering rule2 name");
			response = featuresApi.updateFeature(seasonID, orderingRuleID2, child2Json.toString(), adminToken);
			Assert.assertFalse(response.contains("error"), "Can't change ordering rule2 description: " + response);
			
			//delete second ordering rule
			int respCode = featuresApi.deleteFeature(orderingRuleID2, sessionToken);
			Assert.assertTrue(respCode==200, "Can't delete ordering rule");
			
			//parse follow feature result
			JSONObject result = baseUtils.getNotificationResult(notification);

			Assert.assertTrue(getFollowersSize(result, 0)==1, "Incorrect number of feature followers");
			Assert.assertTrue(result.getJSONArray("allEmails").getJSONObject(0).getString("details").contains("DEVELOPMENT to PRODUCTION"), "Updated ordering rule1 stage  was not registered notification");
			
			Assert.assertTrue(result.getJSONArray("allEmails").getJSONObject(1).getString("details").contains("The ordering rule " + originalName + " was created"), "New ordering rule was not registered notification");
			
			Assert.assertTrue(result.getJSONArray("allEmails").getJSONObject(2).getString("details").contains("to new ordering rule2 name"), "Updated ordering rule name was not registered notification");
			
			Assert.assertTrue(result.getJSONArray("allEmails").getJSONObject(3).getString("details").contains("OR1.new ordering rule2 name was deleted"), "Delete ordering rule was not registered notification");

			int responseCode = featuresApi.unfollowFeature(featureID, sessionToken);
            Assert.assertTrue(responseCode==200, "failed to unfollow");
            
        } catch (Exception e) {
            Assert.fail("follow product api test failed");
        }
    }

   @Test(dependsOnMethods="testNotificationFeatureApi", description = "test product apis")
    public void testNotificationProductApi() {
        try {
        	notification = baseUtils.setNotification(m_notify, m_url, adminToken);
        	
            // follow product
            String response = productsApi.followProduct(productID, adminToken);
            Assert.assertTrue(response.equals(""), "failed to follow");
         
            //move ordering rule to production
 			String child1 = featuresApi.getFeature(orderingRuleID1, sessionToken);
 			JSONObject child1Json = new JSONObject(child1);
 			child1Json.put("stage", "DEVELOPMENT");
 			response = featuresApi.updateFeature(seasonID, orderingRuleID1, child1Json.toString(), adminToken);
 			Assert.assertFalse(response.contains("error"), "Can't move ordering rule to production");

            //move ordering rule to production
  			child1 = featuresApi.getFeature(orderingRuleID1, sessionToken);
  			child1Json = new JSONObject(child1);
  			child1Json.put("stage", "PRODUCTION");
  			response = featuresApi.updateFeature(seasonID, orderingRuleID1, child1Json.toString(), adminToken);
  			Assert.assertFalse(response.contains("error"), "Can't move ordering rule to production");          
            
            int responseCode = productsApi.unfollowProduct(productID, sessionToken);
            Assert.assertTrue(responseCode==200, "failed to unfollow");
            
            JSONObject result = baseUtils.getNotificationResult(notification);
            
            Assert.assertTrue(result.getJSONArray("allEmails").getJSONObject(0).getString("details").contains("PRODUCTION to DEVELOPMENT"), "Updated ordering rule1 stage  was not registered notification");
            Assert.assertTrue(result.getJSONArray("allEmails").getJSONObject(1).getString("details").contains("changed from DEVELOPMENT to PRODUCTION"), "Updated ordering rule1 stage  was not registered notification");

        } catch (Exception e) {
            Assert.fail("follow product api test failed");
        }
    }

 
 

    @AfterTest
    private void reset(){
    	baseUtils.reset(productID, sessionToken);
    }
    
    private int getFollowersSize(JSONObject result, int index) throws JSONException{
    	int size = 0;
		if (!result.getJSONArray("allEmails").getJSONObject(index).isNull("followers")) 
			size = result.getJSONArray("allEmails").getJSONObject(index).getJSONArray("followers").size();				
		
		return size;	
    }
}
