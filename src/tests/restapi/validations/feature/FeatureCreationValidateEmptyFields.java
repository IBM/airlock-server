package tests.restapi.validations.feature;

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

public class FeatureCreationValidateEmptyFields {
	protected String seasonID;
	protected String featureID;
	protected String filePath;
	protected String m_url;
	protected String feature;
	protected FeaturesRestApi f;
	protected ProductsRestApi p;
	private AirlockUtils baseUtils;
	protected String productID;
	private String sessionToken = "";
	
	@BeforeClass
	@Parameters({"url", "analyticsUrl", "translationsUrl", "configPath", "sessionToken", "userName", "userPassword", "appName", "productsToDeleteFile"})
	public void init(String url, String analyticsUrl, String translationsUrl, String configPath, String sToken, String userName, String userPassword, String appName, String productsToDeleteFile) throws Exception{
		filePath = configPath;
		p = new ProductsRestApi();
		p.setURL(url);
		baseUtils = new AirlockUtils(url, analyticsUrl, translationsUrl, configPath, sToken, userName, userPassword, appName, productsToDeleteFile);
		sessionToken = baseUtils.sessionToken;
		productID = baseUtils.createProduct();
		baseUtils.printProductToFile(productID);
		seasonID = baseUtils.createSeason(productID);

		m_url = url;
		f = new FeaturesRestApi();
		f.setURL(url);
		feature = FileUtils.fileToString(filePath + "feature1.txt", "UTF-8", false);
	}
	
	@Test
	public void missingNoCachedResults() throws JSONException{
		feature = JSONUtils.generateUniqueString(feature, 5, "name");
		JSONObject json = new JSONObject(feature);
		json.put("noCachedResults", "");
		addFeature(json.toString());
	}
	
	@Test
	public void missingUniqueId() throws JSONException{
		feature = JSONUtils.generateUniqueString(feature, 5, "name");
		JSONObject json = new JSONObject(feature);
		json.put("uniqueId", "");
		addFeature(json.toString());
	}
	
	@Test 
		public void missingEnabled() throws JSONException{
		feature = JSONUtils.generateUniqueString(feature, 5, "name");
		JSONObject json = new JSONObject(feature);
		json.put("enabled", "");
		addFeature(json.toString());
	}
	
	@Test 
	public void missingForce() throws JSONException{
		feature = JSONUtils.generateUniqueString(feature, 5, "name");
		JSONObject json = new JSONObject(feature);
		JSONObject rule = new JSONObject();
		rule.put("ruleString", "");
		rule.put("force", "");
		json.put("rule", rule);
		addFeature(json.toString());
	}
	
	@Test
	public void missingType() throws JSONException{
		feature = JSONUtils.generateUniqueString(feature, 5, "name");
		JSONObject json = new JSONObject(feature);
		json.put("type", "");
		addFeature(json.toString());
	}
	
	@Test
	public void missingStage() throws JSONException{
		feature = JSONUtils.generateUniqueString(feature, 5, "name");
		JSONObject json = new JSONObject(feature);
		json.put("stage", "");
		addFeature(json.toString());
	}
	
	@Test
	public void missingAdditionalInfo() throws JSONException{
		feature = JSONUtils.generateUniqueString(feature, 5, "name");
		JSONObject json = new JSONObject(feature);
		json.put("additionalInfo", "");
		addFeature(json.toString());
	}
	
	
	@Test 
	public void missingNamespace() throws JSONException{
		feature = JSONUtils.generateUniqueString(feature, 5, "name");
		JSONObject json = new JSONObject(feature);
		json.put("namespace", "");
		addFeature(json.toString());
	}
	
	@Test
	public void missingCreator() throws JSONException{
		feature = JSONUtils.generateUniqueString(feature, 5, "name");
		JSONObject json = new JSONObject(feature);
		json.put("creator", "");
		addFeature(json.toString());
	}	
	
	@Test
	public void missingDescription() throws JSONException, IOException{
		feature = JSONUtils.generateUniqueString(feature, 5, "name");
		JSONObject json = new JSONObject(feature);
		json.put("description", "");
		String response = f.addFeature(seasonID, json.toString(), "ROOT", sessionToken);
		Assert.assertFalse(response.contains("error"), "Test should pass, but instead failed: " + response );

	}
	
	@Test
	public void missingDisplayName() throws JSONException, IOException{
		feature = JSONUtils.generateUniqueString(feature, 5, "name");
		JSONObject json = new JSONObject(feature);
		json.put("displayName", "");
		String response = f.addFeature(seasonID, json.toString(), "ROOT", sessionToken);
		Assert.assertFalse(response.contains("error"), "Test should pass, but instead failed: " + response );

	}
	
	
	@Test
	public void missingRule() throws JSONException{
		feature = JSONUtils.generateUniqueString(feature, 5, "name");
		JSONObject json = new JSONObject(feature);
		json.put("rule", "");
		addFeature(json.toString());
	}
	@Test 
	public void missingRuleString() throws JSONException, IOException{
		feature = JSONUtils.generateUniqueString(feature, 5, "name");
		JSONObject json = new JSONObject(feature);
		JSONObject ruleString = new JSONObject();
		ruleString.put("ruleString", "");
		json.put("rule", ruleString);
		String response = f.addFeature(seasonID, json.toString(), "ROOT", sessionToken);
		Assert.assertFalse(response.contains("error"), "Test should pass, but instead failed: " + response );

	}
	@Test 
	public void missingMinAppVersion() throws JSONException{
		feature = JSONUtils.generateUniqueString(feature, 5, "name");
		JSONObject json = new JSONObject(feature);
		json.put("minAppVersion", "");
		addFeature(json.toString());
	}
	@Test 
	public void missingName() throws JSONException{
		feature = JSONUtils.generateUniqueString(feature, 5, "name");
		JSONObject json = new JSONObject(feature);
		json.put("name", "");
		addFeature(json.toString());
	}

	@Test
	public void missingFeature() throws JSONException{
		feature = JSONUtils.generateUniqueString(feature, 5, "name");
		JSONObject json = new JSONObject(feature);
		json.put("features", "");
		addFeature(json.toString());
	}
	@Test 
	public void missingOwner() throws JSONException, IOException{
		feature = JSONUtils.generateUniqueString(feature, 5, "name");
		JSONObject json = new JSONObject(feature);
		json.put("owner", "");
		String response = f.addFeature(seasonID, json.toString(), "ROOT", sessionToken);
		Assert.assertFalse(response.contains("error"), "Test should pass, but instead failed: " + response );

	}
	@Test
	public void missingDefaultIfAirlockSystemIsDown() throws JSONException{
		feature = JSONUtils.generateUniqueString(feature, 5, "name");
		JSONObject json = new JSONObject(feature);
		json.put("defaultIfAirlockSystemIsDown", "");
		addFeature(json.toString());
	}
	@Test
	public void missingInternalUserGroups() throws JSONException{
		feature = JSONUtils.generateUniqueString(feature, 5, "name");
		JSONObject json = new JSONObject(feature);
		json.put("internalUserGroups", "");
		addFeature(json.toString());
	}
	
	
	@Test
	public void missingRolloutPercentage() throws JSONException{
		feature = JSONUtils.generateUniqueString(feature, 5, "name");
		JSONObject json = new JSONObject(feature);
		json.put("rolloutPercentage", "");
		addFeature(json.toString());
	}
	
/*	@Test
	public void missingRolloutPercentageBitmap() throws JSONException{
		feature = JSONUtils.generateUniqueString(feature, 5, "name");
		JSONObject json = new JSONObject(feature);
		json.put("rolloutPercentageBitmap", "");
		addFeature(json.toString());
	}
	*/
	
	@Test
	public void missingDefaultConfiguration() throws JSONException{
		feature = JSONUtils.generateUniqueString(feature, 5, "name");
		JSONObject json = new JSONObject(feature);
		json.put("defaultConfiguration", "");
		addFeature(json.toString());
	}
	
	@Test 
	public void missingConfigurationSchema() throws JSONException{
		feature = JSONUtils.generateUniqueString(feature, 5, "name");
		JSONObject json = new JSONObject(feature);
		json.put("configurationSchema", "");
		addFeature(json.toString());
	}
	
	
	@Test 
	public void missingDefaultConfigurationObject() throws JSONException, IOException{
		feature = JSONUtils.generateUniqueString(feature, 5, "name");
		JSONObject json = new JSONObject(feature);
		JSONObject defaultConfiguration = new JSONObject();
		json.put("defaultConfiguration", defaultConfiguration);
		String response = f.addFeature(seasonID, json.toString(), "ROOT", sessionToken);
		Assert.assertFalse(response.contains("error"), "Test should pass, but instead failed: " + response );

	}
	
	@Test 
	public void missingConfigurationSchemaObject() throws JSONException, IOException{
		feature = JSONUtils.generateUniqueString(feature, 5, "name");
		JSONObject json = new JSONObject(feature);
		JSONObject configurationSchema = new JSONObject();
		json.put("configurationSchema", configurationSchema);
		JSONObject defaultConfig = new JSONObject();
		json.put("defaultConfiguration", defaultConfig);
		String response = f.addFeature(seasonID, json.toString(), "ROOT", sessionToken);
		Assert.assertFalse(response.contains("error"), "Test should pass, but instead failed: " + response );

	}

	private void addFeature(String featureJson){

		try {
			String response = f.addFeature(seasonID, featureJson, "ROOT", sessionToken);
			Assert.assertTrue(response.contains("error"), "Test should fail, but instead passed: " + response );

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	

	}
	
	@AfterTest
	public void reset(){
		baseUtils.reset(productID, sessionToken);
	}

	

}
