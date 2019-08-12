package tests.restapi.proddevsplit;

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
import tests.restapi.AirlockUtils;
import tests.restapi.FeaturesRestApi;
import tests.restapi.ProductsRestApi;
import tests.restapi.RuntimeDateUtilities;
import tests.restapi.RuntimeRestApi;
import tests.restapi.SeasonsRestApi;

public class CreateNewSeasonFromExistingSeason {
	protected String seasonID;
	protected String seasonID2;
	protected String mtxID1;
	protected String mtxID2;
	protected String featureID1;
	protected String featureID2;
	protected String featureID3;
	protected String featureID4;
	protected String featureID5;
	protected String configID1;
	protected String filePath;
	protected FeaturesRestApi f;
	protected ProductsRestApi p;
	protected SeasonsRestApi s;
	private AirlockUtils baseUtils;
	protected String productID;
	protected String m_url;
	private String sessionToken = "";
	
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
	

	@Test (description="Create an mtx group in production under ROOT")
	public void createFirstMTX() throws JSONException, IOException, InterruptedException{
		String dateFormat = f.setDateFormat();
		String productionChangedOriginal = RuntimeDateUtilities.getProductionChangedFile(m_url, productID, seasonID, sessionToken);
		
		String mtx = FileUtils.fileToString(filePath + "feature-mutual.txt", "UTF-8", false);
		 mtxID1 = f.addFeature(seasonID, mtx, "ROOT", sessionToken);
		 String feature1 = FileUtils.fileToString(filePath + "feature1.txt", "UTF-8", false);
		 JSONObject json1 = new JSONObject(feature1);
		 json1.put("stage", "PRODUCTION");
		 featureID1 = f.addFeature(seasonID, json1.toString(), mtxID1, sessionToken);
		 String feature2 = FileUtils.fileToString(filePath + "feature2.txt", "UTF-8", false);
		 JSONObject json2 = new JSONObject(feature2);
		 json2.put("stage", "PRODUCTION");
		 featureID2 = f.addFeature(seasonID, json2.toString(), mtxID1, sessionToken);
		
		//check if files were changed
		f.setSleep();
		RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getDevelopmentFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseDev.code ==200, "Runtime development feature file was not updated");
		RuntimeRestApi.DateModificationResults responseProd = RuntimeDateUtilities.getProductionFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseProd.code ==200, "Runtime production feature file was not changed");
		RuntimeRestApi.DateModificationResults prodChanged = RuntimeDateUtilities.getProductionChangedDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(prodChanged.code ==200, "productionChanged.txt file was not changed");
		String productionChangedNew = RuntimeDateUtilities.getProductionChangedFile(m_url, productID, seasonID, sessionToken);
		Assert.assertFalse(RuntimeDateUtilities.ifProductionChangedContent(productionChangedOriginal, productionChangedNew), "productionChanged.txt content was not changed");
		
	}
	
	@Test (dependsOnMethods = "createFirstMTX", description="Create an mtx group in development under ROOT")
	public void createSecondMTX() throws JSONException, IOException, InterruptedException{
		String dateFormat = f.setDateFormat();
		String productionChangedOriginal = RuntimeDateUtilities.getProductionChangedFile(m_url, productID, seasonID, sessionToken);
		
		String mtx = FileUtils.fileToString(filePath + "feature-mutual.txt", "UTF-8", false);
		 mtxID2 = f.addFeature(seasonID, mtx, featureID1, sessionToken);
		 String feature1 = FileUtils.fileToString(filePath + "feature3.txt", "UTF-8", false);
		 JSONObject json1 = new JSONObject(feature1);
		 json1.put("stage", "PRODUCTION");
		 featureID3 = f.addFeature(seasonID, json1.toString(), mtxID2, sessionToken);
		 String feature2 = FileUtils.fileToString(filePath + "feature4.txt", "UTF-8", false);
		 JSONObject json2 = new JSONObject(feature2);
		 json1.put("stage", "PRODUCTION");
		 featureID4 = f.addFeature(seasonID, json2.toString(), mtxID2, sessionToken);
		
		//check if files were changed
		f.setSleep();
		RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getDevelopmentFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseDev.code ==200, "Runtime development feature file was not updated");
		RuntimeRestApi.DateModificationResults responseProd = RuntimeDateUtilities.getProductionFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseProd.code ==200, "Runtime production feature file was not changed");
		RuntimeRestApi.DateModificationResults prodChanged = RuntimeDateUtilities.getProductionChangedDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(prodChanged.code ==200, "productionChanged.txt file was not changed");
		String productionChangedNew = RuntimeDateUtilities.getProductionChangedFile(m_url, productID, seasonID, sessionToken);
		Assert.assertFalse(RuntimeDateUtilities.ifProductionChangedContent(productionChangedOriginal, productionChangedNew), "productionChanged.txt content was not changed");

		
		//check development file content
		JSONObject root = RuntimeDateUtilities.getFeaturesList(responseDev.message);
		JSONArray features = root.getJSONArray("features");
		
		JSONArray mtx1 = features.getJSONObject(0).getJSONArray("features");
		JSONArray mtx2 = mtx1.getJSONObject(0).getJSONArray("features");
		
		Assert.assertTrue(mtx2.size()==1, "The inner mtx group is not under the parent feature");
		
	}
	
	@Test (dependsOnMethods = "createSecondMTX", description="Create a sub configuration in production")
	public void createSubConfiguration() throws JSONException, IOException, InterruptedException{
		String dateFormat = f.setDateFormat();
		String productionChangedOriginal = RuntimeDateUtilities.getProductionChangedFile(m_url, productID, seasonID, sessionToken);
		
		String config = FileUtils.fileToString(filePath + "configuration_rule1.txt", "UTF-8", false);
		JSONObject json1 = new JSONObject(config);
		json1.put("stage", "PRODUCTION");
		configID1 = f.addFeature(seasonID, json1.toString(), featureID2, sessionToken);
		Assert.assertFalse(configID1.contains("error"), "Test should pass, but instead failed: " + configID1 );
		
		String config2 = FileUtils.fileToString(filePath + "configuration_rule2.txt", "UTF-8", false);
		JSONObject json2 = new JSONObject(config2);
		json2.put("stage", "PRODUCTION");
		String configID2 = f.addFeature(seasonID, json2.toString(), configID1, sessionToken);
		Assert.assertFalse(configID2.contains("error"), "Test should pass, but instead failed: " + configID2 );

		
		
		//check if files were changed
		f.setSleep();
		RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getDevelopmentFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseDev.code ==200, "Runtime development feature file was not updated");
		RuntimeRestApi.DateModificationResults responseProd = RuntimeDateUtilities.getProductionFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseProd.code ==200, "Runtime production feature file was not changed");
		RuntimeRestApi.DateModificationResults prodChanged = RuntimeDateUtilities.getProductionChangedDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(prodChanged.code ==200, "productionChanged.txt file was not changed");
		String productionChangedNew = RuntimeDateUtilities.getProductionChangedFile(m_url, productID, seasonID, sessionToken);
		Assert.assertFalse(RuntimeDateUtilities.ifProductionChangedContent(productionChangedOriginal, productionChangedNew), "productionChanged.txt content was not changed");

		//check development file content
		JSONObject root = RuntimeDateUtilities.getFeaturesList(responseProd.message);
		JSONArray features = root.getJSONArray("features").getJSONObject(0).getJSONArray("features");
		
		Assert.assertTrue(getNumberOfConfigurations(features, featureID2)==1, "Incorrect subconfiguration in the production file");
		
		
	}
	
	@Test (dependsOnMethods = "createSubConfiguration", description="Create a new season")
	public void createSecondSeason() throws InterruptedException, JSONException, IOException {
		String dateFormat = f.setDateFormat();
		String productionChangedOriginal = RuntimeDateUtilities.getProductionChangedFile(m_url, productID, seasonID, sessionToken);
		
		String season2 = FileUtils.fileToString(filePath + "season2.txt", "UTF-8", false);
		seasonID2 = s.addSeason(productID, season2, sessionToken);
		Assert.assertFalse(seasonID2.contains("error"), "Second season was not created: "  + seasonID2);
		
		f.setSleep();
		RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getDevelopmentFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseDev.code ==200, "Runtime development feature file was not created");
		RuntimeRestApi.DateModificationResults responseProd = RuntimeDateUtilities.getDevelopmentFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseProd.code ==200, "Runtime production feature file was not created");
		RuntimeRestApi.DateModificationResults prodChanged = RuntimeDateUtilities.getProductionChangedDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(prodChanged.code ==200, "productionChanged.txt file was not created");
		String productionChangedNew = RuntimeDateUtilities.getProductionChangedFile(m_url, productID, seasonID, sessionToken);
		Assert.assertFalse(RuntimeDateUtilities.ifProductionChangedContent(productionChangedOriginal, productionChangedNew), "productionChanged.txt content was not changed");
	
		
		//check development file content
		JSONObject root = RuntimeDateUtilities.getFeaturesList(responseDev.message);
		JSONArray features = root.getJSONArray("features");
		JSONArray mtx1 = features.getJSONObject(0).getJSONArray("features");
		JSONArray mtx2 = mtx1.getJSONObject(0).getJSONArray("features");		
		Assert.assertTrue(mtx2.size()==1, "The inner mtx group is not under the parent feature");
		
		String response = f.getFeature(featureID2, sessionToken);
		JSONObject json = new JSONObject(response);
		
		Assert.assertTrue(getNumberOfConfigurationsByName(mtx1, json.getString("name"))==1, "Incorrect subconfiguration in the development file");

		//check production file content
		//check development file content
		root = RuntimeDateUtilities.getFeaturesList(responseProd.message);
		features = root.getJSONArray("features");
		mtx1 = features.getJSONObject(0).getJSONArray("features");
		mtx2 = mtx1.getJSONObject(0).getJSONArray("features");		
		Assert.assertTrue(mtx2.size()==1, "The inner mtx group is not under the parent feature");
		Assert.assertTrue(getNumberOfConfigurations(mtx1, featureID2)==1, "Incorrect subconfiguration in the production file");

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
	
	private int getNumberOfConfigurationsByName(JSONArray features, String featureName) throws JSONException{
		JSONArray subConfigurations = new JSONArray();
		boolean updated = false;
		for(int i=0; i<features.size(); i++){
			if (features.getJSONObject(i).getString("name").equals(featureName)) {
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
