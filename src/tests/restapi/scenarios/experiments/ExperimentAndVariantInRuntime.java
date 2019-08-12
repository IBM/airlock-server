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
import tests.restapi.FeaturesRestApi;
import tests.restapi.RuntimeDateUtilities;
import tests.restapi.RuntimeRestApi;
import tests.restapi.SeasonsRestApi;

public class ExperimentAndVariantInRuntime {
	protected String productID;
	protected String seasonID;
	private String branchID1;
	private String branchID2;
	private String featureID1;
	private String featureID2;
	private String experimentID;
	private String variantID1;
	private String variantID2;
	protected String filePath;
	protected SeasonsRestApi s;
	protected String m_url;
	private String sessionToken = "";
	private AirlockUtils baseUtils;
	private BranchesRestApi br ;
	private FeaturesRestApi f;
	private ExperimentsRestApi exp ;
	private String m_analyticsUrl;
	
	@BeforeClass
	@Parameters({"url", "analyticsUrl", "translationsUrl", "configPath", "sessionToken", "userName", "userPassword", "appName", "productsToDeleteFile"})
	public void init(String url, String analyticsUrl, String translationsUrl, String configPath, String sToken, String userName, String userPassword, String appName, String productsToDeleteFile) throws Exception{
		m_url = url;
		m_analyticsUrl = analyticsUrl;
		filePath = configPath ;
		s = new SeasonsRestApi();
		s.setURL(m_url);
		f = new FeaturesRestApi();
		f.setURL(m_url);
		br = new BranchesRestApi();
		br.setURL(m_url);
		exp = new ExperimentsRestApi();
		exp.setURL(analyticsUrl); 


		baseUtils = new AirlockUtils(m_url, analyticsUrl, translationsUrl, configPath, sToken, userName, userPassword, appName, productsToDeleteFile);
		sessionToken = baseUtils.sessionToken;
		productID = baseUtils.createProduct();
		baseUtils.printProductToFile(productID);
		seasonID = baseUtils.createSeason(productID);

	}
	
/*
 * - add 2 branches & 2 variants
 * - move experiment to production
 * - move variant to production
 * - update variant name
 * - update experiment name
 * - move variant to development
 * - move experiment to development
 * - checkout feature to branch1 & branch 2
 * - delete variant
 * - delete experiment
 * 
 * - delete experiment with variant and branch, make sure that all components were deleted
 */
	@Test (description ="Add components") 
	public void addComponents () throws Exception {
		String dateFormat =   f.setDateFormat();
		
		experimentID = addExperiment("experiment."+RandomStringUtils.randomAlphabetic(5));
		Assert.assertFalse(experimentID.contains("error"), "Experiment was not created: " + experimentID);
		
		branchID1 = addBranch("branch1");
		Assert.assertFalse(branchID1.contains("error"), "Branch1 was not created: " + branchID1);

		branchID2 = addBranch("branch2");
		Assert.assertFalse(branchID2.contains("error"), "Branch2 was not created: " + branchID2);

		variantID1 = addVariant("variant1", "branch1");
		Assert.assertFalse(variantID1.contains("error"), "Variant1 was not created: " + variantID1);

		variantID2 = addVariant("variant2", "branch2");
		Assert.assertFalse(variantID2.contains("error"), "Variant2 was not created: " + variantID2);

		//enable experiment
		String airlockExperiment = exp.getExperiment(experimentID, sessionToken);
		Assert.assertFalse(airlockExperiment.contains("error"), "Experiment was not found: " + experimentID);

		JSONObject expJson = new JSONObject(airlockExperiment);
		expJson.put("enabled", true);
		
		String response = exp.updateExperiment(experimentID, expJson.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Experiment was not updated: " + response);		

		f.setSleep();
		RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getDevelopmentFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);		
		Assert.assertTrue(responseDev.code==200, "Runtime development file was not changed");
		Assert.assertTrue(getBranchesInRuntime(responseDev.message).size()==2, "Incorrect number of branches in runtime development file");
		Assert.assertTrue(getExperimentsInRuntime(responseDev.message).size()==1, "Incorrect number of experiments in runtime development file");
		Assert.assertTrue(getVariantsInRuntime(responseDev.message).size()==2, "Incorrect number of variants in runtime development file");
		
		//experiment name is added to variant object
		JSONObject json = new JSONObject(responseDev.message);
		JSONObject experiment = new JSONObject(exp.getExperiment(experimentID, sessionToken));
		Assert.assertTrue(json.getJSONObject("experiments").getJSONArray("experiments").getJSONObject(0).getJSONArray("variants")
				.getJSONObject(0).getString("experimentName").equals(experiment.getString("name")) , "Experiment name was not added to variant object");
		
		RuntimeRestApi.DateModificationResults responseProd = RuntimeDateUtilities.getProductionFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseProd.code==304, "Runtime production file was changed");

	}
	
	@Test(dependsOnMethods="addComponents", description="Update experiment name and check in runtime variant")
	public void updateExperimentNameInVariant() throws Exception{
		String dateFormat = f.setDateFormat();
		String experiment = exp.getExperiment(experimentID, sessionToken);
		JSONObject json  = new JSONObject(experiment);
		json.put("name", "experiment1a");
		String response = exp.updateExperiment(experimentID, json.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Experiment was not updated");
		
		f.setSleep();
		RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getDevelopmentFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		//experiment name is added to variant object
		JSONObject resp = new JSONObject(responseDev.message);
		JSONObject experimentJson = new JSONObject(exp.getExperiment(experimentID, sessionToken));
		Assert.assertTrue(resp.getJSONObject("experiments").getJSONArray("experiments").getJSONObject(0).getJSONArray("variants")
				.getJSONObject(0).getString("experimentName").equals(experimentJson.getString("name")) , "Experiment name was not added to variant object");
		

		
	}
	
	@Test (dependsOnMethods = "updateExperimentNameInVariant", description = "Move experiment to production")
	public void moveExperimentToProduction() throws Exception{
		String dateFormat =   f.setDateFormat();
		
		//experiment in production
		String experiment = exp.getExperiment(experimentID, sessionToken);
		JSONObject json = new JSONObject(experiment);
		json.put("stage", "PRODUCTION");
		String response = exp.updateExperiment(experimentID, json.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Can't update experiment stage: " + response);

		f.setSleep();
		RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getDevelopmentFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);		
		Assert.assertTrue(responseDev.code==200, "Runtime development file was not changed");
		Assert.assertTrue(getBranchesInRuntime(responseDev.message).size()==2, "Incorrect number of branches in runtime development file");
		Assert.assertTrue(getExperimentsInRuntime(responseDev.message).size()==1, "Incorrect number of experiments in runtime development file");
		Assert.assertTrue(getVariantsInRuntime(responseDev.message).size()==2, "Incorrect number of variants in runtime development file");
		
		RuntimeRestApi.DateModificationResults responseProd = RuntimeDateUtilities.getProductionFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseProd.code==200, "Runtime production file was changed");
		Assert.assertTrue(getBranchesInRuntime(responseProd.message).size()==0, "Incorrect number of branches in runtime production file");
		Assert.assertTrue(getExperimentsInRuntime(responseProd.message).size()==1, "Incorrect number of experiments in runtime production file");
		Assert.assertTrue(getVariantsInRuntime(responseProd.message).size()==0, "Incorrect number of variants in runtime development file");
		
		RuntimeRestApi.DateModificationResults prodChanged = RuntimeDateUtilities.getProductionChangedDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(prodChanged.code ==200, "productionChanged.txt file was not changed");

	}
	
	@Test(dependsOnMethods="moveExperimentToProduction", description="Update experiment in runtime")
	public void updateExperiment() throws Exception{
		String dateFormat = f.setDateFormat();
		String experiment = exp.getExperiment(experimentID, sessionToken);
		JSONObject json  = new JSONObject(experiment);
		json.put("minVersion", "1.5");
		json.put("maxVersion", "3.5");
		String response = exp.updateExperiment(experimentID, json.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Experiment was not updated");
		
		f.setSleep();
		RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getDevelopmentFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseDev.code ==200, "Runtime development feature file was not updated");
		Assert.assertTrue(getExperimentsInRuntime(responseDev.message).getJSONObject(0).getString("minVersion").equals("1.5"), "Experiment minVersion not updated in runtime development file");
		Assert.assertTrue(getExperimentsInRuntime(responseDev.message).getJSONObject(0).getString("maxVersion").equals("3.5"), "Experiment maxVersion not updated in runtime development file");
		
		RuntimeRestApi.DateModificationResults responseProd = RuntimeDateUtilities.getProductionFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseProd.code ==200, "Runtime production feature file was not changed");
		Assert.assertTrue(getExperimentsInRuntime(responseProd.message).getJSONObject(0).getString("minVersion").equals("1.5"), "Experiment minVersion not updated in production development file");
		Assert.assertTrue(getExperimentsInRuntime(responseProd.message).getJSONObject(0).getString("maxVersion").equals("3.5"), "Experiment maxVersion not updated in production development file");
		
		RuntimeRestApi.DateModificationResults prodChanged = RuntimeDateUtilities.getProductionChangedDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(prodChanged.code ==200, "productionChanged.txt file was not changed");

		
	}
	
	@Test (dependsOnMethods = "updateExperiment", description = "Move variant to production")
	public void moveVariantToProduction() throws Exception{
		String dateFormat =   f.setDateFormat();
		
		String variant = exp.getVariant(variantID1, sessionToken);
		JSONObject json = new JSONObject(variant);
		json.put("stage", "PRODUCTION");
		String response = exp.updateVariant(variantID1, json.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Variant1 was not moved to production ");
	
		f.setSleep();
		RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getDevelopmentFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);		
		Assert.assertTrue(responseDev.code==200, "Runtime development file was not changed");
		Assert.assertTrue(getBranchesInRuntime(responseDev.message).size()==2, "Incorrect number of branches in runtime development file");
		Assert.assertTrue(getExperimentsInRuntime(responseDev.message).size()==1, "Incorrect number of experiments in runtime development file");
		Assert.assertTrue(getVariantsInRuntime(responseDev.message).size()==2, "Incorrect number of variants in runtime development file");
		
		RuntimeRestApi.DateModificationResults responseProd = RuntimeDateUtilities.getProductionFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseProd.code==200, "Runtime production file was changed");
		Assert.assertTrue(getBranchesInRuntime(responseProd.message).size()==1, "Incorrect number of branches in runtime production file");
		Assert.assertTrue(getExperimentsInRuntime(responseProd.message).size()==1, "Incorrect number of experiments in runtime production file");
		Assert.assertTrue(getVariantsInRuntime(responseProd.message).size()==1, "Incorrect number of variants in runtime development file");
		
		RuntimeRestApi.DateModificationResults prodChanged = RuntimeDateUtilities.getProductionChangedDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(prodChanged.code ==200, "productionChanged.txt file was not changed");

	}

	
	@Test(dependsOnMethods="moveVariantToProduction", description="Move variant & experiment to development")
	public void checkoutFeatures() throws Exception{
		String dateFormat = f.setDateFormat();
		
		//add features to master
		String feature1 = FileUtils.fileToString(filePath + "feature1.txt", "UTF-8", false);
		JSONObject jsonF1 = new JSONObject(feature1);
		jsonF1.put("stage", "PRODUCTION");
		featureID1 = f.addFeature(seasonID, jsonF1.toString(), "ROOT", sessionToken);
		Assert.assertFalse(featureID1.contains("error"), "Feature was not added to the season: " + featureID1);

		
		String feature2 = FileUtils.fileToString(filePath + "feature2.txt", "UTF-8", false);
		featureID2 = f.addFeature(seasonID, feature2, "ROOT", sessionToken);
		Assert.assertFalse(featureID2.contains("error"), "Feature was not added to the season: " + featureID2);

		String response = br.checkoutFeature(branchID1, featureID1, sessionToken);
		Assert.assertFalse(response.contains("error"), "feature1 was checked out twice");
		
		response = br.checkoutFeature(branchID2, featureID2, sessionToken);
		Assert.assertFalse(response.contains("error"), "feature2 was checked out twice");

		f.setSleep();
		RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getDevelopmentFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseDev.code ==200, "Runtime development feature file was not updated");
		Assert.assertTrue(getBranchesInRuntime(responseDev.message).size()==2, "Incorrect number of variants in runtime development file");		
		Assert.assertTrue(getBranchesInRuntime(responseDev.message).getJSONObject(0).getJSONArray("features").getJSONObject(0).getString("name").equals("Feature1"), "Feature1 was assigned to incorrect branch");
		Assert.assertTrue(getBranchesInRuntime(responseDev.message).getJSONObject(1).getJSONArray("features").getJSONObject(0).getString("name").equals("Feature2"), "Feature2 was assigned to incorrect branch");
		
		RuntimeRestApi.DateModificationResults responseProd = RuntimeDateUtilities.getProductionFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseProd.code ==200, "Runtime production feature file was not changed");

		RuntimeRestApi.DateModificationResults prodChanged = RuntimeDateUtilities.getProductionChangedDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(prodChanged.code ==200, "productionChanged.txt file was changed");

	}
	
	@Test(dependsOnMethods="checkoutFeatures", description="Change variants order")
	public void changeVariantsOrder() throws Exception{
		String dateFormat = f.setDateFormat();

		String experiment = exp.getExperiment(experimentID, sessionToken);
		JSONObject json  = new JSONObject(experiment);
		JSONArray variants = new JSONArray();
		JSONObject variant1 = new JSONObject(exp.getVariant(variantID1, sessionToken));
		JSONObject variant2 = new JSONObject(exp.getVariant(variantID2, sessionToken));
		variants.add(variant2);
		variants.add(variant1);
		json.put("variants", variants);
		String response = exp.updateExperiment(experimentID, json.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Experiment was not updated: " + response);
		
		//experiment in production, variant1 in production, variant2 in development
		f.setSleep();
		RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getDevelopmentFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseDev.code ==200, "Runtime development feature file was not updated");
		Assert.assertTrue(getExperimentsInRuntime(responseDev.message).getJSONObject(0).getJSONArray("variants").getJSONObject(0).getString("uniqueId").equals(variantID2), "Incorrect order of variants in runtime development file");	
		
		RuntimeRestApi.DateModificationResults responseProd = RuntimeDateUtilities.getProductionFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseProd.code ==200, "Runtime production feature file was changed");
		RuntimeRestApi.DateModificationResults prodChanged = RuntimeDateUtilities.getProductionChangedDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(prodChanged.code ==200, "productionChanged.txt file was changed");
		
	}
	
	@Test(dependsOnMethods="changeVariantsOrder", description="Move variant & experiment to development")
	public void moveExperimentToDevelopment() throws Exception{
		String dateFormat = f.setDateFormat();

		String variant = exp.getVariant(variantID1, sessionToken);
		JSONObject json = new JSONObject(variant);
		json.put("stage", "DEVELOPMENT");
		String response = exp.updateVariant(variantID1, json.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Variant1 was not moved to development ");

		String experiment = exp.getExperiment(experimentID, sessionToken);
		JSONObject jsonExp  = new JSONObject(experiment);
		jsonExp.put("stage", "DEVELOPMENT");
		response = exp.updateExperiment(experimentID, jsonExp.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Experiment was not updated");
		
		f.setSleep();
		RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getDevelopmentFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseDev.code ==200, "Runtime development feature file was not updated");
		Assert.assertTrue(getVariantsInRuntime(responseDev.message).size()==2, "Incorrect number of variants in runtime development file");		
		
		RuntimeRestApi.DateModificationResults responseProd = RuntimeDateUtilities.getProductionFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseProd.code ==200, "Runtime production feature file was not changed");
		Assert.assertTrue(getExperimentsInRuntime(responseProd.message).size()==0, "Incorrect number of experiments in runtime production file");
		RuntimeRestApi.DateModificationResults prodChanged = RuntimeDateUtilities.getProductionChangedDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(prodChanged.code ==200, "productionChanged.txt file was changed");
		
	}
	

	
	@Test(dependsOnMethods="moveExperimentToDevelopment", description="Delete variant ")
	public void deleteVariant() throws Exception{
		String dateFormat = f.setDateFormat();

		int responseCode = exp.deleteVariant(variantID2, sessionToken);
		Assert.assertTrue(responseCode==200, "Variant2 was not deleted");
		
		//experiment in development, variant1 in production, variant2 in development
		f.setSleep();
		RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getDevelopmentFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseDev.code ==200, "Runtime development feature file was not updated");
		Assert.assertTrue(getVariantsInRuntime(responseDev.message).size()==1, "Incorrect number of variants in runtime development file");		
		Assert.assertTrue(getBranchesInRuntime(responseDev.message).size()==1, "Incorrect number of branches in runtime production file");
		
		RuntimeRestApi.DateModificationResults responseProd = RuntimeDateUtilities.getProductionFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseProd.code ==304, "Runtime production feature file was  changed");
		RuntimeRestApi.DateModificationResults prodChanged = RuntimeDateUtilities.getProductionChangedDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(prodChanged.code ==304, "productionChanged.txt file was changed");
		
	}
	

	@Test(dependsOnMethods="deleteVariant", description="Delete experiment ")
	public void deleteExperiment() throws Exception{
		String dateFormat = f.setDateFormat();
	
		int responseCode = exp.deleteExperiment(experimentID, sessionToken);
		Assert.assertTrue(responseCode==200, "Experiment was not deleted");
		
		//experiment in development, variant1 in production, variant2 in development
		f.setSleep();
		RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getDevelopmentFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseDev.code ==200, "Runtime development feature file was not updated");
		Assert.assertTrue(getExperimentsInRuntime(responseDev.message).size()==0, "Experiment was not deleted in runtime development file");		
		Assert.assertTrue(getBranchesInRuntime(responseDev.message).size()==0, "Incorrect number of branches in runtime production file");
		
		RuntimeRestApi.DateModificationResults responseProd = RuntimeDateUtilities.getProductionFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseProd.code ==304, "Runtime production feature file was  changed");
		RuntimeRestApi.DateModificationResults prodChanged = RuntimeDateUtilities.getProductionChangedDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(prodChanged.code ==304, "productionChanged.txt file was changed");
		
	}
	private String addExperiment(String experimentName) throws IOException, JSONException{
		return baseUtils.addExperiment(experimentName, m_analyticsUrl, false, false);

	}
	

	private String addVariant(String variantName, String branchName) throws IOException, JSONException{
		String variant = FileUtils.fileToString(filePath + "experiments/variant1.txt", "UTF-8", false);
		JSONObject variantJson = new JSONObject(variant);
		variantJson.put("name", variantName);
		variantJson.put("branchName", branchName);
		return exp.createVariant(experimentID, variantJson.toString(), sessionToken);

	}
	
	private String addBranch(String branchName) throws JSONException, IOException{
		String branch = FileUtils.fileToString(filePath + "experiments/branch1.txt", "UTF-8", false);
		JSONObject branchJson = new JSONObject(branch);
		branchJson.put("name", branchName);
		return br.createBranch(seasonID, branchJson.toString(), BranchesRestApi.MASTER, sessionToken);

	}
	
	private JSONArray getBranchesInRuntime(String input) throws JSONException{
		JSONObject json = new JSONObject(input);
		return json.getJSONArray("branches");
	}
	
	private JSONArray getExperimentsInRuntime(String input) throws JSONException{
		JSONObject json = new JSONObject(input);
		return json.getJSONObject("experiments").getJSONArray("experiments");
	}
	
	private JSONArray getVariantsInRuntime(String input) throws JSONException{
		JSONObject json = new JSONObject(input);
		return json.getJSONObject("experiments").getJSONArray("experiments").getJSONObject(0).getJSONArray("variants");
	}

	
	@AfterTest
	private void reset(){
		baseUtils.reset(productID, sessionToken);
	}
}
