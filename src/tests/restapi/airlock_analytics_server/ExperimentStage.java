package tests.restapi.airlock_analytics_server;


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

public class ExperimentStage {
	protected String productID;
	protected String seasonID;
	private String experimentID;
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
	private String variantID1;
	
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
	public void addDisabledProdExperiment () throws Exception {

		//experiment in production, but created as disabled - nothing published to aas
		JSONObject json = utils.getExperimentFromFile();
		json.put("name", experimentName);
		json.put("stage", "PRODUCTION");
		json.put("enabled", false);
		experimentID = expApi.createExperiment(productID, json.toString(), sessionToken);
		Assert.assertFalse(experimentID.contains("error"), "Experiment was not created: " + experimentID);
		
		String experiment = expApi.getExperiment(experimentID, sessionToken);
		JSONObject expJson = new JSONObject(experiment);
		
		Assert.assertTrue(expJson.getJSONArray("ranges").size()==0, "Number of ranges in new disabled experiment is not 0");
		String aasResponse = expAnalyticsApi.getExperiment(experimentID, sessionToken);
		Assert.assertTrue(aasResponse.contains("not found"), "Experiment created as disabled was published to aas server");

	}
	
	@Test (description ="Add enabled production experiment") 
	public void addEnabledProdExperiment () throws Exception {
		experimentName = utils.setExperimentName();
		
		// experiment created in production
		JSONObject json = utils.getExperimentFromFile();
		json.put("name", experimentName);
		json.put("stage", "PRODUCTION");
		json.put("enabled", false);
		experimentID = expApi.createExperiment(productID, json.toString(), sessionToken);
		Assert.assertFalse(experimentID.contains("error"), "Experiment was not created: " + experimentID);
		
		//add variant
		String branch = FileUtils.fileToString(filePath + "experiments/branch3.txt", "UTF-8", false);
		String branchID = br.createBranch(seasonID, branch, BranchesRestApi.MASTER, sessionToken);
		Assert.assertFalse(branchID.contains("error"), "Branch1 was not created: " + branchID);

		String variant = FileUtils.fileToString(filePath + "experiments/variant3.txt", "UTF-8", false);
		JSONObject variantJson1 = new JSONObject(variant);
		String variantID = expApi.createVariant(experimentID, variantJson1.toString(), sessionToken);
		Assert.assertFalse(variantID.contains("error"), "Variant1 was not created: " + variantID);
					
		String airlockExperiment = expApi.getExperiment(experimentID, sessionToken);
		Assert.assertFalse(airlockExperiment.contains("error"), "Experiment was not found: " + experimentID);

		//enable experiment so a range will be created and the experiment will be published to analytics server
		JSONObject expJson = new JSONObject(airlockExperiment);
		expJson.put("enabled", true);
		
		String response = expApi.updateExperiment(experimentID, expJson.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Experiment was not updated: " + response); 
		
		String aasResponse = expAnalyticsApi.getExperiment(experimentID, sessionToken);
		Assert.assertFalse(aasResponse.contains("not found"), "Experiment was not published to aas server");
		JSONObject aasJson = new JSONObject(aasResponse);		
		
		String experiment = expApi.getExperiment(experimentID, sessionToken);
		expJson = new JSONObject(experiment);		
		
		Assert.assertTrue(aasJson.getJSONArray("ranges").size()==1, "Incorrect number of ranges");
		Assert.assertTrue(aasJson.getString("stage").equals("PRODUCTION"), "Incorrect stage in aas");
		Assert.assertEquals(aasJson.getJSONArray("ranges").getJSONObject(0).getLong("start"), expJson.getLong("lastModified"), "Incorrect start date in the last range");
		Assert.assertTrue(aasJson.getJSONArray("ranges").getJSONObject(0).isNull("end"), "Incorrect end date in the last range");
		Assert.assertEquals(aasJson.getLong("startDate"), expJson.getLong("lastModified"), "Incorrect end date in the second range");

	}
	
	@Test (dependsOnMethods="addEnabledProdExperiment", description ="Move experiment to development") 
	public void moveExperimentToDevelopment () throws Exception {
		String experiment = expApi.getExperiment(experimentID, sessionToken);
		JSONObject expJson = new JSONObject(experiment);
		
		long originalStartDate = expJson.getLong("lastModified");
		
		expJson.put("stage", "DEVELOPMENT");
		experimentID = expApi.updateExperiment(experimentID, expJson.toString(), sessionToken);
		Assert.assertFalse(experimentID.contains("error"), "Experiment was not created: " + experimentID);
		
		experiment = expApi.getExperiment(experimentID, sessionToken);
		expJson = new JSONObject(experiment);
		
		String aasResponse = expAnalyticsApi.getExperiment(experimentID, sessionToken);
		JSONObject aasJson = new JSONObject(aasResponse);
		
		Assert.assertTrue(aasJson.getJSONArray("ranges").size()==1, "Incorrect number of ranges");
		Assert.assertTrue(aasJson.getString("stage").equals("DEVELOPMENT"), "Incorrect stage in aas");
		Assert.assertEquals(aasJson.getJSONArray("ranges").getJSONObject(0).getLong("start"), originalStartDate, "Incorrect start date in the last range");
		Assert.assertTrue(aasJson.getJSONArray("ranges").getJSONObject(0).isNull("end"), "Incorrect end date in the last range");
		Assert.assertEquals(aasJson.getLong("startDate"), originalStartDate, "Incorrect end date in the second range");

	}
	
	@Test (dependsOnMethods="moveExperimentToDevelopment", description ="Move experiment to production") 
	public void moveExperimentToProduction () throws Exception {
		//move experiment to production should open a new range
		String experiment = expApi.getExperiment(experimentID, sessionToken);
		JSONObject expJson = new JSONObject(experiment);
		
		expJson.put("stage", "PRODUCTION");
		experimentID = expApi.updateExperiment(experimentID, expJson.toString(), sessionToken);
		Assert.assertFalse(experimentID.contains("error"), "Experiment was not created: " + experimentID);
		
		experiment = expApi.getExperiment(experimentID, sessionToken);
		expJson = new JSONObject(experiment);
		
		String aasResponse = expAnalyticsApi.getExperiment(experimentID, sessionToken);
		JSONObject aasJson = new JSONObject(aasResponse);
		
		Assert.assertTrue(aasJson.getJSONArray("ranges").size()==2, "Incorrect number of ranges");
		Assert.assertTrue(aasJson.getString("stage").equals("PRODUCTION"), "Incorrect stage in aas");
		Assert.assertEquals(aasJson.getJSONArray("ranges").getJSONObject(1).getLong("start"), expJson.getLong("lastModified"), "Incorrect start date in the last range");
		Assert.assertTrue(aasJson.getJSONArray("ranges").getJSONObject(1).isNull("end"), "Incorrect end date in the last range");
		Assert.assertEquals(aasJson.getLong("startDate"), expJson.getLong("lastModified"), "Incorrect end date in the second range");
		Assert.assertEquals(aasJson.getJSONArray("ranges").getJSONObject(0).getLong("end"), aasJson.getJSONArray("ranges").getJSONObject(1).getLong("start"), "Incorrect end date in the first range");

	}

		
	@AfterTest
	private void reset(){
		utils.reset(productID, sessionToken);
	}
}
