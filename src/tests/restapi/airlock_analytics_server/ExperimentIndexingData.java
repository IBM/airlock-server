package tests.restapi.airlock_analytics_server;


import org.apache.commons.lang3.RandomStringUtils;
import org.apache.wink.json4j.JSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import tests.com.ibm.qautils.FileUtils;
import tests.com.ibm.qautils.JSONUtils;
import tests.restapi.BranchesRestApi;
import tests.restapi.ExperimentAnalyticsApi;
import tests.restapi.ExperimentsRestApi;
import tests.restapi.ProductsRestApi;
import tests.restapi.SeasonsRestApi;

public class ExperimentIndexingData {
	protected String productID;
	protected String seasonID;
	private String experimentID;
	private String experimentID2;
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
		br = new BranchesRestApi();
		br.setURL(m_url);
		expApi = new ExperimentsRestApi();
		expApi.setURL(m_analyticsUrl); 
		expAnalyticsApi = new ExperimentAnalyticsApi();
		expAnalyticsApi.setURL(analyticsServerUrl);
		sessionToken = utils.sessionToken;
 		productID = utils.createProduct();
		seasonID = utils.createSeason(productID);
		
		experimentName = utils.setExperimentName();
	}


	@Test (description ="Add enabled experiment") 
	public void addEnabledExperiment () throws Exception {

		// experiment created as disabled is not published to analytics
		JSONObject json = utils.getExperimentFromFile();
		json.put("name", experimentName);
		json.put("displayName", experimentName);
		json.put("enabled", false);
		experimentID = expApi.createExperiment(productID, json.toString(), sessionToken);
		Assert.assertFalse(experimentID.contains("error"), "Experiment was not created: " + experimentID);
		
	
		String branch = FileUtils.fileToString(filePath + "experiments/branch1.txt", "UTF-8", false);
		String branchID = br.createBranch(seasonID, branch, BranchesRestApi.MASTER, sessionToken);
		Assert.assertFalse(branchID.contains("error"), "Branch1 was not created: " + branchID);

		String variant = FileUtils.fileToString(filePath + "experiments/variant1.txt", "UTF-8", false);
		JSONObject variantJson1 = new JSONObject(variant);
		
		variantJson1.put("name", "variant1");
		variantJson1.put("description", "variant1 desc");
		variantJson1.put("displayName", "variant1 DN");
		String variantID1 = expApi.createVariant(experimentID, variantJson1.toString(), sessionToken);
		Assert.assertFalse(variantID1.contains("error"), "Variant1 was not created: " + variantID1);
		
		JSONObject variantJson2 = new JSONObject(variant);
		variantJson2.put("name", "variant2");
		variantJson2.put("description", "variant2 desc");
		variantJson2.put("displayName", "variant2 DN");
		String variantID2 = expApi.createVariant(experimentID, variantJson2.toString(), sessionToken);
		Assert.assertFalse(variantID2.contains("error"), "Variant2 was not created: " + variantID2);
	
		String airlockExperiment = expApi.getExperiment(experimentID, sessionToken);
		Assert.assertFalse(airlockExperiment.contains("error"), "Experiment was not found: " + experimentID);

		//enable experiment so a range will be created and the experiment will be published to analytics server
		JSONObject expJson = new JSONObject(airlockExperiment);
		expJson.put("enabled", true);
		
		String response = expApi.updateExperiment(experimentID, expJson.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Experiment was not updated: " + response); 
		
		String aasResponse = expAnalyticsApi.getExperiment(experimentID, sessionToken);
		Assert.assertFalse(aasResponse.contains("not found"), "Experiment was not published to aas server");
		
		
		String experiment = expApi.getExperiment(experimentID, true, sessionToken);
		expJson = new JSONObject(experiment);
		
		Assert.assertTrue(expJson.containsKey("indexingInfo"), "Indexing data was not returned");
		JSONObject indexingData = expJson.getJSONObject("indexingInfo");
		Assert.assertEquals(expJson.getJSONArray("ranges"), indexingData.getJSONArray("ranges"), "Ranges are different");
		Assert.assertEquals(expJson.getString("displayName"), indexingData.getString("displayName"), "displayName are different");
		Assert.assertTrue(indexingData.getJSONArray("variants").contains("variant1"), "variant1  was not found in indexingData variants");
		Assert.assertTrue(indexingData.getJSONArray("variants").contains("variant2"), "variant2  was not found in indexingData variants");
		Assert.assertTrue(indexingData.getJSONArray("variantsDescriptions").contains("variant1 desc"), "variant1 desc was not found in indexingData variantsDescriptions");
		Assert.assertTrue(indexingData.getJSONArray("variantsDescriptions").contains("variant2 desc"), "variant2 desc was not found in indexingData variantsDescriptions");
		Assert.assertTrue(indexingData.getJSONArray("variantsDisplayNames").contains("variant1 DN"), "variant1 displayName was not found in indexingData variantsDisplayNames");
		Assert.assertTrue(indexingData.getJSONArray("variantsDisplayNames").contains("variant2 DN"), "variant2 displayName was not found in indexingData variantsDisplayNames");

	}
	
	
	@Test (dependsOnMethods="addEnabledExperiment", description ="Add disabled experiment") 
	public void addDisabledExperiment () throws Exception {

		// experiment created as disabled is not published to analytics
		JSONObject json = utils.getExperimentFromFile();
		String disabledExpName = "experiment."+RandomStringUtils.randomAlphabetic(3);
		json.put("name", disabledExpName);
		json.put("displayName", disabledExpName);
		json.put("enabled", false);
		experimentID2 = expApi.createExperiment(productID, json.toString(), sessionToken);
		Assert.assertFalse(experimentID2.contains("error"), "Experiment was not created: " + experimentID2);
			
		String experiment = expApi.getExperiment(experimentID2, true, sessionToken);
		JSONObject expJson = new JSONObject(experiment);
		
		Assert.assertTrue(expJson.containsKey("indexingInfo"), "Indexing data was not returned");
		Assert.assertTrue(expJson.isNull("indexingInfo"), "Indexing data is not null");

	}
	
	@Test (dependsOnMethods="addDisabledExperiment", description ="Get all experiments")
	public void getAllExperiments() throws Exception{
		String response = expApi.getAllExperiments(productID, true, sessionToken);
		JSONObject allExpJson = new JSONObject(response);
		
		String experiment = expApi.getExperiment(experimentID, true, sessionToken);
		JSONObject expJson = new JSONObject(experiment);		
		Assert.assertTrue(JSONUtils.equals(expJson.getJSONObject("indexingInfo"), allExpJson.getJSONArray("experiments").getJSONObject(0).getJSONObject("indexingInfo"), new String[]{}), "indexing data of experiment1 is different in experiment and allExperiments");
						
		Assert.assertTrue(allExpJson.getJSONArray("experiments").getJSONObject(1).isNull("indexingInfo"), "indexing data of experiment2 is not null in allExperiments");
		
	}
	
		
	@AfterTest
	private void reset(){
		utils.reset(productID, sessionToken);
	}
}
