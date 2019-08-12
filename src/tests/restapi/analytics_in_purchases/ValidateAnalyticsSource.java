package tests.restapi.analytics_in_purchases;

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
import tests.restapi.InAppPurchasesRestApi;
import tests.restapi.InputSchemaRestApi;
import tests.restapi.ProductsRestApi;

public class ValidateAnalyticsSource {
	protected String seasonID;
	protected String productID;
	protected String entitlementID1;
	protected String entitlementID2;
	protected String entitlementID3;
	protected String featureID1;
	protected String featureID2;
	protected String featureID3;
	protected String purchaseOptionsID1;
	protected String purchaseOptionsID2;
	protected String purchaseOptionsID3;
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
	protected FeaturesRestApi f;
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
		purchasesApi = new InAppPurchasesRestApi();
		purchasesApi.setURL(m_url);
		an = new AnalyticsRestApi();
		an.setURL(analyticsUrl);
		f = new FeaturesRestApi();
		f.setURL(m_url);
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
- add entitlement/configuration to analytics from season and from branch. Get globalData in "display" mode and check for each field its source - master of branch

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
	
	
	@Test (dependsOnMethods="addBranch", description="Add entitlements to analytics")
	public void addEntitlementsToAnalytics() throws IOException, JSONException, InterruptedException{

		//add to master
		JSONObject entitlement = new JSONObject(FileUtils.fileToString(filePath + "purchases/inAppPurchase1.txt", "UTF-8", false));
		entitlement.put("name", "Entitlement1");
		entitlementID1 = purchasesApi.addPurchaseItem(seasonID, entitlement.toString(), "ROOT", sessionToken);
		Assert.assertFalse(entitlementID1.contains("error"), "Entitlement1 was not added: " + entitlementID1);
		
		//add to branch1
		entitlement.put("name", "Entitlement2");
		entitlementID2 = purchasesApi.addPurchaseItemToBranch(seasonID, branchID1, entitlement.toString(), "ROOT", sessionToken);
		Assert.assertFalse(entitlementID2.contains("error"), "Entitlement2 was not added: " + entitlementID2);

		//add to branch2
		entitlement.put("name", "Entitlement3");
		entitlementID3 = purchasesApi.addPurchaseItemToBranch(seasonID, branchID2, entitlement.toString(), "ROOT", sessionToken);
		Assert.assertFalse(entitlementID3.contains("error"), "Entitlement3 was not added: " + entitlementID3);
		
		//add all 3 to analytics
		an.addFeatureToAnalytics(entitlementID1, BranchesRestApi.MASTER, sessionToken);
		an.addFeatureToAnalytics(entitlementID2, branchID1, sessionToken);
		an.addFeatureToAnalytics(entitlementID3, branchID2, sessionToken);
		
		JSONObject response = new JSONObject(an.getGlobalDataCollection(seasonID, branchID1, "DISPLAY", sessionToken));
		JSONArray reporteEntitlements = response.getJSONObject("analyticsDataCollection").getJSONArray("analyticsDataCollectionByFeatureNames");
		Assert.assertTrue(reporteEntitlements.size() == 2, "wrong number of reporteEntitlements");
		
		for (int i=0; i< reporteEntitlements.size(); i++) {
			if (reporteEntitlements.getJSONObject(i).getString("name").equals("ns1.Entitlement1"))
				Assert.assertTrue(reporteEntitlements.getJSONObject(i).getString("branchName").equals("MASTER"), "Incorrect source for Entitlement1");
			else if(reporteEntitlements.getJSONObject(i).getString("name").equals("ns1.Entitlement2"))
				Assert.assertTrue(reporteEntitlements.getJSONObject(i).getString("branchName").equals("branch1"), "Incorrect source for Entitlement2");
		}
		
		response = new JSONObject(an.getGlobalDataCollection(seasonID, branchID2, "DISPLAY", sessionToken));
		reporteEntitlements = response.getJSONObject("analyticsDataCollection").getJSONArray("analyticsDataCollectionByFeatureNames");
		Assert.assertTrue(reporteEntitlements.size() == 2, "wrong number of reporteEntitlements");
		for (int i=0; i< reporteEntitlements.size(); i++) {
			if (reporteEntitlements.getJSONObject(i).getString("name").equals("ns1.Entitlement1"))
				Assert.assertTrue(reporteEntitlements.getJSONObject(i).getString("branchName").equals("MASTER"), "Incorrect source for Entitlement1");
			else if(reporteEntitlements.getJSONObject(i).getString("name").equals("ns1.Entitlement3"))
				Assert.assertTrue(reporteEntitlements.getJSONObject(i).getString("branchName").equals("branch2"), "Incorrect source for Entitlement3");
		}
	}

	
	@Test (dependsOnMethods="addEntitlementsToAnalytics", description="Add attributes to analytics")
	public void addAttributesToAnalytics() throws IOException, JSONException, InterruptedException{

		//add to master
		JSONObject feature = new JSONObject(FileUtils.fileToString(filePath + "configuration_rule1.txt", "UTF-8", false));
		JSONObject newConfiguration = new JSONObject();
		newConfiguration.put("color", "red");
		newConfiguration.put("size", "small");
		feature.put("configuration", newConfiguration);

		feature.put("name", "CR1");
		String CRID1 = purchasesApi.addPurchaseItem(seasonID, feature.toString(), entitlementID1, sessionToken);
		Assert.assertFalse(CRID1.contains("error"), "Configuration1 was not added: " + CRID1);
		
		//add to branch1
		feature.put("name", "CR2");
		String CRID2 = purchasesApi.addPurchaseItemToBranch(seasonID, branchID1, feature.toString(), entitlementID2, sessionToken);
		Assert.assertFalse(CRID2.contains("error"), "Configuration2 was not added: " + CRID2);

		//add to branch2
		feature.put("name", "CR3");
		String CRID3 = purchasesApi.addPurchaseItemToBranch(seasonID, branchID2, feature.toString(), entitlementID3, sessionToken);
		Assert.assertFalse(CRID3.contains("error"), "Configuration3 was not added: " + CRID3);
		
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
		
		String resp = an.addAttributesToAnalytics(entitlementID1, BranchesRestApi.MASTER, attributes.toString(), sessionToken);
		Assert.assertFalse(resp.contains("error"), "Attributes were not added to analytics in master" + resp);				
		resp = an.addAttributesToAnalytics(entitlementID2, branchID1, attributes.toString(), sessionToken);
		Assert.assertFalse(resp.contains("error"), "Attributes were not added to analytics in branch1" + resp);				
		resp = an.addAttributesToAnalytics(entitlementID3, branchID2, attributes.toString(), sessionToken);
		Assert.assertFalse(resp.contains("error"), "Attributes were not added to analytics in branch2" + resp);				
		
		JSONObject response = new JSONObject(an.getGlobalDataCollection(seasonID, branchID1, "DISPLAY", sessionToken));
		JSONArray reporteEntitlements = response.getJSONObject("analyticsDataCollection").getJSONArray("analyticsDataCollectionByFeatureNames");
		Assert.assertTrue(reporteEntitlements.size() == 2, "wrong number of reporteEntitlements");
		
		for (int i=0; i< reporteEntitlements.size(); i++) {
			if (reporteEntitlements.getJSONObject(i).getString("name").equals("ns1.Entitlement1"))
				Assert.assertTrue(reporteEntitlements.getJSONObject(i).getJSONArray("attributes").getJSONObject(0).getString("branchName").equals("MASTER"), "Incorrect source for F1 attributes");
			else if(reporteEntitlements.getJSONObject(i).getString("name").equals("ns1.Entitlement2"))
				Assert.assertTrue(reporteEntitlements.getJSONObject(i).getJSONArray("attributes").getJSONObject(0).getString("branchName").equals("branch1"), "Incorrect source for F2 attributes");
		}
		
		response = new JSONObject(an.getGlobalDataCollection(seasonID, branchID2, "DISPLAY", sessionToken));
		reporteEntitlements = response.getJSONObject("analyticsDataCollection").getJSONArray("analyticsDataCollectionByFeatureNames");
		Assert.assertTrue(reporteEntitlements.size() == 2, "wrong number of reporteEntitlements");
		
		for (int i=0; i< reporteEntitlements.size(); i++) {
			if (reporteEntitlements.getJSONObject(i).getString("name").equals("ns1.Entitlement1"))
				Assert.assertTrue(reporteEntitlements.getJSONObject(i).getJSONArray("attributes").getJSONObject(0).getString("branchName").equals("MASTER"), "Incorrect source for F1 attributes");
			else if(reporteEntitlements.getJSONObject(i).getString("name").equals("ns1.Entitlement3"))
				Assert.assertTrue(reporteEntitlements.getJSONObject(i).getJSONArray("attributes").getJSONObject(0).getString("branchName").equals("branch2"), "Incorrect source for F3 attributes");
		}
	}
	
	@Test ( dependsOnMethods="addAttributesToAnalytics", description="Add input field to analytics")
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
		Assert.assertTrue(reportedFields.size() == 2, "wrong number of reportedFields");
		
		for(int i=0; i<reportedFields.size(); i++){
			if (reportedFields.getJSONObject(i).getString("name").equals("context.device.locale"))
				Assert.assertTrue(reportedFields.getJSONObject(i).getString("branchName").equals("MASTER"));
			else if (reportedFields.getJSONObject(i).getString("name").equals("context.device.connectionType"))
				Assert.assertTrue(reportedFields.getJSONObject(i).getString("branchName").equals("branch1"));

		}
		
		analytics = new JSONObject(an.getGlobalDataCollection(seasonID, branchID2, "DISPLAY", sessionToken));
		reportedFields = analytics.getJSONObject("analyticsDataCollection").getJSONArray("inputFieldsForAnalytics");
		Assert.assertTrue(reportedFields.size() == 3, "wrong number of reportedFields");
		
		for(int i=0; i<reportedFields.size(); i++){
			if (reportedFields.getJSONObject(i).getString("name").equals("context.device.locale"))
				Assert.assertTrue(reportedFields.getJSONObject(i).getString("branchName").equals("MASTER"));
			else if (reportedFields.getJSONObject(i).getString("name").equals("context.device.osVersion"))
				Assert.assertTrue(reportedFields.getJSONObject(i).getString("branchName").equals("branch2"));
			else if (reportedFields.getJSONObject(i).getString("name").equals("context.device.connectionType"))
				Assert.assertTrue(reportedFields.getJSONObject(i).getString("branchName").equals("branch2"));
		}
	}

	@Test (dependsOnMethods="addInputFields", description="Add purchaseOptions to analytics")
	public void addPurchaseOptionsToAnalytics() throws IOException, JSONException, InterruptedException{

		//add to master
		JSONObject purchaseOptions = new JSONObject(FileUtils.fileToString(filePath + "purchases/purchaseOptions1.txt", "UTF-8", false));
		purchaseOptions.put("name", "purchaseOptions1");
		purchaseOptionsID1 = purchasesApi.addPurchaseItem(seasonID, purchaseOptions.toString(), entitlementID1, sessionToken);
		Assert.assertFalse(purchaseOptionsID1.contains("error"), "purchaseOptions1 was not added: " + purchaseOptionsID1);
		
		//add to branch1
		purchaseOptions.put("name", "purchaseOptions2");
		purchaseOptionsID2 = purchasesApi.addPurchaseItemToBranch(seasonID, branchID1, purchaseOptions.toString(), entitlementID2, sessionToken);
		Assert.assertFalse(purchaseOptionsID2.contains("error"), "purchaseOptions2 was not added: " + purchaseOptionsID2);

		//add to branch2
		purchaseOptions.put("name", "purchaseOptions3");
		purchaseOptionsID3 = purchasesApi.addPurchaseItemToBranch(seasonID, branchID2, purchaseOptions.toString(), entitlementID3, sessionToken);
		Assert.assertFalse(purchaseOptionsID3.contains("error"), "purchaseOptions3 was not added: " + purchaseOptionsID3);
		
		//add all 3 to analytics
		an.addFeatureToAnalytics(purchaseOptionsID1, BranchesRestApi.MASTER, sessionToken);
		an.addFeatureToAnalytics(purchaseOptionsID2, branchID1, sessionToken);
		an.addFeatureToAnalytics(purchaseOptionsID3, branchID2, sessionToken);
		
		JSONObject response = new JSONObject(an.getGlobalDataCollection(seasonID, branchID1, "DISPLAY", sessionToken));
		JSONArray reporteEntitlements = response.getJSONObject("analyticsDataCollection").getJSONArray("analyticsDataCollectionByFeatureNames");
		Assert.assertTrue(reporteEntitlements.size() == 4, "wrong number of reporteEntitlements");
		
		for (int i=0; i< reporteEntitlements.size(); i++) {
			if (reporteEntitlements.getJSONObject(i).getString("name").equals("ns1.Entitlement1"))
				Assert.assertTrue(reporteEntitlements.getJSONObject(i).getString("branchName").equals("MASTER"), "Incorrect source for Entitlement1");
			else if(reporteEntitlements.getJSONObject(i).getString("name").equals("ns1.Entitlement2"))
				Assert.assertTrue(reporteEntitlements.getJSONObject(i).getString("branchName").equals("branch1"), "Incorrect source for Entitlement2");
			else if(reporteEntitlements.getJSONObject(i).getString("name").equals("ns1.purchaseOptions1"))
				Assert.assertTrue(reporteEntitlements.getJSONObject(i).getString("branchName").equals("MASTER"), "Incorrect source for purchaseOptions1");
			else if(reporteEntitlements.getJSONObject(i).getString("name").equals("ns1.purchaseOptions2"))
				Assert.assertTrue(reporteEntitlements.getJSONObject(i).getString("branchName").equals("branch1"), "Incorrect source for purchaseOptions2");
		}
		
		response = new JSONObject(an.getGlobalDataCollection(seasonID, branchID2, "DISPLAY", sessionToken));
		reporteEntitlements = response.getJSONObject("analyticsDataCollection").getJSONArray("analyticsDataCollectionByFeatureNames");
		Assert.assertTrue(reporteEntitlements.size() == 4, "wrong number of reporteEntitlements");
		for (int i=0; i< reporteEntitlements.size(); i++) {
			if (reporteEntitlements.getJSONObject(i).getString("name").equals("ns1.Entitlement1"))
				Assert.assertTrue(reporteEntitlements.getJSONObject(i).getString("branchName").equals("MASTER"), "Incorrect source for Entitlement1");
			else if(reporteEntitlements.getJSONObject(i).getString("name").equals("ns1.Entitlement3"))
				Assert.assertTrue(reporteEntitlements.getJSONObject(i).getString("branchName").equals("branch2"), "Incorrect source for Entitlement3");
			else if(reporteEntitlements.getJSONObject(i).getString("name").equals("ns1.purchaseOptions1"))
				Assert.assertTrue(reporteEntitlements.getJSONObject(i).getString("branchName").equals("MASTER"), "Incorrect source for purchaseOptions1");
			else if(reporteEntitlements.getJSONObject(i).getString("name").equals("ns1.purchaseOptions3"))
				Assert.assertTrue(reporteEntitlements.getJSONObject(i).getString("branchName").equals("branch2"), "Incorrect source for purchaseOptions3");
		}
	}

	@Test (dependsOnMethods="addPurchaseOptionsToAnalytics", description="Add features to analytics")
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
		JSONArray reportedItems = response.getJSONObject("analyticsDataCollection").getJSONArray("analyticsDataCollectionByFeatureNames");
		Assert.assertTrue(reportedItems.size() == 6, "wrong number of reporteEntitlements");
		for (int i=0; i< reportedItems.size(); i++) {
			if (reportedItems.getJSONObject(i).getString("name").equals("ns1.Entitlement1"))
				Assert.assertTrue(reportedItems.getJSONObject(i).getString("branchName").equals("MASTER"), "Incorrect source for Entitlement1");
			else if(reportedItems.getJSONObject(i).getString("name").equals("ns1.Entitlement2"))
				Assert.assertTrue(reportedItems.getJSONObject(i).getString("branchName").equals("branch1"), "Incorrect source for Entitlement2");
			else if(reportedItems.getJSONObject(i).getString("name").equals("ns1.purchaseOptions1"))
				Assert.assertTrue(reportedItems.getJSONObject(i).getString("branchName").equals("MASTER"), "Incorrect source for purchaseOptions1");
			else if(reportedItems.getJSONObject(i).getString("name").equals("ns1.purchaseOptions2"))
				Assert.assertTrue(reportedItems.getJSONObject(i).getString("branchName").equals("branch1"), "Incorrect source for purchaseOptions2");
			else if (reportedItems.getJSONObject(i).getString("name").equals("ns1.F1"))
				Assert.assertTrue(reportedItems.getJSONObject(i).getString("branchName").equals("MASTER"), "Incorrect source for F1");
			else if(reportedItems.getJSONObject(i).getString("name").equals("ns1.F2"))
				Assert.assertTrue(reportedItems.getJSONObject(i).getString("branchName").equals("branch1"), "Incorrect source for F2");
		}
		
		response = new JSONObject(an.getGlobalDataCollection(seasonID, branchID2, "DISPLAY", sessionToken));
		reportedItems = response.getJSONObject("analyticsDataCollection").getJSONArray("analyticsDataCollectionByFeatureNames");
		Assert.assertTrue(reportedItems.size() == 6, "wrong number of reporteEntitlements");
		for (int i=0; i< reportedItems.size(); i++) {
			if (reportedItems.getJSONObject(i).getString("name").equals("ns1.Entitlement1"))
				Assert.assertTrue(reportedItems.getJSONObject(i).getString("branchName").equals("MASTER"), "Incorrect source for Entitlement1");
			else if(reportedItems.getJSONObject(i).getString("name").equals("ns1.Entitlement3"))
				Assert.assertTrue(reportedItems.getJSONObject(i).getString("branchName").equals("branch2"), "Incorrect source for Entitlement3");
			else if(reportedItems.getJSONObject(i).getString("name").equals("ns1.purchaseOptions1"))
				Assert.assertTrue(reportedItems.getJSONObject(i).getString("branchName").equals("MASTER"), "Incorrect source for purchaseOptions1");
			else if(reportedItems.getJSONObject(i).getString("name").equals("ns1.purchaseOptions3"))
				Assert.assertTrue(reportedItems.getJSONObject(i).getString("branchName").equals("branch2"), "Incorrect source for purchaseOptions3");
			else if (reportedItems.getJSONObject(i).getString("name").equals("ns1.F1"))
				Assert.assertTrue(reportedItems.getJSONObject(i).getString("branchName").equals("MASTER"), "Incorrect source for F1");
			else if(reportedItems.getJSONObject(i).getString("name").equals("ns1.F3"))
				Assert.assertTrue(reportedItems.getJSONObject(i).getString("branchName").equals("branch2"), "Incorrect source for F3");
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
