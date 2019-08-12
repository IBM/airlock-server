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

public class ExperimentLifeCycle {
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
		br = new BranchesRestApi();
		br.setURL(m_url);
		
		sessionToken = utils.sessionToken;
 		productID = utils.createProduct();
		seasonID = utils.createSeason(productID);
		
		experimentName = utils.setExperimentName();
	}


	@Test (description ="Add experiment") 
	public void addExperiment () throws Exception {

		/*JSONObject json = utils.getExperimentFromFile();
		json.put("name", experimentName);
		experimentID = expApi.createExperiment(productID, json.toString(), sessionToken);
		Assert.assertFalse(experimentID.contains("error"), "Experiment was not created: " + experimentID);
		String experiment = expApi.getExperiment(experimentID, sessionToken);
		JSONObject expJson = new JSONObject(experiment);
		
		String aasResponse = expAnalyticsApi.getExperiment(experimentID, sessionToken);
		JSONObject aasJson = new JSONObject(aasResponse);
		*/
		JSONObject json = utils.getExperimentFromFile();
		json.put("name", experimentName);
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
		
		airlockExperiment = expApi.getExperiment(experimentID, sessionToken);
		Assert.assertFalse(airlockExperiment.contains("error"), "Experiment was not found: " + experimentID);
		expJson = new JSONObject(airlockExperiment);
				
		String aasResponse = expAnalyticsApi.getExperiment(experimentID, sessionToken);
		Assert.assertFalse(aasResponse.contains("not found"), "Experiment was not published to aas server");		
		JSONObject aasJson = new JSONObject(aasResponse);
		
		//if owner is not specified, creator is the owner
		Assert.assertEquals(expJson.getString("creator"), aasJson.getString("owner"), "Incorrect owner in aas");
		// productName in aas
		JSONObject product = new JSONObject(p.getProduct(productID, sessionToken));
		Assert.assertEquals(product.getString("name"), aasJson.getString("productName"), "Incorrect product name in aas");
				
		//creationDate in experiment is start in aas & experiment range
		Assert.assertEquals(expJson.getString("lastModified"), aasJson.getString("startDate"), "Incorrect startDate in aas");
		Assert.assertEquals(expJson.getLong("lastModified"), expJson.getJSONArray("ranges").getJSONObject(0).getLong("start"), "Incorrect start in range in experiment");
		Assert.assertEquals(expJson.getLong("lastModified"), aasJson.getJSONArray("ranges").getJSONObject(0).getLong("start"), "Incorrect start in range in aas");
		
		//end in range is null in both
		Assert.assertTrue(expJson.getJSONArray("ranges").getJSONObject(0).isNull("end"), "Incorrect end in range in experiment");
		Assert.assertTrue(aasJson.getJSONArray("ranges").getJSONObject(0).isNull("end"), "Incorrect end in range in aas");
		
		//stage
		Assert.assertEquals(expJson.getString("stage"), aasJson.getString("stage"), "Incorrect stage in aas");
		//hypothesis
		Assert.assertEquals(expJson.getString("hypothesis"), aasJson.getString("hypothesis"), "Incorrect hypothesis in aas");
		//controlGroupVariants
		Assert.assertEqualsNoOrder(expJson.getJSONArray("controlGroupVariants").toArray(), aasJson.getJSONArray("controlGroupVariants").toArray(), "Incorrect controlGroupVariants in aas");
		
		//dashboardUrl
		//Assert.assertEquals(expJson.getString("dashboardUrl"), aasJson.getString("dashboardUrl"), "Incorrect dashboardUrl in aas");
		
		//check in all experiments
		String aasAllExperiments = expAnalyticsApi.getAllExperiments(sessionToken);
		JSONObject allExpJson = new JSONObject(aasAllExperiments);
		boolean found = false;
		for (int i=0; i< allExpJson.getJSONArray("experiments").size(); i++){
			JSONObject singleExp = allExpJson.getJSONArray("experiments").getJSONObject(i);
			if (singleExp.getString("name").equals(experimentName))
				found = true;
		}
		Assert.assertTrue(found, "Experiment was not found in the list of all aas experiments");
		
	}
	
	
	@Test (dependsOnMethods="addExperiment", description ="Update experiment name") 
	public void updateExperimentName () throws Exception {
		String airlockExperiment = expApi.getExperiment(experimentID, sessionToken);
		Assert.assertFalse(airlockExperiment.contains("error"), "Experiment was not found: " + experimentID);
		JSONObject expJson = new JSONObject(airlockExperiment);
		experimentName = utils.setExperimentName();
		expJson.put("name", experimentName);

		//update experiment in airlock
		String response = expApi.updateExperiment(experimentID, expJson.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Experiment was not updated: " + response);
			
		//get updated experiment in aas
		String aasResponse = expAnalyticsApi.getExperiment(experimentID, sessionToken);
		JSONObject aasJson = new JSONObject(aasResponse);
		
		Assert.assertEquals(expJson.getString("name"), aasJson.getString("name"), "Incorrect name in aas");

	}
	
	
	@Test (dependsOnMethods="updateExperimentName", description ="Update experiment fields") 
	public void updateExperiment () throws Exception {

		//add variant 
		String branch = FileUtils.fileToString(filePath + "experiments/branch1.txt", "UTF-8", false);
		String branchID = br.createBranch(seasonID, branch, BranchesRestApi.MASTER, sessionToken);
		Assert.assertFalse(branchID.contains("error"), "Branch1 was not created: " + branchID);

		String variant = FileUtils.fileToString(filePath + "experiments/variant1.txt", "UTF-8", false);
		JSONObject variantJson = new JSONObject(variant);
		variantJson.put("name", "controlGroupVariant");
		String variantID = expApi.createVariant(experimentID, variantJson.toString(), sessionToken);
		Assert.assertFalse(variantID.contains("error"), "Variant was not created: " + variantID);
		
		
		String airlockExperiment = expApi.getExperiment(experimentID, sessionToken);
		Assert.assertFalse(airlockExperiment.contains("error"), "Experiment was not found: " + experimentID);
		JSONObject expJson = new JSONObject(airlockExperiment);

		expJson.put("stage", "PRODUCTION");
		expJson.put("hypothesis", "new hypothesis");
		expJson.put("owner", "vicky1");
		expJson.put("description", "test experiment description");
		//expJson.put("dashboardUrl", "new url");
		
		//update experiment in airlock
		String response = expApi.updateExperiment(experimentID, expJson.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Experiment was not updated: " + response);
			
		//get updated experiment in aas
		String aasResponse = expAnalyticsApi.getExperiment(experimentID, sessionToken);
		JSONObject aasJson = new JSONObject(aasResponse);
		
		Assert.assertEquals(expJson.getString("owner"), aasJson.getString("owner"), "Incorrect owner in aas");
		Assert.assertEquals(expJson.getString("stage"), aasJson.getString("stage"), "Incorrect stage in aas");
		Assert.assertEquals(expJson.getString("hypothesis"), aasJson.getString("hypothesis"), "Incorrect hypothesis in aas");
		Assert.assertEquals(expJson.getString("description"), aasJson.getString("description"), "Incorrect description in aas");
		Assert.assertEqualsNoOrder(expJson.getJSONArray("controlGroupVariants").toArray(), aasJson.getJSONArray("controlGroupVariants").toArray(), "Incorrect controlGroupVariants in aas");
		
		//dashboardUrl
		//Assert.assertEquals(expJson.getString("dashboardUrl"), aasJson.getString("dashboardUrl"), "Incorrect dashboardUrl in aas");
		
	}
	

	
	@Test (dependsOnMethods="updateExperiment", description ="Delete experiment") 
	public void deleteExperiment () throws Exception {
		
		String airlockExperiment = expApi.getExperiment(experimentID, sessionToken);
		Assert.assertFalse(airlockExperiment.contains("error"), "Experiment was not found: " + experimentID);
		JSONObject expJson = new JSONObject(airlockExperiment);
		expJson.put("stage", "DEVELOPMENT");
		String response = expApi.updateExperiment(experimentID, expJson.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Experiment was not updated: " + response);
		
		int respCode = expApi.deleteExperiment(experimentID, sessionToken);
		Assert.assertEquals(respCode, 200, "Experiment was not deleted");
		
		String aasResponse = expAnalyticsApi.getExperiment(experimentID, sessionToken);
		Assert.assertTrue (aasResponse.contains("error"), "Experiment was found in aas though it was deleted in airlock");
		
		//check in all experiments
		String aasAllExperiments = expAnalyticsApi.getAllExperiments(sessionToken);
		JSONObject allExpJson = new JSONObject(aasAllExperiments);
		boolean found = false;
		for (int i=0; i< allExpJson.getJSONArray("experiments").size(); i++){
			JSONObject singleExp = allExpJson.getJSONArray("experiments").getJSONObject(i);
			if (singleExp.getString("name").equals(experimentName))
				found = true;
		}
		Assert.assertFalse(found, "Experiment was found in the list of all aas experiments though it was deleted in airlock");
		
	}
	
	
	@AfterTest
	private void reset(){
		utils.reset(productID, sessionToken);
	}
}
