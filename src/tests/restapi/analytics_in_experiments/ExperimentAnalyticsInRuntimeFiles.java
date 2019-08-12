package tests.restapi.analytics_in_experiments;



import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

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

//TODO: move dev faeture sent to ana to prod in branch - verify runtime update
//TODO: move feature report config to prod in branch - verify runtime update
public class ExperimentAnalyticsInRuntimeFiles {
	protected String seasonID1;
	protected String seasonID2;	
	protected String branchID1Prod;
	protected String branchID2Prod;
	protected String branchID3Dev;
	protected String productID;
	protected String featureIDProd;
	protected String featureIDDev;
	protected String featureID2;
	protected String configIDProd;
	protected String configIDDev;
	protected String configID2;
	protected String filePath;
	protected String m_url;
	protected ProductsRestApi p;
	protected FeaturesRestApi f;
	protected AnalyticsRestApi an;
	protected SeasonsRestApi s;
	private String m_analyticsUrl;
	
	protected InputSchemaRestApi schema;
	private String sessionToken = "";
	protected AirlockUtils baseUtils;
	private BranchesRestApi br ;
	private ExperimentsRestApi exp ;
	private String experimentID;
	private String variantID1Prod;
	private String variantID2Prod;
	private String variantID3Dev;
	
	
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
		s = new SeasonsRestApi();
		s.setURL(m_url);
	
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
		seasonID1 = baseUtils.createSeason(productID);

	}
	
	@Test ( description="Add input schema to the season")
	public void addSchema() throws Exception{
		String sch = schema.getInputSchema(seasonID1, sessionToken);
        JSONObject jsonSchema = new JSONObject(sch);
        String schemaBody = FileUtils.fileToString(filePath + "inputSchemas/inputSchema_update_device_locale_to_production.txt", "UTF-8", false);
        jsonSchema.put("inputSchema", new JSONObject(schemaBody));
        String response = schema.updateInputSchema(seasonID1, jsonSchema.toString(), sessionToken);
        Assert.assertFalse(response.contains("error"), "Schema was not added to the season" + response);
	}
	
	@Test (dependsOnMethods="addSchema", description="Add 2 input fields in production and 2 input fields in development")
	public void addToAnalytics() throws Exception{
		//add input field in master
		JSONArray inputFields = new JSONArray();
		//production fields
		inputFields.put("context.device.locale");
		inputFields.put("context.device.connectionType");
		
		//development fields
		inputFields.put("context.device.datetime");
		inputFields.put("context.device.localeCountryCode");
				
		String response = an.updateInputFieldToAnalytics(seasonID1, BranchesRestApi.MASTER,  inputFields.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Analytics was not updated" + response);
		
		//add feature in production  in master
		String feature1 = FileUtils.fileToString(filePath + "feature1.txt", "UTF-8", false);
		JSONObject jsonF = new JSONObject(feature1);
		jsonF.put("stage", "PRODUCTION");
		jsonF.put("name", "Fprod");
		featureIDProd = f.addFeatureToBranch(seasonID1, BranchesRestApi.MASTER, jsonF.toString(), "ROOT", sessionToken);
		Assert.assertFalse(featureIDProd.contains("error"), "Feature was not added to the season" + featureIDProd);
		
		response = an.addFeatureToAnalytics(featureIDProd, BranchesRestApi.MASTER, sessionToken);
		Assert.assertFalse(response.contains("error"), "Incorrect globalDataCollection response");	
		
		//add attribute  in master
		String config = FileUtils.fileToString(filePath + "configuration_rule1.txt", "UTF-8", false);
		JSONObject jsonConfig = new JSONObject(config);
		JSONObject newConfiguration = new JSONObject();
		newConfiguration.put("colorProd", "red");
		newConfiguration.put("sizeProd", "small");
		jsonConfig.put("configuration", newConfiguration);
		jsonConfig.put("name", "CRprod");
		configIDProd = f.addFeatureToBranch(seasonID1, BranchesRestApi.MASTER, jsonConfig.toString(), featureIDProd, sessionToken);
		Assert.assertFalse(configIDProd.contains("error"), "Configuration1 was not added to the season" + configIDProd);


		JSONArray attributes = new JSONArray();
		JSONObject attr1 = new JSONObject();
		attr1.put("name", "colorProd");
		attr1.put("type", "REGULAR");
		attributes.add(attr1);
		
		response = an.addAttributesToAnalytics(featureIDProd, BranchesRestApi.MASTER, attributes.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Feature was not added to analytics" + response);

		//add feature in development  in master
		jsonF = new JSONObject(feature1);
		jsonF.put("stage", "DEVELOPMENT");
		jsonF.put("name", "Fdev");
		featureIDDev = f.addFeatureToBranch(seasonID1, BranchesRestApi.MASTER, jsonF.toString(), "ROOT", sessionToken);
		Assert.assertFalse(featureIDDev.contains("error"), "Feature was not added to the season" + featureIDDev);
		
		response = an.addFeatureToAnalytics(featureIDDev, BranchesRestApi.MASTER, sessionToken);
		Assert.assertFalse(response.contains("error"), "Incorrect globalDataCollection response");	
		
		JSONObject jsonConfigDev = new JSONObject(config);
		JSONObject newConfigurationDev = new JSONObject();
		newConfigurationDev.put("colorDev", "red");
		newConfigurationDev.put("sizeDev", "small");
		jsonConfigDev.put("configuration", newConfigurationDev);
		jsonConfigDev.put("name", "CRdev");
		configIDDev = f.addFeatureToBranch(seasonID1, BranchesRestApi.MASTER, jsonConfigDev.toString(), featureIDDev, sessionToken);
		Assert.assertFalse(configIDDev.contains("error"), "Configuration was not added to the season" + configIDDev);


		attributes = new JSONArray();
		attr1 = new JSONObject();
		attr1.put("name", "colorDev");
		attr1.put("type", "REGULAR");
		attributes.add(attr1);
		
		response = an.addAttributesToAnalytics(featureIDDev, BranchesRestApi.MASTER, attributes.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Feature was not added to analytics" + response);
		
		//String respWithQuota = an.getGlobalDataCollection(seasonID, BranchesRestApi.MASTER, "DISPLAY", sessionToken);
		//Assert.assertTrue(getDevelopmentItemsReportedToAnalytics(respWithQuota)==3, "Incorrect number of development items"); //feature1+attribute+inputfield
		//Assert.assertTrue(getProductionItemsReportedToAnalytics(respWithQuota)==3, "Incorrect number of production items"); //feature1+attribute+inputfield
	}
	
	@Test (dependsOnMethods="addToAnalytics", description="Add experiment in production")
	public void addExperiment() throws Exception{
		String dateFormat = an.setDateFormat();
		
		experimentID = baseUtils.addExperiment(m_analyticsUrl, true, false);		
		
		Assert.assertFalse(experimentID.contains("error"), "Experiment was not created: " + experimentID);
		String branch = FileUtils.fileToString(filePath + "experiments/branch3.txt", "UTF-8", false);
		JSONObject branchJson = new JSONObject(branch);
		branchJson.put("name", "branch3prod");		
		String branchID3Prod = br.createBranch(seasonID1, branchJson.toString(), BranchesRestApi.MASTER, sessionToken);
		Assert.assertFalse(branchID3Prod.contains("error"), "Branch3 was not created: " + branchID3Prod);

		String variant = FileUtils.fileToString(filePath + "experiments/variant3.txt", "UTF-8", false);
		JSONObject variantJson = new JSONObject(variant);
		variantJson.put("name", "varian3prod");
		variantJson.put("branchName", "branch3prod");
		variantJson.put("stage", "PRODUCTION");
		String variantID3Prod = exp.createVariant(experimentID, variantJson.toString(), sessionToken);
		Assert.assertFalse(variantID3Prod.contains("error"), "Variant3 was not created: " + variantID1Prod);
		
		//enable experiment so a range will be created and the experiment will be published to analytics server
		String airlockExperiment = exp.getExperiment(experimentID, sessionToken);
		Assert.assertFalse(airlockExperiment.contains("error"), "Experiment was not found: " + experimentID);

		JSONObject expJson = new JSONObject(airlockExperiment);
		expJson.put("enabled", true);
		
		String response = exp.updateExperiment(experimentID, expJson.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Experiment was not updated: " + response);		

		
		an.setSleep();
		
		//validate dev experiment analytics
		RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getDevelopmentBranchFileDateModification(m_url, productID, seasonID1, BranchesRestApi.MASTER, dateFormat, sessionToken);
		Assert.assertTrue(responseDev.code ==200, "Runtime development feature file was not updated");
		JSONObject experimentAnalytics = getExperimentAnalytics(responseDev.message);
		
		String err = validateInputFieldsList(experimentAnalytics, new String[]{"context.device.locale", "context.device.localeCountryCode", "context.device.datetime", "context.device.connectionType"});
		Assert.assertTrue(err == null, err);
		
		err = validateFeaturesToAnalytics(experimentAnalytics, new String[]{"ns1.Fdev", "ns1.Fprod"});
		Assert.assertTrue(err == null, err);
		
		JSONArray featuresAttributes = new JSONArray();
		JSONObject featureAtt1 = new JSONObject();
		featureAtt1.put("name", "ns1.Fdev");
		featureAtt1.put("attributes", new String[]{"colorDev"});
		featuresAttributes.add(featureAtt1);
		JSONObject featureAtt2 = new JSONObject();
		featureAtt2.put("name", "ns1.Fprod");
		featureAtt2.put("attributes", new String[]{"colorProd"});
		featuresAttributes.add(featureAtt2);
				
		err = validateFeatureAttributesForAnalytics(experimentAnalytics, featuresAttributes);
		Assert.assertTrue(err == null, err);

		
		//validate prod experiment analytics
		RuntimeRestApi.DateModificationResults responseProd = RuntimeDateUtilities.getProductionBranchFileDateModification(m_url, productID, seasonID1, BranchesRestApi.MASTER, dateFormat, sessionToken);
		Assert.assertTrue(responseProd.code ==200, "Runtime development feature file was not updated");
		experimentAnalytics = getExperimentAnalytics(responseProd.message);
		
		err = validateInputFieldsList(experimentAnalytics, new String[]{"context.device.locale", "context.device.connectionType"});
		Assert.assertTrue(err == null, err);
		
		err = validateFeaturesToAnalytics(experimentAnalytics, new String[]{"ns1.Fprod"});
		Assert.assertTrue(err == null, err);
		
		featuresAttributes = new JSONArray();
		featureAtt2 = new JSONObject();
		featureAtt2.put("name", "ns1.Fprod");
		featureAtt2.put("attributes", new String[]{"colorProd"});
		featuresAttributes.add(featureAtt2);
				
		err = validateFeatureAttributesForAnalytics(experimentAnalytics, featuresAttributes);
		Assert.assertTrue(err == null, err);		
	}

	@Test ( dependsOnMethods="addExperiment", description="Add 2 branches and 2 variants in production")
	public void addBranchesAndVariantsInProduction() throws Exception{
		String dateFormat = an.setDateFormat();

		String branch = FileUtils.fileToString(filePath + "experiments/branch1.txt", "UTF-8", false);
		JSONObject branchJson = new JSONObject(branch);
		branchJson.put("name", "branch1prod");		
		branchID1Prod = br.createBranch(seasonID1, branchJson.toString(), BranchesRestApi.MASTER, sessionToken);
		Assert.assertFalse(branchID1Prod.contains("error"), "Branch1 was not created: " + branchID1Prod);

		String variant = FileUtils.fileToString(filePath + "experiments/variant1.txt", "UTF-8", false);
		JSONObject variantJson = new JSONObject(variant);
		variantJson.put("name", "varian1prod");
		variantJson.put("branchName", "branch1prod");
		variantJson.put("stage", "PRODUCTION");
		variantID1Prod = exp.createVariant(experimentID, variantJson.toString(), sessionToken);
		Assert.assertFalse(variantID1Prod.contains("error"), "Variant1 was not created: " + variantID1Prod);
		
		branchJson = new JSONObject(branch);
		branchJson.put("name", "branch2prod");		
		branchID2Prod = br.createBranch(seasonID1, branchJson.toString(), BranchesRestApi.MASTER, sessionToken);
		Assert.assertFalse(branchID1Prod.contains("error"), "Branch2 was not created: " + branchID2Prod);

		variantJson = new JSONObject(variant);
		variantJson.put("name", "varian2prod");
		variantJson.put("branchName", "branch2prod");
		variantJson.put("stage", "PRODUCTION");
		variantID2Prod = exp.createVariant(experimentID, variantJson.toString(), sessionToken);
		Assert.assertFalse(variantID1Prod.contains("error"), "Variant1 was not created: " + variantID2Prod);

		an.setSleep();
		//experiment analytics should remain the same
				
		//validate dev experiment analytics
		RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getDevelopmentBranchFileDateModification(m_url, productID, seasonID1, BranchesRestApi.MASTER, dateFormat, sessionToken);
		Assert.assertTrue(responseDev.code ==200, "Runtime development feature file was not updated");
		JSONObject experimentAnalytics = getExperimentAnalytics(responseDev.message);
		
		String err = validateInputFieldsList(experimentAnalytics, new String[]{"context.device.locale", "context.device.localeCountryCode", "context.device.datetime", "context.device.connectionType"});
		Assert.assertTrue(err == null, err);
		
		err = validateFeaturesToAnalytics(experimentAnalytics, new String[]{"ns1.Fdev", "ns1.Fprod"});
		Assert.assertTrue(err == null, err);
		
		JSONArray featuresAttributes = new JSONArray();
		JSONObject featureAtt1 = new JSONObject();
		featureAtt1.put("name", "ns1.Fdev");
		featureAtt1.put("attributes", new String[]{"colorDev"});
		featuresAttributes.add(featureAtt1);
		JSONObject featureAtt2 = new JSONObject();
		featureAtt2.put("name", "ns1.Fprod");
		featureAtt2.put("attributes", new String[]{"colorProd"});
		featuresAttributes.add(featureAtt2);
				
		err = validateFeatureAttributesForAnalytics(experimentAnalytics, featuresAttributes);
		Assert.assertTrue(err == null, err);

		
		//validate prod experiment analytics
		RuntimeRestApi.DateModificationResults responseProd = RuntimeDateUtilities.getProductionBranchFileDateModification(m_url, productID, seasonID1, BranchesRestApi.MASTER, dateFormat, sessionToken);
		Assert.assertTrue(responseProd.code ==200, "Runtime development feature file was not updated");
		experimentAnalytics = getExperimentAnalytics(responseProd.message);
		
		err = validateInputFieldsList(experimentAnalytics, new String[]{"context.device.locale", "context.device.connectionType"});
		Assert.assertTrue(err == null, err);
		
		err = validateFeaturesToAnalytics(experimentAnalytics, new String[]{"ns1.Fprod"});
		Assert.assertTrue(err == null, err);
		
		featuresAttributes = new JSONArray();
		featureAtt2 = new JSONObject();
		featureAtt2.put("name", "ns1.Fprod");
		featureAtt2.put("attributes", new String[]{"colorProd"});
		featuresAttributes.add(featureAtt2);
				
		err = validateFeatureAttributesForAnalytics(experimentAnalytics, featuresAttributes);
		Assert.assertTrue(err == null, err);		

	}

	@Test ( dependsOnMethods="addBranchesAndVariantsInProduction", description="Add branch and variant in development")
	public void addBranchAndVariantInDevelopment() throws Exception{
		String dateFormat = an.setDateFormat();

		String branch = FileUtils.fileToString(filePath + "experiments/branch1.txt", "UTF-8", false);
		JSONObject branchJson = new JSONObject(branch);
		branchJson.put("name", "branch3dev");		
		branchID3Dev = br.createBranch(seasonID1, branchJson.toString(), branchID1Prod, sessionToken);
		Assert.assertFalse(branchID3Dev.contains("error"), "Branch3dev was not created: " + branchID1Prod);

		String variant = FileUtils.fileToString(filePath + "experiments/variant1.txt", "UTF-8", false);
		JSONObject variantJson = new JSONObject(variant);
		variantJson.put("name", "varian3dev");
		variantJson.put("branchName", "branch3dev");
		variantJson.put("stage", "DEVELOPMENT");
		variantID3Dev = exp.createVariant(experimentID, variantJson.toString(), sessionToken);
		Assert.assertFalse(variantID3Dev.contains("error"), "Variant1 was not created: " + variantID1Prod);
		
		an.setSleep();
		//experiment analytics should remain the same and only runtime development should be changed
				
		//validate dev experiment analytics
		RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getDevelopmentBranchFileDateModification(m_url, productID, seasonID1, BranchesRestApi.MASTER, dateFormat, sessionToken);
		Assert.assertTrue(responseDev.code ==200, "Runtime development feature file was not updated");
		JSONObject experimentAnalytics = getExperimentAnalytics(responseDev.message);
		
		String err = validateInputFieldsList(experimentAnalytics, new String[]{"context.device.locale", "context.device.localeCountryCode", "context.device.datetime", "context.device.connectionType"});
		Assert.assertTrue(err == null, err);
		
		err = validateFeaturesToAnalytics(experimentAnalytics, new String[]{"ns1.Fdev", "ns1.Fprod"});
		Assert.assertTrue(err == null, err);
		
		JSONArray featuresAttributes = new JSONArray();
		JSONObject featureAtt1 = new JSONObject();
		featureAtt1.put("name", "ns1.Fdev");
		featureAtt1.put("attributes", new String[]{"colorDev"});
		featuresAttributes.add(featureAtt1);
		JSONObject featureAtt2 = new JSONObject();
		featureAtt2.put("name", "ns1.Fprod");
		featureAtt2.put("attributes", new String[]{"colorProd"});
		featuresAttributes.add(featureAtt2);
				
		err = validateFeatureAttributesForAnalytics(experimentAnalytics, featuresAttributes);
		Assert.assertTrue(err == null, err);

		
		//validate prod experiment analytics
		RuntimeRestApi.DateModificationResults responseProd = RuntimeDateUtilities.getProductionBranchFileDateModification(m_url, productID, seasonID1, BranchesRestApi.MASTER, dateFormat, sessionToken);
		Assert.assertTrue(responseProd.code ==304, "Runtime development feature file was not updated");	
	}

	@Test ( dependsOnMethods="addBranchAndVariantInDevelopment", description="Add season in experiment range")
	public void addSeasonInExperimentRange() throws Exception{
		String dateFormat = an.setDateFormat();

		JSONObject sJson = new JSONObject();
		sJson.put("minVersion", "2.0");
		
		seasonID2 = s.addSeason(productID, sJson.toString(), sessionToken);
		Assert.assertFalse(seasonID2.contains("error"), "The second season was not created: " + seasonID2);
		
		an.setSleep();
	
		//experiment analytics should remain the same on both seasons and only runtime development should be changed
				
		//validate dev experiment analytics on seaosn1
		RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getDevelopmentBranchFileDateModification(m_url, productID, seasonID2, BranchesRestApi.MASTER, dateFormat, sessionToken);
		Assert.assertTrue(responseDev.code ==200, "Runtime development feature file was not updated");
		JSONObject experimentAnalytics = getExperimentAnalytics(responseDev.message);
		
		String err = validateInputFieldsList(experimentAnalytics, new String[]{"context.device.locale", "context.device.localeCountryCode", "context.device.datetime", "context.device.connectionType"});
		Assert.assertTrue(err == null, err);
		
		err = validateFeaturesToAnalytics(experimentAnalytics, new String[]{"ns1.Fdev", "ns1.Fprod"});
		Assert.assertTrue(err == null, err);
		
		JSONArray devFeaturesAttributes = new JSONArray();
		JSONObject featureAtt1 = new JSONObject();
		featureAtt1.put("name", "ns1.Fdev");
		featureAtt1.put("attributes", new String[]{"colorDev"});
		devFeaturesAttributes.add(featureAtt1);
		JSONObject featureAtt2 = new JSONObject();
		featureAtt2.put("name", "ns1.Fprod");
		featureAtt2.put("attributes", new String[]{"colorProd"});
		devFeaturesAttributes.add(featureAtt2);
				
		err = validateFeatureAttributesForAnalytics(experimentAnalytics, devFeaturesAttributes);
		Assert.assertTrue(err == null, err);

		an.setSleep();
		
		//validate prod experiment analytics
		RuntimeRestApi.DateModificationResults responseProd = RuntimeDateUtilities.getProductionBranchFileDateModification(m_url, productID, seasonID2, BranchesRestApi.MASTER, dateFormat, sessionToken);
		Assert.assertTrue(responseProd.code ==200, "Runtime development feature file was not updated");
		experimentAnalytics = getExperimentAnalytics(responseProd.message);
		
		err = validateInputFieldsList(experimentAnalytics, new String[]{"context.device.locale", "context.device.connectionType"});
		Assert.assertTrue(err == null, err);
		
		err = validateFeaturesToAnalytics(experimentAnalytics, new String[]{"ns1.Fprod"});
		Assert.assertTrue(err == null, err);
		
		JSONArray prodFeaturesAttributes = new JSONArray();
		featureAtt2 = new JSONObject();
		featureAtt2.put("name", "ns1.Fprod");
		featureAtt2.put("attributes", new String[]{"colorProd"});
		prodFeaturesAttributes.add(featureAtt2);
				
		err = validateFeatureAttributesForAnalytics(experimentAnalytics, prodFeaturesAttributes);
		Assert.assertTrue(err == null, err);
		
		//check that not rewritten in Season 1 - it is rewritten since the season's max version was updated
		//validate dev experiment analytics on seaosn1
		responseDev = RuntimeDateUtilities.getDevelopmentBranchFileDateModification(m_url, productID, seasonID1, BranchesRestApi.MASTER, dateFormat, sessionToken);
		Assert.assertTrue(responseDev.code ==200, "Runtime development feature file was not updated");
		experimentAnalytics = getExperimentAnalytics(responseDev.message);
		
		err = validateInputFieldsList(experimentAnalytics, new String[]{"context.device.locale", "context.device.localeCountryCode", "context.device.datetime", "context.device.connectionType"});
		Assert.assertTrue(err == null, err);
		
		err = validateFeaturesToAnalytics(experimentAnalytics, new String[]{"ns1.Fdev", "ns1.Fprod"});
		Assert.assertTrue(err == null, err);
		
		err = validateFeatureAttributesForAnalytics(experimentAnalytics, devFeaturesAttributes);
		Assert.assertTrue(err == null, err);

		
		//validate prod experiment analytics of season 1
		responseProd = RuntimeDateUtilities.getProductionBranchFileDateModification(m_url, productID, seasonID1, BranchesRestApi.MASTER, dateFormat, sessionToken);
		Assert.assertTrue(responseProd.code ==200, "Runtime development feature file was not updated");
		experimentAnalytics = getExperimentAnalytics(responseProd.message);
		
		err = validateInputFieldsList(experimentAnalytics, new String[]{"context.device.locale", "context.device.connectionType"});
		Assert.assertTrue(err == null, err);
		
		err = validateFeaturesToAnalytics(experimentAnalytics, new String[]{"ns1.Fprod"});
		Assert.assertTrue(err == null, err);
		
		err = validateFeatureAttributesForAnalytics(experimentAnalytics, prodFeaturesAttributes);
		Assert.assertTrue(err == null, err);
		
	}


	@Test (dependsOnMethods="addSeasonInExperimentRange", description="add analytics to branch in development")
	public void addAnalyticsToBranchInDevelopment() throws Exception{
		String dateFormat = an.setDateFormat();

		//add input field in master
		JSONArray inputFields = new JSONArray();
		//production fields
		inputFields.put("context.device.locale");
		inputFields.put("context.device.connectionType");
		inputFields.put("context.device.osVersion");
		
		//development fields
		inputFields.put("context.device.datetime");
		inputFields.put("context.device.localeCountryCode");
		inputFields.put("context.device.screenWidth");
				
		String response = an.updateInputFieldToAnalytics(seasonID1, branchID3Dev,  inputFields.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Analytics was not updated" + response);
		
		//add feature in production  in branch that is in dev variant
		String feature1 = FileUtils.fileToString(filePath + "feature1.txt", "UTF-8", false);
		JSONObject jsonF = new JSONObject(feature1);
		jsonF.put("stage", "PRODUCTION");
		jsonF.put("name", "FprodInDevBranch");
		String featureID = f.addFeatureToBranch(seasonID1, branchID3Dev, jsonF.toString(), "ROOT", sessionToken);
		Assert.assertFalse(featureID.contains("error"), "Feature was not added to the season" + featureID);
		
		response = an.addFeatureToAnalytics(featureID, branchID3Dev, sessionToken);
		Assert.assertFalse(response.contains("error"), "Incorrect globalDataCollection response");	
		
		//add attribute  in branchID3Dev
		String config = FileUtils.fileToString(filePath + "configuration_rule1.txt", "UTF-8", false);
		JSONObject jsonConfig = new JSONObject(config);
		JSONObject newConfiguration = new JSONObject();
		newConfiguration.put("colorProdInDevBranch", "red");
		newConfiguration.put("sizeProdInDevBranch", "small");
		jsonConfig.put("configuration", newConfiguration);
		jsonConfig.put("name", "CRprodInDevBranch");
		String configID = f.addFeatureToBranch(seasonID1, branchID3Dev, jsonConfig.toString(), featureID, sessionToken);
		Assert.assertFalse(configID.contains("error"), "Configuration1 was not added to the season" + configIDProd);


		JSONArray attributes = new JSONArray();
		JSONObject attr1 = new JSONObject();
		attr1.put("name", "sizeProdInDevBranch");
		attr1.put("type", "REGULAR");
		attributes.add(attr1);
		
		response = an.addAttributesToAnalytics(featureID, branchID3Dev, attributes.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Feature was not added to analytics" + response);

		//validate dev experiment analytics
		//season 1
		RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getDevelopmentBranchFileDateModification(m_url, productID, seasonID1, BranchesRestApi.MASTER, dateFormat, sessionToken);
		Assert.assertTrue(responseDev.code ==200, "Runtime development feature file was not updated");
		JSONObject experimentAnalytics = getExperimentAnalytics(responseDev.message);
		
		String err = validateInputFieldsList(experimentAnalytics, new String[]{"context.device.locale", "context.device.localeCountryCode", "context.device.datetime", "context.device.connectionType", "context.device.osVersion", "context.device.screenWidth"});
		Assert.assertTrue(err == null, err);
		
		err = validateFeaturesToAnalytics(experimentAnalytics, new String[]{"ns1.Fdev", "ns1.Fprod", "ns1.FprodInDevBranch"});
		Assert.assertTrue(err == null, err);
		
		JSONArray featuresAttributes = new JSONArray();
		JSONObject featureAtt1 = new JSONObject();
		featureAtt1.put("name", "ns1.Fdev");
		featureAtt1.put("attributes", new String[]{"colorDev"});
		featuresAttributes.add(featureAtt1);
		JSONObject featureAtt2 = new JSONObject();
		featureAtt2.put("name", "ns1.Fprod");
		featureAtt2.put("attributes", new String[]{"colorProd"});
		featuresAttributes.add(featureAtt2);
		JSONObject featureAtt3 = new JSONObject();
		featureAtt3.put("name", "ns1.FprodInDevBranch");
		featureAtt3.put("attributes", new String[]{"sizeProdInDevBranch"});
		featuresAttributes.add(featureAtt3);
				
		err = validateFeatureAttributesForAnalytics(experimentAnalytics, featuresAttributes);
		Assert.assertTrue(err == null, err);
		
		
		//season 2
		responseDev = RuntimeDateUtilities.getDevelopmentBranchFileDateModification(m_url, productID, seasonID2, BranchesRestApi.MASTER, dateFormat, sessionToken);
		Assert.assertTrue(responseDev.code ==200, "Runtime development feature file was not updated");
		experimentAnalytics = getExperimentAnalytics(responseDev.message);
		
		err = validateInputFieldsList(experimentAnalytics, new String[]{"context.device.locale", "context.device.localeCountryCode", "context.device.datetime", "context.device.connectionType", "context.device.osVersion", "context.device.screenWidth"});
		Assert.assertTrue(err == null, err);
		
		err = validateFeaturesToAnalytics(experimentAnalytics, new String[]{"ns1.Fdev", "ns1.Fprod", "ns1.FprodInDevBranch"});
		Assert.assertTrue(err == null, err);
		
		err = validateFeatureAttributesForAnalytics(experimentAnalytics, featuresAttributes);
		Assert.assertTrue(err == null, err);
		
				
		//validate prod experiment analytics not changed and not rewritten
		RuntimeRestApi.DateModificationResults responseProd = RuntimeDateUtilities.getProductionBranchFileDateModification(m_url, productID, seasonID1, BranchesRestApi.MASTER, dateFormat, sessionToken);
		Assert.assertTrue(responseProd.code ==304, "Runtime development feature file was not updated");
		
		responseProd = RuntimeDateUtilities.getProductionBranchFileDateModification(m_url, productID, seasonID2, BranchesRestApi.MASTER, dateFormat, sessionToken);
		Assert.assertTrue(responseProd.code ==304, "Runtime development feature file was not updated");
	}

	@Test (dependsOnMethods="addAnalyticsToBranchInDevelopment", description="add analytics to master of second season")
	public void addAnalyticsToSecondMaster() throws Exception{
		String dateFormat = an.setDateFormat();

		//add input field in master
		JSONArray inputFields = new JSONArray();
		//production fields
		inputFields.put("context.device.locale");
		inputFields.put("context.device.connectionType");
		//inputFields.put("context.device.osVersion");
		
		//development fields
		inputFields.put("context.device.datetime");
		inputFields.put("context.device.localeCountryCode");
		inputFields.put("context.device.screenHeight");
				
		String response = an.updateInputFieldToAnalytics(seasonID2, BranchesRestApi.MASTER,  inputFields.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Analytics was not updated" + response);
		
		//add feature in production  in branch that is in dev variant
		String feature1 = FileUtils.fileToString(filePath + "feature1.txt", "UTF-8", false);
		JSONObject jsonF = new JSONObject(feature1);
		jsonF.put("stage", "PRODUCTION");
		jsonF.put("name", "FprodInSecMaster");
		String featureID = f.addFeatureToBranch(seasonID2, BranchesRestApi.MASTER, jsonF.toString(), "ROOT", sessionToken);
		Assert.assertFalse(featureID.contains("error"), "Feature was not added to the season" + featureID);
		
		response = an.addFeatureToAnalytics(featureID, BranchesRestApi.MASTER, sessionToken);
		Assert.assertFalse(response.contains("error"), "Incorrect globalDataCollection response");	
		
		//add attribute  to second master
		String config = FileUtils.fileToString(filePath + "configuration_rule1.txt", "UTF-8", false);
		JSONObject jsonConfig = new JSONObject(config);
		JSONObject newConfiguration = new JSONObject();
		newConfiguration.put("colorProdInSecMaster", "red");
		newConfiguration.put("sizeProdInSecMaster", "small");
		jsonConfig.put("configuration", newConfiguration);
		jsonConfig.put("name", "CRprodInSecMaster");
		String configID = f.addFeatureToBranch(seasonID2, BranchesRestApi.MASTER, jsonConfig.toString(), featureID, sessionToken);
		Assert.assertFalse(configID.contains("error"), "Configuration1 was not added to the season" + configID);


		JSONArray attributes = new JSONArray();
		JSONObject attr1 = new JSONObject();
		attr1.put("name", "sizeProdInSecMaster");
		attr1.put("type", "REGULAR");
		attributes.add(attr1);
		
		response = an.addAttributesToAnalytics(featureID, BranchesRestApi.MASTER, attributes.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Feature was not added to analytics" + response);

		an.setSleep();
		
		//validate dev experiment analytics
		//season1
		RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getDevelopmentBranchFileDateModification(m_url, productID, seasonID1, BranchesRestApi.MASTER, dateFormat, sessionToken);
		Assert.assertTrue(responseDev.code ==200, "Runtime development feature file was not updated");
		JSONObject experimentAnalytics = getExperimentAnalytics(responseDev.message);
		
		String err = validateInputFieldsList(experimentAnalytics, new String[]{"context.device.locale", "context.device.localeCountryCode", "context.device.datetime", "context.device.connectionType", "context.device.osVersion", "context.device.screenWidth", "context.device.screenHeight"});
		Assert.assertTrue(err == null, err);
		
		err = validateFeaturesToAnalytics(experimentAnalytics, new String[]{"ns1.Fdev", "ns1.Fprod", "ns1.FprodInDevBranch", "ns1.FprodInSecMaster"});
		Assert.assertTrue(err == null, err);
		
		JSONArray featuresAttributesDev = new JSONArray();
		JSONObject featureAtt1 = new JSONObject();
		featureAtt1.put("name", "ns1.Fdev");
		featureAtt1.put("attributes", new String[]{"colorDev"});
		featuresAttributesDev.add(featureAtt1);
		JSONObject featureAtt2 = new JSONObject();
		featureAtt2.put("name", "ns1.Fprod");
		featureAtt2.put("attributes", new String[]{"colorProd"});
		featuresAttributesDev.add(featureAtt2);
		JSONObject featureAtt3 = new JSONObject();
		featureAtt3.put("name", "ns1.FprodInDevBranch");
		featureAtt3.put("attributes", new String[]{"sizeProdInDevBranch"});
		featuresAttributesDev.add(featureAtt3);
		JSONObject featureAtt4 = new JSONObject();
		featureAtt4.put("name", "ns1.FprodInSecMaster");
		featureAtt4.put("attributes", new String[]{"sizeProdInSecMaster"});
		featuresAttributesDev.add(featureAtt4);
				
		err = validateFeatureAttributesForAnalytics(experimentAnalytics, featuresAttributesDev);
		Assert.assertTrue(err == null, err);


		//season2
		responseDev = RuntimeDateUtilities.getDevelopmentBranchFileDateModification(m_url, productID, seasonID2, BranchesRestApi.MASTER, dateFormat, sessionToken);
		Assert.assertTrue(responseDev.code ==200, "Runtime development feature file was not updated");
		experimentAnalytics = getExperimentAnalytics(responseDev.message);
		
		err = validateInputFieldsList(experimentAnalytics, new String[]{"context.device.locale", "context.device.localeCountryCode", "context.device.datetime", "context.device.connectionType", "context.device.osVersion", "context.device.screenWidth", "context.device.screenHeight"});
		Assert.assertTrue(err == null, err);
		
		err = validateFeaturesToAnalytics(experimentAnalytics, new String[]{"ns1.Fdev", "ns1.Fprod", "ns1.FprodInDevBranch", "ns1.FprodInSecMaster"});
		Assert.assertTrue(err == null, err);
				
		err = validateFeatureAttributesForAnalytics(experimentAnalytics, featuresAttributesDev);
		Assert.assertTrue(err == null, err);

		
		//validate prod experiment analytics
		//season1
		RuntimeRestApi.DateModificationResults responseProd = RuntimeDateUtilities.getProductionBranchFileDateModification(m_url, productID, seasonID1, BranchesRestApi.MASTER, dateFormat, sessionToken);
		Assert.assertTrue(responseProd.code ==200, "Runtime production feature file was not updated");
		experimentAnalytics = getExperimentAnalytics(responseProd.message);
		
		err = validateInputFieldsList(experimentAnalytics, new String[]{"context.device.locale", "context.device.connectionType"});
		Assert.assertTrue(err == null, err);
		
		err = validateFeaturesToAnalytics(experimentAnalytics, new String[]{"ns1.Fprod", "ns1.FprodInSecMaster"});
		Assert.assertTrue(err == null, err);
		
		JSONArray featuresAttributesProd = new JSONArray();
		featureAtt2 = new JSONObject();
		featureAtt2.put("name", "ns1.Fprod");
		featureAtt2.put("attributes", new String[]{"colorProd"});
		featuresAttributesProd.add(featureAtt2);
		featureAtt4 = new JSONObject();
		featureAtt4.put("name", "ns1.FprodInSecMaster");
		featureAtt4.put("attributes", new String[]{"sizeProdInSecMaster"});
		featuresAttributesProd.add(featureAtt4);
				
		err = validateFeatureAttributesForAnalytics(experimentAnalytics, featuresAttributesProd);
		Assert.assertTrue(err == null, err);


		//season2
		responseProd = RuntimeDateUtilities.getProductionBranchFileDateModification(m_url, productID, seasonID2, BranchesRestApi.MASTER, dateFormat, sessionToken);
		Assert.assertTrue(responseProd.code ==200, "Runtime development feature file was not updated");
		experimentAnalytics = getExperimentAnalytics(responseProd.message);
		
		err = validateInputFieldsList(experimentAnalytics, new String[]{"context.device.locale", "context.device.connectionType"});
		Assert.assertTrue(err == null, err);
		
		err = validateFeaturesToAnalytics(experimentAnalytics, new String[]{"ns1.Fprod", "ns1.FprodInSecMaster"});
		Assert.assertTrue(err == null, err);
				
		err = validateFeatureAttributesForAnalytics(experimentAnalytics, featuresAttributesProd);
		Assert.assertTrue(err == null, err);
		
	}

	
	@Test (dependsOnMethods="addAnalyticsToSecondMaster", description="update analytics to master of second season")
	public void updateAnalyticsToSecondMaster() throws Exception{
	
	//change schema: switch between dev and prod fields
	
	String sch = schema.getInputSchema(seasonID2, sessionToken);
    JSONObject jsonSchema = new JSONObject(sch);
    String schemaBody = FileUtils.fileToString(filePath + "inputSchemas/inputSchema_update_device_locale_to_development.txt", "UTF-8", false);
    jsonSchema.put("inputSchema", new JSONObject(schemaBody));
    String response = schema.updateInputSchema(seasonID2, jsonSchema.toString(), sessionToken);
    Assert.assertFalse(response.contains("error"), "Schema was not added to the season" + response);
    
    /*	moved to development:
     	inputFields.put("context.device.locale");    
		inputFields.put("context.device.connectionType");
		
		moved to production development fields
		inputFields.put("context.device.datetime");
		inputFields.put("context.device.localeCountryCode");
		inputFields.put("context.device.screenHeight");
	*/
		String dateFormat = an.setDateFormat();

		//move Fprod to development
		JSONArray featuresInSeason = f.getFeaturesBySeason(seasonID2, sessionToken);
		String err = updateFeatureStage(featuresInSeason, "Fprod", "DEVELOPMENT");
		Assert.assertTrue(err == null, err);
		err = updateFeatureStage(featuresInSeason, "Fdev", "PRODUCTION");
		Assert.assertTrue(err == null, err);
	
		an.setSleep();
		
		//validate dev experiment analytics
		//season1 - nothing changes
		RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getDevelopmentBranchFileDateModification(m_url, productID, seasonID1, BranchesRestApi.MASTER, dateFormat, sessionToken);
		Assert.assertTrue(responseDev.code ==200, "Runtime development feature file was not updated");
		

		//season2
		responseDev = RuntimeDateUtilities.getDevelopmentBranchFileDateModification(m_url, productID, seasonID2, BranchesRestApi.MASTER, dateFormat, sessionToken);
		Assert.assertTrue(responseDev.code ==200, "Runtime development feature file was not updated");
		JSONObject experimentAnalytics = getExperimentAnalytics(responseDev.message);
		
		err = validateInputFieldsList(experimentAnalytics, new String[]{"context.device.locale", "context.device.localeCountryCode", "context.device.datetime", "context.device.connectionType", "context.device.osVersion", "context.device.screenWidth", "context.device.screenHeight"});
		Assert.assertTrue(err == null, err);
		
		err = validateFeaturesToAnalytics(experimentAnalytics, new String[]{"ns1.Fdev", "ns1.Fprod", "ns1.FprodInDevBranch", "ns1.FprodInSecMaster"});
		Assert.assertTrue(err == null, err);
			
		JSONArray featuresAttributesDev = new JSONArray();
		JSONObject featureAtt1 = new JSONObject();
		featureAtt1.put("name", "ns1.Fdev");
		featureAtt1.put("attributes", new String[]{"colorDev"});
		featuresAttributesDev.add(featureAtt1);
		JSONObject featureAtt2 = new JSONObject();
		featureAtt2.put("name", "ns1.Fprod");
		featureAtt2.put("attributes", new String[]{"colorProd"});
		featuresAttributesDev.add(featureAtt2);
		JSONObject featureAtt3 = new JSONObject();
		featureAtt3.put("name", "ns1.FprodInDevBranch");
		featureAtt3.put("attributes", new String[]{"sizeProdInDevBranch"});
		featuresAttributesDev.add(featureAtt3);
		JSONObject featureAtt4 = new JSONObject();
		featureAtt4.put("name", "ns1.FprodInSecMaster");
		featureAtt4.put("attributes", new String[]{"sizeProdInSecMaster"});
		featuresAttributesDev.add(featureAtt4);
				

		err = validateFeatureAttributesForAnalytics(experimentAnalytics, featuresAttributesDev);
		Assert.assertTrue(err == null, err);

		
		
		JSONArray featuresAttributesProd = new JSONArray();
		featureAtt1 = new JSONObject();
		featureAtt1.put("name", "ns1.Fprod");
		featureAtt1.put("attributes", new String[]{"colorProd"});
		featuresAttributesProd.add(featureAtt1);
		featureAtt2 = new JSONObject();
		featureAtt2.put("name", "ns1.Fdev");
		featureAtt2.put("attributes", new String[]{"colorDev"});
		featuresAttributesProd.add(featureAtt2);
		featureAtt4 = new JSONObject();
		featureAtt4.put("name", "ns1.FprodInSecMaster");
		featureAtt4.put("attributes", new String[]{"sizeProdInSecMaster"});
		featuresAttributesProd.add(featureAtt4);
		
		//validate prod experiment analytics
		//season1
		RuntimeRestApi.DateModificationResults responseProd = RuntimeDateUtilities.getProductionBranchFileDateModification(m_url, productID, seasonID1, BranchesRestApi.MASTER, dateFormat, sessionToken);
		Assert.assertTrue(responseProd.code ==200, "Runtime production feature file was not updated");

		experimentAnalytics = getExperimentAnalytics(responseProd.message);
		
		err = validateInputFieldsList(experimentAnalytics, new String[]{"context.device.locale", "context.device.localeCountryCode", "context.device.datetime", "context.device.connectionType", "context.device.screenHeight"});
		Assert.assertTrue(err == null, err);

		err = validateFeaturesToAnalytics(experimentAnalytics, new String[]{"ns1.Fdev", "ns1.FprodInSecMaster", "ns1.Fprod"});
		Assert.assertTrue(err == null, err);
		
		err = validateFeatureAttributesForAnalytics(experimentAnalytics, featuresAttributesProd);
		Assert.assertTrue(err == null, err);
		
		//season2
		responseProd = RuntimeDateUtilities.getProductionBranchFileDateModification(m_url, productID, seasonID2, BranchesRestApi.MASTER, dateFormat, sessionToken);
		Assert.assertTrue(responseProd.code ==200, "Runtime production feature file was not updated");
		experimentAnalytics = getExperimentAnalytics(responseProd.message);
		
		err = validateInputFieldsList(experimentAnalytics, new String[]{"context.device.locale", "context.device.localeCountryCode", "context.device.datetime", "context.device.connectionType", "context.device.screenHeight"});
		Assert.assertTrue(err == null, err);
		
		err = validateFeaturesToAnalytics(experimentAnalytics, new String[]{"ns1.Fdev", "ns1.FprodInSecMaster", "ns1.Fprod"});
		Assert.assertTrue(err == null, err);
		
		err = validateFeatureAttributesForAnalytics(experimentAnalytics, featuresAttributesProd);
		Assert.assertTrue(err == null, err);

	}

	private String validateFeatureAttributesForAnalytics(JSONObject experimentAnalytics, JSONArray expectedFeaturesAttributes) {
		if (!experimentAnalytics.containsKey("featuresAttributesForAnalytics"))
			return "Missing 'featuresAttributesForAnalytics' in experiment analytics";
		
		try {
			JSONArray featuresAttributesForAnalytics = experimentAnalytics.getJSONArray("featuresAttributesForAnalytics");
			if (featuresAttributesForAnalytics.size() != expectedFeaturesAttributes.size())
				return "wrong number of features that report attribute to analytics.";
			
			HashMap<String, JSONArray> existingFeaturesAttMap = new HashMap<String, JSONArray>();			
			for (int i=0; i<featuresAttributesForAnalytics.size(); i++) {
				JSONObject faetureAtts = featuresAttributesForAnalytics.getJSONObject(i);
				String name = faetureAtts.getString("name");
				JSONArray atts = faetureAtts.getJSONArray("attributes");
				existingFeaturesAttMap.put (name, atts);
			}
			
			HashMap<String, JSONArray> expectedFeaturesAttMap = new HashMap<String, JSONArray>();			
			for (int i=0; i<expectedFeaturesAttributes.size(); i++) {
				JSONObject faetureAtts = expectedFeaturesAttributes.getJSONObject(i);
				String name = faetureAtts.getString("name");
				JSONArray atts = faetureAtts.getJSONArray("attributes");
				expectedFeaturesAttMap.put (name, atts);
			}
			
			Set<String> expectedFeaturesNames =  expectedFeaturesAttMap.keySet();
			Set<String> existingFeaturesNames =  existingFeaturesAttMap.keySet();
			
			if (!expectedFeaturesNames.equals(existingFeaturesNames)) {
		    	return "features attributes for analytics are not as expected";
		    }
			
			//same features are reporting attributes
			//now, check that the reported attributes are the same
			for (String featureName:expectedFeaturesNames) {
				JSONArray existingAtts = existingFeaturesAttMap.get(featureName);
				List<String> existingAttsList = new LinkedList<String>();
				for (int i=0; i<existingAtts.size(); i++) {
					String s = existingAtts.getString(i);
					existingAttsList.add(s);
				}
				
				JSONArray expectedAtts = expectedFeaturesAttMap.get(featureName);
				List<String> expectedAttsList = new LinkedList<String>();
				for (int i=0; i<expectedAtts.size(); i++) {
					String s = expectedAtts.getString(i);
					expectedAttsList.add(s);
				}
				
				Collections.sort(existingAttsList);
				Collections.sort(expectedAttsList);
				if (!existingAttsList.equals(expectedAttsList)) {
			    	return "features attributes for analytics are not as expected";
			    }
				
			}
		   
		} catch (JSONException je) {
			return je.getMessage();
		}
		
		
		return null;
	}

	private String validateFeaturesToAnalytics(JSONObject experimentAnalytics, String[] expectedFeatures) {
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
		    List<String> expectedFeaturesList =  Arrays.asList(expectedFeatures);
		    Collections.sort(expectedFeaturesList);
		    if (!exisitngfeaturesForAnalyticsList.equals(expectedFeaturesList)) {
		    	return "features And Configurations For Analytics are not as expected";
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
	
	
	private String updateFeatureStage(JSONArray features, String fName, String fStage) throws JSONException, IOException{
		for (int i=0; i<features.size(); i++){
			JSONObject feature = features.getJSONObject(i);
			if (feature.getString("name").equals(fName)){
				String featureId = feature.getString("uniqueId");
				JSONObject json = new JSONObject(f.getFeature(featureId, sessionToken));
				json.put("stage", fStage);
				String response = f.updateFeature(seasonID2, featureId, json.toString(), sessionToken);
				if (response.contains("error"))
					return response;
			}
		}
		return null;
	}
	
		
	@AfterTest
	private void reset(){
		baseUtils.reset(productID, sessionToken);
	}
		
}
