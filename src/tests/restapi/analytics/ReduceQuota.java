package tests.restapi.analytics;

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

public class ReduceQuota {
	protected String seasonID;
	protected String branchID;
	protected String productID;
	protected String featureID1;
	protected String featureID2;
	protected String configID1;
	protected String configID2;
	protected String filePath;
	protected String m_url;
	protected boolean skip = false;
	protected String m_branchType;
	protected ProductsRestApi p;
	protected FeaturesRestApi f;
	protected AnalyticsRestApi an;
	protected InputSchemaRestApi schema;
	private String sessionToken = "";
	protected AirlockUtils baseUtils;
	
	@BeforeClass
	@Parameters({"url", "analyticsUrl", "translationsUrl", "configPath", "sessionToken", "userName", "userPassword", "appName", "productsToDeleteFile", "branchType"})
	public void init(String url, String analyticsUrl, String translationsUrl, String configPath, String sToken, String userName, String userPassword, String appName, String productsToDeleteFile, String branchType) throws IOException{
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
		m_branchType = branchType;
		try {
			if(branchType.equals("Master")) {
				branchID = BranchesRestApi.MASTER;
			}
			else if(branchType.equals("StandAlone")) {
				branchID = baseUtils.addBranchFromBranch("branch1",BranchesRestApi.MASTER,seasonID);
			}
			else if(branchType.equals("DevExp")) {
				branchID = baseUtils.createBranchInExperiment(analyticsUrl);
			}
			else if(branchType.equals("ProdExp")) {
				branchID = baseUtils.createBranchInProdExperiment(analyticsUrl).getString("brId");
			}
			else{
				branchID = null;
			}
		}catch (Exception e){
			branchID = null;
		}
	}
	
	/*
	 *  set quota to 4
	 *  add 5 items in production to analytics
	 *  reduce quota to 2
	 *  add item in production to analytics - error expected
	 *  delete items until only 1 is left
	 *  add item in production to analytics - success expected
	 */
	
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
	
	@Test (dependsOnMethods="updateQuota", description="Add items to analytics above quota")
	public void addToAnalytics() throws Exception{
		
		//add 2 features
		String feature1 = FileUtils.fileToString(filePath + "feature1.txt", "UTF-8", false);
		JSONObject jsonF1 = new JSONObject(feature1);
		jsonF1.put("stage", "PRODUCTION");
		featureID1 = f.addFeatureToBranch(seasonID, branchID, jsonF1.toString(), "ROOT", sessionToken);
		Assert.assertFalse(featureID1.contains("error"), "Feature was not added to the season" + featureID1);
		
		String feature2 = FileUtils.fileToString(filePath + "feature2.txt", "UTF-8", false);
		JSONObject jsonF2 = new JSONObject(feature2);
		jsonF2.put("stage", "PRODUCTION");
		featureID2 = f.addFeatureToBranch(seasonID, branchID, jsonF2.toString(), "ROOT", sessionToken);
		Assert.assertFalse(featureID2.contains("error"), "Feature was not added to the season" + featureID2);
		
		//add 2 input fields and 2 features to analytics
		String response = an.getGlobalDataCollection(seasonID, branchID, "BASIC", sessionToken);	
		String input = an.addInputFieldsToAnalytics(response, "context.device.locale");
		input = an.addInputFieldsToAnalytics(input, "context.device.osVersion");
		input = an.addFeatureOnOff(input, featureID1);
		input = an.addFeatureOnOff(input, featureID2);
		response = an.updateGlobalDataCollection(seasonID, branchID, input, sessionToken);
		Assert.assertFalse(response.contains("error"), "Incorrect globalDataCollection response");
		
		String respWithQuota = an.getGlobalDataCollection(seasonID, branchID, "DISPLAY", sessionToken);
		Assert.assertTrue(getDevelopmentItemsReportedToAnalytics(respWithQuota)==4, "Incorrect number of development items");
		Assert.assertTrue(getProductionItemsReportedToAnalytics(respWithQuota)==4, "Incorrect number of production items");
		
	}

	@Test (dependsOnMethods="addToAnalytics", description="Set quota to 2")
	public void reduceQuota() throws IOException, JSONException, InterruptedException{
		String response = an.updateQuota(seasonID, 2, sessionToken);
		if(!m_branchType.equals("ProdExp")) {
			Assert.assertFalse(response.contains("error"), "Quota was not returned " + response);
		}else{
			skip = true;
			Assert.assertTrue(response.contains("is included in an experiment in production and reducing the quota caused experiment"), "Quota was not returned " + response);
		}
	}

	@Test (dependsOnMethods="reduceQuota", description="Update input schema fields to development")
	public void moveInputSchemaToDevelopment() throws Exception{
		if(skip){
			return;
		}
		String sch = schema.getInputSchema(seasonID, sessionToken);
        JSONObject jsonSchema = new JSONObject(sch);
        
        String schemaBody = FileUtils.fileToString(filePath + "inputSchemas/inputSchema_optionalField_unitsOfMeasure.txt", "UTF-8", false);
        jsonSchema.put("inputSchema", new JSONObject(schemaBody));
        String response = schema.updateInputSchema(seasonID, jsonSchema.toString(), sessionToken);
        Assert.assertFalse(response.contains("error"), "Schema was not added to the season" + response);
		
        String respWithQuota = an.getGlobalDataCollection(seasonID, branchID, "DISPLAY", sessionToken);
		Assert.assertTrue(getDevelopmentItemsReportedToAnalytics(respWithQuota)==4, "Incorrect number of development items");
		Assert.assertTrue(getProductionItemsReportedToAnalytics(respWithQuota)==2, "Incorrect number of production items");

	}
	
	@Test (dependsOnMethods="moveInputSchemaToDevelopment", description="Move feature to development")
	public void moveFeatureToDevelopment() throws Exception{
		if(skip){
			return;
		}
		String feature = f.getFeatureFromBranch(featureID2, branchID, sessionToken);
		JSONObject jsonF = new JSONObject(feature);
		jsonF.put("stage", "DEVELOPMENT");
		featureID2 = f.updateFeatureInBranch(seasonID, branchID, featureID2, jsonF.toString(), sessionToken);
		Assert.assertFalse(featureID1.contains("error"), "Feature was not updated " + featureID2);
			
		String respWithQuota = an.getGlobalDataCollection(seasonID, branchID, "DISPLAY", sessionToken);
		Assert.assertTrue(getDevelopmentItemsReportedToAnalytics(respWithQuota)==4, "Incorrect number of development items");
		Assert.assertTrue(getProductionItemsReportedToAnalytics(respWithQuota)==1, "Incorrect number of production items");

	}
	
	@Test (dependsOnMethods="moveFeatureToDevelopment", description="Add attribute for production feature")
	public void addAttribute() throws Exception{
		if(skip){
			return;
		}
		String config = FileUtils.fileToString(filePath + "configuration_rule1.txt", "UTF-8", false);
		JSONObject jsonConfig = new JSONObject(config);
		JSONObject newConfiguration = new JSONObject();
		newConfiguration.put("color", "red");
		jsonConfig.put("configuration", newConfiguration);
		configID1 = f.addFeatureToBranch(seasonID, branchID, jsonConfig.toString(), featureID1, sessionToken);
		Assert.assertFalse(configID1.contains("error"), "Configuration1 was not added to the season" + configID1);
			
		String response = an.getGlobalDataCollection(seasonID, branchID, "BASIC", sessionToken);

		JSONArray attributes = new JSONArray();
		JSONObject attr1 = new JSONObject();
		attr1.put("name", "color");
		attr1.put("type", "REGULAR");
		attributes.add(attr1);

		String input = an.addFeaturesAttributesToAnalytics(response, featureID1, attributes);
		response = an.updateGlobalDataCollection(seasonID, branchID, input, sessionToken);
		Assert.assertFalse(response.contains("error"), "Feature was not added to analytics" + response);

		String respWithQuota = an.getGlobalDataCollection(seasonID, branchID, "DISPLAY", sessionToken);
		Assert.assertTrue(getDevelopmentItemsReportedToAnalytics(respWithQuota)==5, "Incorrect number of development items");
		Assert.assertTrue(getProductionItemsReportedToAnalytics(respWithQuota)==2, "Incorrect number of production items");

	}
	
	@Test (dependsOnMethods="addAttribute", description="Set quota to 0 (can't add items to analytics)")
	public void reduceQuotaToZero() throws IOException, JSONException, InterruptedException{
		if(skip){
			return;
		}
		String response = an.updateQuota(seasonID, 0, sessionToken);
		Assert.assertFalse(response.contains("error"), "Quota was not returned " + response);				
	}
	
	@Test (dependsOnMethods="reduceQuotaToZero", description="Delete all items from analytics")
	public void deleteFromAnalytics() throws IOException, JSONException, InterruptedException{
		if(skip){
			return;
		}
		String response = an.getGlobalDataCollection(seasonID, branchID, "BASIC", sessionToken);
		JSONObject json = new JSONObject(response);
		JSONObject analyticsDataCollection = json.getJSONObject("analyticsDataCollection");
		analyticsDataCollection.put("featuresAttributesForAnalytics", new JSONArray()); //remove attributes
		analyticsDataCollection.put("featuresAndConfigurationsForAnalytics", new JSONArray()); //remove features
		analyticsDataCollection.put("inputFieldsForAnalytics", new JSONArray()); //remove inputfields
		response = an.updateGlobalDataCollection(seasonID, branchID, json.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Analytics was not updated " + response);
		
	    String respWithQuota = an.getGlobalDataCollection(seasonID, branchID, "DISPLAY", sessionToken);
	    Assert.assertTrue(getDevelopmentItemsReportedToAnalytics(respWithQuota)==0, "Incorrect number of development items");
		Assert.assertTrue(getProductionItemsReportedToAnalytics(respWithQuota)==0, "Incorrect number of production items");

	}
	
	@Test (dependsOnMethods="deleteFromAnalytics", description="Add feature in production to analytics when quota=0")
	public void addFeatureToAnalytics() throws IOException, JSONException, InterruptedException{
		if(skip){
			return;
		}
		String response = an.getGlobalDataCollection(seasonID, branchID, "BASIC", sessionToken);	
		String input = an.addFeatureOnOff(response, featureID1);
		response = an.updateGlobalDataCollection(seasonID, branchID, input, sessionToken);
		Assert.assertTrue(response.contains("The maximum"), "Zero quota exceeded ");
		
		String respWithQuota = an.getGlobalDataCollection(seasonID, branchID, "DISPLAY", sessionToken);
		Assert.assertTrue(getDevelopmentItemsReportedToAnalytics(respWithQuota)==0, "Incorrect number of development items");
		Assert.assertTrue(getProductionItemsReportedToAnalytics(respWithQuota)==0, "Incorrect number of production items");

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
