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
import tests.restapi.StringsRestApi;
import tests.restapi.UtilitiesRestApi;

public class UseStringsInUtility {
	protected String seasonID;
	protected String featureID;
	protected String productID;
	protected String stringID;
	protected String configID;
	protected String configID2;
	protected String  utilityID;
	protected String deepFreezeID;
	protected String filePath;
	protected FeaturesRestApi f;
	protected ProductsRestApi p;
	protected UtilitiesRestApi u;
	protected StringsRestApi t;
	private String sessionToken = "";	
	protected AirlockUtils baseUtils;
	
	
	protected UtilitiesRestApi utilitiesApi;
	
	
	@BeforeClass
	@Parameters({"url", "analyticsUrl", "translationsUrl", "configPath", "sessionToken", "userName", "userPassword", "appName", "productsToDeleteFile"})
	public void init(String url, String analyticsUrl, String translationsUrl, String configPath, String sToken, String userName, String userPassword, String appName, String productsToDeleteFile) throws Exception{
		filePath = configPath;
		baseUtils = new AirlockUtils(url, analyticsUrl, translationsUrl, configPath, sToken, userName, userPassword, appName, productsToDeleteFile);
		sessionToken = baseUtils.sessionToken;

		p = new ProductsRestApi();
		p.setURL(url);
		u = new UtilitiesRestApi();
		u.setURL(url);
		
		f = new FeaturesRestApi();
		f.setURL(url);
		
		t = new StringsRestApi();
		t.setURL(translationsUrl);

		productID = baseUtils.createProduct();
		baseUtils.printProductToFile(productID);
		seasonID = baseUtils.createSeason(productID);
		
		utilitiesApi = new UtilitiesRestApi();
		utilitiesApi.setURL(url);
		
		String feature = FileUtils.fileToString(filePath + "feature1.txt", "UTF-8", false);
		JSONObject json = new JSONObject(feature);
		json.put("stage", "PRODUCTION");
		featureID = f.addFeature(seasonID, json.toString(), "ROOT", sessionToken);		
		baseUtils.createUtility(seasonID);
	}
	
	@Test(description = "Add utility that uses a nonexistent string")
	public void addUtility() throws IOException, JSONException{		
		String utility = FileUtils.fileToString(filePath + "utilities" + File.separator + "utility1.txt", "UTF-8", false);
		Properties utilProps = new Properties();
		utilProps.load(new StringReader(utility));
		utilProps.setProperty("utility", "function A(placeholder){translate (\"k1\", placeholder);}");
		utilProps.setProperty("stage", "PRODUCTION");
		
		utilityID = u.addUtility(seasonID, utilProps, sessionToken);
		Assert.assertFalse(utilityID.contains("error"), "Test should pass, but instead failed: " + utilityID );
	}
	
	@Test(dependsOnMethods = "addUtility",  description = "Add configuration that uses the utility tha uses a nonexisent string.")
	public void addConfigUsingUtilWithMissingString() throws IOException, JSONException{
		
		String configuration = FileUtils.fileToString(filePath + "configuration_rule1.txt", "UTF-8", false);
		
		JSONObject json = new JSONObject(configuration);
		json.put("configuration", "{\"text\": A()}");
		json.put("stage", "PRODUCTION");
		
		configID = f.addFeature(seasonID, json.toString(), featureID, sessionToken );
		Assert.assertTrue(configID.contains("error"), "Test should fail, but instead passed: " + configID );
	}
	
	@Test(dependsOnMethods = "addConfigUsingUtilWithMissingString",  description = "Add configuration that uses the utility after adding the missing string but wrong parameters number")
	public void addConfigUsingUtilWithExistingStringWrongParamsNumber() throws Exception{
		String str = FileUtils.fileToString(filePath + "strings/string1.txt", "UTF-8", false);
		
		JSONObject strJson = new JSONObject(str);
		strJson.put("key", "k1");
		strJson.put("stage", "PRODUCTION");
		
		stringID = t.addString(seasonID, strJson.toString(), sessionToken);
		
		String configuration = FileUtils.fileToString(filePath + "configuration_rule1.txt", "UTF-8", false);
		
		JSONObject configJson = new JSONObject(configuration);
		configJson.put("configuration", "{\"text\": A()}");
		configJson.put("stage", "PRODUCTION");
		
		configID2 = f.addFeature(seasonID, configJson.toString(), featureID, sessionToken );	
		Assert.assertFalse(configID2.contains("error"), "Test should pass, but instead failed: " + configID2 );
	}
	
	@Test(dependsOnMethods = "addConfigUsingUtilWithExistingStringWrongParamsNumber",  description = "Add configuration that uses the utility after adding the missing string but right parameters number")
	public void addConfigUsingUtilWithExistingStringRightParamsNumber() throws Exception{
		String str = t.getString(stringID, sessionToken);
		JSONObject jsonstr = new JSONObject(str);
		jsonstr.put("value", "v2");	
		t.updateString(stringID, jsonstr.toString(), sessionToken);	
		
		String configuration = FileUtils.fileToString(filePath + "configuration_rule2.txt", "UTF-8", false);
		
		JSONObject configJson = new JSONObject(configuration);
		configJson.put("configuration", "{\"text\": A(\"text\")}");
		configJson.put("stage", "PRODUCTION");	
		configID = f.addFeature(seasonID, configJson.toString(), featureID, sessionToken );	
		Assert.assertFalse(configID.contains("error"), configID );
	}
	
	//minAppVersion is removed from strings
	/*
	@Test(dependsOnMethods = "addConfigUsingUtilWithExistingStringRightParamsNumner",  description = "Change string version to higher version than the config's version")
	public void changeStringToHigerVerWhenInuseByConfig() throws Exception{
		String str = t.getString(stringID, sessionToken);
		JSONObject jsonstr = new JSONObject(str);
		jsonstr.put("minAppVersion", "2.0");		
		String response = t.updateString(stringID, jsonstr.toString(), sessionToken);	
		Assert.assertTrue(response.contains("error"), "Test should fail, but instead passed: " + response );
	}*/
	
	@Test(dependsOnMethods = "addConfigUsingUtilWithExistingStringRightParamsNumber",  description = "Change string version to lower version than the config's version")
	public void changeStringVerToLowerWhenInuseByConfig() throws Exception{
		String str = t.getString(stringID, sessionToken);
		JSONObject jsonstr = new JSONObject(str);
		//jsonstr.put("minAppVersion", "0.9");		
		String response = t.updateString(stringID, jsonstr.toString(), sessionToken);	
		Assert.assertFalse(response.contains("error"), "Test should pass, but instead failed: " + configID );
	}
	
	@Test(dependsOnMethods = "changeStringVerToLowerWhenInuseByConfig",  description = "Change string stage to DEVELOPMENT when config is in PRODUCTION")
	public void changeStringToDevWhenInuseByConfig() throws Exception{
		String str = t.getString(stringID, sessionToken);
		JSONObject jsonstr = new JSONObject(str);
		jsonstr.put("stage", "DEVELOPMENT");		
		String response = t.updateString(stringID, jsonstr.toString(), sessionToken);	
		Assert.assertTrue(response.contains("error"), "Test should fail, but instead passed: " + response );
	}
	
	
	@Test(dependsOnMethods = "changeStringToDevWhenInuseByConfig",  description = "Delete string that is in use by confiuration")
	public void moveUtilStringAndConfigToDev() throws Exception{
		String configStr = f.getFeature(configID, sessionToken);
		JSONObject configJson = new JSONObject(configStr);
		configJson.put("stage", "DEVELOPMENT");	
		String response = f.updateFeature(seasonID, configID, configJson.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Test should pass, but instead failed: " + response );
		
		configStr = f.getFeature(configID2, sessionToken);
		configJson = new JSONObject(configStr);
		configJson.put("stage", "DEVELOPMENT");	
		response = f.updateFeature(seasonID, configID2, configJson.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Test should pass, but instead failed: " + response );		
		
		String utilStr = u.getUtility(utilityID, sessionToken);
		JSONObject utilJson = new JSONObject(utilStr);
		utilJson.put("stage", "DEVELOPMENT");	
		response = u.updateUtility(utilityID, utilJson, sessionToken);
		Assert.assertFalse(response.contains("error"), "Test should pass, but instead failed: " + response );
		
		String str = t.getString(stringID, sessionToken);
		JSONObject jsonstr = new JSONObject(str);
		jsonstr.put("stage", "DEVELOPMENT");		
		response = t.updateString(stringID, jsonstr.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Test should pass, but instead failed: " + response );
	}
	
	@Test(dependsOnMethods = "moveUtilStringAndConfigToDev",  description = "Delete string that is in use by confiuration")
	public void deleteStringInuseByConfig() throws Exception{
		int responseCode = t.deleteString(stringID, sessionToken);
		Assert.assertEquals(responseCode, 400, "String in use by config was deleted");
	}
	
	@Test(dependsOnMethods = "deleteStringInuseByConfig",  description = "Delete string that is not  in use by configuration")
	public void deleteStringNotInuseByConfig() throws Exception{
		int responseCode = f.deleteFeature(configID, sessionToken);
		Assert.assertEquals(responseCode, 200, "feature was not deleted");
		
		 responseCode = f.deleteFeature(configID2, sessionToken);
		Assert.assertEquals(responseCode, 200, "feature was not deleted");
		responseCode = t.deleteString(stringID, sessionToken);	
		Assert.assertEquals(responseCode, 200, "String not in use by config cannot be deleted");
	}
	
	@AfterTest
	private void reset(){
		baseUtils.reset(productID, sessionToken);
	}
}
