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

public class AddFeatureTypesToFeatureOnOff {
	protected String seasonID;
	protected String branchID;
	protected String productID;
	protected String featureID1;
	protected String featureID2;
	protected String featureID3;
	protected String filePath;
	protected String m_url;
	protected String m_branchType;
	protected ProductsRestApi p;
	protected FeaturesRestApi f;
	protected AnalyticsRestApi an;
	protected BranchesRestApi br;
	protected InputSchemaRestApi schema;
	private String sessionToken = "";
	protected AirlockUtils baseUtils;
	
	@BeforeClass
	@Parameters({"url", "analyticsUrl", "translationsUrl", "configPath", "sessionToken", "userName", "userPassword", "appName", "productsToDeleteFile", "branchType"})
	public void init(String url, String analyticsUrl, String translationsUrl, String configPath, String sToken, String userName, String userPassword, String appName, String productsToDeleteFile, String branchType) throws Exception{
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
        br = new BranchesRestApi();
        br.setURL(url);
        m_branchType = branchType;
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
	 * add mix of features
	 * add configuration
	 * add mix of configurations
	 * add root
	 */
	

	@Test (description="Add mix to featuresAndConfigurationsForAnalytics")
	public void addMix() throws IOException, JSONException, InterruptedException{
		String dateFormat = an.setDateFormat();
		
		//add feature
		String featureMix = FileUtils.fileToString(filePath + "feature-mutual.txt", "UTF-8", false);
		String mixId = f.addFeatureToBranch(seasonID, branchID, featureMix, "ROOT", sessionToken);
		Assert.assertFalse(mixId.contains("error"), "Feature was not added to the season");
		
		//add featureID to analytics featureOnOff
		String response = an.getGlobalDataCollection(seasonID, branchID, "BASIC", sessionToken);
		Assert.assertFalse(response.contains("error"), "Incorrect globalDataCollection response");		
		String input = an.addFeatureOnOff(response, mixId);
		response = an.updateGlobalDataCollection(seasonID, branchID, input, sessionToken);
		response = an.getGlobalDataCollection(seasonID, branchID, "BASIC", sessionToken);
		Assert.assertTrue(numberOfFeature(response)==1, "The feature was not added to analytics");
		
		an.setSleep();
		RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getDevelopmentBranchFileDateModification(m_url, productID, seasonID, branchID, dateFormat, sessionToken);
		Assert.assertTrue(responseDev.code ==200, "Runtime development feature file was not updated");
		JSONObject root = RuntimeDateUtilities.getFeaturesList(responseDev.message);
		Assert.assertTrue(validateSentToAnalytics(root, mixId), "The field \"sendToAnalytics\" was not updated for feature");
		
		RuntimeRestApi.DateModificationResults responseProd = RuntimeDateUtilities.getProductionBranchFileDateModification(m_url, productID, seasonID, branchID, dateFormat, sessionToken);
		Assert.assertTrue(responseProd.code ==304, "Runtime production feature file was changed");
		
		RuntimeRestApi.DateModificationResults prodChanged = RuntimeDateUtilities.getProductionChangedDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(prodChanged.code ==304, "productionChanged.txt file was changed");

		}
	

	@Test (dependsOnMethods="addMix", description="Add configuration to featuresAndConfigurationsForAnalytics")
	public void addConfiguration() throws IOException, JSONException, InterruptedException{
		String dateFormat = an.setDateFormat();
		
		//add feature and configuration
		String feature = FileUtils.fileToString(filePath + "feature1.txt", "UTF-8", false); 
		String featureId = f.addFeatureToBranch(seasonID, branchID, feature, "ROOT", sessionToken);
		String configuration = FileUtils.fileToString(filePath + "configuration_rule1.txt", "UTF-8", false);
		String configId = f.addFeatureToBranch(seasonID, branchID, configuration, featureId, sessionToken);
		Assert.assertFalse(configId.contains("error"), "Feature was not added to the season");
		
		//add featureID to analytics featureOnOff
		String response = an.getGlobalDataCollection(seasonID, branchID, "BASIC", sessionToken);
		Assert.assertFalse(response.contains("error"), "Incorrect globalDataCollection response");		
		String input = an.addFeatureOnOff(response, configId);
		response = an.updateGlobalDataCollection(seasonID, branchID, input, sessionToken);
		response = an.getGlobalDataCollection(seasonID, branchID, "BASIC", sessionToken);
		Assert.assertTrue(numberOfFeature(response)==2, "The feature was not added to analytics");
		
		an.setSleep();
		RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getDevelopmentBranchFileDateModification(m_url, productID, seasonID, branchID, dateFormat, sessionToken);
		Assert.assertTrue(responseDev.code ==200, "Runtime development feature file was not updated");
		JSONObject root = RuntimeDateUtilities.getFeaturesList(responseDev.message);
		Assert.assertTrue(validateSentToAnalyticsConfiguration(root, featureId, configId), "The field \"sendToAnalytics\" was not updated for feature");
		
		RuntimeRestApi.DateModificationResults responseProd = RuntimeDateUtilities.getProductionBranchFileDateModification(m_url, productID, seasonID, branchID, dateFormat, sessionToken);
		Assert.assertTrue(responseProd.code ==304, "Runtime production feature file was changed");
		
		RuntimeRestApi.DateModificationResults prodChanged = RuntimeDateUtilities.getProductionChangedDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(prodChanged.code ==304, "productionChanged.txt file was changed");

		}
	
	@Test (dependsOnMethods="addConfiguration", description="Add configuration mix to featuresAndConfigurationsForAnalytics")
	public void addConfigurationMix() throws IOException, JSONException, InterruptedException{
		String dateFormat = an.setDateFormat();
		
		//add feature and configuration
		String feature = FileUtils.fileToString(filePath + "feature2.txt", "UTF-8", false); 
		String featureId = f.addFeatureToBranch(seasonID, branchID, feature, "ROOT", sessionToken);
		String configuration = FileUtils.fileToString(filePath + "configuration_feature-mutual.txt", "UTF-8", false);
		String configId = f.addFeatureToBranch(seasonID, branchID, configuration, featureId, sessionToken);
		Assert.assertFalse(configId.contains("error"), "Feature was not added to the season");
		
		//add featureID to analytics featureOnOff
		String response = an.getGlobalDataCollection(seasonID, branchID, "BASIC", sessionToken);
		Assert.assertFalse(response.contains("error"), "Incorrect globalDataCollection response");		
		String input = an.addFeatureOnOff(response, configId);
		response = an.updateGlobalDataCollection(seasonID, branchID, input, sessionToken);
		response = an.getGlobalDataCollection(seasonID, branchID, "BASIC", sessionToken);
		Assert.assertTrue(numberOfFeature(response)==3, "The feature was not added to analytics");
		
		an.setSleep();
		RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getDevelopmentBranchFileDateModification(m_url, productID, seasonID, branchID, dateFormat, sessionToken);
		Assert.assertTrue(responseDev.code ==200, "Runtime development feature file was not updated");
		JSONObject root = RuntimeDateUtilities.getFeaturesList(responseDev.message);
		Assert.assertTrue(validateSentToAnalyticsConfiguration(root, featureId, configId), "The field \"sendToAnalytics\" was not updated for feature");
		
		RuntimeRestApi.DateModificationResults responseProd = RuntimeDateUtilities.getProductionBranchFileDateModification(m_url, productID, seasonID, branchID, dateFormat, sessionToken);
		Assert.assertTrue(responseProd.code ==304, "Runtime production feature file was changed");
		
		RuntimeRestApi.DateModificationResults prodChanged = RuntimeDateUtilities.getProductionChangedDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(prodChanged.code ==304, "productionChanged.txt file was changed");

		}
	
	
	
	@Test (dependsOnMethods="addConfigurationMix", description="Add root to featuresAndConfigurationsForAnalytics")
	public void addRootToAnalytics() throws IOException, JSONException, InterruptedException{
		String dateFormat = an.setDateFormat();
		
		//find root id
		String rootId = f.getBranchRootId(seasonID, branchID, sessionToken);

		if(!m_branchType.equals("Master")){
			String response = br.checkoutFeature(branchID,rootId,sessionToken);
		}

		//add featureID to analytics featureOnOff
		String response = an.getGlobalDataCollection(seasonID, branchID, "BASIC", sessionToken);
		Assert.assertFalse(response.contains("error"), "Incorrect globalDataCollection response");		
		String input = an.addFeatureOnOff(response, rootId);
		response = an.updateGlobalDataCollection(seasonID, branchID, input, sessionToken);
		response = an.getGlobalDataCollection(seasonID, branchID, "BASIC", sessionToken);
		
		an.setSleep();
		RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getDevelopmentBranchFileDateModification(m_url, productID, seasonID, branchID, dateFormat, sessionToken);
		Assert.assertTrue(responseDev.code ==200, "Runtime development feature file was not updated");

		if(m_branchType.equals("Master")) {
			JSONObject root = RuntimeDateUtilities.getFeaturesList(responseDev.message);
			Assert.assertTrue(root.containsKey("sendToAnalytics"), "The field \"sendToAnalytics\" was not updated for root");
		}else {
			Assert.assertTrue(new JSONObject(responseDev.message).getJSONArray("features").getJSONObject(0).getBoolean("sendToAnalytics"), "The field \"sendToAnalytics\" was not updated for root");
		}
		RuntimeRestApi.DateModificationResults responseProd = RuntimeDateUtilities.getProductionBranchFileDateModification(m_url, productID, seasonID, branchID, dateFormat, sessionToken);
		Assert.assertTrue(responseProd.code ==304, "Runtime production feature file was changed");
		
		RuntimeRestApi.DateModificationResults prodChanged = RuntimeDateUtilities.getProductionChangedDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(prodChanged.code ==304, "productionChanged.txt file was changed");

		}
	
	
	private int numberOfFeature(String input){
		
		try{
			JSONObject json = new JSONObject(input);
			JSONObject analytics = json.getJSONObject("analyticsDataCollection");
			JSONArray inputFields = analytics.getJSONArray("featuresAndConfigurationsForAnalytics");
				return inputFields.size();
			
		} catch (Exception e){
				return -1;
		}
	}
	
	private boolean validateSentToAnalytics(JSONObject root, String featureId) throws JSONException{
		JSONArray features = root.getJSONArray("features");
		for (Object f : features) {
			JSONObject feature = new JSONObject(f);
			if (feature.getString("uniqueId").equals(featureId)) {
				if (feature.has("sendToAnalytics"))
					return feature.getBoolean("sendToAnalytics");
			}	
		}
		return false;
	}
	
	
	private boolean validateSentToAnalyticsConfiguration(JSONObject root, String parentId, String featureId) throws JSONException{
		JSONArray features = root.getJSONArray("features");
		//get the first feature
		for (Object f : features) {
			JSONObject feature = new JSONObject(f);
			if (feature.getString("uniqueId").equals(parentId)){
				JSONArray configs = feature.getJSONArray("configurationRules");
				for (Object c : configs) {
					JSONObject configuration = new JSONObject(c);
					if (configuration.getString("uniqueId").equals(featureId)) {
						if (configuration.has("sendToAnalytics"))
							return configuration.getBoolean("sendToAnalytics");
					}
				}
			}	
		}
		
		return false;
	}
	
	@AfterTest
	private void reset(){
		baseUtils.reset(productID, sessionToken);
	}
}
