package tests.restapi.scenarios.utilities;

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
import tests.restapi.InputSchemaRestApi;
import tests.restapi.ProductsRestApi;
import tests.restapi.UtilitiesRestApi;

public class OverwriteContextFieldsInUtility {
	protected String seasonID;
	protected String featureID;
	protected String productID;
	protected String stringID;
	protected String configID;
	protected String  utilityID;
	protected String deepFreezeID;
	protected String filePath;
	protected FeaturesRestApi f;
	protected ProductsRestApi p;
	protected UtilitiesRestApi u;
	//protected StringsRestApi t;
	protected InputSchemaRestApi schemaApi;
	private String sessionToken = "";	
	protected AirlockUtils baseUtils;
	
	
	@BeforeClass
	@Parameters({"url", "analyticsUrl", "translationsUrl", "configPath", "sessionToken", "userName", "userPassword", "appName", "productsToDeleteFile"})
	public void init(String url, String analyticsUrl, String translationsUrl, String configPath, String sToken, String userName, String userPassword, String appName, String productsToDeleteFile) throws Exception{
		filePath = configPath;
		f = new FeaturesRestApi();
		f.setURL(url);
		p = new ProductsRestApi();
		p.setURL(url);
		u = new UtilitiesRestApi();
		u.setURL(url);
		schemaApi = new InputSchemaRestApi();
		schemaApi.setURL(url);
		baseUtils = new AirlockUtils(url, analyticsUrl, translationsUrl, configPath, sToken, userName, userPassword, appName, productsToDeleteFile);
		sessionToken = baseUtils.sessionToken;
		productID = baseUtils.createProduct();
		baseUtils.printProductToFile(productID);
		seasonID = baseUtils.createSeason(productID);
	}
	
	@Test(description = "update inputSchema")
	public void updateInputSchema() throws Exception{
		String schemaStr = FileUtils.fileToString(filePath + "inputSchema.txt", "UTF-8", false);
		String schemaJsonStr = schemaApi.getInputSchema(seasonID, sessionToken);
		JSONObject schemaJson = new JSONObject(schemaJsonStr);
		schemaJson.put("inputSchema", new JSONObject(schemaStr));
		String response = schemaApi.updateInputSchema(seasonID, schemaJson.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Test should pass, but instead failed: " + response );
	}
	
	
	@Test(dependsOnMethods = "updateInputSchema",  description = "Add feature that changes existing context")
	public void addFeature() throws IOException, JSONException{
		String feature = FileUtils.fileToString(filePath + "feature1.txt", "UTF-8", false);
		JSONObject featureJson = new JSONObject(feature);
		JSONObject rule = new JSONObject();
		rule.put("ruleString", "context.viewedLocation.country = \"newcountry\"; true;");
		featureJson.put("rule", rule);

		featureID = f.addFeature(seasonID, featureJson.toString(), "ROOT", sessionToken);				
		Assert.assertTrue(featureID.contains("error"), "Feature was not created " + featureID );
	}
	
	
	@AfterTest
	private void reset(){
		baseUtils.reset(productID, sessionToken);
	}
}
