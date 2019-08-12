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

public class FeatureUpdateValidateNullFields {
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
		String feature = FileUtils.fileToString(filePath + "feature1.txt", "UTF-8", false);
		f = new FeaturesRestApi();
		f.setURL(url);
		feature = JSONUtils.generateUniqueString(feature, 5, "name");
		featureID = f.addFeature(seasonID, feature, "ROOT", sessionToken);
		
	}
	
	
	@Test 
	public void missingNoCachedResults() throws JSONException, IOException{
		feature = JSONUtils.generateUniqueString(feature, 5, "name");
		JSONObject json = new JSONObject(feature);
		json.put("noCachedResults", JSONObject.NULL);
		String response = f.updateFeature(seasonID, featureID, json.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Test should pass, but instead failed: " + response );

	}
	
	@Test 
	public void missingUniqueId() throws JSONException, IOException{
		feature = f.getFeature(featureID, sessionToken);
		JSONObject json = new JSONObject(feature);
		json.put("uniqueId", JSONObject.NULL);
		String response = f.updateFeature(seasonID, featureID, json.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Test should pass, but instead failed: " + response );

	}
	
	@Test 
		public void missingEnabled() throws JSONException{
		feature = f.getFeature(featureID, sessionToken);
		JSONObject json = new JSONObject(feature);
		json.put("enabled", JSONObject.NULL);
		updateFeature(json.toString());
	}

	@Test 
	public void missingForce() throws JSONException, IOException{
		feature = f.getFeature(featureID, sessionToken);
		JSONObject json = new JSONObject(feature);
		
		JSONObject rule = new JSONObject();
		rule.put("ruleString", "");
		rule.put("force", JSONObject.NULL);
		json.put("rule", rule);
		
		String response = f.updateFeature(seasonID, featureID, json.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Test should pass, but instead failed: " + response );

	}
	
	@Test 
	public void missingType() throws JSONException{
		feature = f.getFeature(featureID, sessionToken);
		JSONObject json = new JSONObject(feature);
		json.put("type", JSONObject.NULL);
		updateFeature(json.toString());
	}
	
	@Test 
	public void missingStage() throws JSONException{
		feature = f.getFeature(featureID, sessionToken);
		JSONObject json = new JSONObject(feature);
		json.put("stage", JSONObject.NULL);
		updateFeature(json.toString());
	}
	
	@Test
	public void missingAdditionalInfo() throws JSONException, IOException{
		feature = f.getFeature(featureID, sessionToken);
		JSONObject json = new JSONObject(feature);
		json.put("additionalInfo", JSONObject.NULL);
		String response = f.updateFeature(seasonID, featureID, json.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Test should pass, but instead failed: " + response );

	}
	
	
	@Test 
	public void missingNamespace() throws JSONException{
		feature = f.getFeature(featureID, sessionToken);
		JSONObject json = new JSONObject(feature);
		json.put("namespace", JSONObject.NULL);
		updateFeature(json.toString());
	}
	
	@Test 
	public void missingCreator() throws JSONException{
		feature = f.getFeature(featureID, sessionToken);
		JSONObject json = new JSONObject(feature);
		json.put("creator", JSONObject.NULL);
		updateFeature(json.toString());
	}	
	
	@Test
	public void missingDescription() throws JSONException, IOException{
		feature = f.getFeature(featureID, sessionToken);
		JSONObject json = new JSONObject(feature);
		json.put("description", JSONObject.NULL);
		String response = f.updateFeature(seasonID, featureID, json.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Test should pass, but instead failed: " + response );

	}
	
	@Test
	public void missingDisplayName() throws JSONException, IOException{
		feature = f.getFeature(featureID, sessionToken);
		JSONObject json = new JSONObject(feature);
		json.put("displayName", JSONObject.NULL);
		String response = f.updateFeature(seasonID, featureID, json.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Test should pass, but instead failed: " + response );

	}
	
	@Test 
	public void missingRule() throws JSONException{
		feature = f.getFeature(featureID, sessionToken);
		JSONObject json = new JSONObject(feature);
		json.put("rule", JSONObject.NULL);
		updateFeature(json.toString());
	}
	@Test 
	public void missingRuleString() throws JSONException, IOException{
		feature = f.getFeature(featureID, sessionToken);
		JSONObject json = new JSONObject(feature);
		JSONObject ruleString = new JSONObject();
		ruleString.put("ruleString", JSONObject.NULL);
		json.put("rule", ruleString);
		String response = f.updateFeature(seasonID, featureID, json.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Test should pass, but instead failed: " + response );

	}
	@Test 
	public void missingMinAppVersion() throws JSONException{
		feature = f.getFeature(featureID, sessionToken);
		JSONObject json = new JSONObject(feature);
		json.put("minAppVersion", JSONObject.NULL);
		updateFeature(json.toString());
	}
	@Test 
	public void missingName() throws JSONException{
		feature = f.getFeature(featureID, sessionToken);
		JSONObject json = new JSONObject(feature);
		json.put("name", JSONObject.NULL);
		updateFeature(json.toString());
	}

	@Test 
	public void missingFeature() throws JSONException{
		feature = f.getFeature(featureID, sessionToken);
		JSONObject json = new JSONObject(feature);
		json.put("features", JSONObject.NULL);
		updateFeature(json.toString());
	}
	@Test
	public void missingOwner() throws JSONException, IOException{
		feature = f.getFeature(featureID, sessionToken);
		JSONObject json = new JSONObject(feature);
		json.put("owner", JSONObject.NULL);
		String response = f.updateFeature(seasonID, featureID, json.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Test should pass, but instead failed: " + response );
	}
	@Test 
	public void missingDefaultIfAirlockSystemIsDown() throws JSONException{
		feature = f.getFeature(featureID, sessionToken);
		JSONObject json = new JSONObject(feature);
		json.put("defaultIfAirlockSystemIsDown", JSONObject.NULL);
		updateFeature(json.toString());
	}
	@Test 
	public void missingInternalUserGroups() throws JSONException, IOException{
		feature = f.getFeature(featureID, sessionToken);
		JSONObject json = new JSONObject(feature);
		json.put("internalUserGroups", JSONObject.NULL);
		String response = f.updateFeature(seasonID, featureID, json.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Test should pass, but instead failed: " + response );

	}
	
	
	@Test 
	public void missingRolloutPercentage() throws JSONException{
		feature = f.getFeature(featureID, sessionToken);
		JSONObject json = new JSONObject(feature);
		json.put("rolloutPercentage", JSONObject.NULL);
		updateFeature(json.toString());
	}

/*	@Test 
	public void missingRolloutPercentageBitmap() throws JSONException{
		feature = f.getFeature(featureID, sessionToken);
		JSONObject json = new JSONObject(feature);
		json.put("rolloutPercentageBitmap", JSONObject.NULL);
		updateFeature(json.toString());
	}
*/	
	
	@Test 
	public void missingDefaultConfiguration() throws JSONException, IOException{
		feature = f.getFeature(featureID, sessionToken);
		JSONObject json = new JSONObject(feature);
		json.put("defaultConfiguration", JSONObject.NULL);
		String response = f.updateFeature(seasonID, featureID, json.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Test should pass, but instead failed: " + response );

	}
	
	@Test 
	public void missingConfigurationSchema() throws JSONException, IOException{
		feature = f.getFeature(featureID, sessionToken);
		JSONObject json = new JSONObject(feature);
		json.put("configurationSchema", JSONObject.NULL);
		String response = f.updateFeature(seasonID, featureID, json.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Test should pass, but instead failed: " + response );

	}

	
	private void updateFeature(String json){

		try {
			String response = f.updateFeature(seasonID, featureID, json.toString(), sessionToken);
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
