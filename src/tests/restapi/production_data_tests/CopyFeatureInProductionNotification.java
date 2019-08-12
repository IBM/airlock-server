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

public class CopyFeatureInProductionNotification {
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
	     * The copy from a feature in Production should not notify the Product follower, it should only notify the feature follower
	     */
	    
	    @Test (description="Follow feature")
	    public void followParentFeature() throws IOException, JSONException{
	    	notification = baseUtils.setNotification(m_notify, m_url, adminToken);
	    	String followResp = p.followProduct(productID, adminToken);
	    	
	    	//add feature in production
	    	String feature = FileUtils.fileToString(filePath + "feature1.txt", "UTF-8", false);
			JSONObject json = new JSONObject(feature);
			json.put("name", "feature1");
			json.put("stage", "PRODUCTION");
			String parentID = f.addFeature(seasonID, json.toString(), "ROOT", adminToken);
			
			//f.followFeature(parentID, adminToken);
			
			//update feature description
			String parent = f.getFeature(parentID, adminToken) ;
			JSONObject parentJson = new JSONObject(parent);
			parentJson.put("description", "new feature description");
			String response = f.updateFeature(seasonID, parentID, parentJson.toString(), adminToken);
			Assert.assertFalse(response.contains("error"), "Feature was not updated" + response);
			
			//copy feature
			String rootId = f.getRootId(seasonID, adminToken);
			response = f.copyFeature(parentID, rootId, "ACT", null, "suffix1", adminToken);
			Assert.assertTrue(response.contains("newSubTreeId"), "Feature was not copied: " + response);

			
			JSONObject result = baseUtils.getNotificationResult(notification);

			//move feature to development
			parent = f.getFeature(parentID, adminToken) ;
			parentJson = new JSONObject(parent);
			parentJson.put("stage", "DEVELOPMENT");
			response = f.updateFeature(seasonID, parentID, parentJson.toString(), adminToken);
			Assert.assertFalse(response.contains("error"), "Feature was not updated" + response);

			//parse notification results
			boolean copyNotification = false;
			for(int i = 0; i<result.getJSONArray("allEmails").size(); i++){
				if (result.getJSONArray("allEmails").getJSONObject(i).getString("details").contains("was copied to root"))
					copyNotification = true;
			}
			Assert.assertFalse(copyNotification, "Notification on copy feature was sent");

	    }
	    
	    
	    private int getFollowersSize(JSONObject result, int index) throws JSONException{
	    	int size = 0;
			if (!result.getJSONArray("allEmails").getJSONObject(index).isNull("followers")) 
				size = result.getJSONArray("allEmails").getJSONObject(index).getJSONArray("followers").size();				
			
			return size;	
	    }
	    
	    @AfterTest
	    private void reset(){
	    	p.unfollowProduct(productID, adminToken);
	    	baseUtils.reset(productID, adminToken);
	    }
}
