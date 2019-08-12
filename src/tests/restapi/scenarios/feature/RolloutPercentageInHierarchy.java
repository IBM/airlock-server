package tests.restapi.scenarios.feature;

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


public class RolloutPercentageInHierarchy {
	protected String seasonID;
	protected String featureID;
	protected String featureID2;
	protected String configID;
	protected String productID;
	protected String filePath;
	protected JSONObject json;
	protected FeaturesRestApi f;
	protected ProductsRestApi p;
	private String sessionToken = "";
	protected AirlockUtils baseUtils;
	
	
	@BeforeClass
 	@Parameters({"url", "analyticsUrl", "translationsUrl", "configPath", "sessionToken", "userName", "userPassword", "appName", "productsToDeleteFile"})
	public void init(String url, String analyticsUrl, String translationsUrl, String configPath, String sToken, String userName, String userPassword, String appName, String productsToDeleteFile) throws Exception{
		filePath = configPath;
		f = new FeaturesRestApi();
		f.setURL(url);
		p = new ProductsRestApi();
		p.setURL(url);
		if (sToken != null)
			sessionToken = sToken;
		baseUtils = new AirlockUtils(url, analyticsUrl, translationsUrl, configPath, sToken, userName, userPassword, appName, productsToDeleteFile);
		sessionToken = baseUtils.sessionToken;
		productID = baseUtils.createProduct();
		baseUtils.printProductToFile(productID);
		seasonID = baseUtils.createSeason(productID);
	}
	
	
	@Test (description="Create feature and subfeature with rolloutPercentage=0.001")
	public void rolloutPercentageSubFeatureInCreate() throws JSONException, IOException, InterruptedException{

		String feature = FileUtils.fileToString(filePath + "feature1.txt", "UTF-8", false);
		JSONObject json = new JSONObject(feature);
		json.put("rolloutPercentage", 0.001);
		featureID = f.addFeature(seasonID, json.toString(), "ROOT", sessionToken);
		Assert.assertFalse(featureID.contains("error"), "Feature was not created " + featureID);
		
		String feature2 = FileUtils.fileToString(filePath + "feature2.txt", "UTF-8", false);
		JSONObject json2 = new JSONObject(feature2);
		json2.put("rolloutPercentage", 0.001);
		featureID2 = f.addFeature(seasonID, json2.toString(), featureID, sessionToken);
		Assert.assertFalse(featureID2.contains("error") || featureID2.contains("Invalid"), "Feature was not created " + featureID2);

	}
	
	@Test (dependsOnMethods="rolloutPercentageSubFeatureInCreate", description="Update feature subfeature with rolloutPercentage=0.001")
	public void rolloutPercentageSubfeatureInUpdate() throws JSONException, IOException, InterruptedException{
		
		String feature = FileUtils.fileToString(filePath + "feature3.txt", "UTF-8", false);
		JSONObject json = new JSONObject(feature);
		json.put("rolloutPercentage", 100);
		String featureID3 = f.addFeature(seasonID, json.toString(), featureID, sessionToken);
		Assert.assertFalse(featureID2.contains("error"), "Configuration was not created " + featureID2);

		feature = f.getFeature(featureID3, sessionToken);
		json = new JSONObject(feature);
		json.put("rolloutPercentage", 0.001);
		featureID3 = f.updateFeature(seasonID, featureID3, json.toString(), sessionToken);
		Assert.assertFalse(featureID3.contains("error") || featureID3.contains("Invalid"), "Feature was not created " + featureID2);
	}
	
	@Test (dependsOnMethods="rolloutPercentageSubfeatureInUpdate", description="Create configuration with rolloutPercentage=0.001")
	public void rolloutPercentageConfigurationInCreate() throws JSONException, IOException, InterruptedException{
		
		String configuration = FileUtils.fileToString(filePath + "configuration_rule1.txt", "UTF-8", false);
		JSONObject jsonCR = new JSONObject(configuration);
		jsonCR.put("rolloutPercentage", 0.001);
		configID = f.addFeature(seasonID, jsonCR.toString(), featureID, sessionToken);
		Assert.assertFalse(configID.contains("error") || configID.contains("Invalid"), "Configuration was not created " + featureID2);

	}
	
	@Test (dependsOnMethods="rolloutPercentageConfigurationInCreate", description="Create feature and subfeature with rolloutPercentage=0.001")
	public void rolloutPercentageConfigurationInUpdate() throws JSONException, IOException, InterruptedException{
		
		String configuration = FileUtils.fileToString(filePath + "configuration_rule2.txt", "UTF-8", false);
		JSONObject jsonCR = new JSONObject(configuration);
		jsonCR.put("rolloutPercentage", 100);
		String configID2 = f.addFeature(seasonID, jsonCR.toString(), featureID, sessionToken);
		Assert.assertFalse(configID2.contains("error") || configID2.contains("Invalid"), "Configuration was not created " + featureID2);

		configuration = f.getFeature(configID2, sessionToken);
		jsonCR = new JSONObject(configuration);
		jsonCR.put("rolloutPercentage", 0.001);
		configID2 = f.updateFeature(seasonID, configID2, configuration, sessionToken);
		Assert.assertFalse(configID2.contains("error") || configID2.contains("Invalid"), "Configuration was not created " + featureID2);
	}
	


	@AfterTest
	private void reset(){
		baseUtils.reset(productID, sessionToken);
	}
}
