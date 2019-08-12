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
import tests.restapi.InputSchemaRestApi;
import tests.restapi.ProductsRestApi;
import tests.restapi.RuntimeDateUtilities;
import tests.restapi.RuntimeRestApi;

public class AddFeatureToAnalyticsInMaster {
	private String seasonID;
	private String productID;
	private String featureID1;
	private String featureID2;
	private String mixID;
	private String filePath;
	private String m_url;
	private ProductsRestApi p;
	private FeaturesRestApi f;
	private AnalyticsRestApi an;
	private InputSchemaRestApi schema;
	private String sessionToken = "";
	private AirlockUtils baseUtils;
	private BranchesRestApi br ;
	private ExperimentsRestApi exp ;
	private String experimentID;
	private String branchID;
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
	 * - add dev feature1 in master (seen also in branch)
	 * - checkout feature
	 * - add feature mtx to master
	 * - add feature2 to mtx
	 * - report feature2 to analytics in master (not visible in branch)
	 */

	@Test (description="Add components")
	public void addBranch() throws Exception{
		experimentID = addExperiment("experiment."+RandomStringUtils.randomAlphabetic(5));
		Assert.assertFalse(experimentID.contains("error"), "Experiment was not created: " + experimentID);

		branchID = addBranch("branch1");
		Assert.assertFalse(branchID.contains("error"), "Branch1 was not created: " + branchID);

		String feature1 = FileUtils.fileToString(filePath + "feature1.txt", "UTF-8", false); 
		featureID1 = f.addFeature(seasonID, feature1, "ROOT", sessionToken);
		Assert.assertFalse(featureID1.contains("error"), "Feature was not added to the season: " + featureID1);

		String response = br.checkoutFeature(branchID, featureID1, sessionToken);
		Assert.assertFalse(response.contains("error"), "cannot check out feature");

		String featureMix = FileUtils.fileToString(filePath + "feature-mutual.txt", "UTF-8", false);
		mixID = f.addFeature(seasonID, featureMix, featureID1, sessionToken);
		Assert.assertFalse(mixID.contains("error"), "Feature mtx was not added to the season: " + mixID);

		JSONObject feature2 = new JSONObject(FileUtils.fileToString(filePath + "feature2.txt", "UTF-8", false));
		featureID2 = f.addFeature(seasonID, feature2.toString(), mixID, sessionToken);
		Assert.assertFalse(featureID2.contains("error"), "Feature was not added to the season: " + featureID2);
	}


	@Test (dependsOnMethods="addBranch", description="Add features to analytics in master")
	public void addFeaturesToAnalyticsInMaster() throws IOException, JSONException, InterruptedException{
		//add featureID2 to analytics featureOnOff
		String response = an.addFeatureToAnalytics(featureID2, BranchesRestApi.MASTER, sessionToken);
		response = an.getGlobalDataCollection(seasonID, BranchesRestApi.MASTER, "BASIC", sessionToken);
		Assert.assertTrue(featureOnOff(response).size()==1, "Incorrect number of featureOnOff in master");

		response = an.getGlobalDataCollection(seasonID, branchID, "BASIC", sessionToken);
		Assert.assertTrue(featureOnOff(response).size()==0, "Incorrect number of featureOnOff in branch");

		JSONArray featuresInBranch = f.getFeaturesBySeasonFromBranch(seasonID, branchID, sessionToken);

		Assert.assertTrue(featuresInBranch.size()==1, "Incorrect number of features in branch");
		JSONObject feauteObj1 = featuresInBranch.getJSONObject(0);
		Assert.assertTrue(feauteObj1.getString("branchStatus").equals("CHECKED_OUT"), "Incorrect branch status of features in branch");

		Assert.assertTrue(feauteObj1.getJSONArray("features").size()==0, "Incorrect number of features in branch");

		String respWithQuota = an.getGlobalDataCollection(seasonID, branchID, "DISPLAY", sessionToken);
		Assert.assertTrue(getDevelopmentItemsReportedToAnalytics(respWithQuota)==0, "Incorrect number of development items"); 
		Assert.assertTrue(getProductionItemsReportedToAnalytics(respWithQuota)==0, "Incorrect number of production items");

		respWithQuota = an.getGlobalDataCollection(seasonID, "MASTER", "DISPLAY", sessionToken);
		Assert.assertTrue(getDevelopmentItemsReportedToAnalytics(respWithQuota)==1, "Incorrect number of development items"); 
		Assert.assertTrue(getProductionItemsReportedToAnalytics(respWithQuota)==0, "Incorrect number of production items"); 		
	}

	@Test (dependsOnMethods="addFeaturesToAnalyticsInMaster", description="update feature to production in master")
	public void updateFeaturesToProductionInMaster() throws IOException, JSONException, InterruptedException{
		//update featureID2 to production
		String feature2 = f.getFeature(featureID2, sessionToken);
		JSONObject json = new JSONObject(feature2);
		json.put("stage", "PRODUCTION");
		String response = f.updateFeature(seasonID, featureID2, json.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "cannot udate feature2 to production: " + response);

		response = an.getGlobalDataCollection(seasonID, BranchesRestApi.MASTER, "BASIC", sessionToken);
		Assert.assertTrue(featureOnOff(response).size()==1, "Incorrect number of featureOnOff in master");

		response = an.getGlobalDataCollection(seasonID, branchID, "BASIC", sessionToken);
		Assert.assertTrue(featureOnOff(response).size()==0, "Incorrect number of featureOnOff in branch");

		JSONArray featuresInBranch = f.getFeaturesBySeasonFromBranch(seasonID, branchID, sessionToken);

		Assert.assertTrue(featuresInBranch.size()==1, "Incorrect number of features in branch");
		JSONObject feauteObj1 = featuresInBranch.getJSONObject(0);
		Assert.assertTrue(feauteObj1.getString("branchStatus").equals("CHECKED_OUT"), "Incorrect branch status of features in branch");

		Assert.assertTrue(feauteObj1.getJSONArray("features").size()==0, "Incorrect number of features in branch");

		String respWithQuota = an.getGlobalDataCollection(seasonID, branchID, "DISPLAY", sessionToken);
		Assert.assertTrue(getDevelopmentItemsReportedToAnalytics(respWithQuota)==0, "Incorrect number of development items"); 
		Assert.assertTrue(getProductionItemsReportedToAnalytics(respWithQuota)==0, "Incorrect number of production items");

		respWithQuota = an.getGlobalDataCollection(seasonID, "MASTER", "DISPLAY", sessionToken);
		Assert.assertTrue(getDevelopmentItemsReportedToAnalytics(respWithQuota)==1, "Incorrect number of development items"); 
		Assert.assertTrue(getProductionItemsReportedToAnalytics(respWithQuota)==0, "Incorrect number of production items");


		//update featureID1 to production
		String feature1 = f.getFeature(featureID1, sessionToken);
		json = new JSONObject(feature1);
		json.put("stage", "PRODUCTION");
		response = f.updateFeature(seasonID, featureID1, json.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "cannot udate feature1 to production: " + response);

		response = an.getGlobalDataCollection(seasonID, BranchesRestApi.MASTER, "BASIC", sessionToken);
		Assert.assertTrue(featureOnOff(response).size()==1, "Incorrect number of featureOnOff in master");

		response = an.getGlobalDataCollection(seasonID, branchID, "BASIC", sessionToken);
		Assert.assertTrue(featureOnOff(response).size()==0, "Incorrect number of featureOnOff in branch");

		featuresInBranch = f.getFeaturesBySeasonFromBranch(seasonID, branchID, sessionToken);

		Assert.assertTrue(featuresInBranch.size()==1, "Incorrect number of features in branch");
		feauteObj1 = featuresInBranch.getJSONObject(0);
		Assert.assertTrue(feauteObj1.getString("branchStatus").equals("CHECKED_OUT"), "Incorrect branch status of features in branch");

		Assert.assertTrue(feauteObj1.getJSONArray("features").size()==0, "Incorrect number of features in branch");

		respWithQuota = an.getGlobalDataCollection(seasonID, branchID, "DISPLAY", sessionToken);
		Assert.assertTrue(getDevelopmentItemsReportedToAnalytics(respWithQuota)==0, "Incorrect number of development items"); 
		Assert.assertTrue(getProductionItemsReportedToAnalytics(respWithQuota)==0, "Incorrect number of production items");

		respWithQuota = an.getGlobalDataCollection(seasonID, "MASTER", "DISPLAY", sessionToken);
		Assert.assertTrue(getDevelopmentItemsReportedToAnalytics(respWithQuota)==1, "Incorrect number of development items"); 
		Assert.assertTrue(getProductionItemsReportedToAnalytics(respWithQuota)==1, "Incorrect number of production items");
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

	private JSONArray featureOnOff(String analytics) throws JSONException{
		JSONObject json = new JSONObject(analytics);
		return json.getJSONObject("analyticsDataCollection").getJSONArray("featuresAndConfigurationsForAnalytics");
	}



	@AfterTest
	private void reset(){
		baseUtils.reset(productID, sessionToken);
	}
}
