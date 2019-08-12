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
import tests.com.ibm.qautils.RestClientUtils;
import tests.restapi.AirlockUtils;
import tests.restapi.BranchesRestApi;
import tests.restapi.ExperimentsRestApi;
import tests.restapi.SeasonsRestApi;

public class EnableExperimentWithoutVariant {
	protected String productID;
	protected String seasonID;
	private String experimentID;
	private String variantID;
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
	 * Experiment cannot be enabled when no variant exists. 
	 * - add enabled experiment without variant
	 * - add disabled experiment without variant, update to enabled
	 * - add enabled with variant, delete variant 
	 * - add disabled without variant, add variant, update to enabled

		
	 */

	@Test (description ="Add enabled experiment without variant") 
	public void addEnabledExperiment () throws IOException, JSONException, InterruptedException {
	
		String experiment = FileUtils.fileToString(m_configPath + "experiments/experiment1.txt", "UTF-8", false);
		JSONObject expJson = new JSONObject(experiment);
		expJson.put("name", "experiment."+RandomStringUtils.randomAlphabetic(5));
		expJson.put("enabled", true);
		String response = exp.createExperiment(productID, expJson.toString(), sessionToken);
		Assert.assertTrue (response.contains("error"), "Created enabled experiment when no variant exists");
	}
	
	@Test (dependsOnMethods="addEnabledExperiment", description ="Update experiment without variant to enabled") 
	public void updateExperimentNoVariant () throws Exception {
	
		String experiment = FileUtils.fileToString(m_configPath + "experiments/experiment1.txt", "UTF-8", false);
		JSONObject expJson = new JSONObject(experiment);
		expJson.put("name", "experiment."+RandomStringUtils.randomAlphabetic(5));
		expJson.put("enabled", false);
		experimentID = exp.createExperiment(productID, expJson.toString(), sessionToken);
		Assert.assertFalse (experimentID.contains("error"), "Can't create experiment");
		
		experiment = exp.getExperiment(experimentID, sessionToken);
		JSONObject json = new JSONObject(experiment);
		json.put("enabled", true);
		String response = exp.updateExperiment(experimentID, json.toString(), sessionToken);
		Assert.assertTrue (response.contains("error"), "Update experiment without variant to enabled");
	}
	
	@Test (dependsOnMethods="updateExperimentNoVariant", description ="Delete variant from enabled experiment") 
	public void deleteVariantFromEnabledExperiment () throws Exception {

		String branch = FileUtils.fileToString(filePath + "experiments/branch1.txt", "UTF-8", false);
		JSONObject branchJson = new JSONObject(branch);
		branchJson.put("name", "branch1");
		String branchID = br.createBranch(seasonID, branchJson.toString(), BranchesRestApi.MASTER, sessionToken);
	
		String variant = FileUtils.fileToString(filePath + "experiments/variant1.txt", "UTF-8", false);
		JSONObject variantJson = new JSONObject(variant);
		variantJson.put("name", "variant1");
		variantID = exp.createVariant(experimentID, variantJson.toString(), sessionToken);
	
		//enable experiment with variant
		String experiment = exp.getExperiment(experimentID, sessionToken);
		JSONObject json = new JSONObject(experiment);
		json.put("enabled", true);
		String response = exp.updateExperiment(experimentID, json.toString(), sessionToken);
		Assert.assertFalse (response.contains("error"), "Can't update experiment with variant to enabled");

		
		RestClientUtils.RestCallResults res = RestClientUtils.sendDelete(m_analyticsUrl+"/products/experiments/variants/" + variantID, sessionToken);
		Assert.assertTrue(res.message.contains("error"), "Deleted variant from enabled experiment");
	}
	

	@Test (dependsOnMethods="deleteVariantFromEnabledExperiment", description ="Delete variant from disabled experiment") 
	public void deleteVariantFromDisabledExperiment () throws Exception {

		//disable experiment with variant
		String experiment = exp.getExperiment(experimentID, sessionToken);
		JSONObject json = new JSONObject(experiment);
		json.put("enabled", false);
		String response = exp.updateExperiment(experimentID, json.toString(), sessionToken);
		Assert.assertFalse (response.contains("error"), "Can't update experiment to disabled");

		int respCode = exp.deleteVariant(variantID, sessionToken);
		Assert.assertTrue(respCode==200, "Can't delete variant from disabled experiment");
	}

	@AfterTest
	private void reset(){
		baseUtils.reset(productID, sessionToken);
	}
}
