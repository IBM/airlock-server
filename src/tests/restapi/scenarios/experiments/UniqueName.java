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

public class UniqueName {
	protected String productID;
	private String productID2;
	protected String seasonID;
	private String experimentID;
	private String variantID;
	private String branchID;
	protected String filePath;
	protected ProductsRestApi p;
	protected SeasonsRestApi s;
	protected String m_url;
	private String sessionToken = "";
	private AirlockUtils baseUtils;
	private ExperimentsRestApi exp ;
	private BranchesRestApi br ;

	
	@BeforeClass
	@Parameters({"url", "analyticsUrl", "translationsUrl", "configPath", "sessionToken", "userName", "userPassword", "appName", "productsToDeleteFile"})
	public void init(String url, String analyticsUrl, String translationsUrl, String configPath, String sToken, String userName, String userPassword, String appName, String productsToDeleteFile) throws Exception{
		m_url = url;
		filePath = configPath ;

		s = new SeasonsRestApi();
		s.setURL(m_url);
		exp = new ExperimentsRestApi();
		exp.setURL(analyticsUrl); 
		br = new BranchesRestApi();
		br.setURL(m_url);

		baseUtils = new AirlockUtils(m_url, analyticsUrl, translationsUrl, configPath, sToken, userName, userPassword, appName, productsToDeleteFile);
		sessionToken = baseUtils.sessionToken;
		productID = baseUtils.createProduct();
		baseUtils.printProductToFile(productID);
		seasonID = baseUtils.createSeason(productID);
		

	}
	/*
	 *  
		Variant name unique in experiment
		Branch name unique in season, not in product
		The same name for experiment+branch+variant
	 */

	/*
	@Test (description ="create experiment") 
	public void uniqueExperimentName () throws IOException, JSONException {
		String experiment = FileUtils.fileToString(filePath + "experiments/experiment1.txt", "UTF-8", false);
		JSONObject expJson = new JSONObject(experiment);
		String experimentName = "experiment."+RandomStringUtils.randomAlphabetic(5);
		expJson.put("name", experimentName);
		experimentID = exp.createExperiment(productID, expJson.toString(), sessionToken);
		Assert.assertFalse(experimentID.contains("error"), "Experiment was not created: " + experimentID);
		
		String response =  exp.createExperiment(productID, expJson.toString(), sessionToken);
		Assert.assertTrue(response.contains("error"), "Experiment was created with exsiting name");
		
		//Create second product. Experiment name must be unique across all products
		String product = FileUtils.fileToString(filePath + "product2.txt", "UTF-8", false);
		productID2 = baseUtils.createProduct();
		baseUtils.printProductToFile(productID2);
		String experimentID2 = exp.createExperiment(productID2, expJson.toString(), sessionToken);
		Assert.assertTrue(experimentID2.contains("error"), "Experiment with existing name was created in another product: " + experimentID2);

	}
	*/
	@Test ( description ="create branch") 
	public void uniqueBranchName () throws IOException, JSONException {
		String branch = FileUtils.fileToString(filePath + "experiments/branch1.txt", "UTF-8", false);
		JSONObject branchJson = new JSONObject(branch);
		branchJson.put("name", "branch1");
		branchID = br.createBranch(seasonID, branchJson.toString(), BranchesRestApi.MASTER, sessionToken);
		Assert.assertFalse(branchID.contains("error"), "Experiment was not created: " + branchID);
		
		String response =  br.createBranch(seasonID, branchJson.toString(), BranchesRestApi.MASTER, sessionToken);
		Assert.assertTrue(response.contains("error"), "Branch was created with exsiting name");

	}
	
	@Test (dependsOnMethods="uniqueBranchName", description ="create variant") 
	public void uniqueVariantName () throws IOException, JSONException {
		String experiment = FileUtils.fileToString(filePath + "experiments/experiment1.txt", "UTF-8", false);
		JSONObject expJson = new JSONObject(experiment);
		String experimentName = "experiment."+RandomStringUtils.randomAlphabetic(5);
		expJson.put("name", experimentName);
		expJson.put("enabled", false);
		experimentID = exp.createExperiment(productID, expJson.toString(), sessionToken);
		Assert.assertFalse(experimentID.contains("error"), "Experiment was not created: " + experimentID);

		String variant = FileUtils.fileToString(filePath + "experiments/variant1.txt", "UTF-8", false);
		JSONObject variantJson = new JSONObject(variant);
		variantJson.put("name", "variant1");
		variantID = exp.createVariant(experimentID, variantJson.toString(), sessionToken);
		Assert.assertFalse(variantID.contains("error"), "Variant was not created: " + variantID);
		
		String response =  exp.createVariant(experimentID, variantJson.toString(), sessionToken);
		Assert.assertTrue(response.contains("error"), "Variant was created with exsiting name");

	}
	
	@Test (dependsOnMethods="uniqueVariantName", description ="create variant using branch/experiment name") 
	public void variantName () throws IOException, JSONException {
		String variant = FileUtils.fileToString(filePath + "experiments/variant1.txt", "UTF-8", false);
		JSONObject variantJson = new JSONObject(variant);
		
		variantJson.put("name", "experiment1");
		String response = exp.createVariant(experimentID, variantJson.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Variant was not created with exsiting experiment name");
		
		variantJson.put("name", "branch1");
		response = exp.createVariant(experimentID, variantJson.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Variant was not created with exsiting branch name");
	}
	
	@Test (dependsOnMethods="variantName", description ="create experiment using branch/variant name") 
	public void experimentName () throws IOException, JSONException {
		String experiment = FileUtils.fileToString(filePath + "experiments/experiment1.txt", "UTF-8", false);		
		JSONObject expJson = new JSONObject(experiment);
		expJson.put("name", "branch1");
		expJson.put("enabled", false);
		String response = exp.createExperiment(productID, expJson.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Experiment not was created with exsiting branch name");
		
		expJson.put("name", "variant1");
		response = exp.createExperiment(productID, expJson.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Experiment not was created with exsiting variant name");
	}
	
	@Test (dependsOnMethods="experimentName", description ="create branch using experiment/variant name") 
	public void branchName () throws IOException, JSONException {
		String branch = FileUtils.fileToString(filePath + "experiments/branch1.txt", "UTF-8", false);
		JSONObject branchJson = new JSONObject(branch);
		branchJson.put("name", "variant1");
		String response = br.createBranch(seasonID, branchJson.toString(), BranchesRestApi.MASTER, sessionToken);
		Assert.assertFalse(response.contains("error"), "Branch not was created with exsiting variant name");
		
		branchJson.put("name", "experiment1");
		response = br.createBranch(seasonID, branchJson.toString(), BranchesRestApi.MASTER, sessionToken);
		Assert.assertFalse(response.contains("error"), "Branch not was created with exsiting experiment name");
	}
	
	@AfterTest
	private void reset(){
		baseUtils.reset(productID, sessionToken);
		baseUtils.reset(productID2, sessionToken);
	}
}
