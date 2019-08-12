package tests.restapi.scenarios.airlock_notification;

import org.testng.Assert;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import tests.restapi.scenarios.airlock_notification.Config;

public class InputSchemaUseInNotification
{

	protected String seasonID;
	protected String filePath;
	protected String m_url;
	protected String notification;
	protected String productID;
	protected Config config;
	
	
	@BeforeClass
	@Parameters({"url", "analyticsUrl", "translationsUrl", "configPath", "sessionToken", "userName", "userPassword", "appName", "productsToDeleteFile"})
	public void init(String url, String analyticsUrl, String translationsUrl, String configPath, String sToken, String userName, String userPassword, String appName, String productsToDeleteFile) throws Exception{
		config = new Config(url, analyticsUrl, translationsUrl, configPath, sToken, userName, userPassword, appName, productsToDeleteFile);
		config.stage = "DEVELOPMENT";
		config.addSeason( "1.1.1");
		
	}

	@Test ( description="Schema field used in cancellationRule")
	public void scenario1() throws Exception
	{

		config.updateSchema("validationsTests/inputSchema_update_device_minVersion_5_0.txt");
		
		//context.device.locale - 5.0, dev
		
		String notificationID = config.addNotification ("notification1.txt", "cancellationRule", "context.device.locale == \"value\"", "5.0");
		Assert.assertFalse(notificationID.contains("error"), "Notification was not created: " + notificationID);
		
		//schema field doesn't exist
		String response = config.addNotification ("notification1.txt", "cancellationRule", "context.device.newfield == \"value\"", "4.0");
		Assert.assertTrue(response.contains("error"), "Notification was  created with schema field that doesn't exist: " + response);

		
		//1. move notification field to upper minVersion		
		//move context.device.locale child field to 4.0
        response =  config.updateSchema("validationsTests/inputSchema_update_device_minVersion_4_0.txt");
        Assert.assertFalse(response.contains("error"), "cancellationRule: Schema should be allowed to change - " + response);       
       //move context.device.locale child field to 6.0
        response =  config.updateSchema("validationsTests/inputSchema_update_device_minVersion_6_0.txt");
        Assert.assertTrue(response.contains("error"), "cancellationRule: Schema should not be allowed to change - " + response);
        
        //2. move sub-feature to lower minVersion
        response = config.updateNotificationField(notificationID, "minAppVersion", "2.0");
        Assert.assertTrue(response.contains("error"), "cancellationRule: Notification was updated");


        //3. move  field to production       
        //move context.device.locale to prod, notif in dev
        response =  config.updateSchema("validationsTests/inputSchema_update_device_minVersion_5_0_locale_prod.txt");
        Assert.assertFalse(response.contains("error"), "cancellationRule: Schema should be allowed to change - " + response);

        response = config.updateNotificationField(notificationID, "stage", "PRODUCTION");
        Assert.assertFalse(response.contains("error"), "cancellationRule: Notification was not updated" + response);
        //move field to dev 
        response =  config.updateSchema("validationsTests/inputSchema_update_device_minVersion_5_0.txt");
        Assert.assertTrue(response.contains("error"), "cancellationRule: Schema should not be allowed to change - " + response);
        
        //4. move notification to dev
        response = config.updateNotificationField(notificationID, "stage", "DEVELOPMENT");
        Assert.assertFalse(response.contains("error"), "cancellationRule: Notification was not updated: " + response);
        
        //remove input schema field used notification
        response =  config.updateSchema("validationsTests/inputSchema_update_device_no_locale.txt");
        Assert.assertTrue(response.contains("error"), "cancellationRule: Schema should not be allowed to change - " + response);
        
        //use non-existing schema field
        response = config.updateNotificationRuleField(notificationID, "cancellationRule", "context.device.newfield == \"value\"");
        Assert.assertTrue(response.contains("error"), "cancellationRule: Notification should not be updated: " + response);

        
        int respCode = config.deleteNotification(notificationID);
        Assert.assertEquals(respCode,  200, "cancellationRule: Notification was not deleted");
	}
	
	
	@Test ( description="Schema field used in registrationRule")
	public void scenario2() throws Exception
	{

		config.updateSchema("validationsTests/inputSchema_update_device_minVersion_5_0.txt");
		
		//context.device.locale - 5.0, dev
		
		String notificationID = config.addNotification ("notification1.txt", "registrationRule", "context.device.locale == \"value\"", "5.0");
		Assert.assertFalse(notificationID.contains("error"), "registrationRule: Notification was not created: " + notificationID);
		
		//schema field doesn't exist
		String response = config.addNotification ("notification1.txt", "registrationRule", "context.device.newfield == \"value\"", "4.0");
		Assert.assertTrue(response.contains("error"), "registrationRule: Notification was  created with schema field that doesn't exist: " + response);

		
		//1. move notification field to upper minVersion		
		//move context.device.locale child field to 4.0
        response =  config.updateSchema("validationsTests/inputSchema_update_device_minVersion_4_0.txt");
        Assert.assertFalse(response.contains("error"), "registrationRule: Schema should be allowed to change - " + response);       
       //move context.device.locale child field to 6.0
        response =  config.updateSchema("validationsTests/inputSchema_update_device_minVersion_6_0.txt");
        Assert.assertTrue(response.contains("error"), "registrationRule: Schema should not be allowed to change - " + response);
        
        //2. move sub-feature to lower minVersion
        response = config.updateNotificationField(notificationID, "minAppVersion", "2.0");
        Assert.assertTrue(response.contains("error"), "registrationRule: Notification was updated");


        //3. move  field to production       
        //move context.device.locale to prod, notif in dev
        response =  config.updateSchema("validationsTests/inputSchema_update_device_minVersion_5_0_locale_prod.txt");
        Assert.assertFalse(response.contains("error"), "registrationRule: Schema should be allowed to change - " + response);

        response = config.updateNotificationField(notificationID, "stage", "PRODUCTION");
        Assert.assertFalse(response.contains("error"), "registrationRule: Notification was not updated" + response);
        //move field to dev 
        response =  config.updateSchema("validationsTests/inputSchema_update_device_minVersion_5_0.txt");
        Assert.assertTrue(response.contains("error"), "registrationRule: Schema should not be allowed to change - " + response);
        
        //4. move notification to dev
        response = config.updateNotificationField(notificationID, "stage", "DEVELOPMENT");
        Assert.assertFalse(response.contains("error"), "registrationRule: Notification was not updated: " + response);
        
        //remove input schema field used notification
        response =  config.updateSchema("validationsTests/inputSchema_update_device_no_locale.txt");
        Assert.assertTrue(response.contains("error"), "registrationRule: Schema should not be allowed to change - " + response);
        
        //use non-existing schema field
        response = config.updateNotificationRuleField(notificationID, "registrationRule", "context.device.newfield == \"value\"");
        Assert.assertTrue(response.contains("error"), "registrationRule: Notification should not be updated: " + response);

        
        int respCode = config.deleteNotification(notificationID);
        Assert.assertEquals(respCode,  200, "registrationRule: Notification was not deleted");
	}
	
	@Test ( description="Schema field used in configuration")
	public void scenario3() throws Exception
	{

		config.updateSchema("validationsTests/inputSchema_update_device_minVersion_5_0.txt");
		
		//context.device.locale - 5.0, dev
		
		String notificationID = config.addNotificationWithConfiguration (new String[]{"title", "text"}, "notification1.txt", "configuration", "context.device.locale", "5.0", true);
		Assert.assertFalse(notificationID.contains("error"), "configuration: Notification was not created: " + notificationID);
		
		//schema field doesn't exist
		String response = config.addNotification ("notification1.txt", "configuration", "context.device.newfield", "5.0");
		Assert.assertTrue(response.contains("error"), "configuration: Notification was  created with schema field that doesn't exist: " + response);
		
		//1. move notification field to upper minVersion		
		//move context.device.locale child field to 4.0
        response =  config.updateSchema("validationsTests/inputSchema_update_device_minVersion_4_0.txt");
        Assert.assertFalse(response.contains("error"), "configuration: Schema should be allowed to change - " + response);       
       //move context.device.locale child field to 6.0
        response =  config.updateSchema("validationsTests/inputSchema_update_device_minVersion_6_0.txt");
        Assert.assertTrue(response.contains("error"), "configuration: Schema should not be allowed to change - " + response);
        
        //2. move sub-feature to lower minVersion
        response = config.updateNotificationWithConfiguration(new String[]{"title", "text"}, notificationID, "minAppVersion", "2.0", true);
        Assert.assertTrue(response.contains("error"), "configuration: Notification was updated");


        //3. move  field to production       
        //move context.device.locale to prod, notif in dev
        response =  config.updateSchema("validationsTests/inputSchema_update_device_minVersion_5_0_locale_prod.txt");
        Assert.assertFalse(response.contains("error"), "configuration: Schema should be allowed to change - " + response);

        response = config.updateNotificationField(notificationID, "stage", "PRODUCTION");
        Assert.assertFalse(response.contains("error"), "configuration: Notification was not updated" + response);
        //move field to dev 
        response =  config.updateSchema("validationsTests/inputSchema_update_device_minVersion_5_0.txt");
        Assert.assertTrue(response.contains("error"), "configuration: Schema should not be allowed to change - " + response);
        
        //4. move notification to dev
        response = config.updateNotificationField(notificationID, "stage", "DEVELOPMENT");
        Assert.assertFalse(response.contains("error"), "configuration: Notification was not updated: " + response);
        
        response =  config.updateSchema("validationsTests/inputSchema_update_device_no_locale.txt");
        Assert.assertTrue(response.contains("error"), "configuration: Schema should not be allowed to change - " + response);
        
        response = config.updateNotificationRuleField(notificationID, "configuration", "context.device.newfield");
        Assert.assertTrue(response.contains("error"), "configuration: Notification should not be updated: " + response);

               
        int respCode = config.deleteNotification(notificationID);
        Assert.assertEquals(respCode,  200, "configuration: Notification was not deleted");
	}

	@AfterTest
	private void reset(){
		config.reset();
	}
}
