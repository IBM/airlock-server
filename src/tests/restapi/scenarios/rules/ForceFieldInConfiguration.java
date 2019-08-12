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

public class ForceFieldInConfiguration {
	protected String seasonID;
	protected String featureID;
	protected String configID;
	protected String productID;
	protected String filePath;
	protected FeaturesRestApi f;
	private AirlockUtils baseUtils;
	protected String feature;
	protected ProductsRestApi p;
	private String sessionToken = "";
	
	@BeforeClass
	@Parameters({"url", "analyticsUrl", "translationsUrl", "configPath", "sessionToken", "userName", "userPassword", "appName", "productsToDeleteFile"})
	public void init(String url, String analyticsUrl, String translationsUrl, String configPath, String sToken, String userName, String userPassword, String appName, String productsToDeleteFile) throws Exception{
		filePath = configPath;
		f = new FeaturesRestApi();
		f.setURL(url);
		p = new ProductsRestApi();
		p.setURL(url);
		baseUtils = new AirlockUtils(url, analyticsUrl, translationsUrl, configPath, sToken, userName, userPassword, appName, productsToDeleteFile);
		sessionToken = baseUtils.sessionToken;

		productID = baseUtils.createProduct();
		baseUtils.printProductToFile(productID);
		seasonID = baseUtils.createSeason(productID);
		
		feature = FileUtils.fileToString(filePath + "feature1.txt", "UTF-8", false);
		featureID = f.addFeature(seasonID, feature, "ROOT", sessionToken);
		
		String configuration = FileUtils.fileToString(filePath + "configuration_rule1.txt", "UTF-8", false);

	}
	
	//if force is used in configuration rule, its configuration shouldn't be validated
	
	@Test (description = "When force=false create invalid rule")
	
	public void invalidRuleWithFalseForce() throws JSONException, IOException{
		String configuration = FileUtils.fileToString(filePath + "configuration_rule3.txt", "UTF-8", false);
		String response = f.addFeature(seasonID, configuration, featureID, sessionToken);
		Assert.assertTrue(response.contains("error"), "Configuraton should not be created: " + response );
	}
	
	
	@Test (dependsOnMethods="invalidRuleWithFalseForce", description = "When force=true create invalid configuration")
	
	public void invalidRuleWithTrueForce() throws JSONException, IOException{
		String configuration = FileUtils.fileToString(filePath + "configuration_rule3.txt", "UTF-8", false);
		JSONObject jsonCR = new JSONObject(configuration);
		JSONObject rule = new JSONObject();
		rule.put("force", true);
		rule.put("ruleString", "");
		jsonCR.put("rule", rule);

		
		String response = f.addFeature(seasonID, jsonCR.toString(), featureID, sessionToken);
		Assert.assertFalse(response.contains("error"), "Configuraton should not be created: " + response );
	}

	@AfterTest
	private void reset(){
		baseUtils.reset(productID, sessionToken);
	}
}
