package tests.restapi.airlock_analytics_server;


import org.apache.commons.lang3.RandomStringUtils;
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
import tests.restapi.airlock_analytics_server.Config;

public class S3ObserverUpdate {
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
	private long sleepTime;
	private BranchesRestApi br ;
	
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
		
		sleepTime = Long.parseLong(System.getenv("BUCKET_OBSERVATION_INTERVAL_SEC"))*1000 + 100000;//add additional 1 min to interval
		
		//must add branch & variant to enable experiment
		String branch = FileUtils.fileToString(filePath + "experiments/branch1.txt", "UTF-8", false);
		JSONObject branchJson = new JSONObject(branch);
		branchJson.put("name", "branch1");
		String branchID= br.createBranch(seasonID, branchJson.toString(), BranchesRestApi.MASTER, sessionToken);

	}
	
	/*
	 when updating experiment's range so its start date is prior to the earliest start date, 
	 the  s3Observer start date is updated as well and earlier buckets are indexed.
	 */


	@Test (description ="Add experiment1") 
	public void addExperiment1 () throws Exception {

		
		JSONObject json = utils.getExperimentFromFile();
		json.put("name", "experiment."+RandomStringUtils.randomAlphabetic(3));
		json.put("stage", "PRODUCTION");
		json.put("enabled", false);
		json.put("indexExperiment", false);

		String experimentID1 = expApi.createExperiment(productID, json.toString(), sessionToken);
		Assert.assertFalse(experimentID1.contains("error"), "Experiment was not created: " + experimentID1);

		
		String variant = FileUtils.fileToString(filePath + "experiments/variant1.txt", "UTF-8", false);
		JSONObject variantJson = new JSONObject(variant);
		variantJson.put("name", "variant."+RandomStringUtils.randomAlphabetic(3));
		variantJson.put("branchName", "branch1");
		variantJson.put("stage", "PRODUCTION");		
		String variantID = expApi.createVariant(experimentID1, variantJson.toString(), sessionToken);
		
		String experiment = expApi.getExperiment(experimentID1, sessionToken);
		JSONObject expJson = new JSONObject(experiment);
		expJson.put("enabled", true);
		expJson.put("indexExperiment", true);
		
	    JSONArray ranges = new JSONArray();
	    JSONObject range = new JSONObject();
	  
	    range.put("start", Long.parseLong("1503212709000")); //Sun Aug 20 2017 10:05:09 GMT+0300
	    range.put("end", Long.parseLong("1503216309000")); //Sun Aug 20 2017 11:05:09 GMT+0300


	    ranges.put(range);
	    expJson.put("ranges", ranges);
	        
		String response = expApi.updateExperiment(experimentID1, expJson.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Experiment was not updated: " + response);
		
		String aasResponse = expAnalyticsApi.getExperiment(experimentID1, sessionToken);
		Assert.assertFalse(aasResponse.contains("not found"), "Experiment was not published to aas server");
   }
	
	@Test (dependsOnMethods="addExperiment1", description ="Add experiment2") 
	public void addExperiment2 () throws Exception {

		
		JSONObject json = utils.getExperimentFromFile();
		json.put("name", "experiment."+RandomStringUtils.randomAlphabetic(3));
		json.put("stage", "PRODUCTION");
		json.put("enabled", false);
		json.put("indexExperiment", false);

		String experimentID2 = expApi.createExperiment(productID, json.toString(), sessionToken);
		Assert.assertFalse(experimentID2.contains("error"), "Experiment was not created: " + experimentID2);
		
		String variant = FileUtils.fileToString(filePath + "experiments/variant1.txt", "UTF-8", false);
		JSONObject variantJson = new JSONObject(variant);
		variantJson.put("name", "variant."+RandomStringUtils.randomAlphabetic(3));
		variantJson.put("branchName", "branch1");
		variantJson.put("stage", "PRODUCTION");		
		String variantID = expApi.createVariant(experimentID2, variantJson.toString(), sessionToken);
		
		String experiment = expApi.getExperiment(experimentID2, sessionToken);
		JSONObject expJson = new JSONObject(experiment);
		expJson.put("enabled", true);
		expJson.put("indexExperiment", true);
		
	    JSONArray ranges = new JSONArray();
	    JSONObject range = new JSONObject();
	   
	    range.put("start", Long.parseLong("1503299109000")); //Sun Aug 21 2017 10:05:09 GMT+0300
	    range.put("end", Long.parseLong("1503302709000")); //Sun Aug 21 2017 11:05:09 GMT+0300


	    ranges.put(range);
	    expJson.put("ranges", ranges);
	        
		String response = expApi.updateExperiment(experimentID2, expJson.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Experiment was not updated: " + response);

    }
	
	
	@Test (dependsOnMethods="addExperiment2", description ="Add experiment3") 
	public void addExperiment3 () throws Exception {

		
		JSONObject json = utils.getExperimentFromFile();
		json.put("name", "experiment."+RandomStringUtils.randomAlphabetic(3));
		json.put("stage", "PRODUCTION");
		json.put("enabled", false);
		json.put("indexExperiment", false);

		experimentID = expApi.createExperiment(productID, json.toString(), sessionToken);
		Assert.assertFalse(experimentID.contains("error"), "Experiment was not created: " + experimentID);
		
		String variant = FileUtils.fileToString(filePath + "experiments/variant1.txt", "UTF-8", false);
		JSONObject variantJson = new JSONObject(variant);
		variantJson.put("name", "variant."+RandomStringUtils.randomAlphabetic(3));
		variantJson.put("branchName", "branch1");
		variantJson.put("stage", "PRODUCTION");		
		String variantID = expApi.createVariant(experimentID, variantJson.toString(), sessionToken);
		
		String experiment = expApi.getExperiment(experimentID, sessionToken);
		JSONObject expJson = new JSONObject(experiment);
		expJson.put("enabled", true);
		expJson.put("indexExperiment", true);
		
	    JSONArray ranges = new JSONArray();
	    JSONObject range = new JSONObject();

	    range.put("start", Long.parseLong("1503385509000")); //Sun Aug 22 2017 10:05:09 GMT+0300
	    range.put("end", Long.parseLong("1503389109000")); //Sun Aug 22 2017 11:05:09 GMT+0300


	    ranges.put(range);
	    expJson.put("ranges", ranges);
	        
		String response = expApi.updateExperiment(experimentID, expJson.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Experiment was not updated: " + response);

    }
	
	@Test (dependsOnMethods="addExperiment3", description ="update experiment3") 
	public void updateExperiment3 () throws Exception {

		
		String experiment = expApi.getExperiment(experimentID, sessionToken);
		JSONObject expJson = new JSONObject(experiment);
		
	    JSONArray ranges = new JSONArray();
	    JSONObject range = new JSONObject();

	    range.put("start", Long.parseLong("1503126309000")); //Sun Aug 19 2017 10:05:09 GMT+0300
	    range.put("end", Long.parseLong("1503129909000")); //Sun Aug 19 2017 11:05:09 GMT+0300
	    ranges.put(range);
	    expJson.put("ranges", ranges);
	        
		String response = expApi.updateExperiment(experimentID, expJson.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Experiment was not updated: " + response);
		
	     Thread.sleep(sleepTime);
	     String status = expAnalyticsApi.getStatus(sessionToken);
	     Assert.assertTrue(status.contains("S3 start observation: Sun Aug 19 2017"), "Observer time was not updated to the earlies bucket");
	     System.out.println(status);
	 
    }


	
	@AfterTest
	private void reset(){
		utils.reset(productID, sessionToken);
	}
}
