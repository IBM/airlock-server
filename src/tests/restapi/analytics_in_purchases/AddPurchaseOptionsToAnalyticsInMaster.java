package tests.restapi.analytics_in_purchases;

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
import tests.restapi.InAppPurchasesRestApi;
import tests.restapi.InputSchemaRestApi;
import tests.restapi.ProductsRestApi;
import tests.restapi.RuntimeDateUtilities;
import tests.restapi.RuntimeRestApi;

public class AddPurchaseOptionsToAnalyticsInMaster {
	private String seasonID;
	private String productID;
	private String entitlementID1;
	private String entitlementID2;
	private String entitlementMixID;
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
	private InAppPurchasesRestApi purchasesApi;
	private String experimentID;
	private String branchID;
	private String m_analyticsUrl;
	private String purchaseOptionsID;
	private String purchaseOptionsMixID;
	private String featureID1;
	private String featureID2;
	private String featureMixID;
	

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
		purchasesApi = new InAppPurchasesRestApi();
		purchasesApi.setURL(m_url);

		baseUtils = new AirlockUtils(url, analyticsUrl, translationsUrl, configPath, sToken, userName, userPassword, appName, productsToDeleteFile);
		sessionToken = baseUtils.sessionToken;
		productID = baseUtils.createProduct();
		baseUtils.printProductToFile(productID);
		seasonID = baseUtils.createSeason(productID);
	}

	/*
	 * - add dev entitlement in master (seen also in branch)
	 * - checkout entitlement
	 * - add entitlement mtx to master
	 * - add entitlement2 to mtx
	 * - report entitlement2 to analytics in master (not visible in branch)
	 */

	@Test (description="Add components")
	public void addBranch() throws Exception{
		experimentID = addExperiment("experiment."+RandomStringUtils.randomAlphabetic(5));
		Assert.assertFalse(experimentID.contains("error"), "Experiment was not created: " + experimentID);

		branchID = addBranch("branch1");
		Assert.assertFalse(branchID.contains("error"), "Branch1 was not created: " + branchID);

		String entitlement1 = FileUtils.fileToString(filePath + "purchases/inAppPurchase1.txt", "UTF-8", false); 
		entitlementID1 = purchasesApi.addPurchaseItem(seasonID, entitlement1, "ROOT", sessionToken);
		Assert.assertFalse(entitlementID1.contains("error"), "entitlement was not added to the season: " + entitlementID1);

		String response = br.checkoutFeature(branchID, entitlementID1, sessionToken);
		Assert.assertFalse(response.contains("error"), "cannot check out entitlement");

		String entitlementMix = FileUtils.fileToString(filePath + "purchases/inAppPurchaseMutual.txt", "UTF-8", false);
		entitlementMixID = purchasesApi.addPurchaseItem(seasonID, entitlementMix, entitlementID1, sessionToken);
		Assert.assertFalse(entitlementMixID.contains("error"), "entitlements mtx was not added to the season: " + entitlementMixID);

		String entitlement2 = FileUtils.fileToString(filePath + "purchases/inAppPurchase2.txt", "UTF-8", false);
		entitlementID2 = purchasesApi.addPurchaseItem(seasonID, entitlement2, entitlementMixID, sessionToken);
		Assert.assertFalse(entitlementID2.contains("error"), "entitlement was not added to the season: " + entitlementID2);
	}

	@Test (dependsOnMethods="addBranch", description="Add entitlements to analytics in master")
	public void addEntitlementsToAnalyticsInMaster() throws IOException, JSONException, InterruptedException{
		//add entitlementID2 to analytics featureOnOff
		String response = an.addFeatureToAnalytics(entitlementID2, BranchesRestApi.MASTER, sessionToken);
		response = an.getGlobalDataCollection(seasonID, BranchesRestApi.MASTER, "BASIC", sessionToken);
		Assert.assertTrue(featureOnOff(response).size()==1, "Incorrect number of featureOnOff in master");

		response = an.getGlobalDataCollection(seasonID, branchID, "BASIC", sessionToken);
		Assert.assertTrue(featureOnOff(response).size()==0, "Incorrect number of featureOnOff in branch");

		JSONArray purchasesInBranch = purchasesApi.getPurchasesBySeasonFromBranch(seasonID, branchID, sessionToken);

		Assert.assertTrue(purchasesInBranch.size()==1, "Incorrect number of features in branch");
		JSONObject entitlementObj1 = purchasesInBranch.getJSONObject(0);
		Assert.assertTrue(entitlementObj1.getString("branchStatus").equals("CHECKED_OUT"), "Incorrect branch status of entitlement in branch");

		Assert.assertTrue(entitlementObj1.getJSONArray("entitlements").size()==0, "Incorrect number of features in branch");

		String respWithQuota = an.getGlobalDataCollection(seasonID, branchID, "DISPLAY", sessionToken);
		Assert.assertTrue(getDevelopmentItemsReportedToAnalytics(respWithQuota)==0, "Incorrect number of development items"); 
		Assert.assertTrue(getProductionItemsReportedToAnalytics(respWithQuota)==0, "Incorrect number of production items");

		respWithQuota = an.getGlobalDataCollection(seasonID, "MASTER", "DISPLAY", sessionToken);
		Assert.assertTrue(getDevelopmentItemsReportedToAnalytics(respWithQuota)==1, "Incorrect number of development items"); 
		Assert.assertTrue(getProductionItemsReportedToAnalytics(respWithQuota)==0, "Incorrect number of production items"); 		
	}

	@Test (dependsOnMethods="addEntitlementsToAnalyticsInMaster", description="update entitlement to production in master")
	public void updateEntitlementsToProductionInMaster() throws IOException, JSONException, InterruptedException{
		//update entitlementID2 to analytics featureOnOff
		String entitlement2 = purchasesApi.getPurchaseItem(entitlementID2, sessionToken);
		JSONObject json = new JSONObject(entitlement2);
		json.put("stage", "PRODUCTION");
		String response = purchasesApi.updatePurchaseItem(seasonID, entitlementID2, json.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "cannot udate entitlement2 to production: " + response);

		response = an.getGlobalDataCollection(seasonID, BranchesRestApi.MASTER, "BASIC", sessionToken);
		Assert.assertTrue(featureOnOff(response).size()==1, "Incorrect number of featureOnOff in master");

		response = an.getGlobalDataCollection(seasonID, branchID, "BASIC", sessionToken);
		Assert.assertTrue(featureOnOff(response).size()==0, "Incorrect number of featureOnOff in branch");

		JSONArray purchasesInBranch = purchasesApi.getPurchasesBySeasonFromBranch(seasonID, branchID, sessionToken);

		Assert.assertTrue(purchasesInBranch.size()==1, "Incorrect number of entitlements in branch");
		JSONObject entitlementObj1 = purchasesInBranch.getJSONObject(0);
		Assert.assertTrue(entitlementObj1.getString("branchStatus").equals("CHECKED_OUT"), "Incorrect branch status of entitlements in branch");

		Assert.assertTrue(entitlementObj1.getJSONArray("entitlements").size()==0, "Incorrect number of features in branch");

		String respWithQuota = an.getGlobalDataCollection(seasonID, branchID, "DISPLAY", sessionToken);
		Assert.assertTrue(getDevelopmentItemsReportedToAnalytics(respWithQuota)==0, "Incorrect number of development items"); 
		Assert.assertTrue(getProductionItemsReportedToAnalytics(respWithQuota)==0, "Incorrect number of production items");

		respWithQuota = an.getGlobalDataCollection(seasonID, "MASTER", "DISPLAY", sessionToken);
		Assert.assertTrue(getDevelopmentItemsReportedToAnalytics(respWithQuota)==1, "Incorrect number of development items"); 
		Assert.assertTrue(getProductionItemsReportedToAnalytics(respWithQuota)==0, "Incorrect number of production items");

		//update entitlementID1 to production
		String entitlement1 = purchasesApi.getPurchaseItem(entitlementID1, sessionToken);
		json = new JSONObject(entitlement1);
		json.put("stage", "PRODUCTION");
		response = purchasesApi.updatePurchaseItem(seasonID, entitlementID1, json.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "cannot udate entitlement1 to production: " + response);

		response = an.getGlobalDataCollection(seasonID, BranchesRestApi.MASTER, "BASIC", sessionToken);
		Assert.assertTrue(featureOnOff(response).size()==1, "Incorrect number of featureOnOff in master");

		response = an.getGlobalDataCollection(seasonID, branchID, "BASIC", sessionToken);
		Assert.assertTrue(featureOnOff(response).size()==0, "Incorrect number of featureOnOff in branch");

		purchasesInBranch = purchasesApi.getPurchasesBySeasonFromBranch(seasonID, branchID, sessionToken);

		Assert.assertTrue(purchasesInBranch.size()==1, "Incorrect number of entitlements in branch");
		entitlementObj1 = purchasesInBranch.getJSONObject(0);
		Assert.assertTrue(entitlementObj1.getString("branchStatus").equals("CHECKED_OUT"), "Incorrect branch status of features in branch");

		Assert.assertTrue(entitlementObj1.getJSONArray("entitlements").size()==0, "Incorrect number of entitlements in branch");

		respWithQuota = an.getGlobalDataCollection(seasonID, branchID, "DISPLAY", sessionToken);
		Assert.assertTrue(getDevelopmentItemsReportedToAnalytics(respWithQuota)==0, "Incorrect number of development items"); 
		Assert.assertTrue(getProductionItemsReportedToAnalytics(respWithQuota)==0, "Incorrect number of production items");

		respWithQuota = an.getGlobalDataCollection(seasonID, "MASTER", "DISPLAY", sessionToken);
		Assert.assertTrue(getDevelopmentItemsReportedToAnalytics(respWithQuota)==1, "Incorrect number of development items"); 
		Assert.assertTrue(getProductionItemsReportedToAnalytics(respWithQuota)==1, "Incorrect number of production items");
	}

	@Test (dependsOnMethods="updateEntitlementsToProductionInMaster", description="Add purchaseOptions mtx -> purchaseOptions to master under checked out entitlement")
	public void addPurcahseOptionsToMaster() throws Exception{
		String purchaseOptionsMix = FileUtils.fileToString(filePath + "purchases/purchaseOptionsMutual.txt", "UTF-8", false);
		purchaseOptionsMixID = purchasesApi.addPurchaseItem(seasonID, purchaseOptionsMix, entitlementID1, sessionToken);
		Assert.assertFalse(purchaseOptionsMixID.contains("error"), "purchaseOptions mtx was not added to the season: " + purchaseOptionsMixID);

		String purchaseOptions = FileUtils.fileToString(filePath + "purchases/purchaseOptions1.txt", "UTF-8", false);
		purchaseOptionsID = purchasesApi.addPurchaseItem(seasonID, purchaseOptions, purchaseOptionsMixID, sessionToken);
		Assert.assertFalse(purchaseOptionsID.contains("error"), "purchaseOptions was not added to the season: " + purchaseOptionsID);
	}
	
	@Test (dependsOnMethods="addPurcahseOptionsToMaster", description="Add purchaseOptions to analytics in master")
	public void addPurchaseOptionsToAnalyticsInMaster() throws IOException, JSONException, InterruptedException{
		//add entitlementID2 to analytics featureOnOff
		String response = an.addFeatureToAnalytics(purchaseOptionsID, BranchesRestApi.MASTER, sessionToken);
		response = an.getGlobalDataCollection(seasonID, BranchesRestApi.MASTER, "BASIC", sessionToken);
		Assert.assertTrue(featureOnOff(response).size()==2, "Incorrect number of featureOnOff in master");

		response = an.getGlobalDataCollection(seasonID, branchID, "BASIC", sessionToken);
		Assert.assertTrue(featureOnOff(response).size()==0, "Incorrect number of featureOnOff in branch");

		String respWithQuota = an.getGlobalDataCollection(seasonID, branchID, "DISPLAY", sessionToken);
		Assert.assertTrue(getDevelopmentItemsReportedToAnalytics(respWithQuota)==0, "Incorrect number of development items"); 
		Assert.assertTrue(getProductionItemsReportedToAnalytics(respWithQuota)==0, "Incorrect number of production items");

		respWithQuota = an.getGlobalDataCollection(seasonID, "MASTER", "DISPLAY", sessionToken);
		Assert.assertTrue(getDevelopmentItemsReportedToAnalytics(respWithQuota)==2, "Incorrect number of development items"); 
		Assert.assertTrue(getProductionItemsReportedToAnalytics(respWithQuota)==1, "Incorrect number of production items"); 		
	}

	@Test (dependsOnMethods="addPurchaseOptionsToAnalyticsInMaster", description="update purchaseOptions to production in master")
	public void updatePurchaseOptionsToProductionInMaster() throws IOException, JSONException, InterruptedException{
		//update purchaseOptions to production
		String purchaseOptions2 = purchasesApi.getPurchaseItem(purchaseOptionsID, sessionToken);
		JSONObject json = new JSONObject(purchaseOptions2);
		json.put("stage", "PRODUCTION");
		String response = purchasesApi.updatePurchaseItem(seasonID, purchaseOptionsID, json.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "cannot udate purchaseOptions to production: " + response);

		response = an.getGlobalDataCollection(seasonID, BranchesRestApi.MASTER, "BASIC", sessionToken);
		Assert.assertTrue(featureOnOff(response).size()==2, "Incorrect number of featureOnOff in master");

		response = an.getGlobalDataCollection(seasonID, branchID, "BASIC", sessionToken);
		Assert.assertTrue(featureOnOff(response).size()==0, "Incorrect number of featureOnOff in branch");
		
		String respWithQuota = an.getGlobalDataCollection(seasonID, branchID, "DISPLAY", sessionToken);
		Assert.assertTrue(getDevelopmentItemsReportedToAnalytics(respWithQuota)==0, "Incorrect number of development items"); 
		Assert.assertTrue(getProductionItemsReportedToAnalytics(respWithQuota)==0, "Incorrect number of production items");

		respWithQuota = an.getGlobalDataCollection(seasonID, "MASTER", "DISPLAY", sessionToken);
		Assert.assertTrue(getDevelopmentItemsReportedToAnalytics(respWithQuota)==2, "Incorrect number of development items"); 
		Assert.assertTrue(getProductionItemsReportedToAnalytics(respWithQuota)==2, "Incorrect number of production items");
	}
	
	@Test (dependsOnMethods = "updatePurchaseOptionsToProductionInMaster", description="Add features")
	public void addFeatures() throws Exception{
		String feature1 = FileUtils.fileToString(filePath + "feature1.txt", "UTF-8", false); 
		featureID1 = f.addFeature(seasonID, feature1, "ROOT", sessionToken);
		Assert.assertFalse(featureID1.contains("error"), "Feature was not added to the season: " + featureID1);

		String response = br.checkoutFeature(branchID, featureID1, sessionToken);
		Assert.assertFalse(response.contains("error"), "cannot check out feature");

		String featureMix = FileUtils.fileToString(filePath + "feature-mutual.txt", "UTF-8", false);
		featureMixID = f.addFeature(seasonID, featureMix, featureID1, sessionToken);
		Assert.assertFalse(featureMixID.contains("error"), "Feature mtx was not added to the season: " + featureMixID);

		JSONObject feature2 = new JSONObject(FileUtils.fileToString(filePath + "feature2.txt", "UTF-8", false));
		featureID2 = f.addFeature(seasonID, feature2.toString(), featureMixID, sessionToken);
		Assert.assertFalse(featureID2.contains("error"), "Feature was not added to the season: " + featureID2);
	}

	@Test (dependsOnMethods="addFeatures", description="Add features to analytics in master")
	public void addFeaturesToAnalyticsInMaster() throws IOException, JSONException, InterruptedException{
		//add featureID2 to analytics featureOnOff
		String response = an.addFeatureToAnalytics(featureID2, BranchesRestApi.MASTER, sessionToken);
		response = an.getGlobalDataCollection(seasonID, BranchesRestApi.MASTER, "BASIC", sessionToken);
		Assert.assertTrue(featureOnOff(response).size()==3, "Incorrect number of featureOnOff in master");

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
		Assert.assertTrue(getDevelopmentItemsReportedToAnalytics(respWithQuota)==3, "Incorrect number of development items"); 
		Assert.assertTrue(getProductionItemsReportedToAnalytics(respWithQuota)==2, "Incorrect number of production items"); 		
	}

	@Test (dependsOnMethods="addFeaturesToAnalyticsInMaster", description="update feature to production in master")
	public void updateFeaturesToProductionInMaster() throws IOException, JSONException, InterruptedException{
		//update featureID2 to analytics featureOnOff
		String feature2 = f.getFeature(featureID2, sessionToken);
		JSONObject json = new JSONObject(feature2);
		json.put("stage", "PRODUCTION");
		String response = f.updateFeature(seasonID, featureID2, json.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "cannot udate feature2 to production: " + response);

		response = an.getGlobalDataCollection(seasonID, BranchesRestApi.MASTER, "BASIC", sessionToken);
		Assert.assertTrue(featureOnOff(response).size()==3, "Incorrect number of featureOnOff in master");

		response = an.getGlobalDataCollection(seasonID, branchID, "BASIC", sessionToken);
		Assert.assertTrue(featureOnOff(response).size()==0, "Incorrect number of featureOnOff in branch");

		String respWithQuota = an.getGlobalDataCollection(seasonID, branchID, "DISPLAY", sessionToken);
		Assert.assertTrue(getDevelopmentItemsReportedToAnalytics(respWithQuota)==0, "Incorrect number of development items"); 
		Assert.assertTrue(getProductionItemsReportedToAnalytics(respWithQuota)==0, "Incorrect number of production items");

		respWithQuota = an.getGlobalDataCollection(seasonID, "MASTER", "DISPLAY", sessionToken);
		Assert.assertTrue(getDevelopmentItemsReportedToAnalytics(respWithQuota)==3, "Incorrect number of development items"); 
		Assert.assertTrue(getProductionItemsReportedToAnalytics(respWithQuota)==2, "Incorrect number of production items");
		
		//update featureID1 to production
		String feature1 = f.getFeature(featureID1, sessionToken);
		json = new JSONObject(feature1);
		json.put("stage", "PRODUCTION");
		response = f.updateFeature(seasonID, featureID1, json.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "cannot udate feature1 to production: " + response);

		response = an.getGlobalDataCollection(seasonID, BranchesRestApi.MASTER, "BASIC", sessionToken);
		Assert.assertTrue(featureOnOff(response).size()==3, "Incorrect number of featureOnOff in master");

		response = an.getGlobalDataCollection(seasonID, branchID, "BASIC", sessionToken);
		Assert.assertTrue(featureOnOff(response).size()==0, "Incorrect number of featureOnOff in branch");

		respWithQuota = an.getGlobalDataCollection(seasonID, branchID, "DISPLAY", sessionToken);
		Assert.assertTrue(getDevelopmentItemsReportedToAnalytics(respWithQuota)==0, "Incorrect number of development items"); 
		Assert.assertTrue(getProductionItemsReportedToAnalytics(respWithQuota)==0, "Incorrect number of production items");

		respWithQuota = an.getGlobalDataCollection(seasonID, "MASTER", "DISPLAY", sessionToken);
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

	private String addExperiment(String experimentName) throws IOException, JSONException{
		return baseUtils.addExperiment(m_analyticsUrl, false, false);
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
