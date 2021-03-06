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

public class UpdateConfigurationRule {
	protected String seasonID;
	protected String featureID;
	protected String productID;
	protected String configRuleID;
	protected String filePath;
	protected FeaturesRestApi f;
	protected ProductsRestApi p;
	private AirlockUtils baseUtils;
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
		String feature = FileUtils.fileToString(filePath + "feature1.txt", "UTF-8", false);
		featureID = f.addFeature(seasonID, feature, "ROOT", sessionToken );

	}

	
	@Test (description = "Add configuration rule and assign an existing feature as its parent")
	public void createConfigurationRule() throws IOException, JSONException{
		String configuration = FileUtils.fileToString(filePath + "configuration_rule1.txt", "UTF-8", false);
		String response = configRuleID = f.addFeature(seasonID, configuration, featureID, sessionToken );
		Assert.assertFalse(response.contains("error"), "Test should pass, but instead failed: " + response );
	}
	
	@Test (dependsOnMethods="createConfigurationRule", description = "Update configuration rule")
	public void updateConfigurationRule() throws IOException, JSONException{
		
		String configuration = f.getFeature(configRuleID, sessionToken);
		JSONObject json = new JSONObject(configuration);
		json.put("name", "newCR1");
		String response = f.updateFeature(seasonID, configRuleID, json.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Test should pass, but instead failed: " + response );
	}
	
	@Test (dependsOnMethods="updateConfigurationRule", description = "Validate update of configuration rule")
	public void validateUpdateConfigurationRule() throws IOException, JSONException{
		String configuration = f.getFeature(configRuleID, sessionToken);
		JSONObject json = new JSONObject(configuration);
		Assert.assertTrue(json.getString("name").equals("newCR1"), "configuration rule name was not updated");
		
	}	
	

	@AfterTest
	private void reset(){
		baseUtils.reset(productID, sessionToken);
	}

}
