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
import tests.restapi.ExperimentsRestApi;
import tests.restapi.FeaturesRestApi;
import tests.restapi.InputSchemaRestApi;
import tests.restapi.ProductsRestApi;
import tests.restapi.RuntimeDateUtilities;
import tests.restapi.RuntimeRestApi;


//known issue:  bug#37 Q4-2017
public class ValidateConfigurationInBranch {
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
	private BranchesRestApi br ;
	private ExperimentsRestApi exp ;
	private String experimentID;
	private String branchID;
	private String variantID;
	private String m_analyticsUrl;
	
	@BeforeClass
	@Parameters({"url", "analyticsUrl", "translationsUrl", "configPath", "sessionToken", "userName", "userPassword", "appName", "productsToDeleteFile"})
	public void init(String url, String analyticsUrl, String translationsUrl, String configPath, String sToken, String userName, String userPassword, String appName, String productsToDeleteFile) throws Exception{
		m_url = url;
		m_analyticsUrl = analyticsUrl;
		filePath = configPath;
		p = new ProductsRestApi();
		p.setURL(m_url);
		f = new FeaturesRestApi();
		f.setURL(m_url);
		an = new AnalyticsRestApi();
		an.setURL(analyticsUrl);
        schema = new InputSchemaRestApi();
        schema.setURL(m_url);
		br = new BranchesRestApi();
		br.setURL(m_url);
		exp = new ExperimentsRestApi();
		exp.setURL(analyticsUrl);
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
	
		create branch, add it to experiment and variant in production
		checkout feature with configuration to branch
		add feature and its attribute to analytics
		add invalid utility and save - an error must be thrown 
		make sure this utility is not returned in getFeature from branch and is not written to S3 in branch
		make getFeatures from branch - in bug it returned AnalyticsMerge error
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
		featureID = f.addFeature(seasonID, feature, "ROOT", sessionToken);
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

		//create experiment and branch
		branchID = addBranch("branch1");
		experimentID = baseUtils.addExperiment(m_analyticsUrl, true, false);
		variantID = addVariant("var1", "branch1");
	}
	
	@Test (dependsOnMethods="addFeatures", description="Add to analytics")
	public void addToAnalytics() throws JSONException, IOException{
		//checkout feature to branch
		br.checkoutFeature(branchID, featureID, sessionToken);
		
		String response = an.addFeatureToAnalytics(featureID, branchID, sessionToken);
		Assert.assertFalse(response.contains("error"), "Feature was not added to analytics" + response);
		
		JSONArray attributes = new JSONArray();
		JSONObject attr1 = new JSONObject();
		attr1.put("name", "value");
		attr1.put("type", "REGULAR");
		attributes.add(attr1);
		response = an.addAttributesToAnalytics(featureID, branchID, attributes.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Feature attributes were not added to analytics" + response);

	}
	
	@Test (dependsOnMethods="addToAnalytics", description="Update configuration")
	public void updateConfiguration() throws JSONException, IOException, InterruptedException{
		JSONObject configurationRule = new JSONObject(f.getFeatureFromBranch(configID, branchID, sessionToken));
		String curConfigJsonStr = "{\"value\":\"value1\", \"value2\":isCel()}";
		
		configurationRule.put("configuration", curConfigJsonStr);
		String response = f.updateFeatureInBranch(seasonID, branchID, configID, configurationRule.toString(), sessionToken);
		Assert.assertTrue(response.contains("error"), "Invalid utility was not reported");
		
		//validations
		JSONArray features = f.getFeaturesBySeasonFromBranch(seasonID, branchID, sessionToken);
		Assert.assertTrue(features.size()!=0, "Get features response was not correct");
		
		an.setSleep();
		RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getRuntimeBranchFileContent(RuntimeDateUtilities.RUNTIME_AIRLOCK_BRANCH_FEATURES, m_url, productID, seasonID, branchID, sessionToken);
		JSONObject runtimeContent = new JSONObject(responseDev.message);
		String configuration = runtimeContent.getJSONArray("features").getJSONObject(0).getJSONArray("configurationRules").getJSONObject(0).getString("configuration");
		Assert.assertFalse(configuration.contains("isCel()"), "Invalid utility was written to S3 runtime branch features file");
	}
	

	private String addVariant(String variantName, String branchName) throws IOException, JSONException{
		String variant = FileUtils.fileToString(filePath + "experiments/variant1.txt", "UTF-8", false);
		JSONObject variantJson = new JSONObject(variant);
		variantJson.put("name", variantName);
		variantJson.put("branchName", branchName);
		variantJson.put("stage", "PRODUCTION");
		return exp.createVariant(experimentID, variantJson.toString(), sessionToken);

	}
	
	private String addBranch(String branchName) throws JSONException, IOException{
		String branch = FileUtils.fileToString(filePath + "experiments/branch1.txt", "UTF-8", false);
		JSONObject branchJson = new JSONObject(branch);
		branchJson.put("name", branchName);
		return br.createBranch(seasonID, branchJson.toString(), BranchesRestApi.MASTER, sessionToken);

	}
	
	
	@AfterTest
	private void reset(){
		baseUtils.reset(productID, sessionToken);
	}
}
