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

public class DisableExperiment {
	protected String productID;
	protected String seasonID;
	private String experimentID;
	protected String filePath;
	protected SeasonsRestApi s;
	protected String m_url;
	private String sessionToken = "";
	private Config utils;
	private ExperimentsRestApi expApi ;
	private String m_analyticsUrl;
	private ProductsRestApi p;
	private String experimentName;
	private ExperimentAnalyticsApi expAnalyticsApi;
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


	@Test (description ="Add disabled experiment") 
	public void addExperiment () throws Exception {

		// experiment created as disabled is not published to analytics
		JSONObject json = utils.getExperimentFromFile();
		json.put("name", experimentName);
		json.put("enabled", false);
		experimentID = expApi.createExperiment(productID, json.toString(), sessionToken);
		Assert.assertFalse(experimentID.contains("error"), "Experiment was not created: " + experimentID);
		String experiment = expApi.getExperiment(experimentID, sessionToken);
		JSONObject expJson = new JSONObject(experiment);
		
		String aasResponse = expAnalyticsApi.getExperiment(experimentID, sessionToken);
		Assert.assertTrue(aasResponse.contains("not found"), "Experiment created as disabled was published to aas server");

		//ranges are empty
		Assert.assertTrue(expJson.getJSONArray("ranges").size()==0, "Number of ranges in new disabled experiment is not 0");
	}
	
	
	@Test (dependsOnMethods="addExperiment", description ="Enable experiment") 
	public void enableExperiment () throws Exception {

		//move to enabled, check is aas
		String airlockExperiment = expApi.getExperiment(experimentID, sessionToken);
		Assert.assertFalse(airlockExperiment.contains("error"), "Experiment was not found: " + experimentID);
		JSONObject expJson = new JSONObject(airlockExperiment);
		expJson.put("enabled", true);
		
		//update experiment in airlock
		String response = expApi.updateExperiment(experimentID, expJson.toString(), sessionToken);
		//experiment cannot be enabled when no variant exists
		Assert.assertTrue(response.contains("error"), "Experiment was not updated: " + response); 
			
		//add variant 
		String branch = FileUtils.fileToString(filePath + "experiments/branch1.txt", "UTF-8", false);
		String branchID = br.createBranch(seasonID, branch, BranchesRestApi.MASTER, sessionToken);
		Assert.assertFalse(branchID.contains("error"), "Branch1 was not created: " + branchID);

		String variant = FileUtils.fileToString(filePath + "experiments/variant1.txt", "UTF-8", false);
		JSONObject variantJson1 = new JSONObject(variant);
		
		variantJson1.put("name", "variant1");
		variantJson1.put("description", "variant1 desc");
		variantJson1.put("displayName", "variant1 DN");
		variantID1 = expApi.createVariant(experimentID, variantJson1.toString(), sessionToken);
		Assert.assertFalse(variantID1.contains("error"), "Variant1 was not created: " + variantID1);
				
		airlockExperiment = expApi.getExperiment(experimentID, sessionToken);
		Assert.assertFalse(airlockExperiment.contains("error"), "Experiment was not found: " + experimentID);
		expJson = new JSONObject(airlockExperiment);
		expJson.put("enabled", true);
		
		response = expApi.updateExperiment(experimentID, expJson.toString(), sessionToken);
		//After adding variant the experiemnt cn be enabled
		Assert.assertFalse(response.contains("error"), "Experiment was not updated: " + response); 
				
		//get updated experiment in aas
		String aasResponse = expAnalyticsApi.getExperiment(experimentID, sessionToken);
		JSONObject aasJson = new JSONObject(aasResponse);
		airlockExperiment = expApi.getExperiment(experimentID, sessionToken);
		expJson = new JSONObject(airlockExperiment);

		
		Assert.assertTrue(aasJson.getJSONArray("ranges").size()==1, "Incorrect number of ranges");
		Assert.assertEquals(aasJson.getJSONArray("ranges").getJSONObject(0).getLong("start"), expJson.getLong("lastModified"), "Incorrect start date in the last range");
		Assert.assertTrue(aasJson.getJSONArray("ranges").getJSONObject(0).isNull("end"), "Incorrect end date in the last range");
		Assert.assertEquals(aasJson.getLong("startDate"), expJson.getLong("lastModified"), "Incorrect end date in the second range");
	}
	
	
	@Test (dependsOnMethods="enableExperiment", description ="Disable experiment") 
	public void disableExperiment () throws Exception {

		//When it is moved to disabled, it should remain in analytics with closed range
		String airlockExperiment = expApi.getExperiment(experimentID, sessionToken);
		Assert.assertFalse(airlockExperiment.contains("error"), "Experiment was not found: " + experimentID);
		JSONObject expJson = new JSONObject(airlockExperiment);
		expJson.put("enabled", false);
		
		//update experiment in airlock
		String response = expApi.updateExperiment(experimentID, expJson.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Experiment was not updated: " + response);
		
		String experiment = expApi.getExperiment(experimentID, sessionToken);
		expJson = new JSONObject(experiment);
			
		//get updated experiment in aas
		String aasResponse = expAnalyticsApi.getExperiment(experimentID, sessionToken);
		JSONObject aasJson = new JSONObject(aasResponse);
		
		airlockExperiment = expApi.getExperiment(experimentID, sessionToken);
		expJson = new JSONObject(airlockExperiment);

		
		Assert.assertTrue(aasJson.getJSONArray("ranges").size()==1, "Incorrect number of ranges");
		Assert.assertEquals(aasJson.getJSONArray("ranges").getJSONObject(0).getLong("end"), expJson.getLong("lastModified"), "Incorrect end date in the second range");
		//end date is null if no data is indexed in the last range otherwise it is the date of the last indexed bucket
		//Assert.assertEquals(aasJson.getLong("endDate"), expJson.getLong("lastModified"), "Incorrect end date in the second range");
	}
	
	@Test (dependsOnMethods="disableExperiment", description ="Disable experiment after enabling") 
	public void enableExperimentAfterDisable () throws Exception {

		//move to disable, check is aas
		String airlockExperiment = expApi.getExperiment(experimentID, sessionToken);
		Assert.assertFalse(airlockExperiment.contains("error"), "Experiment was not found: " + experimentID);
		JSONObject expJson = new JSONObject(airlockExperiment);
		expJson.put("enabled", true);
		
		//update experiment in airlock
		String response = expApi.updateExperiment(experimentID, expJson.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Experiment was not updated: " + response);
			
		//get updated experiment in aas
		String aasResponse = expAnalyticsApi.getExperiment(experimentID, sessionToken);
		JSONObject aasJson = new JSONObject(aasResponse);
		airlockExperiment = expApi.getExperiment(experimentID, sessionToken);
		expJson = new JSONObject(airlockExperiment);

		
		Assert.assertTrue(aasJson.getJSONArray("ranges").size()==2, "Incorrect number of ranges");
		Assert.assertEquals(aasJson.getJSONArray("ranges").getJSONObject(1).getLong("start"), expJson.getLong("lastModified"), "Incorrect start date in the last range");
		Assert.assertTrue(aasJson.getJSONArray("ranges").getJSONObject(1).isNull("end"), "Incorrect end date in the last range");
		Assert.assertEquals(aasJson.getLong("startDate"), expJson.getLong("lastModified"), "Incorrect end date in the second range");
	}
	
	
	
	@Test (dependsOnMethods="enableExperimentAfterDisable", description ="Trying to create enabled experiment") 
	public void addEnabledExperiment () throws Exception {

		experimentName = utils.setExperimentName();
		JSONObject json = utils.getExperimentFromFile();
		json.put("name", experimentName);
		json.put("enabled", true);
		experimentID = expApi.createExperiment(productID, json.toString(), sessionToken);
		
		//experiment cannot be enabled when no variant exists
		Assert.assertTrue(experimentID.contains("error"), "Experiment was not created: " + experimentID);
		
		
		json.put("enabled", false);
		
		experimentID = expApi.createExperiment(productID, json.toString(), sessionToken);
		Assert.assertFalse(experimentID.contains("error"), "Experiment was not created: " + experimentID);
		
		//add variant (branch already created) 
		String variant = FileUtils.fileToString(filePath + "experiments/variant1.txt", "UTF-8", false);
		JSONObject variantJson1 = new JSONObject(variant);
		
		variantJson1.put("name", "variant1");
		variantJson1.put("description", "variant1 desc");
		variantJson1.put("displayName", "variant1 DN");
		variantID1 = expApi.createVariant(experimentID, variantJson1.toString(), sessionToken);
		Assert.assertFalse(variantID1.contains("error"), "Variant1 was not created: " + variantID1);
				
		
		String experiment = expApi.getExperiment(experimentID, sessionToken);
		JSONObject expJson = new JSONObject(experiment);
		expJson.put("enabled", true);
		String response = expApi.updateExperiment(experimentID, expJson.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Experiment was not updated: " + response);
		
		String aasResponse = expAnalyticsApi.getExperiment(experimentID, sessionToken);
		Assert.assertFalse(aasResponse.contains("error"), "Experiment was not published to aas server: " + aasResponse);
		
		
		//disable experiment and check ranges
		
		String airlockExperiment = expApi.getExperiment(experimentID, sessionToken);
		Assert.assertFalse(airlockExperiment.contains("error"), "Experiment was not found: " + experimentID);
		expJson = new JSONObject(airlockExperiment);
		expJson.put("enabled", false);
		response = expApi.updateExperiment(experimentID, expJson.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Experiment was not updated: " + response);
		
		aasResponse = expAnalyticsApi.getExperiment(experimentID, sessionToken);
		JSONObject aasJson = new JSONObject(aasResponse);
		airlockExperiment = expApi.getExperiment(experimentID, sessionToken);
		expJson = new JSONObject(airlockExperiment);
		
		Assert.assertTrue(aasJson.getJSONArray("ranges").size()==1, "Incorrect number of ranges");
		Assert.assertFalse(aasJson.getJSONArray("ranges").getJSONObject(0).isNull("end"), "Incorrect end date in the last range");
	}
	
	
	
		
	@AfterTest
	private void reset(){
		utils.reset(productID, sessionToken);
	}
}
