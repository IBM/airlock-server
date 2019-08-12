package tests.restapi.scenarios.experiment_and_branch_validation;

import java.io.IOException;

import org.testng.Assert;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

public class OverrideUtilityInBranch
{
	protected Config config;
	String utilityID ;
//	String utility = "function function isUS() {return true;}function isUK() {return false;}function isGermany() {return false;}function isSpain() {return false;}function isFrance() {return false;}function isPortugal() {return false;}function isIndia() {return false;}function isValidCountry() {    return (isUS() || isUK() || isGermany() || isSpain() || isFrance() || isPortugal() || isIndia());}function isSevereModeOff() { return false;}function showTopVideo() {    return (isSevereModeOff() && isValidCountry());}";
	String utility = "function isValidCountry() { return true; }";
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
		utilityID = config.addUtility("1.1.1", utility);
		Assert.assertFalse(utilityID.contains("error"), "utility was not added to the season:"+utilityID);

		String branchID = config.addBranch("branch1", "experiments/branch1.txt");
		Assert.assertFalse(branchID.contains("error"), "Branch1 was not created: " + branchID);

		String featureID = config.addBranchFeature(branchID, "1.1.1", "feature1.txt", "isValidCountry()");
		Assert.assertFalse(featureID.contains("error"), "Feature was not added to the season");
		
		String experimentID = config.addExperiment("1.1.1", "3.0", "experiments/experiment1.txt", "var x = true; x", false);
		Assert.assertFalse(experimentID.contains("error"), "Experiment was not created: " + experimentID);
	}

	@Test(dependsOnMethods = "initBranch", description = "Rename utility")
	public void renameUtility() throws Exception
	{
		String branch2ID = config.addBranch("branch2", "experiments/branch2.txt");
		Assert.assertFalse(branch2ID.contains("error"), "Branch2 was not created: " + branch2ID);
		
		String feature2ID = config.addBranchFeature(branch2ID, "1.1.1", "feature2.txt", "function isValidCountry(country, countryCodes) {return countryCodes.indexOf(country) !== -1;} countries = [\"US\", \"GB\"]; isValidCountry(\"US\", countries);");
		Assert.assertFalse(feature2ID.contains("error"), "Feature2 was not added to the season");
		
		String branch3ID = config.addBranch("branch3", "experiments/branch3.txt");
		Assert.assertFalse(branch3ID.contains("error"), "Branch3 was not created: " + branch3ID);

		String feature3ID = config.addBranchFeature(branch3ID, "1.1.1", "feature1.txt", "isValidCountry()");
		Assert.assertFalse(feature3ID.contains("error"), "Feature3 was not added to the season");
		
		
		String res = config.updateUtility(utilityID, utility);
		Assert.assertFalse(res.contains("error"), "error saving the same utility");
	}

	@AfterTest
	private void reset(){
		config.reset();
	}
}
