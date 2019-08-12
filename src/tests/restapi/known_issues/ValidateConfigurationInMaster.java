//package tests.restapi.scenarios.validations;
package tests.restapi.known_issues;

import java.io.IOException;





import org.apache.wink.json4j.JSONException;
import org.apache.wink.json4j.JSONObject;
import org.apache.wink.json4j.JSONArray;
import org.testng.Assert;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import tests.com.ibm.qautils.FileUtils;
import tests.restapi.AirlockUtils;
import tests.restapi.AnalyticsRestApi;
import tests.restapi.BranchesRestApi;
import tests.restapi.FeaturesRestApi;
import tests.restapi.InputSchemaRestApi;
import tests.restapi.ProductsRestApi;
import tests.restapi.RuntimeDateUtilities;
import tests.restapi.RuntimeRestApi;

//known issue:  bug#37 Q4-2017

public class ValidateConfigurationInMaster{
	protected String seasonID;
	protected String productID;
	protected String featureID;
	protected String configID;
	protected String filePath;
	protected String m_url;
	protected ProductsRestApi p;
	protected FeaturesRestApi f;
	protected AnalyticsRestApi an;
	protected InputSchemaRestApi schema;
	private String sessionToken = "";
	protected AirlockUtils baseUtils;

	
	@BeforeClass
	@Parameters({"url", "analyticsUrl", "translationsUrl", "configPath", "sessionToken", "userName", "userPassword", "appName", "productsToDeleteFile"})
	public void init(String url, String analyticsUrl, String translationsUrl, String configPath, String sToken, String userName, String userPassword, String appName, String productsToDeleteFile) throws Exception{
		m_url = url;
		filePath = configPath;
		p = new ProductsRestApi();
		p.setURL(m_url);
		f = new FeaturesRestApi();
		f.setURL(m_url);
		an = new AnalyticsRestApi();
		an.setURL(analyticsUrl);
        schema = new InputSchemaRestApi();
        schema.setURL(m_url);
		baseUtils = new AirlockUtils(url, analyticsUrl, translationsUrl, configPath, sToken, userName, userPassword, appName, productsToDeleteFile);
		sessionToken = baseUtils.sessionToken;
		productID = baseUtils.createProduct();
		baseUtils.printProductToFile(productID);
		seasonID = baseUtils.createSeason(productID);
		
	}
	
	/* TEST FOR VALIDATION BUG:
	 * 	add input schema
		add feature with configuration. In configuration use rule context.turbo.vt1daypart.day == \"day\". In configuration add attribute and save. 
		report feature & attribute to analytics
		In configuration add new attribute - invalid (non-existing) utility - an error must be thrown
		make sure this utility is not returned in getFeature and is not written to S3
		make getFeatures - in bug it returned AnalyticsMerge error
	 */
	
	
	@Test (description="Add input schema, feature and configuration")
	public void addFeatures() throws Exception{
		//add schema
		String sch = schema.getInputSchema(seasonID, sessionToken);
        JSONObject jsonSchema = new JSONObject(sch);
        String schemaBody = FileUtils.fileToString(filePath + "inputSchemas/Android_schema_v3.0.json", "UTF-8", false);
        jsonSchema.put("inputSchema", new JSONObject(schemaBody));
        String response = schema.updateInputSchema(seasonID, jsonSchema.toString(), sessionToken);
        Assert.assertFalse(response.contains("error"), "Schema was not added to the season" + response);
		
		
		//add feature
		String feature = FileUtils.fileToString(filePath + "feature1.txt", "UTF-8", false);
		JSONObject jsonF = new JSONObject(feature);
		jsonF.put("minAppVersion", "8.0");
		featureID = f.addFeature(seasonID, jsonF.toString(), "ROOT", sessionToken);
		Assert.assertFalse(featureID.contains("error"), "Feature was not added to the season" + featureID);
		
		//add configuration
		JSONObject configurationRule = new JSONObject(FileUtils.fileToString(filePath + "configuration_rule1.txt", "UTF-8", false));
		
		JSONObject rule = new JSONObject();
		rule.put("ruleString", "context.turbo.vt1daypart.day == 'day'");
		configurationRule.put("rule", rule);
		
		JSONObject configuration = new JSONObject();
		configuration.put("value", "value1");
		configurationRule.put("configuration", configuration);
		
		configurationRule.put("minAppVersion", "8.0");
		
		configID = f.addFeature(seasonID, configurationRule.toString(), featureID, sessionToken);
		Assert.assertFalse(configID.contains("error"), "Configuration rule was not added to the season" + configID);

	}
	
	@Test (dependsOnMethods="addFeatures", description="Add to analytics")
	public void addToAnalytics() throws JSONException{
		String response = an.addFeatureToAnalytics(featureID, BranchesRestApi.MASTER, sessionToken);
		Assert.assertFalse(response.contains("error"), "Feature was not added to analytics" + response);
		
		JSONArray attributes = new JSONArray();
		JSONObject attr1 = new JSONObject();
		attr1.put("name", "value");
		attr1.put("type", "REGULAR");
		attributes.add(attr1);
		response = an.addAttributesToAnalytics(featureID, BranchesRestApi.MASTER, attributes.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Feature attributes were not added to analytics" + response);

	}
	
	@Test (dependsOnMethods="addToAnalytics", description="Update configuration")
	public void updateConfiguration() throws JSONException, IOException, InterruptedException{
		JSONObject configurationRule = new JSONObject(f.getFeatureFromBranch(configID, BranchesRestApi.MASTER, sessionToken));
		String curConfigJsonStr = "{\"value\":\"value1\", \"value2\":isCel()}";
		
		configurationRule.put("configuration", curConfigJsonStr);
		String response = f.updateFeatureInBranch(seasonID, BranchesRestApi.MASTER, configID, configurationRule.toString(), sessionToken);
		Assert.assertTrue(response.contains("error"), "Invalid utility was not reported");

		
		
		//validations
		JSONArray features = f.getFeaturesBySeason(seasonID, sessionToken);
		Assert.assertTrue(features.size()!=0, "Get features response was not correct");
		
		an.setSleep();
		RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getRuntimeFileContent(RuntimeDateUtilities.RUNTIME_AIRLOCK_FEATURES, m_url, productID, seasonID, sessionToken);
		JSONObject runtimeContent = new JSONObject(responseDev.message);
		String configuration = runtimeContent.getJSONObject("root").getJSONArray("features").getJSONObject(0).getJSONArray("configurationRules").getJSONObject(0).getString("configuration");
		Assert.assertFalse(configuration.contains("isCel()"), "Invalid utility was written to S3 runtime features file");
	}
	
	
	@AfterTest
	private void reset(){
		baseUtils.reset(productID, sessionToken);
	}
}
