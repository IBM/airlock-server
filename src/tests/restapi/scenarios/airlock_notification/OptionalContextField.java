package tests.restapi.scenarios.airlock_notification;


import org.testng.Assert;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

//delete from schema an optional field used in branch
// it doesn't matter whether the field is optional or required; deletion is not allowed 
// (our leaf checker assumes that if a field begins with context. it must exist in the schema)
public class OptionalContextField
{
	protected Config config;

	@BeforeClass
	@Parameters({"url", "analyticsUrl", "translationsUrl", "configPath", "sessionToken", "userName", "userPassword",  "appName", "productsToDeleteFile"})
	public void init(String url, String analyticsUrl, String translationsUrl, String configPath, String sToken, String userName, String userPassword, String appName, String productsToDeleteFile) throws Exception
	{
		config = new Config(url, analyticsUrl, translationsUrl, configPath, sToken, userName, userPassword, appName, productsToDeleteFile);
		config.stage = "DEVELOPMENT";
		config.addSeason( "1.1.1");
		
	}

	@Test ( description="optional field in cancellationRule")
	public void scenario1() throws Exception
	{
		
		
		//config.createSchema();
		config.updateSchema("schemaTests/inputSchemaOptionalDsx.txt");

		//schema field minVersion higher that notification
		String response = config.addNotification ("notification1.txt", "cancellationRule", "context.testData.dsx !== undefined", "0.5");
		Assert.assertTrue(response.contains("error"), "Notification should not be created: " + response);

		String notificationID = config.addNotification ("notification1.txt", "cancellationRule", "context.testData.dsx !== undefined", "7.5");
		Assert.assertFalse(notificationID.contains("error"), "Notification was not created: " + notificationID);
		
		//add required field and update with optional field
		String notificationID2 = config.addNotification ("notification1.txt", "cancellationRule", "context.testData.precipitationForecast !== undefined", "7.5");
		Assert.assertFalse(notificationID.contains("error"), "Notification was not created: " + notificationID);
		
		response = config.updateNotificationRuleField(notificationID2, "cancellationRule", "context.testData.dsx !== undefined");
		Assert.assertFalse(response.contains("error"), "Notification was not updated: " + response);
		
		//remove optional field from schema
        response =  config.updateSchema("schemaTests/inputSchemaNoDsx.txt");
        Assert.assertTrue(response.contains("error"), "Schema should not be allowed to change - " + response);
	}
	
	@Test ( description="optional field in registrationRule")
	public void scenario2() throws Exception
	{

		//schema field minVersion higher that notification
		String response = config.addNotification ("notification1.txt", "registrationRule", "context.testData.dsx !== undefined", "0.5");
		Assert.assertTrue(response.contains("error"), "Notification should not be created: " + response);

		String notificationID = config.addNotification ("notification1.txt", "registrationRule", "context.testData.dsx !== undefined", "7.5");
		Assert.assertFalse(notificationID.contains("error"), "Notification was not created: " + notificationID);
		
		//add required field and update with optional field
		String notificationID2 = config.addNotification ("notification1.txt", "registrationRule", "context.testData.precipitationForecast !== undefined", "7.5");
		Assert.assertFalse(notificationID.contains("error"), "Notification was not created: " + notificationID);
		
		response = config.updateNotificationRuleField(notificationID2, "registrationRule", "context.testData.dsx !== undefined");
		Assert.assertFalse(response.contains("error"), "Notification was not updated: " + response);
		
		//remove optional field from schema
        response =  config.updateSchema("schemaTests/inputSchemaNoDsx.txt");
        Assert.assertTrue(response.contains("error"), "Schema should not be allowed to change - " + response);
	}
	

	
	@AfterTest
	private void reset(){
		config.reset();
	}
}
