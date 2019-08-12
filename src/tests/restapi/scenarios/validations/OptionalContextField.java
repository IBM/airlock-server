package tests.restapi.scenarios.validations;


import java.io.IOException;

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
	private String featureID;

	@BeforeClass
	@Parameters({"url", "analyticsUrl", "translationsUrl", "configPath", "sessionToken", "userName", "userPassword",  "appName", "productsToDeleteFile"})
	public void init(String url, String analyticsUrl, String translationsUrl, String configPath, String sToken, String userName, String userPassword, String appName, String productsToDeleteFile) throws IOException
	{
		config = new Config(url, analyticsUrl, translationsUrl, configPath, sToken, userName, userPassword, appName, productsToDeleteFile);
		config.stage = "DEVELOPMENT";
		
	}

	@Test ( description="init season")
	public void initSeason() throws Exception
	{
		config.addSeason( "7.5");
		
		//add feature without schema
		featureID = config.addFeature("feature1.txt", "context.testData.dsx !== undefined", "ROOT", "7.5");
		Assert.assertTrue(featureID.contains("error"), "Feature was not added to the season");

		//config.createSchema();
		config.updateSchema("schemaTests/inputSchemaOptionalDsx.txt");

		//schema field minVersion higher that feature
		featureID = config.addFeature("feature1.txt", "context.testData.dsx !== undefined", "ROOT", "0.5");
		Assert.assertTrue(featureID.contains("error"), "Feature was not added to the season");
		
		featureID = config.addFeature("feature1.txt", "context.testData.dsx !== undefined", "ROOT", "7.5");
		Assert.assertFalse(featureID.contains("error"), "Feature was not added to the season");

		
	}

	@Test (dependsOnMethods = "initSeason", description="remove input schema field used in feature rule")
	public void updateSchemaForFeature() throws Exception
	{
        String response =  config.updateSchema("schemaTests/inputSchemaNoDsx.txt");
        Assert.assertTrue(response.contains("error"), "Schema should not be allowed to change - " + response);
	}
	
	@Test ( dependsOnMethods = "updateSchemaForFeature", description="init configuration rule")
	public void initConfigurationRule() throws Exception
	{
	
		String configID = config.addFeature("configuration_rule1.txt", "context.testData.dsx !== undefined", featureID, "7.5");
		Assert.assertFalse(configID.contains("error"), "Configuration rule was not added to the season");
		
	}
	
	@Test (dependsOnMethods = "initConfigurationRule", description="remove input schema field used in configuration rule")
	public void updateSchemaForConfigurationRule() throws Exception
	{
        String response =  config.updateSchema("schemaTests/inputSchemaNoDsx.txt");
        Assert.assertTrue(response.contains("error"), "Schema should not be allowed to change - " + response);
	}
	
	@Test (dependsOnMethods = "updateSchemaForConfigurationRule", description="configuration of configuration rule uses optional field")
	public void initConfiguration() throws Exception
	{
		//field used in configuration should be protected?
	/*	String configuration = "{\"value\":context.testData.dsx}";
		String configID = config.addFeatureWithConfiguration("configuration_rule2.txt", "", featureID, "7.5", configuration);
		Assert.assertTrue(configID.contains("error"), "Configuration rule was not added to the season");	
	*/	
		
		String configID = config.addFeature("configuration_rule2.txt", "context.testData.dsx !== undefined", featureID, "7.5");
		Assert.assertFalse(configID.contains("error"), "Configuration rule was not added to the season: " + configID);
	}
	
	@AfterTest
	private void reset(){
		config.reset();
	}
}
