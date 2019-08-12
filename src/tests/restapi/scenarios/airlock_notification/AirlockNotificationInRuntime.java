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
import tests.restapi.RuntimeDateUtilities;
import tests.restapi.RuntimeRestApi;


public class AirlockNotificationInRuntime {
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
	
	@Test (description="add dev notification")
	public void addDevNotification() throws JSONException, IOException, InterruptedException{
		String dateFormat = RuntimeDateUtilities.setDateFormat();
		
		String notification = FileUtils.fileToString(filePath + "notifications/notification1.txt", "UTF-8", false);
		JSONObject json = new JSONObject(notification);
		notificationID = notifApi.createNotification(seasonID, json.toString(), sessionToken);
		Assert.assertFalse(notificationID.contains("error"), "Can't create notification: " + notificationID);
		
		//check if files were changed
		RuntimeDateUtilities.setSleep();
		RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getDevelopmentNotificationDateModification(m_url, productID, seasonID, dateFormat, sessionToken);		
		Assert.assertTrue(responseDev.code ==200, "Runtime development feature file was not updated");
		JSONObject runtimeJson = new JSONObject(responseDev.message);
		Assert.assertEquals(runtimeJson.getJSONArray("notifications").size(), 1, "Incorrect number of notifications in runtime development file");
		RuntimeRestApi.DateModificationResults responseProd = RuntimeDateUtilities.getProductionNotificationDateModification(m_url, productID, seasonID, dateFormat, sessionToken);		
		Assert.assertTrue(responseProd.code ==304, "Runtime production feature file was changed");
		RuntimeRestApi.DateModificationResults prodChanged = RuntimeDateUtilities.getProductionChangedDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(prodChanged.code ==304, "productionChanged.txt file was changed");

	}
	
	
	@Test (dependsOnMethods="addDevNotification", description="move notification to production")
	public void moveNotificationToProd() throws JSONException, IOException, InterruptedException{
		String dateFormat = RuntimeDateUtilities.setDateFormat();
		
		String notification = notifApi.getNotification(notificationID, sessionToken);
		JSONObject json = new JSONObject(notification);
		json.put("stage", "PRODUCTION");
		String response = notifApi.updateNotification(notificationID, json.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Can't udpate notification: " + response);
		
		//check if files were changed
		RuntimeDateUtilities.setSleep();
		RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getDevelopmentNotificationDateModification(m_url, productID, seasonID, dateFormat, sessionToken);		
		Assert.assertTrue(responseDev.code ==200, "Runtime development feature file was not updated");
		RuntimeRestApi.DateModificationResults responseProd = RuntimeDateUtilities.getProductionNotificationDateModification(m_url, productID, seasonID, dateFormat, sessionToken);		
		Assert.assertTrue(responseProd.code ==200, "Runtime production feature file was not changed");
		JSONObject runtimeJson = new JSONObject(responseProd.message);
		Assert.assertEquals(runtimeJson.getJSONArray("notifications").size(), 1, "Incorrect number of notifications in runtime production file");

		RuntimeRestApi.DateModificationResults prodChanged = RuntimeDateUtilities.getProductionChangedDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(prodChanged.code ==200, "productionChanged.txt file was not changed");

	}
	
	@Test (dependsOnMethods="moveNotificationToProd", description="move notification to development")
	public void moveNotificationToDev() throws JSONException, IOException, InterruptedException{
		String dateFormat = RuntimeDateUtilities.setDateFormat();
		
		String notification = notifApi.getNotification(notificationID, sessionToken);
		JSONObject json = new JSONObject(notification);
		json.put("stage", "DEVELOPMENT");
		String response = notifApi.updateNotification(notificationID, json.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Can't udpate notification: " + response);
		
		//check if files were changed
		RuntimeDateUtilities.setSleep();
		RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getDevelopmentNotificationDateModification(m_url, productID, seasonID, dateFormat, sessionToken);		
		Assert.assertTrue(responseDev.code ==200, "Runtime development feature file was not updated");
		RuntimeRestApi.DateModificationResults responseProd = RuntimeDateUtilities.getProductionNotificationDateModification(m_url, productID, seasonID, dateFormat, sessionToken);		
		Assert.assertTrue(responseProd.code ==200, "Runtime production feature file was not changed");
		JSONObject runtimeJson = new JSONObject(responseProd.message);
		Assert.assertEquals(runtimeJson.getJSONArray("notifications").size(), 0, "Incorrect number of notifications in runtime production file");
		RuntimeRestApi.DateModificationResults prodChanged = RuntimeDateUtilities.getProductionChangedDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(prodChanged.code ==200, "productionChanged.txt file was not changed");

	}

	

	@AfterTest
	public void reset(){
		baseUtils.reset(productID, sessionToken);
	}

	

}
