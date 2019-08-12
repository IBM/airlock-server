package tests.restapi.proddevsplit;

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
import tests.restapi.AirlockUtils;
import tests.restapi.FeaturesRestApi;
import tests.restapi.ProductsRestApi;
import tests.restapi.RuntimeDateUtilities;
import tests.restapi.RuntimeRestApi;
import tests.restapi.SeasonsRestApi;

public class SubConfigurationsInNewSeason {
	protected String seasonID;
	protected String devFeatureID;
	protected String prodFeatureID;
	protected String devConfigID;
	protected String devSubConfigID;	
	protected String prodConfigID;
	protected String prodSubConfigID;	
	protected String filePath;
	protected FeaturesRestApi f;
	protected ProductsRestApi p;
	protected SeasonsRestApi s;
	protected String productID;
	protected String m_url;
	private String sessionToken = "";
	private AirlockUtils baseUtils;
	
	@BeforeClass
	@Parameters({"url", "analyticsUrl", "translationsUrl", "configPath", "sessionToken", "userName", "userPassword", "appName", "productsToDeleteFile"})
	public void init(String url, String analyticsUrl, String translationsUrl, String configPath, String sToken, String userName, String userPassword, String appName, String productsToDeleteFile) throws Exception{
		m_url = url;
		filePath = configPath ;
		p = new ProductsRestApi();
		p.setURL(m_url);
		s = new SeasonsRestApi();
		f = new FeaturesRestApi();
		s.setURL(m_url);
		f.setURL(m_url);
		baseUtils = new AirlockUtils(url, analyticsUrl, translationsUrl, configPath, sToken, userName, userPassword, appName, productsToDeleteFile);
		sessionToken = baseUtils.sessionToken;

		productID = baseUtils.createProduct();
		baseUtils.printProductToFile(productID);
		seasonID = baseUtils.createSeason(productID);
	}

	

	@Test (description="Create a feature and configuration in development")
	public void createFeatureInDev() throws JSONException, IOException, InterruptedException{
		String dateFormat = f.setDateFormat();
		
		String feature = FileUtils.fileToString(filePath + "feature1.txt", "UTF-8", false);
		devFeatureID = f.addFeature(seasonID, feature, "ROOT", sessionToken);
		Assert.assertFalse(devFeatureID.contains("error"), "Test should pass, but instead failed: " + devFeatureID );
		
		String config = FileUtils.fileToString(filePath + "configuration_rule1.txt", "UTF-8", false);
		devConfigID = f.addFeature(seasonID, config, devFeatureID, sessionToken);
		Assert.assertFalse(devConfigID.contains("error"), "Test should pass, but instead failed: " + devConfigID );
				
		//check if files were changed
		f.setSleep();
		RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getDevelopmentFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseDev.code ==200, "Runtime development feature file was not updated");
		RuntimeRestApi.DateModificationResults responseProd = RuntimeDateUtilities.getProductionFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseProd.code ==304, "Runtime production feature file was changed");
		RuntimeRestApi.DateModificationResults prodChanged = RuntimeDateUtilities.getProductionChangedDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(prodChanged.code ==304, "productionChanged.txt file was changed");

		//check development file content
		JSONObject root = RuntimeDateUtilities.getFeaturesList(responseDev.message);
		JSONArray features = root.getJSONArray("features");
		Assert.assertTrue(features.size()==1, "Incorrect number of features in the development file");
		
		Assert.assertTrue(features.getJSONObject(0).getJSONArray("configurationRules").size() == 1, "No feature configuration in the development file");
		
	}
	
	@Test (dependsOnMethods = "createFeatureInDev", description="Create sub-configuration in development")
	public void createSubConfigInDev() throws IOException, InterruptedException, JSONException{
		String dateFormat = f.setDateFormat();
		
		String config = FileUtils.fileToString(filePath + "configuration_rule1.txt", "UTF-8", false);
		JSONObject jsonConfig = new JSONObject(config);
		jsonConfig.put("name", "subconfig1");
		devSubConfigID = f.addFeature(seasonID, jsonConfig.toString(), devConfigID, sessionToken);
		Assert.assertFalse(devSubConfigID.contains("error"), "Test should pass, but instead failed: " + devSubConfigID );

		//check if files were changed
		f.setSleep();
		RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getDevelopmentFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseDev.code ==200, "Runtime development feature file was not updated");
		RuntimeRestApi.DateModificationResults responseProd = RuntimeDateUtilities.getProductionFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseProd.code ==304, "Runtime production feature file was changed");
		RuntimeRestApi.DateModificationResults prodChanged = RuntimeDateUtilities.getProductionChangedDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(prodChanged.code ==304, "productionChanged.txt file was changed");

	}
	
	@Test (dependsOnMethods = "createSubConfigInDev", description="Create a feature and configuration in production")
	public void createFeatureInProd() throws JSONException, IOException, InterruptedException{
		String dateFormat = f.setDateFormat();
		
		String feature = FileUtils.fileToString(filePath + "feature2.txt", "UTF-8", false);
		JSONObject json = new JSONObject(feature);
		json.put("stage", "PRODUCTION");
		prodFeatureID = f.addFeature(seasonID, json.toString(), "ROOT", sessionToken);
		Assert.assertFalse(prodFeatureID.contains("error"), "Test should pass, but instead failed: " + prodFeatureID );
		
		String config = FileUtils.fileToString(filePath + "configuration_rule2.txt", "UTF-8", false);
		JSONObject jsonConfig = new JSONObject(config);
		jsonConfig.put("stage", "PRODUCTION");
		prodConfigID = f.addFeature(seasonID, jsonConfig.toString(), prodFeatureID, sessionToken);
		Assert.assertFalse(prodConfigID.contains("error"), "Test should pass, but instead failed: " + prodConfigID );

		f.setSleep();
		RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getDevelopmentFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseDev.code==200, "Runtime development feature file was not updated");
		
		JSONObject root = RuntimeDateUtilities.getFeaturesList(responseDev.message);
		JSONArray features = root.getJSONArray("features");
		Assert.assertTrue(features.getJSONObject(1).getJSONArray("configurationRules").size() == 1, "No feature configuration in the development file");
		
		
		RuntimeRestApi.DateModificationResults responseProd = RuntimeDateUtilities.getProductionFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseProd.code==200, "Runtime production feature file was not updated");
		root = RuntimeDateUtilities.getFeaturesList(responseProd.message);
		features = root.getJSONArray("features");
		Assert.assertTrue(features.getJSONObject(0).getJSONArray("configurationRules").size() == 1, "No feature configuration in the production file");
		
		RuntimeRestApi.DateModificationResults prodChanged = RuntimeDateUtilities.getProductionChangedDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(prodChanged.code ==200, "productionChanged.txt file was not changed");


	}
	
	@Test (dependsOnMethods = "createFeatureInProd", description="Create sub-configuration in production")
	public void createSubConfigInProd() throws IOException, InterruptedException, JSONException{
		String dateFormat = f.setDateFormat();
		
		String config = FileUtils.fileToString(filePath + "configuration_rule2.txt", "UTF-8", false);
		JSONObject jsonConfig = new JSONObject(config);
		jsonConfig.put("stage", "PRODUCTION");
		jsonConfig.put("name", "subconfig2");
		prodSubConfigID = f.addFeature(seasonID, jsonConfig.toString(), prodConfigID, sessionToken);
		Assert.assertFalse(prodSubConfigID.contains("error"), "Test should pass, but instead failed: " + devConfigID );

		//check if files were changed
		f.setSleep();
		RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getDevelopmentFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseDev.code ==200, "Runtime development feature file was not updated");
		RuntimeRestApi.DateModificationResults responseProd = RuntimeDateUtilities.getProductionFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseProd.code ==200, "Runtime production feature file was not changed");
		RuntimeRestApi.DateModificationResults prodChanged = RuntimeDateUtilities.getProductionChangedDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(prodChanged.code ==200, "productionChanged.txt file was not changed");

	}
	
	@Test (dependsOnMethods = "createSubConfigInProd", description="Update development configuration name ")
	public void updateFeatureInDev() throws JSONException, IOException, InterruptedException{
		String dateFormat = f.setDateFormat();
		
		String feature = f.getFeature(devSubConfigID, sessionToken);
		JSONObject json = new JSONObject(feature);
		json.put("name", "devsubconfig");
		devSubConfigID = f.updateFeature(seasonID, devSubConfigID, json.toString(), sessionToken);

		f.setSleep();
		RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getDevelopmentFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseDev.code ==200, "Runtime development feature file was not updated");
		RuntimeRestApi.DateModificationResults responseProd = RuntimeDateUtilities.getProductionFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseProd.code ==304, "Runtime production feature file was changed");
		RuntimeRestApi.DateModificationResults prodChanged = RuntimeDateUtilities.getProductionChangedDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(prodChanged.code ==304, "productionChanged.txt file was changed");

		
		JSONObject root = RuntimeDateUtilities.getFeaturesList(responseDev.message);
		JSONArray features = root.getJSONArray("features");
		Assert.assertTrue(validateConfiguration(features, devFeatureID, devSubConfigID, "name", "devsubconfig"), "Feature was not updated in the development file");
		
	}
	
	@Test (dependsOnMethods = "updateFeatureInDev", description="Update configuration in production minAppVersion")
	public void updateFeatureInProd() throws JSONException, IOException, InterruptedException{
		String dateFormat = f.setDateFormat();
		
		String feature = f.getFeature(prodSubConfigID, sessionToken);
		JSONObject json = new JSONObject(feature);
		json.put("minAppVersion", "0.5");
		prodSubConfigID = f.updateFeature(seasonID, prodSubConfigID, json.toString(), sessionToken);
		Assert.assertFalse(prodSubConfigID.contains("error"), "Test should pass, but instead failed: " + prodConfigID );

		f.setSleep();
		RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getDevelopmentFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseDev.code==200, "Runtime development feature file was not updated");
		JSONObject root = RuntimeDateUtilities.getFeaturesList(responseDev.message);
		JSONArray features = root.getJSONArray("features");
		Assert.assertTrue(validateConfiguration(features, prodFeatureID, prodSubConfigID, "minAppVersion", "0.5"), "Feature was not updated in the development file");
		
		RuntimeRestApi.DateModificationResults responseProd = RuntimeDateUtilities.getProductionFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseProd.code ==200, "Runtime production feature file was not updated");
		root = RuntimeDateUtilities.getFeaturesList(responseProd.message);
		features = root.getJSONArray("features");
		Assert.assertTrue(validateConfiguration(features, prodFeatureID, prodSubConfigID, "minAppVersion", "0.5"), "Feature was not updated in the development file");
		
		RuntimeRestApi.DateModificationResults prodChanged = RuntimeDateUtilities.getProductionChangedDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(prodChanged.code ==200, "productionChanged.txt file was not changed");

	}

	
	@Test (dependsOnMethods = "updateFeatureInProd", description="Move sub-configuration from production to development")
	public void moveFeatureToDev() throws JSONException, IOException, InterruptedException{
		String dateFormat = f.setDateFormat();
		
		String config = f.getFeature(prodSubConfigID, sessionToken);
		JSONObject jsonConfig = new JSONObject(config);
		jsonConfig.put("stage", "DEVELOPMENT");
		prodSubConfigID = f.updateFeature(seasonID, prodSubConfigID, jsonConfig.toString(), sessionToken);
		
		f.setSleep();
		RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getDevelopmentFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseDev.code ==200, "Runtime development feature file was not updated");
		RuntimeRestApi.DateModificationResults responseProd = RuntimeDateUtilities.getProductionFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseProd.code ==200, "Runtime production feature file was not updated");
		RuntimeRestApi.DateModificationResults prodChanged = RuntimeDateUtilities.getProductionChangedDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(prodChanged.code ==200, "productionChanged.txt file was not changed");

		
		JSONObject root = RuntimeDateUtilities.getFeaturesList(responseDev.message);
		JSONArray features = root.getJSONArray("features");
		Assert.assertTrue(validateConfiguration(features, devFeatureID, devSubConfigID, "stage", "DEVELOPMENT"), "Feature was not updated in the development file");

		
		
		root = RuntimeDateUtilities.getFeaturesList(responseProd.message);
		features = root.getJSONArray("features");
		Assert.assertTrue(getNumberOfConfigurations(features, prodFeatureID)==0, "Feature was not updated in the development file");
	}
	
	
	
	@Test (dependsOnMethods = "updateFeatureInProd", description="Move feature and configuration from development to production")
	public void moveFeatureToProd() throws JSONException, IOException, InterruptedException{
		String dateFormat = f.setDateFormat();
		
		String config = f.getFeature(prodSubConfigID, sessionToken);
		JSONObject jsonConfig = new JSONObject(config);
		jsonConfig.put("stage", "PRODUCTION");
		prodSubConfigID = f.updateFeature(seasonID, prodSubConfigID, jsonConfig.toString(), sessionToken);
		Assert.assertFalse(prodSubConfigID.contains("error"), "Sub-configuration was not updated");

		f.setSleep();
		RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getDevelopmentFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseDev.code ==200, "Runtime development feature file was not updated");
		RuntimeRestApi.DateModificationResults responseProd = RuntimeDateUtilities.getProductionFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseProd.code ==200, "Runtime production feature file was not updated");
		RuntimeRestApi.DateModificationResults prodChanged = RuntimeDateUtilities.getProductionChangedDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(prodChanged.code ==200, "productionChanged.txt file was not changed");

		
		JSONObject root = RuntimeDateUtilities.getFeaturesList(responseDev.message);
		JSONArray features = root.getJSONArray("features");
		Assert.assertTrue(validateConfiguration(features, prodFeatureID, prodSubConfigID, "stage", "PRODUCTION"), "Feature was not updated in the development file");
		
		root = RuntimeDateUtilities.getFeaturesList(responseProd.message);
		features = root.getJSONArray("features");
		Assert.assertTrue(getNumberOfConfigurations(features, prodFeatureID)==1, "Feature was not updated in the development file");
	}
	
	@Test (dependsOnMethods = "moveFeatureToDev", description="Delete configuration in dev")
	public void deleteFeature1() throws JSONException, IOException, InterruptedException{
		String dateFormat = f.setDateFormat();
		
		int responseCode = f.deleteFeature(devSubConfigID, sessionToken);
		Assert.assertTrue(responseCode == 200, "Sub configuration was not deleted");

		f.setSleep();
		RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getDevelopmentFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseDev.code ==200, "Runtime development feature file was not updated");
		RuntimeRestApi.DateModificationResults responseProd = RuntimeDateUtilities.getProductionFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseProd.code ==304, "Runtime production feature file was not updated");
		RuntimeRestApi.DateModificationResults prodChanged = RuntimeDateUtilities.getProductionChangedDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(prodChanged.code ==304, "productionChanged.txt file was changed");

		
		JSONObject root = RuntimeDateUtilities.getFeaturesList(responseDev.message);
		JSONArray features = root.getJSONArray("features");
		Assert.assertTrue(getNumberOfConfigurations(features, devFeatureID)==0, "Sub configuration was not deleted from the development file");
		
		
	}

	
	private boolean validateConfiguration(JSONArray features, String featureUniqueId, String subConfigurationUniqueId, String field, String value) throws JSONException{
		boolean updatedDev = false;
		for(int i=0; i<features.size(); i++){
			if (features.getJSONObject(i).getString("uniqueId").equals(featureUniqueId)) {
				JSONObject item = features.getJSONObject(i);
				JSONArray configurations = item.getJSONArray("configurationRules");
				JSONObject configuration = configurations.getJSONObject(0);
				JSONArray subConfigurations = configuration.getJSONArray("configurationRules");
				for (int k =0; k<subConfigurations.size(); k++) {
					if (subConfigurations.getJSONObject(k).getString(field).equals(value) && subConfigurations.getJSONObject(k).getString("uniqueId").equals(subConfigurationUniqueId))
						updatedDev = true;
				}	
			}	
		}
		return updatedDev;
	}
	
	
	private int getNumberOfConfigurations(JSONArray features, String featureUniqueId) throws JSONException{
		JSONArray subConfigurations = new JSONArray();
		boolean updated = false;
		for(int i=0; i<features.size(); i++){
			if (features.getJSONObject(i).getString("uniqueId").equals(featureUniqueId)) {
				JSONObject item = features.getJSONObject(i);
				JSONArray configurations = item.getJSONArray("configurationRules");
				JSONObject configuration = configurations.getJSONObject(0);
				subConfigurations = configuration.getJSONArray("configurationRules");
				updated = true;
			}	
		}
		if (updated)
			return subConfigurations.size();
		else
			return -1;
	}
	
	@AfterTest
	private void reset(){
		baseUtils.reset(productID, sessionToken);
	}
}
