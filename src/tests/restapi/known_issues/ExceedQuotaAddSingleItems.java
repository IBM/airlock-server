//package tests.restapi.analytics;
package tests.restapi.known_issues;

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

//known issue: exceed quota design issue. bug#10,15 Q4-2017

public class ExceedQuotaAddSingleItems {
	protected String seasonID;
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

	@Test (dependsOnMethods="addSchema", description="Set quota to 3")
	public void updateQuota() throws IOException, JSONException, InterruptedException{
		String response = an.updateQuota(seasonID, 3, sessionToken);
		Assert.assertFalse(response.contains("error"), "Quota was not returned " + response);				
	}
	
	@Test (dependsOnMethods="updateQuota", description="Add 1 input field, 1 feature, 1 attribute")
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
		configID1 = f.addFeatureToBranch(seasonID, branchID, jsonConfig.toString(), featureID1, sessionToken);
		Assert.assertFalse(configID1.contains("error"), "Configuration1 was not added to the season" + configID1);


		JSONArray attributes = new JSONArray();
		JSONObject attr1 = new JSONObject();
		attr1.put("name", "color");
		attr1.put("type", "REGULAR");
		attributes.add(attr1);
		
		response = an.addAttributesToAnalytics(featureID1, branchID, attributes.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Feature was not added to analytics" + response);

		String respWithQuota = an.getGlobalDataCollection(seasonID, branchID, "DISPLAY", sessionToken);
		Assert.assertTrue(getDevelopmentItemsReportedToAnalytics(respWithQuota)==3, "Incorrect number of development items"); //feature1+attribute+inputfield
		Assert.assertTrue(getProductionItemsReportedToAnalytics(respWithQuota)==3, "Incorrect number of production items"); //feature1+attribute+inputfield
	}
	
	@Test (dependsOnMethods="addToAnalytics", description="Add Production input field - exceeds the quota")
	public void addInputFieldOverQuota() throws JSONException{		
		//add production input field
		JSONArray inputFields = new JSONArray();
		inputFields.put("context.device.locale");
		inputFields.put("context.device.connectionType");
		inputFields.put("context.device.osVersion");
		inputFields.put("context.weatherSummary.nearestSnowAccumulation.dayPart");
		
		String response = an.updateInputFieldToAnalytics(seasonID, branchID, inputFields.toString(), sessionToken);
		Assert.assertTrue(response.contains("The maximum number"), "Field in production was added and quota was exceeded ");

		String respWithQuota = an.getGlobalDataCollection(seasonID, branchID, "DISPLAY", sessionToken);
		Assert.assertTrue(getDevelopmentItemsReportedToAnalytics(respWithQuota)==3, "Incorrect number of development items");
		Assert.assertTrue(getProductionItemsReportedToAnalytics(respWithQuota)==3, "Incorrect number of production items");

	}
	
	@Test (dependsOnMethods="addInputFieldOverQuota", description="Add Production feature - exceeds the quota")
	public void addFeatureOverQuota() throws JSONException, IOException{
		//add new feature in production
		String configuration = f.getFeatureFromBranch(configID1, branchID, sessionToken);
		JSONObject jsonCR = new JSONObject(configuration);
		jsonCR.put("stage", "PRODUCTION");
		f.updateFeatureInBranch(seasonID, branchID, configID1, jsonCR.toString(), sessionToken);
		
		String response = an.addFeatureToAnalytics(configID1, branchID, sessionToken);
		Assert.assertTrue(response.contains("The maximum number"), "Feature in production was added and quota was exceeded");	

		String respWithQuota = an.getGlobalDataCollection(seasonID, branchID, "DISPLAY", sessionToken);
		Assert.assertTrue(getDevelopmentItemsReportedToAnalytics(respWithQuota)==3, "Incorrect number of development items");
		Assert.assertTrue(getProductionItemsReportedToAnalytics(respWithQuota)==3, "Incorrect number of production items");

	}
	
	@Test (dependsOnMethods="addFeatureOverQuota", description="Add attribute - exceeds the quota")
	public void addAttributeOverQuota() throws JSONException{		
		JSONArray attributes = new JSONArray();
		JSONObject attr1 = new JSONObject();
		attr1.put("name", "size");
		attr1.put("type", "REGULAR");
		attributes.add(attr1);
		JSONObject attr2 = new JSONObject();
		attr2.put("name", "color");
		attr2.put("type", "REGULAR");
		attributes.add(attr2);
		
		String response = an.addAttributesToAnalytics(featureID1, branchID, attributes.toString(), sessionToken);
		Assert.assertTrue(response.contains("The maximum number"), "Attribute was not added to analytics" + response);

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
	
	
	@AfterTest
	private void reset(){
		baseUtils.reset(productID, sessionToken);
	}
}
