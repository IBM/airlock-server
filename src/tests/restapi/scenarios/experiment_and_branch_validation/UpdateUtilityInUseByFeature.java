package tests.restapi.scenarios.experiment_and_branch_validation;



import java.io.IOException;

import org.apache.wink.json4j.JSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeClass;

import org.testng.annotations.Parameters;
import org.testng.annotations.Test;


public class UpdateUtilityInUseByFeature {
	protected Config config;
	String utilityID;

	@BeforeClass
	@Parameters({"url", "analyticsUrl", "translationsUrl", "configPath", "sessionToken", "userName", "userPassword",  "appName", "productsToDeleteFile"})
	public void init(String url, String analyticsUrl, String translationsUrl, String configPath, String sToken, String userName, String userPassword, String appName, String productsToDeleteFile) throws IOException
	{
		config = new Config(url, analyticsUrl, translationsUrl, configPath, sToken, userName, userPassword, appName, productsToDeleteFile);
		config.stage = "PRODUCTION";
		
	}
	
	@Test(description = "Add valid utility")
	public void addUtility() throws Exception{
		config.addSeason("1.1.1");
		
		utilityID = config.addUtility("1.1.1", "function isTrue() {return true;}");
		Assert.assertFalse(utilityID.contains("error"), "utility was not added to the season: " + utilityID);

	}
	
	@Test(dependsOnMethods = "addUtility", description = "Add feature with rule using utility")
	public void addFeature() throws Exception{
		String branchID = config.addBranch("branch1", "experiments/branch1.txt");
		Assert.assertFalse(branchID.contains("error"), "Branch1 was not created: " + branchID);

		String featureID = config.addBranchFeature(branchID, "1.1.1", "feature1.txt", "isTrue()");
		Assert.assertFalse(featureID.contains("error"), "Test should pass, but instead failed: " + featureID );
	}
	
	
	@Test(dependsOnMethods = "addFeature", description = "Change utility to development")
	public void updateUtility() throws Exception{
		String response = config.getUtility(utilityID);
		JSONObject utility = new JSONObject(response);
		utility.put("stage", "DEVELOPMENT");
		String resp = config.updateUtility(utilityID, utility.toString());
		
		Assert.assertTrue(resp.contains("error"), "Should not change utility to development if it is in use by feature in production");
	}
	
	@AfterTest
	private void reset(){
		config.reset();
	}

}
