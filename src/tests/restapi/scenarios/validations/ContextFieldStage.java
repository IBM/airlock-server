package tests.restapi.scenarios.validations;


import java.io.IOException;

import org.testng.Assert;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;



public class ContextFieldStage
{
	protected Config config;
	private String featureID;

	@BeforeClass
	@Parameters({"url", "analyticsUrl", "translationsUrl", "configPath", "sessionToken", "userName", "userPassword",  "appName", "productsToDeleteFile"})
	public void init(String url, String analyticsUrl, String translationsUrl, String configPath, String sToken, String userName, String userPassword, String appName, String productsToDeleteFile) throws IOException
	{
		config = new Config(url, analyticsUrl, translationsUrl, configPath, sToken, userName, userPassword, appName, productsToDeleteFile);
		
		
	}

	@Test ( description="init season")
	public void initSeason() throws Exception
	{
		config.addSeason( "7.5");
		
		//config.createSchema();
		config.updateSchema("schemaTests/inputSchemaNoDateTime.txt");

		//schema field dev , feature in prod
		config.stage = "PRODUCTION";
		featureID = config.addFeature("feature1.txt", "context.device.locale == \"US\"", "ROOT", "7.5");
		Assert.assertTrue(featureID.contains("error"), "Feature was added to the season");
		
		//schema field in prod, feature in dev
		config.stage = "DEVELOPMENT";
		featureID = config.addFeature("feature1.txt", "context.device.connectionType == \"US\"", "ROOT", "7.5");
		Assert.assertFalse(featureID.contains("error"), "Feature was not added to the season");

		config.stage = "PRODUCTION";
		String configID = config.addFeature("configuration_rule1.txt", "context.device.locale == \"US\"", featureID, "7.5");
		Assert.assertTrue(configID.contains("error"), "Configuration rule was added to the season");

		config.stage = "DEVELOPMENT";
		configID = config.addFeature("configuration_rule1.txt", "context.device.connectionType == \"US\"", featureID, "7.5");
		Assert.assertFalse(configID.contains("error"), "Configuration rule was not added to the season");
		
		//update feature
		config.stage = "PRODUCTION";
		featureID = config.updateFeature(featureID, "context.device.locale == \"US\"");
		Assert.assertTrue(featureID.contains("error"), "Feature was updated");
		configID = config.updateFeature(configID, "context.device.locale == \"US\"");
		Assert.assertTrue(configID.contains("error"), "Configuration rule was updated");


	}


	@AfterTest
	private void reset(){
		config.reset();
	}
}
