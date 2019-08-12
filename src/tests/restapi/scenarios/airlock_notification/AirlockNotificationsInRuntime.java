package tests.restapi.scenarios.airlock_notification;

import java.io.IOException;
import java.util.UUID;

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
import tests.restapi.RuntimeDateUtilities;
import tests.restapi.RuntimeRestApi;


public class AirlockNotificationsInRuntime {
	protected String seasonID;
	protected String filePath;
	protected String m_url;
	private AirlocklNotificationRestApi notifApi;
	private AirlockUtils baseUtils;
	protected String productID;
	private String sessionToken = "";
	private String notificationDev1;
	private String notificationProd1;
	private String notificationDev2;
	private String notificationProd2;

	
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
	
	//update allNotifications object
	@Test (description="add notifications")
	public void addNotifications() throws JSONException, IOException, InterruptedException{
		String dateFormat = RuntimeDateUtilities.setDateFormat();
		
		String notification = FileUtils.fileToString(filePath + "notifications/notification1.txt", "UTF-8", false);
		JSONObject json = new JSONObject(notification);
		json.put("name", "notifDev1");
		json.put("stage", "DEVELOPMENT");
		notificationDev1 = notifApi.createNotification(seasonID, json.toString(), sessionToken);
		Assert.assertFalse(notificationDev1.contains("error"), "Can't create notification dev1: " + notificationDev1);
		
		json.put("name", "notifProd1");
		json.put("stage", "PRODUCTION");
		notificationProd1 = notifApi.createNotification(seasonID, json.toString(), sessionToken);
		Assert.assertFalse(notificationProd1.contains("error"), "Can't create notification prod1: " + notificationProd1);
		
		json.put("name", "notifDev2");
		json.put("stage", "DEVELOPMENT");
		notificationDev2 = notifApi.createNotification(seasonID, json.toString(), sessionToken);
		Assert.assertFalse(notificationDev2.contains("error"), "Can't create notification dev2: " + notificationDev2);

		json.put("name", "notifProd2");
		json.put("stage", "PRODUCTION");
		notificationProd2 = notifApi.createNotification(seasonID, json.toString(), sessionToken);
		Assert.assertFalse(notificationProd2.contains("error"), "Can't create notification prod2: " + notificationProd2);
		
		//check if files were changed
		RuntimeDateUtilities.setSleep();
		RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getDevelopmentNotificationDateModification(m_url, productID, seasonID, dateFormat, sessionToken);		
		Assert.assertTrue(responseDev.code ==200, "Runtime development feature file was not updated");
		JSONObject runtimeJson = new JSONObject(responseDev.message);
		Assert.assertEquals(runtimeJson.getJSONArray("notifications").size(), 4, "Incorrect number of notifications in runtime development file");
		Assert.assertTrue(runtimeJson.getJSONArray("notifications").getJSONObject(0).getString("name").equals("notifDev1"), "Incorrect first  notification");
		Assert.assertTrue(runtimeJson.getJSONArray("notifications").getJSONObject(1).getString("name").equals("notifProd1"), "Incorrect second  notification");
		Assert.assertTrue(runtimeJson.getJSONArray("notifications").getJSONObject(2).getString("name").equals("notifDev2"), "Incorrect third  notification");
		Assert.assertTrue(runtimeJson.getJSONArray("notifications").getJSONObject(3).getString("name").equals("notifProd2"), "Incorrect fourth  notification");
		

		
		RuntimeRestApi.DateModificationResults responseProd = RuntimeDateUtilities.getProductionNotificationDateModification(m_url, productID, seasonID, dateFormat, sessionToken);		
		Assert.assertTrue(responseProd.code ==200, "Runtime production feature file was not changed");
		runtimeJson = new JSONObject(responseProd.message);
		Assert.assertEquals(runtimeJson.getJSONArray("notifications").size(), 2, "Incorrect number of notifications in runtime production file");
		Assert.assertTrue(runtimeJson.getJSONArray("notifications").getJSONObject(0).getString("name").equals("notifProd1"), "Incorrect first production notification");
		Assert.assertTrue(runtimeJson.getJSONArray("notifications").getJSONObject(1).getString("name").equals("notifProd2"), "Incorrect second production notification");
		
		RuntimeRestApi.DateModificationResults prodChanged = RuntimeDateUtilities.getProductionChangedDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(prodChanged.code ==200, "productionChanged.txt file was not changed");

	}
	
	@Test(dependsOnMethods="addNotifications", description="change notifications stage using update all notificaitons")
	public void updateStage() throws JSONException, InterruptedException, IOException{
		String dateFormat = RuntimeDateUtilities.setDateFormat();
		
		String allNotif = notifApi.getAllNotifications(seasonID, sessionToken);
		JSONObject allNotifJson = new JSONObject(allNotif);
		allNotifJson.getJSONArray("notifications").getJSONObject(0).put("stage", "PRODUCTION");
		allNotifJson.getJSONArray("notifications").getJSONObject(1).put("stage", "DEVELOPMENT");
		allNotifJson.getJSONArray("notifications").getJSONObject(2).put("stage", "PRODUCTION");
		allNotifJson.getJSONArray("notifications").getJSONObject(3).put("stage", "DEVELOPMENT");
		
		String response = notifApi.updateAllNotifications(seasonID, allNotifJson.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Can't update notifications: " + response);
		
		
		RuntimeDateUtilities.setSleep();
		RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getDevelopmentNotificationDateModification(m_url, productID, seasonID, dateFormat, sessionToken);		
		Assert.assertTrue(responseDev.code ==200, "Runtime development feature file was not updated");
		JSONObject runtimeJson = new JSONObject(responseDev.message);
		Assert.assertEquals(runtimeJson.getJSONArray("notifications").size(), 4, "Incorrect number of notifications in runtime development file");
		
		Assert.assertTrue(runtimeJson.getJSONArray("notifications").getJSONObject(0).getString("name").equals("notifDev1"), "Incorrect first  notification");
		Assert.assertTrue(runtimeJson.getJSONArray("notifications").getJSONObject(0).getString("stage").equals("PRODUCTION"), "Incorrect first  notification stage");
		
		Assert.assertTrue(runtimeJson.getJSONArray("notifications").getJSONObject(1).getString("name").equals("notifProd1"), "Incorrect second  notification");
		Assert.assertTrue(runtimeJson.getJSONArray("notifications").getJSONObject(1).getString("stage").equals("DEVELOPMENT"), "Incorrect second  notification stage");
		
		Assert.assertTrue(runtimeJson.getJSONArray("notifications").getJSONObject(2).getString("name").equals("notifDev2"), "Incorrect third  notification");
		Assert.assertTrue(runtimeJson.getJSONArray("notifications").getJSONObject(2).getString("stage").equals("PRODUCTION"), "Incorrect third  notification stage");
		
		Assert.assertTrue(runtimeJson.getJSONArray("notifications").getJSONObject(3).getString("name").equals("notifProd2"), "Incorrect fourth  notification");
		Assert.assertTrue(runtimeJson.getJSONArray("notifications").getJSONObject(3).getString("stage").equals("DEVELOPMENT"), "Incorrect fourth  notification stage");
		
		RuntimeRestApi.DateModificationResults responseProd = RuntimeDateUtilities.getProductionNotificationDateModification(m_url, productID, seasonID, dateFormat, sessionToken);		
		Assert.assertTrue(responseProd.code ==200, "Runtime production feature file was not changed");
		runtimeJson = new JSONObject(responseProd.message);
		Assert.assertEquals(runtimeJson.getJSONArray("notifications").size(), 2, "Incorrect number of notifications in runtime production file");
		Assert.assertTrue(runtimeJson.getJSONArray("notifications").getJSONObject(0).getString("name").equals("notifDev1"), "Incorrect first production notification");
		Assert.assertTrue(runtimeJson.getJSONArray("notifications").getJSONObject(1).getString("name").equals("notifDev2"), "Incorrect second production notification");
		
		RuntimeRestApi.DateModificationResults prodChanged = RuntimeDateUtilities.getProductionChangedDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(prodChanged.code ==200, "productionChanged.txt file was not changed");

	}
	
	
	@Test(dependsOnMethods="updateStage", description="change notifications order")
	public void changeOrder() throws JSONException, InterruptedException, IOException{
		String dateFormat = RuntimeDateUtilities.setDateFormat();
		
		String allNotif = notifApi.getAllNotifications(seasonID, sessionToken);
		JSONObject allNotifJson = new JSONObject(allNotif);
		JSONArray newNotifications = new JSONArray();
		newNotifications.add(allNotifJson.getJSONArray("notifications").getJSONObject(3));
		newNotifications.add(allNotifJson.getJSONArray("notifications").getJSONObject(2));
		newNotifications.add(allNotifJson.getJSONArray("notifications").getJSONObject(1));
		newNotifications.add(allNotifJson.getJSONArray("notifications").getJSONObject(0));
		allNotifJson.put("notifications", newNotifications);		
		
		String response = notifApi.updateAllNotifications(seasonID, allNotifJson.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Can't update notifications: " + response);
		
		
		RuntimeDateUtilities.setSleep();
		RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getDevelopmentNotificationDateModification(m_url, productID, seasonID, dateFormat, sessionToken);		
		Assert.assertTrue(responseDev.code ==200, "Runtime development feature file was not updated");
		JSONObject runtimeJson = new JSONObject(responseDev.message);
		Assert.assertEquals(runtimeJson.getJSONArray("notifications").size(), 4, "Incorrect number of notifications in runtime development file");
		
		Assert.assertTrue(runtimeJson.getJSONArray("notifications").getJSONObject(0).getString("name").equals("notifProd2"), "Incorrect first  notification");
		Assert.assertTrue(runtimeJson.getJSONArray("notifications").getJSONObject(1).getString("name").equals("notifDev2"), "Incorrect second  notification");
		Assert.assertTrue(runtimeJson.getJSONArray("notifications").getJSONObject(2).getString("name").equals("notifProd1"), "Incorrect third  notification");
		Assert.assertTrue(runtimeJson.getJSONArray("notifications").getJSONObject(3).getString("name").equals("notifDev1"), "Incorrect fourth  notification");
		
		RuntimeRestApi.DateModificationResults responseProd = RuntimeDateUtilities.getProductionNotificationDateModification(m_url, productID, seasonID, dateFormat, sessionToken);		
		Assert.assertTrue(responseProd.code ==200, "Runtime production feature file was not changed");
		runtimeJson = new JSONObject(responseProd.message);
		Assert.assertEquals(runtimeJson.getJSONArray("notifications").size(), 2, "Incorrect number of notifications in runtime production file");
		Assert.assertTrue(runtimeJson.getJSONArray("notifications").getJSONObject(0).getString("name").equals("notifDev2"), "Incorrect first production notification");
		Assert.assertTrue(runtimeJson.getJSONArray("notifications").getJSONObject(1).getString("name").equals("notifDev1"), "Incorrect second production notification");
		
		RuntimeRestApi.DateModificationResults prodChanged = RuntimeDateUtilities.getProductionChangedDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(prodChanged.code ==200, "productionChanged.txt file was not changed");

	}
	
	@Test(dependsOnMethods="changeOrder", description="delete notifications")
	public void deleteNotifications() throws JSONException, InterruptedException, IOException{
		String dateFormat = RuntimeDateUtilities.setDateFormat();
		
		int respCode = notifApi.deleteNotification(notificationProd1, sessionToken);
		Assert.assertTrue(respCode==200, "Can't delete notification1");
		respCode = notifApi.deleteNotification(notificationProd2, sessionToken);
		Assert.assertTrue(respCode==200, "Can't delete notification2");		
		
		
		RuntimeDateUtilities.setSleep();
		RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getDevelopmentNotificationDateModification(m_url, productID, seasonID, dateFormat, sessionToken);		
		Assert.assertTrue(responseDev.code ==200, "Runtime development feature file was not updated");
		JSONObject runtimeJson = new JSONObject(responseDev.message);
		Assert.assertEquals(runtimeJson.getJSONArray("notifications").size(), 2, "Incorrect number of notifications in runtime development file");
		
		Assert.assertTrue(runtimeJson.getJSONArray("notifications").getJSONObject(0).getString("name").equals("notifDev2"), "Incorrect first  notification");
		Assert.assertTrue(runtimeJson.getJSONArray("notifications").getJSONObject(1).getString("name").equals("notifDev1"), "Incorrect second  notification");
		
		RuntimeRestApi.DateModificationResults responseProd = RuntimeDateUtilities.getProductionNotificationDateModification(m_url, productID, seasonID, dateFormat, sessionToken);		
		Assert.assertTrue(responseProd.code ==304, "Runtime production feature file was  changed");
		
		RuntimeRestApi.DateModificationResults prodChanged = RuntimeDateUtilities.getProductionChangedDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(prodChanged.code ==304, "productionChanged.txt file was changed");

	}
	
	@Test(dependsOnMethods="deleteNotifications", description="change stage and order")
	public void changeStageAndOrder() throws JSONException, InterruptedException, IOException{
		String dateFormat = RuntimeDateUtilities.setDateFormat();
		
		String allNotif = notifApi.getAllNotifications(seasonID, sessionToken);
		JSONObject allNotifJson = new JSONObject(allNotif);
		JSONArray newNotifications = new JSONArray();
		newNotifications.add(allNotifJson.getJSONArray("notifications").getJSONObject(1));	//dev1
		newNotifications.add(allNotifJson.getJSONArray("notifications").getJSONObject(0));	//dev2
		allNotifJson.put("notifications", newNotifications);
		allNotifJson.getJSONArray("notifications").getJSONObject(0).put("stage", "DEVELOPMENT");
		allNotifJson.getJSONArray("notifications").getJSONObject(1).put("stage", "DEVELOPMENT");
		
		String response = notifApi.updateAllNotifications(seasonID, allNotifJson.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Can't update notifications: " + response);
		
		
		RuntimeDateUtilities.setSleep();
		RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getDevelopmentNotificationDateModification(m_url, productID, seasonID, dateFormat, sessionToken);		
		Assert.assertTrue(responseDev.code ==200, "Runtime development feature file was not updated");
		JSONObject runtimeJson = new JSONObject(responseDev.message);
		Assert.assertEquals(runtimeJson.getJSONArray("notifications").size(), 2, "Incorrect number of notifications in runtime development file");
		
		Assert.assertTrue(runtimeJson.getJSONArray("notifications").getJSONObject(0).getString("name").equals("notifDev1"), "Incorrect first  notification");
		Assert.assertTrue(runtimeJson.getJSONArray("notifications").getJSONObject(1).getString("name").equals("notifDev2"), "Incorrect second  notification");
		
		RuntimeRestApi.DateModificationResults responseProd = RuntimeDateUtilities.getProductionNotificationDateModification(m_url, productID, seasonID, dateFormat, sessionToken);		
		Assert.assertTrue(responseProd.code ==200, "Runtime production feature file was  changed");
		runtimeJson = new JSONObject(responseProd.message);
		Assert.assertEquals(runtimeJson.getJSONArray("notifications").size(), 0, "Incorrect number of notifications in runtime production file");
		
		RuntimeRestApi.DateModificationResults prodChanged = RuntimeDateUtilities.getProductionChangedDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(prodChanged.code ==200, "productionChanged.txt file was not changed");

	}
	
	@Test(dependsOnMethods="changeStageAndOrder", description="add new notification in update")
	public void addNewNotification() throws JSONException, InterruptedException, IOException{
		String dateFormat = RuntimeDateUtilities.setDateFormat();
		
		String allNotif = notifApi.getAllNotifications(seasonID, sessionToken);
		JSONObject allNotifJson = new JSONObject(allNotif);
		
		String notification = FileUtils.fileToString(filePath + "notifications/notification1.txt", "UTF-8", false);
		JSONObject json = new JSONObject(notification);
		json.put("name", "notifNew1");
		json.put("uniqueId", UUID.randomUUID().toString());
		allNotifJson.getJSONArray("notifications").add(json);

		String response = notifApi.updateAllNotifications(seasonID, allNotifJson.toString(), sessionToken);
		Assert.assertTrue(response.contains("error"), "Added new notification on update notifications: " + response);
		
		
		RuntimeDateUtilities.setSleep();
		RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getDevelopmentNotificationDateModification(m_url, productID, seasonID, dateFormat, sessionToken);		
		Assert.assertTrue(responseDev.code ==304, "Runtime development feature file was  updated");
		
		RuntimeRestApi.DateModificationResults responseProd = RuntimeDateUtilities.getProductionNotificationDateModification(m_url, productID, seasonID, dateFormat, sessionToken);		
		Assert.assertTrue(responseProd.code ==304, "Runtime production feature file was  changed");
		
		RuntimeRestApi.DateModificationResults prodChanged = RuntimeDateUtilities.getProductionChangedDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(prodChanged.code ==304, "productionChanged.txt file was  changed");

	}
	
	@Test(dependsOnMethods="addNewNotification", description="delete notification in update")
	public void deleteNotificationOnUpdate() throws JSONException, InterruptedException, IOException{
		String dateFormat = RuntimeDateUtilities.setDateFormat();
		
		String allNotif = notifApi.getAllNotifications(seasonID, sessionToken);
		JSONObject allNotifJson = new JSONObject(allNotif);
		allNotifJson.getJSONArray("notifications").remove(0);
		
		String response = notifApi.updateAllNotifications(seasonID, allNotifJson.toString(), sessionToken);
		Assert.assertTrue(response.contains("error"), "Deleted notification on update notifications: " + response);
		
		
		RuntimeDateUtilities.setSleep();
		RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getDevelopmentNotificationDateModification(m_url, productID, seasonID, dateFormat, sessionToken);		
		Assert.assertTrue(responseDev.code ==304, "Runtime development feature file was  updated");
		
		RuntimeRestApi.DateModificationResults responseProd = RuntimeDateUtilities.getProductionNotificationDateModification(m_url, productID, seasonID, dateFormat, sessionToken);		
		Assert.assertTrue(responseProd.code ==304, "Runtime production feature file was  changed");
		
		RuntimeRestApi.DateModificationResults prodChanged = RuntimeDateUtilities.getProductionChangedDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(prodChanged.code ==304, "productionChanged.txt file was  changed");

	}
	
	
	@AfterTest
	public void reset(){
		baseUtils.reset(productID, sessionToken);
	}

	

}
