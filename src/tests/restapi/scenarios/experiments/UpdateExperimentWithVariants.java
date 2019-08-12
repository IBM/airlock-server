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
import tests.restapi.SeasonsRestApi;

public class UpdateExperimentWithVariants {
	protected String productID;
	protected String seasonID;
	private String experimentID;
	private String variantID1;
	private String variantID2;
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
		filePath = configPath ;
		m_analyticsUrl = analyticsUrl;
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
Change variants order in experiment
Can change variant order when experiment in production
When updating experiment:
	- no new variants
	- all variants must be included
Variant can be updated during experiment update	
	 */

	@Test (description ="Add components") 
	public void addComponents () throws IOException, JSONException {
		experimentID = addExperiment("experiment."+RandomStringUtils.randomAlphabetic(5));
		Assert.assertFalse(experimentID.contains("error"), "Experiment was not created: " + experimentID);

		branchID = addBranch("branch1");
		Assert.assertFalse(branchID.contains("error"), "Experiment was not created: " + branchID);

		variantID1 = addVariant("variant1");
		Assert.assertFalse(variantID1.contains("error"), "Variant1 was not created: " + variantID1);

		variantID2 = addVariant("variant2");
		Assert.assertFalse(variantID2.contains("error"), "Variant2 was not created: " + variantID2);

	}
	

	@Test (dependsOnMethods="addComponents", description ="Update variants order") 
	public void changeVariantsOrder () throws Exception {
		//check variants order after create experiment
		String experiment = exp.getExperiment(experimentID, sessionToken);
		JSONObject json = new JSONObject(experiment);
		
		Assert.assertTrue(json.getJSONArray("variants").getJSONObject(0).getString("uniqueId").equals(variantID1), "The first variant is not variantID1");
		Assert.assertTrue(json.getJSONArray("variants").getJSONObject(1).getString("uniqueId").equals(variantID2), "The second variant is not variantID2");
		
		//change variants order
		JSONArray variants = new JSONArray();
		JSONObject variant1 = new JSONObject(exp.getVariant(variantID1, sessionToken));
		JSONObject variant2 = new JSONObject(exp.getVariant(variantID2, sessionToken));
		variants.add(variant2);
		variants.add(variant1);
		json.put("variants", variants);
		String response = exp.updateExperiment(experimentID, json.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Experiment was not updated: " + response);
		
		//check variants order after update
		experiment = exp.getExperiment(experimentID, sessionToken);
		json = new JSONObject(experiment);
		Assert.assertTrue(json.getJSONArray("variants").getJSONObject(0).getString("uniqueId").equals(variantID2), "Variants order was not updated: The first variant is not variantID2");
		Assert.assertTrue(json.getJSONArray("variants").getJSONObject(1).getString("uniqueId").equals(variantID1), "Variants order was not updated: The second variant is not variantID1");
		
		
	}
	
	@Test (dependsOnMethods="changeVariantsOrder", description ="Can't change variant order when experiment in production") 
	public void changeVariantsOrderWhenExperimentInProduction () throws Exception {
		//update experiment to production
		String experiment = exp.getExperiment(experimentID, sessionToken);
		JSONObject json = new JSONObject(experiment);
		json.put("stage", "PRODUCTION");
		String response = exp.updateExperiment(experimentID, json.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Experiment stage was not updated to production");
		
		
		//change variants order
		experiment = exp.getExperiment(experimentID, sessionToken);
		json = new JSONObject(experiment);
		JSONObject variant1 = new JSONObject(exp.getVariant(variantID1, sessionToken));
		JSONObject variant2 = new JSONObject(exp.getVariant(variantID2, sessionToken));
		JSONArray variants = new JSONArray();
		variants.add(variant1);
		variants.add(variant2);
		json.put("variants", variants);
		response = exp.updateExperiment(experimentID, json.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Variants order was changed for experiment in production");
		
		//change experiment to development
		experiment = exp.getExperiment(experimentID, sessionToken);
		json = new JSONObject(experiment);
		json.put("stage", "DEVELOPMENT");
		response = exp.updateExperiment(experimentID, json.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Experiment stage was not updated to development");

	}
	
	@Test (dependsOnMethods="changeVariantsOrderWhenExperimentInProduction", description ="No new variants in experiment") 
	public void newVariantInExperimentUpdate () throws Exception {
		String variant = FileUtils.fileToString(filePath + "experiments/variant1.txt", "UTF-8", false);
		JSONObject variantJson = new JSONObject(variant);
		variantJson.put("name", "variant3");
		
		String experiment = exp.getExperiment(experimentID, sessionToken);
		JSONObject json = new JSONObject(experiment);
		json.getJSONArray("variants").add(variantJson);

		String response = exp.updateExperiment(experimentID, json.toString(), sessionToken);
		Assert.assertTrue(response.contains("error"), "Experiment updated with new variant in variants array");

	}
	
	@Test (dependsOnMethods="newVariantInExperimentUpdate", description ="No missing variants in update experiment") 
	public void missingVariantInExperimentUpdate () throws Exception {
		JSONObject variant2 = new JSONObject( exp.getVariant(variantID2, sessionToken));
		
		String experiment = exp.getExperiment(experimentID, sessionToken);
		JSONObject json = new JSONObject(experiment);
		json.put("description", "stam");
		JSONArray variants = new JSONArray();
		variants.add(variant2);
		json.put("variants", variants);
		String response = exp.updateExperiment(experimentID, json.toString(), sessionToken);
		Assert.assertTrue(response.contains("error"), "Experiment updated with one missing variant in variants array");

	}
	
	@Test (dependsOnMethods="missingVariantInExperimentUpdate", description ="Updated variant in update experiment") 
	public void updateVariantInExperimentUpdate () throws Exception {
		String variant1 = exp.getVariant(variantID1, sessionToken);
		JSONObject varJson1 = new JSONObject(variant1);
		varJson1.put("description", "variant111");
		
		String variant2 = exp.getVariant(variantID2, sessionToken);
		JSONObject varJson2 = new JSONObject(variant2);
		varJson2.put("description", "variant222");

		
		String experiment = exp.getExperiment(experimentID, sessionToken);
		JSONObject json = new JSONObject(experiment);
		json.put("description", "stam");
		JSONArray variants = new JSONArray();
		variants.add(varJson1);
		variants.add(varJson2);
		json.put("variants", variants);
		String response = exp.updateExperiment(experimentID, json.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Experiment was not updated: " + response);
		
		//variants names had to be updated
		variant1 = exp.getVariant(variantID1, sessionToken);
		varJson1 = new JSONObject(variant1);
		Assert.assertTrue(varJson1.getString("description").equals("variant111"), "Variant1 was not updated");
		
		variant2 = exp.getVariant(variantID2, sessionToken);
		varJson2 = new JSONObject(variant2);
		Assert.assertTrue(varJson2.getString("description").equals("variant222"), "Variant2 was not updated");


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
