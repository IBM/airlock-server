package tests.restapi.scenarios.airlock_notification;


import org.apache.wink.json4j.JSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

public class NotificationUtilityInUse
{
	protected Config config;
	private String utilityID;
	private String utilityID4Config;

	@BeforeClass
	@Parameters({"url", "analyticsUrl", "translationsUrl", "configPath", "sessionToken", "userName", "userPassword",  "appName", "productsToDeleteFile"})
	public void init(String url, String analyticsUrl, String translationsUrl, String configPath, String sToken, String userName, String userPassword, String appName, String productsToDeleteFile) throws Exception
	{
		config = new Config(url, analyticsUrl, translationsUrl, configPath, sToken, userName, userPassword, appName, productsToDeleteFile);
		config.stage = "DEVELOPMENT";
		config.addSeason( "1.1.1");
		utilityID = config.addUtility("1.1.1", "function isTrue() {return true;}");
		Assert.assertFalse(utilityID.contains("error"), "utility was not added to the season: " + utilityID);
		
		utilityID4Config = config.addUtility("1.1.1", "function isAaa() {return \"aaa\";}");
		Assert.assertFalse(utilityID4Config.contains("error"), "utilityID4Config was not added to the season: " + utilityID4Config);

		
	}

	@Test ( description="Use utility in cancellationRule")
	public void scenario1() throws Exception
	{

		String notificationID = config.addNotification ("notification1.txt", "cancellationRule", "isTrue()", "5.0");
		Assert.assertFalse(notificationID.contains("error"), "cancellationRule: Notification was not created: " + notificationID);
		
		//utility doesn't exist
		String response = config.addNotification ("notification1.txt", "cancellationRule", "noUtility()", "5.0");
		Assert.assertTrue(response.contains("error"), "cancellationRule: Notification was not created with utility that doesn't exist " + response);

		//move notification to prod, utility in dev
        response = config.updateNotificationField(notificationID, "stage", "PRODUCTION");
        Assert.assertTrue(response.contains("error"), "cancellationRule: Notification was updated");
       
        //move utility to prod
		response = config.getUtility(utilityID);
		JSONObject utility = new JSONObject(response);
		utility.put("stage", "PRODUCTION");
		String resp = config.updateUtility(utilityID, utility.toString());
		Assert.assertFalse(resp.contains("error"), "cancellationRule: Utility was not updated: " + resp);

        ///move feature to prod, utility in prod
		response = config.updateNotificationField(notificationID, "stage", "PRODUCTION");
	      Assert.assertFalse(response.contains("error"), "cancellationRule: Notification was not updated" + response);
	   
        //move utility to dev, feature in prod
			response = config.getUtility(utilityID);
			utility = new JSONObject(response);
			utility.put("stage", "DEVELOPMENT");
			resp = config.updateUtility(utilityID, utility.toString());
			Assert.assertTrue(resp.contains("error"), "cancellationRule: Utility was updated: " + resp);

		//move feature to dev, utility in prod
		response = config.updateNotificationField(notificationID, "stage", "DEVELOPMENT");
		 Assert.assertFalse(response.contains("error"), "cancellationRule: Notification was not updated" + response);
	  
		 //move utility to dev, feature in dev
		response = config.getUtility(utilityID);
		utility = new JSONObject(response);
		utility.put("stage", "DEVELOPMENT");
		resp = config.updateUtility(utilityID, utility.toString());
		Assert.assertFalse(resp.contains("error"), "cancellationRule: Utility was not updated: " + resp);
		
		int respCode = config.deleteUtility(utilityID);
		Assert.assertNotEquals(respCode,  200, "cancellationRule: Deleted utility in use by notification");
		
	       //use non-existing utility
        response = config.updateNotificationRuleField(notificationID, "cancellationRule", "noUtility()");
        Assert.assertTrue(response.contains("error"), "cancellationRule: Notification should not be updated: " + response);

        respCode = config.deleteNotification(notificationID);
        Assert.assertEquals(respCode,  200, "cancellationRule: Notification was not deleted");

	}
	
	@Test ( description="Use utility in registrationRule")
	public void scenario2() throws Exception
	{

		String response = config.getUtility(utilityID);
		JSONObject utility = new JSONObject(response);
		utility.put("stage", "DEVELOPMENT");
		String resp = config.updateUtility(utilityID, utility.toString());
		Assert.assertFalse(resp.contains("error"), "Utility was not updated: " + resp);

		String notificationID = config.addNotification ("notification1.txt", "registrationRule", "isTrue()", "5.0");
		Assert.assertFalse(notificationID.contains("error"), "registrationRule: Notification was not created: " + notificationID);
		
		//utility doesn't exist
		response = config.addNotification ("notification1.txt", "registrationRule", "noUtility()", "5.0");
		Assert.assertTrue(response.contains("error"), "registrationRule: Notification was not created with utility that doesn't exist " + response);

		//move notification to prod, utility in dev
        response = config.updateNotificationField(notificationID, "stage", "PRODUCTION");
        Assert.assertTrue(response.contains("error"), "registrationRule: Notification was updated");
       
        //move utility to prod
		response = config.getUtility(utilityID);
		utility = new JSONObject(response);
		utility.put("stage", "PRODUCTION");
		resp = config.updateUtility(utilityID, utility.toString());
		Assert.assertFalse(resp.contains("error"), "registrationRule: Utility was not updated: " + resp);

        ///move feature to prod, utility in prod
		response = config.updateNotificationField(notificationID, "stage", "PRODUCTION");
	      Assert.assertFalse(response.contains("error"), "registrationRule: Notification was not updated" + response);
	   
        //move utility to dev, feature in prod
			response = config.getUtility(utilityID);
			utility = new JSONObject(response);
			utility.put("stage", "DEVELOPMENT");
			resp = config.updateUtility(utilityID, utility.toString());
			Assert.assertTrue(resp.contains("error"), "registrationRule: Utility was updated: " + resp);

		//move feature to dev, utility in prod
		response = config.updateNotificationField(notificationID, "stage", "DEVELOPMENT");
		 Assert.assertFalse(response.contains("error"), "registrationRule: notifiction was not updated" + response);
	  
		 //move utility to dev, feature in dev
		response = config.getUtility(utilityID);
		utility = new JSONObject(response);
		utility.put("stage", "DEVELOPMENT");
		resp = config.updateUtility(utilityID, utility.toString());
		Assert.assertFalse(resp.contains("error"), "registrationRule: Utility was not updated: " + resp);
		
	       //use non-existing utility
     response = config.updateNotificationRuleField(notificationID, "registrationRule", "noUtility()");
     Assert.assertTrue(response.contains("error"), "registrationRule: Notification should not be updated: " + response);
		
		int respCode = config.deleteUtility(utilityID);
		Assert.assertNotEquals(respCode,  200, "registrationRule: Deleted utility in use by notification");
		
        respCode = config.deleteNotification(notificationID);
        Assert.assertEquals(respCode,  200, "registrationRule: Notification was not deleted");

	}
	
	@Test ( description="Use utility in configuration")
	public void scenario3() throws Exception
	{


		String notificationID = config.addNotificationWithUtility ("notification1.txt", "configuration", "isAaa()", "5.0");
		Assert.assertFalse(notificationID.contains("error"), "configuration: Notification was not created: " + notificationID);
		
		//utility doesn't exist
		String response = config.addNotificationWithUtility ("notification1.txt", "configuration", "noUtility()", "5.0");
		Assert.assertTrue(response.contains("error"), "configuration: Notification was created with utility that doesn't exist " + response);

		//move notification to prod, utility in dev
        response = config.updateNotificationField(notificationID, "stage", "PRODUCTION");
        Assert.assertTrue(response.contains("error"), "configuration: Notification was updated");
       
        //move utility to prod
		response = config.getUtility(utilityID4Config);
		JSONObject utility = new JSONObject(response);
		utility.put("stage", "PRODUCTION");
		String resp = config.updateUtility(utilityID4Config, utility.toString());
		Assert.assertFalse(resp.contains("error"), "configuration: Utility was not updated: " + resp);

        ///move feature to prod, utility in prod
		response = config.updateNotificationField(notificationID, "stage", "PRODUCTION");
	      Assert.assertFalse(response.contains("error"), "configuration: Notification was not updated" + response);
	   
        //move utility to dev, feature in prod
			response = config.getUtility(utilityID4Config);
			utility = new JSONObject(response);
			utility.put("stage", "DEVELOPMENT");
			resp = config.updateUtility(utilityID4Config, utility.toString());
			Assert.assertTrue(resp.contains("error"), "configuration: Utility was updated: " + resp);

		//move feature to dev, utility in prod
		response = config.updateNotificationField(notificationID, "stage", "DEVELOPMENT");
		 Assert.assertFalse(response.contains("error"), "configuration: Feature was not updated" + response);
	  
		 //move utility to dev, feature in dev
		response = config.getUtility(utilityID4Config);
		utility = new JSONObject(response);
		utility.put("stage", "DEVELOPMENT");
		resp = config.updateUtility(utilityID4Config, utility.toString());
		Assert.assertFalse(resp.contains("error"), "configuration: Utility was not updated: " + resp);
		
	       //use non-existing utility
		response = config.updateNotificationWithUtility(notificationID, "configuration", "noUtility()");
		Assert.assertTrue(response.contains("error"), "configuration: Notification should not be updated: " + response);
		
		int respCode = config.deleteUtility(utilityID4Config);
		Assert.assertNotEquals(respCode,  200, "configuration: Deleted utility in use by notification");
		
        respCode = config.deleteNotification(notificationID);
        Assert.assertEquals(respCode,  200, "configuration: Notification was not deleted");

	}

	@AfterTest
	private void reset(){
		config.reset();
	}
}
