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
import tests.restapi.SeasonsRestApi;

public class ExperimentAndVariantStage {
	protected String productID;
	protected String seasonID;
	private String experimentID;
	private String variantID;
	private String branchID;
	protected String filePath;
	protected SeasonsRestApi s;
	protected String m_url;
	private String sessionToken = "";
	private AirlockUtils baseUtils;
	private ExperimentsRestApi exp ;
	private BranchesRestApi br ;
	private String m_analyticsUrl;
	
	@BeforeClass
	@Parameters({"url", "analyticsUrl", "translationsUrl", "configPath", "sessionToken", "userName", "userPassword", "appName", "productsToDeleteFile"})
	public void init(String url, String analyticsUrl, String translationsUrl, String configPath, String sToken, String userName, String userPassword, String appName, String productsToDeleteFile) throws Exception{
		m_url = url;
		m_analyticsUrl = analyticsUrl;
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
Move experiment to dev/to prod
Move variant to dev/to prod
Experiment in prod/variant in dev
Experiment in dev/variant in prod - not allowed
	 */

	@Test (description ="Add components") 
	public void addComponents () throws IOException, JSONException {
		experimentID = addExperiment("experiment."+RandomStringUtils.randomAlphabetic(5));
		Assert.assertFalse(experimentID.contains("error"), "Experiment was not created: " + experimentID);
		
		branchID = addBranch("branch1");
		Assert.assertFalse(branchID.contains("error"), "Experiment was not created: " + branchID);

		variantID = addVariant("variant1");
		Assert.assertFalse(variantID.contains("error"), "Variant was not created: " + variantID);

	}
	
	
	@Test (dependsOnMethods="addComponents", description ="Update experiment to production") 
	public void updateExperimentStage() throws Exception{
		//experiment in production, variant in development
		String experiment = exp.getExperiment(experimentID, sessionToken);
		JSONObject json = new JSONObject(experiment);
		json.put("stage", "PRODUCTION");
		String response = exp.updateExperiment(experimentID, json.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Can't update experiment stage: " + response);
		
		//update experiment back to development
		experiment = exp.getExperiment(experimentID, sessionToken);
		json = new JSONObject(experiment);
		json.put("stage", "DEVELOPMENT");
		response = exp.updateExperiment(experimentID, json.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Can't update experiment stage: " + response);

	}
	
	@Test (dependsOnMethods="updateExperimentStage", description ="Update variant to production with experiment in development - not allowed") 
	public void updateVariantToProdExperimentInDev() throws Exception{
		//experiment in development, variant in production
		String variant = exp.getVariant(variantID, sessionToken);
		JSONObject json = new JSONObject(variant);
		json.put("stage", "PRODUCTION");
		String response = exp.updateVariant(variantID, json.toString(), sessionToken);
		Assert.assertTrue(response.contains("error"), "Variant was moved to production when experiment is in development");
	}
	
	@Test (dependsOnMethods="updateVariantToProdExperimentInDev", description ="Update variant to production") 
	public void updateVariantToProd() throws Exception{
		//move experiment to production
		String experiment = exp.getExperiment(experimentID, sessionToken);
		JSONObject json = new JSONObject(experiment);
		json.put("stage", "PRODUCTION");
		String response = exp.updateExperiment(experimentID, json.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Can't update experiment stage: " + response);

		
		//experiment in production, variant in production
		String variant = exp.getVariant(variantID, sessionToken);
		json = new JSONObject(variant);
		json.put("stage", "PRODUCTION");
		response = exp.updateVariant(variantID, json.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Variant was not moved to production: " + response);
		
	
	}
	
	@Test (dependsOnMethods="updateVariantToProd", description ="Move experiment and variant to developemnt and delete them to continue workflow") 
	public void deleteComponents() throws Exception{
		//update variant back to development
		String variant = exp.getVariant(variantID, sessionToken);
		JSONObject json = new JSONObject(variant);
		json.put("stage", "DEVELOPMENT");
		String response = exp.updateVariant(variantID, json.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Can't update variant stage to dev: " + response);

		//move experiment to development
		String experiment = exp.getExperiment(experimentID, sessionToken);
		json = new JSONObject(experiment);
		json.put("stage", "DEVELOPMENT");
		response = exp.updateExperiment(experimentID, json.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Can't update experiment stage to dev: " + response);
		
		exp.deleteVariant(variantID, sessionToken);
		exp.deleteExperiment(experimentID, sessionToken);

	}
	
	@Test (dependsOnMethods="deleteComponents", description ="Add experiment and variant in production") 
	public void addComponentsInProduction () throws Exception {

		String experimentID = baseUtils.addExperiment(m_analyticsUrl, false, false);
		Assert.assertFalse(experimentID.contains("error"), "Can't add experiment in dev stage: " + experimentID);

		//when experiment is in development, can't add variant in production
		String variant = FileUtils.fileToString(filePath + "experiments/variant1.txt", "UTF-8", false);
		JSONObject variantJson = new JSONObject(variant);
		variantJson.put("name", "variant1");
		variantJson.put("stage", "PRODUCTION");
		String response =  exp.createVariant(experimentID, variantJson.toString(), sessionToken);
		Assert.assertTrue(response.contains("error"), "Add variant in production to experiment in dev stage");
		
		
		//move experiment to production
		String experiment = exp.getExperiment(experimentID, sessionToken);
		JSONObject expJson = new JSONObject(experiment);
		expJson.put("stage", "PRODUCTION");
		response = exp.updateExperiment(experimentID, expJson.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Can't update experiment stage: " + response);

		//add variant in production to experiment in production
		response =  exp.createVariant(experimentID, variantJson.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Add variant in production to experiment in production failed: " + response);
		
		//enable experiment
		String airlockExperiment = exp.getExperiment(experimentID, sessionToken);
		Assert.assertFalse(airlockExperiment.contains("error"), "Experiment was not found: " + experimentID);

		expJson = new JSONObject(airlockExperiment);
		expJson.put("enabled", true);
		
		response = exp.updateExperiment(experimentID, expJson.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Experiment was not updated: " + response);		


	}

	private String addExperiment(String experimentName) throws IOException, JSONException{
		return baseUtils.addExperiment(experimentName, m_analyticsUrl, false, false);

	}
	

	private String addVariant(String variantName) throws IOException, JSONException{
		String variant = FileUtils.fileToString(filePath + "experiments/variant1.txt", "UTF-8", false);
		JSONObject variantJson = new JSONObject(variant);
		variantJson.put("name", variantName);
		return exp.createVariant(experimentID, variantJson.toString(), sessionToken);

	}
	
	private String addBranch(String branchName) throws JSONException, IOException{
		String branch = FileUtils.fileToString(filePath + "experiments/branch1.txt", "UTF-8", false);
		JSONObject branchJson = new JSONObject(branch);
		branchJson.put("name", branchName);
		return br.createBranch(seasonID, branchJson.toString(), BranchesRestApi.MASTER, sessionToken);

	}
	
	
	@AfterTest
	private void reset(){
		baseUtils.reset(productID, sessionToken);
	}
}
