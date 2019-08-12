package tests.restapi.validations.airlock_notification;


import java.util.UUID;

import org.apache.wink.json4j.JSON;
import org.apache.wink.json4j.JSONArray;
import org.apache.wink.json4j.JSONException;
import org.apache.wink.json4j.JSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import tests.restapi.AirlockUtils;
import tests.restapi.AirlocklNotificationRestApi;


public class AirlockNotificationsFields {
	protected String seasonID;
	protected String filePath;
	protected String m_url;
	protected String notification;
	private AirlocklNotificationRestApi notifApi;
	private AirlockUtils baseUtils;
	protected String productID;
	private String sessionToken = "";
	
	@BeforeClass
	@Parameters({"url", "analyticsUrl", "translationsUrl", "configPath", "sessionToken", "userName", "userPassword", "appName", "productsToDeleteFile"})
	public void init(String url, String analyticsUrl, String translationsUrl, String configPath, String sToken, String userName, String userPassword, String appName, String productsToDeleteFile) throws Exception{
		filePath = configPath;
		baseUtils = new AirlockUtils(url, analyticsUrl, translationsUrl, configPath, sToken, userName, userPassword, appName, productsToDeleteFile);
		sessionToken = baseUtils.sessionToken;
		productID = baseUtils.createProduct();
		baseUtils.printProductToFile(productID);
		seasonID = baseUtils.createSeason(productID);

		m_url = url;
		
		notifApi = new AirlocklNotificationRestApi();
		notifApi.setUrl(url);
		
	}

	
	@Test
	private void notificationsLimitations() throws JSONException{
		JSONObject allNotifications = new JSONObject(notifApi.getAllNotifications(seasonID, sessionToken));
		allNotifications.remove("notificationsLimitations");
		updateNotifications(allNotifications.toString(), "notificationsLimitations", true);
		
		allNotifications = new JSONObject(notifApi.getAllNotifications(seasonID, sessionToken));
		allNotifications.put("notificationsLimitations", JSON.NULL);
		updateNotifications(allNotifications.toString(), "notificationsLimitations", true);
		
		allNotifications = new JSONObject(notifApi.getAllNotifications(seasonID, sessionToken));
		allNotifications.put("notificationsLimitations", new JSONArray());
		updateNotifications(allNotifications.toString(), "notificationsLimitations", false);
		
		allNotifications = new JSONObject(notifApi.getAllNotifications(seasonID, sessionToken));
		allNotifications.put("notificationsLimitations", "limitation");
		updateNotifications(allNotifications.toString(), "notificationsLimitations", true);
				
	}
	
	@Test
	private void configurationSchema() throws JSONException{
		JSONObject allNotifications = new JSONObject(notifApi.getAllNotifications(seasonID, sessionToken));
		allNotifications.remove("configurationSchema");
		updateNotifications(allNotifications.toString(), "configurationSchema", true);
		
		allNotifications = new JSONObject(notifApi.getAllNotifications(seasonID, sessionToken));
		allNotifications.put("configurationSchema", JSON.NULL);
		updateNotifications(allNotifications.toString(), "configurationSchema", true);
		
		allNotifications = new JSONObject(notifApi.getAllNotifications(seasonID, sessionToken));
		allNotifications.put("configurationSchema", new JSONObject());
		updateNotifications(allNotifications.toString(), "configurationSchema", false);
		
		allNotifications = new JSONObject(notifApi.getAllNotifications(seasonID, sessionToken));
		allNotifications.put("configurationSchema", "new string");
		updateNotifications(allNotifications.toString(), "configurationSchema", true);
				
	}
	
	@Test
	private void notifications() throws JSONException{
		JSONObject allNotifications = new JSONObject(notifApi.getAllNotifications(seasonID, sessionToken));
		allNotifications.remove("notifications");
		updateNotifications(allNotifications.toString(), "notifications", true);
		
		allNotifications = new JSONObject(notifApi.getAllNotifications(seasonID, sessionToken));
		allNotifications.put("notifications", JSON.NULL);
		updateNotifications(allNotifications.toString(), "notifications", true);
		
		allNotifications = new JSONObject(notifApi.getAllNotifications(seasonID, sessionToken));
		allNotifications.put("notifications", new JSONArray());
		updateNotifications(allNotifications.toString(), "notifications", false);
		
		allNotifications = new JSONObject(notifApi.getAllNotifications(seasonID, sessionToken));
		allNotifications.put("notifications", "new notification");
		updateNotifications(allNotifications.toString(), "notifications", true);
				
	}
	
	@Test
	private void lastModified() throws JSONException{
		JSONObject allNotifications = new JSONObject(notifApi.getAllNotifications(seasonID, sessionToken));
		allNotifications.remove("lastModified");
		updateNotifications(allNotifications.toString(), "lastModified", true);
		
		allNotifications = new JSONObject(notifApi.getAllNotifications(seasonID, sessionToken));
		allNotifications.put("lastModified", JSON.NULL);
		updateNotifications(allNotifications.toString(), "lastModified", true);
		
		allNotifications = new JSONObject(notifApi.getAllNotifications(seasonID, sessionToken));
		allNotifications.put("lastModified", "");
		updateNotifications(allNotifications.toString(), "lastModified", true);
				
	}
	
	@Test
	private void seasonId() throws JSONException{
		JSONObject allNotifications = new JSONObject(notifApi.getAllNotifications(seasonID, sessionToken));
		allNotifications.remove("seasonId");
		updateNotifications(allNotifications.toString(), "seasonId", false);
		
		allNotifications = new JSONObject(notifApi.getAllNotifications(seasonID, sessionToken));
		allNotifications.put("seasonId", JSON.NULL);
		updateNotifications(allNotifications.toString(), "seasonId", false);
		
		allNotifications = new JSONObject(notifApi.getAllNotifications(seasonID, sessionToken));
		allNotifications.put("seasonId", "");
		updateNotifications(allNotifications.toString(), "seasonId", true);
		
		allNotifications = new JSONObject(notifApi.getAllNotifications(seasonID, sessionToken));
		allNotifications.put("seasonId", UUID.randomUUID().toString());
		updateNotifications(allNotifications.toString(), "seasonId", true);
				
	}
	
	
	private void updateNotifications(String content, String field, boolean expectedResult){
		String response = notifApi.updateAllNotifications(seasonID, content, sessionToken);
		Assert.assertEquals(response.contains("error"), expectedResult,  "Test failed for field: " + field);
	}
	


	
	@AfterTest
	public void reset(){
		baseUtils.reset(productID, sessionToken);
	}

	

}
