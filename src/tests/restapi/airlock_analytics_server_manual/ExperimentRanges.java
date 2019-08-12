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

public class ExperimentRanges {
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


	@Test (description ="Add experiment") 
	public void addExperiment () throws Exception {

		
		JSONObject json = utils.getExperimentFromFile();
		json.put("name", experimentName);
		json.put("stage", "PRODUCTION");
		json.put("enabled", true);
		json.put("indexExperiment", true);
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
		
        Thread.sleep(sleepTime);
        String status = expAnalyticsApi.getStatus(sessionToken);
        System.out.println(status);
    }
	

	@Test (dependsOnMethods="addExperiment", description ="Descrease end range") 
	public void descreaseEndRange () throws Exception {


		String experiment = expApi.getExperiment(experimentID, sessionToken);
		JSONObject expJson = new JSONObject(experiment);
	    JSONArray ranges = new JSONArray();
	    JSONObject range = new JSONObject();
	  
	    range.put("start", Long.parseLong("1503233000000")); //Sun Aug 20 2017 13:05:09 GMT+0300
	    range.put("end", Long.parseLong("1503229400000")); //Sun Aug 20 2017 14:43:20 GMT+0300
	    ranges.put(range);
	    expJson.put("ranges", ranges);
	        
		String response = expApi.updateExperiment(experimentID, expJson.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Experiment was not updated: " + response);
		
		String aasResponse = expAnalyticsApi.getExperiment(experimentID, sessionToken);
		Assert.assertFalse(aasResponse.contains("not found"), "Experiment was not published to aas server");
		
		
		
        Thread.sleep(sleepTime);
        String status = expAnalyticsApi.getStatus(sessionToken);
        System.out.println(status);
    }
	
	@Test (dependsOnMethods="descreaseEndRange", description ="Descrease start range") 
	public void descreaseStartRange () throws Exception {


		String experiment = expApi.getExperiment(experimentID, sessionToken);
		JSONObject expJson = new JSONObject(experiment);
	    JSONArray ranges = new JSONArray();
	    JSONObject range = new JSONObject();

	    range.put("start", Long.parseLong("1503226209000")); //Sun Aug 20 2017 13:50:09 GMT+0300
	    range.put("end", Long.parseLong("1503229400000")); //Sun Aug 20 2017 14:43:20 GMT+0300
	    ranges.put(range);
	    expJson.put("ranges", ranges);
	        
		String response = expApi.updateExperiment(experimentID, expJson.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Experiment was not updated: " + response);
		
		String aasResponse = expAnalyticsApi.getExperiment(experimentID, sessionToken);
		Assert.assertFalse(aasResponse.contains("not found"), "Experiment was not published to aas server");
		
		
		
        Thread.sleep(sleepTime);
        String status = expAnalyticsApi.getStatus(sessionToken);
        System.out.println(status);
    }
	
	@Test (dependsOnMethods="descreaseStartRange", description ="Increase range") 
	public void increaseRange () throws Exception {


		String experiment = expApi.getExperiment(experimentID, sessionToken);
		JSONObject expJson = new JSONObject(experiment);
	    JSONArray ranges = new JSONArray();
	    JSONObject range = new JSONObject();
	   
	    range.put("start", Long.parseLong("1503223509000")); //Sun Aug 20 2017 13:05:09 GMT+0300
	    range.put("end", Long.parseLong("1503233000000")); //Sun Aug 20 2017 15:43:20 GMT+0300


	    ranges.put(range);
	    expJson.put("ranges", ranges);
	        
		String response = expApi.updateExperiment(experimentID, expJson.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Experiment was not updated: " + response);
		
		String aasResponse = expAnalyticsApi.getExperiment(experimentID, sessionToken);
		Assert.assertFalse(aasResponse.contains("not found"), "Experiment was not published to aas server");
		
		
		
        Thread.sleep(sleepTime);
        String status = expAnalyticsApi.getStatus(sessionToken);
        System.out.println(status);
    }
	
	@AfterTest
	private void reset(){
		utils.reset(productID, sessionToken);
	}
}
