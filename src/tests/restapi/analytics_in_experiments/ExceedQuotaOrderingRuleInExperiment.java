package tests.restapi.analytics_in_experiments;

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
import tests.restapi.*;

public class ExceedQuotaOrderingRuleInExperiment {
	protected String seasonID;
	protected String branchID;
	protected String productID;
	protected String featureID1;
	protected String featureID2;
	protected String ORId1;
	protected String ORId2;
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
	
	//F1->F2, OR1
	@Test (description="Set quota to 3")
	public void updateQuota() throws IOException, JSONException, InterruptedException{
		String response = an.updateQuota(seasonID, 3, sessionToken);
		Assert.assertFalse(response.contains("error"), "Quota was not returned " + response);				
	}
	
	
	@Test ( dependsOnMethods="updateQuota", description="Add branch to the season")
	public void addBranch() throws Exception{
		experimentID = baseUtils.addExperiment(m_analyticsUrl, true, false);
		Assert.assertFalse(experimentID.contains("error"), "Experiment was not created: " + experimentID);
		
		branchID = addBranch("branch1");
		Assert.assertFalse(branchID.contains("error"), "Branch1 was not created: " + branchID);

		variantID = addVariant("variant1", "branch1");
		Assert.assertFalse(variantID.contains("error"), "Variant1 was not created: " + variantID);
		
		//enable experiment so a range will be created and the experiment will be published to analytics server
		String airlockExperiment = exp.getExperiment(experimentID, sessionToken);
		Assert.assertFalse(airlockExperiment.contains("error"), "Experiment was not found: " + experimentID);

		JSONObject expJson = new JSONObject(airlockExperiment);
		expJson.put("enabled", true);
		
		String response = exp.updateExperiment(experimentID, expJson.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Experiment was not updated: " + response);		

	}

	@Test (dependsOnMethods="addBranch", description="add features to analytics")
	public void addProdFeaturesToAnalytics() throws Exception{
		
		JSONObject feature =  new JSONObject(FileUtils.fileToString(filePath + "feature1.txt", "UTF-8", false))  ;
		
		feature.put("name", RandomStringUtils.randomAlphabetic(5));
		feature.put("stage", "PRODUCTION");
		featureID1 = f.addFeatureToBranch(seasonID, branchID, feature.toString(), "ROOT", sessionToken);
		Assert.assertFalse(featureID1.contains("error"), "feature was not added to the season" + featureID1);
		
		feature.put("name", RandomStringUtils.randomAlphabetic(5));
		feature.put("stage", "PRODUCTION");
		featureID2 = f.addFeatureToBranch(seasonID, branchID, feature.toString(), featureID1, sessionToken);
		Assert.assertFalse(featureID2.contains("error"), "feature2 was not added to the season" + featureID2);


		String orderingRule = FileUtils.fileToString(filePath + "orderingRule/orderingRule1.txt", "UTF-8", false);
		JSONObject jsonOR = new JSONObject(orderingRule);
		jsonOR.put("name", RandomStringUtils.randomAlphabetic(5));
		String configuration = "{\"" + featureID2 + "\": 1.5 }";
		jsonOR.put("configuration", configuration);
		ORId1 = f.addFeatureToBranch(seasonID, branchID, jsonOR.toString(), featureID1, sessionToken);
		Assert.assertFalse(ORId1.contains("error"), "Configuration1 was not added to the season" + ORId1);

		
		//report F2 to analytics - 1 production item
		String response = an.addFeatureToAnalytics(featureID2, branchID, sessionToken);
		Assert.assertFalse(response.contains("error"), "Feature2 was not added to analytics" + response);

		//report F1 to analytics - 2 production items
		response = an.addFeatureToAnalytics(featureID1, branchID, sessionToken);
		Assert.assertFalse(response.contains("error"), "Parent feature was not added to analytics" + response);
	
		//report OR1 to analytics - 
		response = an.addFeatureToAnalytics(ORId1, branchID, sessionToken);
		Assert.assertFalse(response.contains("error"), "ORId1 was not added to analytics" + response);
		
		String respWithQuota = an.getGlobalDataCollection(seasonID, branchID, "DISPLAY", sessionToken);
		Assert.assertTrue(getDevelopmentItemsReportedToAnalytics(respWithQuota)==4, "Incorrect number of development items");
		Assert.assertTrue(getProductionItemsReportedToAnalytics(respWithQuota)==3, "Incorrect number of production items");
	}
	
	@Test (dependsOnMethods="addProdFeaturesToAnalytics", description="Move ordering rule to production in branch - exceeds the quota")
	public void moveOrderingRuleToProdInBranch() throws JSONException, IOException{
		//checkout parent feature to branch
		br.checkoutFeature(branchID, featureID1, sessionToken);
		
		String orderingRule = f.getFeatureFromBranch(ORId1, branchID, sessionToken);
		JSONObject jsonOR2 = new JSONObject(orderingRule);
		jsonOR2.put("stage", "PRODUCTION");
		String response = f.updateFeatureInBranch(seasonID, branchID, ORId1, jsonOR2.toString(), sessionToken);
		Assert.assertTrue(response.contains("error"), "Moved ordering rule to production when quota limit is reached");
		
		String respWithQuota = an.getGlobalDataCollection(seasonID, branchID, "DISPLAY", sessionToken);
		Assert.assertTrue(getDevelopmentItemsReportedToAnalytics(respWithQuota)==4, "Incorrect number of development items");
		Assert.assertTrue(getProductionItemsReportedToAnalytics(respWithQuota)==3, "Incorrect number of production items");
		
		//remove OR1 from analytics in branch
		response = an.deleteFeatureFromAnalytics(ORId1, branchID, sessionToken);
		Assert.assertFalse(response.contains("error"), "Can't remove ordering rule from analytics in branch");
		
		respWithQuota = an.getGlobalDataCollection(seasonID, branchID, "DISPLAY", sessionToken);
		Assert.assertTrue(getDevelopmentItemsReportedToAnalytics(respWithQuota)==3, "Incorrect number of development items");
		Assert.assertTrue(getProductionItemsReportedToAnalytics(respWithQuota)==3, "Incorrect number of production items");
		
		//move OR1 to production
		response = f.updateFeatureInBranch(seasonID, branchID, ORId1, jsonOR2.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Can't move ordering rule to production");


	}
	
	
	@Test (dependsOnMethods="moveOrderingRuleToProdInBranch", description="Add dev ordering rule in branch")
	public void addOrderingRuleInBranch() throws JSONException, IOException{
		//add ordering rule in dev stage
		String orderingRule = FileUtils.fileToString(filePath + "orderingRule/orderingRule1.txt", "UTF-8", false);
		JSONObject jsonOR = new JSONObject(orderingRule);
		jsonOR.put("name", RandomStringUtils.randomAlphabetic(5));
		String configuration = "{\"" + featureID2 + "\": 1.5 }";
		jsonOR.put("configuration", configuration);
		jsonOR.put("stage", "PRODUCTION");
		ORId2 = f.addFeatureToBranch(seasonID, branchID, jsonOR.toString(), ORId1, sessionToken);
		Assert.assertFalse(ORId2.contains("error"), "Ordering rule2 was not added to the season" + ORId2);
		
		//report OR2 to analytics in branch
		String response = an.addFeatureToAnalytics(ORId2, branchID, sessionToken);
		Assert.assertTrue(response.contains("error"), "Ordering rule was added to analytics and quota was exceeded " + response);

		String respWithQuota = an.getGlobalDataCollection(seasonID, branchID, "DISPLAY", sessionToken);
		
		Assert.assertTrue(getDevelopmentItemsReportedToAnalytics(respWithQuota)==3, "Incorrect number of development items");
		Assert.assertTrue(getProductionItemsReportedToAnalytics(respWithQuota)==3, "Incorrect number of production items");

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
