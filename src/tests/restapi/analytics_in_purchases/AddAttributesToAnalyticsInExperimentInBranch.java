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

public class AddAttributesToAnalyticsInExperimentInBranch {
	protected String seasonID1;
	protected String seasonID2;
	protected String experimentID;
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
		expJson.put("stage", "PRODUCTION");
		experimentID = exp.createExperiment(productID, expJson.toString(), sessionToken);
		Assert.assertFalse(experimentID.contains("error"), "Experiment was not created: " + experimentID);
		
		branchID1 = addBranch(seasonID1, "branch1");
		Assert.assertFalse(branchID1.contains("error"), "Branch1 was not created in season1: " + branchID1);
		
		branchID2 = addBranch(seasonID2, "branch1");
		Assert.assertFalse(branchID2.contains("error"), "Branch1 was not created in season2: " + branchID2);

		String variantID = addVariant("variant1", "branch1", "PRODUCTION");
		Assert.assertFalse(variantID.contains("error"), "Variant1 was not created: " + variantID);
		
		//enable experiment so a range will be created and the experiment will be published to analytics server
		String airlockExperiment = exp.getExperiment(experimentID, sessionToken);
		Assert.assertFalse(airlockExperiment.contains("error"), "Experiment was not found: " + experimentID);

		expJson = new JSONObject(airlockExperiment);
		expJson.put("enabled", true);
		
		String response = exp.updateExperiment(experimentID, expJson.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Experiment was not updated: " + response);		
	}
	
	//Use api for a single entitlement

	@Test (dependsOnMethods="addExperiment", description="Add entitlement in development to season1")
	public void addEntitlementToSeason1() throws IOException, JSONException, InterruptedException{
		String dateFormat = an.setDateFormat();
		
		//add entitlement
		String entitlement1 = FileUtils.fileToString(filePath + "purchases/inAppPurchase1.txt", "UTF-8", false);
		entitlementID1 = purchasesApi.addPurchaseItemToBranch(seasonID1, branchID1, entitlement1, "ROOT", sessionToken);
		Assert.assertFalse(entitlementID1.contains("error"), "entitlement was not added to the season " + entitlementID1);
		
		String config = FileUtils.fileToString(filePath + "configuration_rule1.txt", "UTF-8", false);
		JSONObject jsonConfig = new JSONObject(config);
		JSONObject newConfiguration = new JSONObject();
		newConfiguration.put("color", "red");
		newConfiguration.put("size", "small");
		jsonConfig.put("configuration", newConfiguration);
		configID1 = purchasesApi.addPurchaseItemToBranch(seasonID1, branchID1, jsonConfig.toString(), entitlementID1, sessionToken);
		Assert.assertFalse(configID1.contains("error"), "Configuration1 was not added to the season" + configID1);

		
		//add attribute to analytics
		JSONArray attributes = new JSONArray();
		JSONObject attr1 = new JSONObject();
		attr1.put("name", "color");
		attr1.put("type", "REGULAR");
		JSONObject attr2 = new JSONObject();
		attr2.put("name", "size");
		attr2.put("type", "REGULAR");
		attributes.add(attr1);
		attributes.add(attr2);

		String response = an.addAttributesToAnalytics(entitlementID1, branchID1, attributes.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Attributes were not added to analytics" + response);
		
		an.setSleep();
		RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getDevelopmentFileDateModification(m_url, productID, seasonID1, dateFormat, sessionToken);
		Assert.assertTrue(responseDev.code ==200, "Runtime development file was not updated");
		Assert.assertTrue(ifRuntimeContainsEntitlement(responseDev.message, 2), "Incorrect number of attributes in the runtime development file in season1");		
		RuntimeRestApi.DateModificationResults responseProd = RuntimeDateUtilities.getProductionFileDateModification(m_url, productID, seasonID1, dateFormat, sessionToken);
		Assert.assertTrue(responseProd.code ==304, "Runtime production file was changed");		
		RuntimeRestApi.DateModificationResults prodChanged = RuntimeDateUtilities.getProductionChangedDateModification(m_url, productID, seasonID1, dateFormat, sessionToken);
		Assert.assertTrue(prodChanged.code ==304, "productionChanged.txt file was changed");

		
		responseDev = RuntimeDateUtilities.getDevelopmentFileDateModification(m_url, productID, seasonID2, dateFormat, sessionToken);
		Assert.assertTrue(responseDev.code ==200, "Runtime development file was not updated");
		Assert.assertTrue(ifRuntimeContainsEntitlement(responseDev.message, 2), "Incorrect number of attributes in the runtime development file in season2");		
		responseProd = RuntimeDateUtilities.getProductionFileDateModification(m_url, productID, seasonID2, dateFormat, sessionToken);
		Assert.assertTrue(responseProd.code ==304, "Runtime production file was changed");		
		prodChanged = RuntimeDateUtilities.getProductionChangedDateModification(m_url, productID, seasonID2, dateFormat, sessionToken);
		Assert.assertTrue(prodChanged.code ==304, "productionChanged.txt file was changed");

	}
	

	@Test (dependsOnMethods="addEntitlementToSeason1", description="Move entitlement to production in season1")
	public void moveEntitlementToProduction() throws IOException, JSONException, InterruptedException{
		String dateFormat = an.setDateFormat();
		
		String entitlement = purchasesApi.getPurchaseItemFromBranch(entitlementID1, branchID1, sessionToken);
		JSONObject json = new JSONObject(entitlement);
		json.put("stage", "PRODUCTION");
		String response = purchasesApi.updatePurchaseItemInBranch(seasonID1, branchID1, entitlementID1, json.toString(), sessionToken);				
		Assert.assertFalse(response.contains("error"), "entitlement was not update: " + response);
		
		
		an.setSleep();
		RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getDevelopmentFileDateModification(m_url, productID, seasonID1, dateFormat, sessionToken);
		Assert.assertTrue(responseDev.code ==200, "Runtime development file was not updated");
		Assert.assertTrue(ifRuntimeContainsEntitlement(responseDev.message, 2), "Incorrect number of attributes in the runtime development file in season1");		
		RuntimeRestApi.DateModificationResults responseProd = RuntimeDateUtilities.getProductionFileDateModification(m_url, productID, seasonID1, dateFormat, sessionToken);
		Assert.assertTrue(responseProd.code ==200, "Runtime production file was changed");
		Assert.assertTrue(ifRuntimeContainsEntitlement(responseProd.message, 2), "Incorrect number of attributes in the runtime production file in season1");
		RuntimeRestApi.DateModificationResults prodChanged = RuntimeDateUtilities.getProductionChangedDateModification(m_url, productID, seasonID1, dateFormat, sessionToken);
		Assert.assertTrue(prodChanged.code ==200, "productionChanged.txt file was changed");

		
		responseDev = RuntimeDateUtilities.getDevelopmentFileDateModification(m_url, productID, seasonID2, dateFormat, sessionToken);
		Assert.assertTrue(responseDev.code ==200, "Runtime development file was not updated");
		Assert.assertTrue(ifRuntimeContainsEntitlement(responseDev.message, 2), "Incorrect number of attributes in the runtime development file in season2");		
		responseProd = RuntimeDateUtilities.getProductionFileDateModification(m_url, productID, seasonID2, dateFormat, sessionToken);
		Assert.assertTrue(responseProd.code ==200, "Runtime production file was changed");	
		Assert.assertTrue(ifRuntimeContainsEntitlement(responseProd.message, 2), "Incorrect number of attributes in the runtime production file in season2");
		prodChanged = RuntimeDateUtilities.getProductionChangedDateModification(m_url, productID, seasonID2, dateFormat, sessionToken);
		Assert.assertTrue(prodChanged.code ==200, "productionChanged.txt file was changed");

	}
	
	@Test (dependsOnMethods="moveEntitlementToProduction", description="Move entitlement to development in season1")
	public void moveEntitlementToDevelopment() throws IOException, JSONException, InterruptedException{
		String dateFormat = an.setDateFormat();
		
		//add entitlement
		
		String entitlement = purchasesApi.getPurchaseItemFromBranch(entitlementID1, branchID1, sessionToken);
		JSONObject json = new JSONObject(entitlement);
		json.put("stage", "DEVELOPMENT");
		String response = purchasesApi.updatePurchaseItemInBranch(seasonID1, branchID1, entitlementID1, json.toString(), sessionToken);				
		Assert.assertFalse(response.contains("error"), "entitlement was not update: " + response);
		
		
		an.setSleep();
		RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getDevelopmentFileDateModification(m_url, productID, seasonID1, dateFormat, sessionToken);
		Assert.assertTrue(responseDev.code ==200, "Runtime development file was not updated");
		Assert.assertTrue(ifRuntimeContainsEntitlement(responseDev.message, 2), "Incorrect number of attributes in the runtime development file in season1");		
		RuntimeRestApi.DateModificationResults responseProd = RuntimeDateUtilities.getProductionFileDateModification(m_url, productID, seasonID1, dateFormat, sessionToken);
		Assert.assertTrue(responseProd.code ==200, "Runtime production file was changed");
		Assert.assertFalse(ifRuntimeContainsEntitlement(responseProd.message, 2), "Incorrect number of attributes in the runtime production file in season1");
		RuntimeRestApi.DateModificationResults prodChanged = RuntimeDateUtilities.getProductionChangedDateModification(m_url, productID, seasonID1, dateFormat, sessionToken);
		Assert.assertTrue(prodChanged.code ==200, "productionChanged.txt file was changed");

		
		responseDev = RuntimeDateUtilities.getDevelopmentFileDateModification(m_url, productID, seasonID2, dateFormat, sessionToken);
		Assert.assertTrue(responseDev.code ==200, "Runtime development file was not updated");
		Assert.assertTrue(ifRuntimeContainsEntitlement(responseDev.message, 2), "Incorrect number of attributes in the runtime development file in season2");		
		responseProd = RuntimeDateUtilities.getProductionFileDateModification(m_url, productID, seasonID2, dateFormat, sessionToken);
		Assert.assertTrue(responseProd.code ==200, "Runtime production file was changed");	
		Assert.assertFalse(ifRuntimeContainsEntitlement(responseProd.message, 2), "Incorrect number of attributes in the runtime production file in season2");
		prodChanged = RuntimeDateUtilities.getProductionChangedDateModification(m_url, productID, seasonID2, dateFormat, sessionToken);
		Assert.assertTrue(prodChanged.code ==200, "productionChanged.txt file was changed");

	}
	
	@Test (dependsOnMethods="moveEntitlementToDevelopment", description="Remove entitlement from analytics in season1")
	public void removeEntitlementFromAnalytics() throws IOException, JSONException, InterruptedException{
		String dateFormat = an.setDateFormat();
		
		
		JSONArray attributes = new JSONArray();
		String response = an.addAttributesToAnalytics(entitlementID1, branchID1, attributes.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Incorrect analytics response: " + response);

		
		an.setSleep();
		RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getDevelopmentFileDateModification(m_url, productID, seasonID1, dateFormat, sessionToken);
		Assert.assertTrue(responseDev.code ==200, "Runtime development file was not updated");
		Assert.assertFalse(ifRuntimeContainsEntitlement(responseDev.message, 0), "Incorrect number of attributes in the runtime development file in season1");		
		RuntimeRestApi.DateModificationResults responseProd = RuntimeDateUtilities.getProductionFileDateModification(m_url, productID, seasonID1, dateFormat, sessionToken);
		Assert.assertTrue(responseProd.code ==304, "Runtime production file was changed");
		RuntimeRestApi.DateModificationResults prodChanged = RuntimeDateUtilities.getProductionChangedDateModification(m_url, productID, seasonID1, dateFormat, sessionToken);
		Assert.assertTrue(prodChanged.code ==304, "productionChanged.txt file was changed");

		
		responseDev = RuntimeDateUtilities.getDevelopmentFileDateModification(m_url, productID, seasonID2, dateFormat, sessionToken);
		Assert.assertTrue(responseDev.code ==200, "Runtime development file was not updated");
		Assert.assertFalse(ifRuntimeContainsEntitlement(responseDev.message, 0), "Incorrect number of attributes in the runtime development file in season2");		
		responseProd = RuntimeDateUtilities.getProductionFileDateModification(m_url, productID, seasonID2, dateFormat, sessionToken);
		Assert.assertTrue(responseProd.code ==304, "Runtime production file was changed");	
		prodChanged = RuntimeDateUtilities.getProductionChangedDateModification(m_url, productID, seasonID2, dateFormat, sessionToken);
		Assert.assertTrue(prodChanged.code ==304, "productionChanged.txt file was changed");

	}
	
	
	@Test (dependsOnMethods="removeEntitlementFromAnalytics", description="Add entitlement to season1 in dev and to season2 in prod")
	public void addEntitlementToBothSeasons() throws IOException, JSONException, InterruptedException{
		String dateFormat = an.setDateFormat();
		
		//add attributes to analytics in season1 in dev stage
		//add entitlement and attribute to analytics
		JSONArray attributes = new JSONArray();
		JSONObject attr1 = new JSONObject();
		attr1.put("name", "color");
		attr1.put("type", "REGULAR");
		JSONObject attr2 = new JSONObject();
		attr2.put("name", "size");
		attr2.put("type", "REGULAR");
		attributes.add(attr1);
		attributes.add(attr2);

		String response = an.addAttributesToAnalytics(entitlementID1, branchID1, attributes.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Attributes were not added to analytics in season1: " + response);
	
		
		//add attributes to season2 in prod stage
		String entitlement1 = FileUtils.fileToString(filePath + "purchases/inAppPurchase1.txt", "UTF-8", false);
		JSONObject json = new JSONObject(entitlement1);
		json.put("stage", "PRODUCTION");
		entitlementID2 = purchasesApi.addPurchaseItemToBranch(seasonID2, branchID2, json.toString(), "ROOT", sessionToken);
		Assert.assertFalse(entitlementID2.contains("error"), "entitlement was not added to the season2 " + entitlementID2);
		
		String config = FileUtils.fileToString(filePath + "configuration_rule1.txt", "UTF-8", false);
		JSONObject jsonConfig = new JSONObject(config);
		JSONObject newConfiguration = new JSONObject();
		newConfiguration.put("color", "red");
		newConfiguration.put("size", "small");
		jsonConfig.put("configuration", newConfiguration);
		String configID2 = purchasesApi.addPurchaseItemToBranch(seasonID2, branchID2, jsonConfig.toString(), entitlementID2, sessionToken);
		Assert.assertFalse(configID2.contains("error"), "Configuration rule was not added to the season" + configID2);
		
		JSONArray attributes2 = new JSONArray();
		JSONObject attr1a = new JSONObject();
		attr1a.put("name", "color");
		attr1a.put("type", "REGULAR");
		JSONObject attr2a = new JSONObject();
		attr2a.put("name", "size");
		attr2a.put("type", "REGULAR");
		attributes2.add(attr1a);
		attributes2.add(attr2a);

		response = an.addAttributesToAnalytics(entitlementID2, branchID2, attributes2.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Attributes were not added to analytics in season2" + response);
		
		
		an.setSleep();
		RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getDevelopmentFileDateModification(m_url, productID, seasonID1, dateFormat, sessionToken);
		Assert.assertTrue(responseDev.code ==200, "Runtime development file was not updated");
		Assert.assertTrue(ifRuntimeContainsEntitlement(responseDev.message, 2), "Incorrect number of attributes in the runtime development file in season1");		
		RuntimeRestApi.DateModificationResults responseProd = RuntimeDateUtilities.getProductionFileDateModification(m_url, productID, seasonID1, dateFormat, sessionToken);
		Assert.assertTrue(responseProd.code ==200, "Runtime production file was changed");		
		Assert.assertTrue(ifRuntimeContainsEntitlement(responseProd.message, 2), "Incorrect number of attributes in the runtime production file in season1");
		RuntimeRestApi.DateModificationResults prodChanged = RuntimeDateUtilities.getProductionChangedDateModification(m_url, productID, seasonID1, dateFormat, sessionToken);
		Assert.assertTrue(prodChanged.code ==200, "productionChanged.txt file was not changed");

		
		responseDev = RuntimeDateUtilities.getDevelopmentFileDateModification(m_url, productID, seasonID2, dateFormat, sessionToken);
		Assert.assertTrue(responseDev.code ==200, "Runtime development file was not updated");
		Assert.assertTrue(ifRuntimeContainsEntitlement(responseDev.message, 2), "Incorrect number of attributes in the runtime development file in season2");		
		responseProd = RuntimeDateUtilities.getProductionFileDateModification(m_url, productID, seasonID2, dateFormat, sessionToken);
		Assert.assertTrue(responseProd.code ==200, "Runtime production file was changed");	
		Assert.assertTrue(ifRuntimeContainsEntitlement(responseProd.message, 2), "Incorrect number of attributes in the runtime production file in season2");
		prodChanged = RuntimeDateUtilities.getProductionChangedDateModification(m_url, productID, seasonID2, dateFormat, sessionToken);
		Assert.assertTrue(prodChanged.code ==200, "productionChanged.txt file was changed");

	}
	
	@Test (dependsOnMethods="addEntitlementToBothSeasons", description="Delete entitlement from season1, leave it in season2")
	public void deleteEntitlement() throws IOException, JSONException, InterruptedException{
		String dateFormat = an.setDateFormat();

		int respCode = purchasesApi.deletePurchaseItemFromBranch(entitlementID1, branchID1, sessionToken);
		Assert.assertTrue(respCode == 200, "entitlement was not deleted");
		
		an.setSleep();
		RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getDevelopmentFileDateModification(m_url, productID, seasonID1, dateFormat, sessionToken);
		Assert.assertTrue(responseDev.code ==200, "Runtime development file was not updated");
		Assert.assertTrue(ifRuntimeContainsEntitlement(responseDev.message, 2), "Incorrect number of attributes in the runtime development file in season1");		
		RuntimeRestApi.DateModificationResults responseProd = RuntimeDateUtilities.getProductionFileDateModification(m_url, productID, seasonID1, dateFormat, sessionToken);
		Assert.assertTrue(responseProd.code ==304, "Runtime production file was changed");		
		RuntimeRestApi.DateModificationResults prodChanged = RuntimeDateUtilities.getProductionChangedDateModification(m_url, productID, seasonID1, dateFormat, sessionToken);
		Assert.assertTrue(prodChanged.code ==304, "productionChanged.txt file was changed");
		
		responseDev = RuntimeDateUtilities.getDevelopmentFileDateModification(m_url, productID, seasonID2, dateFormat, sessionToken);
		Assert.assertTrue(responseDev.code ==200, "Runtime development file was not updated");
		Assert.assertTrue(ifRuntimeContainsEntitlement(responseDev.message, 2), "Incorrect number of attributes in the runtime development file in season1");		
		responseProd = RuntimeDateUtilities.getProductionFileDateModification(m_url, productID, seasonID2, dateFormat, sessionToken);
		Assert.assertTrue(responseProd.code ==304, "Runtime production file was changed");		
		prodChanged = RuntimeDateUtilities.getProductionChangedDateModification(m_url, productID, seasonID2, dateFormat, sessionToken);
		Assert.assertTrue(prodChanged.code ==304, "productionChanged.txt file was changed");
	}

	private boolean ifRuntimeContainsEntitlement(String input, int expectedAttributes){	
		try{
			JSONObject json = new JSONObject(input);

			if (json.containsKey("experiments")){
				JSONArray inputFields = json.getJSONObject("experiments").getJSONArray("experiments")
						.getJSONObject(0).getJSONObject("analytics")
						.getJSONArray("featuresAttributesForAnalytics").getJSONObject(0).getJSONArray("attributes");
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
