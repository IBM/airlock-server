package tests.restapi.scenarios.feature_with_configuration;

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

public class AddAndUpdateDefaultConfiguration {
	protected String seasonID;
	protected String featureID;
	protected String productID;
	protected String filePath;
	protected FeaturesRestApi f;
	protected ProductsRestApi p;
	private String sessionToken = "";
	private AirlockUtils baseUtils;
	
	@BeforeClass
 	@Parameters({"url", "analyticsUrl", "translationsUrl", "configPath", "sessionToken", "userName", "userPassword", "appName", "productsToDeleteFile"})
	public void init(String url, String analyticsUrl, String translationsUrl, String configPath, String sToken, String userName, String userPassword, String appName, String productsToDeleteFile) throws Exception{
		filePath = configPath;
		f = new FeaturesRestApi();
		f.setURL(url);
		p = new ProductsRestApi();
		p.setURL(url);
		baseUtils = new AirlockUtils(url, analyticsUrl, translationsUrl, configPath, sToken, userName, userPassword, appName, productsToDeleteFile);
		sessionToken = baseUtils.sessionToken;
		productID = baseUtils.createProduct();
		baseUtils.printProductToFile(productID);
		seasonID = baseUtils.createSeason(productID);
		String feature = FileUtils.fileToString(filePath + "feature1.txt", "UTF-8", false);
		featureID = f.addFeature(seasonID, feature, "ROOT", sessionToken );

	}

	
	
	@Test ( description = "Add defaultSchema object to an existing feature")
	public void createFeatureIncludingDefaultSchema() throws IOException, JSONException{
		String feature = f.getFeature(featureID, sessionToken);
		JSONObject configSchema = new JSONObject();
		configSchema.put("color", "red");
		configSchema.put("text", "some text");
		JSONObject json = new JSONObject(feature);
		json.put("defaultConfiguration", configSchema.toString());

		String response = f.updateFeature(seasonID, featureID, json.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Test should pass, but instead failed: " + response );
	}
	
	
	@Test (dependsOnMethods="createFeatureIncludingDefaultSchema",description = "Update defaultSchema object")
	public void updateDefaultSchema() throws IOException, JSONException{
		String feature = f.getFeature(featureID, sessionToken);
		
		JSONObject configSchema = new JSONObject();
		configSchema.put("color", "green");
		configSchema.put("text", "new text");
		JSONObject json = new JSONObject(feature);
		json.put("defaultConfiguration", configSchema.toString());

		String response = f.updateFeature(seasonID, featureID, json.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Test should pass, but instead failed: " + response );
		
		feature = f.getFeature(featureID, sessionToken);
		json = new JSONObject(feature);
		
		configSchema = new JSONObject(json.getString("defaultConfiguration"));
		Assert.assertTrue(configSchema.getString("color").equals("green"), "defaultSchema was not updated");
		Assert.assertTrue(configSchema.getString("text").equals("new text"), "defaultSchema was not updated");
	}
	


	@AfterTest
	private void reset(){
		baseUtils.reset(productID, sessionToken);
	}

}
