package tests.restapi.scenarios.experiment_and_branch_validation;

import java.io.IOException;

import org.testng.Assert;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

//variant uses non-existing utility
public class NonExistingUtilityInVariant
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

		String experimentID = config.addExperiment("1.1.1", "3.0", "experiments/experiment1.txt", "true", false);
		Assert.assertFalse(experimentID.contains("error"), "Experiment was not created: " + experimentID);

		String branchID = config.addBranch("branch1", "experiments/branch1.txt");
		Assert.assertFalse(branchID.contains("error"), "Branch1 was not created: " + branchID);

		String variantID = config.addVariant(experimentID, "variant1", "branch1", "experiments/variant1.txt", "NoSuchUtility()");
		Assert.assertTrue(variantID.contains("error"), "Variant with missing utility should not be created");
	}

	@AfterTest
	private void reset()
	{
		config.reset();
	}
}
