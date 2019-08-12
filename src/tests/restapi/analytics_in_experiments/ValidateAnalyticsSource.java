package tests.restapi.analytics_in_experiments;

import java.io.IOException;

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

public class ValidateAnalyticsSource {
	protected String seasonID;
	protected String productID;
	protected String featureID1;
	protected String featureID2;
	private String featureID3;
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
	private String branchID1;
	private String branchID2;
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
	
	/*
- add input fields to analytics from season and from branch. Get globalData in "display" mode and check for each field its source - master of branch
- add attributes to analytics from season and from branch. Get globalData in "display" mode and check for each field its source - master of branch
- add feature/configuration to analytics from season and from branch. Get globalData in "display" mode and check for each field its source - master of branch

	 */
	
	@Test (description="Add components")
	public void addBranch() throws Exception{
		experimentID = baseUtils.addExperiment(m_analyticsUrl, true, false);
		Assert.assertFalse(experimentID.contains("error"), "Experiment was not created: " + experimentID);
		
		branchID1 = addBranch("branch1");
		Assert.assertFalse(branchID1.contains("error"), "Branch1 was not created: " + branchID1);
		branchID2 = addBranch("branch2");
		Assert.assertFalse(branchID2.contains("error"), "Branch2 was not created: " + branchID2);


		String variantID1 = addVariant("variant1", "branch1");
		Assert.assertFalse(variantID1.contains("error"), "Variant1 was not created: " + variantID1);
		String variantID2 = addVariant("variant2", "branch2");
		Assert.assertFalse(variantID2.contains("error"), "Variant2 was not created: " + variantID2);

		
	
		
	}
	
	
	@Test (dependsOnMethods="addBranch", description="Add features to analytics")
	public void addFeaturesToAnalytics() throws IOException, JSONException, InterruptedException{

		//add to master
		JSONObject feature = new JSONObject(FileUtils.fileToString(filePath + "feature1.txt", "UTF-8", false));
		feature.put("name", "F1");
		featureID1 = f.addFeature(seasonID, feature.toString(), "ROOT", sessionToken);
		Assert.assertFalse(featureID1.contains("error"), "Feature1 was not added: " + featureID1);
		
		//add to branch1
		feature.put("name", "F2");
		featureID2 = f.addFeatureToBranch(seasonID, branchID1, feature.toString(), "ROOT", sessionToken);
		Assert.assertFalse(featureID2.contains("error"), "Feature2 was not added: " + featureID2);

		//add to branch2
		feature.put("name", "F3");
		featureID3 = f.addFeatureToBranch(seasonID, branchID2, feature.toString(), "ROOT", sessionToken);
		Assert.assertFalse(featureID3.contains("error"), "Feature3 was not added: " + featureID3);
		
		//add all 3 to analytics
		an.addFeatureToAnalytics(featureID1, BranchesRestApi.MASTER, sessionToken);
		an.addFeatureToAnalytics(featureID2, branchID1, sessionToken);
		an.addFeatureToAnalytics(featureID3, branchID2, sessionToken);
		
		JSONObject response = new JSONObject(an.getGlobalDataCollection(seasonID, branchID1, "DISPLAY", sessionToken));
		JSONArray reportedFeatures = response.getJSONObject("analyticsDataCollection").getJSONArray("analyticsDataCollectionByFeatureNames");
		for (int i=0; i< reportedFeatures.size(); i++) {
			if (reportedFeatures.getJSONObject(i).getString("name").equals("ns1.F1"))
				Assert.assertTrue(reportedFeatures.getJSONObject(i).getString("branchName").equals("MASTER"), "Incorrect source for F1");
			else if(reportedFeatures.getJSONObject(i).getString("name").equals("ns1.F2"))
				Assert.assertTrue(reportedFeatures.getJSONObject(i).getString("branchName").equals("branch1"), "Incorrect source for F2");
		}
		
		response = new JSONObject(an.getGlobalDataCollection(seasonID, branchID2, "DISPLAY", sessionToken));
		reportedFeatures = response.getJSONObject("analyticsDataCollection").getJSONArray("analyticsDataCollectionByFeatureNames");
		for (int i=0; i< reportedFeatures.size(); i++) {
			if (reportedFeatures.getJSONObject(i).getString("name").equals("ns1.F1"))
				Assert.assertTrue(reportedFeatures.getJSONObject(i).getString("branchName").equals("MASTER"), "Incorrect source for F1");
			else if(reportedFeatures.getJSONObject(i).getString("name").equals("ns1.F3"))
				Assert.assertTrue(reportedFeatures.getJSONObject(i).getString("branchName").equals("branch2"), "Incorrect source for F3");
		}

	}

	
	@Test (dependsOnMethods="addFeaturesToAnalytics", description="Add attributes to analytics")
	public void addAttributesToAnalytics() throws IOException, JSONException, InterruptedException{

		//add to master
		JSONObject feature = new JSONObject(FileUtils.fileToString(filePath + "configuration_rule1.txt", "UTF-8", false));
		JSONObject newConfiguration = new JSONObject();
		newConfiguration.put("color", "red");
		newConfiguration.put("size", "small");
		feature.put("configuration", newConfiguration);

		feature.put("name", "CR1");
		String CRID1 = f.addFeature(seasonID, feature.toString(), featureID1, sessionToken);
		Assert.assertFalse(CRID1.contains("error"), "Configuration1 was not added: " + CRID1);
		
		//add to branch1
		feature.put("name", "CR2");
		String CRID2 = f.addFeatureToBranch(seasonID, branchID1, feature.toString(), featureID2, sessionToken);
		Assert.assertFalse(CRID2.contains("error"), "Feature2 was not added: " + CRID2);

		//add to branch2
		feature.put("name", "CR3");
		String CRID3 = f.addFeatureToBranch(seasonID, branchID2, feature.toString(), featureID3, sessionToken);
		Assert.assertFalse(CRID3.contains("error"), "Feature3 was not added: " + CRID3);
		
		//add all 3 to analytics
		JSONArray attributes = new JSONArray();
		JSONObject attr1 = new JSONObject();
		attr1.put("name", "color");
		attr1.put("type", "REGULAR");
		attributes.add(attr1);
		JSONObject attr2 = new JSONObject();
		attr2.put("name", "size");
		attr2.put("type", "REGULAR");
		attributes.add(attr2);
		
		String resp = an.addAttributesToAnalytics(featureID1, BranchesRestApi.MASTER, attributes.toString(), sessionToken);
		Assert.assertFalse(resp.contains("error"), "Attributes were not added to analytics in master" + resp);				
		resp = an.addAttributesToAnalytics(featureID2, branchID1, attributes.toString(), sessionToken);
		Assert.assertFalse(resp.contains("error"), "Attributes were not added to analytics in branch1" + resp);				
		resp = an.addAttributesToAnalytics(featureID3, branchID2, attributes.toString(), sessionToken);
		Assert.assertFalse(resp.contains("error"), "Attributes were not added to analytics in branch2" + resp);				

		
		
		JSONObject response = new JSONObject(an.getGlobalDataCollection(seasonID, branchID1, "DISPLAY", sessionToken));
		JSONArray reportedFeatures = response.getJSONObject("analyticsDataCollection").getJSONArray("analyticsDataCollectionByFeatureNames");
		for (int i=0; i< reportedFeatures.size(); i++) {
			if (reportedFeatures.getJSONObject(i).getString("name").equals("ns1.F1"))
				Assert.assertTrue(reportedFeatures.getJSONObject(i).getJSONArray("attributes").getJSONObject(0).getString("branchName").equals("MASTER"), "Incorrect source for F1 attributes");
			else if(reportedFeatures.getJSONObject(i).getString("name").equals("ns1.F2"))
				Assert.assertTrue(reportedFeatures.getJSONObject(i).getJSONArray("attributes").getJSONObject(0).getString("branchName").equals("branch1"), "Incorrect source for F2 attributes");
		}
		
		response = new JSONObject(an.getGlobalDataCollection(seasonID, branchID2, "DISPLAY", sessionToken));
		reportedFeatures = response.getJSONObject("analyticsDataCollection").getJSONArray("analyticsDataCollectionByFeatureNames");
		for (int i=0; i< reportedFeatures.size(); i++) {
			if (reportedFeatures.getJSONObject(i).getString("name").equals("ns1.F1"))
				Assert.assertTrue(reportedFeatures.getJSONObject(i).getJSONArray("attributes").getJSONObject(0).getString("branchName").equals("MASTER"), "Incorrect source for F1 attributes");
			else if(reportedFeatures.getJSONObject(i).getString("name").equals("ns1.F3"))
				Assert.assertTrue(reportedFeatures.getJSONObject(i).getJSONArray("attributes").getJSONObject(0).getString("branchName").equals("branch2"), "Incorrect source for F3 attributes");
		}

	}
	
	
	@Test ( description="Add input field to analytics")
	public void addInputFields() throws Exception{
		String sch = schema.getInputSchema(seasonID, sessionToken);
        JSONObject jsonSchema = new JSONObject(sch);
        String schemaBody = FileUtils.fileToString(filePath + "inputSchemas/inputSchema_optionalField_unitsOfMeasure.txt", "UTF-8", false);
        jsonSchema.put("inputSchema", new JSONObject(schemaBody));
        String response = schema.updateInputSchema(seasonID, jsonSchema.toString(), sessionToken);
        Assert.assertFalse(response.contains("error"), "Schema was not added to the season" + response);
        
		JSONArray inputFields = new JSONArray();
		inputFields.put("context.device.locale");
		an.updateInputFieldToAnalytics(seasonID, BranchesRestApi.MASTER, inputFields.toString(), sessionToken);
		
		inputFields.put("context.device.connectionType");
		response = an.updateInputFieldToAnalytics(seasonID, branchID1, inputFields.toString(), sessionToken);

		inputFields.put("context.device.osVersion");
		response = an.updateInputFieldToAnalytics(seasonID, branchID2, inputFields.toString(), sessionToken);

		JSONObject analytics = new JSONObject(an.getGlobalDataCollection(seasonID, branchID1, "DISPLAY", sessionToken));
		JSONArray reportedFields = analytics.getJSONObject("analyticsDataCollection").getJSONArray("inputFieldsForAnalytics");
		for(int i=0; i<reportedFields.size(); i++){
			if (reportedFields.getJSONObject(i).getString("name").equals("context.device.locale"))
				Assert.assertTrue(reportedFields.getJSONObject(i).getString("branchName").equals("MASTER"));
			else if (reportedFields.getJSONObject(i).getString("name").equals("context.device.connectionType"))
				Assert.assertTrue(reportedFields.getJSONObject(i).getString("branchName").equals("branch1"));

		}
		
		analytics = new JSONObject(an.getGlobalDataCollection(seasonID, branchID2, "DISPLAY", sessionToken));
		reportedFields = analytics.getJSONObject("analyticsDataCollection").getJSONArray("inputFieldsForAnalytics");
		for(int i=0; i<reportedFields.size(); i++){
			if (reportedFields.getJSONObject(i).getString("name").equals("context.device.locale"))
				Assert.assertTrue(reportedFields.getJSONObject(i).getString("branchName").equals("MASTER"));
			else if (reportedFields.getJSONObject(i).getString("name").equals("context.device.osVersion"))
				Assert.assertTrue(reportedFields.getJSONObject(i).getString("branchName").equals("branch2"));

		}
	}


	private String addVariant(String variantName, String branchName) throws IOException, JSONException{
		String variant = FileUtils.fileToString(filePath + "experiments/variant1.txt", "UTF-8", false);
		JSONObject variantJson = new JSONObject(variant);
		variantJson.put("name", variantName);
		variantJson.put("branchName", branchName);
		variantJson.put("stage", "PRODUCTION");
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
