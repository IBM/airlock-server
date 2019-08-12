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


public class FeatureAttributesHierarchy {
	protected String seasonID;
	protected String featureID;
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
	
	
	@Test (description="Create feature with default attributes and mix of 2 configurations")
	public void addComponents() throws JSONException, IOException, InterruptedException{

		String feature = FileUtils.fileToString(filePath + "feature1.txt", "UTF-8", false);
		JSONObject json = new JSONObject(feature);
		JSONObject defaultConfiguration = new JSONObject();
		defaultConfiguration.put("title1", "test");
		defaultConfiguration.put("title2", "test");
		json.put("defaultConfiguration", defaultConfiguration);
		featureID = f.addFeature(seasonID, json.toString(), "ROOT", sessionToken);
		Assert.assertFalse(featureID.contains("error"), "feature was not added to the season" + featureID);
		
		String config = FileUtils.fileToString(filePath + "configuration_rule1.txt", "UTF-8", false);
		JSONObject jsonConfig = new JSONObject(config);
		JSONObject newConfiguration = new JSONObject();
		newConfiguration.put("color1", "red");
		newConfiguration.put("color2", "white");
		jsonConfig.put("configuration", newConfiguration);
		String configID1 = f.addFeature(seasonID, jsonConfig.toString(), featureID, sessionToken);
		Assert.assertFalse(configID1.contains("error"), "Configuration1 was not added to the season" + configID1);
	
		String configMix = FileUtils.fileToString(filePath + "configuration_feature-mutual.txt", "UTF-8", false);
		String configIDMix = f.addFeature(seasonID, configMix, configID1, sessionToken);
		Assert.assertFalse(configIDMix.contains("error"), "Configuration mix was not added to the season" + configIDMix);

		String config2 = FileUtils.fileToString(filePath + "configuration_rule2.txt", "UTF-8", false);
		JSONObject jsonConfig2 = new JSONObject(config2);
		JSONObject newConfiguration2 = new JSONObject();
		newConfiguration2.put("size1", "large");
		jsonConfig2.put("configuration", newConfiguration2);
		String configID2 = f.addFeature(seasonID, jsonConfig2.toString(), configIDMix, sessionToken);
		Assert.assertFalse(configID2.contains("error"), "Configuration3 was not added to the season" + configID2);

		JSONObject jsonConfig3 = new JSONObject(config2);
		jsonConfig3.put("name", "config3");
		JSONObject newConfiguration3 = new JSONObject();
		newConfiguration3.put("color1", "blue");
		newConfiguration3.put("size2", "large");
		jsonConfig3.put("configuration", newConfiguration3);
		String configID3 = f.addFeature(seasonID, jsonConfig3.toString(), configIDMix, sessionToken);
		Assert.assertFalse(configID3.contains("error"), "Configuration3 was not added to the season" + configID3);

	}
	
	@Test (dependsOnMethods="addComponents", description="Check the number of  attributes")
	public void	getAttributes() throws JSONException{
		
		String response = f.getFeatureAttributes(featureID, sessionToken);
		JSONObject attrs = new JSONObject(response);
		Assert.assertTrue(attrs.getJSONArray("attributes").size() == 6, "Incorrect number of attributes");
	
	}

	

	@AfterTest
	private void reset(){
		baseUtils.reset(productID, sessionToken);
	}
}
