package tests.restapi.analytics;

import java.io.IOException;


import org.apache.wink.json4j.JSONArray;
import org.apache.wink.json4j.JSONException;
import org.apache.wink.json4j.JSONObject;
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
import tests.restapi.ProductsRestApi;
import tests.restapi.UtilitiesRestApi;


public class UtilityDefinedInRuleReportToAnalytics {
	protected String seasonID;
	protected String productID;
	protected String filePath;
	protected FeaturesRestApi f;
	private AirlockUtils baseUtils;
	protected ProductsRestApi p;
	private String sessionToken = "";
	private String feature;
	private String configuration;
	protected UtilitiesRestApi u;
	protected AnalyticsRestApi an;
	private String branchID;

	@BeforeClass
	@Parameters({"url", "analyticsUrl", "translationsUrl", "configPath", "sessionToken", "userName", "userPassword", "appName", "productsToDeleteFile", "branchType"})
	public void init(String url, String analyticsUrl, String translationsUrl, String configPath, String sToken, String userName, String userPassword, String appName, String productsToDeleteFile, String branchType) throws Exception{
		filePath = configPath;
		f = new FeaturesRestApi();
		f.setURL(url);
		p = new ProductsRestApi();
		p.setURL(url);
		u = new UtilitiesRestApi();
		u.setURL(url);
		an = new AnalyticsRestApi();
		an.setURL(analyticsUrl);
		baseUtils = new AirlockUtils(url, analyticsUrl, translationsUrl, configPath, sToken, userName, userPassword, appName, productsToDeleteFile);
		sessionToken = baseUtils.sessionToken;

		productID = baseUtils.createProduct();
		baseUtils.printProductToFile(productID);
		seasonID = baseUtils.createSeason(productID);
		feature = FileUtils.fileToString(filePath + "feature1.txt", "UTF-8", false);
		configuration = FileUtils.fileToString(filePath + "configuration_rule1.txt", "UTF-8", false);

		try {
			if(branchType.equals("Master")) {
				branchID = BranchesRestApi.MASTER;
			}
			else if(branchType.equals("StandAlone")) {
				branchID = baseUtils.addBranchFromBranch("branch1",BranchesRestApi.MASTER,seasonID);
			}
			else if(branchType.equals("DevExp")) {
				branchID = baseUtils.createBranchInExperiment(analyticsUrl);
			}
			else if(branchType.equals("ProdExp")) {
				branchID = baseUtils.createBranchInProdExperiment(analyticsUrl).getString("brId");
			}
			else{
				branchID = null;
			}
		}catch (Exception e){
			branchID = null;
		}

	}


	@Test(description = "Create global variable in rule, use it in configuration and report to analytics")
	public void addGlobalVarInConfigurationRule() throws JSONException, IOException{
		String ruleString = "myvar='stam';true";
		String confString = "{\"text\":myvar}";

		JSONObject json = new JSONObject(feature);
		json.put("name", "F1");
		String featureID = f.addFeature(seasonID, json.toString(), "ROOT", sessionToken);
		Assert.assertFalse(featureID.contains("error"), "Can't create feature");

		JSONObject jsonCR = new JSONObject(configuration);
		jsonCR.put("configuration", confString);
		jsonCR.put("name", "CR1");
		JSONObject ruleObj = new JSONObject();
		ruleObj.put("ruleString", ruleString);
		jsonCR.put("rule", ruleObj);
		String configID = f.addFeature(seasonID, jsonCR.toString(), featureID, sessionToken);
		Assert.assertFalse(configID.contains("error"), "Configuraton was not created: " + configID );

		//report title attribute to analytics (due to bug)
		JSONArray attributes = new JSONArray();
		JSONObject attr1 = new JSONObject();
		attr1.put("name", "text");
		attr1.put("type", "REGULAR");
		attributes.add(attr1);

		String response = an.addAttributesToAnalytics(featureID, BranchesRestApi.MASTER, attributes.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Attributes were not added to analytics" + response);

		String anResponse = an.getGlobalDataCollection(seasonID, branchID, "BASIC", sessionToken);
		Assert.assertTrue(validateAttributeInAnalytics(anResponse).size()==1, "Incorrect number of attributes in analytics");

		String display = an.getGlobalDataCollection(seasonID, branchID, "DISPLAY", sessionToken);
		Assert.assertTrue(an.validateDataCollectionByFeatureNames(display, anResponse).equals("true"));

		String verbose = an.getGlobalDataCollection(seasonID, branchID, "VERBOSE", sessionToken);
		Assert.assertFalse(verbose.contains("error"), "error when getting globalCollection in verbose mode");

	}


	@Test(description = "Create locale variable in rule, use it in configuration and report to analytics")
	public void addLocalVarInConfigurationRule() throws JSONException, IOException{
		String ruleString = "var myvar='stam';true";
		String confString = "{\"text\":myvar}";

		JSONObject json = new JSONObject(feature);
		json.put("name", "F2");
		String featureID = f.addFeature(seasonID, json.toString(), "ROOT", sessionToken);
		Assert.assertFalse(featureID.contains("error"), "Can't create feature");

		JSONObject jsonCR = new JSONObject(configuration);
		jsonCR.put("name", "CR2");
		jsonCR.put("configuration", confString);
		JSONObject ruleObj = new JSONObject();
		ruleObj.put("ruleString", ruleString);
		jsonCR.put("rule", ruleObj);

		String configID = f.addFeature(seasonID, jsonCR.toString(), featureID, sessionToken);
		Assert.assertFalse(configID.contains("error"), "Configuraton was not created: " + configID );

		//report title attribute to analytics (due to bug)
		JSONArray attributes = new JSONArray();
		JSONObject attr1 = new JSONObject();
		attr1.put("name", "text");
		attr1.put("type", "REGULAR");
		attributes.add(attr1);

		String response = an.addAttributesToAnalytics(featureID, BranchesRestApi.MASTER, attributes.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Attributes were not added to analytics" + response);

		String anResponse = an.getGlobalDataCollection(seasonID, branchID, "BASIC", sessionToken);
		Assert.assertTrue(validateAttributeInAnalytics(anResponse).size()==1, "Incorrect number of attributes in analytics");

		String display = an.getGlobalDataCollection(seasonID, branchID, "DISPLAY", sessionToken);
		Assert.assertTrue(an.validateDataCollectionByFeatureNames(display, anResponse).equals("true"));

		String verbose = an.getGlobalDataCollection(seasonID, branchID, "VERBOSE", sessionToken);
		Assert.assertFalse(verbose.contains("error"), "error when getting globalCollection in verbose mode");

	}

	@Test(description = "Create locale variable in rule, use it in configuration and report to analytics")
	public void addUtilityInConfigurationRule() throws JSONException, IOException{
		String ruleString = "function myfunc(){return \"stam\";}; true";
		String confString = "{\"text\":myfunc()}";

		JSONObject json = new JSONObject(feature);
		json.put("name", "F3");
		String featureID = f.addFeature(seasonID, json.toString(), "ROOT", sessionToken);
		Assert.assertFalse(featureID.contains("error"), "Can't create feature");

		JSONObject jsonCR = new JSONObject(configuration);
		jsonCR.put("name", "CR3");
		JSONObject ruleObj = new JSONObject();
		ruleObj.put("ruleString", ruleString);
		jsonCR.put("rule", ruleObj);

		jsonCR.put("configuration", confString);
		String configID = f.addFeature(seasonID, jsonCR.toString(), featureID, sessionToken);
		Assert.assertFalse(configID.contains("error"), "Configuraton was not created: " + configID );

		//report title attribute to analytics (due to bug)
		JSONArray attributes = new JSONArray();
		JSONObject attr1 = new JSONObject();
		attr1.put("name", "text");
		attr1.put("type", "REGULAR");
		attributes.add(attr1);

		String response = an.addAttributesToAnalytics(featureID, BranchesRestApi.MASTER, attributes.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Attributes were not added to analytics" + response);

		String anResponse = an.getGlobalDataCollection(seasonID, branchID, "BASIC", sessionToken);
		Assert.assertTrue(validateAttributeInAnalytics(anResponse).size()==1, "Incorrect number of attributes in analytics");

		String display = an.getGlobalDataCollection(seasonID, branchID, "DISPLAY", sessionToken);
		Assert.assertTrue(an.validateDataCollectionByFeatureNames(display, anResponse).equals("true"));

		String verbose = an.getGlobalDataCollection(seasonID, branchID, "VERBOSE", sessionToken);
		Assert.assertFalse(verbose.contains("error"), "error when getting globalCollection in verbose mode");
	}

	@Test(description = "Create locale variable in rule, use it in configuration and report to analytics")
	public void addLocalVarInFeatureRule() throws JSONException, IOException{
		String ruleString = "var myvar='stam';true";
		String confString = "{\"text\":myvar}";

		JSONObject json = new JSONObject(feature);
		json.put("name", "F6");
		json.put("minAppVersion", "0.1");
		JSONObject ruleObj = new JSONObject();
		ruleObj.put("ruleString", ruleString);
		json.put("rule", ruleObj);

		String featureID = f.addFeature(seasonID, json.toString(), "ROOT", sessionToken);
		Assert.assertFalse(featureID.contains("error"), "Can't create feature");

		JSONObject jsonCR = new JSONObject(configuration);
		jsonCR.put("name", "CR62");
		jsonCR.put("minAppVersion", "0.1");		
		jsonCR.put("configuration", confString);

		String configID = f.addFeature(seasonID, jsonCR.toString(), featureID, sessionToken);
		Assert.assertFalse(configID.contains("error"), "Configuraton was not created: " + configID );

		//report title attribute to analytics (due to bug)
		JSONArray attributes = new JSONArray();
		JSONObject attr1 = new JSONObject();
		attr1.put("name", "text");
		attr1.put("type", "REGULAR");
		attributes.add(attr1);

		String response = an.addAttributesToAnalytics(featureID, BranchesRestApi.MASTER, attributes.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Attributes were not added to analytics" + response);

		String anResponse = an.getGlobalDataCollection(seasonID, branchID, "BASIC", sessionToken);
		Assert.assertTrue(validateAttributeInAnalytics(anResponse).size()==1, "Incorrect number of attributes in analytics");

		String display = an.getGlobalDataCollection(seasonID, branchID, "DISPLAY", sessionToken);
		Assert.assertTrue(an.validateDataCollectionByFeatureNames(display, anResponse).equals("true"));

		String verbose = an.getGlobalDataCollection(seasonID, branchID, "VERBOSE", sessionToken);
		Assert.assertFalse(verbose.contains("error"), "error when getting globalCollection in verbose mode");

	}

	@Test(description = "Create locale variable in rule, use it in configuration and report to analytics")
	public void addUtilityInFeatureRule() throws JSONException, IOException{
		String ruleString = "function myfunc(){return \"stam\";}; true";
		String confString = "{\"text\":myfunc()}";

		JSONObject json = new JSONObject(feature);
		json.put("name", "F7");
		JSONObject ruleObj = new JSONObject();
		ruleObj.put("ruleString", ruleString);
		json.put("rule", ruleObj);

		String featureID = f.addFeature(seasonID, json.toString(), "ROOT", sessionToken);
		Assert.assertFalse(featureID.contains("error"), "Can't create feature");

		JSONObject jsonCR = new JSONObject(configuration);
		jsonCR.put("name", "CR73");

		jsonCR.put("configuration", confString);
		String configID = f.addFeature(seasonID, jsonCR.toString(), featureID, sessionToken);
		Assert.assertFalse(configID.contains("error"), "Configuraton was not created: " + configID );

		//report title attribute to analytics (due to bug)
		JSONArray attributes = new JSONArray();
		JSONObject attr1 = new JSONObject();
		attr1.put("name", "text");
		attr1.put("type", "REGULAR");
		attributes.add(attr1);

		String response = an.addAttributesToAnalytics(featureID, BranchesRestApi.MASTER, attributes.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Attributes were not added to analytics" + response);

		String anResponse = an.getGlobalDataCollection(seasonID, branchID, "BASIC", sessionToken);
		Assert.assertTrue(validateAttributeInAnalytics(anResponse).size()==1, "Incorrect number of attributes in analytics");

		String display = an.getGlobalDataCollection(seasonID, branchID, "DISPLAY", sessionToken);
		Assert.assertTrue(an.validateDataCollectionByFeatureNames(display, anResponse).equals("true"));

		String verbose = an.getGlobalDataCollection(seasonID, branchID, "VERBOSE", sessionToken);
		Assert.assertFalse(verbose.contains("error"), "error when getting globalCollection in verbose mode");
	}

	@Test(description = "Create global variable in rule, use it in configuration and report to analytics")
	public void addGlobalVarInFeatureRule() throws JSONException, IOException{
		String featureRuleString = "myvar='stam';true";
		String confString = "{\"text\":myvar}";
		String configRuleString = " typeof(myvar) !== undefined ? true : false";
		
		
		JSONObject json = new JSONObject(feature);
		json.put("name", "F11");
		JSONObject ruleObj = new JSONObject();
		ruleObj.put("ruleString", featureRuleString);
		json.put("rule", ruleObj);

		String featureID = f.addFeature(seasonID, json.toString(), "ROOT", sessionToken);
		Assert.assertFalse(featureID.contains("error"), "Can't create feature");

		JSONObject jsonCR = new JSONObject(configuration);
		jsonCR.put("configuration", confString);
		jsonCR.put("name", "CR11");
		ruleObj = new JSONObject();
		ruleObj.put("ruleString", configRuleString);
		jsonCR.put("rule", ruleObj);

		String configID = f.addFeature(seasonID, jsonCR.toString(), featureID, sessionToken);
		Assert.assertFalse(configID.contains("error"), "Configuraton was not created: " + configID );

		//report title attribute to analytics (due to bug)
		JSONArray attributes = new JSONArray();
		JSONObject attr1 = new JSONObject();
		attr1.put("name", "text");
		attr1.put("type", "REGULAR");
		attributes.add(attr1);

		String response = an.addAttributesToAnalytics(featureID, BranchesRestApi.MASTER, attributes.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Attributes were not added to analytics" + response);

		String anResponse = an.getGlobalDataCollection(seasonID, branchID, "BASIC", sessionToken);
		Assert.assertTrue(validateAttributeInAnalytics(anResponse).size()==1, "Incorrect number of attributes in analytics");

		String display = an.getGlobalDataCollection(seasonID, branchID, "DISPLAY", sessionToken);
		Assert.assertTrue(an.validateDataCollectionByFeatureNames(display, anResponse).equals("true"));

		String verbose = an.getGlobalDataCollection(seasonID, branchID, "VERBOSE", sessionToken);
		Assert.assertFalse(verbose.contains("error"), "error when getting globalCollection in verbose mode");

	}




	@AfterTest
	private void reset(){
		baseUtils.reset(productID, sessionToken);
	}


	private JSONArray validateAttributeInAnalytics(String analytics) throws JSONException{
		JSONObject json = new JSONObject(analytics);
		JSONObject analyticsDataCollection = json.getJSONObject("analyticsDataCollection");
		JSONArray featuresAttributesToAnalytics = analyticsDataCollection.getJSONArray("featuresAttributesForAnalytics");
		return featuresAttributesToAnalytics.getJSONObject(0).getJSONArray("attributes");

	}
}
