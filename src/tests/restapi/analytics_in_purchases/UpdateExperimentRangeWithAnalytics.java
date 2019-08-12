package tests.restapi.analytics_in_purchases;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.lang3.RandomStringUtils;
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
import tests.restapi.ExperimentsRestApi;
import tests.restapi.InAppPurchasesRestApi;
import tests.restapi.InputSchemaRestApi;
import tests.restapi.RuntimeDateUtilities;
import tests.restapi.RuntimeRestApi;
import tests.restapi.SeasonsRestApi;

public class UpdateExperimentRangeWithAnalytics {
	protected String productID;
	protected String seasonID1;
	protected String seasonID2;
	private String experimentID;
	private String branchID1;
	private String branchID2;
	private String variantID1;
	protected String filePath;
	protected String m_url;
	private String sessionToken = "";
	private AirlockUtils baseUtils;
	private ExperimentsRestApi exp ;
	private BranchesRestApi br ;
	private SeasonsRestApi s;
	protected InAppPurchasesRestApi purchasesApi;
	private InputSchemaRestApi schema;
	protected AnalyticsRestApi an;
	
	@BeforeClass
	@Parameters({"url", "analyticsUrl", "translationsUrl", "configPath", "sessionToken", "userName", "userPassword", "appName", "productsToDeleteFile"})
	public void init(String url, String analyticsUrl, String translationsUrl, String configPath, String sToken, String userName, String userPassword, String appName, String productsToDeleteFile) throws Exception{
		m_url = url;
		filePath = configPath ;
		s = new SeasonsRestApi();
		s.setURL(url);
		br = new BranchesRestApi();
		br.setURL(m_url);
		exp = new ExperimentsRestApi();
		exp.setURL(analyticsUrl); 
		purchasesApi = new InAppPurchasesRestApi();
		purchasesApi.setURL(m_url);
	    schema = new InputSchemaRestApi();
	    schema.setURL(m_url);
		an = new AnalyticsRestApi();
		an.setURL(analyticsUrl);
		
		baseUtils = new AirlockUtils(m_url, analyticsUrl, translationsUrl, configPath, sToken, userName, userPassword, appName, productsToDeleteFile);
		sessionToken = baseUtils.sessionToken;

		productID = baseUtils.createProduct();
		baseUtils.printProductToFile(productID);
		
	}
	
	/*
	* 2 seasons participate in experiment
	* reported to analytics from both seasons
	* delete the first season
	* check analytics
	*/
	
	@Test (description="Add seasons and schema")
	public void addSeasons() throws Exception{
		String season1 = "{\"minVersion\":\"1.0\"}";
		seasonID1 = s.addSeason(productID, season1, sessionToken);
		
		String sch = schema.getInputSchema(seasonID1, sessionToken);
        JSONObject jsonSchema = new JSONObject(sch);
        String schemaBody = FileUtils.fileToString(filePath + "inputSchemas/inputSchema_optionalField_unitsOfMeasure.txt", "UTF-8", false);
        jsonSchema.put("inputSchema", new JSONObject(schemaBody));
        String response = schema.updateInputSchema(seasonID1, jsonSchema.toString(), sessionToken);
        Assert.assertFalse(response.contains("error"), "Schema was not added to the season" + response);
		
  		String season2 = "{\"minVersion\":\"2.0\"}";
		seasonID2 = s.addSeason(productID, season2, sessionToken);
		
	    branchID1 = addBranch("branch1", seasonID1);
	    Assert.assertFalse(branchID1.contains("error"), "Branch from season1 was not created: " + branchID1);
		branchID2 = addBranch("branch1", seasonID2);
		Assert.assertFalse(branchID2.contains("error"), "Branch from season2 was not created: " + branchID2);


	}
	
	@Test (dependsOnMethods="addSeasons", description ="Set different quota in seasons") 
	public void setQuota () throws IOException, JSONException {

	
		String response = an.updateQuota(seasonID1, 3, sessionToken);
		Assert.assertFalse(response.contains("error"), "Quota for season1 was not set " + response);
		response = an.updateQuota(seasonID2, 5, sessionToken);
		Assert.assertFalse(response.contains("error"), "Quota for season2 was not set " + response);
	}

	
	@Test (dependsOnMethods="setQuota", description ="Add items to analytics in both seasons") 
	public void reportToAnalytics () throws IOException, JSONException, InterruptedException {
		
		JSONArray inputFields1 = new JSONArray();		
		inputFields1.put("context.device.datetime");		
		JSONArray inputFields2 = new JSONArray();	
		inputFields2.put("context.device.datetime");
		inputFields2.put("context.device.localeCountryCode");

		String response = an.updateInputFieldToAnalytics(seasonID1, BranchesRestApi.MASTER,  inputFields1.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Analytics was not updated in season1: " + response);
		response = an.updateInputFieldToAnalytics(seasonID1, branchID1,  inputFields2.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Analytics was not updated in season1 in branch1: " + response);

		JSONArray inputFields3 = new JSONArray();		
		inputFields3.put("context.device.locale");		
		JSONArray inputFields4 = new JSONArray();	
		inputFields4.put("context.device.locale");		
		inputFields4.put("context.device.localeLanguage");
		response = an.updateInputFieldToAnalytics(seasonID2, BranchesRestApi.MASTER,  inputFields3.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Analytics was not updated in season2: " + response);
		response = an.updateInputFieldToAnalytics(seasonID2, branchID2,  inputFields4.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Analytics was not updated in season2 in branch2: " + response);
		
		String entitlement = FileUtils.fileToString(filePath + "purchases/inAppPurchase1.txt", "UTF-8", false);
		JSONObject jsonF = new JSONObject(entitlement);

		//add entitlements to analytics in season1
		jsonF.put("name", "EntitlementInSeason1");
		String entitlementID1 = purchasesApi.addPurchaseItem(seasonID1, jsonF.toString(), "ROOT", sessionToken);
		an.addFeatureToAnalytics(entitlementID1, BranchesRestApi.MASTER, sessionToken);
		jsonF.put("name", "EntitlementInBranch1");
		String entitlementID2 = purchasesApi.addPurchaseItemToBranch(seasonID1, branchID1, jsonF.toString(), "ROOT", sessionToken);
		an.addFeatureToAnalytics(entitlementID2, branchID1, sessionToken);
		
		//add entitlements to analytics in season2
		jsonF.put("name", "EntitlementInSeason2");
		String entitlementID3 = purchasesApi.addPurchaseItem(seasonID2, jsonF.toString(), "ROOT", sessionToken);
		an.addFeatureToAnalytics(entitlementID3, BranchesRestApi.MASTER, sessionToken);
		jsonF.put("name", "EntitlementInBranch2");
		String entitlementID4 = purchasesApi.addPurchaseItemToBranch(seasonID2, branchID2, jsonF.toString(), "ROOT", sessionToken);
		an.addFeatureToAnalytics(entitlementID4, branchID2, sessionToken);
		
	}
	

	@Test (dependsOnMethods="reportToAnalytics", description ="Add experiment") 
	public void addExperiment () throws Exception{
		String dateFormat = an.setDateFormat();
		
		//experiment includes only season1
		String experiment = FileUtils.fileToString(filePath + "experiments/experiment1.txt", "UTF-8", false);
		JSONObject expJson = new JSONObject(experiment);
		expJson.put("name", "experiment."+RandomStringUtils.randomAlphabetic(5));
		expJson.put("stage", "PRODUCTION");
		expJson.put("minVersion", "0.5");
		expJson.put("maxVersion", "1.8");
		expJson.put("enabled", false);
		
		experimentID = exp.createExperiment(productID, expJson.toString(), sessionToken);
		Assert.assertFalse(experimentID.contains("error"), "Experiment was not created: " + experimentID);

		variantID1 = addVariant("variant1", "branch1");
		Assert.assertFalse(variantID1.contains("error"), "Variant was not created: " + variantID1);

		//enable experiment 
		String airlockExperiment = exp.getExperiment(experimentID, sessionToken);
		Assert.assertFalse(airlockExperiment.contains("error"), "Experiment was not found: " + experimentID);

		expJson = new JSONObject(airlockExperiment);
		expJson.put("enabled", true);
		
		String response = exp.updateExperiment(experimentID, expJson.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Experiment was not updated: " + response);		

		//experiment quota must be maximal from season1
		JSONObject jsonResp = new JSONObject(an.getExperimentQuota(experimentID, sessionToken));
		Assert.assertTrue(jsonResp.getInt("analyticsQuota")==3, "Incorrect quota in experiment");

		String analyticsReponse = an.getExperimentGlobalDataCollection(experimentID, sessionToken);
		Assert.assertTrue(analyticsFeaturesInExperiment(analyticsReponse)==2, "Incorrect number of entitlements in experiment analytics");
		Assert.assertTrue(analyticsInputFieldsInExperiment(analyticsReponse)==2, "Incorrect number of input fields in experiment analytics");

		an.setSleep();
		RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getDevelopmentBranchFileDateModification(m_url, productID, seasonID1, BranchesRestApi.MASTER, dateFormat, sessionToken);
		Assert.assertTrue(responseDev.code ==200, "Runtime development file was not updated");
		JSONObject experimentAnalytics = getExperimentAnalytics(responseDev.message);
		String err = validateInputFieldsList(experimentAnalytics, new String[]{"context.device.datetime", "context.device.localeCountryCode"});
		Assert.assertTrue(err == null, err);		
		err = validateEntitlementsToAnalytics(experimentAnalytics, new String[]{"ns1.EntitlementInBranch1", "ns1.EntitlementInSeason1"});
		Assert.assertTrue(err == null, err);

	
		responseDev = RuntimeDateUtilities.getDevelopmentBranchFileDateModification(m_url, productID, seasonID1, "MASTER", dateFormat, sessionToken);
		Assert.assertTrue(responseDev.code ==200, "Runtime development file was not updated");
		experimentAnalytics = getExperimentAnalytics(responseDev.message);
		err = validateInputFieldsList(experimentAnalytics, new String[]{"context.device.datetime", "context.device.localeCountryCode"});
		Assert.assertTrue(err == null, err);		
		err = validateEntitlementsToAnalytics(experimentAnalytics, new String[]{"ns1.EntitlementInBranch1", "ns1.EntitlementInSeason1"});
		Assert.assertTrue(err == null, err);

	}
	
	@Test (dependsOnMethods="addExperiment", description ="Increase experiment range to include season2") 
	public void increaseExperimentRange () throws Exception {
		String dateFormat = an.setDateFormat();
		
		//update experiment range to include season2
		JSONObject expJson = new JSONObject(exp.getExperiment(experimentID, sessionToken));
		expJson.put("maxVersion", "3.0");
		String response = exp.updateExperiment(experimentID, expJson.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Experiment was not updated: " + response);
		
		//experiment quota must be maximal from season2
		JSONObject jsonResp = new JSONObject(an.getExperimentQuota(experimentID, sessionToken));
		Assert.assertTrue(jsonResp.getInt("analyticsQuota")==5, "Incorrect quota in experiment");
		
		String analyticsReponse = an.getExperimentGlobalDataCollection(experimentID, sessionToken);
		Assert.assertTrue(analyticsFeaturesInExperiment(analyticsReponse)==4, "Incorrect number of in experiment analytics");
		Assert.assertTrue(analyticsInputFieldsInExperiment(analyticsReponse)==4, "Incorrect number of input fields in experiment analytics");

		
		an.setSleep();
		//season1
		RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getDevelopmentBranchFileDateModification(m_url, productID, seasonID1, BranchesRestApi.MASTER, dateFormat, sessionToken);
		Assert.assertTrue(responseDev.code ==200, "Runtime development  file was not updated");
		JSONObject experimentAnalytics = getExperimentAnalytics(responseDev.message);
		String err = validateInputFieldsList(experimentAnalytics, new String[]{"context.device.locale", "context.device.localeLanguage", "context.device.localeCountryCode", "context.device.datetime"});
		Assert.assertTrue(err == null, err);		
		err = validateEntitlementsToAnalytics(experimentAnalytics, new String[]{"ns1.EntitlementInBranch2", "ns1.EntitlementInSeason1", "ns1.EntitlementInBranch1", "ns1.EntitlementInSeason2"});
		Assert.assertTrue(err == null, err);

	
		responseDev = RuntimeDateUtilities.getDevelopmentBranchFileDateModification(m_url, productID, seasonID1, "MASTER", dateFormat, sessionToken);
		Assert.assertTrue(responseDev.code ==200, "Runtime development file was not updated");
		experimentAnalytics = getExperimentAnalytics(responseDev.message);
		err = validateInputFieldsList(experimentAnalytics, new String[]{"context.device.localeCountryCode", "context.device.locale", "context.device.localeLanguage", "context.device.datetime"});
		Assert.assertTrue(err == null, err);		
		err = validateEntitlementsToAnalytics(experimentAnalytics, new String[]{"ns1.EntitlementInBranch2", "ns1.EntitlementInSeason1", "ns1.EntitlementInBranch1", "ns1.EntitlementInSeason2"});
		Assert.assertTrue(err == null, err);

		//season2
		responseDev = RuntimeDateUtilities.getDevelopmentBranchFileDateModification(m_url, productID, seasonID2, BranchesRestApi.MASTER, dateFormat, sessionToken);
		Assert.assertTrue(responseDev.code ==200, "Runtime development file was not updated");
		experimentAnalytics = getExperimentAnalytics(responseDev.message);
		err = validateInputFieldsList(experimentAnalytics, new String[]{"context.device.locale", "context.device.localeLanguage", "context.device.localeCountryCode", "context.device.datetime"});
		Assert.assertTrue(err == null, err);		
		err = validateEntitlementsToAnalytics(experimentAnalytics, new String[]{"ns1.EntitlementInBranch2", "ns1.EntitlementInSeason1", "ns1.EntitlementInBranch1", "ns1.EntitlementInSeason2"});
		Assert.assertTrue(err == null, err);

	
		responseDev = RuntimeDateUtilities.getDevelopmentBranchFileDateModification(m_url, productID, seasonID2, "MASTER", dateFormat, sessionToken);
		Assert.assertTrue(responseDev.code ==200, "Runtime development file was not updated");
		experimentAnalytics = getExperimentAnalytics(responseDev.message);
		err = validateInputFieldsList(experimentAnalytics, new String[]{"context.device.localeCountryCode", "context.device.locale", "context.device.localeLanguage", "context.device.datetime"});
		Assert.assertTrue(err == null, err);		
		err = validateEntitlementsToAnalytics(experimentAnalytics, new String[]{"ns1.EntitlementInBranch2", "ns1.EntitlementInSeason1", "ns1.EntitlementInBranch1", "ns1.EntitlementInSeason2"});
		Assert.assertTrue(err == null, err);

	}
	
	@Test (dependsOnMethods="increaseExperimentRange", description ="Decrease experiment range to exclude season1") 
	public void decreaseExperimentRange () throws Exception {
		String dateFormat = an.setDateFormat();
		
		//update experiment range to exclude season1
		JSONObject expJson = new JSONObject(exp.getExperiment(experimentID, sessionToken));
		expJson.put("minVersion", "2.5");
		String response = exp.updateExperiment(experimentID, expJson.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Experiment was not updated: " + response);
	
		String analyticsReponse = an.getExperimentGlobalDataCollection(experimentID, sessionToken);
		Assert.assertTrue(analyticsFeaturesInExperiment(analyticsReponse)==2, "Incorrect number of entitlements in experiment analytics");
		Assert.assertTrue(analyticsInputFieldsInExperiment(analyticsReponse)==2, "Incorrect number of input fields in experiment analytics");

		an.setSleep();
		//season2
		RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getDevelopmentBranchFileDateModification(m_url, productID, seasonID2, BranchesRestApi.MASTER, dateFormat, sessionToken);
		Assert.assertTrue(responseDev.code ==200, "Runtime development file was not updated");
		JSONObject experimentAnalytics = getExperimentAnalytics(responseDev.message);
		String err = validateInputFieldsList(experimentAnalytics, new String[]{"context.device.locale", "context.device.localeLanguage"});
		Assert.assertTrue(err == null, err);		
		err = validateEntitlementsToAnalytics(experimentAnalytics, new String[]{"ns1.EntitlementInBranch2", "ns1.EntitlementInSeason2"});
		Assert.assertTrue(err == null, err);

	
		responseDev = RuntimeDateUtilities.getDevelopmentBranchFileDateModification(m_url, productID, seasonID2, "MASTER", dateFormat, sessionToken);
		Assert.assertTrue(responseDev.code ==200, "Runtime development file was not updated");
		experimentAnalytics = getExperimentAnalytics(responseDev.message);
		err = validateInputFieldsList(experimentAnalytics, new String[]{"context.device.locale", "context.device.localeLanguage"});
		Assert.assertTrue(err == null, err);		
		err = validateEntitlementsToAnalytics(experimentAnalytics, new String[]{"ns1.EntitlementInBranch2", "ns1.EntitlementInSeason2"});
		Assert.assertTrue(err == null, err);
		
		//season1
		responseDev = RuntimeDateUtilities.getDevelopmentBranchFileDateModification(m_url, productID, seasonID1, BranchesRestApi.MASTER, dateFormat, sessionToken);
		JSONObject runtimeContentJson = new JSONObject(responseDev.message);
		Assert.assertTrue(runtimeContentJson.getJSONObject("experiments").getJSONArray("experiments").size()==0, "Experiment was not updated in season1 runtime");
		
		responseDev = RuntimeDateUtilities.getDevelopmentBranchFileDateModification(m_url, productID, seasonID1, "MASTER", dateFormat, sessionToken);
		runtimeContentJson = new JSONObject(responseDev.message);
		Assert.assertTrue(runtimeContentJson.getJSONObject("experiments").getJSONArray("experiments").size()==0, "Experiment was not updated in season1 runtime");

	}
	
	private String addBranch(String branchName, String season) throws JSONException, IOException{
		String branch = FileUtils.fileToString(filePath + "experiments/branch1.txt", "UTF-8", false);
		JSONObject branchJson = new JSONObject(branch);
		branchJson.put("name", branchName);
		return br.createBranch(season, branchJson.toString(), BranchesRestApi.MASTER, sessionToken);

	}
	
	private String addVariant(String variantName, String branchName) throws IOException, JSONException{
		String variant = FileUtils.fileToString(filePath + "experiments/variant1.txt", "UTF-8", false);
		JSONObject variantJson = new JSONObject(variant);
		variantJson.put("name", variantName);
		variantJson.put("stage", "PRODUCTION");
		variantJson.put("branchName", branchName);
		return exp.createVariant(experimentID, variantJson.toString(), sessionToken);

	}
	
	//assuming there is only one experiment
	private static JSONObject getExperimentAnalytics(String runtimeFileContext) throws JSONException{
		JSONObject res = new JSONObject();
		
		try{
			JSONObject runtimeContentJson = new JSONObject(runtimeFileContext);
			if (runtimeContentJson.containsKey("experiments")){
				JSONObject expeimentsObj = runtimeContentJson.getJSONObject("experiments");
				if (expeimentsObj.containsKey("experiments")){				
					JSONArray expeiments = expeimentsObj.getJSONArray("experiments");
					if (expeiments.size()==0) {
						res.put("error", "No expriment in experiments array.");
						return res;
					}
					JSONObject expeiment = expeiments.getJSONObject(0);
					if (expeiment.containsKey("analytics")) {
						return expeiment.getJSONObject("analytics");
					}
					else {
						res.put("error", "Response doesn't contain experiments object");
						return res;
					}
				}
				else {
					res.put("error", "Experiments object not contain analytics");
					return res;
				}
			} else {
				res.put("error", "Response doesn't contain experiments list");
				return res;
			}
		} catch (Exception e){
				res.put("error", "Response is not a valid json");
				return res;
		}
		

	}
	
	private String validateEntitlementsToAnalytics(JSONObject experimentAnalytics, String[] expectedEntitlements) {
		if (!experimentAnalytics.containsKey("featuresAndConfigurationsForAnalytics"))
			return "Missing 'featuresAndConfigurationsForAnalytics' in experiment analytics";
		
		try {
			JSONArray featuresAndConfigurationsForAnalytics = experimentAnalytics.getJSONArray("featuresAndConfigurationsForAnalytics");			
			List<String> exisitngfeaturesForAnalyticsList =  new LinkedList<String>();
			for (int i=0; i<featuresAndConfigurationsForAnalytics.size(); i++) {
				String s = featuresAndConfigurationsForAnalytics.getString(i);
				exisitngfeaturesForAnalyticsList.add(s);
			}
		    Collections.sort(exisitngfeaturesForAnalyticsList);
		    List<String> expectedFeaturesList =  Arrays.asList(expectedEntitlements);
		    Collections.sort(expectedFeaturesList);
		    if (!exisitngfeaturesForAnalyticsList.equals(expectedFeaturesList)) {
		    	return "entitlements And Configurations For Analytics are not as expected";
		    }
			
		} catch (JSONException je) {
			return je.getMessage();
		}
		
		return null;
	}
	
	private String validateInputFieldsList(JSONObject experimentAnalytics, String[] expectedFields) {
		if (!experimentAnalytics.containsKey("inputFieldsForAnalytics"))
			return "Missing 'inputFieldsForAnalytics' in experiment analytics";
		try {
			JSONArray inputFieldsForAnalytics = experimentAnalytics.getJSONArray("inputFieldsForAnalytics");			
			List<String> exisitngFiledsList =  new LinkedList<String>();
			for (int i=0; i<inputFieldsForAnalytics.size(); i++) {
				String s = inputFieldsForAnalytics.getString(i);
				exisitngFiledsList.add(s);
			}
		    Collections.sort(exisitngFiledsList);
		    List<String> expectedFieldsList =  Arrays.asList(expectedFields);
		    Collections.sort(expectedFieldsList);
		    if (!exisitngFiledsList.equals(expectedFieldsList)) {
		    	return "input fields for analytics are not as expected";
		    }
			
		} catch (JSONException je) {
			return je.getMessage();
		}
		
		return null;
	}
	
	private int analyticsFeaturesInExperiment(String analytics) throws JSONException{
		JSONObject json = new JSONObject(analytics);
		return json.getJSONArray("analyticsDataCollectionByFeatureNames").size();
	}
	
	private int analyticsInputFieldsInExperiment(String analytics) throws JSONException{
		JSONObject json = new JSONObject(analytics);
		return json.getJSONArray("inputFieldsForAnalytics").size();
	}
	
	@AfterTest
	private void reset(){
		baseUtils.reset(productID, sessionToken);
	}
}
