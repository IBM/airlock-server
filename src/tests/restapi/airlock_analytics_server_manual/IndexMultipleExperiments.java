package tests.restapi.airlock_analytics_server_manual;


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

public class IndexMultipleExperiments {
	protected String productID;
	protected String seasonID;
	private String experimentID1;
	private String experimentID2;
	private String experimentID3;
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
	private BranchesRestApi br;
	
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

		sleepTime = Long.parseLong(System.getenv("BUCKET_OBSERVATION_INTERVAL_SEC"))*1000 + 300000;//add additional 5 min to interval
	}


	@Test (description ="Add enabled  experiment") 
	public void enabledIndexExperiment () throws Exception {
		
			experimentName = utils.setExperimentName();
			JSONObject json = utils.getExperimentFromFile();
			json.put("name", experimentName);
			json.put("stage", "PRODUCTION");
			json.put("enabled", true);
			json.put("indexExperiment", true);
			experimentID1 = expApi.createExperiment(productID, json.toString(), sessionToken);
			Assert.assertFalse(experimentID1.contains("error"), "Experiment was not created: " + experimentID1);
	
			String experiment = expApi.getExperiment(experimentID1, sessionToken);
			JSONObject expJson = new JSONObject(experiment);
		    JSONArray ranges = new JSONArray();
		    JSONObject range = new JSONObject();
		   //range contains 10 buckets
		    range.put("end", Long.parseLong("1503233000000")); //Sun Aug 20 2017 15:43:20 GMT+0300
		    range.put("start", Long.parseLong("1503223509000")); //Sun Aug 20 2017 13:05:09 GMT+0300
	
		    ranges.put(range);
		    expJson.put("ranges", ranges);
		        
			String response = expApi.updateExperiment(experimentID1, expJson.toString(), sessionToken);
			Assert.assertFalse(response.contains("error"), "Experiment1 was not updated: " + response);

			String aasResponse = expAnalyticsApi.getExperiment(experimentID1, sessionToken);
			Assert.assertFalse(aasResponse.contains("not found"), "Experiment was not published to aas server");
			
			
			
			//experiment 2
			experimentName = utils.setExperimentName();
			json = utils.getExperimentFromFile();
			json.put("name", experimentName);
			json.put("stage", "DEVELOPMENT");
			json.put("enabled", true);
			json.put("indexExperiment", true);
			experimentID2 = expApi.createExperiment(productID, json.toString(), sessionToken);
			Assert.assertFalse(experimentID2.contains("error"), "Experiment was not created: " + experimentID2);
	
			experiment = expApi.getExperiment(experimentID2, sessionToken);
			JSONObject expJson1 = new JSONObject(experiment);
		    JSONArray ranges1 = new JSONArray();
		    JSONObject range1 = new JSONObject();
		   //range contains 10 buckets
		    range1.put("start", Long.parseLong("1503223509000")); //Sun Aug 20 2017 13:05:09 GMT+0300
		    range1.put("end", Long.parseLong("1503229400000")); //Sun Aug 20 2017 14:43:20 GMT+0300
	
		    ranges1.put(range1);
		    expJson1.put("ranges", ranges1);
		        
			response = expApi.updateExperiment(experimentID2, expJson1.toString(), sessionToken);
			Assert.assertFalse(response.contains("error"), "Experiment2 was not updated: " + response);

			aasResponse = expAnalyticsApi.getExperiment(experimentID2, sessionToken);
			Assert.assertFalse(aasResponse.contains("not found"), "Experiment was not published to aas server");
		
			
			//experiment 3
			experimentName = utils.setExperimentName();
			json = utils.getExperimentFromFile();
			json.put("name", experimentName);
			json.put("stage", "DEVELOPMENT");
			json.put("enabled", true);
			json.put("indexExperiment", true);
			experimentID3 = expApi.createExperiment(productID, json.toString(), sessionToken);
			Assert.assertFalse(experimentID3.contains("error"), "Experiment was not created: " + experimentID3);
	
			experiment = expApi.getExperiment(experimentID3, sessionToken);
			JSONObject expJson2 = new JSONObject(experiment);
		    JSONArray ranges2 = new JSONArray();
		    JSONObject range2 = new JSONObject();
		   //range contains 10 buckets
		    range2.put("end", Long.parseLong("1503233000000")); //Sun Aug 20 2017 15:43:20 GMT+0300
		    range2.put("start", Long.parseLong("1503223509000")); //Sun Aug 20 2017 13:05:09 GMT+0300
	
		    ranges2.put(range2);
		    expJson2.put("ranges", ranges2);
		        
			response = expApi.updateExperiment(experimentID3, expJson2.toString(), sessionToken);
			Assert.assertFalse(response.contains("error"), "Experiment3 was not updated: " + response);

			aasResponse = expAnalyticsApi.getExperiment(experimentID3, sessionToken);
			Assert.assertFalse(aasResponse.contains("not found"), "Experiment was not published to aas server");
		
		//check that this experiment doesn't appear in experiments status
        Thread.sleep(sleepTime);
        String status = expAnalyticsApi.getStatus(sessionToken);
       // Assert.assertTrue(status.contains(experimentName), "Experiment name was not found in status list");
        System.out.println(status);
    }
	
	@Test (dependsOnMethods="enabledIndexExperiment", description ="Add disabled production experiment") 
	public void disableIndexExperiment () throws Exception {

		String experiment = expApi.getExperiment(experimentID1, sessionToken);
		JSONObject json = new JSONObject(experiment);		
		json.put("indexExperiment", false);
		experimentID1 = expApi.updateExperiment(experimentID1, json.toString(), sessionToken);
		Assert.assertFalse(experimentID1.contains("error"), "Experiment was not updated: " + experimentID1);
		
		String experiment2 = expApi.getExperiment(experimentID2, sessionToken);
		JSONObject json2 = new JSONObject(experiment2);		
	    JSONArray ranges2 = new JSONArray();
	    JSONObject range2 = new JSONObject();
	   //range contains 10 buckets
	    range2.put("end", Long.parseLong("1503233000000")); //Sun Aug 20 2017 15:43:20 GMT+0300
	    range2.put("start", Long.parseLong("1503223509000")); //Sun Aug 20 2017 13:05:09 GMT+0300
	    ranges2.put(range2);
	    json2.put("ranges", ranges2);		
		experimentID2 = expApi.updateExperiment(experimentID2, json2.toString(), sessionToken);
		Assert.assertFalse(experimentID2.contains("error"), "Experiment was not created: " + experimentID2);
		
		
		
		String branch = FileUtils.fileToString(filePath + "experiments/branch1.txt", "UTF-8", false);
		String branchID = br.createBranch(seasonID, branch, BranchesRestApi.MASTER, sessionToken);
		Assert.assertFalse(branchID.contains("error"), "Branch1 was not created: " + branchID);
		String variant = FileUtils.fileToString(filePath + "experiments/variant1.txt", "UTF-8", false);
		JSONObject variantJson = new JSONObject(variant);		
		variantJson.put("name", "controlGroupVariant1");
		String variantID1 = expApi.createVariant(experimentID3, variantJson.toString(), sessionToken);
		Assert.assertFalse(variantID1.contains("error"), "Variant was not updated: " + variantID1);		
		variantJson.put("name", "variant2");
		String variantID2 = expApi.createVariant(experimentID3, variantJson.toString(), sessionToken);
		Assert.assertFalse(variantID2.contains("error"), "Variant was not created: " + variantID2);
		String experiment3 = expApi.getExperiment(experimentID3, sessionToken);
		JSONObject json3 = new JSONObject(experiment3);		
		json3.getJSONArray("controlGroupVariants").put("controlGroupVariant1");
		experimentID3 = expApi.updateExperiment(experimentID3, json3.toString(), sessionToken);
		Assert.assertFalse(experimentID3.contains("error"), "Experiment was not updated: " + experimentID3);
		
		
		
		Thread.sleep(sleepTime);
        String status = expAnalyticsApi.getStatus(sessionToken);
        System.out.println(status);
    }
	

	@Test (dependsOnMethods="disableIndexExperiment", description ="Delete experiment") 
	public void deleteExperiment1 () throws Exception {

		String experiment = expApi.getExperiment(experimentID1, sessionToken);
		JSONObject json = new JSONObject(experiment);		
		json.put("stage", "DEVELOPMENT");
		
		String response = expApi.updateExperiment(experimentID1, json.toString(), sessionToken);
		Assert.assertFalse(experimentID1.contains("error"), "Experiment was not updated: " + experimentID1);
		
		int codeResp = expApi.deleteExperiment(experimentID1, sessionToken);
		Assert.assertTrue(codeResp == 200, "Experiment was not deleted");
		
		String aasResponse = expAnalyticsApi.getExperiment(experimentID1, sessionToken);
		Assert.assertTrue(aasResponse.contains("not found"), "Experiment was not deleted from aas server");
		
		String status = expAnalyticsApi.getStatus(sessionToken);
		//
		
		Thread.sleep(sleepTime);
        status = expAnalyticsApi.getStatus(sessionToken);
        System.out.println(status);
    }
	
	@AfterTest
	private void reset() throws Exception{
		utils.reset(productID, sessionToken);
	}
}
