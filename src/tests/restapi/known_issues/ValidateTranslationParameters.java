//package tests.restapi.scenarios.strings;
package tests.restapi.known_issues;

//known issue : bug #9 Q4-2017 : no validation that format of place holders is legal in translation.

import org.apache.wink.json4j.JSONObject;

import org.testng.Assert;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import tests.com.ibm.qautils.FileUtils;
import tests.restapi.AirlockUtils;
import tests.restapi.FeaturesRestApi;
import tests.restapi.InputSchemaRestApi;
import tests.restapi.ProductsRestApi;
import tests.restapi.StringsRestApi;

public class ValidateTranslationParameters {
	protected String seasonID;
	protected String stringID;
	protected String filePath;
	protected String featureID;
	protected String configID;
	protected String str;
	protected StringsRestApi t;
	protected ProductsRestApi p;
	protected FeaturesRestApi f;
	protected InputSchemaRestApi schema;
	private AirlockUtils baseUtils;
	protected String productID;
	protected String m_url;
	private String sessionToken = "";

	@BeforeClass
	@Parameters({"url", "analyticsUrl", "translationsUrl", "configPath", "sessionToken", "userName", "userPassword", "appName", "productsToDeleteFile"})
	public void init(String url, String analyticsUrl, String translationsUrl, String configPath, String sToken, String userName, String userPassword, String appName, String productsToDeleteFile) throws Exception{
		m_url = url;
		filePath = configPath;
		f = new FeaturesRestApi();
		f.setURL(url);
		t = new StringsRestApi();
		t.setURL(translationsUrl);
		p = new ProductsRestApi();
		p.setURL(m_url);

		baseUtils = new AirlockUtils(url, analyticsUrl, translationsUrl, configPath, sToken, userName, userPassword, appName, productsToDeleteFile);
		sessionToken = baseUtils.sessionToken;
		productID = baseUtils.createProduct();
		baseUtils.printProductToFile(productID);
		seasonID = baseUtils.createSeason(productID);
		
		baseUtils.createSchema(seasonID);
		
		String feature = FileUtils.fileToString(filePath + "feature1.txt", "UTF-8", false);
		featureID = f.addFeature(seasonID, feature, "ROOT", sessionToken );
		//baseUtils.createUtility(seasonID);
	}


	@Test (description = "Add string value in dev stage")
	public void addString() throws Exception{
		
		str = FileUtils.fileToString(filePath + "/strings/string2.txt", "UTF-8", false);
		stringID = t.addString(seasonID, str, sessionToken);
	}


	@Test (dependsOnMethods="addString",  description = "String requires 2 parameters. Use without parameters")
	public void addFeatureNoParams() throws Exception{
		
		String configRule = FileUtils.fileToString(filePath + "configuration_rule1.txt", "UTF-8", false);
		JSONObject crJson = new JSONObject(configRule);
		String configuration =  "{ \"text\" :  translate(\"app.hi\")	}" ;		
		crJson.put("configuration", configuration);
		configID = f.addFeature(seasonID, crJson.toString(), featureID, sessionToken );
		Assert.assertTrue(configID.contains("error"), "Test should fail, but instead passed: " + configID );
	}
	
	@Test (dependsOnMethods="addFeatureNoParams",  description = "String requires 2 parameters. Use with less parameters than requried")
	public void addFeatureLessParams() throws Exception{
		String configRule = FileUtils.fileToString(filePath + "configuration_rule1.txt", "UTF-8", false);
		JSONObject crJson = new JSONObject(configRule);
		String configuration =  "{ \"text\" :  translate(\"app.hi\", context.userPreferences.unitsOfMeasure)	}" ;		
		crJson.put("configuration", configuration);
		
		configID = f.addFeature(seasonID, crJson.toString(), featureID, sessionToken );
		Assert.assertTrue(configID.contains("error"), "Test should fail, but instead passed: " + configID );
	}
	
	
	@Test ( dependsOnMethods="addFeatureLessParams",  description = "String requires 2 parameters. Use with more parameters than requried")
	public void addFeatureMoreParams() throws Exception{
		String configRule = FileUtils.fileToString(filePath + "configuration_rule1.txt", "UTF-8", false);
		JSONObject crJson = new JSONObject(configRule);
		String configuration =  "{ \"text\" :  translate(\"app.hi\", context.userPreferences.unitsOfMeasure, context.viewedLocation.country, context.viewedLocation.region)	}" ;		
		crJson.put("configuration", configuration);
		crJson.put("minAppVersion", "2.0");
		configID = f.addFeature(seasonID, crJson.toString(), featureID, sessionToken );
		Assert.assertFalse(configID.contains("error"), "Test should pass, but instead failed: " + configID );
	}
	
	@Test (description = "Add invalid string placeholder")
	public void addStringWithInvalidPlaceholder() throws Exception{
		
		String strContent = FileUtils.fileToString(filePath + "/strings/string1.txt", "UTF-8", false);
		JSONObject jsonStr = new JSONObject(strContent);
		jsonStr.put("value", "Hello [[[1]");
		String response = t.addString(seasonID, jsonStr.toString(), sessionToken);
		Assert.assertTrue(response.contains("error"), "Incorrect string placeholder accepted");
	}

	
	@AfterTest
	private void reset(){
		baseUtils.reset(productID, sessionToken);
	}
}
