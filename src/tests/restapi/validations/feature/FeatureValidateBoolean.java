package tests.restapi.validations.feature;

import java.io.IOException;







import org.apache.commons.lang3.RandomStringUtils;
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

public class FeatureValidateBoolean {
	protected String seasonID;
	protected String featureID;
	protected String filePath;
	protected String m_url;
	protected ProductsRestApi p;
	protected String productID;
	protected FeaturesRestApi f;
	private AirlockUtils baseUtils;
	protected String feature;
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
		feature = FileUtils.fileToString(filePath + "feature1.txt", "UTF-8", false);
		f = new FeaturesRestApi();
		f.setURL(url);
 		
	}
	
	@Test (description = "enabled set to a characters string")
	public void ValidateEnabledIsBoolean1() throws JSONException, IOException{
		JSONObject json = new JSONObject(feature);
		json.put("enabled", "test");
		json.put("name", RandomStringUtils.randomAlphabetic(5));
		addFeature(json.toString());
	}
	
	@Test(description = "enabled set to a negative number")
	public void ValidateEnabledIsBoolean2() throws JSONException, IOException{
		JSONObject json = new JSONObject(feature);
		json.put("enabled", -1);
		json.put("name", RandomStringUtils.randomAlphabetic(5));
		addFeature(json.toString());
	}
	
	@Test (description = "enabled set to the string 'true'")
	public void ValidateEnabledIsBoolean3() throws JSONException, IOException{
		JSONObject json = new JSONObject(feature);
		json.put("enabled", "true");
		json.put("name", RandomStringUtils.randomAlphabetic(5));
		String response = f.addFeature(seasonID, json.toString(), "ROOT", sessionToken);
		Assert.assertFalse(response.contains("error"), "Test should pass, but instead failed: " + response );

	}
	
	@Test(description = "enabled set to an empty string")
	public void ValidateEnabledIsBoolean4() throws JSONException, IOException{
		JSONObject json = new JSONObject(feature);
		json.put("enabled", " ");
		json.put("name", RandomStringUtils.randomAlphabetic(5));
		addFeature(json.toString());
	}
	
	@Test(description = "defaultIfAirlockSystemIsDown set to a characters string")
	public void ValidateDefaultIfAirlockSystemIsDownIsBoolean1() throws JSONException, IOException{
		JSONObject json = new JSONObject(feature);
		json.put("defaultIfAirlockSystemIsDown", "test");
		json.put("name", RandomStringUtils.randomAlphabetic(5));
		addFeature(json.toString());
	}
	
	@Test(description = "defaultIfAirlockSystemIsDown set to a negative number")
	public void ValidateDefaultIfAirlockSystemIsDownIsBoolean2() throws JSONException, IOException{
		JSONObject json = new JSONObject(feature);
		json.put("defaultIfAirlockSystemIsDown", -1);
		json.put("name", RandomStringUtils.randomAlphabetic(5));
		addFeature(json.toString());
	}
	
	@Test (description = "defaultIfAirlockSystemIsDown set to the string 'true' ")
	public void ValidateDefaultIfAirlockSystemIsDownIsBoolean3() throws JSONException, IOException{
		JSONObject json = new JSONObject(feature);
		json.put("defaultIfAirlockSystemIsDown", "true");
		json.put("name", RandomStringUtils.randomAlphabetic(5));
		String response = f.addFeature(seasonID, json.toString(), "ROOT", sessionToken);
		Assert.assertFalse(response.contains("error"), "Test should pass, but instead failed: " + response );

	}
	
	@Test(description = "defaultIfAirlockSystemIsDown set to an empty string")
	public void ValidateDefaultIfAirlockSystemIsDownIsBoolean4() throws JSONException, IOException{
		JSONObject json = new JSONObject(feature);
		json.put("defaultIfAirlockSystemIsDown", " ");
		json.put("name", RandomStringUtils.randomAlphabetic(5));
		addFeature(json.toString());
	}
	
	//_________-----------------------
	
	@Test(description = "defaultIfAirlockSystemIsDown set to a characters string")
	public void ValidateForceIsBoolean1() throws JSONException, IOException{
		JSONObject json = new JSONObject(feature);
		JSONObject rule = new JSONObject();
		rule.put("ruleString", "");
		rule.put("force", "test");
		json.put("rule", rule);
		json.put("name", RandomStringUtils.randomAlphabetic(5));
		addFeature(json.toString());
	}
	
	@Test(description = "defaultIfAirlockSystemIsDown set to a negative number")
	public void ValidateForceIsBoolean2() throws JSONException, IOException{
		JSONObject json = new JSONObject(feature);
		JSONObject rule = new JSONObject();
		rule.put("ruleString", "");
		rule.put("force", -1);
		json.put("rule", rule);
		json.put("name", RandomStringUtils.randomAlphabetic(5));
		addFeature(json.toString());
	}
	
	@Test (description = "defaultIfAirlockSystemIsDown set to the string 'true' ")
	public void ValidateForceIsBoolean3() throws JSONException, IOException{
		JSONObject json = new JSONObject(feature);
		JSONObject rule = new JSONObject();
		rule.put("ruleString", "");
		rule.put("force", true);
		json.put("rule", rule);
		json.put("name", RandomStringUtils.randomAlphabetic(5));
		String response = f.addFeature(seasonID, json.toString(), "ROOT", sessionToken);
		Assert.assertFalse(response.contains("error"), "Test should pass, but instead failed: " + response );

	}
	
	@Test(description = "defaultIfAirlockSystemIsDown set to an empty string")
	public void ValidateForceIsBoolean4() throws JSONException, IOException{
		JSONObject json = new JSONObject(feature);
		JSONObject rule = new JSONObject();
		rule.put("ruleString", " ");
		rule.put("force", "test");
		json.put("rule", rule);
		json.put("name", RandomStringUtils.randomAlphabetic(5));
		addFeature(json.toString());
	}
	
	private void addFeature(String json){

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
