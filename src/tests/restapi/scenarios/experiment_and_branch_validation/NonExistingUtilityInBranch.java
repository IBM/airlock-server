package tests.restapi.scenarios.experiment_and_branch_validation;

import java.io.IOException;

import org.testng.Assert;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

//branch uses non-existing utility
public class NonExistingUtilityInBranch
{
	protected Config config;

	@BeforeClass
	@Parameters({"url", "analyticsUrl", "translationsUrl", "configPath", "sessionToken", "userName", "userPassword",  "appName", "productsToDeleteFile"})
	public void init(String url, String analyticsUrl, String translationsUrl, String configPath, String sToken, String userName, String userPassword, String appName, String productsToDeleteFile) throws IOException
	{
		config = new Config(url, analyticsUrl, translationsUrl, configPath, sToken, userName, userPassword, appName, productsToDeleteFile);
		config.stage = "DEVELOPMENT";
		
	}

	@Test ( description="init branch")
	public void initBranch() throws Exception
	{
		config.addSeason( "1.1.1");
		config.createSchema();

		String branchID = config.addBranch("branch1", "experiments/branch1.txt");
		Assert.assertFalse(branchID.contains("error"), "Branch1 was not created: " + branchID);

		String featureID = config.addBranchFeature(branchID, "1.1.1", "feature1.txt", "NoSuchUtility()");
		Assert.assertTrue(featureID.contains("error"), "Branch feature with missing utility should not be created");
	}

	@AfterTest
	private void reset(){
		config.reset();
	}
}

