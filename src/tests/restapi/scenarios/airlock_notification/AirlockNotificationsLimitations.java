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


public class AirlockNotificationsLimitations {
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
	
	@Test (description="invalid minInterval")
	public void invalidMinInterval() throws JSONException, IOException{
		String notifications = notifApi.getAllNotifications(seasonID, sessionToken);
		JSONObject json = new JSONObject(notifications);
		JSONObject limitation = new JSONObject();
		limitation.put("minInterval", "abc");
		limitation.put("maxNotifications", 0);
		JSONArray allLimitations = new JSONArray();
		allLimitations.add(limitation);
		json.put("notificationsLimitations", allLimitations);
		updateNotifications(json.toString(), true);

	}
	
	@Test (description="invalid maxNotifications")
	public void invalidMaxNotifications() throws JSONException, IOException{
		String notifications = notifApi.getAllNotifications(seasonID, sessionToken);
		JSONObject json = new JSONObject(notifications);
		JSONObject limitation = new JSONObject();
		limitation.put("maxNotifications", "abc");
		limitation.put("minInterval", 0);
		JSONArray allLimitations = new JSONArray();
		allLimitations.add(limitation);
		json.put("notificationsLimitations", allLimitations);
		updateNotifications(json.toString(), true);

	}
	
	@Test (description="negative limitation value")
	public void negativeLimitation() throws JSONException, IOException{
		String notifications = notifApi.getAllNotifications(seasonID, sessionToken);
		JSONObject json = new JSONObject(notifications);
		JSONObject limitation = new JSONObject();
		limitation.put("maxNotifications", -1);
		limitation.put("minInterval", -1);
		JSONArray allLimitations = new JSONArray();
		allLimitations.add(limitation);
		json.put("notificationsLimitations", allLimitations);
		updateNotifications(json.toString(), true);

	}
	
	
	@Test (description="missing maxNotifications")
	public void missingMaxNotifications() throws JSONException, IOException{
		String notifications = notifApi.getAllNotifications(seasonID, sessionToken);
		JSONObject json = new JSONObject(notifications);
		JSONObject limitation = new JSONObject();
		limitation.put("minInterval", -1);
		JSONArray allLimitations = new JSONArray();
		allLimitations.add(limitation);
		json.put("notificationsLimitations", allLimitations);
		updateNotifications(json.toString(), true);

	}
	
	@Test (description="missing minInterval")
	public void missingMinInterval() throws JSONException, IOException{
		String notifications = notifApi.getAllNotifications(seasonID, sessionToken);
		JSONObject json = new JSONObject(notifications);
		JSONObject limitation = new JSONObject();
		limitation.put("maxNotifications", -1);
		JSONArray allLimitations = new JSONArray();
		allLimitations.add(limitation);
		json.put("notificationsLimitations", allLimitations);
		updateNotifications(json.toString(), true);

	}
	
	private void updateNotifications(String content, boolean expectedResult){
		String response = notifApi.updateAllNotifications(seasonID, content, sessionToken);
		Assert.assertEquals(response.contains("error"), expectedResult,  "Test failed: " + response);
		
	}

	@AfterTest
	public void reset(){
		baseUtils.reset(productID, sessionToken);
	}

	

}
