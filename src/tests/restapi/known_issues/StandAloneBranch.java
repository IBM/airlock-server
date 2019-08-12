//package tests.restapi.analytics_in_branch;
package tests.restapi.known_issues;

import org.apache.wink.json4j.JSONArray;

import org.apache.wink.json4j.JSONException;
import org.apache.wink.json4j.JSONObject;
import org.testng.Assert;
import org.testng.annotations.*;

import tests.com.ibm.qautils.FileUtils;
import tests.restapi.*;

import java.io.IOException;

// Known issue; bug #14 Q4-2107 design issue
public class StandAloneBranch {
	protected String seasonID;
	protected String seasonID2;
	protected String branchID;
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
	private String m_analyticsUrl;
	
	@BeforeClass
	@Parameters({"url", "analyticsUrl", "translationsUrl", "configPath", "sessionToken", "userName", "userPassword", "appName", "productsToDeleteFile"})
	public void init(String url, String analyticsUrl, String translationsUrl, String configPath, String sToken, String userName, String userPassword, String appName, String productsToDeleteFile) throws Exception{
		m_url = url;
		filePath = configPath;
		p = new ProductsRestApi();
		p.setURL(m_url);
		f = new FeaturesRestApi();
		f.setURL(m_url);
		an = new AnalyticsRestApi();
		an.setURL(analyticsUrl);
        schema = new InputSchemaRestApi();
        schema.setURL(m_url);
        
		baseUtils = new AirlockUtils(url, analyticsUrl, translationsUrl, configPath, sToken, userName, userPassword, appName, productsToDeleteFile);
		sessionToken = baseUtils.sessionToken;

		productID = baseUtils.createProduct();
		baseUtils.printProductToFile(productID);
		seasonID = baseUtils.createSeason(productID);
		m_analyticsUrl = analyticsUrl;
		try {
			branchID = baseUtils.addBranchFromBranch("branch1",BranchesRestApi.MASTER,seasonID);
		}catch (Exception e){
			branchID = null;
		}
	}
	
	//use separate api-s to add to analytics
	
	@Test ( description="Add input schema to the season")
	public void addSchema() throws Exception{
		String sch = schema.getInputSchema(seasonID, sessionToken);
        JSONObject jsonSchema = new JSONObject(sch);
        String schemaBody = FileUtils.fileToString(filePath + "inputSchemas/inputSchema_update_device_locale_to_production.txt", "UTF-8", false);
        jsonSchema.put("inputSchema", new JSONObject(schemaBody));
        String response = schema.updateInputSchema(seasonID, jsonSchema.toString(), sessionToken);
        Assert.assertFalse(response.contains("error"), "Schema was not added to the season" + response);
	}

	@Test (dependsOnMethods="addSchema", description="Set quota to 4")
	public void updateQuota() throws IOException, JSONException, InterruptedException{
		String response = an.updateQuota(seasonID, 4, sessionToken);
		Assert.assertFalse(response.contains("error"), "Quota was not returned " + response);				
	}
	
	@Test (dependsOnMethods="updateQuota", description="Add 1 input field, 1 feature, 2 attributes")
	public void addToAnalytics() throws Exception{
		//add input field
		JSONArray inputFields = new JSONArray();
		inputFields.put("context.device.locale");
		String response = an.updateInputFieldToAnalytics(seasonID, branchID,  inputFields.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Analytics was not updated" + response);
		
		//add feature in production
		String feature1 = FileUtils.fileToString(filePath + "feature1.txt", "UTF-8", false);
		JSONObject jsonF = new JSONObject(feature1);
		jsonF.put("stage", "PRODUCTION");
		featureID1 = f.addFeatureToBranch(seasonID, branchID, jsonF.toString(), "ROOT", sessionToken);
		Assert.assertFalse(featureID1.contains("error"), "Feature was not added to the season" + featureID1);
		
		response = an.addFeatureToAnalytics(featureID1, branchID, sessionToken);
		Assert.assertFalse(response.contains("error"), "Incorrect globalDataCollection response");	
		
		//add attribute
		String config = FileUtils.fileToString(filePath + "configuration_rule1.txt", "UTF-8", false);
		JSONObject jsonConfig = new JSONObject(config);
		JSONObject newConfiguration = new JSONObject();
		newConfiguration.put("color", "red");
		newConfiguration.put("size", "small");
		jsonConfig.put("configuration", newConfiguration);
		configID1 = f.addFeatureToBranch(seasonID,branchID, jsonConfig.toString(), featureID1, sessionToken);
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
		
		response = an.addAttributesToAnalytics(featureID1, branchID, attributes.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Feature was not added to analytics" + response);

		String respWithQuota = an.getGlobalDataCollection(seasonID, branchID, "DISPLAY", sessionToken);
		Assert.assertTrue(getDevelopmentItemsReportedToAnalytics(respWithQuota)==4, "Incorrect number of development items"); //feature1+attribute+inputfield
		Assert.assertTrue(getProductionItemsReportedToAnalytics(respWithQuota)==4, "Incorrect number of production items"); //feature1+attribute+inputfield
		respWithQuota = an.getGlobalDataCollection(seasonID,  BranchesRestApi.MASTER, "DISPLAY", sessionToken);
		Assert.assertTrue(getDevelopmentItemsReportedToAnalytics(respWithQuota)==0, "Incorrect number of development items"); //feature1+attribute+inputfield
		Assert.assertTrue(getProductionItemsReportedToAnalytics(respWithQuota)==0, "Incorrect number of production items"); //feature1+attribute+inputfield

		response = an.updateQuota(seasonID, 2, sessionToken);
		Assert.assertFalse(response.contains("error"), "Quota was not returned " + response);

	}

	@Test (dependsOnMethods="addToAnalytics", description="Add attribute - exceeds the quota")
	public void featureFromDevToProdInBranch() throws JSONException, IOException{

			String feature2 = FileUtils.fileToString(filePath + "feature2.txt", "UTF-8", false);
			JSONObject jsonF = new JSONObject(feature2);
			featureID2 = f.addFeatureToBranch(seasonID, branchID, jsonF.toString(), "ROOT", sessionToken);
			Assert.assertFalse(featureID2.contains("error"), "Feature was not added to the season" + featureID2);
			
			String response = an.addFeatureToAnalytics(featureID2, branchID, sessionToken);
			Assert.assertFalse(response.contains("error"), "Incorrect globalDataCollection response");

			
			jsonF = new JSONObject(f.getFeatureFromBranch(featureID2,branchID,sessionToken));
			jsonF.put("stage", "PRODUCTION");
			response = f.updateFeatureInBranch(seasonID,branchID,featureID2,jsonF.toString(),sessionToken);
			Assert.assertTrue(response.contains("error"), "Incorrect globalDataCollection response");

	}

	@Test (dependsOnMethods="featureFromDevToProdInBranch", description="Add attribute - exceeds the quota")
	public void attibuteFromDevToProdInBranch() throws JSONException, IOException{

			String response = an.deleteFeatureFromAnalytics(featureID2, branchID, sessionToken);
			Assert.assertFalse(response.contains("error"), "Incorrect globalDataCollection response");

			String config = FileUtils.fileToString(filePath + "configuration_rule1.txt", "UTF-8", false);
			JSONObject jsonConfig = new JSONObject(config);
			JSONObject newConfiguration = new JSONObject();
			newConfiguration.put("color", "red");
			newConfiguration.put("size", "small");
			jsonConfig.put("configuration", newConfiguration);
			jsonConfig.put("name","config2");
			configID2 = f.addFeatureToBranch(seasonID, branchID, jsonConfig.toString(), featureID2, sessionToken);
			Assert.assertFalse(configID2.contains("error"), "Configuration1 was not added to the season" + configID2);

			JSONArray attributes = new JSONArray();
			JSONObject attr1 = new JSONObject();
			attr1.put("name", "color");
			attr1.put("type", "REGULAR");
			attributes.add(attr1);

			response = an.addAttributesToAnalytics(featureID2, branchID, attributes.toString(), sessionToken);
			Assert.assertTrue(response.contains("error"), "Attributes were not added to analytics" + response);

	}

	@Test (dependsOnMethods="attibuteFromDevToProdInBranch", description="Add attribute - exceeds the quota")
	public void inputFieldFromDevToProdInBranch() throws JSONException {
		try {
			String sch = schema.getInputSchema(seasonID, sessionToken);
			JSONObject jsonSchema = new JSONObject(sch);
			String schemaBody = FileUtils.fileToString(filePath + "inputSchemas/inputSchema_Production_fields_viewedLocation.txt", "UTF-8", false);
			jsonSchema.put("inputSchema", new JSONObject(schemaBody));
			String response = schema.updateInputSchema(seasonID, jsonSchema.toString(), sessionToken);
			JSONArray inputFields = new JSONArray();
			inputFields.put("context.device.locale");
			response = an.updateInputFieldToAnalytics(seasonID, branchID,  inputFields.toString(), sessionToken);

			JSONObject jsonF = new JSONObject(f.getFeatureFromBranch(featureID2,branchID,sessionToken));
			jsonF.put("stage", "PRODUCTION");
			response = f.updateFeatureInBranch(seasonID,branchID,featureID2,jsonF.toString(),sessionToken);

			JSONObject test = new JSONObject(an.getGlobalDataCollection(seasonID, branchID, "DISPLAY", sessionToken));

			sch = schema.getInputSchema(seasonID, sessionToken);
			jsonSchema = new JSONObject(sch);
			schemaBody = FileUtils.fileToString(filePath + "inputSchemas/inputSchema_update_device_locale_to_production.txt", "UTF-8", false);
			jsonSchema.put("inputSchema", new JSONObject(schemaBody));
			String validateSchema = schema.validateSchema(seasonID, schemaBody, sessionToken);
			JSONObject validateJson = new JSONObject(validateSchema);
			Assert.assertTrue(validateJson.containsKey("productionAnalyticsItemsQuotaExceeded"), "Validate schema didn't provide information on exceeded quota");
			response = schema.updateInputSchema(seasonID, jsonSchema.toString(), sessionToken);
			test = new JSONObject(an.getGlobalDataCollection(seasonID, branchID, "DISPLAY", sessionToken));
			Assert.assertTrue(response.contains("error"), "Incorrect globalDataCollection response");
		} catch (Exception e) {
			Assert.fail();
		}
	}

	@Test (dependsOnMethods="inputFieldFromDevToProdInBranch", description="Add attribute - exceeds the quota")
	public void addInMaster() throws Exception {
		//add input field
		an.updateQuota(seasonID, 4, sessionToken);

		JSONArray inputFields = new JSONArray();
		inputFields.put("context.device.locale");
		inputFields.put("context.device.osVersion");
		String response = an.updateInputFieldToAnalytics(seasonID, BranchesRestApi.MASTER,  inputFields.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Analytics was not updated" + response);

		//add feature in production
		String feature3 = FileUtils.fileToString(filePath + "feature3.txt", "UTF-8", false);
		JSONObject jsonF = new JSONObject(feature3);
		jsonF.put("stage", "PRODUCTION");
		String featureID3 = f.addFeatureToBranch(seasonID, BranchesRestApi.MASTER, jsonF.toString(), "ROOT", sessionToken);
		Assert.assertFalse(featureID3.contains("error"), "Feature was not added to the season" + featureID3);

		response = an.addFeatureToAnalytics(featureID3, BranchesRestApi.MASTER, sessionToken);
		Assert.assertFalse(response.contains("error"), "Incorrect globalDataCollection response");

		//add attribute
		String config = FileUtils.fileToString(filePath + "configuration_rule1.txt", "UTF-8", false);
		JSONObject jsonConfig = new JSONObject(config);
		JSONObject newConfiguration = new JSONObject();
		newConfiguration.put("age", "18");
		newConfiguration.put("size", "small");
		jsonConfig.put("configuration", newConfiguration);
		String configID3 = f.addFeatureToBranch(seasonID,BranchesRestApi.MASTER, jsonConfig.toString(), featureID3, sessionToken);
		Assert.assertFalse(configID3.contains("error"), "Configuration1 was not added to the season" + configID3);

		JSONArray attributes = new JSONArray();
		JSONObject attr1 = new JSONObject();
		attr1.put("name", "age");
		attr1.put("type", "REGULAR");
		attributes.add(attr1);
		JSONObject attr2 = new JSONObject();
		attr2.put("name", "size");
		attr2.put("type", "REGULAR");
		attributes.add(attr2);

		response = an.addAttributesToAnalytics(featureID3, BranchesRestApi.MASTER, attributes.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Feature was not added to analytics" + response);

		String respWithQuota = an.getGlobalDataCollection(seasonID, branchID, "DISPLAY", sessionToken);
		Assert.assertTrue(getDevelopmentItemsReportedToAnalytics(respWithQuota)==8, "Incorrect number of development items"); //feature1+attribute+inputfield
		Assert.assertTrue(getProductionItemsReportedToAnalytics(respWithQuota)==8, "Incorrect number of production items"); //feature1+attribute+inputfield
		respWithQuota = an.getGlobalDataCollection(seasonID,  BranchesRestApi.MASTER, "DISPLAY", sessionToken);
		Assert.assertTrue(getDevelopmentItemsReportedToAnalytics(respWithQuota)==4, "Incorrect number of development items"); //feature1+attribute+inputfield
		Assert.assertTrue(getProductionItemsReportedToAnalytics(respWithQuota)==4, "Incorrect number of production items"); //feature1+attribute+inputfield
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


	@AfterTest
	private void reset(){
		baseUtils.reset(productID, sessionToken);
	}
}
