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

//known issue: adding array to analytics, then changing the array so the quota is exceeded - allowed
public class CountersForAttributesInHierarchy {
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
	
	/*
	set quota to 3
	add feature1->config1 with 2 attributes in production
	add config1->config2 with 2 attributes (exceeds quota)
	set quota to 4
	add attributes of config2
	add feature2, move config2 to feature2, should change counters
	 */
	

	@Test (description="Set quota to 3")
	public void updateQuota() throws IOException, JSONException, InterruptedException{
		String response = an.updateQuota(seasonID, 3, sessionToken);
		Assert.assertFalse(response.contains("error"), "Quota was not returned " + response);				
	}
	
	@Test (dependsOnMethods="updateQuota", description="add feature1->config1 with 2 attributes in production")
	public void addFeatureToAnalytics() throws Exception{
		String feature1 = FileUtils.fileToString(filePath + "feature1.txt", "UTF-8", false);
	    JSONObject jsonF1 = new JSONObject(feature1);
	    jsonF1.put("stage", "PRODUCTION");
		featureID1 = f.addFeatureToBranch(seasonID, branchID, jsonF1.toString(), "ROOT", sessionToken);
		Assert.assertFalse(featureID1.contains("error"), "Feature was not added to the season" + featureID1);
		
		//
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
		JSONObject attr2 = new JSONObject();
		attr2.put("name", "size");
		attr2.put("type", "REGULAR");
		attributes.add(attr2);
		String response = an.getGlobalDataCollection(seasonID, branchID, "BASIC", sessionToken);
		String input = an.addFeaturesAttributesToAnalytics(response, featureID1, attributes);
		response = an.updateGlobalDataCollection(seasonID, branchID, input, sessionToken);		
		Assert.assertFalse(response.contains("error"), "Attributes were not added to analytics " + response);
		
		String respWithQuota = an.getGlobalDataCollection(seasonID, branchID, "DISPLAY", sessionToken);
		Assert.assertTrue(getDevelopmentItemsReportedToAnalytics(respWithQuota)==2, "Incorrect number of development items");
		Assert.assertTrue(getProductionItemsReportedToAnalytics(respWithQuota)==2, "Incorrect number of production items");


	}
	
	@Test (dependsOnMethods="addFeatureToAnalytics", description="add config1->config2 with 2 attributes (exceeds quota)")
	public void addConfiguration() throws Exception{		
		//add second configuration
		String config = FileUtils.fileToString(filePath + "configuration_rule2.txt", "UTF-8", false);
		JSONObject jsonConfig = new JSONObject(config);
		JSONObject newConfiguration = new JSONObject();
		newConfiguration.put("title", "new title");
		newConfiguration.put("icon", "a.fig");
		jsonConfig.put("configuration", newConfiguration);
		configID2 = f.addFeatureToBranch(seasonID, branchID, jsonConfig.toString(), configID1, sessionToken);
		Assert.assertFalse(configID2.contains("error"), "Configuration2 was not added to the season" + configID2);
			

		JSONArray attributes = new JSONArray();
		JSONObject attr1 = new JSONObject();
		attr1.put("name", "title");
		attr1.put("type", "REGULAR");
		attributes.add(attr1);
		JSONObject attr2 = new JSONObject();
		attr2.put("name", "icon");
		attr2.put("type", "REGULAR");
		attributes.add(attr2);
		JSONObject attr3 = new JSONObject();
		attr3.put("name", "color");
		attr3.put("type", "REGULAR");
		attributes.add(attr3);
		JSONObject attr4 = new JSONObject();
		attr4.put("name", "size");
		attr4.put("type", "REGULAR");
		attributes.add(attr4);

		String responseGet = an.getGlobalDataCollection(seasonID, branchID, "BASIC", sessionToken);
		String response = an.addAttributesToAnalytics(featureID1, branchID, attributes.toString(), sessionToken);

		if(m_branchType.equals("Master")|| m_branchType.equals("ProdExp")) {
			Assert.assertTrue(response.contains("The maximum"), "Attributes were added to analytics and quota exceeded");
			String respWithQuota = an.getGlobalDataCollection(seasonID, branchID, "DISPLAY", sessionToken);
			Assert.assertTrue(getDevelopmentItemsReportedToAnalytics(respWithQuota)==2, "Incorrect number of development items");
			Assert.assertTrue(getProductionItemsReportedToAnalytics(respWithQuota)==2, "Incorrect number of production items");
		}
		else{
			Assert.assertFalse(response.contains("error"), "Attributes were added to analytics and quota exceeded in dev");
			String respWithQuota = an.getGlobalDataCollection(seasonID, branchID, "DISPLAY", sessionToken);
			Assert.assertTrue(getDevelopmentItemsReportedToAnalytics(respWithQuota)==4, "Incorrect number of development items");
			Assert.assertTrue(getProductionItemsReportedToAnalytics(respWithQuota)==4, "Incorrect number of production items");
			JSONArray attributes2 = new JSONArray();
			JSONObject attr12 = new JSONObject();
			attr12.put("name", "color");
			attr12.put("type", "REGULAR");
			attributes2.add(attr12);
			JSONObject attr22 = new JSONObject();
			attr22.put("name", "size");
			attr22.put("type", "REGULAR");
			attributes2.add(attr22);
			response = an.addAttributesToAnalytics(featureID1, branchID, attributes2.toString(), sessionToken);
			Assert.assertFalse(response.contains("error"), "Attributes were added to analytics and quota exceeded in dev");
			respWithQuota = an.getGlobalDataCollection(seasonID, branchID, "DISPLAY", sessionToken);
			Assert.assertTrue(getDevelopmentItemsReportedToAnalytics(respWithQuota)==2, "Incorrect number of development items");
			Assert.assertTrue(getProductionItemsReportedToAnalytics(respWithQuota)==2, "Incorrect number of production items");

		}
	}
	
	

	@Test (dependsOnMethods="addConfiguration", description="Set quota to 3")
	public void updateQuota2() throws IOException, JSONException, InterruptedException{
		String response = an.updateQuota(seasonID, 4, sessionToken);
		Assert.assertFalse(response.contains("error"), "Quota was not returned " + response);				
	}
	
	
	@Test (dependsOnMethods="updateQuota2", description="add config2 attributes to analytics")
	public void addAttributesToAnalytics() throws Exception{		

		JSONArray attributes = new JSONArray();
		JSONObject attr1 = new JSONObject();
		attr1.put("name", "title");
		attr1.put("type", "REGULAR");
		attributes.add(attr1);
		JSONObject attr2 = new JSONObject();
		attr2.put("name", "icon");
		attr2.put("type", "REGULAR");
		attributes.add(attr2);
		JSONObject attr3 = new JSONObject();
		attr3.put("name", "color");
		attr3.put("type", "REGULAR");
		attributes.add(attr3);
		JSONObject attr4 = new JSONObject();
		attr4.put("name", "size");
		attr4.put("type", "REGULAR");
		attributes.add(attr4);

		String response = an.getGlobalDataCollection(seasonID, branchID, "BASIC", sessionToken);
		response = an.addAttributesToAnalytics(featureID1, branchID, attributes.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Attributes were not added to analytics and quota exceeded");
		
		String respWithQuota = an.getGlobalDataCollection(seasonID, branchID, "DISPLAY", sessionToken);
		Assert.assertTrue(getDevelopmentItemsReportedToAnalytics(respWithQuota)==4, "Incorrect number of development items");
		Assert.assertTrue(getProductionItemsReportedToAnalytics(respWithQuota)==4, "Incorrect number of production items");


	}
	
	
	@Test (dependsOnMethods="addAttributesToAnalytics", description="Add feature2 and change parent of config2")
	public void changeParent() throws Exception{		
		String feature2 = FileUtils.fileToString(filePath + "feature2.txt", "UTF-8", false);
		featureID2 = f.addFeatureToBranch(seasonID, branchID, feature2, "ROOT", sessionToken);
		Assert.assertFalse(featureID2.contains("error"), "feature was not added to the season" + featureID2);

		feature2 = f.getFeatureFromBranch(featureID2, branchID, sessionToken);
		String config = f.getFeatureFromBranch(configID2, branchID, sessionToken);
		JSONObject feature2Json = new JSONObject(feature2);
		JSONArray configurationRules = new JSONArray();
		configurationRules.add(new JSONObject(config));
		feature2Json.put("configurationRules", configurationRules);
		
		String response = f.updateFeatureInBranch(seasonID, branchID, featureID2, feature2Json.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "configuration rule was not moved to feature2 " + response);
			
		String respWithQuota = an.getGlobalDataCollection(seasonID, branchID, "DISPLAY", sessionToken);
		Assert.assertTrue(getDevelopmentItemsReportedToAnalytics(respWithQuota)==4, "Incorrect number of development items");
		Assert.assertTrue(getProductionItemsReportedToAnalytics(respWithQuota)==4, "Incorrect number of production items");


	}
	
	@Test (dependsOnMethods="changeParent", description="Add default configuraiton to feature1")
	public void addDefaultConfiguration() throws Exception{		
		//update configuration
		String configuration = f.getFeatureFromBranch(configID1, branchID, sessionToken);
		JSONObject jsonConfig = new JSONObject(configuration);
		JSONObject newConfiguration = new JSONObject();
		newConfiguration.put("color", "white");
		jsonConfig.put("configuration", newConfiguration);

		//update feature - add defaultConfiguration
		String feature = f.getFeatureFromBranch(featureID1, branchID, sessionToken);
		JSONObject json = new JSONObject(feature);
		JSONObject defaultConfiguration = new JSONObject();
		defaultConfiguration.put("color", "red");
		defaultConfiguration.put("size", "small");
		json.put("defaultConfiguration", defaultConfiguration);
		JSONArray configurationRules = new JSONArray();
		configurationRules.put(jsonConfig);
		json.put("configurationRules", configurationRules);
		
		//update feature tree
		featureID1 = f.updateFeatureInBranch(seasonID, branchID, featureID1, json.toString(), sessionToken);
		Assert.assertFalse(featureID1.contains("error"), "feature was not updated" + featureID1);
		
		String respWithQuota = an.getGlobalDataCollection(seasonID, branchID, "DISPLAY", sessionToken);
		Assert.assertTrue(getDevelopmentItemsReportedToAnalytics(respWithQuota)==4, "Incorrect number of development items");
		Assert.assertTrue(getProductionItemsReportedToAnalytics(respWithQuota)==4, "Incorrect number of production items");

	}
	
	@Test (dependsOnMethods="addDefaultConfiguration", description="Delete default configuraiton from feature1")
	public void deleteDefaultConfiguration() throws Exception{		

		//update feature - remove defaultConfiguration
		String feature = f.getFeatureFromBranch(featureID1, branchID, sessionToken);
		JSONObject json = new JSONObject(feature);
		json.put("defaultConfiguration", new JSONObject());
		
		//update feature tree
		featureID1 = f.updateFeatureInBranch(seasonID, branchID, featureID1, json.toString(), sessionToken);
		Assert.assertFalse(featureID1.contains("error"), "feature was not updated" + featureID1);
		
		String respWithQuota = an.getGlobalDataCollection(seasonID, branchID, "DISPLAY", sessionToken);
		Assert.assertTrue(getDevelopmentItemsReportedToAnalytics(respWithQuota)==4, "Incorrect number of development items");
		Assert.assertTrue(getProductionItemsReportedToAnalytics(respWithQuota)==4, "Incorrect number of production items");

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
