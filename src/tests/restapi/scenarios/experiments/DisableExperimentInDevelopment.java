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
import tests.restapi.RuntimeDateUtilities;
import tests.restapi.RuntimeRestApi;
import tests.restapi.SeasonsRestApi;

public class DisableExperimentInDevelopment {
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
	private String m_configPath;
	
	@BeforeClass
	@Parameters({"url", "analyticsUrl", "translationsUrl", "configPath", "sessionToken", "userName", "userPassword", "appName", "productsToDeleteFile"})
	public void init(String url, String analyticsUrl, String translationsUrl, String configPath, String sToken, String userName, String userPassword, String appName, String productsToDeleteFile) throws Exception{
		m_url = url;
		m_analyticsUrl = analyticsUrl;
		m_configPath = configPath;
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
	 * in dev:
		- create disabled experiment with enabled variant and branch
		- enable experiment
		- disable experiment
		- enable experiment & disable variant
		- enable variant
		
	 */

	@Test (description ="Add components") 
	public void addComponents () throws IOException, JSONException, InterruptedException {
		Thread.sleep(2000);
		String dateFormat = RuntimeDateUtilities.getCurrentTimeStamp();
		Thread.sleep(2000);

		
		String experiment = FileUtils.fileToString(m_configPath + "experiments/experiment1.txt", "UTF-8", false);
		JSONObject expJson = new JSONObject(experiment);
		expJson.put("name", "experiment."+RandomStringUtils.randomAlphabetic(5));
		expJson.put("enabled", false);
		experimentID = exp.createExperiment(productID, expJson.toString(), sessionToken);

		String branch = FileUtils.fileToString(filePath + "experiments/branch1.txt", "UTF-8", false);
		JSONObject branchJson = new JSONObject(branch);
		branchJson.put("name", "branch1");
		branchID = br.createBranch(seasonID, branchJson.toString(), BranchesRestApi.MASTER, sessionToken);

		String variant = FileUtils.fileToString(filePath + "experiments/variant1.txt", "UTF-8", false);
		JSONObject variantJson = new JSONObject(variant);
		variantJson.put("name", "variant1");
		variantID = exp.createVariant(experimentID, variantJson.toString(), sessionToken);
		
		//check if files were changed
		Thread.sleep(3000);
		RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getDevelopmentFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseDev.code ==200, "Runtime development feature file was not updated");
		Assert.assertTrue(getExperiments(responseDev.message)==0, "Disabled experiment is written in runtime");
		Assert.assertTrue(getBranches(responseDev.message)==0, "Disabled branch is written in runtime");
		
		RuntimeRestApi.DateModificationResults responseProd = RuntimeDateUtilities.getProductionFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseProd.code ==304, "Runtime production feature file was changed");
		RuntimeRestApi.DateModificationResults prodChanged = RuntimeDateUtilities.getProductionChangedDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(prodChanged.code ==304, "productionChanged.txt file was changed");

	}
	
	@Test (dependsOnMethods="addComponents", description ="Enable experiment") 
	public void enableExperiment () throws Exception {
		String dateFormat = RuntimeDateUtilities.getCurrentTimeStamp();
		Thread.sleep(2000);

		JSONObject experiment = new JSONObject(exp.getExperiment(experimentID, sessionToken));
		experiment.put("enabled", true);
		String response = exp.updateExperiment(experimentID, experiment.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Experiment was not updated");

		//check if files were changed
		Thread.sleep(3000);
		RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getDevelopmentFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseDev.code ==200, "Runtime development feature file was not updated");
		Assert.assertTrue(getExperiments(responseDev.message)==1, "Enabled experiment is not written in runtime");
		Assert.assertTrue(getBranches(responseDev.message)==1, "Enabled branch is not written in runtime");
		
		RuntimeRestApi.DateModificationResults responseProd = RuntimeDateUtilities.getProductionFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseProd.code ==304, "Runtime production feature file was changed");
		RuntimeRestApi.DateModificationResults prodChanged = RuntimeDateUtilities.getProductionChangedDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(prodChanged.code ==304, "productionChanged.txt file was changed");

	}
	
	@Test (dependsOnMethods="enableExperiment", description ="Disable experiment") 
	public void disableExperiment () throws Exception {
		String dateFormat = RuntimeDateUtilities.getCurrentTimeStamp();
		Thread.sleep(2000);

		JSONObject experiment = new JSONObject(exp.getExperiment(experimentID, sessionToken));
		experiment.put("enabled", false);
		String response = exp.updateExperiment(experimentID, experiment.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Experiment was not updated");

		//check if files were changed
		Thread.sleep(3000);
		RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getDevelopmentFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseDev.code ==200, "Runtime development feature file was not updated");
		Assert.assertTrue(getExperiments(responseDev.message)==0, "Disabled experiment is  written in runtime");
		Assert.assertTrue(getBranches(responseDev.message)==0, "Disabled branch is written in runtime");
		
		RuntimeRestApi.DateModificationResults responseProd = RuntimeDateUtilities.getProductionFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseProd.code ==304, "Runtime production feature file was changed");
		RuntimeRestApi.DateModificationResults prodChanged = RuntimeDateUtilities.getProductionChangedDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(prodChanged.code ==304, "productionChanged.txt file was changed");

	}
	
	@Test (dependsOnMethods="disableExperiment", description ="Disable variant") 
	public void disableVariant () throws Exception {
		String dateFormat = RuntimeDateUtilities.getCurrentTimeStamp();
		Thread.sleep(2000);

		JSONObject experiment = new JSONObject(exp.getExperiment(experimentID, sessionToken));
		experiment.put("enabled", true);
		String response = exp.updateExperiment(experimentID, experiment.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Experiment was not updated");

		JSONObject variant = new JSONObject(exp.getVariant(variantID, sessionToken));
		variant.put("enabled", false);
		response = exp.updateVariant(variantID, variant.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Variant was not updated");

		//check if files were changed
		Thread.sleep(3000);
		RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getDevelopmentFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseDev.code ==200, "Runtime development feature file was not updated");
		Assert.assertTrue(getExperiments(responseDev.message)==1, "Enabled experiment is not written in runtime");
		Assert.assertTrue(getVariants(responseDev.message)==0, "Disabled variant is  written in runtime");
		Assert.assertTrue(getBranches(responseDev.message)==0, "Disabled branch is written in runtime");
		
		RuntimeRestApi.DateModificationResults responseProd = RuntimeDateUtilities.getProductionFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseProd.code ==304, "Runtime production feature file was changed");
		RuntimeRestApi.DateModificationResults prodChanged = RuntimeDateUtilities.getProductionChangedDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(prodChanged.code ==304, "productionChanged.txt file was changed");

	}
	
	@Test (dependsOnMethods="disableVariant", description ="Enable variant") 
	public void enableVariant () throws Exception {
		String dateFormat = RuntimeDateUtilities.getCurrentTimeStamp();
		Thread.sleep(2000);


		JSONObject variant = new JSONObject(exp.getVariant(variantID, sessionToken));
		variant.put("enabled", true);
		String response = exp.updateVariant(variantID, variant.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Variant was not updated");

		//check if files were changed
		Thread.sleep(3000);
		RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getDevelopmentFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseDev.code ==200, "Runtime development feature file was not updated");
		Assert.assertTrue(getExperiments(responseDev.message)==1, "Enabled experiment is not written in runtime");
		Assert.assertTrue(getVariants(responseDev.message)==1, "Enabled variant is not written in runtime");
		Assert.assertTrue(getBranches(responseDev.message)==1, "Enabled branch is not written in runtime");
		
		RuntimeRestApi.DateModificationResults responseProd = RuntimeDateUtilities.getProductionFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseProd.code ==304, "Runtime production feature file was changed");
		RuntimeRestApi.DateModificationResults prodChanged = RuntimeDateUtilities.getProductionChangedDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(prodChanged.code ==304, "productionChanged.txt file was changed");

	}
	
	private int getBranches(String input) throws JSONException{
		JSONObject runtime = new JSONObject(input);
		return runtime.getJSONArray("branches").size();
	}
	
	private int getExperiments(String input) throws JSONException{
		JSONObject runtime = new JSONObject(input);
		return runtime.getJSONObject("experiments").getJSONArray("experiments").size();
	}
	private int getVariants(String input) throws JSONException{
		JSONObject runtime = new JSONObject(input);
		return runtime.getJSONObject("experiments").getJSONArray("experiments").getJSONObject(0).getJSONArray("variants").size();
	}


	@AfterTest
	private void reset(){
		baseUtils.reset(productID, sessionToken);
	}
}
