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


public class UpdateFeaturesWithConfigMixAttributesToAnalytics {
	protected String seasonID;
	protected String branchID;
	protected String productID;
	protected String featureID;
	protected String configID1;
	private String mixConfigID;
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
	
	@Test (description="Add feature with mix of configurations")
	public void addComponents() throws JSONException, IOException, InterruptedException{
		String feature = FileUtils.fileToString(filePath + "feature1.txt", "UTF-8", false);
		JSONObject featureJson = new JSONObject(feature);
		JSONObject defaultConfiguration = new JSONObject();
		defaultConfiguration.put("title", "text");
		featureJson.put("defaultConfiguration", defaultConfiguration);
		featureID = f.addFeatureToBranch(seasonID, branchID, featureJson.toString(), "ROOT", sessionToken);
		Assert.assertFalse(featureID.contains("error"), "Feature was not added to the season" + featureID);
		
		String mixConfig = FileUtils.fileToString(filePath + "configuration_feature-mutual.txt", "UTF-8", false);
		mixConfigID = f.addFeatureToBranch(seasonID, branchID, mixConfig, featureID, sessionToken);
		Assert.assertFalse(mixConfigID.contains("error"), "Feature was not added to the season" + mixConfigID);

		//add  configuration

		String config = FileUtils.fileToString(filePath + "configuration_rule1.txt", "UTF-8", false);
		JSONObject jsonConfig = new JSONObject(config);
		JSONObject newConfiguration = new JSONObject();
		newConfiguration.put("color", "red");
		newConfiguration.put("size", "small");
		jsonConfig.put("configuration", newConfiguration);
		configID1 = f.addFeatureToBranch(seasonID, branchID, jsonConfig.toString(), mixConfigID, sessionToken);
		
		//add feature and attribute to analytics
		String anResponse = an.getGlobalDataCollection(seasonID, branchID, "BASIC", sessionToken);
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
		attr3.put("name", "title");
		attr3.put("type", "REGULAR");
		attributes.add(attr3);
		String input = an.addFeaturesAttributesToAnalytics(anResponse, featureID, attributes);
		String response = an.updateGlobalDataCollection(seasonID, branchID, input, sessionToken);
		Assert.assertFalse(response.contains("error"), "Feature was not added to analytics" + response);
		
		anResponse = an.getGlobalDataCollection(seasonID, branchID, "BASIC", sessionToken);
		String display = an.getGlobalDataCollection(seasonID, branchID, "DISPLAY", sessionToken);
		Assert.assertTrue(an.validateDataCollectionByFeatureNames(display, anResponse).equals("true"));

	}
	
	@Test (dependsOnMethods="addComponents", description="Update feature with mix of configurations")
	public void updateConfigMixFeatureTree() throws JSONException, IOException, InterruptedException{
		//update feature & configuration
		String feature = f.getFeatureFromBranch(featureID, branchID, sessionToken);
		JSONObject featureJson = new JSONObject(feature);
		JSONObject mixConfigurations = featureJson.getJSONArray("configurationRules").getJSONObject(0);
		JSONObject config1 = mixConfigurations.getJSONArray("configurationRules").getJSONObject(0);
		
		JSONObject defaultConfiguration = new JSONObject();
		defaultConfiguration.put("mytitle", "text");
		featureJson.put("defaultConfiguration", defaultConfiguration);
		
		//from configuration1 remove attributes 
		JSONObject newConfiguration = new JSONObject();
		newConfiguration.put("title", "red");
		config1.put("configuration", newConfiguration);
		
		String responseUpdate = f.updateFeatureInBranch(seasonID, branchID, featureID, featureJson.toString(), sessionToken);
		Assert.assertFalse(responseUpdate.contains("error"), "Feature was not added to analytics" + responseUpdate);
		
		String anResponse = an.getGlobalDataCollection(seasonID, branchID, "BASIC", sessionToken);
		Assert.assertTrue(validateAttributeInAnalytics(anResponse).size()==3, "Incorrect number of attributes"); //removed attributes should remain in analytics

		String display = an.getGlobalDataCollection(seasonID, branchID, "DISPLAY", sessionToken);
		Assert.assertTrue(an.validateDataCollectionByFeatureNames(display, anResponse).equals("true"));

	}
	
	@Test (dependsOnMethods="updateConfigMixFeatureTree", description="remove all attributes from configuration rule")
	public void removeAttributesFromConfiguration() throws JSONException, IOException, InterruptedException{
		String dateFormat = an.setDateFormat();
		
		//remove attributes from configuration
		String configuration = f.getFeatureFromBranch(configID1, branchID, sessionToken);
		JSONObject jsonConfig = new JSONObject(configuration);
		jsonConfig.put("configuration", new JSONObject());

		String responseUpdate = f.updateFeatureInBranch(seasonID, branchID, configID1, jsonConfig.toString(), sessionToken);
		Assert.assertFalse(responseUpdate.contains("error"), "Configuration was not updated " + responseUpdate);
	
		String anResponse = an.getGlobalDataCollection(seasonID, branchID, "BASIC", sessionToken);
		JSONObject json = new JSONObject(anResponse);
		JSONObject analyticsDataCollection = json.getJSONObject("analyticsDataCollection");
		JSONArray featuresAttributesToAnalytics = analyticsDataCollection.getJSONArray("featuresAttributesForAnalytics");
		Assert.assertTrue(featuresAttributesToAnalytics.size()==1, "Attribute were not deleted from analytics");
		
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
		JSONObject analyticsDataCollection = json.getJSONObject("analyticsDataCollection");
		JSONArray featuresAttributesToAnalytics = analyticsDataCollection.getJSONArray("featuresAttributesForAnalytics");
		return featuresAttributesToAnalytics.getJSONObject(0).getJSONArray("attributes");

	}
	


	@AfterTest
	private void reset(){
		baseUtils.reset(productID, sessionToken);
	}
}
