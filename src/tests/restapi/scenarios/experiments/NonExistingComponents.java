package tests.restapi.scenarios.experiments;

import java.io.IOException;

import java.util.UUID;

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

public class NonExistingComponents {
	protected String productID;
	protected String seasonID;
	private String experimentID;
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
Add variant to non-existing experiment
Add branch to non-existing season
Add experiment to non-existing product
Add variant with non-existing branch name

	 */

	@Test (description ="Add variant to non-existing experiment") 
	public void addVariantToNonExistingExperiment () throws IOException, JSONException {
		String variant = FileUtils.fileToString(filePath + "experiments/variant1.txt", "UTF-8", false);
		JSONObject variantJson = new JSONObject(variant);
		variantJson.put("name", "variant1");
		String response =  exp.createVariant("4c66a83c-c01f-4b22-8d48-2da40f17767e", variantJson.toString(), sessionToken);
		Assert.assertTrue(response.contains("error"), "Variant was added to non existing experiment");
	}
	
	
	@Test (description ="Add branch to non-existing season") 
	public void addBranchToNonExistingSeason () throws IOException, JSONException {
		String branch = FileUtils.fileToString(filePath + "experiments/branch1.txt", "UTF-8", false);
		JSONObject branchJson = new JSONObject(branch);
		branchJson.put("name", "branch1");
		String response =  br.createBranch("4c66a83c-c01f-4b22-8d48-2da40f17767e", branchJson.toString(), BranchesRestApi.MASTER, sessionToken);
		Assert.assertTrue(response.contains("error"), "Branch was added to non existing season");
	}
	
	@Test (description ="Add experiment to non-existing product") 
	public void addExperimentToNonExistingProduct () throws IOException, JSONException {
		String experiment = FileUtils.fileToString(filePath + "experiments/experiment1.txt", "UTF-8", false);
		JSONObject expJson = new JSONObject(experiment);
		expJson.put("name", "experiment."+RandomStringUtils.randomAlphabetic(5));
		String response =  exp.createExperiment(UUID.randomUUID().toString(), expJson.toString(), sessionToken);
		Assert.assertTrue(response.contains("error"), "Experiment was added to non existing product");
	}
	
	@Test (description ="Add variant with non-existing branch name") 
	public void addVariant () throws IOException, JSONException {
		experimentID = addExperiment("experiment."+RandomStringUtils.randomAlphabetic(5));
		String variant = FileUtils.fileToString(filePath + "experiments/variant1.txt", "UTF-8", false);
		JSONObject variantJson = new JSONObject(variant);
		variantJson.put("name", "variant2");
		variantJson.put("branchName", "branch123");
		String response =  exp.createVariant(experimentID, variantJson.toString(), sessionToken);
		Assert.assertTrue(response.contains("error"), "Variant was added with non existing branch name");	}
	
	
	private String addExperiment(String experimentName) throws IOException, JSONException{

		return baseUtils.addExperiment(experimentName, m_analyticsUrl, false, false);

	}
	
	
	@AfterTest
	private void reset(){
		baseUtils.reset(productID, sessionToken);
	}
}
