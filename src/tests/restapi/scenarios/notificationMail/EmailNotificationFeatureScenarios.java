package tests.restapi.scenarios.notificationMail;

import java.io.IOException;








import org.apache.wink.json4j.JSONException;
import org.apache.wink.json4j.JSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import tests.com.ibm.qautils.FileUtils;
import tests.restapi.AirlockUtils;
import tests.restapi.FeaturesRestApi;
import tests.restapi.EmailNotification;
import tests.restapi.ProductsRestApi;

public class EmailNotificationFeatureScenarios {
	   private String sessionToken = "";
	    private String adminToken = "";
	    private String productID;
	    private String seasonID;
	    private ProductsRestApi p;
	    private FeaturesRestApi f;
	    private AirlockUtils baseUtils;
	    protected String m_url;
	    private String filePath ;
	    protected EmailNotification notification;
	   // private String follower;
	    private String m_notify;

	    @BeforeClass
		@Parameters({"url", "analyticsUrl", "translationsUrl", "configPath", "sessionToken", "userName", "userPassword", "appName", "adminUser", "adminPassword", "productLeadName", "productLeadPassword", "productsToDeleteFile", "notify"})
		public void init(String url, String analyticsUrl, String translationsUrl, String configPath, String sToken, String userName, String userPassword, String appName, String adminUser, String adminPassword, String productLeadName, String productLeadPassword, String productsToDeleteFile, String notify) throws Exception{
	        m_url = url;
	        filePath = configPath;

	        if(notify != null){
	        	m_notify = notify;
	        }
	      //  follower = productLeadName;
	        baseUtils = new AirlockUtils(url, analyticsUrl, translationsUrl, configPath, sToken, userName, userPassword, appName, productsToDeleteFile);
	        adminToken = baseUtils.setNewJWTToken(adminUser, adminPassword,appName);
	        sessionToken = baseUtils.setNewJWTToken(productLeadName, productLeadPassword,appName);
	        baseUtils.setSessionToken(adminToken);
	        productID = baseUtils.createProduct();
			baseUtils.printProductToFile(productID);
	        seasonID = baseUtils.createSeason(productID);
	        p = new ProductsRestApi();
	        p.setURL(m_url);
	        f = new FeaturesRestApi();
	        f.setURL(m_url);
	        
	        //p.followProduct(productID, sessionToken);
	    }
	    /*
	     * 
	     * Create feature in dev, follow, update name, move to production, update description
	     * Add 3 children, reorder children, update child name, move 1 child to prod, delete 1 child
	     * Move child1 to production,  move child1 to development
	     * delete child2,
	     * add configuration rule, delete configuraton rule
	     * Move parent to development
	     * 
	     */
	    
	    @Test (description="Follow parent feature")
	    public void followParentFeature() throws IOException, JSONException{
	    		notification = baseUtils.setNotification(m_notify, m_url, adminToken);
	    	
	    		//add feature in development
	    		String feature = FileUtils.fileToString(filePath + "feature1.txt", "UTF-8", false);
			JSONObject json = new JSONObject(feature);
			json.put("name", "parent1");
			String parentID = f.addFeature(seasonID, json.toString(), "ROOT", sessionToken);
			
			String response = f.followFeature(parentID, sessionToken);
			
			//update feature name
			String parent = f.getFeature(parentID, sessionToken) ;
			JSONObject parentJson = new JSONObject(parent);
			parentJson.put("name", "new parent1");
			response = f.updateFeature(seasonID, parentID, parentJson.toString(), sessionToken);
			Assert.assertFalse(response.contains("error"), "Feature was not updated" + response);
			
			
			//move parent feature to production
			parent = f.getFeature(parentID, sessionToken) ;
			parentJson = new JSONObject(parent);
			parentJson.put("stage", "PRODUCTION");
			response = f.updateFeature(seasonID, parentID, parentJson.toString(), adminToken);
			Assert.assertFalse(response.contains("error"), "Feature was not updated" + response);

			
			//update parent feature description
			parent = f.getFeature(parentID, sessionToken) ;
			parentJson = new JSONObject(parent);
			parentJson.put("description", "new description");
			response = f.updateFeature(seasonID, parentID, parentJson.toString(), adminToken);
			Assert.assertFalse(response.contains("error"), "Feature was not updated" + response);

			
			//add 3 child features in development
			json = new JSONObject(feature);
			json.put("name", "child1");
			String childID1 = f.addFeature(seasonID, json.toString(), parentID, sessionToken);
			json.put("name", "child2");
			String childID2 = f.addFeature(seasonID, json.toString(), parentID, sessionToken);
			json.put("name", "child3");
			String childID3 = f.addFeature(seasonID, json.toString(), parentID, sessionToken);

			//Move child1 to production, move child1 to development
			String child1 = f.getFeature(childID1, sessionToken);
			JSONObject child1Json = new JSONObject(child1);
			child1Json.put("stage", "PRODUCTION");
			response = f.updateFeature(seasonID, childID1, child1Json.toString(), adminToken);
			Assert.assertFalse(response.contains("error"), "Can't move child1 to production");
			child1 = f.getFeature(childID1, sessionToken);
			child1Json = new JSONObject(child1);
			child1Json.put("stage", "DEVELOPMENT");
			response = f.updateFeature(seasonID, childID1, child1Json.toString(), adminToken);
			Assert.assertFalse(response.contains("error"), "Can't move child1 to development");
			
			//Delete child2 - email is not sent for deleted sub-features
			int responseCode = f.deleteFeature(childID2, sessionToken);
			Assert.assertTrue(responseCode == 200, "Can't delete child2");
			
			//Add configuration rule
			String configuration = FileUtils.fileToString(filePath + "configuration_rule1.txt", "UTF-8", false);
			JSONObject configJson = new JSONObject(configuration);
			configJson.put("name", "config1");
			String configRuleID = f.addFeature(seasonID, configJson.toString(), parentID, sessionToken ); 
			
			//delete configuration rule
			responseCode = f.deleteFeature(configRuleID, sessionToken);
			Assert.assertTrue(responseCode == 200, "Can't delete configuration rule");

			//move parent feature to development
			parent = f.getFeature(parentID, sessionToken) ;
			parentJson = new JSONObject(parent);
			parentJson.put("stage", "DEVELOPMENT");
			response = f.updateFeature(seasonID, parentID, parentJson.toString(), adminToken);
			Assert.assertFalse(response.contains("error"), "Feature was not updated" + response);
			
			//delete feature
			responseCode = f.deleteFeature(parentID, sessionToken);
			Assert.assertTrue(responseCode == 200, "Can't delete parent feature");

			
			//parse follow feature result
			
			JSONObject result = baseUtils.getNotificationResult(notification);
			Assert.assertTrue(getFollowersSize(result, 0)==1, "Incorrect number of feature followers");
			//Assert.assertTrue(result.getJSONArray("allEmails").getJSONObject(0).getJSONArray("followers").get(0).equals(follower), "Incorrect follower");
			Assert.assertTrue(result.getJSONArray("allEmails").getJSONObject(0).getString("details").contains("new parent1"), "Updated feature name  was not registered notification");
			
			Assert.assertTrue(result.getJSONArray("allEmails").getJSONObject(1).getString("details").contains("PRODUCTION"), "Updated feature stage to production was not registered notification");
			Assert.assertTrue(getFollowersSize(result, 1)==1, "Incorrect number of feature followers");
			
			Assert.assertTrue(result.getJSONArray("allEmails").getJSONObject(2).getString("details").contains("new description"), "Updated feature description was not registered notification");
			Assert.assertTrue(getFollowersSize(result, 2)==1, "Incorrect number of feature followers");
			
			Assert.assertTrue(result.getJSONArray("allEmails").getJSONObject(3).getString("details").contains("child1 was created"), "Add child1 feature was not registered notification");
			Assert.assertTrue(getFollowersSize(result, 3)==1, "Incorrect number of feature followers");
			
			Assert.assertTrue(result.getJSONArray("allEmails").getJSONObject(4).getString("details").contains("child2 was created"), "Add child2 feature was not registered notification");
			Assert.assertTrue(result.getJSONArray("allEmails").getJSONObject(5).getString("details").contains("child3 was created"), "Add child3 feature was not registered notification");
			
			/*Assert.assertTrue(result.getJSONArray("allEmails").getJSONObject(6).getString("details").contains("PRODUCTION"), "Updated child1 stage to production was not registered notification");
			Assert.assertTrue(getFollowersSize(result, 6)==0, "Incorrect number of feature followers");
			
			Assert.assertTrue(result.getJSONArray("allEmails").getJSONObject(7).getString("details").contains("DEVELOPMENT"), "Updated child1 stage to development was not registered notification");
			Assert.assertTrue(getFollowersSize(result, 7)==0, "Incorrect number of feature followers");
			*/
			
			Assert.assertTrue(result.getJSONArray("allEmails").getJSONObject(6).getString("details").contains("config1 was created"), "Add configuration rule was not registered notification");
			Assert.assertTrue(getFollowersSize(result, 6)==1, "Incorrect number of feature followers");			
			Assert.assertTrue(result.getJSONArray("allEmails").getJSONObject(7).getString("details").contains("config1 was deleted"), "Delete configuration rule was not registered notification");					
			Assert.assertTrue(result.getJSONArray("allEmails").getJSONObject(8).getString("details").contains("to DEVELOPMENT") && result.getJSONArray("allEmails").getJSONObject(8).getString("item").contains("parent1"), "Parent1 stage upated was not registered notification");
			Assert.assertTrue(getFollowersSize(result, 8)==1, "Incorrect number of feature followers");
			
			Assert.assertTrue(result.getJSONArray("allEmails").getJSONObject(9).getString("details").contains("new parent1 was deleted"), "Delete followed feature  was not registered notification");
			Assert.assertTrue(getFollowersSize(result, 9)==1, "Incorrect number of feature followers");

	    }
	    
	    /*
	     * 
	     *- Add parent feature + 3 children feature are IN DEVELOPMENT
			- Mark all children to be Followed
			- Move parent feature to Production - 
			- Update parent 
	     */
	    
	    @Test (dependsOnMethods = "followParentFeature", description="Follow children features")
	    public void followChildrenFeature() throws IOException, JSONException{
		    	notification = baseUtils.setNotification(m_notify, m_url, adminToken);
		    	
		    	
		    	//add feature in development
		    	String feature = FileUtils.fileToString(filePath + "feature1.txt", "UTF-8", false);
			JSONObject json = new JSONObject(feature);
			json.put("name", "parent2");
			String parentID = f.addFeature(seasonID, json.toString(), "ROOT", sessionToken);
			
			//add 3 child features in development and follow each of them
			json = new JSONObject(feature);
			json.put("name", "child21");
			String childID1 = f.addFeature(seasonID, json.toString(), parentID, sessionToken);
			f.followFeature(childID1, sessionToken);
			json.put("name", "child22");
			String childID2 = f.addFeature(seasonID, json.toString(), parentID, sessionToken);
			f.followFeature(childID2, sessionToken);
			json.put("name", "child23");
			String childID3 = f.addFeature(seasonID, json.toString(), parentID, sessionToken);
			f.followFeature(childID3, sessionToken);
			
			//move parent feature to production
			String parent = f.getFeature(parentID, sessionToken) ;
			JSONObject parentJson = new JSONObject(parent);
			parentJson.put("stage", "PRODUCTION");
			String response = f.updateFeature(seasonID, parentID, parentJson.toString(), adminToken);
			Assert.assertFalse(response.contains("error"), "Feature was not updated" + response);
			
			//update parent feature description
			parent = f.getFeature(parentID, sessionToken) ;
			parentJson = new JSONObject(parent);
			parentJson.put("description", "new description");
			response = f.updateFeature(seasonID, parentID, parentJson.toString(), adminToken);
			Assert.assertFalse(response.contains("error"), "Feature was not updated" + response);

			//Move child1 to production, move child1 to development
			String child1 = f.getFeature(childID1, sessionToken);
			JSONObject child1Json = new JSONObject(child1);
			child1Json.put("stage", "PRODUCTION");
			response = f.updateFeature(seasonID, childID1, child1Json.toString(), adminToken);
			Assert.assertFalse(response.contains("error"), "Can't move child21 to production");
			child1 = f.getFeature(childID1, sessionToken);
			child1Json = new JSONObject(child1);
			child1Json.put("stage", "DEVELOPMENT");
			response = f.updateFeature(seasonID, childID1, child1Json.toString(), adminToken);
			Assert.assertFalse(response.contains("error"), "Can't move child21 to development");
			
			//Delete child2
			int responseCode = f.deleteFeature(childID2, sessionToken);
			Assert.assertTrue(responseCode == 200, "Can't delete child2");
			
			//parse follow feature result
			JSONObject result = baseUtils.getNotificationResult(notification);
		//	Assert.assertTrue(getFollowersSize(result, 0)==0, "Incorrect number of feature followers");
		//	Assert.assertTrue(result.getJSONArray("allEmails").getJSONObject(0).getString("details").contains("to PRODUCTION") && result.getJSONArray("allEmails").getJSONObject(0).getString("item").contains("parent2"), "Feature parent2 stage  was not registered notification");
			
		//	Assert.assertTrue(getFollowersSize(result, 0)==0, "Incorrect number of feature followers");
		//	Assert.assertTrue(result.getJSONArray("allEmails").getJSONObject(0).getString("details").contains("new description") && result.getJSONArray("allEmails").getJSONObject(1).getString("item").contains("parent2"), "Feature parent2 description was updated but  was not registered notification");
			
			Assert.assertTrue(result.getJSONArray("allEmails").getJSONObject(0).getString("details").contains("to PRODUCTION") && result.getJSONArray("allEmails").getJSONObject(0).getString("item").contains("child21"), "Feature stage  was not registered notification");
			Assert.assertTrue(getFollowersSize(result, 0)==1, "Incorrect number of feature followers");
			
			Assert.assertTrue(result.getJSONArray("allEmails").getJSONObject(1).getString("details").contains("to DEVELOPMENT") && result.getJSONArray("allEmails").getJSONObject(1).getString("item").contains("child21"), "Feature stage  was not registered notification");
			Assert.assertTrue(getFollowersSize(result, 1)==1, "Incorrect number of feature followers");
			
			Assert.assertTrue(result.getJSONArray("allEmails").getJSONObject(2).getString("details").contains("child22 was deleted") && result.getJSONArray("allEmails").getJSONObject(2).getString("item").contains("child22"), "Deleted feature  was not registered notification");
			Assert.assertTrue(getFollowersSize(result, 2)==1, "Incorrect number of feature followers");
			
	    }
	    
	    private int getFollowersSize(JSONObject result, int index) throws JSONException{
	    		int size = 0;
			if (!result.getJSONArray("allEmails").getJSONObject(index).isNull("followers")) 
				size = result.getJSONArray("allEmails").getJSONObject(index).getJSONArray("followers").size();				
			
			return size;	
	    }
	    
	    @AfterTest
	    private void reset(){
	    		p.unfollowProduct(productID, sessionToken);
	    		baseUtils.reset(productID, sessionToken);
	    }
}
