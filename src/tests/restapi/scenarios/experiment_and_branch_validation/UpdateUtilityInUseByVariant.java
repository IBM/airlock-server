package tests.restapi.scenarios.experiment_and_branch_validation;

import java.io.IOException;




import org.apache.wink.json4j.JSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

public class UpdateUtilityInUseByVariant
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

	@Test ( description="init variant")
	public void initVariant() throws Exception
	{
		config.addSeason("1.1.1");
		config.createSchema();

		utilityID = config.addUtility("1.1.1", "function isTrue() {return true;}");
		Assert.assertFalse(utilityID.contains("error"), "utility was not added to the season");

		String experimentID = config.addExperiment("1.1.1", "3.0", "experiments/experiment1.txt", "true", false);
		Assert.assertFalse(experimentID.contains("error"), "Experiment was not created: " + experimentID);

		String branchID = config.addBranch("branch1", "experiments/branch1.txt");
		Assert.assertFalse(branchID.contains("error"), "Branch1 was not created: " + branchID);

		String variantID = config.addVariant(experimentID, "variant1", "branch1", "experiments/variant1.txt", "isTrue()");
		Assert.assertFalse(variantID.contains("error"), "Variant was not created: " + variantID);
	}

	@Test(dependsOnMethods = "initVariant", description = "update utility")
	public void deleteUtility() throws Exception
	{
		String response = config.getUtility(utilityID);
		JSONObject utility = new JSONObject(response);
		utility.put("stage", "DEVELOPMENT");
		String resp = config.updateUtility(utilityID, utility.toString());
		
		Assert.assertTrue(resp.contains("error"), "Should not change utility to development if it is in use by feature in production");
	}

	@AfterTest
	private void reset()
	{
		config.reset();
	}
}
