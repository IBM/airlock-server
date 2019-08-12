package tests.restapi.scenarios.utilities;

import java.io.File;

import java.io.IOException;
import java.io.StringReader;
import java.util.Properties;

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
import tests.restapi.UtilitiesRestApi;

public class NonExistingUtilityInConfigurationRule {
	protected String seasonID;
	protected String featureID;
	protected String configID;
	protected String productID;
	protected String filePath;
	protected FeaturesRestApi f;
	protected ProductsRestApi p;
	protected UtilitiesRestApi u;
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
		u = new UtilitiesRestApi();
		u.setURL(url);
		baseUtils = new AirlockUtils(url, analyticsUrl, translationsUrl, configPath, sToken, userName, userPassword, appName, productsToDeleteFile);
		sessionToken = baseUtils.sessionToken;
		productID = baseUtils.createProduct();
		baseUtils.printProductToFile(productID);
		seasonID = baseUtils.createSeason(productID);
		String feature = FileUtils.fileToString(filePath + "feature1.txt", "UTF-8", false);
		featureID = f.addFeature(seasonID, feature, "ROOT", sessionToken);
	}
	
	@Test(description = "Add configuration rule with rule using non-existing utility")
	public void addInvalidConfigurationRule() throws IOException, JSONException{
		String feature = FileUtils.fileToString(filePath + "configuration_rule1.txt", "UTF-8", false);
		JSONObject fJson = new JSONObject(feature);
		JSONObject rule = new JSONObject();
		rule.put("ruleString", "getLocation()");
		fJson.put("rule", rule);
		String response = f.addFeature(seasonID, fJson.toString(), featureID, sessionToken);
		Assert.assertTrue(response.contains("error"), "Test should fail, but instead passed: " + response );
	}
	
	@Test(dependsOnMethods = "addInvalidConfigurationRule", description = "Add valid utility")
	public void addUtility() throws IOException, JSONException{
		
		String utility = FileUtils.fileToString(filePath + "utilities" + File.separator + "utility1.txt", "UTF-8", false);
		Properties utilProps = new Properties();
		utilProps.load(new StringReader(utility));
		utilProps.setProperty("utility", "function isMetric(){return true;}");
		
		String response = u.addUtility(seasonID, utilProps, sessionToken);
		Assert.assertFalse(response.contains("error"), "Test should pass, but instead failed: " + response );
	}
	
	@Test(dependsOnMethods = "addUtility", description = "Add feature")
	public void addConfigurationRule() throws IOException, JSONException{
		String feature = FileUtils.fileToString(filePath + "configuration_rule1.txt", "UTF-8", false);
		JSONObject fJson = new JSONObject(feature);
		JSONObject rule = new JSONObject();
		rule.put("ruleString", "isMetric()");
		fJson.put("rule", rule);
		configID = f.addFeature(seasonID, fJson.toString(), featureID, sessionToken);
		Assert.assertFalse(configID.contains("error"), "Test should pass, but instead failed: " + configID );
	}
	
	@Test(dependsOnMethods = "addConfigurationRule", description = "Update configuration rule with rule using utility that doesn't exist")
	public void updateFeature() throws IOException, JSONException{
		String feature = f.getFeature(configID, sessionToken);
		JSONObject fJson = new JSONObject(feature);
		JSONObject rule = new JSONObject();
		rule.put("ruleString", "getLocation()");
		fJson.put("rule", rule);
		String response = f.updateFeature(seasonID, configID, fJson.toString(), sessionToken);
		Assert.assertTrue(response.contains("error"), "Test should fail, but instead passed: " + response );
	}
	
	@AfterTest
	private void reset(){
		baseUtils.reset(productID, sessionToken);
	}

}
