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

public class UtilityWithNonExistingContext {
	protected String seasonID;
	protected String featureID;
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
		baseUtils.createSchema(seasonID);
		
	}
	
	@Test(description = "Add valid utility")
	public void addUtility() throws IOException, JSONException{
		
		String utility = FileUtils.fileToString(filePath + "utilities" + File.separator + "utility1.txt", "UTF-8", false);
		Properties utilProps = new Properties();
		utilProps.load(new StringReader(utility));
		utilProps.put("utility", "function getDrivingDifficultyIndex(){context.weatherSummary.lifeIndices.drivingDifficultyIndex<1}");
		
		String response = u.addUtility(seasonID, utilProps, sessionToken);	
		Assert.assertFalse(response.contains("error"), "Test should pass, but instead failed: " + response );
	}
	
	//TODO: when rule validation will look at the new utilities, this test should fail since there is no context.weatherSummary.lifeIndices.drivingDifficultyIndex
	
	@Test(dependsOnMethods = "addUtility", description = "Add feature with rule using utility with non-existing field")
	public void addFeature() throws IOException, JSONException{
		String feature = FileUtils.fileToString(filePath + "feature1.txt", "UTF-8", false);
		JSONObject fJson = new JSONObject(feature);
		JSONObject rule = new JSONObject();
		rule.put("ruleString", "getDrivingDifficultyIndex()");
		fJson.put("rule", rule);
		String response = f.addFeature(seasonID, fJson.toString(), "ROOT", sessionToken);
		Assert.assertTrue(response.contains("error"), "Test should fail, but instead passed: " + response );
	}
	
	@Test(dependsOnMethods = "addFeature", description = "Add configuration rule with rule using utility with non-existing field")
	public void addConfigurationRule() throws IOException, JSONException{
		String feature = FileUtils.fileToString(filePath + "feature2.txt", "UTF-8", false);
		String parentID = f.addFeature(seasonID, feature, "ROOT", sessionToken);
		String configuration = FileUtils.fileToString(filePath + "configuration_rule1.txt", "UTF-8", false);
		JSONObject fJson = new JSONObject(configuration);
		JSONObject rule = new JSONObject();
		rule.put("ruleString", "getDrivingDifficultyIndex()");
		fJson.put("rule", rule);
		String response = f.addFeature(seasonID, fJson.toString(), parentID, sessionToken);
		Assert.assertTrue(response.contains("error"), "Test should fail, but instead passed: " + response );
	}
	
	
	@Test(dependsOnMethods = "addConfigurationRule", description = "Update feature with rule using utility with non-existing field")
	public void updateFeature() throws IOException, JSONException{
		String feature = FileUtils.fileToString(filePath + "feature1.txt", "UTF-8", false);
		featureID = f.addFeature(seasonID, feature, "ROOT", sessionToken);
		
		feature = f.getFeature(featureID, sessionToken);
		
		JSONObject fJson = new JSONObject(feature);
		JSONObject rule = new JSONObject();
		rule.put("ruleString", "getDrivingDifficultyIndex()");
		fJson.put("rule", rule);
		String response = f.updateFeature(seasonID, featureID, fJson.toString(), sessionToken);
		Assert.assertTrue(response.contains("error"), "Test should fail, but instead passed: " + response );
	}
	
	@Test(dependsOnMethods = "addFeature", description = "Update configuration rule with rule using utility with non-existing field")
	public void updateConfigurationRule() throws IOException, JSONException{
		String feature = FileUtils.fileToString(filePath + "feature3.txt", "UTF-8", false);
		String parentID = f.addFeature(seasonID, feature, "ROOT", sessionToken);
		String configuration = FileUtils.fileToString(filePath + "configuration_rule1.txt", "UTF-8", false);
		String configID = f.addFeature(seasonID, configuration, parentID, sessionToken);
		
		configuration = f.getFeature(configID, sessionToken);
		
		JSONObject fJson = new JSONObject(configuration);
		JSONObject rule = new JSONObject();
		rule.put("ruleString", "getDrivingDifficultyIndex()");
		fJson.put("rule", rule);
		String response = f.updateFeature(seasonID, configID, fJson.toString(), sessionToken);
		Assert.assertTrue(response.contains("error"), "Test should fail, but instead passed: " + response );
	}
	
	@AfterTest
	private void reset(){
		baseUtils.reset(productID, sessionToken);
	}

}
