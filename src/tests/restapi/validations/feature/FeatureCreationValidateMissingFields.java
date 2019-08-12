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

public class FeatureCreationValidateMissingFields {
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
		removeKey("noCachedResults", json);
		addFeature(json.toString());
	}
	
	@Test 
	public void missingUniqueId() throws JSONException{
		feature = JSONUtils.generateUniqueString(feature, 5, "name");
		JSONObject json = new JSONObject(feature);
		removeKey("uniqueId", json);
		addFeature(json.toString());
	}
	
	@Test 
		public void missingEnabled() throws JSONException{
		JSONObject json = new JSONObject(feature);
		removeKey("enabled", json);
		addInvalidFeature(json.toString());
	}
	
	@Test 
	public void missingType() throws JSONException{
		JSONObject json = new JSONObject(feature);
		removeKey("type", json);
		addInvalidFeature(json.toString());
	}
	
	@Test 
	public void missingStage() throws JSONException{
		JSONObject json = new JSONObject(feature);
		removeKey("stage", json);
		addInvalidFeature(json.toString());
	}
	
	@Test
	public void missingAdditionalInfo() throws JSONException{
		feature = JSONUtils.generateUniqueString(feature, 5, "name");
		JSONObject json = new JSONObject(feature);
		removeKey("additionalInfo", json);
		addFeature(json.toString());
	}
	
	
	@Test 
	public void missingNamespace() throws JSONException{
		JSONObject json = new JSONObject(feature);
		removeKey("namespace", json);
		addInvalidFeature(json.toString());
	}
	
	@Test 
	public void missingCreator() throws JSONException{
		JSONObject json = new JSONObject(feature);
		removeKey("creator", json);
		addInvalidFeature(json.toString());
	}	
	
	@Test
	public void missingDescription() throws JSONException{
		feature = JSONUtils.generateUniqueString(feature, 5, "name");
		JSONObject json = new JSONObject(feature);
		removeKey("description", json);
		addFeature(json.toString());
	}
	
	@Test
	public void missingDisplayName() throws JSONException{
		feature = JSONUtils.generateUniqueString(feature, 5, "name");
		JSONObject json = new JSONObject(feature);
		removeKey("displayName", json);
		addFeature(json.toString());
	}
	
	@Test 
	public void missingRule() throws JSONException{
		JSONObject json = new JSONObject(feature);
		removeKey("rule", json);
		addInvalidFeature(json.toString());
	}

	
	@Test
	public void missingMinAppVersion() throws JSONException{
		feature = JSONUtils.generateUniqueString(feature, 5, "name");
		JSONObject json = new JSONObject(feature);
		removeKey("minAppVersion", json);
		addInvalidFeature(json.toString());
	}
	
	@Test
	public void missingName() throws JSONException{
		JSONObject json = new JSONObject(feature);
		removeKey("name", json);
		addInvalidFeature(json.toString());
	}
	@Test
	public void missingSeasonId() throws JSONException{
		feature = JSONUtils.generateUniqueString(feature, 5, "name");
		JSONObject json = new JSONObject(feature);
		removeKey("seasonId", json);
		addFeature(json.toString());
	}
	@Test
	public void missingFeature() throws JSONException{
		feature = JSONUtils.generateUniqueString(feature, 5, "name");
		JSONObject json = new JSONObject(feature);
		removeKey("features", json);
		addFeature(json.toString());
	}
	@Test
	public void missingOwner() throws JSONException{
		feature = JSONUtils.generateUniqueString(feature, 5, "name");
		JSONObject json = new JSONObject(feature);
		removeKey("owner", json);
		addFeature(json.toString());
	}
	@Test 
	public void missingDefaultIfAirlockSystemIsDown() throws JSONException{
		JSONObject json = new JSONObject(feature);
		removeKey("defaultIfAirlockSystemIsDown", json);
		addInvalidFeature(json.toString());
	}
	@Test
	public void missingInternalUserGroups() throws JSONException{
		feature = JSONUtils.generateUniqueString(feature, 5, "name");
		JSONObject json = new JSONObject(feature);
		removeKey("internalUserGroups", json);
		addFeature(json.toString());
	}
	
	
	@Test 
	public void missingRolloutPercentage() throws JSONException{
		JSONObject json = new JSONObject(feature);
		removeKey("rolloutPercentage", json);
		addInvalidFeature(json.toString());
	}
	
	

	private void removeKey(String key, JSONObject json){
		json.remove(key);
	}


	private void addFeature(String featureJson){

		try {
			String response = f.addFeature(seasonID, featureJson, "ROOT", sessionToken);
			Assert.assertFalse(response.contains("error"), "Test should pass, but instead failed: " + response );

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	
	}
	
	private void addInvalidFeature(String featureJson){

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
