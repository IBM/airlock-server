//package tests.restapi.scenarios.validations;
package tests.restapi.known_issues;

import java.io.IOException;




import org.testng.Assert;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import tests.restapi.BranchesRestApi;

public class DeleteFromSchemaRequiredByUtility
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

		//String response =  config.updateSchema("schemaTests/inputSchemaNoDateTime.txt");
		
		String utilityID = config.addUtility("1.1.1", "function hasDateTime() { return (context.device.datetime !== undefined) ; }");
		Assert.assertFalse(utilityID.contains("error"), "utility was not added to the season");

		String featureID = config.addBranchFeature(BranchesRestApi.MASTER, "1.1.1", "feature1.txt", "hasDateTime()");
		Assert.assertFalse(featureID.contains("error"), "Feature was not added to the season");
	}

	@Test (dependsOnMethods = "initBranch", description="remove input schema field used in branch utilty")
	public void updateSchema() throws Exception
	{
        String response =  config.updateSchema("schemaTests/inputSchemaNoDateTime.txt");
        Assert.assertTrue(response.contains("error"), "Schema should not be allowed to change - " + response);
	}

	@AfterTest
	private void reset(){
		config.reset();
	}
}
