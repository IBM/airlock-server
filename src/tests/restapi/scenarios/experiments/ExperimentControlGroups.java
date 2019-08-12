package tests.restapi.scenarios.experiments;


import java.io.IOException;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.wink.json4j.JSONException;
import org.apache.wink.json4j.JSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import tests.com.ibm.qautils.FileUtils;
import tests.restapi.AirlockUtils;
import tests.restapi.BranchesRestApi;
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
	private ExperimentsRestApi expApi ;
	private String m_analyticsUrl;
	private ProductsRestApi p;
	private BranchesRestApi br ;
	private String variantID1;
	private AirlockUtils baseUtils;
	
	@BeforeClass
	@Parameters({"url", "analyticsUrl", "translationsUrl", "configPath", "sessionToken", "userName", "userPassword", "appName", "productsToDeleteFile"})
	public void init(String url, String analyticsUrl, String translationsUrl, String configPath, String sToken, String userName, String userPassword, String appName, String productsToDeleteFile) throws Exception{
		filePath = configPath ;
		m_url = url;
		m_analyticsUrl = analyticsUrl;
		s = new SeasonsRestApi();
		s.setURL(m_url);
		p = new ProductsRestApi();
		p.setURL(m_url);
		expApi = new ExperimentsRestApi();
		expApi.setURL(m_analyticsUrl); 

		br = new BranchesRestApi();
		br.setURL(m_url);
		
		baseUtils = new AirlockUtils(m_url, m_analyticsUrl, translationsUrl, configPath, sToken, userName, userPassword, appName, productsToDeleteFile);
		sessionToken = baseUtils.sessionToken;

		productID = baseUtils.createProduct();
		baseUtils.printProductToFile(productID);
		seasonID = baseUtils.createSeason(productID);

	}


	@Test (description ="Add experiment") 
	public void addExperiment () throws Exception {

		experimentID = addExperiment("experiment."+RandomStringUtils.randomAlphabetic(5));
		Assert.assertFalse(experimentID.contains("error"), "Experiment was not created: " + experimentID);
		
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
				
		String airlockExperiment = expApi.getExperiment(experimentID, sessionToken);
		Assert.assertFalse(airlockExperiment.contains("error"), "Experiment was not found: " + experimentID);
		JSONObject expJson = new JSONObject(airlockExperiment);

		expJson.getJSONArray("controlGroupVariants").put("controlGroupVariant1");
		expJson.getJSONArray("controlGroupVariants").put("controlGroupVariant2");
		
		String response = expApi.updateExperiment(experimentID, expJson.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Experiment was not updated: " + response);
		
	}
	
	@Test (dependsOnMethods="addExistingControlGroups", description ="Add variant that doesn't exist to controlGroupVariant") 
	public void addNonExistingControlGroups () throws Exception {
		
		String airlockExperiment = expApi.getExperiment(experimentID, sessionToken);
		Assert.assertFalse(airlockExperiment.contains("error"), "Experiment was not found: " + experimentID);
		JSONObject expJson = new JSONObject(airlockExperiment);

		expJson.getJSONArray("controlGroupVariants").put("noControlGroupVariant1");

		String response = expApi.updateExperiment(experimentID, expJson.toString(), sessionToken);
		Assert.assertTrue(response.contains("error"), "Experiment was incorrectly updated");
			
		
	}
	
	@Test (dependsOnMethods="addNonExistingControlGroups", description ="Remove one controlGroupVariant") 
	public void removeControlGroup () throws Exception {
		
		String airlockExperiment = expApi.getExperiment(experimentID, sessionToken);
		Assert.assertFalse(airlockExperiment.contains("error"), "Experiment was not found: " + experimentID);
		JSONObject expJson = new JSONObject(airlockExperiment);
		expJson.getJSONArray("controlGroupVariants").remove("controlGroupVariant2");
		
		String response = expApi.updateExperiment(experimentID, expJson.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Experiment was not updated: " + response);
		airlockExperiment = expApi.getExperiment(experimentID, sessionToken);			
		expJson = new JSONObject(airlockExperiment);
		Assert.assertFalse(expJson.getJSONArray("controlGroupVariants").contains("controlGroupVariant2"), "Incorrect controlGroupVariants list");
		
	}
	
	@Test (dependsOnMethods="removeControlGroup", description ="Delete variant used in controlGroupVariant") 
	public void deleteVariant () throws Exception {
		
		int respCode = expApi.deleteVariant(variantID1, sessionToken);
		Assert.assertTrue(respCode==200, "variant was not deleted");
		
		String airlockExperiment = expApi.getExperiment(experimentID, sessionToken);
		JSONObject expJson = new JSONObject(airlockExperiment);
		Assert.assertFalse(expJson.getJSONArray("controlGroupVariants").contains("controlGroupVariant1"), "Variant1 was not deleted from the list of  controlGroupVariants" );
		
	}
	
	
	private String addExperiment(String experimentName) throws IOException, JSONException{
		return baseUtils.addExperiment(experimentName, m_analyticsUrl, false, false);

	}

	
	
	@AfterTest
	private void reset(){
		baseUtils.reset(productID, sessionToken);
	}
}
