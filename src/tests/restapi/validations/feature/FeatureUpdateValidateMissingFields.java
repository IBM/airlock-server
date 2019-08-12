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

public class FeatureUpdateValidateMissingFields {
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
		feature = f.getFeature(featureID, sessionToken);
		
	}
	
	@Test 
	public void missingNoCachedResults() throws JSONException, IOException{
		feature = JSONUtils.generateUniqueString(feature, 5, "name");
		JSONObject json = new JSONObject(feature);
		removeKey("noCachedResults", json);
		String response = f.updateFeature(seasonID, featureID, json.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Test should pass, but instead failed: " + response );

	}
	
	
	@Test 
	public void missingUniqueId() throws Exception{
		feature = getFeature();
		JSONObject json = new JSONObject(feature);
		removeKey("uniqueId", json);
		String response = f.updateFeature(seasonID, featureID, json.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Test should pass, but instead failed: " + response );

	}
	
	@Test
		public void missingEnabled() throws Exception{
		feature = getFeature();
		JSONObject json = new JSONObject(feature);
		removeKey("enabled", json);
		updateFeature(json.toString());
	}
	
	@Test 
	public void missingForce() throws JSONException, IOException{
		feature = f.getFeature(featureID, sessionToken);
		JSONObject json = new JSONObject(feature);
		
		JSONObject rule = new JSONObject();
		rule.put("ruleString", "");
		json.put("rule", rule);
		
		String response = f.updateFeature(seasonID, featureID, json.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Test should pass, but instead failed: " + response );

	}	
	
	@Test 
	public void missingType() throws Exception{
		feature = getFeature();
		JSONObject json = new JSONObject(feature);
		removeKey("type", json);
		updateFeature(json.toString());
	}
	
	@Test 
	public void missingStage() throws Exception{
		feature = getFeature();
		JSONObject json = new JSONObject(feature);
		removeKey("stage", json);
		updateFeature(json.toString());
	}
	
	@Test
	public void missingAdditionalInfo() throws Exception{
		feature = getFeature();
		JSONObject json = new JSONObject(feature);
		removeKey("additionalInfo", json);
		String response = f.updateFeature(seasonID, featureID, json.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Test should pass, but instead failed: " + response );

	}
	
	
	@Test 
	public void missingNamespace() throws Exception{
		feature = getFeature();
		JSONObject json = new JSONObject(feature);
		removeKey("namespace", json);
		updateFeature(json.toString());
	}
	
	@Test 
	public void missingCreator() throws Exception{
		feature = getFeature();
		JSONObject json = new JSONObject(feature);
		removeKey("creator", json);
		updateFeature(json.toString());
	}	
	
	@Test
	public void missingDescription() throws Exception{
		feature = getFeature();
		JSONObject json = new JSONObject(feature);
		removeKey("description", json);
		String response = f.updateFeature(seasonID, featureID, json.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Test should pass, but instead failed: " + response );

	}
	
	@Test
	public void missingDisplayName() throws Exception{
		feature = getFeature();
		JSONObject json = new JSONObject(feature);
		removeKey("displayName", json);
		String response = f.updateFeature(seasonID, featureID, json.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Test should pass, but instead failed: " + response );

	}
	
	@Test 
	public void missingRule() throws Exception{
		feature = getFeature();
		JSONObject json = new JSONObject(feature);
		removeKey("rule", json);
		updateFeature(json.toString());
	}
	@Test  
	public void missingMinAppVersion() throws Exception{
		feature = getFeature();
		JSONObject json = new JSONObject(feature);
		removeKey("minAppVersion", json);
		updateFeature(json.toString());
	}
	@Test 
	public void missingName() throws Exception{
		feature = getFeature();
		JSONObject json = new JSONObject(feature);
		removeKey("name", json);
		updateFeature(json.toString());
	}
	@Test 
	public void missingSeasonId() throws Exception{
		feature = getFeature();
		JSONObject json = new JSONObject(feature);
		removeKey("seasonId", json);
		updateFeature(json.toString());
	}
	@Test 
	public void missingFeature() throws Exception{
		feature = getFeature();
		JSONObject json = new JSONObject(feature);
		removeKey("features", json);
		updateFeature(json.toString());
	}
	@Test
	public void missingOwner() throws Exception{
		feature = getFeature();
		JSONObject json = new JSONObject(feature);
		removeKey("owner", json);
		String response = f.updateFeature(seasonID, featureID, json.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Test should pass, but instead failed: " + response );

	}
	@Test 
	public void missingDefaultIfAirlockSystemIsDown() throws Exception{
		feature = getFeature();
		JSONObject json = new JSONObject(feature);
		removeKey("defaultIfAirlockSystemIsDown", json);
		updateFeature(json.toString());
	}
	@Test 
	public void missingInternalUserGroups() throws Exception{
		feature = getFeature();
		JSONObject json = new JSONObject(feature);
		removeKey("internalUserGroups", json);
		String response = f.updateFeature(seasonID, featureID, json.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Test should pass, but instead failed: " + response );

	}
	
	
	@Test 
	public void missingRolloutPercentage() throws Exception{
		feature = getFeature();
		JSONObject json = new JSONObject(feature);
		removeKey("rolloutPercentage", json);
		updateFeature(json.toString());
	}
	
	@Test 
	public void missingCreationDate() throws Exception{
		feature = getFeature();
		JSONObject json = new JSONObject(feature);
		removeKey("creationDate", json);
		updateFeature(json.toString());
	}
	
	@Test 
	public void missingLastModified() throws Exception{
		feature = getFeature();
		JSONObject json = new JSONObject(feature);
		removeKey("lastModified", json);
		updateFeature(json.toString());
	}
	
/*	@Test 
	public void missingRolloutPercentageBitmap() throws Exception{
		feature = getFeature();
		JSONObject json = new JSONObject(feature);
		removeKey("rolloutPercentageBitmap", json);
		
		updateFeature(json.toString());
	}
	*/

	private void removeKey(String key, JSONObject json ){
		json.remove(key);
	}

	private void updateFeature(String json){

		try {
			String response = f.updateFeature(seasonID, featureID, json.toString(), sessionToken);
			Assert.assertTrue(response.contains("error"), "Test should fail, but instead passed: " + response );
		}  catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}


	}
	
	
	private String getFeature() throws Exception{
		
		String feature = f.getFeature(featureID, sessionToken);
		return feature;
	}
	
	
	@AfterTest
	public void reset(){
		baseUtils.reset(productID, sessionToken);
	}

}
