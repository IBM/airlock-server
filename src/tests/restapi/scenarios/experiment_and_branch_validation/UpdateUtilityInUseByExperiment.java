package tests.restapi.scenarios.experiment_and_branch_validation;

import java.io.IOException;

import org.apache.wink.json4j.JSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

public class UpdateUtilityInUseByExperiment
{
	protected Config config;
	String utilityID;

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
		config.addSeason("1.1.1");
		config.createSchema();

		utilityID = config.addUtility("1.1.1", "function isTrue() {return true;}");
		Assert.assertFalse(utilityID.contains("error"), "utility was not added to the season");

		String experimentID = config.addExperiment("1.1.1", "3.0", "experiments/experiment1.txt", "isTrue()", false);
		Assert.assertFalse(experimentID.contains("error"), "Experiment was not created: " + experimentID);
	}

	@Test(dependsOnMethods = "initExperiment", description = "Update utility")
	public void updateUtility() throws Exception
	{
		String response = config.getUtility(utilityID);
		JSONObject utility = new JSONObject(response);
		utility.put("stage", "DEVELOPMENT");
		String resp = config.updateUtility(utilityID, utility.toString());
		
		Assert.assertTrue(resp.contains("error"), "Should not change utility to development if it is in use by experiment in production");	
	}

	@AfterTest
	private void reset()
	{
		config.reset();
	}
}
