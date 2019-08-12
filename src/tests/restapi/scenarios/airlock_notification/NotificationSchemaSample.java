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



public class NotificationSchemaSample {
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

	//there is default notification schema

	
	@Test (description="add notification")
	public void validateNotificationSchemaSample() throws JSONException, IOException, InterruptedException{

		String notifications = notifApi.getAllNotifications(seasonID, sessionToken);
		JSONObject jsonNotification = new JSONObject(notifications);
		String notificationSchema = FileUtils.fileToString(filePath + "notifications/notificationSchema1.txt", "UTF-8", false);
		jsonNotification.put("configurationSchema", new JSONObject(notificationSchema));
		String result = notifApi.updateAllNotifications(seasonID, jsonNotification.toString(), sessionToken);
		Assert.assertFalse(result.contains("error"), "Can't change schema");
		
		String sample = notifApi.getNotificationOutputSample(seasonID, sessionToken);
		JSONObject sampleObj = new JSONObject(sample);
		int count = sampleObj.getJSONObject("notification").size();
		
		Assert.assertTrue(count == 7, "Incorrect number of keys in schema");
		Assert.assertTrue(sampleObj.getJSONObject("notification").containsKey("thumbnail"), "can't find optional key thumbnail in the sample");
		Assert.assertTrue(sampleObj.getJSONObject("notification").containsKey("dueDate"), "can't find optional key dueDate in the sample");
		Assert.assertTrue(sampleObj.getJSONObject("notification").containsKey("sound"), "can't find optional key sound in the sample");
		Assert.assertTrue(sampleObj.getJSONObject("notification").containsKey("additionalInfo"), "can't find optional key additionalInfo in the sample");
		Assert.assertTrue(sampleObj.getJSONObject("notification").containsKey("text"), "can't find optional key text in the sample");
		Assert.assertTrue(sampleObj.getJSONObject("notification").containsKey("actions"), "can't find optional key actions in the sample");
		Assert.assertTrue(sampleObj.getJSONObject("notification").containsKey("title"), "can't find required key title in the sample");
		

		
		//update schema
		notifications = notifApi.getAllNotifications(seasonID, sessionToken);
		jsonNotification = new JSONObject(notifications);
		notificationSchema = FileUtils.fileToString(filePath + "notifications/notificationSchema3.txt", "UTF-8", false);
		jsonNotification.put("configurationSchema", new JSONObject(notificationSchema));
		result = notifApi.updateAllNotifications(seasonID, jsonNotification.toString(), sessionToken);
		Assert.assertFalse(result.contains("error"), "Can't change schema2");
		
		sample = notifApi.getNotificationOutputSample(seasonID, sessionToken);
		sampleObj = new JSONObject(sample);
		count = sampleObj.getJSONObject("notification").size();
		
		Assert.assertTrue(count == 3, "Incorrect number of keys in schema2");
		Assert.assertTrue(sampleObj.getJSONObject("notification").containsKey("dueDate"), "can't find optional key dueDate in the sample");
		Assert.assertTrue(sampleObj.getJSONObject("notification").containsKey("text"), "can't find required key text in the sample");
		Assert.assertTrue(sampleObj.getJSONObject("notification").containsKey("title"), "can't find required key title in the sample");
	}

	
	@AfterTest
	public void reset(){
		baseUtils.reset(productID, sessionToken);
	}

	

}
