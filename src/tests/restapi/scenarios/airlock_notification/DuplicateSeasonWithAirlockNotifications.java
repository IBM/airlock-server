package tests.restapi.scenarios.airlock_notification;

import java.io.IOException;



import org.apache.wink.json4j.JSONArray;
import org.apache.wink.json4j.JSONException;
import org.apache.wink.json4j.JSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import tests.com.ibm.qautils.FileUtils;
import tests.restapi.AirlockUtils;
import tests.restapi.AirlocklNotificationRestApi;
import tests.restapi.SeasonsRestApi;

public class DuplicateSeasonWithAirlockNotifications {
	protected String seasonID;
	protected String filePath;
	protected String m_url;
	private AirlocklNotificationRestApi notifApi;
	private AirlockUtils baseUtils;
	protected String productID;
	private String sessionToken = "";
	private SeasonsRestApi s;
	
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
		
		s = new SeasonsRestApi();
		s.setURL(url);
		
	}
	
	@Test (description="add notifications")
	public void addNotifications() throws JSONException, IOException, InterruptedException{
		
		String notification = FileUtils.fileToString(filePath + "notifications/notification1.txt", "UTF-8", false);
		JSONObject json = new JSONObject(notification);
		json.put("name", "notification1");		
		String notificationID1 = notifApi.createNotification(seasonID, json.toString(), sessionToken);
		Assert.assertFalse(notificationID1.contains("error"), "Can't create notification1: " + notificationID1);
		
		json.put("name", "notification2");		
		String notificationID2 = notifApi.createNotification(seasonID, json.toString(), sessionToken);
		Assert.assertFalse(notificationID2.contains("error"), "Can't create notification1: " + notificationID2);
		
		String notifications = notifApi.getAllNotifications(seasonID, sessionToken);
		JSONObject jsonNotification = new JSONObject(notifications);
		String notificationSchema = FileUtils.fileToString(filePath + "notifications/notificationSchema1.txt", "UTF-8", false);
		jsonNotification.put("configurationSchema", new JSONObject(notificationSchema));
		String result = notifApi.updateAllNotifications(seasonID, jsonNotification.toString(), sessionToken);
		Assert.assertFalse(result.contains("error"), "Can't change schema");


	}
	
	@Test(dependsOnMethods="addNotifications", description="Create and validate season2")
	public void addSeason() throws Exception{
		String season = "{\"minVersion\":\"9.0\"}";
		String seasonID2 = s.addSeason(productID, season, sessionToken);
		
		String allNotificationSeason2 = notifApi.getAllNotifications(seasonID2, sessionToken);
		JSONObject allNotificationObj2 = new JSONObject(allNotificationSeason2);
		String allNotificationSeason1 = notifApi.getAllNotifications(seasonID, sessionToken);
		JSONObject allNotificationObj1 = new JSONObject(allNotificationSeason1);
		
		Assert.assertEquals(allNotificationObj1.getJSONObject("configurationSchema"), allNotificationObj2.getJSONObject("configurationSchema"), "Notification schemas are different in 2 seasons");
				
		JSONArray notifications2 = allNotificationObj2.getJSONArray("notifications");
		JSONArray notifications1 = allNotificationObj1.getJSONArray("notifications");
		
		Assert.assertEquals(notifications2.size(), 2, "Incorrect number of notifications in season2");

		Assert.assertNotEquals(notifications2.getJSONObject(0).getString("uniqueId"), notifications1.getJSONObject(0).getString("uniqueId"), "UniqueId of notification1 is the same in both seasons");
		Assert.assertNotEquals(notifications2.getJSONObject(1).getString("uniqueId"), notifications1.getJSONObject(1).getString("uniqueId"), "UniqueId of notification2 is the same in both seasons");
	}
	
	

	@AfterTest
	public void reset(){
		baseUtils.reset(productID, sessionToken);
	}

	

}
