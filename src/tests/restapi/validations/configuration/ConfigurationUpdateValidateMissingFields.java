package tests.restapi.validations.configuration;

import java.io.IOException;


import org.apache.wink.json4j.JSONException;
import org.apache.wink.json4j.JSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import tests.com.ibm.qautils.FileUtils;
import tests.com.ibm.qautils.JSONUtils;
import tests.restapi.AirlockUtils;
import tests.restapi.FeaturesRestApi;
import tests.restapi.ProductsRestApi;

public class ConfigurationUpdateValidateMissingFields {
	protected String seasonID;
	protected String filePath;
	protected ProductsRestApi p;
	protected FeaturesRestApi f;
	private AirlockUtils baseUtils;
	protected String productID;
	protected String m_url;
	private String sessionToken = "";
	protected String featureID;
	protected String configuration;
	protected String configurationID;
	
	
	@BeforeClass
	@Parameters({"url", "analyticsUrl", "translationsUrl", "configPath", "sessionToken", "userName", "userPassword", "appName", "productsToDeleteFile"})
	public void init(String url, String analyticsUrl, String translationsUrl, String configPath, String sToken, String userName, String userPassword, String appName, String productsToDeleteFile) throws Exception{
		m_url = url;
		filePath = configPath;
		f = new FeaturesRestApi();
		f.setURL(m_url);
		p = new ProductsRestApi();
		p.setURL(m_url);
		baseUtils = new AirlockUtils(url, analyticsUrl, translationsUrl, configPath, sToken, userName, userPassword, appName, productsToDeleteFile);
		sessionToken = baseUtils.sessionToken;

		productID = baseUtils.createProduct();
		baseUtils.printProductToFile(productID);
		seasonID = baseUtils.createSeason(productID);
		String feature = FileUtils.fileToString(filePath + "feature1.txt", "UTF-8", false);
		featureID = f.addFeature(seasonID, feature, "ROOT", sessionToken);
		configuration = FileUtils.fileToString(filePath + "configuration_rule1.txt", "UTF-8", false);
		configurationID = f.addFeature(seasonID, configuration, featureID, sessionToken);
	}
	
	@Test 
	public void missingNoCachedResults() throws JSONException, IOException{
		configuration = JSONUtils.generateUniqueString(configuration, 5, "name");
		JSONObject json = new JSONObject(configuration);
		removeKey("noCachedResults", json);
		String response = f.updateFeature(seasonID, configurationID, json.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Test should pass, but instead failed: " + response );

	}
	
	
	@Test 
	public void missingUniqueId() throws JSONException, IOException{
		configuration = f.getFeature(configurationID, sessionToken);
		JSONObject json = new JSONObject(configuration);
		removeKey("uniqueId", json);
		String response = f.updateFeature(seasonID, configurationID, json.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Test should pass, but instead failed: " + response );

	}

	@Test 
	public void missingEnabled() throws JSONException{
		configuration = f.getFeature(configurationID, sessionToken);
		JSONObject json = new JSONObject(configuration);
		removeKey("enabled", json);
		updateFeature(json.toString());
	}

	@Test 
	public void missingType() throws JSONException{
		configuration = f.getFeature(configurationID, sessionToken);
		JSONObject json = new JSONObject(configuration);
		removeKey("type", json);
		updateFeature(json.toString());
	}
	
	@Test 
	public void missingStage() throws JSONException{
		configuration = f.getFeature(configurationID, sessionToken);
		JSONObject json = new JSONObject(configuration);
		removeKey("stage", json);
		updateFeature(json.toString());
	}
	
	
	@Test 
	public void missingNamespace() throws JSONException{
		configuration = f.getFeature(configurationID, sessionToken);
		JSONObject json = new JSONObject(configuration);
		removeKey("namespace", json);
		updateFeature(json.toString());
	}
	
	@Test
	public void missingCreator() throws JSONException{
		configuration = f.getFeature(configurationID, sessionToken);
		JSONObject json = new JSONObject(configuration);
		removeKey("creator", json);
		updateFeature(json.toString());
	}
	
	@Test 
	public void missingOwner() throws JSONException, IOException{
		configuration = f.getFeature(configurationID, sessionToken);
		JSONObject json = new JSONObject(configuration);
		removeKey("owner", json);
		String response = f.updateFeature(seasonID, configurationID, json.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Test should pass, but instead failed: " + response );

	}
	
	@Test
	public void missingDescription() throws JSONException, IOException{
		configuration = f.getFeature(configurationID, sessionToken);
		JSONObject json = new JSONObject(configuration);
		removeKey("description", json);
		String response = f.updateFeature(seasonID, configurationID, json.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Test should pass, but instead failed: " + response );

	}
	
	@Test 
	public void missingRule() throws JSONException{
		configuration = f.getFeature(configurationID, sessionToken);
		JSONObject json = new JSONObject(configuration);
		removeKey("rule", json);
		updateFeature(json.toString());
	}
	@Test 
	public void missingRuleString() throws JSONException, IOException{
		configuration = f.getFeature(configurationID, sessionToken);
		JSONObject json = new JSONObject(configuration);
		JSONObject ruleString = new JSONObject();
		json.put("rule", ruleString);
		String response = f.updateFeature(seasonID, configurationID, json.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Test should pass, but instead failed: " + response );

	}
	@Test
	public void missingMinAppVersion() throws JSONException{
		configuration = JSONUtils.generateUniqueString(configuration, 5, "name");
		JSONObject json = new JSONObject(configuration);
		removeKey("minAppVersion", json);
		updateFeature(json.toString());
	}
	@Test 
	public void missingName() throws JSONException{
		configuration = f.getFeature(configurationID, sessionToken);
		JSONObject json = new JSONObject(configuration);
		removeKey("name", json);
		updateFeature(json.toString());
	}

	@Test 
	public void missingFeature() throws JSONException, IOException{
		configuration = f.getFeature(configurationID, sessionToken);
		JSONObject json = new JSONObject(configuration);
		removeKey("features", json);
		String response = f.updateFeature(seasonID, configurationID, json.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Test should pass, but instead failed: " + response );

	}


	@Test 
	public void missingInternalUserGroups() throws JSONException, IOException{
		configuration = f.getFeature(configurationID, sessionToken);
		JSONObject json = new JSONObject(configuration);
		removeKey("internalUserGroups", json);
		String response = f.updateFeature(seasonID, configurationID, json.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Test should pass, but instead failed: " + response );

	}
	
	
	@Test 
	public void missingRolloutPercentage() throws JSONException{
		configuration = f.getFeature(configurationID, sessionToken);
		JSONObject json = new JSONObject(configuration);
		removeKey("rolloutPercentage", json);
		updateFeature(json.toString());
	}
	
/*	@Test 
	public void missingRolloutPercentageBitmap() throws JSONException{
		configuration = f.getFeature(configurationID, sessionToken);
		JSONObject json = new JSONObject(configuration);
		removeKey("rolloutPercentageBitmap", json);
		updateFeature(json.toString());
	}
	*/
	
	@Test 
	public void missingLastModified() throws JSONException{
		configuration = f.getFeature(configurationID, sessionToken);
		JSONObject json = new JSONObject(configuration);
		removeKey("lastModified", json);
		updateFeature(json.toString());
	}
	
	@Test 
	public void missingConfiguration() throws JSONException{
		configuration = f.getFeature(configurationID, sessionToken);
		JSONObject json = new JSONObject(configuration);
		removeKey("configuration", json);
		updateFeature(json.toString());
	}

	

	private void updateFeature(String configJson){

		try {
			String response = f.updateFeature(seasonID, configurationID, configJson, sessionToken);
			Assert.assertTrue(response.contains("error"), "Test should fail, but instead passed: " + response );
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	

	}
	
	
	private void removeKey(String key, JSONObject json){
		json.remove(key);
	}
	

	@AfterTest
	private void reset(){
		baseUtils.reset(productID, sessionToken);
	}
}
