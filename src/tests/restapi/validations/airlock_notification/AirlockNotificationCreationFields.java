package tests.restapi.validations.airlock_notification;

import java.io.IOException;








import java.util.UUID;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.wink.json4j.JSON;
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


public class AirlockNotificationCreationFields {
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

		notification = FileUtils.fileToString(filePath + "notifications/notification1.txt", "UTF-8", false);		
		notifApi = new AirlocklNotificationRestApi();
		notifApi.setUrl(url);
		
	}

	
	@Test
	private void maxNotificationsField() throws IOException, JSONException{
		JSONObject json = new JSONObject(notification);
		json.remove("maxNotifications");
		addNotification(json, "maxNotifications", true);
		
		json = new JSONObject(notification);
		json.put("maxNotifications", "");
		addNotification(json, "maxNotifications", true);
		
		json = new JSONObject(notification);
		json.put("maxNotifications", JSON.NULL);
		addNotification(json, "maxNotifications", true);
		
		json = new JSONObject(notification);
		json.put("maxNotifications", -5);
		addNotification(json, "maxNotifications", true);
	}
	
	@Test
	private void minIntervalField() throws IOException, JSONException{
		JSONObject json = new JSONObject(notification);
		json.remove("minInterval");
		addNotification(json, "minInterval", true);
		
		json = new JSONObject(notification);
		json.put("minInterval", "");
		addNotification(json, "minInterval", true);
		
		json = new JSONObject(notification);
		json.put("minInterval", JSON.NULL);
		addNotification(json, "minInterval", true);
		
		json = new JSONObject(notification);
		json.put("minInterval", -5);
		addNotification(json, "minInterval", true);
	}
	
	@Test
	private void stageField() throws IOException, JSONException{
		JSONObject json = new JSONObject(notification);
		json.remove("stage");
		addNotification(json, "stage", true);
		
		json = new JSONObject(notification);
		json.put("stage", "");
		addNotification(json, "stage", true);
		
		json = new JSONObject(notification);
		json.put("stage", JSON.NULL);
		addNotification(json, "stage", true);
	}

	@Test
	private void nameField() throws IOException, JSONException{
		JSONObject json = new JSONObject(notification);
		json.remove("name");
		addNotification(json, "name", true);
		
		json = new JSONObject(notification);
		json.put("name", "");
		addNotification(json, "name", true);
		
		json = new JSONObject(notification);
		json.put("name", JSON.NULL);
		addNotification(json, "name", true);
	}
	
	
	@Test
	private void descriptionField() throws IOException, JSONException{
		JSONObject json = new JSONObject(notification);
		json.remove("description");
		addNotification(json, "description", false);
		
		json = new JSONObject(notification);
		json.put("description", "");
		addNotification(json, "description", false);
		
		json = new JSONObject(notification);
		json.put("description", JSON.NULL);
		addNotification(json, "description", false);
	}
	
	@Test
	private void displayNameField() throws IOException, JSONException{
		JSONObject json = new JSONObject(notification);
		json.remove("displayName");
		addNotification(json, "displayName", false);
		
		json = new JSONObject(notification);
		json.put("displayName", "");
		addNotification(json, "displayName", false);
		
		json = new JSONObject(notification);
		json.put("displayName", JSON.NULL);
		addNotification(json, "displayName", false);
	}
	
	@Test
	private void enabledField() throws IOException, JSONException{
		JSONObject json = new JSONObject(notification);
		json.remove("enabled");
		addNotification(json, "enabled", true);
		
		json = new JSONObject(notification);
		json.put("enabled", "");
		addNotification(json, "enabled", true);
		
		json = new JSONObject(notification);
		json.put("enabled", JSON.NULL);
		addNotification(json, "enabled", true);
	}
	
	@Test
	private void uniqueIdField() throws IOException, JSONException{
		JSONObject json = new JSONObject(notification);
		json = new JSONObject(notification);
		json.put("uniqueId", "");
		addNotification(json, "uniqueId", true);
		
		json = new JSONObject(notification);
		json.put("uniqueId", JSON.NULL);
		addNotification(json, "uniqueId", false);
		
		json = new JSONObject(notification);
		json.put("uniqueId", UUID.randomUUID());
		addNotification(json, "uniqueId", true);
	}
	
	@Test
	private void seasonIdField() throws IOException, JSONException{
		JSONObject json = new JSONObject(notification);
		json = new JSONObject(notification);
		json.put("seasonId", "");
		addNotification(json, "seasonId", true);
		
		json = new JSONObject(notification);
		json.put("seasonId", JSON.NULL);
		addNotification(json, "seasonId", false);
		
		json = new JSONObject(notification);
		json.put("seasonId", UUID.randomUUID());
		addNotification(json, "seasonId", true);
	}
	
	@Test
	private void creationDateField() throws IOException, JSONException{
		JSONObject json = new JSONObject(notification);
		json = new JSONObject(notification);
		json.put("creationDate", System.currentTimeMillis());
		addNotification(json, "creationDate", true);

		json = new JSONObject(notification);
		json.put("creationDate", "");
		addNotification(json, "creationDate", true);
		
		json = new JSONObject(notification);
		json.put("creationDate", JSON.NULL);
		addNotification(json, "creationDate", false);
	}
	
	@Test
	private void creatorField() throws IOException, JSONException{
		JSONObject json = new JSONObject(notification);
		json.remove("creator");
		addNotification(json, "creator", true);
		
		json = new JSONObject(notification);
		json.put("creator", "");
		addNotification(json, "creator", true);
		
		json = new JSONObject(notification);
		json.put("creator", JSON.NULL);
		addNotification(json, "creator", true);
	}
	
	
	@Test
	private void internalUserGroupsField() throws IOException, JSONException{
		JSONObject json = new JSONObject(notification);
		json.remove("internalUserGroups");
		addNotification(json, "internalUserGroups", false);
		
		json = new JSONObject(notification);
		json.put("internalUserGroups", "");
		addNotification(json, "internalUserGroups", true);
		
		json = new JSONObject(notification);
		json.put("internalUserGroups", JSON.NULL);
		addNotification(json, "internalUserGroups", false);
		
		json = new JSONObject(notification);
		json.put("internalUserGroups", new JSONArray());
		addNotification(json, "internalUserGroups", false);

	}
	
	@Test
	private void rolloutPercentageField() throws IOException, JSONException{
		JSONObject json = new JSONObject(notification);
		json.remove("rolloutPercentage");
		addNotification(json, "rolloutPercentage", true);
		
		json = new JSONObject(notification);
		json.put("rolloutPercentage", "");
		addNotification(json, "rolloutPercentage", true);
		
		json = new JSONObject(notification);
		json.put("rolloutPercentage", JSON.NULL);
		addNotification(json, "rolloutPercentage", true);
	}
	
	@Test
	private void minAppVersionField() throws IOException, JSONException{
		JSONObject json = new JSONObject(notification);
		json.remove("minAppVersion");
		addNotification(json, "minAppVersion", true);
		
		json = new JSONObject(notification);
		json.put("minAppVersion", "");
		addNotification(json, "minAppVersion", true);
		
		json = new JSONObject(notification);
		json.put("minAppVersion", JSON.NULL);
		addNotification(json, "minAppVersion", true);
	}
	
	@Test
	private void lastModifiedField() throws IOException, JSONException{
		JSONObject json = new JSONObject(notification);
		json = new JSONObject(notification);
		json.put("lastModified", System.currentTimeMillis()-10000);
		addNotification(json, "lastModified", true);

		json = new JSONObject(notification);
		json.put("lastModified", "");
		addNotification(json, "lastModified", true);
		
		json = new JSONObject(notification);
		json.put("lastModified", JSON.NULL);
		addNotification(json, "lastModified", false);
	}
	
	@Test
	private void ownerField() throws IOException, JSONException{
		JSONObject json = new JSONObject(notification);
		json.remove("owner");
		addNotification(json, "owner", false);
		
		json = new JSONObject(notification);
		json.put("owner", "");
		addNotification(json, "owner", false);
		
		json = new JSONObject(notification);
		json.put("owner", JSON.NULL);
		addNotification(json, "owner", false);
	}
	
	@Test
	private void cancellationRuleField() throws IOException, JSONException{
		JSONObject json = new JSONObject(notification);
		json.remove("cancellationRule");
		addNotification(json, "cancellationRule", true);
		
		json = new JSONObject(notification);
		json.put("cancellationRule", "");
		addNotification(json, "cancellationRule", true);
		
		json = new JSONObject(notification);
		json.put("cancellationRule", JSON.NULL);
		addNotification(json, "cancellationRule", true);
	}
	
	@Test
	private void registrationRuleField() throws IOException, JSONException{
		JSONObject json = new JSONObject(notification);
		json.remove("registrationRule");
		addNotification(json, "registrationRule", true);
		
		json = new JSONObject(notification);
		json.put("registrationRule", "");
		addNotification(json, "registrationRule", true);
		
		json = new JSONObject(notification);
		json.put("registrationRule", JSON.NULL);
		addNotification(json, "registrationRule", true);
	}
	
	@Test
	private void configurationField() throws IOException, JSONException{
		JSONObject json = new JSONObject(notification);
		json.remove("configuration");
		addNotification(json, "configuration", true);
		
		json = new JSONObject(notification);
		json.put("configuration", "");
		addNotification(json, "configuration", true);
		
		json = new JSONObject(notification);
		json.put("configuration", JSON.NULL);
		addNotification(json, "configuration", true);
	}
	
	
	private void addNotification(JSONObject json, String field, boolean expectedResult) throws JSONException{
		if (!field.equals("name"))
			json.put("name", RandomStringUtils.randomAlphabetic(5));

		try {
			String response = notifApi.createNotification(seasonID, json.toString(), sessionToken);
			Assert.assertEquals(response.contains("error"), expectedResult,  "Test failed for field: " + field);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}	
	}
	


	
	@AfterTest
	public void reset(){
		baseUtils.reset(productID, sessionToken);
	}

	

}
