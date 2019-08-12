package tests.restapi.analytics;

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
import tests.restapi.*;


public class ChangeAttributeType {
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
	

	
	@Test (description="Change attribute type from string to array")
	public void changeAttributeFromStringToArray() throws IOException, JSONException, InterruptedException{
		
		String feature = FileUtils.fileToString(filePath + "feature1.txt", "UTF-8", false);
		featureID1 = f.addFeatureToBranch(seasonID, branchID, feature, "ROOT", sessionToken);
		Assert.assertFalse(featureID1.contains("error"), "feature was not added to the season" + featureID1);

		//add first configuration
		String config = FileUtils.fileToString(filePath + "configuration_rule1.txt", "UTF-8", false);
		JSONObject jsonConfig = new JSONObject(config);
		JSONObject newConfiguration = new JSONObject();
		newConfiguration.put("color", "red");
		jsonConfig.put("configuration", newConfiguration);
		configID1 = f.addFeatureToBranch(seasonID, branchID, jsonConfig.toString(), featureID1, sessionToken);
		Assert.assertFalse(configID1.contains("error"), "Configuration1 was not added to the season" + configID1);


		//add feature and attribute to analytics
		JSONArray attributes = new JSONArray();
		JSONObject attr1 = new JSONObject();
		attr1.put("name", "color");
		attr1.put("type", "REGULAR");
		attributes.add(attr1);

		String response = an.addAttributesToAnalytics(featureID1, branchID, attributes.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Feature was not added to analytics" + response);
		
		
		String anResponse = an.getGlobalDataCollection(seasonID, branchID, "BASIC", sessionToken);
		Assert.assertTrue(validateAttributeType(anResponse, "REGULAR"), "Incorrect attribute type in analytics");
		
		//change attribute in configuration from string to array
		String configuration = f.getFeatureFromBranch(configID1, branchID, sessionToken);
		jsonConfig = new JSONObject(configuration);
		newConfiguration = new JSONObject();		
		JSONArray arr = new JSONArray();
		arr.put("red");
		arr.put("green");
		arr.put("blue");
		newConfiguration.put("color", arr);
		jsonConfig.put("configuration", newConfiguration);

		//simulate udpate feature
		response = f.simulateUpdateFeatureInBranch(seasonID, branchID, configID1, jsonConfig.toString(), sessionToken);
		Assert.assertTrue(response.contains("warning"), "Warning was not reported in simulate update feature");
		//update feature
		response = f.updateFeatureInBranch(seasonID, branchID, configID1, jsonConfig.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Configuration was not updated " + response);
		
		anResponse = an.getGlobalDataCollection(seasonID, branchID, "BASIC", sessionToken);
		Assert.assertTrue(validateAttributeType(anResponse, "REGULAR"), "Incorrect attribute type in analytics");
		String displayResponse = an.getGlobalDataCollection(seasonID, branchID, "DISPLAY", sessionToken);
		Assert.assertTrue(validateAttributeTypeWarning(displayResponse, "ARRAY"), "Incorrect attribute type waring in analytics");
		
		//check counters
		Assert.assertTrue(getDevelopmentItemsReportedToAnalytics(displayResponse)==1, "Incorrect number of development items");
		Assert.assertTrue(getProductionItemsReportedToAnalytics(displayResponse)==0, "Incorrect number of production items");

		//move feature to production and check counters
		feature = f.getFeatureFromBranch(featureID1, branchID, sessionToken);
		JSONObject json = new JSONObject(feature);
		json.put("stage", "PRODUCTION");
		response = f.updateFeatureInBranch(seasonID, branchID, featureID1, json.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "feature was not moved to production");
		//check counters
		displayResponse = an.getGlobalDataCollection(seasonID, branchID, "DISPLAY", sessionToken);
		Assert.assertTrue(getDevelopmentItemsReportedToAnalytics(displayResponse)==1, "Incorrect number of development items");
		Assert.assertTrue(getProductionItemsReportedToAnalytics(displayResponse)==1, "Incorrect number of production items");

		//move feature to development and check counters
		feature = f.getFeatureFromBranch(featureID1, branchID, sessionToken);
		json = new JSONObject(feature);
		json.put("stage", "DEVELOPMENT");
		response = f.updateFeatureInBranch(seasonID, branchID, featureID1, json.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "feature was not moved to production");
		//check counters
		displayResponse = an.getGlobalDataCollection(seasonID, branchID, "DISPLAY", sessionToken);
		Assert.assertTrue(getDevelopmentItemsReportedToAnalytics(displayResponse)==1, "Incorrect number of development items");
		Assert.assertTrue(getProductionItemsReportedToAnalytics(displayResponse)==0, "Incorrect number of production items");
		
		int responseCode = f.deleteFeatureFromBranch(featureID1, branchID, sessionToken);
		Assert.assertTrue(responseCode == 200, "Feature was not deleted");
		
	}
	

	@Test (description="Change attribute type from array to string")
	public void changeAttributeFromArrayToString() throws IOException, JSONException, InterruptedException{
		
		String feature = FileUtils.fileToString(filePath + "feature2.txt", "UTF-8", false);
		featureID1 = f.addFeatureToBranch(seasonID, branchID, feature, "ROOT", sessionToken);
		Assert.assertFalse(featureID1.contains("error"), "feature was not added to the season" + featureID1);

		//add first configuration
		String config = FileUtils.fileToString(filePath + "configuration_rule2.txt", "UTF-8", false);
		JSONObject jsonConfig = new JSONObject(config);
		JSONObject newConfiguration = new JSONObject();
		JSONArray arr = new JSONArray();
		arr.put("red");
		arr.put("green");
		newConfiguration.put("color", arr);
		jsonConfig.put("configuration", newConfiguration);
		configID1 = f.addFeatureToBranch(seasonID, branchID, jsonConfig.toString(), featureID1, sessionToken);
		Assert.assertFalse(configID1.contains("error"), "Configuration1 was not added to the season" + configID1);


		//add feature and attribute to analytics
		JSONArray attributes = new JSONArray();
		JSONObject attr1 = new JSONObject();
		attr1.put("name", "color[0-1]");
		attr1.put("type", "ARRAY");
		attributes.add(attr1);

		String response = an.addAttributesToAnalytics(featureID1, branchID, attributes.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Feature was not added to analytics" + response);
				
		String anResponse = an.getGlobalDataCollection(seasonID, branchID, "BASIC", sessionToken);
		Assert.assertTrue(validateAttributeType(anResponse, "ARRAY"), "Incorrect attribute type in analytics");
		
		//change attribute in configuration
		String configuration = f.getFeatureFromBranch(configID1, branchID, sessionToken);
		//JSONObject origJsonConfig = new JSONObject(configuration);
		jsonConfig = new JSONObject(configuration);
		newConfiguration = new JSONObject();
		newConfiguration.put("color", "yellow");
		jsonConfig.put("configuration", newConfiguration);

		
		//simulate udpate feature
		response = f.simulateUpdateFeatureInBranch(seasonID, branchID, configID1, jsonConfig.toString(), sessionToken);
		Assert.assertTrue(response.contains("warning"), "Warning was not reported in simulate update feature ");

		//update feature
		response = f.updateFeatureInBranch(seasonID, branchID, configID1, jsonConfig.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Configuration was not updated " + response);
		
		anResponse = an.getGlobalDataCollection(seasonID, branchID, "BASIC", sessionToken);
		Assert.assertTrue(validateAttributeType(anResponse, "ARRAY"), "Incorrect attribute type in analytics");
		String displayResponse = an.getGlobalDataCollection(seasonID, branchID, "DISPLAY", sessionToken);
		Assert.assertTrue(validateAttributeTypeWarning(displayResponse, "does not exist"), "Incorrect attribute type waring in analytics");

		//move feature to production and check counters
		feature = f.getFeatureFromBranch(featureID1, branchID, sessionToken);
		JSONObject json = new JSONObject(feature);
		json.put("stage", "PRODUCTION");
		response = f.updateFeatureInBranch(seasonID, branchID, featureID1, json.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "feature was not moved to production");
		//check counters
		displayResponse = an.getGlobalDataCollection(seasonID, branchID, "DISPLAY", sessionToken);
		Assert.assertTrue(getDevelopmentItemsReportedToAnalytics(displayResponse)==2, "Incorrect number of development items");
		Assert.assertTrue(getProductionItemsReportedToAnalytics(displayResponse)==2, "Incorrect number of production items");

		//move feature to development and check counters
		feature = f.getFeatureFromBranch(featureID1, branchID, sessionToken);
		json = new JSONObject(feature);
		json.put("stage", "DEVELOPMENT");
		response = f.updateFeatureInBranch(seasonID, branchID, featureID1, json.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "feature was not moved to production");
		//check counters
		displayResponse = an.getGlobalDataCollection(seasonID, branchID, "DISPLAY", sessionToken);
		Assert.assertTrue(getDevelopmentItemsReportedToAnalytics(displayResponse)==2, "Incorrect number of development items");
		Assert.assertTrue(getProductionItemsReportedToAnalytics(displayResponse)==0, "Incorrect number of production items");
	
		
		int responseCode = f.deleteFeatureFromBranch(featureID1, branchID, sessionToken);
		Assert.assertTrue(responseCode == 200, "Feature was not deleted");
		
	}
	private boolean validateAttributeType(String analytics, String expectedType) throws JSONException{
		JSONObject json = new JSONObject(analytics);
		JSONObject analyticsDataCollection = json.getJSONObject("analyticsDataCollection");
		JSONArray featuresAttributesToAnalytics = analyticsDataCollection.getJSONArray("featuresAttributesForAnalytics");
		JSONArray attributes = featuresAttributesToAnalytics.getJSONObject(0).getJSONArray("attributes");
		if (attributes.getJSONObject(0).getString("type").equals(expectedType))
			return true;
 		
		return false;

	}
	

	private boolean validateAttributeTypeWarning(String analytics, String warningType) throws JSONException{
		JSONObject json = new JSONObject(analytics);
		JSONObject analyticsDataCollection = json.getJSONObject("analyticsDataCollection");
		JSONArray featuresAttributesToAnalytics = analyticsDataCollection.getJSONArray("analyticsDataCollectionByFeatureNames");
		JSONArray attributes = featuresAttributesToAnalytics.getJSONObject(0).getJSONArray("attributes");
		if (attributes.getJSONObject(0).getString("warning").contains(warningType))
			return true;
 		
		return false;

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
