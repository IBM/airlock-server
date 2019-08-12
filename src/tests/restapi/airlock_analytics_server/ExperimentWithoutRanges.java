package tests.restapi.airlock_analytics_server;


import org.apache.wink.json4j.JSONArray;
import org.apache.wink.json4j.JSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import tests.com.ibm.qautils.FileUtils;
import tests.restapi.BranchesRestApi;
import tests.restapi.ExperimentAnalyticsApi;
import tests.restapi.ExperimentsRestApi;
import tests.restapi.ProductsRestApi;
import tests.restapi.SeasonsRestApi;

public class ExperimentWithoutRanges {
	protected String productID;
	protected String seasonID;
	protected String filePath;
	protected SeasonsRestApi s;
	protected String m_url;
	private String sessionToken = "";
	private Config utils;
	private ExperimentsRestApi expApi ;
	private ExperimentAnalyticsApi expAnalyticsApi ;
	private String m_analyticsUrl;
	private ProductsRestApi p;
	private String experimentName;
	private BranchesRestApi br ;
	
	//experiment created disabled doesn't have ranges and can be updates
	//experiment created enabled has ranges and can't be updated without ranges
	
	@BeforeClass
	@Parameters({"url", "analyticsServerUrl", "configPath", "userName", "userPassword", "appName", "productsToDeleteFile"})
	public void init(String airlockServerUrl, String analyticsServerUrl, String configPath, String userName, String userPassword, String appName, String productsToDeleteFile) throws Exception{
		m_url = airlockServerUrl;
		utils = new Config(m_url, configPath, userName, userPassword, appName, productsToDeleteFile);
		m_analyticsUrl = utils.getAnalyticsUrl();
		filePath = configPath ;
		s = new SeasonsRestApi();
		s.setURL(m_url);
		p = new ProductsRestApi();
		p.setURL(m_url);
		expApi = new ExperimentsRestApi();
		expApi.setURL(m_analyticsUrl); 
		expAnalyticsApi = new ExperimentAnalyticsApi();
		expAnalyticsApi.setURL(analyticsServerUrl);
		sessionToken = utils.sessionToken;
 		productID = utils.createProduct();
		seasonID = utils.createSeason(productID);
		br = new BranchesRestApi();
		br.setURL(m_url);

		
		experimentName = utils.setExperimentName();
	}


	@Test (description ="Add disabled production experiment") 
	public void addDisabledExperiment () throws Exception {

		JSONObject json = utils.getExperimentFromFile();
		json.put("name", experimentName);
		json.put("enabled", false);
		String experimentID = expApi.createExperiment(productID, json.toString(), sessionToken);
		Assert.assertFalse(experimentID.contains("error"), "Experiment was not created: " + experimentID);
		
		String experiment = expApi.getExperiment(experimentID, sessionToken);
		JSONObject expJson = new JSONObject(experiment);
		expJson.put("enabled", true);
		String response = expApi.updateExperiment(experimentID, experiment, sessionToken);
		Assert.assertFalse(response.contains("error"), "Experiment was not update: " + response);

	}
	
	@Test (description ="Add enabled production experiment") 
	public void addEnabledExperiment () throws Exception {				
		experimentName = utils.setExperimentName();

		JSONObject json = utils.getExperimentFromFile();
		json.put("name", experimentName);
		json.put("enabled", false);
		String experimentID = expApi.createExperiment(productID, json.toString(), sessionToken);
		Assert.assertFalse(experimentID.contains("error"), "Experiment was not created: " + experimentID);
		
		//add variant so the exp can be enabled
		String branch = FileUtils.fileToString(filePath + "experiments/branch1.txt", "UTF-8", false);
		String branchID = br.createBranch(seasonID, branch, BranchesRestApi.MASTER, sessionToken);
		Assert.assertFalse(branchID.contains("error"), "Branch1 was not created: " + branchID);

		String variant = FileUtils.fileToString(filePath + "experiments/variant1.txt", "UTF-8", false);
		JSONObject variantJson1 = new JSONObject(variant);
		
		variantJson1.put("name", "variant1");
		variantJson1.put("description", "variant1 desc");
		variantJson1.put("displayName", "variant1 DN");
		String variantID = expApi.createVariant(experimentID, variantJson1.toString(), sessionToken);
		Assert.assertFalse(variantID.contains("error"), "Variant1 was not created: " + variantID);

		String experiment = expApi.getExperiment(experimentID, sessionToken);
		JSONObject expJson = new JSONObject(experiment);
		expJson.put("enabled", true);
		String response = expApi.updateExperiment(experimentID, expJson.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Experiment was not updated: " + response);
		
		experiment = expApi.getExperiment(experimentID, sessionToken);
		expJson = new JSONObject(experiment);
		expJson.remove("ranges");
		response = expApi.updateExperiment(experimentID, expJson.toString(), sessionToken);
		Assert.assertTrue(response.contains("error"), "Experiment was update without ranges: " + response);
			
		expJson.put("ranges", JSONObject.NULL);
		response = expApi.updateExperiment(experimentID, expJson.toString(), sessionToken);
		Assert.assertTrue(response.contains("error"), "Experiment was update with null ranges: " + response);
		
		expJson.put("ranges", new JSONArray());
		response = expApi.updateExperiment(experimentID, expJson.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Experiment was not update with empty ranges: " + response);
	}
	

	@AfterTest
	private void reset(){
		utils.reset(productID, sessionToken);
	}
}
