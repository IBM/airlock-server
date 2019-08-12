package tests.restapi.scenarios.experiment_and_branch_validation;

import java.io.IOException;

import org.testng.Assert;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

//delete from schema an optional field used in experiment
//it doesn't matter whether the field is optional or required; deletion is not allowed 
//(our leaf checker assumes that if a field begins with context. it must exist in the schema)
public class DeleteFromSchemaOptionalExperimentContext
{
	protected Config config;

	@BeforeClass
	@Parameters({"url", "analyticsUrl", "translationsUrl", "configPath", "sessionToken", "userName", "userPassword",  "appName", "productsToDeleteFile"})
	public void init(String url, String analyticsUrl, String translationsUrl, String configPath, String sToken, String userName, String userPassword, String appName, String productsToDeleteFile) throws IOException
	{
		config = new Config(url, analyticsUrl, translationsUrl, configPath, sToken, userName, userPassword, appName, productsToDeleteFile);
		config.stage = "DEVELOPMENT";
		
	}
	
	@Test ( description="init experiment")
	public void initExperiment() throws Exception
	{
		config.addSeason("7.5");
		//config.createSchema();
		config.updateSchema("schemaTests/inputSchemaOptionalDsx.txt");

		String experimentID = config.addExperiment("7.5", "7.6", "experiments/experiment1.txt", "context.testData.dsx !== undefined", false);
		Assert.assertFalse(experimentID.contains("error"), "Experiment was not created: " + experimentID);
	}

	@Test (dependsOnMethods = "initExperiment", description="remove input schema field used in experiment rule")
	public void updateSchema() throws Exception
	{
		String response = config.updateSchema("schemaTests/inputSchemaNoDsx.txt");
        Assert.assertTrue(response.contains("error"), "Schema should not be allowed to change - " + response);
	}

	@AfterTest
	private void reset()
	{
		config.reset();
	}
}
