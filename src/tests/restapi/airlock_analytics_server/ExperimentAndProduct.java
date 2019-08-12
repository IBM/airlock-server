package tests.restapi.airlock_analytics_server;


import org.apache.commons.lang3.RandomStringUtils;
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

public class ExperimentAndProduct {
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

	
	@Test (description ="Update product name") 
	public void updateProductName () throws Exception {

		JSONObject json = utils.getExperimentFromFile();
		json.put("name", experimentName);
		json.put("enabled", false);
		experimentID = expApi.createExperiment(productID, json.toString(), sessionToken);
		Assert.assertFalse(experimentID.contains("error"), "Experiment was not created: " + experimentID);
		
		//add variant
		String branch = FileUtils.fileToString(filePath + "experiments/branch1.txt", "UTF-8", false);
		String branchID = br.createBranch(seasonID, branch, BranchesRestApi.MASTER, sessionToken);
		Assert.assertFalse(branchID.contains("error"), "Branch1 was not created: " + branchID);

		String variant = FileUtils.fileToString(filePath + "experiments/variant1.txt", "UTF-8", false);
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
		
		//update product
		response = p.getProduct(productID, sessionToken);
		JSONObject pJson = new JSONObject(response);
		String productName = RandomStringUtils.randomAlphabetic(5);
		pJson.put("name", productName);
		pJson.put("codeIdentifier", productName);
		pJson.remove("seasons");	//seasons array can't be passed during product update
		response = p.updateProduct(productID, pJson.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Product name was not updated: " + response);
		
		aasResponse = expAnalyticsApi.getExperiment(experimentID, sessionToken);
		JSONObject aasJson = new JSONObject(aasResponse);
		Assert.assertEquals(aasJson.getString("productName"), productName, "Product name was not updated in experiment in aas");

		//check in all experiments
		String aasAllExperiments = expAnalyticsApi.getAllExperiments(sessionToken);
		JSONObject allExpJson = new JSONObject(aasAllExperiments);
		boolean found = false;
		for (int i=0; i< allExpJson.getJSONArray("experiments").size(); i++){
			JSONObject singleExp = allExpJson.getJSONArray("experiments").getJSONObject(i);
			if (singleExp.getString("name").equals(experimentName) && singleExp.getString("productName").equals(productName)) {
				found = true;
			}	
		}
		Assert.assertTrue(found, "Experiment productName was not updated in the list of all aas experiments");
	}
	
	@Test (dependsOnMethods="updateProductName", description ="Delete product") 
	public void addExperimentDeleteProduct () throws Exception {
		
		//delete product
		int deleted = p.deleteProduct(productID);
		Assert.assertTrue(deleted==200, "Product was not deleted");
		
		String aasResponse = expAnalyticsApi.getExperiment(experimentID, sessionToken);
		Assert.assertTrue(aasResponse.contains("not found"), "Experiment was not deleted from aas when product was deleted");

		//check in all experiments
		String aasAllExperiments = expAnalyticsApi.getAllExperiments(sessionToken);
		JSONObject allExpJson = new JSONObject(aasAllExperiments);
		boolean found = false;
		for (int i=0; i< allExpJson.getJSONArray("experiments").size(); i++){
			JSONObject singleExp = allExpJson.getJSONArray("experiments").getJSONObject(i);
			if (singleExp.getString("name").equals(experimentName))
				found = true;
		}
		Assert.assertFalse(found, "Deleted experiment was found in the list of all aas experiments");
	}
	

		
	@AfterTest
	private void reset(){
		utils.reset(productID, sessionToken);
	}
}
