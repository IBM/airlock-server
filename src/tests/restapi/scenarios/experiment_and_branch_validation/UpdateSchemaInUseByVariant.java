package tests.restapi.scenarios.experiment_and_branch_validation;

import java.io.IOException;



import org.testng.Assert;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

//delete from schema an optional field used in variant
//it doesn't matter whether the field is optional or required; deletion is not allowed 
//(our leaf checker assumes that if a field begins with context. it must exist in the schema)
public class UpdateSchemaInUseByVariant
{
	protected Config config;

	@BeforeClass
	@Parameters({"url", "analyticsUrl", "translationsUrl", "configPath", "sessionToken", "userName", "userPassword",  "appName", "productsToDeleteFile"})
	public void init(String url, String analyticsUrl, String translationsUrl, String configPath, String sToken, String userName, String userPassword, String appName, String productsToDeleteFile) throws IOException
	{
		config = new Config(url, analyticsUrl, translationsUrl, configPath, sToken, userName, userPassword, appName, productsToDeleteFile);
		config.stage = "PRODUCTION";
		
	}

	@Test ( description="init experiment")
	public void initExperiment() throws Exception
	{
		config.addSeason("7.5");
		//config.createSchema();
		config.updateSchema("inputSchemas/inputSchema_update_device_locale_to_production.txt");

		String experimentID = config.addExperiment("7.5", "7.6", "experiments/experiment1.txt", null, false);
		Assert.assertFalse(experimentID.contains("error"), "Experiment was not created: " + experimentID);

		String branchID = config.addBranch("branch1", "experiments/branch1.txt");
		Assert.assertFalse(branchID.contains("error"), "Branch1 was not created: " + branchID);

		String variantID = config.addVariant(experimentID, "variant1", "branch1", "experiments/variant1.txt", "context.device.locale !== undefined");
		Assert.assertFalse(variantID.contains("error"), "Variant was not created: " + variantID);
	}

	@Test (dependsOnMethods = "initExperiment", description="remove input schema field used in variant rule")
	public void updateSchema() throws Exception
	{
	       String response =  config.updateSchema("inputSchemas/inputSchema_update_device_locale_to_development.txt");
	        Assert.assertTrue(response.contains("error"), "Schema should not be allowed to change - " + response);
	}

	@AfterTest
	private void reset()
	{
		config.reset();
	}
}
