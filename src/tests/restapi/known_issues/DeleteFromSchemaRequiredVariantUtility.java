//package tests.restapi.scenarios.experiment_and_branch_validation;
package tests.restapi.known_issues;

import java.io.IOException;

import org.testng.Assert;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

//known issue: in utility we are using field, removing the field is allowed. bug#2-8 Q4-2017

//delete from schema a required field used inside variant utility
public class DeleteFromSchemaRequiredVariantUtility
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
		config.addSeason("1.1.1");
		config.createSchema();

		String utilityID = config.addUtility("1.1.1", "function hasDateTime() { return (context.device.datetime !== undefined) ; }");
		Assert.assertFalse(utilityID.contains("error"), "utility was not added to the season");

		String experimentID = config.addExperiment("1.1.1", "3.0", "experiments/experiment1.txt", null, false);
		Assert.assertFalse(experimentID.contains("error"), "Experiment was not created: " + experimentID);

		String branchID = config.addBranch("branch1", "experiments/branch1.txt");
		Assert.assertFalse(branchID.contains("error"), "Branch1 was not created: " + branchID);

		String variantID = config.addVariant(experimentID, "variant1", "branch1", "experiments/variant1.txt", "hasDateTime()");
		Assert.assertFalse(variantID.contains("error"), "Variant was not created: " + variantID);
	}
	
	@Test (dependsOnMethods = "initExperiment", description="remove input schema field used in variant rule")
	public void updateSchema() throws Exception
	{
		String response = config.updateSchema("schemaTests/inputSchemaNoDateTime.txt");
	    Assert.assertTrue(response.contains("error"), "Schema should not be allowed to change - " + response);
	}
	
	@AfterTest
	private void reset()
	{
		config.reset();
	}

}
