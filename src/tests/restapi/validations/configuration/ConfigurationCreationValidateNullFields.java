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

public class ConfigurationCreationValidateNullFields {
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
	}
	
	@Test 
	public void missingNoCachedResults() throws JSONException, IOException{
		configuration = JSONUtils.generateUniqueString(configuration, 5, "name");
		JSONObject json = new JSONObject(configuration);
		json.put("noCachedResults", JSONObject.NULL);
		String response = f.addFeature(seasonID, json.toString(), featureID, sessionToken);
		Assert.assertFalse(response.contains("error"), "Test should pass, but instead failed: " + response );

	}

	@Test 
	public void missingUniqueId() throws JSONException, IOException{
		configuration = JSONUtils.generateUniqueString(configuration, 5, "name");
		JSONObject json = new JSONObject(configuration);
		json.put("uniqueId", JSONObject.NULL);
		String response = f.addFeature(seasonID, json.toString(), featureID, sessionToken);
		Assert.assertFalse(response.contains("error"), "Test should pass, but instead failed: " + response );

	}
	
	@Test
		public void missingEnabled() throws JSONException{
		configuration = JSONUtils.generateUniqueString(configuration, 5, "name");
		JSONObject json = new JSONObject(configuration);
		json.put("enabled", JSONObject.NULL);
		addFeature(json.toString());
	}
	
	@Test
	public void missingType() throws JSONException{
		configuration = JSONUtils.generateUniqueString(configuration, 5, "name");
		JSONObject json = new JSONObject(configuration);
		json.put("type", JSONObject.NULL);
		addFeature(json.toString());
	}
	
	@Test
	public void missingStage() throws JSONException{
		configuration = JSONUtils.generateUniqueString(configuration, 5, "name");
		JSONObject json = new JSONObject(configuration);
		json.put("stage", JSONObject.NULL);
		addFeature(json.toString());
	}
	
	
	
	@Test
	public void missingNamespace() throws JSONException{
		configuration = JSONUtils.generateUniqueString(configuration, 5, "name");
		JSONObject json = new JSONObject(configuration);
		json.put("namespace", JSONObject.NULL);
		addFeature(json.toString());
	}
	
	@Test
	public void missingCreator() throws JSONException{
		configuration = JSONUtils.generateUniqueString(configuration, 5, "name");
		JSONObject json = new JSONObject(configuration);
		json.put("creator", JSONObject.NULL);
		addFeature(json.toString());
	}
	
	@Test
	public void missingOwner() throws JSONException, IOException{
		configuration = JSONUtils.generateUniqueString(configuration, 5, "name");
		JSONObject json = new JSONObject(configuration);
		json.put("owner", JSONObject.NULL);
		String response = f.addFeature(seasonID, json.toString(), featureID, sessionToken);
		Assert.assertFalse(response.contains("error"), "Test should pass, but instead failed: " + response );

	}
	
	@Test
	public void missingDescription() throws JSONException, IOException{
		configuration = JSONUtils.generateUniqueString(configuration, 5, "name");
		JSONObject json = new JSONObject(configuration);
		json.put("description", JSONObject.NULL);
		String response = f.addFeature(seasonID, json.toString(), featureID, sessionToken);
		Assert.assertFalse(response.contains("error"), "Test should pass, but instead failed: " + response );

	}
	
	@Test
	public void missingRule() throws JSONException{
		configuration = JSONUtils.generateUniqueString(configuration, 5, "name");
		JSONObject json = new JSONObject(configuration);
		json.put("rule", JSONObject.NULL);
		addFeature(json.toString());
	}

	
	@Test
	public void missingMinAppVersion() throws JSONException{
		configuration = JSONUtils.generateUniqueString(configuration, 5, "name");
		JSONObject json = new JSONObject(configuration);
		json.put("minAppVersion", JSONObject.NULL);
		addFeature(json.toString());
	}
	@Test
	public void missingName() throws JSONException{
		configuration = JSONUtils.generateUniqueString(configuration, 5, "name");
		JSONObject json = new JSONObject(configuration);
		json.put("name", JSONObject.NULL);
		addFeature(json.toString());
	}
	@Test
	public void missingSeasonId() throws JSONException, IOException{
		configuration = JSONUtils.generateUniqueString(configuration, 5, "name");
		JSONObject json = new JSONObject(configuration);
		json.put("seasonId", JSONObject.NULL);
		String response = f.addFeature(seasonID, json.toString(), featureID, sessionToken);
		Assert.assertFalse(response.contains("error"), "Test should pass, but instead failed: " + response );

	}
	@Test
	public void missingFeature() throws JSONException, IOException{
		configuration = JSONUtils.generateUniqueString(configuration, 5, "name");
		JSONObject json = new JSONObject(configuration);
		json.put("features", JSONObject.NULL);
		String response = f.addFeature(seasonID, json.toString(), featureID, sessionToken);
		Assert.assertFalse(response.contains("error"), "Test should pass, but instead failed: " + response );

	}


	@Test
	public void missingInternalUserGroups() throws JSONException, IOException{
		configuration = JSONUtils.generateUniqueString(configuration, 5, "name");
		JSONObject json = new JSONObject(configuration);
		json.put("internalUserGroups", JSONObject.NULL);
		String response = f.addFeature(seasonID, json.toString(), featureID, sessionToken);
		Assert.assertFalse(response.contains("error"), "Test should pass, but instead failed: " + response );

	}
	
	
	@Test
	public void missingRolloutPercentage() throws JSONException{
		configuration = JSONUtils.generateUniqueString(configuration, 5, "name");
		JSONObject json = new JSONObject(configuration);
		json.put("rolloutPercentage", JSONObject.NULL);
		addFeature(json.toString());
	}
	
	@Test
	public void missingConfiguration() throws JSONException{
		configuration = JSONUtils.generateUniqueString(configuration, 5, "name");
		JSONObject json = new JSONObject(configuration);
		json.put("configuration", JSONObject.NULL);
		addFeature(json.toString());
	}
	


	private void addFeature(String featureJson){

		try {
			String response = f.addFeature(seasonID, featureJson, featureID, sessionToken);
			Assert.assertTrue(response.contains("error"), "Test should fail, but instead passed: " + response );
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	

	

	@AfterTest
	private void reset(){
		baseUtils.reset(productID, sessionToken);
	}
}
