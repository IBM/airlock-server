package tests.restapi.analytics_in_purchases;

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
import tests.restapi.*;

public class AddAttributesToAnalyticsInDevelopmentExperiment {
	protected String seasonID1;
	protected String seasonID2;
	protected String experimentID;
	protected String variantID;
	protected String branchID1;
	protected String branchID2;
	protected String productID;
	protected String entitlementID1;
	protected String configID1;
	protected String entitlementID2;
	protected String configID2;
	protected String filePath;
	protected String m_branchType;
	protected String m_url;
	protected ProductsRestApi p;
	protected InAppPurchasesRestApi purchasesApi;
	protected AnalyticsRestApi an;
	private String sessionToken = "";
	protected AirlockUtils baseUtils;
	private BranchesRestApi br ;
	private ExperimentsRestApi exp ;
	private String m_analyticsUrl;
	private SeasonsRestApi s;
	
	@BeforeClass
	@Parameters({"url", "analyticsUrl", "translationsUrl", "configPath", "sessionToken", "userName", "userPassword", "appName", "productsToDeleteFile"})
	public void init(String url, String analyticsUrl, String translationsUrl, String configPath, String sToken, String userName, String userPassword, String appName, String productsToDeleteFile) throws Exception{
		m_url = url;
		m_analyticsUrl = analyticsUrl;
		filePath = configPath;
		p = new ProductsRestApi();
		p.setURL(m_url);
		purchasesApi = new InAppPurchasesRestApi();
		purchasesApi.setURL(m_url);
		an = new AnalyticsRestApi();
		an.setURL(analyticsUrl);
		br = new BranchesRestApi();
		br.setURL(m_url);
		exp = new ExperimentsRestApi();
		exp.setURL(m_analyticsUrl);
		s = new SeasonsRestApi();
		s.setURL(m_url);
		
        
		baseUtils = new AirlockUtils(url, analyticsUrl, translationsUrl, configPath, sToken, userName, userPassword, appName, productsToDeleteFile);
		sessionToken = baseUtils.sessionToken;

		productID = baseUtils.createProduct();
		baseUtils.printProductToFile(productID);
		JSONObject season = new JSONObject();
		season.put("minVersion", "1.0");
		seasonID1 = s.addSeason(productID, season.toString(), sessionToken);

		
	}
	
	
	@Test (description="Add season2")
	public void addSeason() throws Exception{
		String season = "{\"minVersion\":\"2.0\"}";
		seasonID2 = s.addSeason(productID, season, sessionToken);
		Assert.assertFalse(seasonID2.contains("error"), "Can't add second season: " + seasonID2);
	}
	
	@Test (dependsOnMethods="addSeason", description="Add components")
	public void addExperiment() throws Exception{
		String experiment = FileUtils.fileToString(filePath + "experiments/experiment1.txt", "UTF-8", false);
		JSONObject expJson = new JSONObject(experiment);
		
		expJson.put("name", "experiment."+RandomStringUtils.randomAlphabetic(5));
		expJson.put("minVersion", "0.5");
		expJson.put("maxVersion", "2.5");
		expJson.put("enabled", false);
		expJson.put("stage", "DEVELOPMENT");
		experimentID = exp.createExperiment(productID, expJson.toString(), sessionToken);
		Assert.assertFalse(experimentID.contains("error"), "Experiment was not created: " + experimentID);
		
		branchID1 = addBranch(seasonID1, "branch1");
		Assert.assertFalse(branchID1.contains("error"), "Branch1 was not created in season1: " + branchID1);
		
		branchID2 = addBranch(seasonID2, "branch1");
		Assert.assertFalse(branchID2.contains("error"), "Branch1 was not created in season2: " + branchID2);

		variantID = addVariant("variant1", "branch1", "DEVELOPMENT");
		Assert.assertFalse(variantID.contains("error"), "Variant1 was not created: " + variantID);
		
		//enable experiment so a range will be created and the experiment will be published to analytics server
		String airlockExperiment = exp.getExperiment(experimentID, sessionToken);
		Assert.assertFalse(airlockExperiment.contains("error"), "Experiment was not found: " + experimentID);

		expJson = new JSONObject(airlockExperiment);
		expJson.put("enabled", true);
		
		String response = exp.updateExperiment(experimentID, expJson.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Experiment was not updated: " + response);		
	}
	


	@Test (dependsOnMethods="addExperiment", description="Add feature in development to season1")
	public void addEntitlements() throws IOException, JSONException, InterruptedException{
		String dateFormat = an.setDateFormat();
		
		//add entitlements to season1, checkout Entitlement2
		String entitlement = FileUtils.fileToString(filePath + "purchases/inAppPurchase1.txt", "UTF-8", false);
		JSONObject jsonE = new JSONObject(entitlement);
		jsonE.put("name", "Entitlement1");
		jsonE.put("stage", "PRODUCTION");
		entitlementID1 = purchasesApi.addPurchaseItemToBranch(seasonID1, BranchesRestApi.MASTER, jsonE.toString(), "ROOT", sessionToken);
		Assert.assertFalse(entitlementID1.contains("error"), "Entitlement1 was not added to the season " + entitlementID1);
		
		String config = FileUtils.fileToString(filePath + "configuration_rule1.txt", "UTF-8", false);
		JSONObject jsonConfig = new JSONObject(config);
		jsonConfig.put("name", "Config1");
		jsonConfig.put("stage", "PRODUCTION");
		JSONObject newConfiguration = new JSONObject();
		newConfiguration.put("title", "stam");
		jsonConfig.put("configuration", newConfiguration);
		configID1 = purchasesApi.addPurchaseItemToBranch(seasonID1, BranchesRestApi.MASTER, jsonConfig.toString(), entitlementID1, sessionToken);
		Assert.assertFalse(configID1.contains("error"), "Configuration1 was not added to master season1 " + configID1);
		
		//add attribute to analytics in master season1
		JSONArray attributes = new JSONArray();
		JSONObject attr1 = new JSONObject();
		attr1.put("name", "title");
		attr1.put("type", "REGULAR");
		attributes.add(attr1);
		String response = an.addAttributesToAnalytics(entitlementID1, BranchesRestApi.MASTER, attributes.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Attributes were not added to analytics" + response);		
		
		//Entitlement2
		jsonE.put("name", "Entitlement2");
		entitlementID2 = purchasesApi.addPurchaseItemToBranch(seasonID1, BranchesRestApi.MASTER, jsonE.toString(), "ROOT", sessionToken);
		Assert.assertFalse(entitlementID2.contains("error"), "Entitlement2 was not added to the season " + entitlementID2);
		
		jsonConfig.put("name", "Config2");
		JSONObject newConfiguration2 = new JSONObject();
		newConfiguration2.put("color", "red");
		jsonConfig.put("configuration", newConfiguration2);
		configID2 = purchasesApi.addPurchaseItemToBranch(seasonID1, BranchesRestApi.MASTER, jsonConfig.toString(), entitlementID2, sessionToken);
		Assert.assertFalse(configID2.contains("error"), "Configuration2 was not added to master season1 " + configID2);

		br.checkoutFeature(branchID1, entitlementID2, sessionToken);
		
		//add attribute to analytics in branch1
		attributes = new JSONArray();
		JSONObject attr2 = new JSONObject();
		attr2.put("name", "color");
		attr2.put("type", "REGULAR");
		attributes.add(attr2);
		response = an.addAttributesToAnalytics(entitlementID2, branchID1, attributes.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Attributes were not added to analytics" + response);
			
		//Entitlement3
		jsonE.put("name", "Entitlement3");
		String entitlementID3 = purchasesApi.addPurchaseItemToBranch(seasonID2, BranchesRestApi.MASTER, jsonE.toString(), "ROOT", sessionToken);
		Assert.assertFalse(entitlementID3.contains("error"), "Entitlement3 was not added to the season2 " + entitlementID3);
		
		jsonConfig.put("name", "Config3");
		JSONObject newConfiguration3 = new JSONObject();
		newConfiguration3.put("size", "small");
		jsonConfig.put("configuration", newConfiguration3);
		String configID3 = purchasesApi.addPurchaseItemToBranch(seasonID2, BranchesRestApi.MASTER, jsonConfig.toString(), entitlementID3, sessionToken);
		Assert.assertFalse(configID3.contains("error"), "Configuration3 was not added to master season1 " + configID3);
		
		//add attribute to analytics in branch1
		attributes = new JSONArray();
		JSONObject attr3 = new JSONObject();
		attr3.put("name", "size");
		attr3.put("type", "REGULAR");
		attributes.add(attr3);
		response = an.addAttributesToAnalytics(entitlementID3, BranchesRestApi.MASTER, attributes.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Attributes were not added to analytics" + response);

		//Entitlement4
		jsonE.put("name", "Entitlement4");
		String featureID4 = purchasesApi.addPurchaseItemToBranch(seasonID2, branchID2, jsonE.toString(), "ROOT", sessionToken);
		Assert.assertFalse(featureID4.contains("error"), "Entitlement4 was not added to branch2 " + featureID4);
		
		jsonConfig.put("name", "Config4");
		JSONObject newConfiguration4 = new JSONObject();
		newConfiguration4.put("text", "small");
		jsonConfig.put("configuration", newConfiguration4);
		String configID4 = purchasesApi.addPurchaseItemToBranch(seasonID2, branchID2, jsonConfig.toString(), featureID4, sessionToken);
		Assert.assertFalse(configID4.contains("error"), "Configuration4 was not added to branch2 " + configID4);
		
		//add attribute to analytics in branch1
		attributes = new JSONArray();
		JSONObject attr4 = new JSONObject();
		attr4.put("name", "text");
		attr4.put("type", "REGULAR");
		attributes.add(attr4);
		response = an.addAttributesToAnalytics(featureID4, branchID2, attributes.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Attributes were not added to analytics" + response);
		
		an.setSleep();
		RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getDevelopmentFileDateModification(m_url, productID, seasonID1, dateFormat, sessionToken);
		Assert.assertTrue(responseDev.code ==200, "Runtime development file was not updated");
		Assert.assertTrue(ifRuntimeContainsAttributes(responseDev.message, 4), "Incorrect number of attributes in the runtime development file in season1");		
		RuntimeRestApi.DateModificationResults responseProd = RuntimeDateUtilities.getProductionFileDateModification(m_url, productID, seasonID1, dateFormat, sessionToken);
		Assert.assertTrue(responseProd.code ==200, "Runtime production file was not changed");
		Assert.assertTrue(noExperimentAnalytics(responseProd.message), "Incorrect number of attributes in the runtime production file in season1");
		RuntimeRestApi.DateModificationResults prodChanged = RuntimeDateUtilities.getProductionChangedDateModification(m_url, productID, seasonID1, dateFormat, sessionToken);
		Assert.assertTrue(prodChanged.code ==200, "productionChanged.txt file was not changed");

		
		responseDev = RuntimeDateUtilities.getDevelopmentFileDateModification(m_url, productID, seasonID2, dateFormat, sessionToken);
		Assert.assertTrue(responseDev.code ==200, "Runtime development file was not updated");
		Assert.assertTrue(ifRuntimeContainsAttributes(responseDev.message, 4), "Incorrect number of attributes in the runtime development file in season2");		
		responseProd = RuntimeDateUtilities.getProductionFileDateModification(m_url, productID, seasonID2, dateFormat, sessionToken);
		Assert.assertTrue(responseProd.code ==200, "Runtime production file was not changed");	
		Assert.assertTrue(noExperimentAnalytics(responseProd.message), "Incorrect number of attributes in the runtime production file in season2");
		prodChanged = RuntimeDateUtilities.getProductionChangedDateModification(m_url, productID, seasonID2, dateFormat, sessionToken);
		Assert.assertTrue(prodChanged.code ==200, "productionChanged.txt file was not changed");

	}
	

	@Test (dependsOnMethods="addEntitlements", description="Move experiment and variant  to production")
	public void moveExperimentToProduction() throws Exception{
		String dateFormat = an.setDateFormat();

		String experiment = exp.getExperiment(experimentID, sessionToken);
		JSONObject json = new JSONObject(experiment);
		json.put("stage", "PRODUCTION");
		String response = exp.updateExperiment(experimentID, json.toString(), sessionToken);
		Assert.assertFalse (response.contains("error"), "Experiment was not updated: " + response);
		
		String variant = exp.getVariant(variantID, sessionToken);
		JSONObject jsonVar = new JSONObject(variant);
		jsonVar.put("stage", "PRODUCTION");
		String respVar = exp.updateVariant(variantID, jsonVar.toString(), sessionToken);
		Assert.assertFalse (respVar.contains("error"), "Variant was not updated: " + respVar);

		
		an.setSleep();
		RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getDevelopmentFileDateModification(m_url, productID, seasonID1, dateFormat, sessionToken);
		Assert.assertTrue(responseDev.code ==200, "Runtime development file was not updated");
		Assert.assertTrue(ifRuntimeContainsAttributes(responseDev.message, 4), "Incorrect number of attributes in the runtime development file in season1");		
		RuntimeRestApi.DateModificationResults responseProd = RuntimeDateUtilities.getProductionFileDateModification(m_url, productID, seasonID1, dateFormat, sessionToken);
		Assert.assertTrue(responseProd.code ==200, "Runtime production file was not changed");
		Assert.assertTrue(ifRuntimeContainsAttributes(responseProd.message, 4), "Incorrect number of attributes in the runtime production file in season1");
		RuntimeRestApi.DateModificationResults prodChanged = RuntimeDateUtilities.getProductionChangedDateModification(m_url, productID, seasonID1, dateFormat, sessionToken);
		Assert.assertTrue(prodChanged.code ==200, "productionChanged.txt file was not changed");

		
		responseDev = RuntimeDateUtilities.getDevelopmentFileDateModification(m_url, productID, seasonID2, dateFormat, sessionToken);
		Assert.assertTrue(responseDev.code ==200, "Runtime development file was not updated");
		Assert.assertTrue(ifRuntimeContainsAttributes(responseDev.message, 4), "Incorrect number of attributes in the runtime development file in season2");		
		responseProd = RuntimeDateUtilities.getProductionFileDateModification(m_url, productID, seasonID2, dateFormat, sessionToken);
		Assert.assertTrue(responseProd.code ==200, "Runtime production file was not changed");	
		Assert.assertTrue(ifRuntimeContainsAttributes(responseProd.message, 4), "Incorrect number of attributes in the runtime production file in season2");
		prodChanged = RuntimeDateUtilities.getProductionChangedDateModification(m_url, productID, seasonID2, dateFormat, sessionToken);
		Assert.assertTrue(prodChanged.code ==200, "productionChanged.txt file was not changed");

	
	}
	
	
	@Test (dependsOnMethods="moveExperimentToProduction", description="Move  experiment and variant  to development")
	public void moveExperimentToDevelopment() throws Exception{
		String dateFormat = an.setDateFormat();

		String variant = exp.getVariant(variantID, sessionToken);
		JSONObject jsonVar = new JSONObject(variant);
		jsonVar.put("stage", "DEVELOPMENT");
		String respVar = exp.updateVariant(variantID, jsonVar.toString(), sessionToken);
		Assert.assertFalse (respVar.contains("error"), "Variant was not updated: " + respVar);
		
		String experiment = exp.getExperiment(experimentID, sessionToken);
		JSONObject json = new JSONObject(experiment);
		json.put("stage", "DEVELOPMENT");
		String response = exp.updateExperiment(experimentID, json.toString(), sessionToken);
		Assert.assertFalse (response.contains("error"), "Experiment was not updated: " + response);
		
		an.setSleep();
		RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getDevelopmentFileDateModification(m_url, productID, seasonID1, dateFormat, sessionToken);
		Assert.assertTrue(responseDev.code ==200, "Runtime development file was not updated");
		Assert.assertTrue(ifRuntimeContainsAttributes(responseDev.message, 4), "Incorrect number of attributes in the runtime development file in season1");		
		RuntimeRestApi.DateModificationResults responseProd = RuntimeDateUtilities.getProductionFileDateModification(m_url, productID, seasonID1, dateFormat, sessionToken);
		Assert.assertTrue(responseProd.code ==200, "Runtime production file was not changed");
		Assert.assertTrue(noExperimentAnalytics(responseProd.message), "Incorrect number of attributes in the runtime production file in season1");
		RuntimeRestApi.DateModificationResults prodChanged = RuntimeDateUtilities.getProductionChangedDateModification(m_url, productID, seasonID1, dateFormat, sessionToken);
		Assert.assertTrue(prodChanged.code ==200, "productionChanged.txt file was not changed");

		
		responseDev = RuntimeDateUtilities.getDevelopmentFileDateModification(m_url, productID, seasonID2, dateFormat, sessionToken);
		Assert.assertTrue(responseDev.code ==200, "Runtime development file was not updated");
		Assert.assertTrue(ifRuntimeContainsAttributes(responseDev.message, 4), "Incorrect number of attributes in the runtime development file in season2");		
		responseProd = RuntimeDateUtilities.getProductionFileDateModification(m_url, productID, seasonID2, dateFormat, sessionToken);
		Assert.assertTrue(responseProd.code ==200, "Runtime production file was not changed");	
		Assert.assertTrue(noExperimentAnalytics(responseProd.message), "Incorrect number of attributes in the runtime production file in season1");
		prodChanged = RuntimeDateUtilities.getProductionChangedDateModification(m_url, productID, seasonID2, dateFormat, sessionToken);
		Assert.assertTrue(prodChanged.code ==200, "productionChanged.txt file was not changed");

	
	}
	
	private boolean noExperimentAnalytics(String input){

		try {
			JSONObject json = new JSONObject(input);
			if (json.getJSONObject("experiments").getJSONArray("experiments").size() == 0)
				return true;
			else
				return false;
		} catch (JSONException e) {
			return false;
		}
	}
	
	private boolean ifRuntimeContainsAttributes(String input, int expectedAttributes){
		
		
		try{
			JSONObject json = new JSONObject(input);

			if (json.containsKey("experiments")){
				JSONArray inputFields = json.getJSONObject("experiments").getJSONArray("experiments")
						.getJSONObject(0).getJSONObject("analytics")
						.getJSONArray("featuresAttributesForAnalytics");
				if (inputFields.size()==expectedAttributes)
					return true;
				else
					return false;
			} else {

				return false;
			}
		} catch (Exception e){
				return false;
		}
	}
	
	private String addVariant(String variantName, String branchName, String stage) throws IOException, JSONException{
		String variant = FileUtils.fileToString(filePath + "experiments/variant1.txt", "UTF-8", false);
		JSONObject variantJson = new JSONObject(variant);
		variantJson.put("name", variantName);
		variantJson.put("branchName", branchName);
		if (stage!=null)
			variantJson.put("stage", stage);
		
		return exp.createVariant(experimentID, variantJson.toString(), sessionToken);

	}
	
	private String addBranch(String seasonId, String branchName) throws JSONException, IOException{
		String branch = FileUtils.fileToString(filePath + "experiments/branch1.txt", "UTF-8", false);
		JSONObject branchJson = new JSONObject(branch);
		branchJson.put("name", branchName);
		return br.createBranch(seasonId, branchJson.toString(), BranchesRestApi.MASTER, sessionToken);

	}
	


	@AfterTest
	private void reset(){
		baseUtils.reset(productID, sessionToken);
	}
}
