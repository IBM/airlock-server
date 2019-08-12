package tests.restapi.production_data_tests;

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

public class ReorderFeatureDummyNotification {
	   private String sessionToken = "";
	    private String adminToken = "";
	    private String productID;
	    private String seasonID;
	    private ProductsRestApi p;
	    private FeaturesRestApi f;
	    private AirlockUtils baseUtils;
	    protected String m_url;
	    private String m_appName = "";
	    private String filePath ;
	    protected EmailNotification notification;
	    private String m_notify;

	    @BeforeClass
		@Parameters({"url", "analyticsUrl", "translationsUrl", "configPath", "sessionToken", "userName", "userPassword", "appName", "productsToDeleteFile", "adminPassword", "notify"})
		public void init(String url, String analyticsUrl, String translationsUrl, String configPath, String sToken, String userName, String userPassword, String appName, String productsToDeleteFile, String adminPassword, String notify) throws Exception{
	        m_url = url;
	        filePath = configPath;
	        if(appName != null){
	            m_appName = appName;
	        }
	        if(notify != null){
	        	m_notify = notify;
	        }
			baseUtils = new AirlockUtils(url, analyticsUrl, translationsUrl, configPath, sToken, userName, userPassword, appName, productsToDeleteFile);
			sessionToken = baseUtils.sessionToken;

	        adminToken = baseUtils.getJWTToken(adminToken, adminPassword,m_appName);
	   
	        baseUtils.setSessionToken(adminToken);
	        productID = baseUtils.createProduct();
	        seasonID = baseUtils.createSeason(productID);
	        p = new ProductsRestApi();
	        p.setURL(m_url);
	        f = new FeaturesRestApi();
	        f.setURL(m_url);
	        
	        p.followProduct(productID, sessionToken);
	    }
	    /*
	     * FOR HOTFIX:
	     * add feature with 2 sub-features. Update parent feature description.
	     * On reorder remove the dummy notification saying "Notification for reorder position N to position N (same one)
	     */
	    
	    @Test (description="Follow feature")
	    public void followParentFeature() throws IOException, JSONException{
	    	notification = baseUtils.setNotification(m_notify, m_url, adminToken);
	    	
	    	//add parent feature
	    	String feature = FileUtils.fileToString(filePath + "feature1.txt", "UTF-8", false);
			JSONObject json = new JSONObject(feature);
			json.put("name", "parent1");
			String parentID = f.addFeature(seasonID, json.toString(), "ROOT", adminToken);
			
			f.followFeature(parentID, adminToken);
			
			//add 2 sub-features
			String ch1 = FileUtils.fileToString(filePath + "feature1.txt", "UTF-8", false);
			JSONObject jsonChild1 = new JSONObject(ch1);
			jsonChild1.put("name", "child1");
			String childID1 = f.addFeature(seasonID, jsonChild1.toString(), parentID, adminToken);

			String ch2 = FileUtils.fileToString(filePath + "feature1.txt", "UTF-8", false);
			JSONObject jsonChild2 = new JSONObject(ch2);
			jsonChild2.put("name", "child2");
			String childID2 = f.addFeature(seasonID, jsonChild2.toString(), parentID, adminToken);

			//update parent feature with the same children order
			String parent = f.getFeature(parentID, adminToken);
			json = new JSONObject(parent);
			json.put("description", "blabla");
			f.updateFeature(seasonID, parentID, json.toString(), adminToken);
			
			JSONObject result = baseUtils.getNotificationResult(notification);
			//Assert.assertTrue(getFollowersSize(result, 0)==0, "Incorrect number of feature followers");
			Assert.assertTrue(result.getJSONArray("allEmails").size()==2, "Incorrect number of  notifications");


	    }
	    

	    
	    @AfterTest
	    private void reset(){
	    	p.unfollowProduct(productID, adminToken);
	    	baseUtils.reset(productID, adminToken);
	    }
}
