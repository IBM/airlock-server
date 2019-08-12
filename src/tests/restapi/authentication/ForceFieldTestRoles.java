package tests.restapi.authentication;

import java.io.File;

import java.io.IOException;
import java.io.StringReader;
import java.util.Properties;

import org.apache.wink.json4j.JSONException;
import org.apache.wink.json4j.JSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import tests.com.ibm.qautils.FileUtils;
import tests.com.ibm.qautils.JSONUtils;
import tests.restapi.AirlockUtils;
import tests.restapi.FeaturesRestApi;
import tests.restapi.InputSchemaRestApi;
import tests.restapi.ProductsRestApi;
import tests.restapi.SeasonsRestApi;
import tests.restapi.UtilitiesRestApi;

public class ForceFieldTestRoles {
	protected String seasonID;
	protected String featureID;
	private String configRuleID;
	private String utilityID;
	protected String productID;
	protected String filePath;
	protected FeaturesRestApi f;
	protected String feature;
	protected ProductsRestApi p;
	private String sessionToken = "";
	private String adminToken;
	protected UtilitiesRestApi u;
	private AirlockUtils baseUtils;
	protected InputSchemaRestApi schema;
	
	@BeforeClass
	@Parameters({"url", "analyticsUrl", "translationsUrl", "configPath", "sessionToken", "productsToDeleteFile", "appName", "adminToken", "adminPassword", "userToken", "userPassword"})
	public void init(String url, String  analyticsUrl, String translationsUrl, String configPath, @Optional String sToken, String productsToDeleteFile, @Optional String appName, String adminToken, String adminPassword, String userToken, String userPassword) throws IOException, JSONException{
		filePath = configPath;
		baseUtils = new AirlockUtils(url, analyticsUrl, translationsUrl, configPath, sToken, userToken, userPassword, appName, productsToDeleteFile);
		p = new ProductsRestApi();
		p.setURL(url);
		
		f = new FeaturesRestApi();
		f.setURL(url);
		
        schema = new InputSchemaRestApi();
        schema.setURL(url);
		
		u = new UtilitiesRestApi();
		u.setURL(url);

		adminToken = baseUtils.getJWTToken(adminToken, adminPassword, appName);
		sessionToken = baseUtils.getJWTToken(userToken, userPassword, appName);
		ProductsRestApi p = new ProductsRestApi();
		p.setURL(url);
		String product = FileUtils.fileToString(configPath + "product1.txt", "UTF-8", false);
		product = JSONUtils.generateUniqueString(product, 5, "codeIdentifier");
		product = JSONUtils.generateUniqueString(product, 8, "name");
		productID = p.addProduct(product, adminToken);

		SeasonsRestApi s = new SeasonsRestApi();
		s.setURL(url);		
		String season = FileUtils.fileToString(configPath + "season1.txt", "UTF-8", false);
		seasonID = s.addSeason(productID, season, adminToken);
		
		
	}
	
	@Test (description = "Add feature with force=true in production")
	
	public void addFeatureWithForce() throws JSONException, IOException{
		feature = FileUtils.fileToString(filePath + "feature1.txt", "UTF-8", false);
		JSONObject json = new JSONObject(feature);
		json.put("stage", "PRODUCTION");
		JSONObject rule = new JSONObject();
		rule.put("force", true);
		rule.put("ruleString", "test.viewedLocation.country");
		json.put("rule", rule);

		featureID = f.addFeature(seasonID, json.toString(), "ROOT", sessionToken);
		Assert.assertTrue(featureID.contains("error"), "Illegal role created feature " + featureID );
		
		featureID = f.addFeature(seasonID, json.toString(), "ROOT", adminToken);
		Assert.assertFalse(featureID.contains("error"), "Administrator failed to create feature " + featureID );

	}
	
	@Test (dependsOnMethods="addFeatureWithForce", description = "Update feature with force=true")
	
	public void updateFeatureWithForce() throws JSONException, IOException{
		feature = f.getFeature(featureID, sessionToken);
		JSONObject json = new JSONObject(feature);
		JSONObject rule = new JSONObject();
		rule.put("force", true);
		rule.put("ruleString", "test.viewedLocation.region");
		json.put("rule", rule);
		String response = f.updateFeature(seasonID, featureID, json.toString(), sessionToken);
		Assert.assertTrue(response.contains("error"), "Illegal role updated feature " + response );
		
		response = f.updateFeature(seasonID, featureID, json.toString(), adminToken);
		Assert.assertFalse(response.contains("error"), "Administrator failed to update feature " + response );

	}
	
	@Test (dependsOnMethods="updateFeatureWithForce", description = "Add configuration rule with force=true")
	public void createConfigurationRule() throws IOException, JSONException{
		String configuration = FileUtils.fileToString(filePath + "configuration_rule1.txt", "UTF-8", false);
		JSONObject json = new JSONObject(configuration);
		json.put("stage", "PRODUCTION");
		JSONObject rule = new JSONObject();
		rule.put("force", true);
		rule.put("ruleString", "test.viewedLocation.country");
		json.put("rule", rule);

		configRuleID = f.addFeature(seasonID, json.toString(), featureID, sessionToken );
		Assert.assertTrue(configRuleID.contains("error"), "Illegal role created configuration rule " );
		
		configRuleID = f.addFeature(seasonID, json.toString(), featureID, adminToken );
		Assert.assertFalse(configRuleID.contains("error"), "Administrator failed to add configuration rule " + configRuleID );

	}
	
	@Test (dependsOnMethods="createConfigurationRule", description = "Add configuration rule with force=true")
	public void updateConfigurationRule() throws IOException, JSONException{
		String configuration = f.getFeature(configRuleID, sessionToken);
		JSONObject json = new JSONObject(configuration);
		json.put("stage", "PRODUCTION");
		JSONObject rule = new JSONObject();
		rule.put("force", true);
		rule.put("ruleString", "test.viewedLocation.country");
		json.put("rule", rule);

		String response = f.updateFeature(seasonID, configRuleID, json.toString(), sessionToken);
		Assert.assertTrue(response.contains("error"), "Illegal role updated configuration rule " + response );
		
		response = f.updateFeature(seasonID, configRuleID, json.toString(), adminToken);
		Assert.assertFalse(response.contains("error"), "Administrator failed to update configuration rule " + response );

	}
	
	@Test(dependsOnMethods="updateConfigurationRule", description = "Add utility with force")
	public void addUtility() throws IOException, JSONException{
		
		String utility = FileUtils.fileToString(filePath + "utilities" + File.separator + "utility1.txt", "UTF-8", false);
		Properties utilProps = new Properties();
		utilProps.load(new StringReader(utility));	
		utilityID = u.addUtilityWithForce(seasonID, utilProps, true, sessionToken);
		Assert.assertTrue(utilityID.contains("error"), "Illegal role added utility with force " );
		
		utilityID = u.addUtilityWithForce(seasonID, utilProps, true, adminToken);
		Assert.assertFalse(utilityID.contains("error"), "Administrator failed to add utility with force " + utilityID);

	}
	
	@Test  (dependsOnMethods="addUtility", description = "Update utility with force")
	public void updateUtility() throws Exception{
			String utility = u.getUtility(utilityID, sessionToken);
			JSONObject json = new JSONObject(utility);
			json.put("utility", "function isFalse(){return true;}");
			
			String reponse = u.updateUtilityWithForce(utilityID, json, true, sessionToken);
			Assert.assertTrue(reponse.contains("error"), "Illegal role updated utility with force " );
			
			reponse = u.updateUtilityWithForce(utilityID, json, true, adminToken);
			Assert.assertFalse(reponse.contains("error"), "Adminitrator failed to update utility with force " ); 

	}
	
	@Test (dependsOnMethods="updateUtility", description="Add input schema to the season")
	public void addSchema() throws Exception{
		String sch = schema.getInputSchema(seasonID, sessionToken);
        JSONObject jsonSchema = new JSONObject(sch);
        String schemaBody = FileUtils.fileToString(filePath + "inputSchemas/inputSchema_optionalField_unitsOfMeasure.txt", "UTF-8", false);
        jsonSchema.put("inputSchema", new JSONObject(schemaBody));
        
        String response = schema.updateInputSchemaWithForce(seasonID, jsonSchema.toString(), true, sessionToken);
        Assert.assertTrue(response.contains("error"), "Illegal role updated schema with force");
        
        response = schema.updateInputSchemaWithForce(seasonID, jsonSchema.toString(), true, adminToken);
        Assert.assertFalse(response.contains("error"), "Administrator failed to update schema with force");
	}
	

	@AfterTest
	private void reset(){
		baseUtils.reset(productID, sessionToken);
	}
}
