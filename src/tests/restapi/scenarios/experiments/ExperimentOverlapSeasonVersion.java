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


public class ExperimentOverlapSeasonVersion {
	protected String seasonID1;
	private String seasonID2;
	private String seasonID3;
	protected String featureID;
	protected String productID;
	protected String filePath;
	private String experimentID1;
	private String experimentID2;
	private String experimentID3;
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
	 * -Experiment min/max version vs season min/max version - allows partial overlap for 3.0 seasons. 
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
		expJson.put("maxVersion", "2.5");expJson.put("enabled", false);
		experimentID1 = exp.createExperiment(productID, expJson.toString(), sessionToken);

		
		//check bug: Shouldn't allow to create a variant with branch that doesn't appear in all seasons that are covered by experiment version range
		
		season.put("minVersion", "2.0");
		seasonID2 = s.addSeason(productID, season.toString(), sessionToken);
		expJson.put("name", "experiment."+RandomStringUtils.randomAlphabetic(5));
		expJson.put("minVersion", "1.5");
		expJson.put("maxVersion", "4.5");
		expJson.put("enabled", false);
		experimentID2 = exp.createExperiment(productID, expJson.toString(), sessionToken);
		branchJson.put("name", "branch2");
		br.createBranch(seasonID2, branchJson.toString(), BranchesRestApi.MASTER, sessionToken);
		variantJson.put("branchName", "branch2");
		variantJson.put("name", "variant2");
		String response = exp.createVariant(experimentID2, variantJson.toString(), sessionToken);
		Assert.assertTrue(response.contains("error"), "Branch2 exists only in season2 and experiment covers both season1 & season2");
		
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
		

	}

	
		
	@AfterTest
	private void reset(){
		baseUtils.reset(productID, sessionToken);
	}
}
