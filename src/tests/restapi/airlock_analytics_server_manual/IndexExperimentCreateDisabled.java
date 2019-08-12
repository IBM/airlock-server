package tests.restapi.airlock_analytics_server_manual;


import org.apache.wink.json4j.JSONArray;
import org.apache.wink.json4j.JSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import tests.restapi.ExperimentAnalyticsApi;
import tests.restapi.ExperimentsRestApi;
import tests.restapi.ProductsRestApi;
import tests.restapi.SeasonsRestApi;
import tests.restapi.airlock_analytics_server.Config;

public class IndexExperimentCreateDisabled {
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
	private long sleepTime;
	
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
		
		experimentName = utils.setExperimentName();
		sleepTime = Long.parseLong(System.getenv("BUCKET_OBSERVATION_INTERVAL_SEC"))*1000 + 300000;//add additional 5 min to interval
	}


	@Test (description ="Add enabled  experiment") 
	public void enabledIndexExperiment () throws Exception {

		//experiment in production, and enabled, but indexExperiment is false - nothing is indexed
		JSONObject json = utils.getExperimentFromFile();
		json.put("name", experimentName);
		json.put("stage", "PRODUCTION");
		json.put("enabled", true);
		json.put("indexExperiment", false);
		experimentID = expApi.createExperiment(productID, json.toString(), sessionToken);
		Assert.assertFalse(experimentID.contains("error"), "Experiment was not created: " + experimentID);

		String experiment = expApi.getExperiment(experimentID, sessionToken);
		JSONObject expJson = new JSONObject(experiment);
	    JSONArray ranges = new JSONArray();
	    JSONObject range = new JSONObject();
	   //range contains 10 buckets
	    range.put("end", Long.parseLong("1503233000000")); //Sun Aug 20 2017 15:43:20 GMT+0300
	    range.put("start", Long.parseLong("1503223509000")); //Sun Aug 20 2017 13:05:09 GMT+0300

	    ranges.put(range);
	    expJson.put("ranges", ranges);
	        
		String response = expApi.updateExperiment(experimentID, expJson.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Experiment was not updated: " + response);
		
		String aasResponse = expAnalyticsApi.getExperiment(experimentID, sessionToken);
		Assert.assertFalse(aasResponse.contains("not found"), "Experiment was not published to aas server");
		
		
		//check that this experiment doesn't appear in experiments status
        Thread.sleep(sleepTime);
        String status = expAnalyticsApi.getStatus(sessionToken);
        Assert.assertTrue(status.contains(experimentName), "Experiment name was not found in status list");
        System.out.println(status);
    }
	
	@Test (dependsOnMethods="enabledIndexExperiment", description ="Add disabled production experiment") 
	public void disableIndexExperiment () throws Exception {

		String experiment = expApi.getExperiment(experimentID, sessionToken);
		JSONObject json = new JSONObject(experiment);		
		json.put("indexExperiment", true);
		
		experimentID = expApi.updateExperiment(experimentID, json.toString(), sessionToken);
		Assert.assertFalse(experimentID.contains("error"), "Experiment was not created: " + experimentID);
		
		String aasResponse = expAnalyticsApi.getExperiment(experimentID, sessionToken);
		Assert.assertFalse(aasResponse.contains("not found"), "Experiment was not published to aas server");
		
		Thread.sleep(sleepTime);
        String status = expAnalyticsApi.getStatus(sessionToken);
        Assert.assertTrue(status.contains(experimentName), "Experiment name was not found in status list");
        System.out.println(status);
    }
	
	@Test (dependsOnMethods="disableIndexExperiment", description ="Enabled experiment") 
	public void enableIndexExperiment () throws Exception {

		String experiment = expApi.getExperiment(experimentID, sessionToken);
		JSONObject json = new JSONObject(experiment);		
		json.put("indexExperiment", false);
		
		experimentID = expApi.updateExperiment(experimentID, json.toString(), sessionToken);
		Assert.assertFalse(experimentID.contains("error"), "Experiment was not created: " + experimentID);
		
		String aasResponse = expAnalyticsApi.getExperiment(experimentID, sessionToken);
		Assert.assertFalse(aasResponse.contains("not found"), "Experiment was not published to aas server");
		
		Thread.sleep(sleepTime);
        String status = expAnalyticsApi.getStatus(sessionToken);
        Assert.assertTrue(status.contains(experimentName), "Experiment name was not found in status list");
        System.out.println(status);
    }
	
	@Test (dependsOnMethods="enableIndexExperiment", description ="Delete experiment") 
	public void deleteExperiment () throws Exception {

		String experiment = expApi.getExperiment(experimentID, sessionToken);
		JSONObject json = new JSONObject(experiment);		
		json.put("stage", "DEVELOPMENT");
		
		String response = expApi.updateExperiment(experimentID, json.toString(), sessionToken);
		Assert.assertFalse(experimentID.contains("error"), "Experiment was not updated: " + experimentID);
		
		int codeResp = expApi.deleteExperiment(experimentID, sessionToken);
		Assert.assertTrue(codeResp == 200, "Experiment was not deleted");
		
		String aasResponse = expAnalyticsApi.getExperiment(experimentID, sessionToken);
		Assert.assertTrue(aasResponse.contains("not found"), "Experiment was not deleted from aas server");
		
		String status = expAnalyticsApi.getStatus(sessionToken);
		//
		
		Thread.sleep(sleepTime);
        status = expAnalyticsApi.getStatus(sessionToken);
        Assert.assertFalse(status.contains(experimentName), "Experiment name was found in status list");
        System.out.println(status);
    }
	
	@AfterTest
	private void reset(){
		utils.reset(productID, sessionToken);
	}
}
