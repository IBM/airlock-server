package tests.restapi.scenarios.airlock_notification;

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
import tests.restapi.AirlocklNotificationRestApi;



public class NotificationResultSchema {
	protected String seasonID;
	protected String filePath;
	protected String m_url;
	private AirlocklNotificationRestApi notifApi;
	private AirlockUtils baseUtils;
	protected String productID;
	private String sessionToken = "";
	private String notificationID;
	protected Config config;
	
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
		
		config = new Config(url, analyticsUrl, translationsUrl, configPath, sToken, userName, userPassword, appName, productsToDeleteFile);
	}

	/*
	 * add notification without adding required fields in configuration
	 * add notification with required fields in configuration 
	 * add new required field in schema without adding it in configuration
	 * remove from schema required field used in configuration
	 */
	//there is default notification schema
	@Test ( description="add notification without configuration fields")
	public void addNotificationNoConfiguration() throws JSONException, IOException, InterruptedException{
		String notification = FileUtils.fileToString(filePath + "notifications/notification1.txt", "UTF-8", false);
		JSONObject json = new JSONObject(notification);
		json.put("configuration", "");
		notificationID = notifApi.createNotification(seasonID, json.toString(), sessionToken);
		Assert.assertTrue(notificationID.contains("error"), "Created notification without using required schema fields");

	}
	
	
	@Test (dependsOnMethods="addNotificationNoConfiguration", description="add notification with configuration fields")
	public void addNotificationWithConfiguration() throws JSONException, IOException, InterruptedException{
		String notification = FileUtils.fileToString(filePath + "notifications/notification1.txt", "UTF-8", false);
		JSONObject json = new JSONObject(notification) ;
		String configuration = config.setConfiguration(new String[]{"title"}, "stam", false);
		//String configuration = "{\"title\":\"some title\", \"text\":\"some text\", \"additionalField\":\"stam vicky\"}";
		json.put("configuration", configuration);
				
		notificationID = notifApi.createNotification(seasonID, json.toString(), sessionToken);
		Assert.assertFalse(notificationID.contains("error"), "Can't create notification : " + notificationID);

	}
	
	@Test (dependsOnMethods="addNotificationWithConfiguration", description="add new required field to schema")
	public void updateSchema() throws JSONException, IOException, InterruptedException{
		String notifications = notifApi.getAllNotifications(seasonID, sessionToken);
		JSONObject jsonNotification = new JSONObject(notifications);
		String notificationSchema = FileUtils.fileToString(filePath + "notifications/notificationSchema2.txt", "UTF-8", false);
		jsonNotification.put("configurationSchema", new JSONObject(notificationSchema));
		String result = notifApi.updateAllNotifications(seasonID, jsonNotification.toString(), sessionToken);
		Assert.assertTrue(result.contains("error"), "required field was added to schema but not to notification configuration");

	}
	

	@Test (dependsOnMethods="updateSchema", description="verify schema fields types")
	public void updateSchema2() throws JSONException, IOException, InterruptedException{
		String notifications = notifApi.getAllNotifications(seasonID, sessionToken);
		JSONObject jsonNotification = new JSONObject(notifications);
		String notificationSchema = FileUtils.fileToString(filePath + "notifications/notificationSchema1.txt", "UTF-8", false);
		jsonNotification.put("configurationSchema", new JSONObject(notificationSchema));
		String response = notifApi.updateAllNotifications(seasonID, jsonNotification.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Schema was not updated: " + response);	
		
		
		String notification = notifApi.getNotification(notificationID, sessionToken);
		JSONObject jsonN = new JSONObject(notification);
		//title is required; dueDate must be integer
		String configuration = "{\"title\":\"some title\", \"dueDate\":\"some text\"}";
		jsonN.put("configuration", configuration);
		response = notifApi.updateNotification(notificationID, jsonN.toString(), sessionToken);
		Assert.assertTrue(response.contains("Validation error"), "Update notification with invalid field type dueDate");

		notification = notifApi.getNotification(notificationID, sessionToken);
		jsonN = new JSONObject(notification);
		//title is required; additionalInfo must be json object
		configuration = "{\"title\":\"some title\", \"additionalInfo\":\"some text\"}";
		jsonN.put("configuration", configuration);
		response = notifApi.updateNotification(notificationID, jsonN.toString(), sessionToken);
		Assert.assertTrue(response.contains("Validation error"), "Update notification with invalid field type additionalInfo");

		notification = notifApi.getNotification(notificationID, sessionToken);
		jsonN = new JSONObject(notification);
		//title is required; actions must be json array
		configuration = "{\"title\":\"some title\", \"actions\":\"some text\"}";
		jsonN.put("configuration", configuration);
		response = notifApi.updateNotification(notificationID, jsonN.toString(), sessionToken);
		Assert.assertTrue(response.contains("Validation error"), "Update notification with invalid field type actions");

		notification = notifApi.getNotification(notificationID, sessionToken);
		jsonN = new JSONObject(notification);
		//title is required; sound must be string
		configuration = "{\"title\":\"some title\", \"sound\":1.0}";
		jsonN.put("configuration", configuration);
		response = notifApi.updateNotification(notificationID, jsonN.toString(), sessionToken);
		Assert.assertTrue(response.contains("Validation error"), "Update notification with invalid field type sound");

	}
	
	@AfterTest
	public void reset(){
		baseUtils.reset(productID, sessionToken);
	}

	

}
