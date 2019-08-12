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


public class AirlockNotificationUpdateFields {
	protected String seasonID;
	protected String filePath;
	protected String m_url;
	private AirlocklNotificationRestApi notifApi;
	private AirlockUtils baseUtils;
	protected String productID;
	private String sessionToken = "";
	private String notificationID;
	
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
	
	@Test (description="update and validate field values")
	public void testFields() throws JSONException, IOException{
		String notification = FileUtils.fileToString(filePath + "notifications/notification1.txt", "UTF-8", false);
		notificationID = notifApi.createNotification(seasonID, notification, sessionToken);
		Assert.assertFalse(notificationID.contains("error"), "Can't create notification: " + notificationID);
		
		
		JSONObject json1 = new JSONObject(notifApi.getNotification(notificationID, sessionToken));
		json1.put("name", "new notif name");
		json1.put("displayName", "new notif displayName");
		json1.put("description", "new notif description");
		//json1.put("stage", "PRODUCTION");	//verify in another test

		json1.put("minAppVersion", "5.0");
		json1.put("owner", "vicky1");
		json1.put("enabled", false);
		json1.put("rolloutPercentage", 55);
		json1.put("minInterval", 5);
		json1.put("maxNotifications", 5);
		
		JSONArray userGroups = new JSONArray();
		userGroups.add("QA");
		json1.put("internalUserGroups", userGroups);
		
		JSONObject rule = new JSONObject();
		rule.put("ruleString", "var a = true; if (a){true;} else {false;}");

		json1.put("cancellationRule", rule);
		json1.put("registrationRule", rule);
		
		
		String response = notifApi.updateNotification(notificationID, json1.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Can't update notification: " + response);
		JSONObject json2 = new JSONObject(notifApi.getNotification(notificationID, sessionToken));
		
		validateParameter(json1.getString("name"), json2.getString("name"), "name");
		validateParameter(json1.getString("displayName"), json2.getString("displayName"), "displayName");
		validateParameter(json1.getString("stage"), json2.getString("stage"), "stage");
		validateParameter(json1.getString("creator"), json2.getString("creator"), "creator");
		validateParameter(json1.getString("description"), json2.getString("description"), "description");
		validateParameter(json1.getString("minAppVersion"), json2.getString("minAppVersion"), "minAppVersion");
		validateParameter(json1.getString("owner"), json2.getString("owner"), "owner");
		Assert.assertEquals(json1.getBoolean("enabled"), json2.getBoolean("enabled"), "Parameter enabled differs from the expected.");
		Assert.assertEquals(json1.getDouble("rolloutPercentage"), json2.getDouble("rolloutPercentage"), "Parameter rolloutPercentage differs from the expected.");
		Assert.assertEquals(json1.getJSONArray("internalUserGroups"), json2.getJSONArray("internalUserGroups"), "Parameter internalUserGroups differs from the expected.");
		Assert.assertEquals(json1.getJSONObject("cancellationRule"), json2.getJSONObject("cancellationRule"), "Parameter cancellationRule differs from the expected.");
		Assert.assertEquals(json1.getJSONObject("registrationRule"), json2.getJSONObject("registrationRule"), "Parameter registrationRule differs from the expected.");
		Assert.assertEquals(json1.getInt("minInterval"), json2.getInt("minInterval"), "Parameter minInterval differs from the expected.");
		Assert.assertEquals(json1.getInt("maxNotifications"), json2.getInt("maxNotifications"), "Parameter maxNotifications differs from the expected.");

	}
	
	
	@Test (dependsOnMethods="testFields", description="update and validate field values")
	public void negativeTestFields() throws JSONException {
		JSONObject json1 = new JSONObject(notifApi.getNotification(notificationID, sessionToken));
		json1.put("creationDate", System.currentTimeMillis());
		json1.put("lastModified", System.currentTimeMillis());
		notifApi.updateNotification(notificationID, json1.toString(), sessionToken);
		JSONObject resultJson = new JSONObject(notifApi.getNotification(notificationID, sessionToken));		
		Assert.assertNotEquals(json1.getLong("creationDate"), resultJson.getLong("creationDate"), "Incorrect value for field creationDate ");
		Assert.assertNotEquals(json1.getLong("lastModified"), resultJson.getLong("lastModified"), "Incorrect value for field lastModified ");
		
		
		json1 = new JSONObject(notifApi.getNotification(notificationID, sessionToken));
		json1.put("creator", "vicky1");		//can't change creator in update
		String response = notifApi.updateNotification(notificationID, json1.toString(), sessionToken);
		Assert.assertTrue(response.contains("error"), "Incorrect update for field creator");
	}

	private void validateParameter(String oldString, String newString, String param){
	
		Assert.assertEquals(newString, oldString, "Parameter " + param + " differs from the expected.");
	
	}
	


	@AfterTest
	public void reset(){
		baseUtils.reset(productID, sessionToken);
	}

	

}
