package tests.restapi.scenarios.rules;

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

public class NonExistingSchemaFieldInRule {
	protected String seasonID;
	protected String featureID;
	protected String productID;
	private AirlockUtils baseUtils;
	protected String filePath;
	protected FeaturesRestApi f;
	protected String feature;
	protected ProductsRestApi p;
	private String sessionToken = "";
	
	@BeforeClass
	@Parameters({"url", "analyticsUrl", "translationsUrl", "configPath", "sessionToken", "userName", "userPassword", "appName", "productsToDeleteFile"})
	public void init(String url, String analyticsUrl, String translationsUrl, String configPath, String sToken, String userName, String userPassword, String appName, String productsToDeleteFile) throws Exception{
		filePath = configPath;
		f = new FeaturesRestApi();
		f.setURL(url);
		baseUtils = new AirlockUtils(url, analyticsUrl, translationsUrl, configPath, sToken, userName, userPassword, appName, productsToDeleteFile);
		sessionToken = baseUtils.sessionToken;
		p = new ProductsRestApi();
		p.setURL(url);
		
		productID = baseUtils.createProduct();
		baseUtils.printProductToFile(productID);
		seasonID = baseUtils.createSeason(productID);
		baseUtils.createSchema(seasonID);
		

	}
	
	@Test (description = "Create a feature with a rule that uses a field that doesn't exist in inputSchema")
	
	public void nonExistingFieldInCreate() throws JSONException, IOException{
		feature = FileUtils.fileToString(filePath + "feature1.txt", "UTF-8", false);
				
		JSONObject json = new JSONObject(feature);
		JSONObject rule = new JSONObject();
		rule.put("ruleString", "context.newField == 1");
		json.put("rule", rule);
		
		featureID = f.addFeature(seasonID, json.toString(), "ROOT", sessionToken);
		Assert.assertTrue(featureID.contains("error"), "Test should fail, but instead passed: " + featureID );
	}
	
	
	@Test (description = "Update a feature with a rule that uses a field that doesn't exist in inputSchema")
	
	public void nonExistingFieldInUpdate() throws JSONException, IOException{
		feature = FileUtils.fileToString(filePath + "feature2.txt", "UTF-8", false);
		featureID = f.addFeature(seasonID, feature, "ROOT", sessionToken);
		
		feature = f.getFeature(featureID, sessionToken);
						JSONObject json = new JSONObject(feature);
		JSONObject rule = new JSONObject();
		rule.put("ruleString", "context.newField == 1");
		json.put("rule", rule);
		
		featureID = f.updateFeature(seasonID, featureID, json.toString(), sessionToken);
		Assert.assertTrue(featureID.contains("error"), "Test should fail, but instead passed: " + featureID );
	}
	
	


	@AfterTest
	private void reset(){
		baseUtils.reset(productID, sessionToken);
	}
}
