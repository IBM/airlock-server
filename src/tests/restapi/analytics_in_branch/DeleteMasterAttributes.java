package tests.restapi.analytics_in_branch;

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
import tests.restapi.ProductsRestApi;


public class DeleteMasterAttributes {
	protected String seasonID;
	protected String productID;
	protected String featureID1;
	private String featureID2;
	protected String configID1;
	protected String configID2;
	protected String filePath;
	protected String m_url;
	protected ProductsRestApi p;
	protected FeaturesRestApi f;
	protected AnalyticsRestApi an;
	private String sessionToken = "";
	protected AirlockUtils baseUtils;
	private BranchesRestApi br ;
	private ExperimentsRestApi exp ;
	private String experimentID;
	private String branchID;
	private String variantID;
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
	- in master add CR with 2 attributes, report to analytics, checkout to branch and delete CR. Attributes are still reported
	 */
	
	@Test (description="Add feature and 2 configuration rules to the season")
	public void addComponents() throws Exception{
		experimentID = addExperiment("experiment."+RandomStringUtils.randomAlphabetic(5));
		Assert.assertFalse(experimentID.contains("error"), "Experiment was not created: " + experimentID);
		
		branchID = addBranch("branch1");
		Assert.assertFalse(branchID.contains("error"), "Branch1 was not created: " + branchID);

		variantID = addVariant("variant1", "branch1");
		Assert.assertFalse(variantID.contains("error"), "Variant1 was not created: " + variantID);

		//enable experiment
		String airlockExperiment = exp.getExperiment(experimentID, sessionToken);
		Assert.assertFalse(airlockExperiment.contains("error"), "Experiment was not found: " + experimentID);

		JSONObject expJson = new JSONObject(airlockExperiment);
		expJson.put("enabled", true);
		
		String response = exp.updateExperiment(experimentID, expJson.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Experiment was not updated: " + response);		


		String feature = FileUtils.fileToString(filePath + "feature1.txt", "UTF-8", false);
		featureID1 = f.addFeature(seasonID, feature, "ROOT", sessionToken);
		Assert.assertFalse(featureID1.contains("error"), "feature1 was not added to the season" + featureID1);
		
		//add first configuration
		String config = FileUtils.fileToString(filePath + "configuration_rule1.txt", "UTF-8", false);
		JSONObject jsonConfig = new JSONObject(config);
		JSONObject newConfiguration = new JSONObject();
		newConfiguration.put("color", "red");
		newConfiguration.put("size", "small");
		jsonConfig.put("configuration", newConfiguration);
		configID1 = f.addFeature(seasonID, jsonConfig.toString(), featureID1, sessionToken);
		Assert.assertFalse(configID1.contains("error"), "Configuration1 was not added to the season" + configID1);
		
		//add second configuration
		config = FileUtils.fileToString(filePath + "configuration_rule2.txt", "UTF-8", false);
		jsonConfig = new JSONObject(config);
		newConfiguration = new JSONObject();
		newConfiguration.put("title", "test");
		jsonConfig.put("configuration", newConfiguration);
		configID2 = f.addFeature(seasonID, jsonConfig.toString(), featureID1, sessionToken);
		Assert.assertFalse(configID2.contains("error"), "Configuration1 was not added to the season" + configID2);

	}
	

	@Test (dependsOnMethods="addComponents", description="Add attributes to analytics in master")
	public void addAttributesToAnalytics() throws IOException, JSONException, InterruptedException{
		//add  attribute to analytics
		JSONArray attributes = new JSONArray();
		JSONObject attr1 = new JSONObject();
		attr1.put("name", "color");
		attr1.put("type", "REGULAR");
		attributes.add(attr1);
		JSONObject attr2 = new JSONObject();
		attr2.put("name", "size");
		attr2.put("type", "REGULAR");
		attributes.add(attr2);

		JSONObject attr3 = new JSONObject();
		attr3.put("name", "title");
		attr3.put("type", "REGULAR");
		attributes.add(attr3);
		
		String response = an.addAttributesToAnalytics(featureID1, BranchesRestApi.MASTER, attributes.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Attributes were not added to analytics" + response);		
				
		br.checkoutFeature(branchID, featureID1, sessionToken);
		
		String anResponseInBranch = an.getGlobalDataCollection(seasonID, branchID, "BASIC", sessionToken);
		Assert.assertTrue(validateAttributeInAnalytics(anResponseInBranch)==3, "Incorrect number of attributes in analytics in branch");

			
	}
	
	@Test (dependsOnMethods="addAttributesToAnalytics", description="Delete configuration in master")
	public void deleteConfiguration() throws IOException, JSONException, InterruptedException{
		int code = f.deleteFeature(configID2, sessionToken);
		Assert.assertTrue(code==200, "Configuration was not deleted");
		
		String anResponse = an.getGlobalDataCollection(seasonID, BranchesRestApi.MASTER, "BASIC", sessionToken);
		Assert.assertTrue(validateAttributeInAnalytics(anResponse)==3, "Incorrect number of attributes in analytics in master");
		
		String anResponseInBranch = an.getGlobalDataCollection(seasonID, branchID, "BASIC", sessionToken);
		Assert.assertTrue(validateAttributeInAnalytics(anResponseInBranch)==3, "Incorrect number of attributes in analytics in branch");
			
	}

	private int validateAttributeInAnalytics(String analytics) throws JSONException{
		//runtime dev/prod
		int attributes=0;
		JSONObject json = new JSONObject(analytics);
		JSONObject analyticsDataCollection = json.getJSONObject("analyticsDataCollection");
		JSONArray featuresAttributesToAnalytics = analyticsDataCollection.getJSONArray("featuresAttributesForAnalytics");
		for(int i=0; i<featuresAttributesToAnalytics.size(); i++){
			attributes = attributes + featuresAttributesToAnalytics.getJSONObject(i).getJSONArray("attributes").size();
		}	
		return attributes;
	}
	
	private String addExperiment(String experimentName) throws IOException, JSONException{
		return baseUtils.addExperiment(m_analyticsUrl, false, false);
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


	@AfterTest
	private void reset(){
		baseUtils.reset(productID, sessionToken);
	}
}
