package tests.restapi.analytics_in_experiments;

import java.io.IOException;









import org.apache.commons.lang3.RandomStringUtils;
import org.apache.wink.json4j.JSONException;
import org.apache.wink.json4j.JSONObject;
import org.apache.wink.json4j.JSONArray;
import org.testng.Assert;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import tests.com.ibm.qautils.FileUtils;
import tests.restapi.AirlockUtils;
import tests.restapi.AnalyticsRestApi;
import tests.restapi.BranchesRestApi;
import tests.restapi.ExperimentsRestApi;
import tests.restapi.FeaturesRestApi;
import tests.restapi.InputSchemaRestApi;
import tests.restapi.ProductsRestApi;
import tests.restapi.RuntimeDateUtilities;
import tests.restapi.RuntimeRestApi;
import tests.restapi.SeasonsRestApi;

public class AddInputFieldsToAnalyticsInDevelopmentExperiment {
	protected String seasonID1;
	protected String seasonID2;
	private String branchID1;
	private String branchID2;
	protected String productID;
	protected String featureID;
	protected String filePath;
	protected String m_url;
	protected ProductsRestApi p;
	protected FeaturesRestApi f;
	protected AnalyticsRestApi an;
	protected InputSchemaRestApi schema;
	private String sessionToken = "";
	protected AirlockUtils baseUtils;
	private BranchesRestApi br ;
	private ExperimentsRestApi exp ;
	private String experimentID;
	private String variantID;
	private String m_analyticsUrl;
	private SeasonsRestApi s;
	
	@BeforeClass
	@Parameters({"url", "analyticsUrl", "translationsUrl", "configPath", "sessionToken", "userName", "userPassword", "appName", "productsToDeleteFile"})
	public void init(String url, String analyticsUrl, String translationsUrl, String configPath, String sToken, String userName, String userPassword, String appName, String productsToDeleteFile) throws Exception{
		m_url = url;
		m_analyticsUrl = analyticsUrl;
		filePath = configPath;
		p = new ProductsRestApi();
		p.setURL(m_url);
		f = new FeaturesRestApi();
		f.setURL(m_url);
		an = new AnalyticsRestApi();
		an.setURL(analyticsUrl);
        schema = new InputSchemaRestApi();
        schema.setURL(m_url);
		br = new BranchesRestApi();
		br.setURL(m_url);
		exp = new ExperimentsRestApi();
		exp.setURL(m_analyticsUrl);
		s = new SeasonsRestApi();
		s.setURL(m_url);
		
        
		baseUtils = new AirlockUtils(url, analyticsUrl, translationsUrl, configPath, sToken, userName, userPassword, appName, productsToDeleteFile);
		sessionToken = baseUtils.sessionToken;

		productID = baseUtils.createProduct();
		baseUtils.printProductToFile(productID);
		JSONObject season = new JSONObject();
		season.put("minVersion", "1.0");
		seasonID1 = s.addSeason(productID, season.toString(), sessionToken);

		
	}
	

	
	
	@Test (description="Add input schema to the season")
	public void addSchema() throws Exception{
		String sch = schema.getInputSchema(seasonID1, sessionToken);
        JSONObject jsonSchema = new JSONObject(sch);
        String schemaBody = FileUtils.fileToString(filePath + "inputSchemas/inputSchema_update_device_locale_to_production.txt", "UTF-8", false);
        jsonSchema.put("inputSchema", new JSONObject(schemaBody));
        String response = schema.updateInputSchema(seasonID1, jsonSchema.toString(), sessionToken);
        Assert.assertFalse(response.contains("error"), "Schema was not added to the season" + response);
	}
	
	@Test (dependsOnMethods="addSchema", description="Add components")
	public void addSeason() throws Exception{
		String season = "{\"minVersion\":\"2.0\"}";
		seasonID2 = s.addSeason(productID, season, sessionToken);
		Assert.assertFalse(seasonID2.contains("error"), "Can't delete second season: " + seasonID2);
	}
	
	@Test (dependsOnMethods="addSeason", description="Add components")
	public void addExperiment() throws Exception{
		String experiment = FileUtils.fileToString(filePath + "experiments/experiment1.txt", "UTF-8", false);
		JSONObject expJson = new JSONObject(experiment);
		
		expJson.put("name", "experiment."+RandomStringUtils.randomAlphabetic(5));
		expJson.put("minVersion", "0.5");
		expJson.put("maxVersion", "2.5");
		expJson.put("enabled", false);
		expJson.put("stage", "DEVELOPMENT");
		experimentID = exp.createExperiment(productID, expJson.toString(), sessionToken);
		Assert.assertFalse(experimentID.contains("error"), "Experiment was not created: " + experimentID);
		
		branchID1 = addBranch(seasonID1, "branch1");
		Assert.assertFalse(branchID1.contains("error"), "Branch1 was not created in season1: " + branchID1);
		
		branchID2 = addBranch(seasonID2, "branch1");
		Assert.assertFalse(branchID2.contains("error"), "Branch1 was not created in season2: " + branchID2);

		variantID = addVariant("variant1", "branch1");
		Assert.assertFalse(variantID.contains("error"), "Variant1 was not created: " + variantID);
			
		String airlockExperiment = exp.getExperiment(experimentID, sessionToken);
		Assert.assertFalse(airlockExperiment.contains("error"), "Experiment was not found: " + experimentID);

		//enable experiment so a range will be created and the experiment will be published to analytics server		
		expJson = new JSONObject(airlockExperiment);
		expJson.put("enabled", true);
		
		String response = exp.updateExperiment(experimentID, expJson.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Experiment was not updated: " + response); 
	}
	
	
	@Test (dependsOnMethods="addExperiment", description="Add  fields  to inputFieldsToAnalytics")
	public void addFieldsToAnalytics() throws IOException, JSONException, InterruptedException{
		String dateFormat = an.setDateFormat();

		//production field in season1 master
		JSONArray inputFieldsInSeason1 = new JSONArray();
		inputFieldsInSeason1.put("context.device.locale");
		String response = an.updateInputFieldToAnalytics(seasonID1, BranchesRestApi.MASTER, inputFieldsInSeason1.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Analytics was not updated" + response);		
		response = an.getGlobalDataCollection(seasonID1, BranchesRestApi.MASTER, "BASIC", sessionToken);
		Assert.assertTrue(inputFields(response).size()==1, "Incorrect number of input fields");
		
		//production field in branch1
		JSONArray inputFieldsInBranch1 = new JSONArray();
		inputFieldsInBranch1.put("context.device.connectionType");
		inputFieldsInBranch1.put("context.device.locale"); //master field must be reported in branch
		response = an.updateInputFieldToAnalytics(seasonID1, branchID1, inputFieldsInBranch1.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Analytics was not updated: " + response);		
		response = an.getGlobalDataCollection(seasonID1, branchID1, "BASIC", sessionToken);
		Assert.assertTrue(inputFields(response).size()==2, "Incorrect number of input fields");
		
		//production field in season2 master
		JSONArray inputFieldsInSeason2 = new JSONArray();
		inputFieldsInSeason2.put("context.device.osVersion");
		response = an.updateInputFieldToAnalytics(seasonID2, BranchesRestApi.MASTER, inputFieldsInSeason2.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Analytics was not updated: " + response);		
		response = an.getGlobalDataCollection(seasonID2, BranchesRestApi.MASTER, "BASIC", sessionToken);
		Assert.assertTrue(inputFields(response).size()==1, "Incorrect number of input fields");

		//production field in branch2
		JSONArray inputFieldsInBranch2 = new JSONArray();
		inputFieldsInBranch2.put("context.device.osVersion");
		response = an.updateInputFieldToAnalytics(seasonID2, branchID2, inputFieldsInBranch2.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Analytics was not updated: " + response);		
		response = an.getGlobalDataCollection(seasonID2, branchID2, "BASIC", sessionToken);
		Assert.assertTrue(inputFields(response).size()==1, "Incorrect number of input fields");	
		
		//check if files were changed
		an.setSleep();
		RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getDevelopmentFileDateModification(m_url, productID, seasonID1, dateFormat, sessionToken);
		Assert.assertTrue(responseDev.code ==200, "Runtime development feature file was not updated");
		Assert.assertTrue(ifRuntimeContainsField(responseDev.message, "context.device.locale"), "The field \"context.device.locale\" was not found in the runtime development file in season1");
		Assert.assertTrue(ifRuntimeContainsField(responseDev.message, "context.device.connectionType"), "The field \"context.device.connectionType\" was not found in the runtime development file in season1");
		Assert.assertTrue(ifRuntimeContainsField(responseDev.message, "context.device.osVersion"), "The field \"context.device.osVersion\" was not found in the runtime development file in season1");
		RuntimeRestApi.DateModificationResults responseProd = RuntimeDateUtilities.getProductionFileDateModification(m_url, productID, seasonID1, dateFormat, sessionToken);
		Assert.assertTrue(responseProd.code ==200, "Runtime production feature file was not changed");
		Assert.assertTrue(!ifRuntimeContainsField(responseProd.message, "context.device.locale"), "The field \"context.device.locale\" was  found in the runtime production file in season1");
		Assert.assertTrue(!ifRuntimeContainsField(responseProd.message, "context.device.connectionType"), "The field \"context.device.connectionType\" was  found in the runtime production file in season1");
		Assert.assertTrue(!ifRuntimeContainsField(responseProd.message, "context.device.osVersion"), "The field \"context.device.osVersion\" was  found in the runtime production file in season1");
		RuntimeRestApi.DateModificationResults prodChanged = RuntimeDateUtilities.getProductionChangedDateModification(m_url, productID, seasonID1, dateFormat, sessionToken);
		Assert.assertTrue(prodChanged.code ==200, "productionChanged.txt file was not changed");
		

		responseDev = RuntimeDateUtilities.getDevelopmentFileDateModification(m_url, productID, seasonID2, dateFormat, sessionToken);
		Assert.assertTrue(responseDev.code ==200, "Runtime development feature file was not updated");
		Assert.assertTrue(ifRuntimeContainsField(responseDev.message, "context.device.locale"), "The field \"context.device.locale\" was not found in the runtime development file in season2");
		Assert.assertTrue(ifRuntimeContainsField(responseDev.message, "context.device.connectionType"), "The field \"context.device.connectionType\" was not found in the runtime development file in season2");
		Assert.assertTrue(ifRuntimeContainsField(responseDev.message, "context.device.osVersion"), "The field \"context.device.osVersion\" was not found in the runtime development file in season2");
		responseProd = RuntimeDateUtilities.getProductionFileDateModification(m_url, productID, seasonID2, dateFormat, sessionToken);
		Assert.assertTrue(responseProd.code ==200, "Runtime production feature file was not changed");
		Assert.assertTrue(!ifRuntimeContainsField(responseProd.message, "context.device.locale"), "The field \"context.device.locale\" was  found in the runtime production file in season2");
		Assert.assertTrue(!ifRuntimeContainsField(responseProd.message, "context.device.connectionType"), "The field \"context.device.connectionType\" was  found in the runtime production file in season2");
		Assert.assertTrue(!ifRuntimeContainsField(responseProd.message, "context.device.osVersion"), "The field \"context.device.osVersion\" was found in the runtime production file in season2");
		prodChanged = RuntimeDateUtilities.getProductionChangedDateModification(m_url, productID, seasonID2, dateFormat, sessionToken);
		Assert.assertTrue(prodChanged.code ==200, "productionChanged.txt file was changed");
	
	}
	
	
	@Test (dependsOnMethods="addFieldsToAnalytics", description="Move  experiment and variant  to production")
	public void moveExperimentToProduction() throws Exception{
		String dateFormat = an.setDateFormat();

		String experiment = exp.getExperiment(experimentID, sessionToken);
		JSONObject json = new JSONObject(experiment);
		json.put("stage", "PRODUCTION");
		String response = exp.updateExperiment(experimentID, json.toString(), sessionToken);
		Assert.assertFalse (response.contains("error"), "Experiment was not updated: " + response);
		
		String variant = exp.getVariant(variantID, sessionToken);
		JSONObject jsonVar = new JSONObject(variant);
		jsonVar.put("stage", "PRODUCTION");
		String respVar = exp.updateVariant(variantID, jsonVar.toString(), sessionToken);
		Assert.assertFalse (respVar.contains("error"), "Variant was not updated: " + respVar);

		
		//check if files were changed
		an.setSleep();
		RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getDevelopmentFileDateModification(m_url, productID, seasonID1, dateFormat, sessionToken);
		Assert.assertTrue(responseDev.code ==200, "Runtime development feature file was not updated");
		RuntimeRestApi.DateModificationResults responseProd = RuntimeDateUtilities.getProductionFileDateModification(m_url, productID, seasonID1, dateFormat, sessionToken);
		Assert.assertTrue(responseProd.code ==200, "Runtime production feature file was not changed");		
		Assert.assertTrue(ifRuntimeContainsField(responseProd.message, "context.device.locale"), "The field \"context.device.locale\" was not found in the runtime production file in season1");
		Assert.assertTrue(ifRuntimeContainsField(responseProd.message, "context.device.connectionType"), "The field \"context.device.connectionType\" was not found in the runtime production file in season1");
		Assert.assertTrue(ifRuntimeContainsField(responseProd.message, "context.device.osVersion"), "The field \"context.device.osVersion\" was not found in the runtime production file in season1");
		RuntimeRestApi.DateModificationResults prodChanged = RuntimeDateUtilities.getProductionChangedDateModification(m_url, productID, seasonID1, dateFormat, sessionToken);
		Assert.assertTrue(prodChanged.code ==200, "productionChanged.txt file was not changed");
		

		responseDev = RuntimeDateUtilities.getDevelopmentFileDateModification(m_url, productID, seasonID2, dateFormat, sessionToken);
		Assert.assertTrue(responseDev.code ==200, "Runtime development feature file was not updated");
		responseProd = RuntimeDateUtilities.getProductionFileDateModification(m_url, productID, seasonID2, dateFormat, sessionToken);
		Assert.assertTrue(responseProd.code ==200, "Runtime production feature file was not changed");
		Assert.assertTrue(ifRuntimeContainsField(responseProd.message, "context.device.locale"), "The field \"context.device.locale\" was not found in the runtime production file in season2");
		Assert.assertTrue(ifRuntimeContainsField(responseProd.message, "context.device.connectionType"), "The field \"context.device.connectionType\" was not found in the runtime production file in season2");
		Assert.assertTrue(ifRuntimeContainsField(responseProd.message, "context.device.osVersion"), "The field \"context.device.osVersion\" was not found in the runtime production file in season2");
		prodChanged = RuntimeDateUtilities.getProductionChangedDateModification(m_url, productID, seasonID2, dateFormat, sessionToken);
		Assert.assertTrue(prodChanged.code ==200, "productionChanged.txt file was changed");
	
	}
	

	@Test (dependsOnMethods="moveExperimentToProduction", description="Move  experiment to development")
	public void moveExperimentToDevelopment() throws Exception{
		String dateFormat = an.setDateFormat();

		String variant = exp.getVariant(variantID, sessionToken);
		JSONObject jsonVar = new JSONObject(variant);
		jsonVar.put("stage", "DEVELOPMENT");
		String respVar = exp.updateVariant(variantID, jsonVar.toString(), sessionToken);
		Assert.assertFalse (respVar.contains("error"), "Variant was not updated: " + respVar);
		
		String experiment = exp.getExperiment(experimentID, sessionToken);
		JSONObject json = new JSONObject(experiment);
		json.put("stage", "DEVELOPMENT");
		String response = exp.updateExperiment(experimentID, json.toString(), sessionToken);
		Assert.assertFalse (response.contains("error"), "Experiment was not updated: " + response);
		
		
		//check if files were changed
		an.setSleep();
		RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getDevelopmentFileDateModification(m_url, productID, seasonID1, dateFormat, sessionToken);
		Assert.assertTrue(responseDev.code ==200, "Runtime development feature file was not updated");
		Assert.assertTrue(ifRuntimeContainsField(responseDev.message, "context.device.locale"), "The field \"context.device.locale\" was not found in the runtime development file in season1");
		Assert.assertTrue(ifRuntimeContainsField(responseDev.message, "context.device.connectionType"), "The field \"context.device.connectionType\" was not found in the runtime development file in season1");
		Assert.assertTrue(ifRuntimeContainsField(responseDev.message, "context.device.osVersion"), "The field \"context.device.osVersion\" was not found in the runtime development file in season1");

		RuntimeRestApi.DateModificationResults responseProd = RuntimeDateUtilities.getProductionFileDateModification(m_url, productID, seasonID1, dateFormat, sessionToken);
		Assert.assertTrue(responseProd.code ==200, "Runtime production feature file was not changed");		
		Assert.assertTrue(!ifRuntimeContainsField(responseProd.message, "context.device.locale"), "The field \"context.device.locale\" was not found in the runtime production file in season1");
		Assert.assertTrue(!ifRuntimeContainsField(responseProd.message, "context.device.connectionType"), "The field \"context.device.connectionType\" was not found in the runtime production file in season1");
		Assert.assertTrue(!ifRuntimeContainsField(responseProd.message, "context.device.osVersion"), "The field \"context.device.osVersion\" was not found in the runtime production file in season1");
		RuntimeRestApi.DateModificationResults prodChanged = RuntimeDateUtilities.getProductionChangedDateModification(m_url, productID, seasonID1, dateFormat, sessionToken);
		Assert.assertTrue(prodChanged.code ==200, "productionChanged.txt file was not changed");
		

		responseDev = RuntimeDateUtilities.getDevelopmentFileDateModification(m_url, productID, seasonID2, dateFormat, sessionToken);
		Assert.assertTrue(responseDev.code ==200, "Runtime development feature file was not updated");
		Assert.assertTrue(ifRuntimeContainsField(responseDev.message, "context.device.locale"), "The field \"context.device.locale\" was not found in the runtime development file in season2");
		Assert.assertTrue(ifRuntimeContainsField(responseDev.message, "context.device.connectionType"), "The field \"context.device.connectionType\" was not found in the runtime development file in season2");
		Assert.assertTrue(ifRuntimeContainsField(responseDev.message, "context.device.osVersion"), "The field \"context.device.osVersion\" was not found in the runtime development file in season2");

		responseProd = RuntimeDateUtilities.getProductionFileDateModification(m_url, productID, seasonID2, dateFormat, sessionToken);
		Assert.assertTrue(responseProd.code ==200, "Runtime production feature file was not changed");
		Assert.assertTrue(!ifRuntimeContainsField(responseProd.message, "context.device.locale"), "The field \"context.device.locale\" was not found in the runtime production file in season2");
		Assert.assertTrue(!ifRuntimeContainsField(responseProd.message, "context.device.connectionType"), "The field \"context.device.connectionType\" was not found in the runtime production file in season2");
		Assert.assertTrue(!ifRuntimeContainsField(responseProd.message, "context.device.osVersion"), "The field \"context.device.osVersion\" was not found in the runtime production file in season2");
		prodChanged = RuntimeDateUtilities.getProductionChangedDateModification(m_url, productID, seasonID2, dateFormat, sessionToken);
		Assert.assertTrue(prodChanged.code ==200, "productionChanged.txt file was changed");
	
	}
	private boolean ifRuntimeContainsField(String input, String field){
		
		try{
			JSONObject json = new JSONObject(input);

			if (json.containsKey("experiments")){
				JSONArray inputFields = json.getJSONObject("experiments").getJSONArray("experiments").getJSONObject(0).getJSONObject("analytics").getJSONArray("inputFieldsForAnalytics");
				for (Object s : inputFields) {
					if (s.equals(field)) 
						return true;
				}
				
				return false;
			} else {

				return false;
			}
		} catch (Exception e){
				return false;
		}
	}
	
	

	private String addVariant(String variantName, String branchName) throws IOException, JSONException{
		String variant = FileUtils.fileToString(filePath + "experiments/variant1.txt", "UTF-8", false);
		JSONObject variantJson = new JSONObject(variant);
		variantJson.put("name", variantName);
		variantJson.put("branchName", branchName);
		variantJson.put("stage", "DEVELOPMENT");
		return exp.createVariant(experimentID, variantJson.toString(), sessionToken);

	}
	
	private String addBranch(String seasonId, String branchName) throws JSONException, IOException{
		String branch = FileUtils.fileToString(filePath + "experiments/branch1.txt", "UTF-8", false);
		JSONObject branchJson = new JSONObject(branch);
		branchJson.put("name", branchName);
		return br.createBranch(seasonId, branchJson.toString(), BranchesRestApi.MASTER, sessionToken);

	}
	
	private JSONArray inputFields(String analytics) throws JSONException{
		JSONObject json = new JSONObject(analytics);
		return json.getJSONObject("analyticsDataCollection").getJSONArray("inputFieldsForAnalytics");
	}
	

	
	@AfterTest
	private void reset(){
		baseUtils.reset(productID, sessionToken);
	}
}
