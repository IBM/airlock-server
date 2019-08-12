package tests.restapi.scenarios.feature;

import java.io.IOException;


import java.util.UUID;

import org.apache.wink.json4j.JSONArray;
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

public class FeatureInDevelopmentStage {
	protected String seasonID;
	protected String featureID;
	protected FeaturesRestApi f;
	protected String feature;
	JSONObject json;
	protected ProductsRestApi p;
	protected String productID;
	protected AirlockUtils baseUtils;
	private String sessionToken = "";

	@BeforeClass
	@Parameters({"url", "analyticsUrl", "translationsUrl", "configPath", "sessionToken", "userName", "userPassword", "appName", "productsToDeleteFile"})
	public void init(String url, String analyticsUrl, String translationsUrl, String configPath, String sToken, String userName, String userPassword, String appName, String productsToDeleteFile) throws Exception{
		f = new FeaturesRestApi();
		f.setURL(url);
		p = new ProductsRestApi();
		p.setURL(url);
		baseUtils = new AirlockUtils(url, analyticsUrl, translationsUrl, configPath, sToken, userName, userPassword, appName, productsToDeleteFile);
		sessionToken = baseUtils.sessionToken;
		productID = baseUtils.createProduct();
		baseUtils.printProductToFile(productID);
		seasonID = baseUtils.createSeason(productID);
		feature = FileUtils.fileToString(configPath + "feature1.txt", "UTF-8", false);
		
	}
	
	
	@Test (description = "If feature in DEVELOPMENT stage, defaultIfAirlockSystemIsDown must be false in the defaults file, even if it is set to true in the feature")
	public void testDefaultIfAirlockSystemIsDownInDev1() throws IOException, JSONException{		
		json = new JSONObject(feature);
		json.put("defaultIfAirlockSystemIsDown", true);
		//test in creation with defaultIfAirlockSystemIsDown=true, stage =DEV - now allowed, but it defaults it should be set to false
		featureID = f.addFeature(seasonID, json.toString(), "ROOT", sessionToken);
		String name = json.getString("name");	
		validateDefaults(seasonID, name);

	}
	
	@Test (dependsOnMethods="testDefaultIfAirlockSystemIsDownInDev1", description = "test in creation with defaultIfAirlockSystemIsDown=false, , stage =DEV - allowed")
	public void testDefaultIfAirlockSystemIsDownInDev2() throws IOException, JSONException{

		//test in creation with defaultIfAirlockSystemIsDown=false, , stage =DEV - allowed		
		featureID = baseUtils.createFeature(seasonID);
		validUUID(featureID);

	}
	
	@Test (dependsOnMethods="testDefaultIfAirlockSystemIsDownInDev2", description = "update a feature in DEV with defaultIfAirlockSystemIsDown=true, stage =DEV - now allowed, but it defaults it should be set to false")
	public void testDefaultIfAirlockSystemIsDownInDev3() throws IOException, JSONException{
		//update a feature in DEV with defaultIfAirlockSystemIsDown=true, stage =DEV - now allowed, but it defaults it should be set to false
		String addedFeature = f.getFeature(featureID, sessionToken);
		JSONObject featureJson = new JSONObject(addedFeature);
		json.put("defaultIfAirlockSystemIsDown", true);		
		featureID = f.updateFeature(seasonID, featureID, featureJson.toString(), sessionToken);
		
		String feature = f.getFeature(featureID, sessionToken);
		featureJson = new JSONObject(feature);
		String name = featureJson.getString("name");	
		validateDefaults(seasonID, name);
		
	}
	
	@Test (dependsOnMethods="testDefaultIfAirlockSystemIsDownInDev3", description = "test in creation defaultIfAirlockSystemIsDown=true, stage=PROD - allowed")
	public void testDefaultIfAirlockSystemIsDownInProd1() throws IOException, JSONException{
		
		json = new JSONObject(feature);
		json.put("stage", "PRODUCTION");
		json.put("defaultIfAirlockSystemIsDown", true);
		feature = JSONUtils.generateUniqueString(json.toString(), 8, "name");
		
		//test in creation defaultIfAirlockSystemIsDown=true, stage=PROD - allowed
		featureID = f.addFeature(seasonID, feature, "ROOT", sessionToken);
		validUUID(featureID);

	}
	
	@Test (dependsOnMethods="testDefaultIfAirlockSystemIsDownInProd1", description = "test in update to stage=DEV when defaultIfAirlockSystemIsDown=true - now allowed, but it defaults it should be set to false")
	public void testDefaultIfAirlockSystemIsDownInProd2() throws IOException, JSONException{
		
		feature = f.getFeature(featureID, sessionToken);
		JSONObject json = new JSONObject(feature);
		json.put("stage", "DEVELOPMENT");
		
		//test in update to stage=DEV when defaultIfAirlockSystemIsDown=true - now allowed, but it defaults it should be set to false
		featureID = f.updateFeature(seasonID, featureID, json.toString(), sessionToken);
		String feature = f.getFeature(featureID, sessionToken);
		JSONObject featureJson = new JSONObject(feature);
		String name = featureJson.getString("name");	
		validateDefaults(seasonID, name);

	}
	
	@Test (dependsOnMethods="testDefaultIfAirlockSystemIsDownInProd2", description = "test in update defaultIfAirlockSystemIsDown=false, stage=PROD - allowed")
	public void testDefaultIfAirlockSystemIsDownInProd3() throws IOException, JSONException{
		
		//test in update defaultIfAirlockSystemIsDown=false, stage=PROD - allowed
		feature = f.getFeature(featureID, sessionToken);
		JSONObject json = new JSONObject(feature);
		json.put("defaultIfAirlockSystemIsDown", false);
		featureID = f.updateFeature(seasonID, featureID, json.toString(), sessionToken);
		validUUID(featureID);

	}
	
	@Test (dependsOnMethods="testDefaultIfAirlockSystemIsDownInProd3", description = "change to defaultIfAirlockSystemIsDown=false in creation, stage=PROD - allowed")
	public void testDefaultIfAirlockSystemIsDownInProd4() throws IOException, JSONException{
		
		//change to defaultIfAirlockSystemIsDown=false in creation, stage=PROD - allowed
		json.put("stage", "PRODUCTION");
		feature = JSONUtils.generateUniqueString(json.toString(), 8, "name");
		featureID = f.addFeature(seasonID, feature, "ROOT", sessionToken);
		validUUID(featureID);

	}
	
	@Test (dependsOnMethods="testDefaultIfAirlockSystemIsDownInProd4", description = "test in update defaultIfAirlockSystemIsDown=true, stage=PROD - allowed")
	public void testDefaultIfAirlockSystemIsDownInProd5() throws IOException, JSONException{

		//test in update defaultIfAirlockSystemIsDown=true, stage=PROD - allowed
		feature = f.getFeature(featureID, sessionToken);
		JSONObject json = new JSONObject(feature);
		json.put("defaultIfAirlockSystemIsDown", true);
		featureID = f.updateFeature(seasonID, featureID, json.toString(), sessionToken);
		validUUID(featureID);

	}

	
	private void validUUID(String featureID){
		try {
			UUID.fromString(featureID);			
		} catch (Exception e){
			Assert.fail("Could not create/update a feature");
		}
	}
	
	private void validateDefaults(String seasonID, String name) throws JSONException{
		String defaults = baseUtils.getDefaults(seasonID);
		JSONObject defaultsJson = new JSONObject(defaults);
		JSONObject root = defaultsJson.getJSONObject("root");
		JSONArray features = root.getJSONArray("features");
		for (int i=0; i<features.size(); i++){
			//JSONObject feature = features.getJSONObject(i);
	
			if (features.getJSONObject(i).getString("name").equals(name)){
				Assert.assertTrue(features.getJSONObject(i).getString("defaultIfAirlockSystemIsDown").equals("false"), "defaultIfAirlockSystemIsDown was not set to false in the defaults file");
			}
		}
	}
	
	
	@AfterTest
	private void reset(){
		baseUtils.reset(productID, sessionToken);
	}
}
