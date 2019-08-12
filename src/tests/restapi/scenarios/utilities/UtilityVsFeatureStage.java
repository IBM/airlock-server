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

public class UtilityVsFeatureStage {
	protected String seasonID;
	protected String featureID;
	protected String productID;
	protected String utilityID;
	protected String deepFreezeID;
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
		
		//String deepFreeze = FileUtils.fileToString(filePath + "utility_deepFreeze.txt", "UTF-8", false);
		//deepFreezeID = u.addUtility(seasonID, deepFreeze, sessionToken);
		
	}
	
	@Test(description = "Add valid utility in development stage")
	public void addUtility() throws IOException, JSONException{
		
		String utility = FileUtils.fileToString(filePath + "utilities" + File.separator + "utility1.txt", "UTF-8", false);
		Properties utilProps = new Properties();
		utilProps.load(new StringReader(utility));
		String response = utilityID = u.addUtility(seasonID, utilProps, sessionToken);
		Assert.assertFalse(response.contains("error"), "Test should pass, but instead failed: " + response );
	}
	
	//utility stage=dev, feature stage=prod
	@Test(dependsOnMethods = "addUtility", description = "utility in dev, feature in prod")
	public void addFeatureInProdUtilityInDev() throws IOException, JSONException{
		String feature = FileUtils.fileToString(filePath + "feature1.txt", "UTF-8", false);
		JSONObject fJson = new JSONObject(feature);
		JSONObject rule = new JSONObject();
		rule.put("ruleString", "isTrue()");
		fJson.put("rule", rule);
		fJson.put("stage", "PRODUCTION");
		featureID = f.addFeature(seasonID, fJson.toString(), "ROOT", sessionToken);
		Assert.assertTrue(featureID.contains("error"), "Test should fail, but instead passed: " + featureID );
	}
	
	//utility dev, feature dev
	@Test(dependsOnMethods = "addFeatureInProdUtilityInDev", description = "utility dev, feature dev")
	public void addFeatureInDevUtilityInDev() throws IOException, JSONException{
		String feature = FileUtils.fileToString(filePath + "feature1.txt", "UTF-8", false);
		JSONObject fJson = new JSONObject(feature);
		JSONObject rule = new JSONObject();
		rule.put("ruleString", "isTrue()");
		fJson.put("rule", rule);
		featureID = f.addFeature(seasonID, fJson.toString(), "ROOT", sessionToken);
		Assert.assertFalse(featureID.contains("error"), "Test should pass, but instead failed: " + featureID );
	}
	
	//utility prod, update feature dev
	@Test(dependsOnMethods = "addFeatureInDevUtilityInDev", description = "utility prod, feature dev")
	public void updateUtilityToProd() throws IOException, JSONException{

		String utility = u.getUtility(utilityID, sessionToken);
		JSONObject uJson = new JSONObject(utility);
		uJson.put("stage", "PRODUCTION");
		String response  =u.updateUtility(utilityID, uJson, sessionToken);
		Assert.assertFalse(response.contains("error"), "Test should pass, but instead failed: " + response );
		
	}

	//utility prod, feature prod
	@Test(dependsOnMethods = "updateUtilityToProd", description = "utility prod, feature prod")
	public void updateFeatureToProdUtilityInProd() throws IOException, JSONException{
		String feature = f.getFeature(featureID, sessionToken);
		JSONObject fJson = new JSONObject(feature);
		fJson.put("stage", "PRODUCTION");
		featureID = f.updateFeature(seasonID, featureID, fJson.toString(), sessionToken);
		Assert.assertFalse(featureID.contains("error"), "Test should pass, but instead failed: " + featureID );
	}
	
	//utility to dev, feature to dev
	@Test(dependsOnMethods = "updateFeatureToProdUtilityInProd", description = "utility in prod, feature to dev")
	public void updateFeatureToDevUtilityInProd() throws IOException, JSONException{
		String feature = f.getFeature(featureID, sessionToken);
		JSONObject fJson = new JSONObject(feature);
		fJson.put("stage", "DEVELOPMENT");
		featureID = f.updateFeature(seasonID, featureID, fJson.toString(), sessionToken);
		Assert.assertFalse(featureID.contains("error"), "Test should pass, but instead failed: " + featureID );
	}
	
	//utility to dev, feature in dev
	@Test(dependsOnMethods = "updateFeatureToDevUtilityInProd", description = "utility in dev, feature in dev")
	public void updateFeatureInDevUtilityToDev() throws IOException, JSONException{
		String utility = u.getUtility(utilityID, sessionToken);
		JSONObject uJson = new JSONObject(utility);
		uJson.put("stage", "DEVELOPMENT");
		String response = u.updateUtility(utilityID, uJson, sessionToken);
		Assert.assertFalse(response.contains("error"), "Test should pass, but instead failed: " + response );
	}

	
	@AfterTest
	private void reset(){
		baseUtils.reset(productID, sessionToken);
	}

}
