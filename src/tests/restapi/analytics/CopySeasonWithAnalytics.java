package tests.restapi.analytics;

import java.io.IOException;

import java.util.ArrayList;
import java.util.HashMap;

import org.apache.wink.json4j.JSONArray;
import org.apache.wink.json4j.JSONException;
import org.apache.wink.json4j.JSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import tests.com.ibm.qautils.FileUtils;
import tests.restapi.*;

public class CopySeasonWithAnalytics {
	protected String seasonID;
	protected String branchID;
	protected String branchInNewSeasonId;
	protected String seasonID2;
	protected String featureID1;
	protected String featureID2;
	protected String productID;
	protected String filePath;
	protected String m_url;
	protected ProductsRestApi p;
	protected SeasonsRestApi s;
	protected FeaturesRestApi f;
	protected AnalyticsRestApi an;
	protected InputSchemaRestApi schema;
	protected BranchesRestApi br;
	private String sessionToken = "";
	protected AirlockUtils baseUtils;
	@SuppressWarnings("rawtypes")
	protected HashMap featuresInSeason = new HashMap();
	@SuppressWarnings("rawtypes")
	protected HashMap attributesInSeason = new HashMap();

	@BeforeClass
	@Parameters({"url", "analyticsUrl", "translationsUrl", "configPath", "sessionToken", "userName", "userPassword", "appName", "productsToDeleteFile", "branchType"})
	public void init(String url, String analyticsUrl, String translationsUrl, String configPath, String sToken, String userName, String userPassword, String appName, String productsToDeleteFile, String branchType) throws IOException{
		m_url = url;
		filePath = configPath;
		p = new ProductsRestApi();
		p.setURL(m_url);
		s = new SeasonsRestApi();
		s.setURL(m_url);
		f = new FeaturesRestApi();
		f.setURL(m_url);
		an = new AnalyticsRestApi();
		an.setURL(analyticsUrl);

		schema = new InputSchemaRestApi();
		schema.setURL(m_url);

		br = new BranchesRestApi();
		br.setURL(m_url);
		baseUtils = new AirlockUtils(url, analyticsUrl, translationsUrl, configPath, sToken, userName, userPassword, appName, productsToDeleteFile);
		sessionToken = baseUtils.sessionToken;
		productID = baseUtils.createProduct();
		baseUtils.printProductToFile(productID);
		seasonID = baseUtils.createSeason(productID);
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

	@SuppressWarnings("unchecked")
	@Test (description="Add components and add to analytics")
	public void addComponents() throws Exception{
		// create schema
		String sch = schema.getInputSchema(seasonID, sessionToken);
		JSONObject jsonSchema = new JSONObject(sch);
		String schemaBody = FileUtils.fileToString(filePath + "inputSchemas/inputSchema_optionalField_unitsOfMeasure.txt", "UTF-8", false);
		jsonSchema.put("inputSchema", new JSONObject(schemaBody));
		String schemaResponse = schema.updateInputSchema(seasonID, jsonSchema.toString(), sessionToken);
		Assert.assertFalse(schemaResponse.contains("error"), "Schema was not added to the season" + schemaResponse);

		//add input field to analytics
		String response = an.getGlobalDataCollection(seasonID, branchID, "BASIC", sessionToken);
		String input = an.addInputFieldsToAnalytics(response, "context.device.locale");
		response = an.updateGlobalDataCollection(seasonID, branchID, input, sessionToken);
		Assert.assertFalse(response.contains("error"), "Analytics was not updated" + response);


		//add 2 features
		String feature1 = FileUtils.fileToString(filePath + "feature1.txt", "UTF-8", false);
		JSONObject json1 = new JSONObject(feature1);
		featureID1 = f.addFeatureToBranch(seasonID, branchID, feature1, "ROOT", sessionToken);
		Assert.assertFalse(featureID1.contains("error"), "Feature1 was not added to the season");
		featuresInSeason.put(json1.getString("namespace") + "." + json1.getString("name"), featureID1);

		String feature2 = FileUtils.fileToString(filePath + "feature2.txt", "UTF-8", false);
		JSONObject json2 = new JSONObject(feature2);
		featureID2 = f.addFeatureToBranch(seasonID, branchID, feature2, "ROOT", sessionToken);
		Assert.assertFalse(featureID2.contains("error"), "Feature2 was not added to the season");
		featuresInSeason.put(json2.getString("namespace") + "." + json2.getString("name"), featureID2);

		//add features ids to analytics featureOnOff
		response = an.getGlobalDataCollection(seasonID, branchID, "BASIC", sessionToken);
		input = an.addFeatureOnOff(response, featureID1);
		input = an.addFeatureOnOff(input, featureID2);
		response = an.updateGlobalDataCollection(seasonID, branchID, input, sessionToken);
		Assert.assertFalse(response.contains("error"), "The features were not added to analytics" + response);
		response = an.getGlobalDataCollection(seasonID, branchID, "VERBOSE", sessionToken);
		Assert.assertTrue(numberOfFeature(response)==2, "Incorrect number of features reported in analytics attributes" + response);


		//add configuration 1 to feature1
		String config = FileUtils.fileToString(filePath + "configuration_rule1.txt", "UTF-8", false);
		JSONObject jsonConfig = new JSONObject(config);
		JSONObject newConfiguration = new JSONObject();
		newConfiguration.put("color", "red");
		jsonConfig.put("configuration", newConfiguration);
		String configID1 = f.addFeatureToBranch(seasonID, branchID, jsonConfig.toString(), featureID1, sessionToken);
		Assert.assertFalse(configID1.contains("error"), "Configuration1 was not added to the season" + configID1);
		attributesInSeason.put(json1.getString("namespace") + "." + json1.getString("name"), "color");

		//add configuration2 to feature2
		config = FileUtils.fileToString(filePath + "configuration_rule2.txt", "UTF-8", false);
		JSONObject jsonConfig2 = new JSONObject(config);
		newConfiguration = new JSONObject();
		newConfiguration.put("size", "small");
		jsonConfig2.put("configuration", newConfiguration);
		String configID2 = f.addFeatureToBranch(seasonID, branchID, jsonConfig2.toString(), featureID2, sessionToken);
		Assert.assertFalse(configID2.contains("error"), "Configuration2 was not added to the season" + configID2);
		attributesInSeason.put(json2.getString("namespace") + "." + json2.getString("name"), "size");

		//add feature1 and attribute to analytics
		response = an.getGlobalDataCollection(seasonID, branchID, "BASIC", sessionToken);
		JSONArray attributes1 = new JSONArray();
		JSONObject attr1 = new JSONObject();
		attr1.put("name", "color");
		attr1.put("type", "REGULAR");
		attributes1.add(attr1);
		input = an.addFeaturesAttributesToAnalytics(response, featureID1, attributes1);
		response = an.updateGlobalDataCollection(seasonID, branchID, input, sessionToken);
		Assert.assertFalse(response.contains("error"), "Feature was not added to analytics" + response);

		//add feature2 and attribute to analytics
		response = an.getGlobalDataCollection(seasonID, branchID, "BASIC", sessionToken);
		JSONArray attributes2 = new JSONArray();
		JSONObject attr2 = new JSONObject();
		attr2.put("name", "size");
		attr2.put("type", "REGULAR");
		attributes2.add(attr2);
		input = an.addFeaturesAttributesToAnalytics(response, featureID2, attributes2);
		response = an.updateGlobalDataCollection(seasonID, branchID, input, sessionToken);
		Assert.assertFalse(response.contains("error"), "Feature was not added to analytics" + response);

		String anResponse = an.getGlobalDataCollection(seasonID, branchID, "BASIC", sessionToken);
		String display = an.getGlobalDataCollection(seasonID, branchID, "DISPLAY", sessionToken);
		Assert.assertTrue(an.validateDataCollectionByFeatureNames(display, anResponse).equals("true"));

	}

	@Test(dependsOnMethods="addComponents", description = "Create new season")
	public void createSeason() throws IOException, JSONException{
		JSONObject season = new JSONObject();
		season.put("minVersion", "5.0");
		seasonID2 =s.addSeason(productID,season.toString(), sessionToken);
		Assert.assertFalse(seasonID2.contains("error"), "New season was not created + " + seasonID2);
	}


	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Test(dependsOnMethods="createSeason", description = "Validate analytics in new season")
	public void validateAnalytics() throws IOException, JSONException,Exception{
		branchInNewSeasonId = "MASTER";
		if(!branchID.equals("MASTER")) {
			String allBranches = br.getAllBranches(seasonID2, sessionToken);
			JSONObject jsonBranch = new JSONObject(allBranches);
			branchInNewSeasonId = jsonBranch.getJSONArray("branches").getJSONObject(1).getString("uniqueId");
		}
		String analytics = an.getGlobalDataCollection(seasonID2, branchInNewSeasonId, "VERBOSE", sessionToken);
		JSONObject analyticsDataCollection = new JSONObject(analytics).getJSONObject("analyticsDataCollection");

		//validate inputFieldsToAnalytics
		Assert.assertTrue(analyticsDataCollection.getJSONArray("inputFieldsForAnalytics").getString(0).equals("context.device.locale"), "Incorrect input field. Expected context.device.locale, but received: " + analyticsDataCollection.getJSONArray("inputFieldsForAnalytics").getString(0));


		//validate featuresAndConfigurationsForAnalytics
		for(Object el: analyticsDataCollection.getJSONArray("featuresAndConfigurationsForAnalytics") ){
			JSONObject feature = new JSONObject(el);
			if (featuresInSeason.containsKey(feature.getString("name"))){
				//assert that feature id is different in the new season
				Assert.assertFalse(feature.getString("id").equals(featuresInSeason.get("name")), "The ids for feature " + feature.getString("name") + " are the same in 2 different seasons");
			} else {
				Assert.fail("Feature " + feature.getString("name") + " was not found in the new season");
			}

		}

		//validate attributes
		//get a list of  feature ids

		ArrayList oldIds = new ArrayList();
		oldIds.add(featureID1);
		oldIds.add(featureID2);

		for(Object el: analyticsDataCollection.getJSONArray("featuresAttributesForAnalytics") ){
			JSONObject analyticsFeature = new JSONObject(el);
			if (attributesInSeason.containsKey(analyticsFeature.getString("name"))){
				Assert.assertTrue(analyticsFeature.getJSONArray("attributes").getJSONObject(0).getString("name").equals(attributesInSeason.get(analyticsFeature.getString("name"))));
				Assert.assertFalse(oldIds.contains(analyticsFeature.getString("id")), "Id was not changed in attribute analytics for feature " + analyticsFeature.getString("name"));
			} else {
				Assert.fail("Feature " + analyticsFeature.getString("name") + " was not found in the new season");
			}

		}

		//check display features
		String display = an.getGlobalDataCollection(seasonID2, branchInNewSeasonId, "DISPLAY", sessionToken);
		Assert.assertTrue(an.validateDataCollectionByFeatureNames(display, analytics).equals("true"));


	}


	@Test(dependsOnMethods="validateAnalytics", description = "Validate runtime in new season")
	public void validateRuntime() throws InterruptedException, IOException, JSONException{
		//need to add feature in production to receive correct runtime files response
		String dateFormat = an.setDateFormat();

		String feature = FileUtils.fileToString(filePath + "feature3.txt", "UTF-8", false);
		String response = f.addFeatureToBranch(seasonID2, branchInNewSeasonId, feature, "ROOT", sessionToken);
		Assert.assertFalse(response.contains("error"), "Test should pass, but instead failed: " + response );

		an.setSleep();
		RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getDevelopmentBranchFileDateModification(m_url, productID, seasonID2, branchInNewSeasonId, dateFormat, sessionToken);
		Assert.assertTrue(responseDev.code ==200, "Runtime development feature file was not updated");
		Assert.assertTrue(ifRuntimeContainsField(responseDev.message, "context.device.locale"), "The field \"context.device.locale\" was not found in the runtime development file");

		JSONObject root = RuntimeDateUtilities.getFeaturesList(responseDev.message);

		//validate featuresAndConfigurationsForAnalytics and configAttributesToAnalytics
		String analytics = an.getGlobalDataCollection(seasonID2, branchInNewSeasonId, "VERBOSE", sessionToken);
		JSONObject analyticsDataCollection = new JSONObject(analytics).getJSONObject("analyticsDataCollection");

		for(Object el: analyticsDataCollection.getJSONArray("featuresAndConfigurationsForAnalytics") ){
			JSONObject feat = new JSONObject(el);
			Assert.assertTrue(validateSentToAnalytics(root, feat.getString("id")), "The field \"sendToAnalytics\" was not updated for feature feat.getString(\"id\")");
			Assert.assertTrue(validateFeatureInRuntime(root, feat.getString("id")), "The field \"configAttributesToAnalytics\" was not updated for feature feat.getString(\"id\")");

		}

	}

	private boolean validateFeatureInRuntime(JSONObject root, String id ) throws JSONException{
		JSONArray features = root.getJSONArray("features");
		for (Object f : features){
			JSONObject feature = new JSONObject(f);
			//find feature
			if (feature.getString("uniqueId").equals(id)){
				if (feature.containsKey("configAttributesForAnalytics"))
					return true;
			}
		}
		return false;
	}

	private boolean validateSentToAnalytics(JSONObject root, String featureId) throws JSONException{
		JSONArray features = root.getJSONArray("features");
		for (Object f : features) {
			JSONObject feature = new JSONObject(f);
			if (feature.getString("uniqueId").equals(featureId)) {
				if (feature.has("sendToAnalytics"))
					return feature.getBoolean("sendToAnalytics");
			}
		}
		return false;
	}

	private boolean ifRuntimeContainsField(String input, String field){

		try{
			JSONObject json = new JSONObject(input);
			if (json.containsKey("inputFieldsForAnalytics")){
				JSONArray inputFields = json.getJSONArray("inputFieldsForAnalytics");
				for (Object s : inputFields) {
					if (s.equals(field))
						return true;
				}
				//Arrays.asList(inputFields).contains(field);
				return false;
			} else {

				return false;
			}
		} catch (Exception e){
			return false;
		}
	}
	private int numberOfFeature(String input){

		try{
			JSONObject json = new JSONObject(input);
			JSONObject analytics = json.getJSONObject("analyticsDataCollection");
			JSONArray inputFields = analytics.getJSONArray("featuresAndConfigurationsForAnalytics");
			return inputFields.size();

		} catch (Exception e){
			return -1;
		}
	}

	@AfterTest
	private void reset(){
		baseUtils.reset(productID, sessionToken);
	}
}
