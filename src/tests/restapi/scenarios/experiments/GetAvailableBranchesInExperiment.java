package tests.restapi.scenarios.experiments;


import java.io.IOException;


import org.apache.commons.lang3.RandomStringUtils;
import org.apache.wink.json4j.JSONException;
import org.apache.wink.json4j.JSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import tests.com.ibm.qautils.FileUtils;
import tests.restapi.AirlockUtils;
import tests.restapi.BranchesRestApi;
import tests.restapi.ExperimentsRestApi;
import tests.restapi.FeaturesRestApi;
import tests.restapi.SeasonsRestApi;
import tests.restapi.UtilitiesRestApi;


public class GetAvailableBranchesInExperiment {
	protected String seasonID1;
	private String seasonID2;
	private String seasonID3;
	protected String featureID;
	protected String productID;
	protected String filePath;
	private String experimentID1;
	private String experimentID2;
	private String experimentID3;
	private String experimentID4;
	protected FeaturesRestApi f;
	protected UtilitiesRestApi u;
	private ExperimentsRestApi exp ;
	private SeasonsRestApi s;
	private String sessionToken = "";	
	protected AirlockUtils baseUtils;
	private BranchesRestApi br ;	
	
	
	@BeforeClass
	@Parameters({"url", "analyticsUrl", "translationsUrl", "configPath", "sessionToken", "userName", "userPassword", "appName", "productsToDeleteFile"})
	public void init(String url, String analyticsUrl, String translationsUrl, String configPath, String sToken, String userName, String userPassword, String appName, String productsToDeleteFile) throws Exception{
		filePath = configPath;
		u = new UtilitiesRestApi();
		u.setURL(url);
		s = new SeasonsRestApi();
		s.setURL(url);
		exp = new ExperimentsRestApi();
		exp.setURL(analyticsUrl); 
		br = new BranchesRestApi();
		br.setURL(url);

		baseUtils = new AirlockUtils(url, analyticsUrl, translationsUrl, configPath, sToken, userName, userPassword, appName, productsToDeleteFile);
		sessionToken = baseUtils.sessionToken;
	
		productID = baseUtils.createProduct();
		baseUtils.printProductToFile(productID);
	}
	
	/*
	 * season1: 1.0-2.0
	 * season2: 2.0-3.0
	 * season3: 3.0-
	 * exp1: 0.5-2.5 (season1, season2)
	 * exp2: 1.5-4.5 (season1, season2, season3)
	 * exp3: 3.5-5.0 (season3)
	 */
	
	@Test(description = "Add seasons, utilities and experiments")
	public void addSeasons() throws JSONException, IOException{
		String branch = FileUtils.fileToString(filePath + "experiments/branch1.txt", "UTF-8", false);
		JSONObject branchJson = new JSONObject(branch);
		String variant = FileUtils.fileToString(filePath + "experiments/variant1.txt", "UTF-8", false);
		JSONObject variantJson = new JSONObject(branch);
		String experiment = FileUtils.fileToString(filePath + "experiments/experiment1.txt", "UTF-8", false);
		JSONObject expJson = new JSONObject(experiment);
		
		//season1
		JSONObject season = new JSONObject();
		season.put("minVersion", "1.0");
		seasonID1 = s.addSeason(productID, season.toString(), sessionToken);
		expJson.put("name", "experiment."+RandomStringUtils.randomAlphabetic(5));
		expJson.put("minVersion", "0.5");
		expJson.put("maxVersion", "2.5");
		expJson.put("enabled", false);
		experimentID1 = exp.createExperiment(productID, expJson.toString(), sessionToken);
		branchJson.put("name", "branch1");
		br.createBranch(seasonID1, branchJson.toString(), BranchesRestApi.MASTER, sessionToken);
		branchJson.put("name", "branch2");
		br.createBranch(seasonID1, branchJson.toString(), BranchesRestApi.MASTER, sessionToken);

		variantJson.put("branchName", "branch1");
		variantJson.put("name", "variant1");
		exp.createVariant(experimentID1, variantJson.toString(), sessionToken);
		
		//move to bug
		/*
		season.put("minVersion", "2.0");
		seasonID2 = s.addSeason(productID, season.toString(), sessionToken);
		expJson.put("name", "experiment2");
		expJson.put("minVersion", "1.5");
		expJson.put("maxVersion", "4.5");
		experimentID2 = exp.createExperiment(productID, expJson.toString(), sessionToken);
		branchJson.put("name", "branch2");
		br.createBranch(seasonID2, branchJson.toString(), BranchesRestApi.MASTER, sessionToken);
		variantJson.put("branchName", "branch2");
		variantJson.put("name", "variant2");
		exp.createVariant(experimentID2, variantJson.toString(), sessionToken);
*/
		//season2
		season.put("minVersion", "2.0");
		seasonID2 = s.addSeason(productID, season.toString(), sessionToken);
		expJson.put("name", "experiment."+RandomStringUtils.randomAlphabetic(5));
		expJson.put("minVersion", "1.5");
		expJson.put("maxVersion", "4.5");
		expJson.put("enabled", false);
		experimentID2 = exp.createExperiment(productID, expJson.toString(), sessionToken);
		variantJson.put("branchName", "branch2");
		variantJson.put("name", "variant2");
		exp.createVariant(experimentID2, variantJson.toString(), sessionToken);
		
		//season3
		season.put("minVersion", "3.0");
		seasonID3 = s.addSeason(productID, season.toString(), sessionToken);
		expJson.put("name", "experiment."+RandomStringUtils.randomAlphabetic(5));
		expJson.put("minVersion", "3.5");
		expJson.put("maxVersion", "5.0");
		expJson.put("enabled", false);
		experimentID3 = exp.createExperiment(productID, expJson.toString(), sessionToken);

		branchJson.put("name", "branch3");
		br.createBranch(seasonID3, branchJson.toString(), BranchesRestApi.MASTER, sessionToken);
		variantJson.put("branchName", "branch3");
		variantJson.put("name", "variant3");
		exp.createVariant(experimentID3, variantJson.toString(), sessionToken);
		
		//add branch to season2
		
		branchJson.put("name", "branch5");
		br.createBranch(seasonID2, branchJson.toString(), BranchesRestApi.MASTER, sessionToken);

		//add branch to season1
		branchJson.put("name", "branch4");
		br.createBranch(seasonID1, branchJson.toString(), BranchesRestApi.MASTER, sessionToken);

		//experiment 4
		expJson.put("name", "experiment."+RandomStringUtils.randomAlphabetic(5));
		expJson.put("minVersion", "2.0");
		expJson.put("maxVersion", "2.5");
		expJson.put("enabled", false);		
		experimentID4 = exp.createExperiment(productID, expJson.toString(), sessionToken);
		variantJson.put("branchName", "branch1");
		variantJson.put("name", "variant4");
		exp.createVariant(experimentID4, variantJson.toString(), sessionToken);

	}
	
	@Test(dependsOnMethods = "addSeasons",  description = "Get available branches")
	public void getAvailableBranches() throws Exception{
		//MASTER is available always in all seasons
		JSONObject res = new JSONObject(exp.getAvailableBranches(experimentID1, sessionToken));		
		Assert.assertTrue(res.getJSONArray("availableInAllSeasons").size()==3);
		Assert.assertTrue(res.getJSONArray("availableInAllSeasons").toString().contains("branch1"), "branch1 not found in experiment1 in availableInAllSeasons");
		Assert.assertTrue(res.getJSONArray("availableInAllSeasons").toString().contains("branch2"), "branch2 not found in experiment1 in availableInAllSeasons");
		Assert.assertTrue(res.getJSONArray("availableInSomeSeasons").size()==2);
		Assert.assertTrue(res.getJSONArray("availableInSomeSeasons").toString().contains("branch4"), "branch4 not found in experiment1 in availableInSomeSeasons");
		Assert.assertTrue(res.getJSONArray("availableInSomeSeasons").toString().contains("branch5"), "branch5 not found in experiment1 in availableInSomeSeasons");
		
		res = new JSONObject(exp.getAvailableBranches(experimentID2, sessionToken));
		Assert.assertTrue(res.getJSONArray("availableInAllSeasons").size()==3);
		Assert.assertTrue(res.getJSONArray("availableInAllSeasons").toString().contains("branch1"), "branch1 not found in experiment2 in availableInAllSeasons");
		Assert.assertTrue(res.getJSONArray("availableInAllSeasons").toString().contains("branch2"), "branch2 not found in experiment2 in availableInAllSeasons");
		Assert.assertTrue(res.getJSONArray("availableInSomeSeasons").size()==3);
		Assert.assertTrue(res.getJSONArray("availableInSomeSeasons").toString().contains("branch4"), "branch4 not found in experiment2 in availableInSomeSeasons");
		Assert.assertTrue(res.getJSONArray("availableInSomeSeasons").toString().contains("branch5"), "branch5 not found in experiment2 in availableInSomeSeasons");
		Assert.assertTrue(res.getJSONArray("availableInSomeSeasons").toString().contains("branch3"), "branch3 not found in experiment2 in availableInSomeSeasons");

		res = new JSONObject(exp.getAvailableBranches(experimentID3, sessionToken));
		Assert.assertTrue(res.getJSONArray("availableInAllSeasons").size()==4);
		Assert.assertTrue(res.getJSONArray("availableInAllSeasons").toString().contains("branch1"), "branch1 not found in experiment3 in availableInAllSeasons");
		Assert.assertTrue(res.getJSONArray("availableInAllSeasons").toString().contains("branch2"), "branch2 not found in experiment3 in availableInAllSeasons");
		Assert.assertTrue(res.getJSONArray("availableInAllSeasons").toString().contains("branch3"), "branch3 not found in experiment3 in availableInAllSeasons");
		Assert.assertTrue(res.getJSONArray("availableInSomeSeasons").size()==0);

		res = new JSONObject(exp.getAvailableBranches(experimentID4, sessionToken));
		Assert.assertTrue(res.getJSONArray("availableInAllSeasons").size()==4);
		Assert.assertTrue(res.getJSONArray("availableInAllSeasons").toString().contains("branch1"), "branch1 not found in experiment4 in availableInAllSeasons");
		Assert.assertTrue(res.getJSONArray("availableInAllSeasons").toString().contains("branch2"), "branch2 not found in experiment4 in availableInAllSeasons");
		Assert.assertTrue(res.getJSONArray("availableInAllSeasons").toString().contains("branch5"), "branch5 not found in experiment4 in availableInAllSeasons");
		Assert.assertTrue(res.getJSONArray("availableInSomeSeasons").size()==0);


	}
	
		
	@AfterTest
	private void reset(){
		baseUtils.reset(productID, sessionToken);
	}
}
