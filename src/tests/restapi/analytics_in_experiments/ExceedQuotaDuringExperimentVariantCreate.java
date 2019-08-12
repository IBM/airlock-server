package tests.restapi.analytics_in_experiments;

import java.io.IOException;

import org.apache.wink.json4j.JSONArray;
import org.apache.wink.json4j.JSONException;
import org.apache.wink.json4j.JSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeClass;

import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import tests.com.ibm.qautils.FileUtils;
import tests.restapi.*;

public class ExceedQuotaDuringExperimentVariantCreate {
	protected String seasonID;
	protected String branchID1;
	protected String productID;
	protected String featureID1;
	protected String featureID2;
	protected String configID1;
	protected String configID2;
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
	private String variantID1;
	private String m_analyticsUrl;
	
	
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
		exp.setURL(analyticsUrl);
        
		baseUtils = new AirlockUtils(url, analyticsUrl, translationsUrl, configPath, sToken, userName, userPassword, appName, productsToDeleteFile);
		sessionToken = baseUtils.sessionToken;

		productID = baseUtils.createProduct();
		baseUtils.printProductToFile(productID);
		seasonID = baseUtils.createSeason(productID);

	}
	
	//experiment and variant in production stage
	
	//use separate api-s to add to analytics
	
	@Test ( description="Add branch to the season")
	public void addBranch() throws Exception{
		branchID1 = addBranch("branch1");
		Assert.assertFalse(branchID1.contains("error"), "Branch1 was not created: " + branchID1);
	}

	
	@Test ( dependsOnMethods="addBranch", description="Add input schema to the season")
	public void addSchema() throws Exception{
		String sch = schema.getInputSchema(seasonID, sessionToken);
        JSONObject jsonSchema = new JSONObject(sch);
        String schemaBody = FileUtils.fileToString(filePath + "inputSchemas/inputSchema_update_device_locale_to_production.txt", "UTF-8", false);
        jsonSchema.put("inputSchema", new JSONObject(schemaBody));
        String response = schema.updateInputSchema(seasonID, jsonSchema.toString(), sessionToken);
        Assert.assertFalse(response.contains("error"), "Schema was not added to the season" + response);
	}

	@Test (dependsOnMethods="addSchema", description="Set quota to 2")
	public void updateQuota() throws IOException, JSONException, InterruptedException{
		String response = an.updateQuota(seasonID, 2, sessionToken);
		Assert.assertFalse(response.contains("error"), "Quota was not returned " + response);

	}
	
	@Test (dependsOnMethods="updateQuota", description="Add 2 input fields in prod and 2 input fields in dev , 1 feature to barnch1, 1 attribute to barnch1")
	public void addToAnalytics() throws Exception{
		//add input field to analytics in branch
		JSONArray inputFields = new JSONArray();
		inputFields.put("context.device.locale");
		inputFields.put("context.device.version");
		inputFields.put("context.device.osVersion");
		inputFields.put("context.device.localeLanguage");
		String response = an.updateInputFieldToAnalytics(seasonID, branchID1,  inputFields.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Analytics was not updated" + response);
		
		//add feature in development to master and report it in branch
		String feature1 = FileUtils.fileToString(filePath + "feature1.txt", "UTF-8", false);
		JSONObject jsonF = new JSONObject(feature1);
		featureID1 = f.addFeature(seasonID, jsonF.toString(), "ROOT", sessionToken);
		Assert.assertFalse(featureID1.contains("error"), "Feature was not added to the season" + featureID1);
		response = an.addFeatureToAnalytics(featureID1, BranchesRestApi.MASTER, sessionToken);
		Assert.assertFalse(response.contains("error"), "Incorrect globalDataCollection response");	
		
		String respWithQuota = an.getGlobalDataCollection(seasonID, branchID1, "DISPLAY", sessionToken);
		Assert.assertTrue(getDevelopmentItemsReportedToAnalytics(respWithQuota)==5, "Incorrect number of development items"); //feature1+attribute+inputfield
		Assert.assertTrue(getProductionItemsReportedToAnalytics(respWithQuota)==2, "Incorrect number of production items"); //feature1+attribute+inputfield
		
		//move feature to prod in master
		jsonF = new JSONObject(f.getFeature(featureID1, sessionToken));
		jsonF.put("stage", "PRODUCTION");
		response = f.updateFeature(seasonID, featureID1, jsonF.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Can't update feature to production: " + response);
		
		
	}
	
	@Test (dependsOnMethods="addToAnalytics", description="Create development variant")
	public void createProdVariant() throws Exception{
		experimentID = baseUtils.addExperiment(m_analyticsUrl, true, false);
		Assert.assertFalse(experimentID.contains("error"), "Experiment was not created: " + experimentID);
		
		String response = an.getExperimentQuota(experimentID, sessionToken);
		JSONObject quota = new JSONObject(response);
		Assert.assertTrue(quota.getInt("analyticsQuota")==2, "Incorrect experiment quota was not returned " + quota.getInt("analyticsQuota"));


		variantID1 = addVariant("variant1", "branch1", "PRODUCTION");
		Assert.assertTrue(variantID1.contains("error"), "Variant1 was created even when quota was exceeded ");
	
	}
	

	private int getDevelopmentItemsReportedToAnalytics(String analytics) throws JSONException{
		JSONObject json = new JSONObject(analytics);
		return json.getJSONObject("analyticsDataCollection").getInt("developmentItemsReportedToAnalytics");
	}
	
	private int getProductionItemsReportedToAnalytics(String analytics) throws JSONException{
		JSONObject json = new JSONObject(analytics);
		//System.out.println("productionItem = " + json.getJSONObject("analyticsDataCollection").getInt("productionItemsReportedToAnalytics"));
		return json.getJSONObject("analyticsDataCollection").getInt("productionItemsReportedToAnalytics");
	}

	
	private String addVariant(String variantName, String branchName, String variantStage) throws IOException, JSONException{
		String variant = FileUtils.fileToString(filePath + "experiments/variant1.txt", "UTF-8", false);
		JSONObject variantJson = new JSONObject(variant);
		variantJson.put("name", variantName);
		variantJson.put("branchName", branchName);
		variantJson.put("stage", variantStage);
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
