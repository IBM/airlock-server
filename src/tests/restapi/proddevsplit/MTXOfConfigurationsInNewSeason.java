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

public class MTXOfConfigurationsInNewSeason {
	protected String seasonID;
	protected String featureID;
	protected String configMtx;
	protected String configFeatureID1;
	protected String configFeatureID2;
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

	@Test (description="Create an mtx group in development")
	public void createFeatureInDev() throws JSONException, IOException, InterruptedException{
		String dateFormat = f.setDateFormat();
		

		 String feature1 = FileUtils.fileToString(filePath + "feature1.txt", "UTF-8", false);
		 featureID = f.addFeature(seasonID, feature1, "ROOT", sessionToken);
		
		 String config = FileUtils.fileToString(filePath + "configuration_feature-mutual.txt", "UTF-8", false);
		 configMtx = f.addFeature(seasonID, config, featureID, sessionToken);
		 Assert.assertFalse(configMtx.contains("error"), "Test should pass, but instead failed: " + configMtx );

		 String config1 = FileUtils.fileToString(filePath + "configuration_rule1.txt", "UTF-8", false);
		 configFeatureID1 = f.addFeature(seasonID, config1, configMtx, sessionToken);
		 Assert.assertFalse(configFeatureID1.contains("error"), "Test should pass, but instead failed: " + configFeatureID1 );

		 String config2 = FileUtils.fileToString(filePath + "configuration_rule2.txt", "UTF-8", false);
		 configFeatureID2 = f.addFeature(seasonID, config2, configMtx, sessionToken);
		 Assert.assertFalse(configFeatureID2.contains("error"), "Test should pass, but instead failed: " + configFeatureID2 );
		 
		//check if files were changed
		f.setSleep();
		RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getDevelopmentFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseDev.code ==200, "Runtime development feature file was not updated");
		RuntimeRestApi.DateModificationResults responseProd = RuntimeDateUtilities.getProductionFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseProd.code ==304, "Runtime production feature file was changed");
		RuntimeRestApi.DateModificationResults prodChanged = RuntimeDateUtilities.getProductionChangedDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(prodChanged.code ==304, "productionChanged.txt file was changed");

	}
	
	@Test (dependsOnMethods = "createFeatureInDev", description="Move parent feature to production")
	public void updateFeatureToProd() throws JSONException, IOException, InterruptedException{
		String dateFormat = f.setDateFormat();
		
		String feature = f.getFeature(featureID, sessionToken);
		JSONObject json = new JSONObject(feature);
		json.put("stage", "PRODUCTION");
		featureID = f.updateFeature(seasonID, featureID, json.toString(), sessionToken);
		Assert.assertFalse(featureID.contains("error"), "Test should pass, but instead failed: " + featureID );
		
		f.setSleep();
		RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getDevelopmentFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseDev.code ==200, "Runtime development feature file was not updated");
		RuntimeRestApi.DateModificationResults responseProd = RuntimeDateUtilities.getProductionFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseProd.code ==200, "Runtime production feature file was not updated");
		RuntimeRestApi.DateModificationResults prodChanged = RuntimeDateUtilities.getProductionChangedDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(prodChanged.code ==200, "productionChanged.txt file was not changed");

		
		//check production file content
		JSONObject root = RuntimeDateUtilities.getFeaturesList(responseProd.message);
		JSONArray features = root.getJSONArray("features");
		Assert.assertTrue(features.size()==1, "Incorrect number of features in the production file");
		Assert.assertTrue(features.getJSONObject(0).getJSONArray("features").size()==0, "Child in development was written to production file");
	
		//check development file content
		root = RuntimeDateUtilities.getFeaturesList(responseDev.message);
		features = root.getJSONArray("features");
		Assert.assertTrue(features.size()==1, "Incorrect number of features in the development file");
		Assert.assertTrue(features.getJSONObject(0).getJSONArray("configurationRules").size()==1, "Child in development was not written to development file");

		
	}
	
	@Test (dependsOnMethods = "updateFeatureToProd", description="Move parent feature to production")
	public void updateConfigurationsToProd() throws JSONException, IOException, InterruptedException{
		String dateFormat = f.setDateFormat();
		
		String feature1 = f.getFeature(configFeatureID1, sessionToken);
		JSONObject json = new JSONObject(feature1);
		json.put("stage", "PRODUCTION");
		configFeatureID1 = f.updateFeature(seasonID, configFeatureID1, json.toString(), sessionToken);
		Assert.assertFalse(configFeatureID1.contains("error"), "Test should pass, but instead failed: " + configFeatureID1 );
		
		String feature2 = f.getFeature(configFeatureID2, sessionToken);
		JSONObject json2 = new JSONObject(feature2);
		json2.put("stage", "PRODUCTION");
		configFeatureID2 = f.updateFeature(seasonID, configFeatureID2, json2.toString(), sessionToken);
		Assert.assertFalse(configFeatureID2.contains("error"), "Test should pass, but instead failed: " + configFeatureID2 );

		
		f.setSleep();
		RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getDevelopmentFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseDev.code ==200, "Runtime development feature file was not updated");
		RuntimeRestApi.DateModificationResults responseProd = RuntimeDateUtilities.getProductionFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseProd.code ==200, "Runtime production feature file was not updated");
		RuntimeRestApi.DateModificationResults prodChanged = RuntimeDateUtilities.getProductionChangedDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(prodChanged.code ==200, "productionChanged.txt file was not changed");

		
		//check production file content
		JSONObject root = RuntimeDateUtilities.getFeaturesList(responseProd.message);
		JSONArray features = root.getJSONArray("features");
		Assert.assertTrue(features.size()==1, "Incorrect number of features in the production file");
		Assert.assertTrue(features.getJSONObject(0).getJSONArray("configurationRules").size()==1, "Child in development was written to production file");
	
		//check development file content
		root = RuntimeDateUtilities.getFeaturesList(responseDev.message);
		features = root.getJSONArray("features");
		Assert.assertTrue(features.size()==1, "Incorrect number of features in the development file");
		Assert.assertTrue(features.getJSONObject(0).getJSONArray("configurationRules").size()==1, "Child in development was not written to development file");

		
	}
	
	@Test (dependsOnMethods="updateConfigurationsToProd", description = "Change featrues order in mutually exclusive group")
	public void testChangeOrder() throws JSONException, IOException, InterruptedException{
		String dateFormat = f.setDateFormat();
		
		String parent = f.getFeature(configMtx, sessionToken);
		JSONObject json = new JSONObject(parent);
		json.remove("configurationRules");
		JSONArray children = new JSONArray();
		JSONObject child2 = new JSONObject(f.getFeature(configFeatureID2, sessionToken));
		children.put(child2);
		JSONObject child1 = new JSONObject(f.getFeature(configFeatureID1, sessionToken));
		children.put(child1);

		json.put("configurationRules", children);
		String response = f.updateFeature(seasonID, configMtx, json.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "MTX group was not updated");
	
		f.setSleep();
		RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getDevelopmentFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseDev.code ==200, "Runtime development feature file was not updated");
		RuntimeRestApi.DateModificationResults responseProd = RuntimeDateUtilities.getProductionFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseProd.code ==200, "Runtime production feature file was not updated");
		RuntimeRestApi.DateModificationResults prodChanged = RuntimeDateUtilities.getProductionChangedDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(prodChanged.code ==200, "productionChanged.txt file was not changed");

		//check production file content
		JSONObject root = RuntimeDateUtilities.getFeaturesList(responseProd.message);
		JSONObject parentFeature = root.getJSONArray("features").getJSONObject(0);
		
		JSONArray mtxRoot = parentFeature.getJSONArray("configurationRules");
		JSONArray newChildren = mtxRoot.getJSONObject(0).getJSONArray("configurationRules");
		Assert.assertTrue(newChildren.size()==2, "Incorrect number of children features after the order change.");

		Assert.assertEquals(children.getJSONObject(0).get("uniqueId"), configFeatureID2, "The first child is incorrect");
		Assert.assertEquals(children.getJSONObject(1).get("uniqueId"), configFeatureID1, "The second child is incorrect");
		

	}
	
	@Test (dependsOnMethods = "testChangeOrder", description="Update configurations to development ")
	public void updateConfigurationsToDev() throws JSONException, IOException, InterruptedException{
		String dateFormat = f.setDateFormat();
		
		String feature1 = f.getFeature(configFeatureID1, sessionToken);
		JSONObject json = new JSONObject(feature1);
		json.put("stage", "DEVELOPMENT");
		configFeatureID1 = f.updateFeature(seasonID, configFeatureID1, json.toString(), sessionToken);
		Assert.assertFalse(configFeatureID1.contains("error"), "Test should pass, but instead failed: " + configFeatureID1 );
		
		String feature2 = f.getFeature(configFeatureID2, sessionToken);
		JSONObject json2 = new JSONObject(feature2);
		json2.put("stage", "DEVELOPMENT");
		configFeatureID2 = f.updateFeature(seasonID, configFeatureID2, json2.toString(), sessionToken);
		Assert.assertFalse(configFeatureID2.contains("error"), "Test should pass, but instead failed: " + configFeatureID2 );

		
		f.setSleep();
		RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getDevelopmentFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseDev.code ==200, "Runtime development feature file was not updated");
		RuntimeRestApi.DateModificationResults responseProd = RuntimeDateUtilities.getProductionFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseProd.code ==200, "Runtime production feature file was not updated");
		RuntimeRestApi.DateModificationResults prodChanged = RuntimeDateUtilities.getProductionChangedDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(prodChanged.code ==200, "productionChanged.txt file was not changed");

		
		//check production file content
		JSONObject root = RuntimeDateUtilities.getFeaturesList(responseProd.message);
		JSONArray features = root.getJSONArray("features");
		Assert.assertTrue(features.size()==1, "Incorrect number of features in the production file");
		Assert.assertTrue(features.getJSONObject(0).getJSONArray("configurationRules").size()==0, "Child in development was written to production file");
	
		//check development file content
		root = RuntimeDateUtilities.getFeaturesList(responseDev.message);
		features = root.getJSONArray("features");
		Assert.assertTrue(features.size()==1, "Incorrect number of features in the development file");
		Assert.assertTrue(features.getJSONObject(0).getJSONArray("configurationRules").size()==1, "Child in development was not written to development file");
	}
	

	
	@Test (dependsOnMethods = "updateConfigurationsToDev", description="Move feature1 from production to development")
	public void moveFeatureToDev() throws JSONException, IOException, InterruptedException{
		String dateFormat = f.setDateFormat();
		
		String feature = f.getFeature(featureID, sessionToken);
		JSONObject json = new JSONObject(feature);
		json.put("stage", "DEVELOPMENT");
		featureID = f.updateFeature(seasonID, featureID, json.toString(), sessionToken);

		f.setSleep();
		RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getDevelopmentFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseDev.code==200, "Runtime development feature file was not updated");
		RuntimeRestApi.DateModificationResults responseProd = RuntimeDateUtilities.getProductionFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseProd.code==200, "Runtime production feature file was not updated");
		RuntimeRestApi.DateModificationResults prodChanged = RuntimeDateUtilities.getProductionChangedDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(prodChanged.code ==200, "productionChanged.txt file was not changed");

		
		JSONObject root = RuntimeDateUtilities.getFeaturesList(responseProd.message);
		JSONArray features = root.getJSONArray("features");
		Assert.assertTrue(features.size()==0, "Feature was not removed from production file");
		
		//check that the feature was updated in the development file
		root = RuntimeDateUtilities.getFeaturesList(responseDev.message);
		features = root.getJSONArray("features");
		Assert.assertTrue(features.getJSONObject(0).getString("stage").equals("DEVELOPMENT"), "Feature was not updated in the development file");
		
	}
	
	@Test (dependsOnMethods = "moveFeatureToDev", description="Delete feature in dev")
	public void deleteFeature() throws JSONException, IOException, InterruptedException{
		String dateFormat = f.setDateFormat();
		
		int responseCode = f.deleteFeature(featureID, sessionToken);
		Assert.assertTrue(responseCode == 200, "Feature was not deleted");

		f.setSleep();
		RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getDevelopmentFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseDev.code==200, "Runtime development feature file was not updated");
		RuntimeRestApi.DateModificationResults responseProd = RuntimeDateUtilities.getProductionFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseProd.code==304, "Runtime production feature file was changed");
		RuntimeRestApi.DateModificationResults prodChanged = RuntimeDateUtilities.getProductionChangedDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(prodChanged.code ==304, "productionChanged.txt file was changed");

		
		JSONObject root = RuntimeDateUtilities.getFeaturesList(responseDev.message);
		JSONArray features = root.getJSONArray("features");
		Assert.assertTrue(features.size()==0, "Feature was not removed from development file");

	}
	


	@AfterTest
	private void reset(){
		baseUtils.reset(productID, sessionToken);
	}
}
