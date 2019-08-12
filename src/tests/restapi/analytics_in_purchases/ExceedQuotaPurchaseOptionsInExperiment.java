package tests.restapi.analytics_in_purchases;

import java.io.IOException;

import org.apache.wink.json4j.JSONException;
import org.apache.wink.json4j.JSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import tests.com.ibm.qautils.FileUtils;
import tests.restapi.*;

public class ExceedQuotaPurchaseOptionsInExperiment {
	protected String seasonID;
	protected String branchID;
	protected String productID;
	protected String entitlementID1;
	protected String entitlementID2;
	protected String purchaseOptionsID1;
	protected String purchaseOptionsID2;
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
	
	//IAP1->IAP2, PO1
	@Test (description="Set quota to 3")
	public void updateQuota() throws IOException, JSONException, InterruptedException{
		String response = an.updateQuota(seasonID, 2, sessionToken);
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

	@Test (dependsOnMethods="addBranch", description="add entitlements to analytics")
	public void addProdEntitlementsToAnalytics() throws Exception{	
		JSONObject entitlement =  new JSONObject(FileUtils.fileToString(filePath + "purchases/inAppPurchase1.txt", "UTF-8", false))  ;
		
		entitlement.put("name", "Entitlement1");
		entitlement.put("stage", "PRODUCTION");
		entitlementID1 = purchasesApi.addPurchaseItemToBranch(seasonID, branchID, entitlement.toString(), "ROOT", sessionToken);
		Assert.assertFalse(entitlementID1.contains("error"), "entitlement was not added to the season" + entitlementID1);
		
		entitlement.put("name", "Entitlement2");
		entitlement.put("stage", "PRODUCTION");
		entitlementID2 = purchasesApi.addPurchaseItemToBranch(seasonID, branchID, entitlement.toString(), entitlementID1, sessionToken);
		Assert.assertFalse(entitlementID2.contains("error"), "entitlement2 was not added to the season" + entitlementID2);

		String purchaseOptions = FileUtils.fileToString(filePath + "purchases/purchaseOptions1.txt", "UTF-8", false);
		JSONObject jsonOR = new JSONObject(purchaseOptions);
		jsonOR.put("name", "PO1");
		purchaseOptionsID1 = purchasesApi.addPurchaseItemToBranch(seasonID, branchID, jsonOR.toString(), entitlementID1, sessionToken);
		Assert.assertFalse(purchaseOptionsID1.contains("error"), "purchaseOptions1 was not added to the season" + purchaseOptionsID1);

		//report Entitlement2 to analytics - 1 production item
		String response = an.addFeatureToAnalytics(entitlementID2, branchID, sessionToken);
		Assert.assertFalse(response.contains("error"), "Entitlement2 was not added to analytics" + response);

		//report Entitlement1 to analytics - 1 production items
		response = an.addFeatureToAnalytics(entitlementID1, branchID, sessionToken);
		Assert.assertFalse(response.contains("error"), "Parent feature was not added to analytics" + response);
	
		//report purchaseOptions1 to analytics - 1 production items
		response = an.addFeatureToAnalytics(purchaseOptionsID1, branchID, sessionToken);
		Assert.assertFalse(response.contains("error"), "purchaseOptions1 was not added to analytics" + response);
		
		String respWithQuota = an.getGlobalDataCollection(seasonID, branchID, "DISPLAY", sessionToken);
		Assert.assertTrue(getDevelopmentItemsReportedToAnalytics(respWithQuota)==3, "Incorrect number of development items");
		Assert.assertTrue(getProductionItemsReportedToAnalytics(respWithQuota)==2, "Incorrect number of production items");
	}
	
	@Test (dependsOnMethods="addProdEntitlementsToAnalytics", description="Move purchaseOptions to production in branch - exceeds the quota")
	public void movePurchaseOptionsToProdInBranch() throws JSONException, IOException{
		//checkout parent feature to branch
		br.checkoutFeature(branchID, entitlementID1, sessionToken);
		
		String purchaseOptions = purchasesApi.getPurchaseItemFromBranch(purchaseOptionsID1, branchID, sessionToken);
		JSONObject jsonPO1 = new JSONObject(purchaseOptions);
		jsonPO1.put("stage", "PRODUCTION");
		String response = purchasesApi.updatePurchaseItemInBranch(seasonID, branchID, purchaseOptionsID1, jsonPO1.toString(), sessionToken);
		Assert.assertTrue(response.contains("error"), "Moved purchaseOptions to production when quota limit is reached");
		
		String respWithQuota = an.getGlobalDataCollection(seasonID, branchID, "DISPLAY", sessionToken);
		Assert.assertTrue(getDevelopmentItemsReportedToAnalytics(respWithQuota)==3, "Incorrect number of development items");
		Assert.assertTrue(getProductionItemsReportedToAnalytics(respWithQuota)==2, "Incorrect number of production items");
		
		//remove purchaseOptions1 from analytics in branch
		response = an.deleteFeatureFromAnalytics(purchaseOptionsID1, branchID, sessionToken);
		Assert.assertFalse(response.contains("error"), "Can't remove purchaseOptions from analytics in branch");
		
		respWithQuota = an.getGlobalDataCollection(seasonID, branchID, "DISPLAY", sessionToken);
		Assert.assertTrue(getDevelopmentItemsReportedToAnalytics(respWithQuota)==2, "Incorrect number of development items");
		Assert.assertTrue(getProductionItemsReportedToAnalytics(respWithQuota)==2, "Incorrect number of production items");
		
		//move OR1 to production
		response = purchasesApi.updatePurchaseItemInBranch(seasonID, branchID, purchaseOptionsID1, jsonPO1.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Can't move purchaseOptions to production");
	}
	
	@Test (dependsOnMethods="movePurchaseOptionsToProdInBranch", description="Add dev purchaseOptions in branch")
	public void addPurchaseOptionsInBranch() throws JSONException, IOException{
		//add purchaseOptions in dev stage
		String purchaseOptions = FileUtils.fileToString(filePath + "purchases/purchaseOptions1.txt", "UTF-8", false);
		JSONObject jsonPO2 = new JSONObject(purchaseOptions);
		jsonPO2.put("name", "PO2");
		jsonPO2.put("stage", "PRODUCTION");
		purchaseOptionsID2 = purchasesApi.addPurchaseItemToBranch(seasonID, branchID, jsonPO2.toString(), entitlementID2, sessionToken);
		Assert.assertFalse(purchaseOptionsID2.contains("error"), "purchaseOptions2 was not added to the season" + purchaseOptionsID2);
		
		//report purchaseOptions2 to analytics in branch
		String response = an.addFeatureToAnalytics(purchaseOptionsID2, branchID, sessionToken);
		Assert.assertTrue(response.contains("error"), "purchaseOptions was added to analytics and quota was exceeded " + response);

		String respWithQuota = an.getGlobalDataCollection(seasonID, branchID, "DISPLAY", sessionToken);
		
		Assert.assertTrue(getDevelopmentItemsReportedToAnalytics(respWithQuota)==2, "Incorrect number of development items");
		Assert.assertTrue(getProductionItemsReportedToAnalytics(respWithQuota)==2, "Incorrect number of production items");
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
