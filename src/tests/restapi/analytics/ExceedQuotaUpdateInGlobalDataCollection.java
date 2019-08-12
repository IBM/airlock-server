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

public class ExceedQuotaUpdateInGlobalDataCollection {
	protected String seasonID;
	protected String branchID;
	protected String productID;
	protected String featureID1;
	protected String featureID2;
	protected String configID1;
	protected String configID2;
	protected String filePath;
	protected String m_url;
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
	
	@Test ( description="Add input schema to the season")
	public void addSchema() throws Exception{
		String sch = schema.getInputSchema(seasonID, sessionToken);
        JSONObject jsonSchema = new JSONObject(sch);
        String schemaBody = FileUtils.fileToString(filePath + "inputSchemas/inputSchema_optionalField_unitsOfMeasure.txt", "UTF-8", false);
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
		//add input field
		String response = an.getGlobalDataCollection(seasonID, branchID, "BASIC", sessionToken);
		String input = an.addInputFieldsToAnalytics(response, "context.device.locale");
		response = an.updateGlobalDataCollection(seasonID, branchID, input, sessionToken);
		Assert.assertFalse(response.contains("error"), "Analytics was not updated" + response);
		response = an.getGlobalDataCollection(seasonID, branchID, "BASIC", sessionToken);
		input = an.addInputFieldsToAnalytics(response, "context.device.osVersion");
		response = an.updateGlobalDataCollection(seasonID, branchID, input, sessionToken);
		
		//add feature
		String feature1 = FileUtils.fileToString(filePath + "feature1.txt", "UTF-8", false);
		featureID1 = f.addFeatureToBranch(seasonID, branchID, feature1, "ROOT", sessionToken);
		Assert.assertFalse(featureID1.contains("error"), "Feature was not added to the season" + featureID1);
		response = an.getGlobalDataCollection(seasonID, branchID, "BASIC", sessionToken);	
		input = an.addFeatureOnOff(response, featureID1);
		response = an.updateGlobalDataCollection(seasonID, branchID, input, sessionToken);
		Assert.assertFalse(response.contains("error"), "Incorrect globalDataCollection response");
		
		String feature2 = FileUtils.fileToString(filePath + "feature2.txt", "UTF-8", false);
		featureID2 = f.addFeatureToBranch(seasonID, branchID, feature2, "ROOT", sessionToken);
		Assert.assertFalse(featureID2.contains("error"), "Feature was not added to the season" + featureID2);
		response = an.getGlobalDataCollection(seasonID, branchID, "BASIC", sessionToken);	
		input = an.addFeatureOnOff(response, featureID2);
		response = an.updateGlobalDataCollection(seasonID, branchID, input, sessionToken);
		Assert.assertFalse(response.contains("error"), "Incorrect globalDataCollection response");	
		
		//add attribute
		String config = FileUtils.fileToString(filePath + "configuration_rule1.txt", "UTF-8", false);
		JSONObject jsonConfig = new JSONObject(config);
		JSONObject newConfiguration = new JSONObject();
		newConfiguration.put("color", "red");
		newConfiguration.put("size", "small");
		jsonConfig.put("configuration", newConfiguration);
		configID1 = f.addFeatureToBranch(seasonID, branchID, jsonConfig.toString(), featureID1, sessionToken);
		Assert.assertFalse(configID1.contains("error"), "Configuration1 was not added to the season" + configID1);
			
		response = an.getGlobalDataCollection(seasonID, branchID, "BASIC", sessionToken);

		JSONArray attributes = new JSONArray();
		JSONObject attr1 = new JSONObject();
		attr1.put("name", "color");
		attr1.put("type", "REGULAR");
		attributes.add(attr1);

		JSONObject attr2 = new JSONObject();
		attr2.put("name", "size");
		attr2.put("type", "REGULAR");
		attributes.add(attr2);
		input = an.addFeaturesAttributesToAnalytics(response, featureID1, attributes);
		response = an.updateGlobalDataCollection(seasonID, branchID, input, sessionToken);
		Assert.assertFalse(response.contains("error"), "Feature was not added to analytics" + response);

		String respWithQuota = an.getGlobalDataCollection(seasonID, branchID, "DISPLAY", sessionToken);
		Assert.assertTrue(getDevelopmentItemsReportedToAnalytics(respWithQuota)==6, "Incorrect number of development items");
		Assert.assertTrue(getProductionItemsReportedToAnalytics(respWithQuota)==0, "Incorrect number of production items");
	}
	
	@Test (dependsOnMethods="addToAnalytics", description="Update to production 2 input fields, 1 feature, 1 attribute")
	public void updateFieldsToProduction() throws Exception{
        String feature = f.getFeatureFromBranch(featureID1, branchID, sessionToken);
		JSONObject jsonF = new JSONObject(feature);
		jsonF.put("stage", "PRODUCTION");
		String response = f.updateFeatureInBranch(seasonID, branchID, featureID1, jsonF.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Feature was not updated to production" + response);

		String respWithQuota = an.getGlobalDataCollection(seasonID, branchID, "DISPLAY", sessionToken);
		Assert.assertTrue(getDevelopmentItemsReportedToAnalytics(respWithQuota)==6, "Incorrect number of development items");
		Assert.assertTrue(getProductionItemsReportedToAnalytics(respWithQuota)==3, "Incorrect number of production items");
		String sch = schema.getInputSchema(seasonID, sessionToken);
		JSONObject jsonSchema = new JSONObject(sch);
		String schemaBody = FileUtils.fileToString(filePath + "inputSchemas/inputSchema_update_device_locale_to_production.txt", "UTF-8", false);
		jsonSchema.put("inputSchema", new JSONObject(schemaBody));

		String validateSchema = schema.validateSchema(seasonID, schemaBody, sessionToken);
		JSONObject validateJson = new JSONObject(validateSchema);
		if(m_branchType.equals("Master")|| m_branchType.equals("ProdExp")) {
			Assert.assertTrue(validateJson.containsKey("productionAnalyticsItemsQuotaExceeded"), "Validate schema didn't provide information on exceeded quota");
			response = schema.updateInputSchema(seasonID, jsonSchema.toString(), sessionToken);
			Assert.assertTrue(response.contains("error"), "Schema was added to the season" + response);
		}
		else{
			Assert.assertFalse(validateJson.containsKey("productionAnalyticsItemsQuotaExceeded"), "Validate schema didn't provide information on exceeded quota");
			response = schema.updateInputSchema(seasonID, jsonSchema.toString(), sessionToken);
			Assert.assertFalse(response.contains("error"), "Schema was not added to the season" + response);
		}
	}
	
	@Test (dependsOnMethods="updateFieldsToProduction", description="Add Production input field to reach the quota")
	public void addInputFieldInProduction() throws JSONException{		
		//add production input field
		String response = an.getGlobalDataCollection(seasonID, branchID, "BASIC", sessionToken);
		String input = an.addInputFieldsToAnalytics(response, "context.device.osVersion");
		response = an.updateGlobalDataCollection(seasonID, branchID, input, sessionToken);		
		String respWithQuota = an.getGlobalDataCollection(seasonID, branchID, "DISPLAY", sessionToken);
		
		if(m_branchType.equals("Master")|| m_branchType.equals("ProdExp")) {
			Assert.assertTrue(getDevelopmentItemsReportedToAnalytics(respWithQuota)==6, "Incorrect number of development items");
			Assert.assertTrue(getProductionItemsReportedToAnalytics(respWithQuota)==3, "Incorrect number of production items");
		}
		else {
			Assert.assertTrue(getDevelopmentItemsReportedToAnalytics(respWithQuota)==6, "Incorrect number of development items");
			Assert.assertTrue(getProductionItemsReportedToAnalytics(respWithQuota)==5, "Incorrect number of production items");
		}
	}
	
	@Test (dependsOnMethods="addInputFieldInProduction", description="Update feature2 to production - exceeds the quota")
	public void addFeatureToBranchOverQuota() throws JSONException, IOException{
	     String feature = f.getFeatureFromBranch(featureID2, branchID, sessionToken);
	      JSONObject jsonF = new JSONObject(feature);
	      jsonF.put("stage", "PRODUCTION");
	      String response = f.updateFeatureInBranch(seasonID, branchID, featureID2, jsonF.toString(), sessionToken);
		if(m_branchType.equals("Master")|| m_branchType.equals("ProdExp")) {
			Assert.assertTrue(response.contains("The maximum number"), "Feature was updated to production" + response);
			String respWithQuota = an.getGlobalDataCollection(seasonID, branchID, "DISPLAY", sessionToken);
			Assert.assertTrue(getDevelopmentItemsReportedToAnalytics(respWithQuota) == 6, "Incorrect number of development items");
			Assert.assertTrue(getProductionItemsReportedToAnalytics(respWithQuota) == 3, "Incorrect number of production items");
		}
		else{
			Assert.assertFalse(response.contains("error"), "Feature was not updated to production" + response);
			String respWithQuota = an.getGlobalDataCollection(seasonID, branchID, "DISPLAY", sessionToken);
			Assert.assertTrue(getDevelopmentItemsReportedToAnalytics(respWithQuota) == 6, "Incorrect number of development items");
			Assert.assertTrue(getProductionItemsReportedToAnalytics(respWithQuota) == 6, "Incorrect number of production items");
		}
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
