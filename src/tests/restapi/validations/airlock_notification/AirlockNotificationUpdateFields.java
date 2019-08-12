package tests.restapi.validations.airlock_notification;

import java.util.UUID;

import org.apache.wink.json4j.JSON;
import org.apache.wink.json4j.JSONArray;
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
		
		String notification = FileUtils.fileToString(filePath + "notifications/notification1.txt", "UTF-8", false);		
		notificationID = notifApi.createNotification(seasonID, notification, sessionToken);
		Assert.assertFalse(notificationID.contains("error"), "Can't create notification: " + notificationID);

		
	}
	
	
	@Test
	private void maxNotificationsField() throws Exception{
		JSONObject json = getNotification();
		json.remove("maxNotifications");
		updateNotification(json, "maxNotifications", true);
		
		json = getNotification();
		json.put("maxNotifications", "");
		updateNotification(json, "maxNotifications", true);
		
		json = getNotification();
		json.put("maxNotifications", JSON.NULL);
		updateNotification(json, "maxNotifications", true);
		
		json = getNotification();
		json.put("maxNotifications", -5);
		updateNotification(json, "maxNotifications", true);
	}
	
	@Test
	private void minIntervalField() throws Exception{
		JSONObject json = getNotification();
		json.remove("minInterval");
		updateNotification(json, "minInterval", true);
		
		json = getNotification();
		json.put("minInterval", "");
		updateNotification(json, "minInterval", true);
		
		json = getNotification();
		json.put("minInterval", JSON.NULL);
		updateNotification(json, "minInterval", true);
		
		json = getNotification();
		json.put("minInterval", -5);
		updateNotification(json, "minInterval", true);
	}

	
	@Test
	private void stageField() throws Exception{
		JSONObject json = getNotification();
		json.remove("stage");
		updateNotification(json, "stage", true);
		
		json = getNotification();
		json.put("stage", "");
		updateNotification(json, "stage", true);
		
		json = getNotification();
		json.put("stage", JSON.NULL);
		updateNotification(json, "stage", true);
	}

	@Test
	private void nameField() throws Exception{
		JSONObject json = getNotification();
		json.remove("name");
		updateNotification(json, "name", true);
		
		json = getNotification();
		json.put("name", "");
		updateNotification(json, "name", true);
		
		json = getNotification();
		json.put("name", JSON.NULL);
		updateNotification(json, "name", true);
	}
	
	
	@Test
	private void descriptionField() throws Exception{
		JSONObject json = getNotification();
		json.remove("description");
		updateNotification(json, "description", false);
		
		json = getNotification();
		json.put("description", "");
		updateNotification(json, "description", false);
		
		json = getNotification();
		json.put("description", JSON.NULL);
		updateNotification(json, "description", false);
	}
	
	@Test
	private void displayNameField() throws Exception{
		JSONObject json = getNotification();
		json.remove("displayName");
		updateNotification(json, "displayName", false);
		
		json = getNotification();
		json.put("displayName", "");
		updateNotification(json, "displayName", false);
		
		json = getNotification();
		json.put("displayName", JSON.NULL);
		updateNotification(json, "displayName", false);
	}
	
	@Test
	private void enabledField() throws Exception{
		JSONObject json = getNotification();
		json.remove("enabled");
		updateNotification(json, "enabled", true);
		
		json = getNotification();
		json.put("enabled", "");
		updateNotification(json, "enabled", true);
		
		json = getNotification();
		json.put("enabled", JSON.NULL);
		updateNotification(json, "enabled", true);
	}
	
	@Test
	private void uniqueIdField() throws Exception{
		JSONObject json = getNotification();
		json = getNotification();
		json.put("uniqueId", "");
		updateNotification(json, "uniqueId", true);
		
		json = getNotification();
		json.put("uniqueId", JSON.NULL);
		updateNotification(json, "uniqueId", false);
		
		json = getNotification();
		json.put("uniqueId", UUID.randomUUID());
		updateNotification(json, "uniqueId", true);
		
		json = getNotification();
		json.remove("uniqueId");
		updateNotification(json, "uniqueId", false);
	}
	
	@Test
	private void seasonIdField() throws Exception{
		JSONObject json = getNotification();
		json = getNotification();
		json.put("seasonId", "");
		updateNotification(json, "seasonId", true);
		
		json = getNotification();
		json.put("seasonId", JSON.NULL);
		updateNotification(json, "seasonId", true);
		
		json = getNotification();
		json.put("seasonId", UUID.randomUUID());
		updateNotification(json, "seasonId", true);
		
		json = getNotification();
		json.remove("seasonId");
		updateNotification(json, "seasonId", true);
	}
	
	@Test
	private void creationDateField() throws Exception{
		JSONObject json = getNotification();
		json = getNotification();
		json.put("creationDate", System.currentTimeMillis());
		updateNotification(json, "creationDate", true);

		json = getNotification();
		json.put("creationDate", "");
		updateNotification(json, "creationDate", true);
		
		json = getNotification();
		json.put("creationDate", JSON.NULL);
		updateNotification(json, "creationDate", true);
	}
	
	@Test
	private void creatorField() throws Exception{
		JSONObject json = getNotification();
		json.remove("creator");
		updateNotification(json, "creator", true);
		
		json = getNotification();
		json.put("creator", "");
		updateNotification(json, "creator", true);
		
		json = getNotification();
		json.put("creator", JSON.NULL);
		updateNotification(json, "creator", true);
	}
	
	
	@Test
	private void internalUserGroupsField() throws Exception{
		JSONObject json = getNotification();
		json.remove("internalUserGroups");
		updateNotification(json, "internalUserGroups", false);
		
		json = getNotification();
		json.put("internalUserGroups", "");
		updateNotification(json, "internalUserGroups", true);
		
		json = getNotification();
		json.put("internalUserGroups", JSON.NULL);
		updateNotification(json, "internalUserGroups", false);
		
		json = getNotification();
		json.put("internalUserGroups", new JSONArray());
		updateNotification(json, "internalUserGroups", false);

	}
	
	@Test
	private void rolloutPercentageField() throws Exception{
		JSONObject json = getNotification();
		json.remove("rolloutPercentage");
		updateNotification(json, "rolloutPercentage", true);
		
		json = getNotification();
		json.put("rolloutPercentage", "");
		updateNotification(json, "rolloutPercentage", true);
		
		json = getNotification();
		json.put("rolloutPercentage", JSON.NULL);
		updateNotification(json, "rolloutPercentage", true);
	}
	
	@Test
	private void minAppVersionField() throws Exception{
		JSONObject json = getNotification();
		json.remove("minAppVersion");
		updateNotification(json, "minAppVersion", true);
		
		json = getNotification();
		json.put("minAppVersion", "");
		updateNotification(json, "minAppVersion", true);
		
		json = getNotification();
		json.put("minAppVersion", JSON.NULL);
		updateNotification(json, "minAppVersion", true);
	}
	
	@Test
	private void lastModifiedField() throws Exception{

		JSONObject json = getNotification();
		json.put("lastModified", "");
		updateNotification(json, "lastModified", true);
		
		json = getNotification();
		json.put("lastModified", JSON.NULL);
		updateNotification(json, "lastModified", true);
		
		json = getNotification();
		json.remove("lastModified");
		updateNotification(json, "lastModified", true);
	}
	
	@Test
	private void ownerField() throws Exception{
		JSONObject json = getNotification();
		json.remove("owner");
		updateNotification(json, "owner", false);
		
		json = getNotification();
		json.put("owner", "");
		updateNotification(json, "owner", false);
		
		json = getNotification();
		json.put("owner", JSON.NULL);
		updateNotification(json, "owner", false);
	}
	
	@Test
	private void cancellationRuleField() throws Exception{
		JSONObject json = getNotification();
		json.remove("cancellationRule");
		updateNotification(json, "cancellationRule", true);
		
		json = getNotification();
		json.put("cancellationRule", "");
		updateNotification(json, "cancellationRule", true);
		
		json = getNotification();
		json.put("cancellationRule", JSON.NULL);
		updateNotification(json, "cancellationRule", true);
	}
	
	@Test
	private void registrationRuleField() throws Exception{
		JSONObject json = getNotification();
		json.remove("registrationRule");
		updateNotification(json, "registrationRule", true);
		
		json = getNotification();
		json.put("registrationRule", "");
		updateNotification(json, "registrationRule", true);
		
		json = getNotification();
		json.put("registrationRule", JSON.NULL);
		updateNotification(json, "registrationRule", true);
	}
	
	@Test
	private void configurationField() throws Exception{
		JSONObject json = getNotification();
		json.remove("configuration");
		updateNotification(json, "configuration", true);
		
		json = getNotification();
		json.put("configuration", "");
		updateNotification(json, "configuration", true);
		
		json = getNotification();
		json.put("configuration", JSON.NULL);
		updateNotification(json, "configuration", true);
	}
	
	
	private void updateNotification(JSONObject json, String field, boolean expectedResult){

			String response = notifApi.updateNotification(notificationID, json.toString(), sessionToken);
			Assert.assertEquals(response.contains("error"), expectedResult,  "Test failed for field: " + field);
	}
	
	private JSONObject getNotification() throws Exception{
		String notification = notifApi.getNotification(notificationID, sessionToken);
		JSONObject json = new JSONObject(notification);
		return json;
	}
	


	
	@AfterTest
	public void reset(){
		baseUtils.reset(productID, sessionToken);
	}

	

}
