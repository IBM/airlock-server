package tests.restapi.proddevsplit;

import java.io.IOException;


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

public class MaxFeatureOnInMTXConfigurations {
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


	@Test (description="Create feature and mtx of configurations in production")
	public void createFeatures() throws JSONException, IOException, InterruptedException{
		String dateFormat = f.setDateFormat();
		

		 String feature1 = FileUtils.fileToString(filePath + "feature1.txt", "UTF-8", false);
		 JSONObject json = new JSONObject(feature1);
		 json.put("stage", "PRODUCTION");
		 featureID = f.addFeature(seasonID, json.toString(), "ROOT", sessionToken);
		
		 String config = FileUtils.fileToString(filePath + "configuration_feature-mutual.txt", "UTF-8", false);
		 configMtx = f.addFeature(seasonID, config, featureID, sessionToken);
		 Assert.assertFalse(configMtx.contains("error"), "Test should pass, but instead failed: " + configMtx );

		 String config1 = FileUtils.fileToString(filePath + "configuration_rule1.txt", "UTF-8", false);
		 JSONObject json1 = new JSONObject(config1);
		 json1.put("stage", "PRODUCTION");
		 configFeatureID1 = f.addFeature(seasonID, json1.toString(), configMtx, sessionToken);
		 Assert.assertFalse(configFeatureID1.contains("error"), "Test should pass, but instead failed: " + configFeatureID1 );

		 String config2 = FileUtils.fileToString(filePath + "configuration_rule2.txt", "UTF-8", false);
		 JSONObject json2 = new JSONObject(config2);
		 json2.put("stage", "PRODUCTION");
		 configFeatureID2 = f.addFeature(seasonID, json2.toString(), configMtx, sessionToken);
		 Assert.assertFalse(configFeatureID2.contains("error"), "Test should pass, but instead failed: " + configFeatureID2 );
		 
		//check if files were changed
		f.setSleep();
		RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getDevelopmentFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseDev.code ==200, "Runtime development feature file was not updated");
		RuntimeRestApi.DateModificationResults responseProd = RuntimeDateUtilities.getProductionFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseProd.code ==200, "Runtime production feature file was not changed");
		RuntimeRestApi.DateModificationResults prodChanged = RuntimeDateUtilities.getProductionChangedDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(prodChanged.code ==200, "productionChanged.txt file was not changed");

	}
	

	
	@Test (dependsOnMethods = "createFeatures", description="Update maxFeatureOn")
	public void changeMaxFeaturesOn() throws JSONException, IOException, InterruptedException{
		String dateFormat = f.setDateFormat();
		
		String parent = f.getFeature(configMtx, sessionToken);
		JSONObject json = new JSONObject(parent);
		json.put("maxFeaturesOn", 3);
		String reponse = f.updateFeature(seasonID, configMtx, json.toString(), sessionToken);
		Assert.assertFalse(reponse.contains("error"), "maxFeaturesOn was not updated");

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
		JSONObject mtxRoot = parentFeature.getJSONArray("configurationRules").getJSONObject(0);
		Assert.assertTrue(mtxRoot.getInt("maxFeaturesOn")==3, "maxFeaturesOn was not updated in the production file");

		
		//check development file content
		root = RuntimeDateUtilities.getFeaturesList(responseDev.message);
		parentFeature = root.getJSONArray("features").getJSONObject(0);
		 mtxRoot = parentFeature.getJSONArray("configurationRules").getJSONObject(0);
		Assert.assertTrue(mtxRoot.getInt("maxFeaturesOn")==3, "maxFeaturesOn was not updated in the development file");
	}

	
	@AfterTest
	private void reset(){
		baseUtils.reset(productID, sessionToken);
	}
}
