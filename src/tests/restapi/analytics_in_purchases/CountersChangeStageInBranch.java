package tests.restapi.analytics_in_purchases;

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
import tests.restapi.*;

public class CountersChangeStageInBranch {
	protected String seasonID;
	protected String productID;
	protected String entitlementID1;
	protected String featureID2;
	protected String configID1;
	protected String configID2;
	protected String filePath;
	protected String m_url;
	protected ProductsRestApi p;
	protected InAppPurchasesRestApi purchasesApi;
	
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
		purchasesApi = new InAppPurchasesRestApi();
		purchasesApi.setURL(m_url);
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
		- in master add feature in dev & report to analytics, checkout to branch, move in branch to prod, move back to dev
		- in master add CR with attributes in dev  & report to analytics, checkout to branch, move in branch to prod, move back to dev
	 */
	
	@Test ( description="Add branch to the season")
	public void addBranch() throws Exception{
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

		response = an.updateQuota(seasonID, 5, sessionToken);
		Assert.assertFalse(response.contains("error"), "Quota was not returned " + response);
	}

	@Test (dependsOnMethods="addBranch", description="add entitlement in development in master to analytics, checkout to branch and move to production")
	public void addEntitlementToAnalytics() throws Exception{
		String entitlement1 = FileUtils.fileToString(filePath + "purchases/inAppPurchase1.txt", "UTF-8", false);
		entitlementID1 = purchasesApi.addPurchaseItem(seasonID, entitlement1, "ROOT", sessionToken);
		Assert.assertFalse(entitlementID1.contains("error"), "entitlement was not added to the season" + entitlementID1);
		
		String response = an.getGlobalDataCollection(seasonID, branchID, "VERBOSE", sessionToken);
		String input = an.addFeatureOnOff(response, entitlementID1);
		
		response = an.updateGlobalDataCollection(seasonID, BranchesRestApi.MASTER, input, sessionToken);
		Assert.assertFalse(response.contains("error"), "entitlement was not added to analytics" + response);
		
		String respWithQuota = an.getGlobalDataCollection(seasonID, branchID, "DISPLAY", sessionToken);
		Assert.assertTrue(getDevelopmentItemsReportedToAnalytics(respWithQuota)==1, "Incorrect number of development items");
		Assert.assertTrue(getProductionItemsReportedToAnalytics(respWithQuota)==0, "Incorrect number of production items");

		
		response = br.checkoutFeature(branchID, entitlementID1, sessionToken);
		Assert.assertFalse(response.contains("error"), "Fail checkoput entitlement: " + response);
		
		//move entitlement to production in branch
		entitlement1 = purchasesApi.getPurchaseItemFromBranch(entitlementID1, branchID, sessionToken);
		JSONObject jsonF1 = new JSONObject(entitlement1);
	    jsonF1.put("stage", "PRODUCTION");
		entitlementID1 = purchasesApi.updatePurchaseItemInBranch(seasonID, branchID, entitlementID1, jsonF1.toString(), sessionToken);
		Assert.assertFalse(entitlementID1.contains("error"), "entitlement was not updated in branch " + entitlementID1);
		respWithQuota = an.getGlobalDataCollection(seasonID, branchID, "DISPLAY", sessionToken);
		Assert.assertTrue(getDevelopmentItemsReportedToAnalytics(respWithQuota)==1, "Incorrect number of development items");
		Assert.assertTrue(getProductionItemsReportedToAnalytics(respWithQuota)==1, "Incorrect number of production items");
		
		//move entitlement to development in branch
		entitlement1 = purchasesApi.getPurchaseItemFromBranch(entitlementID1, branchID, sessionToken);
		jsonF1 = new JSONObject(entitlement1);
	    jsonF1.put("stage", "DEVELOPMENT");
		entitlementID1 = purchasesApi.updatePurchaseItemInBranch(seasonID, branchID, entitlementID1, jsonF1.toString(), sessionToken);
		Assert.assertFalse(entitlementID1.contains("error"), "entitlement was not updated in branch " + entitlementID1);
		respWithQuota = an.getGlobalDataCollection(seasonID, branchID, "DISPLAY", sessionToken);
		Assert.assertTrue(getDevelopmentItemsReportedToAnalytics(respWithQuota)==1, "Incorrect number of development items");
		Assert.assertTrue(getProductionItemsReportedToAnalytics(respWithQuota)==0, "Incorrect number of production items");
	}
	
	
	@Test (dependsOnMethods="addEntitlementToAnalytics", description="add feature attributes in development in master to analytics, checkout to branch and move to production")
	public void addAttributesToAnalytics() throws Exception{
		//cancel checkout to add configuration and then checkout again with configuration rule
		String response = br.cancelCheckoutFeature(branchID, entitlementID1, sessionToken);
		Assert.assertFalse(response.contains("error"), "Fail cancel checkoput entitlement: " + response);
		
		//add configuration and attributes to analytics
		//add attribute
		String config = FileUtils.fileToString(filePath + "configuration_rule1.txt", "UTF-8", false);
		JSONObject jsonConfig = new JSONObject(config);
		JSONObject newConfiguration = new JSONObject();
		newConfiguration.put("color", "red");
		newConfiguration.put("size", "small");
		jsonConfig.put("configuration", newConfiguration);
		configID1 = purchasesApi.addPurchaseItem(seasonID,  jsonConfig.toString(), entitlementID1, sessionToken);
		Assert.assertFalse(configID1.contains("error"), "Configuration1 was not added to the season" + configID1);		

		JSONArray attributes = new JSONArray();
		JSONObject attr1 = new JSONObject();
		attr1.put("name", "color");
		attr1.put("type", "REGULAR");
		attributes.add(attr1);
		JSONObject attr2 = new JSONObject();
		attr2.put("name", "size");
		attr2.put("type", "REGULAR");
		attributes.add(attr2);
		response = an.getGlobalDataCollection(seasonID, "MASTER", "BASIC", sessionToken);
		String input = an.addFeaturesAttributesToAnalytics(response, entitlementID1, attributes);
		response = an.updateGlobalDataCollection(seasonID, "MASTER", input, sessionToken);		
		Assert.assertFalse(response.contains("error"), "Attributes were not added to analytics " + response);
		
        String respWithQuota = an.getGlobalDataCollection(seasonID, BranchesRestApi.MASTER, "DISPLAY", sessionToken);
		Assert.assertTrue(getDevelopmentItemsReportedToAnalytics(respWithQuota)==3, "Incorrect number of development items"); //feature+2 attributes
		Assert.assertTrue(getProductionItemsReportedToAnalytics(respWithQuota)==0, "Incorrect number of production items");	

		br.checkoutFeature(branchID, entitlementID1, sessionToken);
		
		//move feature to prod in branch, attributes should be added to production count
		String feature1 = purchasesApi.getPurchaseItemFromBranch(entitlementID1, branchID, sessionToken);
	    JSONObject jsonF1 = new JSONObject(feature1);
	    jsonF1.put("stage", "PRODUCTION");
		entitlementID1 = purchasesApi.updatePurchaseItemInBranch(seasonID, branchID, entitlementID1, jsonF1.toString(), sessionToken);
		
		String cr1 = purchasesApi.getPurchaseItemFromBranch(configID1, branchID, sessionToken);
	    JSONObject jsonCR1 = new JSONObject(cr1);
	    jsonCR1.put("stage", "PRODUCTION");
	    configID1 = purchasesApi.updatePurchaseItemInBranch(seasonID, branchID, configID1, jsonCR1.toString(), sessionToken);

	    respWithQuota = an.getGlobalDataCollection(seasonID, branchID, "DISPLAY", sessionToken);
		Assert.assertTrue(getDevelopmentItemsReportedToAnalytics(respWithQuota)==3, "Incorrect number of development items"); //entitlement+2 attributes
		Assert.assertTrue(getProductionItemsReportedToAnalytics(respWithQuota)==3, "Incorrect number of production items"); //entitlement+2 attributes

	}
	
	@Test (dependsOnMethods="addAttributesToAnalytics", description="move feature to production, change 1 attributes name (update configuration)")
	public void updateConfiguration() throws Exception{

		//update configuration
		String configuration = purchasesApi.getPurchaseItemFromBranch(configID1, branchID, sessionToken);

		JSONObject jsonConfig = new JSONObject(configuration);
		JSONObject newConfiguration = new JSONObject();
		newConfiguration.put("color", "white");
		newConfiguration.put("newsize", "medium");
		jsonConfig.put("configuration", newConfiguration);
		jsonConfig.put("rolloutPercentage", 50);
		String response = purchasesApi.updatePurchaseItemInBranch(seasonID, branchID, configID1, jsonConfig.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Configuration was not updated " + response);
						
		//We are not changing analytics if an attribute was changed. We are adding a warning unless the missing attribute is reported in 
		//the master while we are looking in the branch
		String respWithQuota = an.getGlobalDataCollection(seasonID, branchID, "DISPLAY", sessionToken);
		Assert.assertTrue(getDevelopmentItemsReportedToAnalytics(respWithQuota)==3, "Incorrect number of development items"); //feature+1 attributes
		Assert.assertTrue(getProductionItemsReportedToAnalytics(respWithQuota)==3, "Incorrect number of production items");	//1 attribute+feature
	}
	
	@Test (dependsOnMethods="updateConfiguration", description="Remove configuration with attributes from master")
	public void deleteConfigurationInMaster() throws Exception{
  
		int response = purchasesApi.deletePurchaseItem(configID1,  sessionToken);
		Assert.assertTrue(response == 200, "Configuration was not deleted from master " + response);
				
		//we checked out after adding to analytics so remains in branch
        String respWithQuota = an.getGlobalDataCollection(seasonID, branchID, "DISPLAY", sessionToken);
		Assert.assertTrue(getDevelopmentItemsReportedToAnalytics(respWithQuota)==3, "Incorrect number of development items"); //feature+attribute
		Assert.assertTrue(getProductionItemsReportedToAnalytics(respWithQuota)==3, "Incorrect number of production items"); //feature+attribute
		
		//we are not removing attributes even when the cr is deleted we are only adding a warning
		String masterRespWithQuota = an.getGlobalDataCollection(seasonID, "MASTER", "DISPLAY", sessionToken);
		Assert.assertTrue(getDevelopmentItemsReportedToAnalytics(masterRespWithQuota)==3, "Incorrect number of development items"); //feature
		Assert.assertTrue(getProductionItemsReportedToAnalytics(masterRespWithQuota)==0, "Incorrect number of production items"); //feature

	}
	
	@Test (dependsOnMethods="deleteConfigurationInMaster", description="Delete feature from master")
	public void deleteEntitlementFromMaster() throws Exception{
		int response = purchasesApi.deletePurchaseItem(entitlementID1,  sessionToken);
		Assert.assertTrue(response == 200, "entitlement was not deleted from master " + response);
		
        String respWithQuota = an.getGlobalDataCollection(seasonID, branchID, "DISPLAY", sessionToken);
		Assert.assertTrue(getDevelopmentItemsReportedToAnalytics(respWithQuota)==3, "Incorrect number of development items"); //feature+attribute
		Assert.assertTrue(getProductionItemsReportedToAnalytics(respWithQuota)==3, "Incorrect number of production items");//feature+attribute
		
		String masterRespWithQuota = an.getGlobalDataCollection(seasonID, "MASTER", "DISPLAY", sessionToken);
		Assert.assertTrue(getDevelopmentItemsReportedToAnalytics(masterRespWithQuota)==0, "Incorrect number of development items"); //feature
		Assert.assertTrue(getProductionItemsReportedToAnalytics(masterRespWithQuota)==0, "Incorrect number of production items"); //feature
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
		return baseUtils.addExperiment(m_analyticsUrl, true, false);
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
