package tests.restapi.analytics_in_purchases;

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

public class ExceedQuotaInExperiment {
	protected String seasonID;
	protected String branchID;
	protected String productID;
	protected String entitlementID1;
	protected String entitlementID2;
	private String entitlementID3;
	protected String configID1;
	protected String configID2;
	protected String filePath;
	protected String m_url;
	protected ProductsRestApi p;
	protected InAppPurchasesRestApi purchasesApi;
	protected AnalyticsRestApi an;
	protected InputSchemaRestApi schema;
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

	
	@Test ( dependsOnMethods="addBranch", description="Add input schema to the season")
	public void addSchema() throws Exception{
		String sch = schema.getInputSchema(seasonID, sessionToken);
        JSONObject jsonSchema = new JSONObject(sch);
        String schemaBody = FileUtils.fileToString(filePath + "inputSchemas/inputSchema_update_device_locale_to_production.txt", "UTF-8", false);
        jsonSchema.put("inputSchema", new JSONObject(schemaBody));
        String response = schema.updateInputSchema(seasonID, jsonSchema.toString(), sessionToken);
        Assert.assertFalse(response.contains("error"), "Schema was not added to the season" + response);
	}

	@Test (dependsOnMethods="addSchema", description="Set quota to 3")
	public void updateQuota() throws IOException, JSONException, InterruptedException{
		String response = an.updateQuota(seasonID, 3, sessionToken);
		Assert.assertFalse(response.contains("error"), "Quota was not returned " + response);				
	}
	
	@Test (dependsOnMethods="updateQuota", description="Add 1 input field, 1 feature, 1 attribute")
	public void addToAnalytics() throws Exception{
		//add input field in master
		JSONArray inputFields = new JSONArray();
		inputFields.put("context.device.locale");
		String response = an.updateInputFieldToAnalytics(seasonID, "MASTER",  inputFields.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Analytics was not updated" + response);
		
		//add feature in production  in master
		String entitlement1 = FileUtils.fileToString(filePath + "purchases/inAppPurchase1.txt", "UTF-8", false);
		JSONObject jsonF = new JSONObject(entitlement1);
		jsonF.put("stage", "PRODUCTION");
		entitlementID1 = purchasesApi.addPurchaseItemToBranch(seasonID, "MASTER", jsonF.toString(), "ROOT", sessionToken);
		Assert.assertFalse(entitlementID1.contains("error"), "Entitlement was not added to the season" + entitlementID1);
		
		response = an.addFeatureToAnalytics(entitlementID1, "MASTER", sessionToken);
		Assert.assertFalse(response.contains("error"), "Incorrect globalDataCollection response");	
		
		//add attribute  in master
		String config = FileUtils.fileToString(filePath + "configuration_rule1.txt", "UTF-8", false);
		JSONObject jsonConfig = new JSONObject(config);
		JSONObject newConfiguration = new JSONObject();
		newConfiguration.put("color", "red");
		newConfiguration.put("size", "small");
		jsonConfig.put("configuration", newConfiguration);
		configID1 = purchasesApi.addPurchaseItemToBranch(seasonID, "MASTER", jsonConfig.toString(), entitlementID1, sessionToken);
		Assert.assertFalse(configID1.contains("error"), "Configuration1 was not added to the season" + configID1);

		JSONArray attributes = new JSONArray();
		JSONObject attr1 = new JSONObject();
		attr1.put("name", "color");
		attr1.put("type", "REGULAR");
		attributes.add(attr1);
		
		response = an.addAttributesToAnalytics(entitlementID1, "MASTER", attributes.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Entitlement was not added to analytics" + response);

		String respWithQuota = an.getGlobalDataCollection(seasonID, "MASTER", "DISPLAY", sessionToken);
		Assert.assertTrue(getDevelopmentItemsReportedToAnalytics(respWithQuota)==3, "Incorrect number of development items"); //feature1+attribute+inputfield
		Assert.assertTrue(getProductionItemsReportedToAnalytics(respWithQuota)==3, "Incorrect number of production items"); //feature1+attribute+inputfield
	}
	
	@Test (dependsOnMethods="addToAnalytics", description="Add Production input field - exceeds the quota")
	public void addInputFieldOverQuotaFromBranch() throws JSONException{		
		//add production input field
		JSONArray inputFields = new JSONArray();
		inputFields.put("context.device.locale");	//it's reported in master
		inputFields.put("context.device.connectionType");
		inputFields.put("context.device.osVersion");
		inputFields.put("context.weatherSummary.nearestSnowAccumulation.dayPart");
		
		String response = an.updateInputFieldToAnalytics(seasonID, branchID, inputFields.toString(), sessionToken);
		Assert.assertTrue(response.contains("The maximum number"), "Field in production was added and quota was exceeded ");

		String respWithQuotaInBranch = an.getGlobalDataCollection(seasonID, branchID, "DISPLAY", sessionToken);
		Assert.assertTrue(getDevelopmentItemsReportedToAnalytics(respWithQuotaInBranch)==3, "Incorrect number of development items in branch");
		Assert.assertTrue(getProductionItemsReportedToAnalytics(respWithQuotaInBranch)==3, "Incorrect number of production items  in branch");

		String respWithQuota = an.getGlobalDataCollection(seasonID, "MASTER", "DISPLAY", sessionToken);
		Assert.assertTrue(getDevelopmentItemsReportedToAnalytics(respWithQuota)==3, "Incorrect number of development items");
		Assert.assertTrue(getProductionItemsReportedToAnalytics(respWithQuota)==3, "Incorrect number of production items");

	}
	
	@Test (dependsOnMethods="addInputFieldOverQuotaFromBranch", description="Add Production entitlement - exceeds the quota")
	public void addEntitlementOverQuotaFromBranch() throws JSONException, IOException{
		//add new entitlement in production to featureID1
		br.checkoutFeature(branchID, entitlementID1, sessionToken);
		
		String config = FileUtils.fileToString(filePath + "configuration_rule1.txt", "UTF-8", false);
		JSONObject jsonConfig = new JSONObject(config);
		jsonConfig.put("name", "CR2");
		jsonConfig.put("stage", "PRODUCTION");
		JSONObject newConfiguration = new JSONObject();
		newConfiguration.put("title", "mytitle");
		jsonConfig.put("configuration", newConfiguration);
		configID2 = purchasesApi.addPurchaseItemToBranch(seasonID, branchID, jsonConfig.toString(), entitlementID1, sessionToken);
		Assert.assertFalse(configID2.contains("error"), "Configuration2 was not added to the season" + configID2);

		
		String response = an.addFeatureToAnalytics(configID2, branchID, sessionToken);
		Assert.assertTrue(response.contains("The maximum number"), "Feature in production was added and quota was exceeded");	

		String respWithQuotaInBranch = an.getGlobalDataCollection(seasonID, branchID, "DISPLAY", sessionToken);
		Assert.assertTrue(getDevelopmentItemsReportedToAnalytics(respWithQuotaInBranch)==3, "Incorrect number of development items in branch");
		Assert.assertTrue(getProductionItemsReportedToAnalytics(respWithQuotaInBranch)==3, "Incorrect number of production items in branch");

		String respWithQuota = an.getGlobalDataCollection(seasonID, "MASTER", "DISPLAY", sessionToken);
		Assert.assertTrue(getDevelopmentItemsReportedToAnalytics(respWithQuota)==3, "Incorrect number of development items");
		Assert.assertTrue(getProductionItemsReportedToAnalytics(respWithQuota)==3, "Incorrect number of production items");
	}
	
	@Test (dependsOnMethods="addEntitlementOverQuotaFromBranch", description="Add attribute - exceeds the quota")
	public void addAttributeOverQuotaFromBranch() throws JSONException{	
		JSONArray attributes = new JSONArray();
		JSONObject attr1 = new JSONObject();
		attr1.put("name", "title");
		attr1.put("type", "REGULAR");
		JSONObject attr2 = new JSONObject();
		attr2.put("name", "color");
		attr2.put("type", "REGULAR");
		attributes.add(attr1);
		attributes.add(attr2);
		
		String response = an.addAttributesToAnalytics(entitlementID1, branchID, attributes.toString(), sessionToken);
		Assert.assertTrue(response.contains("The maximum number"), "Attribute was not added to analytics" + response);

		String respWithQuotaInBranch = an.getGlobalDataCollection(seasonID, branchID, "DISPLAY", sessionToken);
		Assert.assertTrue(getDevelopmentItemsReportedToAnalytics(respWithQuotaInBranch)==3, "Incorrect number of development items");
		Assert.assertTrue(getProductionItemsReportedToAnalytics(respWithQuotaInBranch)==3, "Incorrect number of production items");

		String respWithQuota = an.getGlobalDataCollection(seasonID, "MASTER", "DISPLAY", sessionToken);
		Assert.assertTrue(getDevelopmentItemsReportedToAnalytics(respWithQuota)==3, "Incorrect number of development items");
		Assert.assertTrue(getProductionItemsReportedToAnalytics(respWithQuota)==3, "Incorrect number of production items");
	}
	
	@Test (dependsOnMethods="addAttributeOverQuotaFromBranch", description="Add checked out entitlement over quota")
	public void addCheckedoutEntitlementOverQuotaFromBranch() throws JSONException, IOException{
		//add new entitlement in production to master
		br.checkoutFeature(branchID, entitlementID1, sessionToken);
		
		String entitlement = FileUtils.fileToString(filePath + "purchases/inAppPurchase2.txt", "UTF-8", false);
		JSONObject json = new JSONObject(entitlement);
		json.put("stage", "PRODUCTION");
		entitlementID2 = purchasesApi.addPurchaseItemToBranch(seasonID, "MASTER", json.toString(), "ROOT", sessionToken);
		Assert.assertFalse(entitlementID2.contains("error"), "entitlement2 was not added to the season" + entitlementID2);

		//checkout entitlement to branch
		br.checkoutFeature(branchID, entitlementID2, sessionToken);
		
		//add entitlement to analytics in branch
		String response = an.addFeatureToAnalytics(entitlementID2, branchID, sessionToken);
		Assert.assertTrue(response.contains("The maximum number"), "entitlement in production was added and quota was exceeded");	

		String respWithQuotaInBranch = an.getGlobalDataCollection(seasonID, branchID, "DISPLAY", sessionToken);
		Assert.assertTrue(getDevelopmentItemsReportedToAnalytics(respWithQuotaInBranch)==3, "Incorrect number of development items in branch");
		Assert.assertTrue(getProductionItemsReportedToAnalytics(respWithQuotaInBranch)==3, "Incorrect number of production items in branch");

		String respWithQuota = an.getGlobalDataCollection(seasonID, "MASTER", "DISPLAY", sessionToken);
		Assert.assertTrue(getDevelopmentItemsReportedToAnalytics(respWithQuota)==3, "Incorrect number of development items");
		Assert.assertTrue(getProductionItemsReportedToAnalytics(respWithQuota)==3, "Incorrect number of production items");
	}
	
	
	@Test (dependsOnMethods="addCheckedoutEntitlementOverQuotaFromBranch", description="Add Production entitlement in master - exceeds the quota")
	public void addEntitlementOverQuotaFromMaster() throws JSONException, IOException{
		br.checkoutFeature(branchID, entitlementID1, sessionToken);
		
		String entitlement = FileUtils.fileToString(filePath + "purchases/inAppPurchase1.txt", "UTF-8", false);
		JSONObject json = new JSONObject(entitlement);
		json.put("name", "F3");
		json.put("stage", "PRODUCTION");
		entitlementID3 = purchasesApi.addPurchaseItem(seasonID, json.toString(), "ROOT", sessionToken);
				
		String response = an.addFeatureToAnalytics(entitlementID3, BranchesRestApi.MASTER, sessionToken);
		Assert.assertTrue(response.contains("The maximum number"), "Entitlement in production was added and quota was exceeded");	

		String respWithQuotaInBranch = an.getGlobalDataCollection(seasonID, branchID, "DISPLAY", sessionToken);
		Assert.assertTrue(getDevelopmentItemsReportedToAnalytics(respWithQuotaInBranch)==3, "Incorrect number of development items in branch");
		Assert.assertTrue(getProductionItemsReportedToAnalytics(respWithQuotaInBranch)==3, "Incorrect number of production items in branch");

		String respWithQuota = an.getGlobalDataCollection(seasonID, "MASTER", "DISPLAY", sessionToken);
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
