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

public class ExperimentControlGroups {
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
		br = new BranchesRestApi();
		br.setURL(m_url);
		
		sessionToken = utils.sessionToken;
 		productID = utils.createProduct();
		seasonID = utils.createSeason(productID);
		
		experimentName = utils.setExperimentName();
	}


	@Test (description ="Add experiment") 
	public void addExperiment () throws Exception {

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
		variantID1 = expApi.createVariant(experimentID, variantJson1.toString(), sessionToken);
		Assert.assertFalse(variantID1.contains("error"), "Variant1 was not created: " + variantID1);
					
		String airlockExperiment = expApi.getExperiment(experimentID, sessionToken);
		Assert.assertFalse(airlockExperiment.contains("error"), "Experiment was not found: " + experimentID);

		//enable experiment so a range will be created and the experiment will be published to analytics server
		JSONObject expJson = new JSONObject(airlockExperiment);
		expJson.put("enabled", true);
		
		String response = expApi.updateExperiment(experimentID, expJson.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Experiment was not updated: " + response); 
		
		String aasResponse = expAnalyticsApi.getExperiment(experimentID, sessionToken);
		Assert.assertFalse(aasResponse.contains("not found"), "Experiment was not published to aas server");

	}
	
	
	@Test (dependsOnMethods="addExperiment", description ="Update experiment controlGroupVariant") 
	public void addExistingControlGroups () throws Exception {

		//add variant 
		String branch = FileUtils.fileToString(filePath + "experiments/branch1.txt", "UTF-8", false);
		String branchID = br.createBranch(seasonID, branch, BranchesRestApi.MASTER, sessionToken);
		Assert.assertFalse(branchID.contains("error"), "Branch1 was not created: " + branchID);

		String variant = FileUtils.fileToString(filePath + "experiments/variant1.txt", "UTF-8", false);
		JSONObject variantJson = new JSONObject(variant);
		
		variantJson.put("name", "controlGroupVariant1");
		variantID1 = expApi.createVariant(experimentID, variantJson.toString(), sessionToken);
		Assert.assertFalse(variantID1.contains("error"), "Variant was not created: " + variantID1);
		
		variantJson.put("name", "controlGroupVariant2");
		String variantID2 = expApi.createVariant(experimentID, variantJson.toString(), sessionToken);
		Assert.assertFalse(variantID2.contains("error"), "Variant was not created: " + variantID2);
		
		//check that variants array was updated in aas
		String aasResponse = expAnalyticsApi.getExperiment(experimentID, sessionToken);
		JSONObject aasJson = new JSONObject(aasResponse);
		Assert.assertTrue(aasJson.getJSONArray("variants").contains("controlGroupVariant1"), "controlGroupVariant1 was not found in a list of variants");
		Assert.assertTrue(aasJson.getJSONArray("variants").contains("controlGroupVariant2"), "controlGroupVariant2 was not found in a list of variants");
		
		
		String airlockExperiment = expApi.getExperiment(experimentID, sessionToken);
		Assert.assertFalse(airlockExperiment.contains("error"), "Experiment was not found: " + experimentID);
		JSONObject expJson = new JSONObject(airlockExperiment);

		expJson.getJSONArray("controlGroupVariants").put("controlGroupVariant1");
		expJson.getJSONArray("controlGroupVariants").put("controlGroupVariant2");
		
		String response = expApi.updateExperiment(experimentID, expJson.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Experiment was not updated: " + response);
			
		//get updated experiment in aas
		aasResponse = expAnalyticsApi.getExperiment(experimentID, sessionToken);
		aasJson = new JSONObject(aasResponse);
		
		Assert.assertEqualsNoOrder(expJson.getJSONArray("controlGroupVariants").toArray(), aasJson.getJSONArray("controlGroupVariants").toArray(), "Incorrect controlGroupVariants in aas");
		
	}
	
	@Test (dependsOnMethods="addExistingControlGroups", description ="Add variant that doesn't exist to controlGroupVariant") 
	public void addNonExistingControlGroups () throws Exception {
		
		String airlockExperiment = expApi.getExperiment(experimentID, sessionToken);
		Assert.assertFalse(airlockExperiment.contains("error"), "Experiment was not found: " + experimentID);
		JSONObject expJson = new JSONObject(airlockExperiment);

		expJson.getJSONArray("controlGroupVariants").put("noControlGroupVariant1");
		expJson.getJSONArray("controlGroupVariants").put("noControlGroupVariant2");
		
		String response = expApi.updateExperiment(experimentID, expJson.toString(), sessionToken);
		Assert.assertTrue(response.contains("error"), "Experiment was incorrectly updated");
			
		//get updated experiment in aas
		String aasResponse = expAnalyticsApi.getExperiment(experimentID, sessionToken);
		JSONObject aasJson = new JSONObject(aasResponse);
		airlockExperiment = expApi.getExperiment(experimentID, sessionToken);
		expJson = new JSONObject(airlockExperiment);
		
		Assert.assertEqualsNoOrder(expJson.getJSONArray("controlGroupVariants").toArray(), aasJson.getJSONArray("controlGroupVariants").toArray(), "Incorrect controlGroupVariants list in aas");
		
	}
	
	@Test (dependsOnMethods="addNonExistingControlGroups", description ="Remove one controlGroupVariant") 
	public void removeControlGroup () throws Exception {
		
		String airlockExperiment = expApi.getExperiment(experimentID, sessionToken);
		Assert.assertFalse(airlockExperiment.contains("error"), "Experiment was not found: " + experimentID);
		JSONObject expJson = new JSONObject(airlockExperiment);

		expJson.getJSONArray("controlGroupVariants").remove("controlGroupVariant2");
		
		String response = expApi.updateExperiment(experimentID, expJson.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Experiment was not updated: " + response);
			
		//get updated experiment in aas
		String aasResponse = expAnalyticsApi.getExperiment(experimentID, sessionToken);
		JSONObject aasJson = new JSONObject(aasResponse);
		
		Assert.assertEqualsNoOrder(expJson.getJSONArray("controlGroupVariants").toArray(), aasJson.getJSONArray("controlGroupVariants").toArray(), "Incorrect controlGroupVariants list in aas");
		
	}
	
	@Test (dependsOnMethods="removeControlGroup", description ="Delete variant used in controlGroupVariant") 
	public void deleteVariant () throws Exception {
		
		int respCode = expApi.deleteVariant(variantID1, sessionToken);
		Assert.assertTrue(respCode==200, "variant was not deleted");
		
		String airlockExperiment = expApi.getExperiment(experimentID, sessionToken);
		JSONObject expJson = new JSONObject(airlockExperiment);
		Assert.assertFalse(expJson.getJSONArray("controlGroupVariants").contains("controlGroupVariant1"), "Variant1 was not deleted from the list of  controlGroupVariants" );
					
		//get updated experiment in aas
		String aasResponse = expAnalyticsApi.getExperiment(experimentID, sessionToken);
		JSONObject aasJson = new JSONObject(aasResponse);
		
		Assert.assertFalse(aasJson.getJSONArray("controlGroupVariants").contains("controlGroupVariant1"), "Incorrect controlGroupVariants list in aas after deleting variant");
		
	}
	
	
	@AfterTest
	private void reset(){
		utils.reset(productID, sessionToken);
	}
}
