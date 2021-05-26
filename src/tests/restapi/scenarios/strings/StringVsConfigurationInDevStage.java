package tests.restapi.scenarios.strings;


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
import tests.restapi.StringsRestApi;

public class StringVsConfigurationInDevStage {
	protected String seasonID;
	protected String stringID;
	protected String filePath;
	protected String featureID;
	protected String configID;
	protected String str;
	protected StringsRestApi t;
	protected ProductsRestApi p;
	protected FeaturesRestApi f;
	private AirlockUtils baseUtils;
	protected String productID;
	protected String m_url;
	private String sessionToken = "";
	private String m_translationsUrl;
	
	@BeforeClass
	@Parameters({"url", "analyticsUrl", "translationsUrl", "configPath", "sessionToken", "userName", "userPassword", "appName", "productsToDeleteFile"})
	public void init(String url, String analyticsUrl, String translationsUrl, String configPath, String sToken, String userName, String userPassword, String appName, String productsToDeleteFile) throws Exception{
		m_url = url;
		m_translationsUrl = translationsUrl;
		filePath = configPath;
		t = new StringsRestApi();
		t.setURL(m_translationsUrl);
		p = new ProductsRestApi();
		p.setURL(m_url);
		f = new FeaturesRestApi();
		f.setURL(url);
		baseUtils = new AirlockUtils(url, analyticsUrl, translationsUrl, configPath, sToken, userName, userPassword, appName, productsToDeleteFile);
		sessionToken = baseUtils.sessionToken;
		productID = baseUtils.createProduct();
		baseUtils.printProductToFile(productID);
		seasonID = baseUtils.createSeason(productID);
	}



	
	@Test (description = "Add string value in dev stage")
	public void addString() throws Exception{
		
		str = FileUtils.fileToString(filePath + "/strings/string1.txt", "UTF-8", false);
		stringID = t.addString(seasonID, str, sessionToken);
	}
	
	//configuration
	@Test (dependsOnMethods="addString",  description = "Add feature with configuration in dev stage")
	public void addFeature() throws Exception{
		String feature = FileUtils.fileToString(filePath + "feature1.txt", "UTF-8", false);
		featureID = f.addFeature(seasonID, feature, "ROOT", sessionToken );
		String configRule = FileUtils.fileToString(filePath + "configuration_rule1.txt", "UTF-8", false);
		JSONObject crJson = new JSONObject(configRule);
		String configuration =  "{ \"text\" :  translate(\"app.hello\", \"testing string\")	}" ;		
		crJson.put("configuration", configuration);
		configID = f.addFeature(seasonID, crJson.toString(), featureID, sessionToken );
		Assert.assertFalse(configID.contains("error"), "Test should pass, but instead failed: " + configID );
	}
	
	@Test (dependsOnMethods="addFeature",  description = "Move configuration to production stage")
	public void updateFeature() throws JSONException, IOException{
		String feature = f.getFeature(featureID, sessionToken);
		JSONObject json = new JSONObject(feature);
		json.put("stage", "PRODUCTION");
		f.updateFeature(seasonID, featureID, json.toString(), sessionToken);
		
		String config = f.getFeature(configID, sessionToken);
		JSONObject crJson = new JSONObject(config);
		crJson.put("stage", "PRODUCTION");
		String response = f.updateFeature(seasonID, configID, crJson.toString(), sessionToken);
		Assert.assertTrue(response.contains("error"), "Test should fail, but instead passed: " + response );
		Assert.assertTrue(response.contains("either missing or in the wrong stage"), "Incorrect error message: " + response );
		
	}
	
	@Test (dependsOnMethods="updateFeature",  description = "Update string stage to production")
	public void updateString() throws Exception{
		str = t.getString(stringID, sessionToken);
		JSONObject json = new JSONObject(str);
		json.put("stage", "PRODUCTION");
		String response = t.updateString(stringID, json.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Test should pass, but instead failed: " + response );
	}
	
	@Test (dependsOnMethods="updateString",  description = "Move configuration to production stage after updating string")
	public void updateFeature2() throws JSONException, IOException{
		String feature = f.getFeature(featureID, sessionToken);
		JSONObject json = new JSONObject(feature);
		json.put("stage", "PRODUCTION");
		f.updateFeature(seasonID, featureID, json.toString(), sessionToken);
	
		String configuration = f.getFeature(configID, sessionToken);
		JSONObject crJson = new JSONObject(configuration);
		crJson.put("stage", "PRODUCTION");
		String response = f.updateFeature(seasonID, configID, crJson.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Test should pass, but instead failed: " + response );
	}
	

	
	@AfterTest
	private void reset(){
		baseUtils.reset(productID, sessionToken);
	}
}
