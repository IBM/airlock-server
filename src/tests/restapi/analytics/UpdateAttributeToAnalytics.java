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


public class UpdateAttributeToAnalytics {
	protected String seasonID;
	protected String branchID;
	protected String productID;
	protected String featureID1;
	protected String featureID2;
	protected String mixID;
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
	
	/*Test flow:
	- in mode VALIDATE check warning, prod/dev runtime shouldn't be touched
	- update several fields in config (i.e. Percentage & attributes, both should be updated)

	- update feature and config as a tree
	- update feature->mix of configs->config
	
	- update config to remove all attributes  and check that in analytics no field remains for this feature
	- update several features with configs (for example under mix), so that some attributes will be updated and some removed (removed should be first/last)

	 */
	


	@Test (description="Add feature and 2 configuration rules to the season and to analytics")
	public void addComponents() throws IOException, JSONException, InterruptedException{
		String feature = FileUtils.fileToString(filePath + "feature1.txt", "UTF-8", false);
		featureID1 = f.addFeatureToBranch(seasonID, branchID, feature, "ROOT", sessionToken);
		Assert.assertFalse(featureID1.contains("error"), "feature was not added to the season" + featureID1);
		
		//add first configuration
		String config = FileUtils.fileToString(filePath + "configuration_rule1.txt", "UTF-8", false);
		JSONObject jsonConfig = new JSONObject(config);
		JSONObject newConfiguration = new JSONObject();
		newConfiguration.put("color", "red");
		newConfiguration.put("size", "small");
		newConfiguration.put("text", "medium");	
		jsonConfig.put("configuration", newConfiguration);
		configID1 = f.addFeatureToBranch(seasonID, branchID, jsonConfig.toString(), featureID1, sessionToken);
		Assert.assertFalse(configID1.contains("error"), "Configuration1 was not added to the season" + configID1);
		

		//add feature and attribute to analytics
		String response = an.getGlobalDataCollection(seasonID, branchID, "BASIC", sessionToken);
		JSONArray attributes = new JSONArray();
		JSONObject attr1 = new JSONObject();
		attr1.put("name", "color");
		attr1.put("type", "REGULAR");
		attributes.add(attr1);
		JSONObject attr2 = new JSONObject();
		attr2.put("name", "size");
		attr2.put("type", "REGULAR");
		attributes.add(attr2);
		JSONObject attr3 = new JSONObject();
		attr3.put("name", "text");
		attr3.put("type", "REGULAR");
		attributes.add(attr3);
		String input = an.addFeaturesAttributesToAnalytics(response, featureID1, attributes);
		response = an.updateGlobalDataCollection(seasonID, branchID, input, sessionToken);
		Assert.assertFalse(response.contains("error"), "Feature was not added to analytics" + response);
		
		String anResponse = an.getGlobalDataCollection(seasonID, branchID, "BASIC", sessionToken);
		Assert.assertTrue(validateAttributeInAnalytics(anResponse).size()==3, "Incorrect number of attributes in analytics");

		String display = an.getGlobalDataCollection(seasonID, branchID, "DISPLAY", sessionToken);
		Assert.assertTrue(an.validateDataCollectionByFeatureNames(display, anResponse).equals("true"));

	}
	
	@Test (dependsOnMethods="addComponents", description="Simulate update configuration in configuration rule")
	public void simulateUpdateConfigurationAttribute() throws JSONException, IOException, InterruptedException{
		String dateFormat = an.setDateFormat();
		
		String configuration = f.getFeatureFromBranch(configID1, branchID, sessionToken);
		JSONObject jsonConfig = new JSONObject(configuration);
		JSONObject newConfiguration = new JSONObject();
		newConfiguration.put("newsize", "medium");
		jsonConfig.put("configuration", newConfiguration);
		String response = f.simulateUpdateFeatureInBranch(seasonID, branchID, configID1, jsonConfig.toString(), sessionToken);
		Assert.assertTrue(response.contains("warning"), "Configuration was updated " + response);
		
		String anResponse = an.getGlobalDataCollection(seasonID, branchID, "BASIC", sessionToken);
		Assert.assertTrue(validateAttributeInAnalytics(anResponse).contains("color"), "Attribute was changed in VALIDATE mode");
		Assert.assertTrue(validateAttributeInAnalytics(anResponse).contains("size"), "Attribute was changed in VALIDATE mode");
		
		String display = an.getGlobalDataCollection(seasonID, branchID, "DISPLAY", sessionToken);
		Assert.assertTrue(an.validateDataCollectionByFeatureNames(display, anResponse).equals("true"));

		//check if files were changed
		an.setSleep();
		RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getDevelopmentBranchFileDateModification(m_url, productID, seasonID, branchID, dateFormat, sessionToken);
		Assert.assertTrue(responseDev.code ==304, "Runtime development feature file was updated");		
		RuntimeRestApi.DateModificationResults responseProd = RuntimeDateUtilities.getProductionBranchFileDateModification(m_url, productID, seasonID, branchID, dateFormat, sessionToken);
		Assert.assertTrue(responseProd.code ==304, "Runtime production feature file was changed");
		RuntimeRestApi.DateModificationResults prodChanged = RuntimeDateUtilities.getProductionChangedDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(prodChanged.code ==304, "productionChanged.txt file was changed");

	}
	
	@Test (dependsOnMethods="simulateUpdateConfigurationAttribute", description="Update several fields in configuration rule")
	public void updateConfigurationAttribute() throws JSONException, IOException, InterruptedException{
		String dateFormat = an.setDateFormat();
		String configuration = f.getFeatureFromBranch(configID1, branchID, sessionToken);
		//JSONObject origJsonConfig = new JSONObject(configuration);
		JSONObject jsonConfig = new JSONObject(configuration);
		JSONObject newConfiguration = new JSONObject();
		newConfiguration.put("text", "medium");	
		newConfiguration.put("color", "white");
		newConfiguration.put("newsize", "medium");
		jsonConfig.put("configuration", newConfiguration);
		jsonConfig.put("rolloutPercentage", 50);
		String response = f.updateFeatureInBranch(seasonID, branchID, configID1, jsonConfig.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Configuration was not updated " + response);
		
		String anResponse = an.getGlobalDataCollection(seasonID, branchID, "BASIC", sessionToken);
		
		Assert.assertTrue(validateAttributeInAnalytics(anResponse).contains("color"), "Attribute 'color' was removed");
		Assert.assertTrue(validateAttributeInAnalytics(anResponse).contains("size"), "Attribute 'size' was removed");	//updated attributes shouldn't be removed
		Assert.assertTrue(validateAttributeInAnalytics(anResponse).contains("text"), "Attribute 'text' was removed");
		
		String display = an.getGlobalDataCollection(seasonID, branchID, "DISPLAY", sessionToken);
		Assert.assertTrue(an.validateDataCollectionByFeatureNames(display, anResponse).equals("true"));


		//check if files were changed
		an.setSleep();
		RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getDevelopmentBranchFileDateModification(m_url, productID, seasonID, branchID, dateFormat, sessionToken);
		Assert.assertTrue(responseDev.code ==200, "Runtime development feature file was not updated");		
		RuntimeRestApi.DateModificationResults responseProd = RuntimeDateUtilities.getProductionBranchFileDateModification(m_url, productID, seasonID, branchID, dateFormat, sessionToken);
		Assert.assertTrue(responseProd.code ==304, "Runtime production feature file was changed");
		RuntimeRestApi.DateModificationResults prodChanged = RuntimeDateUtilities.getProductionChangedDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(prodChanged.code ==304, "productionChanged.txt file was changed");

	}
	

	@Test (dependsOnMethods="updateConfigurationAttribute", description="Update feature tree with configuration")
	public void updateFeatureInBranchTree() throws JSONException, IOException, InterruptedException{
		String dateFormat = an.setDateFormat();

		
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
		defaultConfiguration.put("text", "small");
		json.put("defaultConfiguration", defaultConfiguration);
		JSONArray configurationRules = new JSONArray();
		configurationRules.put(jsonConfig);
		json.put("configurationRules", configurationRules);
		
		//update feature tree
		featureID1 = f.updateFeatureInBranch(seasonID, branchID, featureID1, json.toString(), sessionToken);
		Assert.assertFalse(featureID1.contains("error"), "feature was not added to the season" + featureID1);

		String anResponse = an.getGlobalDataCollection(seasonID, branchID, "BASIC", sessionToken);
		Assert.assertTrue(validateAttributeInAnalytics(anResponse).size()==3, "Incorrect number of attributes in analytics ");
		
		Assert.assertTrue(validateAttributeInAnalytics(anResponse).contains("color"), "Attribute color was not added to analytics "); // from config rule
		Assert.assertTrue(validateAttributeInAnalytics(anResponse).contains("text"), "Attribute text was not added to analytics"); // from default configuration
		Assert.assertTrue(validateAttributeInAnalytics(anResponse).contains("size"), "Attribute size was not added to analytics"); // updated attribute with old name configuration
		
		String display = an.getGlobalDataCollection(seasonID, branchID, "DISPLAY", sessionToken);
		Assert.assertTrue(an.validateDataCollectionByFeatureNames(display, anResponse).equals("true"));

		//check if files were changed
		an.setSleep();
		RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getDevelopmentBranchFileDateModification(m_url, productID, seasonID, branchID, dateFormat, sessionToken);
		Assert.assertTrue(responseDev.code ==200, "Runtime development feature file was not updated");		
		RuntimeRestApi.DateModificationResults responseProd = RuntimeDateUtilities.getProductionBranchFileDateModification(m_url, productID, seasonID, branchID, dateFormat, sessionToken);
		Assert.assertTrue(responseProd.code ==304, "Runtime production feature file was changed");
		RuntimeRestApi.DateModificationResults prodChanged = RuntimeDateUtilities.getProductionChangedDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(prodChanged.code ==304, "productionChanged.txt file was changed");

	}
	

	private JSONArray validateAttributeInAnalytics(String analytics) throws JSONException{
		JSONObject json = new JSONObject(analytics);
		JSONArray attrOnly = new JSONArray();
		JSONObject analyticsDataCollection = json.getJSONObject("analyticsDataCollection");
		JSONArray featuresAttributesToAnalytics = analyticsDataCollection.getJSONArray("featuresAttributesForAnalytics").getJSONObject(0).getJSONArray("attributes");
		
		for (int i=0; i< featuresAttributesToAnalytics.size(); i++){
			attrOnly.put(featuresAttributesToAnalytics.getJSONObject(i).getString("name"));
			
		}
		
		return attrOnly;

	}



	@AfterTest
	private void reset(){
		baseUtils.reset(productID, sessionToken);
	}
}
