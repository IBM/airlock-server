package tests.restapi.analytics;

import java.io.IOException;

import org.apache.commons.lang3.RandomStringUtils;
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


public class AttributeToAnalyticsInUseByTwoConfigurations {
	protected String seasonID;
	protected String branchID;
	protected String productID;
	protected String featureID1;
	protected String configID3;
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
	 * add feature and 3 configs with the same configuration field
	 * update config1 field
	 * delete one configuration
	 */
	


	@Test (description="Add feature and 3 configuration rules to the season and to analytics")
	public void addComponents() throws IOException, JSONException, InterruptedException{
		String dateFormat = an.setDateFormat();
		
		String feature = FileUtils.fileToString(filePath + "feature1.txt", "UTF-8", false);
		featureID1 = f.addFeatureToBranch(seasonID, branchID, feature, "ROOT", sessionToken);
		Assert.assertFalse(featureID1.contains("error"), "feature was not added to the season" + featureID1);
		
		//add first configuration
		String config = FileUtils.fileToString(filePath + "configuration_rule1.txt", "UTF-8", false);
		JSONObject jsonConfig1 = new JSONObject(config);
		JSONObject newConfiguration = new JSONObject();
		newConfiguration.put("color", "red");
		jsonConfig1.put("configuration", newConfiguration);
		jsonConfig1.put("name", RandomStringUtils.randomAlphabetic(5));
		configID1 = f.addFeatureToBranch(seasonID, branchID, jsonConfig1.toString(), featureID1, sessionToken);
		Assert.assertFalse(configID1.contains("error"), "Configuration1 was not added to the season" + configID1);
		
		//add second configuration

		JSONObject jsonConfig2 = new JSONObject(config);
		newConfiguration = new JSONObject();
		newConfiguration.put("color", "green");
		jsonConfig2.put("name", RandomStringUtils.randomAlphabetic(5));
		jsonConfig2.put("configuration", newConfiguration);
		configID2 = f.addFeatureToBranch(seasonID, branchID, jsonConfig2.toString(), featureID1, sessionToken);
		Assert.assertFalse(configID2.contains("error"), "Configuration1 was not added to the season" + configID2);
		
		//add third configuration
		
		JSONObject jsonConfig3 = new JSONObject(config);
		newConfiguration = new JSONObject();
		newConfiguration.put("color", "yellow");
		jsonConfig3.put("configuration", newConfiguration);
		jsonConfig3.put("name", RandomStringUtils.randomAlphabetic(5));
		configID3 = f.addFeatureToBranch(seasonID, branchID, jsonConfig3.toString(), featureID1, sessionToken);
		Assert.assertFalse(configID3.contains("error"), "Configuration1 was not added to the season" + configID3);

		//add feature and attribute to analytics
		String response = an.getGlobalDataCollection(seasonID, branchID, "BASIC", sessionToken);
		JSONArray attributes = new JSONArray();
		JSONObject attr1 = new JSONObject();
		attr1.put("name", "color");
		attr1.put("type", "REGULAR");
		attributes.add(attr1);
		String input = an.addFeaturesAttributesToAnalytics(response, featureID1, attributes);
		response = an.updateGlobalDataCollection(seasonID, branchID, input, sessionToken);
		Assert.assertFalse(response.contains("error"), "Could not add feature to analytics" + response);
		
		
		response = an.getGlobalDataCollection(seasonID, branchID, "BASIC", sessionToken);
		Assert.assertTrue(validateAttributeInAnalytics(response).size()==1, "Attribute was removed from analytics");
		
		String display = an.getGlobalDataCollection(seasonID, branchID, "DISPLAY", sessionToken);
		Assert.assertTrue(an.validateDataCollectionByFeatureNames(display, response).equals("true"));

		//check if files were changed
		an.setSleep();
		RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getDevelopmentBranchFileDateModification(m_url, productID, seasonID, branchID, dateFormat, sessionToken);
		Assert.assertTrue(responseDev.code ==200, "Runtime development feature file was not updated");
		JSONObject root = RuntimeDateUtilities.getFeaturesList(responseDev.message);
		Assert.assertTrue(getAttributesInRuntime(root, featureID1).size()==1, "Incorrect number of attributes in runtime development file ");
		
		RuntimeRestApi.DateModificationResults responseProd = RuntimeDateUtilities.getProductionBranchFileDateModification(m_url, productID, seasonID, branchID, dateFormat, sessionToken);
		Assert.assertTrue(responseProd.code ==304, "Runtime production feature file was changed");
		RuntimeRestApi.DateModificationResults prodChanged = RuntimeDateUtilities.getProductionChangedDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(prodChanged.code ==304, "productionChanged.txt file was changed");

	}
	
	@Test (dependsOnMethods="addComponents", description="Update configuration in configuration rule and validate that attribute is not removed from analytics")
	public void updateConfigurationAttribute() throws JSONException, IOException, InterruptedException{
		String dateFormat = an.setDateFormat();
		
		String configuration = f.getFeatureFromBranch(configID3, branchID, sessionToken);
		JSONObject jsonConfig = new JSONObject(configuration);
		JSONObject newConfiguration = new JSONObject();
		newConfiguration.put("mycolor", "test");
		jsonConfig.put("configuration", newConfiguration);
		String response = f.updateFeatureInBranch(seasonID, branchID, configID3, jsonConfig.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Configuration was not updated " + response);
		
		String anResponse = an.getGlobalDataCollection(seasonID, branchID, "BASIC", sessionToken);
		Assert.assertTrue(validateAttributeInAnalytics(anResponse).size()==1, "Updated attribute not found in analytics");
		
		String display = an.getGlobalDataCollection(seasonID, branchID, "DISPLAY", sessionToken);
		Assert.assertTrue(an.validateDataCollectionByFeatureNames(display, anResponse).equals("true"));

		//check if files were changed
		an.setSleep();
		RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getDevelopmentBranchFileDateModification(m_url, productID, seasonID, branchID, dateFormat, sessionToken);
		Assert.assertTrue(responseDev.code ==200, "Runtime development feature file was not updated");
		JSONObject root = RuntimeDateUtilities.getFeaturesList(responseDev.message);
		Assert.assertTrue(getAttributesInRuntime(root, featureID1).contains("color"), "Attribute was not updated in runtime development file ");
		
		RuntimeRestApi.DateModificationResults responseProd = RuntimeDateUtilities.getProductionBranchFileDateModification(m_url, productID, seasonID, branchID, dateFormat, sessionToken);
		Assert.assertTrue(responseProd.code ==304, "Runtime production feature file was changed");
		RuntimeRestApi.DateModificationResults prodChanged = RuntimeDateUtilities.getProductionChangedDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(prodChanged.code ==304, "productionChanged.txt file was changed");

	}
	
	
	@Test (dependsOnMethods="updateConfigurationAttribute", description="Delete configuration rule and validate that attribute is not removed from analytics")
	public void deleteConfiguration() throws JSONException, IOException, InterruptedException{
		String dateFormat = an.setDateFormat();
		
		int responseCode = f.deleteFeatureFromBranch(configID2, branchID, sessionToken);
		Assert.assertTrue(responseCode==200, "Configuration rule was not updated");
		
		String response = an.getGlobalDataCollection(seasonID, branchID, "BASIC", sessionToken);
		Assert.assertTrue(validateAttributeInAnalytics(response).size()==1, "Attribute was removed from analytics");
		
		String display = an.getGlobalDataCollection(seasonID, branchID, "DISPLAY", sessionToken);
		Assert.assertTrue(an.validateDataCollectionByFeatureNames(display, response).equals("true"));

		//check if files were changed
		an.setSleep();
		RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getDevelopmentBranchFileDateModification(m_url, productID, seasonID, branchID, dateFormat, sessionToken);
		Assert.assertTrue(responseDev.code ==200, "Runtime development feature file was not updated");
		JSONObject root = RuntimeDateUtilities.getFeaturesList(responseDev.message);
		Assert.assertTrue(getAttributesInRuntime(root, featureID1).size()==1, "Attribute was not updated in runtime development file ");
		
		RuntimeRestApi.DateModificationResults responseProd = RuntimeDateUtilities.getProductionBranchFileDateModification(m_url, productID, seasonID, branchID, dateFormat, sessionToken);
		Assert.assertTrue(responseProd.code ==304, "Runtime production feature file was changed");
		RuntimeRestApi.DateModificationResults prodChanged = RuntimeDateUtilities.getProductionChangedDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(prodChanged.code ==304, "productionChanged.txt file was changed");

	}

	
	private JSONArray getAttributesInRuntime(JSONObject root, String id ) throws JSONException{
		JSONArray features = root.getJSONArray("features");
		for (Object f : features){
			JSONObject feature = new JSONObject(f);
			//find feature
			if (feature.getString("uniqueId").equals(id)){
				if (feature.containsKey("configAttributesForAnalytics")){
					return feature.getJSONArray("configAttributesForAnalytics");
				}
					
			}
		}
		return new JSONArray(); //if no configAttributesToAnalytics
	}

	private JSONArray validateAttributeInAnalytics(String analytics) throws JSONException{
		JSONObject json = new JSONObject(analytics);
		JSONObject analyticsDataCollection = json.getJSONObject("analyticsDataCollection");
		JSONArray featuresAttributesToAnalytics = analyticsDataCollection.getJSONArray("featuresAttributesForAnalytics");
		return featuresAttributesToAnalytics.getJSONObject(0).getJSONArray("attributes");

	}
	

	@AfterTest
	private void reset(){
		baseUtils.reset(productID, sessionToken);
	}
}
