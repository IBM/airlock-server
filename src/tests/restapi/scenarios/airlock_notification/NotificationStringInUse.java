package tests.restapi.scenarios.airlock_notification;


import org.testng.Assert;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;


public class NotificationStringInUse
{
	protected Config config;
	private String stringID;

	@BeforeClass
	@Parameters({"url", "analyticsUrl", "translationsUrl", "configPath", "sessionToken", "userName", "userPassword",  "appName", "productsToDeleteFile"})
	public void init(String url, String analyticsUrl, String translationsUrl, String configPath, String sToken, String userName, String userPassword, String appName, String productsToDeleteFile) throws Exception
	{
		config = new Config(url, analyticsUrl, translationsUrl, configPath, sToken, userName, userPassword, appName, productsToDeleteFile);
		config.stage = "DEVELOPMENT";
		config.addSeason( "1.1.1");
		stringID = config.addString("strings/string1.txt");	//key=app.hello
		Assert.assertFalse(stringID.contains("error"), "String was not added to the season");		
	}

	@Test ( description="Use string in notification")
	public void scenario1() throws Exception
	{

		
		String notificationID = config.addNotificationWithString ("notification1.txt", "app.hello", "stam", "4.0");
		Assert.assertFalse(notificationID.contains("error"), "Notification was not created: " + notificationID);
		
		//add non-existing string
		String response = config.addNotificationWithString ("notification1.txt", "app.newstr", "stam", "4.0");
		Assert.assertTrue(response.contains("error"), "Notification was created with string that doesn't exist: " + response);
	
		//move feature to prod, string in dev
        response = config.updateNotificationField(notificationID, "stage", "PRODUCTION");
        Assert.assertTrue(response.contains("error"), "Notification was not updated: " + response);
       
        //move string to prod
		String resp = config.updateString(stringID, "PRODUCTION");
		Assert.assertFalse(resp.contains("error"), "String was not updated: " + resp);

        ///move feature to prod, string in prod
		response = config.updateNotificationField(notificationID, "stage", "PRODUCTION");
	    Assert.assertFalse(response.contains("error"), "Notification was not updated" + response);
	   
        //move string  to dev, feature in prod
		resp = config.updateString(stringID, "DEVELOPMENT");
		Assert.assertTrue(resp.contains("error"), "String was updated" );

		//move feature to dev, string in prod
		response = config.updateNotificationField(notificationID, "stage", "DEVELOPMENT");
		 Assert.assertFalse(response.contains("error"), "Notification was not updated" + response);
	  
		 //move string to dev, feature in dev
		 resp = config.updateString(stringID, "DEVELOPMENT");
		Assert.assertFalse(resp.contains("error"), "String was not updated: " + resp);
		 
	       //use non-existing string
		response = config.udpateNotificationWithString(notificationID, "app.newstr", "stam");
		Assert.assertTrue(response.contains("error"), "Notification should not be updated: " + response);
		
		int respCode = config.deleteString(stringID);
		Assert.assertNotEquals(respCode,  200, "Deleted string in use by notification");
        
        respCode = config.deleteNotification(notificationID);
        Assert.assertEquals(respCode,  200, "Notification was not deleted");
	}
	
	


	@AfterTest
	private void reset(){
		config.reset();
	}
}
