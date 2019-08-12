package tests.restapi.scenarios.experiments;

import java.io.IOException;


import org.apache.commons.lang3.RandomStringUtils;
import org.apache.wink.json4j.JSONArray;
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


public class ChangeExperimentsOrder {
	protected String productID;
	protected String seasonID;
	private String experimentID1;
	private String experimentID2;
	private String variantID1;
	private String variantID2;
	protected String filePath;

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
		br = new BranchesRestApi();
		br.setURL(m_url);

		baseUtils = new AirlockUtils(m_url, analyticsUrl, translationsUrl, configPath, sToken, userName, userPassword, appName, productsToDeleteFile);
		sessionToken = baseUtils.sessionToken;

		productID = baseUtils.createProduct();
		baseUtils.printProductToFile(productID);
		seasonID = baseUtils.createSeason(productID);
		
		exp = new ExperimentsRestApi();
		exp.setURL(analyticsUrl); 

	}
	
	/*
	 * update maxExperimentsOn
	Change experiments order in product
	When changing order :
		- can update experiment
		- can update variant
	No new experiment during update
	No missing experiment during update	
	 */

	@Test (description ="Add components") 
	public void addComponents () throws IOException, JSONException {
		addBranch("branch1");

		experimentID1 = addExperiment("experiment."+RandomStringUtils.randomAlphabetic(5));
		Assert.assertFalse(experimentID1.contains("error"), "Experiment was not created: " + experimentID1);
		variantID1 = addVariant("variant1", experimentID1);
		Assert.assertFalse(variantID1.contains("error"), "Variant1 was not created: " + variantID1);
		
		experimentID2 = addExperiment("experiment."+RandomStringUtils.randomAlphabetic(5));
		Assert.assertFalse(experimentID2.contains("error"), "Experiment was not created: " + experimentID2);
		variantID2 = addVariant("variant2", experimentID2);
		Assert.assertFalse(variantID2.contains("error"), "Variant2 was not created: " + variantID2);

	}
	
	@Test (dependsOnMethods = "addComponents", description ="Update maxExperimentsOn") 
	public void updateMaxExperimentsOn() throws Exception {
		JSONObject experiments = new JSONObject(exp.getAllExperiments(productID, sessionToken));
		experiments.put("maxExperimentsOn", 3);
		String response = exp.updateExperiments(productID, experiments.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Product was not updated: " + response);
		
		experiments = new JSONObject(exp.getAllExperiments(productID, sessionToken));
		Assert.assertTrue(experiments.getInt("maxExperimentsOn")==3, "Incorrect maxExperimentsOn " );
	}
	
	@Test (dependsOnMethods = "updateMaxExperimentsOn", description ="Change experiments order") 
	public void changeExperimentsOrder() throws Exception {
		JSONObject experiments = new JSONObject(exp.getAllExperiments(productID, sessionToken));
		JSONObject exp1 = new JSONObject(exp.getExperiment(experimentID1, sessionToken));
		JSONObject exp2 = new JSONObject(exp.getExperiment(experimentID2, sessionToken));
		JSONArray children = new JSONArray();
		children.add(exp2);
		children.add(exp1);
		experiments.put("experiments", children);
		
		String response = exp.updateExperiments(productID, experiments.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Product was not updated: " + response);
		
		experiments = new JSONObject(exp.getAllExperiments(productID, sessionToken));
		Assert.assertTrue(experiments.getJSONArray("experiments").getJSONObject(0).getString("uniqueId").equals(exp2.getString("uniqueId")), "Incorrect first child " );
		Assert.assertTrue(experiments.getJSONArray("experiments").getJSONObject(1).getString("uniqueId").equals(exp1.getString("uniqueId")), "Incorrect second child " );

	}
	
	@Test (dependsOnMethods = "changeExperimentsOrder", description ="Missing experiment") 
	public void missingExperiment() throws Exception {
		JSONObject experiments = new JSONObject(exp.getAllExperiments(productID, sessionToken));
		JSONObject exp1 = new JSONObject(exp.getExperiment(experimentID1, sessionToken));
		JSONArray children = new JSONArray();
		children.add(exp1);
		experiments.put("experiments", children);
		
		String response = exp.updateExperiments(productID, experiments.toString(), sessionToken);
		Assert.assertTrue(response.contains("error"), "Updated with missing experiment");

	}
	
	@Test (dependsOnMethods = "missingExperiment", description ="Change experiments order with updating experiment and variant") 
	public void changeOrderAndUpdateExperiment() throws Exception {
		JSONObject experiments = new JSONObject(exp.getAllExperiments(productID, sessionToken));
		JSONObject exp1 = new JSONObject(exp.getExperiment(experimentID1, sessionToken));
		exp1.put("name", "new experiment1");
		exp1.getJSONArray("variants").getJSONObject(0).put("name", "new variant1");
		JSONObject exp2 = new JSONObject(exp.getExperiment(experimentID2, sessionToken));
		exp2.put("name", "new experiment2");
		exp2.getJSONArray("variants").getJSONObject(0).put("name", "new variant2");
		JSONArray children = new JSONArray();
		children.add(exp1);
		children.add(exp2);
		experiments.put("experiments", children);
		
		String response = exp.updateExperiments(productID, experiments.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Can't change experiments order: " + response);
		
		experiments = new JSONObject(exp.getAllExperiments(productID, sessionToken));
		Assert.assertTrue(experiments.getJSONArray("experiments").getJSONObject(0).getString("uniqueId").equals(exp1.getString("uniqueId")), "Incorrect first child " );
		Assert.assertTrue(experiments.getJSONArray("experiments").getJSONObject(1).getString("uniqueId").equals(exp2.getString("uniqueId")), "Incorrect second child " );

		exp1 = new JSONObject(exp.getExperiment(experimentID1, sessionToken));
		exp2 = new JSONObject(exp.getExperiment(experimentID2, sessionToken));
		Assert.assertTrue(exp1.getString("name").equals("new experiment1"), "Experiment1 name was not updated");
		Assert.assertTrue(exp2.getString("name").equals("new experiment2"), "Experiment2 name was not updated");
		Assert.assertTrue(exp1.getJSONArray("variants").getJSONObject(0).getString("name").equals("new variant1"), "Variant1 name was not updated");
		Assert.assertTrue(exp2.getJSONArray("variants").getJSONObject(0).getString("name").equals("new variant2"), "Variant2 name was not updated");
	}
	
	private String addExperiment(String experimentName) throws IOException, JSONException{

		return baseUtils.addExperiment(experimentName, m_analyticsUrl, false, false);
	}
	
	private String addVariant(String variantName, String expId) throws IOException, JSONException{
		String variant = FileUtils.fileToString(filePath + "experiments/variant1.txt", "UTF-8", false);
		JSONObject variantJson = new JSONObject(variant);
		variantJson.put("name", variantName);
		return exp.createVariant(expId, variantJson.toString(), sessionToken);

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
