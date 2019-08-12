package tests.restapi.scenarios.validations;

import java.io.IOException;


import org.testng.Assert;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

// delete from schema a required field used in branch
public class InputSchemaInUseByFeatures
{
	protected Config config;

	@BeforeClass
	@Parameters({"url", "analyticsUrl", "translationsUrl", "configPath", "sessionToken", "userName", "userPassword",  "appName", "productsToDeleteFile"})
	public void init(String url, String analyticsUrl, String translationsUrl, String configPath, String sToken, String userName, String userPassword, String appName, String productsToDeleteFile) throws IOException
	{
		config = new Config(url, analyticsUrl, translationsUrl, configPath, sToken, userName, userPassword, appName, productsToDeleteFile);
		config.stage = "DEVELOPMENT";
		
	}

	@Test ( description="add features")
	public void addFeatures() throws Exception
	{
		config.addSeason( "1.1.1");
		config.updateSchema("validationsTests/inputSchema_update_device_minVersion_5_0.txt");

		String featureID = config.addFeature ("feature1.txt", "context.device.datetime == \"value\"", "ROOT", "4.0");
		Assert.assertFalse(featureID.contains("error"), "Feature was not added to the season: " + featureID);
		
		//context.device.locale - minVersion=5.0
		String configID = config.addFeature ("configuration_rule1.txt", "context.device.locale == \"value\"", featureID, "4.0");
		Assert.assertTrue(configID.contains("error"), "Configuration rule was added to the season");

		configID = config.addFeature ("configuration_rule1.txt", "context.device.locale == \"value\"", featureID, "5.0");
		Assert.assertFalse(configID.contains("error"), "Configuration rule was not added to the season: " + configID);

	}

	@Test (dependsOnMethods = "addFeatures", description="update input schema field to lower minVersion used in configuration rule")
	public void updateSchema() throws Exception
	{
        String response =  config.updateSchema("validationsTests/inputSchema_update_device_minVersion_4_0.txt");
        Assert.assertFalse(response.contains("error"), "Schema should be allowed to change - " + response);
        
        response =  config.updateSchema("validationsTests/inputSchema_update_device_minVersion_6_0.txt");
        Assert.assertTrue(response.contains("error"), "Schema should not be allowed to change - " + response);

	}

	@AfterTest
	private void reset(){
		config.reset();
	}
}
