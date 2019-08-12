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

public class ConfigurationsInNewSeason {
	protected String seasonID;
	protected String devFeatureID;
	protected String prodFeatureID;
	protected String devConfigID;
	protected String prodConfigID;
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
	
	//TODO
	/*
	 * for all tests here get the files content
	 * 
	 */
	

	@Test (description="Create a feature and configuration in development")
	public void createFeatureInDev() throws JSONException, IOException, InterruptedException{
		String dateFormat = f.setDateFormat();
		String productionChangedOriginal = RuntimeDateUtilities.getProductionChangedFile(m_url, productID, seasonID, sessionToken);
		
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
		String productionChangedNew = RuntimeDateUtilities.getProductionChangedFile(m_url, productID, seasonID, sessionToken);
		Assert.assertTrue(RuntimeDateUtilities.ifProductionChangedContent(productionChangedOriginal, productionChangedNew), "productionChanged.txt content was changed");

		//check development file content
		JSONObject root = RuntimeDateUtilities.getFeaturesList(responseDev.message);
		JSONArray features = root.getJSONArray("features");
		Assert.assertTrue(features.size()==1, "Incorrect number of features in the development file");
		
		Assert.assertTrue(features.getJSONObject(0).getJSONArray("configurationRules").size() == 1, "No feature configuration in the development file");
		
	}
	
	@Test (dependsOnMethods = "createFeatureInDev", description="Create a feature and configuration in production")
	public void createFeatureInProd() throws JSONException, IOException, InterruptedException{
		String dateFormat = f.setDateFormat();
		String productionChangedOriginal = RuntimeDateUtilities.getProductionChangedFile(m_url, productID, seasonID, sessionToken);
		
		String feature = FileUtils.fileToString(filePath + "feature2.txt", "UTF-8", false);
		JSONObject json = new JSONObject(feature);
		json.put("stage", "PRODUCTION");
		prodFeatureID = f.addFeature(seasonID, json.toString(), "ROOT", sessionToken);
		Assert.assertFalse(prodFeatureID.contains("error"), "Test should pass, but instead failed: " + prodFeatureID );
		
		String config = FileUtils.fileToString(filePath + "configuration_rule2.txt", "UTF-8", false);
		JSONObject jsonConfig = new JSONObject(config);
		jsonConfig.put("stage", "PRODUCTION");
		prodConfigID = f.addFeature(seasonID, jsonConfig.toString(), prodFeatureID, sessionToken);
		Assert.assertFalse(devConfigID.contains("error"), "Test should pass, but instead failed: " + devConfigID );

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
		String productionChangedNew = RuntimeDateUtilities.getProductionChangedFile(m_url, productID, seasonID, sessionToken);
		Assert.assertFalse(RuntimeDateUtilities.ifProductionChangedContent(productionChangedOriginal, productionChangedNew), "productionChanged.txt content was not changed");


	}
	
	@Test (dependsOnMethods = "createFeatureInProd", description="Update development configuration name ")
	public void updateFeatureInDev() throws JSONException, IOException, InterruptedException{
		String dateFormat = f.setDateFormat();
		String productionChangedOriginal = RuntimeDateUtilities.getProductionChangedFile(m_url, productID, seasonID, sessionToken);
		
		String feature = f.getFeature(devConfigID, sessionToken);
		JSONObject json = new JSONObject(feature);
		json.put("name", "devconfig");
		devConfigID = f.updateFeature(seasonID, devConfigID, json.toString(), sessionToken);

		f.setSleep();
		RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getDevelopmentFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseDev.code ==200, "Runtime development feature file was not updated");
		RuntimeRestApi.DateModificationResults responseProd = RuntimeDateUtilities.getProductionFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseProd.code ==304, "Runtime production feature file was changed");
		RuntimeRestApi.DateModificationResults prodChanged = RuntimeDateUtilities.getProductionChangedDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(prodChanged.code ==304, "productionChanged.txt file was changed");
		String productionChangedNew = RuntimeDateUtilities.getProductionChangedFile(m_url, productID, seasonID, sessionToken);
		Assert.assertTrue(RuntimeDateUtilities.ifProductionChangedContent(productionChangedOriginal, productionChangedNew), "productionChanged.txt content was changed");

		
		JSONObject root = RuntimeDateUtilities.getFeaturesList(responseDev.message);
		JSONArray features = root.getJSONArray("features");
		
		
		Assert.assertTrue(validateConfiguration(features, devFeatureID, devConfigID, "name", "devconfig"), "Feature was not updated in the development file");
		
	}
	
	@Test (dependsOnMethods = "updateFeatureInDev", description="Update configuration in production minAppVersion")
	public void updateFeatureInProd() throws JSONException, IOException, InterruptedException{
		String dateFormat = f.setDateFormat();
		String productionChangedOriginal = RuntimeDateUtilities.getProductionChangedFile(m_url, productID, seasonID, sessionToken);
		
		String feature = f.getFeature(prodConfigID, sessionToken);
		JSONObject json = new JSONObject(feature);
		json.put("minAppVersion", "0.5");
		prodConfigID = f.updateFeature(seasonID, prodConfigID, json.toString(), sessionToken);
		Assert.assertFalse(prodFeatureID.contains("error"), "Test should pass, but instead failed: " + prodConfigID );

		f.setSleep();
		RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getDevelopmentFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseDev.code==200, "Runtime development feature file was not updated");
		JSONObject root = RuntimeDateUtilities.getFeaturesList(responseDev.message);
		JSONArray features = root.getJSONArray("features");
		Assert.assertTrue(validateConfiguration(features, prodFeatureID, prodConfigID, "minAppVersion", "0.5"), "Feature was not updated in the development file");
		
		RuntimeRestApi.DateModificationResults responseProd = RuntimeDateUtilities.getProductionFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseProd.code ==200, "Runtime production feature file was not updated");
		root = RuntimeDateUtilities.getFeaturesList(responseProd.message);
		features = root.getJSONArray("features");
		Assert.assertTrue(validateConfiguration(features, prodFeatureID, prodConfigID, "minAppVersion", "0.5"), "Feature was not updated in the development file");
		
		RuntimeRestApi.DateModificationResults prodChanged = RuntimeDateUtilities.getProductionChangedDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(prodChanged.code ==200, "productionChanged.txt file was not changed");
		String productionChangedNew = RuntimeDateUtilities.getProductionChangedFile(m_url, productID, seasonID, sessionToken);
		Assert.assertFalse(RuntimeDateUtilities.ifProductionChangedContent(productionChangedOriginal, productionChangedNew), "productionChanged.txt content was not changed");

	}
	
	
	@Test (dependsOnMethods = "updateFeatureInProd", description="Move feature and configuration from development to production")
	public void moveFeatureToProd() throws JSONException, IOException, InterruptedException{
		String dateFormat = f.setDateFormat();
		String productionChangedOriginal = RuntimeDateUtilities.getProductionChangedFile(m_url, productID, seasonID, sessionToken);
		
		String feature = f.getFeature(devFeatureID, sessionToken);
		JSONObject json = new JSONObject(feature);
		json.put("stage", "PRODUCTION");
		devFeatureID = f.updateFeature(seasonID, devFeatureID, json.toString(), sessionToken);
		
		String config = f.getFeature(devConfigID, sessionToken);
		JSONObject jsonConfig = new JSONObject(config);
		jsonConfig.put("stage", "PRODUCTION");
		devConfigID = f.updateFeature(seasonID, devConfigID, jsonConfig.toString(), sessionToken);

		f.setSleep();
		RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getDevelopmentFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseDev.code ==200, "Runtime development feature file was not updated");
		RuntimeRestApi.DateModificationResults responseProd = RuntimeDateUtilities.getProductionFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseProd.code ==200, "Runtime production feature file was not updated");
		RuntimeRestApi.DateModificationResults prodChanged = RuntimeDateUtilities.getProductionChangedDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(prodChanged.code ==200, "productionChanged.txt file was not changed");
		String productionChangedNew = RuntimeDateUtilities.getProductionChangedFile(m_url, productID, seasonID, sessionToken);
		Assert.assertFalse(RuntimeDateUtilities.ifProductionChangedContent(productionChangedOriginal, productionChangedNew), "productionChanged.txt content was not changed");

		
		JSONObject root = RuntimeDateUtilities.getFeaturesList(responseDev.message);
		JSONArray features = root.getJSONArray("features");
		Assert.assertTrue(validateConfiguration(features, devFeatureID, devConfigID, "stage", "PRODUCTION"), "Feature was not updated in the development file");
		
		root = RuntimeDateUtilities.getFeaturesList(responseProd.message);
		features = root.getJSONArray("features");
		Assert.assertTrue(validateConfiguration(features, devFeatureID, devConfigID, "stage", "PRODUCTION"), "Feature was not updated in the production file");
	}
	
	@Test (dependsOnMethods = "moveFeatureToProd", description="Move feature1 from production to development")
	public void moveFeatureToDev() throws JSONException, IOException, InterruptedException{
		String dateFormat = f.setDateFormat();
		String productionChangedOriginal = RuntimeDateUtilities.getProductionChangedFile(m_url, productID, seasonID, sessionToken);
		
		String config = f.getFeature(devConfigID, sessionToken);
		JSONObject jsonConfig = new JSONObject(config);
		jsonConfig.put("stage", "DEVELOPMENT");
		devConfigID = f.updateFeature(seasonID, devConfigID, jsonConfig.toString(), sessionToken);
		
		String feature = f.getFeature(devFeatureID, sessionToken);
		JSONObject json = new JSONObject(feature);
		json.put("stage", "DEVELOPMENT");
		devFeatureID = f.updateFeature(seasonID, devFeatureID, json.toString(), sessionToken);

		f.setSleep();
		RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getDevelopmentFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseDev.code ==200, "Runtime development feature file was not updated");
		RuntimeRestApi.DateModificationResults responseProd = RuntimeDateUtilities.getProductionFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseProd.code ==200, "Runtime production feature file was not updated");
		RuntimeRestApi.DateModificationResults prodChanged = RuntimeDateUtilities.getProductionChangedDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(prodChanged.code ==200, "productionChanged.txt file was not changed");
		String productionChangedNew = RuntimeDateUtilities.getProductionChangedFile(m_url, productID, seasonID, sessionToken);
		Assert.assertFalse(RuntimeDateUtilities.ifProductionChangedContent(productionChangedOriginal, productionChangedNew), "productionChanged.txt content was not changed");

		
		JSONObject root = RuntimeDateUtilities.getFeaturesList(responseDev.message);
		JSONArray features = root.getJSONArray("features");
		Assert.assertTrue(validateConfiguration(features, devFeatureID, devConfigID, "stage", "DEVELOPMENT"), "Feature was not updated in the development file");
		
		root = RuntimeDateUtilities.getFeaturesList(responseProd.message);
		features = root.getJSONArray("features");
		Assert.assertTrue(features.size()==1, "Incorrect number of features in the production file");
	}
	
	@Test (dependsOnMethods = "moveFeatureToDev", description="Delete configuration in dev")
	public void deleteFeature1() throws JSONException, IOException, InterruptedException{
		String dateFormat = f.setDateFormat();
		String productionChangedOriginal = RuntimeDateUtilities.getProductionChangedFile(m_url, productID, seasonID, sessionToken);
		
		int responseCode = f.deleteFeature(devConfigID, sessionToken);
		Assert.assertTrue(responseCode == 200, "Feature was not deleted");

		f.setSleep();
		RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getDevelopmentFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseDev.code ==200, "Runtime development feature file was not updated");
		RuntimeRestApi.DateModificationResults responseProd = RuntimeDateUtilities.getProductionFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseProd.code ==304, "Runtime production feature file was not updated");
		RuntimeRestApi.DateModificationResults prodChanged = RuntimeDateUtilities.getProductionChangedDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(prodChanged.code ==304, "productionChanged.txt file was changed");
		String productionChangedNew = RuntimeDateUtilities.getProductionChangedFile(m_url, productID, seasonID, sessionToken);
		Assert.assertTrue(RuntimeDateUtilities.ifProductionChangedContent(productionChangedOriginal, productionChangedNew), "productionChanged.txt content was changed");

		
		JSONObject root = RuntimeDateUtilities.getFeaturesList(responseDev.message);
		JSONArray features = root.getJSONArray("features");
		
		boolean updatedConfiguration = false;
		for(int i=0; i<features.size(); i++){
			if (features.getJSONObject(i).getString("uniqueId").equals(devFeatureID)) {
				JSONObject item = features.getJSONObject(i);
				JSONArray configurations = item.getJSONArray("configurationRules");
				if (configurations.size()==0)
					updatedConfiguration=true;
			}	
		}
		
		Assert.assertTrue(updatedConfiguration, "Configuration was not removed in the development file");
	}
	
	@Test (dependsOnMethods = "deleteFeature1", description="Move configuration2 from production to development, it's parent feature remains in production")
	public void moveConfigToDev() throws JSONException, IOException, InterruptedException{
		String dateFormat = f.setDateFormat();
		String productionChangedOriginal = RuntimeDateUtilities.getProductionChangedFile(m_url, productID, seasonID, sessionToken);
		
		String config = f.getFeature(prodConfigID, sessionToken);
		JSONObject jsonConfig = new JSONObject(config);
		jsonConfig.put("stage", "DEVELOPMENT");
		prodConfigID = f.updateFeature(seasonID, prodConfigID, jsonConfig.toString(), sessionToken);

		f.setSleep();
		RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getDevelopmentFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseDev.code ==200, "Runtime development feature file was not updated");
		
		RuntimeRestApi.DateModificationResults responseProd = RuntimeDateUtilities.getProductionFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseProd.code ==200, "Runtime production feature file was not updated");
		
		RuntimeRestApi.DateModificationResults prodChanged = RuntimeDateUtilities.getProductionChangedDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(prodChanged.code ==200, "productionChanged.txt file was not changed");
		String productionChangedNew = RuntimeDateUtilities.getProductionChangedFile(m_url, productID, seasonID, sessionToken);
		Assert.assertFalse(RuntimeDateUtilities.ifProductionChangedContent(productionChangedOriginal, productionChangedNew), "productionChanged.txt content was not changed");

		
		//in dev file: feature in prod, config in dev
		JSONObject root = RuntimeDateUtilities.getFeaturesList(responseDev.message);
		JSONArray features = root.getJSONArray("features");
		Assert.assertTrue(validateConfiguration(features, prodFeatureID, prodConfigID, "stage", "DEVELOPMENT"), "Feature was not updated in the development file");
		
		//in prod file: feature in prod without its config
		root = RuntimeDateUtilities.getFeaturesList(responseProd.message);
		features = root.getJSONArray("features");
		boolean emptyConfiguration = false;
		for(int i=0; i<features.size(); i++){
			if (features.getJSONObject(i).getString("uniqueId").equals(prodFeatureID)) {
				JSONObject item = features.getJSONObject(i);
				JSONArray configurations = item.getJSONArray("configurationRules");
					if (configurations.size()==0)
						emptyConfiguration = true;
			}	
		}
		
		Assert.assertTrue(emptyConfiguration, "Configuration in development stage was not removed from production file");
		
	}
	
	@Test (dependsOnMethods = "moveConfigToDev", description="Delete configuration2, it's parent feature remains in production")
	public void deleteConfigWithoutParent() throws JSONException, IOException, InterruptedException{
		String dateFormat = f.setDateFormat();
		String productionChangedOriginal = RuntimeDateUtilities.getProductionChangedFile(m_url, productID, seasonID, sessionToken);

		int responseCode = f.deleteFeature(prodConfigID, sessionToken);
		Assert.assertTrue(responseCode ==200, "Configuration was not deleted");

		f.setSleep();
		RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getDevelopmentFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseDev.code ==200, "Runtime development feature file was not updated");
		RuntimeRestApi.DateModificationResults responseProd = RuntimeDateUtilities.getProductionFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseProd.code ==304, "Runtime production feature file was not updated");
		RuntimeRestApi.DateModificationResults prodChanged = RuntimeDateUtilities.getProductionChangedDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(prodChanged.code ==304, "productionChanged.txt file was changed");
		String productionChangedNew = RuntimeDateUtilities.getProductionChangedFile(m_url, productID, seasonID, sessionToken);
		Assert.assertTrue(RuntimeDateUtilities.ifProductionChangedContent(productionChangedOriginal, productionChangedNew), "productionChanged.txt content was changed");

		
		JSONObject root = RuntimeDateUtilities.getFeaturesList(responseDev.message);
		JSONArray features = root.getJSONArray("features");
		boolean emptyConfiguration = false;
		for(int i=0; i<features.size(); i++){
			if (features.getJSONObject(i).getString("uniqueId").equals(prodFeatureID)) {
				JSONObject item = features.getJSONObject(i);
				JSONArray configurations = item.getJSONArray("configurationRules");
					if (configurations.size()==0)
						emptyConfiguration = true;
			}	
		}
		Assert.assertTrue(emptyConfiguration, "Configuration was not removed from development file");
	}
	
	@Test (dependsOnMethods = "deleteConfigWithoutParent", description="Move feature from production to development")
	public void moveFeatureToDev2() throws JSONException, IOException, InterruptedException{
		String dateFormat = f.setDateFormat();
		String productionChangedOriginal = RuntimeDateUtilities.getProductionChangedFile(m_url, productID, seasonID, sessionToken);

		String feature = f.getFeature(prodFeatureID, sessionToken);
		JSONObject json = new JSONObject(feature);
		json.put("stage", "DEVELOPMENT");
		prodFeatureID = f.updateFeature(seasonID, prodFeatureID, json.toString(), sessionToken);

		Thread.sleep(3000);
		RuntimeRestApi.DateModificationResults response = RuntimeDateUtilities.getDevelopmentFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(response.code ==200, "Runtime development feature file was not updated");
		response = RuntimeDateUtilities.getProductionFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(response.code ==200, "Runtime production feature file was not updated");
		RuntimeRestApi.DateModificationResults prodChanged = RuntimeDateUtilities.getProductionChangedDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(prodChanged.code ==200, "productionChanged.txt file was not changed");
		String productionChangedNew = RuntimeDateUtilities.getProductionChangedFile(m_url, productID, seasonID, sessionToken);
		Assert.assertFalse(RuntimeDateUtilities.ifProductionChangedContent(productionChangedOriginal, productionChangedNew), "productionChanged.txt content was not changed");

		
	}

	
	private boolean validateConfiguration(JSONArray features, String featureUniqueId, String configurationUniqueId, String field, String value) throws JSONException{
		boolean updatedDev = false;
		for(int i=0; i<features.size(); i++){
			if (features.getJSONObject(i).getString("uniqueId").equals(featureUniqueId)) {
				JSONObject item = features.getJSONObject(i);
				JSONArray configurations = item.getJSONArray("configurationRules");
				for (int k =0; k<configurations.size(); k++) {
					if (configurations.getJSONObject(k).getString(field).equals(value) && configurations.getJSONObject(k).getString("uniqueId").equals(configurationUniqueId))
						updatedDev = true;
				}	
			}	
		}
		return updatedDev;
	}
	
	@AfterTest
	private void reset(){
		baseUtils.reset(productID, sessionToken);
	}
}
