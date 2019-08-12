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

public class ConfigurationJsonValidation {
	protected String seasonID;
	protected String feature;
	protected String configurationID;
	protected String filePath;
	protected FeaturesRestApi f;
	protected ProductsRestApi p;
	private AirlockUtils baseUtils;
	protected String productID;
	protected String m_url;
	private String sessionToken = "";
	
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
		feature = FileUtils.fileToString(filePath + "feature1.txt", "UTF-8", false);
		

	}



	@Test (description ="Add defaultSchema with invalid json")
	public void invalidDefaultSchemaJson() throws JSONException, IOException{


		String  defaultSchema = "{\"text\":\"test";
		JSONObject json = new JSONObject(feature);
		json.put("defaultConfiguration", defaultSchema);
		String response = f.addFeature(seasonID, json.toString(), "ROOT", sessionToken);
		Assert.assertTrue(response.contains("error"), "Test should fail, but instead passed: " + response );
	}
	
	
	
	@Test (description ="Add output configuration with invalid json")
	public void invalidConfigurationJson() throws JSONException, IOException{
		String featureID = f.addFeature(seasonID, feature, "ROOT", sessionToken);
		String configuration = FileUtils.fileToString(filePath + "configuration_rule1.txt", "UTF-8", false);
		
		String  outputConfig = "{\"text\":\"test";
		JSONObject json = new JSONObject(configuration);
		json.put("configuration", outputConfig);
		configurationID = f.addFeature(seasonID, json.toString(), featureID, sessionToken);
		Assert.assertTrue(configurationID.contains("error"), "Test should fail, but instead passed: " + configurationID );
	}

	

	@AfterTest
	private void reset(){
		baseUtils.reset(productID, sessionToken);
	}
}
