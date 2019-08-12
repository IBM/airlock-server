package tests.restapi.scenarios.airlock_notification;


import org.testng.Assert;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import tests.restapi.scenarios.airlock_notification.Config;

public class StreamUseInNotification
{

	protected String seasonID;
	protected String filePath;
	protected String m_url;
	protected String notification;
	protected String productID;
	protected Config config;
	protected String streamID;
	
	
	@BeforeClass
	@Parameters({"url", "analyticsUrl", "translationsUrl", "configPath", "sessionToken", "userName", "userPassword", "appName", "productsToDeleteFile"})
	public void init(String url, String analyticsUrl, String translationsUrl, String configPath, String sToken, String userName, String userPassword, String appName, String productsToDeleteFile) throws Exception{
		config = new Config(url, analyticsUrl, translationsUrl, configPath, sToken, userName, userPassword, appName, productsToDeleteFile);
		config.stage = "DEVELOPMENT";
		config.addSeason( "1.1.1");
		streamID = config.createStream("5.0");
		
		config.createSchema();
		
	}

	@Test ( description="Stream field used in cancellationRule")
	public void scenario1() throws Exception
	{

		//stream - 5.0, dev
		
		String notificationID = config.addNotification ("notification1.txt", "cancellationRule", " (context.streams != undefined && context.streams.video_played != undefined && context.streams.video_played.averageAdsTime == \"10\")", "5.0");
		Assert.assertFalse(notificationID.contains("error"), "Notification was not created: " + notificationID);
		
		//1. move stream field to upper minVersion		

        String response =  config.updateStream(streamID, "minAppVersion", "4.0");
        Assert.assertFalse(response.contains("error"), "Stream should be allowed to change - " + response);       
       //move stream field to 6.0
        response =  config.updateStream(streamID, "minAppVersion", "6.0");
        Assert.assertTrue(response.contains("error"), "Stream should not be allowed to change - " + response);
        
        //2. move notification to lower version
        response = config.updateNotificationField(notificationID, "minAppVersion", "2.0");
        Assert.assertTrue(response.contains("error"), "Notification was updated");


        //3. move  field to production , notif in dev
        response = config.updateNotificationField(notificationID, "stage", "PRODUCTION");
        Assert.assertTrue(response.contains("error"), "Notification was updated" + response);
 
        response =  config.updateStream(streamID, "stage", "PRODUCTION");
        Assert.assertFalse(response.contains("error"), "Stream should be allowed to change - " + response);

        response = config.updateNotificationField(notificationID, "stage", "PRODUCTION");
        Assert.assertFalse(response.contains("error"), "Notification was not updated" + response);
        
        //move field to dev 
        response =  config.updateStream(streamID, "stage", "DEVELOPMENT");
        Assert.assertTrue(response.contains("error"), "Stream should not be allowed to change - " + response);
        
        //4. move notification to dev
        response = config.updateNotificationField(notificationID, "stage", "DEVELOPMENT");
        Assert.assertFalse(response.contains("error"), "Notification was not updated: " + response);
        
        response =  config.updateStream(streamID, "stage", "DEVELOPMENT");
        Assert.assertFalse(response.contains("error"), "Stream should be allowed to change - " + response);
 
        //remove stream field used by notification
        response =  config.updateStreamRemoveField(streamID);
        Assert.assertTrue(response.contains("error"), "Stream should not be allowed to change - " + response);
               
        int respCode = config.deleteNotification(notificationID);
        Assert.assertEquals(respCode,  200, "Notification was not deleted");
	}
	
	
	@Test ( description="Schema field used in registrationRule")
	public void scenario2() throws Exception
	{

		//stream - 5.0, dev
		
		String notificationID = config.addNotification ("notification1.txt", "registrationRule", " (context.streams != undefined && context.streams.video_played != undefined && context.streams.video_played.averageAdsTime == \"10\")", "5.0");
		Assert.assertFalse(notificationID.contains("error"), "Notification was not created: " + notificationID);
		
		//1. move stream field to upper minVersion		

        String response =  config.updateStream(streamID, "minAppVersion", "4.0");
        Assert.assertFalse(response.contains("error"), "Stream should be allowed to change - " + response);       
       //move stream field to 6.0
        response =  config.updateStream(streamID, "minAppVersion", "6.0");
        Assert.assertTrue(response.contains("error"), "Stream should not be allowed to change - " + response);
        
        //2. move notification to lower version
        response = config.updateNotificationField(notificationID, "minAppVersion", "2.0");
        Assert.assertTrue(response.contains("error"), "Notification was updated");


        //3. move  field to production , notif in dev
        response = config.updateNotificationField(notificationID, "stage", "PRODUCTION");
        Assert.assertTrue(response.contains("error"), "Notification was updated" + response);

        response =  config.updateStream(streamID, "stage", "PRODUCTION");
        Assert.assertFalse(response.contains("error"), "Stream should be allowed to change - " + response);

        response = config.updateNotificationField(notificationID, "stage", "PRODUCTION");
        Assert.assertFalse(response.contains("error"), "Notification was not updated" + response);
        
        //move field to dev 
        response =  config.updateStream(streamID, "stage", "DEVELOPMENT");
        Assert.assertTrue(response.contains("error"), "Stream should not be allowed to change - " + response);
        
        //4. move notification to dev
        response = config.updateNotificationField(notificationID, "stage", "DEVELOPMENT");
        Assert.assertFalse(response.contains("error"), "Notification was not updated: " + response);
        
        response =  config.updateStream(streamID, "stage", "DEVELOPMENT");
        Assert.assertFalse(response.contains("error"), "Stream should be allowed to change - " + response);
 
        //remove stream field used by notification
        response =  config.updateStreamRemoveField(streamID);
        Assert.assertTrue(response.contains("error"), "Stream should not be allowed to change - " + response);
               
        int respCode = config.deleteNotification(notificationID);
        Assert.assertEquals(respCode,  200, "Notification was not deleted");
	}
	
	@Test ( description="Stream field used in configuration")
	public void scenario3() throws Exception
	{
		//stream doesn't exist
		String response = config.addNotificationWithConfiguration (new String[]{"title"}, "notification1.txt", "configuration", "context.streams.video_played.noField", "4.0", true);
		Assert.assertTrue(response.contains("error"), "Notification was created with non-existing stream fields " + response);
				
		String notificationID = config.addNotificationWithConfiguration (new String[]{"title"},"notification1.txt", "configuration", "context.streams.video_played.averageAdsTime", "4.0", true);
		Assert.assertFalse(notificationID.contains("error"), "Notification was not created: " + notificationID);
		
		//1. move stream field to upper minVersion		

        response =  config.updateStream(streamID, "minAppVersion", "4.0");
        Assert.assertFalse(response.contains("error"), "Stream should be allowed to change - " + response);       
       //move stream field to 6.0
        response =  config.updateStream(streamID, "minAppVersion", "6.0");
        Assert.assertTrue(response.contains("error"), "Stream should not be allowed to change - " + response);
        
        //2. move notification to lower version
        response = config.updateNotificationField(notificationID, "minAppVersion", "2.0");
        Assert.assertTrue(response.contains("error"), "Notification was updated");


        //3. move  field to production , notif in dev
        response = config.updateNotificationField(notificationID, "stage", "PRODUCTION");
        Assert.assertTrue(response.contains("error"), "Notification was updated" + response);

        response =  config.updateStream(streamID, "stage", "PRODUCTION");
        Assert.assertFalse(response.contains("error"), "Stream should be allowed to change - " + response);

        response = config.updateNotificationField(notificationID, "stage", "PRODUCTION");
        Assert.assertFalse(response.contains("error"), "Notification was not updated" + response);
        
        //move field to dev 
        response =  config.updateStream(streamID, "stage", "DEVELOPMENT");
        Assert.assertTrue(response.contains("error"), "Stream should not be allowed to change - " + response);
        
        //4. move notification to dev
        response = config.updateNotificationField(notificationID, "stage", "DEVELOPMENT");
        Assert.assertFalse(response.contains("error"), "Notification was not updated: " + response);
        
        response =  config.updateStream(streamID, "stage", "DEVELOPMENT");
        Assert.assertFalse(response.contains("error"), "Stream should be allowed to change - " + response);
 
        //remove stream field used by notification
        response =  config.updateStreamRemoveField(streamID);
        Assert.assertTrue(response.contains("error"), "Stream should not be allowed to change - " + response);
               
        int respCode = config.deleteNotification(notificationID);
        Assert.assertEquals(respCode,  200, "Notification was not deleted");

	}

	@AfterTest
	private void reset(){
		config.reset();
	}
}
