package tests.restapi.scenarios.feature;

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

public class FeatureInProductionNoUpdate {
	protected String seasonID;
	protected String featureID;
	protected FeaturesRestApi f;
	protected String feature;
	protected String filePath;
	protected ProductsRestApi p;
	protected String productID;
	protected AirlockUtils baseUtils;
	private String sessionToken = "";
	
	@BeforeClass
	@Parameters({"url", "analyticsUrl", "translationsUrl", "configPath", "sessionToken", "userName", "userPassword", "appName", "productsToDeleteFile"})
	public void init(String url, String analyticsUrl, String translationsUrl, String configPath, String sToken, String userName, String userPassword, String appName, String productsToDeleteFile) throws Exception{
		filePath = configPath;
		f = new FeaturesRestApi();
		f.setURL(url);
		p = new ProductsRestApi();
		p.setURL(url);
		if (sToken != null)
			sessionToken = sToken;
		baseUtils = new AirlockUtils(url, analyticsUrl, translationsUrl, configPath, sToken, userName, userPassword, appName, productsToDeleteFile);
		sessionToken = baseUtils.sessionToken;
		productID = baseUtils.createProduct();
		baseUtils.printProductToFile(productID);
		seasonID = baseUtils.createSeason(productID);
	}

	@Test (description ="Add feature in production stage")
	public void addFeatureInProd() throws IOException, JSONException{
		feature = FileUtils.fileToString(filePath + "feature1.txt", "UTF-8", false);
		JSONObject json = new JSONObject(feature);
		json.put("stage", "PRODUCTION");
		featureID = f.addFeature(seasonID, json.toString(), "ROOT", sessionToken);

	}
	
	@Test (dependsOnMethods="addFeatureInProd", description = "If feature in productions stage, its name can't be updated")
	public void updateName() throws IOException, JSONException{		
		feature = f.getFeature(featureID, sessionToken);
		JSONObject json = new JSONObject(feature);
		json.put("name", "feature in production");
		String response = f.updateFeature(seasonID, featureID, json.toString(), sessionToken);
		Assert.assertTrue(response.contains("error"), "Test should fail, but instead passed: " + response );

	}
	
	
	@Test (dependsOnMethods="updateName", description = "If feature in productions stage, its namespace can't be updated")
	public void updateNamespace() throws IOException, JSONException{		
		feature = f.getFeature(featureID, sessionToken);
		JSONObject json = new JSONObject(feature);
		json.put("namespace", "featureInProd");
		String response = f.updateFeature(seasonID, featureID, json.toString(), sessionToken);
		Assert.assertTrue(response.contains("error"), "Test should fail, but instead passed: " + response );
	}
	
	@Test (dependsOnMethods="updateNamespace", description = "Add feature in development stage")
	public void addFeatureInDev() throws IOException, JSONException{
		feature = FileUtils.fileToString(filePath + "feature2.txt", "UTF-8", false);
		JSONObject json = new JSONObject(feature);
		json.put("stage", "PRODUCTION");
		featureID = f.addFeature(seasonID, json.toString(), "ROOT", sessionToken);
		Assert.assertFalse(featureID.contains("error"), "Test should pass, but instead failed: " + featureID );

	}
	
	@Test (dependsOnMethods="addFeatureInDev", description = "Update both feature stage and name")
	public void updateNameAndStage() throws IOException, JSONException{
		feature = f.getFeature(featureID, sessionToken);
		JSONObject json = new JSONObject(feature);
		json.put("stage", "PRODUCTION");
		json.put("name", "feature in production");
		String response = f.updateFeature(seasonID, featureID, json.toString(), sessionToken);
		Assert.assertTrue(response.contains("error"), "Test should fail, but instead passed: " + response );

	}
	
	
	@Test (dependsOnMethods="updateNameAndStage", description = "Update both feature stage and namespace")
	public void updateNamespaceAndStage() throws IOException, JSONException{
		feature = f.getFeature(featureID, sessionToken);
		JSONObject json = new JSONObject(feature);
		json.put("stage", "PRODUCTION");
		json.put("namespace", "feature in production");
		String response = f.updateFeature(seasonID, featureID, json.toString(), sessionToken);
		Assert.assertTrue(response.contains("error"), "Test should fail, but instead passed: " + response );
	}
	
	@AfterTest
	private void reset(){
		baseUtils.reset(productID, sessionToken);
	}
}
