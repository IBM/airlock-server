package tests.restapi.analytics_in_branch;

import java.io.IOException;










import org.apache.commons.lang3.RandomStringUtils;
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
import tests.restapi.ProductsRestApi;
import tests.restapi.RuntimeDateUtilities;
import tests.restapi.RuntimeRestApi;


public class AddAttributesToAnalytics {
	protected String seasonID;
	protected String productID;
	protected String featureID1;
	protected String featureID2;
	private String featureID3;
	protected String configID1;
	protected String configID2;
	protected String filePath;
	protected String m_url;
	protected ProductsRestApi p;
	protected FeaturesRestApi f;
	protected AnalyticsRestApi an;
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
	
	//check add attributes as a separate action in api
	/*
	 * - add dev & prod attr in master
	 * - remove these features in branch (fail)
	 * - remove dev & prod in master
	 * - add dev & prod in branch
	 * - remove dev & prod in branch
	* 	-add in branch, then add in master (should be ok, counter not updated)
	 * - feature reported in both master and branch can be removed from master and should remain in branch
	 * - report feature in master & branch and uncheck it from branch, it remains in analytics in branch
	 * - add new feature with attr to branch and add it to analytics
	 * - remove new feature from analytics in branch
	 */
	
	@Test (description="Add feature and 2 configuration rules to the season")
	public void addComponents() throws Exception{
		experimentID = addExperiment("experiment."+RandomStringUtils.randomAlphabetic(5), false);
		Assert.assertFalse(experimentID.contains("error"), "Experiment was not created: " + experimentID);
		
		branchID = addBranch("branch1");
		Assert.assertFalse(branchID.contains("error"), "Branch1 was not created: " + branchID);

		variantID = addVariant("variant1", "branch1");
		Assert.assertFalse(variantID.contains("error"), "Variant1 was not created: " + variantID);

		//enable experiment
		String airlockExperiment = exp.getExperiment(experimentID, sessionToken);
		Assert.assertFalse(airlockExperiment.contains("error"), "Experiment was not found: " + experimentID);

		JSONObject expJson = new JSONObject(airlockExperiment);
		expJson.put("enabled", true);
		
		String response = exp.updateExperiment(experimentID, expJson.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Experiment was not updated: " + response);		


		String feature = FileUtils.fileToString(filePath + "feature1.txt", "UTF-8", false);
		featureID1 = f.addFeature(seasonID, feature, "ROOT", sessionToken);
		Assert.assertFalse(featureID1.contains("error"), "feature1 was not added to the season" + featureID1);

		
		JSONObject feature2 = new JSONObject(FileUtils.fileToString(filePath + "feature2.txt", "UTF-8", false));
		feature2.put("stage", "PRODUCTION");
		featureID2 = f.addFeature(seasonID, feature2.toString(), "ROOT", sessionToken);
		Assert.assertFalse(featureID2.contains("error"), "feature2 was not added to the season" + featureID1);

		
		//add first configuration
		String config = FileUtils.fileToString(filePath + "configuration_rule1.txt", "UTF-8", false);
		JSONObject jsonConfig = new JSONObject(config);
		JSONObject newConfiguration = new JSONObject();
		newConfiguration.put("color", "red");
		newConfiguration.put("size", "small");
		jsonConfig.put("configuration", newConfiguration);
		configID1 = f.addFeature(seasonID, jsonConfig.toString(), featureID1, sessionToken);
		Assert.assertFalse(configID1.contains("error"), "Configuration1 was not added to the season" + configID1);
		
		//add second configuration
		config = FileUtils.fileToString(filePath + "configuration_rule2.txt", "UTF-8", false);
		jsonConfig = new JSONObject(config);
		newConfiguration = new JSONObject();
		newConfiguration.put("title", "test");
		jsonConfig.put("configuration", newConfiguration);
		configID2 = f.addFeature(seasonID, jsonConfig.toString(), featureID2, sessionToken);
		Assert.assertFalse(configID2.contains("error"), "Configuration1 was not added to the season" + configID2);

	}
	
	@Test (dependsOnMethods="addComponents", description="Add attributes to analytics in master")
	public void addAttributesToAnalyticsInMaster() throws IOException, JSONException, InterruptedException{
		
		String dateFormat = an.setDateFormat();
		
		//add  attribute to analytics
		JSONArray attributes = new JSONArray();
		JSONObject attr1 = new JSONObject();
		attr1.put("name", "color");
		attr1.put("type", "REGULAR");
		attributes.add(attr1);
		JSONObject attr2 = new JSONObject();
		attr2.put("name", "size");
		attr2.put("type", "REGULAR");
		attributes.add(attr2);
		
		String response = an.addAttributesToAnalytics(featureID1, BranchesRestApi.MASTER, attributes.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Attributes were not added to analytics" + response);				
		JSONArray attributesProd = new JSONArray();
		JSONObject attr3 = new JSONObject();
		attr3.put("name", "title");
		attr3.put("type", "REGULAR");
		attributesProd.add(attr3);
		
		response = an.addAttributesToAnalytics(featureID2, BranchesRestApi.MASTER, attributesProd.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Attributes were not added to analytics" + response);		

		
		String anResponse = an.getGlobalDataCollection(seasonID, BranchesRestApi.MASTER, "BASIC", sessionToken);
		Assert.assertTrue(validateAttributeInAnalytics(anResponse)==3, "Incorrect number of attributes in analytics");

		anResponse = an.getGlobalDataCollection(seasonID, branchID, "BASIC", sessionToken);
		Assert.assertTrue(validateAttributeInAnalytics(anResponse)==3, "Incorrect number of attributes in analytics in branch");
		
		
		//check if files were changed
		an.setSleep();
		RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getDevelopmentFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseDev.code ==200, "Runtime development feature file was not updated");
		Assert.assertTrue(getAttributesInFeatureRuntimeInBranch(responseDev.message, featureID1)==0, "Incorrect number of attributes in dev runtime for branches");
		Assert.assertTrue(getAttributesInFeatureRuntimeInBranch(responseDev.message, featureID2)==0, "Incorrect number of attributes in dev runtime for branches");
		RuntimeRestApi.DateModificationResults responseProd = RuntimeDateUtilities.getProductionFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseProd.code ==200, "Runtime production feature file was changed");
		Assert.assertTrue(getAttributesInFeatureRuntimeInBranch(responseDev.message, featureID2)==0, "Incorrect number of attributes in prod runtime for branches");
		RuntimeRestApi.DateModificationResults prodChanged = RuntimeDateUtilities.getProductionChangedDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(prodChanged.code ==200, "productionChanged.txt file was changed");
		
		//branches		
		RuntimeRestApi.DateModificationResults branchesRuntimeDev = RuntimeDateUtilities.getDevelopmentBranchFileDateModification(m_url, productID, seasonID, branchID, dateFormat, sessionToken);
		Assert.assertTrue(branchesRuntimeDev.code==304, "Branch runtime development file was not changed");		
		RuntimeRestApi.DateModificationResults branchesRuntimeProd = RuntimeDateUtilities.getProductionBranchFileDateModification(m_url, productID, seasonID, branchID, dateFormat, sessionToken);
		Assert.assertTrue(branchesRuntimeProd.code==304, "Branch runtime production file was changed");
		
	}
		
	@Test (dependsOnMethods="addAttributesToAnalyticsInMaster", description="Remove reported attributes from analytics")
	public void removeAttributesFromAnalyticsInMaster() throws IOException, JSONException, InterruptedException{
		String dateFormat = an.setDateFormat();
		
		JSONArray attributes = new JSONArray();
		String response = an.addAttributesToAnalytics(featureID1, BranchesRestApi.MASTER, attributes.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Incorrect analytics response: " + response);
		response = an.addAttributesToAnalytics(featureID2, BranchesRestApi.MASTER, attributes.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Incorrect analytics response: " + response);
		
		//validate analytics
		String analytics = an.getGlobalDataCollection(seasonID, BranchesRestApi.MASTER, "BASIC", sessionToken);
		JSONObject json = new JSONObject(analytics);
		Assert.assertTrue(json.getJSONObject("analyticsDataCollection").getJSONArray("featuresAttributesForAnalytics").size()==0, "Analytics was not updated in master");

		analytics = an.getGlobalDataCollection(seasonID, branchID, "BASIC", sessionToken);
		json = new JSONObject(analytics);
		Assert.assertTrue(json.getJSONObject("analyticsDataCollection").getJSONArray("featuresAttributesForAnalytics").size()==0, "Analytics was not updated in branch");
		
		
		an.setSleep();
		RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getDevelopmentFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseDev.code ==200, "Runtime development feature file was not updated");
		Assert.assertTrue(getAttributesInFeatureRuntimeInBranch(responseDev.message, featureID1)==0, "Incorrect number of attributes in dev runtime for branches");
		Assert.assertTrue(getAttributesInFeatureRuntimeInBranch(responseDev.message, featureID2)==0, "Incorrect number of attributes in dev runtime for branches");
		RuntimeRestApi.DateModificationResults responseProd = RuntimeDateUtilities.getProductionFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseProd.code ==200, "Runtime production feature file was changed");
		Assert.assertTrue(getAttributesInFeatureRuntimeInBranch(responseDev.message, featureID2)==0, "Incorrect number of attributes in dev runtime for branches");
		RuntimeRestApi.DateModificationResults prodChanged = RuntimeDateUtilities.getProductionChangedDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(prodChanged.code ==200, "productionChanged.txt file was changed");
		
		//branches		
		RuntimeRestApi.DateModificationResults branchesRuntimeDev = RuntimeDateUtilities.getDevelopmentBranchFileDateModification(m_url, productID, seasonID, branchID, dateFormat, sessionToken);
		Assert.assertTrue(branchesRuntimeDev.code==304, "Branch runtime development file was not changed");		
		RuntimeRestApi.DateModificationResults branchesRuntimeProd = RuntimeDateUtilities.getProductionBranchFileDateModification(m_url, productID, seasonID, branchID, dateFormat, sessionToken);
		Assert.assertTrue(branchesRuntimeProd.code==304, "Branch runtime production file was changed");
		

	}
	
	@Test (dependsOnMethods="removeAttributesFromAnalyticsInMaster", description="Add unchecked attributes to analytics in branch")
	public void addUncheckedAttributesToAnalyticsInBranch() throws IOException, JSONException, InterruptedException{
		String dateFormat = an.setDateFormat();
		
		//add  attribute to analytics
		JSONArray attributes = new JSONArray();
		JSONObject attr1 = new JSONObject();
		attr1.put("name", "color");
		attr1.put("type", "REGULAR");
		attributes.add(attr1);
		JSONObject attr2 = new JSONObject();
		attr2.put("name", "size");
		attr2.put("type", "REGULAR");
		attributes.add(attr2);
		
		String response = an.addAttributesToAnalytics(featureID1, branchID, attributes.toString(), sessionToken);
		Assert.assertTrue(response.contains("error"), "Attributes were not added to analytics" + response);		
		
			
		//check if files were changed
		an.setSleep();
		RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getDevelopmentFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseDev.code ==304, "Runtime development feature file was not updated");
		
		RuntimeRestApi.DateModificationResults responseProd = RuntimeDateUtilities.getProductionFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseProd.code ==304, "Runtime production feature file was changed");
		
		RuntimeRestApi.DateModificationResults prodChanged = RuntimeDateUtilities.getProductionChangedDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(prodChanged.code ==304, "productionChanged.txt file was changed");
		
		//branches		
		RuntimeRestApi.DateModificationResults branchesRuntimeDev = RuntimeDateUtilities.getDevelopmentBranchFileDateModification(m_url, productID, seasonID, branchID, dateFormat, sessionToken);
		Assert.assertTrue(branchesRuntimeDev.code==304, "Branch runtime development file was not changed");		
	
		RuntimeRestApi.DateModificationResults branchesRuntimeProd = RuntimeDateUtilities.getProductionBranchFileDateModification(m_url, productID, seasonID, branchID, dateFormat, sessionToken);
		Assert.assertTrue(branchesRuntimeProd.code==304, "Branch runtime production file was changed");
		
	}
	
	@Test (dependsOnMethods="addUncheckedAttributesToAnalyticsInBranch", description="Add attributes to analytics in branch")
	public void addAttributesToAnalytics() throws IOException, JSONException, InterruptedException{
		br.checkoutFeature(branchID, featureID1, sessionToken);
		br.checkoutFeature(branchID, featureID2, sessionToken);
		
		String dateFormat = an.setDateFormat();
		
		//add  attribute to analytics
		JSONArray attributes = new JSONArray();
		JSONObject attr1 = new JSONObject();
		attr1.put("name", "color");
		attr1.put("type", "REGULAR");
		attributes.add(attr1);
		JSONObject attr2 = new JSONObject();
		attr2.put("name", "size");
		attr2.put("type", "REGULAR");
		attributes.add(attr2);
		
		String response = an.addAttributesToAnalytics(featureID1, branchID, attributes.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Attributes were not added to analytics" + response);		
		
		JSONArray attributesProd = new JSONArray();
		JSONObject attr3 = new JSONObject();
		attr3.put("name", "title");
		attr3.put("type", "REGULAR");
		attributesProd.add(attr3);
		
		response = an.addAttributesToAnalytics(featureID2, branchID, attributesProd.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Attributes were not added to analytics" + response);		
				
		JSONObject anResponse = new JSONObject(an.getGlobalDataCollection(seasonID, BranchesRestApi.MASTER, "BASIC", sessionToken));
		Assert.assertTrue(anResponse.getJSONObject("analyticsDataCollection").getJSONArray("featuresAttributesForAnalytics").size()==0, "Incorrect number of attributes in analytics in master");

		String anResponseInBranch = an.getGlobalDataCollection(seasonID, branchID, "BASIC", sessionToken);
		Assert.assertTrue(validateAttributeInAnalytics(anResponseInBranch)==3, "Incorrect number of attributes in analytics in branch");

		
		//check if files were changed
		an.setSleep();
		RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getDevelopmentFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseDev.code ==200, "Runtime development feature file was not updated");
		Assert.assertTrue(getAttributesInFeatureRuntimeInExperiment(responseDev.message, featureID1)==2, "Incorrect number of attributes in dev runtime for branches");		
		RuntimeRestApi.DateModificationResults responseProd = RuntimeDateUtilities.getProductionFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseProd.code ==304, "Runtime production feature file was changed");
		RuntimeRestApi.DateModificationResults prodChanged = RuntimeDateUtilities.getProductionChangedDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(prodChanged.code ==304, "productionChanged.txt file was changed");
		
		//branches		
		RuntimeRestApi.DateModificationResults branchesRuntimeDev = RuntimeDateUtilities.getDevelopmentBranchFileDateModification(m_url, productID, seasonID, branchID, dateFormat, sessionToken);
		Assert.assertTrue(branchesRuntimeDev.code==200, "Branch runtime development file was not changed");		
		Assert.assertTrue(getAttributesInRuntimeBranch(branchesRuntimeDev.message, featureID1)==2, "Incorrect number of attributes in development branches runtime");
		RuntimeRestApi.DateModificationResults branchesRuntimeProd = RuntimeDateUtilities.getProductionBranchFileDateModification(m_url, productID, seasonID, branchID, dateFormat, sessionToken);
		Assert.assertTrue(branchesRuntimeProd.code==200, "Branch runtime production file was changed");
		Assert.assertTrue(getAttributesInRuntimeBranch(branchesRuntimeProd.message, featureID2)==1, "Incorrect number of attributes in production branches runtime");
		
	}
	
	@Test (dependsOnMethods="addAttributesToAnalytics", description="Remove reported attributes from analytics in branch")
	public void removeAttributesFromAnalyticsInBranch() throws IOException, JSONException, InterruptedException{
		String dateFormat = an.setDateFormat();
		
		JSONArray attributes = new JSONArray();
		String response = an.addAttributesToAnalytics(featureID1, branchID, attributes.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Incorrect analytics response: " + response);
		response = an.addAttributesToAnalytics(featureID2, branchID, attributes.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Incorrect analytics response: " + response);
		
		//validate analytics
		String analytics = an.getGlobalDataCollection(seasonID, BranchesRestApi.MASTER, "BASIC", sessionToken);
		JSONObject json = new JSONObject(analytics);
		Assert.assertTrue(json.getJSONObject("analyticsDataCollection").getJSONArray("featuresAttributesForAnalytics").size()==0, "Analytics was not updated in master");

		analytics = an.getGlobalDataCollection(seasonID, branchID, "BASIC", sessionToken);
		json = new JSONObject(analytics);
		Assert.assertTrue(json.getJSONObject("analyticsDataCollection").getJSONArray("featuresAttributesForAnalytics").size()==0, "Analytics was not updated in branch");

		an.setSleep();
		
		RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getDevelopmentFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseDev.code ==200, "Runtime development feature file was not updated");
		Assert.assertTrue(getAttributesInFeatureRuntimeInExperiment(responseDev.message, featureID1)==0, "Incorrect number of attributes in dev runtime for branches");
		RuntimeRestApi.DateModificationResults responseProd = RuntimeDateUtilities.getProductionFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseProd.code ==304, "Runtime production feature file was not changed");
		RuntimeRestApi.DateModificationResults prodChanged = RuntimeDateUtilities.getProductionChangedDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(prodChanged.code ==304, "productionChanged.txt file was not changed");
		
		//branches		
		RuntimeRestApi.DateModificationResults branchesRuntimeDev = RuntimeDateUtilities.getDevelopmentBranchFileDateModification(m_url, productID, seasonID, branchID, dateFormat, sessionToken);
		Assert.assertTrue(branchesRuntimeDev.code==200, "Branch runtime development file was not changed");		
		Assert.assertTrue(getAttributesInRuntimeBranch(branchesRuntimeDev.message, featureID1)==0, "Incorrect number of attributes in development branches runtime");
		RuntimeRestApi.DateModificationResults branchesRuntimeProd = RuntimeDateUtilities.getProductionBranchFileDateModification(m_url, productID, seasonID, branchID, dateFormat, sessionToken);
		Assert.assertTrue(branchesRuntimeProd.code==200, "Branch runtime production file was changed");
		Assert.assertTrue(getAttributesInRuntimeBranch(branchesRuntimeDev.message, featureID2)==0, "Incorrect number of attributes in production branches runtime");
	}
	
	
	@Test (dependsOnMethods="removeAttributesFromAnalyticsInBranch", description="Add attributes to analytics in branch and then in master")
	public void addAttributesToAnalyticsInBranchAndMaster() throws IOException, JSONException, InterruptedException{
		br.checkoutFeature(branchID, featureID1, sessionToken);
		
		String dateFormat = an.setDateFormat();
		
		//add  attribute to analytics
		JSONArray attributes = new JSONArray();
		JSONObject attr1 = new JSONObject();
		attr1.put("name", "color");
		attr1.put("type", "REGULAR");
		attributes.add(attr1);
		JSONObject attr2 = new JSONObject();
		attr2.put("name", "size");
		attr2.put("type", "REGULAR");
		attributes.add(attr2);
		
		//add in branch
		
		String response = an.addAttributesToAnalytics(featureID1, branchID, attributes.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Attributes were not added to analytics" + response);		
		
		JSONObject anResponse = new JSONObject(an.getGlobalDataCollection(seasonID, BranchesRestApi.MASTER, "BASIC", sessionToken));
		Assert.assertTrue(anResponse.getJSONObject("analyticsDataCollection").getJSONArray("featuresAttributesForAnalytics").size()==0, "Incorrect number of attributes in analytics in master");

		String anResponseInBranch = an.getGlobalDataCollection(seasonID, branchID, "BASIC", sessionToken);
		Assert.assertTrue(validateAttributeInAnalytics(anResponseInBranch)==2, "Incorrect number of attributes in analytics in branch");

		//add in master
		response = an.addAttributesToAnalytics(featureID1, BranchesRestApi.MASTER, attributes.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Attributes were not added to analytics in master " + response);
		String resp = an.getGlobalDataCollection(seasonID, BranchesRestApi.MASTER, "BASIC", sessionToken);
		Assert.assertTrue(validateAttributeInAnalytics(resp)==2, "Incorrect number of attributes in analytics in branch");

		anResponseInBranch = an.getGlobalDataCollection(seasonID, branchID, "BASIC", sessionToken);
		Assert.assertTrue(validateAttributeInAnalytics(anResponseInBranch)==2, "Incorrect number of attributes in analytics in branch");
		
		//check if files were changed
		an.setSleep();
		RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getDevelopmentFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseDev.code ==200, "Runtime development feature file was not updated");
		Assert.assertTrue(getAttributesInFeatureRuntimeInExperiment(responseDev.message, featureID1)==2, "Incorrect number of attributes in dev runtime for branches");
		RuntimeRestApi.DateModificationResults responseProd = RuntimeDateUtilities.getProductionFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseProd.code ==304, "Runtime production feature file was changed");
		RuntimeRestApi.DateModificationResults prodChanged = RuntimeDateUtilities.getProductionChangedDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(prodChanged.code ==304, "productionChanged.txt file was changed");
		
		//branches		
		RuntimeRestApi.DateModificationResults branchesRuntimeDev = RuntimeDateUtilities.getDevelopmentBranchFileDateModification(m_url, productID, seasonID, branchID, dateFormat, sessionToken);
		Assert.assertTrue(branchesRuntimeDev.code==200, "Branch runtime development file was not changed");		
		Assert.assertTrue(getAttributesInRuntimeBranch(branchesRuntimeDev.message, featureID1)==2, "Incorrect number of attributes in development branches runtime");
		RuntimeRestApi.DateModificationResults branchesRuntimeProd = RuntimeDateUtilities.getProductionBranchFileDateModification(m_url, productID, seasonID, branchID, dateFormat, sessionToken);
		Assert.assertTrue(branchesRuntimeProd.code==304, "Branch runtime production file was changed");
		
	}
	
	@Test (dependsOnMethods="addAttributesToAnalyticsInBranchAndMaster", description="Uncheck feature in branch, it remains in branch analytics")
	public void uncheckFeature() throws IOException, JSONException, InterruptedException{		
		br.cancelCheckoutFeature(branchID, featureID1, sessionToken);
		
		String dateFormat = an.setDateFormat();

		String anResponse = an.getGlobalDataCollection(seasonID, BranchesRestApi.MASTER, "BASIC", sessionToken);
		Assert.assertTrue(validateAttributeInAnalytics(anResponse)==2, "Incorrect number of attributes in analytics in master");
		String anResponseInBranch = an.getGlobalDataCollection(seasonID, branchID, "BASIC", sessionToken);
		Assert.assertTrue(validateAttributeInAnalytics(anResponseInBranch)==2, "Incorrect number of attributes in analytics in branch");
		
		//check if files were changed
		an.setSleep();
		RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getDevelopmentFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseDev.code ==304, "Runtime development feature file was not updated");
		RuntimeRestApi.DateModificationResults responseProd = RuntimeDateUtilities.getProductionFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseProd.code ==304, "Runtime production feature file was changed");
		RuntimeRestApi.DateModificationResults prodChanged = RuntimeDateUtilities.getProductionChangedDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(prodChanged.code ==304, "productionChanged.txt file was changed");
		
		//branches		
		RuntimeRestApi.DateModificationResults branchesRuntimeDev = RuntimeDateUtilities.getDevelopmentBranchFileDateModification(m_url, productID, seasonID, branchID, dateFormat, sessionToken);
		Assert.assertTrue(branchesRuntimeDev.code==304, "Branch runtime development file was  changed");		
		RuntimeRestApi.DateModificationResults branchesRuntimeProd = RuntimeDateUtilities.getProductionBranchFileDateModification(m_url, productID, seasonID, branchID, dateFormat, sessionToken);
		Assert.assertTrue(branchesRuntimeProd.code==304, "Branch runtime production file was changed");
		
	}

	@Test (dependsOnMethods="uncheckFeature", description="Delete feature  in master - it should remain in branch")
	public void deleteFeatureInMaster() throws Exception{
		
		//checkout feature1 again
		String response = br.checkoutFeature(branchID, featureID1, sessionToken);
		Assert.assertFalse(response.contains("error"), "can't checkout feature to branch");
		
		JSONObject masterFeature = new JSONObject(f.getFeature(featureID1, sessionToken));
		
		String dateFormat = an.setDateFormat();
		
		int code = f.deleteFeature(featureID1, sessionToken);
		Assert.assertTrue(code==200, "feature was not deleted from master");
		
		String anResponse =an.getGlobalDataCollection(seasonID, BranchesRestApi.MASTER, "BASIC", sessionToken);
		Assert.assertTrue(validateAttributeInAnalytics(anResponse)==0, "Incorrect number of attributes in analytics in master");
		
		//find feature1 in branch
		JSONObject branch = new JSONObject(br.getBranchWithFeatures(branchID, sessionToken));
		JSONArray features = branch.getJSONArray("features");
		String newId = "";
		for (int i=0; i< features.size(); i++){
			if (features.getJSONObject(i).getString("name").equals(masterFeature.getString("name"))){
				newId = features.getJSONObject(i).getString("uniqueId");
			}
		}


		String anResponseInBranch = an.getGlobalDataCollection(seasonID, branchID, "BASIC", sessionToken);
		Assert.assertTrue(validateAttributeInAnalytics(anResponseInBranch)==2, "Incorrect number of attributes in analytics in branch");
		JSONObject jsonAnalytics = new JSONObject(anResponseInBranch);
		boolean found = false;
		for (int i=0; i< jsonAnalytics.getJSONObject("analyticsDataCollection").getJSONArray("featuresAttributesForAnalytics").size(); i++){
			if (jsonAnalytics.getJSONObject("analyticsDataCollection").getJSONArray("featuresAttributesForAnalytics").getJSONObject(i).getString("id").equals(newId))
				found = true;
		}
		Assert.assertTrue(found, "Incorrect feature id in analytics");
		
		//check if files were changed
		an.setSleep();
		RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getDevelopmentFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseDev.code ==200, "Runtime development feature file was not updated");
		Assert.assertTrue(getAttributesInFeatureRuntimeInExperiment(responseDev.message, newId)==2, "Incorrect number of attributes in dev runtime for branches");
		RuntimeRestApi.DateModificationResults responseProd = RuntimeDateUtilities.getProductionFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseProd.code ==304, "Runtime production feature file was changed");
		RuntimeRestApi.DateModificationResults prodChanged = RuntimeDateUtilities.getProductionChangedDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(prodChanged.code ==304, "productionChanged.txt file was changed");
		
		//branches		
		RuntimeRestApi.DateModificationResults branchesRuntimeDev = RuntimeDateUtilities.getDevelopmentBranchFileDateModification(m_url, productID, seasonID, branchID, dateFormat, sessionToken);
		Assert.assertTrue(branchesRuntimeDev.code==200, "Branch runtime development file was  changed");		
		RuntimeRestApi.DateModificationResults branchesRuntimeProd = RuntimeDateUtilities.getProductionBranchFileDateModification(m_url, productID, seasonID, branchID, dateFormat, sessionToken);
		Assert.assertTrue(branchesRuntimeProd.code==304, "Branch runtime production file was changed");
		
	}
	
	@Test (dependsOnMethods="deleteFeatureInMaster", description="Add new feature in branch")
	public void addFeatureInBranch() throws IOException, JSONException, InterruptedException{		
		JSONObject feature2 = new JSONObject(FileUtils.fileToString(filePath + "feature2.txt", "UTF-8", false));
		feature2.put("stage", "PRODUCTION");
		feature2.put("name", "newF2");
		featureID3 = f.addFeatureToBranch(seasonID, branchID, feature2.toString(), "ROOT", sessionToken);
		Assert.assertFalse(featureID3.contains("error"), "newfeature2 was not added to the branch" + featureID3);
		
		//add configuration
		String config = FileUtils.fileToString(filePath + "configuration_rule1.txt", "UTF-8", false);
		JSONObject jsonConfig = new JSONObject(config);
		jsonConfig.put("name", "newCR");
		JSONObject newConfiguration = new JSONObject();
		newConfiguration.put("color", "red");
		newConfiguration.put("size", "small");
		jsonConfig.put("configuration", newConfiguration);
		String configID = f.addFeatureToBranch(seasonID, branchID, jsonConfig.toString(), featureID3, sessionToken);
		Assert.assertFalse(configID.contains("error"), "Configuration1 was not added to the season" + configID);
		
		JSONArray attributes = new JSONArray();
		JSONObject attr1 = new JSONObject();
		attr1.put("name", "color");
		attr1.put("type", "REGULAR");
		attributes.add(attr1);
		JSONObject attr2 = new JSONObject();
		attr2.put("name", "size");
		attr2.put("type", "REGULAR");
		attributes.add(attr2);
		
		String dateFormat = an.setDateFormat();
		
		String response = an.getGlobalDataCollection(seasonID, BranchesRestApi.MASTER, "BASIC", sessionToken);
		
		String input = an.addFeaturesAttributesToAnalytics(response, featureID3, attributes);
		response = an.updateGlobalDataCollection(seasonID, branchID, input, sessionToken);
		Assert.assertFalse(response.contains("error"), "Feature was not added to analytics" + response);

		
		String anResponse = an.getGlobalDataCollection(seasonID, BranchesRestApi.MASTER, "BASIC", sessionToken);
		Assert.assertTrue(validateAttributeInAnalytics(anResponse)==0, "Incorrect number of attributes in analytics in master");

		String anResponseInBranch = an.getGlobalDataCollection(seasonID, branchID, "BASIC", sessionToken);
		Assert.assertTrue(validateAttributeInAnalytics(anResponseInBranch)==2, "Incorrect number of attributes in analytics in branch");
		
		//check if files were changed
		an.setSleep();
		RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getDevelopmentFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseDev.code ==200, "Runtime development feature file was not updated");
		Assert.assertTrue(getAttributesInFeatureRuntimeInExperiment(responseDev.message, featureID3)==2, "Incorrect number of attributes in dev runtime for branches");
		RuntimeRestApi.DateModificationResults responseProd = RuntimeDateUtilities.getProductionFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseProd.code ==304, "Runtime production feature file was changed");
		RuntimeRestApi.DateModificationResults prodChanged = RuntimeDateUtilities.getProductionChangedDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(prodChanged.code ==304, "productionChanged.txt file was changed");
		
		//branches		
		RuntimeRestApi.DateModificationResults branchesRuntimeDev = RuntimeDateUtilities.getDevelopmentBranchFileDateModification(m_url, productID, seasonID, branchID, dateFormat, sessionToken);
		Assert.assertTrue(branchesRuntimeDev.code==200, "Branch runtime development file was  changed");		
		RuntimeRestApi.DateModificationResults branchesRuntimeProd = RuntimeDateUtilities.getProductionBranchFileDateModification(m_url, productID, seasonID, branchID, dateFormat, sessionToken);
		Assert.assertTrue(branchesRuntimeProd.code==200, "Branch runtime production file was changed");
		
	}
	
	
	@Test (dependsOnMethods="addFeatureInBranch", description="Report to analytics feature that exists only in branch")
	public void addBranchFeatureToMaster() throws IOException, JSONException, InterruptedException{		

		JSONArray attributes = new JSONArray();
		JSONObject attr1 = new JSONObject();
		attr1.put("name", "color");
		attr1.put("type", "REGULAR");
		attributes.add(attr1);
		JSONObject attr2 = new JSONObject();
		attr2.put("name", "size");
		attr2.put("type", "REGULAR");
		attributes.add(attr2);
		
		String response = an.getGlobalDataCollection(seasonID, BranchesRestApi.MASTER, "BASIC", sessionToken);
		
		String input = an.addFeaturesAttributesToAnalytics(response, featureID3, attributes);
		response = an.updateGlobalDataCollection(seasonID, BranchesRestApi.MASTER,  input, sessionToken);
		Assert.assertTrue(response.contains("error"), "Branch feature was  added to analytics in master");
		
	}
	
	
	@Test (dependsOnMethods="addBranchFeatureToMaster", description="Remove feature attributes from analytics in branch")
	public void removeFeatureInBranch() throws IOException, JSONException, InterruptedException{		
		String dateFormat = an.setDateFormat();
		
		JSONArray attributes = new JSONArray();
		String response = an.addAttributesToAnalytics(featureID3, branchID, attributes.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Incorrect analytics response: " + response);
	
		String anResponse = an.getGlobalDataCollection(seasonID, BranchesRestApi.MASTER, "BASIC", sessionToken);
		Assert.assertTrue(validateAttributeInAnalytics(anResponse)==0, "Incorrect number of attributes in analytics in master");

		String anResponseInBranch = an.getGlobalDataCollection(seasonID, branchID, "BASIC", sessionToken);
		Assert.assertTrue(validateAttributeInAnalytics(anResponseInBranch)==0, "Incorrect number of attributes in analytics in branch");
		
		//check if files were changed
		an.setSleep();
		RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getDevelopmentFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseDev.code ==200, "Runtime development feature file was not updated");
		RuntimeRestApi.DateModificationResults responseProd = RuntimeDateUtilities.getProductionFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseProd.code ==304, "Runtime production feature file was changed");
		RuntimeRestApi.DateModificationResults prodChanged = RuntimeDateUtilities.getProductionChangedDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(prodChanged.code ==304, "productionChanged.txt file was changed");
		
		//branches		
		RuntimeRestApi.DateModificationResults branchesRuntimeDev = RuntimeDateUtilities.getDevelopmentBranchFileDateModification(m_url, productID, seasonID, branchID, dateFormat, sessionToken);
		Assert.assertTrue(branchesRuntimeDev.code==200, "Branch runtime development file was  changed");		
		RuntimeRestApi.DateModificationResults branchesRuntimeProd = RuntimeDateUtilities.getProductionBranchFileDateModification(m_url, productID, seasonID, branchID, dateFormat, sessionToken);
		Assert.assertTrue(branchesRuntimeProd.code==200, "Branch runtime production file was changed");
		
	}
	

	private int getAttributesInFeatureRuntimeInBranch(String input, String featureId) throws JSONException{
		//runtime dev/prod branches section
		JSONArray features = new JSONObject(input).getJSONArray("branches").getJSONObject(0).getJSONArray("features");
		for (int i=0; i< features.size(); i++){
			if (features.getJSONObject(i).getString("uniqueId").equals(featureId) && features.getJSONObject(i).containsKey("configAttributesForAnalytics")){
				return features.getJSONObject(i).getJSONArray("configAttributesForAnalytics").size();
			}
		}	
		return 0; //if no configAttributesToAnalytics
	}
	
	private int getAttributesInFeatureRuntimeInExperiment(String input, String featureId) throws JSONException{
		//runtime dev/prod experiments section
		JSONObject feature = new JSONObject(f.getFeatureFromBranch(featureId, branchID, sessionToken));
		String name = feature.getString("namespace") + "." + feature.getString("name");
		
		JSONArray features = new JSONObject(input).getJSONObject("experiments").getJSONArray("experiments").getJSONObject(0).getJSONObject("analytics").getJSONArray("featuresAttributesForAnalytics");
		for (int i=0; i< features.size(); i++){
			if (features.getJSONObject(i).getString("name").equals(name) && features.getJSONObject(i).containsKey("attributes")){
				return features.getJSONObject(i).getJSONArray("attributes").size();
			}
		}	
		return 0; //if no attributes
	}
	
	private int getAttributesInRuntimeBranch(String input, String featureId ) throws JSONException{
		//runtime branches
		JSONArray features = new JSONObject(input).getJSONArray("features");

		for (int i=0; i< features.size(); i++){
			if (features.getJSONObject(i).getString("uniqueId").equals(featureId) && features.getJSONObject(i).containsKey("configAttributesForAnalytics")){
				return features.getJSONObject(i).getJSONArray("configAttributesForAnalytics").size();
			}
		}	
		return 0; //if no configAttributesToAnalytics
	}


	private int validateAttributeInAnalytics(String analytics) throws JSONException{
		//runtime dev/prod
		int attributes=0;
		JSONObject json = new JSONObject(analytics);
		JSONObject analyticsDataCollection = json.getJSONObject("analyticsDataCollection");
		JSONArray featuresAttributesToAnalytics = analyticsDataCollection.getJSONArray("featuresAttributesForAnalytics");
		for(int i=0; i<featuresAttributesToAnalytics.size(); i++){
			attributes = attributes + featuresAttributesToAnalytics.getJSONObject(i).getJSONArray("attributes").size();
		}	
		return attributes;
	}
	
	private String addExperiment(String experimentName, boolean enabled) throws IOException, JSONException{
		return baseUtils.addExperiment(m_analyticsUrl, false, enabled);

	}
	

	private String addVariant(String variantName, String branchName) throws IOException, JSONException{
		String variant = FileUtils.fileToString(filePath + "experiments/variant1.txt", "UTF-8", false);
		JSONObject variantJson = new JSONObject(variant);
		variantJson.put("name", variantName);
		variantJson.put("branchName", branchName);
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
