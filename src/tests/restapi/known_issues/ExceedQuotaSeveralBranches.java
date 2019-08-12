//package tests.restapi.analytics_in_branch;
package tests.restapi.known_issues;

//known issue: exceed quota design issue. bug#10,15 Q4-2017

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.wink.json4j.JSONArray;
import org.apache.wink.json4j.JSONException;
import org.apache.wink.json4j.JSONObject;
import org.testng.Assert;
import org.testng.annotations.*;

import tests.com.ibm.qautils.FileUtils;
import tests.restapi.*;

import java.io.IOException;

public class ExceedQuotaSeveralBranches {
	protected String seasonID;
	protected String seasonID2;
	protected String branchID;
	protected String branchID2;
	protected String branchID3;
	protected String variantID;
	protected String experimentID;
	protected String productID;
	protected String featureID1;
	protected String featureID2;
	protected String featureID3;
	protected String configID1;
	protected String configID2;
	protected String filePath;
	protected String m_url;
	protected ProductsRestApi p;
	protected FeaturesRestApi f;
	protected AnalyticsRestApi an;
	protected BranchesRestApi br ;
	protected SeasonsRestApi s;
	protected InputSchemaRestApi schema;
	protected ExperimentsRestApi exp;
	private String sessionToken = "";
	protected AirlockUtils baseUtils;
	private String m_analyticsUrl;
	
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
		br = new BranchesRestApi();
		br.setURL(m_url);
		s = new SeasonsRestApi();
		s.setURL(m_url);
		exp = new ExperimentsRestApi();
		exp.setURL(analyticsUrl);
        
		baseUtils = new AirlockUtils(url, analyticsUrl, translationsUrl, configPath, sToken, userName, userPassword, appName, productsToDeleteFile);
		sessionToken = baseUtils.sessionToken;
		productID = baseUtils.createProduct();
		baseUtils.printProductToFile(productID);
		seasonID = baseUtils.createSeason(productID);
		m_analyticsUrl = analyticsUrl;
		try {
			JSONObject exp = baseUtils.createBranchInProdExperiment(analyticsUrl);
			branchID = exp.getString("brId");
			branchID2= baseUtils.addBranchFromBranch("branch2",branchID,seasonID);
			String season = FileUtils.fileToString(filePath + "season3.txt", "UTF-8", false);
			seasonID2 = s.addSeason(productID, season, sessionToken);
			String response = an.updateQuota(seasonID2, 2, sessionToken);
			Assert.assertFalse(response.contains("error"), "Quota was not returned " + response);

		}catch (Exception e){
			branchID = null;
		}
	}
	
	//use separate api-s to add to analytics
	
	@Test ( description="Add input schema to the season")
	public void addSchema() throws Exception{
		String sch = schema.getInputSchema(seasonID, sessionToken);
        JSONObject jsonSchema = new JSONObject(sch);
        String schemaBody = FileUtils.fileToString(filePath + "inputSchemas/inputSchema_update_device_locale_to_production.txt", "UTF-8", false);
        jsonSchema.put("inputSchema", new JSONObject(schemaBody));
        String response = schema.updateInputSchema(seasonID, jsonSchema.toString(), sessionToken);
        Assert.assertFalse(response.contains("error"), "Schema was not added to the season" + response);
	}

	@Test (dependsOnMethods="addSchema", description="Set quota to 3")
	public void updateQuota() throws IOException, JSONException, InterruptedException{
		String response = an.updateQuota(seasonID, 3, sessionToken);
		Assert.assertFalse(response.contains("error"), "Quota was not returned " + response);				
	}
	
	@Test (dependsOnMethods="updateQuota", description="Add 1 input field, 1 feature, 1 attribute")
	public void addToAnalytics() throws Exception{
		//add input field
		JSONArray inputFields = new JSONArray();
		inputFields.put("context.device.locale");
		String response = an.updateInputFieldToAnalytics(seasonID, branchID,  inputFields.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Analytics was not updated" + response);
		
		//add feature in production
		String feature1 = FileUtils.fileToString(filePath + "feature1.txt", "UTF-8", false);
		JSONObject jsonF = new JSONObject(feature1);
		jsonF.put("stage", "PRODUCTION");
		featureID1 = f.addFeatureToBranch(seasonID, BranchesRestApi.MASTER, jsonF.toString(), "ROOT", sessionToken);
		Assert.assertFalse(featureID1.contains("error"), "Feature was not added to the season" + featureID1);
		
		response = an.addFeatureToAnalytics(featureID1, BranchesRestApi.MASTER, sessionToken);
		Assert.assertFalse(response.contains("error"), "Incorrect globalDataCollection response");	
		
		//add attribute
		String config = FileUtils.fileToString(filePath + "configuration_rule1.txt", "UTF-8", false);
		JSONObject jsonConfig = new JSONObject(config);
		JSONObject newConfiguration = new JSONObject();
		newConfiguration.put("color", "red");
		newConfiguration.put("size", "small");
		jsonConfig.put("configuration", newConfiguration);
		configID1 = f.addFeatureToBranch(seasonID, BranchesRestApi.MASTER, jsonConfig.toString(), featureID1, sessionToken);
		Assert.assertFalse(configID1.contains("error"), "Configuration1 was not added to the season" + configID1);

		JSONArray attributes = new JSONArray();
		JSONObject attr1 = new JSONObject();
		attr1.put("name", "color");
		attr1.put("type", "REGULAR");
		attributes.add(attr1);
		
		response = an.addAttributesToAnalytics(featureID1, BranchesRestApi.MASTER, attributes.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Feature was not added to analytics" + response);

		String respWithQuota = an.getGlobalDataCollection(seasonID, branchID, "DISPLAY", sessionToken);
		Assert.assertTrue(getDevelopmentItemsReportedToAnalytics(respWithQuota)==3, "Incorrect number of development items"); //feature1+attribute+inputfield
		Assert.assertTrue(getProductionItemsReportedToAnalytics(respWithQuota)==3, "Incorrect number of production items"); //feature1+attribute+inputfield
		respWithQuota = an.getGlobalDataCollection(seasonID,  BranchesRestApi.MASTER, "DISPLAY", sessionToken);
		Assert.assertTrue(getDevelopmentItemsReportedToAnalytics(respWithQuota)==2, "Incorrect number of development items"); //feature1+attribute+inputfield
		Assert.assertTrue(getProductionItemsReportedToAnalytics(respWithQuota)==2, "Incorrect number of production items"); //feature1+attribute+inputfield


	}
	@Test (dependsOnMethods="addToAnalytics", description="Add attribute - exceeds the quota")
	public void removeFromMasterAddInBranch() throws JSONException{
		JSONArray inputFields = new JSONArray();
		inputFields.put("context.device.locale");
		inputFields.put("context.device.osVersion");
		String response = an.updateInputFieldToAnalytics(seasonID, branchID,  inputFields.toString(), sessionToken);
		Assert.assertTrue(response.contains("The maximum number of items in production to send to analytics was exceeded"), "Analytics was updated" + response);

		an.deleteFeatureFromAnalytics(featureID1,BranchesRestApi.MASTER,sessionToken);

		response = an.updateInputFieldToAnalytics(seasonID, branchID,  inputFields.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Analytics was not updated" + response);

	}

	@Test (dependsOnMethods="removeFromMasterAddInBranch", description="Add attribute - exceeds the quota")
	public void removeFromBranchAddInMaster() throws JSONException{
		String response = an.addFeatureToAnalytics(featureID1, BranchesRestApi.MASTER, sessionToken);
		Assert.assertTrue(response.contains("The maximum number of items in production to send to analytics for experiment experimentProd was exceeded"), "Analytics was updated" + response);

		JSONArray inputFields = new JSONArray();
		inputFields.put("context.device.locale");
		response = an.updateInputFieldToAnalytics(seasonID, branchID,  inputFields.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Analytics was not updated" + response);

		response = an.addFeatureToAnalytics(featureID1, BranchesRestApi.MASTER, sessionToken);
		Assert.assertFalse(response.contains("error"), "Analytics was not updated" + response);
	}
	@Test (dependsOnMethods="removeFromBranchAddInMaster", description="Add attribute - exceeds the quota")
	public void reduceQuota() throws JSONException{
		String response = an.deleteFeatureFromAnalytics(featureID1,BranchesRestApi.MASTER,sessionToken);
		response = an.updateQuota(seasonID, 2, sessionToken);
		Assert.assertFalse(response.contains("error"), "Quota was not returned " + response);

		JSONArray inputFields = new JSONArray();
		inputFields.put("context.device.locale");
		inputFields.put("context.device.osVersion");
		response = an.updateInputFieldToAnalytics(seasonID, branchID,  inputFields.toString(), sessionToken);
		Assert.assertTrue(response.contains("The maximum number of items in production to send to analytics was exceeded"), "Analytics was updated" + response);
		response = an.addFeatureToAnalytics(featureID1, BranchesRestApi.MASTER, sessionToken);
		Assert.assertTrue(response.contains("The maximum number of items in production to send to analytics for experiment experimentProd was exceeded"), "Analytics was updated" + response);
	}

	@Test (dependsOnMethods="reduceQuota", description="Add attribute - exceeds the quota")
	public void featureFromDevToProdInBranch() throws JSONException{
		try {
			String feature2 = FileUtils.fileToString(filePath + "feature2.txt", "UTF-8", false);
			JSONObject jsonF = new JSONObject(feature2);
			featureID2 = f.addFeatureToBranch(seasonID, branchID, jsonF.toString(), "ROOT", sessionToken);
			Assert.assertFalse(featureID2.contains("error"), "Feature was not added to the season" + featureID2);
			String response = an.addFeatureToAnalytics(featureID2, branchID, sessionToken);
			Assert.assertFalse(response.contains("error"), "Incorrect globalDataCollection response");

			jsonF = new JSONObject(f.getFeatureFromBranch(featureID2,branchID,sessionToken));
			jsonF.put("stage", "PRODUCTION");
			response = f.updateFeatureInBranch(seasonID,branchID,featureID2,jsonF.toString(),sessionToken);
			Assert.assertTrue(response.contains("The maximum number of items in production to send to analytics for experiment"), response);

		} catch (IOException e) {
			Assert.fail();
		}
	}

	@Test (dependsOnMethods="featureFromDevToProdInBranch", description="Add attribute - exceeds the quota")
	public void attibuteFromDevToProdInBranch() throws JSONException{
		try {
			String response = an.deleteFeatureFromAnalytics(featureID2, branchID, sessionToken);
			Assert.assertFalse(response.contains("error"), "Incorrect globalDataCollection response");

			String config = FileUtils.fileToString(filePath + "configuration_rule1.txt", "UTF-8", false);
			JSONObject jsonConfig = new JSONObject(config);
			JSONObject newConfiguration = new JSONObject();
			newConfiguration.put("color", "red");
			newConfiguration.put("size", "small");
			jsonConfig.put("configuration", newConfiguration);
			jsonConfig.put("name","config2");
			configID2 = f.addFeatureToBranch(seasonID, branchID, jsonConfig.toString(), featureID2, sessionToken);
			Assert.assertFalse(configID2.contains("error"), "Configuration1 was not added to the season" + configID2);

			JSONArray attributes = new JSONArray();
			JSONObject attr1 = new JSONObject();
			attr1.put("name", "color");
			attr1.put("type", "REGULAR");
			attributes.add(attr1);

			response = an.addAttributesToAnalytics(featureID2, branchID, attributes.toString(), sessionToken);
			Assert.assertFalse(response.contains("error"), "Feature was not added to analytics" + response);

			JSONObject jsonF = new JSONObject(f.getFeatureFromBranch(featureID2,branchID,sessionToken));
			jsonF.put("stage", "PRODUCTION");
			response = f.updateFeatureInBranch(seasonID,branchID,featureID2,jsonF.toString(),sessionToken);
			Assert.assertTrue(response.contains("The maximum number of items in production to send to analytics for experiment"), "Incorrect globalDataCollection response");

		} catch (IOException e) {
			Assert.fail();
		}
	}

	@Test (dependsOnMethods="attibuteFromDevToProdInBranch", description="Add attribute - exceeds the quota")
	public void inputFieldFromDevToProdInBranch() throws JSONException {
		try {
			String sch = schema.getInputSchema(seasonID, sessionToken);
			JSONObject jsonSchema = new JSONObject(sch);
			String schemaBody = FileUtils.fileToString(filePath + "inputSchemas/inputSchema_Production_fields_viewedLocation.txt", "UTF-8", false);
			jsonSchema.put("inputSchema", new JSONObject(schemaBody));
			String response = schema.updateInputSchema(seasonID, jsonSchema.toString(), sessionToken);
			JSONArray inputFields = new JSONArray();
			inputFields.put("context.device.locale");
			response = an.updateInputFieldToAnalytics(seasonID, branchID,  inputFields.toString(), sessionToken);

			JSONObject jsonF = new JSONObject(f.getFeatureFromBranch(featureID2,branchID,sessionToken));
			jsonF.put("stage", "PRODUCTION");
			response = f.updateFeatureInBranch(seasonID,branchID,featureID2,jsonF.toString(),sessionToken);

			JSONObject test = new JSONObject(an.getGlobalDataCollection(seasonID, branchID, "DISPLAY", sessionToken));

			sch = schema.getInputSchema(seasonID, sessionToken);
			jsonSchema = new JSONObject(sch);
			schemaBody = FileUtils.fileToString(filePath + "inputSchemas/inputSchema_update_device_locale_to_production.txt", "UTF-8", false);
			jsonSchema.put("inputSchema", new JSONObject(schemaBody));
			String validateSchema = schema.validateSchema(seasonID, schemaBody, sessionToken);
			JSONObject validateJson = new JSONObject(validateSchema);
			Assert.assertTrue(validateJson.containsKey("productionAnalyticsItemsQuotaExceeded"), "Validate schema didn't provide information on exceeded quota");
			response = schema.updateInputSchema(seasonID, jsonSchema.toString(), sessionToken);
			test = new JSONObject(an.getGlobalDataCollection(seasonID, branchID, "DISPLAY", sessionToken));
			Assert.assertTrue(response.contains("The maximum number of items in production to send to analytics for experiment"), "Schema was not added to the season" + response);
		} catch (Exception e) {
			Assert.fail();
		}
	}
	@Test (dependsOnMethods="inputFieldFromDevToProdInBranch", description="Add attribute - exceeds the quota")
	public void twoBranch() throws JSONException {
		try{
			String response = an.updateQuota(seasonID, 3, sessionToken);
			Assert.assertFalse(response.contains("error"), "Quota was not returned " + response);
			String feature3 = FileUtils.fileToString(filePath + "feature3.txt", "UTF-8", false);
			JSONObject jsonF = new JSONObject(feature3);
			jsonF.put("stage", "PRODUCTION");
			featureID3 = f.addFeatureToBranch(seasonID, branchID2, jsonF.toString(), "ROOT", sessionToken);
			Assert.assertFalse(featureID3.contains("error"), "Feature was not added to the season" + featureID2);
			response = an.addFeatureToAnalytics(featureID3, branchID2, sessionToken);
			Assert.assertFalse(response.contains("error"), "Incorrect globalDataCollection response");

			String respWithQuota = an.getGlobalDataCollection(seasonID, branchID2, "DISPLAY", sessionToken);
			Assert.assertTrue(getDevelopmentItemsReportedToAnalytics(respWithQuota)==2, "Incorrect number of development items"); //feature1+attribute+inputfield
			Assert.assertTrue(getProductionItemsReportedToAnalytics(respWithQuota)==2, "Incorrect number of production items"); //feature1+attribute+inputfield
			respWithQuota = an.getGlobalDataCollection(seasonID, branchID, "DISPLAY", sessionToken);
			Assert.assertTrue(getDevelopmentItemsReportedToAnalytics(respWithQuota)==3, "Incorrect number of development items"); //feature1+attribute+inputfield
			Assert.assertTrue(getProductionItemsReportedToAnalytics(respWithQuota)==2, "Incorrect number of production items"); //feature1+attribute+inputfield
			respWithQuota = an.getGlobalDataCollection(seasonID,  BranchesRestApi.MASTER, "DISPLAY", sessionToken);
			Assert.assertTrue(getDevelopmentItemsReportedToAnalytics(respWithQuota)==1, "Incorrect number of development items"); //feature1+attribute+inputfield
			Assert.assertTrue(getProductionItemsReportedToAnalytics(respWithQuota)==1, "Incorrect number of production items"); //feature1+attribute+inputfield

		} catch (Exception e) {
			Assert.fail();
		}

	}

	@Test (dependsOnMethods="twoBranch", description="Add attribute - exceeds the quota")
	public void checkoutAndRemoveFromMasterAnalytics() throws JSONException {
		try{

			String response = an.updateQuota(seasonID, 4, sessionToken);
			Assert.assertFalse(response.contains("error"), "Quota was not returned " + response);

			response = an.addFeatureToAnalytics(featureID1, BranchesRestApi.MASTER, sessionToken);
			Assert.assertFalse(response.contains("error"), "Incorrect globalDataCollection response");
			response = br.checkoutFeature(branchID2, featureID1, sessionToken);
			Assert.assertFalse(response.contains("error"), "feature was not checked out to branch");
			response = an.deleteFeatureFromAnalytics(featureID1, BranchesRestApi.MASTER, sessionToken);
			Assert.assertFalse(response.contains("error"), "Incorrect globalDataCollection response");

			String respWithQuota = an.getGlobalDataCollection(seasonID, branchID2, "DISPLAY", sessionToken);
			Assert.assertTrue(getDevelopmentItemsReportedToAnalytics(respWithQuota)==3, "Incorrect number of development items"); //feature1+attribute+inputfield
			Assert.assertTrue(getProductionItemsReportedToAnalytics(respWithQuota)==3, "Incorrect number of production items"); //feature1+attribute+inputfield
			respWithQuota = an.getGlobalDataCollection(seasonID, branchID, "DISPLAY", sessionToken);
			Assert.assertTrue(getDevelopmentItemsReportedToAnalytics(respWithQuota)==3, "Incorrect number of development items"); //feature1+attribute+inputfield
			Assert.assertTrue(getProductionItemsReportedToAnalytics(respWithQuota)==2, "Incorrect number of production items"); //feature1+attribute+inputfield
			respWithQuota = an.getGlobalDataCollection(seasonID,  BranchesRestApi.MASTER, "DISPLAY", sessionToken);
			Assert.assertTrue(getDevelopmentItemsReportedToAnalytics(respWithQuota)==1, "Incorrect number of development items"); //feature1+attribute+inputfield
			Assert.assertTrue(getProductionItemsReportedToAnalytics(respWithQuota)==1, "Incorrect number of production items"); //feature1+attribute+inputfield


			JSONArray attributes = new JSONArray();
			JSONObject attr1 = new JSONObject();
			attr1.put("name", "size");
			attr1.put("type", "REGULAR");
			attributes.add(attr1);

			response = an.addAttributesToAnalytics(featureID1, BranchesRestApi.MASTER, attributes.toString(), sessionToken);
			Assert.assertFalse(response.contains("error"), "Feature was not added to analytics" + response);

			respWithQuota = an.getGlobalDataCollection(seasonID, branchID2, "DISPLAY", sessionToken);
			Assert.assertTrue(getDevelopmentItemsReportedToAnalytics(respWithQuota)==4, "Incorrect number of development items"); //feature1+attribute+inputfield
			Assert.assertTrue(getProductionItemsReportedToAnalytics(respWithQuota)==4, "Incorrect number of production items"); //feature1+attribute+inputfield
			respWithQuota = an.getGlobalDataCollection(seasonID, branchID, "DISPLAY", sessionToken);
			Assert.assertTrue(getDevelopmentItemsReportedToAnalytics(respWithQuota)==3, "Incorrect number of development items"); //feature1+attribute+inputfield
			Assert.assertTrue(getProductionItemsReportedToAnalytics(respWithQuota)==2, "Incorrect number of production items"); //feature1+attribute+inputfield
			respWithQuota = an.getGlobalDataCollection(seasonID,  BranchesRestApi.MASTER, "DISPLAY", sessionToken);
			Assert.assertTrue(getDevelopmentItemsReportedToAnalytics(respWithQuota)==1, "Incorrect number of development items"); //feature1+attribute+inputfield
			Assert.assertTrue(getProductionItemsReportedToAnalytics(respWithQuota)==1, "Incorrect number of production items"); //feature1+attribute+inputfield

		} catch (Exception e) {
			Assert.fail();
		}

	}
	@Test (dependsOnMethods="checkoutAndRemoveFromMasterAnalytics", description="Add attribute - exceeds the quota")
	public void checkoutAndRemoveFromBranchAnalytics() throws JSONException {
		try{

			String response = an.addFeatureToAnalytics(featureID1, BranchesRestApi.MASTER, sessionToken);
			Assert.assertFalse(response.contains("error"), "Incorrect globalDataCollection response");
			response = an.deleteFeatureFromAnalytics(featureID1, branchID2, sessionToken);
			Assert.assertTrue(response.contains("The status of the item is being sent to analytics from the master branch. To stop sending item status to analytics, first go to the master and stop sending to analytics. Then, return to the branch and stop sending to analytics"), "Incorrect globalDataCollection response");

			String respWithQuota = an.getGlobalDataCollection(seasonID, branchID2, "DISPLAY", sessionToken);
			Assert.assertTrue(getDevelopmentItemsReportedToAnalytics(respWithQuota)==4, "Incorrect number of development items"); //feature1+attribute+inputfield
			Assert.assertTrue(getProductionItemsReportedToAnalytics(respWithQuota)==4, "Incorrect number of production items"); //feature1+attribute+inputfield
			respWithQuota = an.getGlobalDataCollection(seasonID, branchID, "DISPLAY", sessionToken);
			Assert.assertTrue(getDevelopmentItemsReportedToAnalytics(respWithQuota)==4, "Incorrect number of development items"); //feature1+attribute+inputfield
			Assert.assertTrue(getProductionItemsReportedToAnalytics(respWithQuota)==3, "Incorrect number of production items"); //feature1+attribute+inputfield
			respWithQuota = an.getGlobalDataCollection(seasonID,  BranchesRestApi.MASTER, "DISPLAY", sessionToken);
			Assert.assertTrue(getDevelopmentItemsReportedToAnalytics(respWithQuota)==2, "Incorrect number of development items"); //feature1+attribute+inputfield
			Assert.assertTrue(getProductionItemsReportedToAnalytics(respWithQuota)==2, "Incorrect number of production items"); //feature1+attribute+inputfield


			JSONArray attributes = new JSONArray();
			JSONObject attr1 = new JSONObject();
			attr1.put("name", "color");
			attr1.put("type", "REGULAR");
			attributes.add(attr1);

			response = an.addAttributesToAnalytics(featureID1, branchID2, attributes.toString(), sessionToken);
			Assert.assertTrue(response.contains("You must report all attributes that are reported in the master, in addition to the attributes that you want to report in the branch"), "Feature was not added to analytics" + response);
			respWithQuota = an.getGlobalDataCollection(seasonID, branchID2, "DISPLAY", sessionToken);
			Assert.assertTrue(getDevelopmentItemsReportedToAnalytics(respWithQuota)==4, "Incorrect number of development items"); //feature1+attribute+inputfield
			Assert.assertTrue(getProductionItemsReportedToAnalytics(respWithQuota)==4, "Incorrect number of production items"); //feature1+attribute+inputfield
			respWithQuota = an.getGlobalDataCollection(seasonID, branchID, "DISPLAY", sessionToken);
			Assert.assertTrue(getDevelopmentItemsReportedToAnalytics(respWithQuota)==4, "Incorrect number of development items"); //feature1+attribute+inputfield
			Assert.assertTrue(getProductionItemsReportedToAnalytics(respWithQuota)==3, "Incorrect number of production items"); //feature1+attribute+inputfield
			respWithQuota = an.getGlobalDataCollection(seasonID,  BranchesRestApi.MASTER, "DISPLAY", sessionToken);
			Assert.assertTrue(getDevelopmentItemsReportedToAnalytics(respWithQuota)==2, "Incorrect number of development items"); //feature1+attribute+inputfield
			Assert.assertTrue(getProductionItemsReportedToAnalytics(respWithQuota)==2, "Incorrect number of production items"); //feature1+attribute+inputfield

			//change attribute in configuration from string to array
			String configuration = f.getFeatureFromBranch(configID1, branchID2, sessionToken);
			JSONObject jsonConfig = new JSONObject(configuration);
			JSONObject newConfiguration = new JSONObject();
			JSONArray arr = new JSONArray();
			arr.put("red");
			arr.put("green");
			arr.put("blue");
			newConfiguration.put("color", arr);
			jsonConfig.put("configuration", newConfiguration);
			//simulate udpate feature
			response = f.simulateUpdateFeatureInBranch(seasonID, branchID2, configID1, jsonConfig.toString(), sessionToken);
			Assert.assertTrue(response.contains("warning"), "Warning was not reported in simulate update feature");
			//update feature
			response = f.updateFeatureInBranch(seasonID, branchID2, configID1, jsonConfig.toString(), sessionToken);
			Assert.assertFalse(response.contains("error"), "Configuration was not updated " + response);

			response = an.addAttributesToAnalytics(featureID1, branchID2, attributes.toString(), sessionToken);
			Assert.assertTrue(response.contains("You must report all attributes that are reported in the master, in addition to the attributes that you want to report in the branch"), "Feature was not added to analytics" + response);
			respWithQuota = an.getGlobalDataCollection(seasonID, branchID2, "DISPLAY", sessionToken);
			Assert.assertTrue(getDevelopmentItemsReportedToAnalytics(respWithQuota)==4, "Incorrect number of development items"); //feature1+attribute+inputfield
			Assert.assertTrue(getProductionItemsReportedToAnalytics(respWithQuota)==4, "Incorrect number of production items"); //feature1+attribute+inputfield
			respWithQuota = an.getGlobalDataCollection(seasonID, branchID, "DISPLAY", sessionToken);
			Assert.assertTrue(getDevelopmentItemsReportedToAnalytics(respWithQuota)==4, "Incorrect number of development items"); //feature1+attribute+inputfield
			Assert.assertTrue(getProductionItemsReportedToAnalytics(respWithQuota)==3, "Incorrect number of production items"); //feature1+attribute+inputfield
			respWithQuota = an.getGlobalDataCollection(seasonID,  BranchesRestApi.MASTER, "DISPLAY", sessionToken);
			Assert.assertTrue(getDevelopmentItemsReportedToAnalytics(respWithQuota)==2, "Incorrect number of development items"); //feature1+attribute+inputfield
			Assert.assertTrue(getProductionItemsReportedToAnalytics(respWithQuota)==2, "Incorrect number of production items"); //feature1+attribute+inputfield

		} catch (Exception e) {
			Assert.fail();
		}

	}
	@Test (dependsOnMethods="checkoutAndRemoveFromBranchAnalytics", description="Add attribute - exceeds the quota")
	public void addFeatureInMasterAfterChekout() throws JSONException {
		try{

			//add feature in production
			String feature1 = FileUtils.fileToString(filePath + "feature4.txt", "UTF-8", false);
			JSONObject jsonF = new JSONObject(feature1);
				jsonF.put("stage", "PRODUCTION");
			String newFeatureID = f.addFeatureToBranch(seasonID, BranchesRestApi.MASTER, jsonF.toString(), featureID1, sessionToken);
				Assert.assertFalse(newFeatureID.contains("error"), "Feature was not added to the season" + newFeatureID);

			String response = an.addFeatureToAnalytics(newFeatureID, BranchesRestApi.MASTER, sessionToken);
			Assert.assertFalse(response.contains("error"), "Incorrect globalDataCollection response");

			String respWithQuota = an.getGlobalDataCollection(seasonID, branchID2, "DISPLAY", sessionToken);
			Assert.assertTrue(getDevelopmentItemsReportedToAnalytics(respWithQuota)==4, "Incorrect number of development items"); //feature1+attribute+inputfield
			Assert.assertTrue(getProductionItemsReportedToAnalytics(respWithQuota)==4, "Incorrect number of production items"); //feature1+attribute+inputfield
			respWithQuota = an.getGlobalDataCollection(seasonID, branchID, "DISPLAY", sessionToken);
			Assert.assertTrue(getDevelopmentItemsReportedToAnalytics(respWithQuota)==5, "Incorrect number of development items"); //feature1+attribute+inputfield
			Assert.assertTrue(getProductionItemsReportedToAnalytics(respWithQuota)==4, "Incorrect number of production items"); //feature1+attribute+inputfield
			respWithQuota = an.getGlobalDataCollection(seasonID,  BranchesRestApi.MASTER, "DISPLAY", sessionToken);
			Assert.assertTrue(getDevelopmentItemsReportedToAnalytics(respWithQuota)==3, "Incorrect number of development items"); //feature1+attribute+inputfield
			Assert.assertTrue(getProductionItemsReportedToAnalytics(respWithQuota)==3, "Incorrect number of production items"); //feature1+attribute+inputfield

		} catch (Exception e) {
			Assert.fail();
		}

	}
	@Test (dependsOnMethods="addFeatureInMasterAfterChekout", description="Add attribute - exceeds the quota")
	public void variantInDevToProd() throws JSONException {
		try {

			String response = an.updateQuota(seasonID2, 4, sessionToken);
			Assert.assertFalse(response.contains("error"), "Quota was not returned " + response);

			experimentID = baseUtils.addExperiment("experiment."+RandomStringUtils.randomAlphabetic(5),m_analyticsUrl,false, false);
			branchID3 = baseUtils.addBranchFromBranch("branch3",BranchesRestApi.MASTER,seasonID2);
			String branchID3s1 = baseUtils.addBranchFromBranch("branch3",BranchesRestApi.MASTER,seasonID);
			variantID = baseUtils.addVariant(experimentID,"variant3","branch3",m_analyticsUrl,false);
			
			//enable experiment
			String airlockExperiment = exp.getExperiment(experimentID, sessionToken);
			Assert.assertFalse(airlockExperiment.contains("error"), "Experiment was not found: " + experimentID);

			JSONObject expJson = new JSONObject(airlockExperiment);
			expJson.put("enabled", true);
			
			response = exp.updateExperiment(experimentID, expJson.toString(), sessionToken);
			Assert.assertFalse(response.contains("error"), "Experiment was not updated: " + response);		


			//add feature in production
			String feature1 = FileUtils.fileToString(filePath + "feature5.txt", "UTF-8", false);
			JSONObject jsonF = new JSONObject(feature1);
			jsonF.put("stage", "PRODUCTION");
			String featureIDBranch = f.addFeatureToBranch(seasonID2, branchID3, jsonF.toString(), "ROOT", sessionToken);
			Assert.assertFalse(featureIDBranch.contains("error"), "Feature was not added to the season" + featureIDBranch);
			response = an.addFeatureToAnalytics(featureIDBranch, branchID3, sessionToken);
			Assert.assertFalse(response.contains("error"), "Incorrect globalDataCollection response");

			//add attribute
			String config = FileUtils.fileToString(filePath + "configuration_rule2.txt", "UTF-8", false);
			JSONObject jsonConfig = new JSONObject(config);
			JSONObject newConfiguration = new JSONObject();
			newConfiguration.put("color", "red");
			newConfiguration.put("size", "small");
			newConfiguration.put("a", "a");
			newConfiguration.put("b", "b");
			newConfiguration.put("c", "c");
			jsonConfig.put("configuration", newConfiguration);
			String configIDbranch = f.addFeatureToBranch(seasonID2, branchID3, jsonConfig.toString(), featureIDBranch, sessionToken);
			Assert.assertFalse(configIDbranch.contains("error"), "Configuration1 was not added to the season" + configIDbranch);

			JSONArray attributes = new JSONArray();
			JSONObject attr1 = new JSONObject();
			attr1.put("name", "color");
			attr1.put("type", "REGULAR");
			attributes.add(attr1);
			JSONObject attr2 = new JSONObject();
			attr2.put("name", "size");
			attr2.put("type", "REGULAR");
			attributes.add(attr2);
			JSONObject attr3 = new JSONObject();
			attr3.put("name", "a");
			attr3.put("type", "REGULAR");
			attributes.add(attr3);
			JSONObject attr4 = new JSONObject();
			attr4.put("name", "b");
			attr4.put("type", "REGULAR");
			attributes.add(attr4);

			response = an.addAttributesToAnalytics(featureIDBranch, branchID3, attributes.toString(), sessionToken);
			Assert.assertFalse(response.contains("error"), "Feature was not added to analytics" + response);

			String respWithQuota = an.getGlobalDataCollection(seasonID2, branchID3, "DISPLAY", sessionToken);
			Assert.assertTrue(getDevelopmentItemsReportedToAnalytics(respWithQuota)==5, "Incorrect number of development items"); //feature1+attribute+inputfield
			Assert.assertTrue(getProductionItemsReportedToAnalytics(respWithQuota)==5, "Incorrect number of production items"); //feature1+attribute+inputfield
			respWithQuota = an.getGlobalDataCollection(seasonID2,  BranchesRestApi.MASTER, "DISPLAY", sessionToken);
			Assert.assertTrue(getDevelopmentItemsReportedToAnalytics(respWithQuota)==0, "Incorrect number of development items"); //feature1+attribute+inputfield
			Assert.assertTrue(getProductionItemsReportedToAnalytics(respWithQuota)==0, "Incorrect number of production items"); //feature1+attribute+inputfield

			 response = an.updateQuota(seasonID, 4, sessionToken);
			Assert.assertFalse(response.contains("error"), "Quota was not returned " + response);

			JSONObject experiment = new JSONObject(exp.getExperiment(experimentID, sessionToken));
			experiment.put("stage", "PRODUCTION");
			response = exp.updateExperiment(experimentID, experiment.toString(), sessionToken);
			Assert.assertFalse(response.contains("error"), "Experiment was not updated");

			JSONObject variant = new JSONObject(exp.getVariant(variantID, sessionToken));
			variant.put("stage", "PRODUCTION");
			response = exp.updateVariant(variantID, variant.toString(), sessionToken);
			Assert.assertTrue(response.contains("error"), "variant was updated");

			JSONArray attributes2 = new JSONArray();
			response = an.addAttributesToAnalytics(featureIDBranch, branchID3, attributes2.toString(), sessionToken);
			Assert.assertFalse(response.contains("error"), "Feature was not added to analytics" + response);

			response = exp.updateVariant(variantID, variant.toString(), sessionToken);
			Assert.assertFalse(response.contains("error"), "variant was not updated");

			response = an.updateQuota(seasonID, 2, sessionToken);
			Assert.assertFalse(response.contains("The seaosn is included in an experiment in production and reducing the quota caused experiment experimentProd to exceed quota"), "Quota was not returned " + response);

			response = an.updateQuota(seasonID2, 2, sessionToken);
			Assert.assertTrue(response.contains("The seaosn is included in an experiment in production and reducing the quota caused experiment experimentProd to exceed quota"), "Quota was not returned " + response);


		}
		catch (Exception e){
			Assert.fail();
		}


	}


	private int getDevelopmentItemsReportedToAnalytics(String analytics) throws JSONException{
		JSONObject json = new JSONObject(analytics);
		return json.getJSONObject("analyticsDataCollection").getInt("developmentItemsReportedToAnalytics");
	}
	
	private int getProductionItemsReportedToAnalytics(String analytics) throws JSONException{
		JSONObject json = new JSONObject(analytics);
		//System.out.println("productionItem = " + json.getJSONObject("analyticsDataCollection").getInt("productionItemsReportedToAnalytics"));
		return json.getJSONObject("analyticsDataCollection").getInt("productionItemsReportedToAnalytics");
	}


	@AfterTest
	private void reset(){
		baseUtils.reset(productID, sessionToken);
	}
}
