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


public class UpdateMixOfFeaturesWithAttributesToAnalytics {
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
	
	

	@Test (description="Create mix of features with configurations")
	public void createMixFeatureTree() throws JSONException, IOException, InterruptedException{
		
		//add mix
		String mix = FileUtils.fileToString(filePath + "feature-mutual.txt", "UTF-8", false);
		mixID = f.addFeatureToBranch(seasonID, branchID, mix, "ROOT", sessionToken);
		Assert.assertFalse(mixID.contains("error"), "Mix was not added to the season" + mixID);
		
		
		//add first feature
		String feature = FileUtils.fileToString(filePath + "feature1.txt", "UTF-8", false);
		featureID1 = f.addFeatureToBranch(seasonID, branchID, feature, mixID, sessionToken);
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

		//add feature1 and attribute to analytics
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

		
		//add second feature
		String feature2 = FileUtils.fileToString(filePath + "feature2.txt", "UTF-8", false);
		featureID2 = f.addFeatureToBranch(seasonID, branchID, feature2, mixID, sessionToken);
		Assert.assertFalse(featureID2.contains("error"), "Mix was not added to the season" + featureID2);
		
		//add second configuration

		String config2 = FileUtils.fileToString(filePath + "configuration_rule2.txt", "UTF-8", false);
		JSONObject jsonConfig2 = new JSONObject(config2);
		newConfiguration = new JSONObject();
		newConfiguration.put("title", "red");
		newConfiguration.put("size", "small");
		jsonConfig2.put("configuration", newConfiguration);
		configID2 = f.addFeatureToBranch(seasonID, branchID, jsonConfig2.toString(), featureID2, sessionToken);
		
		
		//add feature2 and attribute to analytics
		response = an.getGlobalDataCollection(seasonID, branchID, "BASIC", sessionToken);
		attributes = new JSONArray();
		JSONObject attr4 = new JSONObject();
		attr4.put("name", "size");
		attr4.put("type", "REGULAR");
		attributes.add(attr4);
		JSONObject attr5 = new JSONObject();
		attr5.put("name", "title");
		attr5.put("type", "REGULAR");
		attributes.add(attr5);
		input = an.addFeaturesAttributesToAnalytics(response, featureID2, attributes);
		response = an.updateGlobalDataCollection(seasonID, branchID, input, sessionToken);
		Assert.assertFalse(response.contains("error"), "Feature was not added to analytics" + response);

		String anResponse = an.getGlobalDataCollection(seasonID, branchID, "BASIC", sessionToken);
		String display = an.getGlobalDataCollection(seasonID, branchID, "DISPLAY", sessionToken);
		Assert.assertTrue(an.validateDataCollectionByFeatureNames(display, anResponse).equals("true"));

	}
	@Test (dependsOnMethods="createMixFeatureTree", description="Update several features with configurations")
	public void updateMixFeatureTree() throws JSONException, IOException, InterruptedException{
		String dateFormat = an.setDateFormat();
		//updates: featureID1 default configuration, config1 attribute, config2 attributes
		
		String mix = f.getFeatureFromBranch(mixID,branchID, sessionToken);
		JSONObject mixJson = new JSONObject(mix);
		JSONArray features = mixJson.getJSONArray("features");
		JSONObject feature1 = features.getJSONObject(0);
		JSONObject config1 = feature1.getJSONArray("configurationRules").getJSONObject(0);
		JSONObject feature2 = features.getJSONObject(1);
		JSONObject config2 = feature2.getJSONArray("configurationRules").getJSONObject(0);
		
		//from configuration1 remove attributes 
		config1.put("configuration", new JSONObject());

		//in feautre1 change defaultConfiguration
		
		JSONObject defaultConfiguration = new JSONObject();
		defaultConfiguration.put("color", "red");
		feature1.put("defaultConfiguration", defaultConfiguration);
		
		//in configuration2 rename attribute

		JSONObject newConfiguration2 = new JSONObject();
		newConfiguration2.put("mysize", "newsize");
		newConfiguration2.put("title", "stam");
		config2.put("configuration", newConfiguration2);

		
		//update mix tree
		String response = f.updateFeatureInBranch(seasonID, branchID, mixID, mixJson.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Mix feature was not updated" + response);
		
		String analytics = an.getGlobalDataCollection(seasonID, branchID, "BASIC", sessionToken);
		
		JSONObject json = new JSONObject(analytics);
		JSONObject analyticsDataCollection = json.getJSONObject("analyticsDataCollection");
		JSONArray featuresAttributesToAnalytics = analyticsDataCollection.getJSONArray("featuresAttributesForAnalytics");
			
		//removed attributes should remain in analytics
		Assert.assertTrue(featuresAttributesToAnalytics.getJSONObject(0).getJSONArray("attributes").size()==3, "Incorrect number of attributes in analytics for the first feature " + featuresAttributesToAnalytics.getJSONObject(0).getJSONArray("attributes"));
		Assert.assertTrue(featuresAttributesToAnalytics.getJSONObject(1).getJSONArray("attributes").size()==2, "Incorrect number of attributes in analytics for the second feature " + featuresAttributesToAnalytics.getJSONObject(1).getJSONArray("attributes"));
		
		String display = an.getGlobalDataCollection(seasonID, branchID, "DISPLAY", sessionToken);
		Assert.assertTrue(an.validateDataCollectionByFeatureNames(display, analytics).equals("true"));

		//check if files were changed
		an.setSleep();
		RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getDevelopmentBranchFileDateModification(m_url, productID, seasonID, branchID, dateFormat, sessionToken);
		Assert.assertTrue(responseDev.code ==200, "Runtime development feature file was not updated");		
		RuntimeRestApi.DateModificationResults responseProd = RuntimeDateUtilities.getProductionBranchFileDateModification(m_url, productID, seasonID, branchID, dateFormat, sessionToken);
		Assert.assertTrue(responseProd.code ==304, "Runtime production feature file was changed");
		RuntimeRestApi.DateModificationResults prodChanged = RuntimeDateUtilities.getProductionChangedDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(prodChanged.code ==304, "productionChanged.txt file was changed");

		
	}
	


	@AfterTest
	private void reset(){
		baseUtils.reset(productID, sessionToken);
	}
}
